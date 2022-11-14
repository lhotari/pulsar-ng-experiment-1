package org.apache.pulsar.experiment.admin.app;

import org.apache.pulsar.experiment.Constants;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.Message;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class AdminApp {
    public static void main(String[] args) {
        SpringApplication.run(AdminApp.class, args);
    }

    public record TopicName(String name) {

    }

    @RestController
    public static class Controller {
        private final RaftClient client = RaftClient.newBuilder()
                .setProperties(new RaftProperties())
                .setRaftGroup(Constants.RAFT_GROUP)
                .build();

        @PostMapping("/topics")
        public Mono<Void> createTopic(@RequestBody TopicName topicName) {
            return Mono.fromFuture(() -> client.async().send(Message.valueOf(topicName.name())))
                    .then();
        }
    }
}
