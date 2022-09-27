package com.cdfholding.notificationcenter.kafka;

import com.cdfholding.notificationcenter.domain.User;
import com.cdfholding.notificationcenter.dto.AllowedUserApplyRequest;
import com.cdfholding.notificationcenter.serialization.JsonSerdes;
import com.cdfholding.notificationcenter.service.LdapService;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationTopology {

    @Autowired
    LdapService ldapService;

    @Autowired
    void pipeline(StreamsBuilder streamsBuilder) {
        KStream<String, AllowedUserApplyRequest> commandStream = streamsBuilder
                .stream("allowed-user-command", Consumed.with(Serdes.String(), JsonSerdes.AllowedUserApplyRequest()));

        commandStream.print(Printed.toSysOut());

        Map<String, KStream<String, AllowedUserApplyRequest>> branches = commandStream.split(Named.as("Branch-"))
                .branch((key, value) -> value.getType().equals("apply"), Branched.as("ApplyRequest"))
                .defaultBranch(Branched.as("Others"));

        KStream<String, AllowedUserApplyRequest> applyRequestKStream = branches.get("Branch-ApplyRequest");
        //KStream<String, AllowedUserApplyRequest> otherKStream = branches.get("Branch-Others");
        KStream<String, User> userKStream = applyRequestKStream
                .mapValues(allowedUserApplyRequest -> queryLdap(allowedUserApplyRequest.getAdUser()));

        Map<String, KStream<String, User>> userBranches = userKStream
                //.split(Named.as("Branch-")) //0923 comment
        		.split(Named.as("Branch2-")) //0923 add
                .branch((key, value) -> value.getLdapInfo().getIsValid(), Branched.as("ValidUsers"))
                .branch((key, value) -> !value.getLdapInfo().getIsValid(), Branched.as("InvalidUsers"))
                .noDefaultBranch();

        // KStream<String, User> validUsers = userBranches.get("Branch-ValidUsers"); //0923 comment
        KStream<String, User> validUsers = userBranches.get("Branch2-ValidUsers"); //0923 add
        // 後面還差 送到 events 和 allowed-user 的 compacted topic 裡
        // 0926 add 3.1將user寫allowed-user (compacted topic) 
        validUsers.to("allowed-user", Produced.with(Serdes.String(), JsonSerdes.UserSerde()));
        // 0926 add 3.2將event寫allowed-user-events
        userKStream.to("allowed-user-events", Produced.with(Serdes.String(), JsonSerdes.UserSerde()));
        // 0927 create KTable for "allowed-user-events"
        streamsBuilder.table(
            "allowed-user-events", 
                Consumed.with(Serdes.String(), JsonSerdes.UserSerde()), 
                    Materialized.as("userEventTable"));
    }

    private User queryLdap(String adUser) {
        User user = new User();
        user.setAdUser(adUser);
        user.setLdapInfo(ldapService.query(adUser));
        return user;
    }
}
