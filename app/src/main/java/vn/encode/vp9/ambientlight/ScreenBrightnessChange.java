package vn.encode.vp9.ambientlight;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenBrightnessChange extends AppCompatActivity implements
        MediaPlayer.OnPreparedListener,
        TextureView.SurfaceTextureListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener,
        MediaPlayer.OnBufferingUpdateListener{

    static {
        System.loadLibrary("native-lib");
        if(OpenCVLoader.initDebug())
        {
            Log.e("ryu", "ngon");
        }
        else
        {
            Log.e("ryu", "f***");
        }
    }

    Timer timer;
    int ambientValue = 0;
    int delayTime=1000;
    Button auto,next,hide;
    boolean isAuto=false;
    ImageView img;
    RadioButton oldStyle,newStyle;

    TextureView textureView;
    MediaPlayer mp;
    List<Mat> totalFrame;
    boolean isRun =true;
    int biChia = 1;
    List<Float> listData;
    List<Float> result;
    MediaPlayer playSound;
    int frameCount =0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_screen_brightness_change);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent intent = getIntent();
        delayTime = intent.getIntExtra("RYU",1000);

        auto= (Button) findViewById(R.id.auto);
        auto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isAuto)
                {
                    isAuto = true;
//                    img.setAlpha(0);
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            IncreaseBrighness();
                        }
                    },0,delayTime);
//                    Log.e("ryu", "isplayijg: "+ mp.isPlaying());
//                    if(!isRun && !mp.isPlaying())
//                    {
//                        Handler handler = new Handler();
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                mp.start();
//                                img.setAlpha(0);
//                            }
//                        }, 5000);
//                    }
                }
            }
        });
        img = (ImageView) findViewById(R.id.imageView);
        ChangeImageColor(0);

        hide = (Button) findViewById(R.id.hide);
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if(img.getAlpha()==(float) 0)
//                {
//                    img.setAlpha((float) 1.0);
//                }
//                else
//                {
//                    img.setAlpha((float)0);
//                }
                if(!isRun)
                {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mp.start();
//                            textureView.setAlpha(0);

                        }
                    }, 5000);
                }
            }
        });
        oldStyle = (RadioButton) findViewById(R.id.radioButton);
        oldStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newStyle.setChecked(false);
                ambientValue = 0;
            }
        });
        newStyle = (RadioButton) findViewById(R.id.radioButton2);
        newStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oldStyle.setChecked(false);
                ambientValue = 0;
                Intent intent = getIntent();
                delayTime = intent.getIntExtra("RYU",1000);
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
                    Log.e("ryu","cancel purge");

                }
//
//                IncreaseBrighness();
//                Mat matImg = new Mat(1080,1920, CvType.CV_8UC1,new Scalar(ambientValue));
//                Log.e("ryu", "mat size: "+matImg.rows() + " "+ matImg.cols());
//                Bitmap bmp = Bitmap.createBitmap(matImg.cols(), matImg.rows(), Bitmap.Config.RGB_565);
//                Utils.matToBitmap(matImg, bmp);
//                img.setImageBitmap(bmp);
                ChangeImageColor(ambientValue);
                ambientValue+=50;
                if(ambientValue>250)
                    ambientValue=0;

            }
        });

        textureView = (TextureView) findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(this);
        textureView.setAlpha(0);
        playSound = MediaPlayer.create(this,R.raw.beep);
        Log.e("Ryu", "create second page"+ String.valueOf(delayTime));

        result = new ArrayList<>();
        listData = new ArrayList<>();
        totalFrame = new ArrayList<>();

        stringFromJNI();
        Settings.System.putInt(getApplicationContext().getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,255);
    }

    void ChangeImageColor(final int value)
    {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Mat matImg = new Mat(3240,2100, CvType.CV_8UC1,new Scalar(value));
                Log.e("ryu", "mat size: "+matImg.rows() + " "+ matImg.cols());
                Bitmap bmp = Bitmap.createBitmap(matImg.cols(), matImg.rows(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(matImg, bmp);
                img.setImageBitmap(bmp);
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        timer.cancel();
    }
    long lastTime=0;
    void IncreaseBrighness()
    {
//        Log.e("ryu", "last: "+String.valueOf(System.currentTimeMillis()-lastTime)+" ambient"+ ambientValue);
//            if(ambientValue>250) {
//                ambientValue = 0;
//            }
//            ChangeImageColor(ambientValue);
//            lastTime = System.currentTimeMillis();
//            if(ambientValue == 0 )
//            {
//                long ftime = System.currentTimeMillis();
//                while(System.currentTimeMillis()-ftime<(delayTime+1000))
//                {
//
//                }
//            }
//            Log.e("ryu", "ambientValue: "+ ambientValue);

//            ambientValue+=50;
//        Log.e("ryu", "delay : "+delayTime);

        ChangeImageColor(ambientValue);
        ambientValue+=50;
        if(ambientValue>250)
            ambientValue=0;

    }

    void IncreaseBrighness2()
    {
       switch (ambientValue)
       {
           case 0:
               ambientValue=56; break;
           case 56:
               ambientValue=84; break;
           case 84:
               ambientValue=112; break;
           case 112:
               ambientValue=126; break;
           case 126:
               ambientValue=140; break;
           case 140:
               ambientValue=168; break;
           case 168:
               ambientValue=196; break;
           case 196:
               ambientValue=224; break;
           case 224:
               ambientValue=252; break;
           case 252:
               ambientValue=0; break;
       }
        Settings.System.putInt(getApplicationContext().getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,ambientValue);
        Log.e("ryu", "ambientValue: "+ ambientValue);

    }
    public native String stringFromJNI();
    public native int getIntfromMat(long matAdrr);
    public native void setInt2Mat(long matAdrr, int value);

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public void onPrepared(MediaPlayer mediap) {
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mp.start();
////                textureView.setAlpha(0);
//            }
//        }, 10000);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
//
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
       if(!isRun)
       {
           Surface s = new Surface(surface);

           try
           {
               mp = new MediaPlayer();
               mp.setDataSource("rtsp://admin:1111@10.11.11.41:554/av0_0");
               mp.setSurface(s);
               mp.prepare();

               mp.setOnBufferingUpdateListener(this);
               mp.setOnCompletionListener(this);
               mp.setOnPreparedListener(this);
               mp.setOnVideoSizeChangedListener(this);



           }
           catch (IllegalArgumentException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           } catch (SecurityException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           } catch (IllegalStateException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           } catch (IOException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           }
       }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if(!isRun)
        {
            if(frameCount<1500)
            {
                Mat mat = new Mat();
                Bitmap bm = textureView.getBitmap();
                bm = bm .copy(Bitmap.Config.ARGB_8888, true);
                Utils.bitmapToMat(bm, mat);
                Imgproc.cvtColor(mat,mat,Imgproc.COLOR_RGB2GRAY);
//                Imgproc.resize(mat, mat, new Size(mat.cols()*0.3,mat.rows()*0.3));
                float addValue = getIntfromMat(mat.getNativeObjAddr());
                listData.add(addValue);
//                totalFrame.add(mat);
                frameCount++;
            }
            else
            {
                isRun=true;
                ImageDo();
            }

        }
    }

    void ImageDo()
    {
//        Log.e("ryu", "end: "+totalFrame.size());
        int stable=0;
//        int totalPixelValue=0;
        float sub=0, totalPlus=0;
//        Log.e("ryu","row: "+ totalFrame.get(0).rows()+" height: "+ totalFrame.get(0).cols());
//        int width = totalFrame.get(0).cols();
//        int heith = totalFrame.get(0).rows();
//        biChia = width*heith;
//        listData.clear();
        /////////////////
//        for(int pos=0; pos<totalFrame.size();pos++)
//        {
//            Mat mat = totalFrame.get(pos);
//            totalPixelValue = getIntfromMat(mat.getNativeObjAddr());
//            Log.e("ryu", "origin: "+ totalPixelValue/(biChia)+ " "+ listData.size());
//            listData.add((float) totalPixelValue/(biChia));
//            totalPixelValue =0;
//        }



        ///////////////////
        for(int i=0; i<listData.size()-1;i++)
        {
            sub = listData.get(i+1)-listData.get(i);
            if(sub<2 && sub>(-2))
            {
                stable++;
                totalPlus +=listData.get(i);
            }
            else
            {
                if(stable>10)
                {
                    totalPlus+=listData.get(i);
                    stable++;
                    float rs = totalPlus/stable;
                    result.add(rs);
                    stable =0;
                    totalPlus=0;
                }
                else
                {
                    stable=0;
                    totalPlus=0;
                }

            }
        }
//        UniqueList();
        Log.e("ryu","ryusult size: " + result.size());
        for(int i=0; i<result.size();i++)
        {
            Log.e("ryu", "result: " + result.get(i));
        }
        playSound.start();
        textureView.setAlpha(0);
        listData.clear();
    }

    void UniqueList()
    {
        HashSet<Float> hashSet = new HashSet<Float>();
        hashSet.addAll(result);
        result.clear();
        result.addAll(hashSet);
    }


}
