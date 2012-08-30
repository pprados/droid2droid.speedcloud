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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.os.RemoteException;

public class SpeedImpl extends Speed.Stub
{
	@Override
	public void calc(int maxnumber) throws RemoteException
	{
		while (maxnumber-->=0)
		{
			benchCPU();
//			benchNet();
		}
	}

	private void benchNet()
	{
		byte[] buf=new byte[4096];
		
		try
		{
			InputStream in=new URL("http://www.google.fr").openConnection().getInputStream();
			while (in.read(buf)!=-1);
			in.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	private void benchCPU()
	{
		int cycle=100000;
		float denominator = 3.0f; 
		float numerator = 1.0f; 
		float temp, sum = 1.0f;  
		while(cycle-->=0) 
		{
			numerator = -numerator;
			temp = numerator;
			temp /= denominator;  
			sum += temp;
			denominator += 2; 
//			System.out.println(sum * 4 + "\n"); 
		}
		
	}
}
