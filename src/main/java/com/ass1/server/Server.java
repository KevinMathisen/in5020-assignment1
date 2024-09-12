package com.ass1.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Server class implements the ServerInterface and handles the processing
 * of statistical data related to geographical information stored in a CSV file.
 */
public class Server extends UnicastRemoteObject implements ServerInterface {
    private Map<String, List<City>> countries = new HashMap<>();

    /**
     * Constructs a Server object and initializes the country data from the CSV file.
     * @throws RemoteException if a network issue occurs during RMI setup.
     */
    public Server() throws IOException {
        System.out.println("Initializing server...");
        loadCitiesFromCSV();
        System.out.println("Loaded " + countries.size() + " countries.");
    }

    @Override
    public int GetQueueLength() throws RemoteException {
        return 0;
        // should return the size of its queue
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
                        Integer.parseInt(fields[0]),
                        fields[1],
                        fields[2],
                        fields[3],
                        Integer.parseInt(fields[4]),
                        fields[5],
                        fields[6]
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
    @Override
    public int getPopulationOfCountry(String countryName) {
        return countries.getOrDefault(countryName, Collections.emptyList())
                        .stream()
                        .mapToInt(city -> city.population)
                        .sum();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumberOfCities(String countryName, int minPopulation) {
        return (int) countries.getOrDefault(countryName, Collections.emptyList())
                              .stream()
                              .filter(city -> city.population >= minPopulation)
                              .count();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumberOfCountries(int cityCount, int minPopulation) {
        return (int) countries.values().stream()
                              .filter(cities -> cities.stream().filter(city -> city.population >= minPopulation).count() >= cityCount)
                              .count();
    }

    /** {@inheritDoc} */
    @Override
    public int getNumberOfCountries(int cityCount, int minPopulation, int maxPopulation) {
        return (int) countries.values().stream()
                              .filter(cities -> cities.stream().filter(city -> city.population >= minPopulation && city.population <= maxPopulation).count() >= cityCount)
                              .count();
    }

    /**
     * The main method to launch the RMI server and register it in the RMI registry.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args){
        try {
            Registry registry = LocateRegistry.getRegistry();
            Server[] servers = new Server[5];

            for (int i = 0; i < servers.length; i++) {
                servers[i] = new Server();
                ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(registry, 0);
                String serverName = "Server zone " + i;
                registry.bind(serverName, serverStub);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
