package com.github.flutterumpush;

import android.util.Log;

import com.umeng.message.PushAgent;
import com.umeng.message.UTrack;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class FlutterUmpushPlugin
        implements MethodCallHandler {
    private static String TAG = "umeng_push_Plugin";
    public static FlutterUmpushPlugin instance;
    public final MethodChannel channel;
    private static Registrar registrar;

    public static void registerWith(Registrar registrar) {
        instance = new FlutterUmpushPlugin(registrar);
    }

    private FlutterUmpushPlugin(Registrar registrar) {
        FlutterUmpushPlugin.registrar = registrar;
        MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_umpush");
        channel.setMethodCallHandler(this);
        this.channel = channel;
    }


    @Override
    public void onMethodCall(MethodCall call, Result result) {
        Log.i(TAG, "onMethodCall: " + call.toString());
        if ("configure".equals(call.method)) {
            //当通过友盟离线唤醒的时候，系统首先执行的是UmengOtherPushActivity，而MainActivity尚未启动
            // ，所以UmengApplication的onCreate函数执行友盟注册时，Flutter尚未准备完成，不能接收信息，
            // 为了防止丢失通知，先缓存到SharedPreferences，等flutter初始化完成后，
            // 调用configure函数时，才执行onToken或onMessage回调

            //查看缓存是否存在Token，存在在执行Flutter的回调函数onToken，通知flutter进行更新
//            String token = UmengApplication.getPushData(registrar.activity(), UmengApplication.UMENG_PUSH_DEVICE_TOKEN);
//            if (token != null && !token.equals("")) {
//                channel.invokeMethod("onToken", token, new Result() {
//                    @Override
//                    public void success(Object o) {
//                        //UmengApplication.savePushData(registrar.activity(), UmengApplication.UMENG_PUSH_DEVICE_TOKEN, null);
//                    }
//
//                    @Override
//                    public void error(String s, String s1, Object o) {
//
//                    }
//
//                    @Override
//                    public void notImplemented() {
//
//                    }
//                });
//            }
            //查看缓存是否存在Token，存在在回调
            String umsgPushMsg = UmengApplication.getPushData(registrar.activity(), UmengApplication.UMENG_PUSH_MESSAGE);
            if (umsgPushMsg != null && !umsgPushMsg.equals("")) {
                channel.invokeMethod("onMessage", umsgPushMsg, new Result() {
                    @Override
                    public void success(Object o) {
                        //删除数据
                        UmengApplication.savePushData(registrar.activity(), UmengApplication.UMENG_PUSH_MESSAGE, null);
                    }

                    @Override
                    public void error(String s, String s1, Object o) {

                    }

                    @Override
                    public void notImplemented() {

                    }
                });
            }
            result.success(null);
        } else if ("getToken".equals(call.method)) {
            //添加一个获取Token的方法
            String useName = call.argument("useName");
            String umengDeviceToken = UmengApplication.getPushData(registrar.activity(), UmengApplication.UMENG_PUSH_DEVICE_TOKEN);
            final String  alias = umengDeviceToken + useName;
            PushAgent mPushAgent = PushAgent.getInstance(registrar.activity());
            //别名绑定，将某一类型的别名ID绑定至某设备，老的绑定设备信息被覆盖，别名ID和deviceToken是一对一的映射关系
            mPushAgent.setAlias(alias,"自有id", new UTrack.ICallBack() {
                @Override
                public void onMessage(boolean isSuccess, String message) {
                    if (isSuccess) {
                        channel.invokeMethod("onGetToken", alias);

                    }else{
                        Log.e("wilson","设置别名失败");
                    }
                }
            });

            result.success(null);
        }else  {
            result.notImplemented();
        }
    }


}
