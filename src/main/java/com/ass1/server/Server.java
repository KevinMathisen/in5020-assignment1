package com.ass1.server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.AlreadyBoundException;

public class Server implements ServerInterface{
    public int Add(int num1, int num2) {
        return num1 + num2;
    }

    public static void main(String[] args){
        try {
            Registry registry = LocateRegistry.getRegistry();
            Server server = new Server();
            ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(server, 0);
            registry.bind("server", serverStub);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }

    }
}
