package tp2.repartiteur;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.lang.Math;

import tp2.partage.*;
import tp2.repartiteur.Task;

public class Repartiteur {

    private String username;
    private String password;
	
	public static void main(String args[]) {	
		// Fichier a lire
		String fileName = null;
		if (args.length > 0) {
			fileName = args[0];
		}

		// Creation du repartiteur
        Repartiteur repartiteur = new Repartiteur();
        
        // Authentification Client
        repartiteur.loginClient();

        // Calcul du resultat
        long debut = System.currentTimeMillis();
        int resultat = repartiteur.process(fileName);
        long fin = System.currentTimeMillis();

		// Affichage du résultat et du temps
        System.out.println("Resultat: " + String.valueOf(resultat));
        System.out.println("Temps: " + String.format("%d", fin - debut));

        System.exit(0);
	}

	private NomsInterface nomsServerStub = null;
	private ArrayList<CalculInterface> calculServerStubs = null;

	public Repartiteur() {
        super();
        
        username = "";
        password = "";

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

        // Connexion du repartiteur au serveur de noms pour recevoir la liste des serveurs de calcul.
		nomsServerStub = loadNomsServerStub(NomsInterface.ipServeurNoms);
		if (nomsServerStub != null) {
			calculServerStubs = new ArrayList<CalculInterface>();
			ArrayList<ArrayList<String>> calculServerInfos = null;
			try {
				calculServerInfos = nomsServerStub.getCalculServerInfos();

                // Ajout et connexion de tout les serveurs de calcul a une liste locale. 
				for (ArrayList<String> calculServerInfo : calculServerInfos) {

                    calculServerStubs.add(loadCalculServerStub(calculServerInfo.get(0), calculServerInfo.get(2)));
				}
			} catch (RemoteException e) {
				System.out.println("Erreur: " + e.getMessage());
			}
		}
	}

    // Connexion du repartiteur au serveur de nom.
	private NomsInterface loadNomsServerStub(String hostname) {
		NomsInterface stub = null;

		try {
            // Application sur le port 5000
			Registry registry = LocateRegistry.getRegistry(hostname, 5000);
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

    // Connexion du repartiteur au serveur de calcul selon leur nom.
	private CalculInterface loadCalculServerStub(String hostname, String registryName) {
		CalculInterface stub = null;

		try {
            // Application sur le port 5000
			Registry registry = LocateRegistry.getRegistry(hostname, 5000);
			stub = (CalculInterface) registry.lookup(registryName);
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
    
    // Au debut du programme du repartiteur, on demande a l'usager de se connecter.
    private void loginClient(){
        Scanner sc = new Scanner(System.in);

        System.out.println(" --- Connexion --- ");
        System.out.println(" Entrer votre username ");
        username = sc.next();
        System.out.println(" Entrer votre mot de passe ");
        password = sc.next();

        try {
            nomsServerStub.addClient(username, password);
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        sc.close();
    }

    // On commence l'application. On sépares deux sortes d'exécution, avec des serveurs malicieux ou non.
	private int process(String fileName) {
		if (detectMaliciousness()) {
			return insecureProcess(fileName);
		} else {
			return secureProcess(fileName);
		}
	}

    // Lorsqu'il n'y a aucun serveur malicieux
	private int secureProcess(String fileName) {
		int resultat = 0;
        
        // Creation des elements necessaire a un pool de thread.
        // Liste de taches a executer par differents threads.
		ArrayList<Task> tasks = new ArrayList<Task>();
        Executor executor = Executors.newCachedThreadPool();
		ExecutorCompletionService<Task.TaskInfo> execService = new ExecutorCompletionService<Task.TaskInfo>(executor);

        // Creation de la liste des operations et obtention des informations des serveurs de calcul.
        HashMap<String, ServerChunkSize> calculServerEssentials = buildCalculServerEssentials();
        ArrayList<String> listOperation = readFile(fileName);

        // Pour tout les serveurs de calculs
        calculServerEssentials.forEach((serveurNom, serverChunkSize) -> {

            // On separe la liste de tout les operations en sous liste de longueur maximum à un niveau acceptable de rejet. 
            List<String> operations = listOperation.subList(Math.max(listOperation.size() - serverChunkSize.getChunkSize(), 0), listOperation.size());
            int opSize = operations.size();

            // Creation de la tache et ajout de celles ci a la liste de taches.
            Task task = new Task(serveurNom, serverChunkSize.getServeurCalcul(), new ArrayList<String>(operations), -1, username, password);
            tasks.add(task);

            // Retrait de toute les operations deja ajoutés a la tache.
            for (int i = 0; i < opSize; i++) {
                listOperation.remove(listOperation.size() - 1);
            }
        });
        
        // Pour toute les taches dans la liste, on les ajoutes a la liste de l'executorService
        for (Task task: tasks){
            execService.submit(task);
        }

        // Tant que la liste des taches a execute n'est pas nulle.
        int tasksSize = tasks.size();
        while (tasksSize > 0) {
            try {
                // On prend le resultat d'une tache
                Task.TaskInfo partialResult = execService.take().get();

                // Selon le statut du resultat de la tache
                switch (partialResult.getStatut()) {

                    // Dans le case ou le serveur rmi est innacessible.
                    case RMI_EXCEPTION:
                        System.out.println("Serveur de calcul innaccessible");
                        // On enleve le serveur de la liste des serveurs accessible.
                        calculServerEssentials.remove(partialResult.getServeurNom());
                        tasksSize -= 1;
                        // On rajoute la liste des operations non effectué a la liste de toute les operations a effectuer.
                        listOperation.addAll(partialResult.getListOperation());

                        // Si aucun serveur de calcul restant
                        if (calculServerEssentials.isEmpty()){
                            System.out.println("Aucun serveur de calcul accessible");
                            System.exit(1);
                        }
                        break;

                    case BAD_NAMESERVER:
                        System.out.println("Mauvais serveur de noms");
                        System.exit(1);

                    case BAD_AUTHENTIFICATION:
                        System.out.println("Mauvaise authentification du repartieur");
                        System.exit(1);

                    // Dans le cas ou nous avons allouer trop de tache au serveur et qu'il n'avait pas assez de ressources
                    case REFUSED:
                        System.out.println("Tache refusee");
                        // On recree la meme tache avec les memes operations et on la remet dans la liste de taches a effectuer.
                        Task taskRefused = new Task(partialResult.getServeurNom(), 
                                             calculServerEssentials.get(partialResult.getServeurNom()).getServeurCalcul(),
                                             partialResult.getListOperation(), -1, username, password);
                        execService.submit(taskRefused);
                        break;

                    // Si on un resultat coherent.
                    case OK:
                        System.out.println("Tache executee");

                        // On ajoute ce resultat a notre liste.
                        resultat += partialResult.getResultat();
                        resultat %= 5000;

                        // Si la liste des operations n'est pas vide
                        if (!listOperation.isEmpty()){
                            // On recree une tache avec la prochaine sous liste d'operations
                            List<String> operations = listOperation.subList(
                                Math.max(listOperation.size() - calculServerEssentials.get(partialResult.getServeurNom()).getChunkSize(), 0),
                                listOperation.size());
                            int opSize = operations.size();
                            Task taskOk = new Task(partialResult.getServeurNom(), 
                                calculServerEssentials.get(partialResult.getServeurNom()).getServeurCalcul(),
                                new ArrayList<String>(operations), -1, username, password);

                            // On remet la tache a l'exceutor service
                            execService.submit(taskOk);

                            // On enleve la tache de la liste de taches.
                            for (int i = 0; i < opSize; i++) {
                                listOperation.remove(listOperation.size() - 1);
                            }

                        } else {
                            // Sinon on enleve la tache.
                            tasksSize -= 1;
                        }
                        break;

                    default:
                        break;
                }
                
            } catch (InterruptedException e) {
                System.out.println("Erreur: " + e.getMessage());
            } catch (ExecutionException e) {
                System.out.println("Erreur: " + e.getMessage());
            }
        }

        // Modulo 5000, juste au cas
		return resultat % 5000;
	}
    
    // Lorsqu'il y au moins un serveur malicieux
    // Très semblable a la fonction precedente. Ajout de commentaires pour les differences.
	private int insecureProcess(String fileName) {
        int resultat = 0;

        ArrayList<Task> tasks = new ArrayList<Task>();
        Executor executor = Executors.newCachedThreadPool();
		ExecutorCompletionService<Task.TaskInfo> execService = new ExecutorCompletionService<Task.TaskInfo>(executor);
        // Dans ce cas-ci, nous identifions les listes d'operations avec un id, afin de savoir quelles operations sont relies a quelle thread.
        // Sera tres utile lorsqu'il viendra le temps de faire un double check.
        final int[] chunkId = {0};
        
        HashMap<String, ServerChunkSize> calculServerEssentials = buildCalculServerEssentials();
        ArrayList<String> listOperation = readFile(fileName);


        calculServerEssentials.forEach((serveurNom, serverChunkSize) -> {

            List<String> operations = listOperation.subList(Math.max(listOperation.size() - serverChunkSize.getChunkSize(), 0), listOperation.size());
            int opSize = operations.size();
            // chunkId n'égale plus à -1.
            Task task = new Task(serveurNom, serverChunkSize.getServeurCalcul(), new ArrayList<String>(operations), chunkId[0], username, password);
            chunkId[0] += 1;
            tasks.add(task);

            for (int i = 0; i < opSize; i++) {
                listOperation.remove(listOperation.size() - 1);
            }
        });

        for (Task task: tasks){
            execService.submit(task);
        }

        // Ce hashmap sera utile afin d'identifier de regarder une deuxieme fois le resultat d'un serveur.
        HashMap<Integer, List<Integer>> resultatDoubleCheck = new HashMap<Integer, List<Integer>>();

        int tasksSize = tasks.size();
        while (tasksSize > 0) {
            try {
                Task.TaskInfo partialResult = execService.take().get();

                switch (partialResult.getStatut()) {

                    case RMI_EXCEPTION:
                        System.out.println("Serveur de calcul innaccessible");
                        calculServerEssentials.remove(partialResult.getServeurNom());
                        // retrait du premier resultat ajouter si il y en avait un.
                        resultatDoubleCheck.remove(partialResult.getChunkID());
                        tasksSize -= 1;
                        listOperation.addAll(partialResult.getListOperation());

                        if (calculServerEssentials.isEmpty()){
                            System.out.println("Aucun serveur de calcul accessible");
                            System.exit(1);
                        }
                        break;

                    case BAD_NAMESERVER:
                        System.out.println("Mauvais serveur de noms");
                        System.exit(1);

                    case BAD_AUTHENTIFICATION:
                        System.out.println("Mauvaise authentification du repartieur");
                        System.exit(1);

                    case REFUSED:
                        System.out.println("Tache refusee");
                        Task taskRefused = new Task(partialResult.getServeurNom(), 
                                             calculServerEssentials.get(partialResult.getServeurNom()).getServeurCalcul(),
                                             partialResult.getListOperation(), -1, username, password);
                        execService.submit(taskRefused);
                        break;

                    case OK:
                        // Informations sur le retour d'un resultat d'un serveur.
                        int firstResult = partialResult.getResultat();
                        int firstChunkId = partialResult.getChunkID();

                        // Si le resultat recu n'est pas present dans le hashmap des verifications, ajoute le a la liste
                        List<Integer> oldResult = resultatDoubleCheck.putIfAbsent(firstChunkId, Arrays.asList(firstResult));
                        
                        // Si le resultat avait deja ete ajouter
                        if (oldResult != null){
                            ArrayList<Integer> resultList = new ArrayList<Integer>(oldResult);

                            // Et que le resultat est le meme.
                            if (resultList.indexOf(firstResult) != -1 ){

                                // Il s'agit d'un resultat valide et on peut l'additionner a notre resultat finale.
                                // Meme implementation que dans la premiere fonction.
                                System.out.println("Resultat Valide");
                                resultatDoubleCheck.remove(firstChunkId);
                                resultat += firstResult;
                                resultat %= 5000;

                                if (!listOperation.isEmpty()){
                                    List<String> operations = listOperation.subList(
                                        Math.max(listOperation.size() - calculServerEssentials.get(partialResult.getServeurNom()).getChunkSize(), 0),
                                        listOperation.size());
                                    int opSize = operations.size();

                                    // Chunk id different.
                                    Task taskOk = new Task(partialResult.getServeurNom(), 
                                        calculServerEssentials.get(partialResult.getServeurNom()).getServeurCalcul(),
                                        new ArrayList<String>(operations), chunkId[0], username, password);
                                    chunkId[0] += 1;
                                    execService.submit(taskOk);
        
                                    for (int i = 0; i < opSize; i++) {
                                        listOperation.remove(listOperation.size() - 1);
                                    }
        
                                } else {
                                    tasksSize -= 1;
                                }

                            
                            } else {
                                // Double Check. Un premier resultat a ete ajoutee, mais pas le deuxieme.
                                resultList.add(firstResult);
                                resultatDoubleCheck.replace(firstChunkId, resultList);

                                // Parmi tout les serveurs sauf celui qui vient tout juste de faire la premiere operation
                                ArrayList<String> servers = new ArrayList<String>(calculServerEssentials.keySet());
                                servers.remove(partialResult.getServeurNom());

                                boolean reassignedTask = false;

                                for (String server: servers) {
                                    // Si le serveur a assez de capacite pour faire l'operation
                                    if (calculServerEssentials.get(server).getChunkSize() + 1 >= calculServerEssentials.get(partialResult.getServeurNom()).getChunkSize()){
                                        // On lui assigne une nouvelle tache qui va venir doublecheck la liste d'operations.
                                        Task taskSecond = new Task(server, 
                                        calculServerEssentials.get(server).getServeurCalcul(),
                                        partialResult.getListOperation(), partialResult.getChunkID(), username, password);
        
                                        execService.submit(taskSecond);
                                        reassignedTask = true;
                                        break;
                                    }
                                }

                                // Si la tache n'a pas pu etre assigner, on quitte. On ne peut reverifier
                                if (!reassignedTask){
                                    System.out.println("N'a pas pu réassigner nouvelles taches");
                                    System.exit(1);
                                }
                            }
                        } else {
                            // Premier check. Aucun resultat n'a encore ete ajouter.
                            // On veut assigner le double check a un serveur differents de celui qui vient de calculer la premiere operation.
                            // Meme implementation que le double check d'une operation.
                            ArrayList<String> servers = new ArrayList<String>(calculServerEssentials.keySet());
                            servers.remove(partialResult.getServeurNom());

                            boolean reassignedTask = false;

                            for (String server: servers) {
                                if (calculServerEssentials.get(server).getChunkSize() + 1 >= calculServerEssentials.get(partialResult.getServeurNom()).getChunkSize()){
                                    Task taskSecond = new Task(server, 
                                        calculServerEssentials.get(server).getServeurCalcul(),
                                        partialResult.getListOperation(), partialResult.getChunkID(), username, password);
    
                                    execService.submit(taskSecond);
                                    reassignedTask = true;
                                    break;
                                }
                            }

                            if (!reassignedTask){
                                System.out.println("N'a pas pu réassigner nouvelles taches");
                                System.exit(1);
                            }
                        }
                        break;

                    default:
                        break;
                }
                
            } catch (InterruptedException e) {
                System.out.println("Erreur: " + e.getMessage());
            } catch (ExecutionException e) {
                System.out.println("Erreur: " + e.getMessage());
            }
        }

        return resultat % 5000;
    }
    

    // Lecture d'un fichier d'operation et on retourne la liste des operations en string.
    private ArrayList<String> readFile(String fileName){
        ArrayList<String> listOperation = new ArrayList<String>();

        try {
            File file = new File(fileName);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = "";
            while((line = br.readLine()) != null){
                listOperation.add(line);
            }
            br.close();
        } catch (FileNotFoundException e){
            System.out.println("Erreur: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Erreur: " + e.getMessage());
        }
        return listOperation;
    }


    // Parmi tout les serveurs de calcul, est-ce qu'au moins un de ceux-ci est malicieux ?
    private boolean detectMaliciousness() {
		ArrayList<ArrayList<String>> calculServerInfos = null;
		try {
			calculServerInfos = nomsServerStub.getCalculServerInfos();
			for (ArrayList<String> calculServerInfo : calculServerInfos) {
				if (Integer.parseInt(calculServerInfo.get(3)) > 0) {
					return true;
				}
			}
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return false;
    }
    
    // Afin d'utiliser correctement les informations du serveur de calcul et de pouvoir les acceder correctement.
    // On utilise un hasmap avec la clé comme nom du serveur.
    // L'attribut lié à la clé est un objet qui contient le stub du meme nom de serveur et la capacite de ce meme stub.
    private HashMap<String, ServerChunkSize> buildCalculServerEssentials() {
        ArrayList<ArrayList<String>> calculServerInfos = null;
        HashMap<String, ServerChunkSize> calculServerEssentials = new HashMap<String,ServerChunkSize>();
		try {
            calculServerInfos = nomsServerStub.getCalculServerInfos();
            
			for (int i = 0; i < calculServerStubs.size(); i++) {
                ServerChunkSize item = new ServerChunkSize(calculServerStubs.get(i), Integer.parseInt(calculServerInfos.get(i).get(1)));
                calculServerEssentials.put(calculServerInfos.get(i).get(2), item);
            }
        } catch (RemoteException e) {
            System.out.println("Erreur: " + e.getMessage());
        }

        return calculServerEssentials;
    }
}


// Objet de la classe precedente. Il permet d'associer plus facilement le nom du serveur a ses informations.
class ServerChunkSize {
    // Taux de refus acceptable maximum des operations par le serveur de calcul.
    private static final int TAUX_DE_REFUS = 15;
    private CalculInterface serveurCalcul;
    private int chunkSize;

    ServerChunkSize(CalculInterface serveur, int capacity) {
        serveurCalcul = serveur;
        chunkSize = calculateChunkSize(capacity);
    }

    // Formule donnee
    private int calculateChunkSize(int ressources) {
        return (int)Math.floor(5*ressources * TAUX_DE_REFUS/100 + ressources);
    }

    public CalculInterface getServeurCalcul() {
        return serveurCalcul;
    }

    public int getChunkSize() {
        return chunkSize;
    }
}