# edi-receiver

A Clojure application designed to store EDI messages into postgresql.

## Config

Config is java .properties file:

| parameter | type | description |
|---|---|---|
| upstream.topics | string  | comma separated list of topics to receive |
| upstream.cache | string | path where upstream schemas cached |
| upstream.sync | boolean | set true validate upstream cache on startup |
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

See [edi-receiver.properties](resources/edi-receiver.properties) for defaults

Check [src/edi_receiver/db/pg.clj](src/edi_receiver/db/pg.clj) for more postgresql options

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
