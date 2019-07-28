package com.plugin_framework;

import android.content.Context;
import android.content.Intent;

/**
 * Created by Dou on 2019/7/28.
 */
public interface ProxyBroadCastInterface {

    void attch(Context context);

    void onReceive(Context context, Intent intent);
}
