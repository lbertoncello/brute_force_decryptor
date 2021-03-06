/*
 * Escravo especial para a medição do overhead.
 */
package br.inf.ufes.ppd.utils.special;

import br.inf.ufes.ppd.Master;
import br.inf.ufes.ppd.Slave;
import br.inf.ufes.ppd.SlaveManager;
import br.inf.ufes.ppd.impl.SlaveImpl;
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
public class SlaveDump implements Slave {

    private final String dicFilename = "dictionary.txt";

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

    public void startSubAttack(byte[] ciphertext, byte[] knowntext,
            long initialwordindex, long finalwordindex, int attackNumber,
            SlaveManager callbackinterface) throws RemoteException {

        System.out.println("Attack number: " + attackNumber);
        callbackinterface.checkpoint(id, attackNumber, finalwordindex);
    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];

        try {
            //System.setProperty( "java.rmi.server.hostname", "192.168.0.0");
            Registry registry = LocateRegistry.getRegistry(host);
            Master master = (Master) registry.lookup("mestre");

            UUID id = java.util.UUID.randomUUID();
            String name = "Escravo Dump" + id;

            SlaveDump obj = new SlaveDump();
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
