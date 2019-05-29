/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
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
    private Map<Integer, List<Integer>> relatedAttacks = new ConcurrentHashMap<>();

    private Guess[] listToArray(List<Guess> list) {
        Guess[] array = new Guess[list.size()];

        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }

        return array;
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
        attacksInfo.get(attackId).put(slavekey, si).setTime(attacksInfo.get(attackId - 1).get(slavekey).getTime());

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
            si.setTime(System.nanoTime() / 1000000000);
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
            int originalAttackId = relatedAttacks.get(attackNumber).get(0);
            guesses.get(originalAttackId).add(currentguess);

            System.out.println("--------------Guess-----------------------");
            System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                    + " índice: " + currentindex + " | Chave candidata: "
                    + currentguess.getKey());
            System.out.println("------------------------------------------");

            attacksInfo.get(attackNumber).get(slaveKey).setCurrentIndex((int) currentindex);
        } catch (Exception er) {
            System.out.println(er);
        }

    }

    @Override
    public void checkpoint(UUID slaveKey, int attackNumber, long currentindex)
            throws RemoteException {

        attacksInfo.get(attackNumber).get(slaveKey).setCurrentIndex((int) currentindex);

        this.attacksInfo.get(attackNumber).get(slaveKey).setTime(System.nanoTime() / 1000000000);

        if (attacksInfo.get(attackNumber).get(slaveKey).getFinalIndex() == currentindex) {
            attacksInfo.get(attackNumber).get(slaveKey).setEnded(true);
            System.out.println("Último checkpoint!");
            System.out.println("Escravo " + attacksInfo.get(attackNumber).get(slaveKey).getNome() + " terminou");
            if (this.checkToNotify(attackNumber)) {
                int originalAttackId = relatedAttacks.get(attackNumber).get(0);
                synchronized (attacksInfo.get(originalAttackId)) {
                    attacksInfo.get(originalAttackId).notify();
                }
            }
        } else {
            System.out.println("--------------------Checkpoint--------------------");
            System.out.println("Nome do escravo: " + attacksInfo.get(attackNumber).get(slaveKey).getNome()
                    + " índice: " + currentindex);
            System.out.println("---------------------------------------------------");
        }

        //for (int i = 0; i < attacksInfo.size(); i++) {
        //System.out.println("nome: "+this.attacksInfo.get(attackNumber).get(slaveKey).getNome()+" tempo: "+this.attacksInfo.get(attackNumber).get(slaveKey).getTempo());
        //}
    }

    /*
    private boolean checkToNotify() {
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
     */
    private boolean checkToNotify(int checkpointAttackId) {
        Iterator attacks = attacksInfo.entrySet().iterator();

        for (Integer attackId : relatedAttacks.get(checkpointAttackId)) {
            Iterator entr = slaves.entrySet().iterator();

            while (entr.hasNext()) {
                Map.Entry entry = (Map.Entry) entr.next();
                UUID idd = (UUID) entry.getKey();

                if (!attacksInfo.get(attackId).get(idd).isEnded()) {
                    return false;
                }
            }
        }

        return true;
    }

    private synchronized void redivideIndex(int attackId, UUID key, byte[] ciphertext, byte[] knowntext) throws RemoteException {
        System.err.println("O escravo " + attacksInfo.get(attackId).get(key).getNome() + " falhou!");
        System.err.println("Redividindo ataque...");

        int initialIndex = attacksInfo.get(attackId).get(key).getCurrentIndex() + 1;
        int finalIndex = attacksInfo.get(attackId).get(key).getFinalIndex();

        List<Integer> _relatedAttacks = new ArrayList<>(relatedAttacks.get(attackId));
        _relatedAttacks.add(currentAttackId);
        relatedAttacks.put(currentAttackId, _relatedAttacks);

        attackId = currentAttackId;
        List<Guess> Lguesses = new ArrayList<>();
        guesses.put(attackId, Lguesses);

        this.currentAttackId++;

        try {
            removeSlave(key);
        } catch (RemoteException ex) {
            Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (initialIndex != finalIndex) {
            System.out.println("Novo Attack: " + attackId);
            this.addSlavesInfo(attacksInfo.size());

            int numberOfSlaves = slaves.size();
            final int amountPerSlave = (finalIndex - initialIndex) / numberOfSlaves;
            final int residualAmount = (finalIndex - initialIndex) % numberOfSlaves;

            //System.out.println("amount: "+amountPerSlave+" residual: "+residualAmount);
            Iterator entries = slaves.entrySet().iterator();
            //System.out.println("tamanho escravos: "+slaves.size());
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                Slave slave = (Slave) entry.getValue();

                if (entries.hasNext()) {
                    attacksInfo.get(attacksInfo.size() - 1).get(entry.getKey()).setFinalIndex(initialIndex + amountPerSlave + 1);
                    slave.startSubAttack(ciphertext, knowntext, initialIndex,
                            initialIndex + amountPerSlave, attackId, this);
                } else {
                    attacksInfo.get(attacksInfo.size() - 1).get(entry.getKey()).setFinalIndex(initialIndex + amountPerSlave + residualAmount);
                    slave.startSubAttack(ciphertext, knowntext, initialIndex,
                            initialIndex + amountPerSlave + residualAmount - 1,
                            attackId, this);
                }

                initialIndex += amountPerSlave;
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

            List<Integer> _relatedAttacks = new ArrayList<>();
            _relatedAttacks.add(attackId);
            relatedAttacks.put(attackId, _relatedAttacks);

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
                        long diff = t - attacksInfo.get(localAttackId).get(localSlaveKey).getTime();
                        if (diff > 20 && !attacksInfo.get(localAttackId).get(localSlaveKey).isEnded()) {
                            redivideIndex(localAttackId, localSlaveKey, ciphertext, knowntext);

                            timers.get(localSlaveKey).cancel();
                        } else {
                            System.err.println("Escravo funcionando");
                        }
                    } catch (RemoteException er) {
                        try {
                            redivideIndex(localAttackId, localSlaveKey, ciphertext, knowntext);
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

            attacksInfo.get(attackId).get(entry.getKey()).setInitialIndex(currentIndex);
            attacksInfo.get(attackId).get(entry.getKey()).setCurrentIndex(currentIndex);

            if (entries.hasNext()) {
                attacksInfo.get(attackId).get(entry.getKey()).setFinalIndex(currentIndex + amountPerSlave + 1);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave, attackId, this);
            } else {
                attacksInfo.get(attackId).get(entry.getKey()).setFinalIndex(currentIndex + amountPerSlave + residualAmount);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave + residualAmount - 1,
                        attackId, this);
                //attacksInfo.get(attackId).get(entry.getKey()).setTerminou(true);
                //System.out.println("Escravo "+attacksInfo.get(attackId).get(entry.getKey()).getNome()+" terminou");
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
        synchronized (attacksInfo.get(attackId)) {
            try {
                attacksInfo.get(attackId).wait();
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
        return listToArray(guesses.get(attackId));
    }

    public static void main(String[] args) {

        try {

            MasterImpl obj = new MasterImpl();
            Master objref = (Master) UnicastRemoteObject.exportObject(obj, 0);

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
