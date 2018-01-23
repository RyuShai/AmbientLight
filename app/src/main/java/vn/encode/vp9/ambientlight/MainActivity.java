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
    Button btn;
    SensorManager sManger;
    Sensor sLight;
    List<Float> value;
    Timer stableTimer;
    boolean stop=false;
    float currentValue,lastValue=-1;
    EditText editText;
    int iTimer=0;
    ArrayAdapter<Float> adapter;
    Button clear,pause;
    FileOutputStream outputStream;
    ArrayList<ArrayList<Float>> totalList;
    int listSize=20;
    int cycle=0;
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
        totalList = new ArrayList<>();
        for(int i=0; i<listSize;i++)
        {
            totalList.add(new ArrayList<Float>());
        }
    }


    List<Float> RemoveSameValue(List<Float> list)
    {
        float current=-1;
        List<Float> result = new ArrayList<Float>();
        Collections.sort(list);
        while (list.size()>0)
        {
//            Log.e ("ryu","sort: "+ list.get(0));
            current = list.get(0);
            result.add(current);
            list.remove(0);
            List<Float> countList = list;
            while(list.remove(current))
            {

            }
        }
        for(int i=0; i<result.size();i++)
        {
            Log.e("ryu", "resutl: "+ result.get(i));
        }
        for(int i=0; i<result.size()-1;i++)
        {
            float sub = result.get(i+1)-result.get(i);
            float ratio = (float) Math.ceil(result.get(i)*0.2);
            Log.e("ryu", "sub: "+ sub +" ratio: "+ ratio);
            if(sub<=ratio)
            {
                result.remove(i+1);
                i=0;
            }
        }
        if(result.size()>10)
        {
             float chechLech=result.get(result.size()-1);
             float subValue;
             List<Float> pos = new ArrayList<>();
            for( int i=0; i>result.size()-1;i++)
            {
                pos.add(result.get(i+1)-result.get(i));
            }
            float min=500;
            int position=0;
            while (result.size()>10)
            {
                for(int i=0; i<result.size();i++)
                {
                    if(pos.get(i)<min)
                    {
                        min= pos.get(i);
                        position = i;
                    }
                }
                result.remove(result.get(position));
                pos.remove(result.get(position));
            }
        }
        return result;
    }

    Float TotalListValue(List<Float> list)
    {
        float result=0;
        if(list.size()==0)
            return  result;
        for(int i=0;i<list.size();i++)
        {
            result+=list.get(i);
        }
        return (result/list.size());
    }

    float GetMostCommonValue(ArrayList<Float> list)
    {
        float result=-1,current=-1;
        int i=0,count=0,maxcount=0;
        Collections.sort(list);
        while (list.size()>0)
        {
//           Log.e ("ryu","sort: "+ list.get(0));
           current = list.get(0);

           list.remove(0);
           List<Float> countList = list;
           while(list.remove(current))
           {
               count++;
               if(count>maxcount)
               {
                   maxcount=count;
                   result = current;
               }
           }
           count =0;
        }
        if(result==-1)
            return current;
        else
            return result;
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
        if(stop)
            return;
        currentValue = event.values[0];
        if(lastValue == -1)
        {
            lastValue = currentValue;
            totalList.get(cycle).add(currentValue);
        }
        else
        {
            if(ItinRange())
            {
                totalList.get(cycle).add(currentValue);
            }
            else
            {
                cycle++;
                if(cycle>=listSize)
                {
                    Log.e("ryu", "cycle > 9");
                    cycle=1;
                    stop = true;
                    pause.setText("paused");
                    for(int i =0 ;i<totalList.size();i++)
                    {
//                        Log.e("ryu", "list i: "+i);
                        value.add(GetMostCommonValue(totalList.get(i)));
                    }
                    value = RemoveSameValue(value);
                    Log.e("ryu","value size: " + value.size());
                    for(int i =0 ;i<value.size();i++)
                    {
                        Log.e("ryu", "value: "+ value.get(i));
                    }
                }
                totalList.get(cycle).add(currentValue);
            }
        }
        lastValue = currentValue;
        text.setText("Lux: "+String.valueOf(currentValue));

    }
    boolean ItinRange()
    {
        if(totalList.get(cycle).size()<=0)
            return false;
        float subtain = Math.abs(TotalListValue(totalList.get(cycle))-currentValue);
        if(subtain>(lastValue*0.2))
        {
            return false;
        }
        else
            return true;
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
