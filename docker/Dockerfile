FROM gcr.io/distroless/java17

ENV TZ="Europe/Oslo"

WORKDIR /app
COPY build/libs/app.jar app.jar

EXPOSE 8080
USER nonroot
CMD ["app.jar"]

