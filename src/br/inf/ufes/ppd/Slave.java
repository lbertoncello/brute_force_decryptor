package br.inf.ufes.ppd;

/**
 * Slave.java
 */
import java.rmi.Remote;

public interface Slave extends Remote {

    /**
     * Solicita a um escravo que inicie sua parte do ataque.
     *
     * @param ciphertext mensagem critografada
     * @param knowntext trecho conhecido da mensagem decriptografada
     * @param initialwordindex índice inicial do trecho do dicionário a ser
     * considerado no sub-ataque.
     * @param finalwordindex índice final do trecho do dicionário a ser
     * considerado no sub-ataque.
     * @param attackNumber chave que identifica o ataque
     * @param callbackinterface interface do mestre para chamada de checkpoint e
     * foundGuess
     * @throws java.rmi.RemoteException
     */
    public void startSubAttack(
            byte[] ciphertext,
            byte[] knowntext,
            long initialwordindex,
            long finalwordindex,
            int attackNumber,
            SlaveManager callbackinterface)
            throws java.rmi.RemoteException;

}
