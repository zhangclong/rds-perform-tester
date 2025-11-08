package com.uh.rds.testing.conn;

import com.uh.rds.testing.utils.ValueUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 连接RDS或者Redis的连接信息。
 */
public class RdsConnectInfo {

    ConnectionMode mode;

    List<Endpoint>  endpoints = new ArrayList<>(6);

    Map<Integer, Shard> shardsMap = new java.util.HashMap<>(3);

    List<Shard> shards = null;

    String password = null;

    // 超时时间毫秒
    int timeout = 3000;

    private boolean initialized = false; //是否已经初始化, 被类内部使用

    public RdsConnectInfo(ConnectionMode mode) {
        this.mode = mode;
    }

    /**
     * 构造函数,
     * @param mode  连接模式   MASTER|SENTINEL|MASTER_SLAVE|CLUSTER|CLUSTER_POOL
     * @param password  密码。如果没有密码，可以为空
     */
    public RdsConnectInfo(ConnectionMode mode, String password) {
        this.mode = mode;
        this.password = password;
    }

    public ConnectionMode getMode() {
        return mode;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public List<Endpoint> getMasterEndpoints() {
        List<Endpoint> masterEndpoints = new ArrayList<>();
        for(Endpoint endpoint : endpoints) {
            if(endpoint.isMaster()) {
                masterEndpoints.add(endpoint);
            }
        }
        return masterEndpoints;
    }

    public List<Endpoint> getSlaveEndpoints() {
        List<Endpoint> slaveEndpoints = new ArrayList<>();
        for(Endpoint endpoint : endpoints) {
            if(!endpoint.isMaster()) {
                slaveEndpoints.add(endpoint);
            }
        }
        return slaveEndpoints;
    }

    public List<Shard> getShards() {
        if(initialized == false) {
            if(shardsMap.size() > 0) {
                resetEndpointsToShards();
                this.shards = shardsMap.values().stream().sorted((s1, s2) -> s1.getIndex() - s2.getIndex()).collect(Collectors.toList());
            }
            else {
                this.shards = new ArrayList<>(0);
            }
            initialized = true;
        }

        return this.shards;
    }


    public String getPassword() {
        return password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isAuth() {
        return ValueUtils.notEmpty(password);
    }

    public RdsConnectInfo addEndpoint(Endpoint endpoint) {
        endpoint.setConnectInfo(this);
        this.endpoints.add(endpoint);
        return this;
    }

    public RdsConnectInfo addShard(Shard shard) {
        this.shardsMap.put(shard.getIndex(), shard);
        return this;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RdsConnectInfo{");
        sb.append("mode=").append(mode)
                .append(", endpoints=").append(endpoints)
                .append(", shards=").append(getShards());
                if (password != null && !password.isEmpty()) {
                    sb.append(", password='").append("*".repeat(password.length())).append("'");
                }
                sb.append(", timeout=").append(timeout)
                .append(", initialized=").append(initialized)
                .append('}');
        return sb.toString();
    }

    private void resetEndpointsToShards() {
        // 清空主从节点信息
        for(Shard shard : shardsMap.values()) {
            shard.cleanEndpoints();
        }

        // 装填主从节点信息
        for(Endpoint endpoint : endpoints) {
            Shard shard = shardsMap.get(endpoint.getShard());
            if(shard != null) {
                if (endpoint.isMaster()) {
                    shard.setMaster(endpoint);
                } else {
                    shard.addSlave(endpoint);
                }
            }
        }
    }
}
