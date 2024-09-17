package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    Response getPopulationOfCountry(String countryName, int clientZone) throws RemoteException;
    Response getNumberOfCities(String countryName, int minPopulation, int clientZone) throws RemoteException;
    Response getNumberOfCountries(int cityCount, int minPopulation, int clientZone) throws RemoteException;
    Response getNumberOfCountries(int cityCount, int minPopulation, int maxPopulation, int clientZone) throws RemoteException;
    int getQueueLength() throws RemoteException;
}
