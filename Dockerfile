# ---------- Build stage (Gradle + JDK) ----------
FROM gradle:8.6-jdk21 AS build
WORKDIR /workspace

# 1) 의존성 캐시를 활용하려면 build.gradle / settings.gradle 먼저 복사
COPY build.gradle settings.gradle* ./

# 2) 만약 gradle.properties나 설정 파일이 있으면 추가 복사 (선택)
# COPY gradle.properties ./

# 3) 의존성만 먼저 다운로드 (캐시 활용)
RUN gradle --no-daemon dependencies || true

# 4) 소스 전체 복사
COPY src ./src
# 만약 멀티모듈이면 각 모듈의 build.gradle도 복사하는 방식으로 조정 필요

# 5) 빌드 (테스트 제외)
RUN gradle --no-daemon clean assemble -x test

# ---------- Runtime stage (lightweight JRE) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# change this path according to your Gradle output (e.g. build/libs/*.jar)
COPY --from=build /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
