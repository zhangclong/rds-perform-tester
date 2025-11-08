package com.uh.rds.testing.performance;

import com.uh.rds.testing.config.CommandConfig;
import com.uh.rds.testing.utils.StringUtils;
import com.uh.rds.testing.utils.VelocityEvaluator;
import org.apache.velocity.VelocityContext;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.Rawable;

import java.nio.charset.StandardCharsets;

import static com.uh.rds.testing.utils.RdsConnectionUtils.buildCommandArgs;
import static com.uh.rds.testing.utils.RdsConnectionUtils.isWriteCommand;



public class CommandHelper {

    public static final int RETURN_TYPE_STRING = 1;
    public static final int RETURN_TYPE_LONG = 2;

    public static final int COMPARE_NOT_EMPTY = 1; // 非空
    public static final int COMPARE_EQ = 6; // ==

    private static final int MAX_VALUES = 5; // 支持的最大值个数

    public static final String[] EMPTY_VALUES = {};

    private final VelocityEvaluator evaluator = new VelocityEvaluator();

    private VelocityContext context;

    public static String toCommandLine(CommandArguments commandArgs) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Rawable r : commandArgs) {
            if (!first) {
                sb.append(' ');
            }
            first = false;

            byte[] raw;
            try {
                raw = r.getRaw();
            } catch (Exception e) {
                // 取 raw 失败时降级为 toString()
                sb.append(escapeToken(String.valueOf(r)));
                continue;
            }

            if (raw == null) {
                sb.append("null");
            } else if (isPrintableUtf8(raw)) {
                String token = new String(raw, StandardCharsets.UTF_8);
                sb.append(escapeToken(token));
            } else {
                sb.append("0x").append(bytesToHex(raw));
            }
        }
        return sb.toString();
    }

    public CommandTarget toCommandTarget(CommandConfig commandConfig, String key, String[] values) {
        CommandTarget target = new CommandTarget();

        // 如果配置了sleep，则表示仅仅是等待，不执行命令
        if(commandConfig.getSleep() != 0) {
            target.sleepMillis = commandConfig.getSleep();
            return target;
        }

        String commandLine = evaluateTemplate(commandConfig.getLine(), composeContext(key, values));

        target.commandArgs = buildCommandArgs(commandLine);
        target.isWrite = isWriteCommand(target.commandArgs);
        target.repeatTimes = commandConfig.getRepeatTimes();

        String type = commandConfig.getReturnType();
        if ("string".equalsIgnoreCase(type)) {
            target.returnType = RETURN_TYPE_STRING;
        } else if ("long".equalsIgnoreCase(type)) {
            target.returnType = RETURN_TYPE_LONG;
        } else {
            target.returnType = RETURN_TYPE_STRING; // 默认字符串类型
        }

        String returnAssert = commandConfig.getReturnAssert();
        if (StringUtils.isNotEmpty(returnAssert)) {
            if(returnAssert.equals("#{NOT_EMPTY}")) {
                target.compareMethod = CommandHelper.COMPARE_NOT_EMPTY;
                target.compareValue = null;
            }
            else if(returnAssert.equals("${VALUE}") || returnAssert.equals("${VALUE1}")) {
                target.compareMethod = CommandHelper.COMPARE_EQ;
                if(target.returnType == RETURN_TYPE_LONG) {
                    try {
                        target.compareValue = Long.parseLong(values[0]);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Return assert value is not a valid long: " + values[0]);
                    }
                } else {
                    target.compareValue = values[0];
                }
            }
            else if(returnAssert.equals("${VALUE2}")) {
                target.compareMethod = CommandHelper.COMPARE_EQ;
                if(target.returnType == RETURN_TYPE_LONG) {
                    try {
                        target.compareValue = Long.parseLong(values[1]);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Return assert value is not a valid long: " + values[1]);
                    }
                } else {
                    target.compareValue = values[1];
                }
            }
            else if(returnAssert.equals("${VALUE3}")) {
                target.compareMethod = CommandHelper.COMPARE_EQ;
                if(target.returnType == RETURN_TYPE_LONG) {
                    try {
                        target.compareValue = Long.parseLong(values[2]);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Return assert value is not a valid long: " + values[2]);
                    }
                } else {
                    target.compareValue = values[2];
                }
            }
            else if(returnAssert.equals("${VALUE4}")) {
                target.compareMethod = CommandHelper.COMPARE_EQ;
                if(target.returnType == RETURN_TYPE_LONG) {
                    try {
                        target.compareValue = Long.parseLong(values[3]);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Return assert value is not a valid long: " + values[3]);
                    }
                } else {
                    target.compareValue = values[3];
                }
            }
            else if(returnAssert.equals("${VALUE5}")) {
                target.compareMethod = CommandHelper.COMPARE_EQ;
                if(target.returnType == RETURN_TYPE_LONG) {
                    try {
                        target.compareValue = Long.parseLong(values[4]);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Return assert value is not a valid long: " + values[4]);
                    }
                } else {
                    target.compareValue = values[4];
                }
            }
            else {
                target.compareMethod = CommandHelper.COMPARE_EQ;
                if(target.returnType == RETURN_TYPE_LONG) {
                    try {
                        target.compareValue = Long.parseLong(returnAssert);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Return assert value is not a valid long: " + returnAssert);
                    }
                } else {
                    target.compareValue = returnAssert;
                }
            }
        }

        return target;
    }

    private VelocityContext composeContext(String key, String[] values) {
        if(values == null) {
            values = EMPTY_VALUES;
        }

        int valLen = values.length;
        if(valLen > MAX_VALUES) {
            throw new IllegalArgumentException("Too many values, max supported is " + MAX_VALUES);
        }

        if(context == null) {
            context = new VelocityContext();
        }

        context.put("KEY", key);
        for(int i = 0; i < values.length; i++) {
            if(i == 0) {
                context.put("VALUE", values[i]);
            }
            context.put("VALUE" + (i+1), values[i]);
        }

        return context;
    }

    private String evaluateTemplate(String template, VelocityContext context) {
        return evaluator.evaluatedTemplate(template, context);
    }

    private static boolean isPrintableUtf8(byte[] data) {
        if (data.length == 0) {
            return true;
        }
        // 快速判断二进制控制字符
        for (byte b : data) {
            int v = b & 0xFF;
            if (v < 0x20 && v != '\n' && v != '\r' && v != '\t') {
                return false;
            }
            if (v == 0x7F) {
                return false;
            }
        }
        // 尝试按 UTF-8 解码，检测替换字符
        String s = new String(data, StandardCharsets.UTF_8);
        return !s.contains("\uFFFD");
    }

    private static String escapeToken(String token) {
        if (token == null) {
            return "null";
        }
        boolean needQuote = token.isEmpty() || token.matches(".*\\s.*") || token.contains("\"") || token.contains("\\");
        if (!needQuote) {
            return token;
        }
        String escaped = token.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
