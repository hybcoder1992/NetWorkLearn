package org.hyb.networklearn;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btn_get;
    private TextView tv_response;
    private EditText et_url;
    Context mContext;
    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==0x123)
            {
                String result=(String) msg.obj;
                tv_response.setText(result);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_get = (Button)findViewById(R.id.btn_get);
        tv_response = (TextView)findViewById(R.id.tv_response);
        et_url = (EditText)findViewById(R.id.et_url);
        mContext=this;
        btn_get.setOnClickListener(this);
    }
    //将输入流转成字符串
    private String streamToString(InputStream input) throws IOException
    {
        //字符流
        BufferedReader br=new BufferedReader(new InputStreamReader(input));
        //写入流
        StringWriter sw=new StringWriter();
        //数据缓冲区
        String line=null;
        while((line=br.readLine())!=null)
        {
            sw.write(line);
        }
        sw.close();
        br.close();
        return sw.toString();

    }
    @Override
    public void onClick(View view) {
        new Thread(){
            @Override
            public void run() {
                super.run();
                String url_str=et_url.getText().toString().trim();
                if(TextUtils.isEmpty(url_str))
                {
                    return;
                }
                //创建url对象
                try {
                    URL url=new URL(url_str);

                    //获取一个URLConnection对象
                    HttpURLConnection conn=(HttpURLConnection)url.openConnection();
                    //为URLConnection对象设置一些请求参数,请求方式,连接的超时时间
                    conn.setConnectTimeout(500);
                    conn.setRequestMethod("GET");
                    //获取url请求返回的数据前需要判断响应码,200:成功,300:跳转或重定向,400:错误,500:服务器异常
                    if(conn.getResponseCode()==200)
                    {
                        String response_str=streamToString(conn.getInputStream());
                        Message msg=Message.obtain();
                        msg.obj=response_str;
                        msg.what=0x123;
                        handler.sendMessage(msg);
                    }
                    //获取有效数据,并将获取的流数据解析成字符串
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext,"捕获到异常",Toast.LENGTH_SHORT).show();
                }
            }
        }.start();



    }
}
