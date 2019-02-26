package tp2.calcul;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.io.BufferedReader;


import tp2.partage.CalculInterface;

public class Calcul implements CalculInterface {

    private int numberOfClients;

	public static void main(String[] args) {
		Calcul calcul = new Calcul();
		calcul.run();
	}

	public Calcul() {
        super();

        numberOfClients = 0;
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
            CalculInterface stub = (CalculInterface) UnicastRemoteObject.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("calcul", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur1: " + e);
		}
	}

    // Si l'identifiant et le mot de passe passe par l'utilisateur est le meme, nous pouvons accorder l'acces a lutilisateur.
    // Si la connexion sest bien fait, un numero de client est associer au client. 
    @Override
    public int openSession(String login) throws RemoteException {
        numberOfClients += 1;
        int clientId = numberOfClients;
        
        System.out.println("Connexion de l'utilisateur " + login + " avec ID " + String.valueOf(clientId));

        return clientId;
    }
}
