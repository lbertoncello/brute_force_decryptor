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

/**
 *
 * @author lucas
 */
public class MasterImpl implements Master {

    private final String filename = "dictionary.txt";

    private int attackCurrentId = 0;
    private Map<UUID, Slave> slaves = new TreeMap<>();
    private Map<UUID, String> slavesNames = new TreeMap<>();
    private List<Guess> guesses = new ArrayList<>();
    private Map<Integer, Slave> attacks = new TreeMap<>();
    private Map<UUID, SlaveInfo> dados_slaves = new TreeMap<>();
    
    private Guess[] listToArray(List<Guess> list) {
        Guess[] guessesArray = new Guess[list.size()];
        
        for(int i = 0; i < guesses.size(); i++) {
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
                SlaveInfo si = new SlaveInfo(slavekey,slaveName,s);
                slaves.put(slavekey, s);
                dados_slaves.put(slavekey, si);
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
            dados_slaves.remove(slaveKey);
        }
    }

    @Override
    public void foundGuess(UUID slaveKey, int attackNumber, long currentindex,
            Guess currentguess) throws RemoteException {
        guesses.add(currentguess);

        System.out.println("--------------Guess-----------------------");
        System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                + " índice: " + currentindex + " | Mensagem candidata: "
                + new String(currentguess.getMessage()));
        System.out.println("------------------------------------------");
        
        dados_slaves.get(slaveKey).setCorrente_Index((int)currentindex);
        
    }

    @Override
    public void checkpoint(UUID slaveKey, int attackNumber, long currentindex)
            throws RemoteException {
        System.out.println("--------------------Checkpoint--------------------");
        System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                + " índice: " + currentindex);
        System.out.println("---------------------------------------------------");
        
        if(this.dados_slaves.get(slaveKey).getTempo() == 0)
        {
            this.dados_slaves.get(slaveKey).setTempo(System.nanoTime()/1000000000);
        }
        else
        {
            long t = System.nanoTime()/1000000000;
            
            long diff = this.dados_slaves.get(slaveKey).getTempo()-t;
            
            if(diff > 20 && !this.dados_slaves.get(slaveKey).isTerminou())
            {
                System.out.println("Remover escravo");
            }
        }
        
    }

    /**
     * Operação oferecida pelo mestre para iniciar um ataque.
     *
     * @param ciphertext mensagem critografada
     * @param knowntext trecho conhecido da mensagem decriptografada
     * @return vetor de chutes: chaves candidatas e mensagem decriptografada com
     * chaves candidatas
     */
    @Override
    public Guess[] attack(byte[] ciphertext, byte[] knowntext)
            throws RemoteException {

        int numberOfSlaves = slaves.size();
        List<String> dictionary = readDictionary(filename);

        int amountPerSlave =  dictionary.size() / numberOfSlaves;
        int residualAmount =  dictionary.size() % numberOfSlaves;
        int currentIndex = 0;

        Iterator entries = slaves.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            UUID idd = (UUID) entry.getKey();
            
            Slave slave = (Slave) entry.getValue();
            attacks.put(attackCurrentId, slave);
            attackCurrentId++;
            
            dados_slaves.get(idd).setInicio_Index(currentIndex);

            if (entries.hasNext()) {
                dados_slaves.get(idd).setFinal_Index(currentIndex+ amountPerSlave - 1);
                
                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave - 1, attackCurrentId, this);
            } else {
                dados_slaves.get(idd).setFinal_Index(currentIndex + amountPerSlave+amountPerSlave - 1);
                slave.startSubAttack(ciphertext, knowntext, currentIndex,
                        currentIndex + amountPerSlave + residualAmount - 1,
                        attackCurrentId, this);
            }

            currentIndex += amountPerSlave;
        }

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
