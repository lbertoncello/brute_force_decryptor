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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author lucas
 */
public class SlaveImpl implements Slave {

    private final String dicFilename = "dictionary.txt";
    private final String docFilename = "teste.dat";
    private UUID id = java.util.UUID.randomUUID();

    public void setId(UUID id) {
        this.id = id;
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

    private List<String> readDecryptedTextAsList(String filename) {
        List<String> decryptedText = new ArrayList<>();

        try {
            FileReader file = new FileReader(filename);
            BufferedReader readFile = new BufferedReader(file);

            String line = readFile.readLine(); // lê a primeira linha
// a variável "linha" recebe o valor "null" quando o processo
// de repetição atingir o final do arquivo texto

            while (line != null) {
                String[] words = line.replace("\n", "").toLowerCase().split(" ");

                for (String word : words) {
                    decryptedText.add(word);
                }

                line = readFile.readLine(); // lê da segunda até a última linha
            }

            file.close();
        } catch (IOException e) {
            System.err.printf("Erro na abertura do arquivo: %s.\n",
                    e.getMessage());
        }

        return decryptedText;
    }

    private byte[] readDecryptedTextAsBytes(String filename) {
        StringBuilder sb = new StringBuilder();

        try {
            FileReader file = new FileReader(filename);
            BufferedReader readFile = new BufferedReader(file);

            String line = readFile.readLine(); // lê a primeira linha
// a variável "linha" recebe o valor "null" quando o processo
// de repetição atingir o final do arquivo texto

            while (line != null) {
                sb.append(line);

                line = readFile.readLine(); // lê da segunda até a última linha
            }

            file.close();
        } catch (IOException e) {
            System.err.printf("Erro na abertura do arquivo: %s.\n",
                    e.getMessage());
        }

        return sb.toString().getBytes();
    }

    private boolean checkDecryptedText(String textFilename, byte[] knowntext) {
        List<String> decryptedText = readDecryptedTextAsList(textFilename);

        for (String word : decryptedText) {
            if (word.compareTo(Arrays.toString(knowntext)) == 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void startSubAttack(byte[] ciphertext, byte[] knowntext,
            long initialwordindex, long finalwordindex, int attackNumber,
            SlaveManager callbackinterface) throws RemoteException {

        List<String> dictionary = readDictionary(dicFilename);

        for (long i = initialwordindex; i <= finalwordindex; i++) {
            String key = dictionary.get((int) i);
            
            String[] args = new String[2];
            args[0] = key;
            args[1] = docFilename;

            Decrypt.main(args);

            String decryptedFilename = key+".msg";
            
            if (checkDecryptedText(decryptedFilename, knowntext)) {
                Guess guess = new Guess();
                guess.setKey(key);
                guess.setMessage(readDecryptedTextAsBytes(decryptedFilename));
                
                callbackinterface.foundGuess(this.id, attackNumber, i, guess);
            }
        }

    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            Master master = (Master) registry.lookup("Mestre");

            UUID id = java.util.UUID.randomUUID();
            String name = "Escravo " + id;
            
            SlaveImpl obj = new SlaveImpl();
            obj.setId(id);
            Slave objref = (Slave) UnicastRemoteObject.exportObject(obj, 0);
            
            System.err.println("Tentando se registrar no mestre...");
            master.addSlave(objref, name, id);
            System.err.println("Registro concluído!");
        } catch (Exception e) {
            System.err.println("Master exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
