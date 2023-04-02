FROM busybox as java-common

RUN wget -O /dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.1/dumb-init_1.2.1_amd64
RUN echo "057ecd4ac1d3c3be31f82fc0848bf77b1326a975b4f8423fe31607205a0fe945  /dumb-init" | sha256sum -c
RUN chmod +x /dumb-init

COPY init-scripts/ /init-scripts
COPY entrypoint.sh /entrypoint.sh
COPY run-java.sh /run-java.sh

#FROM navikt/java:common AS java-common
FROM eclipse-temurin:17-jre

RUN umask o+r

RUN groupadd -r --gid 1069 apprunner && useradd -r --uid 1069 -g apprunner apprunner

COPY --chown=apprunner:root --from=java-common /init-scripts /init-scripts
COPY --chown=apprunner:root --from=java-common /run-java.sh /run-java.sh
COPY --from=java-common /entrypoint.sh /entrypoint.sh
COPY --from=java-common /dumb-init /dumb-init

RUN apt-get update && apt-get install -y wget locales

RUN sed -i -e 's/# nb_NO.UTF-8 UTF-8/nb_NO.UTF-8 UTF-8/' /etc/locale.gen && locale-gen
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


