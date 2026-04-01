package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.BusinessPartner.SearchBusinessPartnerRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Product.SearchProductRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersFilterRequest;
import org.demo.whs.entity.dto.request.chatbot.ChatBotRequest;
import org.demo.whs.entity.dto.request.chatbot.GeminiRequest;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.entity.dto.response.chatbot.ChatBotResponse;
import org.demo.whs.entity.dto.response.chatbot.ChatBotSuggestion;
import org.demo.whs.entity.dto.response.chatbot.GeminiResponse;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.service.BatchService;
import org.demo.whs.service.BusinessPartnerService;
import org.demo.whs.service.ChatBotService;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.LocationService;
import org.demo.whs.service.ProductService;
import org.demo.whs.service.PurchaseOrdersService;
import org.demo.whs.service.RedisService;
import org.demo.whs.service.SalesOrdersService;
import org.demo.whs.service.WareHouseService;
import org.demo.whs.service.chatbot.ChatBotCommand;
import org.demo.whs.service.chatbot.ChatBotConversationContext;
import org.demo.whs.service.chatbot.ChatBotIntent;
import org.demo.whs.service.chatbot.ChatBotIntentResolver;
import org.demo.whs.service.chatbot.ChatBotResponseFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatBotServiceImpl implements ChatBotService {

    private static final String AI_CACHE_PREFIX = "chatbot:ai:";
    private static final String CONVERSATION_CONTEXT_PREFIX = "chatbot:conversation:";
    private static final int PRODUCT_LOOKUP_LIMIT = 5;
    private static final long CONVERSATION_CONTEXT_TTL_MINUTES = 30;

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final LocationService locationService;
    private final BatchService batchService;
    private final WareHouseService wareHouseService;
    private final BusinessPartnerService businessPartnerService;
    private final PurchaseOrdersService purchaseOrdersService;
    private final SalesOrdersService salesOrdersService;
    private final WebClient.Builder webClientBuilder;
    private final RedisService redisService;
    private final ChatBotIntentResolver intentResolver;
    private final ChatBotResponseFormatter responseFormatter;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model}")
    private String model;

    @Value("${app.gemini.base-url}")
    private String baseUrl;

    @Override
    public ChatBotResponse chat(ChatBotRequest request) {
        String conversationId = resolveConversationId(request.getConversationId());
        ChatBotConversationContext conversationContext = loadConversationContext(conversationId);

        ChatBotIntent intent = request.getIntent();
        Map<String, Object> payload = request.getPayload();
        String sku = payload != null ? (String) payload.get("sku") : null;
        ChatBotCommand command;

        if (intent != null && StringUtils.hasText(sku)) {
            log.info("Chatbot conversation={} bypass intent={} sku={}", conversationId, intent, sku);
            command = new ChatBotCommand(intent, "", "", sku, null);
        } else if (intent != null) {
            log.info("Chatbot conversation={} bypass intent={} (no sku)", conversationId, intent);
            String originalMessage = request.getMessage() != null ? request.getMessage().trim() : "";
            ChatBotCommand parsedCommand = intentResolver.resolve(originalMessage);
            command = new ChatBotCommand(
                    intent,
                    originalMessage,
                    parsedCommand.normalizedMessage(),
                    parsedCommand.subjectKeyword(),
                    parsedCommand.thresholdDays()
            );
        } else {
            String originalMessage = request.getMessage() != null ? request.getMessage().trim() : "";
            command = intentResolver.resolve(originalMessage);
            command = enrichCommandWithContext(command, conversationContext);
        }

        log.info(
                "Chatbot conversation={} intent={} subject='{}' lastProduct='{}'",
                conversationId,
                command.intent(),
                command.subjectKeyword(),
                conversationContext.getLastProductSku()
        );

        try {
            ChatBotResponse response = switch (command.intent()) {
                case GREETING -> buildResponse(conversationId, responseFormatter.greeting(), command.intent(), null, null);
                case HELP -> buildResponse(conversationId, responseFormatter.help(), command.intent(), null, null);
                case SYSTEM_GUIDE -> buildResponse(conversationId, responseFormatter.systemGuide(), command.intent(), null, null);
                case PRODUCT_LOOKUP -> handleProductLookup(command, conversationId, conversationContext);
                case INVENTORY_SUMMARY -> handleInventorySummary(command, conversationId, conversationContext);
                case INVENTORY_BY_LOCATION -> handleInventoryByLocation(command, conversationId, conversationContext);
                case BATCH_EXPIRING -> handleBatchExpiring(command, conversationId, conversationContext);
                case WAREHOUSE_LOOKUP -> handleWarehouseLookup(command, conversationId);
                case PARTNER_LOOKUP -> handlePartnerLookup(command, conversationId);
                case INBOUND_LOOKUP -> handleInboundLookup(command, conversationId);
                case OUTBOUND_LOOKUP -> handleOutboundLookup(command, conversationId);
                case UNKNOWN -> handleUnknown(command, conversationId, conversationContext);
            };

            saveConversationContext(conversationId, conversationContext);
            return response;
        } catch (IllegalStateException ex) {
            saveConversationContext(conversationId, conversationContext);
            return buildResponse(conversationId, ex.getMessage(), ChatBotIntent.UNKNOWN, null, null);
        }
    }

    private ChatBotResponse handleWarehouseLookup(ChatBotCommand command, String conversationId) {
        if (isWarehouseLocationQuery(command.normalizedMessage())) {
            return handleWarehouseLocations(command, conversationId);
        }

        String keyword = command.subjectKeyword();
        List<WareHouseResponse> warehouses = wareHouseService.getWareHouses();

        if (StringUtils.hasText(keyword)) {
            warehouses = warehouses.stream()
                    .filter(w -> containsNormalized(w.getName(), keyword)
                            || containsNormalized(w.getCode(), keyword))
                    .collect(Collectors.toList());
        }

        return buildResponse(conversationId, responseFormatter.warehouseLookup(warehouses), command.intent(), null, keyword);
    }

    private ChatBotResponse handleWarehouseLocations(ChatBotCommand command, String conversationId) {
        String warehouseKeyword = extractWarehouseKeyword(command);

        if (!StringUtils.hasText(warehouseKeyword)) {
            PageResponse<LocationResponse> page = locationService.getAllLocations(0, 20);
            List<LocationResponse> locations = page.getContent() != null ? page.getContent() : List.of();
            return buildResponse(
                    conversationId,
                    responseFormatter.warehouseLocationsOverview(locations),
                    command.intent(),
                    null,
                    null
            );
        }

        List<WareHouseResponse> warehouses = wareHouseService.getWareHouses().stream()
                .filter(w -> containsNormalized(w.getName(), warehouseKeyword)
                        || containsNormalized(w.getCode(), warehouseKeyword))
                .collect(Collectors.toList());

        if (warehouses.isEmpty()) {
            return buildResponse(
                    conversationId,
                    "Không tìm thấy kho nào phù hợp với từ khóa '" + warehouseKeyword + "'.",
                    command.intent(),
                    null,
                    warehouseKeyword
            );
        }

        if (warehouses.size() > 1) {
            return buildResponse(
                    conversationId,
                    "Tìm thấy nhiều kho phù hợp với từ khóa '" + warehouseKeyword + "'. Hãy chọn rõ hơn:\n"
                            + warehouses.stream()
                            .map(w -> "- " + safeWarehouseLabel(w))
                            .collect(Collectors.joining("\n")),
                    command.intent(),
                    null,
                    warehouseKeyword
            );
        }

        WareHouseResponse warehouse = warehouses.get(0);
        PageResponse<LocationResponse> page = locationService.getLocationsByWarehouse(warehouse.getId(), 0, 50);
        List<LocationResponse> locations = page.getContent() != null ? page.getContent() : List.of();

        return buildResponse(
                conversationId,
                responseFormatter.warehouseLocations(warehouse, locations),
                command.intent(),
                null,
                warehouseKeyword
        );
    }

    private ChatBotResponse handlePartnerLookup(ChatBotCommand command, String conversationId) {
        String keyword = command.subjectKeyword();
        if (!StringUtils.hasText(keyword)) {
            return buildResponse(conversationId, "Bạn hãy cung cấp tên hoặc mã đối tác để tôi tìm kiếm.", command.intent(), null, null);
        }

        SearchBusinessPartnerRequest searchRequest = new SearchBusinessPartnerRequest();
        searchRequest.setName(keyword);
        PageResponse<BusinessPartnerResponse> pageResponse = businessPartnerService.searchBusinessPartners(searchRequest, 0, 10);
        List<BusinessPartnerResponse> partners = pageResponse.getContent() != null ? pageResponse.getContent() : List.of();

        if (partners.isEmpty()) {
            searchRequest.setName(null);
            searchRequest.setCode(keyword);
            pageResponse = businessPartnerService.searchBusinessPartners(searchRequest, 0, 10);
            partners = pageResponse.getContent() != null ? pageResponse.getContent() : List.of();
        }

        return buildResponse(conversationId, responseFormatter.partnerLookup(partners), command.intent(), null, keyword);
    }

    private ChatBotResponse handleInboundLookup(ChatBotCommand command, String conversationId) {
        String keyword = command.subjectKeyword();
        if (!StringUtils.hasText(keyword)) {
            return buildResponse(conversationId, "Bạn hãy cung cấp mã đơn nhập (PO) hoặc từ khóa để tôi tìm kiếm.", command.intent(), null, null);
        }

        PurchaseOrdersFilterRequest filter = PurchaseOrdersFilterRequest.builder()
                .purchaseOrderNumber(keyword)
                .build();
        PageResponse<PurchaseOrdersResponse> pageResponse = purchaseOrdersService.getAll(filter, PageRequest.of(0, 10));
        List<PurchaseOrdersResponse> orders = pageResponse.getContent() != null ? pageResponse.getContent() : List.of();

        PurchaseOrdersResponse exactOrder = orders.stream()
                .filter(order -> keyword.equalsIgnoreCase(order.getPurchaseOrderNumber()))
                .findFirst()
                .map(order -> purchaseOrdersService.getById(order.getId()))
                .orElse(null);

        if (exactOrder != null) {
            return buildResponse(conversationId, responseFormatter.purchaseOrderDetail(exactOrder), command.intent(), null, keyword);
        }

        return buildResponse(conversationId, responseFormatter.purchaseOrderLookup(orders), command.intent(), null, keyword);
    }

    private ChatBotResponse handleOutboundLookup(ChatBotCommand command, String conversationId) {
        String keyword = command.subjectKeyword();
        if (!StringUtils.hasText(keyword)) {
            return buildResponse(conversationId, "Bạn hãy cung cấp mã đơn xuất (SO) hoặc từ khóa để tôi tìm kiếm.", command.intent(), null, null);
        }

        SalesOrdersFilterRequest filter = SalesOrdersFilterRequest.builder()
                .soNumber(keyword)
                .build();
        PageResponse<SalesOrdersResponse> pageResponse = salesOrdersService.getAll(filter, PageRequest.of(0, 10));
        List<SalesOrdersResponse> orders = pageResponse.getContent() != null ? pageResponse.getContent() : List.of();

        SalesOrdersResponse exactOrder = orders.stream()
                .filter(order -> keyword.equalsIgnoreCase(order.getSoNumber()))
                .findFirst()
                .map(order -> salesOrdersService.getById(order.getId()))
                .orElse(null);

        if (exactOrder != null) {
            return buildResponse(conversationId, responseFormatter.salesOrderDetail(exactOrder), command.intent(), null, keyword);
        }

        return buildResponse(conversationId, responseFormatter.salesOrderLookup(orders), command.intent(), null, keyword);
    }

    private ChatBotResponse handleProductLookup(
            ChatBotCommand command,
            String conversationId,
            ChatBotConversationContext conversationContext
    ) {
        String keyword = resolveLookupKeyword(command, conversationContext);
        List<ProductResponse> products = findProducts(keyword, PRODUCT_LOOKUP_LIMIT);

        if (products.isEmpty()) {
            rememberConversation(conversationContext, command.intent(), null, command.thresholdDays(), keyword);
            return buildResponse(conversationId, responseFormatter.noProductMatch(keyword), command.intent(), null, keyword);
        }

        if (products.size() == 1) {
            rememberConversation(conversationContext, command.intent(), products.get(0), command.thresholdDays(), keyword);
        }

        return buildResponse(conversationId, responseFormatter.productLookup(products), command.intent(), products.size() == 1 ? products.get(0) : null, keyword);
    }

    private ChatBotResponse handleInventorySummary(
            ChatBotCommand command,
            String conversationId,
            ChatBotConversationContext conversationContext
    ) {
        ProductResponse product = resolveSingleProduct(command, conversationContext);
        if (product == null) {
            String keyword = resolveLookupKeyword(command, conversationContext);
            rememberConversation(conversationContext, command.intent(), null, command.thresholdDays(), keyword);
            return buildResponse(conversationId, responseFormatter.noProductMatch(keyword), command.intent(), null, keyword);
        }

        InventorySummaryResponse summary;
        try {
            summary = inventoryService.getSummaryByProduct(product.getId());
        } catch (NotFoundException ex) {
            summary = emptyInventorySummary(product);
        }

        rememberConversation(conversationContext, command.intent(), product, command.thresholdDays(), resolveLookupKeyword(command, conversationContext));
        return buildResponse(conversationId, responseFormatter.inventorySummary(product, summary), command.intent(), product, resolveLookupKeyword(command, conversationContext));
    }

    private ChatBotResponse handleInventoryByLocation(
            ChatBotCommand command,
            String conversationId,
            ChatBotConversationContext conversationContext
    ) {
        ProductResponse product = resolveSingleProduct(command, conversationContext);
        if (product == null) {
            String keyword = resolveLookupKeyword(command, conversationContext);
            rememberConversation(conversationContext, command.intent(), null, command.thresholdDays(), keyword);
            return buildResponse(conversationId, responseFormatter.noProductMatch(keyword), command.intent(), null, keyword);
        }

        InventoryFilterRequest filterRequest = InventoryFilterRequest.builder()
                .productId(product.getId())
                .build();

        List<InventoryByLocationResponse> locations = inventoryService.getInventoryByLocation(filterRequest);
        rememberConversation(conversationContext, command.intent(), product, command.thresholdDays(), resolveLookupKeyword(command, conversationContext));

        if (locations == null || locations.isEmpty()) {
            return buildResponse(conversationId, responseFormatter.noInventoryByLocation(product), command.intent(), product, resolveLookupKeyword(command, conversationContext));
        }

        return buildResponse(conversationId, responseFormatter.inventoryByLocation(product, locations), command.intent(), product, resolveLookupKeyword(command, conversationContext));
    }

    private ChatBotResponse handleBatchExpiring(
            ChatBotCommand command,
            String conversationId,
            ChatBotConversationContext conversationContext
    ) {
        int thresholdDays = command.thresholdDays() != null ? command.thresholdDays() : 30;
        String keyword = resolveLookupKeyword(command, conversationContext);

        if (!StringUtils.hasText(keyword)) {
            List<BatchExpiringResponse> batches = batchService.getExpiringBatches(thresholdDays, null);
            rememberConversation(conversationContext, command.intent(), null, thresholdDays, null);

            if (batches == null || batches.isEmpty()) {
                return buildResponse(conversationId, responseFormatter.noBatchExpiring(thresholdDays, null), command.intent(), null, null);
            }

            return buildResponse(conversationId, responseFormatter.batchExpiringGlobal(thresholdDays, batches), command.intent(), null, null);
        }

        ProductResponse product = resolveSingleProduct(command, conversationContext);
        if (product == null) {
            String resolvedKeyword = resolveLookupKeyword(command, conversationContext);
            rememberConversation(conversationContext, command.intent(), null, thresholdDays, resolvedKeyword);
            return buildResponse(conversationId, responseFormatter.noProductMatch(resolvedKeyword), command.intent(), null, resolvedKeyword);
        }

        LocalDate deadline = LocalDate.now().plusDays(thresholdDays);
        List<BatchByProductResponse> batches = batchService.getBatchesByProduct(product.getId(), null).stream()
                .filter(batch -> batch.getExpiryDate() != null)
                .filter(batch -> !batch.getExpiryDate().isAfter(deadline))
                .filter(batch -> batch.getInventorySnapshot() != null
                        && batch.getInventorySnapshot().getTotalAvailableQuantity() != null
                        && batch.getInventorySnapshot().getTotalAvailableQuantity().signum() > 0)
                .toList();

        rememberConversation(conversationContext, command.intent(), product, thresholdDays, resolveLookupKeyword(command, conversationContext));

        if (batches.isEmpty()) {
            return buildResponse(conversationId, responseFormatter.noBatchExpiring(thresholdDays, product), command.intent(), product, resolveLookupKeyword(command, conversationContext));
        }

        return buildResponse(conversationId, responseFormatter.batchExpiringByProduct(product, thresholdDays, batches), command.intent(), product, resolveLookupKeyword(command, conversationContext));
    }

    private ChatBotResponse handleUnknown(
            ChatBotCommand command,
            String conversationId,
            ChatBotConversationContext context
    ) {
        String msg = command.normalizedMessage();
        boolean hasContext = StringUtils.hasText(context.getLastProductSku());

        if (!hasContext) {
            if (msg.split("\\s+").length <= 4) {
                return buildResponse(conversationId,
                        "Tôi có thể hỗ trợ tra cứu Sản phẩm, Kho, Đối tác hoặc Đơn hàng. Bạn muốn tìm thông tin gì?",
                        command.intent(), null, null);
            }
        }

        if (hasContext && isSimpleProductQuestion(msg)) {
            return buildResponse(conversationId,
                    """
                    Bạn muốn xem gì về sản phẩm này:
                    1. Giá & Thông tin
                    2. Tồn kho tổng quát
                    3. Vị trí kho chi tiết
                    """,
                    command.intent(), null, context.getLastProductSku());
        }

        rememberConversation(context, command.intent(), null,
                command.thresholdDays(), command.subjectKeyword());

        String reply = callGeminiWithRetry(
                buildFallbackPrompt(command.originalMessage(), context)
        );

        return buildResponse(conversationId, reply, command.intent(), null, context.getLastProductSku());
    }

    private ProductResponse resolveSingleProduct(ChatBotCommand command, ChatBotConversationContext context) {
        String keyword = resolveLookupKeyword(command, context);
        List<ProductResponse> products = findProducts(keyword, PRODUCT_LOOKUP_LIMIT);

        if (products.isEmpty()) {
            return null;
        }

        if (products.size() > 1) {
            throw new IllegalStateException(responseFormatter.ambiguousProducts(keyword, products));
        }

        return products.get(0);
    }

    private String resolveLookupKeyword(ChatBotCommand command, ChatBotConversationContext context) {
        if (StringUtils.hasText(command.subjectKeyword())) {
            return command.subjectKeyword();
        }
        if (!StringUtils.hasText(command.originalMessage()) && context != null 
                && StringUtils.hasText(context.getLastProductSku())) {
            return context.getLastProductSku();
        }
        if (!StringUtils.hasText(command.originalMessage())) {
            return null;
        }
        return command.originalMessage();
    }

    private List<ProductResponse> findProducts(String keyword, int size) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        SearchProductRequest byName = new SearchProductRequest();
        byName.setName(keyword.trim());
        PageResponse<ProductResponse> byNamePage = productService.searchProducts(byName, 0, size);
        if (byNamePage.getContent() != null && !byNamePage.getContent().isEmpty()) {
            return byNamePage.getContent();
        }

        SearchProductRequest bySku = new SearchProductRequest();
        bySku.setSku(keyword.trim());
        PageResponse<ProductResponse> bySkuPage = productService.searchProducts(bySku, 0, size);
        return bySkuPage.getContent() != null ? bySkuPage.getContent() : List.of();
    }

    private boolean isWarehouseLocationQuery(String normalizedMessage) {
        if (!StringUtils.hasText(normalizedMessage)) {
            return false;
        }

        return normalizedMessage.contains("vi tri")
                || normalizedMessage.contains("location")
                || normalizedMessage.contains("slot");
    }

    private String extractWarehouseKeyword(ChatBotCommand command) {
        String source = StringUtils.hasText(command.subjectKeyword())
                ? command.subjectKeyword()
                : command.normalizedMessage();

        if (!StringUtils.hasText(source)) {
            return null;
        }

        String keyword = source
                .replaceAll("\\b(cac|tat ca|danh sach|xem|cho toi|giup toi|vui long|vi tri|location|slot|cua|trong|thuoc|tai|o|warehouse|kho)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();

        return keyword.isBlank() ? null : keyword;
    }

    private boolean containsNormalized(String source, String keyword) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(keyword)) {
            return false;
        }

        return normalizeText(source).contains(normalizeText(keyword));
    }

    private String normalizeText(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D')
                .toLowerCase()
                .trim();
    }

    private String safeWarehouseLabel(WareHouseResponse warehouse) {
        return (warehouse.getName() != null ? warehouse.getName() : "N/A")
                + " (`" + (warehouse.getCode() != null ? warehouse.getCode() : "N/A") + "`)";
    }

    private String callGeminiWithRetry(String prompt) {

        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(model) || !StringUtils.hasText(baseUrl)) {
            return responseFormatter.aiFallbackUnavailable();
        }

        // normalize prompt (QUAN TRỌNG)
        String normalized = prompt.toLowerCase().trim().replaceAll("\\s+", " ");
        String cacheKey = AI_CACHE_PREFIX + normalized;

        Optional<String> cached = redisService.get(cacheKey, String.class);
        if (cached.isPresent()) {
            log.info("Chatbot AI cache hit");
            return cached.get();
        }

        int maxAttempts = 2;
        int delaySeconds = 2;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                String result = doCallGemini(prompt);
                redisService.set(cacheKey, result, 10, TimeUnit.MINUTES);
                return result;
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.TooManyRequests ex) {

                if (attempt == maxAttempts - 1) {
                    return responseFormatter.aiFallbackUnavailable();
                }

                try {
                    TimeUnit.SECONDS.sleep(delaySeconds);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return responseFormatter.aiFallbackUnavailable();
                }

            } catch (Exception ex) {
                return responseFormatter.aiFallbackUnavailable();
            }
        }

        return responseFormatter.aiFallbackUnavailable();
    }
    private String doCallGemini(String prompt) {
        String url = baseUrl + model + ":generateContent?key=" + apiKey;
        GeminiRequest geminiRequest = GeminiRequest.builder()
                .contents(Collections.singletonList(
                        GeminiRequest.Content.builder()
                                .parts(Collections.singletonList(GeminiRequest.Part.builder().text(prompt).build()))
                                .build()
                ))
                .build();

        GeminiResponse response = webClientBuilder.build()
                .post()
                .uri(url)
                .bodyValue(geminiRequest)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .block();

        if (response != null
                && response.getCandidates() != null
                && !response.getCandidates().isEmpty()
                && response.getCandidates().get(0).getContent() != null
                && response.getCandidates().get(0).getContent().getParts() != null
                && !response.getCandidates().get(0).getContent().getParts().isEmpty()) {
            return response.getCandidates().get(0).getContent().getParts().get(0).getText();
        }

        return responseFormatter.aiFallbackUnavailable();
    }

    private String buildFallbackPrompt(String originalMessage, ChatBotConversationContext conversationContext) {
        return """
                Bạn là trợ lý thông minh của hệ thống quản lý kho WHS (Warehouse Management System).
                
                Nhiệm vụ:
                1. Trả lời các câu hỏi mở về quy trình vận hành kho (nhập kho, xuất kho, kiểm kê, v.v.).
                2. Hướng dẫn người dùng cách sử dụng các tính năng tra cứu của hệ thống.
                
                Dữ liệu hệ thống có thể tra cứu trực tiếp (Local Lookups):
                - Sản phẩm (Product/SKU)
                - Kho bãi (Warehouse)
                - Đối tác (Business Partner/Supplier/Customer)
                - Đơn hàng (Purchase Order/Sales Order)
                
                Lưu ý quan trọng:
                - Không tự bịa ra số liệu tồn kho, mã đơn hàng, hoặc giá cả nếu không có trong ngữ cảnh.
                - Nếu người dùng hỏi về thông tin cụ thể (ví dụ: "Đơn hàng PO-123 ở đâu?"), hãy bảo họ sử dụng đúng từ khóa hoặc cung cấp thêm mã để hệ thống tra cứu trực tiếp.
                - Luôn giữ thái độ chuyên nghiệp, ngắn gọn.

                Ngữ cảnh gần nhất: %s
                Câu hỏi người dùng: %s
                """.formatted(buildContextSummary(conversationContext), originalMessage);
    }

    private InventorySummaryResponse emptyInventorySummary(ProductResponse product) {
        return InventorySummaryResponse.builder()
                .productId(product.getId())
                .productSku(product.getSku())
                .productName(product.getName())
                .totalOnHandQuantity(java.math.BigDecimal.ZERO)
                .totalReservedQuantity(java.math.BigDecimal.ZERO)
                .warehouseCount(0L)
                .locationCount(0L)
                .build();
    }

    private ChatBotConversationContext loadConversationContext(String conversationId) {
        return redisService.getOptional(conversationContextKey(conversationId), new TypeReference<ChatBotConversationContext>() {
        }).orElseGet(() -> ChatBotConversationContext.builder()
                .conversationId(conversationId)
                .build());
    }

    private void saveConversationContext(String conversationId, ChatBotConversationContext conversationContext) {
        conversationContext.setConversationId(conversationId);
        conversationContext.setUpdatedAt(LocalDateTime.now());
        redisService.saveWithTTL(
                conversationContextKey(conversationId),
                conversationContext,
                CONVERSATION_CONTEXT_TTL_MINUTES,
                TimeUnit.MINUTES
        );
    }

    private ChatBotCommand enrichCommandWithContext(
            ChatBotCommand command,
            ChatBotConversationContext conversationContext
    ) {
        if (!StringUtils.hasText(conversationContext.getLastProductSku()) || StringUtils.hasText(command.subjectKeyword())) {
            return command;
        }

        if (command.intent() == ChatBotIntent.INVENTORY_SUMMARY
                || command.intent() == ChatBotIntent.INVENTORY_BY_LOCATION) {
            return new ChatBotCommand(
                    command.intent(),
                    command.originalMessage(),
                    command.normalizedMessage(),
                    conversationContext.getLastProductSku(),
                    command.thresholdDays()
            );
        }

        if ((command.intent() == ChatBotIntent.PRODUCT_LOOKUP || command.intent() == ChatBotIntent.BATCH_EXPIRING)
                && referencesCurrentSubject(command.normalizedMessage())) {
            return new ChatBotCommand(
                    command.intent(),
                    command.originalMessage(),
                    command.normalizedMessage(),
                    conversationContext.getLastProductSku(),
                    command.thresholdDays()
            );
        }

        return command;
    }

    private boolean referencesCurrentSubject(String normalizedMessage) {
        if (!StringUtils.hasText(normalizedMessage)) {
            return false;
        }

        return normalizedMessage.contains(" san pham nay")
                || normalizedMessage.contains(" mat hang nay")
                || normalizedMessage.contains(" sku nay")
                || normalizedMessage.contains(" sp nay")
                || normalizedMessage.endsWith(" nay")
                || normalizedMessage.contains(" san pham do")
                || normalizedMessage.contains(" mat hang do")
                || normalizedMessage.endsWith(" do")
                || normalizedMessage.contains(" cua no")
                || normalizedMessage.contains(" cua san pham nay");
    }

    private void rememberConversation(
            ChatBotConversationContext conversationContext,
            ChatBotIntent intent,
            ProductResponse product,
            Integer thresholdDays,
            String resolvedKeyword
    ) {
        conversationContext.setLastIntent(intent);
        conversationContext.setLastResolvedKeyword(resolvedKeyword);
        conversationContext.setLastThresholdDays(thresholdDays);

        if (product != null) {
            conversationContext.setLastProductId(product.getId());
            conversationContext.setLastProductSku(product.getSku());
            conversationContext.setLastProductName(product.getName());
        }
    }

    private String buildContextSummary(ChatBotConversationContext conversationContext) {
        if (!StringUtils.hasText(conversationContext.getLastProductSku())) {
            return "Không có.";
        }

        return "lastIntent=%s, product=%s - %s".formatted(
                conversationContext.getLastIntent(),
                conversationContext.getLastProductSku(),
                StringUtils.hasText(conversationContext.getLastProductName())
                        ? conversationContext.getLastProductName()
                        : "N/A"
        );
    }

    private String resolveConversationId(String conversationId) {
        return StringUtils.hasText(conversationId) ? conversationId.trim() : UUID.randomUUID().toString();
    }

    private String conversationContextKey(String conversationId) {
        return CONVERSATION_CONTEXT_PREFIX + conversationId;
    }

    private ChatBotResponse buildResponse(String conversationId, String reply, ChatBotIntent intent, ProductResponse product, String keyword) {
        List<ChatBotSuggestion> suggestions = responseFormatter.getSuggestions(intent, product, keyword);
        return ChatBotResponse.builder()
                .reply(reply)
                .conversationId(conversationId)
                .suggestions(suggestions)
                .build();
    }
    private boolean isSimpleProductQuestion(String msg) {
        return msg.contains("giá")
                || msg.contains("bao nhiêu")
                || msg.contains("còn không")
                || msg.contains("tồn")
                || msg.contains("số lượng")
                || msg.contains("ở đây")
                || msg.contains("vi trí")
                || msg.contains("kho nào");
    }
}
