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

//java code for thread creation by extending 
// the Thread class 
// class MultithreadingDemo extends Thread
// { 
//     public void run() 
//     { 
//         try { 
//             System.out.println ("Thread " + Thread.currentThread().getId() + " is running"); 
//         } 
//         catch (Exception e) { 
//             System.out.println ("Exception is caught"); 
//         } 
//     } 
// } 

public class Calcul implements CalculInterface {

    private int numberOfClients;
    private int numberOfTasks;
    private int maliciousness;
    private String username; 
    private String password;
    private NomsInterface nomsServerStub = null;

	public static void main(String[] args) {
        int numOfTasks = 0;
        int malicious = 0; 
        if (args.length >= 2) {
			numOfTasks = Integer.parseInt(args[0]);
            malicious = Integer.parseInt(args[1]);
        } else {
            System.out.println("Wrong number of arguments");
        }
        Calcul calcul = new Calcul(numOfTasks, malicious);
		calcul.run();
	}

	public Calcul(int numOfTasks, int malicious) {
        super();
        numberOfClients = 0;
        numberOfTasks = numOfTasks;
        maliciousness = malicious;
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

            nomsServerStub = loadNomsServerStub("127.0.0.1");

            if (nomsServerStub != null) {
                ArrayList<String> info = new ArrayList<String>();
                info.add("127.0.0.1");
                info.add(String.valueOf(numberOfTasks));
                nomsServerStub.addInfo(info);
            }

            // for (int i=0; i < numberOfTasks; i++) 
            // { 
            //     MultithreadingDemo object = new MultithreadingDemo(); 
            //     object.start();
            // }

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

    public String getIP() {
        String ip = "";
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            ip = inetAddress.toString();
            System.out.println(ip);
        } catch (UnknownHostException e) {
            System.out.println("Erreur: " + e.getMessage());
		}
        return ip;
    }

    @Override
    public int calculate(ArrayList<String> operations) throws RemoteException {
        int partialResult = 0;
        for (String item : operations){
            String operation = item.split(" ")[0];
            int operande = Integer.parseInt(item.split(" ")[1]);

            partialResult += executeOperation(operation, operande);
            partialResult %= 5000;
        }
        return (int) partialResult;
    }

    private long executeOperation(String operation, int operande){
        if (generateRandomNumber(100) < maliciousness){
            return generateRandomNumber(5000);
        } else {
            if (operation.equals("prime")){
                // System.out.println("Calculate prime");
                return prime(operande);
            } 
            else if (operation.equals("pell")){
                // System.out.println("Calculate Pell");
                return pell(operande);
            } 
            else {
                System.out.println("unknown operation.");
                return 0;
            }
        }
    }

    public int generateRandomNumber(int range){
        Random rand = new Random();
        return rand.nextInt(range+1); // Between 0 and range + 1.
    }

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
