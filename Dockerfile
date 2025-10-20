# ----------------------------------------------------
# Stage 1: Build (빌드 환경) - JAR 파일 생성
# ----------------------------------------------------
# openjdk:21-jdk-slim은 빌드 도구(javac 등)를 포함합니다.
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Gradle Wrapper 및 소스 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .
COPY src src

# 빌드 실행
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# ----------------------------------------------------
# Stage 2: Run (실행 환경) - 최소한의 JRE만 사용
# ----------------------------------------------------
# JRE 환경으로 전환하여 이미지 크기를 줄입니다.
# openjdk:21-jre-slim 태그가 불안정할 경우, 대신 jdk-slim을 다시 사용하고 빌드 툴을 삭제하여 최적화합니다.
# 하지만 여기서는 현재 공식적으로 존재하는 JRE slim 태그인 bullseye 기반을 사용합니다.
FROM openjdk:21-jre-slim-bullseye

# 환경 변수 설정
ENV TZ=Asia/Seoul

# 최종 JAR 파일을 복사합니다. (실제 JAR 파일 이름에 맞춰 수정하세요)
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Batch 서비스 실행
ENTRYPOINT ["java", "-jar", "app.jar"]