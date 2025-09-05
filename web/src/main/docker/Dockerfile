FROM openjdk:8-alpine as builder
WORKDIR application
ARG JAR_FILE=./app.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract


FROM openjdk:8-alpine
COPY --from=hengyunabc/arthas:latest /opt/arthas /opt/arthas

MAINTAINER zhimin (zhimin711@gmail.com)

#ADD apache-skywalking-java-agent-8.12.0.tgz .

WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./

EXPOSE  8080

ENV JAVA_OPTS=""
#-javaagent:/skywalking-agent/skywalking-agent.jar
ENV AGENT_OPTS=""

ENTRYPOINT ["sh","-c","exec java ${JAVA_OPTS} ${JAVA_AGENT} -Djava.ext.dirs=$JAVA_HOME/jre/lib/ext:./skywalking-agent/ org.springframework.boot.loader.JarLauncher"]
