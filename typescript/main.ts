/**
 * Approval workflow without a workflow engine — TypeScript example.
 *
 * Submit a purchase request with a multi-step approval chain:
 * manager approval → finance approval → processing.
 * No Temporal, no Airflow, no Step Functions.
 *
 * Usage:
 *   npm install @axme/axme
 *   export AXME_API_KEY="your-key"
 *   npx tsx main.ts
 */

import { AxmeClient } from "@axme/axme";

async function main() {
  const client = new AxmeClient({ apiKey: process.env.AXME_API_KEY! });

  // Submit purchase request with approval chain
  const intentId = await client.sendIntent({
    intentType: "purchase.request.v1",
    toAgent: "agent://myorg/production/procurement-service",
    payload: {
      item: "MacBook Pro M4",
      amountUsd: 3499,
      requester: "alice@company.com",
      costCenter: "engineering",
    },
  });
  console.log(`Purchase request submitted: ${intentId}`);

  // Wait for full approval chain to complete
  const result = await client.waitFor(intentId);
  console.log(`Final status: ${result.status}`);
}

main().catch(console.error);
