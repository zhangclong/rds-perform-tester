package com.uh.rds.testing.config;

import com.uh.rds.testing.utils.AntPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class TestConfigManager {

    private final static Logger logger = LoggerFactory.getLogger(TestConfigManager.class);
    private static String configFile = "test-config.yml";

    private final static String configFolder = "conf/";

    private static TestsConfigRoot configRoot = null;


    private static TestsConfigRoot loadRoot() {
        if(configRoot == null) {
            //Load the main configuration file
            Yaml mainYaml = new Yaml(new Constructor(TestsConfigRoot.class, new LoaderOptions()));
            File mainConfigFile = new File(configFolder + configFile);
            try(InputStream in = mainConfigFile.toURI().toURL().openStream()) {
                configRoot = mainYaml.load(in);
                logger.info("Loaded the main config file: {}", mainConfigFile);
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to load the main config file: " + mainConfigFile, e);
            }

            //Load all test configurations under folder "conf/tests/"
            File testsFolder = new File(configFolder + configRoot.getTestsBase());
            if(testsFolder.exists() && testsFolder.isDirectory()) {
                File[] testFiles = testsFolder.listFiles((dir, name) -> AntPathUtils.matches(name, configRoot.getTestPatterns()));
                if(testFiles != null) {
                    for (File testFile : testFiles) {
                        configRoot.addTests(loadConfigs(testFile));
                    }
                }
            } else {
                logger.warn("Tests configuration folder does not exist or is not a directory: {}", testsFolder);
            }

        }

        return configRoot;
    }


    private static TestsConfig loadConfigs(File configFile) {
        if(configFile.exists() && configFile.isFile()) {
            TestsConfig config;
            //Load the main configuration file
            Yaml mainYaml = new Yaml(new Constructor(TestsConfig.class, new LoaderOptions()));
            try(InputStream in = configFile.toURI().toURL().openStream()) {
                config = mainYaml.load(in);
                logger.info("Loaded the config file: {}", configFile);
                return config;
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to load the main config file: " + configFile, e);
            }
        }
        else {
            throw new RuntimeException("Config file not found: " + configFile);
        }
    }

    /**
     * Set the configuration file name under folder "conf/".  "test-config.yml" is used by default.
     * This method must be called before any other method in this class.
     * @param fileName The configuration file name, e.g. "test-config.yml"
     */
    public static void setConfigFile(String fileName) {
        if(configRoot != null) {
            throw new IllegalStateException("Cannot change config file after it has been loaded.");
        }

        configFile = fileName;
    }

    public static List<TestConfig> getTestConfigs() {
        return loadRoot().getTests();
    }

    public static TestConfig getTestConfig(String id) {
        return loadRoot().getTestConfig(id);
    }

    public static ConnectionConfig getConnectionConfig(String name) {
        Map<String, ConnectionConfig> connections = loadRoot().getConnections();
        if(connections == null) {
            return null;
        }
        else {
            return connections.get(name);
        }
    }

    public static DataFileConfig getDataFileConfig(String name) {
        Map<String, DataFileConfig> dataFiles = loadRoot().getDataFiles();
        if(dataFiles == null) {
            return null;
        }
        else {
            return dataFiles.get(name);
        }
    }


}
