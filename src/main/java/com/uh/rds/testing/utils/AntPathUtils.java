package com.uh.rds.testing.utils;



import java.util.List;

public class AntPathUtils {

    /**
     * 查找指定字符串是否匹配指定字符串列表中的任意一个字符串
     *
     * @param str 指定字符串
     * @param patterns 需要检查的规则列表
     * @return 是否匹配
     */
    public static boolean matches(String str, List<String> patterns)
    {
        if (ValueUtils.isEmpty(str) || ValueUtils.isEmpty(patterns))
        {
            return false;
        }
        for (String pattern : patterns)
        {
            if (isMatch(pattern, str))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找指定字符串是否匹配指定字符串列表中的任意一个字符串
     *
     * @param str 指定字符串
     * @param patterns 需要检查的规则列表
     * @return 是否匹配
     */
    public static boolean matches(String str, String[] patterns)
    {
        if (ValueUtils.isEmpty(str) || ValueUtils.isEmpty(patterns))
        {
            return false;
        }
        for (String pattern : patterns)
        {
            if (isMatch(pattern, str))
            {
                return true;
            }
        }
        return false;
    }


    /**
     * 判断url是否与规则配置:
     * ? 表示单个字符;
     * * 表示一层路径内的任意字符串，不可跨层级;
     * ** 表示任意层路径;
     *
     * @param pattern 匹配规则
     * @param url 需要匹配的url
     * @return
     */
    public static boolean isMatch(String pattern, String url)
    {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }


}
