/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author lucas
 * Classe responsável por mandar o mestre iniciar o ataque.
 */
public class Cliente {

    //Realiza letura de arquivo do dicionário
    private static byte[] readFile(String filename) throws IOException {
        File file = new File(filename);
        InputStream is = new FileInputStream(file);
        long length = file.length();
        // creates array (assumes file length<Integer.MAX_VALUE)
        byte[] data = new byte[(int) length];
        int offset = 0;
        int count = 0;
        while ((offset < data.length)
                && (count = is.read(data, offset, data.length - offset)) >= 0) {
            offset += count;
        }
        is.close();
        return data;
    }

    private static void saveFile(String filename, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(filename);
        out.write(data);
        out.close();
    }

    private static byte[] encrypt(byte[] key, String filename) throws Exception {

        byte[] message = readFile(filename);

        SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        byte[] encrypted = cipher.doFinal(message);

        saveFile(filename + ".cipher", encrypted);

        return encrypted;

    }
    

    public static void main(String[] args) throws RemoteException, NotBoundException, Exception {
        
        String hostname = (args.length < 2) ? null : args[2];

        byte[] chave = args[0].getBytes();

        byte[] ciphertext = encrypt(chave, args[1]);

            //Palavra conhecida
        byte[] palavra = args[2].getBytes();

            
        Registry registry = LocateRegistry.getRegistry(hostname);

        Master master = (Master) registry.lookup("mestre");

        Guess[] guesses = master.attack("teste.txt.cipher".getBytes(), "ipsum".getBytes());
            
            System.out.println("------------------------Guesses------------------------");
            for(Guess guess : guesses) {
                System.out.println("Guess: " + new String(guess.getMessage()));
            }
            System.out.println("-------------------------------------------------------");
    }
}
