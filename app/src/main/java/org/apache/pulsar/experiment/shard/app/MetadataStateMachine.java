package org.apache.pulsar.experiment.shard.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.ratis.proto.RaftProtos.LogEntryProto;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;

public class MetadataStateMachine extends BaseStateMachine {
    List<String> entries = new ArrayList<>();

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final LogEntryProto entry = trx.getLogEntry();
        final String logData = entry.getStateMachineLogEntry().getLogData().toStringUtf8();
        final TermIndex termIndex = TermIndex.valueOf(entry);
        entries.add(logData);
        LOG.info("{} {}: Added {}, Total size {}", trx.getServerRole(), termIndex, logData, entries.size());
        updateLastAppliedTermIndex(termIndex);
        return CompletableFuture.completedFuture(Message.valueOf("ok"));
    }
}
