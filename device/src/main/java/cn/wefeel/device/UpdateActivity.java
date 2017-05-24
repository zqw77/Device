package cn.wefeel.device;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.FileOutputStream;

import cn.wefeel.device.UpdateService.OnCallback;
import cn.wefeel.device.base.BaseActivity;
import cn.wefeel.device.data.MyData;

public class UpdateActivity extends BaseActivity {
    private Context mContext;// 父Activity
    private TextView tvInfo;
    private ScrollView scrollView1;
    private ProgressBar pbDownload;
    private final int INFO = 4;
    private final int APPENDINFO = 6;
    private final int IMPORT_BEGIN = 5;
    private UpdateService downloadService;
    private String[] fileTimes = {"", ""};

    private Handler mHandler;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            downloadService = ((UpdateService.DownloadBinder) binder).getService();
            //注册回调接口来接收下载进度的变化  
            downloadService.setOnCallback(new OnCallback() {
                @Override
                public void onProgress(int progress) {
                    pbDownload.setProgress(progress);
                    //sendMessage(APPENDINFO,".");
                }

                @Override
                public void onMessage(String message) {
                    sendMessage(INFO, message);
                }

                public void onDownloadSuccess() {
                    sendMessage(IMPORT_BEGIN, null);
                }

                public void onImportSuccess() {
                    Intent intent = getIntent();
                    fileTimes = intent.getStringArrayExtra("fileTimes");
                    MyData db = new MyData();
                    db.setParameterValue(Constants.DEVICE_FILE_NAME, fileTimes[0]);
                    db.setParameterValue(Constants.LOG_FILE_NAME, fileTimes[1]);
                }
            });
        }

        public void onServiceDisconnected(ComponentName arg0) {
            downloadService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        mContext = this;

        tvInfo = (TextView) this.findViewById(R.id.tvInfo);
        scrollView1 = (ScrollView) this.findViewById(R.id.scrollView1);
        pbDownload = (ProgressBar) this.findViewById(R.id.pbDownload);

        //处理消息，用于刷新界面
        mHandler = new Handler(this.getMainLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case INFO:
                        tvInfo.append((String) msg.obj + "\n");
                        scrollView1.post(new Runnable() {
                            public void run() {
                                scrollView1.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });// 滚动到最后
                        break;
                    case APPENDINFO:
                        tvInfo.append((String) msg.obj);
                        scrollView1.post(new Runnable() {
                            public void run() {
                                scrollView1.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                        break;
                    case IMPORT_BEGIN:
                        downloadService.startImport();//开始导入
                        break;
                    default:
                        break;
                }
            }
        };
        //绑定服务，此时未运行服务的动作
        Intent intent = new Intent(mContext, UpdateService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //检查是否有已下载的数据文件
//		File dir = mContext.getDir("remote", Context.MODE_PRIVATE | Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
//		File apkfile = new File(dir.getPath(), "device.txt");
//		if (apkfile.exists()) {
//			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//			builder.setMessage("发现有上次未完成同步的数据，是否重新下载？")
//			.setPositiveButton("重新下载数据", new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int which) {
//					confirmDownload();
//				}
//			}).setNegativeButton("用上次的数据", new DialogInterface.OnClickListener() {
//				public void onClick(DialogInterface dialog, int which) {
//					downloadService.startImport();//开始导入
//				}
//			}).setOnCancelListener(new OnCancelListener(){
//				@Override
//				public void onCancel(DialogInterface arg0) {
//					finish();
//				}
//			}).create().show();
//		}else{
//			confirmDownload();
//		}
        confirmDownload();
    }

    private void confirmDownload() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.hint_traffic)
                .setPositiveButton(R.string.hint_traffic_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        downloadService.startDownload();//开始下载
                    }
                })
                .setNegativeButton(R.string.hint_traffic_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface arg0) {
                finish();
            }
        }).create().show();
    }

    @Override
    protected void onDestroy() {
        //写日志文件
        try {
            // 打开文件获取输出流，文件不存在则自动创建
            FileOutputStream fos = openFileOutput(Constants.UPDATELOG_FILE_NAME, Context.MODE_PRIVATE);
            fos.write(tvInfo.getText().toString().getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (downloadService != null) {
            downloadService.isCanceled = true;
            unbindService(mConnection);
        }
        super.onDestroy();
    }

    private void sendMessage(int what, Object obj) {
        Message message = Message.obtain();
        message.what = what;
        message.obj = obj;
        mHandler.sendMessage(message);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (downloadService.isRunning > 0) {
                    AlertDialog.Builder build = new AlertDialog.Builder(this);
                    build.setTitle(R.string.hint_attention)
                            .setMessage(R.string.hint_download_exit)
                            .setPositiveButton(R.string.hint_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    downloadService.isCanceled = true;
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.hint_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                } else {
                    return super.onKeyDown(keyCode, event);
                }
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }
        return false;

    }


}
