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
import java.util.List;
import java.util.UUID;

/**
 *
 * @author lucas
 */
public class SlaveImpl implements Slave {

    private final String dicFilename = "dictionary.txt";
    private final String docFilename = "teste.dat";

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
    public void startSubAttack(byte[] ciphertext, byte[] knowntext,
            long initialwordindex, long finalwordindex, int attackNumber,
            SlaveManager callbackinterface) throws RemoteException {

        List<String> dictionary = readDictionary(dicFilename);

        for (long i = initialwordindex; i <= finalwordindex; i++) {
            String[] args = new String[2];
            args[0] = dictionary.get((int) i);
            args[1] = docFilename;

            Decrypt.main(args);
        }

    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            Master master = (Master) registry.lookup("Mestre");
            
            SlaveImpl obj = new SlaveImpl();
            Slave objref = (Slave) UnicastRemoteObject.exportObject(obj, 0);
            UUID id = java.util.UUID.randomUUID();
            String name = "Escravo " + id;
            
            master.addSlave(objref, name, id);
        } catch (Exception e) {
            System.err.println("Master exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
