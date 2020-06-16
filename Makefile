compile:
	lein build-all

run:
	java -jar target/edi-receiver-standalone.jar

run-sync:
	java -jar target/edi-control-standalone.jar --deploy && java -jar target/edi-receiver-standalone.jar --sync

docker-build:
	docker build -t edi-receiver-pg -f docker/Dockerfile.pg .

docker-run: docker-build
	docker run -it edi-receiver-pg

docker-build-mysql:
	docker build -t edi-receiver-mysql -f docker/Dockerfile.mysql .

docker-run-mysql: docker-build-mysql
	docker run -it edi-receiver-mysql

clean:
	$(RM) -r target
