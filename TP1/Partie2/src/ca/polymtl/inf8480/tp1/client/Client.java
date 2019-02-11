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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import ca.polymtl.inf8480.tp1.shared.ServerInterface;

public class Client {

    private Scanner reader;         // Permet de lire les donnees du terminal
    private boolean ableToPublish;  // Pour le lock principalement
    private int clientId;           // Afin de savoir quel numero de client nous sommes
    private String login = "";      // Le nom de notre login

	public static void main(String[] args) {
        String distantHostname = null;

		if (args.length > 0) {
			distantHostname = args[0];
		}

        // Connexion au niveau du serveur distant
        Client client = new Client(distantHostname);
        client.connexion();
        if (client.clientId != 0){
            System.out.println("Connexion réussie");
            client.menu();
        } else {
            System.out.println("Connexion échouée");
        }

	}
	private ServerInterface localServerStub = null;
	// private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
        super();
        
        // Permet de lire les donnees du terminal
        reader = new Scanner(System.in);  // Reading from System.in
        ableToPublish = false;

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		// localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			localServerStub = loadServerStub(distantServerHostname);
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

    private void connexion(){
        String password = "";

        System.out.println(" --- Connexion --- ");
        System.out.println(" Entrée votre email ");
        login = reader.next();
        System.out.println(" Entrée votre mot de passe ");
        password = reader.next();

        try {
            clientId = localServerStub.openSession(login, password);
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    private void printMenu() {

        System.out.println(" --- Menu --- ");
        System.out.println(" 1  - Read ");
        System.out.println(" 2  - Send ");
        System.out.println(" 3  - Delete ");
        System.out.println(" 4  - List ");
        System.out.println(" 5  - Search ");
        System.out.println(" 6  - Lock Group List ");
        System.out.println(" 7  - Create Group ");
        System.out.println(" 8  - Join Group ");
        System.out.println(" 9  - Publish Group List ");
        System.out.println(" 10 - Get Group List ");
        System.out.println(" 0  - Exit ");
    }
    
    private void menu() {
        String command = "";
        
        while(!command.equals("0")) {
            printMenu();
            try {
                command = reader.next();
            } catch (InputMismatchException e) {
            } catch (NoSuchElementException e) {
            }

            switch (command) {
                case "1":
                    System.out.println("read");
                    read();
                    break;
                case "2":
                    System.out.println("send");
                    send();
                    break;
                case "3":
                    System.out.println("delete");
                    delete();
                    break;
                case "4":
                    System.out.println("list");
                    list();
                    break;
                case "5":
                    System.out.println("search");
                    search();
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
                    joinGroup();
                    break;
                case "9":
                    System.out.println("Publish Group List");
                    publishGroup();
                    break;
                case "10":
                    System.out.println("Get Group List");
                    updateGroupList();
                    break;
                case "0":
                    System.out.println("Exit");
                    break;
                default:
                    System.out.println("Opération invalide.\nChoisissez un commande de 0 à 10");
                    break;
            }
        }
    }

    // Afin de creer un groupe au niveau local
    private void createGroup() {
        String groupName = "";
        Path path = Paths.get(ServerInterface.userPath.concat("client/groupClient.txt"));

        System.out.println(" Entrer le nom du groupe ");
        groupName = reader.next();

        // Si le fichier n existe pas, on le cree et on ecrit dedans
        // Sinon on ajoute a la fin du fichier.
        try {
            Files.write(path, Arrays.asList(groupName.concat("@poly.ca")), StandardCharsets.UTF_8,
                Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        System.out.println("Le groupe " + groupName + "@poly.ca est créé avec succès.");
    }

    // Afin d ajouter un utilisateur a notre liste de contact, il faut lire le fichier de groupe ...
    // ... metre tout les composantes dans un tableau, ajouter le nouvel utilisateur au tableau ...
    // ... et tout reecrire dans le fichier
    private void joinGroup() {

        String groupName = "";
        String userName = "";
        boolean userAdded = false;
        ArrayList<ArrayList<String>> groupList = new ArrayList<ArrayList<String>>();

        System.out.println(" Entrer le nom du groupe :");
        groupName = reader.next().concat("@poly.ca");

        System.out.println(" Entrer le nom de l'utilisateur :");
        userName = reader.next().concat("@poly.ca");

        // Lecture du fichier pour mettre dans le tableau
        try {
            File file = new File(ServerInterface.userPath.concat("client/groupClient.txt"));
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                ArrayList<String> tempList = new ArrayList<String>();
                boolean userAlreadyExists = false;
                for (String part: parts){

                    if (part.equals(userName)){
                        userAlreadyExists = true;
                    }
                    // Ajout au tableau des groupes
                    tempList.add(part);
                }
                if (parts[0].equals(groupName) && !userAlreadyExists){
                    tempList.add(userName);
                    userAdded = true;
                    System.out.println("Le contact " + userName + " est ajouté avec succès au groupe " + groupName);
                }
                groupList.add(tempList);
            }
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        if (!userAdded) {
            System.out.println("L'utilisateur n'a pas pu être ajoute");
        }

        // Réécriture des groupes dans le fichier groupClient
        try {
            File file = new File(ServerInterface.userPath.concat("client/groupClient.txt"));
            file.delete();
            FileWriter f2 = new FileWriter(file, true);
            
            for (ArrayList<String> group: groupList){
                for (String user: group){
                    f2.write(String.valueOf(user));
                    f2.write(" ");
                }
                f2.write("\n");
            }
            f2.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }

    // Le lock vient verrouille qu un seul utilisateur a la fois pousse les changements de liste des groupes sur le serveur.
    private void lock() {
        boolean return_bool = false;
        try {
            // Envoie de son clientID pour savoir si la liste est disponible pour verrouillage
            return_bool = localServerStub.lockGroupList(clientId);
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

    // Afin de publier la liste des groupes, il faut l avoir precedement verrouillee.
    // Par la suite, afin de ne pas publier inutilement la nouvelle liste de groupe, nous regardons le cheksum afin de savoir si les deux fichiers sont identiques des deux cotes.
    // Si ils ne sont pas identiques, nous publions la liste au niveau serveur
    // Lorsqu'on publie, on ne peut plus publier. Le fichier lock est alors disponible a tous.
    private void publishGroup() {

        String fileName = ServerInterface.userPath.concat("client/groupClient.txt");
        String groupListContent = "";

        if (ableToPublish){
            try {
                if (!localServerStub.getGroupList(getFileChecksum(Paths.get(fileName)))) {

                    File file = new File(fileName);
                    FileReader fr = new FileReader(file);
                    BufferedReader br = new BufferedReader(fr);
                    String line;
                    while((line = br.readLine()) != null){
                        groupListContent += line;
                        groupListContent += "\n";
                    }
                    br.close();

                    localServerStub.pushLocalGroupList(groupListContent);
                    System.out.println("Les modifications apportées à la liste de groupes globale sont publiées avec succès.");
                }
            } catch (RemoteException e) {
                System.out.println("Erreur: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Erreur: " + e.getMessage());
            }
        } else {
            System.out.println("La liste n'a pas pu être publie. Elle n'a pas été verouillee");
        }
        ableToPublish = false;
    }


    // MD5, On regarde le hash du fichier passer en parametre.
    private String getFileChecksum(Path path) {
        byte[] hash = new byte[0];
        try {
            byte[] b = Files.readAllBytes(path);
            hash = MessageDigest.getInstance("MD5").digest(b);
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return DatatypeConverter.printHexBinary(hash);
    }


    // Afin de faire un "pull" du fichier des groupes du serveur, si les fichiers sont differents (cheksum). 
    private void updateGroupList() {

        String filePath = ServerInterface.userPath.concat("client/groupClient.txt");
        File file = new File(filePath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        try {
            if (!localServerStub.getGroupList(getFileChecksum(Paths.get(filePath)))) {
                overwriteFile();
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }
    
    // Overwrite du fichier des groupes clients apres avoir appeler la fonction updateGroupList()
    private void overwriteFile() {
        System.out.println("Mise à jour de la liste des groupes locales");

        File fileToRewrite = new File(ServerInterface.userPath.concat("client/groupClient.txt"));
        try {
            String groupContent = localServerStub.getServerGroupList();
            FileWriter f2 = new FileWriter(fileToRewrite, false);
            f2.write(groupContent);
            f2.close();
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
    }


    // Afin d evoyer un message dun client a un autre. 
    // Creation dun message (String), en passant les parametres (subject, addrDest, content, login)
    private void send() {

        System.out.println(" Entrer le destinataire du message : ");
        String addrDest = reader.next();

        System.out.println(" Entrer le sujet du message : ");
        String subject = reader.next();

        System.out.println(" Entrer le contenu du message (Pour Quitter : ctrl + D) ");
        String content = "";
        String line;
        try {
            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            while((line = systemIn.readLine()) != null) {
                content += line;
                content += "\n";
            }
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        reader.nextLine();

        try {

            if (localServerStub.sendMail(subject, addrDest, content, login)) {
                System.out.println("Courrier envoyé avec succès à " + addrDest);
            } else {
                System.out.println("Courrier n'a pas pu être envoyé à tous les destinataires.");
            }

        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

    }

    // Afin de lister tout les messages recus par l'usager (option d'afficher les options des fichiers non lus.)
    private void list() {

        System.out.println("Voulez vous afficher seulement les messages non lu ? (o/n) : ");
        String unread = reader.next().toLowerCase();
        boolean justUnread = true;
        String inboxList = "";

        while(!unread.equals("o") && !unread.equals("n") && !unread.equals("oui") && !unread.equals("non")) {
            System.out.println("Entrer oui ou non : ");
            unread = reader.next().toLowerCase();
        }
        if (unread.equals("non") || unread.equals("n")){
            justUnread = false;
        }

        try {
            inboxList = localServerStub.listMails(justUnread, login);
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        System.out.println(inboxList);
    }

    // Afin de lire un message.
    private void read() {

        try {
            String inboxList = localServerStub.listMails(false, login);
            System.out.println(inboxList);

            System.out.println("Lire le courrier numéro : - ");
            String mailNumber = reader.next();

            String emailContent = localServerStub.readMail(Integer.valueOf(mailNumber), login);
            if (emailContent.equals("")){
                System.out.println("Fichier introuvable");
            } else {
                System.out.println(emailContent);
            }

        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Erreur: " + e.getMessage() + ": Mauvaise entree");
        }
    }

    // Pour supprimer un message (message de confirmation)
    private void delete() {

        try {
            String inboxList = localServerStub.listMails(false, login);
            System.out.println(inboxList);

            System.out.println("Supprimer le courrier numéro : - ");
            String mailNumber = reader.next();

            System.out.println("Etes vous sur (o/n) ?");
            String confirmation = reader.next().toLowerCase();

            while(!confirmation.equals("o") && !confirmation.equals("n") && !confirmation.equals("oui") && !confirmation.equals("non")) {
                System.out.println("Entrer oui ou non : ");
                confirmation = reader.next().toLowerCase();
            }
            if (confirmation.equals("oui") || confirmation.equals("o")){
                if (localServerStub.deleteMail(Integer.valueOf(mailNumber), login)) {
                System.out.println("Le fichier a été supprimé avec succès");
                } else {
                System.out.println("Erreur de suppresion. Mauvais input ou erreur inconnue.");
                }
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Erreur: " + e.getMessage() + ": Mauvaise entree");
        }
    }

    // Pour chercher un mot dans les messages de la boite de reception
    private void search() {
        String inboxList = "";
        System.out.println("Mot(s) cle(s) a rechercher : ");
        System.out.println("Pour plusiers mots, il faut les separer par un espace. ");
        System.out.println("Ex : souvenir bonjour");
        
        String keywords = "";

        try {
            BufferedReader systemIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            keywords = systemIn.readLine();
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        try {
            inboxList = localServerStub.searchMail(keywords, login);
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        System.out.println(inboxList);

        // Si au moins un message a ete retourner
        // On peut lire les messages
        if (!inboxList.substring(0, 1).equals("0")){
            System.out.println("Lire le courrier numéro : -");
            String mailNumber = reader.next();
            String emailContent = "";
            try {
                emailContent = localServerStub.readMail(Integer.valueOf(mailNumber), login);
            } catch (RemoteException e) {
                System.out.println("Erreur: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Erreur: " + e.getMessage() + ": Mauvaise entree");
            }

            if (emailContent.equals("")){
                System.out.println("Fichier introuvable");
            } else {
                System.out.println(emailContent);
            }

        }
    }
}
