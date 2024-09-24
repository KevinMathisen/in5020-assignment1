package com.ass1.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The Server class implements the ServerInterface and handles the processing
 * of statistical data related to geographical information stored in a CSV file.
 */
public class Server extends UnicastRemoteObject implements ServerInterface {
    private final int serverZone;
    private final BlockingQueue<Request> waitingList;
    private final Cache cache;
    private final boolean cacheEnabled;
    private final PrintWriter logWaitingListWriter;

    
    private final Map<String, List<City>> countries = new HashMap<>();

    /**
     * Constructs a Server object and initializes the country data from the CSV file.
     * @throws RemoteException if a network issue occurs during RMI setup.
     */
    public Server(int serverZone, String cacheMode, int delay) throws IOException {
        System.out.println("Initializing server zone " + serverZone + "...");
        
        this.serverZone = serverZone;
        this.cacheEnabled = ("server".equals(cacheMode) || "client".equals(cacheMode));
        this.waitingList = new LinkedBlockingQueue<>();
        this.cache = new Cache(150);

        String fileName = "server_zone_"+serverZone+"_log_" + cacheMode + "_delay_" + delay + ".txt";
        String filePath = Paths.get("output", fileName).toString();

        this.logWaitingListWriter = new PrintWriter(new FileWriter(filePath, true), false); //set false to overwrite previous file

        loadCitiesFromCSV();
        System.out.println("Loaded " + countries.size() + " countries.");

        startThreadExecutor();
    }

    /**
     * The main method to launch the RMI servers and register them in the RMI registry.
     * @param args Command line arguments
     */
    public static void main(String[] args){
        try {
            String cacheMode = "naive";
            int delay = 50;

            // Parse command-line arguments
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--delay") && i + 1 < args.length) {
                    delay = Integer.parseInt(args[i + 1]);
                }
                if (args[i].equals("--cache") && i + 1 < args.length) {
                    cacheMode = args[i + 1];
                }
            }
            
            // Create or get the registry
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
            } catch (Exception e) {
                registry = LocateRegistry.getRegistry();
            }
            Server[] servers = new Server[5];

            // For each server, export it and register it to the registry
            for (int i = 0; i < servers.length; i++) {
                servers[i] = new Server(i+1, cacheMode, delay);

                // Unexport servers if already exported
                try {
                    UnicastRemoteObject.unexportObject(servers[i], true);
                } catch (Exception e) {}
                ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(servers[i], 0);
                String serverName = "Server zone " + (i+1);
                registry.bind(serverName, serverStub);
            }

            System.out.println("Server up and ready with cache mode " + cacheMode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The thread that processes requests from the waiting list. 
     * Uses the cache if enabled, and retrieves the result for the specified method.
     * For each request it returns both the result of the method and the time it took to execute.
     */
    private void startThreadExecutor() {
        new Thread(() -> {
            while (true) {
                try {
                    Request request = waitingList.take();
                    long executionStart = System.currentTimeMillis();

                    // Pause execution thread based on where client is from
                    Thread.sleep((request.getClientZone() == serverZone) ? 80 : 170);

                    // get a key for the request, including its arguments
                    String[] argsAsString = Arrays.stream(request.getArgs()).map(Object::toString).toArray(String[]::new);
                    String requestKey = request.getMethod() + ": " + String.join(",", argsAsString);
                    int result;

                    if (cacheEnabled && cache.containsKey(requestKey)) {
                        result = cache.get(requestKey);
                    } else {
                        switch (request.getMethod()) {
                            case "getPopulationOfCountry" -> result = getPopulationOfCountryInternal((String) request.getArgs()[0]);
                            case "getNumberOfCities" -> result = getNumberOfCitiesInternal((String) request.getArgs()[0], (int) request.getArgs()[1]);
                            case "getNumberOfCountries" -> {
                                if (request.getArgs().length == 2) {
                                    result = getNumberOfCountriesInternal((int) request.getArgs()[0], (int) request.getArgs()[1]);
                                } else {
                                    result = getNumberOfCountriesInternal((int) request.getArgs()[0], (int) request.getArgs()[1], (int) request.getArgs()[2]);
                                }
                            }
                            default -> result = 0;
                        }
                    }
                    // Update cache if enabled
                    if (cacheEnabled) {
                        cache.put(requestKey, result);
                    }

                    long executionStopTime = System.currentTimeMillis();
                    long executionTime = executionStopTime - executionStart;
                    long waitingTime = executionStart - request.getQueueTime();

                    // Complete request, returning a reponse object
                    request.getResponseFuture().complete(
                        new Response(result, executionTime, waitingTime, serverZone));

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getQueueLength() throws RemoteException {
        return waitingList.size();
    } 

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getPopulationOfCountry(String countryName, int clientZone) throws RemoteException {
        try {
            Request request = new Request("getPopulationOfCountry", new Object[] {countryName}, clientZone);
            waitingList.put(request);
            logWaitingList();

            return request.getResponseFuture().get();
        } catch (Exception e) {
            throw new RemoteException("Error when processing request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getNumberOfCities(String countryName, int minPopulation, int clientZone) throws RemoteException {
        try {
            Request request = new Request("getNumberOfCities", new Object[] {countryName, minPopulation}, clientZone);
            waitingList.put(request);
            logWaitingList();

            return request.getResponseFuture().get();
        } catch (Exception e) {
            throw new RemoteException("Error when processing request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getNumberOfCountries(int cityCount, int minPopulation, int clientZone) throws RemoteException {
        try {
            Request request = new Request("getNumberOfCountries", new Object[] {cityCount, minPopulation}, clientZone);
            waitingList.put(request);
            logWaitingList();

            return request.getResponseFuture().get();
        } catch (Exception e) {
            throw new RemoteException("Error when processing request", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getNumberOfCountries(int cityCount, int minPopulation, int maxPopulation, int clientZone) throws RemoteException {
        try {
            Request request = new Request("getNumberOfCountries", new Object[] {cityCount, minPopulation, maxPopulation}, clientZone);
            waitingList.put(request);
            logWaitingList();

            return request.getResponseFuture().get();
        } catch (Exception e) {
            throw new RemoteException("Error when processing request", e);
        }
    }



    /**
     * Logs the amount of requests in the waiting list and the current time
     */
    private void logWaitingList() {
        synchronized (logWaitingListWriter) {
            logWaitingListWriter.println(System.currentTimeMillis() + ": " + waitingList.size());
            logWaitingListWriter.flush(); 
        }
        System.out.println("Server zone " + serverZone + " has queue length: " + waitingList.size());
    }

    /**
     * Loads city data from a CSV file and stores it in a map organized by country names.
     * @param filename The path to the CSV file containing city data.
     */
    private void loadCitiesFromCSV() throws IOException {
        try {
            InputStream inputStream = Server.class.getClassLoader().getResourceAsStream("com/ass1/server/data/exercise_1_dataset.csv");
            if (inputStream == null) {
                throw new FileNotFoundException("Cannot find exercise_1_dataset.csv");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            br.readLine(); // Skip header line
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(";");
                // Ensure that the split line has all required fields
                if (fields.length >= 7) {
                    City city = new City(
                        Integer.parseInt(fields[0]), fields[1], fields[2], fields[3], 
                        Integer.parseInt(fields[4]), fields[5], fields[6]
                    );
                    countries.computeIfAbsent(fields[3], k -> new ArrayList<>()).add(city);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    /** {@inheritDoc} */
    private int getPopulationOfCountryInternal(String countryName) {
        return countries.getOrDefault(countryName, Collections.emptyList())
                        .stream()
                        .mapToInt(city -> city.population)
                        .sum();
    }

    /** {@inheritDoc} */
    private int getNumberOfCitiesInternal(String countryName, int minPopulation) {
        return (int) countries.getOrDefault(countryName, Collections.emptyList())
                              .stream()
                              .filter(city -> city.population >= minPopulation)
                              .count();
    }

    /** {@inheritDoc} */
    private int getNumberOfCountriesInternal(int cityCount, int minPopulation) {
        return (int) countries.values().stream()
                              .filter(cities -> cities.stream().filter(city -> city.population >= minPopulation).count() >= cityCount)
                              .count();
    }

    /** {@inheritDoc} */
    private int getNumberOfCountriesInternal(int cityCount, int minPopulation, int maxPopulation) {
        return (int) countries.values().stream()
                              .filter(cities -> cities.stream().filter(city -> city.population >= minPopulation && city.population <= maxPopulation).count() >= cityCount)
                              .count();
    }

}

/**
 * Simple cache with max 150 entries, removing the oldest entry when full
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

/**
 * A representation of a client request, including the time it was put into queue and a future for a reponse
 */
class Request {
    private final String method;
    private final Object[] args;
    private final long queueTime;
    private final CompletableFuture<Response> responseFuture;
    private final int clientZone;

    public Request(String method, Object[] args, int clientZone) {
        this.method = method;
        this.args = args;
        this.queueTime = System.currentTimeMillis();
        this.responseFuture = new CompletableFuture<>();
        this.clientZone = clientZone;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public long getQueueTime() {
        return queueTime;
    }

    public long getClientZone() {
        return clientZone;
    }

    public CompletableFuture<Response> getResponseFuture() {
        return responseFuture;
    }
}

/**
 * Represents a city with geographical and demographic data.
 */
class City {
    int geonameId;
    String name;
    String countryCode;
    String countryName;
    int population;
    String timezone;
    String coordinates;

    /**
     * Constructs a city instance with provided details.
     * @param geonameId Unique identifier for the city.
     * @param name Name of the city.
     * @param countryCode ISO country code.
     * @param countryName Full country name.
     * @param population Population of the city.
     * @param timezone Timezone of the city.
     * @param coordinates Geographical coordinates of the city.
     */
    public City(int geonameId, String name, String countryCode, String countryName, int population, String timezone, String coordinates) {
        this.geonameId = geonameId;
        this.name = name;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.population = population;
        this.timezone = timezone;
        this.coordinates = coordinates;
    }
}
