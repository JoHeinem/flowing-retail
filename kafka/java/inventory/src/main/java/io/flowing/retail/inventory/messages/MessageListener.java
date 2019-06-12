package io.flowing.retail.inventory.messages;

import io.flowing.retail.inventory.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@EnableBinding(Sink.class)
public class MessageListener {

  private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
  
  @Autowired
  private MessageSender messageSender;
  
  @StreamListener(target = Sink.INPUT,
      condition="(headers['messageType']?:'')=='PaymentReceivedEvent'")
  @Transactional
  public void paymentReveivedReveived(Message<Order> message) {

    logger.info("Received an PaymentReceivedEvent with the order: {}", message.getPayload());

    message.setId(UUID.randomUUID().toString());
    double prob = ThreadLocalRandom.current().nextDouble();
    if (prob > 0.05) {
      message.setMessageType("GoodsAvailableEvent");
    } else {
      message.setMessageType("GoodsUnavailableEvent");
    }

    messageSender.send(message);
  }

}
