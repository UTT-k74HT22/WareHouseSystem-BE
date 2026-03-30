package org.demo.whs.service.impl;

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
import org.demo.whs.entity.dto.response.chatbot.GeminiResponse;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.service.BatchService;
import org.demo.whs.service.ChatBotService;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.ProductService;
import org.demo.whs.service.RedisService;
import org.demo.whs.service.chatbot.ChatBotCommand;
import org.demo.whs.service.chatbot.ChatBotIntentResolver;
import org.demo.whs.service.chatbot.ChatBotResponseFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatBotServiceImpl implements ChatBotService {

    private static final String AI_CACHE_PREFIX = "chatbot:ai:";
    private static final int PRODUCT_LOOKUP_LIMIT = 5;

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
        String originalMessage = request.getMessage().trim();
        ChatBotCommand command = intentResolver.resolve(originalMessage);

        log.info("Chatbot intent={} subject='{}'", command.intent(), command.subjectKeyword());

        try {
            return switch (command.intent()) {
                case GREETING -> new ChatBotResponse(responseFormatter.greeting());
                case HELP -> new ChatBotResponse(responseFormatter.help());
                case PRODUCT_LOOKUP -> handleProductLookup(command);
                case INVENTORY_SUMMARY -> handleInventorySummary(command);
                case INVENTORY_BY_LOCATION -> handleInventoryByLocation(command);
                case BATCH_EXPIRING -> handleBatchExpiring(command);
                case UNKNOWN -> handleUnknown(command);
            };
        } catch (IllegalStateException ex) {
            return new ChatBotResponse(ex.getMessage());
        }
    }

    private ChatBotResponse handleProductLookup(ChatBotCommand command) {
        String keyword = resolveLookupKeyword(command);
        List<ProductResponse> products = findProducts(keyword, PRODUCT_LOOKUP_LIMIT);

        if (products.isEmpty()) {
            return new ChatBotResponse(responseFormatter.noProductMatch(keyword));
        }

        return new ChatBotResponse(responseFormatter.productLookup(products));
    }

    private ChatBotResponse handleInventorySummary(ChatBotCommand command) {
        ProductResponse product = resolveSingleProduct(command);
        if (product == null) {
            return new ChatBotResponse(responseFormatter.noProductMatch(resolveLookupKeyword(command)));
        }

        InventorySummaryResponse summary;
        try {
            summary = inventoryService.getSummaryByProduct(product.getId());
        } catch (NotFoundException ex) {
            summary = emptyInventorySummary(product);
        }
        return new ChatBotResponse(responseFormatter.inventorySummary(product, summary));
    }

    private ChatBotResponse handleInventoryByLocation(ChatBotCommand command) {
        ProductResponse product = resolveSingleProduct(command);
        if (product == null) {
            return new ChatBotResponse(responseFormatter.noProductMatch(resolveLookupKeyword(command)));
        }

        InventoryFilterRequest filterRequest = InventoryFilterRequest.builder()
                .productId(product.getId())
                .build();

        List<InventoryByLocationResponse> locations = inventoryService.getInventoryByLocation(filterRequest);
        if (locations == null || locations.isEmpty()) {
            return new ChatBotResponse(responseFormatter.noInventoryByLocation(product));
        }

        return new ChatBotResponse(responseFormatter.inventoryByLocation(product, locations));
    }

    private ChatBotResponse handleBatchExpiring(ChatBotCommand command) {
        int thresholdDays = command.thresholdDays() != null ? command.thresholdDays() : 30;
        String keyword = command.subjectKeyword();

        if (!StringUtils.hasText(keyword)) {
            List<BatchExpiringResponse> batches = batchService.getExpiringBatches(thresholdDays, null);
            if (batches == null || batches.isEmpty()) {
                return new ChatBotResponse(responseFormatter.noBatchExpiring(thresholdDays, null));
            }
            return new ChatBotResponse(responseFormatter.batchExpiringGlobal(thresholdDays, batches));
        }

        ProductResponse product = resolveSingleProduct(command);
        if (product == null) {
            return new ChatBotResponse(responseFormatter.noProductMatch(resolveLookupKeyword(command)));
        }

        LocalDate deadline = LocalDate.now().plusDays(thresholdDays);
        List<BatchByProductResponse> batches = batchService.getBatchesByProduct(product.getId(), null).stream()
                .filter(batch -> batch.getExpiryDate() != null)
                .filter(batch -> !batch.getExpiryDate().isAfter(deadline))
                .filter(batch -> batch.getInventorySnapshot() != null
                        && batch.getInventorySnapshot().getTotalAvailableQuantity() != null
                        && batch.getInventorySnapshot().getTotalAvailableQuantity().signum() > 0)
                .toList();

        if (batches.isEmpty()) {
            return new ChatBotResponse(responseFormatter.noBatchExpiring(thresholdDays, product));
        }

        return new ChatBotResponse(responseFormatter.batchExpiringByProduct(product, thresholdDays, batches));
    }

    private ChatBotResponse handleUnknown(ChatBotCommand command) {
        String reply = callGeminiWithRetry(buildFallbackPrompt(command.originalMessage()));
        return new ChatBotResponse(reply);
    }

    private ProductResponse resolveSingleProduct(ChatBotCommand command) {
        String keyword = resolveLookupKeyword(command);
        List<ProductResponse> products = findProducts(keyword, PRODUCT_LOOKUP_LIMIT);

        if (products.isEmpty()) {
            return null;
        }

        if (products.size() > 1) {
            throw new IllegalStateException(responseFormatter.ambiguousProducts(keyword, products));
        }

        return products.get(0);
    }

    private String resolveLookupKeyword(ChatBotCommand command) {
        if (StringUtils.hasText(command.subjectKeyword())) {
            return command.subjectKeyword();
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

        String cacheKey = AI_CACHE_PREFIX + prompt.hashCode();
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
                    log.error("Gemini rate limit persists after retry");
                    return responseFormatter.aiFallbackUnavailable();
                }

                log.warn("Gemini rate limit hit, retrying in {} seconds", delaySeconds);
                try {
                    TimeUnit.SECONDS.sleep(delaySeconds);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return responseFormatter.aiFallbackUnavailable();
                }
            } catch (Exception ex) {
                log.error("Gemini API error", ex);
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

    private String buildFallbackPrompt(String originalMessage) {
        return """
                Bạn là trợ lý của hệ thống kho WHS.
                Chỉ được trả lời các câu hỏi mở, hướng dẫn sử dụng, hoặc giải thích tổng quan.
                Không được tự ý đưa ra tồn kho, SKU, batch, giá, hoặc số liệu vận hành nếu prompt không cung cấp dữ liệu.
                Nếu câu hỏi cần dữ liệu thời gian thực, hãy nói rằng bạn không đủ dữ liệu và yêu cầu người dùng hỏi theo SKU hoặc tên sản phẩm.

                Câu hỏi người dùng: %s
                """.formatted(originalMessage);
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
}
