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
import android.media.Image;
import android.media.MediaPlayer;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    SeekBar seek;
    int ambientValue = 0;
    TextView text, screenBrightness;
    ListView listView;
    Button btn, hide;
    SensorManager sManger;
    Sensor sLight;
    List<Float> value;
    Timer stableTimer;
    boolean stop=false;
    float currentValue,lastValue=-1;
    EditText editText;
    ArrayList<String> arrayList;
    ArrayAdapter<String> adapter;
    Button clear,pause;
    FileOutputStream outputStream;
    MediaPlayer mdp;


    List<Float> listValue;
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
        pause.setText("running");
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = !stop;
                if(stop)
                    pause.setText("stop");
                else
                    pause.setText("running");
                listValue.clear();
                arrayList.clear();
                lastValue = currentValue = 0;
            }
        });
        editText = (EditText) findViewById(R.id.editText);
        listValue = new ArrayList<>();
        arrayList = new ArrayList<>();
        listView = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,arrayList);
        listView.setAdapter(adapter);
        clear = (Button) findViewById(R.id.clear);
        clear.setEnabled(false);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WriteValueToFile();
                arrayList.clear();
                updateListview();
                clear.setEnabled(false);
            }
        });
        stableTimer = new Timer();
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop = true;
                Intent brightnessView = new Intent(MainActivity.this,ScreenBrightnessChange.class);
                brightnessView.putExtra("RYU",Integer.parseInt(editText.getText().toString()));
                startActivity(brightnessView);
            }
        });

        value = new ArrayList<Float>();
        text = (TextView) findViewById(R.id.textView);
        GetSensor();


        mdp = MediaPlayer.create(this,R.raw.beep);

    }

    List<Float> Sub5Value(List<Float> list)
    {
        Collections.sort(list);
        List<Float> resutl = new ArrayList<>();
        int stable=0;
        int iAddPoint=-1;
        for(int i=0; i<list.size()-1;i++)
        {
            if(list.get(i+1)-list.get(i)<5)
            {
                if(iAddPoint==-1)
                    iAddPoint =i;

                    stable++;
            }
            else
            {
                if(iAddPoint!= -1 && stable>8)
                {
                    resutl.add(list.get(iAddPoint));
                    iAddPoint=-1;
                    stable=0;
                }
            }
        }
        if(iAddPoint!= -1 && stable>8)
        {
            resutl.add(list.get(iAddPoint));
        }
        return  resutl;
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
        Log.e("ryu","data chaged");
        for(int i =0; i<arrayList.size();i++)
        {
            Log.e("ryu", "arrayList: "+ listValue.get(i));
        }
        adapter.notifyDataSetChanged();
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(stop)
            return;
        currentValue = event.values[0];
        listValue.add(currentValue);

        if(listValue.size()>=200)
        {
            Print(listValue);
            OnStopChanged();
//            GetMostCommonValue((ArrayList<Float>) listValue);
            listValue =(ArrayList<Float>) Sub5Value(listValue);
            arrayList.clear();
            Collections.sort(listValue);
            for(int i =0; i<listValue.size();i++)
            {
                Log.e("ryu", "arrayList: "+ listValue.get(i));
                arrayList.add(String.valueOf(listValue.get(i)));
            }
            updateListview();
            clear.setEnabled(true);
            mdp.start();
            return;
        }

        lastValue = currentValue;
        text.setText("Lux: "+String.valueOf(currentValue));

    }

    void WriteValueToFile()
    {
        String directory = Environment.getExternalStorageDirectory()+"/AmbientLight/";

        String filename = String.valueOf(System.currentTimeMillis())+".txt";
        Log.e("RYu",directory+filename);
        try {
            File f = new File(directory);
            if(!f.exists()|| !f.isDirectory())
            {
                f.mkdir();
            }
            filename = directory+filename;
            File yourFile = new File(filename);
            yourFile.createNewFile();
            outputStream = new FileOutputStream(yourFile,false);
            for(int i=0; i<listValue.size();i++)
            {
                outputStream.write(Float.toString(listValue.get(i)).getBytes());
                outputStream.write(" ".getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    void OnStopChanged()
    {
        stop = !stop;
        if(stop)
        {
            pause.setText("stop");
        }
        else
        {
            pause.setText("running");
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        sManger.registerListener(this,sLight,SensorManager.SENSOR_DELAY_UI,50);
    }

    void Print(List<Float> list)
    {

        for(int i =0; i<list.size();i++)
        {
            Log.e("ryu", "origin: "+ list.get(i));
        }
        Collections.sort(list);
        for(int i =0; i<list.size();i++)
        {
            Log.e("ryu", "sorted: "+ list.get(i));
        }
    }

}
