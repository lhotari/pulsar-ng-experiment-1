package org.apache.pulsar.experiment.admin.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.experiment.RaftGroupUtil;
import org.apache.pulsar.experiment.TopicName;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.protocol.Message;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@SpringBootApplication
public class AdminApp {
    public static void main(String[] args) {
        SpringApplication.run(AdminApp.class, args);
    }

    @RestController
    @Slf4j
    public static class Controller {
        private static final int DEFAULT_BATCH_SIZE = 100;

        private static final int SHARD_COUNT = 5;
        private final List<RaftClient> clients = IntStream.range(0, SHARD_COUNT)
                .mapToObj(shardIndex ->  RaftClient.newBuilder()
                .setProperties(new RaftProperties())
                .setRaftGroup(RaftGroupUtil.createRaftGroup(shardIndex))
                .build()).toList();
        private final ObjectMapper objectMapper;

        public Controller(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @PostMapping("/topics")
        public Flux<Void> createTopic(@RequestBody Flux<TopicName> topicNames,
                                      @RequestParam(required = false) Optional<Integer> batchSize) {
            return topicNames.map(topicName -> Tuples.of(calculateHash(topicName), topicName))
                    .groupBy(tuple -> signSafeMod(tuple.getT1(), SHARD_COUNT), DEFAULT_BATCH_SIZE * SHARD_COUNT)
                    .flatMap(groupFlux -> {
                        int shardIndex = groupFlux.key();
                        RaftClient client = clients.get(shardIndex);
                        return groupFlux.buffer(batchSize.orElse(DEFAULT_BATCH_SIZE)).concatMap(topicNameBatch -> {
                            AtomicReference<Long> startTime = new AtomicReference<>();
                            return Mono.fromFuture(() -> {
                                        startTime.set(System.nanoTime());
                                        String json;
                                        try {
                                            json = objectMapper.writeValueAsString(topicNameBatch);
                                        } catch (JsonProcessingException e) {
                                            throw new UncheckedIOException(e);
                                        }
                                        return client.async().send(Message.valueOf(json));
                                    })
                                    .doOnSuccess(reply -> {
                                        long durationNanos = System.nanoTime() - startTime.get();
                                        log.info("Completed adding batch size {} index:{} duration:{}ms",
                                                topicNameBatch.size(),
                                                reply.getLogIndex(),
                                                TimeUnit.NANOSECONDS.toMillis(durationNanos));
                                    })
                                    .then();
                        }, 0);
                    });
        }

        private static int signSafeMod(long dividend, int divisor) {
            int mod = (int) (dividend % divisor);

            if (mod < 0) {
                mod += divisor;
            }

            return mod;
        }

        private int calculateHash(TopicName topicName) {
            return Hashing.murmur3_32_fixed().hashString(topicName.name(), StandardCharsets.UTF_8).asInt();
        }
    }
}
