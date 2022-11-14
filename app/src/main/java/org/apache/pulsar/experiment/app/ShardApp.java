package org.apache.pulsar.experiment.app;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import lombok.Data;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(ShardApp.ShardAppConfig.class)
public class ShardApp {
    @ConfigurationProperties("shardapp")
    @Data
    public static class ShardAppConfig {
        int shardIndex;
    }


    public static void main(String[] args) {
        SpringApplication.run(ShardApp.class, args);
    }

    @Bean
    MetadataServer metadataServer(ShardAppConfig config) throws IOException {
        RaftPeer[] peers = new RaftPeer[3];
        for (int i = 0; i < 3; i++) {
            peers[i]=RaftPeer.newBuilder()
                    .setAddress(InetSocketAddress.createUnresolved("localhost", 10050 + i))
                    .setId("peer" + i)
                    .build();
        }
        RaftGroup raftGroup = RaftGroup.valueOf(RaftGroupId.valueOf(ByteString.copyFromUtf8("helloworld123456")), peers);
        RaftPeer peer = peers[config.getShardIndex()];
        File storageDirRoot = new File("/tmp/shardapp", peer.getId().toString());
        if (!storageDirRoot.exists()) {
            storageDirRoot.mkdirs();
        }
        MetadataServer metadataServer = new MetadataServer(raftGroup, peer, storageDirRoot);
        metadataServer.start();
        return metadataServer;
    }
}
