package org.metawatch.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

public class RMilk
{
	static final String API_URL = "https://api.rememberthemilk.com/services/rest/",
			AUTH_URL = "https://www.rememberthemilk.com/services/auth/",
			API_KEY = null,
			SECRET = null,
			TOKEN = null,
			LIST_ID = null;

	static {
		// Work around http://code.google.com/p/android/issues/detail?id=2939
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	public static String getTasksText(Context c)
	{
		TreeMap<String,String> args = new TreeMap<String,String>();
		JSONObject o;
		try {
			if (API_KEY == null) return "No API key";
			/*args.clear();
			args.put("method", "rtm.auth.getFrob");
			o = call(args);
			String frob = o.getJSONObject("rsp").getString("frob");
			args.clear();
			args.put("frob", frob);
			args.put("perms", "write");
			String u = makeURL(AUTH_URL, args).toString();
			Log.d(MetaWatch.TAG, "frob: "+u);
			c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			return u;*/
			// wait for user...
			/*String frob = "...";
			args.clear();
			args.put("method", "rtm.auth.getToken");
			args.put("frob", frob);
			o = call(args);
			String t = o.getJSONObject("rsp").getJSONObject("auth").getString("token");
			Log.d(MetaWatch.TAG, "token: "+t);
			return t;*/
			// we have a token
			/*args.clear();
			args.put("method", "rtm.lists.getList");
			args.put("auth_token", TOKEN);
			o = call(args);
			String list_id = o.getJSONObject("rsp").getJSONObject("lists").getJSONArray("list")
					.getJSONObject(0).getString("id"); */
			// we have a list id
			args.clear();
			args.put("method", "rtm.tasks.getList");
			args.put("list_id", LIST_ID);  // or "list:Supermarket" in the filter
			args.put("filter", "status:incomplete");
			o = call(args);
			JSONArray list = o.getJSONObject("rsp").getJSONObject("tasks")
					.optJSONArray("list");
			if (list == null) return "(no tasks)";
			JSONArray a = list.getJSONObject(0).optJSONArray("taskseries");
			if (a == null) {
				// Silly one-task case
				a = new JSONArray();
				a.put(list.getJSONObject(0).getJSONObject("taskseries"));
			}
			String ret = "";
			for (int i=0; i<a.length(); i++) {
				ret += "- " + a.getJSONObject(i).getString("name") + "\n";
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return e.toString();
		}
	}

	public static JSONObject call(TreeMap<String,String> args) throws IOException, JSONException
	{
		HttpURLConnection urlConnection = null;
		args.put("format", "json");
		if (TOKEN != null) args.put("auth_token", TOKEN);
		URL url = makeURL(API_URL, args);
		Log.d(MetaWatch.TAG, url.toString());
		try {
			urlConnection = (HttpURLConnection) url.openConnection();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					urlConnection.getInputStream()));
			String line;
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
			Log.d(MetaWatch.TAG, sb.toString());
			return new JSONObject(new JSONTokener(sb.toString()));
		} finally {
			if (urlConnection != null) urlConnection.disconnect();
		}
	}

	public static URL makeURL(String svc, TreeMap<String,String> args)
	{
		args.put("api_key", API_KEY);
		args.put("api_sig", sign(args));
		String q = "?";
		// TODO proper URL encoder/builder
		for (String k : args.keySet()) {
			if (q.length() > 1) q += "&";
			q += k + "=" + args.get(k);
		}
		try {
			return new URL(svc + q);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static String sign(TreeMap<String,String> args)
	{
		String s = SECRET;
		for (String k : args.keySet()) {
			s += k + args.get(k);
		}
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		byte[] d = md5.digest(s.getBytes());
		StringBuffer hex = new StringBuffer();
		for (int i=0; i<d.length; i++) {
			String b = Integer.toHexString(0xff & d[i]);
			if (b.length() == 1) b = "0" + b;
			hex.append(b);
		}
		return hex.toString();
	}
}
