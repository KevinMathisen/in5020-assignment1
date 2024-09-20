# IN5020 Assignment 1: Distributed Application User Guide

## Overview

This user guide explains how to compile and run the distributed application, including the server, proxy, and client components.
The application accepts delays and caching through command-line flags.

## Prerequisites

- **Java Development Kit (JDK) 8** 
- **Apache Maven** 

  

 **Compile the project:**
Before running the application, the project needs to be compiled and packaged.
Run the following Maven command in the root directory of the project to clean and package the application into a JAR file:

          mvn clean package


**To run the server:**
The server can be configured with different delays (20ms or 50ms) and caching options (no cache, server cache, or client cache).
Use the following command to start the server:

         java -cp target/solution-1.0-SNAPSHOT.jar com.ass1.server.Server --delay <50|20> --cache <|server|client>


**To run the proxy:**

          java -cp target/solution-1.0-SNAPSHOT.jar com.ass1.proxy.Proxy


**To run the client:**

          java -cp target/solution-1.0-SNAPSHOT.jar com.ass1.client.Client --delay <50|20> --cache <|server|client>
