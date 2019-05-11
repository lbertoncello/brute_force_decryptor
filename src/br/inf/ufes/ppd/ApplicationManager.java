/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

/**
 *
 * @author lucas
 * Classe responsável por mandar o mestre iniciar o ataque.
 */
public class ApplicationManager {

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            Master master = (Master) registry.lookup("Mestre");

            Guess[] guesses = master.attack("teste.txt.cipher".getBytes(), "ipsum".getBytes());
            
            System.out.println("------------------------Guesses------------------------");
            for(Guess guess : guesses) {
                System.out.println("Guess: " + new String(guess.getMessage()));
            }
            System.out.println("-------------------------------------------------------");
        } catch (Exception e) {
            System.err.println("Master exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
