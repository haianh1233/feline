FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY ./config /app/config
COPY ./target/feline-1.0-SNAPSHOT.jar /app/myapp.jar

ENTRYPOINT ["java", "-jar", "/app/myapp.jar"]