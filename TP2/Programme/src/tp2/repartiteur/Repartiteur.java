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
		int resultat = repartiteur.process(fileName);

		// Affichage du résultat
        System.out.println(resultat);
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

		nomsServerStub = loadNomsServerStub("127.0.0.1");
		if (nomsServerStub != null) {
			calculServerStubs = new ArrayList<CalculInterface>();
			ArrayList<ArrayList<String>> calculServerInfos = null;
			try {
				calculServerInfos = nomsServerStub.getCalculServerInfos();

				for (ArrayList<String> calculServerInfo : calculServerInfos) {

                    calculServerStubs.add(loadCalculServerStub(calculServerInfo.get(0), calculServerInfo.get(2)));
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

	private CalculInterface loadCalculServerStub(String hostname, String registryName) {
		CalculInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
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

	private int process(String fileName) {
		if (detectMaliciousness()) {
			return insecureProcess(fileName);
		} else {
			return secureProcess(fileName);
		}
	}

	private int secureProcess(String fileName) {
		int resultat = 0;
		
		ArrayList<Task> tasks = new ArrayList<Task>();
        Executor executor = Executors.newCachedThreadPool();
		ExecutorCompletionService<Task.TaskInfo> execService = new ExecutorCompletionService<Task.TaskInfo>(executor);
        
        HashMap<String, ServerChunkSize> calculServerEssentials = buildCalculServerEssentials();
        ArrayList<String> listOperation = readFile(fileName);

        calculServerEssentials.forEach((serveurNom, serverChunkSize) -> {

            List<String> operations = listOperation.subList(Math.max(listOperation.size() - serverChunkSize.getChunkSize(), 0), listOperation.size());
            int opSize = operations.size();
            Task task = new Task(serveurNom, serverChunkSize.getServeurCalcul(), new ArrayList<String>(operations), -1, username, password);
            tasks.add(task);

            for (int i = 0; i < opSize; i++) {
                listOperation.remove(listOperation.size() - 1);
            }
        });
        
        for (Task task: tasks){
            execService.submit(task);
        }

        int tasksSize = tasks.size();
        while (tasksSize > 0) {
            try {
                Task.TaskInfo partialResult = execService.take().get();

                switch (partialResult.getStatut()) {

                    case RMI_EXCEPTION:
                        System.out.println("Serveur de calcul innaccessible");
                        calculServerEssentials.remove(partialResult.getServeurNom());
                        tasksSize -= 1;
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

                    case REFUSED:
                        System.out.println("Tache refusee");
                        Task taskRefused = new Task(partialResult.getServeurNom(), 
                                             calculServerEssentials.get(partialResult.getServeurNom()).getServeurCalcul(),
                                             partialResult.getListOperation(), -1, username, password);
                        execService.submit(taskRefused);
                        break;

                    case OK:
                        System.out.println("Tache executee");
                        resultat += partialResult.getResultat();
                        resultat %= 5000;

                        if (!listOperation.isEmpty()){
                            List<String> operations = listOperation.subList(
                                Math.max(listOperation.size() - calculServerEssentials.get(partialResult.getServeurNom()).getChunkSize(), 0),
                                listOperation.size());
                            int opSize = operations.size();
                            Task taskOk = new Task(partialResult.getServeurNom(), 
                                calculServerEssentials.get(partialResult.getServeurNom()).getServeurCalcul(),
                                new ArrayList<String>(operations), -1, username, password);

                            execService.submit(taskOk);

                            for (int i = 0; i < opSize; i++) {
                                listOperation.remove(listOperation.size() - 1);
                            }

                        } else {
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

		return resultat % 5000;
	}
	
	private int insecureProcess(String fileName) {
        int resultat = 0;

        ArrayList<Task> tasks = new ArrayList<Task>();
        Executor executor = Executors.newCachedThreadPool();
		ExecutorCompletionService<Task.TaskInfo> execService = new ExecutorCompletionService<Task.TaskInfo>(executor);
        final int[] chunkId = {0};
        
        HashMap<String, ServerChunkSize> calculServerEssentials = buildCalculServerEssentials();
        ArrayList<String> listOperation = readFile(fileName);


        calculServerEssentials.forEach((serveurNom, serverChunkSize) -> {

            List<String> operations = listOperation.subList(Math.max(listOperation.size() - serverChunkSize.getChunkSize(), 0), listOperation.size());
            int opSize = operations.size();
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

        HashMap<Integer, List<Integer>> resultatDoubleCheck = new HashMap<Integer, List<Integer>>();

        int tasksSize = tasks.size();
        while (tasksSize > 0) {
            try {
                Task.TaskInfo partialResult = execService.take().get();

                switch (partialResult.getStatut()) {

                    case RMI_EXCEPTION:
                        System.out.println("Serveur de calcul innaccessible");
                        calculServerEssentials.remove(partialResult.getServeurNom());
                        resultatDoubleCheck.remove(partialResult.getChunkID());
                        tasksSize -= 1;
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

                    case REFUSED:
                        System.out.println("Tache refusee");
                        Task taskRefused = new Task(partialResult.getServeurNom(), 
                                             calculServerEssentials.get(partialResult.getServeurNom()).getServeurCalcul(),
                                             partialResult.getListOperation(), -1, username, password);
                        execService.submit(taskRefused);
                        break;

                    case OK:
                        int firstResult = partialResult.getResultat();
                        int firstChunkId = partialResult.getChunkID();

                        List<Integer> oldResult = resultatDoubleCheck.putIfAbsent(firstChunkId, Arrays.asList(firstResult));
                        if (oldResult != null){
                            ArrayList<Integer> resultList = new ArrayList<Integer>(oldResult);

                            if (resultList.indexOf(firstResult) != -1 ){

                                System.out.println("Resultat Valide");
                                resultatDoubleCheck.remove(firstChunkId);
                                resultat += firstResult;
                                resultat %= 5000;

                                if (!listOperation.isEmpty()){
                                    List<String> operations = listOperation.subList(
                                        Math.max(listOperation.size() - calculServerEssentials.get(partialResult.getServeurNom()).getChunkSize(), 0),
                                        listOperation.size());
                                    int opSize = operations.size();

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
                                // Double Check
                                resultList.add(firstResult);
                                resultatDoubleCheck.replace(firstChunkId, resultList);

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
                                    }
                                }

                                if (!reassignedTask){
                                    System.out.println("N'a pas pu réassigner nouvelles taches");
                                    System.exit(1);
                                }
                            }
                        } else {
                            // Premier check
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

class ServerChunkSize {
    private static final int TAUX_DE_REFUS = 15;
    private CalculInterface serveurCalcul;
    private int chunkSize;

    ServerChunkSize(CalculInterface serveur, int capacity) {
        serveurCalcul = serveur;
        chunkSize = calculateChunkSize(capacity);
    }

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