package com.headswilllol.basiclauncher;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Downloader implements Runnable {

	private URL url;
	private String fileName;
	private String name;
	private boolean spoof;

	/**
	 *
	 * @param url The online URL of the file to download
	 * @param fileName The local file to download into
	 * @param name The name to be displayed in case of error
	 */

	public Downloader(URL url, String fileName, String name, boolean spoof){
		this.url = url;
		this.fileName = fileName;
		this.name = name;
	}

	public void run(){
		try {
			URLConnection conn = url.openConnection();
			if (!url.toString().toLowerCase().contains("sourceforge") && !spoof) // SourceForge will serve a download page if we spoof the user agent
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
			conn.connect();
			ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
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
