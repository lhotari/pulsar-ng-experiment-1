package org.apache.pulsar.experiment.shard.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.metrics.JVMMetrics;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.util.NetUtils;
import org.apache.ratis.util.TimeDuration;

public final class MetadataServer implements Closeable {
    private final RaftServer server;

    public MetadataServer(RaftGroup raftGroup, RaftPeer peer, File storageDir, ObjectMapper objectMapper) throws IOException {
        JVMMetrics.initJvmMetrics(TimeDuration.valueOf(60, TimeUnit.SECONDS));
        final RaftProperties properties = new RaftProperties();
        RaftServerConfigKeys.setStorageDir(properties, Collections.singletonList(storageDir));
        final int port = NetUtils.createSocketAddr(peer.getAddress()).getPort();
        GrpcConfigKeys.Server.setPort(properties, port);
        final MetadataStateMachine metadataStateMachine = new MetadataStateMachine(objectMapper);
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
