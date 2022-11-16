package org.apache.pulsar.experiment.shard.app;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.ratis.io.MD5Hash;
import org.apache.ratis.proto.RaftProtos.LogEntryProto;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.protocol.TermIndex;
import org.apache.ratis.server.raftlog.RaftLog;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.StateMachineStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.apache.ratis.statemachine.impl.SingleFileSnapshotInfo;
import org.apache.ratis.util.MD5FileUtil;

public class MetadataStateMachine extends BaseStateMachine {
    private final SimpleStateMachineStorage storage = new SimpleStateMachineStorage();
    List<String> entries = new ArrayList<>();
    @Override
    public void initialize(RaftServer server, RaftGroupId groupId,
                           RaftStorage raftStorage) throws IOException {
        super.initialize(server, groupId, raftStorage);
        this.storage.init(raftStorage);
        loadSnapshot(storage.getLatestSnapshot());
    }

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

    @Override
    public StateMachineStorage getStateMachineStorage() {
        return storage;
    }

    @Override
    public long takeSnapshot() {
        final TermIndex last = getLastAppliedTermIndex();
        final File snapshotFile = storage.getSnapshotFile(last.getTerm(), last.getIndex());
        LOG.info("Taking a snapshot to file {}", snapshotFile);
        try (FileOutputStream fos = new FileOutputStream(snapshotFile)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(this.entries);
            }
        } catch (IOException e) {
            LOG.error("failed to take snapshot", e);
        }
        computeAndSaveMd5ForFile(snapshotFile);

        return last.getIndex();
    }

    void reset() {
        entries.clear();
        setLastAppliedTermIndex(null);
    }

    private long loadSnapshot(SingleFileSnapshotInfo snapshot) throws IOException {
        if (snapshot == null) {
            LOG.warn("The snapshot info is null.");
            return RaftLog.INVALID_LOG_INDEX;
        }
        final File snapshotFile = snapshot.getFile().getPath().toFile();
        if (!snapshotFile.exists()) {
            LOG.warn("The snapshot file {} does not exist for snapshot {}", snapshotFile, snapshot);
            return RaftLog.INVALID_LOG_INDEX;
        }

        // verify md5
        final MD5Hash md5 = snapshot.getFile().getFileDigest();
        if (md5 != null) {
            MD5FileUtil.verifySavedMD5(snapshotFile, md5);
        }

        final TermIndex last = SimpleStateMachineStorage.getTermIndexFromSnapshotFile(snapshotFile);
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(snapshotFile)))) {
            reset();
            setLastAppliedTermIndex(last);
            entries = (List<String>) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load " + snapshot, e);
        }
        return last.getIndex();
    }

    private static MD5Hash computeAndSaveMd5ForFile(File dataFile) {
        final MD5Hash md5;
        try {
            md5 = MD5FileUtil.computeMd5ForFile(dataFile);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compute MD5 for file " + dataFile, e);
        }
        try {
            MD5FileUtil.saveMD5File(dataFile, md5);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save MD5 " + md5 + " for file " + dataFile, e);
        }
        return md5;
    }

}
