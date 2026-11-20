FROM eclipse-temurin:25-jdk-noble

WORKDIR /opt/app

USER root

COPY ./mass-index-creator/build/libs/mass-index-creator-0.0.1-SNAPSHOT.jar app.jar
COPY ./mass-index-creator/job_script.sh script.sh
COPY ./mass-index-creator/periodic_script.sh periodic_script.sh

RUN groupadd --gid 1001 javauser && useradd --uid 1001 --gid 1001 -m javauser

RUN chown -R javauser:javauser .
RUN chmod +x /opt/app/script.sh /opt/app/periodic_script.sh

USER javauser

ENTRYPOINT ["/opt/app/periodic_script.sh"]
