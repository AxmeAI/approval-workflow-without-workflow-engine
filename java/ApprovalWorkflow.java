/*
 * Approval workflow without a workflow engine — Java example.
 *
 * Submit a purchase request with a multi-step approval chain:
 * manager approval → finance approval → processing.
 * No Temporal, no Airflow, no Step Functions.
 *
 * Usage:
 *   export AXME_API_KEY="your-key"
 *   mvn compile exec:java -Dexec.mainClass="ApprovalWorkflow"
 */

import ai.axme.sdk.AxmeClient;
import ai.axme.sdk.AxmeClientConfig;
import java.util.Map;

public class ApprovalWorkflow {
    public static void main(String[] args) throws Exception {
        var client = new AxmeClient(
            AxmeClientConfig.builder()
                .apiKey(System.getenv("AXME_API_KEY"))
                .build()
        );

        // Submit purchase request with approval chain
        String intentId = client.sendIntent(Map.of(
            "intent_type", "purchase.request.v1",
            "to_agent", "agent://myorg/production/procurement-service",
            "payload", Map.of(
                "item", "MacBook Pro M4",
                "amount_usd", 3499,
                "requester", "alice@company.com",
                "cost_center", "engineering"
            )
        ));
        System.out.println("Purchase request submitted: " + intentId);

        // Wait for full approval chain to complete
        var result = client.waitFor(intentId);
        System.out.println("Final status: " + result.getStatus());
    }
}
