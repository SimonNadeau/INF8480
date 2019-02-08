package ca.polymtl.inf8480.tp1.server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedReader;


import ca.polymtl.inf8480.tp1.shared.ServerInterface;

public class Server implements ServerInterface {

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public Server() {
        super();

        try {
            String fileName = ServerInterface.userPath.concat("Server/lock.txt");
            File file = new File(fileName);

            FileWriter f2 = new FileWriter(file, false);
            f2.write("false");
            f2.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
            ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/*
	 * Méthode accessible par RMI. Ne fait rien, mais prend un tableau de taille variable en argument.
	 */
	@Override
	public void execute() throws RemoteException {
    }

    @Override
    public boolean openSession(String login, String password) throws RemoteException {
        boolean bool_login = false;

        try {
            String fileName = ServerInterface.userPath.concat("Server/user.txt");
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while((line = br.readLine()) != null){
                //process the line
                String[] parts = line.split(" ");
                if (login.equals(parts[0]) && password.equals(parts[1])){
                    bool_login = true;
                    System.out.println("Connexion de l'utilisateur " + login);
                }
            }

            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return bool_login;
    }

    @Override
    public boolean lockGroupList() throws RemoteException {
        boolean locked = false;

        try {
            String fileName = ServerInterface.userPath.concat("Server/lock.txt");
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);

            if (br.readLine().equals("false")) {
                locked = true;
                FileWriter f2 = new FileWriter(file, false);
                f2.write("true");
                f2.close();

            }

            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return locked;
    }

    @Override
    public void pushGroupList() throws RemoteException {
        try {
            String fileName = ServerInterface.userPath.concat("Server/lock.txt");
            File file = new File(fileName);

            FileWriter f2 = new FileWriter(file, false);
            f2.write("false");
            f2.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        System.out.println("Liste de groupe a été publié.");
    }

    @Override
    public boolean getGroupList(String clientChecksum) throws RemoteException {

        boolean sameFile = false;

        try {

            byte[] b = Files.readAllBytes(Paths.get(ServerInterface.userPath.concat("Server/groupServer.txt")));
            byte[] hash = MessageDigest.getInstance("MD5").digest(b);
            String serverChecksum = DatatypeConverter.printHexBinary(hash);

            sameFile = serverChecksum.equals(clientChecksum);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return sameFile;
    }

    @Override
    public File getServerGroupList() throws RemoteException {
        try {
            Files.copy(Paths.get(ServerInterface.userPath.concat("Server/groupServer.txt")),
             Paths.get(ServerInterface.userPath.concat("Server/groupServer1.txt")));
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return new File(ServerInterface.userPath.concat("Server/groupServer1.txt"));
    }


    @Override
    public String send() throws RemoteException {
        return "Only one at a time";
    }


}
