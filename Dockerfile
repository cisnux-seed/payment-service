FROM gradle:jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon --stacktrace --info --console=plain --refresh-dependencies -x test

FROM eclipse-temurin:21-jre-alpine
ARG APP_DIR=app
WORKDIR /$APP_DIR
COPY --from=build /app/build/libs/*.jar payment.jar
ENV PROFILE_MODE=prod
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=$PROFILE_MODE -jar payment.jar"]