package org.remoteandroid.speedcloud;

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
