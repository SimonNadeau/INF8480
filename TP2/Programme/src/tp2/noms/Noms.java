package tp2.noms;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import tp2.partage.NomsInterface;

public class Noms implements NomsInterface {

	public static void main(String[] args) {
		Noms calcul = new Noms();
		calcul.run();
	}

    private ArrayList<ArrayList<String>> calculServerInfos = null;

	public Noms() {
		super();
		
		calculServerInfos = new ArrayList<ArrayList<String>>();

		//TODO: retirer cette partie quand les serveurs de calcul vont envoyer leurs infos
		ArrayList<String> calculServerInfo = new ArrayList<String>();
		calculServerInfo.add("127.0.0.1");
		calculServerInfos.add(calculServerInfo);
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
            NomsInterface stub = (NomsInterface) UnicastRemoteObject.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
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
    public ArrayList<String> getCalculServerHostnames() throws RemoteException {
		ArrayList<String> calculServerHostnames = new ArrayList<String>();
		
		for (ArrayList<String> calculServerInfo : calculServerInfos) {
			calculServerHostnames.add(calculServerInfo.get(0));
		}

        return calculServerHostnames;
    }
}