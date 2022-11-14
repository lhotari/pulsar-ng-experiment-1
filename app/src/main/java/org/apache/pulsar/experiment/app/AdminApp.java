package org.apache.pulsar.experiment.app;

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

        @PostMapping("/topics")
        public Mono<String> createTopic(@RequestBody TopicName topicName) {
            return Mono.just("Creating " + topicName);
        }
    }
}
