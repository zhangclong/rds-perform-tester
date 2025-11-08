package com.uh.rds.testing;

public class TestingConstants {

    public final static int MAX_EXPIRE_SECONDS = 3 * 60; // 最长过期时间，单位秒

    public final static long THREADS_WAIT_JOIN = 2000; // 线程等待join时间，单位毫秒

    public final static long STATE_BEGIN_WAIT = 500; // 线程开始后等待多长时间开始统计，单位毫秒
}
