package io.flowing.retail.payment.messages;

import io.flowing.retail.payment.dto.Order;
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
      condition="(headers['messageType']?:'')=='OrderApprovedEvent'")
  @Transactional
  public void orderApprovedReceived(Message<Order> message) {

    logger.info("Received an OrderApprovedEvent with the order: {}", message.getPayload());
    Message<Order> newMessage = new Message<>(message.getMessageType(), message.getTraceId(), message.getPayload());
    newMessage.setMessageType("PaymentReceivedEvent");
    messageSender.send(newMessage);

  }
  
    
    
}
