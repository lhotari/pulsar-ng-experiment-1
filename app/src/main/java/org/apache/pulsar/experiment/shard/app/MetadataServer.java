package org.apache.pulsar.experiment.shard.app;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.util.NetUtils;

public final class MetadataServer implements Closeable {
    private final RaftServer server;

    public MetadataServer(RaftGroup raftGroup, RaftPeer peer, File storageDir) throws IOException {
        final RaftProperties properties = new RaftProperties();
        RaftServerConfigKeys.setStorageDir(properties, Collections.singletonList(storageDir));
        RaftServerConfigKeys.Snapshot.setAutoTriggerEnabled(properties, true);
        RaftServerConfigKeys.Snapshot.setAutoTriggerThreshold(properties, 5);
        final int port = NetUtils.createSocketAddr(peer.getAddress()).getPort();
        GrpcConfigKeys.Server.setPort(properties, port);
        final MetadataStateMachine metadataStateMachine = new MetadataStateMachine();
        this.server = RaftServer.newBuilder()
                .setGroup(raftGroup)
                .setProperties(properties)
                .setServerId(peer.getId())
                .setStateMachine(metadataStateMachine)
                .build();
    }

    public void start() throws IOException {
        server.start();
    }

    @Override
    public void close() throws IOException {
        server.close();
    }
}
