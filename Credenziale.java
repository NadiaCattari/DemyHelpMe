package com.example.nadia.myproject.model;

/**
 * Created by nadia on 04/02/2017.
 */

public class Credenziale {

    int id;
    String cod_fiscale;
    String password;

    // Costruttori
    public Credenziale(){

    }

    public Credenziale(String cod_fiscale, String password) {
        this.cod_fiscale = cod_fiscale;
        this.password = password;
    }

    public Credenziale(int id, String cod_fiscale, String password){
        this.id = id;
        this.cod_fiscale = cod_fiscale;
        this.password = password;
    }

    //Setters
    public void setId(int id) {this.id = id;}

    public void setCodFiscale(String cod_fiscale) {this.cod_fiscale = cod_fiscale; }

    public void setPassword(String password) {this.password = password; }

    //Getters
    public long getId() { return this.id; }

    public String getCodFiscale() { return this.cod_fiscale; }

    public String getPassword() { return this.password; }


}
