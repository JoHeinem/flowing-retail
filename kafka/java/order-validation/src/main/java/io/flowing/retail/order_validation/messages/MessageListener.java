package io.flowing.retail.order_validation.messages;

import io.flowing.retail.order_validation.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Component
@EnableBinding(Sink.class)
public class MessageListener {

  private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
  
  @Autowired
  private MessageSender messageSender;

  @StreamListener(target = Sink.INPUT, 
      condition="(headers['messageType']?:'')=='OrderPlacedEvent'")
  @Transactional
  public void orderPlacedReceived(Message<Order> message) {

    logger.info("Received an OrderPlacedEvent with the order: {}", message.getPayload());

    double prob = ThreadLocalRandom.current().nextDouble();
    if (prob > 0.1) {
      message.setMessageType("OrderApprovedEvent");
    } else {
      message.setMessageType("OrderRejectedEvent");
    }
    messageSender.send(message);
  }
  
    
    
}
