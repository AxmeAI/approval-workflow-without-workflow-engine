/*
 * Procurement service agent — Java example.
 *
 * Fetches an intent by ID, validates a purchase request, and resumes.
 * Workflow then pauses for manager approval.
 *
 * Usage:
 *   export AXME_API_KEY="<agent-key>"
 *   javac -cp axme-sdk.jar Agent.java
 *   java -cp .:axme-sdk.jar Agent <intent_id>
 */

import dev.axme.sdk.AxmeClient;
import dev.axme.sdk.AxmeClientConfig;
import dev.axme.sdk.RequestOptions;
import java.time.Instant;
import java.util.Map;

public class Agent {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java Agent <intent_id>");
            System.exit(1);
        }

        String apiKey = System.getenv("AXME_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: AXME_API_KEY not set.");
            System.exit(1);
        }

        String intentId = args[0];
        var client = new AxmeClient(AxmeClientConfig.forCloud(apiKey));

        System.out.println("Processing intent: " + intentId);

        var intentData = client.getIntent(intentId, new RequestOptions());
        @SuppressWarnings("unchecked")
        var intent = (Map<String, Object>) intentData.getOrDefault("intent", intentData);
        @SuppressWarnings("unchecked")
        var payload = (Map<String, Object>) intent.getOrDefault("payload", Map.of());
        if (payload.containsKey("parent_payload")) {
            @SuppressWarnings("unchecked")
            var pp = (Map<String, Object>) payload.get("parent_payload");
            payload = pp;
        }

        String requestId = (String) payload.getOrDefault("request_id", "unknown");
        double amount = payload.containsKey("amount") ? ((Number) payload.get("amount")).doubleValue() : 0;
        String dept = (String) payload.getOrDefault("department", "unknown");

        System.out.printf("  Processing purchase %s: $%.0f for %s...%n", requestId, amount, dept);
        Thread.sleep(1000);
        System.out.println("  Validating budget availability...");
        Thread.sleep(1000);

        var result = Map.<String, Object>of(
            "action", "complete",
            "request_id", requestId,
            "budget_available", true,
            "validated_at", Instant.now().toString()
        );

        client.resumeIntent(intentId, result, new RequestOptions());
        System.out.println("  Purchase " + requestId + " validated. Waiting for manager approval.");
        System.out.println("  To approve: axme tasks approve <intent_id>");
    }
}
