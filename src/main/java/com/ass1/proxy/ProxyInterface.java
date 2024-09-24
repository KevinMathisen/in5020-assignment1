package com.ass1.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ProxyInterface extends Remote {
	Integer getAvailableServer(Integer zone) throws RemoteException;
}