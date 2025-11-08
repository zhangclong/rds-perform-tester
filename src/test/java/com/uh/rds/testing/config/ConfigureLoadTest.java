package com.uh.rds.testing.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.uh.rds.testing.conn.RdsConnectInfo;
import com.uh.rds.testing.performance.DataFileManager;
import com.uh.rds.testing.performance.PerformanceTestRunner;
import com.uh.rds.testing.utils.RdsConnectionUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class ConfigureLoadTest {

    private static final Logger logger = LoggerFactory.getLogger(ConfigureLoadTest.class);

    private static List<PerformanceConfig> configList;


    public static void loadConfigs() {
        logger.info("PerformanceMainTest init ...");
        TestConfigManager.setConfigFile("perform-config.yml");
        List<TestConfig> allConfigs = TestConfigManager.getTestConfigs();

        configList = new ArrayList<>();
        for (TestConfig config : allConfigs) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            PerformanceConfig performConfig = mapper.convertValue(config.getConfigs(), PerformanceConfig.class);
            performConfig.setId(config.getId());
            performConfig.setDescription(config.getDescription());
            // 通过配置的连接名称获取连接信息
            String connectionName = performConfig.getConnectionName();
            if(connectionName != null && !connectionName.isEmpty()) {
                performConfig.setConnection(TestConfigManager.getConnectionConfig(connectionName.trim()));
            }

            // 通过配置的数据文件名称获取数据文件信息
            String dataFileName = performConfig.getDataFileName();
            if(dataFileName != null && !dataFileName.isEmpty()) {
                performConfig.setDataFileConfig(TestConfigManager.getDataFileConfig(dataFileName.trim()));
            }

            configList.add(performConfig);
        }

        // configList 按 id 排序
        configList.sort(Comparator.comparing(PerformanceConfig::getId));

    }

    @Test
    public void testLoadConfigs() {
        loadConfigs();

        //getConnectionConfig from：localSingle, sentinelWorker, sentinel, localCluster, localClusterPool
//        ConnectionConfig localSingle = TestConfigManager.getConnectionConfig("localSingle");
//        System.out.println(localSingle);
//        System.out.println(localSingle.getConnectInfo() + "\n\n");
//
//        ConnectionConfig sentinelWorker = TestConfigManager.getConnectionConfig("sentinelWorker");
//        System.out.println(sentinelWorker);
//        System.out.println(sentinelWorker.getConnectInfo() + "\n\n");

        ConnectionConfig sentinel = TestConfigManager.getConnectionConfig("sentinel");
        System.out.println(sentinel);
        System.out.println(sentinel.getConnectInfo() + "\n\n");
        RdsConnectInfo masterSlaveConn = RdsConnectionUtils.getSentinelMasterSlave(sentinel, sentinel.getConnectInfo());
        System.out.println("masterSlaveConn: " + masterSlaveConn + "\n\n");

//        ConnectionConfig localCluster = TestConfigManager.getConnectionConfig("localCluster");
//        System.out.println(localCluster);
//        System.out.println(localCluster.getConnectInfo() + "\n\n");
//
//        ConnectionConfig localClusterPool = TestConfigManager.getConnectionConfig("localClusterPool");
//        System.out.println(localClusterPool);
//        System.out.println(localClusterPool.getConnectInfo() + "\n\n");






//        if(configList != null) {
//            for(PerformanceConfig config : configList) {
//                logger.info("Loaded Performance Config: id={}, description={}", config.getId(), config.getDescription());
//            }
//        } else {
//            logger.warn("No performance configurations loaded.");
//        }
    }


}
