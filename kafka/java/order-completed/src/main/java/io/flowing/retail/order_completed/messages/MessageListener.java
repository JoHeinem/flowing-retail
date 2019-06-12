package io.flowing.retail.order_completed.messages;

import io.flowing.retail.order_completed.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@EnableBinding(Sink.class)
public class MessageListener {

  private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
  
  @Autowired
  private MessageSender messageSender;

  @StreamListener(target = Sink.INPUT, 
      condition="(headers['messageType']?:'')=='GoodShippedEvent'")
  @Transactional
  public void goodShippedReceived(Message<Order> message) {

    logger.info("Received an GoodShippedEvent with the order: {}", message.getPayload());
    message.setId(UUID.randomUUID().toString());
    message.setMessageType("OrderCompletedEvent");
    messageSender.send(message);
  }

}
