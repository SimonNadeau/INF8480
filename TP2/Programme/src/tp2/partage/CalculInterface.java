package tp2.partage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface CalculInterface extends Remote {

    int calculate(ArrayList<String> operations) throws RemoteException;
}
