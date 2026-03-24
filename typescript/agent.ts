/**
 * Procurement service agent — TypeScript example.
 *
 * Validates purchase requests and resumes. Workflow then pauses
 * for manager approval.
 *
 * Usage:
 *   export AXME_API_KEY="<agent-key>"
 *   npx tsx agent.ts
 */

import { AxmeClient } from "@axme/axme";

const AGENT_ADDRESS = "procurement-service-demo";

async function handleIntent(client: AxmeClient, intentId: string) {
  const intentData = await client.getIntent(intentId);
  const intent = intentData.intent ?? intentData;
  let payload = intent.payload ?? {};
  if (payload.parent_payload) {
    payload = payload.parent_payload;
  }

  const requestId = payload.request_id ?? "unknown";
  const amount = payload.amount ?? 0;
  const dept = payload.department ?? "unknown";

  console.log(`  Processing purchase ${requestId}: $${amount} for ${dept}...`);
  await new Promise((r) => setTimeout(r, 1000));
  console.log(`  Validating budget availability...`);
  await new Promise((r) => setTimeout(r, 1000));

  const result = {
    action: "complete",
    request_id: requestId,
    budget_available: true,
    validated_at: new Date().toISOString(),
  };

  await client.resumeIntent(intentId, result, { ownerAgent: "procurement-service-demo" });
  console.log(`  Purchase ${requestId} validated. Waiting for manager approval.`);
  console.log(`  To approve: axme tasks approve <intent_id>`);
}

async function main() {
  const apiKey = process.env.AXME_API_KEY;
  if (!apiKey) {
    console.error("Error: AXME_API_KEY not set.");
    process.exit(1);
  }

  const client = new AxmeClient({ apiKey });

  console.log(`Agent listening on ${AGENT_ADDRESS}...`);
  console.log("Waiting for intents (Ctrl+C to stop)\n");

  for await (const delivery of client.listen(AGENT_ADDRESS)) {
    const intentId = delivery.intent_id;
    const status = delivery.status;
    if (intentId && ["DELIVERED", "CREATED", "IN_PROGRESS"].includes(status)) {
      console.log(`[${status}] Intent received: ${intentId}`);
      try {
        await handleIntent(client, intentId);
      } catch (e) {
        console.error(`  Error: ${e}`);
      }
    }
  }
}

main().catch(console.error);
