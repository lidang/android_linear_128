package com.medical.lepu.wirelessscan_ultrasound.activity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.MutableInt;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncSocket;
import com.koushikdutta.async.callback.ConnectCallback;
import com.medical.lepu.wirelessscan_ultrasound.R;
import com.medical.lepu.wirelessscan_ultrasound.base.BaseActivity;
import com.medical.lepu.wirelessscan_ultrasound.base.BaseMessage;
import com.medical.lepu.wirelessscan_ultrasound.base.BaseModel;
import com.medical.lepu.wirelessscan_ultrasound.base.BaseTask;
import com.medical.lepu.wirelessscan_ultrasound.base.C;
import com.medical.lepu.wirelessscan_ultrasound.util.AppUtil;
import com.medical.lepu.wirelessscan_ultrasound.util.DSC_Util;
import com.medical.lepu.wirelessscan_ultrasound.widget.RawImage;
import com.medical.lepu.wirelessscan_ultrasound.widget.VerticalSeekBar;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends BaseActivity implements View.OnClickListener,View.OnTouchListener{


    private ImageButton btnWifi;

    private TextView ssid_wifi;

    private TextView view_num;

    private TextView time_zone;

    private TextView gain_zone;

    private TextView depth_zone;

    private TextView freezing_zone;

    private ImageView ultrasound_view;

    private SeekBar scan_seekbar;

    private ImageButton btn_pre;

    private ImageButton btn_play;

    private ImageButton btn_next;

    private ImageButton btn_save;

    private ImageButton btn_scan;

    private ImageButton btn_settting;

    private RelativeLayout imageView_layout;

    private ImageButton btn_freeze;

    private VerticalSeekBar btn_gain_ctl;


    // below   variable

    private boolean ssid_valid;           // ssid有效
    private boolean pitch_connected;       // 探头连接
    private boolean running;                // 运行状态

    private int snd_running;    // 需要发送的运行状态 (-1: 无需要发送状态， 0：冻结状态， 1:运行状态）

    private int snd_num;    //  发送状态的等待计数

    private boolean have_image;    //  图像区是否有图像

    private boolean loaded_image;   //  载入的图像

    private boolean circle_loop;   //  是否处于回放状态中

    private boolean full_screen;  //  是否处于全屏状态


    private double gama_value;   //  图像的gama校正

    private int probe_type;  //  探头类型信息

    private int gain_value;  // 探头当前的增益

    private int zoom_value;  //  探头当前的缩放

    private int sale_code;  // 销售区域代码

    private ArrayList image_array; // 回放图像数据数组

    private ImageView gradientView;  //  灰度条图像

    private BaseMessage message;    //  解帧

    private BaseModel model;     //  绘制图像数据

    private MutableInt[] imageData;

    private int imagIndex;

    private WifiManager wifiManager;

    private WifiInfo currentWifiInfo;  // 当前所连接的wifi

    private List<ScanResult> wifiList;// wifi列表

    private ContentResolver mContentResolver;

    private GestureDetector detector;

    private Timer playTimer;

    private TimerTask playTimerTask;

    private Timer nullImageTimer;

    private TimerTask nullImageTimerTask;

    private Timer connectTimer;

    private TimerTask connectTimerTask;

    private AsyncServer dataControlSocket;



    private final static int horizontaldistance = 20;               //水平最小识别距离
    private final static int minVelocity = 10;               //最小识别速度
    private final static int verticaldistance = 20;           //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initView();

        initListener();

        initVariable();

        initItemState();

        loadData();

    }


    private void initView() {

        // view

        btnWifi = (ImageButton) findViewById(R.id.btn_wifi);

        ssid_wifi = (TextView) findViewById(R.id.ssid_wifi);

        view_num = (TextView) findViewById(R.id.view_num);

        scan_seekbar = (SeekBar) findViewById(R.id.scan_seekbar);

        btn_pre = (ImageButton) findViewById(R.id.btn_pre);

        btn_play = (ImageButton) findViewById(R.id.btn_play);

        btn_next = (ImageButton) findViewById(R.id.btn_next);

        btn_save = (ImageButton) findViewById(R.id.btn_save);

        btn_scan = (ImageButton) findViewById(R.id.btn_scan);

        btn_settting = (ImageButton) findViewById(R.id.btn_settting);

        imageView_layout = (RelativeLayout) findViewById(R.id.imageView_layout);

        btn_freeze = (ImageButton) findViewById(R.id.btn_freeze);

        btn_gain_ctl = (VerticalSeekBar) findViewById(R.id.btn_gain_ctl);

        time_zone = (TextView) findViewById(R.id.time_zone);

        gain_zone = (TextView) findViewById(R.id.gain_zone);

        depth_zone = (TextView) findViewById(R.id.depth_zone);

        freezing_zone = (TextView) findViewById(R.id.freezing_zone);

        ultrasound_view = (ImageView) findViewById(R.id.ultrasound_image);


        // add  the  scan  view  content,through  the   ViewGroup  add  the subview

        detector = new GestureDetector(MainActivity.this, new ViewGestureListener());

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

        // touch
        ultrasound_view.setOnTouchListener(this);
        ultrasound_view.setLongClickable(true);

        //seekbar
        btn_gain_ctl.setOnSeekBarChangeListener(new VerticalOnSeekBarChangeListener());

        scan_seekbar.setOnSeekBarChangeListener(new HorizontalOnSeekBarChangeListener());

    }


    public void initVariable() {

        Log.i("creat", "main_resume");

        ssid_valid = false;

        running = false;

        pitch_connected = false;

        snd_running = -1;

        snd_num = 0;

        have_image = false;

        loaded_image = false;

        circle_loop = false;

        full_screen = false;


        gama_value = 1.3;

     //   probe_type = AppUtil.getInt("probe_type", 0);

        gain_value = AppUtil.getInt("gain_value", 0);

        zoom_value = AppUtil.getInt("zoom_value", 0);

        probe_type = DSC_Util.PROBE_SECTORARRAY ;



        sale_code = 0;

        image_array = new ArrayList();

        //  imageData      =   new  MutableByte [0] ;

        imagIndex = 0;

        mContentResolver = getContentResolver();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);


    }


    public void initItemState() {
        //  状态的一致性检测

        if (!ssid_valid) {
            pitch_connected = false;
        }

        if (!pitch_connected) {
            running = false;
        }

        if (running) {
            circle_loop = false;
        }

        if (!have_image) {
            loaded_image = false;
        }

        //  各个按钮的状态调整

        if (pitch_connected) {
            this.btn_freeze.setEnabled(true);
        } else {
            this.btn_freeze.setEnabled(false);
        }

        if (pitch_connected && running) {
            this.btn_gain_ctl.setEnabled(true);
        } else {
            this.btn_gain_ctl.setEnabled(false);
        }

        if (running) {
            this.btnWifi.setEnabled(false);
        } else {
            this.btnWifi.setEnabled(true);
        }

        if (running || image_array == null || image_array.isEmpty()) {
            scan_seekbar.setEnabled(false);
        } else {
            scan_seekbar.setEnabled(true);
        }

        if (running || image_array == null || image_array.isEmpty()) {
            btn_pre.setEnabled(false);
            btn_play.setEnabled(false);
            btn_next.setEnabled(false);

        } else {
            btn_pre.setEnabled(true);
            btn_play.setEnabled(true);
            btn_next.setEnabled(true);

        }

        if (!running && have_image && !loaded_image) {
            btn_save.setEnabled(true);
        } else {
            btn_save.setEnabled(false);
        }

        if (!running && !circle_loop) {
            btn_scan.setEnabled(true);
        } else {
            btn_scan.setEnabled(false);
        }


        if (!running && !circle_loop) {
            btn_settting.setEnabled(true);
        } else {
            btn_settting.setEnabled(false);
        }

        //  更新Frozen标签

        if (running) {
            freezing_zone.setText(R.string.running);
        } else {
            freezing_zone.setText(R.string.freezing);
        }

        // 更新播放按钮的图标

        if (circle_loop) {
            btn_play.setBackgroundResource(R.drawable.btn_pause_pre);
        } else {
            btn_play.setBackgroundResource(R.drawable.btn_play_pre);
        }


    }


    public void loadData() {

        openWifi();

    }


    @Override
    protected void onStart() {

        super.onStart();

        if (!have_image) {
            nullImageRefresh();
        }

        startNullImgTimer();

        startConnectTimer();


        initItemState();


    }


    @Override
    protected void onResume() {

        Log.i("resume", "main_resume");

        String   thread_id   =   Thread.currentThread().getName();

        Log.i("main", thread_id);

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
                unLock();
                if (circle_loop) {
                    circle_loop = false;
                    stopPlayTimer();
                }

                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
                break;


        }

    }


    public void unLock() {
        //推荐使用
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }


    public void wifiScan() {
        wifiManager.startScan();
        // 获取扫描结果
        wifiList = wifiManager.getScanResults();
        String tempStr = null;
        for (int i = 0; i < wifiList.size(); i++) {
            tempStr = wifiList.get(i).SSID;
            if (tempStr.startsWith("SS-1 ")) {
                tempStr = tempStr + "(已连接)";

                //   sendMessage   (BaseTask.TASK_COMPLETED, C.task.wifi  , tempStr ) ;

                break;
            } else {

                tempStr = null;

            }


        }

        if (tempStr != null) {
            sendMessage(BaseTask.TASK_COMPLETED, C.task.wifi, tempStr);
        } else {

            sendMessage(BaseTask.TASK_COMPLETED, C.task.wifi);
        }

    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {


        return detector.onTouchEvent(motionEvent);
    }


    class ViewGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            //大于设定的最小滑动距离并且在水平/竖直方向速度绝对值大于设定的最小速度，则执行相应方法
            if (e1.getX() - e2.getX() > horizontaldistance && Math.abs(velocityX) > minVelocity) {
                Toast.makeText(MainActivity.this, "turn left", Toast.LENGTH_SHORT).show();

                swipeImageView(C.direction.LEFT);

            } else if (e2.getX() - e1.getX() > horizontaldistance && Math.abs(velocityX) > minVelocity) {
                Toast.makeText(MainActivity.this, "turn right", Toast.LENGTH_SHORT).show();

                swipeImageView(C.direction.RIGHT);

            } else if (e1.getY() - e2.getY() > verticaldistance && Math.abs(velocityY) > minVelocity) {
                Toast.makeText(MainActivity.this, "turn up", Toast.LENGTH_SHORT).show();

                swipeImageView(C.direction.UP);

            } else if (e2.getY() - e1.getY() > verticaldistance && Math.abs(velocityY) > minVelocity) {
                Toast.makeText(MainActivity.this, "turn down", Toast.LENGTH_SHORT).show();

                swipeImageView(C.direction.DOWN);
            }

            return super.onFling(e1, e2, velocityX, velocityY);

        }

        @Override
        public boolean onDown(MotionEvent e) {
            return super.onDown(e);
        }
    }


    private void swipeImageView(String direction) {

        unLock();

        if (running) {       //  运行中，上下扫手势调节ZOOM

            if (direction.equals(C.direction.DOWN)) {
                if (zoom_value > 0)
                    zoom_value--;

            } else if (direction.equals(C.direction.UP)) {
                zoom_value++;
            }

            if (zoom_value < 0) {
                zoom_value = 0;
            }
            if (zoom_value > 3) {
                zoom_value = 3;
            }

            // 发送命令


            // 发送完毕
            AppUtil.setInt("zoom_value", zoom_value);

        } else if (!loaded_image) {   //  冻结后，左右扫手势回放图像
            if (!image_array.isEmpty()) {
                if (direction.equals(C.direction.LEFT)) {
                    preImageView();
                } else if (direction.equals(C.direction.RIGHT)) {
                    nextImageView();
                }

            }
        }


    }

    private void preImageView() {


    }


    private void nextImageView() {


    }


    class VerticalOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {


        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            unLock();
            gain_value = i;

            if (gain_value > 127) gain_value = 127;

            if (gain_value < 0) gain_value = 0;

            // 发送命令

            //结束

            AppUtil.setInt("gain_value", gain_value);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }


    class HorizontalOnSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {


        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            unLock();
            if (circle_loop) {       //停止回放

                //停止回放部分

            }
            imagIndex = i;

            int sum = image_array.size();
            String num = i + "/" + sum;
            view_num.setText(num);

            if (imagIndex < image_array.size()) {   //绘制图像

                RawImage rawImage = (RawImage) image_array.get(imagIndex);

                //绘制图像部分

            }

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }


    }


    private void stopPlayTimer() {
        if (playTimer != null) {
            playTimer.cancel();
            playTimer = null;
        }

        if (playTimerTask != null) {
            playTimerTask.cancel();
            playTimerTask = null;
        }

    }


    private void startNullImgTimer() {
        if (nullImageTimer == null) {
            nullImageTimer = new Timer();
        }

        if (nullImageTimerTask == null) {

            nullImageTimerTask = new TimerTask() {
                @Override
                public void run() {

                    nullImageRefresh();

                }
            };

        }

        nullImageTimer.schedule(nullImageTimerTask, 0, 1000);       //1s


    }


    private void stopNullImgTimer() {
        if (nullImageTimer != null) {
            nullImageTimer.cancel();
            nullImageTimer = null;
        }
        if (nullImageTimerTask != null) {
            nullImageTimerTask.cancel();
            nullImageTimerTask = null;

        }

    }


    private void startConnectTimer() {

        if (connectTimer == null) {
            connectTimer = new Timer();
        }

        if (connectTimerTask == null) {
            connectTimerTask = new TimerTask() {
                @Override
                public void run() {

                    sendConnectCommand();
                }
            };
        }

        connectTimer.schedule(connectTimerTask, 0, 1000);        //1s

    }


    private void nullImageRefresh() {


    }


    private void sendConnectCommand() {
        wifiScan();

    }


    @Override
    protected void onTaskCompleted(int taskId) {
        super.onTaskCompleted(taskId);

            switch (taskId)     {

                case  C.task.wifi  :

                    try {
                        ssid_wifi.setText("");

                        ssid_valid= false  ;

                        initItemState();



                    } catch (Exception e) {

                        e.printStackTrace();

                        toast(e.getMessage());

                    }

                    break;


                    case  C.task.dataControl  :

                        pitch_connected  = true  ;

                        initItemState();


                    break;


                  case  C.task.stateControl  :

                      pitch_connected  = true  ;

                      initItemState();

                    break;



                case   C.task.receiveControl :

                    pitch_connected  = true  ;

                    initItemState();

                     break;


            }





    }


    @Override
    protected void onTaskCompleted(int taskId, String msg) {
        super.onTaskCompleted(taskId,msg);

             switch (taskId)    {

                 case  C.task.wifi   : {

                     try {

                         ssid_wifi.setText(msg);

                         ssid_valid   = true ;

                         initItemState();

                         socketCommunication ();




                     } catch (Exception e) {

                         e.printStackTrace();

                         toast(e.getMessage());

                     }

                     break;
                 }







             }








    }


    @Override
    protected void onTaskError(int taskId) {

        super.onTaskError(taskId);
    }



      private    void    socketCommunication ()   {

            if (ssid_valid && !pitch_connected)  {

            //    doTaskAsync(C.task.dataControl,C.api.host,C.port.dataControl);

           //     doTaskAsync(C.task.stateControl,C.api.host,C.port.stateControl);

           //     doTaskAsync(C.task.receiveControl,C.api.host,C.port.receiveControl);

           //       new AsyncServer().connectSocket(new InetSocketAddress(C.api.host, C.port.dataControl),this);

           //       new AsyncServer().connectSocket(new InetSocketAddress(C.api.host, C.port.stateControl),this);

           //       new AsyncServer().connectSocket(new InetSocketAddress(C.api.host, C.port.receiveControl),this);


                  if (dataControlSocket==null)  {

                      dataControlSocket  =  new AsyncServer() ;
                  }



                {
                    AsyncServer.getDefault().connectSocket(new InetSocketAddress(C.api.host, C.port.dataControl),dataControlCallback);

                    AsyncServer.getDefault().connectSocket(new InetSocketAddress(C.api.host, C.port.stateControl),stateControlCallback);

                    AsyncServer.getDefault().connectSocket(new InetSocketAddress(C.api.host, C.port.receiveControl),receiveControlCallback) ;

                }




            }

      }



    ConnectCallback      dataControlCallback    =      new ConnectCallback() {
        @Override
        public void onConnectCompleted(Exception ex, AsyncSocket socket) {

            Log.d("dataControl","dataControl  on  connected !")  ;

            sendMessage(BaseTask.TASK_COMPLETED,C.task.dataControl);










        }
    }  ;


    ConnectCallback     stateControlCallback  =     new ConnectCallback() {
        @Override
        public void onConnectCompleted(Exception ex, AsyncSocket socket) {

            Log.d("stateControl","stateControl  on  connected !")  ;

            sendMessage(BaseTask.TASK_COMPLETED,C.task.stateControl);



        }
    }   ;



    ConnectCallback   receiveControlCallback   =    new ConnectCallback() {
        @Override
        public void onConnectCompleted(Exception ex, AsyncSocket socket) {

            Log.d("receiveControl","receiveControl  on  connected !")  ;

            sendMessage(BaseTask.TASK_COMPLETED,C.task.receiveControl);


            




        }
    }   ;







    private   void     sendParamZoom   ( )   {




    }






}


