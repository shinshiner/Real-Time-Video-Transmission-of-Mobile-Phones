package com.example.administrator.myapplication;

import com.example.administrator.myapplication.ClientThread;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity{
    MyHandler handler;
    ClientThread clientThread;
    ByteArrayOutputStream outstream;

    Button start;
    SurfaceView surfaceView;
    EditText et1;
    SurfaceHolder  sfh;
    Camera camera;
    boolean isPreview = false;        //是否在浏览中
    int screenWidth=300, screenHeight=300;
    String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;// 获取屏幕分辨率宽度
        screenHeight = dm.heightPixels;
        Log.i("screen width", "" + screenWidth);
        Log.i("screen height", "" + screenHeight);

        start = (Button)findViewById(R.id.start);
        surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        et1 = (EditText)findViewById(R.id.ip1);
        sfh = surfaceView.getHolder();
        sfh.setFixedSize(screenWidth, screenHeight/4*3);

        handler = new MyHandler();

        sfh.addCallback(new Callback(){
            @Override
            public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}
            @Override
            public void surfaceCreated(SurfaceHolder arg0) {
                initCamera();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder arg0) {
                if (camera != null) {
                    if (isPreview)
                        camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            }
        });

        //开启连接服务
        start.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View arg0) {
                ip = et1.getText().toString();
                et1.setVisibility(View.INVISIBLE);
                start.setVisibility(View.INVISIBLE);
                clientThread = new ClientThread(handler, ip);
                new Thread(clientThread).start();
                start.setEnabled(false);
            }
        });

    }
    @SuppressWarnings("deprecation")
    private void initCamera() {
        if (!isPreview) {
            camera = Camera.open();
            ClientThread.size = camera.getParameters().getPreviewSize();
        }
        if (camera != null && !isPreview) {
            try{
                camera.setPreviewDisplay(sfh);                 // 通过SurfaceView显示取景画面
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(screenWidth, screenHeight/4*3);
                 /* 每秒从摄像头捕获5帧画面， */
                parameters.setPreviewFrameRate(2);
                parameters.setPictureFormat(ImageFormat.NV21);           // 设置图片格式
                parameters.setPictureSize(screenWidth, screenHeight/4*3);    // 设置照片的大小
                camera.setDisplayOrientation(90);
                camera.setPreviewCallback(new PreviewCallback(){
                    @Override
                    public void onPreviewFrame(byte[] data, Camera c) {
                        Size size = camera.getParameters().getPreviewSize();
                        try{
                            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                            // ......
                        // ......
                            if(image!=null){
                                Message msg = clientThread.revHandler.obtainMessage();
                                msg.what=0x111;
                                msg.obj=image;
                                clientThread.revHandler.sendMessage(msg);
                            }
                        }catch(Exception ex){
                            Log.e("Sys","Error:"+ex.getMessage());
                        }
                    }

                });
                camera.startPreview();                                   // 开始预览
                //camera.autoFocus(null);                                  // 自动对焦
            } catch (Exception e) {
                e.printStackTrace();
            }
            isPreview = true;
        }
    }

    static class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            if(msg.what==0x222){
                //返回信息显示代码
            }
        }
    }

    private String getLocalIP(){
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        if(ipAddress==0){
            ip = "";
        }
        else {
            ip = ((ipAddress & 0xff) + "." + (ipAddress >> 8 & 0xff) + "."
                    + (ipAddress >> 16 & 0xff) + "." + (ipAddress >> 24 & 0xff));
        }
        Log.i("int ip ", ip);
        return ip;
    }
    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
    }
}
//
//import java.io.ByteArrayOutputStream;
//import java.io.DataOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Timer;
//
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Canvas;
//import android.graphics.ImageFormat;
//import android.graphics.Paint;
//import android.graphics.Rect;
//import android.graphics.YuvImage;
//import android.graphics.drawable.BitmapDrawable;
//import android.hardware.Camera;
//import android.hardware.Camera.Parameters;
//import android.hardware.Camera.PreviewCallback;
//import android.net.wifi.WifiInfo;
//import android.net.wifi.WifiManager;
//import android.os.Bundle;
//import android.os.Message;
//import android.util.Log;
//import android.view.Display;
//import android.view.Surface;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//
//import java.util.concurrent.LinkedBlockingQueue;
//
//public class MainActivity extends Activity implements SurfaceHolder.Callback,PreviewCallback{
//    SurfaceHolder surfaceHolder;
//    Camera camera ;
//    Bitmap bitmap;
//    byte[] rawImage;
//    ByteArrayOutputStream baos;
//    BitmapFactory.Options options = new BitmapFactory.Options();
//
//    EditText et1 = null;
//    EditText et2 = null;
//    Button btn1 = null;
//
//    String ip = null;
//    int port = 2333;
//    boolean readyToSend = true;
//
//    public static LinkedBlockingQueue<Bitmap> buff_queue = new LinkedBlockingQueue<Bitmap>(1000);
//
//    /* Called when the activity is first created. */
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        SurfaceView view = (SurfaceView) findViewById(R.id.surfaceView);
//        view.getHolder().addCallback(this);
//        view.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//
//        // get ip and port
//        et1 = (EditText)findViewById(R.id.ip1);
//        et2 = (EditText)findViewById(R.id.port1);
//        btn1 = (Button)findViewById(R.id.btn1);
//        btn1.setOnClickListener(new Button.OnClickListener(){
//            public void onClick (View view){
//                ip = et1.getText().toString();
//                port = Integer.parseInt(et2.getText().toString());
//                Log.i("ip:", ip);
//                Log.i("ip:", "" + port);
//
//                btn1.setVisibility(View.INVISIBLE);
//                et1.setVisibility(View.INVISIBLE);
//                et2.setVisibility(View.INVISIBLE);
//
//                // start to send image
//                readyToSend = true;
//                try{Thread.sleep(100);}catch (Exception e){}
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Log.i("Starting to ", "connect");
//                            Socket socket = new Socket();
//                            socket.connect(new InetSocketAddress(ip, port), 1000);
//                            //socket.setKeepAlive(true);
//                            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                            Log.i("Connected to ", "server");
//
//                            while (true){
//                                Bitmap bitmap = buff_queue.poll();
//                                while (bitmap == null){
//                                    bitmap = buff_queue.poll();
//                                }
//                                ByteArrayOutputStream bout = new ByteArrayOutputStream();
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, bout);
//
//                                long len = bout.size();
//                                Log.i("image size = ", "" + len);
//                                if (out != null) {
//                                    out.write(bout.toByteArray());
//                                    out.flush();
//                                    System.out.println("send an image");
//                                }
//                                else {
//                                    System.out.println("the output stream is broken");
//                                }
//                            }
//                        }catch(Exception e){
//                            Log.i("send msg ", "error");
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
//            }
//        });
//    }
//    public void surfaceCreated(SurfaceHolder holder) {}
//
//    @Deprecated
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        try{
//            camera = Camera.open();
//            camera.setPreviewDisplay(holder);
//            Parameters params = camera.getParameters();
//            //params.setPreviewFpsRange(15000, 15000);
//            //params.setPreviewSize(352, 288);
//            camera.setParameters(params);
//            camera.startPreview();
//            camera.setPreviewCallback(this);
//
//            int rotation = getDisplayOrientation(); // get current orientation of the window
//            camera.setDisplayOrientation(rotation);
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        if(camera != null) camera.release() ;
//        camera = null ;
//    }
//
//    public void onPreviewFrame(byte[] data, Camera camera) {
//        if (readyToSend) {
//            // pre-process the raw image
//            camera.autoFocus(null);
//            Camera.Size previewSize = camera.getParameters().getPreviewSize();//获取尺寸,格式转换的时候要用到
//            BitmapFactory.Options newOpts = new BitmapFactory.Options();
//            newOpts.inJustDecodeBounds = true;
//            YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
//            baos = new ByteArrayOutputStream();
//            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 20, baos);// 80--JPG图片的质量[0-100],100最高
//            rawImage = baos.toByteArray();
//            options.inPreferredConfig = Bitmap.Config.RGB_565;
//            bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options).copy(Bitmap.Config.ARGB_8888, true);
//            //bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
//
//            //System.out.println(bitmap.toString());
//            boolean success = buff_queue.offer(bitmap);
//            if (success == false){
//                buff_queue.poll();
//                buff_queue.offer(bitmap);
//            }
//            //server.send();
//        }
//    }
//    private String getLocalIP(){
//        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int ipAddress = wifiInfo.getIpAddress();
//        Log.d("", "int ip "+ipAddress);
//        if(ipAddress==0)return null;
//        return ((ipAddress & 0xff)+"."+(ipAddress>>8 & 0xff)+"."
//                +(ipAddress>>16 & 0xff)+"."+(ipAddress>>24 & 0xff));
//    }
//    private int getDisplayOrientation(){
//        WindowManager windowManager = getWindowManager();
//        Display display = windowManager.getDefaultDisplay();
//        int rotation = display.getRotation();
//        int degrees = 0;
//        switch (rotation){
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//        }
//
//        android.hardware.Camera.CameraInfo camInfo =
//                new android.hardware.Camera.CameraInfo();
//        android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);
//
//        // 这里其实还是不太懂：为什么要获取camInfo的方向呢？相当于相机标定？？
//        int result = (camInfo.orientation - degrees + 360) % 360;
//
//        return result;
//    }
//    private class myServer{
//        private Socket socket = new Socket();
//        private DataOutputStream out;
//        ImageView iv2 = (ImageView)findViewById(R.id.myimageview);
//
//        public myServer(){
//            try {
//                System.out.println("开始连接");
//                //ServerSocket serverSocket = new ServerSocket(port);
//                //socket = serverSocket.accept();
//                socket.connect(new InetSocketAddress(ip, port), 3000);
//                socket.setKeepAlive(true);
//                out = new DataOutputStream(socket.getOutputStream());
//            }catch(Exception e){
//                e.printStackTrace();
//            }
//        }
//
//        public void send(){
//            Bitmap bitmap = buff_queue.poll();
//            ByteArrayOutputStream bout = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, bout);
//
////            Canvas mCanvas = new Canvas(bitmap);
////            Paint mPaint = new Paint();
////            mCanvas.drawBitmap(bitmap, 100, 100, null);
//
//            BitmapDrawable bmpDraw=new BitmapDrawable(bitmap);
//            iv2.setImageDrawable(bmpDraw);
//
//            long len = bout.size();
//            Log.i("image size = ", "" + len);
//            try {
//                //out.writeLong(len);
//                if (out != null) {
//                    out.write(bout.toByteArray());
//                    out.flush();
//                    System.out.println("send an image");
//                }
//                else {
//                    System.out.println("the output stream is broken");
//                }
////                socket.sendUrgentData(0xFF); // 发送心跳包
////                System.out.println("目前是处于链接状态！");
//            }catch(Exception e){
//                Log.i("can not send an img", "" + len);
//                e.printStackTrace();
//            }
//        }
//    }
//}