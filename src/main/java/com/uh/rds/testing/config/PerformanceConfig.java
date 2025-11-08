package com.uh.rds.testing.config;

import java.util.List;

public class PerformanceConfig {
    // 名字
    private String id;
    // 描述
    private String description;
    // 是否禁用
    private boolean disable = false;

    // 生成的数据文件, 相对于项目根目录
    private String dataFileName;

    // 连接信息的名称
    private String connectionName;

    private ConnectionConfig connection;

    private DataFileConfig dataFileConfig;

    // 是否在测试前先清空DB
    private boolean flushBefore;
    // 是否在测试后清理测试数据，根据key值逐条清理
    private boolean cleanAfter;
    // 是否从从节点读取数据,
    private boolean readFromSlave = false;

    // 测试时同时产生的客户端连接数
    private int clientsCount;

    // 每个客户端循环执行的次数, 如果是0表示无限循环；默认值为1
    private int loopCount;

    // 是否运行时解析命令行中的变量表达式，运行时解析会耗用较少的内存但会影响运行效率增大CPU使用，默认是false
    private boolean runtimeParse = false;

    // 逐个子项循环执行的命令，如果是String类型相当于子项只有一个，set, zset, list, hash这些类型都是集合内子项的循环。
    private List<CommandConfig> commands;

    // 表示按key逐一循环执行，并在commands定义的子项循环前执行。
    private List<CommandConfig> keyBeforeCommands;

    // 表示按key逐一循环执行，并在commands定义的子项循环后执行。
    private List<CommandConfig> keyAfterCommands;

    private long stateInterval = 1000; // 状态打印间隔，默认1秒


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int loopCount) {
        this.loopCount = loopCount;
    }


    public ConnectionConfig getConnection() {
        return connection;
    }

    public void setConnection(ConnectionConfig connection) {
        this.connection = connection;
    }

    public boolean isFlushBefore() {
        return flushBefore;
    }

    public void setFlushBefore(boolean flushBefore) {
        this.flushBefore = flushBefore;
    }

    public boolean isCleanAfter() {
        return cleanAfter;
    }

    public void setCleanAfter(boolean cleanAfter) {
        this.cleanAfter = cleanAfter;
    }

    public boolean isReadFromSlave() {
        return readFromSlave;
    }

    public void setReadFromSlave(boolean readFromSlave) {
        this.readFromSlave = readFromSlave;
    }

    public int getClientsCount() {
        return clientsCount;
    }

    public void setClientsCount(int clientsCount) {
        this.clientsCount = clientsCount;
    }

    public String getConnectionName() {
        return connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public boolean isRuntimeParse() {
        return runtimeParse;
    }

    public void setRuntimeParse(boolean runtimeParse) {
        this.runtimeParse = runtimeParse;
    }

    public List<CommandConfig> getCommands() {
        return commands;
    }

    public void setCommands(List<CommandConfig> commands) {
        this.commands = commands;
    }

    public List<CommandConfig> getKeyBeforeCommands() {
        return keyBeforeCommands;
    }

    public void setKeyBeforeCommands(List<CommandConfig> keyBeforeCommands) {
        this.keyBeforeCommands = keyBeforeCommands;
    }

    public List<CommandConfig> getKeyAfterCommands() {
        return keyAfterCommands;
    }

    public void setKeyAfterCommands(List<CommandConfig> keyAfterCommands) {
        this.keyAfterCommands = keyAfterCommands;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public void setDataFileName(String dataFileName) {
        this.dataFileName = dataFileName;
    }

    public DataFileConfig getDataFileConfig() {
        return dataFileConfig;
    }

    public void setDataFileConfig(DataFileConfig dataFileConfig) {
        this.dataFileConfig = dataFileConfig;
    }

    public long getStateInterval() {
        return stateInterval;
    }

    public void setStateInterval(long stateInterval) {
        this.stateInterval = stateInterval;
    }

    /**
     * 获取命令的总数量合计
     * @return
     */
    public int getCommandsTotal() {
        int keySize = dataFileConfig.getDataCount();
        int subSize = dataFileConfig.getSubDataCount();
        int commandSize = (commands != null) ? commands.size() : 0;
        int beforeSize = (keyBeforeCommands != null) ? keyBeforeCommands.size() : 0;
        int afterSize = (keyAfterCommands != null) ? keyAfterCommands.size() : 0;

        int keyCommands = (beforeSize + afterSize) * keySize;  //key 维度的命令数量
        int subCommands = commandSize * keySize * subSize; //子项维度的命令数量

        return keyCommands + subCommands;
    }

    public String getSummary() {
        return String.format("['%s']: desc='%s', " +
                        "clients=%d, loop=%d, dataFile='%s', connection='%s' commands=%d, keyBefore=%d, keyAfter=%d",
                id, description, clientsCount, loopCount, dataFileName, connectionName,
                (commands != null) ? commands.size() : 0,
                (keyBeforeCommands != null) ? keyBeforeCommands.size() : 0,
                (keyAfterCommands != null) ? keyAfterCommands.size() : 0
                );
    }

    @Override
    public String toString() {
        return "PerformanceConfig{\n" +
                "name='" + id + '\'' + '\n' +
                ", description='" + description + '\'' +'\n' +
                ", disable=" + disable +'\n' +
                ", readFromSlave=" + readFromSlave +'\n' +
                ", dataFileConfig='" + dataFileConfig + '\'' +'\n' +
                ", connection='" + connection + '\'' +'\n' +
                ", flushBefore=" + flushBefore +'\n' +
                ", cleanAfter=" + cleanAfter +'\n' +
                ", clientsCount=" + clientsCount +'\n' +
                ", loopCount=" + loopCount +'\n' +
                ", commands=" + commands +'\n' +
                ", keyBeforeCommands=" + keyBeforeCommands +'\n' +
                ", keyAfterCommands=" + keyAfterCommands +'\n' +
                ", stateInterval=" + stateInterval +'\n' +
                '}';
    }
}