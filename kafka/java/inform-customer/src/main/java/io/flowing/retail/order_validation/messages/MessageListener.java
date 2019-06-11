package io.flowing.retail.order_validation.messages;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.flowing.retail.order_validation.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@EnableBinding(Sink.class)
public class MessageListener {

  private static final Logger logger = LoggerFactory.getLogger(MessageListener.class);
  
  @Autowired
  private MessageSender messageSender;

  @StreamListener(target = Sink.INPUT,
    condition = "(headers['messageType']?:'')=='GoodsUnavailableEvent'")
  @Transactional
  public void goodUnavailableReveived(Message<Order> message) throws JsonParseException, JsonMappingException, IOException {

    logger.info(
      "The following order could not be shipped, since they are not available in our inventory: {}",
      message.getPayload()
    );
  }


}
