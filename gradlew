#!/usr/bin/env sh

if [ -z "$APP_HOME" ]; then
  APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
fi

APP_HOME="${APP_HOME:-$PWD}"

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ ! -f "$CLASSPATH" ]; then
  echo "Gradle wrapper JAR not found at $CLASSPATH" >&2
  exit 1
fi

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
