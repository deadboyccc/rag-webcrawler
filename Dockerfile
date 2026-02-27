FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN ./mvnw -q -e -B -DskipTests package || mvn -q -e -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine

ENV JAVA_OPTS="-Xms256m -Xmx512m"
WORKDIR /app

COPY --from=build /app/target/rag-webcrawler-*-SNAPSHOT-shaded.jar /app/app.jar

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar $*"]

