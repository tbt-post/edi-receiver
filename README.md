# edi-receiver

![Docker Image CI](https://github.com/tbt-post/edi-receiver/workflows/Docker%20Image%20CI/badge.svg?branch=trunk)
![Clojure CI](https://github.com/tbt-post/edi-receiver/workflows/Clojure%20CI/badge.svg?branch=trunk)

A clojure application designed to store EDI messages into local postgresql database.

## Running with docker

[Install docker](https://docs.docker.com/install/) first, then run 
```
sudo docker build -t edi-receiver -f docker/Dockerfile .
sudo docker run -it edi-receiver
```

## Installing clojure
```
sudo apt-get update 
sudo apt-get install wget default-jdk
sudo wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -P /bin
sudo chmod a+x /bin/lein
lein upgrade
```

## Installing postgresql
```
sudo apt-get update 
sudo apt-get install -y wget gnupg
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add - 
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt/ `lsb_release -cs`-pgdg main" >> /etc/apt/sources.list.d/pgdg.list'
sudo apt-get update
sudo apt-get install postgresql
```

## Creating empty database
```
su - postgres -c "psql --command \"ALTER USER postgres WITH PASSWORD 'postgres';\""
su - postgres -c "psql --command \"CREATE DATABASE edi;\""
```

## Running application 
Create tables, download schemas and run
```
java -jar `ls target/*-standalone.jar` --autoinit-tables --sync
```
Regular run, with schema caching  
```
java -jar `ls target/*-standalone.jar`
```

## Config
Config is java .properties file:

| parameter | type | description |
|---|---|---|
| upstream.topics | string  | comma separated list of topics to receive |
| upstream.cache | string | path where upstream schemas cached |
| upstream.sync | boolean | set true to validate upstream cache on startup |
| autoinit-tables | boolean | create tables on startup |
| api.host | string | http server ip address |
| api.port | integer | http server port, default is 8000 |
| api.auth.username | string | basic auth username, if auth required |
| api.auth.password | string | basic auth password, if auth required |
| pg.host | string | postgresql host, default is "localhost" |
| pg.port | integer | postgresql port, default is 5432 |
| pg.database | string | postgresql database, default is "edi" |
| pg.user | string | postgresql user |
| pg.password | string | postgresql password |
| pg.host | string | postgresql host |

See [edi-receiver.properties](resources/edi-receiver.properties) for defaults.
Check [src/edi_receiver/db/pg.clj](src/edi_receiver/db/pg.clj) for more postgresql options.

## Customizing config
```
cp resources/edi-receiver.properties local.properties
```
Then edit local.properties file and run
```
java -jar `ls target/*-standalone.jar` -c local.properties
```
Default properties will be updated wuth local.properties

Some config options can be customized from command line, see help:
```
java -jar `ls target/*-standalone.jar` --help
```

## Development

Execute (go) in repl to start devel profile with autoreload and local.properties config.

## License

Copyright © 2020 Kasta Group LLC

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
