package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
    int getPopulationOfCountry(String countryName) throws RemoteException;
    int getNumberOfCities(String countryName, int minPopulation) throws RemoteException;
    int getNumberOfCountries(int cityCount, int minPopulation) throws RemoteException;
    int getNumberOfCountries(int cityCount, int minPopulation, int maxPopulation) throws RemoteException;
}
