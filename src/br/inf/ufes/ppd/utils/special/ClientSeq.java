/*
 * Cliente especial responsável por fazer a execução sequencial.
 */
package br.inf.ufes.ppd.utils.special;

import br.inf.ufes.ppd.Guess;
import br.inf.ufes.ppd.utils.TrabUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author natanael
 */
public class ClientSeq {

    public static List<Guess> attack(List<String> dictionary, byte[] cipherText, byte[] knowText) {
        List<Guess> listGuess = new ArrayList<>();

        for (String key : dictionary) {
            Guess g = isValidKey(cipherText, knowText, key);
            if (g != null) {
                listGuess.add(g);
            }
        }

        return listGuess;
    }

    public static Guess isValidKey(byte[] ciphertext, byte[] knowntext, String key) {
        Guess guess = null;
        try {
            byte[] dec = TrabUtils.decrypt(key.getBytes(), ciphertext);

            if (TrabUtils.findBytes(dec, knowntext)) {
                guess = new Guess();
                TrabUtils.saveFile(key + ".msg", dec);
                guess.setKey(key);
                guess.setMessage(dec);
            }

        } catch (Exception e) {
            guess = null;
        }

        return guess;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, Exception {
        byte[] ciphertext = null;
        byte[] palavra;
        List<String> Keys;
        List<Guess> guesses;
        double tempo1;
        double tempo2;
        String filename;

        /*
         * Argumentos que devem ser fornecidos 
         * args[0] - arquivo para geração de texto
         * args[1] - palavra conhecida
         * args[2] - tamanho arquivo(somente em casos que o arquivo para geração de texto não exista)
         */
        filename = args[0];

        if (Files.exists(Paths.get(filename))) {
            System.out.println("Arquivo existe");

            ciphertext = TrabUtils.readFile(filename);
            //Palavra conhecida
            palavra = args[1].getBytes();

        } else {

            System.out.println("Arquivo não existe");
            byte[] Text;
            byte[] key;
            int len;

            if (args.length > 2) {
                len = Integer.parseInt(args[2]);
            } else {
                len = (int) (Math.random() * (100000 - 1000)) + 1000;
                len = len - (len % 8);
            }

            Text = TrabUtils.createRandomArrayBytes(len);

            //extraindo somente 8 bytes de informação
            palavra = TrabUtils.extractKnowText(Text, 8);

            key = TrabUtils.sortKey().getBytes();

            ciphertext = TrabUtils.encrypt(key, Text);
            TrabUtils.saveFile(filename, ciphertext);
        }

        Keys = TrabUtils.readDictionary("dictionary.txt");

        tempo1 = System.nanoTime();
        guesses = attack(Keys, ciphertext, palavra);
        tempo2 = System.nanoTime() - tempo1;

        double tempo_final = tempo2 / 1_000_000_000;
        if (guesses.size() > 0) {
            System.out.println("Foram encontradas " + guesses.size() + " possíveis chaves.");

        } else {
            System.out.println("Sem palavras chave candidatas!");
        }

        System.out.println("Tempo total " + tempo_final + " s");
        TrabUtils.saveResults("analise_cliente_seq.csv", ciphertext.length, tempo_final);

    }

}
