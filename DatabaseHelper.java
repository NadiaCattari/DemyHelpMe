package com.example.nadia.myproject.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.nadia.myproject.model.Credenziale;
import com.example.nadia.myproject.model.TableRecord;


public class DatabaseHelper extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "applicationDatabase";

    // Nomi delle tabelle
    private static final String TABLE_LOGIN = "Login";
    private static final String TABLE_FERMO_SEDUTO = "Fermo_Seduto";
    private static final String TABLE_FERMO_SDRAIATO = "Fermo_Sdraiato";
    private static final String TABLE_IN_MOVIMENTO = "In_Movimento";

    // Colonne a comune a tutte le tabelle
    private static final String ID_CHIAVE = "id";
    private static final String COD_FISCALE = "codice_fiscale";

    // Colonne della tabella Login
    private static final String PASSWORD = "password";

    // Colonne a comune tra Fermo_Seduto, Fermo_Sdraiato e In_Movimento
    private static final String LETTERA = "lettera";
    private static final String DATA = "data";
    private static final String DIRECTORY = "directory";

    // Dichiarazioni per la creazione delle tabelle
    //Tabella Login
    private static final String CREA_TABELLA_LOGIN = "CREATE TABLE " + TABLE_LOGIN
            + "(" + ID_CHIAVE + " INTEGER PRIMARY KEY," + COD_FISCALE + " TEXT NOT NULL,"
            + PASSWORD + " TEXT NOT NULL" + ")";

    //Tabella Fermo_Seduto
    private static final String CREA_TABELLA_FERMO_SEDUTO = "CREATE TABLE " + TABLE_FERMO_SEDUTO
            + "(" + ID_CHIAVE + " INTEGER PRIMARY_KEY," + COD_FISCALE + " TEXT NOT NULL,"
            + LETTERA + " TEXT NOT NULL," + DATA + " TEXT NOT NULL," + DIRECTORY + " TEXT NOT NULL" + ")";

    // Tabella Fermo_Sdraiato
    private static final String CREA_TABELLA_FERMO_SDRAIATO = "CREATE TABLE " + TABLE_FERMO_SDRAIATO
            + "(" + ID_CHIAVE + " INTEGER PRIMARY KEY," + COD_FISCALE + " TEXT NOT NULL,"
            + LETTERA + " TEXT NOT NULL," + DATA + " TEXT NOT NULL," + DIRECTORY + " TEXT NOT NULL" + ")";

    // Tabella In_Movimento
    private static final String CREA_TABELLA_IN_MOVIMENTO = "CREATE TABLE " + TABLE_IN_MOVIMENTO
            + "(" + ID_CHIAVE + " INTEGER PRIMARY KEY," + COD_FISCALE + " TEXT NOT NULL,"
            + LETTERA + " TEXT NOT NULL," + DATA + " TEXT NOT NULL," + DIRECTORY + " TEXT NOT NULL" + ")";

    public DatabaseHelper(Context applicationContext) {
        super(applicationContext,DATABASE_NAME,null,DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){

        //Creo le tabelle richieste
        db.execSQL(CREA_TABELLA_LOGIN);
        db.execSQL(CREA_TABELLA_FERMO_SEDUTO);
        db.execSQL(CREA_TABELLA_FERMO_SDRAIATO);
        db.execSQL(CREA_TABELLA_IN_MOVIMENTO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

        // Se si fa l'upDate del database le vecchie cartelle vengono eliminate e si richiama il metodo onCreate
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGIN);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FERMO_SEDUTO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FERMO_SDRAIATO);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IN_MOVIMENTO);

        onCreate(db);

    }

    // Definisco il metodo per l'inserimento di un record nella tabella Login
    public void addCredenziale(Credenziale credenziale){

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues valori = new ContentValues();
        valori.put(COD_FISCALE, credenziale.getCodFiscale());
        valori.put(PASSWORD, credenziale.getPassword());

        //Possiamo ora inserire la riga
        db.insert(TABLE_LOGIN,null,valori);
        db.close(); // chiudo la comunicazione con il database

    }

    // Definisco il metodo utilizzato per vedere se le credenziali di accesso sono presenti nel database
    public boolean Esiste (String cod_fiscale,String password){

        SQLiteDatabase db = this.getReadableDatabase();

        String [] colonne = {COD_FISCALE,PASSWORD};
        String selezione = COD_FISCALE + " =? AND " + PASSWORD + " =?";
        String [] argomenti = {cod_fiscale,password};
        String limite = "1";

        Cursor cursore = db.query(TABLE_LOGIN,colonne,selezione,argomenti,null,null,null,limite);
        boolean esiste = (cursore.getCount() > 0);
        cursore.close();
        db.close();
        return esiste;

    }

    //Definisco il metodo per l'inserimento di un record nella tabella Fermo_Seduto
    public void addFermoSeduto (TableRecord tableRecord){

        SQLiteDatabase db;
        db = this.getWritableDatabase();
        db = this.getReadableDatabase();

        ContentValues valori = new ContentValues();
        valori.put(COD_FISCALE,tableRecord.getCod_fiscale());
        valori.put(LETTERA,tableRecord.getVocale());
        valori.put(DATA,tableRecord.getData());
        valori.put(DIRECTORY,tableRecord.getPercorso());

        String[] colonne = {COD_FISCALE,LETTERA,DATA};
        String selezione = COD_FISCALE + " =? AND " + LETTERA + " =? AND " + DATA + " =?";
        String [] argomenti = {tableRecord.getCod_fiscale(),tableRecord.getVocale(),tableRecord.getData()};
        String limite = "1";

        Cursor cursore = db.query(TABLE_FERMO_SEDUTO,colonne,selezione,argomenti,null,null,null,limite);
        boolean presente = (cursore.getCount() > 0);
        cursore.close();

        if(!presente){
            db.insert(TABLE_FERMO_SEDUTO,null,valori);
            db.close();
        } else {
            Log.e("Inserimento database","Record già esistente: inserimento fallito.");
            db.close();
        }

    }

    //Definisco il metodo per l'inserimento di un record nella tabella Fermo_Sdraiato
    public void addFermoSdraiato (TableRecord tableRecord){

        SQLiteDatabase db;
        db = this.getWritableDatabase();
        db = this.getReadableDatabase();

        ContentValues valori = new ContentValues();
        valori.put(COD_FISCALE,tableRecord.getCod_fiscale());
        valori.put(LETTERA,tableRecord.getVocale());
        valori.put(DATA,tableRecord.getData());
        valori.put(DIRECTORY,tableRecord.getPercorso());

        String[] colonne = {COD_FISCALE,LETTERA,DATA};
        String selezione = COD_FISCALE + " =? AND " + LETTERA + " =? AND " + DATA + " =?";
        String [] argomenti = {tableRecord.getCod_fiscale(),tableRecord.getVocale(),tableRecord.getData()};
        String limite = "1";

        Cursor cursore = db.query(TABLE_FERMO_SDRAIATO,colonne,selezione,argomenti,null,null,null,limite);
        boolean presente = (cursore.getCount() > 0);
        cursore.close();

        if(!presente){
            db.insert(TABLE_FERMO_SDRAIATO,null,valori);
            db.close();
        } else {
            Log.e("Inserimento database","Record già esistente: inserimento fallito.");
            db.close();
        }

    }

    //Definisco il metodo per l'inserimento di un recod nella tabella In_Movimento
    public void addInMovimento (TableRecord tableRecord){

        SQLiteDatabase db;// = this.getWritableDatabase();
        db = this.getWritableDatabase();
        db = this.getReadableDatabase();

        ContentValues valori = new ContentValues();
        valori.put(COD_FISCALE,tableRecord.getCod_fiscale());
        valori.put(LETTERA,tableRecord.getVocale());
        valori.put(DATA,tableRecord.getData());
        valori.put(DIRECTORY,tableRecord.getPercorso());

        String[] colonne = {COD_FISCALE,LETTERA,DATA};
        String selezione = COD_FISCALE + " =? AND " + LETTERA + " =? AND " + DATA + " =?";
        String [] argomenti = {tableRecord.getCod_fiscale(),tableRecord.getVocale(),tableRecord.getData()};
        String limite = "1";

        Cursor cursore = db.query(TABLE_IN_MOVIMENTO,colonne,selezione,argomenti,null,null,null,limite);
        boolean presente = (cursore.getCount() > 0);
        cursore.close();

        if(!presente){
            db.insert(TABLE_IN_MOVIMENTO,null,valori);
            db.close();
        } else {
            Log.e("Inserimento database","Record già esistente: inserimento fallito.");
            db.close();
        }

    }

}
