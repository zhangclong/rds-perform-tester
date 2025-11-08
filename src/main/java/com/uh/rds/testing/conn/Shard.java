package com.uh.rds.testing.conn;

import redis.clients.jedis.util.JedisClusterCRC16;

import java.util.ArrayList;
import java.util.List;

public class Shard {

    int index;

    int beginSlot;

    int endSlot;

    String slot;

    Endpoint master;

    List<Endpoint> slaves = new ArrayList<>(2);

    public Shard(int index, String slot) {
        this.index = index;
        String[] slots = slot.trim().split("-");
        beginSlot = Integer.parseInt(slots[0]);
        endSlot = Integer.parseInt(slots[1]);
    }

    public Shard(int index, int beginSlot, int endSlot) {
        this.index = index;
        this.beginSlot = beginSlot;
        this.endSlot = endSlot;
        this.slot = beginSlot + "-" + endSlot;
    }

    public int getIndex() {
        return index;
    }

    public int getBeginSlot() {
        return beginSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }

    public String getSlot() {
        return slot;
    }

    public Endpoint getMaster() {
        return master;
    }

    public List<Endpoint> getSlaves() {
        return slaves;
    }

    public void setMaster(Endpoint master) {
        this.master = master;
    }

    public void addSlave(Endpoint slave) {
        slaves.add(slave);
    }

    /**
     * 判断key是否在当前分片中
     * @param key
     * @return
     */
    public boolean isKeyInShard(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        return (slot >= beginSlot) && (slot <= endSlot);
    }


    /**
     * 清空主从节点信息
     */
    public void cleanEndpoints() {
        if (master != null) {
            master = null;
        }

        if(!slaves.isEmpty()) {
            slaves.clear();
        }
    }

    @Override
    public String toString() {
        return "Shard{" +
                "index=" + index +
                ", beginSlot=" + beginSlot +
                ", endSlot=" + endSlot +
                "}\n";
    }
}
