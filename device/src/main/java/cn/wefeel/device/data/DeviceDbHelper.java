package cn.wefeel.device.data;

import android.util.Log;

import org.xutils.DbManager;
import org.xutils.x;

import cn.wefeel.device.Constants;

/**
 * 数据库DbHelper.
 */

public class DeviceDbHelper {
    private DbManager.DaoConfig daoConfig;
    private static DbManager db;
    private final String DB_NAME = "device.db";    //数据库名
    private final int VERSION = 2; //数据库版本号

    private DeviceDbHelper() {
        daoConfig = new DbManager.DaoConfig()
                .setDbName(DB_NAME)
                .setDbVersion(VERSION)
                .setDbOpenListener(new DbManager.DbOpenListener() {
                    @Override
                    public void onDbOpened(DbManager db) {
                        db.getDatabase().enableWriteAheadLogging();  //开启WAL, 对写入加速提升巨大(作者原话)
                    }
                })
                .setDbUpgradeListener(new DbManager.DbUpgradeListener() {
                    @Override
                    public void onUpgrade(DbManager db, int oldVersion, int newVersion) {
                        //数据库升级操作
                        Log.e(Constants.TAG,"数据库版本号升级到"+VERSION);
                        if(oldVersion==1) {
                            try {
                                Log.e(Constants.TAG,"oldversion="+oldVersion+"   newversion="+newVersion);
                                db.execNonQuery("CREATE INDEX stateindex ON Device(state)");
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                });
        db = x.getDb(daoConfig);
    }

    public static DbManager getInstance() {
        if (db == null) {
            DeviceDbHelper databaseOpenHelper = new DeviceDbHelper();
        }
        return db;
    }
}
