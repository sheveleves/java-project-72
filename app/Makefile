run-dist:
	./build/install/app/bin/app
	
build:
	./gradlew clean build
	
report:
	./gradlew jacocoTestReport

start:
	APP_ENV=development ./gradlew run

start-dist:
	APP_ENV=production ./build/install/app/bin/app

generate-migrations:
	./gradlew generateMigrations

test:
	./gradlew test

.PHONY: build
