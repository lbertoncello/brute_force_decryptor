package br.inf.ufes.ppd;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;



public class TesteOverhead implements Slave{
	static UUID id;
        static Slave objref;
	static String name;
        static Master master;
        static List<String> dic;
        static TesteOverhead obj;
        static Boolean findServer;
	static	String pathDictionary;
        
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
        
        
	public static void main(String[] args) throws RemoteException, NotBoundException {
		
		/***
		 * Argumentos que devem ser fornecidos
		 * args[0] - Endereço IP de onde o Registry está 	
		 */
		
                	
		boolean run = true;
		int tryConnect = 0;
		Registry registry;
				
		id = java.util.UUID.randomUUID();
                name = "Escravo " + id;
		String host = "localhost";
                byte[] ciphertext = null;
            try {
                ciphertext = TrabUtils.readFile("teste.txt.cipher");
            } catch (IOException ex) {
                Logger.getLogger(TesteOverhead.class.getName()).log(Level.SEVERE, null, ex);
            }
		
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
		

		System.out.println("Localizando Servidor Mestre..");
					
		registry = LocateRegistry.getRegistry(host);
		master = (Master) registry.lookup("Mestre");
						
		System.out.println("Registrando Interface no Mestre...");
            try {
                master.addSlave(objref, name,id);
            } catch (RemoteException ex) {
                Logger.getLogger(TesteOverhead.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            double t1 = System.nanoTime()/1_000_000_000;
            objref.startSubAttack(ciphertext,"ipsum".getBytes(), 0,10,0, master);
            double t2  = System.nanoTime()/1_000_000_000 - t1;
            
            System.out.println("overhead: "+t2);
            
            
                
    }
		
			
	
	
	
}
