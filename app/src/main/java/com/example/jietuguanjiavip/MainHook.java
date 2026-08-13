package com.example.jietuguanjiavip;

import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TARGET_PACKAGE = "com.YiGeTechnology.XiaoWai.business";
    private static final String MODULE_PACKAGE = "com.example.jietuguanjiavip";
    private static boolean isHooked = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 1. 进程过滤：仅在目标 App 的主进程运行，避开推送/Webview等子进程
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;
        if (lpparam.processName != null && !lpparam.processName.equals(TARGET_PACKAGE)) return;

        // 2. 延迟 Hook 机制（完美兼容冷启动与热加载）
        if (!tryHook(lpparam.classLoader)) {
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.Application",
                    lpparam.classLoader,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Context context = (Context) param.args[0];
                            if (context != null) {
                                tryHook(context.getClassLoader());
                            }
                        }
                    }
                );
            } catch (Throwable ignored) {
            }
        }
    }

    private synchronized boolean tryHook(ClassLoader classLoader) {
        if (isHooked || classLoader == null) return isHooked;
        try {
            Class<?> responseBodyClass = XposedHelpers.findClass("okhttp3.ResponseBody", classLoader);
            XposedHelpers.findAndHookMethod(responseBodyClass, "string", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String body = (String) param.getResult();
                    if (body == null || !body.contains("\"isVip\"")) return;

                    // 3. 性能优化：直接处理数据，彻底删除 getStackTrace 堆栈遍历
                    String newBody = modifyVip(body);
                    if (newBody != null) {
                        param.setResult(newBody);
                    }
                }
            });
            isHooked = true;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private String modifyVip(String original) {
        try {
            // 4. 动态读取 UI 保存的配置
            XSharedPreferences pref = new XSharedPreferences(MODULE_PACKAGE, "settings");
            pref.makeWorldReadable();
            pref.reload();

            String username = pref.getString("username", "谁明浪子心");
            String expireTime = pref.getString("expire_time", "2099-12-31 23:59:59");

            return original
                    .replace("\"isVip\":false", "\"isVip\":true")
                    .replace("\"isVip\": false", "\"isVip\": true")
                    .replaceAll("\"vipExpiredTime\":null", "\"vipExpiredTime\":\"" + expireTime + "\"")
                    .replaceAll("\"vipExpiredTime\": null", "\"vipExpiredTime\":\"" + expireTime + "\"")
                    .replaceAll("\"parseVideoRemain\":\\d+", "\"parseVideoRemain\":9999")
                    .replaceAll("\"username\":\"[^\"]*\"", "\"username\":\"" + username + "\"");
        } catch (Exception e) {
            return null;
        }
    }
}
