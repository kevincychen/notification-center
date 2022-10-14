package com.cdfholding.notificationcenter.controller;


import com.cdfholding.notificationcenter.dto.AllowedUserApplyRequest;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyResponse;
import com.cdfholding.notificationcenter.dto.DeletedAllowedUserResponse;
import com.cdfholding.notificationcenter.events.AllowedUserAppliedEvent;
import com.cdfholding.notificationcenter.service.KafkaService;
import com.cdfholding.notificationcenter.service.RestTemplateService;
import com.cdfholding.notificationcenter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Admin Management 管理功能")
@RestController
public class AdminController {

  KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate;
  @Autowired
  StreamsBuilderFactoryBean factoryBean;
  @Autowired
  UserService userService;
  @Autowired
  RestTemplateService restTemplateService;
  @Autowired
  KafkaService<AllowedUserApplyRequest, AllowedUserAppliedEvent> kafkaService;

  public AdminController(KafkaTemplate<String, AllowedUserApplyRequest> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }


  @Operation(summary = "新增", description = "將使用者資訊傳入並新增")
  @PostMapping(path = "/apply")
  public AllowedUserApplyResponse apply(@RequestBody AllowedUserApplyRequest request) {
    request.setType("apply");

    // send to Kafka
    try {
      kafkaService.sendToTopic(
          "allowed-user-command", request.getAdUser(), request, 10);

      AllowedUserAppliedEvent value = kafkaService.getKTable("eventTable",
          "checkEvent/" + request.getAdUser(), request.getAdUser(), AllowedUserAppliedEvent.class,
          false);
      System.out.println("value(Controller) = " + value);

      return new AllowedUserApplyResponse(value.getAdUser(), value.getResult(), value.getReason());
    } catch (ExecutionException e) {
      return new AllowedUserApplyResponse(request.getAdUser(), "ExecutionException", null);
    } catch (InterruptedException e) {
      return new AllowedUserApplyResponse(request.getAdUser(), "InterruptedException", null);
    } catch (TimeoutException e) {
      return new AllowedUserApplyResponse(request.getAdUser(), "TimeoutException", null);
    }
  }

  @SneakyThrows
  @Operation(summary = "刪除", description = "刪除使用者使用權限")
  @GetMapping(path = "/delete/{adUser}")
  public DeletedAllowedUserResponse delete(
      @Parameter(required = true, description = "使用者名稱", example = "AD12345") @PathVariable("adUser") String adUser) {

    DeletedAllowedUserResponse response;
    int deleteNum = userService.delete(adUser);
    if (deleteNum <= 0) {

      response = new DeletedAllowedUserResponse(adUser, "Failure", "USER NOT FOUND");

    } else {

      response = new DeletedAllowedUserResponse(adUser, "Successful", "");

    }
    return response;
  }

  @SneakyThrows
  @Operation(summary = "檢查", description = "檢查使用者新增結果")
  @GetMapping(path = "/checkEvent/{adUser}")
  public AllowedUserAppliedEvent checkEvent(
      @Parameter(required = true, description = "使用者名稱", example = "AD12345") @PathVariable("adUser") String adUser) {
    return kafkaService.getKTable("eventTable", null, adUser, AllowedUserAppliedEvent.class, true);
  }

}
