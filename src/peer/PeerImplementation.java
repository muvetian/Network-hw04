package peer;

import java.rmi.AccessException;
import java.rmi.RemoteException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.server.UnicastRemoteObject;

import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import client.Client;
import manager.Manager;

// In the original skeleton code, you actually have a client-server architecture,
// not a peer-to-peer. But as you change this class, you'll end up there.
public class PeerImplementation implements Peer {
	private Manager manager;
	private Map<Integer, String> table;
	private ConcurrentSkipListMap<Integer, Peer> myPeers;

	public PeerImplementation(Registry registry, Manager manager) {
		super();

		this.manager = manager;
		this.table = new ConcurrentHashMap<Integer, String>();
	}

	public void put(Integer key, String value, Client client) {
		Peer destination = find(key);

		if(destination == this) {
			try {
				if(client != null) {
					client.submitAnswerPut(key, table.put(key, value));
				}
			}
			catch(RemoteException exception) {
				System.err.print("Cannot contact client: ");
				exception.printStackTrace();
			}
		}
		else {
			System.err.println("TODO: Forward the request to the right peer after you have the membership list.");
		}
	}

	public void get(Integer key, Client client) {
		Peer destination = find(key);

		if(destination == this) {
			try {
				if(client != null) {
					client.submitAnswerGet(key, table.get(key));
				}
			}
			catch(RemoteException exception) {
				System.err.print("Cannot contact client: ");
				exception.printStackTrace();
			}
		}
		else {
			System.err.println("TODO: Forward the request to the right peer after you have the membership list.");
		}
	}

	public Boolean ping() {
		System.out.println("Tum-Tum");
		return true;
	}

	public void updateMembers() {
		System.out.println("Updating member list");

		ArrayList<Peer> peers = null;

		try {
			peers = manager.getCurrentPeers();
		}
		catch (RemoteException exception) {
			System.err.println("Unable to contact manager to update members.");
			return;
		}

		ConcurrentSkipListMap<Integer, Peer> updatedPeers = new ConcurrentSkipListMap<Integer, Peer>();

		for(Peer peer: peers) {
			updatedPeers.put(peer.hashCode(), peer);
		}

		myPeers = updatedPeers;
	}

	private Peer find(Integer key) {
		return this;
	}

	public void join(Manager manager) {
		try {
			Peer peerStub = (Peer) UnicastRemoteObject.exportObject(this, 0);
			manager.register(peerStub);
		}
		catch(AccessException exception) {
			System.err.println("Error binding peer into the registry.");
		}
		catch(RemoteException exception) {
			System.err.println("Error accessing registry.");
		}
	}
	public void move(Integer begin, Integer end, Peer destination){
		ConcurrentSkipListMap<Integer,Peer> submap=(ConcurrentSkipListMap<Integer, Peer>) this.myPeers.subMap(begin, end);
		
		Iterator<Integer> itr = submap.keySet().iterator();
		while(itr.hasNext()){
			Integer key = itr.next();
			String value = this.table.get(key);
			try{
				destination.put(key,value, null);
			}
			catch(Exception e){
				System.out.print("Error while moving the submap");
			}
			
		}
		
		
	}

	public static void main(String[] args) {
		try {
			Registry registry = LocateRegistry.getRegistry();

			Manager manager = (Manager) registry.lookup("DynamoClone");

			PeerImplementation peer = new PeerImplementation(registry, manager);

			peer.join(manager);

			PeriodicAgent agent = new PeriodicAgent(peer);
			agent.start();
		}
		catch(Exception exception) {
			System.err.print("Failed to locate manager in registry: ");
			exception.printStackTrace();
		}
	}
	
}