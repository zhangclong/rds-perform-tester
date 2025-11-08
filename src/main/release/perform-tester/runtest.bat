@echo off

rem jar平级目录
set AppName=rds-testing.jar

rem JVM参数
set JVM_OPTS=-Xmx8g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC

rem 在新窗口中运行
start "rds-testing" java %JVM_OPTS% -jar %AppName%


