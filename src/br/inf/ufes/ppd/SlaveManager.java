package br.inf.ufes.ppd;

/**
 * SlaveManager.java
 */
import java.rmi.Remote;

public interface SlaveManager extends Remote {

    /**
     * Registra escravo no mestre. Deve ser chamada a cada 30s por um escravo
     * para se re-registrar.
     *
     * Note que o escravo deve gerar uma chave ao se registrar pela primeira vez
     * apenas usando java.util.UUID.randomUUID()
     *
     * @param s referência para o escravo
     * @param slaveName nome descritivo para o escravo
     * @param slaveKey chave para o escravo
     * @throws java.rmi.RemoteException
     */
    public void addSlave(Slave s, String slaveName, java.util.UUID slavekey)
            throws java.rmi.RemoteException;

    /**
     * Desegistra escravo no mestre.
     *
     * @param slaveKey chave que identifica o escravo
     * @throws java.rmi.RemoteException
     */
    public void removeSlave(java.util.UUID slaveKey)
            throws java.rmi.RemoteException;

    /**
     * Indica para o mestre que o escravo achou uma chave candidata.
     *
     * @param slaveKey chave que identifica o escravo
     * @param attackNumber chave que identifica o ataque
     * @param currentindex índice da chave candidata no dicionário
     * @param currentguess chute que inclui chave candidata e mensagem
     * decriptografada com a chave candidata
     * @throws java.rmi.RemoteException
     */
    public void foundGuess(java.util.UUID slaveKey, int attackNumber, long currentindex,
            Guess currentguess)
            throws java.rmi.RemoteException;

    /**
     * Chamado por cada escravo a cada 10s durante ataque para indicar progresso
     * no ataque, e ao final do ataque.
     *
     * @param slaveKey chave que identifica o escravo
     * @param attackNumber chave que identifica o ataque
     * @param currentindex índice da chave já verificada
     * @throws java.rmi.RemoteException
     */
    public void checkpoint(java.util.UUID slaveKey, int attackNumber, long currentindex)
            throws java.rmi.RemoteException;
}
