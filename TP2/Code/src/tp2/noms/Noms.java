package tp2.noms;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import tp2.partage.NomsInterface;

public class Noms implements NomsInterface {

    // stock in there the username and the password of the repartiteur for the authentification of the calcul server.
    private String repartiteurUsername = "";
    private String repartiteurPassword = "";

	public static void main(String[] args) {
		Noms calcul = new Noms();
		calcul.run();
	}

    // List of all the infos of the calcul server
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
            // rmi Port 5001 for the name server. 
            NomsInterface stub = (NomsInterface) UnicastRemoteObject.exportObject(this, 5001);

            // rmi Port 5000 for the application
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
    
    // Methods use by the calcul server to send informations when creating itself.
    @Override
    public void addInfo(ArrayList<String> info) throws RemoteException {
        calculServerInfos.add(info);
    }

    // Method use by the repartiteur to get all the informations of the created servers
    @Override
    public ArrayList<ArrayList<String>> getCalculServerInfos() throws RemoteException {
        return calculServerInfos;
    }

    // When the repartiteur creates itself, it sends his login to the name server so that it can authentificate it correctly. 
    @Override
    public void addClient(String username, String password) throws RemoteException {
        repartiteurUsername = username;
        repartiteurPassword = password;
    }

    // Authentificate the client correctly.
    @Override
    public boolean authentificationClient(String username, String password) throws RemoteException {
        if (username.equals(repartiteurUsername) && password.equals(repartiteurPassword)){
            return true;
        } else {
            return false;
        }
    }
}