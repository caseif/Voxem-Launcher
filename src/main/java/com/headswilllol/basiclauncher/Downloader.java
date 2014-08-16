package com.headswilllol.basiclauncher;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Downloader implements Runnable {

	private URL url;
	private String fileName;
	private String name;

	/**
	 * 
	 * @param url The online URL of the file to download
	 * @param fileName The local file to download into
	 * @param name The name to be displayed in case of error
	 */
	
	public Downloader(URL url, String fileName, String name){
		this.url = url;
		this.fileName = fileName;
		this.name = name;
	}

	public void run(){
		try {
			ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			File file = new File(fileName);
			file.setReadable(true, false);
			file.setWritable(true, false);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream os = new FileOutputStream(fileName);
			os.getChannel().transferFrom(rbc, 0, Launcher.getFileSize(url));
			os.close();
		}
		catch (Exception ex){
			ex.printStackTrace();
			Launcher.createExceptionLog(ex);
			Launcher.progress = "Failed to download " + name;
			Launcher.fail = "Errors occurred; see console for details";
			new Launcher().repaint();
		}
	}
}
