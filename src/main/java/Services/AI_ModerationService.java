package Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * AI_ModerationService — OpenRouter with local keyword fallback
 *
 * ✅ Get your FREE key at: https://openrouter.ai/settings/keys
 *    ⚠ Create a new key with "No expiration"!
 *
 * Strategy:
 *  1. Try OpenRouter free models (with reasoning-model content fix)
 *  2. If ALL API calls fail → fall back to local keyword moderation (always works)
 */
public class AI_ModerationService {

    // ── Paste your key from https://openrouter.ai/settings/keys ─────────────
    private static final String API_KEY = "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";
    // ─────────────────────────────────────────────────────────────────────────

    private static final String[] FREE_MODELS = {
            "openrouter/auto",
            "meta-llama/llama-3.3-70b-instruct:free",
            "mistralai/mistral-small-3.1-24b-instruct:free",
            "google/gemma-3-27b-it:free",
            "qwen/qwq-32b:free",
            "deepseek/deepseek-chat-v3-0324:free",
            "tngtech/deepseek-r1t-chimera:free"
    };

    private static final String API_URL             = "https://openrouter.ai/api/v1/chat/completions";
    private static final double AUTO_HIDE_THRESHOLD = 0.7;

    // ── Local keyword moderation — always works, no API needed ───────────────
    // Organized by severity score
    private static final Map<Double, List<String>> KEYWORD_RULES = Map.of(
            0.95, List.of(
                    "i will kill you", "i'll kill you", "kill yourself", "kys",
                    "i want to kill", "i'm going to kill", "bomb threat", "shoot you",
                    "i will murder", "death threat", "rape you", "i will rape",
                    "child porn", "cp porn", "molest"
            ),
            0.85, List.of(
                    "kill you", "murder you", "stab you", "shoot you", "blow up",
                    "terrorist", "terrorism", "jihad", "nazi", "genocide",
                    "hang yourself", "go die", "i hope you die", "you should die"
            ),
            0.70, List.of(
                    "fuck you", "fucking idiot", "piece of shit", "go fuck yourself",
                    "motherfucker", "die bitch", "stupid bitch", "dumb ass",
                    "hate you", "you're pathetic", "loser", "worthless"
            ),
            0.45, List.of(
                    "idiot", "stupid", "dumb", "moron", "jerk", "asshole",
                    "shut up", "you suck", "hate this", "awful", "terrible"
            )
    );
    // ─────────────────────────────────────────────────────────────────────────

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // =========================================================================
    // Public API
    // =========================================================================

    public ModerationResult moderate(String content) {
        if (content == null || content.isBlank()) {
            return new ModerationResult(0.0, "Empty content", false);
        }

        // ── Step 1: Try AI moderation via OpenRouter ──────────────────────────
        for (String model : FREE_MODELS) {
            try {
                System.out.println("[AI Moderation] Trying model: " + model);
                ModerationResult result = callOpenRouterAPI(content, model);
                if (result != null) {
                    System.out.println("[AI Moderation] ✅ Success with: " + model);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("[AI Moderation] Error with " + model + ": " + e.getMessage());
            }
        }

        // ── Step 2: All API models failed → use local keyword moderation ─────
        System.out.println("[AI Moderation] ⚠ All API models failed. Using local keyword moderation.");
        return localKeywordModeration(content);
    }

    // =========================================================================
    // Local Keyword Moderation (offline, always reliable)
    // =========================================================================

    private ModerationResult localKeywordModeration(String content) {
        String lower = content.toLowerCase().trim();

        // Check each severity tier from highest to lowest
        double[] tiers = {0.95, 0.85, 0.70, 0.45};
        for (double score : tiers) {
            List<String> keywords = KEYWORD_RULES.get(score);
            for (String keyword : keywords) {
                if (lower.contains(keyword)) {
                    boolean shouldHide = score >= AUTO_HIDE_THRESHOLD;
                    String reason = buildLocalReason(score, keyword);
                    System.out.println("[AI Moderation] 🔍 Local match: \"" + keyword
                            + "\" → score=" + score + " shouldHide=" + shouldHide);
                    return new ModerationResult(score, reason, shouldHide);
                }
            }
        }

        // No keywords matched — content is clean
        System.out.println("[AI Moderation] 🔍 Local scan: no harmful keywords found.");
        return new ModerationResult(0.05, "No harmful content detected.", false);
    }

    private String buildLocalReason(double score, String matchedKeyword) {
        if (score >= 0.90) return "Contains explicit threat or severely harmful content.";
        if (score >= 0.80) return "Contains violent or threatening language.";
        if (score >= 0.65) return "Contains highly offensive or harmful language.";
        return "Contains mildly inappropriate language.";
    }

    // =========================================================================
    // OpenRouter API Call
    // =========================================================================

    private ModerationResult callOpenRouterAPI(String content, String model) throws Exception {

        String systemPrompt =
                "You are a strict content moderation AI for a public blog platform. " +
                        "Analyze the post content provided by the user and return ONLY a valid JSON object. " +
                        "No preamble, no explanation, no markdown backticks, just raw JSON. " +
                        "The JSON must have exactly two fields:\n" +
                        "  \"score\": a float between 0.0 (completely safe) and 1.0 (extremely harmful)\n" +
                        "  \"reason\": a short English explanation of the score, maximum 20 words\n\n" +
                        "Scoring guidelines:\n" +
                        "  0.0 to 0.2 : normal, acceptable, everyday content\n" +
                        "  0.3 to 0.5 : mildly inappropriate (mild insults, borderline language)\n" +
                        "  0.6 to 0.79: moderately harmful (harassment, targeted hate, graphic content)\n" +
                        "  0.8 to 1.0 : severely harmful (violence threats, explicit abuse, illegal content)\n\n" +
                        "Example outputs:\n" +
                        "  {\"score\": 0.05, \"reason\": \"Normal everyday post about food.\"}\n" +
                        "  {\"score\": 0.85, \"reason\": \"Contains explicit death threats and hate speech.\"}";

        String body = "{"
                + "\"model\":" + toJsonString(model) + ","
                + "\"max_tokens\":150,"
                + "\"temperature\":0.1,"
                + "\"messages\":["
                + "  {\"role\":\"system\",\"content\":" + toJsonString(systemPrompt) + "},"
                + "  {\"role\":\"user\",\"content\":" + toJsonString(content) + "}"
                + "]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .header("HTTP-Referer", "http://localhost")
                .header("X-Title", "Blog Moderation")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();

        if (status == 429) {
            System.err.println("[AI Moderation] 429 rate-limit on " + model + " — trying next…");
            return null;
        }
        if (status == 404) {
            System.err.println("[AI Moderation] 404 not found: " + model + " — trying next…");
            return null;
        }
        if (status == 503 || status == 502) {
            System.err.println("[AI Moderation] " + status + " unavailable: " + model + " — trying next…");
            return null;
        }
        if (status != 200) {
            System.err.println("[AI Moderation] HTTP " + status + " on " + model + ": " + response.body());
            return null;
        }

        return parseOpenRouterResponse(response.body(), model);
    }

    // =========================================================================
    // Response Parsing — handles both normal and reasoning models
    // =========================================================================

    private ModerationResult parseOpenRouterResponse(String responseBody, String model) {
        try {
            // ── Try standard content field first ─────────────────────────────
            String textContent = extractMessageContent(responseBody);

            // ── FIX: Reasoning models (like gpt-5-nano via openrouter/auto) ──
            // store output in reasoning_details[].data instead of content.
            // If content is empty, we can't use the reasoning output directly
            // (it's encrypted). Skip this model and let fallback handle it.
            if (textContent == null || textContent.isBlank()) {
                System.err.println("[AI Moderation] Empty content from " + model
                        + " (likely a reasoning model) — trying next…");
                return null;
            }

            // Strip markdown backticks if model wrapped the JSON
            textContent = textContent.trim()
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)^```\\s*",     "")
                    .replaceAll("(?s)\\s*```$",     "")
                    .trim();

            System.out.println("[AI Moderation] (" + model + ") output: " + textContent);

            String scoreStr = extractJsonField(textContent, "score");
            String reason   = extractJsonField(textContent, "reason");

            if (scoreStr == null) {
                System.err.println("[AI Moderation] No 'score' in response from " + model);
                return null;
            }

            double score = Double.parseDouble(scoreStr.trim());
            score = Math.max(0.0, Math.min(1.0, score));
            if (reason == null) reason = "No reason provided";

            boolean shouldHide = score >= AUTO_HIDE_THRESHOLD;
            System.out.println("[AI Moderation] score=" + score
                    + " shouldHide=" + shouldHide + " reason=" + reason);

            return new ModerationResult(score, reason, shouldHide);

        } catch (Exception e) {
            System.err.println("[AI Moderation] Parse error from " + model
                    + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts choices[0].message.content from OpenAI-style JSON.
     */
    private String extractMessageContent(String json) {
        // Find "message" then "content" after it
        int messageIdx = json.indexOf("\"message\"");
        if (messageIdx < 0) messageIdx = 0;

        String searchKey = "\"content\"";
        int keyIdx = json.indexOf(searchKey, messageIdx);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        int valueStart = colonIdx + 1;
        while (valueStart < json.length()
                && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) return null;

        // Handle null value
        if (json.startsWith("null", valueStart)) return null;

        if (json.charAt(valueStart) != '"') return null;

        StringBuilder sb = new StringBuilder();
        int i = valueStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"'  -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n'  -> sb.append('\n');
                    case 'r'  -> sb.append('\r');
                    case 't'  -> sb.append('\t');
                    default   -> sb.append(next);
                }
                i += 2;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String toJsonString(String s) {
        return "\"" + escapeJson(s) + "\"";
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractJsonField(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        int valueStart = colonIdx + 1;
        while (valueStart < json.length()
                && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) return null;

        char firstChar = json.charAt(valueStart);

        if (firstChar == '"') {
            int end = valueStart + 1;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }
            return json.substring(valueStart + 1, end)
                    .replace("\\\"", "\"")
                    .replace("\\n",  "\n")
                    .replace("\\\\", "\\");
        } else {
            int end = valueStart;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
                end++;
            }
            return json.substring(valueStart, end);
        }
    }
}