/*
 * Cliente especial para a medição do overhead.
 */
package br.inf.ufes.ppd.utils.special;

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
 * @author lucas
 * Classe responsável por mandar o mestre iniciar o ataque.
 */
public class ClientOverhead {
    

    public static void main(String[] args) throws RemoteException, NotBoundException, Exception {

	byte[] ciphertext = null;
        String hostname = args[0];
        String fileName = args[1];
        byte[] knowText;
        int qtdTestes =0;
        System.out.println("Cliente");
        //String hostname = "localhost";
        byte[] key = null;
		
        System.out.println("nome arquivo: "+fileName);
        if(Files.exists(Paths.get(fileName))) { 
      
            System.out.println("Arquivo existe");
            
            ciphertext = TrabUtils.readFile(fileName);
                //Palavra conhecida
            knowText = args[2].getBytes();
            qtdTestes = Integer.parseInt(args[3]);

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
                
                double mean = 0;
            System.out.println("Teste Overhead..");

            for (int i = 0; i < qtdTestes+1; i++) {
                //System.out.print("Teste " + (i + 1));
                double tempoInicio = System.nanoTime() / 1000000000.0;
                master.attack(ciphertext, knowText);
                double tempoFinal = System.nanoTime() / 1000000000.0;
                double diffTempo = tempoFinal - tempoInicio;
                
                if(i > 0){
                mean += diffTempo;
                }
                
                //System.out.println(" - " + diffTempo + " s");
                TrabUtils.saveResults("analise_cliente.csv", ciphertext.length, diffTempo);

            }
            mean = mean / qtdTestes;
            
               System.out.println("Overhead: "+mean);
               TrabUtils.saveResults("analise_overhead.csv", ciphertext.length,mean);
                System.out.println("-------------------------------------------------------");
            } catch (Exception e) {
                System.err.println("Master exception: " + e.toString());
              
		
        }

    }
    
}

