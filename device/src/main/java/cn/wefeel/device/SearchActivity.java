package cn.wefeel.device;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.wefeel.device.base.BaseActivity;
import cn.wefeel.device.data.MyData;
import cn.wefeel.device.entity.Device;

public class SearchActivity extends BaseActivity {

    String mAllStation;
    String mAllOrgname;
    String[] mStates;//状态中文

    SearchView svSearch;
    TextView tvSelectedStation;
    TextView tvSelectedOrgname;
    ListView lvDevice;
    TextView tvHeader;
    TabHost thSearch;

    DeviceAdapter mAdapter = new DeviceAdapter();
    Cursor mCursor;
    Handler mHandler;

    int mState;
    String mStation;
    String mOrgname;
    String mKey = null;
    static final int MAXROWS = 100;
    String mHeader;

    private final ViewGroup.LayoutParams mProgressBarLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
    private final ViewGroup.LayoutParams mTipContentLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mAllStation = getString(R.string.all_station);//"（全部车站）";
        mAllOrgname = getString(R.string.all_orgname);//"（全部车间）";
        mStates = getResources().getStringArray(R.array.states);

        Toolbar toolbar = (Toolbar) findViewById(R.id.barTool);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        //选择车站按钮
        tvSelectedStation = (TextView) this.findViewById(R.id.tvSelectedStation);
        tvSelectedStation.setText(mAllStation);
        Button btnSelectStation = (Button) this.findViewById(R.id.btnSelectStation);
        btnSelectStation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyData db = new MyData();
                ArrayList<String> stationArray = db.getStationsArray();//获取所有车站
                stationArray.add(0, mAllStation);//加上“全部车站”到最前面
                final CharSequence[] stations = stationArray.toArray(new CharSequence[stationArray.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setIcon(android.R.drawable.ic_menu_search);//调用Android的默认搜索图标
                builder.setTitle(getString(R.string.title_select_station));
                builder.setItems(stations, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tvSelectedStation.setText(stations[which]);
                        search();
                    }
                });
                builder.show();
            }
        });
        //选择车间按钮
        tvSelectedOrgname = (TextView) this.findViewById(R.id.tvSelectedOrgname);
        tvSelectedOrgname.setText(mAllOrgname);
        Button btnSelectOrgname = (Button) this.findViewById(R.id.btnSelectOrgname);
        btnSelectOrgname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyData db = new MyData();
                ArrayList<String> orgnamesArray = db.getOrgnamesArray();//获取所有车间
                orgnamesArray.add(0, mAllOrgname);//加上“全部车间”到最前面
                final CharSequence[] orgnames = orgnamesArray.toArray(new CharSequence[orgnamesArray.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setIcon(android.R.drawable.ic_menu_search);//调用Android的默认搜索图标
                builder.setTitle(getString(R.string.title_select_orgname));
                builder.setItems(orgnames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tvSelectedOrgname.setText(orgnames[which]);
                        search();
                    }
                });
                builder.show();
            }
        });
        //初始化列表
        lvDevice = (ListView) this.findViewById(R.id.lvDevice);
        lvDevice.setEmptyView(this.findViewById(R.id.tvNoDevice));
        lvDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //显示明细
//                Cursor c = (Cursor) parent.getItemAtPosition(position);
//                String code = c.getString(c.getColumnIndex("code"));
                Device device = (Device) parent.getItemAtPosition(position);
                Intent intent = new Intent(view.getContext(), DetailActivity.class);
                intent.putExtra("code", device.code);
                startActivity(intent);
            }
        });
//        tvHeader = new TextView(this);
//        tvHeader.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//        tvHeader.setTextColor(Color.BLUE);
//        tvHeader.setOnClickListener(null);
//        lvDevice.addHeaderView(tvHeader);

        /**
         * 初始化"加载项"布局，此布局被添加到ListView的Footer中。
         */
        final LinearLayout mLoadLayout = new LinearLayout(this);
        mLoadLayout.setMinimumHeight(60);
        mLoadLayout.setGravity(Gravity.CENTER);
        mLoadLayout.setOrientation(LinearLayout.HORIZONTAL);
        /**
         * 向"加载项"布局中添加一个圆型进度条。
         */
        ProgressBar mProgressBar = new ProgressBar(this);
        mProgressBar.setPadding(0, 0, 15, 0);
        mLoadLayout.addView(mProgressBar, mProgressBarLayoutParams);
        /**
         * 向"加载项"布局中添加提示信息。
         */
        TextView mTipContent = new TextView(this);
        mTipContent.setText("加载中...");
        mLoadLayout.addView(mTipContent, mTipContentLayoutParams);
        /**
         * 将"加载项"布局添加到ListView组件的Footer中。
         */
//        lvDevice.addFooterView(mLoadLayout);


        //初始化tab标签
        thSearch = (TabHost) findViewById(R.id.thSearch);
        thSearch.setup();
        for (int i = 1; i < mStates.length; i++) {
            String key = String.valueOf(i);
            TabHost.TabSpec tab = thSearch.newTabSpec(key);
            tab.setIndicator(mStates[i]);
            tab.setContent(R.id.tab1);//因界面相同都指向同一个tab1
            thSearch.addTab(tab);
        }
        thSearch.setCurrentTab(3);//默认是0，如不切到3或其他，下面就不会真正执行setCurrentTab(0)
        thSearch.setCurrentTab(0);//setCurrentTab中有某个初始化操作，之后才能正常显示tab
        thSearch.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                search();
            }
        });

        //处理列表滚动操作
        lvDevice.setOnScrollListener(new AbsListView.OnScrollListener() {
            private boolean isBottom = false;    //用于标记是否到达顶端

            //listview的状态发送改变时执行
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (isBottom && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {

                    lvDevice.addFooterView(mLoadLayout);//显示加载中
                    final AbsListView myview = view;
                    new Thread(){
                        @Override
                        public void run() {
                            MyData myData = new MyData();
                            List<Device> list = myData.loadDevice(myview.getCount() - 1, MAXROWS, mState, mStation, mOrgname, mKey);
                            mAdapter.getList().addAll(list);//因为加了headerview所以不能用lvDeivce.getAdapter，多次转换还不如用mAdapter
                            isBottom = false;
                            mHandler.sendEmptyMessage(Messages.REFRESH_LIST);
//                            mAdapter.notifyDataSetChanged();
//                            lvDevice.removeFooterView(mLoadLayout);//不显示加载中
                            super.run();
                        }
                    }.start();
                    //可以不用线程直接执行，没啥影响
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            MyData myData = new MyData();
//                            List<Device> list = myData.loadDevice(myview.getCount() - 1, MAXROWS, mState, mStation, mOrgname, mKey);
//                            mAdapter.getList().addAll(list);//因为加了headerview所以不能用lvDeivce.getAdapter，多次转换还不如用mAdapter
//                            mAdapter.notifyDataSetChanged();
//                            isBottom = false;
//                            lvDevice.removeFooterView(mLoadLayout);//不显示加载中
//                        }
//                    }, 0);//设置延时大点就能看到footerview
                }
            }

            //在滚动的过程中不断执行
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                System.out.println(firstVisibleItem + ":" + visibleItemCount + ":" + totalItemCount);
                if (firstVisibleItem + visibleItemCount == totalItemCount) {
                    isBottom = true;
                } else {
                    isBottom = false;
                }
            }
        });

        //处理线程中发来的消息刷新UI
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == Messages.REFRESH_ALL) {    //刷新界面
                    for (int i = 0; i < thSearch.getTabWidget().getTabCount(); i++) {
                        TextView tvTitle = (TextView) thSearch.getTabWidget().getChildTabViewAt(i).findViewById(android.R.id.title);
                        tvTitle.setText(mStates[i + 1]);
                    }
//                    tvHeader.setText(mHeader);
                    lvDevice.setAdapter(mAdapter);
                }else if(msg.what==Messages.REFRESH_LIST){//加载列表
                    mAdapter.notifyDataSetChanged();
                    lvDevice.removeFooterView(mLoadLayout);//不显示加载中
                }
                super.handleMessage(msg);
            }
        };

        search();//填充listview
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void search() {
        try {
//            Log.e(TAG,"查询时间："+String.valueOf(System.nanoTime()-start));
//            List<HashMap<String, Object>> mapList = (new MyData()).loadDevice();效率太低不能用
//            SimpleAdapter adapter = new SimpleAdapter(this.getContext(), mapList, R.layout.item_device, from, to);
            mState = Integer.valueOf(thSearch.getCurrentTabTag());
            mStation = tvSelectedStation.getText().toString().equals(mAllStation) ? null : tvSelectedStation.getText().toString();
            mOrgname = tvSelectedOrgname.getText().toString().equals(mAllOrgname) ? null : tvSelectedOrgname.getText().toString();
            mKey = null;
            if (svSearch != null) {//search()如在菜单生成前调用就会null
                mKey = svSearch.getQuery().toString();
            }

//            long start = System.nanoTime();
//            Log.e(TAG,"填充时间："+String.valueOf(System.nanoTime()-start));
            //用Cursor方式填充，效率较高
//            if (mCursor != null) mCursor.close();
//            mCursor = (new MyData()).queryDevice(Integer.valueOf(mState), mStation, mOrgname, mKey);
//            String[] from = {"code", "station", "orgname", "type", "name", "producer", "model", "online"};
//            int[] to = {R.id.tvCode, R.id.tvStation, R.id.tvOrgname, R.id.tvType, R.id.tvName, R.id.tvProducer, R.id.tvModel, R.id.tvOnline};
//            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.item_device, mCursor, from, to, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
//            lvDevice.setAdapter(adapter);

            //线程操作，用自定义Adapter和少量数据逐步填充，效率较高，实际延时与Cursor差不多
            new Thread() {
                @Override
                public void run() {
                    MyData myData = new MyData();
                    //统计各种状态的符合数量
                    for (int i = 0; i < mStates.length; i++) {
                        long count = myData.countDevice(i, mStation, mOrgname, mKey);
                        mStates[i] = getResources().getStringArray(R.array.states)[i] + "\n(" + count + ")";
                    }
                    //加载前50条数据
                    List<Device> list = myData.loadDevice(0, MAXROWS, mState, mStation, mOrgname, mKey);
                    mAdapter.getList().clear();
                    if( list!=null ) {
                        mAdapter.getList().addAll(list);
                    }
                    mHandler.sendEmptyMessage(Messages.REFRESH_ALL);
                }
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        //实现搜索功能
        MenuItem menuItem = menu.findItem(R.id.action_search);//
        svSearch = (SearchView) MenuItemCompat.getActionView(menuItem);//加载searchview
        svSearch.setSubmitButtonEnabled(true);//设置是否显示搜索按钮
        svSearch.setQueryHint(getString(R.string.hint_search));//设置提示信息
        svSearch.setIconifiedByDefault(true);//设置搜索默认为图标
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (mCursor != null) mCursor.close();
        super.onDestroy();
    }


    //以下为改进ListView加载而新增
    private class DeviceAdapter extends BaseAdapter {
        private List<Device> mDeviceList = new ArrayList<>();
        private int[] colors = {R.color.white, R.color.lightest_gray};//隔行颜色

        public List<Device> getList() {
            return mDeviceList;
        }

        @Override
        public int getCount() {
            return mDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return mDeviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mDeviceList.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;//用holder可免去重复findView的过程，提高效率;不用也行
            if (convertView == null) {
                convertView = SearchActivity.this.getLayoutInflater().inflate(R.layout.item_device, null);
                holder = new ViewHolder();
                holder.tvName = ((TextView) convertView.findViewById(R.id.tvName));
                holder.tvCode = ((TextView) convertView.findViewById(R.id.tvCode));
                holder.tvStation = ((TextView) convertView.findViewById(R.id.tvStation));
                holder.tvOrgname = ((TextView) convertView.findViewById(R.id.tvOrgname));
                holder.tvType = ((TextView) convertView.findViewById(R.id.tvType));
                holder.tvProducer = ((TextView) convertView.findViewById(R.id.tvProducer));
                holder.tvModel = ((TextView) convertView.findViewById(R.id.tvModel));
                holder.tvOnline = ((TextView) convertView.findViewById(R.id.tvOnline));
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Device device = mDeviceList.get(position);
            holder.tvName.setText(device.name);
            holder.tvCode.setText(device.code);
            holder.tvStation.setText(device.station);
            holder.tvOrgname.setText(device.orgname);
            holder.tvType.setText(device.type);
            holder.tvProducer.setText(device.producer);
            holder.tvModel.setText(device.model);
            holder.tvOnline.setText(device.online);
            //隔行换色
            convertView.setBackgroundResource(colors[position % 2]);
            return convertView;
        }
    }

    static class ViewHolder {
        public TextView tvName;
        public TextView tvCode;
        public TextView tvStation;
        public TextView tvOrgname;
        public TextView tvType;
        public TextView tvProducer;
        public TextView tvModel;
        public TextView tvOnline;
    }

    public class Messages{
        public static final int REFRESH_ALL=0;
        public static final int REFRESH_LIST=1;
    }

}
