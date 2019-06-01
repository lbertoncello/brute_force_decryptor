/*
 * Armazena as informações sobre o subataque de um escravo.
 */
package br.inf.ufes.ppd.utils;

import br.inf.ufes.ppd.Slave;
import java.util.UUID;

/**
 *
 * @author natanael
 */
public class SlaveInfo {

    private double time;
    private UUID id;
    private int initialIndex;
    private int finalIndex;
    private int currentIndex;
    private String nome;
    private Slave slaveReference;
    private boolean ended;

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public SlaveInfo(UUID id, String nome, Slave slaveReference) {
        this.id = id;
        this.nome = nome;
        this.slaveReference = slaveReference;
        this.initialIndex = 0;
        this.finalIndex = 0;
        this.currentIndex = 0;
        this.ended = false;

    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getInitialIndex() {
        return initialIndex;
    }

    public void setInitialIndex(int inicio_Index) {
        this.initialIndex = inicio_Index;
    }

    public int getFinalIndex() {
        return finalIndex;
    }

    public void setFinalIndex(int final_Index) {
        this.finalIndex = final_Index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int corrente_Index) {
        this.currentIndex = corrente_Index;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

}
