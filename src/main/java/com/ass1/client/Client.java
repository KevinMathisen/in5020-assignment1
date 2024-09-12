package com.ass1.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.ass1.server.ServerInterface;

public class Client {

    public static void main(String[] args) {
        try {
            // Connect to RMI registry at default port 1099
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface server = (ServerInterface) registry.lookup("server");

            // Path to the input file
            String inputFile = "src/main/resources/exercise_1_input.txt";
            String outputFile = "naive_server.txt"; // maybe different path?

            // Parse and execute queries
            List<Query> queries = parseInputFile(inputFile);
            executeQueries(queries, server, outputFile);

        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses an input file containing queries and converts them into Query
     * objets Each line of the input file should specify a method name,
     * arguments, and a zone. The method reads the input file line by line,
     * parses the information, and stores each query as a Query object in a
     * list.
     *
     * Expected input format: <method name> <arg1> <arg2> <arg3>
     * Zone:<zone number>
     * Example line in input file: getPopulationofCountry United States Zone:1
     *
     * @param inputFile The path to the input file containing the queries.
     * @return A list of Query objects created from the parsed input file.
     */
    private static List<Query> parseInputFile(String inputFile) {
        // List to store parsed queries
        List<Query> queries = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;

            // Loop through each line in the input file
            while ((line = br.readLine()) != null) {
                // Split the line into individual "parts" based on spaces
                String[] parts = line.split(" ");
                // The first part is the method name (e.g., getPopulationofCountry)
                String methodName = parts[0];
                // A list to store the arguments for the method (e.g., "United States")
                List<String> args = new ArrayList<>();
                // Variable to store the zone number (e.g., Zone:1 -> zone = 1)
                int zone = -1;

                // Start from the second part to capture arguments and zone number
                for (int i = 1; i < parts.length; i++) {
                    // If the part starts with "Zone:", extract the zone number
                    if (parts[i].startsWith("Zone:")) {
                        // Extract the number after "Zone:" and convert it to an integer
                        zone = Integer.parseInt(parts[i].substring(5));
                    } else {
                        // Otherwise, add the part to the arguments list
                        args.add(parts[i]);
                    }
                }
                // Create a new Query object with the method name, arguments, and zone
                // and add it to the list of queries
                queries.add(new Query(methodName, args, zone));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return queries;
    }

    /**
     * A class representing a single query parsed from the input file. A Query
     * consists of a method name, a list of arguments, and a zone.
     */
    static class Query {

        String methodName;
        List<String> args;
        int zone;

        /**
         * Constructor to initialize a Query object.
         *
         * @param methodName The name of the method (e.g.,
         * getPopulationofCountry).
         * @param args The list of arguments for the method.
         * @param zone The zone number associated with the query.
         */
        Query(String methodName, List<String> args, int zone) {
            this.methodName = methodName;
            this.args = args;
            this.zone = zone;
        }

        /**
         * Overrides the default toString method to provide a string
         *
         * @return A string representation of the Query object.
         */
        @Override
        public String toString() {
            return "Query{"
                    + "methodName='" + methodName + '\''
                    + ", args=" + args
                    + ", zone=" + zone
                    + '}';
        }
    }

    /**
     * Executes the list of queries by invoking the appropriate method on the
     * server, records the turnaround, execution, and waiting times, and logs
     * the results to an output file. Also, it tracks stats (like average, min,
     * and max times) for each method type.
     *
     * @param queries A list of Query objects that represent the parsed queries.
     * @param server The RMI server interface that allows for remote method
     * invocation.
     * @param outputFile The path to the output file where results and stats
     * will be logged.
     */
    private static void executeQueries(List<Query> queries, ServerInterface server, String outputFile) {
        // Create a map to store stats for each method
        HashMap<String, TaskStats> methodStats = new HashMap<>();

        // Try-with-resources to automatically close the FileWriter after usage 
        try (FileWriter writer = new FileWriter(outputFile)) {
            for (Query query : queries) {
                // Measure the total turnaround time, starting from the beginning of the query execution
                long startTurnaroundTime = System.currentTimeMillis();
                long startExecutionTime = 0;
                long endExecutionTime = 0;

                // Stores the result of the method invocation
                int result = 0;

                // Executing remote method invocation based on the method name
                switch (query.methodName) {
                    case "getPopulationofCountry":
                        // Measure execution start time for the specific method
                        startExecutionTime = System.currentTimeMillis();
                        // Call the server-side method getPopulationofCountry
                        result = server.getPopulationofCountry(query.args.get(0));
                        // Measure execution end time after getting the result from the server
                        endExecutionTime = System.currentTimeMillis();
                        break;

                    case "getNumberofCities":
                        startExecutionTime = System.currentTimeMillis();
                        // Parse minimum population from the query's arguments
                        int minPop = Integer.parseInt(query.args.get(1));
                        // Call the server-side method getNumberofCities
                        result = server.getNumberOfCities(query.args.get(0), minPop);  // e.g., "Norway"
                        endExecutionTime = System.currentTimeMillis();
                        break;

                    case "getNumberofCountries":
                        startExecutionTime = System.currentTimeMillis();

                    // ++ resterende methods ...
                }

                // Log the result and time metrics for the query
                logResult(writer, query, result, startTurnaroundTime, startExecutionTime, endExecutionTime);

                // Update stats for the current method type (turnaround, execution, waiting times)
                long endTurnaroundTime = System.currentTimeMillis();
                long turnaroundTime = endTurnaroundTime - startTurnaroundTime;
                long executionTime = endExecutionTime - startExecutionTime;
                long waitingTime = turnaroundTime - executionTime;

                // Record the stats for this method type
                updateStats(methodStats, query.methodName, turnaroundTime, executionTime, waitingTime);

                // Simulate delay between queries (either 50ms or 20ms) - two threads???
                Thread.sleep(50);
            }
            // After all queries are processed, log the final stats for each method type
            logFinalStats(writer, methodStats);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Logs the result of a query execution and its timing metrics to the output
     * file.
     *
     * @param writer The FileWriter used to log the data to the output file.
     * @param query The Query object that contains the method name, arguments,
     * and zone.
     * @param result The result of the method invocation (e.g., population,
     * number of cities).
     * @param startTurnaroundTime The time at which the query execution started.
     * @param startExecutionTime The time at which the remote method execution
     * started.
     * @param endExecutionTime The time at which the remote method execution
     * ended.
     * @throws IOException If there is an error writing to the output file.
     */
    private static void logResult(FileWriter writer, Query query, int result, long startTurnaroundTime, long startExecutionTime, long endExecutionTime) throws IOException {
        long endTurnaroundTime = System.currentTimeMillis();  // The time after the query result is obtained
        long turnaroundTime = endTurnaroundTime - startTurnaroundTime;  // Total time taken for the query
        long executionTime = endExecutionTime - startExecutionTime;  // Time taken by the server to execute the query
        long waitingTime = turnaroundTime - executionTime;  // Time spent waiting (turnaround - execution)

        // Log the result and time metrics in the spesific format
        writer.write(String.format("%d %s (turnaround time: %d ms, execution time: %d ms, waiting time: %d ms, processed by Server Zone:%d)\n",
                result, query.methodName, turnaroundTime, executionTime, waitingTime, query.zone));
    }

    /**
     * Updates the stats (turnaround, execution, waiting times) for each method
     * type.
     *
     * @param methodStats A map that stores stats for each method type (method
     * name -> TaskStats).
     * @param methodName The name of the method whose stats are being updated.
     * @param turnaroundTime The total time taken for the query (turnaround
     * time).
     * @param executionTime The time taken by the server to execute the query
     * (execution time).
     * @param waitingTime The time the query spent waiting before being
     * processed (waiting time).
     */
    private static void updateStats(HashMap<String, TaskStats> methodStats, String methodName, long turnaroundTime, long executionTime, long waitingTime) {
        // Retrieve the current stats for this method type or create a new TaskStats if none exist
        TaskStats stats = methodStats.getOrDefault(methodName, new TaskStats());
        // Add the new task metrics to the stats
        stats.addTask(turnaroundTime, executionTime, waitingTime);
        // Update the map with the new stats
        methodStats.put(methodName, stats);
    }

    /**
     * Logs the final stats for all method types to the output file after all
     * queries are processed.
     *
     * @param writer The FileWriter used to log the final stats to the output
     * file.
     * @param methodStats A map that contains the stats for each method type
     * (method name -> TaskStats).
     * @throws IOException If there is an error writing to the output file.
     */
    private static void logFinalStats(FileWriter writer, HashMap<String, TaskStats> methodStats) throws IOException {
        // Iterate over each method type and log its stats (average, min, max times)
        for (String methodName : methodStats.keySet()) {
            TaskStats stats = methodStats.get(methodName);
            writer.write(String.format("%s avg turn-around time: %d ms, avg execution time: %d ms, avg waiting time: %d ms, min turn-around time: %d ms, max turn-around time: %d ms\n",
                    methodName, stats.getAverageTurnaroundTime(), stats.getAverageExecutionTime(), stats.getAverageWaitingTime(),
                    stats.getMinTurnaroundTime(), stats.getMaxTurnaroundTime()));
        }
    }

    /**
     * A helper class to track stats (turnaround, execution, waiting times) for
     * each method type.
     */
    static class TaskStats {

        private long totalTurnaroundTime = 0;  // Total turnaround time for all tasks of this method type
        private long totalExecutionTime = 0;  // Total execution time for all tasks of this method type
        private long totalWaitingTime = 0;  // Total waiting time for all tasks of this method type
        private long minTurnaroundTime = Long.MAX_VALUE;  // Minimum turnaround time observed
        private long maxTurnaroundTime = Long.MIN_VALUE;  // Maximum turnaround time observed
        private int count = 0;  // Number of tasks executed for this method type

        /**
         * Adds a new task's metrics to the stats for this method type.
         *
         * @param turnaroundTime The total time taken for the task (turnaround
         * time).
         * @param executionTime The time taken by the server to execute the task
         * (execution time).
         * @param waitingTime The time the task spent waiting before being
         * processed (waiting time).
         */
        public void addTask(long turnaroundTime, long executionTime, long waitingTime) {
            totalTurnaroundTime += turnaroundTime;
            totalExecutionTime += executionTime;
            totalWaitingTime += waitingTime;
            count++;

            // Update the minimum and maximum turnaround times
            if (turnaroundTime < minTurnaroundTime) {
                minTurnaroundTime = turnaroundTime;
            }
            if (turnaroundTime > maxTurnaroundTime) {
                maxTurnaroundTime = turnaroundTime;
            }
        }

        // Getters for stats
        public long getAverageTurnaroundTime() {
            return count > 0 ? totalTurnaroundTime / count : 0;
        }

        public long getAverageExecutionTime() {
            return count > 0 ? totalExecutionTime / count : 0;
        }

        public long getAverageWaitingTime() {
            return count > 0 ? totalWaitingTime / count : 0;
        }

        public long getMinTurnaroundTime() {
            return minTurnaroundTime;
        }

        public long getMaxTurnaroundTime() {
            return maxTurnaroundTime;
        }
    }
}