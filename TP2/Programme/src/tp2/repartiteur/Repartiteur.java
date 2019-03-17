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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
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
                    System.out.println(calculServerInfo.get(0));
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
        
        

//         csStubs.forEach((name, stub) -> {
//             int qty = (int)(capacities.get(name) * 2);

//             // Prendre les 'qty' derniers éléments de la liste d'opérations.
//             List<OperationPair> subOps = ops.subList(Math.max(ops.size() - qty, 0), ops.size());

//             // Lancer la tâche avec cette sous-liste.
//             ClientTask task = new ClientTask(name, stub, new ArrayList<>(subOps), username, password, -1);
//             tasks.add(task);

//             // Enlever la sous-liste des opérations restantes.
//             ops.removeAll(subOps);
// });
        // int chunkSize = 1;

        // ArrayList<String> listOperation = readFile(fileName);

        // try {
        //     chunkSize = calculateChunkSize(Integer.parseInt(nomsServerStub.getCalculServerInfos().get(0).get(1)));
        //     System.out.println(chunkSize);
        // } catch (RemoteException e) {
		// 	System.out.println("Erreur: " + e.getMessage());
		// }

        // while (!listOperation.isEmpty()) {
        //     System.out.println("Nouveau Chunk");
        //     ArrayList<String> operations = new ArrayList<String>();
        //     int i = 0;
        //     while (i < chunkSize && !listOperation.isEmpty()) {
        //         String item = listOperation.remove(listOperation.size()-1);
        //         operations.add(item);
        //         i += 1;
        //     }
        //     // Thread
        //     try {
        //         int partialResult = calculServerStubs.get(0).calculate(operations);
        //         System.out.println(String.valueOf(partialResult));
        //         resultat += partialResult;
        //     } catch (RemoteException e) {
        //         System.out.println("Erreur: " + e.getMessage());
        //     }
        // }

		return resultat % 5000;
	}
	
	private int insecureProcess(String fileName) {
        int resultat = 0;
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