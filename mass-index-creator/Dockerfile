FROM eclipse-temurin:25-jdk-noble

WORKDIR /opt/app

USER root

COPY ./mass-index-creator/build/libs/mass-index-creator-0.0.1-SNAPSHOT.jar app.jar

RUN groupadd --gid 1001 javauser && useradd --uid 1001 --gid 1001 -m javauser

RUN chown -R javauser:javauser .

USER javauser

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=85", "-XX:MinRAMPercentage=85", "-jar", "/opt/app/app.jar"]
