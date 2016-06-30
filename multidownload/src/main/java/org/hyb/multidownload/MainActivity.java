package org.hyb.multidownload;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.StreamHandler;

public class MainActivity extends AppCompatActivity {
    int total=0;
    private boolean downloading=false;
    private EditText et_fileurl;
    private URL fileUrl;
    private File file;
    List<HashMap<String,Integer>> listThread;
    private ProgressBar progress;
    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==0x123)
            {
                progress.setProgress((int)msg.obj);
            }

        }
    };
    private int content_length;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button btn_download=(Button) findViewById(R.id.btn_down);
        et_fileurl = (EditText)findViewById(R.id.et_fileurl);
        progress = (ProgressBar) findViewById(R.id.progress);
        listThread=new ArrayList<>();
        btn_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(downloading){
                    downloading=false;
                    btn_download.setText("开始下载");
                    return;
                }
                downloading=true;
                btn_download.setText("暂停下载");
                if(listThread.size()==0){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                fileUrl = new URL(et_fileurl.getText().toString().trim());
                                HttpURLConnection conn=(HttpURLConnection) fileUrl.openConnection();
                                conn.setRequestMethod("GET");
                                conn.setConnectTimeout(5000);
                                conn.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322)");
                                //获取文件长度
                                content_length = conn.getContentLength();
                                progress.setMax(content_length);
                                progress.setProgress(0);
                                if(content_length <0)//文件不存在
                                {
                                    Log.d("hyb","文件不存在");
                                    return;
                                }
                                //把文件下载到手机sd卡中
                                file = new File(Environment.getExternalStorageDirectory(),getFileName(et_fileurl.getText().toString()));
                                //RandomAccessFile randomFile=new RandomAccessFile(file,"rw");
                                //randomFile.setLength(content_length);
                                int blockSize= content_length / 3;//把文件分成3个线程来下载
                                for(int i=0;i<3;i++)
                                {
                                    int begin= i*blockSize;
                                    int end=(i+1) * blockSize;
                                    if(i==2)
                                        end= content_length;
                                    HashMap<String,Integer> map=new HashMap<String, Integer>();
                                    map.put("begin",begin);
                                    map.put("end",end);
                                    map.put("finish",0);
                                    listThread.add(map);
                                    new Thread(new DownloadRunnable(i,begin,end, file, fileUrl)).start();
                                }
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                                Log.d("hyb","url格式不正确");
                            }catch (IOException e){
                                e.printStackTrace();
                            }

                        }
                    }).start();
                }else{
                    //恢复下载
                    Log.d("hyb","恢复下载");
                    for(int i=0;i<listThread.size();i++)
                    {
                        HashMap<String,Integer> map=listThread.get(i);
                        int begin=map.get("begin");
                        int end=map.get("end");
                        int finish=map.get("finish");
                        new Thread(new DownloadRunnable(i,begin+finish,end,file,fileUrl)).start();
                    }
                }

            }
        });
    }
    private String getFileName(String url)
    {
        int index=url.lastIndexOf("/")+1;
        return url.substring(index);
    }
    class DownloadRunnable implements  Runnable{
        private int id;
        private int begin;
        private int end;
        private File file;
        private URL url;

        public DownloadRunnable(int id, int begin, int end, File file, URL url) {
            this.id = id;
            this.begin = begin;
            this.end = end;
            this.file = file;
            this.url = url;
        }

        @Override
        public void run() {
            try {
                if(begin>end)return;
                HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                //conn.setConnectTimeout(5000);
                conn.setRequestProperty("User-Agent","Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; SV1; .NET CLR 1.1.4322)");
                conn.setRequestProperty("Range","bytes="+begin+"-"+end);//设置文件下载范围
                InputStream is=conn.getInputStream();
                byte [] buf=new byte[1024 * 1024];
                RandomAccessFile randomAccessFile=new RandomAccessFile(file,"rw");
                randomAccessFile.seek(begin);
                int len=0;
                HashMap<String,Integer> map=listThread.get(id);
                while((len=is.read(buf))!=-1 && downloading)
                {
                    randomAccessFile.write(buf,0,len);
                    map.put("finish",map.get("finish")+len);
                    updateProgress(len);
                    //Log.d("hyb","下载了-->"+total);
                }
                randomAccessFile.close();
                is.close();
            } catch (ProtocolException e) {
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    synchronized private void updateProgress(int add)
    {
        total+=add;
        Message msg=Message.obtain();
        msg.what=0x123;
        msg.obj=total;
        handler.sendMessage(msg);
    }
}
