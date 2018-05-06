package manager;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.ArrayList;

import client.Client;
import peer.Peer;

public interface Manager extends Remote {


	void register(Peer peer) throws RemoteException;
	void unregister(Peer peer) throws RemoteException;
	
	Peer getRandom() throws RemoteException;
	

}