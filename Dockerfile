FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system ecommerce && useradd --system --gid ecommerce ecommerce && mkdir -p /app/data && chown ecommerce:ecommerce /app/data
COPY --from=build /build/target/ecommerce-1.0.0.jar app.jar
USER ecommerce
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
