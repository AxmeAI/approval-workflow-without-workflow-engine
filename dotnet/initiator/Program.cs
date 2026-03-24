// Approval workflow without a workflow engine — .NET example.
//
// Submit a purchase request with a multi-step approval chain:
// manager approval → finance approval → processing.
// No Temporal, no Airflow, no Step Functions.
//
// Usage:
//   export AXME_API_KEY="your-key"
//   dotnet run

using Axme.Sdk;
using System.Text.Json.Nodes;

var client = new AxmeClient(new AxmeClientConfig
{
    ApiKey = Environment.GetEnvironmentVariable("AXME_API_KEY")!
});

// Submit purchase request with approval chain
var intentId = await client.SendIntentAsync(new JsonObject
{
    ["intent_type"] = "purchase.request.v1",
    ["to_agent"] = "agent://myorg/production/procurement-service",
    ["payload"] = new JsonObject
    {
        ["item"] = "MacBook Pro M4",
        ["amount_usd"] = 3499,
        ["requester"] = "alice@company.com",
        ["cost_center"] = "engineering"
    }
});
Console.WriteLine($"Purchase request submitted: {intentId}");

// Wait for full approval chain to complete
var result = await client.WaitForAsync(intentId);
Console.WriteLine($"Final status: {result["status"]}");
