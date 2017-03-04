package com.example.nadia.myproject;

/**
 * Created by nadia on 10/02/2017.
 */
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import com.example.nadia.myproject.helper.DatabaseHelper;
import com.example.nadia.myproject.model.TableRecord;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Date;
import java.io.File;
import java.lang.Math;

public class Registrazione2 extends AppCompatActivity{

    Button buttonStart;
    TextView pos,voc;

    MediaRecorder mediaRecorder;
    MediaExtractor estrattore;
    MediaCodec codec;
    ByteBuffer[] codecInputBuffers;
    ByteBuffer[] codecOutputBuffers;

    private static final int RICHIESTA_PERMESSO = 1;

    String nomeFile = null;
    String percorsoSD = null;

    File directory;
    File fileAudio;

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(getApplicationContext());

        final String vocale = getIntent().getStringExtra("vocaleSelezionata");
        final String posizione = getIntent().getStringExtra("posizioneSelezionata");
        final String id_utente = getIntent().getStringExtra("id_utente");
        final String[] data = new String[1];


        AlertDialog.Builder tempo = new AlertDialog.Builder(Registrazione2.this);
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


        buttonStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if(controllaPermesso()) {

                    nomeFile = "Reg_" + id_utente + "_" + posizione + "_" + vocale + ".3gpp";
                    data[0] = new SimpleDateFormat("yyyy_MM_dd").format(new Date());
                    percorsoSD = String.valueOf(Environment.getExternalStorageDirectory());
                    if(Build.DEVICE.contains("Samsung") || Build.MANUFACTURER.contains("Samsung")){
                        percorsoSD = percorsoSD + "/external_sd/";
                    }

                    directory = new File(percorsoSD + "/Registrazioni");
                    if(!directory.exists())
                        directory.mkdirs();

                    fileAudio = new File(directory + "/" + nomeFile);

                    mediaRecorderPronto();

                    mediaRecorder.start();
                    buttonStart.setEnabled(false);

                    //directory = getFilesDir().getAbsolutePath() + nomeFile;

                    final TextView counter;
                    counter = (TextView) findViewById(R.id.counter);
                    new CountDownTimer(25000, 1000) {

                        public void onTick(long secUntilFinish) {
                            counter.setText("Secondi rimanenti: " + secUntilFinish / 1000);
                        }

                        public void onFinish() {
                            counter.setText("Registrazione conclusa!");
                            buttonStart.setEnabled(true);
                            mediaRecorder.stop();
                            mediaRecorder.release();

                            //File file;

                            try {
                                File file = new File(directory,nomeFile);
                                FileInputStream input = new FileInputStream(file);// = new File(fileAudio.getAbsolutePath()));
                                byte [] audioByte = new byte[(int) file.length()];
                                input.read(audioByte,0,audioByte.length);

                                Log.i("audioByte: ", bytesToHex(audioByte));

                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("audio/*");
                                intent.setPackage("com.android.bluetooth");
                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(String.valueOf(input))));
                                startActivity(Intent.createChooser(intent,"Invia il file audio"));


                                short[] audioShort = new short[audioByte.length / 2];
                                ByteBuffer.wrap(audioByte).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioShort);
                                int lunghezzaShort = audioShort.length;

                                Log.i("lunghezza short","" + lunghezzaShort);
                                Log.i("lunghezza byte","" + audioByte.length);

                                short[] tempRumore = new short[(lunghezzaShort*5)/25];
                                short[] tempSegnale = new short[(lunghezzaShort*20)/25];

                                tempRumore = Arrays.copyOfRange(audioShort,0,(lunghezzaShort*5)/25 + 1);
                                tempSegnale = Arrays.copyOfRange(audioShort,(lunghezzaShort*5)/25 + 1,audioShort.length);

                                double potenzaRumore = calcoloPotenzaDB(tempRumore,tempRumore.length);
                                double potenzaSegRum = calcoloPotenzaDB(tempSegnale,tempSegnale.length);

                                double SNR = (potenzaSegRum - potenzaRumore)/potenzaRumore;
                                Log.i("SNR",""+SNR);
                                double SNR_dB = Math.log10(SNR)*10d;
                                Log.i("SNR in dB",""+SNR_dB);
                                
                            } catch(IOException e){
                                e.printStackTrace();
                            }

                            /*byte[] audioByte = new byte[4096]; // freq. di campionamento*lunghezza acquisizione * num. di byte per sample
                            try {
                                InputStream input = getContentResolver().openInputStream(Uri.fromFile(new File(directory)));
                                audioByte = new byte[input.available()];
                                audioByte = toByteArray(input);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            short[] audioShort = new short[audioByte.length / 2];
                            ByteBuffer.wrap(audioByte).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioShort);
                            int lunghezzaShort = audioShort.length;

                            Log.i("lunghezza short","" + lunghezzaShort);
                            Log.i("lunghezza byte","" + audioByte.length);

                            short[] tempRumore = new short[(lunghezzaShort*5)/25];
                            short[] tempSegnale = new short[(lunghezzaShort*20)/25];

                            tempRumore = Arrays.copyOfRange(audioShort,0,(lunghezzaShort*5)/25 + 1);
                            tempSegnale = Arrays.copyOfRange(audioShort,(lunghezzaShort*5)/25 + 1,audioShort.length);

                            double potenzaRumore = calcoloPotenzaDB(tempRumore,tempRumore.length);
                            double potenzaSegRum = calcoloPotenzaDB(tempSegnale,tempSegnale.length);

                            double SNR = (potenzaSegRum - potenzaRumore)/potenzaRumore;
                            Log.i("SNR",""+SNR);*/

                            TableRecord newRecord = new TableRecord(id_utente, vocale, data[0], fileAudio.getAbsolutePath());
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
                            Toast.makeText(Registrazione2.this, "VIA!", Toast.LENGTH_SHORT).show();
                        }
                    }.start();


                } else {
                    richiestaPermesso();
                }
            }
        });
    }

    private void richiestaPermesso(){
        ActivityCompat.requestPermissions(Registrazione2.this,new String[]{WRITE_EXTERNAL_STORAGE,RECORD_AUDIO,READ_EXTERNAL_STORAGE},RICHIESTA_PERMESSO);
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
                        Toast.makeText(Registrazione2.this,"Permessi accordati",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Registrazione2.this,"Permessi negati",Toast.LENGTH_SHORT).show();
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

    /*public boolean controllaPermesso() {

        int risultato = ContextCompat.checkSelfPermission(getApplicationContext(),RECORD_AUDIO);
        return risultato == PackageManager.PERMISSION_GRANTED;

    }

    private void richiestaPermesso() {

        ActivityCompat.requestPermissions(Registrazione2.this,new String[]{RECORD_AUDIO},RICHIESTA_PERMESSO);

    }

    @Override
    public void onRequestPermissionsResult(int codiceRichiesta, String permesso[],int[] risultatoGrant) {
        super.onRequestPermissionsResult(codiceRichiesta,permesso,risultatoGrant);

        switch (codiceRichiesta) {
            case RICHIESTA_PERMESSO:
                boolean permessoRecord = risultatoGrant[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }*/

    public void mediaRecorderPronto() {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(fileAudio.getAbsolutePath());
        Log.i("Percorso salvataggio",fileAudio.getAbsolutePath());


        try{
            mediaRecorder.prepare();
        }catch(IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] toByteArray(InputStream in) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read = 0;
        byte[] buffer = new byte[0];
        while (read != -1) {
            read = in.read(buffer);
            if (read != -1){
                out.write(buffer,0,read);
            }
        }
        out.close();
        Log.i("lunghezza bufferByte","" + out.size() );
        return out.toByteArray();

    }


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

   /* public byte[] decoder (){

        byte decodedBuffer[];

        estrattore = new MediaExtractor();
        try {
            estrattore.setDataSource(fileAudio.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String mime = "audio/3gpp";
        MediaFormat format = new MediaFormat();
        format = MediaFormat.createAudioFormat(mime,format.getInteger(MediaFormat.KEY_SAMPLE_RATE),format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));

        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        codec.configure(format,null,null,0);
        codec.start();

        MediaCodec.BufferInfo buf_info = new MediaCodec.BufferInfo();
        int outputBufferIndex = codec.dequeueOutputBuffer(buf_info,0);
        byte[] pcm = new byte[buf_info.size];
        //decodedBuffer[outputBufferIndex].get(pcm, 0, buf_info.size);

    }
*/



    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }



}
