package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.chatbot.ChatBotRequest;
import org.demo.whs.entity.dto.chatbot.ChatBotResponse;
import org.demo.whs.entity.dto.chatbot.GeminiRequest;
import org.demo.whs.entity.dto.chatbot.GeminiResponse;
import org.demo.whs.entity.dto.request.Product.SearchProductRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.service.ChatBotService;
import org.demo.whs.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatBotServiceImpl implements ChatBotService {

    private final ProductService productService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model}")
    private String model;

    @Value("${app.gemini.base-url}")
    private String baseUrl;

    @Override
    public ChatBotResponse chat(ChatBotRequest request) {
        String userMsg = request.getMessage();
        log.info("Processing chat message: {}", userMsg);
        
        String keyword = extractKeyword(userMsg);
        log.info("Extracted keyword for search: '{}'", keyword);

        String productData = "";
        if (keyword != null && !keyword.equalsIgnoreCase("NONE") && !keyword.isBlank()) {
            SearchProductRequest searchReq = new SearchProductRequest();
            searchReq.setName(keyword);
            PageResponse<ProductResponse> products = productService.searchProducts(searchReq, 0, 5);
            
            if (products.getContent() == null || products.getContent().isEmpty()) {
                searchReq.setName(null);
                searchReq.setSku(keyword);
                products = productService.searchProducts(searchReq, 0, 5);
            }

            if (products.getContent() != null && !products.getContent().isEmpty()) {
                productData = products.getContent().stream()
                        .map(p -> String.format("| %s | %s | %s | %s %s |", 
                                p.getSku(), p.getName(), p.getCategoryName(), 
                                p.getSellingPrice() != null ? p.getSellingPrice() : "N/A", 
                                p.getUomName()))
                        .collect(Collectors.joining("\n"));
            }
        }

        String finalPrompt = String.format(
            "Context: You are a Warehouse Assistant for 'WareHouseSystem'. \n" +
            "User Question: '%s' \n" +
            "Database Result (Table format): \n" +
            "| SKU | Tên sản phẩm | Danh mục | Giá bán | \n" +
            "|---|---|---|---| \n%s\n\n" +
            "Task: Provide a concise and elegant response in VIETNAMESE. \n" +
            "Requirements: \n" +
            "1. Use Markdown tables if there are multiple products. \n" +
            "2. Highlight key info (SKU, Name) using bold text. \n" +
            "3. If no product found, be polite and suggest checking the keyword. \n" +
            "4. Keep it professional and avoid unnecessary long sentences.",
            userMsg, productData
        );

        String geminiReply = callGemini(finalPrompt);

        return ChatBotResponse.builder()
                .reply(geminiReply)
                .build();
    }

    private String extractKeyword(String message) {
        String prompt = String.format(
            "Extract ONLY the main product name or SKU from: '%s'. Return ONLY the raw keyword, or 'NONE'.",
            message
        );
        return callGemini(prompt).trim().replaceAll("[\"']", "");
    }

    private String callGemini(String prompt) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + model + ":generateContent")
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        GeminiRequest geminiRequest = GeminiRequest.builder()
                .contents(Collections.singletonList(
                        GeminiRequest.Content.builder()
                                .parts(Collections.singletonList(
                                        GeminiRequest.Part.builder()
                                                .text(prompt)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        try {
            GeminiResponse response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .bodyValue(geminiRequest)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                return response.getCandidates().get(0).getContent().getParts().get(0).getText();
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API: ", e);
            return "⚠️ Đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.";
        }
        return "Tôi không tìm thấy thông tin phù hợp.";
    }
}
