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
    private Map<Integer, Guess> guesses = new TreeMap<>();
    private Map<Integer, Slave> attacks = new TreeMap<>();

    protected List<String> readDictionary(String filename) {
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
            slaves.put(slavekey, s);
            slavesNames.put(slavekey, slaveName);
        }

        System.out.println("Slave de nome " + slaveName
                + " foi registrado com sucesso!");
    }

    @Override
    public void removeSlave(UUID slaveKey) throws RemoteException {
        synchronized (slaves) {
            slaves.remove(slaveKey);
            slavesNames.remove(slaveKey);
        }
    }

    @Override
    public void foundGuess(UUID slaveKey, int attackNumber, long currentindex,
            Guess currentguess) throws RemoteException {
        guesses.put(attackNumber, currentguess);

        System.out.println("Guess encontrado!");
        System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                + " índice: " + currentindex + " | Mensagem candidata: "
                + currentguess.getMessage());
    }

    @Override
    public void checkpoint(UUID slaveKey, int attackNumber, long currentindex)
            throws RemoteException {
        System.out.println("Guess encontrado!");
        System.out.println("Nome do escravo: " + slavesNames.get(slaveKey)
                + " índice: " + currentindex);
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

        int amountPerSlave = numberOfSlaves / dictionary.size();
        int residualAmount = numberOfSlaves % dictionary.size();
        int currentIndex = 0;

        for (Map.Entry<UUID, Slave> entry : slaves.entrySet()) {
            Slave slave = entry.getValue();
            attacks.put(attackCurrentId, slave);
            attackCurrentId++;

            slave.startSubAttack(ciphertext, knowntext, currentIndex,
                    amountPerSlave - 1, attackCurrentId, this);
        }

        return null;
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
