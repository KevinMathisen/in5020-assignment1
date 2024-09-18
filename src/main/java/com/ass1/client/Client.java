package com.ass1.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ass1.proxy.ProxyInterface;
import com.ass1.server.Response;
import com.ass1.server.ServerInterface;

public class Client {

    public static void main(String[] args) {
        try {
            // Default values for delay, input, output file and cache type
            int delay = 50;
            String inputFile = "src/main/resources/com/ass1/client/data/exercise_1_input.txt";
            String outputFile = "naive.txt";
            String cacheType = "";
            Cache cache = new Cache(45);

            // Create a map to store stats for each method
            HashMap<String, TaskStats> methodStats = new HashMap<>();
            
            List<Thread> threads = new ArrayList<>();

            // Parse command-line arguments
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--delay") && i + 1 < args.length) {
                    delay = Integer.parseInt(args[i + 1]);
                }
                if (args[i].equals("--cache") && i + 1 < args.length) {
                    cacheType = args[i + 1];
                } else if (args[i].equals("--output") && i + 1 < args.length) {
                    outputFile = args[i + 1];
                }
            }

            final String finalCacheType = cacheType;

            // Change output file based on cache type
            if (outputFile.equals("naive_server.txt")) {
                switch (cacheType) {
                    case "server_cache":
                        outputFile = "server_cache.txt";
                        break;
                    case "client_cache":
                        outputFile = "client_cache.txt";
                        break;
                    default:
                        outputFile = "naive.txt";
                        break;
                }
            }

            try (FileWriter writer = new FileWriter(outputFile)) {

                // Connect to RMI registry at default port 1099 and search for the proxy from that registry
                Registry registry = LocateRegistry.getRegistry();
                ProxyInterface proxy = (ProxyInterface) registry.lookup("Proxy"); // Assume 'Proxy' is registered with this name
                
                // Parse and execute queries
                List<Query> queries = parseInputFile(inputFile);
                
                // Loop through each query, get the available server for the querys zone, and execute the query
                for (Query query : queries) {

                    ServerInterface server = proxy.getAvailableServer(query.zone);
                    
                    // Execute the query on a new thread
                    Thread thread = new Thread(() -> {
                        try {
                            executeQueries(query, server, writer, finalCacheType.equals("client_cache"), cache, methodStats);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    thread.start();
                    threads.add(thread);
                    
                    Thread.sleep(delay);
                }
                
                // wait for threads to finish
                for (Thread thread : threads) {
                    thread.join();
                }
                
                // After all queries log the final stats for each method type
                logFinalStats(writer, methodStats);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses an input file containing queries and converts them into Query
     * objets Each line of the input file should specify a method name,
     * arguments, and a zone. The method reads the input file line by line,
     * parses the information, and stores each query as a Query object in a
     * list. Expected input format: <method name> <arg1> <arg2> <arg3>
     * Zone:<zone number>
     *
     * @param inputFile The path to the input file containing the queries.
     * @return A list of Query objects created from the parsed input file.
     */
    private static List<Query> parseInputFile(String inputFile) {
        List<Query> queries = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;

            while ((line = br.readLine()) != null) {
                int zoneIndex = line.indexOf("Zone:");
                if (zoneIndex == -1) {
                    continue; // Skip lines without zone information
                }

                int zone = Integer.parseInt(line.substring(zoneIndex + 5).trim());  // Extracts the zone number safely

                // The rest of the line without the zone part
                String methodAndArgs = line.substring(0, zoneIndex).trim();

                String[] parts = methodAndArgs.split(" ", 2);  // Split into method name and the arguments as one string
                String methodName = parts[0];
                List<String> args = new ArrayList<>();
                if (parts.length > 1) {
                    String arguments = parts[1];
                    // Handle arguments that could be multiple words
                    args.addAll(Arrays.asList(arguments.split(
                            " (?=\\d)")));  // Splits by space before a number
                }

                queries.add(new Query(methodName, args, zone, line));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return queries;
    }

    /**
     * Executes list of queries by invoking the appropriate method on the
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
    private static void executeQueries(Query query, ServerInterface server, FileWriter writer, boolean clientCacheEnabled, Cache cache, HashMap<String, TaskStats> methodStats) {
        try {
            int clientZone = query.zone;
                
            // Measure the total turnaround time, starting from the beginning of the query execution
            long startTurnaroundTime = System.currentTimeMillis();

            // Stores the result of the method invocation
            Response result = new Response(-1, -1, -1, -1);

            // Use a local client cache if enabled
            if (clientCacheEnabled && cache.containsKey(query.toString())) {
                result = new Response(cache.get(query.toString()), 0, 0, 0);                    
            } else {
                // Executing remote method invocation based on the method name
                switch (query.getMethodName()) {
                    case "getPopulationOfCountry"   -> result = server.getPopulationOfCountry(query.args.get(0), clientZone);
                    case "getNumberofCities"        -> result = server.getNumberOfCities(query.args.get(0), Integer.parseInt(query.args.get(1)), clientZone);
                    case "getNumberofCountries1"    -> result = server.getNumberOfCountries(Integer.parseInt(query.args.get(0)), Integer.parseInt(query.args.get(1)), clientZone);
                    case "getNumberofCountries2"    -> result = server.getNumberOfCountries(Integer.parseInt(query.args.get(0)), Integer.parseInt(query.args.get(1)), Integer.parseInt(query.args.get(2)), clientZone);
                }
                if (clientCacheEnabled) {
                    cache.put(query.toString(), result.getResult());
                }
            }

            long turnaroundTime = System.currentTimeMillis() - startTurnaroundTime;

            // Log the result and time metrics for the query
            logResult(writer, query, result, turnaroundTime);

            // Record the stats for this method type
            updateStats(methodStats, query.getMethodName(), turnaroundTime, result.getExecutionTime(), result.getWaitingTime());
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
    private static void logResult(FileWriter writer, Query query, Response result, long turnaroundTime) throws IOException {
        // Log the result and time metrics in the spesific format
        synchronized (writer) {
            writer.write(String.format("%d %s (turnaround time: %d ms, execution time: %d ms, waiting time: %d ms, processed by Server Zone:%d)\n",
                    result.getResult(), query.toString(), turnaroundTime, result.getExecutionTime(), result.getWaitingTime(), result.getServerZone()));
        }
    }

    /**
     * Updates the stats (turnaround, execution, waiting times) for each method
     * type.
     *
     * @param methodStats A map that stores stats for each method type (method
     * name -> TaskStats).
     *
     */
    private static void updateStats(HashMap<String, TaskStats> methodStats, String methodName, long turnaroundTime, long executionTime, long waitingTime) {
        // Retrieve the current stats for this method type or create a new TaskStats if none exist
        TaskStats stats = methodStats.getOrDefault(methodName, new TaskStats());
        // Add the new task metrics to the stats
        stats.addTask(turnaroundTime, executionTime, waitingTime);
        // Update the map with the new stats
        methodStats.put(methodName, stats);
    }

    /* Logs the final stats for all method types to the output file after all queries are processed. */
    private static void logFinalStats(FileWriter writer, HashMap<String, TaskStats> methodStats) throws IOException {
        // Iterate over each method type and log its stats (average, min, max times)
        synchronized (writer) {
            for (String methodName : methodStats.keySet()) {
                TaskStats stats = methodStats.get(methodName);
                writer.write(String.format("%s avg turn-around time: %d ms, avg execution time: %d ms, avg waiting time: %d ms, min turn-around time: %d ms, max turn-around time: %d ms\n",
                        methodName, stats.getAverageTurnaroundTime(), stats.getAverageExecutionTime(), stats.getAverageWaitingTime(),
                        stats.getMinTurnaroundTime(), stats.getMaxTurnaroundTime()));
            }
    }
    }

    /**
     * A class representing a single query parsed from the input file. A Query
     * consists of a method name, a list of arguments, and a zone.
     */
    static class Query {
        String methodName;
        List<String> args;
        int zone;
        String queryString;

        /**
         * Constructor to initialize a Query object.
         *
         * @param methodName The name of the method (e.g.,
         * getPopulationofCountry).
         * @param args The list of arguments for the method.
         * @param zone The zone number associated with the query.
         */
        Query(String methodName, List<String> args, int zone, String queryString) {
            this.methodName = methodName;
            this.args = args;
            this.zone = zone;
            this.queryString = queryString;
        }

        /**
         * Overrides the default toString method to provide a string
         *
         * @return A string representation of the Query object.
         */
        @Override
        public String toString() {
            return queryString;
        }

        /**
         * Returns name of methods (differentating based on number of args)
         */
        public String getMethodName() {
            if ("getNumberofCountries".equals(methodName)) {
                if (args.size() == 2) {
                    return methodName + "1";
                } else if (args.size() == 3) {
                    return methodName + "2";
                }
            }
            return methodName;
        }
    }

    /* A helper class to track stats (turnaround, execution, waiting times) for each method type.*/
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

/**
 * Simple cache with configurable max entries, removing the oldest entry when full
 */
class Cache extends LinkedHashMap<String, Integer> {
    private final int cacheSize;

    public Cache(int cacheSize) {
        super(cacheSize+1, 1.0f, true);
        this.cacheSize = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
        return size() > cacheSize;
    }
}