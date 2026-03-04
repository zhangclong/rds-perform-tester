package com.uh.rds.testing.performance;

import com.uh.rds.testing.config.CommandConfig;
import com.uh.rds.testing.utils.StringUtils;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.Rawable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.uh.rds.testing.utils.RdsConnectionUtils.buildCommandArgs;
import static com.uh.rds.testing.utils.RdsConnectionUtils.isWriteCommand;



public class CommandHelper {

    public static final int RETURN_TYPE_STRING = 1;
    public static final int RETURN_TYPE_LONG = 2;

    public static final int COMPARE_NOT_EMPTY = 1; // 非空
    public static final int COMPARE_EMPTY = 2; // 为空
    public static final int COMPARE_EQ = 6; // ==
    public static final int COMPARE_EVL = 7; // Aviator表达式

    private static final int MAX_VALUES = 5; // 支持的最大值个数

    /** 匹配 ${VAR} 占位符，VAR 为字母数字组成的标识符 */
    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\$\\{([A-Za-z][A-Za-z0-9]*)\\}");

    /**
     * 将 Java 风格的字符串方法调用（如 VAR.contains(...)）转换为 Aviator 内置函数形式
     * （如 string.contains(VAR, ...)），以支持 using-guide.md 中记录的表达式写法。
     * 支持转换的方法：contains、startsWith、endsWith。
     */
    private static final Pattern STRING_METHOD_PATTERN =
            Pattern.compile("(\\w+)\\.(contains|startsWith|endsWith)\\((.+?)\\)");

    public static final String[] EMPTY_VALUES = {};

    private Map<String, String> templateContext;

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
        String returnAssertEvl = commandConfig.getReturnAssertEvl();

        if (StringUtils.isNotEmpty(returnAssert) && StringUtils.isNotEmpty(returnAssertEvl)) {
            throw new IllegalArgumentException(
                "returnAssert and returnAssertEvl cannot both be set on the same command: " + commandConfig.getLine());
        }

        if (StringUtils.isNotEmpty(returnAssert)) {
            if(returnAssert.equals("#{NOT_EMPTY}")) {
                target.compareMethod = CommandHelper.COMPARE_NOT_EMPTY;
                target.compareValue = null;
            }
            else if(returnAssert.equals("#{EMPTY}")) {
                target.compareMethod = CommandHelper.COMPARE_EMPTY;
                target.compareValue = null;
            }
            else {
                // 对 returnAssert 进行模板变量替换，支持 ${KEY} ${VALUE} ${VALUE1} ${VALUE2} 等占位符
                String evaluatedAssert = evaluateTemplate(returnAssert, composeContext(key, values));
                target.compareMethod = CommandHelper.COMPARE_EQ;
                if(target.returnType == RETURN_TYPE_LONG) {
                    try {
                        target.compareValue = Long.parseLong(evaluatedAssert);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Return assert value is not a valid long: " + evaluatedAssert);
                    }
                } else {
                    target.compareValue = evaluatedAssert;
                }
            }
        }

        if (StringUtils.isNotEmpty(returnAssertEvl)) {
            // 将 ${VAR} 形式的占位符转换为 Aviator 变量名（去掉 ${ 和 }）
            String aviatorExpr = TEMPLATE_VAR_PATTERN.matcher(returnAssertEvl).replaceAll("$1");
            // 将 Java 风格方法调用（如 RETURNS.contains(...)）转换为 Aviator 内置函数形式
            aviatorExpr = STRING_METHOD_PATTERN.matcher(aviatorExpr).replaceAll("string.$2($1, $3)");
            target.assertEvlExpr = aviatorExpr;
            target.evlKey = key;
            target.evlValues = values;
            target.compareMethod = COMPARE_EVL;
        }

        return target;
    }

    private Map<String, String> composeContext(String key, String[] values) {
        if(values == null) {
            values = EMPTY_VALUES;
        }

        int valLen = values.length;
        if(valLen > MAX_VALUES) {
            throw new IllegalArgumentException("Too many values, max supported is " + MAX_VALUES);
        }

        if(templateContext == null) {
            templateContext = new HashMap<>();
        }
        templateContext.clear();

        templateContext.put("KEY", key);
        for(int i = 0; i < values.length; i++) {
            if(i == 0) {
                templateContext.put("VALUE", values[i]);
            }
            templateContext.put("VALUE" + (i+1), values[i]);
        }

        return templateContext;
    }

    /**
     * 对模板字符串进行变量替换。
     * 支持的占位符格式为 ${VAR}，其中 VAR 为变量名（字母开头，由字母数字组成）。
     *
     * @param template 模板字符串，包含 ${VAR} 形式的占位符
     * @param context 变量映射表，key 为变量名，value 为替换值
     * @return 替换后的字符串
     */
    private String evaluateTemplate(String template, Map<String, String> context) {
        if (template == null) {
            return null;
        }

        Matcher matcher = TEMPLATE_VAR_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1); // 提取变量名（不含 ${ 和 }）
            String replacement = context.get(varName);
            if (replacement != null) {
                // 需要转义 $ 和 \ 以避免 appendReplacement 将其解释为特殊字符
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // 变量未定义时保留原占位符
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
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
