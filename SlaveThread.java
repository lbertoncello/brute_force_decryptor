/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package br.inf.ufes.ppd;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lucas
 */
public class SlaveThread implements Runnable {
    private Slave slave;
    private SlaveInfo si;
    private byte[] ciphertext;
    private byte[] knowntext;
    private long initialwordindex;
    private long finalwordindex;
    private int attackNumber;
    private SlaveManager callbackinterface;

    public SlaveThread(Slave slave, SlaveInfo si,byte[] ciphertext, byte[] knowntext, long initialwordindex, long finalwordindex, int attackNumber, SlaveManager callbackinterface) {
        this.slave = slave;
        this.si = si;
        this.ciphertext = ciphertext;
        this.knowntext = knowntext;
        this.initialwordindex = initialwordindex;
        this.finalwordindex = finalwordindex;
        this.attackNumber = attackNumber;
        this.callbackinterface = callbackinterface;
    }
    
    @Override
    public void run() {
        try {
            slave.startSubAttack(ciphertext, knowntext, initialwordindex, finalwordindex, attackNumber, callbackinterface);
            si.setTerminou(true);
        } catch (RemoteException ex) {
            Logger.getLogger(SlaveThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
