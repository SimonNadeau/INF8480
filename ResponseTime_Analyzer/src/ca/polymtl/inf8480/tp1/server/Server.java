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
        createInbox();
        try {
            String fileName = ServerInterface.userPath.concat("Server/lock.txt");
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
			System.err.println("Erreur: " + e.getMessage());
		}
	}

    private void createInbox() {

        try {
            String fileName = ServerInterface.userPath.concat("Server/user.txt");
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;

            while((line = br.readLine()) != null){
                //process the line
                String[] parts = line.split(" ");
                new File(ServerInterface.userPath.concat("Server/").concat(parts[0])).mkdirs();
            }

            br.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

    }


    @Override
    public int openSession(String login, String password) throws RemoteException {
        int clientId = 0;
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

    @Override
    public boolean lockGroupList(int clientId) throws RemoteException {
        boolean locked = false;

        try {
            String fileName = ServerInterface.userPath.concat("Server/lock.txt");
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

    @Override
    public void pushLocalGroupList(File localFile) throws RemoteException {
        try {
            String lockFileName = ServerInterface.userPath.concat("Server/lock.txt");
            File lockFile = new File(lockFileName);

            FileWriter f2 = new FileWriter(lockFile, false);
            f2.write("0");
            f2.close();

        } catch (FileNotFoundException e) {
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        File fileToDelete = new File(ServerInterface.userPath.concat("Server/groupServer.txt"));
        fileToDelete.delete();

        System.out.println("Mise à jour de la liste des groupes serveur");

        localFile.renameTo(fileToDelete);  
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
    public boolean sendMail(String subject, String addrDest, String content, String clientAdress) throws RemoteException {
        
        ArrayList<ArrayList<String>> groupList = new ArrayList<ArrayList<String>>();
        try {
            File file = new File(ServerInterface.userPath.concat("Server/groupServer.txt"));
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
        for (ArrayList<String> group : groupList){
            if (group.get(0).equals(addrDest)){
                boolean emailSent = true;
                for (String user: group){
                    if (!user.equals(addrDest)){
                        emailSent = emailSent && sendToUser(subject, user, content, clientAdress);
                    }
                }
                return emailSent;
            }
        }
        return sendToUser(subject, addrDest, content, clientAdress);
    }

    private boolean sendToUser(String subject, String addrDest, String content, String clientAdress){

        ArrayList<String> userList = new ArrayList<String>();
        try {
            File file = new File(ServerInterface.userPath.concat("Server/user.txt"));
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
            if (user.equals(addrDest)){
                try {
                    String fileName = ServerInterface.userPath + "Server/" + addrDest + "/" + subject + ".txt";
                    File file = new File(fileName);
                        
                    FileWriter f2 = new FileWriter(file, true);
                    f2.write("unread\n");
                    f2.write(clientAdress + "\n");
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

    @Override
    public String listMails(boolean justUnread, String clientAdress) throws RemoteException {

        String inboxList = "";
        File folder = new File(ServerInterface.userPath + "Server/" + clientAdress);
        int numberOfFile = 0;
        int numberOfUnreadFile = 0;


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
                numberOfUnreadFile += 1;
            } else {
                status = "-";
            }
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

    @Override
    public boolean deleteMail(int mailNumber, String clientAdress) throws RemoteException {

        boolean deleted = false;
        int fileNumber = 0;
        File folder = new File(ServerInterface.userPath + "Server/" + clientAdress);

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

    @Override
    public String readMail(int mailNumber, String clientAdress) throws RemoteException {
        String emailContent = "";
        String readEmail = "read\n";

        int fileNumber = 0;
        File folder = new File(ServerInterface.userPath + "Server/" + clientAdress);

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

                    fileEntry.delete();
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

    @Override
    public String searchMail(String keywords, String clientAdress) throws RemoteException {

        File folder = new File(ServerInterface.userPath + "Server/" + clientAdress);
        int mailNumber = 0;
        String inboxString = "";
        int numberOfFile = 0;

        for (File fileEntry : folder.listFiles() ) {
            mailNumber += 1;
            boolean added = false;

            for (String keyword : keywords.split(" ")) {
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
