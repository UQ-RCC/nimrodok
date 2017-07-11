package org.monash.nimrod.optim;


//package org.monash.nimrod.optim;

//import ptolemy.kernel.util.IllegalActionException;
//import java.lang.RuntimeException;
//import java.lang.Math;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

//import ptolemy.data.StringToken;
import ptolemy.kernel.util.IllegalActionException;



//import OptimParameter.Toke;





public class Debug 
{
	static File file;
	static FileWriter out;	
	static String fileName;

 	
	// given one point, creates simplex
	public static void startDebug(String debugFileName) throws IllegalActionException 
	{
		//out.write("bbb");

		fileName = debugFileName;
		if(debugFileName==null)
			throw new IllegalActionException("Debug file name not specified");
		try
		{
			

			file = new File(debugFileName);
			out = new FileWriter(file);
			out.write("Debug file for NimrodO\n");
		}
		catch (IOException e) 
		{
			throw new IllegalActionException("Cannot construct writer for file '"+ debugFileName + "'"); 
		}
		String str = file.getAbsolutePath();
		//System.out.println("Writing debug log to: '" + str + "'");
		
	}
 
	public static void write(String message) 
	{
		//try
		{
			
			//out.write(message);	
			//System.out.println(message);
		}
		//catch (IOException e) 
		{
			//throw new IllegalActionException("Cannot write to debug file '" + fileName + "'");
		}
		
	}
	
	public static void write(String message, int level) 
	{
		if(level <= 1)
			write(message);
		
	}

	
	public static void write(String message, int level, int levelRequired) 
	{
		if(level >= levelRequired)
			write(message);
		
	}

	
	public static void showDoubleArray(String message, double[] array, int size) 
	{
		//try
	
			
			//out.write(message);	
		//System.out.println(message);
	
		for(int i=0; i < size; ++i)
		{
			//System.out.print(array[i]+" ");
			
			//if(i%10 == 9)
				//System.out.println("");
				
		}
		//System.out.println("");
		//catch (IOException e) 
		{
			//throw new IllegalActionException("Cannot write to debug file '" + fileName + "'");
		}
		
	}
	
	public void close() throws IllegalActionException 
	{
		try
		{
			out.close();
		}
		catch (IOException e) 
		{
			throw new IllegalActionException("Cannot close writer to '" + fileName + "'");
		}
	}

   	
}



