package ca.polymtl.inf8480.tp1.shared;

import java.io.File;
import java.nio.file.Paths;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {

    String userPath = Paths.get("").toAbsolutePath().toString() + "/src/ca/polymtl/inf8480/tp1/";

    int openSession(String login, String password) throws RemoteException;
    boolean lockGroupList(int clientId) throws RemoteException;
    boolean getGroupList(String clientChecksum) throws RemoteException;
    String getServerGroupList() throws RemoteException;
    void pushLocalGroupList(String localFile) throws RemoteException;
    boolean sendMail(String subject, String addrDest, String content, String clientAdress) throws RemoteException;
    String listMails(boolean justUnread, String clientAdress)throws RemoteException;
    boolean deleteMail(int mailNumber, String clientAdress) throws RemoteException;
    String readMail(int mailNumber, String clientAdress) throws RemoteException;
    String searchMail(String keywords, String clientAdress)throws RemoteException;
}
