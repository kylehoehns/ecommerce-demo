package com.kylehoehns.ecommerce.notification;

import com.kylehoehns.ecommerce.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class CustomerNotificationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerNotificationService.class);

    public void informCustomer(Order order, String message) {
        log.info("Informing customer {}", message);
    }
}
