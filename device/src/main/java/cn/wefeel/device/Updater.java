package cn.wefeel.device;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import cn.wefeel.device.data.MyData;

/**
 * 检查是否有新的数据并进行更新类
 *
 * @author Administrator
 */
public class Updater {
    private static Context mContext;//父Activity
    private static HashMap<String, String> mHashMap;//保存解析的信息
    private static String[] fileTimes = {"", ""};

    /**
     * 用线程检查是否有新的数据并进行更新
     */
    public static void check(final Context context, final boolean isKeepSilent) {

        mContext = context;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                boolean haveNewData = false;
                try {
                    URL url = new URL(Constants.DOWNLOAD_PATH);// 创建页面的连接地址。
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    InputStream inStream = connection.getInputStream();// 从输入流获取页面数据
                    mHashMap = parseInput(inStream);// 得到解析信息，
                    fileTimes[0] = mHashMap.get(Constants.DEVICE_FILE_NAME);
                    fileTimes[1] = mHashMap.get(Constants.LOG_FILE_NAME);
                    MyData mDb = new MyData();
                    if (!mDb.getParameterValue(Constants.DEVICE_FILE_NAME).equals(fileTimes[0])
                            || !mDb.getParameterValue(Constants.LOG_FILE_NAME).equals(fileTimes[1])) {
                        haveNewData = true;
//                        Log.e(Constants.TAG,"新数据"+fileTimes[0]+ " "+fileTimes[1]);
                    }
                    if (!isKeepSilent || haveNewData)
                        showDialog(haveNewData);    //显示对话框
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Looper.loop();
            }
        };
        new Thread(runnable).start();
    }

    /**
     * 显示数据更新对话框
     */
    private static void showDialog(boolean haveNewData) {
        String message, positiveText, negativeText;
        if (haveNewData) {
            message = mContext.getString(R.string.hint_downnew);
            positiveText = mContext.getString(R.string.hint_downnew_ok);
            negativeText = mContext.getString(R.string.hint_downnew_cancel);
        } else {
            message = mContext.getString(R.string.hint_downagain);
            positiveText = mContext.getString(R.string.hint_downagain_ok);
            negativeText = mContext.getString(R.string.hint_downnew_cancel);
        }
        Builder builder = new Builder(mContext);
        builder.setTitle(R.string.title_download);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent intent = new Intent(mContext, UpdateActivity.class); //
                intent.putExtra("fileTimes", fileTimes);
                ((MainActivity) mContext).startActivityForResult(intent, 201);
            }
        });
        builder.setNegativeButton(negativeText, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    /**
     * 解析返回的页面格式，得到文件时间。页面格式见http://59.63.127.197/ncdwdbqgl/Files/TempFiles/
     */
    public static HashMap<String, String> parseInput(InputStream inStream) throws Exception {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        String x = InputStreamToString(inStream, "UTF-8");
        String lines[] = x.split("<br>");
        for (String line : lines) {
            line = line.trim();
            if (line.indexOf(Constants.DEVICE_FILE_NAME) >= 0) {
                String s[] = line.split("\\s+");
                hashMap.put(Constants.DEVICE_FILE_NAME, s[0] + " " + s[1]);
            } else if (line.indexOf(Constants.LOG_FILE_NAME) >= 0) {
                String s[] = line.split("\\s+");
                hashMap.put(Constants.LOG_FILE_NAME, s[0] + " " + s[1]);
            } else {
            }
        }
        return hashMap;
    }

    /**
     * 将InputStream转换成某种字符编码的String
     *
     * @param in
     * @param encoding
     * @return
     * @throws Exception
     */
    public static String InputStreamToString(InputStream in, String encoding) throws Exception {
        int BUFFER_SIZE = 4096;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
            outStream.write(data, 0, count);
        data = null;
        return new String(outStream.toByteArray(), encoding);
    }

}
