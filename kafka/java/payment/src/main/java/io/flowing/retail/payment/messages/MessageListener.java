package io.flowing.retail.payment.messages;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import io.flowing.retail.payment.dto.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@EnableBinding(Sink.class)
public class MessageListener {    
  
  @Autowired
  private MessageSender messageSender;

  @StreamListener(target = Sink.INPUT, 
      condition="(headers['messageType']?:'')=='OrderApprovedEvent'")
  @Transactional
  public void orderApprovedReceived(Message<Order> message) throws JsonParseException, JsonMappingException, IOException {

    message.setMessageType("PaymentReceivedEvent");
    messageSender.send(message);

  }
  
    
    
}
