/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author lucas
 * Classe responsável por mandar o mestre iniciar o ataque.
 */
public class Cliente {
    

    public static void main(String[] args) throws RemoteException, NotBoundException, Exception {

	byte[] ciphertext = null;
        String hostname = args[0];
        System.out.println("Cliente");
        //String hostname = "localhost";
        byte[] key = null;
        byte[] palavra;
		
        System.out.println("nome arquivo: "+args[1]);
        if(Files.exists(Paths.get(args[1]))) { 
            
            System.out.println(args[1]);
            System.out.println("Arquivo existe");
            
            ciphertext = TrabUtils.readFile(args[1]);
                //Palavra conhecida
            palavra = args[2].getBytes();

        }
        else
        {
            
            System.out.println("Arquivo não existe");
            byte[] knowText;
            int len;
            
            if(args.length > 3)
                {
                    System.out.println("tamanho informado");
                    len = Integer.parseInt(args[3]);
                }
            else
            {
                System.out.println("tamanho nao informado");
                len = (int) (Math.random() * (100000 - 1000)) + 1000;
                len = len - (len%8);
            }
        	
	    ciphertext = TrabUtils.createRandomArrayBytes(len);
		
            System.out.println("Arquivo criado");
			//extraindo somente 8 bytes de informação
	    palavra = TrabUtils.extractKnowText(ciphertext, 8);
								
	    key = TrabUtils.sortKey().getBytes();

	    ciphertext = TrabUtils.encrypt(key,ciphertext);
	    TrabUtils.saveFile(args[1], ciphertext);
            System.out.println("Arquivo salvo");
					
	}
        
         try {

                Registry registry = LocateRegistry.getRegistry(hostname);
                Master master = (Master) registry.lookup("Mestre");
                String name = args[1];
                Guess[] guesses = master.attack(name.getBytes(), palavra);
               
                System.out.println("------------------------Guesses------------------------");
                for (Guess guess : guesses) {
                    System.out.println("Guess: " + guess.getKey());
                }
                System.out.println("-------------------------------------------------------");
            } catch (Exception e) {
                System.err.println("Master exception: " + e.toString());
              
		
        }

    }
    
}

