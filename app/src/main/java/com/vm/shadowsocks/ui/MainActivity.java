package com.vm.shadowsocks.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.app.ListActivity;
import android.net.VpnService;
import android.widget.AdapterView;
import android.os.Handler;
import android.os.Message;

import com.vm.shadowsocks.R;
import com.vm.shadowsocks.core.AppProxyManager;
import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.ProxyConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.util.HashMap;

import at.grabner.circleprogress.CircleProgressView;
import at.markushi.ui.CircleButton;

import org.json.JSONObject;
import org.json.JSONArray;

import android.widget.Spinner;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;
import me.shaohui.bottomdialog.BottomDialog;
import androidx.fragment.app.FragmentActivity;
import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.model.TableColumnWeightModel;
import androidx.core.content.ContextCompat;
import java.lang.String;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.view.KeyEvent;

public class MainActivity extends FragmentActivity  implements
        View.OnClickListener,
        OnCheckedChangeListener,
        LocalVpnService.onStatusChangedListener {
    static {
        System.loadLibrary("native-lib");
    }
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CONFIG_URL_KEY = "CONFIG_URL_KEY";
    private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;
    private Calendar mCalendar;
    private boolean StartVpnChecked = false;
    private Animation operatingAnim;
    private Vector<String> country_vec = new Vector<String>();
    private String selectCountry = "America";
    private VpnService vpn_service = new VpnService();
    private CheckTransaction check_tx = new CheckTransaction();
    private static final int COMPLETED = 0;
    private static final int GOT_VPN_SERVICE = 1;
    private static final int GOT_TANSACTIONS = 2;
    private static final int GOT_BALANCE = 3;
    private static final int GOT_VPN_ROUTE = 5;
    private static final int GOT_INVALID_SERVER_STATUS = 6;
    private static final int GOT_INVALID_STATUS = 7;
    private static final int GOT_CHANGE_VIP_STATUS = 8;
    private int check_vip_times = 0;
    private boolean upgrade_dialog_showing = false;

    private CheckBox global_mode_checkbox;
    private HashMap<Integer, String> block_hashmap = new HashMap<Integer, String>();
    int list_counter = 0;

    public ArrayList<String> vpn_country_list = new ArrayList<String>();

    private HashMap<String, Vector<String>> country_vpn_map = new HashMap<String, Vector<String>>();
    private HashMap<String, Vector<String>> country_route_map = new HashMap<String, Vector<String>>();
    private HashMap<String, String> country_to_short = new HashMap<String, String>();

    private CircleProgressView mCircleView;

    private Spinner mSpinner;
    private String[] spinnerTitles;
    private int[] spinnerImages;
    private String now_choosed_country = "US";
    private BottomDialog bottom_dialog;
    private BottomDialog upgrade_dialog;
    private BottomDialog webview_dialog;
    private BottomDialog outof_bandwidth_dialog;

    private String transactions_res = new String("");
    private long account_balance = -1;
    public static String choosed_vpn_url = "";
    private final int kDefaultVpnServerPort = 9033;
    private String version_download_url = "";
    private WebView wv_produce;
    private Button web_view_open_btn;
    private TextView tilte_text_view;
    private long goback_prev_timestamp = 0;
    private boolean isExit;
    private WebView pay_view;
    private BottomDialog payview_dialog;

    private static JSONArray getAllowedCardNetworks() {
            return new JSONArray()
                .put("AMEX")
                .put("DISCOVER")
                .put("INTERAC")
                .put("JCB")
                .put("MASTERCARD")
                .put("VISA");
    }

    private static JSONArray getAllowedCardAuthMethods() {
        return new JSONArray()
                .put("PAN_ONLY")
                .put("CRYPTOGRAM_3DS");
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == COMPLETED) {
                String res = (String)msg.obj;
                String[] split = res.split("\t");
                if (split.length == 5) {
                    TextView balance = (TextView)findViewById(R.id.balance_lego);
                    balance.setText(split[1] + " Tenon");
                    TextView balance_d = (TextView)findViewById(R.id.balance_dollar);
                    balance_d.setText(String.format("%.2f", Integer.parseInt(split[1]) * 0.002) + "$");
                    String block_item = "\n\n\n    Transaction Hash: \n    " + split[4] + "\n\n    Block Height：" + split[2] + "\n\n    Block Hash：\n    " + split[3] + "\n\n\n";
                    block_hashmap.put(list_counter, block_item);
                    ++list_counter;
                }
            }

            if (msg.what == GOT_VPN_SERVICE) {
                String res = (String)msg.obj;
                String[] c_split = res.split("\t");
                String country = c_split[0];
                String[] split = c_split[1].split(",");
                if (!country_vpn_map.containsKey(country)) {
                    country_vpn_map.put(country, new Vector<String>());
                }

                Vector<String> url_vec = country_vpn_map.get(country);
                for (int i = 0; i < split.length; ++i) {
                    url_vec.add(split[i]);
                    if (url_vec.size() > 16) {
                        url_vec.remove(0);
                    }
                }
            }

            if (msg.what == GOT_VPN_ROUTE) {
                String res = (String)msg.obj;
                String[] c_split = res.split("\t");
                String country = c_split[0];
                String[] split = c_split[1].split(",");
                if (!country_route_map.containsKey(country)) {
                    country_route_map.put(country, new Vector<String>());
                }

                Vector<String> url_vec = country_route_map.get(country);
                for (int i = 0; i < split.length; ++i) {
                    url_vec.add(split[i]);
                    if (url_vec.size() > 16) {
                        url_vec.remove(0);
                    }
                }
            }

            if (msg.what == GOT_TANSACTIONS) {
                String res = (String)msg.obj;
                if (res.isEmpty()) {
                    return;
                }
                transactions_res = res;
            }

            if (msg.what == GOT_BALANCE) {
                long res = (long)msg.obj;
                account_balance = res;
                TextView balance = (TextView)findViewById(R.id.balance_lego);
                balance.setText(account_balance + " Tenon");
                TextView main_balance = (TextView)findViewById(R.id.main_balance_lego);
                main_balance.setText(account_balance + " Tenon");
                P2pLibManager.getInstance().now_balance = account_balance;
                TextView balance_d = (TextView)findViewById(R.id.balance_dollar);
                balance_d.setText(String.format("%.2f", account_balance * 0.002) + "$");
            }

//            if (msg.what == GOT_INVALID_SERVER_STATUS) {
//                String res = (String)msg.obj;
//                if (res.equals("bwo")) {
//                    LocalVpnService.IsRunning = false;
//                    Log.e("main 224", "bwo stop vpn server.");
//                }
//            }

            if (msg.what == GOT_INVALID_STATUS) {
                setNotice();
            }

            if (msg.what == GOT_CHANGE_VIP_STATUS) {
                setVipStatus();
            }
        }
    };

    public void hideDialog(View view) {
        bottom_dialog.dismiss();
    }

    public void hideOutofBandwidth(View view) {
        outof_bandwidth_dialog.dismiss();
    }

    public void copyAccount(View view) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("Label", P2pLibManager.getInstance().account_id);
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this, getString(R.string.copy_succ), Toast.LENGTH_SHORT).show();
    }

    public void copyPrikey(View view) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("Label", P2pLibManager.getInstance().private_key);
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this, getString(R.string.copy_succ), Toast.LENGTH_SHORT).show();
    }

    public void shareVip(View view) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData mClipData = ClipData.newPlainText("Label", P2pLibManager.getInstance().share_ip + "?id=" + P2pLibManager.getInstance().account_id);
        cm.setPrimaryClip(mClipData);
        Toast.makeText(this, getString(R.string.copy_share_vip), Toast.LENGTH_SHORT).show();
    }

    public void JoinUs(View view) {
        String url = "https://github.com/tenondvpn/tenonvpn-join";
        if (url.isEmpty() || url.startsWith("file")) {
            Toast.makeText(this, getString(R.string.select_a_website_string), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(url);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(uri);
        startActivity(intent);
    }

    public void buyTenon(View view)
    {
        String url = P2pLibManager.getInstance().buy_tenon_ip + "/chongzhi/" + P2pLibManager.getInstance().account_id;
        if (url.isEmpty() || url.startsWith("file")) {
            Toast.makeText(this, getString(R.string.select_a_website_string), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(url);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(uri);
        startActivity(intent);
    }

    public void hidePayDialog(View view) {
        payview_dialog.dismiss();
    }

    public void pastePrikey(View view) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String tmp_prikey = cm.getPrimaryClip().getItemAt(0).getText().toString().toLowerCase().trim();
        if (tmp_prikey.length() != 64) {
            Toast.makeText(this, getString(R.string.invalid_prikey) + ":" + tmp_prikey, Toast.LENGTH_SHORT).show();
            return;
        }

        for (int i = 0; i < tmp_prikey.length(); i++) {
            char ch = tmp_prikey.charAt(i);
            if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'))) {
                Toast.makeText(this, getString(R.string.invalid_prikey) + ":" + tmp_prikey, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (tmp_prikey.equals(P2pLibManager.getInstance().private_key)) {
            Toast.makeText(this, getString(R.string.set_prikey), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!P2pLibManager.getInstance().SaveUserPrivateKey(tmp_prikey)) {
            Toast.makeText(this, getString(R.string.max_set_prikey), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!P2pLibManager.getInstance().ResetPrivateKey(tmp_prikey)) {
            Toast.makeText(this, getString(R.string.invalid_prikey) + ":" + tmp_prikey, Toast.LENGTH_SHORT).show();
            return;
        }

        EditText prikey_text=(EditText)bottom_dialog.getView().findViewById(R.id.dlg_private_key);
        prikey_text.setText(tmp_prikey.toUpperCase());
        P2pLibManager.getInstance().private_key = tmp_prikey;

        EditText acc_text=(EditText)bottom_dialog.getView().findViewById(R.id.dlg_account_address);
        acc_text.setText(P2pLibManager.getInstance().account_id.toUpperCase());
        Toast.makeText(this, getString(R.string.set_prikey), Toast.LENGTH_SHORT).show();

        TextView main_balance = (TextView)findViewById(R.id.main_balance_lego);
        main_balance.setText(getString(R.string.now_wait_to_sync));
        bottom_dialog.dismiss();
        LocalVpnService.IsRunning = false;
        P2pLibManager.getInstance().InitResetPrivateKey();
        TextView vip_txt = (TextView)findViewById(R.id.main_vip_left_status);
        vip_txt.setText("");
        TextView notice_txt = (TextView)findViewById(R.id.notice_text);
        notice_txt.setText("");
        check_vip_times = 0;
        account_balance = 0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.restart_app));
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalVpnService.IsRunning = false;
                LocalVpnService.removeOnStatusChangedListener(MainActivity.this);
                p2pDestroy();
                MainActivity.this.finish();
            }
        });
        builder.show();
    }

    public void hideUpgrade(View view) {
        upgrade_dialog.dismiss();

        upgrade_dialog_showing = false;

    }
    public void switchSmartRoute(View view) {
        LocalVpnService.IsRunning = false;
        P2pLibManager.getInstance().use_smart_route = true;
    }

    public void homePage(View view) {
        wv_produce.loadUrl("file:///android_asset/index.html");
        wv_produce.clearHistory();
    }


    public void useBrower(View view) {
        String url = wv_produce.getUrl();
        if (url.isEmpty() || url.startsWith("file")) {
            Toast.makeText(this, getString(R.string.select_a_website_string), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(url);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(uri);
        startActivity(intent);
    }

    void setNotice() {
        TextView notice_txt = (TextView)findViewById(R.id.notice_text);
        if (P2pLibManager.getInstance().now_status.equals("cnn")) {
            notice_txt.setText(getString(R.string.disconnect_vpn_network));
        }

        if (P2pLibManager.getInstance().now_status.equals("cni")) {
            notice_txt.setText(getString(R.string.invalid_country));
        }

        if (P2pLibManager.getInstance().now_status.equals("bwo")) {
           // notice_txt.setText(getString(R.string.bandwidth_over));
        }
    }

    void setVipStatus() {
        TextView vip_txt = (TextView)findViewById(R.id.main_vip_left_status);
        if (P2pLibManager.getInstance().vip_left_days == -1 && P2pLibManager.getInstance().now_balance != -1) {
            vip_txt.setText(getString(R.string.now_free_to_use));
//            Button no_vip_button = (Button)findViewById(R.id.vip_no);
//            no_vip_button.setVisibility(View.VISIBLE);
//            Button vip_button = (Button)findViewById(R.id.vip_yes);
//            vip_button.setVisibility(View.INVISIBLE);
        }

        if (P2pLibManager.getInstance().vip_left_days >= 0) {
            vip_txt.setText(getString(R.string.layters_over_before) + " " + P2pLibManager.getInstance().vip_left_days + " " + getString(R.string.layters_over_after));
//            Button no_vip_button = (Button)findViewById(R.id.vip_no);
//            no_vip_button.setVisibility(View.INVISIBLE);
//            Button vip_button = (Button)findViewById(R.id.vip_yes);
//            vip_button.setVisibility(View.VISIBLE);
        }
    }

    void initPayView(final View view) {
        pay_view=(WebView) view.findViewById(R.id.wv_pay);
        pay_view.loadUrl(P2pLibManager.getInstance().buy_tenon_ip + "/chongzhi/" + P2pLibManager.getInstance().account_id);
        WebSettings webSettings = pay_view.getSettings();
        webSettings.setJavaScriptEnabled(true);
        pay_view.getSettings().setSupportZoom(true);
        pay_view.getSettings().setAllowFileAccess(true);
        pay_view.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        pay_view.getSettings().setJavaScriptEnabled(true);
        pay_view.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        pay_view.getSettings().setLoadWithOverviewMode(true);
        pay_view.getSettings().setDefaultTextEncodingName("utf-8");
    }

    void initWebView(final View view) {
        wv_produce=(WebView) view.findViewById(R.id.wv_produce1);
        wv_produce.loadUrl("file:///android_asset/index.html");
        WebSettings webSettings = wv_produce.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wv_produce.getSettings().setSupportZoom(true);
        wv_produce.getSettings().setAllowFileAccess(true);
        wv_produce.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        wv_produce.getSettings().setJavaScriptEnabled(true);
        wv_produce.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        wv_produce.getSettings().setLoadWithOverviewMode(true);
        wv_produce.getSettings().setDefaultTextEncodingName("utf-8");
    }

    private void initView(final View view) {
        transactions_res = getTransactions();
        String[] lines = transactions_res.split(";");
        String[][] DATA_TO_SHOW = new String[lines.length][4];
        for (int i = 0; i < lines.length; ++i) {
            String[] items = lines[i].split(",");
            if (items.length != 4) {
                continue;
            }

            DATA_TO_SHOW[i][0] = items[0];
            DATA_TO_SHOW[i][1] = items[1];
            DATA_TO_SHOW[i][2] = items[2].substring(0, 5).toUpperCase() + "..." + items[2].substring(items[2].length() - 5).toUpperCase();
            DATA_TO_SHOW[i][3] = items[3];
        }

        Button hide_dlg = (Button) view.findViewById(R.id.dlg_hide_dialog);
        hide_dlg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bottom_dialog.dismiss();
            }
        });

        Button copy_prikey = (Button) view.findViewById(R.id.dlg_copy_prikey);
        copy_prikey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                copyPrikey(v);
            }
        });

        Button paste_prikey = (Button) view.findViewById(R.id.dlg_paste_prikey);
        paste_prikey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pastePrikey(v);
            }
        });

        Button copy_id = (Button) view.findViewById(R.id.dlg_copy_account_id);
        copy_id.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                copyAccount(v);
            }
        });

        Button buy_tenon = (Button) view.findViewById(R.id.dlg_buy_button);
        buy_tenon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buyTenon(v);
            }
        });

        EditText prikey_text=(EditText)view.findViewById(R.id.dlg_private_key);
        prikey_text.setText(P2pLibManager.getInstance().private_key.toUpperCase());
        EditText acc_text=(EditText)view.findViewById(R.id.dlg_account_address);
        acc_text.setText(P2pLibManager.getInstance().account_id.toUpperCase());

        TextView balance = (TextView)findViewById(R.id.balance_lego);
        TextView dlg_balance = (TextView)view.findViewById(R.id.dlg_balance_lego);
        dlg_balance.setText(balance.getText());

        TextView balance_d = (TextView)findViewById(R.id.balance_dollar);
        TextView dlg_balance_d = (TextView)view.findViewById(R.id.dlg_balance_dollar);
        dlg_balance_d.setText(balance_d.getText());

        String[] data_header ={"datetime", "type", "account", "amount"};
        final SimpleTableHeaderAdapter simpleTableHeaderAdapter = new SimpleTableHeaderAdapter(MainActivity.this, data_header);
        simpleTableHeaderAdapter.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.white));
        simpleTableHeaderAdapter.setTextSize(14);
        TableView<String[]> tableView = (TableView<String[]>) view.findViewById(R.id.tableView);

        final int rowColorEven = ContextCompat.getColor(MainActivity.this, R.color.env);
        final int rowColorOdd = ContextCompat.getColor(MainActivity.this, R.color.odd);
        tableView.setDataRowBackgroundProvider(TableDataRowBackgroundProviders.alternatingRowColors(rowColorEven, rowColorOdd));

        final TableColumnWeightModel tableColumnWeightModel = new TableColumnWeightModel(4);
        tableColumnWeightModel.setColumnWeight(0, 35);
        tableColumnWeightModel.setColumnWeight(1, 15);
        tableColumnWeightModel.setColumnWeight(2, 30);
        tableColumnWeightModel.setColumnWeight(3, 20);
        tableView.setColumnModel(tableColumnWeightModel);

        tableView.setHeaderAdapter(simpleTableHeaderAdapter);
        if (!transactions_res.isEmpty()) {
            SimpleTableDataAdapter dataAdapter = new SimpleTableDataAdapter(MainActivity.this, DATA_TO_SHOW);
            dataAdapter.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.dark_gray));
            dataAdapter.setTextSize(12);
            tableView.setDataAdapter(dataAdapter);
        }

        global_mode_checkbox = (CheckBox) view.findViewById(R.id.global_mode_check);
        global_mode_checkbox.setChecked(ProxyConfig.Instance.globalMode);
        global_mode_checkbox.setOnCheckedChangeListener(MainActivity.this);
    }

    public void showWebview(View view) {
        if (LocalVpnService.IsRunning != true) {
            Toast.makeText(this, getString(R.string.connect_first_string), Toast.LENGTH_SHORT).show();
            return;
        }
        webview_dialog.show();
        web_view_open_btn.setText("");
        tilte_text_view.setText("");
    }

    public void hideWebview(View view) {
        web_view_open_btn.setText(getString(R.string.navigation_string));
        tilte_text_view.setText("TenonVPN");
        webview_dialog.dismiss();
    }

    public void webPrev(View view) {
        if (wv_produce.canGoBack()) {
            wv_produce.goBack();
        }
    }

    public void webNext(View view) {
        if (wv_produce.canGoForward()) {
            wv_produce.goForward();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isExit) {
                LocalVpnService.IsRunning = false;
                LocalVpnService.removeOnStatusChangedListener(this);
                p2pDestroy();
                this.finish();
                System.exit(0);
            } else {
                Toast.makeText(this, getString(R.string.exit_check), Toast.LENGTH_SHORT).show();
                isExit = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isExit= false;
                    }
                }, 2000);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void showDialog(View view) {
        bottom_dialog.show();
    }

    public void showBandwidthOutDialog(View view) {
        outof_bandwidth_dialog.show();
    }

    public void checkVer(View view) {
        version_download_url = "";
        String ver = checkVersion();

        if (ver.isEmpty()) {
            if (view != null) {
                Toast.makeText(this, getString(R.string.latest_version), Toast.LENGTH_SHORT).show();
            }
        } else {
            String[] downs = ver.split(",");
            for (int i = 0; i < downs.length; ++i) {
                String[] item = downs[i].split(";");

                if (item.length < 2) {
                    continue;
                }

                if (item[0].equals("android")) {
                    if (item[1].compareTo(P2pLibManager.getInstance().kCurrentVersion) <= 0) {
                        if (view != null) {
                            Toast.makeText(this, getString(R.string.latest_version), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        version_download_url = item[2];
                    }
                }

                if (item[0].equals("share_ip")) {
                    if (!item[1].isEmpty()) {
                        P2pLibManager.getInstance().share_ip = item[1];
                    }
                }

                if (item[0].equals("buy_ip")) {
                    if (!item[1].isEmpty()) {
                        P2pLibManager.getInstance().buy_tenon_ip = item[1];
                    }
                }

                if (item[0].equals("dr")) {
                    if (!item[1].isEmpty()) {
                        P2pLibManager.getInstance().SetDefaultRouting(item[1]);
                    }
                }

                if (item[0].equals("er")) {
                    if (!item[1].isEmpty()) {
                        P2pLibManager.getInstance().SetExRouting(item[1]);
                    }
                }
            }

            if (version_download_url.isEmpty()) {
                if (view != null) {
                    Toast.makeText(this, getString(R.string.latest_version), Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (view != null) {
                upgrade_dialog_showing = true;
                upgrade_dialog.show();
            }
        }
    }

    public void upgradeNow(View view)
    {
        Uri uri = Uri.parse(version_download_url);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setData(uri);
        startActivity(intent);
        upgrade_dialog.dismiss();
        upgrade_dialog_showing = false;

    }

    private void InitSpinner() {
        mSpinner = (Spinner) findViewById(R.id.spinner);
        country_vec.add("America");
        country_vec.add("Brazil");
        country_vec.add("Germany");
        country_vec.add("France");
        country_vec.add("Korea");
        country_vec.add("Netherlands");
        country_vec.add("Canada");
        country_vec.add("Australia");
        country_vec.add("Portugal");
        country_vec.add("Japan");
        country_vec.add("Hong Kong");
        country_vec.add("New Zealand");
        country_vec.add("India");
        country_vec.add("Indonesia");
        country_vec.add("England");
        country_vec.add("China");

        country_to_short.put("Australia", "AU");
        country_to_short.put("Singapore", "SG");
        country_to_short.put("Brazil", "BR");
        country_to_short.put("Germany", "DE");
        country_to_short.put("France", "FR");
        country_to_short.put("Korea", "KR");
        country_to_short.put("Netherlands", "NL");
        country_to_short.put("Canada", "CA");
        country_to_short.put("America", "US");
        country_to_short.put("Portugal", "PT");
        country_to_short.put("Japan", "JP");
        country_to_short.put("Hong Kong", "HK");
        country_to_short.put("New Zealand", "NZ");
        country_to_short.put("India", "IN");
        country_to_short.put("Indonesia", "ID");
        country_to_short.put("England", "GB");
        country_to_short.put("China", "CN");

        for (String value: country_to_short.values()) {
            vpn_country_list.add(value);
        }
        spinnerTitles = new String[]{"America", "Singapore", "Brazil","Germany", "Netherlands","France","Korea", "Japan", "Canada","Australia","Hong Kong", "India", "England", "China"};
        spinnerImages = new int[]{
                R.drawable.us
                , R.drawable.sg
                , R.drawable.br
                , R.drawable.de
                , R.drawable.nl
                , R.drawable.fr
                , R.drawable.kr
                , R.drawable.jp
                , R.drawable.ca
                , R.drawable.au
                , R.drawable.hk
                , R.drawable.in
                , R.drawable.gb
                , R.drawable.cn
        };

        CustomAdapter mCustomAdapter = new CustomAdapter(MainActivity.this, spinnerTitles, spinnerImages);
        mSpinner.setAdapter(mCustomAdapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                now_choosed_country = country_to_short.get(spinnerTitles[i]);
                P2pLibManager.getInstance().choosed_country = country_to_short.get(spinnerTitles[i]);
                LocalVpnService.IsRunning = false;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        P2pLibManager.getInstance().Init();
        InitSpinner();
        ProxyConfig.Instance.globalMode = false;
        mCircleView = (CircleProgressView) findViewById(R.id.circleView);
        mCircleView.setValue(100);
        mCircleView.setBarColor(getResources().getColor(R.color.disconnect_succ_out));

        mCalendar = Calendar.getInstance();
        LocalVpnService.addOnStatusChangedListener(this);

        web_view_open_btn = findViewById(R.id.web_open_buton);
        tilte_text_view = findViewById(R.id.title_text);

        TextView share_text = findViewById(R.id.share_link_text);
        String htmlString="<u>" + getString(R.string.share_vip) + "</u>";
        share_text.setText(Html.fromHtml(htmlString));

        if (!P2pLibManager.getInstance().InitNetwork(this)) {
            Toast.makeText(this, getString(R.string.init_failed) , Toast.LENGTH_SHORT).show();
            try {
                Thread.sleep(1000);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
            this.finish();
            return;
        }

        int p2p_socket = getP2PSocket();
        if (!vpn_service.protect(p2p_socket)) {
            Log.e(TAG,"protect vpn socket failed");
        }

        //Pre-App Proxy
        if (AppProxyManager.isLollipopOrAbove){
            new AppProxyManager(this);
        }
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.tip);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);

        ProxyConfig.Instance.globalMode = P2pLibManager.getInstance().GetGlobalMode();
        Log.e(TAG, "get local country: " + P2pLibManager.getInstance().local_country);
        createAccount();

        Thread t1 = new Thread(check_tx,"check tx");
        t1.start();
        Log.e("init", "init OK");
        bottom_dialog = BottomDialog.create(getSupportFragmentManager())
                .setViewListener(new BottomDialog.ViewListener() {
                    @Override
                    public void bindView(View v) {
                        initView(v);
                    }
                })
                .setLayoutRes(R.layout.dialog_layout)
                .setDimAmount(0.1f)
                .setCancelOutside(false)
                .setTag("BottomDialog");

        upgrade_dialog = BottomDialog.create(getSupportFragmentManager())
                .setViewListener(new BottomDialog.ViewListener() {
                    @Override
                    public void bindView(View view) {
                        Button upgrade_btn = (Button) view.findViewById(R.id.dlg_upgrade_button);
                        upgrade_btn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                upgradeNow(v);
                            }
                        });

                        Button hide_upgrade = (Button) view.findViewById(R.id.dlg_upgrade_hide);
                        hide_upgrade.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                hideUpgrade(v);
                            }
                        });
                    }
                })
                .setLayoutRes(R.layout.upgrade)
                .setDimAmount(0.1f)
                .setCancelOutside(false)
                .setTag("UpgradeDialog");

        webview_dialog = BottomDialog.create(getSupportFragmentManager());
        webview_dialog.setViewListener(new BottomDialog.ViewListener() {
                    @Override
                    public void bindView(View v) {
                        initWebView(v);

                        webview_dialog.getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
                        webview_dialog.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                                if (i == KeyEvent.KEYCODE_BACK) {
                                    long now_timestamp = System.currentTimeMillis();
                                    if (now_timestamp - goback_prev_timestamp <= 200) {
                                        return true;
                                    }

                                    goback_prev_timestamp = now_timestamp;
                                    if (wv_produce.canGoBack()) {
                                        wv_produce.goBack();
                                        return true;
                                    } else {
                                        web_view_open_btn.setText(getString(R.string.navigation_string));
                                        tilte_text_view.setText("TenonVPN");
                                        webview_dialog.dismiss();
                                        return false;
                                    }
                                }
                                return true;
                            }
                        });
                    }
                })
                .setLayoutRes(R.layout.link_web)
                .setDimAmount(0.1f)
                .setCancelOutside(false)
                .setTag("WebviewDialog");

        outof_bandwidth_dialog = BottomDialog.create(getSupportFragmentManager());
        outof_bandwidth_dialog.setViewListener(new BottomDialog.ViewListener() {
            @Override
            public void bindView(View view) {
                Button bwo_buy_tenon = (Button) view.findViewById(R.id.dlg_bwo_buy_tenon);
                bwo_buy_tenon.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        buyTenon(v);
                    }
                });

                Button bwo_hide = (Button) view.findViewById(R.id.dlg_bwo_hide);
                bwo_hide.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        outof_bandwidth_dialog.dismiss();
                    }
                });

                outof_bandwidth_dialog.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                        if (i == KeyEvent.KEYCODE_BACK) {
                            long now_timestamp = System.currentTimeMillis();
                            if (now_timestamp - goback_prev_timestamp <= 200) {
                                return true;
                            }

                            goback_prev_timestamp = now_timestamp;
                            if (pay_view.canGoBack()) {
                                pay_view.goBack();
                                return true;
                            } else {
                                outof_bandwidth_dialog.dismiss();
                                return false;
                            }
                        }
                        return true;
                    }
                });
            }
        }) .setLayoutRes(R.layout.outof_bandwidth).setDimAmount(0.1f).setCancelOutside(false).setTag("outbandwidthDialog");
    }

    String getVersionName() {
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            Log.e(TAG, "null package manager is impossible");
            return null;
        }

        try {
            return packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "package not found is impossible", e);
            return null;
        }
    }

    boolean isValidUrl(String url) {
        try {
            if (url == null || url.isEmpty())
                return false;

            if (url.startsWith("ss://")) {//file path
                return true;
            } else { //url
                Uri uri = Uri.parse(url);
                if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
                    return false;
                if (uri.getHost() == null)
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onClick(View v) {

    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onLogReceived(String logString) {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        logString = String.format("[%1$02d:%2$02d:%3$02d] %4$s\n",
                mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE),
                mCalendar.get(Calendar.SECOND),
                logString);

        System.out.println(logString);
    }

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        CircleButton c_btn = (CircleButton)findViewById(R.id.start_vpn);
        c_btn.setEnabled(true);
        if (isRunning) {
            c_btn.setColor(getResources().getColor(R.color.connect_succ_in));
            //possiblyShowGooglePayButton();
            mCircleView.stopSpinning();
            mCircleView.setSpinSpeed(10);
            c_btn.setImageDrawable(getResources().getDrawable(R.drawable.connected));
            mCircleView.setValueAnimated(100);
            mCircleView.setBarColor(getResources().getColor(R.color.connect_succ_out));
        } else {
            c_btn.setColor(getResources().getColor(R.color.disconnect_succ_in));
            mCircleView.stopSpinning();
            mCircleView.setBarColor(getResources().getColor(R.color.disconnect_succ_out));
            c_btn.setImageDrawable(getResources().getDrawable(R.drawable.connect));
            mCircleView.setValue(100);
            mCircleView.setSpinSpeed(2);
        }
    }

    public void startVpn(View view) {
        TextView notice_txt = (TextView)findViewById(R.id.notice_text);
        notice_txt.setText("");
        if (P2pLibManager.getInstance().now_status.equals("bwo")) {
            if (P2pLibManager.getInstance().now_balance >= P2pLibManager.getInstance().min_payfor_vpn_tenon || P2pLibManager.getInstance().vip_level > 0) {
                P2pLibManager.getInstance().now_status = "ok";
                P2pLibManager.getInstance().PayforVpn();
            } else {
                showBandwidthOutDialog(view);
                // Toast.makeText(MainActivity.this, getString(R.string.bandwidth_over), Toast.LENGTH_SHORT).show();
            }
            return;
        }

//        if (P2pLibManager.getInstance().now_status.equals("cni")) {
//            Toast.makeText(MainActivity.this, getString(R.string.invalid_country), Toast.LENGTH_SHORT).show();
//            return;
//        }

        if (LocalVpnService.IsRunning != true) {
            Intent intent = LocalVpnService.prepare(this);
            if (intent == null) {
                startVPNService();
            } else {
                startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
                startVPNService();
            }
        } else {
            LocalVpnService.IsRunning = false;
        }
    }
    @Override
    public void onCheckedChanged(CompoundButton checkBox, boolean isChecked) {
        switch (checkBox.getId()) {
            case R.id.global_mode_check:
                if (isChecked) {
                    ProxyConfig.Instance.globalMode = true;
                } else {
                    ProxyConfig.Instance.globalMode = false;
                }

                P2pLibManager.getInstance().SaveGlobalMode(ProxyConfig.Instance.globalMode);
                break;
            default:
                break;
        }
    }

    private String ChooseRouteProxyUrl(String dest_country) {
        {
            String nodes = getRouteNodes(dest_country);
            System.out.println("getRouteNodes: " + nodes);
            if (!nodes.isEmpty()) {
                String[] node_list = nodes.split(",");
                int rand_num = (int) (Math.random() * node_list.length);
                String[] item_split = node_list[rand_num].split(":");
                if (item_split.length >= 6) {
                    return "ss://aes-128-cfb:passwd@" + item_split[0] + ":" + item_split[2];
                }
            }
        }

        if (country_route_map.containsKey(now_choosed_country)) {
            String nodes = getRouteNodes(now_choosed_country);
            if (!nodes.isEmpty()) {
                String[] node_list = nodes.split(",");
                int rand_num = (int)(Math.random() * node_list.length);
                String[] item_split = node_list[rand_num].split(":");
                if (item_split.length >= 6) {
                    return "ss://aes-128-cfb:passwd@" + item_split[0] + ":" + item_split[2];
                }
            }
        }

        for (String key : country_route_map.keySet()) {
            String nodes = getRouteNodes(key);
            if (!nodes.isEmpty()) {
                String[] node_list = nodes.split(",");
                int rand_num = (int) (Math.random() * node_list.length);
                String[] item_split = node_list[rand_num].split(":");
                if (item_split.length >= 6) {
                    return "ss://aes-128-cfb:passwd@" + item_split[0] + ":" + item_split[2];
                }
            }
        }
        return "";
    }

    private String ChooseVpnProxyUrl() {
        {
            String nodes = getVpnNodes(now_choosed_country);
            if (!nodes.isEmpty()) {
                String[] node_list = nodes.split(",");
                int rand_num = (int)(Math.random() * node_list.length);
                String[] item_split = node_list[rand_num].split(":");
                if (item_split.length >= 6) {
                    return node_list[rand_num];
                }
            }
        }

        String def_vpn_coutry[] = {"US", "AU", "CA", "FR"};
        for (String key : def_vpn_coutry) {
            if (key.equals(P2pLibManager.getInstance().local_country)) {
                continue;
            }

            String nodes = getVpnNodes(key);
            if (!nodes.isEmpty()) {
                String[] node_list = nodes.split(",");
                int rand_num = (int)(Math.random() * node_list.length);
                String[] item_split = node_list[rand_num].split(":");
                if (item_split.length >= 6) {
                    return node_list[rand_num];
                }
            }
        }
        return "";
    }

    private void startVPNService() {
        String route_proxy_url = ChooseRouteProxyUrl(P2pLibManager.getInstance().local_country);
        System.out.println("get routing node: " + route_proxy_url);
        if (!isValidUrl(route_proxy_url)) {
            Toast.makeText(this, "Waiting Decentralized Routing...", Toast.LENGTH_SHORT).show();
            return;
        }

        String vpn_proxy_url = ChooseVpnProxyUrl();
        Log.e(TAG, "vpn proxy url: " + vpn_proxy_url);
        if (vpn_proxy_url.isEmpty()) {
            Toast.makeText(this, "Waiting Decentralized Vpn Server...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] item_split = vpn_proxy_url.split(":");
        if (item_split.length < 7) {
            Toast.makeText(this, "Waiting Decentralized Vpn Server...", Toast.LENGTH_SHORT).show();
            return;
        }
        String direct_vpn_proxy_url = "ss://aes-128-cfb:passwd@" + item_split[0] + ":" + item_split[1];
        //String login_gid = vpnLogin(item_split[6]);
        //Log.e(TAG, "join account address:" + item_split[6]);


        boolean res = P2pLibManager.getInstance().GetVpnNode();
        if(!res) {
            Toast.makeText(this, "Waiting Decentralized Vpn Server...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (P2pLibManager.getInstance().use_smart_route) {
            //Toast.makeText(this, " to " + direct_vpn_proxy_url +  " use " + route_proxy_url, Toast.LENGTH_SHORT).show();
            choosed_vpn_url = vpn_proxy_url;
            LocalVpnService.ProxyUrl = route_proxy_url;
            Log.e(TAG, " to " + vpn_proxy_url +  " use " + route_proxy_url);
        } else {
            //Toast.makeText(this, "local " + P2pLibManager.getInstance().local_country + " direct to " + now_choosed_country + ", " + direct_vpn_proxy_url, Toast.LENGTH_SHORT).show();
            choosed_vpn_url = vpn_proxy_url;
            LocalVpnService.ProxyUrl = direct_vpn_proxy_url;
        }
        startService(new Intent(this, LocalVpnService.class));
        CircleButton c_btn = (CircleButton)findViewById(R.id.start_vpn);
        c_btn.setEnabled(false);
        LocalVpnService.IsRunning = true;
        mCircleView.spin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_about:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_name) + getVersionName())
                        .setMessage(R.string.about_info)
                        .setPositiveButton(R.string.btn_ok, null)
                        .setNegativeButton(R.string.btn_more, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                                // startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("")));
                            }
                        })
                        .show();

                return true;
            case R.id.menu_item_exit:
                if (!LocalVpnService.IsRunning) {
                    finish();
                    return true;
                }

                new AlertDialog.Builder(this)
                        .setTitle(R.string.menu_item_exit)
                        .setMessage(R.string.exit_confirm_info)
                        .setPositiveButton(R.string.btn_ok, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LocalVpnService.IsRunning = false;
                                LocalVpnService.Instance.disconnectVPN();
                                stopService(new Intent(MainActivity.this, LocalVpnService.class));
                                System.runFinalization();
                                System.exit(0);
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        LocalVpnService.IsRunning = false;
        LocalVpnService.removeOnStatusChangedListener(this);
        p2pDestroy();
        super.onDestroy();
    }

    public class CheckTransaction extends ListActivity implements Runnable {
        public List<String> gid_list = new ArrayList<String>();
        private ArrayList<String> not_get_country_list = new ArrayList<String>();
        private int bandwidth_used = 0;
        public void run() {
            for (String value: country_to_short.values()) {
                not_get_country_list.add(value);
            }

            while (true) {
//                for (int i = 0; i < not_get_country_list.size(); ++i) {
//                    String vpn_url = getVpnNodes(not_get_country_list.get(i));
//                    if (!vpn_url.isEmpty()) {
//                        vpn_url = not_get_country_list.get(i) + "\t" + vpn_url;
//                        Message message = new Message();
//                        message.what = GOT_VPN_SERVICE;
//                        message.obj = vpn_url;
//                        handler.sendMessage(message);
//                    }
//
//                    String route_url = getRouteNodes(not_get_country_list.get(i));
//                    if (!route_url.isEmpty()) {
//                        route_url = not_get_country_list.get(i) + "\t" + route_url;
//                        Message message = new Message();
//                        message.what = GOT_VPN_ROUTE;
//                        message.obj = route_url;
//                        handler.sendMessage(message);
//                    }
//                }

                {
                    long now_balance = getBalance();
                    P2pLibManager.getInstance().SetBalance(now_balance);
                    if (now_balance != -1) {
                        Message message = new Message();
                        message.what = GOT_BALANCE;
                        message.obj = now_balance;
                        handler.sendMessage(message);
                    }
                }

                {
                    if (!P2pLibManager.getInstance().now_status.equals("OK")) {
                        Message message = new Message();
                        message.what = GOT_INVALID_STATUS;
                        handler.sendMessage(message);
                    }
                }

                vpn_service.protect(getP2PSocket());
                if (check_vip_times < 1) {
                    String res = P2pLibManager.checkVip();
                    String[] items = res.split(",");
                    if (items.length == 2) {
                        long tm = Long.parseLong(items[0]);
                        long amount = Long.parseLong(items[1]);
                        if (P2pLibManager.getInstance().payfor_timestamp == 0 || tm != Long.MAX_VALUE) {
                            if (tm != Long.MAX_VALUE && tm != 0)
                            {
                                check_vip_times = 11;
                            }
                            P2pLibManager.getInstance().payfor_timestamp = tm;
                            P2pLibManager.getInstance().payfor_amount = amount;
                        }
                        check_vip_times++;
                    }
                } else {
                    P2pLibManager.getInstance().PayforVpn();
                    Message message = new Message();
                    message.what = GOT_CHANGE_VIP_STATUS;
                    handler.sendMessage(message);
                }

                if (P2pLibManager.getInstance().now_balance == -1) {
                    createAccount();
                }

                String new_bootstrap = getNewBootstrap();
                P2pLibManager.getInstance().SaveNewBootstrapNodes(new_bootstrap);
                checkVer(null);
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void AddTxGid(String tx_gid) {
            synchronized (this) {
                gid_list.add(tx_gid);
            }
        }
    }

    public native int getP2PSocket();
    public native String createAccount();
    public native String getVpnNodes(String country);
    public native String getRouteNodes(String country);
    public native String getTransactions();
    public native long getBalance();
    public native String checkVersion();
    public native void p2pDestroy();
    public native String getNewBootstrap();

}
