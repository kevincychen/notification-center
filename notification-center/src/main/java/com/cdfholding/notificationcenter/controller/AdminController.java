package com.cdfholding.notificationcenter.controller;

import com.cdfholding.notificationcenter.dto.AllowedUserApplyRequest;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyResponse;
import com.cdfholding.notificationcenter.events.AllowedUserAppliedEvent;
import com.cdfholding.notificationcenter.service.RestTemplateService;
import java.util.Collection;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsMetadata;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

  final HostInfo hostInfo = new HostInfo("127.0.0.1", 8090);

  KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate;

  @Autowired
  StreamsBuilderFactoryBean factoryBean;

  @Autowired
  RestTemplateService restTemplateService;

  public AdminController(KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }


  @PostMapping(path = "/apply")
  public AllowedUserApplyResponse apply(@RequestBody AllowedUserApplyRequest request) {
    request.setType("apply");
    // send to Kafka
    kafkaTemplate.send("allowed-user-command", request.getAdUser(), request);

    // Create KafkaStreams
    KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();
    StringSerializer stringSerializer = new StringSerializer();

    // stream eventTable find HostInfo
    KeyQueryMetadata keyMetada = kafkaStreams.queryMetadataForKey("eventTable", request.getAdUser(),
        stringSerializer);

    AllowedUserAppliedEvent value = new AllowedUserAppliedEvent();

    if (!hostInfo.equals(keyMetada.activeHost())) {
      System.out.println("HostInfo is different!!");

      // Print all metadata HostInfo
      Collection<StreamsMetadata> metadata = kafkaStreams.metadataForAllStreamsClients();
      System.out.println("MetaDataclient:" + metadata.size());
      for (StreamsMetadata streamsMetadata : metadata) {
        System.out.println(
            "Host info -> " + streamsMetadata.hostInfo().host() + " : " + streamsMetadata.hostInfo()
                .port());
        System.out.println(streamsMetadata.stateStoreNames());
      }

      // Remote
      value = (AllowedUserAppliedEvent) restTemplateService.restTemplate(
          "checkEvent/" + request.getAdUser(), keyMetada.activeHost().host(),
          keyMetada.activeHost().port());

    } else {

      ReadOnlyKeyValueStore<String, AllowedUserAppliedEvent> keyValueStore = kafkaStreams.store(
          StoreQueryParameters.fromNameAndType("eventTable", QueryableStoreTypes.keyValueStore()));
      
      value = keyValueStore.get(request.getAdUser());

      System.out.println(value);

      KeyValueIterator<String, AllowedUserAppliedEvent> range = keyValueStore.all();
    }

    return new AllowedUserApplyResponse(value.getAdUser(), value.getResult(), value.getReason());
  }

  @GetMapping(path = "/checkEvent/{adUser}")
  public AllowedUserAppliedEvent checkEvent(@PathVariable("adUser") String adUser) {

    KafkaStreams kafkaStreams = factoryBean.getKafkaStreams();

    ReadOnlyKeyValueStore<String, AllowedUserAppliedEvent> keyValueStore = kafkaStreams.store(
        StoreQueryParameters.fromNameAndType("eventTable", QueryableStoreTypes.keyValueStore()));

    AllowedUserAppliedEvent value = keyValueStore.get(adUser);

    System.out.println(value);

    KeyValueIterator<String, AllowedUserAppliedEvent> range = keyValueStore.all();

    return value;
  }

}
