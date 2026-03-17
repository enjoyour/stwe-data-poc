## AdoptOpenJDK 停止发布 OpenJDK 二进制，而 Eclipse Temurin 是它的延伸，提供更好的稳定性
FROM eclipse-temurin:21-jdk-alpine

## 创建目录，并使用它作为工作目录
RUN mkdir -p /app
WORKDIR /app
## 将后端项目的 Jar 文件，复制到镜像中
COPY ./target/*.jar app.jar

## 设置 TZ 时区
ENV TZ=Asia/Shanghai
## 设置 JAVA_OPTS 环境变量，可通过 docker run -e "JAVA_OPTS=" 进行覆盖
ENV JAVA_OPTS="-Xms512m -Xmx512m -Djava.security.egd=file:/dev/./urandom"

## 应用参数
ENV ARGS=""

EXPOSE 20263

## 启动后端项目
CMD java ${JAVA_OPTS} -jar app.jar $ARGS