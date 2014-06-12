package info.beastarman.e621.middleware;

import java.io.IOException;
import java.io.InputStream;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

public class ImageHandler extends Handler
{
	public View loader;
	
	public ImageHandler(View loader)
	{
		this.loader = loader;
	}
	
	protected void handleInputStream(InputStream in)
	{
	}
	
	@Override
	public void handleMessage(Message msg)
	{
		InputStream in = (InputStream)msg.obj;
		
		handleInputStream(in);

		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(this.loader != null)
		{
			ViewGroup v = ((ViewGroup)this.loader.getParent()); 
			
			v.removeView(this.loader);
		}
	}
}