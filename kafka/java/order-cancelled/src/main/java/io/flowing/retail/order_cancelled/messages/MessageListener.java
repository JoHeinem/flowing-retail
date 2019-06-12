package io.flowing.retail.order_cancelled.messages;

import io.flowing.retail.order_cancelled.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableBinding(Sink.class)
public class MessageListener {

  private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
  
  @Autowired
  private MessageSender messageSender;

  @StreamListener(target = Sink.INPUT, 
      condition="(headers['messageType']?:'')=='OrderRejectedEvent'")
  @Transactional
  public void orderPlacedReceived(Message<Order> message) {

    logger.info("Received an OrderRejectedEvent with the order: {}", message.getPayload());
    Message<Order> newMessage = new Message<>(message.getMessageType(), message.getTraceId(), message.getPayload());
    newMessage.setMessageType("OrderCancelledEvent");
    messageSender.send(newMessage);
  }

  @StreamListener(target = Sink.INPUT,
      condition="(headers['messageType']?:'')=='CustomerInformedEvent'")
  @Transactional
  public void customerInformedReceived(Message<Order> message) {

    logger.info("Received an CustomerInformedEvent with the order: {}", message.getPayload());
    Message<Order> newMessage = new Message<>(message.getMessageType(), message.getTraceId(), message.getPayload());
    newMessage.setMessageType("OrderCancelledEvent");
    messageSender.send(newMessage);
  }
  
    
    
}
