# MomentKeep 后端 Dockerfile - 微信云托管部署
# 基于 Spring Boot 3.5.13 + JDK 21

# ==================== 构建阶段 ====================
FROM maven:3.9-amazoncorretto-21-alpine AS builder

WORKDIR /build

# 配置国内Maven镜像源，加速依赖下载
RUN mkdir -p /root/.m2 && \
    echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > /root/.m2/settings.xml && \
    echo "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\"" >> /root/.m2/settings.xml && \
    echo "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" >> /root/.m2/settings.xml && \
    echo "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd\">" >> /root/.m2/settings.xml && \
    echo "    <mirrors>" >> /root/.m2/settings.xml && \
    echo "        <mirror>" >> /root/.m2/settings.xml && \
    echo "            <id>aliyunmaven</id>" >> /root/.m2/settings.xml && \
    echo "            <mirrorOf>central</mirrorOf>" >> /root/.m2/settings.xml && \
    echo "            <url>https://maven.aliyun.com/repository/public</url>" >> /root/.m2/settings.xml && \
    echo "        </mirror>" >> /root/.m2/settings.xml && \
    echo "    </mirrors>" >> /root/.m2/settings.xml && \
    echo "</settings>" >> /root/.m2/settings.xml

# 先复制pom.xml下载依赖（利用Docker缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -B

# ==================== 运行阶段 ====================
FROM amazoncorretto:21-alpine

WORKDIR /app

# 安装 curl 用于健康检查（可选）
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

COPY --from=builder /build/target/*.jar app.jar

# 创建非 root 用户（可选，提高安全性）
RUN addgroup -g 1000 -S momentkeep && \
    adduser -u 1000 -S momentkeep -G momentkeep && \
    chown -R momentkeep:momentkeep /app
USER momentkeep

EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE="prod"

# 动态绑定端口
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --server.port=${PORT:8080}"]