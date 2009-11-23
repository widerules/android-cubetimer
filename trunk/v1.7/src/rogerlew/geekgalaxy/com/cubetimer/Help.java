package rogerlew.geekgalaxy.com.cubetimer;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class Help extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView helpWebView = new WebView(this);
        WebSettings webSettings = helpWebView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        setContentView(helpWebView);
        
        helpWebView.loadUrl("file:///android_asset/help.htm");
    }
 }