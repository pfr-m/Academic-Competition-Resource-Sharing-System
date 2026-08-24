# 1. 基础镜像：使用官方 JDK 21 运行环境
FROM eclipse-temurin:21-jre

# 2. 声明端口：与 application.properties 中的 8099 保持一致
EXPOSE 8099

# 3. 复制 target 目录下的 jar 包
COPY target/competition-hub-backend-0.0.1-SNAPSHOT.jar app.jar

# 4. 启动命令（设置时区为上海，避免容器内日志时间不对）
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-jar", "/app.jar"]