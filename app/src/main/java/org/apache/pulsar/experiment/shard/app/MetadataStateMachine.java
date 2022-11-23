package org.apache.pulsar.experiment.shard.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.pulsar.experiment.TopicName;
import org.apache.ratis.proto.RaftProtos.LogEntryProto;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;

public class MetadataStateMachine extends BaseStateMachine {
    private final ObjectReader listOfTopicNameReader;
    Set<TopicName> entries = new HashSet<>();

    public MetadataStateMachine(ObjectMapper objectMapper) {
        listOfTopicNameReader = objectMapper.readerForListOf(TopicName.class);
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final LogEntryProto entry = trx.getLogEntry();
        final List<TopicName> topicNames;
        try {
            topicNames = listOfTopicNameReader.readValue(entry.getStateMachineLogEntry().getLogData().newInput());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        final TermIndex termIndex = TermIndex.valueOf(entry);
        entries.addAll(topicNames);
        LOG.info("{} {}: Added {} entries, Total size {}", trx.getServerRole(), termIndex, topicNames.size(),
                entries.size());
        updateLastAppliedTermIndex(termIndex);
        return CompletableFuture.completedFuture(Message.EMPTY);
    }

    @Override
    public CompletableFuture<Message> query(Message request) {
        String requestString = request.getContent().toStringUtf8();
        LOG.info("Querying {}", requestString);
        if (entries.contains(new TopicName(requestString))) {
            return CompletableFuture.completedFuture(request);
        } else {
            return CompletableFuture.completedFuture(Message.EMPTY);
        }
    }
}
