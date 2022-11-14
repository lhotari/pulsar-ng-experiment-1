package org.apache.pulsar.experiment;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

public class Constants {
    public static final int PEER_COUNT = 3;
    public static final int STARTING_PORT = 10050;
    public static final RaftGroupId RAFT_GROUP_ID = RaftGroupId.valueOf(ByteString.copyFromUtf8("helloworld123456"));
    public static List<RaftPeer> RAFT_PEERS = createPeers();

    private static List<RaftPeer> createPeers() {
        List<RaftPeer> peers = new ArrayList<>();
        for (int i = 0; i < PEER_COUNT; i++) {
            peers.add(RaftPeer.newBuilder()
                    .setAddress(InetSocketAddress.createUnresolved("localhost", STARTING_PORT + i))
                    .setId("peer" + i)
                    .build());
        }
        return Collections.unmodifiableList(peers);
    }

    public static RaftGroup RAFT_GROUP = createRaftGroup();

    private static RaftGroup createRaftGroup() {
        return RaftGroup.valueOf(RAFT_GROUP_ID, RAFT_PEERS);
    }
}
