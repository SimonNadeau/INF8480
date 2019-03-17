package tp2.noms;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import tp2.partage.NomsInterface;

public class Noms implements NomsInterface {

    private String repartiteurUsername = "";
    private String repartiteurPassword = "";

	public static void main(String[] args) {
		Noms calcul = new Noms();
		calcul.run();
	}

    private ArrayList<ArrayList<String>> calculServerInfos = null;

	public Noms() {
		super();
		
		calculServerInfos = new ArrayList<ArrayList<String>>();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
            NomsInterface stub = (NomsInterface) UnicastRemoteObject.exportObject(this, 5001);

			Registry registry = LocateRegistry.getRegistry(5000);
			registry.rebind("noms", stub);
			System.out.println("Serveur de noms prêt.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur1: " + e);
		}
    }
    
    @Override
    public void addInfo(ArrayList<String> info) throws RemoteException {
        calculServerInfos.add(info);
    }

    @Override
    public ArrayList<ArrayList<String>> getCalculServerInfos() throws RemoteException {
        return calculServerInfos;
    }

    @Override
    public void addClient(String username, String password) throws RemoteException {
        repartiteurUsername = username;
        repartiteurPassword = password;
    }

    @Override
    public boolean authentificationClient(String username, String password) throws RemoteException {
        if (username.equals(repartiteurUsername) && password.equals(repartiteurPassword)){
            return true;
        } else {
            return false;
        }
    }
}