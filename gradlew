#!/bin/sh
# Скрипт для скачивания Gradle Wrapper в CI

if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Downloading Gradle Wrapper JAR..."
    mkdir -p gradle/wrapper
    curl -sL -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar
    curl -sL -o gradle/wrapper/gradle-wrapper.properties https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.properties
fi

exec java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
