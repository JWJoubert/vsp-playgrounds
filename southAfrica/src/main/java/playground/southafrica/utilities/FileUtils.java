package playground.southafrica.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {

	/**
	 * Copies a file from one location to another.
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
	
	/**
	 * Cleans a given file. If the file is a directory, it first cleans all
	 * its contained file (or folders).
	 * @param folder
	 */
	public static void delete(File folder){
		if(folder.isDirectory()){
			File[] contents = folder.listFiles();
			for(File file : contents){
				FileUtils.delete(file);
			}
			folder.delete();
		} else{
			folder.delete();
		}
	}


}
