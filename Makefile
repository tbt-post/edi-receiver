compile:
	lein uberjar

run:
	java -jar `ls target/*-standalone.jar`

docker-build:
	docker build -t edi-receiver -f docker/Dockerfile .

docker-run: docker-build
	docker run -it edi-receiver
