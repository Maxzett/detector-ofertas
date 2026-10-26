# --- Build -------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependencias primero: esta capa se cachea mientras no cambie el pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# chmod: en Windows se pierde el bit de ejecución al copiar
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# --- Runtime -----------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --uid 10001 --no-create-home --shell /usr/sbin/nologin app
USER app

COPY --from=build /app/target/detector-ofertas-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
