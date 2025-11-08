package com.uh.rds.testing.utils;

import com.uh.rds.testing.config.ConnectionConfig;
import com.uh.rds.testing.conn.ConnectionMode;
import com.uh.rds.testing.conn.Endpoint;
import com.uh.rds.testing.conn.RdsConnectInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.ProtocolCommand;

import java.util.*;

import static redis.clients.jedis.Protocol.Command.*;

public class RdsConnectionUtils {

    // 写入命令的集合
    private static Set<ProtocolCommand> writeCommands = Set.of(SET, SETEX, PSETEX, SETNX, MSET, MSETNX, GETSET, APPEND, INCR, INCRBY, INCRBYFLOAT, DECR, DECRBY,
            DEL, UNLINK, EXPIRE, EXPIREAT, PEXPIRE, PEXPIREAT, PERSIST, RENAME, RENAMENX, RESTORE, DUMP,
            HSET, HSETNX, HMSET, HDEL, HINCRBY, HINCRBYFLOAT, HEXPIRE, HPEXPIRE, HPEXPIREAT, HPERSIST,
            LPUSH, RPUSH, LPOP, RPOP, LPUSHX, RPUSHX, LSET, LREM, LTRIM, BLPOP, BRPOP, RPOPLPUSH, BRPOPLPUSH, LINSERT, LMOVE, BLMOVE, LMPOP, BLMPOP,
            SADD, SREM, SPOP, SMOVE, SUNIONSTORE, SINTERSTORE, SDIFFSTORE,
            ZADD, ZREM, ZINCRBY, ZUNIONSTORE, ZINTERSTORE, ZDIFFSTORE, ZRANGESTORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZREMRANGEBYLEX,
            GEOADD, PFADD, PFMERGE, XADD, XDEL, XTRIM, XACK, XGROUP, XCLAIM, XAUTOCLAIM,
            EVAL, EVALSHA, SCRIPT, FUNCTION, FCALL, SUBSCRIBE, UNSUBSCRIBE, PUBLISH, SPUBLISH, SSUBSCRIBE, SUNSUBSCRIBE,
            FLUSHDB, FLUSHALL, SWAPDB, MOVE, MIGRATE, COPY, ACL, TOUCH, MODULE);

    // 用于命令行模式命令解析
    // 空格
    private static final char SPACE = ' ';
    // 双引号
    private static final char DOUBLE_QUOTATION = '"';
    // 单引号
    private static final char QUOTATION = '\'';

    private static final Logger logger = LoggerFactory.getLogger(RdsConnectionUtils.class);

    public static void flushDb(RdsConnectInfo connection) {
        logger.info("FlushDB for :" + connection);

        connection.getEndpoints().forEach((ep) -> {
            try(Jedis jd = ep.newJedis()) {
                jd.flushAll();
            } catch (Exception e) {
                logger.error("Failed to clear data on " + ep, e);
            }
        });
    }

    public static CommandArguments buildCommandArgs(String commandLine) {
        String[] parts = parseCommandArgs(commandLine.trim());
        return buildCommandArgs(parts);
    }

    public static CommandArguments buildCommandArgs(String[] stringArgs) {

        if(stringArgs.length == 0) {
            throw new IllegalArgumentException("Invalid stringArgs: " + Arrays.toString(stringArgs));
        }
        ProtocolCommand cmd = Protocol.Command.valueOf(stringArgs[0].toUpperCase());
        CommandArguments cmdArgs = new CommandArguments(cmd);

        if(stringArgs.length == 1) {
            return cmdArgs;
        }
        else {
            for (int i = 1; i < stringArgs.length; i++) {
                cmdArgs.add(stringArgs[i]);
            }
            return cmdArgs;
        }
    }

    public static boolean isWriteCommand(CommandArguments args) {
        return writeCommands.contains(args.getCommand());
    }


    public static String[] parseCommandArgs(String command) {
        List<String> stringArgs = new ArrayList<>();

        if (!command.contains("\"") && !command.contains("'")) {
            String[] args =  command.split(" ");
            // 删除掉args中的空字符串
            List<String> argList = new ArrayList<>(args.length);
            for(String s : args) {
                if (!s.trim().isEmpty()) {
                    argList.add(s);
                }
            }
            return argList.toArray(new String[0]);
        }

        char mode = '0';
        boolean isEscape = false;

        StringBuilder arg = new StringBuilder();

        for (int i = 0; i < command.length(); i++) {
            boolean first = false;
            char c = command.charAt(i);
            if (mode == '0') {
                if (c == DOUBLE_QUOTATION || c == QUOTATION) {
                    mode = c;
                    first = true;
                } else if (c == SPACE) {
                    continue;
                } else {
                    mode = SPACE;
                }
            }

            if (c != mode || first || isEscape) {
                arg.append(c);
                isEscape = c == '\\';
            } else if (arg.length() > 0 ) {
                if (mode != SPACE)
                    arg.append(c);
                stringArgs.add(arg.toString());
                arg = new StringBuilder();
                mode = '0';
            }

            if (i == (command.length() - 1) && mode == SPACE && c != QUOTATION && c != DOUBLE_QUOTATION) {
                stringArgs.add(arg.toString());
                arg.setLength(0);
            }

        }

        if (arg.length() > 0) throw new RuntimeException("Command format error, please check!");

        String[] args = stringArgs.toArray(new String[0]);
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.charAt(0) == QUOTATION || s.charAt(0) == DOUBLE_QUOTATION) {
                args[i] = s.substring(1, s.length() - 1);
            }
        }
        return args;
    }


    public static Object sendCommand(Jedis jedis, CommandArguments args) {
        Connection conn = jedis.getConnection();
        conn.sendCommand(args);
        return conn.getOne();
    }






    public static void main(String[] args) {
        // Example usage
        Jedis jedis = new Jedis("localhost", 6379);
        CommandArguments args1 = buildCommandArgs("SET key1 value1");
        Object setReturn = sendCommand(jedis, args1);
        System.out.println("Is write command:" + isWriteCommand(args1) + ", SET return: " + getBuilder("STRING").build(setReturn));

        CommandArguments args2 = buildCommandArgs("GET key1");
        Object getValue = sendCommand(jedis, args2);
        System.out.println("Is write command:" + isWriteCommand(args2) + ", GET key1: " + getBuilder("STRING").build(getValue));

        CommandArguments args3 = buildCommandArgs("INCR counter");
        Object incrValue = sendCommand(jedis, args3);
        System.out.println("Is write command:" + isWriteCommand(args3) + ", INCR counter: " + getBuilder("long").build(incrValue));

        CommandArguments args4 = buildCommandArgs("HSET myhash field1 'Hello' field2 'World'");
        Object hsetReturn = sendCommand(jedis, args4);
        System.out.println("Is write command:" + isWriteCommand(args4) + ", HSET return: " + getBuilder("long").build(hsetReturn));

        CommandArguments args5 = buildCommandArgs("HGETALL myhash");
        Object hgetallValue = sendCommand(jedis, args5);
        System.out.println("Is write command:" + isWriteCommand(args5) + ", HGETALL myhash: " + getBuilder("map").build(hgetallValue));

        jedis.close();

//        printArgs(parseCommandArgs("SET key1 \"value 1\" 'value 2' value3"));
//        printArgs(parseCommandArgs("SET key1 value1 value2 value3"));
//        printArgs(parseCommandArgs("  SET   key1    value1   value2   value3   "));
//        printArgs(parseCommandArgs("SET key1 'value 1' \"value 2\" value3"));
//        printArgs(parseCommandArgs("SET key1 'value 1' value2 \"value 3\""));
//        printArgs(parseCommandArgs("SET key1 'value 1' value2 'value 3'"));
//        printArgs(parseCommandArgs("eval \"return redis.call('set', KEYS[1], ARGV[1])\" 1 mykey 'my value'"));

    }

    public static Builder getBuilder(String returnTypeName) {
        switch (returnTypeName.toLowerCase()) {
            case "string":
                return BuilderFactory.STRING;
            case "long":
                return BuilderFactory.LONG;
            case "boolean":
                return BuilderFactory.BOOLEAN;
            case "double":
                return BuilderFactory.DOUBLE;
            case "map":
            case "string_map":
                return BuilderFactory.STRING_MAP;
            case "set":
            case "string_set":
                return BuilderFactory.STRING_SET;
            case "list":
            case "string_list":
                return BuilderFactory.STRING_LIST;
            default:
                throw new IllegalArgumentException("Unsupported return type: " + returnTypeName);
        }
    }



    public static void printArgs(String[] args) {
        // Print all arguments in one line
        System.out.print("Arguments(" + args.length + "): [");
        for(String arg : args) {
            System.out.print(arg + ", ");
        }
        System.out.print("]\n");
    }

    /**
     * 根据哨兵连接信息获取主从连接信息
     * @param connectionConfig 连接配置
     * @param sentinelConn 哨兵连接信息
     * @return 主从连接信息
     */
    public static RdsConnectInfo getSentinelMasterSlave(ConnectionConfig connectionConfig, RdsConnectInfo sentinelConn) {
        if(sentinelConn.getMode() != ConnectionMode.SENTINEL) {
            throw new IllegalArgumentException("Sentinel connection mode required!");
        }
        if(connectionConfig.getMasterName() == null || connectionConfig.getMasterName().isEmpty()) {
            throw new IllegalArgumentException("Must provide master name for sentinel connection!");
        }


        RdsConnectInfo conn = new RdsConnectInfo(ConnectionMode.MASTER_SLAVE, connectionConfig.getPassword());

        // 连接哨兵，获取主从节点信息
        for(Endpoint ep : sentinelConn.getEndpoints()) {
            try(Jedis jd = ep.newJedis()) {
                // 获取主节点信息
                List<String> masterInfo = jd.sentinelGetMasterAddrByName(connectionConfig.getMasterName());
                if(masterInfo != null && masterInfo.size() == 2) {
                    String masterHost = masterInfo.get(0);
                    int masterPort = Integer.parseInt(masterInfo.get(1));
                    Endpoint masterEp = new Endpoint(masterHost, masterPort, true);
                    conn.addEndpoint(masterEp);

                    // 获取从节点信息
                    List<Map<String, String>> slaves = jd.sentinelSlaves(connectionConfig.getMasterName());
                    for (Map<String, String> slave : slaves) {
                        String ip = slave.get("ip");
                        String portStr = slave.get("port");
                        String flags = slave.get("flags");
                        boolean isDown = flags != null && (flags.contains("s_down") || flags.contains("o_down"));
                        if (ip != null && portStr != null && isDown == false) {
                            int port = Integer.parseInt(portStr);
                            Endpoint slaveEp = new Endpoint(ip, port, false);
                            conn.addEndpoint(slaveEp);
                        }
                    }
                    return conn;
                }
            } catch (Exception e) {
                // 连接失败，尝试下一个哨兵节点
                logger.warn("Failed to connect to sentinel " + ep + ": " + e.getMessage());
            }
        }

        throw new RuntimeException("Failed to get master/slave info from sentinels for master name: " + connectionConfig.getMasterName());
    }

}
