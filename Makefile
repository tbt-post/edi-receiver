compile:
	lein uberjar

run:
	java -jar `ls target/*-standalone.jar`

run-sync:
	java -jar `ls target/*-standalone.jar` --sync --autoinit-tables


docker-build:
	docker build -t edi-receiver-pg -f docker/Dockerfile.pg .

docker-run: docker-build
	docker run -it edi-receiver-pg

docker-build-mysql:
	docker build -t edi-receiver-mysql -f docker/Dockerfile.mysql .

docker-run-mysql: docker-build-mysql
	docker run -it edi-receiver-mysql

clean:
	lein clean

dist-clean: clean
	$(RM) -r target
