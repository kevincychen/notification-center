package com.cdfholding.notificationcenter.kafka;

import com.cdfholding.notificationcenter.config.KafkaConfigProperties;
import com.cdfholding.notificationcenter.serialization.JsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfiguration {

  @Autowired
  KafkaConfigProperties kafkaConfigProperties;

  @Bean
  public ProducerFactory<String, ?> producerFactory() {
    Map<String, Object> props = new HashMap<>();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigProperties.getProducer_bootstrap_servers());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, kafkaConfigProperties.getStreams_replication_factor());

    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  public KafkaTemplate<String, ?> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

}
