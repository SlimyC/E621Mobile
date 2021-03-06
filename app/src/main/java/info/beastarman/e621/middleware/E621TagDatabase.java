package info.beastarman.e621.middleware;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import info.beastarman.e621.api.E621Tag;
import info.beastarman.e621.api.E621TagAlias;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.backend.ReadWriteLockerWrapper;

public class E621TagDatabase
{
	File file_path;
	ReadWriteLockerWrapper lock = new ReadWriteLockerWrapper();

	int version = 1;

	DatabaseHelper dbHelper;

	private class DatabaseHelper extends SQLiteOpenHelper
	{
		public DatabaseHelper(Context context, File f)
		{
			super(context, f.getAbsolutePath(), null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			try
			{
				db.execSQL("CREATE TABLE tag (" +
								"id UNSIGNED INTEGER PRIMARY KEY" +
								", " +
								"name TEXT" +
								", " +
								"type SMALL INTEGER" +
								");"
				);

				db.execSQL("CREATE TABLE tag_alias (" +
								"alias TEXT" +
								", " +
								"id UNSIGNED INTEGER" +
								", " +
								"tag UNSIGNED INTEGER" +
								", " +
								"PRIMARY KEY(id)" +
								", " +
								"FOREIGN KEY (tag) REFERENCES tag(id)" +
								");"
				);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

			onUpgrade(db,0,version);
		}

		@Override
		public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2)
		{
		}
	}
	
	public E621TagDatabase(Context ctx, File file_path)
	{
		this.file_path = file_path;

		dbHelper = new DatabaseHelper(ctx,file_path);
	}

	public void clean()
	{
		lock.write(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				db.beginTransaction();
				
				try
				{
					db.delete("tag", "1", null);
					db.delete("tag_alias", "1", null);
					
					db.setTransactionSuccessful();
				}
				finally
				{
					db.endTransaction();
				}
			}
		});
	}
	
	public void addTag(final E621Tag[] tags)
	{
		lock.write(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				db.beginTransaction();
				
				try
				{
					for(E621Tag tag : tags)
					{
						ContentValues values = new ContentValues();
						values.put("name", tag.getTag());
						values.put("id", tag.getId());
						values.put("type", tag.type);
						
						try
						{
							if(db.insert("tag", null, values) == -1)
							{
								values.remove("id");
								
								db.update("tag", values, "id = ?", new String[]{String.valueOf(tag.getId())});
							}
						}
						catch(SQLiteConstraintException e)
						{
							values.remove("id");
							
							db.update("tag", values, "id = ?", new String[]{String.valueOf(tag.getId())});
						}
					}
					
					db.setTransactionSuccessful();
				}
				finally
				{
					db.endTransaction();
				}
			}
		});
	}
	
	public void addTagAlias(final E621TagAlias[] aliases)
	{
		lock.write(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getWritableDatabase();
				db.beginTransaction();
				
				try
				{
					for(E621TagAlias alias : aliases)
					{
						ContentValues values = new ContentValues();
						values.put("alias", alias.name);
						values.put("id", alias.id);
						values.put("tag", alias.alias_id);
						
						try
						{
							if(db.insert("tag_alias", null, values) == -1)
							{
								values.remove("id");
								
								db.update("tag_alias", values, "id = ?", new String[]{String.valueOf(alias.alias_id)});
							}
						}
						catch(SQLiteConstraintException e)
						{
							values.remove("id");
							
							db.update("tag_alias", values, "id = ?", new String[]{String.valueOf(alias.alias_id)});
						}
					}
					
					db.setTransactionSuccessful();
				}
				finally
				{
					db.endTransaction();
				}
			}
		});
	}

	public E621Tag getTag(final String name)
	{
		final GTFO<E621Tag> ret = new GTFO<E621Tag>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT id, type FROM tag WHERE name = ?", new String[]{name});
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = new E621Tag(name, c.getInt(c.getColumnIndex("id")), null, c.getInt(c.getColumnIndex("type")), null);
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}
	
	public ArrayList<E621Tag> getTags(final String[] names)
	{
		final ArrayList<E621Tag> ret = new ArrayList<E621Tag>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					String query = "SELECT name, id, type FROM tag WHERE name = ?";
					
					int i = names.length - 1;
					
					while(i>0)
					{
						query += " OR name = ?";
						
						i--;
					}
					
					c = db.rawQuery(query, names);
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					while(!c.isAfterLast())
					{
						ret.add(new E621Tag(c.getString(c.getColumnIndex("name")), c.getInt(c.getColumnIndex("id")), null, c.getInt(c.getColumnIndex("type")), null));
						
						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret;
	}
	
	public E621Tag getTag(final Integer id)
	{
		final GTFO<E621Tag> ret = new GTFO<E621Tag>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT name, type FROM tag WHERE name = ?", new String[]{String.valueOf(id)});
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = new E621Tag(c.getString(c.getColumnIndex("name")), id, null, c.getInt(c.getColumnIndex("type")), null);
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}
	
	public E621Tag getMaxTag()
	{
		final GTFO<E621Tag> ret = new GTFO<E621Tag>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT MAX(id) AS id, name, type FROM tag", new String[]{});
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = new E621Tag(c.getString(c.getColumnIndex("name")), c.getInt(c.getColumnIndex("id")), null, c.getInt(c.getColumnIndex("type")), null);
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}
	
	public E621TagAlias getMaxTagAlias()
	{
		final GTFO<E621TagAlias> ret = new GTFO<E621TagAlias>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT MAX(id) AS id, alias, tag FROM tag_alias", new String[]{});
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = new E621TagAlias(c.getInt(c.getColumnIndex("id")), c.getInt(c.getColumnIndex("tag")), true, c.getString(c.getColumnIndex("alias")));
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}

	public String tryResolveAlias(final String name)
	{
		final GTFO<String> ret = new GTFO<String>();
		ret.obj = name;
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT tag.name AS tag_name FROM tag INNER JOIN tag_alias ON tag.id = tag_alias.tag WHERE tag_alias.alias = ?", new String[]{name});
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = c.getString(c.getColumnIndex("tag_name"));
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}

	public int getTagCount()
	{
		final GTFO<Integer> ret = new GTFO<Integer>();
		ret.obj = 0;

		lock.read(new Runnable()
		{
			@Override
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;

				try
				{
					c = db.rawQuery("SELECT COUNT(*) FROM tag;", null);

					if(c == null || !c.moveToFirst() || c.isAfterLast())
					{
						return;
					}

					ret.obj = c.getInt(0);
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});

		return ret.obj;
	}
	
	public E621Tag[] getAllTags()
	{
		final GTFO<E621Tag[]> ret = new GTFO<E621Tag[]>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT id, name FROM tag;", null);
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					ret.obj = new E621Tag[c.getCount()];
					int i=0;
					
					while(!c.isAfterLast())
					{
						ret.obj[i] = new E621Tag(
											c.getString(c.getColumnIndex("name")),
											c.getInt(c.getColumnIndex("id"))
										);
						
						i++;
						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret.obj;
	}
	
	public HashMap<String,Integer> getAllTagsAsHashMap()
	{
		final HashMap<String,Integer> ret = new HashMap<String,Integer>();
		
		lock.read(new Runnable()
		{
			public void run()
			{
				SQLiteDatabase db = dbHelper.getReadableDatabase();
				Cursor c = null;
				
				try
				{
					c = db.rawQuery("SELECT id, name FROM tag;", null);
					
					if(c == null || !c.moveToFirst())
					{
						return;
					}
					
					while(!c.isAfterLast())
					{
						ret.put(c.getString(c.getColumnIndex("name")),
								c.getInt(c.getColumnIndex("id")));
						
						c.moveToNext();
					}
				}
				finally
				{
					if(c != null) c.close();
				}
			}
		});
		
		return ret;
	}
}
