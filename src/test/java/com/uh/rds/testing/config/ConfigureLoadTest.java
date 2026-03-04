package com.uh.rds.testing.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.uh.rds.testing.conn.ConnectionMode;
import com.uh.rds.testing.conn.RdsConnectInfo;
import com.uh.rds.testing.utils.RdsConnectionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试配置加载功能：测试 TestConfigManager 对 perform-config.yml 的解析，
 * 以及连接配置、数据文件配置、测试用例配置的正确加载和校验。
 */
public class ConfigureLoadTest {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureLoadTest.class);

    private static List<PerformanceConfig> configList;

    @BeforeAll
    public static void setUp() {
        logger.info("ConfigureLoadTest setup: loading configurations");
        TestConfigManager.setConfigFile("perform-config.yml");
    }

    /**
     * 加载所有测试配置，并将 TestConfig 转换为 PerformanceConfig
     */
    public static List<PerformanceConfig> loadConfigs() {
        logger.info("Loading test configs from perform-config.yml");
        List<TestConfig> allConfigs = TestConfigManager.getTestConfigs();

        List<PerformanceConfig> configs = new ArrayList<>();
        for (TestConfig config : allConfigs) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            PerformanceConfig performConfig = mapper.convertValue(config.getConfigs(), PerformanceConfig.class);
            performConfig.setId(config.getId());
            performConfig.setDescription(config.getDescription());

            // 通过配置的连接名称获取连接信息
            String connectionName = performConfig.getConnectionName();
            if (connectionName != null && !connectionName.isEmpty()) {
                performConfig.setConnection(TestConfigManager.getConnectionConfig(connectionName.trim()));
            }

            // 通过配置的数据文件名称获取数据文件信息
            String dataFileName = performConfig.getDataFileName();
            if (dataFileName != null && !dataFileName.isEmpty()) {
                performConfig.setDataFileConfig(TestConfigManager.getDataFileConfig(dataFileName.trim()));
            }

            configs.add(performConfig);
        }

        // 按 id 排序
        configs.sort(Comparator.comparing(PerformanceConfig::getId));
        return configs;
    }

    // ---- 测试用例配置加载 -----------------------------------------------

    @Test
    public void testLoadConfigs() {
        configList = loadConfigs();

        assertNotNull(configList, "配置列表不应为空");
        assertFalse(configList.isEmpty(), "至少应该加载一个测试配置");

        logger.info("成功加载 {} 个测试配置", configList.size());
        for (PerformanceConfig config : configList) {
            logger.info("Loaded config: id={}, description={}", config.getId(), config.getDescription());
            assertNotNull(config.getId(), "配置 ID 不应为空");
            assertNotNull(config.getConnection(), "连接配置不应为空");
            assertNotNull(config.getDataFileConfig(), "数据文件配置不应为空");
        }
    }

    @Test
    public void testConfigsSortedById() {
        List<PerformanceConfig> configs = loadConfigs();

        // 验证配置按 ID 排序
        for (int i = 0; i < configs.size() - 1; i++) {
            String currentId = configs.get(i).getId();
            String nextId = configs.get(i + 1).getId();
            assertTrue(currentId.compareTo(nextId) <= 0,
                "配置应按 ID 排序: " + currentId + " 应该在 " + nextId + " 之前");
        }
    }

    // ---- 连接配置测试 -----------------------------------------------

    @Test
    public void testLoadLocalSingleConnection() {
        ConnectionConfig localSingle = TestConfigManager.getConnectionConfig("localSingle");

        assertNotNull(localSingle, "localSingle 连接配置不应为空");
        assertEquals("master", localSingle.getMode().toLowerCase(), "连接模式应为 master");
        assertNotNull(localSingle.getEndpoints(), "端点列表不应为空");
        assertTrue(localSingle.getEndpoints().length > 0, "至少应有一个端点");

        // 测试生成 RdsConnectInfo
        RdsConnectInfo connectInfo = localSingle.getConnectInfo();
        assertNotNull(connectInfo, "连接信息不应为空");
        assertEquals(ConnectionMode.MASTER, connectInfo.getMode(), "连接模式应为 MASTER");
        assertTrue(connectInfo.getEndpoints().size() > 0, "至少应有一个端点");

        logger.info("localSingle 配置: {}", localSingle);
        logger.info("localSingle 连接信息: {}", connectInfo);
    }

    @Test
    public void testLoadSentinelConnection() {
        ConnectionConfig sentinel = TestConfigManager.getConnectionConfig("sentinel");

        assertNotNull(sentinel, "sentinel 连接配置不应为空");
        assertEquals("sentinel", sentinel.getMode().toLowerCase(), "连接模式应为 sentinel");
        assertNotNull(sentinel.getMasterName(), "哨兵主节点名称不应为空");
        assertNotNull(sentinel.getEndpoints(), "端点列表不应为空");
        assertTrue(sentinel.getEndpoints().length > 0, "至少应有一个哨兵端点");

        // 测试生成 RdsConnectInfo
        RdsConnectInfo connectInfo = sentinel.getConnectInfo();
        assertNotNull(connectInfo, "连接信息不应为空");
        assertEquals(ConnectionMode.SENTINEL, connectInfo.getMode(), "连接模式应为 SENTINEL");

        // 测试获取哨兵管理的主从节点信息
        RdsConnectInfo masterSlaveConn = RdsConnectionUtils.getSentinelMasterSlave(sentinel, connectInfo);
        assertNotNull(masterSlaveConn, "主从连接信息不应为空");

        logger.info("sentinel 配置: {}", sentinel);
        logger.info("sentinel 连接信息: {}", connectInfo);
        logger.info("sentinel 主从连接: {}", masterSlaveConn);
    }

    @Test
    public void testLoadMasterSlaveConnection() {
        ConnectionConfig masterSlave = TestConfigManager.getConnectionConfig("sentinelWorker");

        assertNotNull(masterSlave, "sentinelWorker 连接配置不应为空");
        assertEquals("master_slave", masterSlave.getMode().toLowerCase(), "连接模式应为 master_slave");
        assertNotNull(masterSlave.getEndpoints(), "端点列表不应为空");

        // 测试生成 RdsConnectInfo
        RdsConnectInfo connectInfo = masterSlave.getConnectInfo();
        assertNotNull(connectInfo, "连接信息不应为空");
        assertEquals(ConnectionMode.MASTER_SLAVE, connectInfo.getMode(), "连接模式应为 MASTER_SLAVE");
        assertTrue(connectInfo.getEndpoints().size() > 0, "至少应有一个端点");

        logger.info("masterSlave 配置: {}", masterSlave);
        logger.info("masterSlave 连接信息: {}", connectInfo);
    }

    @Test
    public void testLoadClusterConnection() {
        ConnectionConfig cluster = TestConfigManager.getConnectionConfig("localCluster");

        assertNotNull(cluster, "localCluster 连接配置不应为空");
        assertEquals("cluster", cluster.getMode().toLowerCase(), "连接模式应为 cluster");
        assertNotNull(cluster.getShards(), "分片配置不应为空");
        assertNotNull(cluster.getEndpoints(), "端点列表不应为空");
        assertEquals(cluster.getShards().length, cluster.getEndpoints().length,
            "分片数量应该等于端点组数量");

        // 测试生成 RdsConnectInfo
        RdsConnectInfo connectInfo = cluster.getConnectInfo();
        assertNotNull(connectInfo, "连接信息不应为空");
        assertEquals(ConnectionMode.CLUSTER, connectInfo.getMode(), "连接模式应为 CLUSTER");
        assertEquals(cluster.getShards().length, connectInfo.getShards().size(),
            "分片数量应该匹配");
        assertTrue(connectInfo.getEndpoints().size() > 0, "至少应有一个端点");

        logger.info("cluster 配置: {}", cluster);
        logger.info("cluster 连接信息: {}", connectInfo);
    }

    // ---- 数据文件配置测试 -----------------------------------------------

    @Test
    public void testLoadStringDataFile() {
        DataFileConfig stringData = TestConfigManager.getDataFileConfig("string-data");

        assertNotNull(stringData, "string-data 配置不应为空");
        assertEquals("string", stringData.getDataType(), "数据类型应为 string");
        assertTrue(stringData.getDataCount() > 0, "数据行数应大于 0");
        assertTrue(stringData.getValueLength() >= 0, "值长度应大于等于 0");
        assertNotNull(stringData.getDataFile(), "数据文件路径不应为空");
        assertNotNull(stringData.getKeyGenMode(), "Key 生成模式不应为空");

        // 验证配置有效性
        assertDoesNotThrow(() -> stringData.validate(), "string-data 配置应该有效");

        logger.info("string-data 配置: {}", stringData);
    }

    @Test
    public void testLoadHashDataFile() {
        DataFileConfig hashData = TestConfigManager.getDataFileConfig("hash-data");

        assertNotNull(hashData, "hash-data 配置不应为空");
        assertEquals("hash", hashData.getDataType(), "数据类型应为 hash");
        assertTrue(hashData.getDataCount() > 0, "数据行数应大于 0");
        assertTrue(hashData.getSubDataCount() > 0, "集合类型子数据数量应大于 0");
        assertTrue(hashData.getValueLength() >= 0, "值长度应大于等于 0");
        assertNotNull(hashData.getDataFile(), "数据文件路径不应为空");

        // 验证配置有效性
        assertDoesNotThrow(() -> hashData.validate(), "hash-data 配置应该有效");

        logger.info("hash-data 配置: {}", hashData);
    }

    // ---- 性能配置合并测试 -----------------------------------------------

    @Test
    public void testPerformanceConfigMerge() {
        List<PerformanceConfig> configs = loadConfigs();

        // 验证每个配置都正确合并了连接和数据文件信息
        for (PerformanceConfig config : configs) {
            assertNotNull(config.getConnection(),
                "配置 " + config.getId() + " 的连接信息应正确加载");
            assertNotNull(config.getDataFileConfig(),
                "配置 " + config.getId() + " 的数据文件配置应正确加载");

            // 验证连接名称匹配
            if (config.getConnectionName() != null) {
                ConnectionConfig conn = TestConfigManager.getConnectionConfig(config.getConnectionName());
                assertEquals(conn, config.getConnection(),
                    "连接配置应该匹配 connectionName 引用");
            }

            // 验证数据文件名称匹配
            if (config.getDataFileName() != null) {
                DataFileConfig dataFile = TestConfigManager.getDataFileConfig(config.getDataFileName());
                assertEquals(dataFile, config.getDataFileConfig(),
                    "数据文件配置应该匹配 dataFileName 引用");
            }
        }
    }

    // ---- 配置验证测试 -----------------------------------------------

    @Test
    public void testConnectionConfigValidation() {
        ConnectionConfig localSingle = TestConfigManager.getConnectionConfig("localSingle");
        assertDoesNotThrow(() -> localSingle.validate(),
            "localSingle 连接配置应该有效");

        ConnectionConfig sentinel = TestConfigManager.getConnectionConfig("sentinel");
        assertDoesNotThrow(() -> sentinel.validate(),
            "sentinel 连接配置应该有效");

        ConnectionConfig cluster = TestConfigManager.getConnectionConfig("localCluster");
        assertDoesNotThrow(() -> cluster.validate(),
            "cluster 连接配置应该有效");
    }

    @Test
    public void testPerformanceConfigValidation() {
        List<PerformanceConfig> configs = loadConfigs();

        for (PerformanceConfig config : configs) {
            if (!config.isDisable()) {
                assertDoesNotThrow(() -> config.validate(),
                    "配置 " + config.getId() + " 应该有效");

                // 验证基本属性
                assertTrue(config.getThreads() > 0,
                    "配置 " + config.getId() + " 的线程数应大于 0");
                assertTrue(config.getThreadClients() > 0,
                    "配置 " + config.getId() + " 的每线程客户端数应大于 0");
                assertTrue(config.getLoopCount() >= 0,
                    "配置 " + config.getId() + " 的循环次数应大于等于 0");
                assertNotNull(config.getCommands(),
                    "配置 " + config.getId() + " 的命令列表不应为空");
            }
        }
    }

    // ---- 命令配置测试 -----------------------------------------------

    @Test
    public void testCommandConfiguration() {
        List<PerformanceConfig> configs = loadConfigs();

        for (PerformanceConfig config : configs) {
            if (config.getCommands() != null && !config.getCommands().isEmpty()) {
                for (CommandConfig cmd : config.getCommands()) {
                    assertNotNull(cmd.getLine(),
                        "配置 " + config.getId() + " 的命令行不应为空");

                    logger.info("配置 {}, 命令: {}, 返回断言: {}, 表达式断言: {}",
                        config.getId(), cmd.getLine(), cmd.getReturnAssert(), cmd.getReturnAssertEvl());
                }
            }
        }
    }
}
