package com.uh.rds.testing.config;

import com.uh.rds.testing.conn.ConnectionMode;
import com.uh.rds.testing.conn.Endpoint;
import com.uh.rds.testing.conn.RdsConnectInfo;
import com.uh.rds.testing.conn.Shard;

import java.util.Arrays;

import static com.uh.rds.testing.conn.ConnectionMode.*;

/**
 * 连接配置类，包含连接RDS实例的相关参数
 */
public class ConnectionConfig {

    // 连接模式, 选项不区分大小写: “master”, “sentinel”, “master_slave”, “cluster”, “cluster_pool”
    private String mode;

    // 连接RDS的password
    private String password;

    //哨兵密码
    private String sentinelPassword;

    //哨兵管理的主节点名称
    private String masterName;

    // 集群的分片信息, 如果是单节点或哨兵模式，不需要此配置
    private String[] shards;

    // 连接RDS的地址列表，每个地址格式为：IP:端口,IP:端口,IP:端口。第一个地址为主节点，后续地址为该分片的备节点。注意这里的端口是Redis端口（默认6379）。
    private String[] endpoints;

    // 连接超时时间，单位毫秒，默认2000ms
    private int timeout = 2000;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSentinelPassword() {
        return sentinelPassword;
    }

    public void setSentinelPassword(String sentinelPassword) {
        this.sentinelPassword = sentinelPassword;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public String[] getShards() {
        return shards;
    }

    public void setShards(String[] shards) {
        this.shards = shards;
    }

    public String[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(String[] endpoints) {
        this.endpoints = endpoints;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 生成RdsConnectInfo对象, RdsConnectInfo包含了连接Redis/RDS服务所需的所有信息
     * @return RdsConnectInfo对象
     */
    public RdsConnectInfo getConnectInfo() {
        // 1. 解析Deploy Mode
        ConnectionMode connectionMode = ConnectionMode.from(mode);

        // 2. 创建连接对象
        RdsConnectInfo connection = new RdsConnectInfo(connectionMode,  (connectionMode == SENTINEL) ? sentinelPassword : password);
        connection.setTimeout(timeout);

        if(connectionMode == CLUSTER) {
            // 3. 解析Shards
            for (int i = 0; i < shards.length; i++) {
                connection.addShard(new Shard(i, shards[i]));
            }

            // 4. 解析Endpoints
            if(endpoints == null || endpoints.length != shards.length) {
                throw new IllegalArgumentException("For cluster mode, must provide endpoints for each shard.");
            }

            for(int i=0 ; i<endpoints.length; i++) {
                String shardEndpoints = endpoints[i];
                String[] endpointsArray = shardEndpoints.split(",");
                for(int j=0 ; j<endpointsArray.length; j++) {
                    String ep = endpointsArray[j];
                    String[] parts = ep.split(":");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid endpoint format: " + ep);
                    }
                    String host = parts[0];
                    int port;
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid port number in endpoint: " + ep);
                    }
                    boolean isMaster = (j == 0);
                    connection.addEndpoint(new Endpoint(host, port, isMaster, i));
                }
            }
        }
        else { // MASTER, SENTINEL, MASTER_SLAVE, CLUSTER_POOL
            // 3. 解析Shards
            if(endpoints == null || endpoints.length == 0) {
                throw new IllegalArgumentException("Must provide exactly one endpoint.");
            }
            for(int j=0 ; j<endpoints.length; j++) {
                String[] endpointsArray = endpoints[j].split(",");
                for (int i = 0; i < endpointsArray.length; i++) {
                    String ep = endpointsArray[i];
                    String[] parts = ep.split(":");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid endpoint format: " + ep);
                    }
                    String host = parts[0];
                    int port;
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid port number in endpoint: " + ep);
                    }
                    boolean isMaster = (connectionMode != MASTER_SLAVE) || (i == 0);
                    connection.addEndpoint(new Endpoint(host, port, isMaster));
                }
            }
        }

        return connection;
    }

    public void validate() {
        // 验证mode
        ConnectionMode.from(mode);

        // 验证endpoints
        if(endpoints == null || endpoints.length == 0) {
            throw new IllegalArgumentException(toString() + " endpoints must be provided.");
        }

        // 如果是Cluster模式，验证shards
        ConnectionMode connectionMode = ConnectionMode.from(mode);
        if(connectionMode == CLUSTER) {
            if(shards == null || shards.length == 0) {
                throw new IllegalArgumentException(toString() + "shards must be provided for cluster mode.");
            }
            if(shards.length != endpoints.length) {
                throw new IllegalArgumentException(toString() + "shards length must match endpoints length for cluster mode.");
            }
        }
    }

    @Override
    public String toString() {
        return "ConnectionConfig{" +
                "mode='" + mode + '\'' +
                ", password='" + password + '\'' +
                ", sentinelPassword='" + sentinelPassword + '\'' +
                ", masterName='" + masterName + '\'' +
                ", shards=" + Arrays.toString(shards) +
                ", endpoints=" + Arrays.toString(endpoints) +
                '}';
    }
}
