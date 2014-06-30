import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * 
 * @author Maxim Roncacé
 * THIS SOFTWARE IS LICENSED UNDER THE GNU GENERAL PUBLIC LICENSE VERSION 3, AND AS SUCH,
 * ALL DERIVATIVES MUST BE RELEASED UNDER THE SAME LICENSE. THE FULL LICENSE TEXT MAY BE
 * VIEWED IN THE FILE ENTITLED "LICENSE" IN THE ROOT DIRECTORY OF THIS JAR. THIS LICENSE
 * FILE MUST BE INCLUDED IN ALL DERIVATIVES OF THIS SOFTWARE.
 *
 */

public class Launcher extends JPanel implements ActionListener {

	/**
	 * The name of the program to be launched
	 */
	public static final String 	NAME = "MineFlat";

	/**
	 * The name of the program's folder in the application data directory
	 */
	public static final String 	FOLDER_NAME = ".mineflat";

	/**
	 * The URL to fetch the JSON configuration from
	 */
	public static final String JSON_LOCATION = "http://amigocraft.net/mineflat/resources.json";

	/**
	 * The rate in milliseconds at which to update the download speed
	 */
	public static final int		SPEED_UPDATE_INTERVAL = 500;

	/**
	 * If a download directory is specified, it will be stored here.
	 */
	public static String downloadDir = "";

	private static Launcher launcher; // standard workaround for static methods

	private static final long serialVersionUID = 1L;

	public static JFrame f; // need I explain this?

	protected JButton play, force, quit, updateYes, updateNo; // or this?

	// maps OS string to library extension (e.g. .dll, .so, whatever ridiculous string it is for Mac)
	public static HashMap<String, String> osExt = new HashMap<String, String>();

	private int btnWidth = 200; // width of buttons
	private int btnHeight = 50; // height of buttons
	private static int width = 800; // width of the window
	private static int height = 500; // height of the window
	private boolean update = false; // whether or not the user chose to update
	boolean updateAvailable = false; // self-explanatory
	public static String progress = null; // current status message
	public static String fail = null; // message displayed when an error occurs
	public static double eSize = -1; // expected size of current file
	public static double aSize = -1; // actual size of downloaded file (atm)
	public static double lastTime = -1; // last time download GUI was updated
	public static double lastSize = -1; // last size of file (used in calculating speed)
	public static double speed = -1; // download speed
	public static String updateMsg = null; // message displayed when an update is available

	public static File main = null;
	public static File natives = null;

	Font font = new Font("Verdana", Font.BOLD, 30); // the font to be used in most places
	Font smallFont = new Font("Verdana", Font.BOLD, 16); // literally used only for the update message

	public Launcher(){
		f.setTitle(NAME + " Launcher");
		if (progress == null){
			this.setLayout(null);
			play = new JButton("Play Game");
			play.setVerticalTextPosition(AbstractButton.CENTER);
			play.setHorizontalTextPosition(AbstractButton.CENTER);
			play.setMnemonic(KeyEvent.VK_ENTER);
			play.setActionCommand("play");
			play.addActionListener(this);
			play.setPreferredSize(new Dimension(btnWidth, btnHeight));
			play.setBounds((width / 2) - (btnWidth / 2), 150, btnWidth, btnHeight);
			this.add(play);

			force = new JButton("Force Update");
			force.setVerticalTextPosition(AbstractButton.CENTER);
			force.setHorizontalTextPosition(AbstractButton.CENTER);
			force.setMnemonic(KeyEvent.VK_ENTER);
			force.setActionCommand("force");
			force.addActionListener(this);
			force.setPreferredSize(new Dimension(btnWidth, btnHeight));
			force.setBounds((width / 2) - (btnWidth / 2), 225, btnWidth, btnHeight);
			this.add(force);

			quit = new JButton("Exit Launcher");
			quit.setVerticalTextPosition(AbstractButton.CENTER);
			quit.setHorizontalTextPosition(AbstractButton.CENTER);
			quit.setMnemonic(KeyEvent.VK_ESCAPE);
			quit.setActionCommand("quit");
			quit.addActionListener(this);
			quit.setPreferredSize(new Dimension(btnWidth, btnHeight));
			quit.setBounds((width / 2) - (btnWidth / 2), 300, btnWidth, btnHeight);
			this.add(quit);
		}
		launcher = this;
	}

	public void actionPerformed(ActionEvent e){ // button was pressed
		if (e.getActionCommand().equals("play")){ // play button was pressed
			// clear dem buttons
			this.remove(play);
			this.remove(force);
			this.remove(quit);
			File dir = new File(appData(), FOLDER_NAME + File.separator + "resources");
			if (!downloadDir.isEmpty()) // -d flag was used
				dir = new File(downloadDir, FOLDER_NAME + File.separator + "resources");
			dir.mkdir();
			try {
				progress = "Downloading JSON filelist...";
				Downloader jsonDl = new Downloader(new URL(JSON_LOCATION), dir.getPath() + File.separator + "resources.json", "JSON filelist");
				Thread jsonT = new Thread(jsonDl);
				jsonT.start();
				while (jsonT.isAlive()){}
				JSONArray files = (JSONArray)((JSONObject)new JSONParser().parse(
						new InputStreamReader(new File(dir.getPath(), "resources.json").toURI().toURL().openStream()))).get("resources");
				List<String> paths = new ArrayList<String>();
				for (Object obj : files){
					JSONObject jFile = (JSONObject)obj;
					String launch = ((String)jFile.get("launch"));
					if (launch != null && launch.equals("true"))
						main = new File(dir, ((String)jFile.get("localPath")).replace("/", File.separator));
					paths.add(((String)jFile.get("localPath")).replace("/", File.separator));
					File file = new File(dir, ((String)jFile.get("localPath")).replace("/", File.separator));
					boolean reacquire = false;
					if (!file.exists() || update || !jFile.get("md5").equals(md5(file.getPath()))){
						reacquire = true;
						if (update)
							System.out.println("Update forced, so file " + (String)jFile.get("localPath") + " must be updated");
						else
							System.out.println("MD5 checksum for file " + (String)jFile.get("localPath") + " does not match expected value");
						System.out.println("Attempting to reacquire...");
						file.delete();
						file.getParentFile().mkdirs();
						file.createNewFile();
						progress = "Downloading " + (String)jFile.get("id");
						paintImmediately(0, 0, width, height);
						Downloader dl = new Downloader(new URL((String)jFile.get("location")),
								dir + File.separator + ((String)jFile.get("localPath")).replace("/", File.separator),
								(String)jFile.get("id"));
						Thread th = new Thread(dl);
						th.start();
						eSize = getFileSize(new URL((String)jFile.get("location"))) / 8;
						speed = 0;
						lastSize = 0;
						while (th.isAlive()){
							aSize = file.length() / 8;
							if (lastTime != -1){
								if (System.currentTimeMillis() - lastTime >= SPEED_UPDATE_INTERVAL){
									speed = ((aSize - lastSize) / 1000) /
											((System.currentTimeMillis() - lastTime) / 1000);
									lastTime = System.currentTimeMillis();
									lastSize = aSize;
								}
							}
							else {
								speed = 0;
								lastTime = System.currentTimeMillis();
							}
							paintImmediately(0, 0, width, height);
						}
						eSize = -1;
						aSize = -1;
					}
					if (jFile.containsKey("extract")){
						HashMap<String, JSONObject> elements = new HashMap<String, JSONObject>();
						for (Object ex : (JSONArray)jFile.get("extract")){
							elements.put((String)((JSONObject)ex).get("path"), (JSONObject)ex);
							paths.add(((String)((JSONObject)ex).get("localPath")).replace("/", File.separator));
							File f = new File(dir, ((String)((JSONObject)ex).get("localPath")).replace("/", File.separator));;
							if (!f.exists() ||
									(!f.isDirectory() && ((JSONObject)ex).get("md5") != null &&
									!md5(f.getPath()).equals(((String)((JSONObject)ex).get("md5")))))
								reacquire = true;
							if (((JSONObject)ex).get("id").equals("natives"))
								natives = new File(dir, ((String)((JSONObject)ex).get("localPath")).replace("/", File.separator));
						}
						if (reacquire){
							try {
								ZipFile zip = new ZipFile(new File(dir, ((String)jFile.get("localPath")).replace("/", File.separator)));
								@SuppressWarnings("rawtypes")
								Enumeration en = zip.entries();
								List<String> dirs = new ArrayList<String>();
								while (en.hasMoreElements()){
									ZipEntry entry = (ZipEntry)en.nextElement();
									boolean extract = false;
									String parentDir = "";
									if (elements.containsKey(entry.getName()))
										extract = true;
									else
										for (String d : dirs)
											if (entry.getName().contains(d)){
												extract = true;
												parentDir = d;
											}
									if (extract){
										progress = "Extracting " + (elements.containsKey(entry.getName()) ?
												elements.get(entry.getName()).get("id") :
													entry.getName().substring(entry.getName().indexOf(parentDir),
															entry.getName().length()).replace("/", File.separator));
										paintImmediately(0, 0, width, height);
										if (entry.isDirectory()){
											if (parentDir.equals(""))
												dirs.add((String)elements.get(entry.getName()).get("localPath"));
										}
										else {
											File path = new File(dir, (parentDir.equals("")) ?
													((String)elements.get(entry.getName()).get("localPath")).replace("/", File.separator) :
														entry.getName().substring(entry.getName().indexOf(parentDir),
																entry.getName().length()).replace("/", File.separator)
													);
											if (path.exists())
												path.delete();
											unzip(zip, entry, path);
										}
									}
								}
							}
							catch (Exception ex){
								ex.printStackTrace();
								createExceptionLog(ex);
								progress = "Failed to extract files from " + (String)jFile.get("id");
								fail = "Errors occurred; see log file for details";
								launcher.paintImmediately(0, 0, width, height);
							}
						}
					}
				}
				
				for (String s : paths)
					System.out.println(s);

				checkFile(dir, dir, paths);
			}
			catch (Exception ex){
				ex.printStackTrace();
				createExceptionLog(ex);
				progress = "Failed to read JSON filelist";
				fail = "Errors occurred; see log file for details";
				launcher.paintImmediately(0, 0, width, height);
			}

			launch();
		}
		else if (e.getActionCommand().equals("force")){
			force.setEnabled(false);
			force.setText("Will Force!");
			update = true;
		}
		else if (e.getActionCommand().equals("quit")){
			pullThePlug();
		}
	}

	// in a separate method for organizational purposes
	private static void createAndShowGUI(){
		f = new JFrame(" Launcher");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Launcher l = new Launcher();
		l.setOpaque(true);
		f.setContentPane(l);
		f.pack();
		f.setVisible(true);
		f.setSize(width, height);
		f.setResizable(false);
		f.setLocationRelativeTo(null);
		try {
			f.setIconImage(ImageIO.read(Launcher.class.getResourceAsStream("/images/icon.png")));
		}
		catch (Exception ex){}
	}

	public static void main(String[] args){
		int i = 0;
		for (String s : args){
			if (s.equalsIgnoreCase("-dir")){
				downloadDir = args[i + 1];
				if (!new File(downloadDir).exists()){
					try {
						new File(downloadDir).mkdir();
					}
					catch (Exception ex){
						ex.printStackTrace();
						progress = "Failed to create download directory";
						fail = "Errors occurred; see console for details";
						launcher.paintImmediately(0, 0, width, height);
					}
				}
			}
			i += 1;
		}
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				createAndShowGUI();
			}
		});
	}

	public void paintComponent(Graphics g){
		super.paintComponent(g);
		g.setFont(font);
		if (updateMsg != null){
			g.drawString("Update Available!", centerText(g, "Update Available!"), 50);
			g.setFont(smallFont);
			g.drawString(updateMsg, centerText(g, updateMsg), 100);
		}

		else if (progress == null)
			g.drawString(NAME + " Launcher", centerText(g, NAME + " Launcher"), 50);

		else {
			g.drawString(progress, centerText(g, progress), height / 2);
			if (fail != null)
				g.drawString(fail, centerText(g, fail), height / 2 + 50);
			else {
				if (aSize != -1 && eSize != -1){
					String s = (int)((double)aSize * 8 / 1024) + "/" + (int)((double)eSize * 8 / 1024) + " kb";
					g.drawString(s, centerText(g, s), height / 2 + 40);
					String sp = "@" + (int)speed + " kiB/s";
					if (speed >= 1000)
						sp = "@" + String.format("%.2f", (speed / 1024)) + " Mbps";
					g.drawString(sp, centerText(g, sp), height / 2 + 80);
					int barWidth = 500;
					int barHeight = 35;
					g.setColor(Color.LIGHT_GRAY);
					g.drawRect(width / 2 - barWidth / 2, height / 2 + 100, barWidth, barHeight);
					g.setColor(Color.GREEN);
					g.fillRect(width / 2 - barWidth / 2 + 1, height / 2 + 100 + 1,
							(int)(((double)aSize / (double)eSize) * (double)barWidth - 2),
							barHeight - 1);
					g.setColor(new Color(.2f, .2f, .2f));
					int percent = (int)((double)aSize / (double)eSize * 100);
					g.drawString(percent + "%", centerText(g, percent + "%"), height / 2 + 128);
				}
			}
		}
	}

	private static int centerText(Graphics g, String text){
		int stringLen = (int)
				g.getFontMetrics().getStringBounds(text, g).getWidth();
		return width / 2 - stringLen / 2;
	}

	private static void pullThePlug(){
		WindowEvent wev = new WindowEvent(f, WindowEvent.WINDOW_CLOSING);
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
		f.setVisible(false);
		f.dispose();
		System.exit(0); 
	}

	private static String appData(){
		String OS = System.getProperty("os.name").toUpperCase();
		if (OS.contains("WIN"))
			return System.getenv("APPDATA");
		else if (OS.contains("MAC"))
			return System.getProperty("user.home") + "/Library/Application Support";
		try {
			String username = convertStreamToString(
					Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "echo $USER"})
					.getInputStream(), false);
			if (new File("/usr/home/" + username).exists())
				return "/usr/home/" + username;
			else
				return "/home/" + username;
		}
		catch (Exception ex){
			ex.printStackTrace();
			progress = "Failed to get home directory";
			fail = "Errors occurred; see console for details";
			launcher.paintImmediately(0, 0, width, height);
		}
		return System.getProperty("user.dir");
	}

	public static void unzip(ZipFile zip, ZipEntry entry, File dest){
		if (dest.exists())
			dest.delete();
		dest.getParentFile().mkdirs();
		try {
			BufferedInputStream bIs = new BufferedInputStream(zip.getInputStream(entry));
			int b;
			byte buffer[] = new byte[1024];
			FileOutputStream fOs = new FileOutputStream(dest);
			BufferedOutputStream bOs = new BufferedOutputStream(fOs, 1024);
			while ((b = bIs.read(buffer, 0, 1024)) != -1)
				bOs.write(buffer, 0, b);
			bOs.flush();
			bOs.close();
			bIs.close();
		}
		catch (Exception ex){
			ex.printStackTrace();
			createExceptionLog(ex);
			progress = "Failed to unzip " + entry.getName();
			fail = "Errors occurred; see log file for details";
			launcher.paintImmediately(0, 0, width, height);
		}
	}

	private void launch(){
		if (main == null){
			progress = "Failed to find launch candidate!";
			paintImmediately(0, 0, width, height);
			return;
		}
		String os = "";
		if (System.getProperty("os.name").toUpperCase().contains("WIN"))
			os = "windows";
		else if (System.getProperty("os.name").toUpperCase().contains("MAC"))
			os = "macosx";
		else
			os = "linux";
		natives = new File(natives, os);

		progress = "Launching";
		paintImmediately(0, 0, width, height);
		try {
			Process p = Runtime.getRuntime().exec(
					new String[]{"java", "-Djava.library.path=" + natives, "-jar", main.getPath()}, null, main.getParentFile());
			InputStream errStream = p.getErrorStream();
			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			byte[] bytes = new byte[4096];
			while (in.read(bytes) != -1){}
			String errors = convertStreamToString(errStream, true).replaceAll("\\s+","");
			if (errors.isEmpty())
				pullThePlug();
			else {
				System.err.println(errors);
				createExceptionLog(errors, true);
				progress = "Exception occurred in game thread";
				fail = "Errors occurred; see log file for details";
				paintImmediately(0, 0, width, height);
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
			createExceptionLog(ex);
			progress = "Exception occurred in launcher thread";
			fail = "Errors occurred; see log file for details";
			paintImmediately(0, 0, width, height);
		}
	}

	public static int getFileSize(URL url){
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.getInputStream();
			return conn.getContentLength();
		}
		catch (Exception e){
			return -1;
		}
		finally {
			conn.disconnect();
		}
	}

	public static String convertStreamToString(InputStream is, boolean newlines){
		/*
		 * Why don't people update to Java 7? Like, seriously. It's November of 2013 as of
		 * typing this, Java 8 is some 4 months away, and there are still people who use my
		 * code who are running Java 6. Like 5% of the users are using an obsolete version
		 * of Java from 2011. It's not like it's even that hard to update. Honestly, you
		 * just click a couple of buttons, check a box, and bam, you've made my job easier.
		 * And yet, because these people are so lazy, I'm forced to compile with Java 6 to
		 * accommodate for this ridiculous minority. I say this because I could accomplish
		 * what this try-block does in about 2 lines of code, but nope, gotta use Java 6.
		 * I'm really psyched for lambda expressions in Java 8, but I can't even use those
		 * until Java 9 is out, because there'll be those morons who just refuse to update
		 * past 7. It's stupid, it really is.</rant>
		 */
		InputStreamReader isr = new InputStreamReader(is);
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(isr);
		try {
			String read = br.readLine();
			while(read != null) {
				sb.append(read);
				if (newlines)
					sb.append("\n");
				read = br.readLine();
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
			//createExceptionLog(); // I'm leaving this here as a lesson to my future self.
			progress = "Failed to get output from launch command";
			fail = "Errors occurred; see log file for details";
			launcher.paintImmediately(0, 0, width, height);
		}
		finally {
			try {
				isr.close();
				br.close();
			}
			catch (Exception ex){
				// dunno why this would happen anyway, but whatever
				ex.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static void createExceptionLog(Exception ex){
		createExceptionLog(ex, false);
	}

	public static void createExceptionLog(String s){
		createExceptionLog(s, false);
	}

	public static void createExceptionLog(Exception ex, boolean gameThread){
		Calendar cal = Calendar.getInstance();
		String minute = cal.get(Calendar.MINUTE) + "";
		if (minute.length() < 2)
			minute = "0" + minute;
		String second = cal.get(Calendar.SECOND) + "";
		if (second.length() < 2)
			second = "0" + second;
		String time = cal.get(Calendar.YEAR) + "-" +
				(cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "_" +
				cal.get(Calendar.HOUR_OF_DAY) + "-" + minute + "-" +
				second + "-" + cal.get(Calendar.MILLISECOND);
		try {
			if (!new File(appData(), FOLDER_NAME + File.separator + "errorlogs").exists())
				new File(appData(), FOLDER_NAME + File.separator + "errorlogs").mkdir();
			new File(appData(), FOLDER_NAME + File.separator +
					"errorlogs" + File.separator + time + ".log").createNewFile();
			System.out.println("Saved error log to " +
					appData() + File.separator + FOLDER_NAME + File.separator +
					"errorlogs" + File.separator + time + ".log");
			PrintWriter writer = new PrintWriter(appData() + File.separator + FOLDER_NAME +
					File.separator + "errorlogs" + File.separator + time + ".log", "UTF-8");
			writer.print(getLogHeader(gameThread));
			ex.printStackTrace(writer);
			writer.print("-----------------END ERROR LOG-----------------\n");
			writer.close();
		}
		catch (Exception exc){
			// Well, shit.
			exc.printStackTrace();
			Launcher.progress = "An exception occurred while saving an exception log.";
			Launcher.fail = "Errors occurred; see console for details.";
		}
	}

	public static void createExceptionLog(String s, boolean gameThread){
		String log = getLogHeader(gameThread);
		log += s;
		log += "\n-----------------END ERROR LOG-----------------\n";
		Calendar cal = Calendar.getInstance();
		String minute = cal.get(Calendar.MINUTE) + "";
		if (minute.length() < 2)
			minute = "0" + minute;
		String second = cal.get(Calendar.SECOND) + "";
		if (second.length() < 2)
			second = "0" + second;
		String time = cal.get(Calendar.YEAR) + "-" +
				(cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "_" +
				cal.get(Calendar.HOUR_OF_DAY) + "-" + minute + "-" +
				second + "-" + cal.get(Calendar.MILLISECOND);
		try {
			if (!new File(appData(), FOLDER_NAME + File.separator + "errorlogs").exists())
				new File(appData(), FOLDER_NAME + File.separator + "errorlogs").mkdir();
			new File(appData(), FOLDER_NAME + File.separator +
					"errorlogs" + File.separator + time + ".log").createNewFile();
			System.out.println("Saved error log to " +
					appData() + File.separator + FOLDER_NAME + File.separator +
					"errorlogs" + File.separator + time + ".log");
			PrintWriter writer = new PrintWriter(appData() + File.separator + FOLDER_NAME +
					File.separator + "errorlogs" + File.separator + time + ".log", "UTF-8");
			writer.print(log);
			writer.close();
		}
		catch (Exception ex){
			// Well, shit.
			ex.printStackTrace();
			Launcher.progress = "An exception occurred while saving an exception log.";
			Launcher.fail = "Errors occurred; see console for details.";
		}
	}

	public static String getLogHeader(boolean gameThread){
		Calendar cal = Calendar.getInstance();
		String minute = cal.get(Calendar.MINUTE) + "";
		if (minute.length() < 2)
			minute = "0" + minute;
		String second = cal.get(Calendar.SECOND) + "";
		if (second.length() < 2)
			second = "0" + second;
		String stage = "";
		String version = "";
		try {
			File versionFile = new File(appData(), FOLDER_NAME + File.separator + "resources");
			if (!downloadDir.isEmpty())
				versionFile = new File(downloadDir, FOLDER_NAME + File.separator + "resources");
			versionFile = new File(versionFile, "version");
			BufferedReader currentVersionReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(versionFile)));
			String line;
			while ((line = currentVersionReader.readLine()) != null){
				if (line.startsWith("stage: ")){
					stage = line.split(": ")[1];
				}
				else if (line.startsWith("version: ")){
					version = line.split(": ")[1];
				}
			}
			currentVersionReader.close();
		}
		catch (Exception ex){
			ex.printStackTrace();
			stage = "Failed to determine";
			version = "Failed to determine";
		}
		String log = "--";
		for (int i = 0; i < NAME.length(); i++)
			log += "-";
		log += "------------\n"; // ASCII blocks like a boss
		log += "| " + NAME.toUpperCase() + " ERROR LOG |\n";
		log = "--";
		for (int i = 0; i < NAME.length(); i++)
			log += "-";
		log += "------------\n"; // #3fancy5u
		log += "Generated at " + cal.get(Calendar.HOUR_OF_DAY) + ":" +
				minute + ":" + second + " on " +
				(cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "-" +
				cal.get(Calendar.YEAR) + "\n";
		log += "Exception occurred in game thread: " + gameThread + "\n";
		log += NAME + " stage: " + stage + "\n";
		log += NAME + " version: " + version + "\n";
		log += "\n----------------BEGIN ERROR LOG----------------\n";
		return log;
	}

	public static void checkFile(File dir, File file, List<String> allowed){
		System.out.println(file.getPath().replace(dir.getPath() + File.separator, ""));
		if (file.isDirectory()){
			if (!allowed.contains(file.getPath().replace(dir.getPath() + File.separator, "")))
				for (File f : file.listFiles())
					checkFile(dir, f, allowed);
		}
		else if (!allowed.contains(file.getPath().replace(dir.getPath() + File.separator, "")))
			file.delete();
	}

	public static String md5(String path){
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			FileInputStream fis = new FileInputStream(path);
			byte[] dataBytes = new byte[1024];

			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1){
				md.update(dataBytes, 0, nread);
			}
			fis.close();

			byte[] mdbytes = md.digest();

			StringBuffer sb = new StringBuffer();
			for (int i=0;i<mdbytes.length;i++) {
				String hex=Integer.toHexString(0xff & mdbytes[i]);
				if(hex.length()==1)
					sb.append('0');
				sb.append(hex);
			}
			return sb.toString();
		}
		catch (Exception ex){
			ex.printStackTrace();
			System.err.println("Failed to calculate checksum for " + path);
			createExceptionLog(ex);
			progress = "Failed to calculate checksum for file!";
			fail = "Errors occurred, see exception log for details";
		}

		return null;
	}
}
