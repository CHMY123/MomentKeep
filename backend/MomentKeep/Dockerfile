# MomentKeep 后端服务 Dockerfile
# 用于 Render 平台 + TiDB Cloud

# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装 curl（用于健康检查）
RUN apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

# 添加非 root 用户
RUN addgroup -S momentkeep && adduser -S momentkeep -G momentkeep

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 创建必要目录并设置权限
RUN mkdir -p /app/config /app/logs && \
    chown -R momentkeep:momentkeep /app

# 切换到非 root 用户
USER momentkeep

# 暴露端口
EXPOSE 8080

# JVM 优化参数
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 健康检查（可选）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/ || exit 1

# 启动命令（动态绑定 Render 的端口）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:8080}"]