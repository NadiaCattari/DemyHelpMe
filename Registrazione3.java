package com.example.nadia.myproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeUnit;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore.Files;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.example.nadia.myproject.helper.DatabaseHelper;
import com.example.nadia.myproject.model.TableRecord;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by nadia on 23/02/2017.
 */

public class Registrazione3 extends AppCompatActivity{

    Button buttonStart;
    File directory;
    File fileAudio;
    TextView pos,voc;

    String nomeFile;
    String percorsoSD;
    String data;

    BufferedOutputStream os;
    AudioRecord recorder;

    byte audioByte[];

    boolean keepRec;


    private static final int RICHIESTA_PERMESSO = 1;
    public static final int FREQ_CAMPIONAMENTO = 44100;
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int CONFIG_CANALE = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    //public static final int DIM_BUFFER = AudioRecord.getMinBufferSize(FREQ_CAMPIONAMENTO,CONFIG_CANALE,AUDIO_FORMAT);

    //AudioRecord recorder;// = new AudioRecord(AUDIO_SOURCE,FREQ_CAMPIONAMENTO,CONFIG_CANALE,AUDIO_FORMAT,DIM_BUFFER);
    private Thread recordingThread;

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(getApplicationContext());
        
        final String vocale = getIntent().getStringExtra("vocaleSelezionata");
        final String posizione = getIntent().getStringExtra("posizioneSelezionata");
        final String id_utente = getIntent().getStringExtra("id_utente");


        AlertDialog.Builder tempo = new AlertDialog.Builder(Registrazione3.this);
        tempo.setTitle("Attenzione");
        tempo.setMessage("La registrazione ha una durata di 25 secondi così distribuiti:\n- 5 secondi di silenzio; \n- 20 secondi in cui si ha l'effettiva registrazione della vocale.");
        tempo.setCancelable(false);
        tempo.setPositiveButton("Avanti", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog1, int which) {
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

        buttonStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (controllaPermesso()){
                    nomeFile = "Reg_" + id_utente + "_" + posizione + "_" + vocale + ".pcm";
                    DateFormat df = new SimpleDateFormat("yyyy_MM_dd");
                    data = df.format(Calendar.getInstance().getTime());
                    Log.i("Data:",""+data.toString());

                    percorsoSD = String.valueOf(Environment.getExternalStorageDirectory());
                    if(Build.DEVICE.contains("Samsung") || Build.MANUFACTURER.contains("Samsung")){
                        percorsoSD = percorsoSD + "/external_SD/";
                    }

                    directory = new File(percorsoSD + "/Registrazioni");
                    if(!directory.exists())
                        directory.mkdirs();

                    fileAudio = new File(directory + File.separator + nomeFile);
                    Log.i("Percorso", fileAudio.getAbsolutePath());


                    startRecording();

                    final TextView contatore;
                    contatore = (TextView) findViewById(R.id.counter);
                    new CountDownTimer(25000, 1000) {

                        public void onTick(long secUntilFinish) {
                            contatore.setText("Secondi rimanenti: " + secUntilFinish / 1000);
                        }

                        public void onFinish() {

                            stopRecording();

                            contatore.setText("Registrazione conclusa!");
                            buttonStart.setEnabled(true);



                            File file = new File(fileAudio.getAbsolutePath());
                            byte[] audioData = new byte[(int) file.length()];

                            try {
                                FileInputStream input = new FileInputStream(file);

                                while(input.available() > 0 ){
                                   input.read(audioData,0,audioData.length);
                                }

                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("audio/*");
                                intent.setPackage("com.android.bluetooth");
                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(String.valueOf(audioData))));
                                startActivity(Intent.createChooser(intent,"Invia il file audio"));

                                short[] audioShort = new short[audioData.length / 2];
                                ByteBuffer.wrap(audioData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioShort);
                                int lunghezzaShort = audioShort.length;

                                Log.i("lunghezza short","" + lunghezzaShort);
                                Log.i("lunghezza byte","" + audioData.length);

                                short[] tempRumore;// = new short[(lunghezzaShort*5)/25];
                                short[] tempSegnale;// = new short[(lunghezzaShort*20)/25];

                                tempRumore = Arrays.copyOfRange(audioShort,0,(lunghezzaShort*5)/25 + 1);
                                tempSegnale = Arrays.copyOfRange(audioShort,(lunghezzaShort*5)/25 + 1,audioShort.length);

                                double potenzaRumore = calcoloPotenzaDB(tempRumore,tempRumore.length);
                                double potenzaSegRum = calcoloPotenzaDB(tempSegnale,tempSegnale.length);

                                double SNR = (potenzaSegRum - potenzaRumore)/potenzaRumore;
                                Log.i("SNR",""+SNR);
                                double SNR_dB = Math.log10(SNR)*10d;
                                Log.i("SNR in dB",""+SNR_dB);

                            } catch(FileNotFoundException e){
                                e.printStackTrace();
                            } catch (IOException e){
                                e.printStackTrace();
                            }
                            /*try {
                                File file = new File(fileAudio.getAbsolutePath());
                                FileInputStream input = new FileInputStream(file);
                                byte [] audio = new byte[(int) file.length()];

                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("audio/*");
                                intent.setPackage("com.android.bluetooth");
                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(String.valueOf(input))));
                                startActivity(Intent.createChooser(intent,"Invia il file audio"));*/



                            /*} catch (IOException e) {
                                e.printStackTrace();
                            }*/



                            TableRecord newRecord = new TableRecord(id_utente, vocale, data, String.valueOf(fileAudio.getAbsolutePath()));
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

                    new CountDownTimer(5000, 1000) {

                        public void onTick(long secUntilFinish) {
                        }

                        public void onFinish() {
                            Toast.makeText(Registrazione3.this, "VIA!", Toast.LENGTH_SHORT).show();
                        }
                    }.start();





                } else {
                    richiestaPermesso();
                }
            }
        });


    }

    private void richiestaPermesso(){
        ActivityCompat.requestPermissions(Registrazione3.this,new String[]{WRITE_EXTERNAL_STORAGE,RECORD_AUDIO,READ_EXTERNAL_STORAGE},RICHIESTA_PERMESSO);
    }

    @Override
    public void onRequestPermissionsResult(int codiceRichiesta, String permessi[], int[] risultatiGrant){

        switch(codiceRichiesta){
            case RICHIESTA_PERMESSO:
                if (risultatiGrant.length > 0) {
                    boolean permessoStorage = risultatiGrant[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permessoRecord = risultatiGrant[1] == PackageManager.PERMISSION_GRANTED;
                    boolean permessoLettura = risultatiGrant[2] == PackageManager.PERMISSION_GRANTED;

                    if (permessoRecord && permessoStorage && permessoLettura){
                        Toast.makeText(Registrazione3.this,"Permessi accordati",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Registrazione3.this,"Permessi negati",Toast.LENGTH_SHORT).show();
                    }
                }
                break;

        }

    }

    public boolean controllaPermesso(){

        int risultatoStorage = ContextCompat.checkSelfPermission(getApplicationContext(),WRITE_EXTERNAL_STORAGE);
        int risultatoRecord = ContextCompat.checkSelfPermission(getApplicationContext(),RECORD_AUDIO);
        int risultatoLettura = ContextCompat.checkSelfPermission(getApplicationContext(),READ_EXTERNAL_STORAGE);

        return risultatoStorage == PackageManager.PERMISSION_GRANTED && risultatoRecord == PackageManager.PERMISSION_GRANTED && risultatoLettura == PackageManager.PERMISSION_GRANTED;
    }



    public void startRecording() {

        if(recordingThread != null)
            return;

        buttonStart.setEnabled(false);
        keepRec = true;
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        recordingThread.start();
    }

    public void stopRecording() {

        if(recordingThread == null)
            return;

        //buttonStart.setEnabled(true);
        keepRec = false;
        recorder.stop();
        recorder.release();
        recorder = null;
        recordingThread = null;

    }

    private void record() {

        //android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        int bufferSize = AudioRecord.getMinBufferSize(FREQ_CAMPIONAMENTO,CONFIG_CANALE,AUDIO_FORMAT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = FREQ_CAMPIONAMENTO * 2;
        }

        audioByte = new byte[bufferSize];

        recorder = new AudioRecord(AUDIO_SOURCE,FREQ_CAMPIONAMENTO,CONFIG_CANALE,AUDIO_FORMAT,bufferSize);
        if(recorder.getState() != AudioRecord.STATE_INITIALIZED){
            Log.e("Errore: ","AudioRecord non può inizializzare la risorsa");
            return;
        }
        recorder.startRecording();

        long byteLetti = 0;

        try {
            os = new BufferedOutputStream(new FileOutputStream(fileAudio.getAbsolutePath()));

            while (keepRec) {
                int numberOfByte = recorder.read(audioByte, 0, audioByte.length);
                byteLetti += numberOfByte;

                try {
                    os.write(audioByte,0,audioByte.length);
                } catch(IOException e){
                    Log.e("Errore: ", "errore nel savataggio del file!", e);
                    return;
                }

            }
        } catch(FileNotFoundException e){
            Log.e("Errore: ", "file non trovato per il recording!", e);
        }

        try {
            os.close();
        } catch(IOException e){
            Log.e("Errore: ", "Errore nel releasing del recorder", e);
        }
        /*recorder.stop();
        recorder.release();
        recordingThread = null;*/

        Log.v("Fine lettura ",String.format("Recording stoppato. Numero di byte letti: %d",byteLetti));


    }

    /*public void storeDatabase(){

        short[] audioShort = new short[audioByte.length / 2];
        ByteBuffer.wrap(audioByte).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioShort);
        int lunghezzaShort = audioShort.length;

        Log.i("lunghezza short","" + lunghezzaShort);
        Log.i("lunghezza byte","" + audioByte.length);

        short[] tempRumore;// = new short[(lunghezzaShort*5)/25];
        short[] tempSegnale;// = new short[(lunghezzaShort*20)/25];

        tempRumore = Arrays.copyOfRange(audioShort,0,(lunghezzaShort*5)/25 + 1);
        tempSegnale = Arrays.copyOfRange(audioShort,(lunghezzaShort*5)/25 + 1,audioShort.length);

        double potenzaRumore = calcoloPotenzaDB(tempRumore,tempRumore.length);
        double potenzaSegRum = calcoloPotenzaDB(tempSegnale,tempSegnale.length);

        double SNR = (potenzaSegRum - potenzaRumore)/potenzaRumore;
        Log.i("SNR",""+SNR);
        double SNR_dB = Math.log10(SNR)*10d;
        Log.i("SNR in dB",""+SNR_dB);

    }*/

    public final static double calcoloPotenzaDB(short[] dati, int campioni){

        double somma = 0;
        double sqsomma = 0;
        for (int i = 0; i < campioni; i++){

            final long v = dati[i];
            somma += v;
            sqsomma += v*v;

        }

        double potenza = (sqsomma - (somma*somma)/campioni)+((somma/campioni)*(somma/campioni));
        return  potenza;
        //return Math.log10(potenza) * 10f;


    }

    /*private void startRecording() {

        buttonStart.setEnabled(false);
        stopRec = false;

        final TextView contatore;
        contatore = (TextView) findViewById(R.id.counter);

        audioByte = new byte[DIM_BUFFER];
        final AudioRecord recorder = new AudioRecord(AUDIO_SOURCE,FREQ_CAMPIONAMENTO,CONFIG_CANALE,AUDIO_FORMAT,DIM_BUFFER);

        final BufferedOutputStream os;

        recorder.startRecording();

        try {
            os = new BufferedOutputStream(new FileOutputStream(fileAudio.getAbsolutePath()));

            while (!stopRec) {

                new CountDownTimer(25000,1000) {

                    public void onTick(long secUntilFinish){
                        contatore.setText("Secondi rimanenti: " + secUntilFinish/1000);

                        int status = recorder.read(audioByte,0,audioByte.length);

                        if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE){
                            Log.e("Errore: ", "Errore nella lettura del file audio!");
                            return;
                        }

                        try {
                            os.write(audioByte,0,audioByte.length);
                        } catch (IOException e){
                            Log.e("Errore: ", "Errore nel salvataggio del file", e);
                            return;
                        }

                    }

                    public void onFinish(){

                        //contatore.setText("Registrazione conclusa!");
                        //buttonStart.setEnabled(true);
                        stopRec = false;

                    }
                }.start();
            }

            try {
                os.close();

                recorder.stop();
                recorder.release();

                contatore.setText("Registrazione conclusa!");
                buttonStart.setEnabled(true);

            } catch (IOException e) {
                Log.e("Errore: ", "Errore nel releasing del recorder",e);
            }


        } catch (FileNotFoundException e){
            Log.e("Errore: ", "File non trovato per il recording", e);
        }
    }*/


    /*public void startRecording(){

        buttonStart.setEnabled(false);
        recorder = new AudioRecord(AUDIO_SOURCE,FREQ_CAMPIONAMENTO,CONFIG_CANALE,AUDIO_FORMAT,DIM_BUFFER);
        recorder.startRecording();
        stopRec = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                salvaAudioSuFile();
            }
        },"AudioRecord Thread");
        recordingThread.start();
    }

    private void salvaAudioSuFile() {

        audioByte = new byte[DIM_BUFFER];

        try {
            os = new BufferedOutputStream(new FileOutputStream(fileAudio.getAbsolutePath()));

            while (stopRec) {

                int status = recorder.read(audioByte, 0, audioByte.length);

                if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e("Errore: ", "errore nella lettura del file audio!");
                    return;
                }

                try {
                    os.write(audioByte, 0, audioByte.length);
                } catch (IOException e) {
                    Log.e("Errore: ", "errore nel savataggio del file!", e);
                    return;
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                Log.e("Errore: ", "Errore nel releasing del recorder", e);
            }
        } catch (FileNotFoundException e){
            Log.e("Errore: ", "file non trovato per il recording!", e);
        }
    }*/

    /*public void startRecording() {

        buttonStart.setEnabled(false);

        audioByte = new byte[DIM_BUFFER];
        recorder = new AudioRecord(AUDIO_SOURCE,FREQ_CAMPIONAMENTO,CONFIG_CANALE,AUDIO_FORMAT,DIM_BUFFER);
        recorder.startRecording();
        stopRec = true;

        //BufferedOutputStream os;
        try {
            os = new BufferedOutputStream(new FileOutputStream(fileAudio.getAbsolutePath()));

            while(stopRec){

                int status = recorder.read(audioByte,0,audioByte.length);

                if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE){
                    Log.e("Errore: ", "Errore nella lettura del file audio!");
                    return;
                }

                try {
                    os.write(audioByte,0,audioByte.length);
                } catch (IOException e){
                    Log.e("Errore: ", "Errore nel salvataggio del file", e);
                    return;
                }

            }

            try {

                os.close();

                recorder.stop();
                recorder.release();

            } catch (IOException e) {
                Log.e("Errore: ", "Errore nel releasing del recorder",e);
            }


        } catch (FileNotFoundException e){
            Log.e("Errore: ", "File non trovato per il recording", e);
        }



    }*/

    /*public void stopRecording() {
        if(null != recorder){
            stopRec = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }*/






}
