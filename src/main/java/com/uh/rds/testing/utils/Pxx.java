package com.uh.rds.testing.utils;
/**
 * 百分位数监控指标计算类。例如P90表示包含90%的值，即总样本中从少到多计数第90%个样本的值，
 * 如需要计算一组样本内P90、P99和P99.9的值，可定义：
 * Pxx pxx = new Pxx(9000, 9900, 9990);
 * pxx.set(long)依次输入各样本值
 * pxx.get()返回上述样本区间内的P90、P99、P99.9、总样本数、总样本时间共5个长整数的数组，同时并清空累计值。
 */
public class Pxx {

    private final static int MAX_PERCENT = 10000;

    private final static long[] DEVISIONS = new long[]{2, 4, 7, 10, 15, 23, 34, 51, 76, 114
            , 171, 256, 384, 577, 865, 1298, 2596, 5192, 10384, 32768
            , 131072, 524288, 2097152};

    private final static int BLOCKS = DEVISIONS.length;

    private final int[] datas = new int[BLOCKS * 2];

    private final int[] caculateData = new int[BLOCKS];

    private final int[] indicators;

    private volatile long totalTimes = 0;
    private volatile long totalTime = 0;

    public Pxx() {
        // P90
        this(9000);
    }

    /**
     * for excample: Pxx pxx = new Pxx(9000, 9900, 9990);
     * and call pxx.set(duration); many times
     * then call pxx.get() will obtain long[]{P90, P99, P99.9}
     *
     * @param indicators the indicators
     */
    public Pxx(int... indicators) {
        if (indicators == null || indicators.length == 0) {
            throw new IllegalArgumentException("null argument");
        }

        for (int i = 0; i < indicators.length; ++i) {
            if (indicators[i] >= MAX_PERCENT) {
                throw new IllegalArgumentException("the max value of indicator is " + MAX_PERCENT);
            } else if (indicators[i] < (MAX_PERCENT >> 2)) {
                throw new IllegalArgumentException("indicator " + indicators[i] + " is too small. the max value of indicator is "
                        + MAX_PERCENT + ", the indicator must be greater or equal than " + (MAX_PERCENT >> 2));
            }
        }

        this.indicators = indicators;
        for (int i = 0; i < datas.length; ++i) {
            datas[i] = 0;
        }

        //logger.debug("New Pxx created for {} indicators", indicators.length);
        //if (logger.isDebug()) {
        //    logger.debugLog("Pxx::() New Pxx created for " + indicators.length + " indicators");
        //}
    }

    public void set(long duration) {
        int start = 0;
        int stop = BLOCKS - 1;
        while (start != stop) {
            int cur = (start + stop) >> 1;
            if (duration >= DEVISIONS[cur]) {
                // Here cur must be less than BLOCKS - 1
                start = cur + 1;
            } else if (cur > 0 && duration < DEVISIONS[cur - 1]) {
                stop = cur - 1;
            } else {
                start = stop = cur;
            }
        }
        synchronized (datas) {
            ++datas[start];
            ++totalTimes;
            totalTime += duration;
        }
    }

    public long[] get() {
        long[] ret = new long[indicators.length + 2];
        synchronized (caculateData) {
            synchronized (datas) {
                System.arraycopy(datas, 0, caculateData, 0, BLOCKS);
                System.arraycopy(datas, BLOCKS, datas, 0, BLOCKS);

                ret[ret.length - 2] = totalTimes;
                totalTimes = 0;
                ret[ret.length - 1] = totalTime;
                totalTime = 0;
            }

//            if (logger.isDebugEnable()) {
//                StringBuilder buf = new StringBuilder("Duration distribution: [ ");
//                for (int i = 0; i < BLOCKS; ++i) {
//                    buf.append(caculateData[i]).append(' ');
//                }
//                buf.append(']');
//                logger.debug(buf.toString());
//            }
//            if (logger.isDebug()) {
//                StringBuilder buf = new StringBuilder(64);
//                for (int i = 0; i < BLOCKS; ++i) {
//                    buf.append(caculateData[i]).append(' ');
//                }
//                logger.debugLog("Pxx::get() Duration distribution: [ {}]",buf);
//            }

            long total = ret[ret.length - 2];
            for (int i = 0; i < indicators.length; ++i) {
                if (total > 0) {
                    long abnormal = (MAX_PERCENT - indicators[i]) * total / MAX_PERCENT;
                    ret[i] = getPxx(Math.max(1, abnormal));
                } else {
                    ret[i] = 0;
                }
            }
        }
        return ret;
    }

    private long getPxx(long slown) {
        int i = caculateData.length - 1;
        int slows = 0;
        for (; i >= 0; --i) {
            if (slows + caculateData[i] >= slown) {
                break;
            }
            slows += caculateData[i];
        }

        if (i < 0) {
            return 0;
        } else {
            long min = 0;
            if (i > 0) {
                min = DEVISIONS[i - 1];
            }
            long max = DEVISIONS[DEVISIONS.length - 1];
            if (i < DEVISIONS.length - 1) {
                max = DEVISIONS[i];
            }
            long left = caculateData[i] - (slown - slows);
            return (max - min) * left / caculateData[i] + min;
        }
    }

    public static void main(String[] args) {
        Pxx pxx = new Pxx(5000, 9000, 9900, 9990);
        pxx.set(0);
        pxx.set(10);
        pxx.set(38);
        pxx.set(27);
        pxx.set(83);
        pxx.set(42);
        pxx.set(31);
        pxx.set(51);
        pxx.set(47);
        pxx.set(71);
        pxx.set(129000000l);
        pxx.set(82);
        pxx.set(286);
        pxx.set(21);
        pxx.set(6);
        pxx.set(15);
        pxx.set(25);
        pxx.set(34);
        pxx.set(62);
        pxx.set(34);
        pxx.set(75);
        pxx.set(598);
        pxx.set(19);
        pxx.set(24);
        pxx.set(35);
        pxx.set(48);
        pxx.set(68);
        pxx.set(93);
        pxx.set(152);
        pxx.set(29);
        pxx.set(67);
        pxx.set(63);
        pxx.set(70);


        StringBuilder buf = new StringBuilder();
        buf.append("[ ");
        for (long l : pxx.get()) {
            buf.append(l).append(' ');
        }
        buf.append(']');
        System.out.println(buf);

        buf.setLength(0);
        buf.append("[ ");
        for (int i = 0; i < pxx.caculateData.length; ++i) {
            long min = i > 0 ? DEVISIONS[i - 1] : 0;
            long l = pxx.caculateData[i];
            buf.append(l).append('(').append(min).append('-').append(DEVISIONS[i] - 1).append(") ");
        }
        buf.append(']');
        System.out.println(buf);
    }
}
