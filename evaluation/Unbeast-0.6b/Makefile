CFLAGS=$(shell gcc compilerOptionGenerator.c -o /tmp/BFAbstractionCompilerOptionsProducer-$(USER);/tmp/BFAbstractionCompilerOptionsProducer-$(USER))

default:
	g++ -O2 -DNDEBUG -c $(CFLAGS) -Icudd/include -I/usr/include/libxml2 unbeast.cpp
	g++ unbeast.o -Lcudd/cudd -Lcudd/dddmp -Lcudd/epd -Lcudd/mtr -Lcudd/sis -Lcudd/st -Lcudd/util -ldddmp -lcudd -lepd -lmtr -lst -lutil -lxml2 -lboost_iostreams -o unbeast

