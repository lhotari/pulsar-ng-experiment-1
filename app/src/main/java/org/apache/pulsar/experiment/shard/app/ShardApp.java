package org.apache.pulsar.experiment.shard.app;

import java.io.File;
import java.io.IOException;
import lombok.Data;
import org.apache.pulsar.experiment.Constants;
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
    }


    public static void main(String[] args) {
        SpringApplication.run(ShardApp.class, args);
    }

    @Bean
    MetadataServer metadataServer(ShardAppConfig config) throws IOException {
        RaftPeer peer = Constants.RAFT_PEERS.get(config.getShardIndex());
        File storageDirRoot = new File("/tmp/shardapp", peer.getId().toString());
        if (!storageDirRoot.exists()) {
            storageDirRoot.mkdirs();
        }
        MetadataServer metadataServer = new MetadataServer(Constants.RAFT_GROUP, peer, storageDirRoot);
        metadataServer.start();
        return metadataServer;
    }
}
