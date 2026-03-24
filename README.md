# Approval Workflow Without a Workflow Engine

You need a simple approval chain: purchase request goes to a manager, then to finance, then processing. Temporal needs a platform team. Airflow is batch-only. Step Functions is AWS-locked. You just want approvals.

**There is a better way.** Model each approval step as an intent with a human gate. No workflow engine, no state machine, no infrastructure to manage.

> **Alpha** · Built with [AXME](https://github.com/AxmeAI/axme) (AXP Intent Protocol).
> [cloud.axme.ai](https://cloud.axme.ai) · [hello@axme.ai](mailto:hello@axme.ai)

---

## The Problem

A two-step approval chain should be simple. Instead you build:

```
1. User submits purchase request → insert into DB
2. Email manager with approval link → track email delivery
3. Manager clicks approve → update DB → email finance
4. Finance clicks approve → update DB → trigger processing
5. Cron job checks for stale requests (3 days? 7 days?)
6. Edge cases: manager on vacation, finance rejects, re-approval needed
```

What you end up maintaining:
- **Database state machine** — `pending_manager`, `approved_manager`, `pending_finance`, `approved`, `rejected`, `expired`
- **Email delivery** — templates, SMTP config, bounce handling, tracking
- **Timeout logic** — cron job to escalate or expire stale approvals
- **Audit trail** — who approved what, when, with what comments
- **Error recovery** — what happens when the DB update succeeds but the email fails?

---

## The Solution: Intent with Human Approval Gates

```
Client → send_intent("purchase request")
         ↓
   Manager gate → approve/reject
         ↓
   Finance gate → approve/reject
         ↓
   Processing → COMPLETED
```

Each approval step is a durable intent. The platform waits for human input, handles timeouts, and tracks the full audit trail.

---

## Quick Start

### Python

```bash
pip install axme
export AXME_API_KEY="your-key"   # Get one: axme login
```

```python
from axme import AxmeClient, AxmeClientConfig
import os

client = AxmeClient(AxmeClientConfig(api_key=os.environ["AXME_API_KEY"]))

# Submit purchase request with approval chain
intent_id = client.send_intent({
    "intent_type": "purchase.request.v1",
    "to_agent": "agent://myorg/production/procurement-service",
    "payload": {
        "item": "MacBook Pro M4",
        "amount_usd": 3499,
        "requester": "alice@company.com",
        "cost_center": "engineering",
    },
})

print(f"Purchase request submitted: {intent_id}")

# Wait for full approval chain to complete
result = client.wait_for(intent_id)
print(f"Final status: {result['status']}")
# COMPLETED = approved + processed
# FAILED = rejected at any step
```

### TypeScript

```bash
npm install @axme/axme
```

```typescript
import { AxmeClient } from "@axme/axme";

const client = new AxmeClient({ apiKey: process.env.AXME_API_KEY! });

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

const result = await client.waitFor(intentId);
console.log(`Final status: ${result.status}`);
```

---

## More Languages

Full implementations in all 5 languages:

| Language | Directory | Install |
|----------|-----------|---------|
| [Python](python/) | `python/` | `pip install axme` |
| [TypeScript](typescript/) | `typescript/` | `npm install @axme/axme` |
| [Go](go/) | `go/` | `go get github.com/AxmeAI/axme-sdk-go` |
| [Java](java/) | `java/` | Maven Central: `ai.axme:axme-sdk` |
| [.NET](dotnet/) | `dotnet/` | `dotnet add package Axme.Sdk` |

---

## Before / After

### Before: Manual Approval Chain (300+ lines)

```python
@app.post("/purchase/request")
async def create_request(req):
    request_id = str(uuid4())
    db.insert("purchase_requests", {
        "id": request_id, "status": "pending_manager",
        "item": req.item, "amount": req.amount,
        "requester": req.requester, "created_at": datetime.now(),
    })
    send_email(get_manager(req.requester),
        subject=f"Approval needed: {req.item} (${req.amount})",
        body=f"Click to approve: {BASE_URL}/approve/{request_id}")
    return {"request_id": request_id, "status": "pending_manager"}

@app.post("/approve/{request_id}")
async def approve(request_id, decision):
    row = db.get("purchase_requests", request_id)
    if row["status"] == "pending_manager":
        if decision == "approve":
            db.update(request_id, {"status": "pending_finance"})
            send_email(get_finance_approver(), ...)  # another email
        else:
            db.update(request_id, {"status": "rejected"})
    elif row["status"] == "pending_finance":
        if decision == "approve":
            db.update(request_id, {"status": "approved"})
            queue.enqueue(process_purchase, request_id)
        else:
            db.update(request_id, {"status": "rejected"})

# Plus: cron for expired approvals, audit log table, email bounce handling...
```

### After: AXME Approval Workflow (20 lines)

```python
from axme import AxmeClient, AxmeClientConfig

client = AxmeClient(AxmeClientConfig(api_key=os.environ["AXME_API_KEY"]))

intent_id = client.send_intent({
    "intent_type": "purchase.request.v1",
    "to_agent": "agent://myorg/production/procurement-service",
    "payload": {
        "item": "MacBook Pro M4",
        "amount_usd": 3499,
        "requester": "alice@company.com",
        "cost_center": "engineering",
    },
})

# Observe the full approval chain
for event in client.observe(intent_id):
    print(f"[{event['status']}] {event['event_type']}")
    if event["status"] in ("COMPLETED", "FAILED"):
        break
```

No state machine. No email templates. No cron job. No audit log table. The platform tracks it all.

---

## How It Works

```
┌──────────┐   send_intent()      ┌──────────────┐                 ┌─────────────┐
│ Requester │ ──────────────────►  │              │    deliver      │ Procurement │
│           │                      │  AXME Cloud  │ ──────────────► │   Service   │
│           │ ◄── observe(SSE) ──  │  (platform)  │                 │   (agent)   │
│           │                      │              │                 │             │
└──────────┘                      └──────┬───────┘                 └──────┬──────┘
                                          │                                │
                                   ┌──────▼───────┐                       │
                                   │  Manager      │◄──── approval gate ──┘
                                   │  approves     │
                                   └──────┬───────┘
                                          │
                                   ┌──────▼───────┐
                                   │  Finance      │◄──── approval gate
                                   │  approves     │
                                   └──────┬───────┘
                                          │
                                     COMPLETED
```

1. Requester submits a purchase **intent** via AXME SDK
2. Platform **delivers** it to the procurement service agent
3. Service creates **manager approval gate** — intent pauses, waits for human input
4. Manager approves — service creates **finance approval gate**
5. Finance approves — service **resumes** with completion
6. Requester **observes** every step via SSE — full visibility, full audit trail

---

## Run the Full Example

```bash
# Install CLI (one-time)
curl -fsSL https://raw.githubusercontent.com/AxmeAI/axme-cli/main/install.sh | sh
source ~/.zshrc

# Log in
axme login

# Run the built-in example
axme examples run workflow/approval
```

---

## Related

- [AXME](https://github.com/AxmeAI/axme) — project overview
- [AXP Spec](https://github.com/AxmeAI/axme-spec) — open Intent Protocol specification
- [AXME Examples](https://github.com/AxmeAI/axme-examples) — 20+ runnable examples across 5 languages
- [AXME CLI](https://github.com/AxmeAI/axme-cli) — manage intents, agents, scenarios from the terminal

---

Built with [AXME](https://github.com/AxmeAI/axme) (AXP Intent Protocol).
