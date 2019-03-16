package tp2.partage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NomsInterface extends Remote {
    ArrayList<ArrayList<String>> getCalculServerInfos() throws RemoteException;
    void addInfo(ArrayList<String> info) throws RemoteException;
}
