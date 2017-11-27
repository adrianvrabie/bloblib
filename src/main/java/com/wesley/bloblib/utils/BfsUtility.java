/**
 */
package com.wesley.bloblib.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.pmw.tinylog.Logger;

import com.fasterxml.jackson.core.json.ByteSourceJsonBootstrapper;
import com.microsoft.azure.storage.blob.BlockEntry;
import com.wesley.bloblib.Constants;
import com.wesley.bloblib.BfsException;


/**
 * A class which provides utility methods
 * 
 */
/**
 * @author weswu
 *
 */
/**
 * @author weswu
 *
 */
public final class BfsUtility {
	public static String OS = System.getProperty("os.name").toLowerCase();
	/* formate file size */
	public static String formatFileSize(long v) {
	    if (v < 1024) return v + " B";
	    int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
	    return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
	}
	
	
	/**
	 * the exception handle function of the blobfs
	 * @param ex
	 * @param errMessage
	 * @throws BfsException
	 */
	public static void throwBlobfsException(Exception ex, String errMessage) throws BfsException{
		String finalErrMsg = errMessage;
		if (ex instanceof BfsException){
			finalErrMsg = ex.getMessage();
		}
		throw new BfsException(finalErrMsg);
	}
	
	/**
	 * change a list of BlickEntry's block id into a string.
	 * @param blockList
	 * @return
	 */
	public static String blockIds(List<BlockEntry> blockList){
		StringBuilder sb = new StringBuilder();
		for (BlockEntry b : blockList)
		{
		    sb.append(b.getId());
		    sb.append(",");
		}
		return sb.toString();
	}
	
    /**
     * Execute a command and returns the (standard) output through a StringBuilder.
     *
     * @param command The command
     * @param output The output
     * @return The exit code of the command
     * @throws IOException if an I/O error is detected.
     */
    public static int commandExecutor(final String command, final StringBuilder output) throws IOException
    {
        final Process process = Runtime.getRuntime().exec(command);
        final InputStreamReader stream = new InputStreamReader(process.getInputStream());

        // Read the stream
        final char[] buffer = new char[Constants.COMMAND_EXECUTION_BUFFER_SIZE];
        int read;
        while ((read = stream.read(buffer, 0, buffer.length)) >= 0)
        {
            output.append(buffer, 0, read);
        }
        stream.close();
        // Wait until the command finishes (should not be long since we read the output stream)
        while (isRunning(process))
        {
            try
            {
                Thread.sleep(Constants.DEFAULT_THREAD_SLEEP_MILLS);
            }
            catch (final Exception ex)
            {
            	String errMessage = "Exception occurred when execute the command: " + command + ". " + ex.getMessage();
    			BfsUtility.throwBlobfsException(ex, errMessage);
            }
        }
        process.destroy();
        return process.exitValue();
    }
    
    /**
     * @param process
     * @return
     */
    public static boolean isRunning(Process process) {
        try {
            process.exitValue();
            return false;
        } catch (Exception e) {
            return true;
        }
    }
	
	/**
	 * get uid and gid on unix
	 * @return
	 * @throws IOException
	 */
	public static int getIdOnUnix(String cmd) throws IOException{
		int uid = -1;
		final StringBuilder output = new StringBuilder();
		int exitValue = commandExecutor(cmd, output);
		if (exitValue == 0 && null != output){
			uid = Integer.parseInt(output.toString().trim());
		}
		return uid;		
	}
	
	/**
	 * @param path
	 * @return
	 */
	public static String removeLastSlash (String path){
		String tmpPath = "";
		if (path.endsWith("/")) {
			tmpPath = path.substring(0, path.length() - 1);
		} else {
			tmpPath = path;
		}
		return tmpPath;
	}
	
	/**
	 * @param date
	 * @param timeZone
	 * @return
	 */
	public static String changeDateTimeZone(Date date , String timeZone){
		DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return dateFormat.format(date);
	}
	
	/**
	 * @param fileOrDirPath
	 * @return
	 */
	public static String getParentDirPath(String fileOrDirPath) {
	    boolean endsWithSlash = fileOrDirPath.endsWith(File.separator);
	    return fileOrDirPath.substring(0, fileOrDirPath.lastIndexOf(File.separatorChar, 
	            endsWithSlash ? fileOrDirPath.length() - 2 : fileOrDirPath.length() - 1));
	}


	/**
	 * @param date
	 * @return
	 */
	public static String convertDateToString(Date date){
		DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
		return dateFormat.format(date);
	}
	
	/**
	 * @param dateString
	 * @return
	 */
	public static Date convertStringToDate(String dateString){
		DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
		Date date = null;
		try {
			date = dateFormat.parse(dateString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return date;
	}
	
	/**
	 * @return
	 */
	public static boolean isWindows() {		
        return (OS.indexOf("win") >= 0);
    }
	
	/**
	 * @param length
	 * @return
	 */
	public static boolean is512BytesAligned(long length){
	    if (length % 512 == 0){return true;}
	    return false;
	}

	/**
	 * get the 512 bytes aligned length
	 * @param length
	 * @return
	 */
	public static long shrinkTo512BytesAlignedLength(long length){
		long tmpLen = 0;
	    if (length % 512 == 0){tmpLen = length; }
	    else{
	    	if (length < 512){tmpLen = 0; }
	    	else{
	    		tmpLen = (length - length % 512);
	    	}
	    }
	    return tmpLen;
	}
	
	public static long extendTo512BytesAlignedLength(long length){
		long tmpLen = 0;
	    if (length % 512 == 0){ tmpLen = length; }
	    else{
	    	tmpLen = length + (512 - length % 512);
	    }
	    return tmpLen;
	}
	
	/**
	 * make data 512 bytes aligned by padding the rest slots with zero;
	 * @param origData
	 * @return
	 */
	public static byte[]  extendTo512BytesAlignedData(byte[] origData){
		long length = origData.length;
	    if (length % 512 == 0){ return  origData; }
	    else{
	    	long tmpLen = length + (512 - length % 512);
	    	byte[] tgtData = new byte [(int) tmpLen]; // all bytes are initialized with 0's
	    	System.arraycopy(origData, 0, tgtData, 0, (int) length);
	    	return tgtData;
	    }
	}
	
	
}
