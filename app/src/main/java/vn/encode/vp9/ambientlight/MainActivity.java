package vn.encode.vp9.ambientlight;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    SeekBar seek;
    int ambientValue = 0;
    TextView text, screenBrightness;
    ListView listView;
    Button btn;
    SensorManager sManger;
    Sensor sLight;
    List<Float> value;
    Timer stableTimer;
    boolean stop=false;
    float currentValue,lastValue=0;
    EditText editText;
    int iTimer=0;
    ArrayAdapter<Float> adapter;
    Button clear,pause;
    FileOutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        CheckPermission();
        screenBrightness = (TextView) findViewById(R.id.textView);
        screenBrightness.setTypeface(null, Typeface.BOLD);
        btn = (Button) findViewById(R.id.button);

        pause = (Button) findViewById(R.id.pause);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = !stop;
                if(stop)
                    pause.setText("true");
                else
                    pause.setText("false");
            }
        });
        editText = (EditText) findViewById(R.id.editText);
        listView = (ListView) findViewById(R.id.listView);
        clear = (Button) findViewById(R.id.clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WriteValueToFile();
                value.clear();
                updateListview();
            }
        });
        stableTimer = new Timer();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent brightnessView = new Intent(MainActivity.this,ScreenBrightnessChange.class);
                brightnessView.putExtra("RYU",Integer.parseInt(editText.getText().toString()));
                startActivity(brightnessView);
            }
        });

        value = new ArrayList<Float>();
        text = (TextView) findViewById(R.id.textView);
        updateListview();
        GetSensor();
    }

    void CheckPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));

                startActivityForResult(intent, 200);

            }
        }
    }

    void GetSensor()
    {
        sManger = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sLight = sManger.getDefaultSensor(Sensor.TYPE_LIGHT);

    }

    void updateListview()
    {
        if(adapter == null)
        {
            adapter = new ArrayAdapter<Float>(this,android.R.layout.simple_list_item_1,value);
            listView.setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentValue = event.values[0];

//        Log.e("ryu","event: "+ currentValue);
//        Log.e("RYU", "current: "+currentValue + " "+lastValue+" "+stop);
        if((currentValue == lastValue) || stop)
        {
//            Log.e("RYu ","return ");
            return;
        }

        stableTimer.cancel();
        stableTimer.purge();


        stableTimer = new Timer();
        stableTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                iTimer += 100;
                if(iTimer>2000 && currentValue!= lastValue && !stop)
                {
                    value.add(currentValue);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateListview();
                        }
                    });
                    lastValue =currentValue;
                    iTimer=0;
                    stableTimer.purge();
                }
            }
        }, 100, 100);
        text.setText("Lux: "+String.valueOf(currentValue));

    }

    void WriteValueToFile()
    {
        String filename = Environment.getExternalStorageDirectory()+"/"+String.valueOf(System.currentTimeMillis())+".txt";
        Log.e("RYu",filename);
        try {
            File yourFile = new File(filename);
            yourFile.createNewFile();
            outputStream = new FileOutputStream(yourFile,false);
            for(int i=0; i<value.size();i++)
            {
                outputStream.write(Float.toString(value.get(i)).getBytes());
                outputStream.write(" ".getBytes());
                if(value.get(i)>200 && value.get(i+1)<10)
                {
                    outputStream.write("\n".getBytes());
                }
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.e("ryu","accuraty: "+ accuracy);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sManger.registerListener(this,sLight,SensorManager.SENSOR_DELAY_UI,1000);
    }
}
