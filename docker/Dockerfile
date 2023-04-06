FROM navikt/java:common AS java-common

FROM eclipse-temurin:17-alpine

RUN umask o+r

RUN addgroup -S apprunner && adduser -S apprunner -G apprunner

RUN apk add --no-cache bash
COPY --chown=apprunner:root --from=java-common /init-scripts /init-scripts
COPY --chown=apprunner:root --from=java-common /run-java.sh /run-java.sh
COPY --from=java-common /entrypoint.sh /entrypoint.sh
COPY --from=java-common /dumb-init /dumb-init

RUN apk add tzdata
RUN cp /usr/share/zoneinfo/Europe/Oslo /etc/localtime
RUN echo "Europe/Oslo" > /etc/timezone

ENV LC_ALL="nb_NO.UTF-8"
ENV LANG="nb_NO.UTF-8"
ENV TZ="Europe/Oslo"
ENV APP_BINARY=app
ENV APP_JAR=app.jar
ENV MAIN_CLASS="Main"
ENV CLASSPATH="/app/WEB-INF/classes:/app/WEB-INF/lib/*"

WORKDIR /app
COPY build/libs/*.jar ./

EXPOSE 8080

RUN chown -R apprunner /app
USER apprunner

ENTRYPOINT ["/dumb-init", "--", "/entrypoint.sh"]


