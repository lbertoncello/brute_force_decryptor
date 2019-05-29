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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
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
public class SlaveImpl implements Slave {

    private final String dicFilename = "dictionary.txt";
    //private final String docFilename = "IMG_0804.JPG.cipher";
    
    List<String> dictionary = readDictionary(dicFilename);

    private UUID id = java.util.UUID.randomUUID();

    private Map<Integer, Integer> currentIndex = new ConcurrentHashMap<>();

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return this.id;
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
                dictionary.add(line.replace("\n", "").replace(" ", ""));

                line = readFile.readLine(); // lê da segunda até a última linha
            }

            file.close();
        } catch (IOException e) {
            System.err.printf("Erro na abertura do arquivo: %s.\n",
                    e.getMessage());
        }

        return dictionary;
    }

    private byte[] readDecryptedTextAsBytes(String filename) {

        Path fileLocation = Paths.get(filename);
        byte[] data = null;

        try {
            data = Files.readAllBytes(fileLocation);
        } catch (IOException ex) {
            Logger.getLogger(SlaveImpl.class.getName()).log(Level.SEVERE, null, ex);
        }

        return data;
    }

    //Retorna true se 
    private boolean compareBytes(byte[] text, byte[] knowntext) {
        for (int i = 0; i < text.length - knowntext.length; i++) {
            for (int j = 0; j < knowntext.length; j++) {
                if (text[i + j] != knowntext[j]) {
                    break;
                }
                if (j == knowntext.length - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    //Verifica se o knowtext está na mensagem descriptografada
    private boolean checkDecryptedText(String textFilename, byte[] knowntext) {
        if (checkFileExists(textFilename)) {         
            byte[] decryptedText = readDecryptedTextAsBytes(textFilename);
            if (compareBytes(decryptedText, knowntext)) {
                return true;
            }

            deleteFile(textFilename);
        }

        return false;
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

    @Override
    public void startSubAttack(byte[] ciphertext, byte[] knowntext,
            long initialwordindex, long finalwordindex, int attackNumber,
            SlaveManager callbackinterface) throws RemoteException {

        currentIndex.put(attackNumber, (int) initialwordindex);

        Thread thread = new Thread() {
            public void run() {
                Decrypt decrypt = new Decrypt();
                Timer timer = new Timer();

                //Envia um checkpoint a cada 10 segundos
                timer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        System.err.println("Tentando enviar o checkpoint...");
                        try {
                            callbackinterface.checkpoint(id, attackNumber, currentIndex.get(attackNumber));

                            System.err.println("Checkpoint enviado com sucesso!");
                        } catch (RemoteException e) {
                            System.err.println("Error trying to call 'checkpoint' "
                                    + "function: " + e.toString());
                            e.printStackTrace();
                        }
                    }
                },
                        10000,
                        10000);

                for (long index = initialwordindex; index <= finalwordindex; index++) {
                    currentIndex.put(attackNumber, (int) index);
                    String key = dictionary.get((int) index);

                    if (key.length() < 3) {
                        continue;
                    }

                    if(!decrypt.decrypt(key, ciphertext)) {
                        continue;
                    }

                    System.out.println("key " + key);
                    String decryptedFilename = key + ".msg";


                    if (checkDecryptedText(decryptedFilename, knowntext)) {
                        System.out.println("Decrypted filename: " + decryptedFilename);
                        Guess guess = new Guess();
                        guess.setKey(key);
                        guess.setMessage(readDecryptedTextAsBytes(decryptedFilename));
                        System.out.println("gueses passou");

                        try {
                            System.out.println("callback");
                            callbackinterface.foundGuess(id, attackNumber, currentIndex.get(attackNumber), guess);
                            System.out.println("passou");
                        } catch (RemoteException ex) {
                            System.out.println("Deu ruim");
                            Logger.getLogger(SlaveImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                currentIndex.put(attackNumber, currentIndex.get(attackNumber) + 1);

                try {
                    callbackinterface.checkpoint(id, attackNumber, currentIndex.get(attackNumber));
                } catch (RemoteException ex) {
                    Logger.getLogger(SlaveImpl.class.getName()).log(Level.SEVERE, null, ex);
                }

                timer.cancel();
                System.out.println("Fim do subtaque do escravo " + id);

            }

        };
        thread.start();
    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];

        try {
            //System.setProperty( "java.rmi.server.hostname", "192.168.0.0");
            Registry registry = LocateRegistry.getRegistry("localhost");
            Master master = (Master) registry.lookup("Mestre");

            UUID id = java.util.UUID.randomUUID();
            String name = "Escravo " + id;

            SlaveImpl obj = new SlaveImpl();
            obj.setId(id);
            Slave objref = (Slave) UnicastRemoteObject.exportObject(obj, 0);

            System.err.println("Tentando se registrar no mestre...");
            master.addSlave(objref, name, id);
            System.err.println("Registro concluído!");

            Timer timer = new Timer();

            //Se registra novamente a cada 30 segundos
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    System.err.println("Tentando se re-registrar...");

                    try {
                        master.addSlave(objref, name, id);
                        System.err.println("Re-registro feito com sucesso!");
                    } catch (RemoteException e) {
                        System.err.println("Erro ao se re-registrar!");
                        e.printStackTrace();
                    }
                }
            },
                    30000,
                    30000);

        } catch (Exception e) {
            System.err.println("Slave exception: " + e.toString());
            e.printStackTrace();
        }
    }
}