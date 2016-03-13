package com.flipkart.storm.mysql;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

public class ZkClient {

    private static final Logger LOGGER              = LoggerFactory.getLogger(ZkClient.class);
    private static final String DEFAULT_CHARSET     = "UTF-8";
    private CuratorFramework client;

    public ZkClient(List<String> servers, int port,
                    int sessionTimeoutMs, int connectionTimeoutMs,
                    int retryTimes, int sleepMsBetweenRetries) {
        try {
                client = CuratorFrameworkFactory.newClient(getZkServerPorts(servers, port),
                                                           sessionTimeoutMs,
                                                           connectionTimeoutMs,
                                                           new RetryNTimes(retryTimes, sleepMsBetweenRetries));

                client.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    public <T> void write(String path, T payload) {
        String data = JSONValue.toJSONString(payload);
        LOGGER.debug("Writing to Zookeeper Path {} Payload {}", path, data);
        writeInternal(path, data.getBytes(Charset.forName(DEFAULT_CHARSET)));
    }

    public <T> T read(String path) {
        try {
            byte[] bytes = readInternal(path);
            if (bytes == null) {
                return null;
            }
            return (T) JSONValue.parse(new String(bytes, DEFAULT_CHARSET));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        client.close();
        client = null;
    }

    private void writeInternal(String path, byte[] payload) {
        try {
            if (client.checkExists().forPath(path) == null) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path, payload);
            } else {
                client.setData().forPath(path, payload);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readInternal(String path) {
        try {
            if (client.checkExists().forPath(path) != null) {
                return client.getData().forPath(path);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getZkServerPorts(List<String> servers, int port) {
        String serverPorts = "";
        for (String server : servers) {
            serverPorts = serverPorts + server + ":" + port + ",";
        }
        return serverPorts;
    }
}