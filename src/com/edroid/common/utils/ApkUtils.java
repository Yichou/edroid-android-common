package com.edroid.common.utils;

import java.io.File;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;

/**
 * APK 文件操作根据
 * 
 * @author Yichou 2013-6-21
 * 
 */
public final class ApkUtils {
	/**
	 * 检查一个 apk 是否已安装
	 * 
	 * @param context
	 * @param pkg
	 * @return
	 */
	public static boolean isInstalled(Context context, String pkg) {
		PackageManager pm = context.getPackageManager();
		try {
			PackageInfo info = pm.getPackageInfo(pkg, 0);
			if (info != null)
				return true;
		} catch (NameNotFoundException e) {
		}
		
		return false;
	}
	
	public static Intent getInstallIntent(Context context, File path) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.fromFile(path), "application/vnd.android.package-archive");
		
		return intent;
	}
	
	public static void install(Context context, File path) {
		context.startActivity(getInstallIntent(context, path));
	}
	
	/**
	 * 卸载 apk
	 * 
	 * @param context
	 * @param pkg
	 *            包名
	 */
	public static void uninstall(Context context, String pkg) {
		context.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
	}
	
	public static Intent getRunIntent(Context context, String pkg) {
		return context.getPackageManager().getLaunchIntentForPackage(pkg);
	}
	
	public static void runPackge(Context context, String pkg) {
		context.startActivity(getRunIntent(context, pkg));
	}
	
	public static PackageInfo getPackageInfo(Context context, String path, int flags) {
		return context.getPackageManager().getPackageArchiveInfo(path, flags);
	}
	
	public static ApplicationInfo getApplicationInfo(Context context, String path) {
		PackageInfo info = getPackageInfo(context, path, 0);
		if (info != null)
			return info.applicationInfo;
		return null;
	}
	
	/**
	 * 取安装包 包名
	 * 
	 * @param context
	 * @param path
	 * @return
	 */
	public static String getPackgeName(Context context, String path) {
		ApplicationInfo info = getApplicationInfo(context, path);
		if (info != null)
			return info.packageName;
		
		// ApplicationInfo appInfo = info.applicationInfo;
		// String packageName = appInfo.packageName; // 得到安装包名称
		// String appName = pm.getApplicationLabel(appInfo).toString();
		// String version=info.versionName; //得到版本信息
		// Drawable icon = pm.getApplicationIcon(appInfo);//得到图标信息
		
		return null;
	}
	
	public static int getAppIconResId(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			return info.applicationInfo.icon;
		} catch (NameNotFoundException e) {
		}
		
		return 0;
	}
	
	public static final class ApkResources extends Resources {
		private AssetManager asset;
		
		
		public ApkResources(Context context, AssetManager asset) {
			super(asset, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
			
			this.asset = asset;
		}
		
		public void close() {
			asset.close();
		}
	}
	
	/**
	 * 获取一个 apk 包的 Resource 对象
	 * 
	 * 注意：使用完后调用 resources.getAssets().close() 释放资源
	 * 
	 * @param context
	 * @param path
	 * @return
	 */
	public static ApkResources makeApkResources(Context context, String path) {
		try {
			AssetManager mAsset = AssetManager.class.newInstance();
			Method method = AssetManager.class.getMethod("addAssetPath", String.class);
			method.setAccessible(true);
			method.invoke(mAsset, path);
			
			return new ApkResources(context, mAsset);
		} catch (Exception e) {
		}
		
		return null;
	}
}
