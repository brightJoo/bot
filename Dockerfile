# ----------------------------------------------------
# Stage 1: Build (빌드 환경)
# ----------------------------------------------------
# Spring Boot 3.x은 Java 17 이상을 사용해야 합니다.
FROM openjdk:21-jdk-slim AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper 및 소스 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .
COPY src src

# 빌드 실행
# --no-daemon을 사용하여 빌드 속도를 높이고, JAR 파일을 생성합니다.
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# ----------------------------------------------------
# Stage 2: Run (실행 환경)
# ----------------------------------------------------
# Alpine 또는 OpenJDK JRE-slim을 사용하여 최종 이미지 크기를 최소화합니다.
FROM openjdk:21-jre-slim

# 환경 변수 설정
ENV TZ=Asia/Seoul

# 최종 JAR 파일을 복사합니다. (실제 JAR 파일 이름에 맞춰 수정하세요)
# /app/build/libs/binance-bot-0.0.1-SNAPSHOT.jar 형태로 생성되었다고 가정
COPY --from=builder /app/build/libs/*.jar app.jar

# Spring Batch는 24시간 서비스로 구동되어야 하므로 ENTRYPOINT로 실행합니다.
# 애플리케이션 시작 시 DB 설정을 위해 h2-console을 열지 않도록 명령어를 조정합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]

# ECS Fargate에서 이 컨테이너를 띄우고, JobScheduler의 @Scheduled가 작동하게 됩니다.