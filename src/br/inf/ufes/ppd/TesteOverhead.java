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
public class TesteOverhead {
    

    public static void main(String[] args) throws RemoteException, NotBoundException, Exception {

	byte[] ciphertext = null;
        String hostname = args[0];
        String fileName = args[1];
        byte[] knowText;
        System.out.println("Cliente");
        //String hostname = "localhost";
        byte[] key = null;
		
        System.out.println("nome arquivo: "+fileName);
        if(Files.exists(Paths.get(fileName))) { 
      
            System.out.println("Arquivo existe");
            
            ciphertext = TrabUtils.readFile(fileName);
                //Palavra conhecida
            knowText = args[2].getBytes();

        }
        else
        {
            
            System.out.println("Arquivo não existe");
         
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
	    knowText = TrabUtils.extractKnowText(ciphertext, 8);
								
	    key = TrabUtils.sortKey().getBytes();

	    ciphertext = TrabUtils.encrypt(key,ciphertext);
	    TrabUtils.saveFile(args[1], ciphertext);
            System.out.println("Arquivo salvo");
					
	}
        
         try {

                Registry registry = LocateRegistry.getRegistry(hostname);
                Master master = (Master) registry.lookup("mestre");
                double t1 = System.nanoTime();
                Guess[] guesses = master.attack(ciphertext, knowText);
                double t2 = System.nanoTime() - t1;
                double t3 = t2/1_000_000_000;
               System.out.println("Overhead: "+t3);
               TrabUtils.Resultados("analise_overhead.csv", ciphertext.length,t3);
                System.out.println("-------------------------------------------------------");
            } catch (Exception e) {
                System.err.println("Master exception: " + e.toString());
              
		
        }

    }
    
}

