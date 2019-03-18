package tp2.partage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NomsInterface extends Remote {

    // A MODIFIER pour le serveur de noms
    static final String ipServeurNoms = "132.207.12.45";

    ArrayList<ArrayList<String>> getCalculServerInfos() throws RemoteException;
    void addInfo(ArrayList<String> info) throws RemoteException;
    void addClient(String username, String password) throws RemoteException;
    boolean authentificationClient(String username, String password) throws RemoteException;
}
