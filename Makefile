run-dist:
	make -C app run-dist
	
build:
	make -C app build

start:
	make -C app start
	
start-dist:
	make -C app start-dist	
	
generate-migrations:
	make -C app generate-migrations	
	
test:
	make -C app test	
	
report:
	make -C app report
	
.PHONY: build
