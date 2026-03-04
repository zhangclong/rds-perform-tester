package com.uh.rds.testing.performance;

import com.uh.rds.testing.config.CommandConfig;
import com.uh.rds.testing.config.PerformanceConfig;
import com.uh.rds.testing.conn.Endpoint;
import com.uh.rds.testing.logger.TestLoggerFactory;
import com.uh.rds.testing.utils.Pxx;
import com.uh.rds.testing.utils.RdsConnectionUtils;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import org.slf4j.Logger;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static com.uh.rds.testing.performance.CommandHelper.toCommandLine;

public class PerformanceThread implements Runnable {
    private Logger logger;
    private Logger commandLogger;

    /** 共享的 Aviator 实例（线程安全），用于 returnAssertEvl 表达式求值 */
    private static final AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.getInstance();

    private final PerformanceConfig config;
    private final Pxx pxx;
    private Endpoint endpoint; //做操作时的连接地址
    private Endpoint slaveEndpoint;//做验证时的连接地址
    private List<CommandTarget> commandTargets;
    private List<String> keys; //所有的Key列表, 用于清除时使用

    // 如果  config.isRuntimeParse() == true 则使用下面的threadData进行运行时参数解析，此时commandTargets将不使用值为null
    private Map<String, List<String[]>> threadData;

    private final CyclicBarrier startBarrier;

    private final String name;

    private final AtomicInteger op = new AtomicInteger(0);  //操作次数
    private int opSum = 0;                                      //总操作次数
    private long startTime = 0;                                //开始时间，单位毫秒
    private long time = 0;                                      //总耗时，单位毫秒
    private int assertFailed = 0;                               //断言失败次数

    private int jedisIndex = 0;

    private final Jedis[] writeJedisArray;
    private final Jedis[] readJedisArray;

    public PerformanceThread(int index, Endpoint endpoint, Endpoint slaveEndpoint, PerformanceTestRunner runner, CyclicBarrier startBarrier) {
        this(index, -1, endpoint, slaveEndpoint, runner, startBarrier);
    }

    public PerformanceThread(int index, int shard, Endpoint endpoint, Endpoint slaveEndpoint, PerformanceTestRunner runner, CyclicBarrier startBarrier) {
        this.config = runner.config;
        this.pxx = runner.pxx;

        if(shard >= 0) {
            this.name = "[" + config.getId() + ", Shard" + shard + ", Thread" + index + "]";
        }
        else {
            this.name = "[" + config.getId() + ", Thread" + index + "]";
        }

        this.logger = TestLoggerFactory.getLogger(name);
        this.commandLogger = TestLoggerFactory.getLogger("commands." + name, 100);
        this.endpoint = endpoint;
        this.slaveEndpoint = slaveEndpoint;
        this.startBarrier = startBarrier;
        this.writeJedisArray = new Jedis[config.getThreadClients()];
        this.readJedisArray = new Jedis[config.getThreadClients()];
    }

    public void initData(Map<String, List<String[]>> threadData) {
        this.keys = new ArrayList<>(threadData.keySet());
        this.threadData = threadData;
    }

    public void initData(List<String> keys, List<CommandTarget> commandTargets) {
        this.keys = keys;
        this.commandTargets = commandTargets;
    }

    public String getName() {
        return name;
    }

    /**
     * 重置操作次数为0，并返回重置前的操作次数
     * @return 重置前的操作次数
     */
    public synchronized int resetOp() {
        int old =  op.getAndSet(0);
        if(old > 0) opSum += old;
        return old;
    }


    public synchronized int getOpSum() {
        if(op.get() > 0) {
            return opSum + op.get();
        }
        else {
            return opSum;
        }
    }

    /**
     * 获取总耗时，单位毫秒。总耗时是循环执行所有命令的总时间之和，不包括线程准备的时间和清除数据的时间。
     * @return 总耗时，单位毫秒
     */
    public synchronized long getTime() {
        return time;
    }

    public synchronized int getAssertFailed() {
        return assertFailed;
    }

    public synchronized long getStartTime() {
        return startTime;
    }


    @Override
    public void run() {
        //Jedis writeJedis = null;
        //Jedis readJedis = null;
        long endTime = 0;
        startTime = 0;
        try {
            // 建立连接
            for(int i=0; i<config.getThreadClients(); i++) {
                writeJedisArray[i] = endpoint.newJedis();
                if (config.isReadFromSlave()) {
                    if (slaveEndpoint == null) {
                        throw new RuntimeException("Slave endpoint is null!");
                    }
                    readJedisArray[i] = slaveEndpoint.newJedis();
                } else {
                    readJedisArray[i] = writeJedisArray[i];
                }
            }

            // 等待所有线程准备就绪
            startBarrier.await();
            //logger.info("{} starts to run testing!", name);

            int loopCount = config.getLoopCount();

            startTime = System.currentTimeMillis();
            for(int i=0; i<loopCount ; i++) {
                if(config.isRuntimeParse()) {
                    CommandHelper commandHelper = new CommandHelper();
                    List<CommandConfig> commandConfigs = config.getCommands();
                    List<CommandConfig> beforeCommands = config.getKeyBeforeCommands();
                    List<CommandConfig> afterCommands = config.getKeyAfterCommands();

                    for (Map.Entry<String, List<String[]>> entry : threadData.entrySet()) {
                        String key = entry.getKey();
                        List<String[]> valuesList = entry.getValue();

                        if (beforeCommands != null) {
                            for (CommandConfig commandConfig : beforeCommands) {
                                CommandTarget target = commandHelper.toCommandTarget(commandConfig, key, null);
                                runCommand(target);
                            }
                        }

                        for (String[] values : valuesList) {
                            for (CommandConfig commandConfig : commandConfigs) {
                                CommandTarget target = commandHelper.toCommandTarget(commandConfig, key, values);
                                runCommand(target);
                            }
                        }

                        if (afterCommands != null) {
                            for (CommandConfig commandConfig : afterCommands) {
                                CommandTarget target = commandHelper.toCommandTarget(commandConfig, key, null);
                                runCommand(target);
                            }
                        }
                    }


                }
                else {
                    for (CommandTarget target : commandTargets) {
                        runCommand(target);
                    }
                }
            }

            endTime = System.currentTimeMillis();
        }
        catch (Exception e) {
            endTime = System.currentTimeMillis();
            logger.error("stopped by error!", e);
        }
        finally {
            if(endTime == 0) { endTime = System.currentTimeMillis(); }
            time += (endTime - startTime);
            // 清除数据
            if(config.isCleanAfter()) {
                for(String key : keys) {
                    try {
                        getNextJedis(true).del(key);
                    }
                    catch (Exception e) {
                        // 发生异常后，不再继续删除其它Key
                        logger.error("Delete key {} is error!", key, e);
                        break;
                    }
                }
            }

            // 关闭所有连接
            for(int i=0; i<config.getThreadClients(); i++) {
                if(writeJedisArray[i] != null && writeJedisArray[i].isConnected()) {
                    try {
                        writeJedisArray[i].close();
                    }
                    catch (Throwable e) {
                        logger.warn("Close writeJedis error!", e);
                    }
                }
                if(readJedisArray[i] != null && readJedisArray[i] != writeJedisArray[i] && readJedisArray[i].isConnected()) {
                    try {
                        readJedisArray[i].close();
                    }
                    catch (Throwable e) {
                        logger.warn("Close readJedis error!", e);
                    }
                }
            }
        }
    }

    private Jedis getNextJedis(boolean isWrite) {
        Jedis[] jedisArray = isWrite ? writeJedisArray : readJedisArray;
        Jedis jedis = jedisArray[jedisIndex];
        jedisIndex = (jedisIndex + 1) % jedisArray.length;
        return jedis;
    }


    private void runCommand(CommandTarget target) {
        if(target.sleepMillis > 0) {
            try {
                Thread.sleep(target.sleepMillis);
            }
            catch (InterruptedException ie) {// ignore
            }
        }
        else {
            Jedis execJedis = getNextJedis(target.isWrite);
            for (int j = 0; j < target.repeatTimes; j++) {

                long before = System.nanoTime();
                Object returnObj = RdsConnectionUtils.sendCommand(execJedis, target.commandArgs);
                long duration = (System.nanoTime() - before) / 1000; //微秒
                pxx.set(duration);

                if(target.compareMethod == CommandHelper.COMPARE_NOT_EMPTY) {
                    if(returnObj == null) {
                        assertFailed ++;
                        commandLogger.error("Command:{}, return null, not match NOT_EMPTY", toCommandLine(target.commandArgs));
                    }
                }
                else if(target.compareMethod == CommandHelper.COMPARE_EMPTY) {
                    if(target.returnType == CommandHelper.RETURN_TYPE_STRING) {
                        String returnValue = getReturnValueAsString(returnObj);
                        if(returnValue != null && !returnValue.isEmpty()) {
                            assertFailed ++;
                            commandLogger.error("Command:{}, return '{}', not match EMPTY", toCommandLine(target.commandArgs), returnValue);
                        }
                    }
                    else if(target.returnType == CommandHelper.RETURN_TYPE_LONG) {
                        Long returnValue = BuilderFactory.LONG.build(returnObj);
                        if(returnValue != null && returnValue != 0L) {
                            assertFailed ++;
                            commandLogger.error("Command:{}, return {}, not match EMPTY", toCommandLine(target.commandArgs), returnValue);
                        }
                    }
                    else {
                        assertFailed ++;
                        commandLogger.error("Command:{}, Unknown return type: {}", toCommandLine(target.commandArgs), target.returnType);
                    }
                }
                else if(target.compareMethod == CommandHelper.COMPARE_EQ) {
                    if(target.returnType == CommandHelper.RETURN_TYPE_STRING) {
                        String returnValue = getReturnValueAsString(returnObj);
                        if(!target.compareValue.equals(returnValue)) {
                            assertFailed ++;
                            commandLogger.error("Command:{}, return '{}', not match EQ '{}'", toCommandLine(target.commandArgs), returnValue, target.compareValue);
                        }
                    }
                    else if(target.returnType == CommandHelper.RETURN_TYPE_LONG) {
                        Long returnValue = BuilderFactory.LONG.build(returnObj);
                        if(!target.compareValue.equals(returnValue)) {
                            assertFailed ++;
                            commandLogger.error("Command:{}, return {}, not match EQ  {}", toCommandLine(target.commandArgs), returnValue, target.compareValue);
                        }
                    }
                    else {
                        assertFailed ++;
                        commandLogger.error("Command:{}, Unknown return type: {}", toCommandLine(target.commandArgs), target.returnType);
                    }
                }
                else if(target.compareMethod == CommandHelper.COMPARE_EVL) {
                    evaluateAviatorAssert(target, returnObj);
                }
            }

            op.addAndGet(target.repeatTimes);
        }
    }

    /**
     * 使用Aviator表达式对命令返回值进行断言校验。
     * 将 RETURNS 绑定为实际返回值，KEY/VALUE/VALUE1/VALUE2 绑定为命令执行时的数据变量。
     * 表达式求值结果为 true 表示断言通过，否则记录失败。
     */
    private void evaluateAviatorAssert(CommandTarget target, Object returnObj) {
        Object returns;
        if (target.returnType == CommandHelper.RETURN_TYPE_LONG) {
            returns = BuilderFactory.LONG.build(returnObj);
        } else {
            returns = getReturnValueAsString(returnObj);
        }

        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", returns);

        String key = target.evlKey;
        String[] values = target.evlValues != null ? target.evlValues : CommandHelper.EMPTY_VALUES;
        env.put("KEY", key);
        for (int i = 0; i < values.length; i++) {
            if (i == 0) {
                env.put("VALUE", values[i]);
            }
            env.put("VALUE" + (i + 1), values[i]);
        }

        try {
            Object result = AVIATOR.execute(target.assertEvlExpr, env);
            if (!Boolean.TRUE.equals(result)) {
                assertFailed++;
                commandLogger.error("Command:{}, return '{}', EVL assert failed, expr: [{}]",
                        toCommandLine(target.commandArgs), returns, target.assertEvlExpr);
            }
        } catch (Exception e) {
            assertFailed++;
            commandLogger.error("Command:{}, return '{}', EVL assert error, expr: [{}], error: {}",
                    toCommandLine(target.commandArgs), returns, target.assertEvlExpr, e.getMessage());
        }
    }

    /**
     * 将返回对象转换为String。支持 byte[]、List、Set、Map 等类型：
     * - byte[] 按 UTF-8 解码为字符串；
     * - List/Set 等集合类型递归转换为字符串后拼接，格式为 [e1, e2, ...]；
     * - Map 类型格式为 {k1=v1, k2=v2, ...}；
     * - 其他类型调用 toString()。
     */
    private String getReturnValueAsString(Object returnObj) {
        if (returnObj == null) {
            return null;
        }
        if (returnObj instanceof byte[]) {
            return BuilderFactory.STRING.build(returnObj);
        }
        if (returnObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) returnObj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(getReturnValueAsString(entry.getKey()));
                sb.append("=");
                sb.append(getReturnValueAsString(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (returnObj instanceof Collection) {
            Collection<?> coll = (Collection<?>) returnObj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : coll) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(getReturnValueAsString(item));
            }
            sb.append("]");
            return sb.toString();
        }
        return returnObj.toString();
    }


}



