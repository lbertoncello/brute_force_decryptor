/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;


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
public class ClienteSeq {
    
    public static List<Guess> attack(List<String> dictionary, byte[] cipherText, byte[] knowText){
		List<Guess> listGuess = new ArrayList<>();

		for(String key : dictionary) {
			Guess g = isValidKey(cipherText, knowText, key);
			if(g != null) {
				listGuess.add(g);
			}
		}
		
		return listGuess;
	}
    
    public static Guess isValidKey(byte[] ciphertext, byte[] knowntext, String key) {
		Guess guess = null;
		try {
			byte[] dec = TrabUtils.decrypt(key.getBytes(), ciphertext);
			
			if(TrabUtils.findBytes(dec, knowntext)) {
				guess = new Guess();
                                TrabUtils.saveFile(key+ ".msg", dec);
				guess.setKey(key);
				guess.setMessage(dec);
			}			
					
		} catch (Exception e) {
			guess = null;
		}
		
		return guess;
	}
    
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, Exception
    {
        byte[] ciphertext = null;
        byte[] palavra;
        List<String> Keys;
	List<Guess> guesses;
        double tempo1;
        double tempo2;
      
		
    
        if(Files.exists(Paths.get(args[1]))) { 
                System.out.println("Arquivo existe");

                ciphertext = TrabUtils.readFile(args[1]);
                    //Palavra conhecida
                palavra = args[2].getBytes();

            }
        else
            {

                System.out.println("Arquivo não existe");
                byte[] Text;
                byte[] key;
                byte[] knowText;
                int len;

                if(args.length > 2)
                    {
                        len = Integer.parseInt(args[3]);
                    }
                else
                {
                    len = (int) (Math.random() * (100000 - 1000)) + 1000;
                    len = len - (len%8);
                }


                Text = TrabUtils.createRandomArrayBytes(len);

                            //extraindo somente 5 bytes de informação
                palavra = TrabUtils.extractKnowText(Text, 8);

                key = TrabUtils.sortKey().getBytes();

                ciphertext = TrabUtils.encrypt(key,Text);
                TrabUtils.saveFile(args[1], ciphertext);
            }
        
       

			Keys = TrabUtils.readDictionary("dictionary.txt");
						
			tempo1 = System.nanoTime()/1_000_000_000;
			guesses = attack(Keys, ciphertext,palavra);
			tempo2 = System.nanoTime()/1_000_000_000 - tempo1;
                        
			
			if(guesses.size() > 0) {
				System.out.println("Foram encontradas " + guesses.size() + " possíveis chaves.");
				
			}else {
				System.out.println("Sem palavras chave candidatas!");
			}
			
			System.out.println("Tempo total " +  tempo2  + " s");
			
		
	}
	
        
        
        
					
	
    
    }

