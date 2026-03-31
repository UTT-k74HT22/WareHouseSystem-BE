package org.demo.whs.service.chatbot;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChatBotIntentResolver {

    private static final List<String> GREETING_KEYWORDS = List.of(
            "xin chao",
            "chao",
            "hello",
            "hi",
            "hey"
    );

    private static final List<String> HELP_KEYWORDS = List.of(
            "help",
            "giup",
            "huong dan",
            "tro giup",
            "ban lam duoc gi"
    );

    private static final List<String> INVENTORY_BY_LOCATION_KEYWORDS = List.of(
            "o kho nao",
            "o dau con",
            "ton theo kho",
            "ton theo vi tri",
            "vi tri nao con",
            "kho nao con"
    );

    private static final List<String> INVENTORY_SUMMARY_KEYWORDS = List.of(
            "ton kho",
            "con bao nhieu",
            "con hang khong",
            "so luong ton",
            "so luong",
            "con trong kho",
            "trong kho con",
            "so luong con",
            "hien tai trong kho",
            "kiem tra so luong",
            "available",
            "kiem tra ton"
    );

    private static final List<String> BATCH_EXPIRING_KEYWORDS = List.of(
            "sap het han",
            "het han",
            "expiring batch",
            "lo sap het han",
            "batch sap het han",
            "lo het han",
            "cac lo sap het han",
            "lo hang sap het han"
    );

    private static final List<String> WAREHOUSE_LOOKUP_KEYWORDS = List.of(
            "kho",
            "warehouse",
            "danh sach kho",
            "dia chi kho",
            "thong tin kho"
    );

    private static final List<String> PARTNER_LOOKUP_KEYWORDS = List.of(
            "doi tac",
            "nha cung cap",
            "khach hang",
            "supplier",
            "customer",
            "partner"
    );

    private static final List<String> INBOUND_LOOKUP_KEYWORDS = List.of(
            "nhap hang",
            "po",
            "don nhap",
            "receipt",
            "inbound"
    );

    private static final List<String> OUTBOUND_LOOKUP_KEYWORDS = List.of(
            "xuat hang",
            "don xuat",
            "shipment",
            "outbound"
    );

    private static final List<String> SYSTEM_GUIDE_KEYWORDS = List.of(
            "quy trinh",
            "lam sao de",
            "cach dung",
            "huong dan su dung"
    );

    private static final List<String> PRODUCT_LOOKUP_KEYWORDS = List.of(
            "sku",
            "ma",
            "san pham",
            "tim",
            "gia",
            "thong tin",
            "mo ta"
    );

    private static final Pattern DAYS_PATTERN = Pattern.compile("\\b(\\d{1,3})\\s*(ngay|day|days)\\b");
    private static final Pattern SKU_TOKEN_PATTERN = Pattern.compile("(?i)\\bsku[-_][A-Za-z0-9_-]+\\b");
    private static final Pattern SKU_AFTER_LABEL_PATTERN = Pattern.compile("(?i)\\b(?:sku|ma)\\b(?:\\s*[:#]\\s*|\\s+)([A-Za-z0-9][A-Za-z0-9_-]*)\\b");
    private static final Pattern PO_PATTERN = Pattern.compile("(?i)\\b(po|receipt)[-_][A-Za-z0-9_-]+\\b");
    private static final Pattern SO_PATTERN = Pattern.compile("(?i)\\b(so|shipment)[-_][A-Za-z0-9_-]+\\b");

    public ChatBotCommand resolve(String originalMessage) {
        String safeMessage = originalMessage == null ? "" : originalMessage.trim();
        String normalizedMessage = normalize(safeMessage).toLowerCase();

        if (containsAny(normalizedMessage, GREETING_KEYWORDS)) {
            return new ChatBotCommand(ChatBotIntent.GREETING, safeMessage, normalizedMessage, null, null);
        }

        if (containsAny(normalizedMessage, HELP_KEYWORDS)) {
            return new ChatBotCommand(ChatBotIntent.HELP, safeMessage, normalizedMessage, null, null);
        }

        if (containsAny(normalizedMessage, SYSTEM_GUIDE_KEYWORDS)) {
            return new ChatBotCommand(ChatBotIntent.SYSTEM_GUIDE, safeMessage, normalizedMessage, null, null);
        }

        if (containsAny(normalizedMessage, INVENTORY_BY_LOCATION_KEYWORDS)) {
            return new ChatBotCommand(
                    ChatBotIntent.INVENTORY_BY_LOCATION,
                    safeMessage,
                    normalizedMessage,
                    extractSubjectKeyword(normalizedMessage),
                    null
            );
        }

        if (isBatchExpiringQuestion(normalizedMessage)) {
            return new ChatBotCommand(
                    ChatBotIntent.BATCH_EXPIRING,
                    safeMessage,
                    normalizedMessage,
                    extractSubjectKeyword(normalizedMessage),
                    extractThresholdDays(normalizedMessage)
            );
        }

        if (containsAny(normalizedMessage, BATCH_EXPIRING_KEYWORDS)) {
            return new ChatBotCommand(
                    ChatBotIntent.BATCH_EXPIRING,
                    safeMessage,
                    normalizedMessage,
                    extractSubjectKeyword(normalizedMessage),
                    extractThresholdDays(normalizedMessage)
            );
        }

        if (containsAny(normalizedMessage, INVENTORY_SUMMARY_KEYWORDS)) {
            return new ChatBotCommand(
                    ChatBotIntent.INVENTORY_SUMMARY,
                    safeMessage,
                    normalizedMessage,
                    extractSubjectKeyword(normalizedMessage),
                    null
            );
        }

        if (containsAny(normalizedMessage, INBOUND_LOOKUP_KEYWORDS) || PO_PATTERN.matcher(normalizedMessage).find()) {
            return new ChatBotCommand(ChatBotIntent.INBOUND_LOOKUP, safeMessage, normalizedMessage, extractOrderNumber(normalizedMessage, PO_PATTERN), null);
        }

        if (containsAny(normalizedMessage, OUTBOUND_LOOKUP_KEYWORDS) || SO_PATTERN.matcher(normalizedMessage).find()) {
            return new ChatBotCommand(ChatBotIntent.OUTBOUND_LOOKUP, safeMessage, normalizedMessage, extractOrderNumber(normalizedMessage, SO_PATTERN), null);
        }

        if (containsAny(normalizedMessage, WAREHOUSE_LOOKUP_KEYWORDS)) {
            return new ChatBotCommand(ChatBotIntent.WAREHOUSE_LOOKUP, safeMessage, normalizedMessage, extractSubjectKeyword(normalizedMessage), null);
        }

        if (containsAny(normalizedMessage, PARTNER_LOOKUP_KEYWORDS)) {
            return new ChatBotCommand(ChatBotIntent.PARTNER_LOOKUP, safeMessage, normalizedMessage, extractSubjectKeyword(normalizedMessage), null);
        }

        if (containsAny(normalizedMessage, PRODUCT_LOOKUP_KEYWORDS) || looksLikeSku(safeMessage) || isShortLookup(normalizedMessage)) {
            return new ChatBotCommand(
                    ChatBotIntent.PRODUCT_LOOKUP,
                    safeMessage,
                    normalizedMessage,
                    extractSubjectKeyword(normalizedMessage),
                    null
            );
        }

        return new ChatBotCommand(ChatBotIntent.UNKNOWN, safeMessage, normalizedMessage, null, null);
    }

    private boolean containsAny(String message, List<String> keywords) {
        String paddedMessage = " " + message.trim() + " ";
        return keywords.stream().anyMatch(keyword -> paddedMessage.contains(" " + keyword + " "));
    }

    private boolean looksLikeSku(String originalMessage) {
        return SKU_TOKEN_PATTERN.matcher(originalMessage).find() || SKU_AFTER_LABEL_PATTERN.matcher(originalMessage).find();
    }

    private boolean isShortLookup(String normalizedMessage) {
        return !normalizedMessage.contains(" ") && normalizedMessage.length() >= 3;
    }

    private Integer extractThresholdDays(String normalizedMessage) {
        Matcher matcher = DAYS_PATTERN.matcher(normalizedMessage);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 30;
    }

    private boolean isBatchExpiringQuestion(String normalizedMessage) {
        return normalizedMessage.contains("het han")
                && (normalizedMessage.contains("batch") || normalizedMessage.contains("lo"));
    }

    private String extractOrderNumber(String normalizedMessage, Pattern pattern) {
        Matcher matcher = pattern.matcher(normalizedMessage);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return extractSubjectKeyword(normalizedMessage);
    }

    private String extractSubjectKeyword(String normalizedMessage) {
        Matcher skuTokenMatcher = SKU_TOKEN_PATTERN.matcher(normalizedMessage);
        if (skuTokenMatcher.find()) {
            return skuTokenMatcher.group().trim();
        }

        Matcher skuMatcher = SKU_AFTER_LABEL_PATTERN.matcher(normalizedMessage);
        if (skuMatcher.find()) {
            return skuMatcher.group(1).trim();
        }

        String candidate = normalizedMessage
                .replaceAll("\\b(xin|cho toi|giup toi|vui long|hay|kiem tra|xem|tim|thong tin|ve|cua|cho|san pham|sku|ma|gia|ton kho|con bao nhieu|con hang khong|so luong ton|so luong con|so luong|con trong kho|trong kho con|hien tai trong kho|kiem tra so luong|o kho nao|o dau con|ton theo kho|ton theo vi tri|vi tri nao con|kho nao con|batch|lo|sap het han|het han|trong|ngay|nay|do|ay|no|kho|warehouse|dia chi|doi tac|nha cung cap|khach hang|supplier|customer|partner|nhap hang|po|don nhap|receipt|inbound|xuat hang|so|don xuat|shipment|outbound|quy trinh|lam sao de|cach dung|huong dan)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalizedMessage.contains("ton kho")
                || normalizedMessage.contains("so luong")
                || normalizedMessage.contains("con bao nhieu")) {
            return null;
        }
        return candidate.isBlank() ? null : candidate;
    }

    private String normalize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{M}+", "")
                .replace('\u0111', 'd')
                .replace('\u0110', 'D');
    }
}
