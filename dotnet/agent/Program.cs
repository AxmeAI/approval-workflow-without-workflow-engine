// Procurement service agent — .NET example.
//
// Fetches an intent by ID, validates a purchase request, and resumes.
// Workflow then pauses for manager approval.
//
// Usage:
//   export AXME_API_KEY="<agent-key>"
//   dotnet run -- <intent_id>

using Axme.Sdk;
using System.Text.Json.Nodes;

if (args.Length < 1)
{
    Console.Error.WriteLine("Usage: dotnet run -- <intent_id>");
    return 1;
}

var apiKey = Environment.GetEnvironmentVariable("AXME_API_KEY");
if (string.IsNullOrEmpty(apiKey))
{
    Console.Error.WriteLine("Error: AXME_API_KEY not set.");
    return 1;
}

var intentId = args[0];
var client = new AxmeClient(new AxmeClientConfig { ApiKey = apiKey });

Console.WriteLine($"Processing intent: {intentId}");

var intentData = await client.GetIntentAsync(intentId);
var intent = intentData["intent"]?.AsObject() ?? intentData;
var payload = intent["payload"]?.AsObject() ?? new JsonObject();
if (payload["parent_payload"] is JsonObject parentPayload)
{
    payload = parentPayload;
}

var requestId = payload["request_id"]?.ToString() ?? "unknown";
var amount = payload["amount"]?.GetValue<double>() ?? 0;
var dept = payload["department"]?.ToString() ?? "unknown";

Console.WriteLine($"  Processing purchase {requestId}: ${amount} for {dept}...");
await Task.Delay(1000);
Console.WriteLine("  Validating budget availability...");
await Task.Delay(1000);

var result = new JsonObject
{
    ["action"] = "complete",
    ["request_id"] = requestId,
    ["budget_available"] = true,
    ["validated_at"] = DateTime.UtcNow.ToString("o")
};

await client.ResumeIntentAsync(intentId, result);
Console.WriteLine($"  Purchase {requestId} validated. Waiting for manager approval.");
Console.WriteLine("  To approve: axme tasks approve <intent_id>");
return 0;
