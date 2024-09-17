package com.ass1.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

public class ClientTest {

    @Test
    void testParseInputFile() {
        // Construct the path to the test input file
        String path = Paths.get("src", "test", "java", "com", "ass1", "client", "test_input.txt").toString();

        // Invoke the method to test
        List<Client.Query> queries = Client.parseInputFile(path);  // Ensure parseInputFile is at least package-private

        // Assertions to check if the output is as expected
        assertNotNull(queries, "The query list should not be null.");
        assertTrue(queries.size() > 0, "There should be one or more queries parsed.");

        // Example assertion for the first query
        assertEquals("getNumberofCountries", queries.get(0).methodName);
        assertEquals(List.of("41", "63867", "79086"), queries.get(0).args);
        assertEquals(2, queries.get(0).zone);
    }
}
