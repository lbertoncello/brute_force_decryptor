/*
 * Cliente especial que não necessita de argumentos da linha de comando (apenas para testes).
 */
package br.inf.ufes.ppd.utils.special;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.Master;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lucas Classe responsável por mandar o mestre iniciar o ataque.
 */
public class ApplicationManager {

    public static void saveFile(String filename, byte[] data) throws FileNotFoundException, IOException {
        FileOutputStream out = new FileOutputStream(filename);
        out.write(data);
        out.close();
    }

    private static byte[] readDecryptedTextAsBytes(String filename) throws IOException {

        File file = new File(filename);
        InputStream is = new FileInputStream(file);
        long length = file.length();

        //creates array (assumes file length<Integer.MAX_VALUE)
        byte[] data = new byte[(int) length];

        int offset = 0;
        int count = 0;

        while ((offset < data.length) && (count = is.read(data, offset, data.length - offset)) >= 0) {
            offset += count;
        }
        is.close();
        return data;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String host = (args.length < 1) ? "localhost" : args[0];
        //System.setProperty( "java.rmi.server.hostname", "192.168.0.0");
        String decryptedFilename = "50kb.txt.cipher";
        byte[] knowText = "ipsum".getBytes();

        if (Files.exists(Paths.get(decryptedFilename))) {
            System.out.println("Arquivo existe");
            try {

                Registry registry = LocateRegistry.getRegistry(host);
                Master master = (Master) registry.lookup("mestre");
                byte[] ciphertext = readDecryptedTextAsBytes(decryptedFilename);

                Guess[] guesses = master.attack(ciphertext, knowText);

                System.out.println("------------------------Guesses------------------------");
                for (Guess guess : guesses) {
                    System.out.println("Guess: " + guess.getKey());
                }
                System.out.println("-------------------------------------------------------");
            } catch (Exception e) {
                System.err.println("Master exception: " + e.toString());
                e.printStackTrace();
            }

        } else {
            int len;

            if (args.length > 3) {
                len = Integer.parseInt(args[3]);
            } else {
                len = (int) (Math.random() * (100000 - 1000)) + 1000;
            }

            System.out.println("Arquivo não existe");
            byte[] bytes = new byte[20];
            SecureRandom.getInstanceStrong().nextBytes(bytes);

            try {
                saveFile("t.txtcipher", bytes);
                System.out.println("criou arquivo");
            } catch (IOException ex) {
                Logger.getLogger(ApplicationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
