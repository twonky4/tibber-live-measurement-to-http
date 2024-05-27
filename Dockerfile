FROM curlimages/curl:8.8.0 AS downloader
ARG git_user
ARG git_token

RUN cd /home/curl_user &&\
    curl --insecure -sL -H "Authorization: token $git_token" -L https://api.github.com/repos/$git_user/tibber-live-measurement-to-http/tarball > tarball.tar.gz

FROM gradle:8.7.0-jdk21 AS builder
ARG git_user

COPY --from=downloader /home/curl_user/tarball.tar.gz /tarball.tar.gz

RUN cd / &&\
    tar xf tarball.tar.gz &&\
    rm tarball.tar.gz &&\
    mv $git_user-tibber-live-measurement-to-http* tibber-live-measurement-to-http &&\
    cd /tibber-live-measurement-to-http &&\
    gradle build -x test --no-daemon &&\
    mv build/libs/tibber-live-measurement-to-http-*-SNAPSHOT.jar tibber-live-measurement-to-http.jar

FROM openjdk:21-jdk as runner

COPY --from=builder /tibber-live-measurement-to-http/tibber-live-measurement-to-http.jar /tibber-live-measurement-to-http/tibber-live-measurement-to-http.jar

RUN echo "Europe/Berlin" > /etc/timezone

ENV TZ "Europe/Berlin"

WORKDIR /tibber-live-measurement-to-http

HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=5 CMD curl --fail --silent localhost:8080/actuator/health | grep UP || exit 1

CMD ["java", "-jar", "/tibber-live-measurement-to-http/tibber-live-measurement-to-http.jar"]
