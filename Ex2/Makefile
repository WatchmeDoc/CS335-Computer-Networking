
###################################################
#
# file: Makefile
#
# @Author:   Georgios Manos
# @Version:  25-03-2022
# @email:    csd4333@csd.uoc.gr
#
# Makefile
#
####################################################
all: Main


Main:
	@rm -rf bin && cd src/ && javac Main.java && mkdir ../bin && cp *.class ../bin/ && cd ../
	@printf "\n-> Main.class successfully built in /bin\n"
	@printf "\n-> Execute using java Main -id N -p M\n"
	@printf "\n-> Params: N = server ID, any positive Integer\n\t   M = server port, should be >1000\n"
	@printf "\t   (optional) -ip <host> to define the host (either an IP or localhost)\n"
	@printf "\n-> To add another server to the grid add --connect <port> <host>,\n   where port and host the target server's port and host.\n"
	@printf "\ne.g.\n java bin/Main -id 1 -p 4333\n"
	@printf "\n(in a different terminal)\n java bin/Main -p 4334 -id 2 --connect 4333 localhost\n"

clean:
	@rm -rf bin
	@clear

