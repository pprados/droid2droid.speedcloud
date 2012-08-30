/******************************************************************************
 *
 * droid2droid - Distributed Android Framework
 * ==========================================
 *
 * Copyright (C) 2012 by Atos (http://www.http://atos.net)
 * http://www.droid2droid.org
 *
 ******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
******************************************************************************/
package org.droid2droid.speedcloud;


import static org.droid2droid.Droid2DroidManager.FLAG_ACCEPT_ANONYMOUS;
import static org.droid2droid.Droid2DroidManager.FLAG_PROPOSE_PAIRING;

import java.io.IOException;

import org.droid2droid.Droid2DroidManager;
import org.droid2droid.Droid2DroidManager.ManagerListener;
import org.droid2droid.RemoteAndroid;
import org.droid2droid.RemoteAndroid.PublishListener;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
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
	private final Speed mSpeedInBoard=new SpeedImpl();
	private Speed mSpeedInCloud;
	private EditText mNumberText;
	private TextView mTextInBoard;
	private TextView mTextInCloud;
	private Spinner mSpinner;
	private Button mStart;
	
	private String mTarget="ip://192.168.1.130";
	
	private Intent mIntent;
	
	private final ServiceConnection mServiceConnection=new ServiceConnection()
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
		
		WifiManager wifiManager=(WifiManager)getSystemService(Context.WIFI_SERVICE);
		final boolean x33b="X33B".equals(wifiManager.getConnectionInfo().getSSID());
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
		        @Override
				public void onItemSelected(AdapterView<?> parent,
		            View view, int pos, long id) 
		        {
		        	final String[] ips=getResources().getStringArray(x33b 
		        		? R.array.devicesipX33 
		        		: R.array.devicesip);
		        	if (pos<ips.length)
		        	{
		        		mTarget=ips[pos];
		        		bindRemoteAndroid();
		        	}
		        }
	
		        @Override
				public void onNothingSelected(AdapterView<?> parent) 
		        {
		          // Do nothing.
		        }
		    });
		mIntent=new Intent(this,SpeedService.class);
	    bindRemoteAndroid();
//		bindOtherProcessService(this,mIntent,mServiceConnection,BIND_AUTO_CREATE);
	}
	private void bindRemoteAndroid()
	{
		mStart.setEnabled(false);
		bindRemoteAndroidService(this,
			
			// Connect Remote android
			Uri.parse(mTarget),FLAG_PROPOSE_PAIRING|FLAG_ACCEPT_ANONYMOUS,
			
			// Publish APK
			mPublishListener,0,10000,
			
			// Bind remote service
			mIntent,mServiceConnection, BIND_AUTO_CREATE);
		
	}
	private final PublishListener mPublishListener=new PublishListener()
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
	private void bindRemoteAndroidService(
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
		Droid2DroidManager.bindManager(context, 
    		new ManagerListener()
			{
				
				@Override
				public void unbind(Droid2DroidManager manager)
				{
					serviceConnection.onServiceDisconnected(null);
				}
				
				@Override
				public void bind(final Droid2DroidManager manager)
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
		new AsyncTask<Void,Long,long[]>()
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
					publishProgress(rc[0]);
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
			protected void onProgressUpdate(Long... values) 
			{
				mTextInCloud.setText("In cloud:"+values[0]+"ms");
			}
			@Override
			protected void onPostExecute(long[] result)
			{
				mStart.setEnabled(true);
				if (result!=null)
				{
					mTextInCloud.setText("In cloud:"+result[0]+"ms");
					mTextInBoard.setText("In board:"+result[1]+"ms");
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
	}

}