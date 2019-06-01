package br.inf.ufes.ppd.utils;

import br.inf.ufes.ppd.Guess;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * *
 * Classe de utilidades para utilização por parte do Aplicativo Cliente.
 */
public class TrabUtils {

    public static List<String> readDictionary(String filename) {
        List<String> dictionary = new ArrayList<>();

        try {
            FileReader file = new FileReader(filename);
            BufferedReader readFile = new BufferedReader(file);

            String line = readFile.readLine();

            while (line != null) {
                dictionary.add(line);

                line = readFile.readLine();
            }

            file.close();
        } catch (IOException e) {
            System.err.printf("Erro na abertura do arquivo: %s.\n",
                    e.getMessage());
        }

        return dictionary;
    }

    //Realiza letura de arquivo do dicionário
    public static byte[] readFile(String filename) throws IOException {
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

    public static void saveFile(String filename, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(filename);
        out.write(data);
        out.close();
    }

    public static byte[] encrypt(byte[] key, byte[] message) throws Exception {

        SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        byte[] encrypted = cipher.doFinal(message);

        return encrypted;
    }

    public static byte[] decrypt(byte[] key, byte[] message) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, IOException {
        byte[] decrypted;
        SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");

        Cipher cipher = Cipher.getInstance("Blowfish");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        System.out.println("message size (bytes) = " + message.length);
        try {

            decrypted = cipher.doFinal(message);

        } catch (javax.crypto.BadPaddingException e) {
            // essa excecao e jogada quando a senha esta incorreta
            // porem nao quer dizer que a senha esta correta se nao jogar essa excecao
            System.out.println("Senha invalida.");
            return null;

        }
        return decrypted;
    }

    /**
     * *
     * Sorteia uma string dentre um arquivo com várias chaves.
     *
     * @param path Local do arquivo com as chaves disponíveis
     * @return String: chave sorteada
     * @throws IOException
     */
    public static String sortKey() throws IOException {
        String filename = "dictionary.txt";
        List<String> dic = readDictionary(filename);
        Random r = new Random();
        int ChoosenIndex = r.nextInt(dic.size() + 1);
        return dic.get(ChoosenIndex);
    }

    /**
     * *
     * Cria um vetor de bytes de tamanho aleatório dentro do range especificado.
     *
     * @param min Quantidade minima de bytes
     * @param max Quantidade maxima de bytes
     * @return Vetor de bytes preenchidos com valores aleatórios
     */
    public static byte[] createRandomArrayBytes(int length) throws NoSuchAlgorithmException {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }

    /**
     * *
     * Extrai um determinado trecho de um vetor de bytes
     *
     * @param text Vetor de byytes com a informação
     * @param qtdBytes Quantidade de informação extraída
     * @return Vetor de bytes contendo a informação
     */
    public static byte[] extractKnowText(byte[] text, int qtdBytes) {
        byte[] knowText = new byte[qtdBytes];
        int metade = (int) (text.length / 2);
        for (int i = 0; i < qtdBytes; i++) {
            knowText[i] = text[metade + i];
        }
        return knowText;
    }

    public static Guess isValidKey(byte[] ciphertext, byte[] knowntext, String key) {
        Guess guess = null;
        try {
            byte[] dec = TrabUtils.decrypt(key.getBytes(), ciphertext);

            if (findBytes(dec, knowntext)) {
                guess = new Guess();
                guess.setKey(key);
                guess.setMessage(dec);
            }

        } catch (Exception e) {
            guess = null;
        }

        return guess;
    }

    public static boolean findBytes(byte[] data, byte[] find) {
        try {
            if (find.length <= data.length) {
                for (int i = 0; i < data.length; i++) {
                    if (data[i] == find[0]) {
                        int j, k;
                        for (j = 1, k = i + 1; j < find.length; j++, k++) {
                            if (data[k] != find[j]) {
                                break;
                            }
                        }

                        if (j == find.length) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    public static void saveResults(String arq, int tam, double time) throws IOException {
        BufferedWriter buffWrite = new BufferedWriter(new FileWriter(arq, true));
        buffWrite.append(tam + "," + time + System.getProperty("line.separator"));
        buffWrite.close();
    }

}
