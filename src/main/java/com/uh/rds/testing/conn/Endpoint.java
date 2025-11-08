package com.uh.rds.testing.conn;

import com.uh.rds.testing.utils.ValueUtils;
import redis.clients.jedis.Jedis;

import java.util.Objects;

public class Endpoint {

    String host;

    int port;

    boolean master;

    int shard;

    RdsConnectInfo connectInfo;

    public Endpoint(String host, int port) {
        this(host, port, true, 0);
    }

    public Endpoint(String host, int port, boolean master) {
        this(host, port, master, 0);
    }

    public Endpoint(String host, int port, boolean master, int shard) {
        this.host = host;
        this.port = port;
        this.master = master;
        this.shard = shard;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public int getShard() {
        return shard;
    }

    public String getPassword() {
        return connectInfo.password;
    }

    public int getTimeout() {
        return connectInfo.timeout;
    }

    public void setConnectInfo(RdsConnectInfo connectInfo) {
        this.connectInfo = connectInfo;
    }

    public Jedis newJedis() {
        Jedis jedis = new Jedis(host, port, getTimeout());
        if(ValueUtils.notEmpty(getPassword()) ){
            jedis.auth(getPassword());
        }
        return jedis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return port == endpoint.port && master == endpoint.master && shard == endpoint.shard && Objects.equals(host, endpoint.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, master, shard);
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", master=" + master +
                ", shard=" + shard +
                "}\n";
    }
}
