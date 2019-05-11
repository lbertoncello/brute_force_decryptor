/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lucas
 */
public class SlaveImpl implements Slave {

    private final String dicFilename = "dictionary.txt";
    private final String docFilename = "teste.txt.cipher";

    private UUID id = java.util.UUID.randomUUID();

    private long currentIndex;

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

    //Lê a mensagem que foi descriptografada
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

    //Lê a mensagem que foi descriptografada
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

    //Verifica se  o arquivo existe
    private boolean checkFileExists(String filename) {
        File file = new File(filename);

        return file.exists();
    }

    //Deleta o arquivo
    private void deleteFile(String filename) {
        File file = new File(filename);
        file.delete();
    }

    //Verifica se o knowtext está na mensagem descriptografada
    private boolean checkDecryptedText(String textFilename, byte[] knowntext) {
        if (checkFileExists(textFilename)) {
            List<String> decryptedText = readDecryptedTextAsList(textFilename);

            for (String word : decryptedText) {
                if (word.compareTo(new String(knowntext)) == 0) {
                    return true;
                }
            }
            deleteFile(textFilename);
        }

        return false;
    }

    @Override
    public void startSubAttack(byte[] ciphertext, byte[] knowntext,
            long initialwordindex, long finalwordindex, int attackNumber,
            SlaveManager callbackinterface) throws RemoteException {

        List<String> dictionary = readDictionary(dicFilename);

        //Envia um checkpoint a cada 10 segundos
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
            @Override
            public void run() {
                System.err.println("Tentando enviar o checkpoint...");
                try {
                    callbackinterface.checkpoint(id, attackNumber, currentIndex);
                    System.err.println("Checkpoint enviado com sucesso!");
                } catch (RemoteException e) {
                    System.err.println("Error trying to call 'checkpoint' "
                            + "function: " + e.toString());
                    e.printStackTrace();
                }
            }
        },
                10000
        );

        for (currentIndex = initialwordindex; currentIndex <= finalwordindex; currentIndex++) {
            String key = dictionary.get((int) currentIndex);

            if (key.length() < 3) {
                continue;
            }

            String[] args = new String[2];
            args[0] = key;
            args[1] = docFilename;

            Decrypt.main(args);

            String decryptedFilename = key + ".msg";

            if (checkDecryptedText(decryptedFilename, knowntext)) {
                System.out.println("Decrypted filename: " + decryptedFilename);
                Guess guess = new Guess();
                guess.setKey(key);
                guess.setMessage(readDecryptedTextAsBytes(decryptedFilename));

                callbackinterface.foundGuess(this.id, attackNumber, currentIndex, guess);
            }
        }

        callbackinterface.checkpoint(id, attackNumber, currentIndex);
        System.out.println("Fim do subtaque do escravo " + id);
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

            //Se registra novamente a cada 30 segundos
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                @Override
                public void run() {
                    System.err.println("Tentando se re-registrar...");

                    try {
                        master.addSlave(objref, name, id);
                        System.err.println("Re-registro feito com sucesso!");
                    } catch (RemoteException e) {
                        System.err.println("Error trying to call 'checkpoint' "
                                + "function: " + e.toString());
                        e.printStackTrace();
                    }
                }
            },
                    30000
            );
        } catch (Exception e) {
            System.err.println("Master exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
