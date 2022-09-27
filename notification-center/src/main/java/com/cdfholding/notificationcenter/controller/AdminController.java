package com.cdfholding.notificationcenter.controller;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyRequest;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyResponse;

@RestController
public class AdminController {

    KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate;

    public AdminController(KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

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
            System.out.println("Sent message=[allowed-user-command] with offset=[" + 
                result.getRecordMetadata().offset() + "]: " +
                    result.getRecordMetadata().topic());
          }
          
        });
        return new AllowedUserApplyResponse("cdfh3593", "success", "none");
    }
}
