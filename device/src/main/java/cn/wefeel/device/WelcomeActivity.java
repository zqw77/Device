package cn.wefeel.device;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.TextView;

import org.xutils.image.ImageOptions;
import org.xutils.x;

import cn.wefeel.device.base.BaseActivity;

//@ContentView(R.layout.activity_welcome)
public class WelcomeActivity extends BaseActivity {

//    @ViewInject(R.id.ivWelcome)
//    private ImageView ivWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //因为用了这个所以不能用@ContentView和@ViewInject
        final View view = View.inflate(this, R.layout.activity_welcome, null);
        setContentView(view);

        //显示版本号
        TextView tvVersion = (TextView) this.findViewById(R.id.tvVersion);
        tvVersion.setText(getVersionName(this));

        //用xutils3的图片加载功能，在页面上方随机显示网络图片welcome0-9，失败就显示本地图片
        String welcomeUrl=String.format(Constants.WELCOME_FILE,(int)(Math.random()*10));
        ImageView ivWelcome=(ImageView)this.findViewById(R.id.ivWelcome);
        ImageOptions options = new ImageOptions.Builder()
                // 是否忽略GIF格式的图片
                .setIgnoreGif(false)
                // 图片缩放模式
                .setImageScaleType(ImageView.ScaleType.CENTER_CROP)
                // 下载中显示的图片
                .setLoadingDrawableId(R.mipmap.highway)
                // 下载失败显示的图片
                .setFailureDrawableId(R.mipmap.highway)
                // 得到ImageOptions对象
                .build();
        x.image().bind(ivWelcome, welcomeUrl,options);

        //渐变展示启动屏
        AlphaAnimation aa = new AlphaAnimation(0.2f, 1.0f);
        aa.setDuration(2200);
        view.startAnimation(aa);
        aa.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation arg0) {
                redirectTo();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

        });
    }

    /**
     * 跳转到...
     */
    private void redirectTo() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * 获取版本名称用于显示
     *
     * @return 当前应用的版本名称
     */
    public static String getVersionName(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            return "";
        }
    }
}

