FROM registry.access.redhat.com/ubi8/openjdk-21

WORKDIR /opt/app

USER root

COPY ./server/build/libs/server-0.0.1-SNAPSHOT.jar app.jar

RUN groupadd --gid 1001 javauser && useradd --uid 1001 --gid 1001 -m javauser

RUN chown -R javauser:javauser .

USER javauser

ENTRYPOINT ["/usr/lib/jvm/java-21/bin/java", "-jar", "-XX:MaxRAMPercentage=85", "-XX:MinRAMPercentage=85", "/opt/app/app.jar"]
