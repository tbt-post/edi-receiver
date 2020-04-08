compile:
	lein uberjar

run:
	java -jar `ls target/*-standalone.jar`

run-sync:
	java -jar `ls target/*-standalone.jar` --sync --autoinit-tables


docker-build:
	docker build -t edi-receiver -f docker/Dockerfile .

docker-run: docker-build
	docker run -it edi-receiver
