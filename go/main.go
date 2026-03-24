// Approval workflow without a workflow engine — Go example.
//
// Submit a purchase request with a multi-step approval chain:
// manager approval -> finance approval -> processing.
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
	client, err := axme.NewClient(axme.ClientConfig{
		APIKey: os.Getenv("AXME_API_KEY"),
	})
	if err != nil {
		log.Fatalf("create client: %v", err)
	}

	ctx := context.Background()

	// Submit purchase request with approval chain
	intentID, err := client.SendIntent(ctx, map[string]any{
		"intent_type": "purchase.request.v1",
		"to_agent":    "agent://myorg/production/procurement-service",
		"item":        "MacBook Pro M4",
		"amount_usd":  3499,
		"requester":   "alice@company.com",
		"cost_center": "engineering",
	}, axme.RequestOptions{})
	if err != nil {
		log.Fatalf("send intent: %v", err)
	}
	fmt.Printf("Purchase request submitted: %s\n", intentID)

	// Wait for full approval chain to complete
	result, err := client.WaitFor(ctx, intentID, axme.ObserveOptions{})
	if err != nil {
		log.Fatalf("wait: %v", err)
	}
	fmt.Printf("Final status: %v\n", result["status"])
}
