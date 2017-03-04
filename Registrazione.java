package com.example.nadia.myproject;

/**
 * Created by nadia on 30/01/2017.
 */
import android.Manifest;
import static android.Manifest.permission.RECORD_AUDIO;
//import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.content.Intent;
import android.util.Log;
import android.app.AlertDialog;
import android.content.Context;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaRecorder;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.provider.Settings;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Date;
import java.io.IOException;
import java.text.SimpleDateFormat;


import com.example.nadia.myproject.helper.DatabaseHelper;
import com.example.nadia.myproject.model.TableRecord;

public class Registrazione extends AppCompatActivity {

    Button buttonStart;
    File SalvaPercorsoInMemoria;
    File fileAudio;
    TextView pos;
    TextView voc;
    MediaRecorder mediaRecorder;
    private static final int RICHIESTA_PERMESSO = 1;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    short[] tempRumore;
    short[] tempSegnale;


    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(getApplicationContext());
        Log.i("bufferSize","" + bufferSize);


        final String vocale = getIntent().getStringExtra("vocaleSelezionata");
        final String posizione = getIntent().getStringExtra("posizioneSelezionata");
        final String id_utente = getIntent().getStringExtra("id_utente");
        final String[] data = new String[1];
        //Log.i("Utente",id_utente);
        //Log.i("Posizione",posizione);

        AlertDialog.Builder tempo = new AlertDialog.Builder(Registrazione.this);
        tempo.setTitle("Attenzione!");
        tempo.setMessage("La registrazione ha una durata di 25 secondi così distribuiti: per i primi 5 secondi è necessario mantenere il silenzio; la registrazione vera e propria della lettera sarà negli ultimi 20 secondi.");
        tempo.setCancelable(false);
        tempo.setPositiveButton("Avanti", new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog1,int which){
                dialog1.dismiss();
            }
        });
        tempo.show();

        setContentView(R.layout.reg_mic);
        buttonStart = (Button) findViewById(R.id.start);
        pos = (TextView) findViewById(R.id.posizione);
        voc = (TextView) findViewById(R.id.vocale);
        pos.setText(posizione);
        voc.setText(vocale);

        buttonStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {


                if(controllaPermesso()) {

                    final String nomeFile = "Reg_" + id_utente + "_" + posizione + "_" + vocale + ".pcm";
                    data[0] = new SimpleDateFormat("yyyy_MM_dd").format(new Date()).toString();

                    File percorso = getFilesDir();
                    SalvaPercorsoInMemoria = new File(percorso.getAbsolutePath() + "/.Registrazioni");
                    if (!SalvaPercorsoInMemoria.exists())
                        SalvaPercorsoInMemoria.mkdir();

                    fileAudio = new File(SalvaPercorsoInMemoria + "/" + nomeFile);

                    final String directory = fileAudio.getAbsolutePath().toString();
                    Log.i("Percorso", directory);

                    startRecording();

                    final TextView counter;
                    counter = (TextView) findViewById(R.id.counter);

                    new CountDownTimer(10000,1000){
                        public void onTick(long secUntilFinish){
                            counter.setText("Secondi rimanenti: " + secUntilFinish / 1000);
                        }

                        public void onFinish() {
                            counter.setText("Registrazione conclusa!");
                            buttonStart.setEnabled(true);
                            stopRecording();

                            long lunghezzaTotale = fileAudio.length() / 2;
                            byte[] tempByte = new byte[(int) lunghezzaTotale];
                            try {
                                FileInputStream is = openFileInput(nomeFile);
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                //tempByte = new byte[(int) lunghezzaTotale];
                                while (is.available() > 0) {
                                    bos.write(is.read());
                                }
                                tempByte = bos.toByteArray();

                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }

                            long lunghezzaByte = tempByte.length;
                            Log.i("lunghezza tempByte", ""+lunghezzaByte);



                            Log.i("lunghezza file audio", "" + lunghezzaTotale);
                            //short[] tempRumore = new short[(int) ((lunghezzaTotale*5)/25)];
                            //short[] tempSegnale = new short[(int) ((lunghezzaTotale*20)/25)];
                            tempRumore = Arrays.copyOfRange(tempRumore,0,(int) ((lunghezzaTotale*5)/25 + 1));
                            tempSegnale = Arrays.copyOfRange(tempSegnale,(int) ((lunghezzaTotale*5)/25 + 1), (int) lunghezzaTotale);

                            double potenzaRumore = calcoloPotenzaDB(tempRumore, tempRumore.length);
                            double potenzaSegRum = calcoloPotenzaDB(tempSegnale, tempSegnale.length);

                            double SNR = (potenzaSegRum - potenzaRumore)/potenzaRumore;
                            Log.i("SNR",""+SNR);
                            double SNR_dB = Math.log10(SNR)*10;
                            Log.i("SNR in dB",""+SNR_dB);







                            TableRecord newRecord = new TableRecord(id_utente, vocale, data[0], directory);
                            switch (posizione) {
                                case "Fermo_e_seduto":
                                    db.addFermoSeduto(newRecord);
                                    break;
                                case "Fermo_e_sdraiato":
                                    db.addFermoSdraiato(newRecord);
                                    break;
                                case "In_movimento_(dx/sx)":
                                    db.addInMovimento(newRecord);
                                    break;
                            }
                        }
                    }.start();

                    new CountDownTimer(5000,1000){

                        public void onTick(long secUntilFinish){}

                        public void onFinish(){
                            Toast.makeText(Registrazione.this,"VIA!",Toast.LENGTH_SHORT).show();
                        }
                    }.start();

                    buttonStart.setEnabled(false);

                } else {
                    richiestaPermesso();

                }


            }

        });



    }


    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,44100,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bufferSize*10);//freq campionamento*secondi)*bytePerShort

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                scriviAudioSuFile();
            }
        }, "AudioRecord Thread");
        recordingThread.start();
    }

    private byte[] short2byte(short[] audioData) {

        int shortArraySize = audioData.length;
        byte[] bytes = new byte[shortArraySize * 2];
        for (int i = 0; i < shortArraySize; i++){
            bytes[i*2] = (byte) (audioData[i] & 0x00FF);
            bytes[(i*2)+1] = (byte) (audioData[i] >> 8);
            audioData[i] = 0;
        }

        return bytes;
    }

    private void scriviAudioSuFile(){
        //Scrivo l'output in short sulla memoria interna.
        short[] audioData = new short[bufferSize];



        FileOutputStream oS = null;

        try {
            oS = new FileOutputStream(fileAudio.getAbsolutePath());
        } catch(IOException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            //int numberOfShort = recorder.read(audioData, 0, bufferSize);
            //Log.i("Lunghezza", "" + numberOfShort);
            recorder.read(audioData, 0, bufferSize);
            //tempRumore = audioData;
            //tempSegnale = audioData;

            try{
                byte audioDataByte[] = short2byte(audioData);
                oS.write(audioDataByte,0,(bufferSize*2));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        try{
            oS.close();
        } catch(IOException e){
            e.printStackTrace();
        }
        //Log.i("lunghezza array", "" + tempSegnale.length);
    }


    private void stopRecording(){
        if(null != recorder){
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }

    }

    private void richiestaPermesso() {

        ActivityCompat.requestPermissions(Registrazione.this, new String[]{RECORD_AUDIO}, RICHIESTA_PERMESSO);

    }

    @Override
    public void onRequestPermissionsResult(int codiceRichiesta, String permesso[], int[] risultatoGrant) {
        super.onRequestPermissionsResult(codiceRichiesta,permesso,risultatoGrant);

        switch (codiceRichiesta) {
            case RICHIESTA_PERMESSO:
                boolean permessoRecord = risultatoGrant[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

    }

    public boolean controllaPermesso() {

        int risultato = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);

        return risultato == PackageManager.PERMISSION_GRANTED;
    }

    public final static double calcoloPotenzaDB(short[] dati, int campioni){

        double sum = 0;
        double sqsum = 0;
        for (int i = 0; i < campioni; i++){
            final long v = dati[i];
            sum += v;
            sqsum += v * v;
        }

        double potenza = (sqsum - sum * sum / campioni) / campioni;
        return potenza;
        //return Math.log10(potenza) * 10d;

    }








}
