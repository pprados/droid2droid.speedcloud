package org.droid2droid.speedcloud;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SpeedService extends Service
{

	@Override
	public IBinder onBind(Intent arg0)
	{
		return new SpeedImpl();
	}

}
