package org.apache.pulsar.experiment;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

public class RaftGroupUtil {
    private static final int PEER_COUNT = 3;
    private static final int STARTING_PORT = 10050;

    public static List<RaftPeer> createPeers(int shardIndex) {
        List<RaftPeer> peers = new ArrayList<>();
        for (int i = 0; i < PEER_COUNT; i++) {
            peers.add(RaftPeer.newBuilder()
                    .setAddress(InetSocketAddress.createUnresolved("localhost", STARTING_PORT + (10 * shardIndex) + i))
                    .setId("shard" + shardIndex + "peer" + i)
                    .build());
        }
        return Collections.unmodifiableList(peers);
    }

    public static RaftGroup createRaftGroup(int shardIndex, List<RaftPeer> peers) {
        RaftGroupId raftGroupId =
                RaftGroupId.valueOf(ByteString.copyFromUtf8("shard" + String.format("%011d", shardIndex)));
        return RaftGroup.valueOf(raftGroupId, peers);
    }
    public static RaftGroup createRaftGroup(int shardIndex) {
        return createRaftGroup(shardIndex, createPeers(shardIndex));
    }
}
