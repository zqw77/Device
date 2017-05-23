package cn.wefeel.library.softwareupgrader;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 2017.3.24是否显示提示信息由true,false改Upgrader.KEEP_SILENT和Upgrader.NO_KEEP_SILENT
 * 2017.1.31改用Android Studio环境
 * 2015.7.31更新，不再要求手机上有SD卡
 * 软件自动更新模块。 2015.4.28
 * 注意：本升级程序要求手机上有 SD卡。
 * 用法:
 * 1、在要使用升级模块的项目properties->Android中引用对本项目Library（Android Studio下改为在build.gradle中增加compile project(path: ':softwareupgrader') ）
 * 3、修改upgrade.xml文件并放到网络远端的地址；
 * 4、然后在你的程序的升级buttton下执行以下语句。
 * Upgrader.checkUpgrade(MainActivity.this,"http://bcs.duapp.com/wefeel/DeviceManager/upgrade.xml",false);
 */
public class Upgrader {
    public static final boolean KEEP_SILENT = true;
    public static final boolean NO_KEEP_SILENT = false;
    private static Context mContext;//父Activity
    private static HashMap<String, String> mHashMap;//保存解析的XML信息
    private static String mSavePath;//下载保存路径
    private static String mNoUpgrade, mUpgrading, mUpgradeNow, mUpgradeLater, mUpgradeCancel;//各种提示信息和按钮

    private static ProgressBar mProgressBar;//进度条
    private static int mProgress;//进度
    private static Dialog mDownloadDialog;//提示框
    private static boolean mIsCanceled = false;//取消更新

    private static Handler mHandler;
    private static final int DOWNLOADING = 1;
    private static final int DOWNLOAD_FINISH = 2;

    /**
     * 默认:已经是最新版本
     *
     * @param resId
     */
    public static void setNoUpgradeHint(int resId) {
        setNoUpgradeHint(mContext.getString(resId));
    }

    /**
     * 默认:已经是最新版本
     *
     * @param hint
     */
    public static void setNoUpgradeHint(String hint) {
        mNoUpgrade = hint;
    }

    /**
     * 默认:正在更新
     *
     * @param resId
     */
    public static void setUpgradingHint(int resId) {
        setUpgradingHint(mContext.getString(resId));
    }

    /**
     * 默认:正在更新
     *
     * @param hint
     */
    public static void setUpgradingHint(String hint) {
        mUpgrading = hint;
    }

    /**
     * 默认:现在更新
     *
     * @param resId
     */
    public static void setNowLabel(int resId) {
        setNowLabel(mContext.getString(resId));
    }

    /**
     * 默认:现在更新
     *
     * @param label
     */
    public static void setNowLabel(String label) {
        mUpgradeNow = label;
    }

    /**
     * 默认:暂不更新
     *
     * @param resId
     */
    public static void setLaterLabel(int resId) {
        setLaterLabel(mContext.getString(resId));
    }

    /**
     * 默认:暂不更新
     *
     * @param label
     */
    public static void setLaterLabel(String label) {
        mUpgradeLater = label;
    }

    /**
     * 默认:取消
     *
     * @param resId
     */
    public static void setCancelLabel(int resId) {
        setCancelLabel(mContext.getString(resId));
    }

    /**
     * 默认:取消
     *
     * @param label
     */
    public static void setCancelLabel(String label) {
        mUpgradeCancel = label;
    }

    /**
     * 用线程检查是否有更新并进行升级
     */
    public static void check(final Context context, final String url, final boolean isKeepSilent) {
        mContext = context;
        setNoUpgradeHint(R.string.upgrade_no);
        setUpgradingHint(R.string.upgrading);
        setNowLabel(R.string.upgrade_now);
        setLaterLabel(R.string.upgrade_later);
        setCancelLabel(R.string.upgrade_cancel);

        mHandler = new Handler(mContext.getMainLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DOWNLOADING:// 正在下载
                        mProgressBar.setProgress(mProgress);// 设置进度
                        break;
                    case DOWNLOAD_FINISH:// 安装文件
                        installApk();
                        break;
                    default:
                        break;
                }
            }

            ;
        };
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                if (canUpgrade(url)) {
                    // 显示提示对话框
                    showNoticeDialog();
                } else {
                    if (!isKeepSilent)
                        Toast.makeText(mContext, mNoUpgrade, Toast.LENGTH_LONG).show();
                }
                Looper.loop();
            }
        };
        new Thread(runnable).start();
    }

    /**
     * 检查软件是否有更新版本
     */
    private static boolean canUpgrade(final String urlAddress) {
        int localCode = getVersionCode(mContext); // 获取当前软件版本
        URL url;// 定义网络中version.xml的连接
        try { // 一个测试
            url = new URL(urlAddress);// 创建version.xml的连接地址。
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream inStream = connection.getInputStream();// 从输入流获取数据
            //ParseXmlService service = new ParseXmlService();// 将数据通过ParseXmlService这个类解析
            //mHashMap = service.parseXml(inStream);// 得到解析信息
            mHashMap = parseXml(inStream);// 得到解析信息
            // 版本判断
            int remoteCode = Integer.valueOf(mHashMap.get("version"));
            if (remoteCode > localCode)
                return true;
        } catch (Exception e) {
            e.printStackTrace();// 测试失败
        }
        return false;
    }

    /**
     * 获取本地软件版本号
     *
     * @param context
     * @return
     */
    private static int getVersionCode(Context context) {
        int versionCode = 0;
        try {
            // 获取软件版本号，对应AndroidManifest.xml下android:versionCode
            versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 显示软件更新对话框
     */
    private static void showNoticeDialog() {
        // 构造对话框
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(mHashMap.get("title"));
        builder.setMessage(mHashMap.get("message"));
        // 更新
        builder.setPositiveButton(mUpgradeNow, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 显示下载对话框
                showDownloadDialog();
            }
        });
        // 稍后更新
        builder.setNegativeButton(mUpgradeLater, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        Dialog noticeDialog = builder.create();
        noticeDialog.show();
    }

    /**
     * 显示软件下载对话框
     */
    private static void showDownloadDialog() {
        // 构造软件下载对话框
        AlertDialog.Builder builder = new Builder(mContext);
        builder.setTitle(mUpgrading);
        // 给下载对话框增加进度条
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.progress_upgrade, null);
        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar1);
        builder.setView(v);
        // 取消更新
        builder.setNegativeButton(mUpgradeCancel, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // 设置取消状态
                mIsCanceled = true;
            }
        });
        mDownloadDialog = builder.create();
        mDownloadDialog.show();
        // 启动新线程下载软件
        new downloadApkThread().start();
    }

    /**
     * 下载文件线程
     */
    private static class downloadApkThread extends Thread {
        @Override
        public void run() {
            InputStream is = null;
            FileOutputStream fos = null;
            HttpURLConnection conn = null;
            try {
                if (true) {
                    // 判断SD卡是否存在，并且是否具有读写权限
//				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                    URL url = new URL(mHashMap.get("url"));
                    // 创建连接
                    conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    // 获取文件大小
                    int length = conn.getContentLength();
                    // 创建输入流
                    is = conn.getInputStream();

                    // 获得存储卡的路径
//					String sdpath = Environment.getExternalStorageDirectory() + "/";
//					mSavePath = sdpath + "download";
//					File dir = new File(mSavePath);
                    //改为存储在应用程序目录中
                    File dir = mContext.getDir("upgrade", Context.MODE_PRIVATE);
                    mSavePath = dir.getPath();

                    // 判断文件目录是否存在
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    File apkFile = new File(mSavePath, mHashMap.get("name"));

                    fos = new FileOutputStream(apkFile);
                    int count = 0;
                    // 缓存
                    byte buf[] = new byte[1024];
                    // 写入到文件中
                    do {
                        int numread = is.read(buf);
                        count += numread;
                        // 计算进度条位置
                        mProgress = (int) (((float) count / length) * 100);
                        // 更新进度
                        mHandler.sendEmptyMessage(DOWNLOADING);
                        if (numread <= 0) {
                            // 下载完成
                            mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                            break;
                        }
                        // 写入文件
                        fos.write(buf, 0, numread);
                    } while (!mIsCanceled);// 点击取消就停止下载.
                    fos.close();
                    is.close();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {//发现上次升级中断后必须要清程序缓存才能再次升级，因此加上finally操作试试
                if (conn != null) conn.disconnect();
                try {
                    if (fos != null) fos.close();
                } catch (Exception e) {
                }
            }
            // 取消下载对话框显示
            mDownloadDialog.dismiss();
        }
    }

    ;

    /**
     * 安装APK文件
     */
    private static void installApk() {
        //修改权限，让升级文件能够执行
        String[] command = {"chmod", "777", mSavePath + "/" + mHashMap.get("name")};
        ProcessBuilder builder = new ProcessBuilder(command);
        try {
            builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File apkfile = new File(mSavePath, mHashMap.get("name"));
        if (!apkfile.exists()) {
            return;
        }
        // 通过Intent安装APK文件
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
        mContext.startActivity(i);
    }

    /**
     * 解析xml文件
     */
    public static HashMap<String, String> parseXml(InputStream inStream) throws Exception {
        HashMap<String, String> hashMap = new HashMap<String, String>();

        // 实例化一个文档构建器工厂
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 通过文档构建器工厂获取一个文档构建器
        DocumentBuilder builder = factory.newDocumentBuilder();
        // 通过文档通过文档构建器构建一个文档实例
        Document document = builder.parse(inStream);
        // 获取XML文件根节点
        Element root = document.getDocumentElement();
        // 获得所有子节点
        NodeList childNodes = root.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            // 遍历子节点
            Node childNode = (Node) childNodes.item(j);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) childNode;
                // 版本号
                if ("version".equals(childElement.getNodeName())) {
                    hashMap.put("version", childElement.getFirstChild().getNodeValue());
                }
                // 软件名称
                else if (("name".equals(childElement.getNodeName()))) {
                    hashMap.put("name", childElement.getFirstChild().getNodeValue());
                }
                // 下载地址
                else if (("url".equals(childElement.getNodeName()))) {
                    hashMap.put("url", childElement.getFirstChild().getNodeValue());
                } else if (("title".equals(childElement.getNodeName()))) {
                    hashMap.put("title", childElement.getFirstChild().getNodeValue());
                } else if (("message".equals(childElement.getNodeName()))) {
                    hashMap.put("message", childElement.getFirstChild().getNodeValue());
                }
            }
        }
        return hashMap;
    }

}
