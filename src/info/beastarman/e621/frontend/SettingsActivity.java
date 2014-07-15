package info.beastarman.e621.frontend;

import java.util.HashSet;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.views.SeekBarDialogPreference;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class SettingsActivity extends PreferenceActivity
{
	E621Middleware e621;
	
	@Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        e621 = E621Middleware.getInstance(getApplicationContext());
        
        MyPreferenceFragment fragment = new MyPreferenceFragment();
        fragment.activity = this;
        
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }
	
	protected void updateTags()
	{
		final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Updating tags. This may take an while. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				e621.update_tags();
				dialog.dismiss();
			}
		}).start();
	}
	
	protected void clearCache()
	{
		final ProgressDialog dialog = ProgressDialog.show(SettingsActivity.this, "","Clearing cache. Please wait...", true);
		dialog.setIndeterminate(true);
		dialog.show();
		
		new Thread(new Runnable()
		{
			@Override
			public void run() {
				e621.clearCache();
				dialog.dismiss();
			}
		}).start();
	}

    public static class MyPreferenceFragment extends PreferenceFragment
    {
    	SettingsActivity activity;
    	
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            getPreferenceManager().setSharedPreferencesName(E621Middleware.PREFS_NAME);
            
            CheckBoxPreference hideDownload = (CheckBoxPreference)findPreference("hideDownloadFolder");
            hideDownload.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("hideDownloadFolder", true));
            
            CheckBoxPreference playGifs = (CheckBoxPreference)findPreference("playGifs");
            playGifs.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("playGifs", true));
            
            ListPreference downloadSize = (ListPreference)findPreference("prefferedFileDownloadSize");
            downloadSize.setValue(String.valueOf(getPreferenceManager().getSharedPreferences().getInt("prefferedFileDownloadSize", 2)));
            
            SeekBarDialogPreference thumbnailCacheSize = (SeekBarDialogPreference)findPreference("thumbnailCacheSize");
            thumbnailCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("thumbnailCacheSize", 5));
            
            SeekBarDialogPreference fullCacheSize = (SeekBarDialogPreference)findPreference("fullCacheSize");
            fullCacheSize.setProgress(getPreferenceManager().getSharedPreferences().getInt("fullCacheSize", 10));
            
            MultiSelectListPreference ratings = (MultiSelectListPreference)findPreference("allowedRatings");
            ratings.setValues(getPreferenceManager().getSharedPreferences().getStringSet("allowedRatings",new HashSet<String>()));
            
            Preference clearCache = (Preference)getPreferenceManager().findPreference("clearCache");
            clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	activity.clearCache();
                    return true;
                }
            });
            
            Preference button = (Preference)getPreferenceManager().findPreference("updateTags");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                	activity.updateTags();
                    return true;
                }
            });
            
            CheckBoxPreference lazyLoad = (CheckBoxPreference)findPreference("lazyLoad");
            lazyLoad.setChecked(getPreferenceManager().getSharedPreferences().getBoolean("lazyLoad", true));
        }
    }
}
