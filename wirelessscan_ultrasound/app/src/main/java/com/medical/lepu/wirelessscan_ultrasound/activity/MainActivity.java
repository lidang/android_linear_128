package com.medical.lepu.wirelessscan_ultrasound.activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.util.MutableByte;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.medical.lepu.wirelessscan_ultrasound.R;
import com.medical.lepu.wirelessscan_ultrasound.base.BaseActivity;
import com.medical.lepu.wirelessscan_ultrasound.base.BaseMessage;
import com.medical.lepu.wirelessscan_ultrasound.widget.VerticalSeekBar;

import java.util.ArrayList;
import java.util.List;




public class MainActivity extends BaseActivity implements View.OnClickListener {


     private  ImageButton       btnWifi;

     private   TextView         ssid_wifi ;

     private    TextView        view_num ;

     private    TextView       time_zone  ;

     private    TextView       gain_zone  ;

     private     TextView      depth_zone  ;

     private     TextView      freezing_zone ;

     private     ImageView     ultrasound_view;

     private    SeekBar        scan_seekbar ;

     private   ImageButton      btn_pre  ;

     private    ImageButton     btn_play ;

    private    ImageButton     btn_next  ;

    private    ImageButton     btn_save  ;

    private    ImageButton     btn_scan  ;

    private    ImageButton     btn_settting ;

    private   RelativeLayout   imageView_layout ;

    private    ImageButton     btn_freeze  ;

   private   VerticalSeekBar  btn_gain_ctl ;


  // below   variable

    private   boolean    ssid_valid  ;           // ssid有效
    private   boolean    pitch_connected ;       // 探头连接
    private   boolean    running;                // 运行状态

    private   int       snd_running ;    // 需要发送的运行状态 (-1: 无需要发送状态， 0：冻结状态， 1:运行状态）

    private    int      snd_num     ;    //  发送状态的等待计数

    private   boolean    have_image ;    //  图像区是否有图像

    private   boolean    loaded_image;   //  载入的图像

    private   boolean     circle_loop;   //  是否处于回放状态中

    private   boolean     full_screen ;  //  是否处于全屏状态


    private   double      gama_value ;   //  图像的gama校正

    private     int       probe_type ;  //  探头类型信息

    private     int       gain_value ;  // 探头当前的增益

    private    int       zoom_value ;  //  探头当前的缩放

    private    int       sale_code  ;  // 销售区域代码

    private ArrayList    image_array ; // 回放图像数据数组

    private   ImageView     gradientView ;  //  灰度条图像

    private  BaseMessage      message  ;    //  解帧

    private MutableByte[]     imageData ;

    private    int           imagIndex ;

    private    TextView      labelTime ;

    private    TextView      labelDepth;

    private    TextView      labelGain ;

    private WifiManager      wifiManager;

    private WifiInfo         currentWifiInfo;  // 当前所连接的wifi

    private List<ScanResult>   wifiList;// wifi列表

    private ContentResolver    mContentResolver ;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initView();

        initListener();

        initVariable();


        loadData();

    }


    private void initView() {

        // view

        btnWifi     =  (ImageButton) findViewById(R.id.btn_wifi) ;

        ssid_wifi   =  (TextView) findViewById(R.id.ssid_wifi)  ;

        view_num    =   (TextView) findViewById(R.id.view_num)  ;

        scan_seekbar =   (SeekBar)  findViewById(R.id.scan_seekbar) ;

        btn_pre     =   (ImageButton) findViewById(R.id.btn_pre)  ;

         btn_play  =   (ImageButton) findViewById(R.id.btn_play) ;

        btn_next   =    (ImageButton)findViewById(R.id.btn_next) ;

        btn_save   =    (ImageButton) findViewById(R.id.btn_save) ;

        btn_scan   =     (ImageButton) findViewById(R.id.btn_scan) ;

        btn_settting =   (ImageButton)  findViewById(R.id.btn_settting) ;

        imageView_layout  =   (RelativeLayout) findViewById(R.id.imageView_layout);

        btn_freeze   =   (ImageButton) findViewById(R.id.btn_freeze) ;

        btn_gain_ctl  =  (VerticalSeekBar)findViewById(R.id.btn_gain_ctl) ;

        time_zone     =   (TextView)  findViewById(R.id.time_zone) ;

        gain_zone     =   (TextView)  findViewById(R.id.gain_zone) ;

        depth_zone    =   (TextView) findViewById(R.id.depth_zone) ;

        freezing_zone =   (TextView)   findViewById(R.id.freezing_zone) ;

        ultrasound_view =  (ImageView)  findViewById(R.id.ultrasound_image) ;


        // add  the  scan  view  content,though  the   ViewGroup  add  the subview
        
    }


    public void initListener() {

        btnWifi.setOnClickListener(this);

        btn_pre.setOnClickListener(this);

        btn_play.setOnClickListener(this);

        btn_next.setOnClickListener(this);

        btn_save.setOnClickListener(this);

        btn_scan.setOnClickListener(this);

        btn_settting.setOnClickListener(this);

        btn_freeze.setOnClickListener(this);


    }


    public void initVariable() {

        Log.i("creat", "main_resume");

        ssid_valid     = false  ;

        running        = false  ;

        pitch_connected = false ;

        snd_running    =  -1   ;

        snd_num      =    0   ;

        have_image    =  false ;

        loaded_image = false   ;

        circle_loop = false   ;

        full_screen  =  false ;


        gama_value   =  1.3  ;

     //   AppUtil.getInt("probe_type",0) ;

     //   AppUtil.getFloat("gain_value",0);

     //   AppUtil.getFloat("zoom_value",0) ;


        sale_code      =    0   ;

         image_array   =   new ArrayList()  ;

      //  imageData      =   new  MutableByte [0] ;

        imagIndex     =  0  ;

        mContentResolver = getContentResolver();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);


    }





    public void loadData() {

        openWifi();
        new ScanWifiThread().start();


    }


    @Override
    protected void onStart() {

        super.onStart();


    }



    @Override
    protected void onResume() {


        Log.i("resume", "main_resume");



        //     openWifi();

        //      currentWifiInfo = wifiManager.getConnectionInfo();

        //       ssidWifi.setText("当前网络：" + currentWifiInfo.getSSID()+ " ip:"
        //              + WifiUtil.intToIp(currentWifiInfo.getIpAddress()));


        super.onResume();
    }


    private void openWifi() {

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

    }


    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_wifi:
                Log.i("wifi_setting", "jump to the setting page");
              //  unLock() ;

                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
                break;


        }

    }



       public   void unLock(){
        //推荐使用
       // setLockPatternEnabled(android.provider.Settings.Secure.,false);
    }

    private void setLockPatternEnabled(String systemSettingKey, boolean enabled) {
        //推荐使用
        android.provider.Settings.Secure.putInt(mContentResolver, systemSettingKey,enabled ? 1 : 0);
    }




    class ScanWifiThread extends Thread
    {

        @Override
        public void run()
        {
            while (true)
            {
                currentWifiInfo = wifiManager.getConnectionInfo();
                startScan();
                try
                {
                    Thread.sleep(10);
                }
                catch (InterruptedException e)
                {
                    break;
                }
            }
        }
    }




    public void startScan()
    {
        wifiManager.startScan();
        // 获取扫描结果
        wifiList = wifiManager.getScanResults();
        String tempStr = null;
        for (int i = 0; i < wifiList.size(); i++)
        {
            tempStr = wifiList.get(i).SSID;
            if (tempStr.startsWith("SS-1 "))
            {
                tempStr = tempStr + "(已连接)";

                Message msg = new Message();
                msg.what = 1;
                msg.obj =   tempStr;
                handler.sendMessage(msg);


                 break;
            }

        }
    }



    // handler类接收数据
    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ssid_wifi.setText((String)msg.obj);

            }
        };
    };








}
