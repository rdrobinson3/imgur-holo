package com.krayzk9s.imgurholo;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.support.v4.app.Fragment;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.ImgUr3Api;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class MainActivity extends FragmentActivity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mMenuList;

    String consumerkey = "4cd3f96f162ac80";
    String secretkey = "9cd3c621a4e064422e60aba4ccf84d6b149b4463";

    private static final Token EMPTY_TOKEN = null;
    public static final String OAUTH_CALLBACK_SCHEME = "imgur-holo";
    public static final String OAUTH_CALLBACK_HOST = "authcallback";
    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME + "://" + OAUTH_CALLBACK_HOST;
    public static final String MASHAPE_KEY = "CoV9d8oMmqhy8YdAbCAnB1MroW1xMJpP";
    public static final String PREFS_NAME = "ImgurPrefs";
    public static final String MASHAPE_URL = "https://imgur-apiv3.p.mashape.com/";
    Token accessToken;
    String accessString;
    Verifier verifier;
    Adapter adapter;
    final OAuthService service = new ServiceBuilder().provider(ImgUr3Api.class).apiKey(consumerkey).debug().callback(OAUTH_CALLBACK_URL).apiSecret(secretkey).build();

    boolean loggedin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.contains("RefreshToken")) {
            loggedin = true;
        }
        updateMenu();


        // enable ActionBar app icon to behave as action to toggle nav drawer
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void updateMenu() {
        if (loggedin)
            mMenuList = getResources().getStringArray(R.array.imgurMenuListLoggedIn);
        else
            mMenuList = getResources().getStringArray(R.array.imgurMenuListLoggedOut);
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        adapter = new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mMenuList);
        mDrawerList.setAdapter((ListAdapter) adapter);
        adapter = new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mMenuList);
        mDrawerList.setAdapter((ListAdapter) adapter);
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("URI", intent.toString());
        Uri uri = intent.getData();
        Log.d("URI", "resumed2!");
        String uripath = "";
        if (uri != null)
            uripath = uri.toString();
        Log.d("URI", uripath);
        Log.d("URI", "HERE");

        if (uri != null && uripath.startsWith(OAUTH_CALLBACK_URL)) {
            verifier = new Verifier(uri.getQueryParameter("code"));

            AsyncTask<Void, Void, Void> async = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    accessToken = service.getAccessToken(Token.empty(), verifier);
                    Log.d("URI", verifier.toString());
                    Log.d("URI", accessToken.getToken());
                    Log.d("URI", accessToken.getSecret());
                    editor.putString("RefreshToken", accessToken.getSecret());
                    editor.putString("AccessToken", accessToken.getToken());
                    editor.commit();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    updateMenu();
                }
            };
            async.execute();
        }
    }

    public Token renewAccessToken() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        accessToken = service.refreshAccessToken(accessToken);
        Log.d("URI", accessToken.getRawResponse());
        editor.putString("AccessToken", accessToken.getToken());
        editor.commit();
        return accessToken;
    }

    public Token getAccessToken() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if (settings.contains("RefreshToken")) {
            accessToken = new Token(settings.getString("AccessToken", ""), settings.getString("RefreshToken", ""));
            loggedin = true;
            Log.d("URI", accessToken.toString());
        } else
        {
            loggedin = false;
            login();
        }
        return accessToken;
    }

    public void login() {

        AsyncTask<Void, Void, String> async = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                String authURL = service.getAuthorizationUrl(EMPTY_TOKEN);
                Log.d("AuthURL", authURL);
                return authURL;
            }

            @Override
            protected void onPostExecute(String authURL) {
                startActivity(new Intent("android.intent.action.VIEW",
                        Uri.parse(authURL)).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_FROM_BACKGROUND));
                Log.d("AuthURL2", authURL);
            }
        };
        async.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action buttons
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void createGallery() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        GalleryFragment galleryFragment = new GalleryFragment();
        fragmentManager.beginTransaction()
                .replace(R.id.frame_layout, galleryFragment)
                .commit();
    }

    private void selectItem(int position) {
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
        switch (position) {
            case 0:
                if (!loggedin)
                    login();
                else
                    createGallery();
                break;
            case 1:
                if (loggedin) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    AccountFragment accountFragment = new AccountFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, accountFragment)
                            .commit();
                }
                break;
            case 3:
                if (loggedin) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    ImagesFragment imagesFragment = new ImagesFragment();
                    imagesFragment.setImageCall("3/account/me/images/0");
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, imagesFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 4:
                if (loggedin) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    AlbumsFragment albumsFragment = new AlbumsFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, albumsFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 7:
                if (loggedin) {
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    SettingsFragment settingsFragment = new SettingsFragment();
                    fragmentManager.beginTransaction()
                            .replace(R.id.frame_layout, settingsFragment)
                            .commit();
                    updateMenu();
                }
                break;
            case 8:
                if (loggedin) {
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.remove("AccessToken");
                    //editor.remove("RefreshToken");
                    editor.commit();
                    loggedin = false;
                    updateMenu();
                }
                break;
            default:
                return;
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    public JSONObject makeGetCall(String url)
    {
        Token accessKey = getAccessToken();
        Log.d("Making Call", accessKey.toString());
        HttpResponse<JsonNode> response = Unirest.get(MASHAPE_URL + url)
                .header("accept", "application/json")
                .header("X-Mashape-Authorization", MASHAPE_KEY)
                .header("Authorization", "Bearer " + accessKey.getToken())
                .asJson();
        Log.d("Getting Code", String.valueOf(response.getCode()));
        int code = response.getCode();
        if(code == 403)
        {
            accessKey = renewAccessToken();
            response = Unirest.get(MASHAPE_URL + url)
                    .header("accept", "application/json")
                    .header("X-Mashape-Authorization", MASHAPE_KEY)
                    .header("Authorization", "Bearer " + accessKey.getToken())
                    .asJson();
        }
        JSONObject data = response.getBody().getObject();
        Log.d("Got data", data.toString());
        return data;
    }

    public void changeFragment(Fragment newFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, newFragment).addToBackStack("tag").commit();
        updateMenu();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public Token getToken() {
        return accessToken;
    }
}