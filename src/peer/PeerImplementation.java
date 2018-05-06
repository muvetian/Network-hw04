package peer;

import java.rmi.AccessException;
import java.rmi.RemoteException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.rmi.server.UnicastRemoteObject;

import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import client.Client;
import manager.Manager;

// In the original skeleton code, you actually have a client-server architecture,
// not a peer-to-peer. But as you change this class, you'll end up there.
public class PeerImplementation implements Peer {
	private Manager manager;
	private Map<Integer, String> table;
	private ConcurrentSkipListMap<Integer, Peer> myPeers;
	
	public PeerImplementation(Registry registry, Manager manager) throws RemoteException {
		super();

		this.manager = manager;
		this.table = new ConcurrentSkipListMap<Integer, String>();
		
		
		
		myPeers = new ConcurrentSkipListMap<Integer,Peer>();


	}
	
	public void put(Integer key, String value, Client client) throws RemoteException {
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
			//Do we have to consider when the key is not found in the system and we have to create a new key-value pair?
			Peer randomPeer = (PeerImplementation)this.manager.getRandom();
			randomPeer.put(key,value,client);
			
		}
	}

	public void get(Integer key, Client client) throws RemoteException {
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
			
			Peer randomPeer = (PeerImplementation)this.manager.getRandom();
			randomPeer.get(key, client);
			
		}
	}

	public Boolean ping() {
		System.out.println("Tum-Tum");
		return true;
	}
	public ConcurrentSkipListMap<Integer,String> getTable(){
		return (ConcurrentSkipListMap<Integer, String>) table;
	}
	public ArrayList<Peer> getCurrentPeers(){
		ArrayList<Peer> result = new ArrayList<Peer>();
		for(Entry<Integer, Peer> entry: myPeers.entrySet()) {
			result.add(entry.getValue());
		}
		
		return result;
	}
	public void updateMembers() {
		System.out.println("Updating member list");

		ArrayList<Peer> peers = new ArrayList<Peer>();

		try {
			Peer random_peer = (PeerImplementation)this.manager.getRandom();
			if(random_peer == null){
				// Random returns null so the node is the first one
				// Add itself to the list
				peers.add(this);
			}
			else{
				// It does not return null so update it with whatever the random peer has
				peers = random_peer.getCurrentPeers(); 
			}
			
		}
		catch (RemoteException exception) {
			System.err.println("Unable to contact manager to update members.");
			return;
		}

		ConcurrentSkipListMap<Integer, Peer> updatedPeers = new ConcurrentSkipListMap<Integer, Peer>();

		for(Peer peer: peers) {
			updatedPeers.put((int) (peer.hashCode()% (java.lang.Math.pow(2,16))), peer);
		}

		myPeers = updatedPeers;
	}

	private Peer find(Integer key) {
		int hashedKey = (int) (key.hashCode() % (java.lang.Math.pow(2,16)));
		Entry<Integer,Peer> entry = myPeers.ceilingEntry(hashedKey);	

		if(entry != null) {
			return entry.getValue();
		}
		// What if the key is not in the system
		return myPeers.firstEntry().getValue();
	}
	public void addPeer(Peer peer){
		Integer peerID = (int) (peer.hashCode() % (java.lang.Math.pow(2,16)));
		myPeers.put(peerID, peer);
	}
	public void join(Manager manager) {
		try {
			Peer peerStub = (Peer) UnicastRemoteObject.exportObject(this, 0);
			manager.register(peerStub);
			// Contact every peer in myPeers and inform them that another peer should be added
			for (Entry<Integer,Peer> peerEntry : myPeers.entrySet()){
				Peer peer = peerEntry.getValue();
				((PeerImplementation) peer).addPeer(this);
			}
			
		}
		catch(AccessException exception) {
			System.err.println("Error binding peer into the registry.");
		}
		catch(RemoteException exception) {
			System.err.println("Error accessing registry.");
		}
	}
	public void move(Integer begin, Integer end, Peer destination){
	

//		Iterator<Integer> itr = submap.keySet().iterator();
		Integer peerID = (int) (destination.hashCode() % (java.lang.Math.pow(2,16)));
		// Finding the predecessor
		Integer predID = myPeers.floorKey(peerID);
		Integer succID = myPeers.ceilingKey(peerID);
		Peer successor = this.myPeers.get(succID);
		ConcurrentNavigableMap<Integer, String> submap = this.getTable().subMap(begin, end);//should be moving this to the newly added node
		Iterator<Entry<Integer,String>> itr = submap.descendingMap().entrySet().iterator();
		while(itr.hasNext()){
			Entry<Integer,String> entry = itr.next();
			Integer key = entry.getKey();
			String value = entry.getValue();
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
		
			peer.updateMembers(); // update members here?
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
