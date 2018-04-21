package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import client.Client;

public interface Peer extends Remote {
    void put(Integer key, String value, Client client) throws RemoteException;
    void get(Integer key, Client client) throws RemoteException;
    void move(Integer begin, Integer end, Peer destination);
    Boolean ping() throws RemoteException;
}