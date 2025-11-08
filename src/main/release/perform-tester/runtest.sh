#!/bin/sh
# ./console.sh run|start|stop|status
AppName=rds-testing.jar

# Check if Java exists in the system
JAVA=$(command -v java || which java)

if [ -z "$JAVA" ]; then
    echo -e "\033[0;31m ERROR: Java was not found \033[0m"
    exit 1
fi

# JVM参数
JVM_OPTS="-Xmx8g -XX:MaxMetaspaceSize=512m -XX:+UseG1GC"

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Set APP_HOME as current working directory
APP_HOME=`cd "$PRGDIR" >/dev/null; pwd`
cd $APP_HOME

echo "APP_HOME: $APP_HOME"

java $JVM_OPTS -jar $AppName
