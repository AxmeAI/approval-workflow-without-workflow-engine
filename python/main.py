"""
Approval workflow without a workflow engine — Python example.

Submit a purchase request with a multi-step approval chain:
manager approval → finance approval → processing.
No Temporal, no Airflow, no Step Functions.

Usage:
    pip install axme
    export AXME_API_KEY="your-key"
    python main.py
"""

import os
from axme import AxmeClient, AxmeClientConfig


def main():
    client = AxmeClient(
        AxmeClientConfig(api_key=os.environ["AXME_API_KEY"])
    )

    # Submit purchase request with approval chain
    intent_id = client.send_intent(
        {
            "intent_type": "purchase.request.v1",
            "to_agent": "agent://myorg/production/procurement-service",
            "payload": {
                "item": "MacBook Pro M4",
                "amount_usd": 3499,
                "requester": "alice@company.com",
                "cost_center": "engineering",
            },
        }
    )
    print(f"Purchase request submitted: {intent_id}")

    # Observe the full approval chain in real time
    print("Watching approval chain...")
    for event in client.observe(intent_id):
        status = event.get("status", "")
        event_type = event.get("event_type", "")
        print(f"  [{status}] {event_type}")
        if status in ("COMPLETED", "FAILED", "TIMED_OUT", "CANCELLED"):
            break

    # Fetch final state
    intent = client.get_intent(intent_id)
    print(f"\nFinal status: {intent['intent']['lifecycle_status']}")


if __name__ == "__main__":
    main()
