package tp2.calcul;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.Random;

import tp2.partage.CalculInterface;
import tp2.partage.NomsInterface;

public class Calcul implements CalculInterface {

    private int numberOfTasks;
    private int maliciousness;
    private String registryName;
    private NomsInterface nomsServerStub = null;

	public static void main(String[] args) {
        int numOfTasks = 0;
        int malicious = 0;
        String name = "";

        // Parse the command line to create a compute server
        if (args.length >= 2) {
            numOfTasks = Integer.parseInt(args[0]);
            malicious = Integer.parseInt(args[1]);
            name = args[2];
        } else {
            System.out.println("Wrong number of arguments");
        }
        Calcul calcul = new Calcul(numOfTasks, malicious, name);
		calcul.run();
	}

    // Create "calcul" server
	public Calcul(int numOfTasks, int malicious, String name) {
        super();
        numberOfTasks = numOfTasks;
        maliciousness = malicious;
        registryName = name;
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {

            // rmi Port 5002 for calcul servers
            CalculInterface stub = (CalculInterface) UnicastRemoteObject.exportObject(this, 5002);
            
            // rmi Port 5000 for application
            Registry registry = LocateRegistry.getRegistry(5000);
            registry.rebind(registryName, stub);
            System.out.println("Server ready.");

            nomsServerStub = loadNomsServerStub(NomsInterface.ipServeurNoms);

            // Ajout de toutes des informations du serveur a envoyer vers le serveur de noms.
            if (nomsServerStub != null) {
                ArrayList<String> info = new ArrayList<String>();
                info.add(getIP());
                info.add(String.valueOf(numberOfTasks));
                info.add(registryName);
                info.add(String.valueOf(maliciousness));
                nomsServerStub.addInfo(info);
            }

		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur1: " + e);
		}
	}

    private NomsInterface loadNomsServerStub(String hostname) {
		NomsInterface stub = null;

		try {
            // rmi Port 5000 for application
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

    // Return a string of the ip of the calcul server.
    public String getIP() {
        String ip = "";
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            ip = inetAddress.toString().split("/")[1];
        } catch (UnknownHostException e) {
            System.out.println("Erreur: " + e.getMessage());
		}
        return ip;
    }

    // Takes a short list of operations, a username and a password.
    // If he can connect to the name server, and the username and the password match, he can go on.
    // This function also simulates if the calcul name can accept the list of operations.
    // This function also simulates a malicious server who sends a bad response in a fix percentage of time.
    // it returns a number, which is sometimes an error (negative), a wrong result (can't test yet) or the good result.
    @Override
    public int calculate(ArrayList<String> operations, String username, String password) throws RemoteException {

        if (nomsServerStub != null){
            if (nomsServerStub.authentificationClient(username, password)){
                if (enoughRessources(operations.size()))
                    if (generateRandomNumber(100) < maliciousness){
                        System.out.println("Resultat malicieux");

                        return generateRandomNumber(5000); // Resultat malicieux
                    } else {
                        int partialResult = 0;
                        for (String item : operations){
                            String operation = item.split(" ")[0];
                            int operande = Integer.parseInt(item.split(" ")[1]);
                
                            partialResult += executeOperation(operation, operande);
                            partialResult %= 5000;
                        }

                        return (int) partialResult; //Good result
                    }
                else {

                    return -3; // Not enough ressources
                }
            } else {
    
                return -1; // Error of authentification
            }
        } else {

            return -2; // Can't find server noms
        }
    }

    // Simulates if there is enough ressources for the server to accomplish the operations
    private boolean enoughRessources(int numberOfOperations){

        double rateOfRefusal = (((double)(numberOfOperations - numberOfTasks))/((double)(5*numberOfTasks)))*100;
        if ((int)rateOfRefusal < generateRandomNumber(100)) {

            return true;
        }

        return false;
    }

    // Execute one of the operation, either if it is a prime or a pell
    private long executeOperation(String operation, int operande){

        if (operation.equals("prime")){
            return prime(operande);
        } 
        else if (operation.equals("pell")){
            return pell(operande);
        } 
        else {
            System.out.println("unknown operation.");
            return 0;
        }
    }

    // Generate a random number Between 0 and range + 1.
    public int generateRandomNumber(int range){
        Random rand = new Random();
        return rand.nextInt(range+1);


    private long pell(int x) {
		if (x == 0)
			return 0;
		if (x == 1)
			return 1;
		return 2 * pell(x - 1) + pell(x - 2);
	}
    
	private int prime(int x) {
		int highestPrime = 0;
		
		for (int i = 1; i <= x; ++i)
		{
			if (isPrime(i) && x % i == 0 && i > highestPrime)
				highestPrime = i;
		}
		
		return highestPrime;
	}
	
	private boolean isPrime(int x) {
		if (x <= 1)
			return false;

		for (int i = 2; i < x; ++i)
		{
			if (x % i == 0)
				return false;
		}
		
		return true;		
	}
}
