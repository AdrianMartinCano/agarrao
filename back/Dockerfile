# syntax=docker/dockerfile:1
### Etapa 1: build con Maven ###
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# settings.xml con el token de GitHub Packages (read:packages) para resolver spring-*
# El token se inyecta como secret/arg de build en Coolify.
# OJO Coolify: GITHUB_TOKEN debe estar marcado como "Build Variable" o llega vacío.
ARG GITHUB_ACTOR=adrianmartincano
ARG GITHUB_TOKEN
RUN test -n "$GITHUB_TOKEN" || { echo "ERROR: GITHUB_TOKEN vacío en build. Márcalo como Build Variable en Coolify."; exit 1; }
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>github-adrianmartincano</id>
      <username>${GITHUB_ACTOR}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

# Dependencias primero (mejor caché de capa).
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository mvn -B -q clean package -DskipTests

### Etapa 2: runtime ###
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# TOPE DE MEMORIA. Sin esto, la JVM se reserva por su cuenta hasta el 25% de la RAM que
# ve —en el VPS, unos 1,9 GB— y NO la devuelve: el recolector la pide y se la queda. Es
# memoria que se le quita a los demás proyectos del servidor aunque esta app no la use.
#
# MaxRAMPercentage se aplica sobre el límite del CONTENEDOR (ver docker-compose.yml), no
# sobre la RAM de la máquina, así que los dos números van juntos: sin el límite de allí,
# este porcentaje se calcula sobre los 7,7 GB del host y no sirve de nada.
#
# ExitOnOutOfMemoryError: si aun así se queda sin memoria, mejor morir y reiniciar que
# quedarse vivo respondiendo a medias.
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
