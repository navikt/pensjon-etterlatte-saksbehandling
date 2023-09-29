FROM gcr.io/distroless/java17
ENV TZ="Europe/Oslo"
COPY build/libs/app.jar /app/app.jar
EXPOSE 8080
USER nonroot
WORKDIR /app
CMD ["app.jar"]