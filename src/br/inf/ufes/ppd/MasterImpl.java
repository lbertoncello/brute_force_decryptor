/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lucas
 */
public class MasterImpl implements Master {

    private final String filename = "dictionary.txt";

    private int attackCurrentId = 0;
    private Map<UUID, Slave> slaves = new ConcurrentHashMap<>();
    private Map<UUID, String> slavesNames = new ConcurrentHashMap<>();
    //private Map<Guess> guesses = new ArrayList<>();
    private Map<Integer, List<Guess>> guesses = new ConcurrentHashMap<>();
    //private Map<Integer, Slave> attacks = new ConcurrentHashMap<>();
    private Map<Integer, Map<UUID, SlaveInfo>> attacksInfo = new ConcurrentHashMap<>();

    private Guess[] listToArray(List<Guess> list) {
        Guess[] guessesArray = new Guess[list.size()];
        int contador = 0;
        for (int i = 0; i < guesses.size(); i++) {
            List<Guess> listGuess = guesses.get(i);

            for (Guess guess : listGuess) {
                guessesArray[contador] = guess;
                contador++;
            }

        }

        return guessesArray;
    }

    private List<String> readDictionary(String filename) {
        List<String> dictionary = new ArrayList<>();

        try {
            FileReader file = new FileReader(filename);
            BufferedReader readFile = new BufferedReader(file);

            String line = readFile.readLine(); // lê a primeira linha
// a variável "linha" recebe o valor "null" quando o processo
// de repetição atingir o final do arquivo texto

            while (line != null) {
                dictionary.add(line);

                line = readFile.readLine(); // lê da segunda até a última linha
            }

            file.close();
        } catch (IOException e) {
            System.err.printf("Erro na abertura do arquivo: %s.\n",
                    e.getMessage());
        }

        return dictionary;
    }

    private void addSlaveInfo(int attackId, UUID slavekey, String slaveName, Slave s) {

        SlaveInfo si = new SlaveInfo(slavekey, slaveName, s);
        attacksInfo.get(attackId).put(slavekey, si);

    }

    private void addSlavesInfo(int attackId) {
        Map<UUID, SlaveInfo> slavesInfo = new ConcurrentHashMap<>();
        attacksInfo.put(attackId, slavesInfo);

        Iterator entries = slaves.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            Slave slave = (Slave) entry.getValue();
            UUID key = (UUID) entry.getKey();

            SlaveInfo si = new SlaveInfo(key, slavesNames.get(key), slave);

            attacksInfo.get(attackId).put(key, si);

        }
    }

    @Override
    public void addSlave(Slave s, String slaveName, UUID slavekey)
            throws RemoteException {

        synchronized (slaves) {
            if (!slaves.containsKey(slavekey)) {
                SlaveInfo si = new SlaveInfo(slavekey, slaveName, s);
                slaves.put(slavekey, s);
                slavesNames.put(slavekey, slaveName);
                System.out.println("Slave de nome " + slaveName
                        + " foi registrado com sucesso!");
            }
        }
    }

    @Override
    public void removeSlave(UUID slaveKey) throws RemoteException {
        synchronized (slaves) {
            slaves.remove(slaveKey);
            slavesNames.remove(slaveKey);
            //slavesInfo.remove(slaveKey);
        }
    }

    @Override
    public void foundGuess(UUID slaveKey, int attackNumber, long currentindex,
            Guess currentguess) throws RemoteException {
        guesses.get(attackNumber).add(currentguess);

        System.out.println("--------------Guess-----------------------");
        System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                + " índice: " + currentindex + " | Chave candidata: "
                + currentguess.getKey());
        System.out.println("------------------------------------------");

        attacksInfo.get(attackNumber).get(slaveKey).setCorrente_Index((int) currentindex);

    }

    @Override
    public void checkpoint(UUID slaveKey, int attackNumber, long currentindex)
            throws RemoteException {

        attacksInfo.get(attackNumber).get(slaveKey).setCorrente_Index((int) currentindex);

        if (attacksInfo.get(attackNumber).get(slaveKey).getFinal_Index() == currentindex) {
            attacksInfo.get(attackNumber).get(slaveKey).setTerminou(true);
            System.out.println("Último checkpoint!");

            if (this.check_to_notify(attackNumber)) {
                synchronized (attacksInfo.get(attackNumber)) {
                    attacksInfo.get(attackNumber).notify();
                }
            }
        }

        System.out.println("--------------------Checkpoint--------------------");
        System.out.println("Nome do escravo: " + attacksInfo.get(attackNumber).get(slaveKey).getNome()
                + " índice: " + currentindex);
        System.out.println("---------------------------------------------------");

        this.attacksInfo.get(attackNumber).get(slaveKey).setTempo(System.nanoTime() / 1000000000);
    }

    private boolean check_to_notify(int attackNumber) {
        Iterator entr = slaves.entrySet().iterator();

        while (entr.hasNext()) {
            Map.Entry entry = (Map.Entry) entr.next();
            UUID idd = (UUID) entry.getKey();

            if (!attacksInfo.get(attackNumber).get(idd).isTerminou()) {
                return false;
            }

        }

        return true;
    }

    private synchronized void redividirIndices(int attackCurrentId, UUID key, byte[] ciphertext, byte[] knowntext) throws RemoteException {
        System.out.println("Retirar escravo");
        this.addSlavesInfo(attackCurrentId);
        int current_index = attacksInfo.get(attackCurrentId).get(key).getCorrente_Index();
        int final_index = attacksInfo.get(attackCurrentId).get(key).getFinal_Index();
        try {
            removeSlave(key);
        } catch (RemoteException ex) {
            Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        int numberOfSlaves = slaves.size();
        final int amountPerSlave = (current_index - final_index) / numberOfSlaves;
        final int residualAmount = (current_index - final_index) % numberOfSlaves;

        Iterator entries = slaves.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            Slave slave = (Slave) entry.getValue();

            if (entries.hasNext()) {
                attacksInfo.get(attackCurrentId).get(entry.getKey()).setFinal_Index(current_index + amountPerSlave + 1);

                slave.startSubAttack(ciphertext, knowntext, current_index,
                        current_index + amountPerSlave, attackCurrentId, this);
            } else {
                attacksInfo.get(attackCurrentId).get(entry.getKey()).setFinal_Index(current_index + amountPerSlave + residualAmount);

                slave.startSubAttack(ciphertext, knowntext, current_index,
                        current_index + amountPerSlave + residualAmount - 1,
                        attackCurrentId, this);
            }

            current_index += amountPerSlave;
        }
    }

    @Override
    public Guess[] attack(byte[] ciphertext, byte[] knowntext)
            throws RemoteException {

        int numberOfSlaves = slaves.size();
        List<String> dictionary = readDictionary(filename);
        List<Timer> timers = new ArrayList<>();

        this.addSlavesInfo(attackCurrentId);
        int attackId = attackCurrentId;
        attackCurrentId++;

        final int amountPerSlave = dictionary.size() / numberOfSlaves;
        final int residualAmount = dictionary.size() % numberOfSlaves;
        int currentIndex = 0;

        Iterator entries = slaves.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            Slave slave = (Slave) entry.getValue();
            //attacks.put(attackCurrentId, slave);
            //attackCurrentId++;

            // creating timer task, timer
            Timer timer = new Timer();

            // scheduling the task at interval
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    
                    int attackId = attackCurrentId--;

                    try {

                        System.err.println("Tentando verificar se o escravo ainda funciona...");

                        long t = System.nanoTime() / 1000000000;
                        long diff = t - attacksInfo.get(attackId).get((UUID) entry.getKey()).getTempo();

                        if (diff > 20 && !attacksInfo.get(attackId).get((UUID) entry.getKey()).isTerminou()) {
                            redividirIndices(attackId, (UUID) entry.getKey(), ciphertext, knowntext);

                        } else {
                            System.err.println("Escravo funcionando");
                        }
                    } catch (Exception er) {
                        try {
                            redividirIndices(attackId, (UUID) entry.getKey(), ciphertext, knowntext);
                        } catch (RemoteException ex) {
                            Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            },
                    20000,
                    20000);

            timers.add(timer);

            attacksInfo.get(attackId).get(entry.getKey()).setInicio_Index(currentIndex);

            if (entries.hasNext()) {
                attacksInfo.get(attackId).get(entry.getKey()).setFinal_Index(currentIndex + amountPerSlave + 1);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave, attackId, this);
            } else {
                attacksInfo.get(attackId).get(entry.getKey()).setFinal_Index(currentIndex + amountPerSlave + residualAmount);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave + residualAmount - 1,
                        attackId, this);
            }

            currentIndex += amountPerSlave;
        }

        List<Guess> Lguesses = new ArrayList<>();
        guesses.put(attackId, Lguesses);
        /*
            FAZER ESPERAR ATÉ QUE OS ESCRAVOS TENHAM TERMINADO PRA RETORNAR A LISTA.
            PRA SABER QUANDO ELES TERMINARAM ACHO QUE PODE CRIAR UMA LISTA BOOLEANA
            DIZENDO SE ELES TERMINARAM OU NÃO.
         */
        synchronized (attacksInfo.get(attackCurrentId - 1)) {
            try {
                attacksInfo.get(attackCurrentId - 1).wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }

        };

        for (Timer timer : timers) {
            timer.cancel();
        }

        System.out.println("Ataque terminado!");
        return listToArray(guesses.get(attackId));
    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? "localhost" : args[0];

        try {
            MasterImpl obj = new MasterImpl();
            Master objref = (Master) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry(host);
            System.err.println("Server bindind");

            registry.rebind("Mestre", objref);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Master exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
