package com.edroid.common.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONObject;

/**
 * 通过 IP 获取省份，城市工具
 * 
 * @author Yichou 2013-11-8 11:36:58
 * 
 */
public final class IpUtils {
	public static final class Location {
		public String country;
		public String city;
		public String province;
		
		@Override
		public String toString() {
			return new StringBuilder(128)
//				.append(country).append('/')
				.append(province).append('/').append(city)
				.toString();
		}
	}

	@SuppressWarnings("unchecked")
	public static void printJsonObject(JSONObject jsonObject) {
		Iterator<String> iterator = jsonObject.keys();
		do {
			String key = iterator.next();
			System.out.println(key + "=" + jsonObject.opt(key).toString());
		} while (iterator.hasNext());
	}

	/**
	 * 获取本机 ip 地域
	 * 
	 * @return
	 */
	public static Location getLocationFromBaidu() {
		return getLocationFromBaidu(null);
	}
	
	public static String readString(InputStream is) throws IOException {
		byte[] buf = new byte[256];
		int read = 0;
		ByteArrayBuffer array = new ByteArrayBuffer(1024);
		
		while ((read = is.read(buf)) != -1) {
			array.append(buf, 0, read);
		}
		
		return new String(array.buffer(), 0, array.length());
	}
	
	public static Location getLocationFromBaidu(String ip) {
		try {
			final String API = "http://api.map.baidu.com/location/ip";
			final String AK = "E8b6b0261df95a6d57ac45cbde61e5f2";
			
			StringBuilder sb = new StringBuilder(128);
			sb.append(API).append("?ak=").append(AK);
			if(ip != null)
				sb.append("&ip=").append(ip);

			InputStream is = new BufferedInputStream(
					new URL(sb.toString()).openConnection().getInputStream());

			String jsonString = readString(is);

//			System.out.println("ret=" + jsonString);

			JSONObject jsonObject = new JSONObject(jsonString);
			int status = jsonObject.getInt("status");
			if (status == 0) {
				jsonObject = jsonObject.getJSONObject("content").getJSONObject("address_detail");

				Location location = new Location();
				location.city = jsonObject.getString("city");
				location.province = jsonObject.getString("province");

				return location;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static Location getLocationFromTaobao(String ip) {
		try {
			URL url = new URL("http://ip.taobao.com/service/getIpInfo.php?ip=" + ip);
			
			InputStream is = url.openConnection().getInputStream();
			byte[] data = new byte[is.available()];
			is.read(data);
			
			String jsonString = new String(data);
			System.out.println("ret=" + jsonString);

			JSONObject jsonObject = new JSONObject(jsonString);
			int code = jsonObject.getInt("code");
			if (code == 0) {
				jsonObject = jsonObject.getJSONObject("data");

				Location location = new Location();

				location.country = jsonObject.getString("country");
				location.city = jsonObject.getString("city");
				location.province = jsonObject.getString("area");
				
				return location;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 获取本机本地 ip （非外网）ip v4
	 * 
	 * @return
	 */
	public static String getLocalIpV4() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						String ip = inetAddress.getHostAddress();
						if(InetAddressUtils.isIPv4Address(ip))
							return ip;
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		
		return "";
	}
}
