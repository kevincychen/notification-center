package com.cdfholding.notificationcenter.controller;

import com.cdfholding.notificationcenter.dto.AllowedUserApplyRequest;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate;

    public AdminController(KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping(path = "/apply")
    public AllowedUserApplyResponse apply(@RequestBody AllowedUserApplyRequest request) {
        request.setType("apply");
        ListenableFuture<SendResult<String, AllowedUserApplyRequest>> resultTemplate = 
            kafkaTemplate.send("allowed-user-command", request.getAdUser(), request);
        while(null != resultTemplate.get()) {
          
        }
        return new AllowedUserApplyResponse("cdfh3593", "success", "none");
    }
}
