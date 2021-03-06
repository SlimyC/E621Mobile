package info.beastarman.e621.frontend;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Set;

import info.beastarman.e621.backend.SingleUseFileStorage;
import info.beastarman.e621.backend.TemporaryFileInputStream;
import info.beastarman.e621.middleware.E621Middleware;

public class BaseActivity extends Activity implements UncaughtExceptionHandler
{
	public E621Middleware e621;

	ArrayList<ImageView> recyclableImageViews = new ArrayList<ImageView>();

	public ImageView gimmeRecyclableImageView()
	{
		ImageView iv = new ImageView(this);

		recyclableImageViews.add(iv);

		return iv;
	}

    public Intent shareIntent(String str)
    {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, str);
        sendIntent.setType("text/plain");
        return sendIntent;
    }
	
	protected int dpToPx(int dp)
	{
	    DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
	    int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
	    return px;
	}
	
	private String safeObjToStr(Object obj)
	{
		if(obj == null) return "null";
		
		return obj.toString();
	}
	
	private String safeObjToClassName(Object obj)
	{
		if(obj == null) return "null";
		
		return obj.getClass().getName();
	}

	public void sendAnalytics()
	{
		String analyticsPath = this.getClass().getName();

		Tracker t = ((E621Application) getApplication()).getTracker();

		t.setScreenName(analyticsPath);

		t.send(new HitBuilders.AppViewBuilder().build());
	}

	public void sendAnalyticsError(Throwable e)
	{
		String analyticsPath = this.getClass().getName();

		Tracker t = ((E621Application) getApplication()).getTracker();

		t.send(new HitBuilders.ExceptionBuilder().
			setDescription(new StandardExceptionParser(this, null).getDescription(analyticsPath, e)).
			setFatal(false).
			build()
		);

	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onCreate() " + this.getClass().getName());
		
		Intent intent = getIntent();
		if(intent != null)
		{
			Bundle bundle = intent.getExtras();
			
			if(bundle != null)
			{
				Set<String> keys = bundle.keySet();
				
				for(String key : keys)
				{
					Object value = bundle.get(key);
					Log.i(E621Middleware.LOG_TAG + "_Browsing", "\t" + key + ": <" + safeObjToStr(value) + "> from class <" + safeObjToClassName(value) + ">");
				}
			}
		}
		
		super.onCreate(savedInstanceState);
		
		e621 = E621Middleware.getInstance(this);
		
		Thread.setDefaultUncaughtExceptionHandler(this);
	}
	
	@Override
	protected void onDestroy()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onDestroy() " + this.getClass().getName());
		
		super.onDestroy();
	}
	
	@Override
	protected void onStart()
	{
        sendAnalytics();

		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onStart() " + this.getClass().getName());
		
		super.onStart();
	}
	
	@Override
	protected void onStop()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onStop() " + this.getClass().getName());

		for(ImageView iv : recyclableImageViews)
		{
			Drawable drawable = iv.getDrawable();
			if (drawable instanceof BitmapDrawable)
			{
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();

				if(bitmap != null && !bitmap.isRecycled())
				{
					bitmap.recycle();
				}
			}
		}

		recyclableImageViews.clear();
		
		super.onStop();
	}

	private boolean alive = false;

	public boolean isAlive()
	{
		return alive;
	}
	
	@Override
	protected void onResume()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onResume() " + this.getClass().getName());

		alive = true;
		
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onPause() " + this.getClass().getName());

		alive = false;

		super.onPause();
	}
	
	@Override
	protected void onRestart()
	{
		Log.i(E621Middleware.LOG_TAG + "_Browsing", hashCode() + " onRestart() " + this.getClass().getName());
		
		super.onRestart();
	}
	
	public void uncaughtException()
	{
        Intent intent = new Intent(getApplicationContext(), ErrorReportActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |   Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
	}
	
	@Override
	public void uncaughtException(Thread thread, Throwable e)
	{
		Log.e(E621Middleware.LOG_TAG + "_Exception",Log.getStackTraceString(e));

		sendAnalyticsError(e);

		uncaughtException();
	}

	static SingleUseFileStorage _singleUseFileStorage = null;
	SingleUseFileStorage getSingleUseFileStorage()
	{
		if(_singleUseFileStorage == null)
		{
			File f = new File(getCacheDir(),"BaseActivity/SingleUseFielStorage/");
			f.mkdirs();
			_singleUseFileStorage = new SingleUseFileStorage(f);
		}

		return _singleUseFileStorage;
	}
	
	public Bitmap decodeFile(InputStream source, int width, int height)
	{
		TemporaryFileInputStream in = null;

		try
		{
			in = getSingleUseFileStorage().store(source);
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}

		//Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in,null,o);

		try
		{
			in.resetInputStream();
		}
		catch (IOException e)
		{
			e.printStackTrace();

			return null;
		}

		//Find the correct scale value. It should be the power of 2.
		int scale=1;
		while(o.outWidth/scale/2>=width && o.outHeight/scale/2>=height)
		{
			scale*=2;
		}

		//Decode with inSampleSize
		BitmapFactory.Options o2 = new BitmapFactory.Options();
		o2.inSampleSize=scale;
		Bitmap bitmap_temp = BitmapFactory.decodeStream(in, null, o2);

		in.close();

		if(width == bitmap_temp.getWidth() && height == bitmap_temp.getHeight())
		{
			return bitmap_temp;
		}
		else
		{
			Bitmap ret = Bitmap.createScaledBitmap(bitmap_temp,width,height,false);

			bitmap_temp.recycle();

			return ret;
		}
	}
	
	private Bitmap decodeFile(Bitmap bmp, int width, int height)
	{
		if(width == bmp.getWidth() && height == bmp.getHeight())
		{
			return bmp.copy(bmp.getConfig(),false);
		}
		
		Bitmap ret = Bitmap.createScaledBitmap(bmp,width,height,false);
        
        return ret;
	}
	
	public void drawInputStreamToImageView(final InputStream in, final ImageView imgView)
	{
		final ImageViewHandler handler = new ImageViewHandler(imgView);
		
		new Thread(new Runnable()
		{
			public void run()
			{
				Bitmap bitmap = decodeFile(in, imgView.getLayoutParams().width, imgView.getLayoutParams().height);
				try
				{
					in.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
				
				Message msg = handler.obtainMessage();
		    	msg.obj = bitmap;
		    	handler.sendMessage(msg);
			}
		}).start();
	}
	
	public void drawInputStreamToImageView(final Bitmap bmp, final ImageView imgView)
	{
		final ImageViewHandler handler = new ImageViewHandler(imgView);
		
		new Thread(new Runnable()
		{
			public void run()
			{
				Bitmap bitmap = decodeFile(bmp, imgView.getLayoutParams().width, imgView.getLayoutParams().height);
				bmp.recycle();
				
				Message msg = handler.obtainMessage();
		    	msg.obj = bitmap;
		    	handler.sendMessage(msg);
			}
		}).start();
	}
	
	private static class ImageViewHandler extends Handler
	{
		private ImageView imgView;
		
		public ImageViewHandler(ImageView imgView)
		{
			this.imgView = imgView;
		}
		
		public void handleMessage(Message msg)
		{
			this.imgView.setBackgroundResource(0);
			this.imgView.setImageBitmap((Bitmap)msg.obj);
		}
	}

	public int getWidth()
	{
		return getWindow().getDecorView().getWidth();
	}

	public int getHeight()
	{
		return getWindow().getDecorView().getHeight();
	}
}
