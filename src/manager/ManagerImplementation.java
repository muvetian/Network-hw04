package manager;
import java.lang.Math;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.server.UnicastRemoteObject;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.ArrayList;
import java.util.Random;
import java.util.Map.Entry;

import client.Client;
import peer.Peer;

public class ManagerImplementation implements Manager {
	private Registry registry;
	private ConcurrentSkipListMap<Integer, Peer> myPeers;

	private Random random;

	public ManagerImplementation(Registry registry) {
		super();

		this.registry = registry;
		this.myPeers = new ConcurrentSkipListMap<Integer, Peer>();

		this.random = new Random();
	}

	public void put(Integer key, String value, Client client) {
		try {
			Peer destinationPeer = find(key);

			if(destinationPeer != null) {
				destinationPeer.put(key, value, client);
			}
			else {
				System.err.println("Error finding destination peer");
			}
		}
		catch(Exception exeption) {
			System.err.println("Error contacting destination peer");
		}
	}

	public void get(Integer key, Client client) {
		try {
			Peer destinationPeer = find(key);

			if(destinationPeer != null) {
				destinationPeer.get(key, client);
			}
			else {
				System.err.println("Error finding destination peer");
			}
		}
		catch(Exception exeption) {
			System.err.println("Error contacting destination peer");
		}
	}

	public void register(Peer peer) {
		int peerID = peer.hashCode();
		peerID = (int) (peerID % (java.lang.Math.pow(2,16)));

		String peerName = "Peer-" + peerID;
		try {
			registry.rebind(peerName, peer);
			Entry<Integer, Peer> entry = myPeers.floorEntry(peerID);
			Integer successor_ID = myPeers.ceilingKey(peerID);
			myPeers.put(peerID, peer);


			Peer predecessor = entry.getValue();
			// Finds the largest entry whose key is less or equal than the random number


			// move part of the data stored in peer's predecessor to peer
			// Namely the begin is the newly added node's ID, and the end is the ID of this newly added node's sucessor


			// Move anything between this newly added node's predecessor and this node from old successor
			predecessor.move(peerID,successor_ID, peer);


		}
		catch(AccessException exception) {
			System.err.println("Error binding peer into the registry.");
		}
		catch(RemoteException exception) {
			System.err.println("Error accessing registry.");
		}
	}

	public void unregister(Peer peer) {
		int peerID = peer.hashCode();
		String peerName = "Peer-" + peerID;

		try {
			registry.unbind(peerName);
			myPeers.remove(peerID);
		}
		catch(AccessException exception) {
			System.err.println("Error binding peer into the registry.");
		}
		catch(RemoteException exception) {
			System.err.println("Error accessing registry.");
		}
		catch(NotBoundException exception) {
			System.err.println("Error removing peer from the registry.");
		}
	}

	public Peer getRandom() {
		// Take a random number
		int randomNumber = random.nextInt();

		// Finds the largest entry whose key is less or equal than the random number
		Entry<Integer, Peer> entry = myPeers.floorEntry(randomNumber);

		if(entry != null) {
			return entry.getValue();
		}

		return null;
	}

	public ArrayList<Peer> getCurrentPeers() {
		ArrayList<Peer> result = new ArrayList<Peer>();

		for(Entry<Integer, Peer> entry: myPeers.entrySet()) {
			result.add(entry.getValue());
		}

		return result;
	}

	public void heartbeat() {
		for(Entry<Integer, Peer> entry: myPeers.entrySet()) {
			int peerID = entry.getKey();
			Peer peer = entry.getValue();

			try {
				peer.ping();
			}
			catch(RemoteException exception) {
				System.err.println("Peer with ID " + peerID + " might be down.");
			}
		}
	}

	private Peer find(Integer key) {
		// The first node is always the destination,
		// so every time a node joins, you have a new hash table. Ugh.
		int hashedKey = (int) (key.hashCode() % (java.lang.Math.pow(2,16)));
		Entry<Integer,Peer> entry = myPeers.ceilingEntry(hashedKey);	

		if(entry != null) {
			return entry.getValue();
		}
		// What if the key is not in the system
		return myPeers.firstEntry().getValue();
	}

	public static void main(String[] args) {
		try {
			Registry registry = LocateRegistry.getRegistry();

			ManagerImplementation manager = new ManagerImplementation(registry);
			Manager managerStub = (Manager) UnicastRemoteObject.exportObject(manager, 0);

			registry.rebind("DynamoClone", managerStub);

			PeriodicAgent agent = new PeriodicAgent(manager);
			agent.start();
		}
		catch(Exception exception) {
			System.err.print("Failed to bind manager to registry: ");
			exception.printStackTrace();
		}
	}
}
