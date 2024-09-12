package com.ass1.proxy;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.ass1.server.ServerInterface;

public interface ProxyInterface extends Remote {
	ServerInterface getAvailableServer(Integer zone) throws RemoteException;
}