package com.ass1.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerTest {
    private Server server;

    @BeforeEach
    void setUp() throws Exception {
        server = new Server(); // Make sure this can load the CSV file without issues
    }

    @Test
    void testGetPopulationOfCountry() {
        // Assuming "Belgium" has a specific known population in your test CSV
        assertEquals(15411242, server.getPopulationOfCountry("Belgium"),
            "The population count for Belgium should be accurate.");
    }

    @Test
    void testGetNumberOfCities() {
        // Assuming "Belgium" has a certain number of cities above a population threshold
        assertEquals(372, server.getNumberOfCities("Belgium", 10000),
            "The number of cities in Belgium with population over 10000 should be accurate.");
    }

    @Test
    void testGetNumberOfCountriesWithMinCityCountAndPopulation() {
        // Test the count of countries with a minimum number of cities having at least a certain population
        assertEquals(7, server.getNumberOfCountries(2, 5000000),
            "The number of countries with at least 2 cities having populations over 5000000 should be accurate.");
    }

    @Test
    void testGetNumberOfCountriesWithCityCountRange() {
        // Test the number of countries with a specified range of city counts and population limits
        assertEquals(30, server.getNumberOfCountries(30, 100000, 800000),
            "The number of countries with 30 cities having populations between 100000 and 800000 should be accurate.");
    }
}
