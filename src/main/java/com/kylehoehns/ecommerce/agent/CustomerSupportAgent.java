package com.kylehoehns.ecommerce.agent;

import com.embabel.agent.api.annotation.*;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.kylehoehns.ecommerce.classification.Intent;
import com.kylehoehns.ecommerce.classification.Sentiment;
import com.kylehoehns.ecommerce.inventory.InventoryService;
import com.kylehoehns.ecommerce.order.Order;
import com.kylehoehns.ecommerce.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;


@Agent(
    name = "Customer Support Agent",
    description = "Assists customers with information about getting refunds or replacements for orders they've made"
)
public class CustomerSupportAgent {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportAgent.class);

    private final OrderService orderService;
    private final InventoryService inventoryService;

    public CustomerSupportAgent(OrderService orderService, InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    public enum OperationType {
        REFUND,
        REPLACE
    }

    public record ParsedRequest(String orderId, Intent intent, Sentiment sentiment) {
    }

    public record OrderSearchResult(Order order, boolean orderExists) {
    }

    public record OrderAdjustmentResponse(
        @JsonPropertyDescription("Information about the original order that was created") Order originalOrder,
        @JsonPropertyDescription("Information about the new order that is being sent out - if null, no new order was placed.") Order updatedOrder,
        @JsonPropertyDescription("What the system did - either refunded or send out a replacement order") OperationType operationType,
        @JsonPropertyDescription("The message that should be displayed to the end user about what happened with their order") String userMessage) {
    }

    public record CustomerResponse(String message) {
    }

    @AchievesGoal(
        description = "Handles the refund or replacement of an order for a customer",
        export = @Export(
            name = "refundOrReplaceOrder",
            remote = true,
            startingInputTypes = {UserInput.class}
        )
    )
    @Action
    OrderAdjustmentResponse refundOrReplaceOrder(OrderAdjustmentResponse orderAdjustmentResponse,
                                                 OrderSearchResult searchResult,
                                                 CustomerResponse customerResponse) {
        if (!searchResult.orderExists()) {
            return new OrderAdjustmentResponse(orderAdjustmentResponse.originalOrder(), orderAdjustmentResponse.updatedOrder(), null, "Order not found");
        }

        return new OrderAdjustmentResponse(orderAdjustmentResponse.originalOrder(), orderAdjustmentResponse.updatedOrder(), orderAdjustmentResponse.operationType(), customerResponse.message());
    }

    @Action
    CustomerResponse createCustomerResponse(OrderAdjustmentResponse adjustmentResponse, OperationContext context, ParsedRequest parsedRequest) {
        var customerMessage = context.promptRunner()
            .createObject("""
                You are a customer support representative informing a customer that their order has been %s.
                
                Order details:
                %s
                
                Customer's original sentiment: %s
                Customer requested: %s
                Action taken: %s
                
                If the customer requested a replacement but the system processed a refund, it was due to inventory limitations.
                
                Create a professional response that acknowledges their sentiment and shares in frustration if negative.
                Keep it under 100 words.
                
                Standard timelines:
                - Replacements ship within 1-3 business days
                - Refunds appear on card within 1 business day
                
                Company name: ACME
                """.formatted(
                adjustmentResponse.operationType().toString().toLowerCase(),
                adjustmentResponse.originalOrder(),
                parsedRequest.sentiment(),
                parsedRequest.intent(),
                adjustmentResponse.operationType().toString().toLowerCase()
            ), String.class);

        return new CustomerResponse(customerMessage);
    }


    @Action(pre = "shouldReplace")
    OrderAdjustmentResponse processReplacement(OrderSearchResult searchResult) {
        var originalOrder = searchResult.order();
        var replacementOrder = orderService.processReplacement(
            originalOrder.id(),
            originalOrder.sku(),
            originalOrder.sku(),
            originalOrder.quantity(),
            originalOrder.price()
        );
        return new OrderAdjustmentResponse(originalOrder, replacementOrder, OperationType.REPLACE, "Replacement order created");
    }

    @Action(pre = "shouldRefund")
    OrderAdjustmentResponse processRefund(OrderSearchResult searchResult) {
        var order = searchResult.order();
        var refundMessage = orderService.processRefund(order.id(), order.sku(), order.quantity(), order.price());
        return new OrderAdjustmentResponse(order, null, OperationType.REFUND, refundMessage);
    }

    @Condition(name = "shouldReplace")
    public boolean shouldReplace(ParsedRequest parsedRequest, OrderSearchResult searchResult) {
        var wantsReplacement = Intent.REPLACEMENT.equals(parsedRequest.intent());
        boolean hasInventory = inventoryService.getQuantity(searchResult.order().sku()) > 0;
        return hasInventory && wantsReplacement;
    }

    @Condition(name = "shouldRefund")
    public boolean shouldRefund(ParsedRequest parsedRequest, OrderSearchResult searchResult) {
        boolean hasInventory = inventoryService.getQuantity(searchResult.order().sku()) > 0;
        return Intent.REFUND.equals(parsedRequest.intent()) || !hasInventory;
    }

    @Action(post = {"shouldRefund", "shouldReplace"})
    OrderSearchResult retrieveOrder(ParsedRequest parsedRequest) {
        log.info("Retrieving order for ID: {}", parsedRequest.orderId());
        return orderService.getOrder(parsedRequest.orderId())
            .map(order -> new OrderSearchResult(order, true))
            .orElse(new OrderSearchResult(null, false));
    }

    @Action
    ParsedRequest parseRequest(UserInput userInput, OperationContext context) {
        log.info("Parsing customer request: {}", userInput.getContent());
        var analysisPrompt = """
            Analyze this customer support request to extract:
            1. Order ID they're referring to
            2. Whether they want a REFUND or REPLACEMENT
            3. Their emotional sentiment (POSITIVE, NEUTRAL, or NEGATIVE)
            
            Customer request: %s
            """.formatted(userInput.getContent());

        return context.promptRunner().createObject(analysisPrompt, ParsedRequest.class);
    }

}
