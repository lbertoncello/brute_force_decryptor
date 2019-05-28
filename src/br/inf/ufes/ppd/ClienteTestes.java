package br.inf.ufes.ppd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;


public class ClienteTestes {
	public static void main(String args[]) throws NotBoundException, IOException, NoSuchAlgorithmException, Exception {
		/**
		 * Argumentos que devem ser fornecidos
		 * args[0] - Endereço IP de onde o Registry está 
		 * args[1] - Path do arquivo para geração de texto
		 * args[2] - Palavra conhecida
                 * args[3] - quantidade de testes
                 * args[4] - tamanho arquivo(somente em casos que o arquivo para geração de texto não exista)
		 */
		
		String host, fileName, key;
                byte[] knowText;
                byte[] texto = null;
		int qtdTestes = 0,tamTexto;
                byte[] crypt;
		fileName = args[1];
		if(Files.exists(Paths.get(fileName)))  {
			host = args[0];
                        knowText = args[2].getBytes();
			qtdTestes = Integer.parseInt(args[3]);
                        texto = TrabUtils.readFile(fileName);
			
		}else {
			System.out.println("Parametros nao inseridos corretamente. Finalizando..");
                        
				int sizeFile = Integer.parseInt(args[4]);
				texto = TrabUtils.createRandomArrayBytes(sizeFile);
				
				knowText = TrabUtils.extractKnowText(texto, 8);
				//gerando uma chave para criptografia..
				key = TrabUtils.sortKey();
				
				texto = TrabUtils.encrypt(key.getBytes(), texto);

		}
				
		System.out.println("Serão criados [" + qtdTestes + "] casos de teste, com vetores de bytes");
		
		
		System.out.println("Criando casos de teste automaticos..");
		/*List<EDtestes> testList = new ArrayList<>();
		try {
			
			
			
		} catch (Exception e1) {
			System.out.println("Erro: " + e1.getMessage());
		}*/	
		
		
		System.out.println("\nTudo pronto, iniciando conexão com o servidor mestre..");
		
		try {
			Registry registry = LocateRegistry.getRegistry();
                        Master master = (Master) registry.lookup("Mestre");
			
			
                        System.out.println("Envio dos dados feito de forma sequencial..");
				
				 
                        for(int i = 0; i < qtdTestes;i++)
                                {
					System.out.print("Teste " + (i+1));
					long tempoInicio = System.nanoTime()/1_000_000_000;
					master.attack(texto,knowText);
					long tempoFinal = System.nanoTime()/1_000_000_000; 
					long diffTempo  = tempoFinal - tempoInicio; 
					System.out.println(" - " + diffTempo + " s");
					TrabUtils.Resultados("analise_cliente.csv", texto.length, diffTempo);
					
				}
												
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("\nTestes finalizados..");

			
	}
	

			
	static class EDtestes{
		private byte[] ciphertext;
		private byte[] knowntext;
		
		public EDtestes(byte[] ciphertext, byte[] knowntext) {
			this.ciphertext = ciphertext;
			this.knowntext = knowntext;
		}
		
		public byte[] getCipherText() {
			return this.ciphertext;
		}
		
		public byte[] getKnowText() {
			return this.knowntext;
		}
		
		
	}
}
