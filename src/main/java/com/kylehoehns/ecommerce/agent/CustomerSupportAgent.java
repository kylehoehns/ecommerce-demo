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

    record OrderAdjustmentResponse(String orderId, OperationType operationType, String userMessage) {
    }
    record OrderAdjustmentResponse2(String orderId, OperationType operationType, String userMessage) {
    }

    enum OperationType {
        REFUND,
        REPLACE,
        UNKNOWN
    }

    @AchievesGoal(description = "Handles the refund or replacement of an order for a customer")
    @Action
    OrderAdjustmentResponse2 completeAdjustment(OrderAdjustmentResponse orderAdjustmentResponse, OrderSearchResult searchResult) {
        if (!searchResult.orderExists()) {
            // TODO: Maybe this just throws?
            return new OrderAdjustmentResponse2(orderAdjustmentResponse.orderId(), OperationType.UNKNOWN, "An order with that ID does not exist");
        }

        return new OrderAdjustmentResponse2(orderAdjustmentResponse.orderId(), orderAdjustmentResponse.operationType(), orderAdjustmentResponse.userMessage());
    }

    @Action( pre = "hasInventory")
    OrderAdjustmentResponse processReplacement(OrderSearchResult searchResult) {
        var order = searchResult.order();
        var replacement = orderService.processReplacement(order.id(), order.sku(), order.sku(), 1, BigDecimal.ONE);
        return new OrderAdjustmentResponse(replacement.id(), OperationType.REPLACE, "We replaced it dude");
    }

    @Action(pre = "doesNotHaveInventory")
    OrderAdjustmentResponse processRefund(OrderSearchResult searchResult) {
        var order = searchResult.order();
        var refundMsg = orderService.processRefund(order.id(), order.sku(), 1, BigDecimal.ONE);
        return new OrderAdjustmentResponse(order.id(), OperationType.REFUND, refundMsg);
    }

    @Condition(name = "hasInventory")
    public boolean hasInventory(OrderSearchResult orderSearchResult) {
        return inventoryService.getQuantity(orderSearchResult.order().sku()) > 0;
    }

    @Condition(name = "doesNotHaveInventory")
    public boolean doesNotHaveInventory(OrderSearchResult orderSearchResult) {
        return inventoryService.getQuantity(orderSearchResult.order().sku()) <= 0;
    }

    record OrderSearchResult(Order order, boolean orderExists) {
    }

    @Action( post = { "hasInventory", "doesNotHaveInventory"} )
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
