package ca.polymtl.inf8480.tp1.server;

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


import ca.polymtl.inf8480.tp1.shared.ServerInterface;

public class Server implements ServerInterface {

    private int numberOfClients;

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public Server() {
        super();

        numberOfClients = 0;
        // Creation de toutes les boites de reception.
        createInbox();
        try {
            // Creation du fichier de metadonne lock.
            String fileName = ServerInterface.userPath.concat("server/lock.txt");
            File file = new File(fileName);

            FileWriter f2 = new FileWriter(file, false);
            f2.write("0");
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
			System.err.println("Erreur1: " + e);
		}
	}

    // Creation de toutes les boites de reception pour tout les usagers.
    // Nous creons simplement un dossier associer a leur login dans le dossier server
    private void createInbox() {

        try {
            String fileName = ServerInterface.userPath.concat("server/user.txt");
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while((line = br.readLine()) != null){
                //process the line
                String[] parts = line.split(" ");
                new File(ServerInterface.userPath.concat("server/").concat(parts[0])).mkdirs();
            }

            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

    }

    // Si l'identifiant et le mot de passe passe par l'utilisateur est le meme, nous pouvons accorder l'acces a lutilisateur.
    // Si la connexion sest bien fait, un numero de client est associer au client. 
    @Override
    public int openSession(String login, String password) throws RemoteException {
        int clientId = 0;
        try {
            String fileName = ServerInterface.userPath.concat("server/user.txt");
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while((line = br.readLine()) != null){
                //process the line
                String[] parts = line.split(" ");
                if (login.equals(parts[0]) && password.equals(parts[1])){
                    numberOfClients += 1;
                    clientId = numberOfClients;
                    System.out.println("Connexion de l'utilisateur " + login + " avec ID " + String.valueOf(clientId));
                }
            }

            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return clientId;
    }

    // Afin de verrouiller la liste de groupe pour publier, le client appel cette fonction.
    // Le lock est un fichier. Il ne contient qu'un seul nombre associer au numero de client si il est verrouiller par celui-ci ...
    // ... sinon, si il est disponible a tous, le fichier un 0.
    // Le fichier est creer automatiquement lorsqu on lance le serveur.
    @Override
    public boolean lockGroupList(int clientId) throws RemoteException {
        boolean locked = false;

        try {
            String fileName = ServerInterface.userPath.concat("server/lock.txt");
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();

            if (line.equals("0") || line.equals(String.valueOf(clientId))) {
                locked = true;
                FileWriter f2 = new FileWriter(file, false);
                f2.write(String.valueOf(clientId));
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

    // On reecrit completement la liste de groupe serveur avec celle passer par lutilisateur.
    // On rend disponible le lock a tout les utilisateurs. Tout le monde peut alors verrouiller le fichier.
    @Override
    public void pushLocalGroupList(String localFile) throws RemoteException {
        try {
            String lockFileName = ServerInterface.userPath.concat("server/lock.txt");
            File lockFile = new File(lockFileName);

            FileWriter f2 = new FileWriter(lockFile, false);
            f2.write("0");
            f2.close();

            File fileToRewrite = new File(ServerInterface.userPath.concat("server/groupServer.txt"));
            FileWriter f3 = new FileWriter(fileToRewrite, false);
            f3.write(localFile);
            f3.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        System.out.println("Mise à jour de la liste des groupes serveur");
    }

    // Cette fonction compare le checksum du fichier grouple du client avec le sien.
    // Elle renvoie un booleen dependamement du resultat. 
    @Override
    public boolean getGroupList(String clientChecksum) throws RemoteException {

        boolean sameFile = false;

        try {

            byte[] b = Files.readAllBytes(Paths.get(ServerInterface.userPath.concat("server/groupServer.txt")));
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

    // Renvoie la liste des groupes du serveur au client.
    @Override
    public String getServerGroupList() throws RemoteException {

        String groupListContent = "";

        try {
            String fileName = ServerInterface.userPath.concat("server/groupServer.txt");
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while((line = br.readLine()) != null){
                groupListContent += line;
                groupListContent += "\n";
            }

            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return groupListContent;
    }

    // Cette fonction gere l'envoie dun message si l'adresse du destinateur est un groupe.
    @Override
    public boolean sendMail(String subject, String addrDest, String content, String clientAdress) throws RemoteException {
        
        // On met tout les groupes dans un tableau (pour que ca soit plus facile a gerer par la suite.)
        ArrayList<ArrayList<String>> groupList = new ArrayList<ArrayList<String>>();
        try {
            File file = new File(ServerInterface.userPath.concat("server/groupServer.txt"));
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                ArrayList<String> tempList = new ArrayList<String>();
                for (String part: parts){
                    // Ajout au tableau des groupes
                    tempList.add(part);
                }
                groupList.add(tempList);
            }
            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        // On parcoure la liste des groupes, si l'adresse est un groupe, on envoie un message a tout ses usagers.
        for (ArrayList<String> group : groupList){
            if (group.get(0).equals(addrDest)){
                boolean emailSent = true;
                for (String user: group){
                    if (!user.equals(addrDest)){
                        // Le retour est afin de gerer les erreurs. Si un email n a pas pu etre envoyer, emailSent est faux.
                        emailSent = emailSent && sendToUser(subject, user, content, clientAdress);
                    }
                }
                return emailSent;
            }
        }
        // Si l'adresse n'est pas un groupe, on l'envoie au bon usager.
        return sendToUser(subject, addrDest, content, clientAdress);
    }

    // Envoie du message a un utilsateur. Appeler par sendMail()
    // Retourne un booleen si le message a bien ete expedie.
    private boolean sendToUser(String subject, String addrDest, String content, String clientAdress){

        // On place tout les utilisateurs dans un tableau afin de savoir lorsqu'on envoie, il existe.
        ArrayList<String> userList = new ArrayList<String>();
        try {
            File file = new File(ServerInterface.userPath.concat("server/user.txt"));
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                userList.add(parts[0]);
            }
            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        for (String user : userList){
            //  si l'utilisateur existe dans le tableau precedemment enregistrer.
            if (user.equals(addrDest)){
                try {
                    // Creation du message. Il s'agit en fait d'un fichier .txt dans le repertoire de l'utilsateur (boite de reception)
                    // Afin d'enregistrer si le courriel a ete lu, et son expediteur, nous les enregistrons dans les deux premieres lignes.
                    String fileName = ServerInterface.userPath + "server/" + addrDest + "/" + subject + ".txt";
                    File file = new File(fileName);
                        
                    FileWriter f2 = new FileWriter(file, true);
                    // Premiere ligne : unread ou read
                    f2.write("unread\n");
                    // Deuxieme ligne : Expediteur
                    f2.write(clientAdress + "\n");
                    // Reste : Contenu du message
                    f2.write(content);
                    f2.close();
        
                } catch (FileNotFoundException e) {
                    System.out.println("Erreur: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Erreur: " + e.getMessage());
                }
                return true;
            }
        }
        return false;
    }

    // Liste tout les messages dans la boite de reception (dossier du client)
    // L'affichage est important.
    @Override
    public String listMails(boolean justUnread, String clientAdress) throws RemoteException {

        String inboxList = "";
        File folder = new File(ServerInterface.userPath + "server/" + clientAdress);
        int numberOfFile = 0;
        int numberOfUnreadFile = 0;

        // Tout les fichiers dans le dossier (boite de reception)
        for (File fileEntry : folder.listFiles()) {
            String fileName = fileEntry.getName();
            String subject = fileName.split(".txt")[0];
            String status = "";
            String sender = "";

            try {
                FileReader fr = new FileReader(fileEntry);
                BufferedReader br = new BufferedReader(fr);
                status = br.readLine();
                sender = br.readLine();
                br.close();
            } catch (FileNotFoundException e) {
                System.out.println("Erreur: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("Erreur: " + e.getMessage());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm");
            String dateTime = sdf.format(fileEntry.lastModified());

            if (status.equals("unread")){
                status = "N";
                // Nombre de fichiers non lus dans le repertoire.
                numberOfUnreadFile += 1;
            } else {
                status = "-";
            }
            // Nombre de fichier dans le repertoire.
            numberOfFile += 1;

            if (justUnread){
                if (status.equals("N")){
                    inboxList += (String.valueOf(numberOfUnreadFile) + " " + status + "   " + sender + "  " + dateTime + "    " + subject + "\n");
                }
            } else {
                inboxList += (String.valueOf(numberOfFile) + "  " + status + "   " + sender + "  " + dateTime + "    " + subject + "\n");
            }
        }
        String returnMessage = "";
        if (justUnread){
            returnMessage = String.valueOf(numberOfUnreadFile) + " courriers sont non-lus\n";
        } else {
            returnMessage = String.valueOf(numberOfFile) + " courriers dont " + String.valueOf(numberOfUnreadFile) + " sont non-lus\n";
        }

        returnMessage += inboxList;

        return returnMessage;
    }

    // Supprime le fichier dans la boite de reception (dans le dossier du bon usager)
    @Override
    public boolean deleteMail(int mailNumber, String clientAdress) throws RemoteException {

        boolean deleted = false;
        int fileNumber = 0;
        File folder = new File(ServerInterface.userPath + "server/" + clientAdress);

        for (File fileEntry : folder.listFiles()) {

            fileNumber += 1;

            if (fileNumber == mailNumber) {
                if (fileEntry.delete()){
                    deleted = true;
                }
            }
        }

        return deleted;
    }

    // Lecture d'un fichier, qu'on retourne au client
    // On recoit un nombre qu'il faut alors comparer avec la meme liste qu'on possede.
    // Dans le fichier, on reecrit que le message a ete lu.
    @Override
    public String readMail(int mailNumber, String clientAdress) throws RemoteException {
        String emailContent = "";
        String readEmail = "read\n";

        int fileNumber = 0;
        File folder = new File(ServerInterface.userPath + "server/" + clientAdress);

        for (File fileEntry : folder.listFiles()) {

            fileNumber += 1;

            if (fileNumber == mailNumber) {
                try {
                    FileReader fr = new FileReader(fileEntry);
                    BufferedReader br = new BufferedReader(fr);
                    
                    br.readLine();
                    readEmail += br.readLine() + "\n";
                    String line;
        
                    while((line = br.readLine()) != null){
                        emailContent += line + "\n";
                    }
                    readEmail += emailContent;
                    br.close();

                    // On reeecrit que le fichier a ete lu.
                    // Pour dire qu'il a ete lu.
                    FileWriter f2 = new FileWriter(fileEntry, false);
                    f2.write(readEmail);
                    f2.close();

                } catch (FileNotFoundException e) {
                    System.out.println("Erreur: " + e.getMessage());
                } catch (IOException e) {
                    System.out.println("Erreur: " + e.getMessage());
                }
            }
        }
        return emailContent;
    }

    // On cherche le mot cle a travers tout les messages (fichiers) de la boite de reception (dossier)
    // Affichage specifique.
    @Override
    public String searchMail(String keywords, String clientAdress) throws RemoteException {

        File folder = new File(ServerInterface.userPath + "server/" + clientAdress);
        int mailNumber = 0;
        String inboxString = "";
        int numberOfFile = 0;

        // Pour tout les fichiers
        for (File fileEntry : folder.listFiles() ) {
            mailNumber += 1;
            boolean added = false;

            // Pour tout les mots cles passe en parametre. (tous separes par un espace)
            for (String keyword : keywords.split(" ")) {
                // Regarde si le fichier contient le mot passe en parametre (pas de doublons de fichiers)
                if (readMail(mailNumber, clientAdress).contains(keyword) && !added){
                    String fileName = fileEntry.getName();
                    String subject = fileName.split(".txt")[0];
                    String status = "";
                    String sender = "";
        
                    try {
                        FileReader fr = new FileReader(fileEntry);
                        BufferedReader br = new BufferedReader(fr);
                        status = br.readLine();
                        sender = br.readLine();
                        br.close();
                    } catch (FileNotFoundException e) {
                        System.out.println("Erreur: " + e.getMessage());
                    } catch (IOException e) {
                        System.out.println("Erreur: " + e.getMessage());
                    }
        
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm");
                    String dateTime = sdf.format(fileEntry.lastModified());
        
                    if (status.equals("unread")){
                        status = "N";
                    } else {
                        status = "-";
                    }
                    numberOfFile += 1;

                    inboxString += (String.valueOf(mailNumber) + "    " + status + "   " + sender + "  " + dateTime + "    " + subject + "\n");
                    added = true;
                }
            }
        }

        String returnMessage = String.valueOf(numberOfFile) + " courriers qui correspondent à votre recherche sont trouvés.\n";
        returnMessage += inboxString;

        return returnMessage;
    }

}
