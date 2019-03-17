package tp2.repartiteur;

import tp2.partage.CalculInterface;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

enum SatutResultat {
    OK, REFUSED, BAD_AUTHENTIFICATION, BAD_NAMESERVER, RMI_EXCEPTION
}

public class Task implements Callable<Task.TaskInfo> {

    private String serveurNom;
    private CalculInterface serveurInterface;
    private ArrayList<String> listOperation;
    private int chunkID;
    private TaskInfo statut;
    
    private String repartiteurNom;
    private String repartiteurMDP;
    
    Task(String taskNom, CalculInterface taskServeur, ArrayList<String> taskOperations, int chunkNumber, String usr, String mdp) {
        serveurNom = taskNom;
        serveurInterface = taskServeur;
        listOperation = taskOperations;
        chunkID = chunkNumber;

        repartiteurNom = usr;
        repartiteurMDP = mdp;
    }

    class TaskInfo {

        private String serveurNom;
        private ArrayList<String> listOperation;
        private int resultat;
        private SatutResultat statut;
        private int chunkID;

        TaskInfo(String taskNom, ArrayList<String> taskOperations, int result, SatutResultat statutResultat, int chunkNumber) {
            serveurNom = taskNom;
            listOperation = taskOperations;
            resultat = result;
            statut = statutResultat;
            chunkID = chunkNumber;
        }

        public int getResultat() {
            return resultat;
        }

        public SatutResultat getStatut() {
            return statut;
        }

        public ArrayList<String> getListOperation() {
            return listOperation;
        }

        public String getServeurNom() {
            return serveurNom;
        }

        public int getChunkID() {
            return chunkID;
        }
    }

    @Override
    public TaskInfo call() throws Exception {
        try {
            int resultat = serveurInterface.calculate(listOperation, repartiteurNom, repartiteurMDP);
            if (resultat >= 0) {
                statut = new TaskInfo(serveurNom, listOperation, resultat, SatutResultat.OK, chunkID);
            } else if (resultat == -1) {
                statut = new TaskInfo(serveurNom, listOperation, resultat, SatutResultat.REFUSED, chunkID);
            } else if (resultat == -2) {
                statut = new TaskInfo(serveurNom, listOperation, resultat, SatutResultat.BAD_AUTHENTIFICATION, chunkID);
            } else if (resultat == -3) {
                statut = new TaskInfo(serveurNom, listOperation, resultat, SatutResultat.BAD_NAMESERVER, chunkID);
            } else {
                statut = new TaskInfo(serveurNom, listOperation, resultat, SatutResultat.RMI_EXCEPTION, chunkID);
            }

        } catch (RemoteException e) {
            statut = new TaskInfo(serveurNom, listOperation, -4, SatutResultat.RMI_EXCEPTION, chunkID);
        }

        return statut;
    }
}