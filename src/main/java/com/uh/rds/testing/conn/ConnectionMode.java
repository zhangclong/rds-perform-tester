package com.uh.rds.testing.conn;

public enum ConnectionMode {
    MASTER, //单节点，代理模式，可伸缩模式都属于单节点模式。
    SENTINEL,  //哨兵节点的连接
    MASTER_SLAVE, //主从
    CLUSTER; //集群

    // 通过字符串获取枚举
    public static ConnectionMode from(String mode) {
        if ("master".equalsIgnoreCase(mode)) return MASTER;
        if ("sentinel".equalsIgnoreCase(mode)) return SENTINEL;
        if ("master_slave".equalsIgnoreCase(mode)) return MASTER_SLAVE;
        if ("cluster".equalsIgnoreCase(mode)) return CLUSTER;
        throw new IllegalArgumentException("Unknown deploy mode: " + mode);
    }

}
