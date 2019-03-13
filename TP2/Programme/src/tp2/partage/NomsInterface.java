package tp2.partage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface NomsInterface extends Remote {

    //int openSession(String login) throws RemoteException;
    ArrayList<ArrayList<String>> getCalculServerInfos() throws RemoteException;
}
