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

    private int currentAttackId = 0;
    private UUID currentSlaveKey;
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

    private List<Guess> mapToList(Map<Integer, List<Guess>> map) {
        Iterator entries = map.entrySet().iterator();
        List<Guess> guessesList = new ArrayList<>();

        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            for (Guess e : (List<Guess>) entry.getValue()) {
                guessesList.add(e);
            }
        }

        return guessesList;
    }

    private Guess[] mapToArray(Map<Integer, List<Guess>> map) {
        return listToArray(mapToList(map));
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
        attacksInfo.get(attackId).put(slavekey, si).setTempo(attacksInfo.get(attackId - 1).get(slavekey).getTempo());

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
            si.setTempo(System.nanoTime() / 1000000000);
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
        try {
            System.out.println("guess attack " + attackNumber);
            System.out.println("tamanho guesses: " + guesses.size());
            guesses.get(attackNumber).add(currentguess);

            System.out.println("--------------Guess-----------------------");
            System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                    + " índice: " + currentindex + " | Chave candidata: "
                    + currentguess.getKey());
            System.out.println("------------------------------------------");

            attacksInfo.get(attackNumber).get(slaveKey).setCorrente_Index((int) currentindex);
        } catch (Exception er) {
            System.out.println(er);
            System.out.println("DEu merda");
        }

    }

    @Override
    public void checkpoint(UUID slaveKey, int attackNumber, long currentindex)
            throws RemoteException {

        attacksInfo.get(attackNumber).get(slaveKey).setCorrente_Index((int) currentindex);

        this.attacksInfo.get(attackNumber).get(slaveKey).setTempo(System.nanoTime() / 1000000000);

        if (attacksInfo.get(attackNumber).get(slaveKey).getFinal_Index() == currentindex) {
            attacksInfo.get(attackNumber).get(slaveKey).setTerminou(true);
            System.out.println("Último checkpoint!");
            System.out.println("Escravo " + attacksInfo.get(attackNumber).get(slaveKey).getNome() + " terminou");
            if (this.check_to_notify()) {
                synchronized (attacksInfo) {
                    attacksInfo.notify();
                }
            }
        }

        System.out.println("--------------------Checkpoint--------------------");
        System.out.println("Nome do escravo: " + attacksInfo.get(attackNumber).get(slaveKey).getNome()
                + " índice: " + currentindex);
        System.out.println("---------------------------------------------------");

        //for (int i = 0; i < attacksInfo.size(); i++) {
        //System.out.println("nome: "+this.attacksInfo.get(attackNumber).get(slaveKey).getNome()+" tempo: "+this.attacksInfo.get(attackNumber).get(slaveKey).getTempo());
        //}
    }

    private boolean check_to_notify() {
        Iterator attacks = attacksInfo.entrySet().iterator();

        while (attacks.hasNext()) {
            Map.Entry attack = (Map.Entry) attacks.next();
            int attackId = (Integer) attack.getKey();

            Iterator entr = slaves.entrySet().iterator();

            while (entr.hasNext()) {
                Map.Entry entry = (Map.Entry) entr.next();
                UUID idd = (UUID) entry.getKey();

                if (!attacksInfo.get(attackId).get(idd).isTerminou()) {
                    return false;
                }

            }
        }

        return true;
    }

    private synchronized void redividirIndices(int attackId, UUID key, byte[] ciphertext, byte[] knowntext) throws RemoteException {
        System.out.println("tamanho : " + attacksInfo.size());

        System.out.println("Retirar escravo " + attacksInfo.get(attackId).get(key).getNome());
        int current_index = attacksInfo.get(attackId).get(key).getCorrente_Index() + 1;
        int final_index = attacksInfo.get(attackId).get(key).getFinal_Index();
        System.out.println("current_index: " + current_index);
        System.out.println("final index: " + final_index);

        attackId = currentAttackId;
        List<Guess> Lguesses = new ArrayList<>();
        guesses.put(attackId, Lguesses);

        this.currentAttackId++;

        try {
            removeSlave(key);
        } catch (RemoteException ex) {
            Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (current_index != final_index) {
            System.out.println("novo attack: " + attackId);
            this.addSlavesInfo(attacksInfo.size());
            
            System.out.println("INDICE INICIAL: " + current_index);
            System.out.println("INDICE FINAL: " + final_index);

            int numberOfSlaves = slaves.size();
            System.out.println("NUM OF SLAVES: " + numberOfSlaves);
            final int amountPerSlave = (final_index - current_index) / numberOfSlaves;
            final int residualAmount = (final_index - current_index) % numberOfSlaves;

            //System.out.println("amount: "+amountPerSlave+" residual: "+residualAmount);
            Iterator entries = slaves.entrySet().iterator();
            //System.out.println("tamanho escravos: "+slaves.size());
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Slave slave = (Slave) entry.getValue();

                if (entries.hasNext()) {
                    attacksInfo.get(attacksInfo.size() - 1).get(entry.getKey()).setFinal_Index(current_index + amountPerSlave + 1);
                    System.out.println("novo sub-ataque");
                    slave.startSubAttack(ciphertext, knowntext, current_index,
                            current_index + amountPerSlave, attackId, this);
                    System.out.println("");
                } else {
                    attacksInfo.get(attacksInfo.size() - 1).get(entry.getKey()).setFinal_Index(current_index + amountPerSlave + residualAmount);
                    System.out.println("novo sub-ataque");
                    slave.startSubAttack(ciphertext, knowntext, current_index,
                            current_index + amountPerSlave + residualAmount - 1,
                            attackId, this);
                }

                current_index += amountPerSlave;
            }
        }
    }

    @Override
    public Guess[] attack(byte[] ciphertext, byte[] knowntext)
            throws RemoteException {

        int numberOfSlaves = slaves.size();
        List<String> dictionary = readDictionary(filename);
        Map<UUID, Timer> timers = new ConcurrentHashMap<>();

        this.addSlavesInfo(this.currentAttackId);
        int attackId = this.currentAttackId;
        this.currentAttackId++;

        final int amountPerSlave = dictionary.size() / numberOfSlaves;
        final int residualAmount = dictionary.size() % numberOfSlaves;
        int currentIndex = 0;

        Iterator entries = slaves.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            currentSlaveKey = (UUID) entry.getKey();

            Slave slave = (Slave) entry.getValue();
            //attacks.put(currentAttackId, slave);
            //attackCurrentId++;

            // creating timer task, timer
            Timer timer = new Timer();

            // scheduling the task at interval
            timer.schedule(new java.util.TimerTask() {
                int localAttackId = currentAttackId - 1;
                UUID localSlaveKey = currentSlaveKey;

                @Override
                public void run() {

                    //int attackId = currentAttackId--;
                    try {
                        System.err.println("Tentando verificar se o escravo " + attacksInfo.get(localAttackId).get(localSlaveKey).getNome()
                                + " ainda funciona...");

                        long t = System.nanoTime() / 1000000000;
                        long diff = t - attacksInfo.get(localAttackId).get(localSlaveKey).getTempo();
                        System.out.println("tempo recente: " + t);
                        System.out.println("tempo antigo: " + attacksInfo.get(localAttackId).get(localSlaveKey).getTempo());
                        if (diff > 20 && !attacksInfo.get(localAttackId).get(localSlaveKey).isTerminou()) {
                            System.out.println("diff " + diff);
                            redividirIndices(localAttackId, localSlaveKey, ciphertext, knowntext);

                            timers.get(localSlaveKey).cancel();
                        } else {
                            System.err.println("Escravo funcionando");
                        }
                    } catch (RemoteException er) {

                        System.out.println("erro: " + er);
                        System.out.println("attackid 2: " + localAttackId);
                        try {
                            redividirIndices(localAttackId, localSlaveKey, ciphertext, knowntext);
                        } catch (RemoteException ex) {
                            Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        timers.get(localSlaveKey).cancel();

                    }
                }
            },
                    20000,
                    20000);

            timers.put(currentSlaveKey, timer);
            System.out.println("attackid " + attackId);
            attacksInfo.get(attackId).get(entry.getKey()).setInicio_Index(currentIndex);
            attacksInfo.get(attackId).get(entry.getKey()).setCorrente_Index(currentIndex);

            if (entries.hasNext()) {
                attacksInfo.get(attackId).get(entry.getKey()).setFinal_Index(currentIndex + amountPerSlave + 1);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave, attackId, this);
            } else {
                attacksInfo.get(attackId).get(entry.getKey()).setFinal_Index(currentIndex + amountPerSlave + residualAmount);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave + residualAmount - 1,
                        attackId, this);
                //attacksInfo.get(attackId).get(entry.getKey()).setTerminou(true);
                //System.out.println("Escravo "+attacksInfo.get(attackId).get(entry.getKey()).getNome()+" terminou");
            }

            currentIndex += amountPerSlave;
            System.out.println("CURRENT INDEX: " + currentIndex);
        }

        List<Guess> Lguesses = new ArrayList<>();
        guesses.put(attackId, Lguesses);
        /*
            FAZER ESPERAR ATÉ QUE OS ESCRAVOS TENHAM TERMINADO PRA RETORNAR A LISTA.
            PRA SABER QUANDO ELES TERMINARAM ACHO QUE PODE CRIAR UMA LISTA BOOLEANA
            DIZENDO SE ELES TERMINARAM OU NÃO.
         */
        synchronized (attacksInfo) {
            try {
                System.out.println("attack synchronized: " + (currentAttackId - 1));
                attacksInfo.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }

        };

        entries = slaves.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            UUID slaveId = (UUID) entry.getKey();
            timers.get(slaveId).cancel();
        }

        System.out.println("Ataque terminado!");
        return mapToArray(guesses);
    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? "localhost" : args[0];

        try {

            MasterImpl obj = new MasterImpl();
            Master objref = (Master) UnicastRemoteObject.exportObject(obj, 2000);

            Registry registry = LocateRegistry.getRegistry("localhost");
            System.err.println("Server bindind");

            registry.rebind("Mestre", objref);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Master exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
