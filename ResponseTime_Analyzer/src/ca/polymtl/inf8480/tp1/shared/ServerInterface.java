package ca.polymtl.inf8480.tp1.shared;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {

    String userPath = "/Users/mathieuchateauvert/Documents/École/Polytechnique/Session6/INF8480/INF8480/ResponseTime_Analyzer/src/ca/polymtl/inf8480/tp1/";

    void execute() throws RemoteException;
    boolean openSession(String login, String password) throws RemoteException;
    String send() throws RemoteException;
    boolean lockGroupList() throws RemoteException;
    void pushGroupList() throws RemoteException;
    boolean getGroupList(String clientChecksum) throws RemoteException;
    File getServerGroupList() throws RemoteException;
}
