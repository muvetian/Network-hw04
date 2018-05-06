package peer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;

import client.Client;

public interface Peer extends Remote {
    void put(Integer key, String value, Client client) throws RemoteException;
    void get(Integer key, Client client) throws RemoteException;
    void move(Integer begin, Integer end, Peer destination);
    ConcurrentSkipListMap<Integer,String> getTable();
    void updateMembers();
    ArrayList<Peer> getCurrentPeers() throws RemoteException;
    Boolean ping() throws RemoteException;
}