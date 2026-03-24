"""Procurement service agent - validates purchase and resumes."""

import os, sys, time
sys.stdout.reconfigure(line_buffering=True)
from axme import AxmeClient, AxmeClientConfig

AGENT_ADDRESS = "procurement-service-demo"

def handle_intent(client, intent_id):
    intent_data = client.get_intent(intent_id)
    intent = intent_data.get("intent", intent_data)
    payload = intent.get("payload", {})
    if "parent_payload" in payload:
        payload = payload["parent_payload"]

    request_id = payload.get("request_id", "unknown")
    amount = payload.get("amount", 0)
    dept = payload.get("department", "unknown")

    print(f"  Processing purchase {request_id}: ${amount} for {dept}...")
    time.sleep(1)
    print(f"  Validating budget availability...")
    time.sleep(1)

    result = {
        "action": "complete",
        "request_id": request_id,
        "budget_available": True,
        "validated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    }
    client.resume_intent(intent_id, result)
    print(f"  Purchase {request_id} validated. Waiting for manager approval.")
    print(f"  To approve: axme tasks approve <intent_id>")

def main():
    api_key = os.environ.get("AXME_API_KEY", "")
    if not api_key:
        print("Error: AXME_API_KEY not set."); sys.exit(1)
    client = AxmeClient(AxmeClientConfig(api_key=api_key))
    print(f"Agent listening on {AGENT_ADDRESS}...")
    print("Waiting for intents (Ctrl+C to stop)\n")
    for delivery in client.listen(AGENT_ADDRESS):
        intent_id = delivery.get("intent_id", "")
        status = delivery.get("status", "")
        if intent_id and status in ("DELIVERED", "CREATED", "IN_PROGRESS"):
            print(f"[{status}] Intent received: {intent_id}")
            try:
                handle_intent(client, intent_id)
            except Exception as e:
                print(f"  Error: {e}")

if __name__ == "__main__":
    main()
