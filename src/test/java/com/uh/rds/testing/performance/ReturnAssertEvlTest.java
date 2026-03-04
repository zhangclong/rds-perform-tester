package com.uh.rds.testing.performance;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.exception.ExpressionSyntaxErrorException;
import com.uh.rds.testing.config.CommandConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 returnAssertEvl 功能：配置解析、表达式变换、Aviator 表达式求值及错误处理。
 */
public class ReturnAssertEvlTest {

    private static final AviatorEvaluatorInstance AVIATOR = AviatorEvaluator.getInstance();

    // ---- CommandConfig 解析 -----------------------------------------------

    @Test
    public void testCommandConfigParsesReturnAssertEvl() {
        CommandConfig config = new CommandConfig();
        config.setReturnAssertEvl("  ${RETURNS} == ${VALUE}  ");
        assertEquals("${RETURNS} == ${VALUE}", config.getReturnAssertEvl());
    }

    // ---- CommandHelper: ${VAR} → VAR 变换 ------------------------------------

    @Test
    public void testExprTransformReplacesPlaceholders() {
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnAssertEvl("${RETURNS} == ${VALUE}");
        config.setReturnType("STRING");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "k1", new String[]{"v1"});

        assertEquals(CommandHelper.COMPARE_EVL, target.compareMethod);
        assertEquals("RETURNS == VALUE", target.assertEvlExpr);
        assertEquals("k1", target.evlKey);
        assertArrayEquals(new String[]{"v1"}, target.evlValues);
    }

    @Test
    public void testExprTransformReturnTypeAndKey() {
        CommandConfig config = new CommandConfig();
        config.setLine("INCR ${KEY}");
        config.setReturnAssertEvl("${RETURNS} > 0");
        config.setReturnType("LONG");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "counter1", null);

        assertEquals(CommandHelper.COMPARE_EVL, target.compareMethod);
        assertEquals("RETURNS > 0", target.assertEvlExpr);
        assertEquals(CommandHelper.RETURN_TYPE_LONG, target.returnType);
    }

    @Test
    public void testExprTransformAllPlaceholders() {
        CommandConfig config = new CommandConfig();
        config.setLine("HSET ${KEY} ${VALUE1} ${VALUE2}");
        config.setReturnAssertEvl("${RETURNS} == ${KEY}");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "myKey", new String[]{"field1", "val1"});

        assertEquals("RETURNS == KEY", target.assertEvlExpr);
    }

    @Test
    public void testMutualExclusivityThrows() {
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnAssert("OK");
        config.setReturnAssertEvl("${RETURNS} == 'OK'");

        CommandHelper helper = new CommandHelper();
        assertThrows(IllegalArgumentException.class,
                () -> helper.toCommandTarget(config, "k1", new String[]{"v1"}));
    }

    // ---- Aviator 表达式求值：字符串相等 ----------------------------------------

    @Test
    public void testAviatorStringEquals_pass() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", "v00000001");
        env.put("VALUE", "v00000001");

        Object result = AVIATOR.execute("RETURNS == VALUE", env);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testAviatorStringEquals_fail() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", "v00000002");
        env.put("VALUE", "v00000001");

        Object result = AVIATOR.execute("RETURNS == VALUE", env);
        assertEquals(Boolean.FALSE, result);
    }

    // ---- Aviator 表达式求值：Long 数值相等 ----------------------------------------

    @Test
    public void testAviatorLongEquals_pass() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", 200L);

        Object result = AVIATOR.execute("RETURNS == 200", env);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testAviatorLongComparison_pass() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", 200L);

        assertTrue(Boolean.TRUE.equals(AVIATOR.execute("RETURNS > 100", env)));
        assertTrue(Boolean.TRUE.equals(AVIATOR.execute("RETURNS < 1000", env)));
    }

    @Test
    public void testAviatorLongComparison_fail() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", 50L);

        assertEquals(Boolean.FALSE, AVIATOR.execute("RETURNS > 100", env));
    }

    @Test
    public void testExprTransformStringMethodContains() {
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnAssertEvl("${RETURNS}.contains('ok')");
        config.setReturnType("STRING");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "k1", new String[]{"v1"});

        assertEquals("string.contains(RETURNS, 'ok')", target.assertEvlExpr);
    }

    @Test
    public void testExprTransformStringMethodStartsWith() {
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnAssertEvl("${RETURNS}.startsWith('v000')");
        config.setReturnType("STRING");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "k1", new String[]{"v1"});

        assertEquals("string.startsWith(RETURNS, 'v000')", target.assertEvlExpr);
    }

    @Test
    public void testExprTransformStringMethodEndsWith() {
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnAssertEvl("${RETURNS}.endsWith('end')");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "k1", null);

        assertEquals("string.endsWith(RETURNS, 'end')", target.assertEvlExpr);
    }

    // ---- Aviator 表达式求值：字符串 contains ----------------------------------------

    @Test
    public void testAviatorStringContains_pass() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", "hello ok world");

        // Aviator built-in function form (after expression transformation)
        Object result = AVIATOR.execute("string.contains(RETURNS, 'ok')", env);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testAviatorStringContains_fail() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", "hello world");

        Object result = AVIATOR.execute("string.contains(RETURNS, 'ok')", env);
        assertEquals(Boolean.FALSE, result);
    }

    // ---- Aviator 表达式求值：字符串 startsWith ----------------------------------------

    @Test
    public void testAviatorStringStartsWith_pass() {
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", "v000001");

        // Aviator built-in function form (after expression transformation)
        Object result = AVIATOR.execute("string.startsWith(RETURNS, 'v000')", env);
        assertEquals(Boolean.TRUE, result);
    }

    // ---- 通过 CommandHelper 完整转换验证 contains/startsWith  ----------------------

    @Test
    public void testDocSyntaxContainsViaTransform() {
        // 验证文档中的写法 "${RETURNS}.contains('ok')" 经过转换后可被 Aviator 正确求值
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnAssertEvl("${RETURNS}.contains('ok')");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "k1", null);

        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", "hello ok world");
        Object result = AVIATOR.execute(target.assertEvlExpr, env);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testDocSyntaxStartsWithViaTransform() {
        // 验证文档中的写法 "${RETURNS}.startsWith('v000')" 经过转换后可被 Aviator 正确求值
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnAssertEvl("${RETURNS}.startsWith('v000')");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "k1", null);

        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", "v000123");
        Object result = AVIATOR.execute(target.assertEvlExpr, env);
        assertEquals(Boolean.TRUE, result);
    }

    // ---- 空值/缺字段处理 -------------------------------------------------------

    @Test
    public void testAviatorReturnsNullStringHandling() {
        // RETURNS 为 null 时，表达式 "RETURNS == nil" 应为 true
        Map<String, Object> env = new HashMap<>();
        env.put("RETURNS", null);

        Object result = AVIATOR.execute("RETURNS == nil", env);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testNoAssertEvlWhenNotConfigured() {
        // 未配置 returnAssertEvl 时，compareMethod 应为 0（不比较）
        CommandConfig config = new CommandConfig();
        config.setLine("GET ${KEY}");
        config.setReturnType("STRING");

        CommandHelper helper = new CommandHelper();
        CommandTarget target = helper.toCommandTarget(config, "k1", new String[]{"v1"});

        assertEquals(0, target.compareMethod);
        assertNull(target.assertEvlExpr);
    }

    // ---- 表达式语法错误 -------------------------------------------------------

    @Test
    public void testAviatorSyntaxError() {
        // Aviator 无效表达式（=== 不是合法操作符）会抛出 ExpressionSyntaxErrorException
        assertThrows(ExpressionSyntaxErrorException.class,
                () -> AVIATOR.execute("RETURNS === VALUE", new HashMap<>()));
    }

}
