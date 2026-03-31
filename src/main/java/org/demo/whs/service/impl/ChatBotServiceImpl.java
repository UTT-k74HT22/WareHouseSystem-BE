package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Product.SearchProductRequest;
import org.demo.whs.entity.dto.request.chatbot.ChatBotRequest;
import org.demo.whs.entity.dto.request.chatbot.GeminiRequest;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.dto.response.chatbot.ChatBotResponse;
import org.demo.whs.entity.dto.response.chatbot.ChatBotSuggestion;
import org.demo.whs.entity.dto.response.chatbot.GeminiResponse;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.service.BatchService;
import org.demo.whs.service.ChatBotService;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.ProductService;
import org.demo.whs.service.RedisService;
import org.demo.whs.service.chatbot.ChatBotCommand;
import org.demo.whs.service.chatbot.ChatBotConversationContext;
import org.demo.whs.service.chatbot.ChatBotIntent;
import org.demo.whs.service.chatbot.ChatBotIntentResolver;
import org.demo.whs.service.chatbot.ChatBotResponseFormatter;
import org.springframework.beans.factory.annotation.Value;
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
    private final BatchService batchService;
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
            command = new ChatBotCommand(intent, request.getMessage() != null ? request.getMessage().trim() : "", 
                    request.getMessage() != null ? request.getMessage().trim().toLowerCase() : "", null, null);
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
                case PRODUCT_LOOKUP -> handleProductLookup(command, conversationId, conversationContext);
                case INVENTORY_SUMMARY -> handleInventorySummary(command, conversationId, conversationContext);
                case INVENTORY_BY_LOCATION -> handleInventoryByLocation(command, conversationId, conversationContext);
                case BATCH_EXPIRING -> handleBatchExpiring(command, conversationId, conversationContext);
                case UNKNOWN -> handleUnknown(command, conversationId, conversationContext);
            };

            saveConversationContext(conversationId, conversationContext);
            return response;
        } catch (IllegalStateException ex) {
            saveConversationContext(conversationId, conversationContext);
            return buildResponse(conversationId, ex.getMessage(), ChatBotIntent.UNKNOWN, null, null);
        }
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
            if (msg.split("\\s+").length <= 5) {
                return buildResponse(conversationId,
                        "Bạn hãy cung cấp tên hoặc SKU sản phẩm để tôi hỗ trợ chính xác hơn.",
                        command.intent(), null, null);
            }
        }

        if (hasContext && isSimpleProductQuestion(msg)) {
            return buildResponse(conversationId,
                    """
                    Bạn muốn xem:
                    1. Giá
                    2. Tồn kho
                    3. Vị trí
                    """,
                    command.intent(), null, context.getLastProductSku());
        }

        if (msg.split("\\s+").length <= 3) {
            return buildResponse(conversationId,
                    "Bạn muốn hỏi rõ hơn về sản phẩm (giá, tồn kho hay vị trí)?",
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
                Bạn là trợ lý của hệ thống kho WHS.
                Chỉ được trả lời các câu hỏi mở, hướng dẫn sử dụng, hoặc giải thích tổng quan.
                Không được tự ý đưa ra tồn kho, SKU, batch, giá, hoặc số liệu vận hành nếu prompt không cung cấp dữ liệu.
                Nếu câu hỏi cần dữ liệu thời gian thực, hãy nói rằng bạn không đủ dữ liệu và yêu cầu người dùng hỏi theo SKU hoặc tên sản phẩm.

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
