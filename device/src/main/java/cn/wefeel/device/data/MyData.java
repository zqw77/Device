package cn.wefeel.device.data;

import android.content.ContentValues;
import android.database.Cursor;

import org.xutils.DbManager;
import org.xutils.db.sqlite.SqlInfo;
import org.xutils.db.table.DbModel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wefeel.device.Constants;
import cn.wefeel.device.entity.Device;
import cn.wefeel.device.entity.Log;
import cn.wefeel.device.entity.Parameter;

/**
 * 运用DbHelper对数据库进行应用层操作
 */

public class MyData {
    private DbManager mDbManager;

    //接收构造方法初始化的DbManager对象
    public MyData() {
        mDbManager = DeviceDbHelper.getInstance();
    }

    /****************************************************************************************/
    //将Device实例存进数据库
    public int saveDevice(Device device) {
        try {
            mDbManager.save(device);
            return 1;
        } catch (Exception e) {
            android.util.Log.d(Constants.TAG, e.toString());
            return 0;
        }
    }

    /**
     * 根据code查找设备
     *
     * @param code
     * @return
     */
    public Device getDevice(String code) {
        Device device = null;
        try {
            device = mDbManager.selector(Device.class).where("code", "=", code).findFirst();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return device;
    }

    ////读取所有Device信息
    public List<Device> loadDevicexx() {
        List<Device> list = null;
        try {
            list = mDbManager.selector(Device.class).findAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 获取SimpleAdapter要用的Device信息（效率太低，不能用）
     *
     * @return
     */
    public List<HashMap<String, Object>> loadDevicexxx() {
        List<HashMap<String, Object>> mapList = new ArrayList<HashMap<String, Object>>();
        try {
            String[] posNames = {"", "车站", "道口", "机房", "调度所", "其他"};
            List<Device> list = mDbManager.selector(Device.class).limit(10000).findAll();//到几千条就很慢且费内存了，不适合findall
            Field[] fields = Device.class.getDeclaredFields();
            for (int i = 0; i < 100; i++) {//list.size(); i++) {
                if (!(list.get(i) instanceof Map)) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    for (int j = 0; j < fields.length; j++) {
                        map.put(fields[j].getName(), fields[j].get(list.get(i)));
                    }
                    map.put("posname", posNames[list.get(i).posflag]);
                    mapList.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapList;
    }

    public List<HashMap<String, Object>> getLog(String code) {
        List<HashMap<String, Object>> mapList = new ArrayList<HashMap<String, Object>>();
        try {
            List<Log> list = mDbManager.selector(Log.class).where("code","=",code).findAll();
            Field[] fields = Log.class.getDeclaredFields();
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Map)) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    for (int j = 0; j < fields.length; j++) {
                        map.put(fields[j].getName(), fields[j].get(list.get(i)));
                    }
                    mapList.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapList;
    }

    /**
     * 获取SimpleCursorAdapter要用的全部Log
     * @return
     */
    public List<HashMap<String, Object>> loadLog() {
        List<HashMap<String, Object>> mapList = new ArrayList<HashMap<String, Object>>();
        try {
            List<Log> list = mDbManager.selector(Log.class).findAll();
            Field[] fields = Log.class.getDeclaredFields();
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Map)) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    for (int j = 0; j < fields.length; j++) {
                        map.put(fields[j].getName(), fields[j].get(list.get(i)));
                    }
                    mapList.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mapList;
    }

    public ContentValues getCountByState(){
        ContentValues values=new ContentValues();
        Cursor c = null;
        try {
            c = mDbManager.execQuery("SELECT state,COUNT(*) FROM device GROUP BY state ORDER BY state");
            if (c != null) {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    values.put(c.getString(0),c.getInt(1));
//                    counts[c.getInt(0)] = c.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }
        return values;
    }
    ////按state计算每类设备的数量
    public int[] countByState() {
        int[] counts = new int[5];//state不能超过5，否则出错
        Cursor c = null;
        try {
            c = mDbManager.execQuery("SELECT state,COUNT(*) FROM device GROUP BY state ORDER BY state");
            if (c != null) {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    counts[c.getInt(0)] = c.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) c.close();
        }
        return counts;
    }

    public ArrayList<String> getStationsArray() {
        List<DbModel> dbModels=getDbModelsList("station");
        ArrayList<String> results=new ArrayList<String>();
        for (DbModel dbModel:dbModels) {
            results.add(dbModel.getString("value"));
        }
        return results;
    }

    public ArrayList<String> getOrgnamesArray() {
        List<DbModel> dbModels=getDbModelsList("orgname");
        ArrayList<String> results=new ArrayList<String>();
        for (DbModel dbModel:dbModels) {
            results.add(dbModel.getString("value"));
        }
        return results;
    }

    private List<DbModel> getDbModelsList(String columnName) {
        List<DbModel> dbModels = null;
        String sql = "SELECT DISTINCT " + columnName + " AS value FROM device WHERE flag<>3 ORDER BY " + columnName + " COLLATE LOCALIZED";
        try {
            dbModels = mDbManager.findDbModelAll(new SqlInfo(sql));
        } catch (Exception e) {
            dbModels=new ArrayList<DbModel>();
        }
        return dbModels;
    }

    ///查询设备
    public Cursor queryDevice(int state, String station, String orgname, String key) {
        String where = " WHERE a.flag<>3 AND state=" + state;
        if (station != null) {
            where += " AND station='" + station + "'"; //传递了station的情况都是非备品
        }
        if (orgname != null) {
            where += " AND orgname='" + orgname + "'"; //传递了orgname的情况都是备品
        }
        if (key != null) {
            where += " AND ((a.code LIKE '" + key + "%') OR (type LIKE '%" + key + "%') OR (name LIKE '%" + key + "%'))";
        }
        String sql = "SELECT CAST(a.code AS INT) AS _id,a.*," +
                "(CASE posflag WHEN 1 THEN '车站'" +
                " WHEN 2 THEN '道口'" +
                " WHEN 3 THEN '机房'" +
                " WHEN 4 THEN '调度所'" +
                " WHEN 5 THEN '其他'" +
                " ELSE '其他' END) AS posname" +
                " FROM device a"
                + where;

        Cursor c = null;
        try {
            c = mDbManager.execQuery(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c;
    }

    private void zap(Class<?> entityType) {
        try {
            mDbManager.delete(entityType);
//            int t = mDbManager.executeUpdateDelete("DELETE FROM "+entityType.getSimpleName());
//            android.util.Log.e(Constants.TAG, "删除 " + t);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void zapDevice() {
        zap(Device.class);
    }

    public void zapLog() {
        zap(Log.class);
    }

    public int saveLog(Log log) {
        try {
            mDbManager.save(log);
            return 1;
        } catch (Exception e) {
            android.util.Log.d(Constants.TAG, e.toString());
            return 0;
        }
    }

    public String getParameterValue(String name) {
        String value = "";
        try {
            Parameter parameter = mDbManager.selector(Parameter.class).where("name", "=", name).findFirst();
            value = parameter.value;
        } catch (Exception e) {
            android.util.Log.d(Constants.TAG, e.toString());
        }
        return value;
    }

    public void setParameterValue(String name, String value) {
        try {
            Parameter parameter = mDbManager.selector(Parameter.class).where("name", "=", name).findFirst();
            if (parameter == null) parameter = new Parameter();
            parameter.name = name;
            parameter.value = value;
            mDbManager.saveOrUpdate(parameter);
        } catch (Exception e) {
            android.util.Log.d(Constants.TAG, e.toString());
        }
    }

    public int insertDevice(ContentValues value) {
        Device device = new Device();
        device.code = (value.getAsString("code"));
        device.station = (value.getAsString("station").trim());
        device.type = (value.getAsString("type").trim());
        device.name = (value.getAsString("name"));
        device.producer = (value.getAsString("producer"));
        device.model = (value.getAsString("model"));
        device.unit = (value.getAsString("unit"));
        device.online = (value.getAsString("online"));
        device.life = (value.getAsString("life"));
        device.orgname = (value.getAsString("orgname").trim());
        device.state = (value.getAsInteger("state"));
        try {
            device.posflag = value.getAsInteger("posflag");
        } catch (Exception e) {
            device.posflag = 0;
            e.printStackTrace();
        }
        device.flag = (0);
        return saveDevice(device);
    }

    public int insertLog(ContentValues value) {
        Log log = new Log();
        log.id = (value.getAsString("id"));
        log.code = (value.getAsString("code"));
        log.orgname = (value.getAsString("orgname").trim());
        log.repairdate = (value.getAsString("repairdate").trim());
        log.repairperson = (value.getAsString("repairperson").trim());
        log.place = (value.getAsString("place"));
        log.content = (value.getAsString("content"));
        log.flag = (0);
        return saveLog(log);
    }
}
