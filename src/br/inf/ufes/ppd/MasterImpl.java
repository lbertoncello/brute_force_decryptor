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
    private List<Guess> guesses = new ArrayList<>();
    private Map<Integer, Slave> attacks = new ConcurrentHashMap<>();
    private Map<UUID, SlaveInfo> slavesInfo = new ConcurrentHashMap<>();

    private Guess[] listToArray(List<Guess> list) {
        Guess[] guessesArray = new Guess[list.size()];

        for (int i = 0; i < guesses.size(); i++) {
            guessesArray[i] = guesses.get(i);
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

    @Override
    public void addSlave(Slave s, String slaveName, UUID slavekey)
            throws RemoteException {

        synchronized (slaves) {
            if (!slaves.containsKey(slavekey)) {
                SlaveInfo si = new SlaveInfo(slavekey, slaveName, s);
                slaves.put(slavekey, s);
                slavesInfo.put(slavekey, si);
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
            slavesInfo.remove(slaveKey);
        }
    }

    @Override
    public void foundGuess(UUID slaveKey, int attackNumber, long currentindex,
            Guess currentguess) throws RemoteException {
        guesses.add(currentguess);

        System.out.println("--------------Guess-----------------------");
        System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                + " índice: " + currentindex + " | Chave candidata: "
                + currentguess.getKey());
        System.out.println("------------------------------------------");

        slavesInfo.get(slaveKey).setCorrente_Index((int) currentindex);

    }

    @Override
    public void checkpoint(UUID slaveKey, int attackNumber, long currentindex)
            throws RemoteException {
        
        if (slavesInfo.get(slaveKey).getFinal_Index() == currentindex) {
            slavesInfo.get(slaveKey).setTerminou(true);
            System.out.println("Último checkpoint!");

            if (this.check_to_notify()) {
                synchronized (slavesInfo) {
                    slavesInfo.notify();
                }
            }
        }

        System.out.println("--------------------Checkpoint--------------------");
        System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                + " índice: " + currentindex);
        System.out.println("---------------------------------------------------");

        this.slavesInfo.get(slaveKey).setTempo(System.nanoTime() / 1000000000);
    }

    private boolean check_to_notify() {
        Iterator entr = slaves.entrySet().iterator();

        while (entr.hasNext()) {
            Map.Entry entry = (Map.Entry) entr.next();
            UUID idd = (UUID) entry.getKey();

            if (!slavesInfo.get(idd).isTerminou()) {
                return false;
            }

        }

        return true;
    }

    @Override
    public Guess[] attack(byte[] ciphertext, byte[] knowntext)
            throws RemoteException {

        int numberOfSlaves = slaves.size();
        List<String> dictionary = readDictionary(filename);

        final int amountPerSlave = dictionary.size() / numberOfSlaves;
        final int residualAmount = dictionary.size() % numberOfSlaves;
        int attackCurrentId = 0;
        int currentIndex = 0;

        Iterator entries = slaves.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            Slave slave = (Slave) entry.getValue();
            attacks.put(attackCurrentId, slave);
            attackCurrentId++;

            SlaveThread slaveThread;

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    System.err.println("Tentando verificar se o escravo ainda funciona...");

                    long t = System.nanoTime() / 1000000000;
                    long diff = t - slavesInfo.get((UUID) entry.getKey()).getTempo();

                    if (diff > 20 && !slavesInfo.get((UUID) entry.getKey()).isTerminou()) {
                        System.out.println("Retirar escravo");
                    } else {
                        System.err.println("Escravo funcionando");
                    }
                }
            },
                    20000
            );

            slavesInfo.get(entry.getKey()).setInicio_Index(currentIndex);

            if (entries.hasNext()) {
                slavesInfo.get(entry.getKey()).setFinal_Index(currentIndex + amountPerSlave + 1);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave, attackCurrentId, this);
            } else {
                slavesInfo.get(entry.getKey()).setFinal_Index(currentIndex + amountPerSlave + residualAmount);

                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave + residualAmount - 1,
                        attackCurrentId, this);
            }

            currentIndex += amountPerSlave;
        }

        /*
            FAZER ESPERAR ATÉ QUE OS ESCRAVOS TENHAM TERMINADO PRA RETORNAR A LISTA.
            PRA SABER QUANDO ELES TERMINARAM ACHO QUE PODE CRIAR UMA LISTA BOOLEANA
            DIZENDO SE ELES TERMINARAM OU NÃO.
         */
        synchronized (slavesInfo) {
            try {
                slavesInfo.wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(MasterImpl.class.getName()).log(Level.SEVERE, null, ex);
            }

        };
        System.out.println("Ataque terminado!");
        return listToArray(guesses);
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
