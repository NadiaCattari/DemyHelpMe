package com.example.nadia.myproject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import com.example.nadia.myproject.helper.DatabaseHelper;
import com.example.nadia.myproject.model.Credenziale;


public class  MainActivity extends Activity {

    //Database Helper
    DatabaseHelper db;
    int contatore = 3;
    String utente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(getApplicationContext());

        // Inserisco il mio codice fiscale e la mia password per il login.
        Credenziale miaCredenziale = new Credenziale("CTTNDA92R67I452J", "ADMIN");
        Credenziale prova = new Credenziale("cacca", "cacca");
        db.addCredenziale(miaCredenziale);
        db.addCredenziale(prova);

        setContentView(R.layout.pagina_iniziale);
        final Button login = (Button) findViewById(R.id.bottoneLogin);
        final EditText cf = (EditText) findViewById(R.id.codice_fiscale);
        final EditText psw = (EditText) findViewById(R.id.password);
        final TextView tentativi_rimasti = (TextView) findViewById(R.id.num_tentativi);
        tentativi_rimasti.setVisibility(View.GONE);

        login.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                String ID_utente = cf.getText().toString();
                String PSW_utente = psw.getText().toString();
                boolean esiste = db.Esiste(ID_utente,PSW_utente);
                utente = ID_utente;

                if (esiste) {
                    clickSuLogin();

                } else {

                    Toast.makeText(getApplicationContext(),"Credenziali errate",Toast.LENGTH_SHORT).show();
                    tentativi_rimasti.setVisibility(View.VISIBLE);
                    contatore--;
                    tentativi_rimasti.setText(Integer.toString(contatore));
                    if (contatore == 0){

                        login.setEnabled(false);

                        AlertDialog.Builder chiusura = new AlertDialog.Builder(MainActivity.this);
                        chiusura.setTitle("Accesso negato!");
                        chiusura.setMessage("Avete sbagliato per 3 volte consecutive. L'applicazione verrà chiusa.");
                        chiusura.setCancelable(false);
                        chiusura.setPositiveButton("Chiudi", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();

                            }
                        });
                        chiusura.show();


                    }

                }

            }

        });

    }


    private void clickSuLogin() {

        setContentView(R.layout.registrazione);

        final Spinner sceltaPosizione = (Spinner) findViewById(R.id.spinnerPosizione);
        ArrayAdapter<CharSequence> adapterPosizione = ArrayAdapter.createFromResource(this,R.array.sceltaPosizione,android.R.layout.simple_spinner_item);
        adapterPosizione.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sceltaPosizione.setAdapter(adapterPosizione);

        final Spinner sceltaVocale = (Spinner) findViewById(R.id.spinnerVocale);
        ArrayAdapter<CharSequence> adapterVocale = ArrayAdapter.createFromResource(this,R.array.sceltaVocale,android.R.layout.simple_spinner_item);
        adapterVocale.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sceltaVocale.setAdapter(adapterVocale);

        final Button avviaReg = (Button) findViewById(R.id.avviaReg);
        avviaReg.setOnClickListener (new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (sceltaVocale.getSelectedItemPosition() == 0 || sceltaPosizione.getSelectedItemPosition() == 0) {

                    AlertDialog.Builder attenzione = new AlertDialog.Builder(MainActivity.this);
                    attenzione.setTitle("Attenzione!");
                    attenzione.setMessage("Assicurarsi che entrambe le scelte siano state effettuate in maniera corretta.");
                    attenzione.setCancelable(false);
                    attenzione.setPositiveButton("Chiudi", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                        }
                    });
                    attenzione.show();


                } else {

                    String vocale = sceltaVocale.getSelectedItem().toString();
                    String posizione = sceltaPosizione.getSelectedItem().toString();
                    //String utente = ID_utente;
                    //Log.i("Prima intent", "" + utente);


                    Intent i = new Intent(MainActivity.this, Registrazione3.class);
                    i.putExtra("vocaleSelezionata", vocale);
                    i.putExtra("posizioneSelezionata",posizione);
                    i.putExtra("id_utente",utente);
                    startActivity(i);
                }
            }
        });

    }

}
