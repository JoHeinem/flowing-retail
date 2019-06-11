package io.flowing.retail.inform_customer.messages;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.flowing.retail.inform_customer.dto.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Component
@EnableBinding(Sink.class)
public class MessageListener {    
  
  @Autowired
  private MessageSender messageSender;

  @StreamListener(target = Sink.INPUT, 
      condition="(headers['messageType']?:'')=='OrderPlacedEvent'")
  @Transactional
  public void orderPlacedReceived(Message<Order> message) throws JsonParseException, JsonMappingException, IOException {

    double prob = ThreadLocalRandom.current().nextDouble();
    if (prob > 0.1) {
      message.setMessageType("OrderApprovedEvent");
    } else {
      message.setMessageType("OrderRejectedEvent");
    }
    messageSender.send(message);
  }
  
    
    
}
