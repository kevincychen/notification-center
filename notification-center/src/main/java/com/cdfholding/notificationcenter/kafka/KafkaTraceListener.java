package com.cdfholding.notificationcenter.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaTraceListener {


  @KafkaListener(topics = "kafka-test", groupId = "group_id")
  public void listenKafkaTest(String message) {
    System.out.println("Received Message: " + message);
  }

}
