FROM openjdk:8-alpine

RUN set -ex && \
    apk upgrade --no-cache && \
    apk add --no-cache bash tini libc6-compat linux-pam && \
    mkdir -p /opt/spark && \
    mkdir -p /opt/spark/work-dir && \
    touch /opt/spark/RELEASE && \
    rm /bin/sh && \
    ln -sv /bin/bash /bin/sh && \
    echo "auth required pam_wheel.so use_uid" >> /etc/pam.d/su && \
    chgrp root /etc/passwd && chmod ug+rw /etc/passwd

RUN wget http://mirror.easyname.ch/apache/spark/spark-2.4.0/spark-2.4.0-bin-hadoop2.7.tgz
RUN tar -xzf spark-2.4.0-bin-hadoop2.7.tgz -C /opt/
RUN rm spark-2.4.0-bin-hadoop2.7.tgz

RUN cp /opt/spark-2.4.0-bin-hadoop2.7/kubernetes/dockerfiles/spark/entrypoint.sh /opt/
COPY SVM.jar /SVM.jar
ENV SPARK_HOME /opt/spark-2.4.0-bin-hadoop2.7

WORKDIR /opt/spark-2.4.0-bin-hadoop2.7/work-di

ENTRYPOINT [ "/opt/entrypoint.sh" ]
