#!/bin/bash
set -e

WORKSPACE_DIR="/home/wyb/linuxvm"
VECTOR_DIR="${WORKSPACE_DIR}/../Vector"

echo "=== Vector Build Script ==="

# 设置环境变量
export JAVA_HOME="${WORKSPACE_DIR}/jdk-21.0.10"
export ANDROID_HOME="${WORKSPACE_DIR}/android-sdk"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

echo "Environment variables set:"
echo "JAVA_HOME=${JAVA_HOME}"
echo "ANDROID_HOME=${ANDROID_HOME}"

# 进入项目目录
cd "${VECTOR_DIR}"

# 清理构建缓存以解决资源链接错误
echo "Cleaning build cache..."
./gradlew clean

# 执行完整模块构建
echo "Building Vector module package..."
./gradlew zipAll

echo "=== Build completed! ==="
echo "Output: ${VECTOR_DIR}/zygisk/release/"
ls -lh "${VECTOR_DIR}/zygisk/release/" | grep ".zip"
