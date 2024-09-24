package com.ass1.proxy;

import java.nio.channels.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ass1.server.ServerInterface;

public class Proxy extends UnicastRemoteObject implements ProxyInterface {
	private final HashMap<Integer, String> serverNames;
	private final HashMap<Integer, Integer> serverQueueLength;
	private final HashMap<Integer, Integer> serverAccessCount;

	private final Registry registry;
	private final ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * Constructs a proxy object
	 * Initializes the registry, sets the server names, and get their initial queue lengths
	 * @throws RemoteException if remote error occurs
	 */
	public Proxy() throws RemoteException {
		super();

		this.registry = LocateRegistry.getRegistry();

		serverNames = new HashMap<>();
		serverQueueLength = new HashMap<>();
		serverAccessCount = new HashMap<>();

		for (int i = 1; i <= 5; i++) {
			serverNames.put(i, "Server zone "+i);
			fetchServerQueueLength(i);
		}
	}

	/**
	 * Gets the registry, initializes the proxy objecy, export the remote object, and binds it to the registry
	 * @param args
	 * @throws java.rmi.AlreadyBoundException
	 */
	public static void main(String[] args) throws java.rmi.AlreadyBoundException {
		try {
			Registry registry = LocateRegistry.getRegistry();

			Proxy proxy = new Proxy();

			// Unexport proxy if already exported
			try {
				UnicastRemoteObject.unexportObject(proxy, true);
			} catch (Exception e) {}

			// Export proxy and bind to the name 'Proxy' in the registry
			ProxyInterface proxyStub = (ProxyInterface) UnicastRemoteObject.exportObject(proxy, 0);
			registry.bind("Proxy", proxyStub);

			System.out.println("Proxy up and ready");

		} catch (RemoteException | AlreadyBoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns a reference to an available server in proximity to a given zone
	 * @param zone - the zone the client requsts from
	 * @return the available server interface
	 * @throws RemoteException if a remote error occurs
	 */
	@Override
	public Integer getAvailableServer(Integer zone) throws RemoteException {
		Integer selectedZone = zone;

		Integer localZoneQueueLength = serverQueueLength.get(zone);

		// Consider the next 2 zones if the local zone is overloaded
		if (localZoneQueueLength >= 18) {
			int zone2 = (zone%5)+1;
			int zone3 = ((zone+1)%5) + 1;
			Integer zone2QueueLength = serverQueueLength.get(zone2);
			Integer zone3QueueLength = serverQueueLength.get(zone3);

			// Select zone with shortest queue if any of them are shorter than 18
			if (zone2QueueLength < 18 || zone3QueueLength < 18) {
				if (zone2QueueLength <= zone3QueueLength) {
					selectedZone = zone2;
				} else {
					selectedZone = zone3;
				}
			}
		}

		updateServerAccessCount(selectedZone);

		return selectedZone;
	}

	/**
	 * Updates the access count of server in a zone, and retrieves its queue length every 18th access
	 * @param zone - zone of the server to access
	 */
	private void updateServerAccessCount(Integer zone) {
		// Update server access count by 1, if it is 18 reset it to 0
		serverAccessCount.compute(zone, (k, accessCount) -> (accessCount == null ? 0 : accessCount + 1) % 18);
		if (serverAccessCount.get(zone) == 0) {
			// fetch the server queue length in another thread
			executor.submit(() -> fetchServerQueueLength(zone));
		}
	}

	/**
	 * fetches the specified server for their queue length, and then updates it in the serverQueueLength
	 * @param zone - zone to get queue length from
	 */
	private void fetchServerQueueLength(Integer zone) {
		try {
			String serverName = serverNames.get(zone); 
			ServerInterface server = (ServerInterface) registry.lookup(serverName);

			int queueLength = server.getQueueLength();

			serverQueueLength.put(zone, queueLength);
		} catch (RemoteException | AlreadyBoundException | NotBoundException e) {
			System.out.print(e);
		}
	}

}