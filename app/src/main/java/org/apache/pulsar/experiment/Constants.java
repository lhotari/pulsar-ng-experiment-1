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
    public static List<RaftPeer> RAFT_PEERS = createPeers();

    private static List<RaftPeer> createPeers() {
        List<RaftPeer> peers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            peers.add(RaftPeer.newBuilder()
                    .setAddress(InetSocketAddress.createUnresolved("localhost", 10050 + i))
                    .setId("peer" + i)
                    .build());
        }
        return Collections.unmodifiableList(peers);
    }

    public static RaftGroup RAFT_GROUP = createRaftGroup();

    private static RaftGroup createRaftGroup() {
        return RaftGroup.valueOf(RaftGroupId.valueOf(ByteString.copyFromUtf8("helloworld123456")), RAFT_PEERS);
    }
}
