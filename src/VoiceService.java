package com.bananaia.voz;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class VoiceService extends Service implements RecognitionListener {
    static volatile boolean running = false;
    SpeechRecognizer sr;
    final Handler h = new Handler(Looper.getMainLooper());
    boolean alive = false;
    AudioManager am;

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    @Override
    public int onStartCommand(Intent i, int f, int id) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel("voz", "Bananaia Voz", NotificationManager.IMPORTANCE_LOW));
        Notification n = new Notification.Builder(this, "voz")
            .setContentTitle("Bananaia Voz")
            .setContentText("Escutando o microfone")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(1, n);
        running = true;
        alive = true;
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mute(true);
        h.post(this::startListening);
        return START_STICKY;
    }

    void mute(boolean m) {
        try {
            int d = m ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE;
            am.adjustStreamVolume(AudioManager.STREAM_SYSTEM, d, 0);
            am.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, d, 0);
        } catch (Throwable ignored) {}
    }

    void startListening() {
        if (!alive) return;
        if (sr != null) sr.destroy();
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        sr.setRecognitionListener(this);
        Intent in = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        in.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        in.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        in.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        in.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800L);
        sr.startListening(in);
    }

    void restart(long delay) {
        if (alive) h.postDelayed(this::startListening, delay);
    }

    void write(String text) {
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "bananaia");
            dir.mkdirs();
            File tmp = new File(dir, "voz.tmp");
            FileOutputStream o = new FileOutputStream(tmp);
            o.write(text.getBytes("UTF-8"));
            o.close();
            tmp.renameTo(new File(dir, "voz.txt"));
        } catch (Throwable ignored) {}
    }

    @Override
    public void onResults(Bundle b) {
        ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (r != null && !r.isEmpty() && !r.get(0).trim().isEmpty()) write(r.get(0).trim());
        restart(150);
    }

    @Override
    public void onError(int e) {
        restart(e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ? 150 : 700);
    }

    @Override
    public void onDestroy() {
        alive = false;
        running = false;
        h.removeCallbacksAndMessages(null);
        if (sr != null) sr.destroy();
        mute(false);
        super.onDestroy();
    }

    @Override public void onReadyForSpeech(Bundle p) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float v) {}
    @Override public void onBufferReceived(byte[] b) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onPartialResults(Bundle b) {}
    @Override public void onEvent(int t, Bundle b) {}
}
