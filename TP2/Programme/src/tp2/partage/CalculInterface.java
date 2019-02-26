package tp2.partage;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CalculInterface extends Remote {

    int openSession(String login) throws RemoteException;
}
