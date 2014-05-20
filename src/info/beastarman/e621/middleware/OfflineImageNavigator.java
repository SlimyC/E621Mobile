package info.beastarman.e621.middleware;

import java.util.ArrayList;

public class OfflineImageNavigator implements ImageNavigator
{
	E621DownloadedImage img;
	int position;
	String query;
	
	public OfflineImageNavigator(E621DownloadedImage img, int position, String query)
	{
		this.img = img;
		this.position = position;
		this.query = query;
	}
	
	@Override
	public ImageNavigator next()
	{
		ArrayList<E621DownloadedImage> ret = E621Middleware.getInstance().localSearch(position+1, 1, query);
		
		if(ret.size() > 0)
		{
			return new OfflineImageNavigator(ret.get(0),position+1,query);
		}
		
		return null;
	}

	@Override
	public ImageNavigator prev()
	{
		if(position <= 0)
		{
			return null;
		}
		
		ArrayList<E621DownloadedImage> ret = E621Middleware.getInstance().localSearch(position-1, 1, query);
		
		if(ret.size() > 0)
		{
			return new OfflineImageNavigator(ret.get(0),position-1,query);
		}
		
		return null;
	}
	
	@Override
	public String getId() {
		return img.filename;
	}
	
	public String toString()
	{
		return getId();
	}
}
