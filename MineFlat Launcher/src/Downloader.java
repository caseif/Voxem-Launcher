import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class Downloader implements Runnable {

	private URL url;
	private String fileName;

	public Downloader(URL url, String fileName){
		this.url = url;
		this.fileName = fileName;
	}

	public void run(){
		try {
			ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			File file = new File(fileName);
			file.setReadable(true, false);
			file.setWritable(true, false);
			file.createNewFile();
			FileOutputStream os = new FileOutputStream(fileName);
			os.getChannel().transferFrom(rbc, 0, Launcher.getFileSize(url));
			os.close();
		}
		catch (Exception ex){
			ex.printStackTrace();
			Launcher.progress = "Failed to download " + fileName;
			Launcher.fail = "Errors occurred; see console for details";
		}
	}
}
