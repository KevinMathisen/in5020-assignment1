package com.ass1.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{
    int Add(int num1,int num2) throws RemoteException;
    
}
