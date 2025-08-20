package com.kylehoehns.ecommerce.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Condition;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.kylehoehns.ecommerce.classification.Intent;
import com.kylehoehns.ecommerce.classification.Sentiment;
import com.kylehoehns.ecommerce.inventory.InventoryService;
import com.kylehoehns.ecommerce.order.Order;
import com.kylehoehns.ecommerce.order.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;


@Agent(description = "Assists customers with information about getting refunds or replacements for orders they've made")
public class CustomerSupportAgent {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportAgent.class);

    private final OrderService orderService;
    private final InventoryService inventoryService;

    public CustomerSupportAgent(OrderService orderService, InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    record OrderAdjustmentResponse(Order order, OperationType operationType, String userMessage) {
    }

    record OrderAdjustmentResponse2(Order order, OperationType operationType, String userMessage) {
    }

    enum OperationType {
        REFUND,
        REPLACE,
        UNKNOWN
    }

    @AchievesGoal(description = "Handles the refund or replacement of an order for a customer")
    @Action
    OrderAdjustmentResponse2 completeAdjustment(OrderAdjustmentResponse orderAdjustmentResponse,
                                                OrderSearchResult searchResult,
                                                CustomerResponse customerResponse) {
        if (!searchResult.orderExists()) {
            // TODO: Maybe this just throws?
            return new OrderAdjustmentResponse2(orderAdjustmentResponse.order(), OperationType.UNKNOWN, "An order with that ID does not exist");
        }

        return new OrderAdjustmentResponse2(orderAdjustmentResponse.order(), orderAdjustmentResponse.operationType(), customerResponse.message());
    }

    record CustomerResponse(String message) { }

    @Action
    CustomerResponse createCustomerResponse(OrderAdjustmentResponse oar, OperationContext context, ParsedRequest parsedRequest) {
        var message = context.promptRunner()
            .createObject("""
                You are a customer support representative letting a customer know that their order has been %s
                
                Here is information about their order:
                %s
                
                When they originally submitted the request, they had the following sentiment: %s
                
                They wanted the order to be: %s
                The order was actually: %s
                
                If the user wanted a replacement, but we issued a refund. It was due to inventory issues.
                
                Please create a message for them in the proper tone, taking into effect their original tone and letting them know about their order in 100 words or less.
                
                Orders usually ship within 1-3 business days.
                If it is a refund, funds will be back on their card within 1 business day.
                
                Our company name is ACME, co if you refer to the company.
                """.formatted(oar.operationType(), oar.order(), parsedRequest.sentiment(), parsedRequest.intent(), oar.operationType()), String.class);

        return new CustomerResponse(message);
    }


    @Action(pre = "shouldReplace")
    OrderAdjustmentResponse processReplacement(OrderSearchResult searchResult) {
        var order = searchResult.order();
        var replacement = orderService.processReplacement(order.id(), order.sku(), order.sku(), 1, BigDecimal.ONE);
        return new OrderAdjustmentResponse(replacement, OperationType.REPLACE, "We replaced it dude");
    }

    @Action(pre = "shouldRefund")
    OrderAdjustmentResponse processRefund(OrderSearchResult searchResult) {
        var order = searchResult.order();
        var refundMsg = orderService.processRefund(order.id(), order.sku(), 1, BigDecimal.ONE);
        return new OrderAdjustmentResponse(order, OperationType.REFUND, refundMsg);
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

    record OrderSearchResult(Order order, boolean orderExists) {
    }

    @Action(post = {"shouldRefund", "shouldReplace"})
    OrderSearchResult retrieveOrder(ParsedRequest parsedRequest) {
        log.info("Retrieving order {}", parsedRequest);
        return orderService.getOrder(parsedRequest.orderId())
            .map(o -> new OrderSearchResult(o, true))
            .orElseGet(() -> new OrderSearchResult(null, false)); // TODO: Heck of a lot easier if this throws
    }

    record ParsedRequest(String orderId, Intent intent, Sentiment sentiment) {
    }

    @Action
    ParsedRequest parseRequest(UserInput userInput, OperationContext context) {
        log.info("Parsing request {}", userInput);
        var prompt = String.format("""
               Take this user's request for an order replacement or refund and determine what they'd like to do, as well as their sentiment.
            
               %s
            """, userInput.getContent());

        return context.promptRunner().createObject(prompt, ParsedRequest.class);
    }

}
