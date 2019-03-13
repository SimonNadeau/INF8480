package tp2.repartiteur;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

import tp2.partage.*;

public class Repartiteur {
	
	public static void main(String args[]) {	
		// Fichier a lire
		String fileName = null;
		if (args.length > 0) {
			fileName = args[0];
		}

		// Creation du repartiteur
		Repartiteur repartiteur = new Repartiteur();

		// Calcul du resultat
		int resultat = repartiteur.process(fileName);

		// Affichage du résultat
		System.out.println(resultat);
	}

	private NomsInterface nomsServerStub = null;
	private ArrayList<CalculInterface> calculServerStubs = null;

	private ArrayList<ArrayList<String>> calculServerInfos = null;

	public Repartiteur() {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		nomsServerStub = loadNomsServerStub("127.0.0.1");
		if (nomsServerStub != null) {
			calculServerStubs = new ArrayList<CalculInterface>();
			try {
				calculServerInfos = nomsServerStub.getCalculServerInfos();

				for (ArrayList<String> calculServer : calculServerInfos) {
					calculServerStubs.add(loadCalculServerStub(calculServer.get(0)));
				}
			} catch (RemoteException e) {
				System.out.println("Erreur: " + e.getMessage());
			}
		}
	}

	private NomsInterface loadNomsServerStub(String hostname) {
		NomsInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (NomsInterface) registry.lookup("noms");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private CalculInterface loadCalculServerStub(String hostname) {
		CalculInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (CalculInterface) registry.lookup("calcul");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private int process(String fileName) {
		int resultat = 0;

		return resultat;
	}

}



// import java.security.DigestInputStream;
// import java.security.MessageDigest;
// import java.security.NoSuchAlgorithmException;
// import java.lang.Math.*;
// import java.nio.charset.StandardCharsets;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.nio.file.StandardOpenOption;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.InputMismatchException;
// import java.util.List;
// import java.util.NoSuchElementException;
// import java.util.Scanner;

// import javax.xml.bind.DatatypeConverter;

// import java.io.File;
// import java.io.FileInputStream;
// import java.io.FileNotFoundException;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.IOException;
// import java.io.InputStream;
// import java.io.InputStreamReader;
// import java.io.BufferedReader;

// import tp2.partage.CalculInterface;

// public class Repartiteur {

//     private int clientId;           // Afin de savoir quel numero de client nous sommes
//     private String login = "";      // Le nom de notre login

// 	public static void main(String[] args) {
//         String distantHostname = null;

// 		if (args.length > 0) {
// 			distantHostname = args[0];
// 		}

//         // Connexion au niveau du serveur distant
//         Repartiteur repartiteur = new Repartiteur(distantHostname);
//         repartiteur.connexion();
//         if (repartiteur.clientId != 0){
//             System.out.println("Connexion réussie");
//         } else {
//             System.out.println("Connexion échouée");
//         }

// 	}
// 	private CalculInterface localServerStub = null;
// 	// private CalculInterface distantServerStub = null;

// 	public Repartiteur(String distantServerHostname) {
//         super();

// 		if (System.getSecurityManager() == null) {
// 			System.setSecurityManager(new SecurityManager());
// 		}
// 		localServerStub = loadServerStub("127.0.0.1");

// 		// if (distantServerHostname != null) {
// 		// 	localServerStub = loadServerStub(distantServerHostname);
// 		// }
//     }

//     private CalculInterface loadServerStub(String hostname) {
// 		CalculInterface stub = null;

// 		try {
// 			Registry registry = LocateRegistry.getRegistry(hostname);
// 			stub = (CalculInterface) registry.lookup("calcul");
// 		} catch (NotBoundException e) {
// 			System.out.println("Erreur: Le nom '" + e.getMessage()
// 					+ "' n'est pas défini dans le registre.");
// 		} catch (AccessException e) {
// 			System.out.println("Erreur: " + e.getMessage());
// 		} catch (RemoteException e) {
// 			System.out.println("Erreur: " + e.getMessage());
// 		}

// 		return stub;
// 	}

//     private void connexion(){

//         try {
//             clientId = localServerStub.openSession("Mathieu");
//         } catch (RemoteException e) {
//             System.out.println("Erreur: " + e.getMessage());
//         }
//     }

// }
