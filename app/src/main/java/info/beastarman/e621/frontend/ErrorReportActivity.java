package info.beastarman.e621.frontend;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Collections;
import java.util.Date;

import info.beastarman.e621.R;
import info.beastarman.e621.backend.EventManager;
import info.beastarman.e621.backend.GTFO;
import info.beastarman.e621.backend.errorReport.ErrorReportReport;
import info.beastarman.e621.middleware.AndroidAppUpdaterInterface;
import info.beastarman.e621.middleware.AndroidAppVersion;
import info.beastarman.e621.middleware.E621Middleware;
import info.beastarman.e621.views.StepsProgressDialog;

public class ErrorReportActivity extends Activity
{
	public static String LOG = "log";
	
	E621Middleware e621;

	String log = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_error_report);
		
		e621 = E621Middleware.getInstance(getApplicationContext());

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				String l = e621.generateErrorReport();
				log = l;
			}
		}).start();
	}
	
	public void doNotSendReport(View v)
	{
		end();
	}

    public void showStatistics(View v)
    {
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // dummy
            }
        });

        final TextView logArea = (TextView) v.findViewById(R.id.logcat);
        logArea.setText("Retrieving log...");

        new Thread(new Runnable() {
            @Override
            public void run()
            {
                while(log == null)
				{
					try
					{
						Thread.currentThread().sleep(1000);
					} catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logArea.setText(log.trim());
                    }
                });
            }
        }).start();
    }

	protected void fillTags(ErrorReportReport report)
	{
		report.tags.add("crash");
	}

	public void sendReport(View v)
	{
		EditText error_description = (EditText) findViewById(R.id.errorDescription);
		
		final String text = error_description.getText().toString().trim();

		final CheckBox sendStatistics = (CheckBox) findViewById(R.id.sendStatistics);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while(log == null)
				{
					try
					{
						Thread.currentThread().sleep(1000);
					} catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}

				ErrorReportReport report = e621.getBaseReport();

				if(sendStatistics.isChecked())
				{
					report.log = log;
				}

				report.text = text;

				fillTags(report);

				e621.getErrorReportManager().sendReport(report);
			}
		}).start();
		
		end();
	}
	
	@Override
	public void onBackPressed()
	{
		end();
	}
	
	public void end()
	{
		Intent i = new Intent(this,MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		
		finish();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.error, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.restore_backup:
			restoreBackup(Collections.max(e621.getBackups()));
			return true;
		case R.id.look_for_update:
			lookForUpdate();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private static class FailException extends Exception
	{
		private static final long serialVersionUID = 1615513842090522333L;
		
		public int code;
		
		public FailException(int code)
		{
			this.code = code;
		}
	};
	
	private void lookForUpdate()
	{
		final AndroidAppUpdaterInterface appUpdater = e621.getAndroidAppUpdater();
		
		new Thread(new Runnable()
		{
			public void run()
			{
				PackageInfo pInfo = null;
				
				try
				{
					try {
						pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					} catch (NameNotFoundException e) {
						e.printStackTrace();
						throw new FailException(0);
					}
					
					int currentVersion = pInfo.versionCode;
					final AndroidAppVersion version = appUpdater.getLatestVersionInfo();
					
					e621.updateMostRecentVersion(version);
					
					if(version == null)
					{
						throw new FailException(1);
					}
					
					if(version.versionCode > currentVersion)
					{
						final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ErrorReportActivity.this).setTitle("New Version Found").setCancelable(true).
								setMessage(String.format(getResources().getString(R.string.new_version_found),version.versionName));
						
						runOnUiThread(new Runnable()
						{
							public void run()
							{
								final AlertDialog dialog = dialogBuilder.create();
								
								dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Update", new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface arg0,int arg1)
									{
										dialog.dismiss();
										
										final GTFO<StepsProgressDialog> dialogWrapper = new GTFO<StepsProgressDialog>();
										dialogWrapper.obj = new StepsProgressDialog(ErrorReportActivity.this);
										dialogWrapper.obj.show();
										
										e621.updateApp(version, new EventManager()
										{
											@Override
											public void onTrigger(Object obj)
											{
												if(obj == E621Middleware.UpdateState.START)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.addStep("Retrieving package file").showStepsMessage();
								    					}
								    				});
								    			}
								    			else if(obj == E621Middleware.UpdateState.DOWNLOADED)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.addStep("Package downloaded").showStepsMessage();
								    					}
								    				});
								    			}
								    			else if(obj == E621Middleware.UpdateState.SUCCESS)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.setDone("Starting package install");
								    					}
								    				});
								    			}
								    			else if(obj == E621Middleware.UpdateState.FAILURE)
								    			{
								    				runOnUiThread(new Runnable()
								    				{
								    					public void run()
								    					{
								    						dialogWrapper.obj.setDone("Package could not be retrieved");
								    					}
								    				});
								    			}
											}
										});
									}
								});
								
								dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Maybe later", new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface arg0,int arg1)
									{
										dialog.dismiss();
									}
								});
								
								dialog.show();
							}
						});
					}
					else
					{
						throw new FailException(2);
					}
				}
				catch(FailException e)
				{
					final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ErrorReportActivity.this).setTitle("Update").
							setCancelable(true);
					
					switch(e.code)
					{
						case 1:
							dialogBuilder.setMessage("Could not retrieve latest version");
							break;
						case 2:
							dialogBuilder.setMessage("No newer version found");
							break;
						default:
							dialogBuilder.setMessage("Unknown error happened");
							break;
					}
					
					runOnUiThread(new Runnable()
					{
						public void run()
						{
							final AlertDialog dialog = dialogBuilder.create();
							
							dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Ok", new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface arg0,int arg1)
								{
									dialog.dismiss();
								}
							});
							
							dialog.show();
						}
					});
				}
			}
		}).start();
	}
	
	protected void restoreBackup(final Date date)
	{
		AlertDialog.Builder removeNewBuilder = new AlertDialog.Builder(this);
		removeNewBuilder.setMessage("Keep images not present on backup?");
		removeNewBuilder.setPositiveButton("Keep", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				restoreBackup(date,true);
			}
		});
		removeNewBuilder.setNegativeButton("Delete", new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				restoreBackup(date,false);
			}
		});
		
		removeNewBuilder.create().show();
	}
	
	private void restoreBackup(final Date date, final boolean keep)
	{
		final GTFO<StepsProgressDialog> dialogWrapper = new GTFO<StepsProgressDialog>();
		dialogWrapper.obj = new StepsProgressDialog(this);
		dialogWrapper.obj.show();
		
		new Thread(new Runnable()
		{
			public void run()
			{
				final GTFO<String> message = new GTFO<String>();
				message.obj = "";
				
				e621.restoreBackup(date,keep,new EventManager()
		    	{
		    		@Override
					public void onTrigger(Object obj)
		    		{
		    			if(obj == E621Middleware.BackupStates.OPENING)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Opening current backup").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.READING)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Reading current backup").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.CURRENT)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Creating emergency backup").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.SEARCHES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Overriding saved searches").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.SEARCHES_COUNT)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Updating saved searches remaining images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.REMOVE_EMERGENCY)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Removing emergency backup").showStepsMessage();
		    						dialogWrapper.obj.allowDismiss();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.GETTING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Getting current images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.DELETING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Removing unnecessary images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.INSERTING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Inserting images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.DOWNLOADING_IMAGES)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Downloading images").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.UPDATE_TAGS)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.addStep("Updating tags").showStepsMessage();
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.SUCCESS)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.setDone("Backup finished!");
		    					}
		    				});
		    			}
		    			else if(obj == E621Middleware.BackupStates.FAILURE)
		    			{
		    				runOnUiThread(new Runnable()
		    				{
		    					public void run()
		    					{
		    						dialogWrapper.obj.setDone("Backup could not be restored!");
		    					}
		    				});
		    			}
					}
		    	});
			}
		}).start();
	}
}
