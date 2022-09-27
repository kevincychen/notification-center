package com.cdfholding.notificationcenter.controller;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyRequest;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyResponse;
import com.cdfholding.notificationcenter.events.AllowedUserAppliedEvent;

@RestController
public class AdminController {

    KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate;

    public AdminController(KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Autowired
    StreamsBuilderFactoryBean streamBuilderFactoryBeam;
    
    @PostMapping(path = "/apply")
    public AllowedUserApplyResponse apply(@RequestBody AllowedUserApplyRequest request) {
        request.setType("apply");
        ListenableFuture<SendResult<String, AllowedUserApplyRequest>> resultFuture = 
            kafkaTemplate.send("allowed-user-command", request.getAdUser(), request);
        // 0927 add callback
        resultFuture.addCallback(new ListenableFutureCallback<SendResult<String, AllowedUserApplyRequest>>() {

          @Override
          public void onFailure(Throwable ex) {
              System.out.println("Unable to send message=[allowed-user-command] due to : " + ex.getMessage());           
          }

          @Override
          public void onSuccess(SendResult<String, AllowedUserApplyRequest> result) {
              System.out.println("Sent message=[" + result.getRecordMetadata().topic() + "] with offset=[" + 
                  result.getRecordMetadata().offset() + "]");
          }
          
        });
        // 0927 get message of NotificationTopology from KTable
        KafkaStreams kafkaStreams = streamBuilderFactoryBeam.getKafkaStreams();
        ReadOnlyKeyValueStore<String, AllowedUserAppliedEvent> keyValueStore = 
            kafkaStreams.store(StoreQueryParameters.fromNameAndType(
                "userEventTable", QueryableStoreTypes.keyValueStore()));
        AllowedUserAppliedEvent eventValue = keyValueStore.get(request.getAdUser());
        System.out.println("AdminController->apply->eventValue: " + eventValue);
        KeyValueIterator<String, AllowedUserAppliedEvent> iter = 
            keyValueStore.all();
        while(null != iter.next()) {
          
        }
        return new AllowedUserApplyResponse("cdfh3593", "success", "none");
    }
}
