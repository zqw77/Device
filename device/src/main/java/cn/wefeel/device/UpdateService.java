package cn.wefeel.device;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.JsonReader;
import android.util.JsonToken;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.wefeel.device.data.MyData;

public class UpdateService extends Service {
    private Context mContext;
    //	 String[] urls={"http://59.63.127.197/ncdwdbqgl/Files/TempFiles/line.txt"
//	 ,"http://59.63.127.197/ncdwdbqgl/Files/TempFiles/sector.txt"
//	 ,"http://59.63.127.197/ncdwdbqgl/Files/TempFiles/device.txt"
//	 ,"http://59.63.127.197/ncdwdbqgl/Files/TempFiles/log.txt"};
    //"http://59.63.127.197/ncdwdbqgl/Files/TempFiles/device.txt"
    //"http://59.63.127.197/ncdwdbqgl/Files/TempFiles/log.txt"
    String[] names = {Constants.DEVICE_FILE_NAME, Constants.LOG_FILE_NAME};
    public boolean isCanceled = false;// 取消更新

    @Override
    public IBinder onBind(Intent intent) {
        return new UpdateBinder();
    }

    public class UpdateBinder extends Binder {
        public UpdateService getService() {
            return UpdateService.this;
        }
    }

    public interface OnCallback {
        void onProgress(int progress);

        void onMessage(String message);

        void onDownloadSuccess();

        void onImportSuccess();
    }

    private OnCallback onCallback;

    public void setOnCallback(OnCallback onCallback) {
        this.onCallback = onCallback;
    }

    @Override
    public void onCreate() {
        mContext = this;
    }

    public boolean isRunning() {
        if (mImportThread != null)
            return mImportThread.isAlive();
        return false;
    }

    private ImportThread mImportThread;

    public void start() {
        mImportThread = new ImportThread();
        mImportThread.start();
    }

    private class ImportThread extends Thread {
        @Override
        public void run() {
            try {
                for (int i = 0; i < names.length; i++) {
                    downloadFile(Constants.DOWNLOAD_PATH, names[i]);
                }
                onCallback.onDownloadSuccess();
            } catch (Exception e) {
                onCallback.onMessage("未知错误！" + e.getLocalizedMessage() + "\n");
            }
            importDevice();
            importLog();
        }

        private void downloadFile(String url, String fileName) throws Exception {
            onCallback.onMessage("下载" + fileName);
            try {
                // 创建连接
                HttpURLConnection conn = (HttpURLConnection) new URL(url + fileName).openConnection();
                conn.connect();
                // 获取文件大小
                int length = conn.getContentLength();
                // 创建输入流
                InputStream is = conn.getInputStream();

                // 改为存储在应用程序目录中
                File dir = mContext.getDir("remote", Context.MODE_PRIVATE);

                // 判断文件目录是否存在
                if (!dir.exists()) {
                    dir.mkdir();
                }
                File file = new File(dir.getPath(), fileName);

                FileOutputStream fos = new FileOutputStream(file);
                int count = 0;
                byte buf[] = new byte[4048];
                do {
                    int numread = is.read(buf);
                    count += numread;
                    onCallback.onProgress(count * 100 / length);// 更新进度
                    if (numread <= 0) {
                        onCallback.onMessage("完成" + fileName);
                        fos.close();
                        is.close();
                        break;
                    }
                    fos.write(buf, 0, numread);// 写入文件
                } while (!isCanceled);// 点击取消就停止下载.
                if (isCanceled)    //如果是主动取消就删除文件
                    file.delete();
            } catch (Exception e) {
                throw (e);
            }
        }

        private void importDevice() {
            MyData db = new MyData();
            if (!isCanceled) {
                onCallback.onMessage("正在导入设备数据");
                db.zapDevice();
                FileInputStream inputStream = null;
                InputStreamReader reader = null;
                JsonReader jsonReader = null;
                try {
                    long start = System.currentTimeMillis();
                    File dir = mContext.getDir("remote", Context.MODE_PRIVATE);
                    File file = new File(dir.getPath(), Constants.DEVICE_FILE_NAME);
                    inputStream = new FileInputStream(file);
                    int length = inputStream.available();
                    int estimate = (int) (length / 250) + 1; //估计的行数，按每行250个字节算
                    int interval = estimate / 100 + 1;

                    reader = new InputStreamReader(inputStream, "utf-8");//必须用InputStreadmReader，直接用InputStream也能读但中文会乱码
                    jsonReader = new JsonReader(reader);
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String name = jsonReader.nextName();
                        if (name.equals("total")) {
                            estimate = jsonReader.nextInt();
                            interval = estimate / 100 + 1;
                        } else if (name.equals("rows")) {
                            int success = 0, codeisnull = 0, amount = 0;
                            ContentValues values = new ContentValues();
                            jsonReader.beginArray();
                            while (jsonReader.hasNext()) {
                                if (amount / interval * interval == amount) { //每1%才发一次消息，避免发过多的消息
                                    onCallback.onProgress(amount * 100 / estimate);
                                }
                                amount++;
                                values.clear();
                                jsonReader.beginObject();
                                while (jsonReader.hasNext()) {
                                    String jsonName = jsonReader.nextName();
                                    if (jsonReader.peek() == JsonToken.NULL) {
                                        jsonReader.skipValue();
                                        continue;
                                    }
                                    values.put(jsonName, jsonReader.nextString().trim());
                                }
                                jsonReader.endObject();
                                values.put("flag", 0);
                                if (values.containsKey("code") && !values.getAsString("code").isEmpty()) {
                                    int row = db.insertDevice(values);
                                    if (row > 0)
                                        success++;
                                    else
                                        onCallback.onMessage("重复编码：" + values.getAsString("code"));
                                } else {
                                    codeisnull++;
                                }
                                if (isCanceled)
                                    break;
                            }
                            jsonReader.endArray();
                            onCallback.onMessage("共" + String.valueOf(codeisnull) + "条设备编码为空");
                            String info = "保存设备数据" + String.valueOf(success) + "成功";
                            if (success != length)
                                info += "，" + String.valueOf(amount - success) + "失败！";
                            info += "\n耗时" + ((System.currentTimeMillis() - start)) / 1000 + "s";
                            onCallback.onMessage(info);
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                } catch (IOException e) {
                    onCallback.onMessage("数据文件错误！\n" + e.getLocalizedMessage());
                } catch (Exception e) {
                    onCallback.onMessage("未知错误！" + e.getLocalizedMessage());
                } finally {
                    close(jsonReader);
//                    if (jsonReader != null)
//                        try {
//                            jsonReader.close();
//                        } catch (IOException e) {
//                        }
                }
            }
        }

        private void importLog() {
            MyData db = new MyData();
            if (!isCanceled) {
                onCallback.onMessage("正在导入维修记录");
                db.zapLog();
                FileInputStream inputStream = null;
                InputStreamReader reader = null;
                JsonReader jsonReader = null;
                try {
                    File dir = mContext.getDir("remote", Context.MODE_PRIVATE);
                    File file = new File(dir.getPath(), Constants.LOG_FILE_NAME);
                    inputStream = new FileInputStream(file);
                    int length = inputStream.available();
                    int estimate = (int) (length / 250) + 1; //估计的行数，按每行250个字节算
                    int interval = estimate / 100 + 1;

                    reader = new InputStreamReader(inputStream, "utf-8");//必须用InputStreadmReader，直接用InputStream也能读但中文会乱码
                    jsonReader = new JsonReader(reader);
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        String name = jsonReader.nextName();
                        if (name.equals("total")) {
                            estimate = jsonReader.nextInt();
                            interval = estimate / 100 + 1;
                        } else if (name.equals("rows")) {
                            int success = 0, amount = 0;
                            ContentValues values = new ContentValues();
                            jsonReader.beginArray();
                            while (jsonReader.hasNext()) {
                                if (amount / interval * interval == amount) { //每1%才发一次消息，避免发过多的消息
                                    onCallback.onProgress(amount * 100 / estimate);
                                }
                                amount++;
                                values.clear();
                                jsonReader.beginObject();
                                while (jsonReader.hasNext()) {
                                    String jsonName = jsonReader.nextName();
                                    if (jsonReader.peek() == JsonToken.NULL) {
                                        jsonReader.skipValue();
                                        continue;
                                    }
                                    values.put(jsonName, jsonReader.nextString().trim());
                                }
                                jsonReader.endObject();
                                values.put("flag", 0);
                                int row = db.insertLog(values);
                                if (row > 0)
                                    success++;
                                else
                                    onCallback.onMessage("导入错误：" + values.getAsString("code"));
                                if (isCanceled)
                                    break;
                            }
                            jsonReader.endArray();
//			                onCallback.onMessage("共"+String.valueOf(codeisnull)+"条设备编码为空");
                            String info = "保存维修记录" + String.valueOf(success) + "成功";
                            if (success != length)
                                info += "，" + String.valueOf(amount - success) + "失败！";
                            onCallback.onMessage(info);
                            onCallback.onMessage("数据同步完成。请退出。");
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                    onCallback.onImportSuccess();
                } catch (IOException e) {
                    onCallback.onMessage("数据文件错误！\n" + e.getLocalizedMessage());
                } catch (Exception e) {
                    onCallback.onMessage("未知错误！" + e.getLocalizedMessage());
                } finally {
                    close(jsonReader);
//                    if (jsonReader != null)
//                        try {
//                            jsonReader.close();
//                        } catch (IOException e) {
//                        }
                }
            }
        }
        private void close(Closeable closeable){
            if(closeable!=null) {
                try {
                    closeable.close();
                } catch (IOException e) {
                }
            }
        }
/*		private void importData() {
            MyData myData = new MyData(mContext);
			myData.sector.zap();
			try {
				JSONArray ja = getSectors();
				onCallback.onMessage("正在导入机构名称");
				int success = 0;
				int length = ja.length();
				try {
					for (int i = 0; i < ja.length(); i++) {
//						sendMessage(PROGRESS, i * 100 / length + 1);
						JSONObject jo1 = ja.getJSONObject(i);
						ContentValues values = new ContentValues();
						values.put("name", jo1.getString("name").trim());
						values.put("parent", jo1.getString("pname").trim());
						long row = myData.sector.insert(values);
						if (row > 0)
							success++;
						// if (isCanceled)
						// break;
					}
				} catch (Exception e) {
					onCallback.onMessage("解析机构名称错误！\n");
				}
				String info = "保存机构名称" + String.valueOf(success) + "条成功";
				if (success != length)
					info += "，" + String.valueOf(length - success) + "条失败！";
				info += "\n";
				onCallback.onMessage( info);
			} catch (Exception e) {
				onCallback.onMessage("导入机构名称错误！\n"+e.getLocalizedMessage()+"\n");
			}
		}
        private JSONArray getSectors() throws Exception {
            JSONArray ja = new JSONArray(readFile("sector.txt"));
            return ja;
        }
		private String readFile(String fileName) throws IOException {
			File dir = mContext.getDir("remote", Context.MODE_PRIVATE);
			File file = new File(dir.getPath(),fileName);
			FileInputStream fis = new FileInputStream(file);
			int length = fis.available();
			byte[] buffer = new byte[length];
			fis.read(buffer);
			String res = EncodingUtils.getString(buffer, "UTF-8");
			fis.close();
			return res;
		}
*/
    }
}
