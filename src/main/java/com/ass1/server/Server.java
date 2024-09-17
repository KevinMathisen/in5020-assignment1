package com.ass1.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
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
    public Server(int serverZone, boolean cacheEnabled) throws IOException {
        this.serverZone = serverZone;
        this.cacheEnabled = cacheEnabled;
        this.waitingList = new LinkedBlockingQueue<>();
        this.cache = new Cache(150);
        this.logWaitingListWriter = new PrintWriter(new FileWriter("server_zone_"+serverZone+"_waiting_list_log.txt", true), true);


        System.out.println("Initializing server zone " + serverZone + "...");
        loadCitiesFromCSV();
        System.out.println("Loaded " + countries.size() + " countries.");

        startThreadExecutor();
    }

    /**
     * The main method to launch the RMI server and register it in the RMI registry.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args){
        try {
            boolean enableCache = args.length > 0 && args[0].equals("useServerCaching");
            
            Registry registry = LocateRegistry.getRegistry();
            Server[] servers = new Server[5];

            for (int i = 0; i < servers.length; i++) {
                servers[i] = new Server(i, enableCache);
                ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(registry, 0);
                String serverName = "Server zone " + i;
                registry.bind(serverName, serverStub);
            }
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

                    String requestKey = request.getMethod() + ": " + String.join(",", (CharSequence[]) request.getArgs());
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

                        if (cacheEnabled) {
                            cache.put(requestKey, result);
                        }
                    }

                    long executionStopTime = System.currentTimeMillis();
                    long executionTime = executionStopTime - executionStart;
                    long waitingTime = executionStopTime - request.getQueueTime();

                    request.getResponseFuture().complete(new Response(result, executionTime, waitingTime, serverZone));

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
            Thread.sleep((clientZone == serverZone) ? 80 : 170);

            Request request = new Request("getPopulationOfCountry", new Object[] {countryName});
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
            Thread.sleep((clientZone == serverZone) ? 80 : 170);

            Request request = new Request("getNumberOfCities", new Object[] {countryName, minPopulation});
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
            Thread.sleep((clientZone == serverZone) ? 80 : 170);

            Request request = new Request("getNumberOfCountries", new Object[] {cityCount, minPopulation});
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
            Thread.sleep((clientZone == serverZone) ? 80 : 170);

            Request request = new Request("getNumberOfCountries", new Object[] {cityCount, minPopulation, maxPopulation});
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
        logWaitingListWriter.println(System.currentTimeMillis() + ": " + waitingList.size());
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

    public Request(String method, Object[] args) {
        this.method = method;
        this.args = args;
        this.queueTime = System.currentTimeMillis();
        this.responseFuture = new CompletableFuture<>();
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
