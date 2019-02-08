package ca.polymtl.inf8480.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.lang.Math.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;

public class Client {

    private Scanner reader;
    private boolean ableToPublish;
	public static void main(String[] args) {
        String distantHostname = null;

		if (args.length > 0) {
			distantHostname = args[0];
		}

        Client client = new Client(distantHostname);
        if (client.connexion()){
            System.out.println("Connexion réussie");
            client.menu();
        } else {
            System.out.println("Connexion échouée");
        }

	}
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
        super();
        
        reader = new Scanner(System.in);  // Reading from System.in
        ableToPublish = false;

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
    }

    private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
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

    private boolean connexion(){
        String login = "";
        String password = "";
        boolean connexion_bool = false;

        System.out.println(" --- Connexion --- ");
        System.out.println(" Entrée votre email ");
        login = reader.next();
        System.out.println(" Entrée votre mot de passe ");
        password = reader.next();

        try {
            connexion_bool = localServerStub.openSession(login, password);
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return connexion_bool;
    }

    private void printMenu() {

        System.out.println(" --- Menu --- ");
        System.out.println(" 1 - Read ");
        System.out.println(" 2 - Send ");
        System.out.println(" 3 - Delete ");
        System.out.println(" 4 - List ");
        System.out.println(" 5 - Search ");
        System.out.println(" 6 - Lock Group List ");
        System.out.println(" 7 - Create Group ");
        System.out.println(" 8 - Join Group ");
        System.out.println(" 9 - Publish Group List ");
        System.out.println(" 0 - Exit ");
    }
    
    private void menu() {
        String command = "";
        
        while(!command.equals("0")) {
            printMenu();
            try {
                command = reader.next();
            } catch (InputMismatchException e) {
            }

            switch (command) {
                case "1":
                    System.out.println("read");
                    break;
                case "2":
                    System.out.println("send");
                    try {
                        System.out.println(localServerStub.send());
                    } catch (RemoteException e) {
                        System.out.println("Erreur: " + e.getMessage());
                    }
                    break;
                case "3":
                    System.out.println("delete");
                    break;
                case "4":
                    System.out.println("list");
                    break;
                case "5":
                    System.out.println("search");
                    break;
                case "6":
                    System.out.println("Lock Group List");
                    lock();
                    break;
                case "7":
                    System.out.println("Create Group");
                    createGroup();
                    break;
                case "8":
                    System.out.println("Join Group");
                    updateGroupList();
                    break;
                case "9":
                    System.out.println("Publish Group List");
                    publishGroup();
                    break;
                case "0":
                    System.out.println("Exit");
                    break;
                default:
                    System.out.println("Opération invalide.\nChoisissez un commande de 0 à 9");
                    break;
            }
        }
    }

    private void createGroup() {
        String groupName = "";
        Path path = Paths.get(ServerInterface.userPath.concat("Client/groupClient.txt"));


        System.out.println(" Entrer le nom du groupe ");
        groupName = reader.next();

        try {
            Files.write(path, Arrays.asList(groupName.concat("@poly.ca")), StandardCharsets.UTF_8,
                Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        } catch (final IOException ioe) {
            // Add your own exception handling...
        }

        System.out.println("Le groupe " + groupName + "@poly.ca est créé avec succès.");
    }

    private void lock() {
        boolean return_bool = false;
        try {
            return_bool = localServerStub.lockGroupList();
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        if (return_bool) {
            System.out.println("La liste de groupes globale est verrouillée avec succès.");
            ableToPublish = true;
        } else {
            System.out.println("La liste de groupes globale est déjà verrouillée par un client.");
            ableToPublish = false;
        }
    }

    private void publishGroup() {
        if (ableToPublish){
            try {
                localServerStub.pushGroupList();
                System.out.println("Liste Publiee!");
            } catch (RemoteException e) {
                System.out.println("Erreur: " + e.getMessage());
            }
        } else {
            System.out.println("La liste n'a pas pu être publie. Elle n'a pas été verouillee");
        }
        ableToPublish = false;
    }

    private void updateGroupList() {

        try {
            byte[] b = Files.readAllBytes(Paths.get(ServerInterface.userPath.concat("Client/groupClient.txt")));
            byte[] hash = MessageDigest.getInstance("MD5").digest(b);
            if (!localServerStub.getGroupList(DatatypeConverter.printHexBinary(hash))) {
                overwriteFile();
            }
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

    }

    private void overwriteFile() {
        File fileToDelete = new File(ServerInterface.userPath.concat("Client/groupClient.txt"));
        fileToDelete.delete();
        try {
            File newFile = localServerStub.getServerGroupList();
            newFile.renameTo(fileToDelete);     
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }





	// private void run() {

	// 	if (localServerStub != null) {
	// 		appelRMILocal();
	// 	}

	// 	// if (distantServerStub != null) {
	// 	// 	appelRMIDistant();
	// 	// }
	// }

	// private void appelRMILocal() {
	// 	try {
	// 		long start = System.nanoTime();
	// 		localServerStub.execute();
	// 		long end = System.nanoTime();

	// 		System.out.println("Temps écoulé appel RMI local: " + (end - start)
	// 				+ " ns");
	// 		System.out.println("Résultat appel RMI local: ");
	// 	} catch (RemoteException e) {
	// 		System.out.println("Erreur: " + e.getMessage());
	// 	}
	// }

	// private void appelRMIDistant() {
	// 	try {
	// 		long start = System.nanoTime();
	// 		distantServerStub.execute();
	// 		long end = System.nanoTime();

	// 		System.out.println("Temps écoulé appel RMI distant: "
	// 				+ (end - start) + " ns");
	// 	} catch (RemoteException e) {
	// 		System.out.println("Erreur: " + e.getMessage());
	// 	}
	// }
}
