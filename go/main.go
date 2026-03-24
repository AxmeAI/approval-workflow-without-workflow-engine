// Approval workflow without a workflow engine — Go example.
//
// Submit a purchase request with a multi-step approval chain:
// manager approval → finance approval → processing.
// No Temporal, no Airflow, no Step Functions.
//
// Usage:
//
//	export AXME_API_KEY="your-key"
//	go run main.go
package main

import (
	"context"
	"fmt"
	"log"
	"os"

	"github.com/AxmeAI/axme-sdk-go/axme"
)

func main() {
	client := axme.NewClient(axme.Config{
		APIKey: os.Getenv("AXME_API_KEY"),
	})

	ctx := context.Background()

	// Submit purchase request with approval chain
	intentID, err := client.SendIntent(ctx, axme.SendIntentRequest{
		IntentType: "purchase.request.v1",
		ToAgent:    "agent://myorg/production/procurement-service",
		Payload: map[string]interface{}{
			"item":        "MacBook Pro M4",
			"amount_usd":  3499,
			"requester":   "alice@company.com",
			"cost_center": "engineering",
		},
	})
	if err != nil {
		log.Fatalf("send intent: %v", err)
	}
	fmt.Printf("Purchase request submitted: %s\n", intentID)

	// Wait for full approval chain to complete
	result, err := client.WaitFor(ctx, intentID)
	if err != nil {
		log.Fatalf("wait: %v", err)
	}
	fmt.Printf("Final status: %s\n", result.Status)
}
