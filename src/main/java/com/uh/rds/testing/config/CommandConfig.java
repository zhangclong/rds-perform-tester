package com.uh.rds.testing.config;

public class CommandConfig{
    // 带参数的命令行，运行时会自动替换 ${KEY} ${FIELD} ${VALUE}为相应的数据值
    private String line;

    // 执行这个命令时，返回值的校验
    private String returnAssert;

    // 执行这个命令时，返回值的Aviator表达式校验（与returnAssert互斥）
    private String returnAssertEvl;

    // 重复执行次数，默认1次
    private int repeatTimes = 1;

    // 返回值类型，可选LONG, STRING. 默认STRING
    private String returnType = "STRING";

    // 每次执行命令后的等待时间，单位毫秒，默认0
    private int sleep = 0;


    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getReturnAssert() {
        return returnAssert;
    }

    public void setReturnAssert(String returnAssert) {
        this.returnAssert = returnAssert.trim();
    }

    public String getReturnAssertEvl() {
        return returnAssertEvl;
    }

    public void setReturnAssertEvl(String returnAssertEvl) {
        this.returnAssertEvl = returnAssertEvl.trim();
    }

    public int getRepeatTimes() {
        return repeatTimes;
    }

    public void setRepeatTimes(int repeatTimes) {
        this.repeatTimes = repeatTimes;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public int getSleep() {
        return sleep;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    /**
     * 简短描述, 用于日志打印.
     * @return
     */
    public String toShort() {
        if (sleep > 0) return "Command[sleep=" + sleep + "]";
        String assertInfo = returnAssertEvl != null ? returnAssertEvl : returnAssert;
        return "Command['" + line + "' assert='" + assertInfo + "']";
    }


    @Override
    public String toString() {
        return "CommandConfig{" +
                "line='" + line + '\'' +
                ", returnAssert='" + returnAssert + '\'' +
                ", returnAssertEvl='" + returnAssertEvl + '\'' +
                ", repeatTimes=" + repeatTimes +
                ", returnType='" + returnType + '\'' +
                ", sleep=" + sleep +
                '}';
    }
}
