package com.bananaia.voz;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    TextView status;
    Button btn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setPadding(48, 48, 48, 48);
        status = new TextView(this);
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        btn = new Button(this);
        btn.setTextSize(20);
        l.addView(status);
        l.addView(btn);
        setContentView(l);
        btn.setOnClickListener(v -> toggle());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    void refresh() {
        btn.setText(VoiceService.running ? "PARAR" : "INICIAR");
        status.setText(VoiceService.running
            ? "Escutando... fale com a banana no jogo.\nTexto vai para /sdcard/bananaia/voz.txt"
            : "Toque em INICIAR e volte para o Minecraft.");
    }

    boolean hasStorage() {
        return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
    }

    void toggle() {
        if (VoiceService.running) {
            stopService(new Intent(this, VoiceService.class));
            VoiceService.running = false;
            refresh();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        if (!hasStorage()) {
            status.setText("Permita 'Acesso a todos os arquivos' para o app e volte.");
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + getPackageName())));
            return;
        }
        Intent i = new Intent(this, VoiceService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        VoiceService.running = true;
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] r) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) toggle();
        else status.setText("Sem permissao de microfone.");
    }
}
