package io.flowing.retail.checkout.rest;

import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.flowing.retail.checkout.domain.Customer;
import io.flowing.retail.checkout.domain.Order;
import io.flowing.retail.checkout.messages.Message;
import io.flowing.retail.checkout.messages.MessageSender;

@RestController
public class ShopRestController {

  private static final Logger logger = LoggerFactory.getLogger(ShopRestController.class);
  
  @Autowired
  private MessageSender messageSender;
  
  @RequestMapping(path = "/api/cart/order", method = PUT)
  public String placeOrder(@RequestParam(value = "customerId") String customerId) {
    
    Order order = new Order();
    order.addItem("article1", 5);
    order.addItem("article2", 10);
    
    order.setCustomer(new Customer("Camunda", "Zossener Strasse 55\n10961 Berlin\nGermany"));

    Message<Order> message = new Message<Order>("OrderPlacedEvent", order);
    messageSender.send(message);
        
    // note that we cannot easily return an order id here - as everything is asynchronous
    // and blocking the client is not what we want.
    // but we return an own correlationId which can be used in the UI to show status maybe later
    return "{\"traceId\": \"" + message.getTraceId() + "\"}";
  }

  @RequestMapping(path = "/api/order", method = PUT, consumes = {MediaType.APPLICATION_JSON_VALUE})
  public String placeOrder(@RequestBody Order order) {

    logger.info("The following order has been received: {}", order.toString());

    Message<Order> message = new Message<Order>("OrderPlacedEvent", order);
    messageSender.send(message);

    // note that we cannot easily return an order id here - as everything is asynchronous
    // and blocking the client is not what we want.
    // but we return an own correlationId which can be used in the UI to show status maybe later
    return "{\"traceId\": \"" + message.getTraceId() + "\"}";
  }

}