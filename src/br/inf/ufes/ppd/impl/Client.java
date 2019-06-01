/*
 * Client da aplicação.
 */
package br.inf.ufes.ppd.impl;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.utils.TrabUtils;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author lucas Classe responsável por mandar o mestre iniciar o ataque.
 */
public class Client {

    public static void main(String[] args) throws RemoteException, NotBoundException, Exception {

        byte[] ciphertext = null;
        String hostname = args[0];
        String filename = args[1];
        byte[] knowntext;
        System.out.println("Cliente");
        byte[] key = null;

        System.out.println("nome arquivo: " + filename);
        //Verifica se foi passado um arquivo criptografado
        if (Files.exists(Paths.get(filename))) {

            System.out.println("Arquivo existe");

            ciphertext = TrabUtils.readFile(filename);
            //Palavra conhecida
            knowntext = args[2].getBytes();
        //Caso não tenha sido, gera-se um aleatóriamente
        } else {

            System.out.println("Arquivo não existe");

            int len;

            if (args.length > 3) {
                System.out.println("tamanho informado");
                len = Integer.parseInt(args[3]);
            } else {
                System.out.println("tamanho nao informado");
                len = (int) (Math.random() * (100000 - 1000)) + 1000;
                len = len - (len % 8);
            }

            ciphertext = TrabUtils.createRandomArrayBytes(len);

            System.out.println("Arquivo criado");
            //extraindo somente 8 bytes de informação
            knowntext = TrabUtils.extractKnowText(ciphertext, 8);

            key = TrabUtils.sortKey().getBytes();

            ciphertext = TrabUtils.encrypt(key, ciphertext);
            TrabUtils.saveFile(args[1], ciphertext);
            System.out.println("Arquivo salvo");

        }

        try {

            Registry registry = LocateRegistry.getRegistry(hostname);
            Master master = (Master) registry.lookup("mestre");
            Guess[] guesses = master.attack(ciphertext, knowntext);

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
