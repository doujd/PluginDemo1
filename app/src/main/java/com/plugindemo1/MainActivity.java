package com.plugindemo1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    static final String ACTION = "com.plugindemo1.receiver";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.mBtnLoadPlugin)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadPlugin();
                    }
                });


        findViewById(R.id.mBtnStartProxy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProxy();
            }

        });
        findViewById(R.id.mBtnSendBroadCast).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "我是宿主  插件插件!收到请回答!!  1", Toast.LENGTH_SHORT).show();
                Intent newintent = new Intent();
                newintent.setAction("com.plugindemo1.receiver.MainActivity");
                sendBroadcast(newintent);
            }
        });
        registerReceiver(mReceiver, new IntentFilter(ACTION));
        //在这里注册一个广播 用来和插件进行静态广播交互的
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, " 我是宿主，收到你的消息,握手完成!", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 加载插件
     */
    private void loadPlugin() {
        HookManager.getInstance().loadPlugin(this);

    }

    /**
     * 跳转插件
     */
    private void startProxy() {
        //
        Intent intent = new Intent(this, ProxyActivity.class);
        intent.putExtra("ClassName", HookManager.getInstance().packageInfo.activities[0].name);
        startActivity(intent);
    }
}
