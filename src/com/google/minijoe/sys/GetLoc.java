package com.google.minijoe.sys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * ��google��json�ӿڻ�ȡ����λ����Ϣ�Ĵ���
 *
 * @author lizongbo
 *
 */
public class GetLoc {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String url = "http://www.google.com/loc/json";
		JSONObject json = new JSONObject();
		json.put("version", "1.1.0");
		json.put("host", "maps.618119.com");
		json.put("home_mobile_country_code", 460);// ���Ҵ���
		json.put("home_mobile_network_code", 0);// �ƶ���Ӫ�̴���
		json.put("radio_type", "gsm");
		json.put("carrier", "lizongbo");
		json.put("request_address", true);
		json.put("address_language", "zh_CN");
		JSONArray jsoncells = new JSONArray();
		json.put("cell_towers", jsoncells);
		JSONArray jsonwifis = new JSONArray();
		json.put("wifi_towers", jsonwifis);
		JSONObject jsoncell = new JSONObject();
		jsoncell.put("mobile_country_code", 460);// ���Ҵ���,mcc
		jsoncell.put("mobile_network_code", 0);// �ƶ���Ӫ�̴���,mnc
		jsoncell.put("location_area_code", 9364);// λ���������,lac
		jsoncell.put("cell_id", "3851");// �ƶ���վid
		// jsoncell.put("age", 0);
		// jsoncell.put("signal_strength", -70);
		// jsoncell.put("timing_advance", 7777);
		jsoncells.put(jsoncell);
		JSONObject jsonwifi = new JSONObject();
		// jsonwifi.put("mac_address", "00-11-22-33-44-55");
		// jsonwifi.put("signal_strength", 8);
		// jsonwifi.put("age", 0);
		// jsonwifis.put(jsonwifi);
		// jsonwifi = new JSONObject();
		jsonwifi.put("mac_address", "00-55-44-33-22-11");//
		jsonwifi.put("ssid", "TPLINK_618119");// ����·����������
		jsonwifi.put("signal_strength", 8);// �ź�ǿ��
		jsonwifi.put("age", 0);
		// jsonwifis.put(jsonwifi);

		System.out.println(json.toString());
		System.out.println(downloadUrlbyPOST(url, json.toString(), null,
				"UTF-8"));

	}

	public static String downloadUrlbyPOST(String urlStr, String query,
			String referer, String encoding) throws Exception {
		String line = "";
		StringBuilder sb = new StringBuilder();
		HttpURLConnection httpConn = null;
		try {
			URL url = new URL(urlStr);
			System.out.println(urlStr + "?" + query);
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
					"proxy.lizongbo.com", 8080));
			proxy = Proxy.NO_PROXY;
			httpConn = (HttpURLConnection) url.openConnection(proxy);
			httpConn.setDoInput(true);
			httpConn.setDoOutput(true);
			httpConn.setRequestMethod("POST");
			if (referer != null) {
				httpConn.setRequestProperty("Referer", referer);
			}
			httpConn.setConnectTimeout(5000);
			// httpConn.getOutputStream().write(
			// java.net.URLEncoder.encode(query, "UTF-8").getBytes());
			httpConn.getOutputStream().write(query.getBytes());
			httpConn.getOutputStream().flush();
			httpConn.getOutputStream().close();

			BufferedReader in = null;
			if (httpConn.getResponseCode() != 200) {
				System.err.println("error:" + httpConn.getResponseMessage());
				in = new BufferedReader(new InputStreamReader(
						httpConn.getErrorStream(), "UTF-8"));
			} else {
				in = new BufferedReader(new InputStreamReader(
						httpConn.getInputStream(), "UTF-8"));
			}
			while ((line = in.readLine()) != null) {
				sb.append(line).append('\n');
			}
			// �ر�����
			httpConn.disconnect();
			return sb.toString();
		} catch (Exception e) {
			// �ر�����
			httpConn.disconnect();
			System.out.println(e.getMessage());
			throw e;
		}
	}
}