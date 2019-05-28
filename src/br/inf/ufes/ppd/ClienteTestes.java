package br.inf.ufes.ppd;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ClienteTestes {
	public static void main(String args[]) throws NotBoundException, IOException {
		/**
		 * Argumentos que devem ser fornecidos
		 * args[0] - Endereço IP de onde o Registry está 				- Obrigatório
		 * args[1] - Path do arquivo para geração de texto				- Obrigatório
		 * args[2] - (s - Testes sequenciais | p - Testes em paralelo)  - Obrigatório
		 * args[3] - Quantidade de testes								- Obrigatório
		 * args[4] - Quantidade minima de texto (sorteio)				- Obrigatório
		 * args[5] - Quantidade maxima de texto (sorteio)				- Obrigatório 
		 */
		
		String host, tipoProcessamento, dictionary, key;
		int qtdTestes,tamTexto;

		
		//if(args.length >= 4) {
                if(true){
			/*host = args[0];
			tipoProcessamento = args[1];
			qtdTestes = Integer.parseInt(args[2]);
			tamTexto = Integer.parseInt(args[3]);*/
                        host = "localhost";
			tipoProcessamento = "s";
			qtdTestes = 5;
			tamTexto = 400;
		}else {
			System.out.println("Parametros nao inseridos corretamente. Finalizando..");
			return;
		}
				
		System.out.println("Serão criados [" + qtdTestes + "] casos de teste, com vetores de bytes");
		System.out.println("Os testes Serão enviados ao servidor mestre de forma [" + (tipoProcessamento.equals("s") ? "Sequencial" : "Paralela") + "]\n");
		
		
		System.out.println("Criando casos de teste automaticos..");
		List<EDtestes> testList = new ArrayList<>();
		try {
			
			for(int i = 0; i < qtdTestes; i++) {
				
				byte[] texto = TrabUtils.createRandomArrayBytes(tamTexto);
				
				byte[] know = TrabUtils.extractKnowText(texto, 8);
				//gerando uma chave para criptografia..
				key = TrabUtils.sortKey();
				
				byte[] crypt = TrabUtils.encrypt(key.getBytes(), texto);
                                
                                //TrabUtils.saveFile("test.txt.cipher",crypt);
				
				testList.add(new EDtestes(crypt, know));
				System.out.println("Caso de teste " + (i + 1) + " - OK");
				
			}
			
		} catch (Exception e1) {
			System.out.println("Erro: " + e1.getMessage());
		}	
		
		
		System.out.println("\nTudo pronto, iniciando conexão com o servidor mestre..");
		
		try {
			Registry registry = LocateRegistry.getRegistry();
                        Master master = (Master) registry.lookup("Mestre");
			
			if(tipoProcessamento.equals("s")) {
				System.out.println("Envio dos dados feito de forma sequencial..");
				
				 
                                for(int i = 0; i < testList.size();i++)
                                {
					System.out.print("Teste " + i+1);
					long tempoInicio = System.nanoTime()/1_000_000_000;
					master.attack(testList.get(i).getCipherText(), testList.get(i).getKnowText());
					long tempoFinal = System.nanoTime()/1_000_000_000; 
					long diffTempo  = tempoFinal - tempoInicio; 
					System.out.println(" - " + diffTempo + " s");
					TrabUtils.Resultados("analise_cliente.csv", testList.get(i).getCipherText().length, diffTempo, "sequencial");
					
				}
												
			}else {
				System.out.println("Envio dos dados feito de forma paralela..");
				ExecutorService executor = Executors.newFixedThreadPool(qtdTestes);
				
				List<Future<StatisticsMeasure>> statistics = new ArrayList<>();
				for(Future<StatisticsMeasure> fst : statistics) {
					//bloqueia e espera
					StatisticsMeasure st;
					try {
						st = fst.get();
						TrabUtils.Resultados("analise_cliente.csv", st.getQtdBytes(), (long) st.getTime(), "paralelo");
						System.out.println("Caso de teste " + st.getId() + " <stop>");
					} catch (InterruptedException | ExecutionException e) {
						System.out.println("Erro na Thread: " + e.getMessage());
					}
					
				}
				
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("\nTestes finalizados..");

			
	}
	
	static class ThreadMeasure implements Callable<StatisticsMeasure>{
		private Master master;
		EDtestes ed;
		private int id;
		public ThreadMeasure(Master master, EDtestes ed, int id) {
			this.master = master;
			this.ed = ed;
			this.id = id;
		}
		@Override
		public StatisticsMeasure call() throws Exception {
			System.out.println("Caso de teste " + id + " <start>");
			long initialTime = System.nanoTime()/1_000_000_000;
			master.attack(ed.getCipherText(), ed.getKnowText());
			long finalTime = System.nanoTime()/1_000_000_000; 
			long seg = finalTime - initialTime; 
			
			return new StatisticsMeasure(seg, ed.getCipherText().length, id);
		}
		
	}
	
	static class StatisticsMeasure{
		private double time;
		private int qtdBytes, id;
		
		public StatisticsMeasure(double time, int qtdBytes, int id) {
			this.time = time;
			this.qtdBytes = qtdBytes;
			this.id = id;
		}
		
		public double getTime() {
			return this.time;
		}
		
		public int getQtdBytes() {
			return this.qtdBytes;
		}
		
		public int getId() {
			return this.id;
		}
		
		
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
