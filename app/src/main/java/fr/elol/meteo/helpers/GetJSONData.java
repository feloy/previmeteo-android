package fr.elol.meteo.helpers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import fr.elol.meteo.BuildConfig;
import fr.elol.meteo.R;

/**
 * Get JSON data from webservice
 */

public class GetJSONData extends AsyncTask<String, Void, String> {

    private CallBackListener mListener;
    private int mRange;
    private Context mContext;

    private static final String NO_COM_GOOGLE_ACCOUNT = "no com.google account";

    public GetJSONData (Context context, int range, CallBackListener listener) {
        mListener = listener;
        mRange = range;
        mContext = context;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            URL url;
            String response = "";
            AccountManager manager = (AccountManager) mContext.getSystemService(Activity.ACCOUNT_SERVICE);
            Account[] list = manager.getAccountsByType("com.google");
            if (list.length < 1)
                return NO_COM_GOOGLE_ACCOUNT;
            String username = list[0].name;
            String token = GoogleAuthUtil.getToken(mContext,
                    username,
                    mContext.getResources().getString(R.string.webservice_clientid));

/*
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(params[0]);
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("token", token));
            nameValuePairs.add(new BasicNameValuePair("version", ""+ BuildConfig.VERSION_CODE));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse httpresponse = httpclient.execute(httppost);
            HttpEntity httpEntity = httpresponse.getEntity();
            response = EntityUtils.toString(httpEntity);
*/
            url = new URL(params[0]);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(15000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            HashMap<String, String> postDataParams = new HashMap<>();
            postDataParams.put("token", token);
            postDataParams.put("version", ""+ BuildConfig.VERSION_CODE);
            writer.write(getPostDataString(postDataParams));

            writer.flush();
            writer.close();
            os.close();
            int responseCode=conn.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line=br.readLine()) != null) {
                    response+=line;
                }
            }
            else {
                response="";
            }
            Log.d("response is", response);
            return response;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result == null) {
        } else if (result.equals(NO_COM_GOOGLE_ACCOUNT)) {
            alertNoGoogleAccount ();
        } else {
            try {
                if (mListener != null)
                    mListener.callback(mRange, result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public interface CallBackListener{
        public void callback(int range, String res);
    }

    private void alertNoGoogleAccount () {
        Toast toast = Toast.makeText(mContext, "Afin de sécuriser les échanges sur le réseau, vous devez avoir un compte Google", Toast.LENGTH_LONG);
        toast.show();
        mContext.startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS));
    }
}