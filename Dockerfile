FROM eclipse-temurin:21-jdk-alpine-3.20 AS build

WORKDIR /app

# Gradle wrapper와 빌드 스크립트 먼저 복사 (캐싱 최적화)
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Gradle 의존성 캐시를 활용하여 의존성 다운로드
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew dependencies --no-daemon -x test

# 소스 코드 복사 및 빌드
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon -x test

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