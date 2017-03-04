package com.example.nadia.myproject.model;

/**
 * Created by nadia on 06/02/2017.
 */

public class TableRecord {

    int id;
    String cod_fiscale;
    String vocale;
    String data;
    String percorso;

    // Costruttori
    public TableRecord(){

    }

    public TableRecord(String codiceFiscale, String lettera, String data, String directory ){
        this.cod_fiscale = codiceFiscale;
        this.vocale = lettera;
        this.data = data;
        this.percorso = directory;
    }

    public TableRecord(int id, String codiceFiscale, String lettera, String data, String directory){
        this.id = id;
        this.cod_fiscale = codiceFiscale;
        this.vocale = lettera;
        this.data = data;
        this.percorso = directory;
    }

    //Setters
    public void setId(int id) {this.id = id;}

    public void setCod_fiscale(String codiceFiscale) {this.cod_fiscale = codiceFiscale;}

    public void setVocale(String lettera) {this.vocale = lettera;}

    public void setData(String data) {this.data = data;}

    public void setPercorso(String directory) {this.percorso = directory;}

    //Getters
    public long getId() {return this.id;}

    public String getCod_fiscale() {return this.cod_fiscale;}

    public String getVocale() {return this.vocale;}

    public String getData() {return this.data;}

    public String getPercorso() {return this.percorso;}

}
