# Embabel Framework Demo - E-commerce Customer Support Agent

A practical example demonstrating how to use [Rod Johnson's embabel framework](https://github.com/embabel/embabel-agent) to add AI-powered customer support agents to existing Java Spring Boot applications.

## What is Embabel?

Embabel is an innovative framework that allows you to create agentic AI systems using Java annotations. Instead of writing complex orchestration code, you define agent behavior declaratively using annotations like `@Agent`, `@Action`, and `@Condition`.

This repository shows how to integrate embabel into a traditional Spring Boot e-commerce application to handle customer refunds and replacements automatically.

## Prerequisites

- **Java 21** or higher
- **LLM API Key** - Either OpenAI or Anthropic API key required for LLM operations

## Quick Start

### 1. Set Your LLM API Key

The embabel framework requires either an OpenAI or Anthropic API key to function. Choose one:

**For OpenAI:**
```bash
export OPENAI_API_KEY=your-openai-api-key-here
```

**For Anthropic:**
```bash
export ANTHROPIC_API_KEY=your-anthropic-api-key-here
```

Or on Windows:
```cmd
set OPENAI_API_KEY=your-openai-api-key-here
# OR
set ANTHROPIC_API_KEY=your-anthropic-api-key-here
```

### 2. Run the Application

```bash
git clone <this-repo>
cd ecommerce-demo
./mvnw spring-boot:run
```

The application will start on port 8080.

### 3. Test the Customer Support Agent

You can interact with the agent in two ways:

**Option A: Using the Interactive Shell**

To use the shell version, you need to modify the Application.java file:
1. Comment out `@EnableAgentMcpServer` on line 10
2. Uncomment `@EnableAgentShell` on line 11

Then run:
```bash
./scripts/shell.sh
```
This starts an interactive shell where you can chat directly with the customer support agent. Once the shell is open, prefix your messages with `x` followed by your request:
```
x "Hello! I'd like to return order ORD-123."
```

**Option B: MCP Server (for Claude Desktop integration)**
The agent is also available at `http://localhost:8080/sse` and can handle customer requests like:

- "I need a refund for order ORD-123"
- "Can I get a replacement for order ORD-456?"
- "I'm angry about my order ORD-789, I want my money back!"

## Key Embabel Concepts Demonstrated

### Agent Declaration
```java
@Agent(
    name = "Customer Support Agent",
    description = "Assists customers with refunds and replacements"
)
public class CustomerSupportAgent {
    // Agent implementation
}
```

### Actions and Flow Control
```java
@Action
ParsedRequest parseRequest(UserInput userInput, OperationContext context) {
    // Uses LLM to extract order ID, operation type, and sentiment
}

@Action(pre = "shouldReplace")
OrderAdjustmentResponse processReplacement(OrderSearchResult searchResult) {
    // Only executes if shouldReplace condition is true
}
```

### Conditional Logic
```java
@Condition(name = "shouldReplace")
public boolean shouldReplace(ParsedRequest request, OrderSearchResult result) {
    boolean wantsReplacement = OperationType.REPLACE.equals(request.operationType());
    boolean hasInventory = inventoryService.getQuantity(result.order().sku()) > 0;
    return hasInventory && wantsReplacement;
}
```

### MCP Server Integration
```java
@AchievesGoal(
    export = @Export(
        name = "refundOrReplaceOrder",
        remote = true,
        startingInputTypes = {UserInput.class}
    )
)
```

## Testing with Claude Desktop

To interact with the agent through Claude Desktop, add this to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "customer-support": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8080/sse"]
    }
  }
}
```

Then restart Claude Desktop and you can chat with the customer support agent directly.

## Project Structure

```
src/main/java/com/kylehoehns/ecommerce/
├── agent/
│   ├── CustomerSupportAgent.java    # Main embabel agent
│   ├── OperationType.java          # REFUND/REPLACE enum
│   └── Sentiment.java              # POSITIVE/NEUTRAL/NEGATIVE enum
├── order/                          # Order management services
├── inventory/                      # Inventory management
└── Application.java                # Spring Boot app with @EnableAgentMcpServer
```

## How the Agent Works

1. **Input Processing**: Customer requests are parsed using LLM to extract:
   - Order ID
   - Desired operation (refund/replacement)
   - Customer sentiment

2. **Business Logic**: The agent checks:
   - If the order exists
   - Current inventory levels
   - Customer preferences

3. **Decision-Making**: Based on conditions:
   - Replacements are offered if inventory is available
   - Refunds are processed if no inventory or customer prefers refund

4. **Response Generation**: LLM generates personalized responses considering customer sentiment

## Learning More About Embabel

- **GitHub Repository**: [embabel-agent](https://github.com/embabel/embabel-agent)
- **Rod Johnson's Blog**: [Medium @springrod](https://medium.com/@springrod) - Contains in-depth examples and tutorials
- **Framework Documentation**: Available in the embabel-agent repository

## API Examples

While the agent handles most operations, you can also interact with the underlying services:

```bash
# Check inventory
curl http://localhost:8080/api/inventory

# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"sku": "shirt-m"}'

# List orders
curl http://localhost:8080/api/orders
```

## Dependencies

The key embabel dependency is added to `pom.xml`:

```xml
<dependency>
    <groupId>com.embabel.agent</groupId>
    <artifactId>embabel-agent-starter</artifactId>
    <version>0.1.1-SNAPSHOT</version>
</dependency>
```

## Next Steps

1. Explore the `CustomerSupportAgent.java` to understand embabel patterns
2. Try modifying conditions to change agent behavior
3. Add new actions for additional customer support scenarios
4. Read Rod Johnson's blog posts for advanced embabel techniques
5. Check out the embabel-agent repository for more examples
