# CS335-Networks

Exercises for course CS335-Networks and Applications, Spring Semester 2022.


## Description
Assignments for CS335-Networks and Applications course, from Computer Science Department of the University of Crete, taught by professor Kostantinos Magkoutis. 


### Assignment 1

In this assignment, we simply had to modify the provided code which establishes a communication between a client application and a Web Server. It is a toy example and mostly for educational purposes, while providing a simple taste of Web programming.

### Assignment 2

Now this is where things get interesting. Assignment 2 creates a grid of user-configured servers, where a client may request to store a file and its contents on the grid, as well as retrieve it later on. Inner server communication includes constant health checks, serving a join request whenever a new server wants to join the grid, as well as updating the server list of every node in the grid and whenever a client requests a file that 1 server node does not have, the server node must communicate with the rest of the grid and look up for the file. 

Each request is assigned with an incremental ID for debugging purposes and better understanding. Threads are also used to serve requests and make periodic health checks on the grid. Prints originating from the request serving thread include the request ID, while prints originating from the health checking thread include the keyword "Doctor" on the very beginning of the message.

This assignment, unlike the 1st one was built from scratch by me. Debugging was pretty fun since you had to handle several different processes, and each process had 2 different threads other than the main one. 


## Usage

Build this project using the provided Makefile in a bash shell. 

```console
foo@bar:.../cs335-networks/Ex2$ make
```
The make file provides sufficient info on how to create the server grid. Note that you configure the client/each server node via command line arguments when executing the equivalent .class file. Please make sure to provide sufficient arguments as OutOfBounds exceptions will be thrown if some of them are missing.

After creating a server grid (localhost or not), to execute the client and make a request use:

```console
foo@bar:.../Ex2/bin$ java WebClient -p <port> -ip <host> -request <REQUEST> <filepath>
```
<REQUEST> can either be "GET" or "PUT", e.g.

```console
foo@bar:.../Ex2/bin$ java WebClient -p 4333 -request PUT test.txt
```
If -ip param is not included then the used host will be localhost.

## Roadmap

Need to refactor the 1st assignment - currently to configure the connecting host and port you have to change the code, which is obviously a bad practice.

In assignment 2, need to add an optional configuration for the health checking period (essentially the sleep time of the health checking thread)


## Authors and acknowledgment

As mentioned above, the 1st assignment was mainly built by the course's TAs while also including a code example from Networking: A Top-Down Approach Featuring the Internet, second edition (copyright 1996-2002 J.F Kurose and K.W. Ross, All Rights Reserved) and was modified by me so it works on a loop. The 2nd assignment was built from scratch by me. 


