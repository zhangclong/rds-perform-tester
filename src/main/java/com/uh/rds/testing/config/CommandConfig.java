package com.uh.rds.testing.config;

public class CommandConfig{
    // 带参数的命令行，运行时会自动替换 ${KEY} ${FIELD} ${VALUE}为相应的数据值
    private String line;

    // 执行这个命令时，返回值的校验
    private String returnAssert;

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
        return (sleep > 0) ? "Command[sleep=" + sleep + "]" : "Command[" +
                "'" + line + "' assert='" + returnAssert + "']";
    }


    @Override
    public String toString() {
        return "CommandConfig{" +
                "line='" + line + '\'' +
                ", returnAssert='" + returnAssert + '\'' +
                ", repeatTimes=" + repeatTimes +
                ", returnType='" + returnType + '\'' +
                ", sleep=" + sleep +
                '}';
    }
}
