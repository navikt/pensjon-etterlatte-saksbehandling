FROM gcr.io/distroless/java17
ENV TZ="Europe/Oslo"
WORKDIR /app
COPY build/libs/*.jar ./
ENV CLASSPATH="/app/WEB-INF/classes:/app/WEB-INF/lib/*"
EXPOSE 8080
USER nonroot
CMD ["app.jar"]