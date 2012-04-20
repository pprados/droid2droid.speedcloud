package org.remoteandroid.speedcloud;


import java.io.IOException;

import org.remoteandroid.RemoteAndroid;
import org.remoteandroid.RemoteAndroid.PublishListener;
import org.remoteandroid.RemoteAndroidManager;
import org.remoteandroid.RemoteAndroidManager.ManagerListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SpeedCloudActivity extends Activity
{
	private Speed mSpeedInBoard=new SpeedImpl();
	private Speed mSpeedInCloud;
	private EditText mNumberText;
	private TextView mTextInBoard;
	private TextView mTextInCloud;
	private Spinner mSpinner;
	private Button mStart;
	
	private String mTarget="ip://192.168.1.130";
	
	private Intent mIntent;
	
	private ServiceConnection mServiceConnection=new ServiceConnection()
	{
		
		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mStart.setEnabled(false);
			mSpeedInCloud=null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mSpeedInCloud=Speed.Stub.asInterface(service);
			mStart.setEnabled(true);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mTextInBoard=(TextView)findViewById(R.id.in_board);
		mTextInCloud=(TextView)findViewById(R.id.in_cloud);
		mStart=(Button)findViewById(R.id.start);
		mNumberText=(EditText)findViewById(R.id.number);
		mSpinner = (Spinner) findViewById(R.id.spinner);
	    ArrayAdapter<CharSequence> adapter = 
	    		ArrayAdapter.createFromResource(
	    			this, 
	    			R.array.devices, 
	    			android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    mSpinner.setAdapter(adapter);
	    mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() 
		    {
		        public void onItemSelected(AdapterView<?> parent,
		            View view, int pos, long id) 
		        {
		        	mTarget=getResources().getStringArray(R.array.devicesip)[pos];
		        	bindRemoteAndroid();
		        }
	
		        public void onNothingSelected(AdapterView parent) 
		        {
		          // Do nothing.
		        }
		    });
	    bindRemoteAndroid();
		mIntent=new Intent(this,SpeedService.class);
//		bindOtherProcessService(this,mIntent,mServiceConnection,BIND_AUTO_CREATE);
	}
	private void bindRemoteAndroid()
	{
		mStart.setEnabled(false);
		bindRemoteAndroidService(this,
			
			// Connect Remote android
			Uri.parse(mTarget),RemoteAndroidManager.FLAG_PROPOSE_PAIRING,
			
			// Publish APK
			mPublishListener,0,10000,
			
			// Bind remote service
			mIntent,mServiceConnection, BIND_AUTO_CREATE);
		
	}
	private PublishListener mPublishListener=new PublishListener()
	{
		
		@Override
		public void onProgress(int progress)
		{
		}
		
		@Override
		public void onFinish(int status)
		{
			switch(status)
			{
				case RemoteAndroid.ERROR_INSTALL_REFUSE_FOR_UNKNOW_SOURCE:
				case RemoteAndroid.ERROR_INSTALL_REFUSED:
				default:
					break;
			}
		}
		
		@Override
		public void onError(Throwable e)
		{
		}
		
		@Override
		public boolean askIsPushApk()
		{
			return true;
		}
	};

	private void bindOtherProcessService(final Context context,
			final Intent serviceIntent,
			final ServiceConnection serviceConnection,
			final int flags)
	{
		context.bindService(serviceIntent,serviceConnection, flags);
	}
	/**
	 * Integrated state machine to bind a remote service.
	 * 
	 * @param context				A context
	 * @param remoteAndroidUri		The URI to remote android
	 * @param flagRemoteAndroid		The flag to connect to use. May be {@link FLAG_PROPOSE_PAIRING}
	 * @param intallListener		The listener to manage the installation
	 * @param flagInstall			The flag to install. Accept zero or {@link INSTALL_REPLACE_EXISTING} if you want to force the installation.
	 * @param timeoutInstall		The timeout in ms for a user answer to a question.
	 * @param serviceIntent			Identifies the service to connect to. The Intent may specify either an explicit component name, or a logical description (action, category, etc) to match an IntentFilter published by a service.
	 * @param serviceConnection		The callback to manage the life cycle to remote objet.
	 * @param flagsService			Operation options for the binding. May be 0, BIND_AUTO_CREATE, BIND_DEBUG_UNBIND, BIND_NOT_FOREGROUND, BIND_ABOVE_CLIENT, BIND_ALLOW_OOM_MANAGEMENT, or BIND_WAIVE_PRIORITY.
	 */
	private static void bindRemoteAndroidService(
			final Context context,
			
			final Uri remoteAndroidUri,
			final int flagRemoteAndroid,
			
			final PublishListener intallListener,
			final int flagInstall,
			final int timeoutInstall,
			
			final Intent serviceIntent,
			final ServiceConnection serviceConnection,
			final int flagsService)
	{
		// 1. Bind manager
		RemoteAndroidManager.bindManager(context, 
    		new ManagerListener()
			{
				
				@Override
				public void unbind(RemoteAndroidManager manager)
				{
					serviceConnection.onServiceDisconnected(null);
				}
				
				@Override
				public void bind(final RemoteAndroidManager manager)
				{
					final ManagerListener me=this;
					// 2. Bind remote android
					manager.bindRemoteAndroid(
						new Intent(Intent.ACTION_MAIN,remoteAndroidUri), 
						new ServiceConnection()
						{
	
							@Override
							public void onServiceConnected(final ComponentName name, final IBinder service)
							{
								final RemoteAndroid ra=(RemoteAndroid)service;
								try
								{
									// 3. Install apk
									ra.pushMe(context,
										new PublishListener()
										{
	
											@Override
											public boolean askIsPushApk()
											{
												boolean rc=true;
												if (intallListener!=null)
													rc=intallListener.askIsPushApk();
												return rc;
											}
	
											@Override
											public void onError(Throwable e)
											{
												if (intallListener!=null)
													intallListener.onError(e);
											}
	
											@Override
											public void onFinish(int status)
											{
												if (intallListener!=null)
													intallListener.onFinish(status);
												
												// 4. Bind remote service
												ra.bindService(serviceIntent, serviceConnection, flagsService);
											}
	
											@Override
											public void onProgress(int progress)
											{
												if (intallListener!=null)
													intallListener.onProgress(progress);
											}
										
										},
										flagInstall,timeoutInstall);
								}
								catch (RemoteException e)
								{
									ra.close(); // FIXME: check si onServiceDisconnected
								}
								catch (IOException e)
								{
									ra.close();
								}
							}
	
							@Override
							public void onServiceDisconnected(ComponentName name)
							{
								serviceConnection.onServiceDisconnected(name);
								me.unbind(manager);
							}
						}, 
						flagRemoteAndroid);
				}
			});
	}
	public void onClick(View view)
	{
		mStart.setEnabled(false);
		new AsyncTask<Void,Void,long[]>()
		{
			@Override
			protected void onPreExecute()
			{
				mTextInCloud.setText("...");
				mTextInBoard.setText("...");
			}
			@Override
			protected long[] doInBackground(Void... params)
			{
				try
				{
					long[] rc=new long[2];
					long start;
					int maxnumber=Integer.parseInt(mNumberText.getText().toString());
					start=System.currentTimeMillis();
					mSpeedInCloud.calc(maxnumber);
					rc[0]=System.currentTimeMillis()-start;
	
					start=System.currentTimeMillis();
					mSpeedInBoard.calc(maxnumber);
					rc[1]=System.currentTimeMillis()-start;
					return rc;
				}
				catch (RemoteException e)
				{
					return null;
				}
			}
			@Override
			protected void onPostExecute(long[] result)
			{
				mStart.setEnabled(true);
				if (result!=null)
				{
					mTextInCloud.setText("In cloud:"+result[0]);
					mTextInBoard.setText("In board:"+result[1]);
				}
			}
		}.execute();
//		new DoCalc(mTextInCloud,"In cloud:",mSpeedInCloud).execute();
//		new DoCalc(mTextInBoard,"In board:",mSpeedInBoard).execute();
	}
	
	class DoCalc extends AsyncTask<Void, Void, Void>
	{
		long start;
		TextView mTextView;
		Speed mSpeed;
		String mMsg;
		int mNumber;
		
		DoCalc(TextView textView,String msg,Speed speed)
		{
			mTextView=textView;
			mSpeed=speed;
			mMsg=msg;
		}
		
		@Override
		protected void onPreExecute()
		{
			mTextView.setText("...");
			start=System.currentTimeMillis();
			mNumber=Integer.parseInt(mNumberText.getText().toString());
		}
		@Override
		protected Void doInBackground(Void... params)
		{
			try
			{
				mSpeed.calc(mNumber);
			}
			catch (RemoteException e)
			{
				Toast.makeText(SpeedCloudActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result)
		{
			mTextView.setText(mMsg+(System.currentTimeMillis()-start));
		}
	};
}