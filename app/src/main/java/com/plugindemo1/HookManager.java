package com.plugindemo1;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Created by Dou on 2019/7/27.
 */
public class HookManager {
    private static final HookManager ourInstance = new HookManager();
    private Resources resources;
    private DexClassLoader loader;
    public PackageInfo packageInfo;

    public static HookManager getInstance() {
        return ourInstance;
    }

    private HookManager() {
    }


    //用来加载插件
    public void loadPlugin(Activity activity) {
        // 假如这里是从网络获取的插件 我们直接从sd卡获取 然后读取到我们的cache目录
        String pluginName = "plugin.apk";
        File filesDir = activity.getDir("plugin", activity.MODE_PRIVATE);
        String filePath = new File(filesDir, pluginName).getAbsolutePath();
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        FileInputStream is = null;
        FileOutputStream os = null;
        //读取的目录
        try {
            is = new FileInputStream(new File(Environment.getExternalStorageDirectory(), pluginName));
            //要输入的目录
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            File f = new File(filePath);
            if (f.exists()) {
                Toast.makeText(activity, "加载完成", Toast.LENGTH_LONG).show();
            }
            loadPathToPlugin(activity);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                os.close();
                is.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }


    }

    private void loadPathToPlugin(Activity activity) {
        File filesDir = activity.getDir("plugin", activity.MODE_PRIVATE);
        String name = "plugin.apk";
        String path = new File(filesDir, name).getAbsolutePath();

        //然后我们开始加载我们的apk 使用DexClassLoader
        File dexOutDir = activity.getDir("dex", activity.MODE_PRIVATE);
        loader = new DexClassLoader(path, dexOutDir.getAbsolutePath(), null, activity.getClassLoader());

        //通过PackAgemanager 来获取插件的第一个activity是哪一个
        PackageManager packageManager = activity.getPackageManager();
        packageInfo = packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES);


        //然后开始加载我们的资源 肯定要使用Resource 但是它是AssetManager创建出来的 就是AssertManager 有一个addAssertPath 这个方法 但是私有的 所有使用反射
        Class<?> assetManagerClass = AssetManager.class;
        try {
            AssetManager assertManagerObj = (AssetManager) assetManagerClass.newInstance();
            Method addAssetPathMethod = assetManagerClass.getMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            addAssetPathMethod.invoke(assertManagerObj, path);
            //在创建一个Resource
            resources = new Resources(assertManagerObj, activity.getResources().getDisplayMetrics(), activity.getResources().getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }

        parseReceivers(activity, path);
    }

    /**
     * 通过解析清单文件来 拿到静态广播并且进行注册
     *
     * @param activity
     * @param path
     */
    private void parseReceivers(Activity activity, String path) {
        try {
            //我们知道解析一个apk文件的入口就是PackageParse.parsePackage 这个方法
            //所以我们使用反射 来调用这个方法 最终得到了一个 PackageParse$Package 这个类
            Class<?> mPackageParseClass = Class.forName("android.content.pm.PackageParser");
            Method mParsePackageMethod = mPackageParseClass.getDeclaredMethod("parsePackage", File.class, int.class);
            Object mPackageParseObj = mPackageParseClass.newInstance();
            Object mPackageObj = mParsePackageMethod.invoke(mPackageParseObj, new File(path), PackageManager.GET_ACTIVITIES);

            //解析出来的receiver就存在PackageParse$Package 这个类里面的一个receivers集合里面
            Field mReceiversListField = mPackageObj.getClass().getDeclaredField("receivers");
            //然后得到反射得到这个属性的值 最终得到一个集合
            List mReceiverList = (List) mReceiversListField.get(mPackageObj);

            //接下来我们要拿到 IntentFilter 和name属性 这样才能反射创建对象，动态在宿主里面注册广播
            Class<?> mComponetClass = Class.forName("android.content.pm.PackageParser$Component");
            Field mIntentFields = mComponetClass.getDeclaredField("intents");

            //这两行是为了调用generateActivityInfo 而反射拿到的参数
            Class<?> mPackageParse$ActivityClass = Class.forName("android.content.pm.PackageParser$Activity");
            Class<?> mPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");


            Object mPackzgeUserStateObj = mPackageUserStateClass.newInstance();


            // 拿到generateActivityInfo这个方法
            Method mGeneReceiverInfo = mPackageParseClass.getMethod("generateActivityInfo", mPackageParse$ActivityClass, int.class, mPackageUserStateClass, int.class);

            Class<?> mUserHandlerClass = Class.forName("android.os.UserHandle");
            Method getCallingUserIdMethod = mUserHandlerClass.getDeclaredMethod("getCallingUserId");

            int userId = (int) getCallingUserIdMethod.invoke(null);


            //然后for循环 去拿到name和 intentFilter
            for (Object activityObj : mReceiverList) {
                //调用generateActivityInfo
                // 这个是我们要调用的方法的形参 public static final ActivityInfo generateActivityInfo(Activity a, int flags,PackageUserState state, int userId);
                //得到一个ActivityInfo
                ActivityInfo info = (ActivityInfo) mGeneReceiverInfo.invoke(mPackageParseObj, activityObj, 0, mPackzgeUserStateObj, userId);
                //拿到这个name 相当于我们在清单文件中Android:name 这样，是一个全类名，然后通过反射去创建 对象
                BroadcastReceiver broadcastReceiver = (BroadcastReceiver) getClassLoader().loadClass(info.name).newInstance();

                //在拿到IntentFilter
                List<? extends IntentFilter> intents = (List<? extends IntentFilter>) mIntentFields.get(activityObj);
                //然后直接调用registerReceiver方法发
                for (IntentFilter intentFilter : intents) {
                    activity.registerReceiver(broadcastReceiver, intentFilter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public ClassLoader getClassLoader() {
        return loader;
    }

    public Resources getResource() {
        return resources;
    }
}
