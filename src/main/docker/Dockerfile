FROM java:openjdk-8-alpine
VOLUME /tmp
ADD ch-gateway-1.0.1-SNAPSHOT.jar app.jar
RUN sh -c 'touch /app.jar'
ENV JAVA_OPTS="-Dspring.profiles.active=prod -Dnacos.namespace=d9c61a97-f53c-49a7-ac26-7aae03e08c1a"
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
