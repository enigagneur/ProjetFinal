package com.example.projetfinal;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.health.UidHealthStats;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ActionBarActivity extends AppCompatActivity implements View.OnClickListener {

    List<String> Names = new ArrayList<String>(); // Liste des noms des processus executés sur le téléphone
    List<Integer> UIDS = new ArrayList<Integer>();
    List<String> RSSs = new ArrayList<String>();
    int nb_proc_running = 0;
    private int buttonId; // ID du bouton qui nous aidera à mettre à jour le RSS qui correspond au bouton sur lequel l'utilisateur a cliqué
    final int MSG_CALCUL = 1;
    List<Boolean> clicks = new ArrayList<Boolean>(); // Liste de booléen nous informant sur l'état du bouton , pour la mise à jour périodique du RSS.
    Boolean pause = false; // Lorsque l'utilisateur utilise l'application la valeur de pause est false , lorsqu'on quitte notre appli elle se met à true avec la méthode onPause

    // Thread
    Runnable r = new Runnable(){
        public void run() {
                if (!pause) {
                    getInformation();
                    String messageString = "Utilisation de mémoire mise-à-jour" + buttonId;
                    Message msg = mHandler.obtainMessage(MSG_CALCUL, (Object) messageString);
                    mHandler.sendMessage(msg);
                    mHandler.postDelayed(r, 5000);
                }
        }
    };

    // Handler
    final Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            if (msg.what==MSG_CALCUL & clicks.get(buttonId)) {
                Toast.makeText(getBaseContext(), "Info:" + (String) msg.obj, Toast.LENGTH_LONG).show();
                TextView tx = (TextView) findViewById(buttonId + 200);
                tx.setText("RSS:" + RSSs.get(buttonId));
            }
            else if (!clicks.get(buttonId)){
                mHandler.removeCallbacks(r);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_bar);
        getInformation();
        LinearLayout ll = (LinearLayout) findViewById(R.id.linearlayout);
        for (int i = 0; i < nb_proc_running; ++i) {
            View v = createProcessView(UIDS.get(i), Names.get(i), RSSs.get(i), i);
            clicks.add(false);
            ll.addView(v);
        }
    }
        // Cette méthode réalise le design de notre deuxième activité elle crée nos vues dans notre layout
        @SuppressLint("ResourceType")
        public View createProcessView (int uid, String napp, String vmon, int id) {
            RelativeLayout layout = new RelativeLayout(this);
            RelativeLayout.LayoutParams paramsTopLeft =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);

            paramsTopLeft.addRule(RelativeLayout.ALIGN_PARENT_LEFT,
                    RelativeLayout.TRUE);
            paramsTopLeft.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                    RelativeLayout.TRUE);
            RelativeLayout.LayoutParams paramsTopRight =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
            paramsTopRight.addRule(RelativeLayout.ALIGN_PARENT_RIGHT,
                    RelativeLayout.TRUE);
            paramsTopRight.addRule(RelativeLayout.ALIGN_PARENT_TOP,
                    RelativeLayout.TRUE);
            RelativeLayout.LayoutParams paramsbelow =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);



            TextView someTextView = new TextView(this); // 1er TextView
            TextView textView = new TextView(this); //2eme TextView
            textView.setId(id + 200);
            Button btn = new Button(this); //2eme bouton
            btn.setText("Monitor");
            btn.setId(id);
            someTextView.setText("[" + uid + "]" + napp);
            someTextView.setId(999);
            paramsbelow.addRule(RelativeLayout.BELOW, 999);
            textView.setText("RSS:" + vmon);


            layout.addView(someTextView, paramsTopLeft);
            layout.addView(btn, paramsTopRight);
            btn.setOnClickListener(this);
            layout.addView(textView, paramsbelow);

            return layout;
        }
        // getInformation permet de mettre à jour les informations de tous les processus sur notre téléphones ( leurs noms , UIDS ainsi que leurs RSSs)
        public void getInformation () {
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            final List pkgAppsList = getPackageManager().queryIntentActivities(mainIntent, 0);
            //Nom des applications installées
            int UID = 0;
            String strPackageName = null;
            for (Object object : pkgAppsList) {
                ResolveInfo info = (ResolveInfo) object;
                strPackageName = info.activityInfo.applicationInfo.packageName.toString();
                UID = info.activityInfo.applicationInfo.uid;
                //Affichage dans les logs
                Log.d("App Name", strPackageName);
                Log.d("UID", String.valueOf(UID));
                Process process = null;
                try {
                    process = new ProcessBuilder("ps").start();
                } catch (IOException e) {
                    return;
                }
                InputStream in = process.getInputStream();
                Scanner scanner = new Scanner(in);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("u0_")) {
                        String[] temp = line.split(" ");
                        String packageName = temp[temp.length - 1];
                        if (packageName.equals(strPackageName)) {
                            //memoire qu’occupe le processus
                            String RSS = temp[4];
                            Log.d("RSS?", RSS);
                            RSSs.add(RSS);
                            UIDS.add(UID);
                            Names.add(packageName);
                            nb_proc_running++;
                        }
                    }
                }
            }
        }

     // Méthode executé lorsqu'on clique sur un des boutons Monitor de notre seconde activité
    @Override
    public void onClick(View v) {
        buttonId = v.getId();
        clicks.set(buttonId,!clicks.get(buttonId));
        Log.d("buttonId", String.valueOf(buttonId));
        new Thread(r).start();
    }
    // onPause est executé lorsqu'on quitte notre application
    @Override
    protected void onPause() {
        super.onPause();
        pause = true;
    }
    // onResume est executé lorsqu'on revient à notre application
    @Override
    protected void onResume() {
        super.onResume();
        getInformation();
    }
}

