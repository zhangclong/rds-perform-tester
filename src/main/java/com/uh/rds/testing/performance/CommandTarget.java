package com.uh.rds.testing.performance;

import com.uh.rds.testing.utils.RdsConnectionUtils;
import com.uh.rds.testing.utils.StringUtils;
import com.uh.rds.testing.utils.ValueUtils;
import redis.clients.jedis.CommandArguments;
import com.uh.rds.testing.config.CommandConfig;

/**
 * 命令目标类，表示一个命令的执行目标
 */
public class CommandTarget {

    // 对返回结果进行比较，比较的方法： 0 不比较, 1 非空(NOT_EMPTY), 2 为空(EMPTY), 6 相等(EQ), 7 Aviator表达式(EVL)
    int compareMethod = 0; //  默认不比较

    CommandArguments commandArgs;

    boolean isWrite; // 是否为写命令

    int sleepMillis; // 执行后等待的时间，单位毫秒

    Object compareValue; // 用于比较的值

    int returnType; // 返回值类型 1 String, 2 Long

    int repeatTimes; // 重复执行次数，默认1次

    // returnAssertEvl 相关字段
    String assertEvlExpr; // 已转换的Aviator表达式（${VAR} 已替换为 VAR）
    String evlKey;        // 用于Aviator变量绑定的KEY值
    String[] evlValues;   // 用于Aviator变量绑定的VALUES值

    public CommandTarget() {
    }


}
