FROM openjdk:21-jdk as builder

RUN microdnf install findutils

ADD ./ /usr/share/sources

WORKDIR /usr/share/sources

RUN ./gradlew :server:build

FROM openjdk:21-jdk

WORKDIR /opt/app

COPY --from=builder /usr/share/sources/server/build/libs/server-0.0.1-SNAPSHOT.jar app.jar

RUN groupadd --gid 1000 javauser && useradd --uid 1000 --gid 1000 -m javauser

RUN chown -R javauser:javauser .

USER javauser

ENTRYPOINT ["/usr/java/openjdk-21/bin/java", "-jar", "-XX:MaxRAMPercentage=85", "-XX:MinRAMPercentage=85", "/opt/app/app.jar"]
