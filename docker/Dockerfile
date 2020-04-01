FROM ubuntu:bionic

RUN apt-get update && apt-get install -y wget gnupg default-jdk
RUN wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -
RUN sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ bionic-pgdg main" >> /etc/apt/sources.list.d/pgdg.list'
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y postgresql

RUN pg_ctlcluster 12 main start &&\
    su - postgres -c "psql --command \"ALTER USER postgres WITH PASSWORD 'postgres';\"" &&\
    su - postgres -c "psql --command \"CREATE DATABASE edi;\"" &&\
    pg_ctlcluster 12 main stop

RUN wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -P /bin
RUN chmod a+x /bin/lein
RUN lein upgrade

WORKDIR /opt/edi-receiver
COPY resources resources
COPY src src
COPY test test
COPY project.clj project.clj

RUN lein uberjar

EXPOSE 5432 8000

COPY docker/entrypoint.sh entrypoint.sh
RUN chmod a+x entrypoint.sh
ENTRYPOINT ["/opt/edi-receiver/entrypoint.sh"]
