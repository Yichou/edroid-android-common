package com.edroid.common.utils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * 封装 Http 
 * 
 * @author Yichou 2013-9-9
 *
 * 完善 2013-9-27 by:yichou
 * 2013-10-23 增加两种模式创建 httpClinet by:yichou
 * 
 */
public class HttpUtils {
	private static final String LOG_TAG = "HttpUtils";
	static final Logger log = Logger.create(true, LOG_TAG);
	
	public static final String CHARSET = HTTP.UTF_8;
	public static final String USERAGENT = "android";
	
	private static HttpClient mClient;
	
	/**
	 * 获取  httpClient ，单实例，多线程模式
	 * 
	 * @param context
	 * @return
	 */
	public static synchronized HttpClient getHttpClientSingle(Context context) {
		if (mClient == null) {
			HttpParams params = new BasicHttpParams();

			// 设置协议参数
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, CHARSET);
			HttpProtocolParams.setUseExpectContinue(params, true);
			HttpProtocolParams.setUserAgent(params, USERAGENT);

			// 超时设置
			// 1.从连接池取连接超时时间
			ConnManagerParams.setTimeout(params, 1000);
			// 2.建立连接超时时间，该socket连接的超时时间
			HttpConnectionParams.setConnectionTimeout(params, 4000); // 4S
			// 3.socket 请求超时，从服务器获取响应数据需要等待的时间
			HttpConnectionParams.setSoTimeout(params, 10000); // 10S
			HttpConnectionParams.setSocketBufferSize(params, 16 * 1024); // 16K

			// 设置模式
			SchemeRegistry reg = new SchemeRegistry();
			reg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			reg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

			// 使用线程安全的连接管理来创建HttpClient
			ClientConnectionManager connMgr = new ThreadSafeClientConnManager(params, reg);
			mClient = new DefaultHttpClient(connMgr, params);
		}
		
		mClient.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, getProxyHttpHost(context));

		return mClient;
	}
	
	/**
	 * 创建 httpClient
	 * 
	 * @param context
	 * @return
	 */
	public static HttpClient newHttpClient(Context context) {
		HttpParams params = new BasicHttpParams();
		// UserAgent
		HttpProtocolParams.setUserAgent(params, USERAGENT);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);// http-1.1
		// 设置连接超时和 Socket 超时，以及 Socket 缓存大小:
		HttpConnectionParams.setConnectionTimeout(params, 4000); // 4S
		HttpConnectionParams.setSoTimeout(params, 10000); // 10S
		HttpConnectionParams.setSocketBufferSize(params, 16 * 1024); // 16K

		/**
		 * 创建一个 HttpClient 实例: 注意: HttpClient httpClient = new HttpClient();
		 * 是CommonsHttpClient中的用法, 在 Android 1.5 中我们需要使用 Apache 的缺省实现
		 * DefaultHttpClient.
		 */
		HttpClient mHttpClient = new DefaultHttpClient(params);
		mHttpClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);

		//代理
		mHttpClient.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, getProxyHttpHost(context));

		return mHttpClient;
	}
	
	public static HttpClient getHttpClient(Context context) {
		return getHttpClientSingle(context);
	}
	
	public static String get(Context context, String uri) throws Exception {
		return get(context, new HttpGet(uri));
	}

	/**
	 * 2013-9-27 获取当前已连接的网络
	 */
	public static NetworkInfo getActiveNetworkInfo(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
				Context.CONNECTIVITY_SERVICE);

		return cm.getActiveNetworkInfo();
	}
	
	@SuppressWarnings("deprecation")
	public static boolean needProxy(Context context) {
		NetworkInfo networkInfo = getActiveNetworkInfo(context);
		
		// 如果是使用的运营商网络
		if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
			// 获取默认代理主机ip
			String host = android.net.Proxy.getDefaultHost();
			// 获取端口
			int port = android.net.Proxy.getDefaultPort();
			if (host != null && port != -1) {
				log.i("Proxy addr: " + host + ":" + port);
				
				return true;
			} 
		} 
		
		return false;
	}
	
	/**
	 * 2013-9-27 获取代理地址
	 * 
	 * @return 不需要代理返回 null
	 */
	public static InetSocketAddress getProxyAddress(Context context) {
		if(!needProxy(context))
			return null;

		String proxyHost = android.net.Proxy.getDefaultHost();
		int port = android.net.Proxy.getDefaultPort();
		if (proxyHost != null) {
			log.i("Proxy addr: " + proxyHost + ":" + port);
			
			return new InetSocketAddress(proxyHost, port);
		}
		
		return null;
	}
	
	/**
	 * 2013-9-27
	 * 
	 * @param context
	 * @return
	 */
	public static Proxy getProxy(Context context) {
		InetSocketAddress sa = getProxyAddress(context);
		if(sa != null)
			return new Proxy(Proxy.Type.HTTP, sa);
		return null;
	}
	
	/**
	 * 获取代理
	 * 
	 * @param context
	 * @return
	 */
	public static HttpHost getProxyHttpHost(Context context) {
		InetSocketAddress sa = getProxyAddress(context);
		if(sa != null)
			return new HttpHost(sa.getHostName(), sa.getPort());
		return null;
	}
	
	public static String get(Context context, HttpGet get) throws Exception {
		HttpResponse rsp = getHttpClient(context).execute(get);
		int code = rsp.getStatusLine().getStatusCode();
		
		if (code == HttpStatus.SC_OK) {
			HttpEntity ret = rsp.getEntity();
			if (ret != null) {
				return EntityUtils.toString(ret, CHARSET);
			} else {
				log.e("rsp getEntity FAIL!");
			}
		} else {
			log.e("get() HttpStatus ERROR, code = " + code);
		}

		return null;
	}
	
	public static String post(Context context, String uri, NameValuePair... params) throws Exception {
		ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();
		for (NameValuePair p : params) {
			list.add(p);
		}

		UrlEncodedFormEntity entity;
		entity = new UrlEncodedFormEntity(list, CHARSET);

		// 创建post请求
		HttpPost post = new HttpPost(uri);
		post.setEntity(entity);

		return post(context, post);
	}
	
	public static String post(Context context, HttpPost post) throws Exception {
		HttpResponse rsp = getHttpClient(context).execute(post);
		int code = rsp.getStatusLine().getStatusCode();

		if (code == HttpStatus.SC_OK) {
			HttpEntity ret = rsp.getEntity();
			if (ret != null) {
				return EntityUtils.toString(ret, CHARSET);
			} else {
				log.e("rsp getEntity FAIL!");
			}
		} else {
			log.e("post() HttpStatus ERROR, code = " + code);
		}

		return null;
	}
}
