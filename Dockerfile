FROM gradle:jdk25-ubi AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./

RUN gradle dependencies --no-daemon

COPY src ./src

RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app

COPY --from=builder /app/build/libs/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]