package com.financemanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class FinanceAssistantController {
    private static final String INSTRUCTIONS = "You are Clarity, a friendly helper built into the Clarity personal finance manager. "
            + "Answer the user's question directly and conversationally. You can explain how to use this app, budgeting, everyday spending, saving habits, household or student finances, and general financial concepts. "
            + "Never claim to see or change a user's transactions, bank accounts, or balances: this chat does not send their private finance records. "
            + "For personal investment, tax, debt, or legal questions, give general educational information, mention key assumptions, and suggest a qualified professional for decisions with significant consequences. "
            + "Keep answers clear, useful, and concise. Do not invent application features.";

    private final String apiKey;
    private final String model;
    private final RestClient client;

    public FinanceAssistantController(@Value("${finance.assistant.openai.api-key:}") String apiKey,
                                      @Value("${finance.assistant.openai.model:gpt-6-astra}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(5000);
        timeouts.setReadTimeout(45000);
        this.client = RestClient.builder().baseUrl("https://api.openai.com/v1").requestFactory(timeouts).build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> answer(@RequestBody AssistantRequest request,
                                      @AuthenticationPrincipal UserDetails principal) {
        String question = request.message() == null ? "" : request.message().trim();
        if (question.isEmpty()) return response("Type a question and I’ll help you find an answer.", "/dashboard", "Open overview", false);
        if (question.length() > 2000) question = question.substring(0, 2000);
        String[] destination = destination(question);

        if (apiKey.isBlank()) {
            return response("I can answer questions about money and this app once an OpenAI API key is added to the IntelliJ run configuration as OPENAI_API_KEY. For now, I can still point you to the right section.", destination[0], destination[1], false);
        }
        try {
            List<Map<String, String>> input = new ArrayList<>();
            if (request.history() != null) for (ChatTurn turn : request.history().stream().limit(10).toList()) {
                if (turn != null && ("user".equals(turn.role()) || "assistant".equals(turn.role()))
                        && turn.content() != null && turn.content().length() <= 2000) {
                    input.add(Map.of("role", turn.role(), "content", turn.content()));
                }
            }
            input.add(Map.of("role", "user", "content", question));
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "instructions", INSTRUCTIONS,
                    "input", input,
                    "max_output_tokens", 650,
                    "store", false);
            JsonNode result = client.post().uri("/responses")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve().body(JsonNode.class);
            String answer = outputText(result);
            if (answer.isBlank()) answer = "I couldn't form a clear answer just now. Please try asking in a different way.";
            return response(answer, destination[0], destination[1], true);
        } catch (RestClientException ex) {
            return response("I couldn’t reach the AI service just now. Check the OPENAI_API_KEY and internet connection in your IntelliJ run configuration, then try again.", destination[0], destination[1], false);
        }
    }

    public record AssistantRequest(String message, List<ChatTurn> history) {}
    public record ChatTurn(String role, String content) {}

    private String outputText(JsonNode response) {
        if (response == null) return "";
        JsonNode output = response.path("output");
        StringBuilder text = new StringBuilder();
        if (output.isArray()) for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (content.isArray()) for (JsonNode part : content) {
                if ("output_text".equals(part.path("type").asText())) {
                    if (!text.isEmpty()) text.append('\n');
                    text.append(part.path("text").asText(""));
                }
            }
        }
        return text.toString().trim();
    }

    private String[] destination(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        if (has(q, "budget", "limit", "alert")) return new String[]{"/budgets", "Open budgets"};
        if (has(q, "income", "salary", "deposit")) return new String[]{"/addIncome", "Add income"};
        if (has(q, "expense", "spend", "purchase", "payment")) return new String[]{"/addExpense", "Add an expense"};
        if (has(q, "goal", "save", "saving")) return new String[]{"/dashboard", "View savings goals"};
        if (has(q, "pdf", "report", "download", "statement")) return new String[]{"/reports/download?format=pdf", "Download report"};
        if (has(q, "upi", "phonepe", "paytm", "google pay", "sync", "connect")) return new String[]{"/integrations/upi", "Open payment sync"};
        if (has(q, "profile", "bank", "account", "card")) return new String[]{"/profile", "Open your profile"};
        if (has(q, "transaction", "history")) return new String[]{"/transactions", "View transactions"};
        return new String[]{"/dashboard", "Back to overview"};
    }

    private boolean has(String value, String... words) {
        for (String word : words) if (value.contains(word)) return true;
        return false;
    }

    private Map<String, String> response(String answer, String url, String label, boolean ai) {
        return Map.of("answer", answer, "url", url, "label", label, "ai", Boolean.toString(ai));
    }
}
