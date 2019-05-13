/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.inf.ufes.ppd;

import java.util.UUID;

/**
 *
 * @author natanael
 */
public class SlaveInfo {
    private long tempo;
    private UUID id;
    private int inicio_Index;
    private int final_Index;
    private int corrente_Index;
    private String nome;
    private Slave slaveReference;
    private boolean terminou;

    public boolean isTerminou() {
        return terminou;
    }

    public void setTerminou(boolean terminou) {
        this.terminou = terminou;
    }

    public SlaveInfo(UUID id, String nome, Slave slaveReference) {
        this.id = id;
        this.nome = nome;
        this.slaveReference = slaveReference;
        this.inicio_Index = 0;
        this.final_Index = 0;
        this.corrente_Index = 0;
        this.terminou = false;
        
    }
    
    

    public long getTempo() {
        return tempo;
    }

    public void setTempo(long tempo) {
        this.tempo = tempo;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getInicio_Index() {
        return inicio_Index;
    }

    public void setInicio_Index(int inicio_Index) {
        this.inicio_Index = inicio_Index;
    }

    public int getFinal_Index() {
        return final_Index;
    }

    public void setFinal_Index(int final_Index) {
        this.final_Index = final_Index;
    }

    public int getCorrente_Index() {
        return corrente_Index;
    }

    public void setCorrente_Index(int corrente_Index) {
        this.corrente_Index = corrente_Index;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
    
    
    
}
