package vn.encode.vp9.ambientlight;

import android.content.Intent;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.util.Timer;
import java.util.TimerTask;

public class ScreenBrightnessChange extends AppCompatActivity {
    Timer timer;
    int ambientValue = 0;
    int delayTime=4000;
    Button auto,next;
    boolean isAuto=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_screen_brightness_change);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();

        auto= (Button) findViewById(R.id.auto);

        auto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAuto = true;
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        IncreaseBrighness();
                    }
                },0,delayTime);
            }
        });

        next = (Button) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAuto)
                {
                    timer.cancel();
                    timer.purge();
                    isAuto = false;
                }
                IncreaseBrighness();

            }
        });
        delayTime = intent.getIntExtra("RYU",4000);


        Log.e("Ryu", "create second page"+ String.valueOf(delayTime));
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        timer.cancel();
    }

    void IncreaseBrighness()
    {
        if(ambientValue>252){
            ambientValue= 0;
        }
        Settings.System.putInt(getApplicationContext().getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,ambientValue);
        Log.e("ryu", "ambientValue: "+ ambientValue);

        ambientValue+=28;

    }
}
