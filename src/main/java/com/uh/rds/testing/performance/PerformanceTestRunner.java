package com.uh.rds.testing.performance;

import com.uh.rds.testing.config.CommandConfig;
import com.uh.rds.testing.config.ConnectionConfig;
import com.uh.rds.testing.config.PerformanceConfig;
import com.uh.rds.testing.conn.ConnectionMode;
import com.uh.rds.testing.conn.Endpoint;
import com.uh.rds.testing.conn.RdsConnectInfo;
import com.uh.rds.testing.conn.Shard;
import com.uh.rds.testing.utils.RdsConnectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static com.uh.rds.testing.TestingConstants.STATE_BEGIN_WAIT;
import static com.uh.rds.testing.data.TestDataLoader.readSubsetData;
import static com.uh.rds.testing.data.TestDataLoader.splitMapList;

/**
 * 多线程并发的数据验证运行器
 */
public class PerformanceTestRunner {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestRunner.class);

    private List<PerformanceThread> threads;

    private PerformanceConfig config;

    private RdsConnectInfo testingConnection;

    private boolean ready = false;

    private List<Thread> startedThreads; //启动中的线程

    private Thread stateThread; //状态统计线程

    private CyclicBarrier startBarrier;

    private static final Logger state_logger = LoggerFactory.getLogger("state-info");

    public PerformanceTestRunner() {}

    public PerformanceTestRunner(PerformanceConfig config) {
        this.config = config;

        ConnectionConfig connConfig = config.getConnection();
        RdsConnectInfo conn = connConfig.getConnectInfo();
        if(conn.getMode() == ConnectionMode.SENTINEL) { //哨兵模式，连接哨兵并获取主从节点信息
            this.testingConnection = RdsConnectionUtils.getSentinelMasterSlave(connConfig, conn);
        }
        else {
            this.testingConnection = conn;
        }

        this.startBarrier = new CyclicBarrier(config.getClientsCount() + 1);
    }

    /**
     * 准备测试线程和数据
     */
    public void prepareThreadsData() {
        this.ready = false;
        this.threads = new ArrayList<>();
        ConnectionMode mode = testingConnection.getMode();
        if(mode == ConnectionMode.CLUSTER) {
            prepareClusterThreadsData(testingConnection);
        }
        else if(mode == ConnectionMode.MASTER || mode == ConnectionMode.MASTER_SLAVE) {
            //注意 SENTINEL模式已经在构造函数中转换为MASTER_SLAVE了
            prepareSingleThreadsData(testingConnection);
        }
        else {
            throw new RuntimeException("Not support deploy mode:" + mode);
        }

        this.ready = true;
    }



    private void prepareSingleThreadsData(RdsConnectInfo connection) {
        // 读取数据文件
        File dataFile = new File(config.getDataFileConfig().getDataFile());
        logger.info("[{}] Loading data from file: {}" , config.getId(), dataFile.getAbsolutePath());

        Map<String, List<String[]>> data = readSubsetData(dataFile);
        logger.info("[{}] total {} rows Data loaded! ", config.getId(), data.size());

        List<Map<String, List<String[]>>> threadsData = splitMapList(data, config.getClientsCount());

        data.clear();
        System.gc();


        // 获得连接地址
        List<Endpoint> masters = connection.getMasterEndpoints();
        List<Endpoint> slaves = connection.getSlaveEndpoints();

        System.out.print(config.getId() + " preparing data for threads[" + config.getClientsCount() + "] ");
        for(int i = 0 ; i < config.getClientsCount() ; i++) {
            Endpoint endpoint = masters.get(i % masters.size());
            Endpoint slaveEndpoint = slaves.isEmpty() ? null : slaves.get(i % slaves.size());
            Map<String, List<String[]>> threadData = threadsData.get(i);

            PerformanceThread thread = new PerformanceThread(i, endpoint, slaveEndpoint, config, startBarrier);

            buildThread(thread, threadData);
            threads.add(thread);
            System.out.print("."); // 进度标记，每处理完一个thread打印一个点
        }
        System.out.println(" done!\n");
    }


    private void prepareClusterThreadsData(RdsConnectInfo connection) {

        File dataFile = new File(config.getDataFileConfig().getDataFile());
        Map<String, List<String[]>> data = readSubsetData(dataFile);
        int dataCount = data.size();
        logger.info("[{}] total {} rows Data loaded!", config.getId(), dataCount);

        int threadSize = config.getClientsCount();
        List<Shard> shards = connection.getShards();
        int shardCount = shards.size();

        // 创建shards数量的thread列表
        List<List<PerformanceThread>> shardsThreads = new ArrayList<>(shardCount);
        for (int i = 0; i < shardCount; i++) {  shardsThreads.add(new ArrayList<>()); }

        // 1. 创建Thread列表, 放入: threads(所有线程)中,  和shardsThreads(按shardIndex拆分的线程子集)中。
        int subThreadSize = threadSize / shardCount; //每个分片平均分配几个Thread
        int shardIdx = 0;
        logger.info("Shard size:{}, thread size:{}", shardCount, threadSize);
        // 循环创建指定数量的thread，并把它们分配到各个shard下（List<List<DataValidatorThread>> shardsThreads）
        for(int i = 0 ; i < threadSize ; i++) {
            if( i!=0 && (i % subThreadSize == 0) && (shardIdx<shardCount-1)) {
                shardIdx++;
            }

            Shard shardInfo = shards.get(shardIdx);
            Endpoint masterJedis = shardInfo.getMaster();
            Endpoint slaveJedis =  shardInfo.getSlaves().isEmpty() ? null : shardInfo.getSlaves().get(i % shardInfo.getSlaves().size());
            PerformanceThread thread = new PerformanceThread(i, shardIdx, masterJedis, slaveJedis, config, startBarrier);
            threads.add(thread);
            List<PerformanceThread> shardThreads = shardsThreads.get(shardIdx);
            shardThreads.add(thread);
        }

        // 2. 把每种类型的数据从磁盘中读取，分配到不同的shard 》再分配到其下的threads中。
        List<Map<String, List<String[]>>> shardsData = new ArrayList<>(shardCount);
        int perShardDataCount = (int) (((float)dataCount / shardCount) * 1.05f); //预估每个分片的数据量，乘以1.05是为了避免数据分配不均衡导致某个分片数据过多
        for(int i=0 ; i<shardCount ; i++) { shardsData.add(new HashMap<String, List<String[]>>(perShardDataCount)); }

        //把数据分配到不同的shard中
        for(Map.Entry<String, List<String[]>> entry : data.entrySet()) {
            String key = entry.getKey();
            List<String[]> value = entry.getValue();
            for(int i=0 ; i<shardCount ; i++) {
                Shard shard = shards.get(i);
                if(shard.isKeyInShard(key)) {
                    shardsData.get(i).put(key, value);
                    break;
                }
            }
        }

        data.clear();
        System.gc();
        // 打印每个分片中的数据量
        for(int i=0 ; i<shardCount ; i++) {
            Shard shardInfo = shards.get(i);
            int dataSize = shardsData.get(i).size();
            logger.info("[{}] Shard{}[slot:{}-{}] data size:{}", config.getId(), shardInfo.getIndex(),
                    shardInfo.getBeginSlot(), shardInfo.getEndSlot(), dataSize);
        }

        System.out.print(config.getId() + " preparing data for threads[" + config.getClientsCount() + "] ");

        //再对每个分片中的数据平均分配到其下的threads中
        for(int i=0 ; i<shardCount ; i++) {
            Shard shardInfo = shards.get(i);
            Map<String, List<String[]>> shardData = shardsData.get(i);
            List<PerformanceThread> subThreads = shardsThreads.get(i);
            List<Map<String, List<String[]>>> subThreadsData = splitMapList(shardData, subThreads.size());

            // 为每一个thread设置数据。
            for (int j = 0; j < subThreads.size(); j++) {
                buildThread(subThreads.get(j), subThreadsData.get(j));
                System.out.print("."); // 进度标记，每处理完一个thread打印一个点
            }

            shardData.clear();
            subThreadsData.clear();
            System.gc();
        }

        System.out.print(" done!\n");
    }


    /**
     * 构建单个测试线程
     * @param thread 测试线程
     * @param threadData  线程数据
     */
    private void buildThread(PerformanceThread thread, Map<String, List<String[]>> threadData) {
        if(config.isRuntimeParse()) {
            thread.initData(threadData);
        }
        else {
            CommandHelper commandHelper = new CommandHelper();
            List<CommandConfig> commandConfigs = config.getCommands();
            List<CommandConfig> beforeCommands = config.getKeyBeforeCommands();
            List<CommandConfig> afterCommands = config.getKeyAfterCommands();

            List<String> keys = new ArrayList<>(threadData.keySet());

            List<CommandTarget> commandTargets = new ArrayList<>(config.getCommandsTotal());

            for (Map.Entry<String, List<String[]>> entry : threadData.entrySet()) {
                String key = entry.getKey();
                List<String[]> valuesList = entry.getValue();

                if (beforeCommands != null) {
                    for (CommandConfig commandConfig : beforeCommands) {
                        CommandTarget target = commandHelper.toCommandTarget(commandConfig, key, null);
                        commandTargets.add(target);
                    }
                }

                for (String[] values : valuesList) {
                    for (CommandConfig commandConfig : commandConfigs) {
                        CommandTarget target = commandHelper.toCommandTarget(commandConfig, key, values);
                        commandTargets.add(target);
                    }
                }

                if (afterCommands != null) {
                    for (CommandConfig commandConfig : afterCommands) {
                        CommandTarget target = commandHelper.toCommandTarget(commandConfig, key, null);
                        commandTargets.add(target);
                    }
                }
            }

            threadData.clear(); // 清理数据，释放内存
            thread.initData(keys, commandTargets);
        }
    }



    /**
     * 是否准备好
     * @return
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * 启动测试线程
     */
    private void startTests() {
        if(!isReady()) {
            throw new RuntimeException("Not ready! Please call prepareThreadsData(connection) first!");
        }

        if(startedThreads != null && !startedThreads.isEmpty()) {
            throw new RuntimeException("Threads already started, please call runTest() instead of startTest()!");
        }

        if(stateThread != null && stateThread.isAlive()) {
            throw new RuntimeException("State thread is still alive!");
        }

        startedThreads = new ArrayList<>();
        //启动所有线程
        for(PerformanceThread t : threads) {
            Thread thread = new Thread(t, t.getName());
            startedThreads.add(thread);
            thread.start();
        }
    }

    /**
     * 启动统计线程
     */
    private void startState() {
        stateThread = new Thread(()->{

            try {
                Thread.sleep(STATE_BEGIN_WAIT); //等待一段时间，避免刚开始时线程都没有来得及执行
                startBarrier.await(); //等待所有线程启动
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }


            state_logger.info("[{}] testing start...", config.getId());

            long averageOpsSum = 0;
            long averageTimes = 0;

            long ops = 0; // 当前每秒操作数
            int threadsSize = startedThreads.size();

            long stateInterval = config.getStateInterval();
            boolean firstAvailbleLoop = true;
            boolean running = true;
            while (running) {
                long begin = System.currentTimeMillis();

                try {
                    Thread.sleep(stateInterval);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                running = false;
                int op = 0;
                int runningThreads = 0;
                for (int i = 0; i < threadsSize; i++) {
                    PerformanceThread pt = threads.get(i);
                    int oneOp = pt.resetOp();
                    if (oneOp > 0) {
                        op += oneOp;
                        running = true; //只要有一个线程在运行，就继续
                        runningThreads ++;
                    }
                }

                if(threadsSize == runningThreads && !firstAvailbleLoop) {
                    long duration = (System.currentTimeMillis() - begin);
                    if(ops > 0) {
                        // 只有在所有线程都在运行时，才计算平均值
                        // 去掉第一条，和最后一条记录
                        averageOpsSum += ops;
                        averageTimes++;
                    }

                    ops = (long) (op * 1000.0 / duration);
                    state_logger.info("[{}] -- process {} times in {}ms({}/s), running threads:{}/{}", config.getId(), op, duration, ops, runningThreads, threadsSize);
                }

                if (firstAvailbleLoop) {
                    firstAvailbleLoop = false;
                }
            }

            // 去掉开头和结果的 OPS 平均值
            if(averageTimes > 0) {
                state_logger.info("[{}] testing end, Steady-State OPS average {}/s\n", config.getId(), averageOpsSum / averageTimes);
            }
            else {
                state_logger.info("[{}] testing end, No enough data to calculate Steady-State OPS average.\n", config.getId());
            }


            // 计算并打印出其他统计值
            long maxStartTime = 0, minStartTime = 0; // 获得最大和最小的开始时间
            long maxDuration = 0, minDuration = 0; // 最大和最小的持续时间
            int totalOp = 0;// 总操作数
            int totalAssertFailed = 0; // 总断言失败数
            for (int i = 0; i < threadsSize; i++) {
                PerformanceThread pt = threads.get(i);

                // 统计最大和最小的开始时间； 最大和最小的持续时间
                long startTime = pt.getStartTime();
                long duration = pt.getTime();
                if (i == 0) {
                    maxStartTime = startTime;
                    minStartTime = startTime;
                    maxDuration = duration;
                    minDuration = duration;
                } else {
                    if (startTime > maxStartTime) {
                        maxStartTime = startTime;
                    }
                    if (startTime < minStartTime) {
                        minStartTime = startTime;
                    }

                    if(duration > maxDuration) {
                        maxDuration = duration;
                    }
                    if(duration < minDuration) {
                        minDuration = duration;
                    }
                }

                //统计操作数和断言失败数
                totalOp += pt.getOpSum();
                totalAssertFailed += pt.getAssertFailed();
            }
            // 格式 09:26:06.722 把 minStartTime 转换为可读的时间格式
            String readableStartTime = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new Date(minStartTime));
            String readableMaxStartTime = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new Date(maxStartTime));

            // 打印时间统计：
            state_logger.info("[{}] 线程数：{} \n" +
                    "   启动时间：[{} ~ {}] \n" +
                    "   持续时间：[{}ms ~ {}ms] \n" +
                    "   操作次数(每线程 | 总数)：[{} | {}}；\n" +
                    "   断言失败次数(每线程 | 总数)：[{} | {}]",
                    config.getId(), threadsSize,
                    readableStartTime, readableMaxStartTime,
                    minDuration, maxDuration,
                    totalOp/threadsSize, totalOp,
                    totalAssertFailed/threadsSize, totalAssertFailed);
        });

        stateThread.start();
    }




    /**
     * 运行测试
     * @return 测试结果
     */
    public void runTest() {

        if(config.isFlushBefore()) {
            // 运行前清理数据
            logger.info("[{}]Flush all data before testing...", config.getId());
            RdsConnectionUtils.flushDb(testingConnection);
            logger.info("[{}]Flush all data done.", config.getId());
        }

        startBarrier.reset();
        startTests();
        startState();

        try {
            stateThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

}
