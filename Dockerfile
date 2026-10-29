# syntax=docker/dockerfile:1

# ---- Build: compila o jar com o Maven Wrapper ----
FROM eclipse-temurin:25.0.4.1_1-jdk-noble AS build
WORKDIR /build

# Dependencias primeiro: esta camada so e refeita quando o pom.xml ou o wrapper mudam.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline

# Os testes rodam com ./mvnw clean package fora da imagem, nao no build dela.
COPY src src
RUN ./mvnw -B clean package -DskipTests \
    && java -Djarmode=tools -jar target/peca-backend-*.jar extract --layers \
        --destination target/extracted --application-filename app.jar

# ---- Runtime: JRE, usuario sem privilegios ----
FROM eclipse-temurin:25.0.4.1_1-jre-noble

# A imagem base nao traz curl nem wget; o wget (~1 MB) e usado so pelo HEALTHCHECK.
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
# Jar em camadas: as dependencias (que mudam pouco) ficam em camadas separadas do codigo da aplicacao.
COPY --from=build /build/target/extracted/dependencies/ ./
COPY --from=build /build/target/extracted/spring-boot-loader/ ./
COPY --from=build /build/target/extracted/snapshot-dependencies/ ./
COPY --from=build /build/target/extracted/application/ ./

# Pasta das imagens (STORAGE_DIR no docker-compose.yml), gravavel pelo usuario da aplicacao.
RUN mkdir -p /dados/imagens && chown -R app:app /dados

USER app

# Sem segredos aqui: toda a configuracao (DB_*, JWT_*, MAIL_*, STORAGE_*, CORS_ALLOWED_ORIGINS) entra
# por variavel de ambiente em runtime. Definir JAVA_OPTS substitui as opcoes padrao abaixo.
ENV TZ=UTC \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null "http://localhost:${PORT:-8080}/actuator/health/readiness" || exit 1

# sh -c para expandir JAVA_OPTS; o exec faz o java virar o PID 1 e receber o SIGTERM do docker stop.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar \"$@\"", "--"]
