package ca.polymtl.inf8480.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.lang.Math.*;
import java.util.InputMismatchException;
import java.util.Scanner;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;

public class Client {

	public static void main(String[] args) {
		String distantHostname = null;

		if (args.length > 0) {
			distantHostname = args[0];
		}

		Client client = new Client(distantHostname);
		client.menu();
	}
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
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
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        
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
                    break;
                case "7":
                    System.out.println("Create Group");
                    break;
                case "8":
                    System.out.println("Join Group");
                    break;
                case "9":
                    System.out.println("Publish Group List");
                    break;
                case "0":
                    System.out.println("Exit");
                    break;
                default:
                    System.out.println("Opération invalide.\nChoisissez un commande de 0 à 9");
                    break;
            }
        }
        reader.close();
    }

	private void run() {

		if (localServerStub != null) {
			appelRMILocal();
		}

		// if (distantServerStub != null) {
		// 	appelRMIDistant();
		// }
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

	private void appelRMILocal() {
		try {
			long start = System.nanoTime();
			localServerStub.execute();
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI local: " + (end - start)
					+ " ns");
			System.out.println("Résultat appel RMI local: ");
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	// private void appelRMIDistant() {
	// 	try {
	// 		long start = System.nanoTime();
	// 		distantServerStub.execute(1);
	// 		long end = System.nanoTime();

	// 		System.out.println("Temps écoulé appel RMI distant: "
	// 				+ (end - start) + " ns");
	// 		System.out.println("Résultat appel RMI distant: " + String.valueOf(bytes.length));
	// 	} catch (RemoteException e) {
	// 		System.out.println("Erreur: " + e.getMessage());
	// 	}
	// }
}
