package cn.wefeel.device;

import android.app.Application;

import org.xutils.x;

/**
 * 因为需要初始化xutils3所以需要这个文件.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        x.Ext.init(this); //初始化xutils3
        x.Ext.setDebug(BuildConfig.DEBUG); // 是否输出debug日志
    }
}
