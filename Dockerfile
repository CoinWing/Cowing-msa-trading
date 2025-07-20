FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
RUN gradle dependencies --no-daemon --build-cache

COPY src ./src
RUN gradle bootJar -x test --no-daemon --build-cache

RUN java -Djarmode=tools -jar build/libs/*.jar extract --layers --launcher --destination trading-app

FROM eclipse-temurin:21-jre-alpine-3.20

ARG ID=1001
RUN addgroup --gid ${ID} javauser && \
    adduser --uid ${ID} --ingroup javauser --no-create-home --disabled-password javauser

WORKDIR /app

ARG LAYERED_JAR=/app/trading-app
COPY --from=build --chown=javauser:javauser ${LAYERED_JAR}/dependencies/ ./
COPY --from=build --chown=javauser:javauser ${LAYERED_JAR}/spring-boot-loader/ ./
COPY --from=build --chown=javauser:javauser ${LAYERED_JAR}/snapshot-dependencies/ ./
COPY --from=build --chown=javauser:javauser ${LAYERED_JAR}/application/ ./

RUN chmod -R 555 /app

EXPOSE 8082

USER javauser

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "org.springframework.boot.loader.launch.JarLauncher"]