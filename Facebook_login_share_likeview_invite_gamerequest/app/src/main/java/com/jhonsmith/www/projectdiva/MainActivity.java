package com.jhonsmith.www.projectdiva;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.AppInviteDialog;
import com.facebook.share.widget.GameRequestDialog;
import com.facebook.share.widget.LikeView;
import com.facebook.share.widget.ShareDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    Context m_context;
    Activity m_activity;
    CallbackManager m_callbackmanager;
    Button m_fb_login_button;
    LinearLayout m_fb_content_linear;
    LinearLayout m_fb_friends_linear;
    LikeView m_likeview;
    GameRequestDialog m_gamerequest_dialog;
    ShareDialog m_sharedialog;
    AppInviteDialog m_appinivte_dialog;
    FacebookCallback<LoginResult> m_facbeook_logincallback;             //facebook login callback
    FacebookCallback<GameRequestDialog.Result> m_facebook_gamerequest_callback;    //facebook gamerequest callback
    FacebookCallback<ShareDialog.Result> m_facebook_sharedialog_callback;           //facebook share callback
    FacebookCallback<AppInviteDialog.Result> m_facebook_appinvite_callback;     //facebook app invite callback
    Map<String, Bitmap> m_listview_bitmapcache_map;             //好友列表  头像缓存map
    List<Map<String, String>> m_fb_friendslist;                 //好友数据
    String m_facbeook_object = "";               //facebook object id
    List<String> m_temp_string_list;
    enum Fb_logintype {          // facebook login type sign
        LOGIN, SHARE, REQUEST
    }

    enum Fb_sharetype {             //facebook share api type
        LINK, OBJECT, SCREENSHOT
    }

    Fb_logintype m_fb_logintype = Fb_logintype.LOGIN;
    Fb_sharetype m_fb_sharetype = Fb_sharetype.LINK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_context = getApplicationContext();
        m_activity = MainActivity.this;
        init_facebook();
        m_fb_login_button = (Button) findViewById(R.id.facebook_login_button);
        m_fb_content_linear = (LinearLayout) findViewById(R.id.facebook_content_linearlayout);
        m_fb_friends_linear = (LinearLayout) findViewById(R.id.facbeookfriends_linear);
    }

    private void init_facebook() {
        FacebookSdk.sdkInitialize(getApplicationContext());
        m_callbackmanager = CallbackManager.Factory.create();

        //login listener
        m_facbeook_logincallback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                switch (m_fb_logintype) {
                    case LOGIN: {
                        LOGD("facebook login succ token = " + loginResult.getAccessToken().getUserId(), m_context);
                        m_fb_login_button.setVisibility(View.GONE);
                        m_fb_content_linear.setVisibility(View.VISIBLE);
                        break;
                    }
                    case SHARE: {
                        LOGD("facebook login via share succ " + loginResult.getAccessToken(), m_context);
                        share();
                        break;
                    }
                    case REQUEST: {
                        LOGD("facebook login via graph request " + loginResult.getAccessToken(), m_context);
                        get_inapp_friends();
                        break;
                    }
                    default:
                        break;
                }
            }

            @Override
            public void onCancel() {
                switch (m_fb_logintype) {
                    case LOGIN: {
                        LOGD("facebook login user cancel", null);
                        break;
                    }
                    case SHARE: {
                        LOGD("facebook login via share cancel", m_context);
                        break;
                    }
                    default:
                        LOGD("facebook login cancel", m_context);
                        break;
                }
            }

            @Override
            public void onError(FacebookException error) {
                switch (m_fb_logintype) {
                    case LOGIN: {
                        LOGD("facebook login error " + error.getMessage(), m_context);
                        break;
                    }
                    case SHARE: {
                        LOGD("facebook login via share error " + error.getMessage(), m_context);
                        break;
                    }
                    default:
                        LOGD("facebook login error " + error.getMessage(), m_context);
                        break;
                }
            }
        };
        //login manager add callback
        LoginManager.getInstance().registerCallback(m_callbackmanager, m_facbeook_logincallback);

        //facebook likeview init
        m_likeview = (LikeView) findViewById(R.id.facebook_likeview);
        m_likeview.setObjectIdAndType("https://www.facebook.com/pages/CLANNAD/108202942533689", LikeView.ObjectType.PAGE);          //https://developers.facebook.com/docs/reference/android/current/class/LikeView

        //facebook game request dialog init
        m_gamerequest_dialog = new GameRequestDialog(m_activity);
        //facebook game request call back
        m_facebook_gamerequest_callback = new FacebookCallback<GameRequestDialog.Result>() {
            @Override
            public void onSuccess(GameRequestDialog.Result result) {
                LOGD("facebook game request succ " + result.getRequestRecipients().toString(), m_context);
            }

            @Override
            public void onCancel() {
                LOGD("facebook game request cancel", null);
            }

            @Override
            public void onError(FacebookException error) {
                LOGD("facebook game request error " + error.getMessage(), m_context);
            }
        };
        m_gamerequest_dialog.registerCallback(m_callbackmanager, m_facebook_gamerequest_callback);

        //facebook share init
        m_sharedialog = new ShareDialog(m_activity);
        //facebook share callback
        m_facebook_sharedialog_callback = new FacebookCallback<ShareDialog.Result>() {
            @Override
            public void onSuccess(ShareDialog.Result result) {
                LOGD("facebook share succ " + result.getPostId(), m_context);
            }

            @Override
            public void onCancel() {
                LOGD("facebook share cancel", null);
            }

            @Override
            public void onError(FacebookException error) {
                LOGD("facebook share error " + error.getMessage(), m_context);
            }
        };
        m_sharedialog.registerCallback(m_callbackmanager, m_facebook_sharedialog_callback);

        //facebook app invite init
        m_appinivte_dialog = new AppInviteDialog(m_activity);
        //facbeook app invite callback init
        m_facebook_appinvite_callback = new FacebookCallback<AppInviteDialog.Result>() {
            @Override
            public void onSuccess(AppInviteDialog.Result result) {
                LOGD("facebook app invite succ " + result.getData(), m_context);
            }

            @Override
            public void onCancel() {
                LOGD("facebook app invite cancel", m_context);
            }

            @Override
            public void onError(FacebookException error) {
                LOGD("facebook app invite error " + error.getMessage(), m_context);
            }
        };
        m_appinivte_dialog.registerCallback(m_callbackmanager, m_facebook_appinvite_callback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //facebook callback manager
        m_callbackmanager.onActivityResult(requestCode, resultCode, data);
    }

    //onclick method
    public void cokcok(View view) {
        switch (view.getId()) {
            case R.id.facebook_login_button: {
                m_fb_logintype = Fb_logintype.LOGIN;                            //https://developers.facebook.com/docs/reference/android/current/class/LoginManager/
                LoginManager.getInstance().logInWithReadPermissions(m_activity, Arrays.asList("public_profile", "user_friends"));   //facbeook login with read permission
                break;
            }
            case R.id.facebook_me_likeview: {
                LOGD("facebook good clicked", m_context);
                try {
                    ((ViewGroup) m_likeview.getChildAt(0)).getChildAt(0).callOnClick();   //模拟facebook 点赞按钮点击事件.
                } catch (Exception e) {
                    //pass
                }
                break;
            }
            case R.id.facebook_appinvite_button: {              //https://developers.facebook.com/docs/app-invites/android
                String appLinkUrl = "https://fb.me/589810544543824";                            //facebook app link 托管平台 https://developers.facebook.com/quickstarts/?platform=app-links-host
                String preview_imageUrl = "https://s-media-cache-ak0.pinimg.com/originals/44/89/ed/4489ed44e908e9c6ec7fbec691c1e700.png";
                if (m_appinivte_dialog.canShow()) {
                    AppInviteContent invite_content = new AppInviteContent.Builder()
                            .setApplinkUrl(appLinkUrl)
                            .setPreviewImageUrl(preview_imageUrl)
                            .build();
                    m_appinivte_dialog.show(m_activity, invite_content);
                }
                break;
            }
            case R.id.facebook_gamerequest_button: {
                GameRequestContent content = new GameRequestContent.Builder()               //https://developers.facebook.com/docs/games/services/gamerequests
                        .setTitle("gamerequest title")              //显示在上边栏
                        .setMessage("gamerequest message")          //邀请中显示的信息
                        .build();
                m_gamerequest_dialog.show(content);
                break;
            }
            case R.id.facebook_linkshare_button: {
                m_fb_sharetype = Fb_sharetype.LINK;
                share();
                break;
            }
            case R.id.facebook_objectshare_button: {
                m_fb_sharetype = Fb_sharetype.OBJECT;
                share();
                break;
            }
            case R.id.facebook_screenshot_share_button: {
                m_fb_sharetype = Fb_sharetype.SCREENSHOT;
                share();
                break;
            }
            case R.id.facebook_inapp_friends_button: {
                get_inapp_friends();
                break;
            }
            case R.id.return_button: {
                m_fb_content_linear.setVisibility(View.VISIBLE);
                m_fb_friends_linear.setVisibility(View.GONE);
                break;
            }
            case R.id.gamerequest_send_button: {
                if (m_fb_friendslist != null && m_fb_friendslist.size() >= 0) {
                    List<String> ids = new ArrayList<>();
                    for (Map<String, String> map : m_fb_friendslist) {
                        if (map.containsKey("check") && map.containsKey("id") && map.get("check").equals("t")) {
                            ids.add(map.get("id"));
                        }
                    }
                    if(ids.size() >= 0){
                        /*
                        GameRequestContent turn_content = new GameRequestContent.Builder()              //应用邀请, action type TRURN 不需要新建object, 其他两种需要新建Object
                                .setMessage("facebook turn action invite")
                                .setRecipients(ids)
                                .setActionType(GameRequestContent.ActionType.TURN)
                                .build();
                        m_gamerequest_dialog.show(turn_content);
                        */
                        m_temp_string_list = ids;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String object_id = get_facebook_product_object();
                                if(!object_id.equals("")){
                                    GameRequestContent askfor_or_sendto_content = new GameRequestContent.Builder()
                                            .setMessage("facebook askfor or sendto action invite")
                                            .setRecipients(m_temp_string_list)
                                            .setActionType(GameRequestContent.ActionType.SEND)              //这里可以设置send 跟 ask for 类型 逻辑是一致的
                                            .setObjectId(object_id)
                                            .build();
                                    m_gamerequest_dialog.show(askfor_or_sendto_content);
                                }
                            }
                        }).start();
                    }
                }
            }
            default:
                break;
        }
    }

    private String get_facebook_product_object(){
        if(m_facbeook_object.equals("")){
            Bundle params = new Bundle();           //目前facbeook 不支持自定义object, 只能使用 facebook    官方提供的object, 以下以product为例, 发出去会显示
            params.putString("object", "{'og':{'type':'product','title':'life'}}");             //https://developers.facebook.com/docs/reference/opengraph/object-type/product/
            GraphRequest request = new GraphRequest(
                    AccessToken.getCurrentAccessToken(),
                    "me/objects/product",
                    params,
                    HttpMethod.POST,
                    new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse response) {
                            try{
                                JSONObject jo = response.getJSONObject();
                                m_facbeook_object = jo.getString("id");
                            }catch(Exception e){
                                LOGD("create object error " + e.getMessage(), null);
                                m_facbeook_object = "";
                            }
                        }
                    }
            );
            request.executeAndWait();
        }
        return m_facbeook_object;
    }


    private void get_inapp_friends() {   //通过图谱API 获得授权过此应用的好友      https://developers.facebook.com/docs/graph-api
        if (AccessToken.getCurrentAccessToken() == null || !AccessToken.getCurrentAccessToken().getPermissions().contains("user_friends")) {
            m_fb_logintype = Fb_logintype.REQUEST;
            LoginManager.getInstance().logInWithReadPermissions(m_activity, Arrays.asList("user_friends"));
            return;
        }
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "me/friends?fields=id,name,picture.height(200)",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        List<Map<String, String>> temp_list = new ArrayList<>();        //从返回数据里面 解析出想要使用的数据
                        m_listview_bitmapcache_map = new HashMap<>();           //用来存储用户头像, 防止多次下载
                        try {
                            JSONObject jo = response.getJSONObject();
                            JSONArray ja = jo.getJSONArray("data");
                            if (ja.length() > 0) {
                                for (int i = 0; i < ja.length(); i++) {
                                    JSONObject joo = ja.getJSONObject(i);
                                    Map<String, String> temp_map = new HashMap<>();
                                    temp_map.put("id", joo.getString("id"));
                                    temp_map.put("name", joo.getString("name"));
                                    temp_map.put("picture", joo.getJSONObject("picture").getJSONObject("data").getString("url"));
                                    temp_map.put("check", "f");
                                    temp_list.add(temp_map);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            LOGD("get facebook firends error " + e.getMessage(), m_context);
                        }
                        if (temp_list.size() > 0) {
                            ListView lv = (ListView) findViewById(R.id.facebook_friends_listview);
                            lv.setAdapter(new Me_adapter(m_context, temp_list));
                            m_fb_content_linear.setVisibility(View.GONE);
                            m_fb_friends_linear.setVisibility(View.VISIBLE);
                        }
                    }
                }
        ).executeAsync();
    }

    private void share() {              //https://developers.facebook.com/docs/sharing/android
        if (AccessToken.getCurrentAccessToken() == null || !AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions")) {   //分享之前要先判断用户有没有publish_actions权限
            m_fb_logintype = Fb_logintype.SHARE;
            LoginManager.getInstance().logInWithPublishPermissions(m_activity, Arrays.asList("publish_actions"));
        } else {
            switch (m_fb_sharetype) {
                case LINK: {
                    ShareLinkContent link_content = new ShareLinkContent.Builder()
                            .setContentTitle("clannad dai su ki")
                            .setContentDescription("dan go dai ka zo ku dan go dan go dango")
                            .setContentUrl(Uri.parse("http://www.clannadbar.com"))
                            .setImageUrl(Uri.parse("https://s-media-cache-ak0.pinimg.com/originals/44/89/ed/4489ed44e908e9c6ec7fbec691c1e700.png"))
                            .build();
                    m_sharedialog.show(link_content);
                    break;
                }
                case OBJECT: {
                    ShareOpenGraphObject object = new ShareOpenGraphObject.Builder()    //facebook 不再支持自定义 obejct  需要参照文档使用官方object
                            .putString("og:type", "fitness.course")
                            .putString("og:title", "Sample Course")
                            .putString("og:description", "This is a sample course.")
                            .putInt("fitness:duration:value", 100)
                            .putString("fitness:duration:units", "s")
                            .putInt("fitness:distance:value", 12)
                            .putString("fitness:distance:units", "km")
                            .putInt("fitness:speed:value", 5)
                            .putString("fitness:speed:units", "m/s")
                            .build();
                    ShareOpenGraphAction action = new ShareOpenGraphAction.Builder()
                            .setActionType("fitness.runs")
                            .putObject("fitness:course", object)
                            .build();
                    ShareOpenGraphContent object_content = new ShareOpenGraphContent.Builder()
                            .setPreviewPropertyName("fitness:course")
                            .setAction(action)
                            .build();
                    m_sharedialog.show(object_content);
                    break;
                }
                case SCREENSHOT: {
                    final ImageView temp = (ImageView) findViewById(R.id.test_imageview);
                    temp.setImageBitmap(screen_shot(getWindow().getDecorView().getRootView()));
                    SharePhoto photo = new SharePhoto.Builder()
                            .setBitmap(screen_shot(getWindow().getDecorView().getRootView()))
                            .build();
                    SharePhotoContent screenshot_content = new SharePhotoContent.Builder()
                            .addPhoto(photo)
                            .build();
                    m_sharedialog.show(screenshot_content);
                    break;
                }
                default:
                    break;
            }
        }
    }

    public Bitmap screen_shot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public class Me_adapter extends BaseAdapter {
        Context context;
        LayoutInflater inflater;

        class ElementsHolder {
            TextView id;
            TextView name;
            ImageView picture;
        }

        Me_adapter(Context context, List<Map<String, String>> fb_friendslist) {
            this.context = context;
            m_fb_friendslist = fb_friendslist;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return m_fb_friendslist.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (m_fb_friendslist != null && m_fb_friendslist.size() != 0) {
                ElementsHolder holder;
                if (convertView != null) {
                    holder = (ElementsHolder) convertView.getTag();
                } else {
                    convertView = this.inflater.inflate(R.layout.fb_friends_layout, parent, false);
                    holder = new ElementsHolder();
                    holder.id = (TextView) convertView.findViewById(R.id.fb_friends_list_id);
                    holder.name = (TextView) convertView.findViewById(R.id.fb_friends_list_name);
                    holder.picture = (ImageView) convertView.findViewById(R.id.fb_friends_list_picture);
                    convertView.setTag(holder);
                }
                holder.id.setText(m_fb_friendslist.get(position).get("id"));
                holder.name.setText(m_fb_friendslist.get(position).get("name"));
                new Me_downloadImg_Task(holder.picture).execute(m_fb_friendslist.get(position).get("picture"));
                if (m_fb_friendslist.get(position).get("check").equals("f")) {
                    convertView.setBackgroundColor(0xFFAACCFF);
                } else {
                    convertView.setBackgroundColor(0xFF6633FF);
                }
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (m_fb_friendslist.get(position).get("check").equals("f")) {
                            v.setBackgroundColor(0xFF6633FF);
                            m_fb_friendslist.get(position).put("check", "t");
                        } else {
                            v.setBackgroundColor(0xFFAACCFF);
                            m_fb_friendslist.get(position).put("check", "f");
                        }
                    }
                });
                return convertView;
            }
            return convertView;
        }
    }

    public class Me_downloadImg_Task extends AsyncTask<String, Void, Bitmap> {
        ImageView imageView;

        Me_downloadImg_Task(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String urlDisplay = params[0];
            if (m_listview_bitmapcache_map.containsKey(urlDisplay)) {
                return m_listview_bitmapcache_map.get(urlDisplay);
            }
            InputStream is = null;
            try {
                is = new java.net.URL(urlDisplay).openStream();
                Bitmap result = BitmapFactory.decodeStream(is);
                m_listview_bitmapcache_map.put(urlDisplay, result);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                LOGD("download prcture error " + e.getMessage(), null);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            BitmapDrawable ob = new BitmapDrawable(m_context.getResources(), result);
            imageView.setBackgroundDrawable(ob);
        }


    }

    public static void LOGD(String msg, Context context) {
        Log.d("123000", msg);
        if (context != null) {
            try {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                //pass
            }
        }
    }
}
