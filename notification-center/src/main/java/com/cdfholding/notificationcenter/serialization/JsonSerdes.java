package com.cdfholding.notificationcenter.serialization;

import com.cdfholding.notificationcenter.domain.User;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyRequest;
import com.cdfholding.notificationcenter.events.AllowedUserAppliedEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

public class JsonSerdes {

    public static Serde<AllowedUserApplyRequest> AllowedUserApplyRequest() {
        JsonSerializer<AllowedUserApplyRequest> jsonSerializer = new JsonSerializer<>();
        JsonDeserializer<AllowedUserApplyRequest> jsonDeserializer = new JsonDeserializer<>(AllowedUserApplyRequest.class);

        return Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    }

    // 0926 add
    public static Serde<User> UserSerde() {
        JsonSerializer<User> jsonSerializer = new JsonSerializer<>();
        JsonDeserializer<User> jsonDeserializer = new JsonDeserializer<>(User.class);

        return Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    }
    
    // 0928 add
    public static Serde<AllowedUserAppliedEvent> AllowedUserAppliedEvent() {
        JsonSerializer<AllowedUserAppliedEvent> jsonSerializer = new JsonSerializer<>();
        JsonDeserializer<AllowedUserAppliedEvent> jsonDeserializer = new JsonDeserializer<>(AllowedUserAppliedEvent.class);

        return Serdes.serdeFrom(jsonSerializer, jsonDeserializer);      
    }
}
