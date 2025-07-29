FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon -x test

COPY src ./src
RUN gradle bootJar --no-daemon -x test

RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination trading-app

FROM eclipse-temurin:21-jre-alpine-3.20

ARG ID=1001
RUN addgroup --gid ${ID} javauser && \
    adduser --uid ${ID} --ingroup javauser --no-create-home --disabled-password javauser

WORKDIR /app

ARG LAYERED_JAR=/app/trading-app
COPY --from=build ${LAYERED_JAR}/dependencies/ ./
COPY --from=build ${LAYERED_JAR}/spring-boot-loader/ ./
COPY --from=build ${LAYERED_JAR}/snapshot-dependencies/ ./
COPY --from=build ${LAYERED_JAR}/application/ ./

EXPOSE 8082

USER javauser

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "org.springframework.boot.loader.launch.JarLauncher"]