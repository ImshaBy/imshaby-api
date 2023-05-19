#сборка war
FROM maven:3.6.3-openjdk-8 as builder

COPY src /usr/src/app/src

COPY pom.xml /usr/src/app

RUN mvn -f /usr/src/app/pom.xml clean package


#сборка образа с томкатом и war, получившейся на стадии builder
FROM tomcat:9-jdk8-temurin

COPY --from=builder /usr/src/app/target/imshaby-api-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

#дальше запуск сборки образа
# docker build -t imshaby-api:0.1 .
#и непосредственно сам запуск контейнера (в примере активация профиля qa)
# docker run -it -e "SPRING_PROFILES_ACTIVE=qa" -p 80:8080 --name imshaby-api-qa imshaby-api:0.1