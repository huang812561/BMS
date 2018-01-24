package com.bms.util.httputil;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSONObject;
import com.bms.util.commmon.DesEncryptHelper;
import com.bms.util.commmon.EncryptException;

public class TestMain {

	public static void main(String[] args) throws IOException, IOException {
		Map<String, String> paramMap = new HashMap<String, String>();
		String url = "http://192.168.1.32:9090/oop/common/api/login";
		paramMap.put("loginName", "admin");
		paramMap.put("password", "admin123");
		
		
		
		System.out.println(doWork(url, paramMap));
	}
	
	public static void buildLoginParams(JSONObject jsonObject) {
		jsonObject.put("loginName", "admin");
		jsonObject.put("password", "admin123");
	}

	public static String doWork(String url , Map<String,String> param){

		// 创建Httpclient对象
		HttpClient httpClient = new DefaultHttpClient();
		String resultString = "";
		HttpResponse httpResponse = null;
		try {
			//创建url
			URIBuilder builder = new URIBuilder(url);
			//设置参数
			URI uri = builder.build();
			JSONObject jsonObject = new JSONObject(true); 
			buildLoginParams(jsonObject);
			builder.addParameter("requestData", jsonObject.toJSONString());
			HttpPost httpPost = new HttpPost();
			httpPost.setURI(uri);
            
			httpResponse = httpClient.execute(httpPost);
			if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
				System.out.println("服务器正常响应.....");
				resultString = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultString;
	}

	
	
	/**
	 * 
	 * @Description: 将map集合的键值对转化成：key1=value1&key2=value2 的形式
	 * @param param
	 * @return
	 * @throws EncryptException 
	 */
	public static String convertStringParameter(Map<String, String> param) throws EncryptException {

		StringBuffer resultStr = new StringBuffer();
		for (Map.Entry<String, String> entry : param.entrySet()) {
			String paramKey = "";
			String paramValue = "";
			if (entry.getKey() != null) {
				paramKey = entry.getKey();
				paramValue = entry.getValue();
				resultStr.append(paramKey).append("=").append(paramValue).append("&");
			}
		}
		resultStr.delete(resultStr.length()-1, resultStr.length());
		return DesEncryptHelper.encryption(resultStr.toString());
	}

	/**
	 * 将map集合的键值对转化成：key1=value1&key2=value2 的形式
	 * 
	 * @param parameterMap
	 *            需要转化的键值对集合
	 * @return 字符串
	 */
	public static String convertStringParamter(Map parameterMap) {
		StringBuffer parameterBuffer = new StringBuffer();
		if (parameterMap != null) {
			Iterator iterator = parameterMap.keySet().iterator();
			String key = null;
			String value = null;
			while (iterator.hasNext()) {
				key = (String) iterator.next();
				if (parameterMap.get(key) != null) {
					value = (String) parameterMap.get(key);
				} else {
					value = "";
				}
				parameterBuffer.append(key).append("=").append(value);
				if (iterator.hasNext()) {
					parameterBuffer.append("&");
				}
			}
		}
		return parameterBuffer.toString();
	}

}
