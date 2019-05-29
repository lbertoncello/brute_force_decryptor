package br.inf.ufes.ppd;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.UUID;



public class TesteOverhead implements Slave{
	static UUID id;
        static Slave objref;
	static String name;
        
        /***
	 * Procura pelo mestre usando o host fornecido e o ID de identificação de registro junto ao Registry.
	 * @throws RemoteException 
	 * @throws NotBoundException 
	 */
	static void searchMaster() throws RemoteException, NotBoundException{
		
	}
	

	@Override
	public void startSubAttack(byte[] ciphertext, byte[] knowntext, long initialwordindex, long finalwordindex,
			int attackNumber, SlaveManager callbackinterface) throws RemoteException {
		
		callbackinterface.checkpoint(id, attackNumber, finalwordindex);
		
	}
        
        
	public static void main(String[] args) {
		
		/***
		 * Argumentos que devem ser fornecidos
		 * args[0] - Endereço IP de onde o Registry está 	- Obrigatório
		 * args[1] - Local do dicionário de chaves			- Obrigatório
		 * args[2] - Nome utilizado pelo escravo			- Opcional
		 */
		
                Master master = null;
                objref = null;
                List<String> dic;
                TesteOverhead obj;
                Boolean findServer;
		String pathDictionary;	
		boolean run = true;
		int tryConnect = 0;
		Registry registry;
				
		id = java.util.UUID.randomUUID();
                name = "Escravo " + id;
		String host = args[0];
		
                dic = TrabUtils.readDictionary("dictionary.txt");
				
		System.out.println("Escravo: " + name + ", UUID: " + id.toString());
		
		try {
			
			obj = new TesteOverhead();
			objref = (Slave) UnicastRemoteObject.exportObject(obj, 0);
			System.out.println("Serviço remoto disponibilizado..");
		} catch (RemoteException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		findServer = false;
		/*
		while(run && (tryConnect < 10)) {		
			try {
				if(!findServer) {
					System.out.println("Localizando Servidor Mestre..");
					
					registry = LocateRegistry.getRegistry(host);
					master = (Master) registry.lookup("mestre");
						
					System.out.println("Registrando Interface no Mestre...");
					master.addSlave(objref, name,id);

					findServer = true;
				}else {
					new Thread() {
						public void run() {
							try {
								master.addSlave(objref, name, id);
								System.err.println("Re-registro feito com sucesso!");
							} catch (RemoteException e) {
								synchronized (findServer) {
									findServer = false;
								}
							}
						}
					}.start();
				}
				
			}catch (NullPointerException eNull) {
				System.out.println("Referência nula para o mestre. Finalizando " + name);
				run = false;
			}catch (RemoteException eRemote) {
				System.out.print("Servidor mestre Offline. ");
				System.out.println("Tentativa de Conexão (" + (++tryConnect) + " / " + 10 + ")");
				synchronized (findServer) {
					findServer = false;
				}
			} catch (NotBoundException eNotBound) {
				System.out.println("O serviço  mestre não está disponível pelo servidor mestre.");
				System.out.println("Tentativa de Conexão (" + (++tryConnect) + " / " + 10+ ")");
				synchronized (findServer) {
					findServer = true;
				}
			} 
			
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				run = false;
			}
		}
		*/
	
		if(master != null) {
			try {
				System.out.println("Solicitando remoção ao Mestre.");
				master.removeSlave(id);
				System.out.println("Removido do servidor com sucesso.");
				run = false;
			} catch (RemoteException e1) {
				System.out.println("Falha na Conexão com o Mestre.");
			}
		}

		System.out.println("Execução de " + name + " encerrada.");
			
	}
	
	
}
