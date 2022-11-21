package org.apache.pulsar.experiment.shard.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.Data;
import org.apache.pulsar.experiment.RaftGroupUtil;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftPeer;
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
        int peerIndex;
        File storageDirRoot = new File("build/storage");
    }


    public static void main(String[] args) {
        SpringApplication.run(ShardApp.class, args);
    }

    @Bean
    MetadataServer metadataServer(ShardAppConfig config, ObjectMapper objectMapper) throws IOException {
        List<RaftPeer> peers = RaftGroupUtil.createPeers(config.getShardIndex());
        RaftGroup raftGroup = RaftGroupUtil.createRaftGroup(config.getShardIndex(), peers);
        RaftPeer peer = peers.get(config.getPeerIndex());
        File storageDirRoot = new File(config.getStorageDirRoot(), peer.getId().toString());
        if (!storageDirRoot.exists()) {
            storageDirRoot.mkdirs();
        }
        MetadataServer metadataServer =
                new MetadataServer(raftGroup, peer, storageDirRoot,
                        objectMapper);
        metadataServer.start();
        return metadataServer;
    }
}
