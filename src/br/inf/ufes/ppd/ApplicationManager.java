/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lucas
 * Classe responsável por mandar o mestre iniciar o ataque.
 */
public class ApplicationManager {
    
    public static void saveFile(String filename, byte[] data) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(filename);
        out.write(data);
        out.close();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String host = (args.length < 1) ? null : args[0];
        
        if(Files.exists(Paths.get("teste.txt"))) { 
            System.out.println("Arquivo existe");
            try {
                Registry registry = LocateRegistry.getRegistry(host);
                Master master = (Master) registry.lookup("Mestre");

                Guess[] guesses = master.attack("teste.txt.cipher".getBytes(), "ipsum".getBytes());

                System.out.println("------------------------Guesses------------------------");
                for(Guess guess : guesses) {
                    System.out.println("Guess: " + guess.getKey());
                }
                System.out.println("-------------------------------------------------------");
            } catch (Exception e) {
                System.err.println("Master exception: " + e.toString());
                e.printStackTrace();
            }
        
        }
       else
        {
            int len;
            
            if(args.length > 3)
                {
                    len = Integer.parseInt(args[3]);
                }
            else
            {
                len = (int) (Math.random() * (100000 - 1000)) + 1000;
            }
            
            System.out.println("Arquivo não existe");
            byte[] bytes = new byte[20];
            SecureRandom.getInstanceStrong().nextBytes(bytes);
            
            try {
                saveFile("t.txtcipher",bytes);
                System.out.println("criou arquivo");
            } catch (IOException ex) {
                Logger.getLogger(ApplicationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
