FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S apprunner && adduser -S apprunner -G apprunner
RUN chown -R apprunner /app
USER apprunner

ENV TZ="Europe/Oslo"

COPY build/libs/app.jar /app/app.jar
WORKDIR /app

EXPOSE 8080

CMD ["app.jar"]

