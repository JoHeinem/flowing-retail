package io.flowing.retail.shipping.messages;

import io.flowing.retail.shipping.dto.Order;
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
      condition="(headers['messageType']?:'')=='GoodsAvailableEvent'")
  @Transactional
  public void goodAvailableReveived(Message<Order> message) {

    logger.info(
      "The following order has successfully being shipped: {}",
      message.getPayload()
    );

    message.setMessageType("GoodShippedEvent");
    messageSender.send(message);

  }
    
    
}
