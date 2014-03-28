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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
	 * The name of the program's main jarfile in the application data directory
	 */
	public static final String 	JAR_NAME = "mineflat.jar";
	/**
	 * The name of the program's folder in the application data directory
	 */
	public static final String 	FOLDER_NAME = ".mineflat";
	/**
	 * The location to download the LWJGL/Slick libraries from.
	 */
	public static final String 	LIB_LOCATION = "http://slick.ninjacave.com/slick.zip";
	/**
	 * The location from which to download the program's main JAR.
	 * This may be replaced with a PHP script which serves the file in order to handle
	 * validation and to keep track of downloads
	 */
	public static final String 	JAR_LOCATION = "http://amigocraft.net/mineflat/serve.php";
	/**
	 * The location to download the online version file from (used in updating)
	 */
	public static final String 	VERSION_FILE_LOCATION = "http://amigocraft.net/mineflat/version";
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
		// OS used in determining which natives to extract
		String os = "";
		if (System.getProperty("os.name").toUpperCase().contains("WIN"))
			os = "windows";
		else if (System.getProperty("os.name").toUpperCase().contains("MAC"))
			os = "macosx";
		else
			os = "linux"; // probably not the best way to do this, but what else could it be?
		// associate the OS with the extension
		osExt.put("windows", ".dll");
		osExt.put("macosx", ".jnilib");
		osExt.put("macosx2", ".dylib");
		osExt.put("linux", ".so");
		if (e.getActionCommand().equals("play")){ // play button was pressed
			// clear dem buttons
			this.remove(play);
			this.remove(force);
			this.remove(quit);
			File dir = new File(appData(), FOLDER_NAME);
			if (!downloadDir.isEmpty()) // -d flag was used
				dir = new File(downloadDir, FOLDER_NAME);
			dir.mkdir();
			File bin = new File (dir, "bin");
			if (update)
				bin.delete(); // remove bin folder altogether because it's the easiest way
			bin.mkdir();
			File main = new File(bin, JAR_NAME);
			File lwjgl = new File(bin, "lwjgl.jar");
			File lwjgl_util = new File(bin, "lwjgl_util.jar");
			File jinput = new File(bin, "jinput.jar");
			File slick = new File(bin, "slick.jar");
			File nativeDir = new File(bin, "native");
			nativeDir = new File(nativeDir, os);
			if (!lwjgl.exists() || !lwjgl_util.exists() || !jinput.exists() ||
					!nativeDir.exists() || update){ // we should probably download the libraries
				File lwjglZip = new File(bin, "lwjgl.zip");
				progress = "Downloading libraries";
				paintImmediately(0, 0, width, height);
				try {
					Downloader dl = new Downloader(new URL(
							LIB_LOCATION),
							lwjglZip.getPath(), "LWJGL");
					eSize = getFileSize(new URL(LIB_LOCATION));
					Thread t = new Thread(dl);
					t.start();
					while (t.isAlive()){
						aSize = lwjglZip.length();
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
					lastTime = -1;
					lastSize = 0;
					speed = 0;
					aSize = -1;
					eSize = -1;
				}
				catch (Exception ex){
					ex.printStackTrace();
					createExceptionLog(ex);
					progress = "Failed to download libraries";
					fail = "Errors occurred; see log file for details";
					repaint();
				}
				if (fail == null){
					if (!new File(bin, "native").exists())
						new File(bin, "native").mkdir();
					if (!nativeDir.exists())
						nativeDir.mkdir();
					try {
						ZipFile zip = new ZipFile(new File(bin, "lwjgl.zip"));
						@SuppressWarnings("rawtypes")
						Enumeration en = zip.entries();
						List<String> extracted = new ArrayList<String>();
						while (en.hasMoreElements()){
							ZipEntry entry = (ZipEntry)en.nextElement();
							if (entry.getName().equals("lib/lwjgl.jar")){
								if (!lwjgl.exists() || update){
									progress = "Extracting LWJGL";
									paintImmediately(0, 0, width, height);
									unzip(zip, entry, lwjgl);
								}
								extracted.add("lwjgl");
							}
							else if (entry.getName().equals("lib/lwjgl_util.jar")){
								if (!lwjgl_util.exists() || update){
									progress = "Extracting LWJGL Util";
									paintImmediately(0, 0, width, height);
									unzip(zip, entry, lwjgl_util);
								}
								extracted.add("util");
							}
							else if (entry.getName().equals("lib/jinput.jar")){
								if (!jinput.exists() || update){
									progress = "Extracting JInput";
									paintImmediately(0, 0, width, height);
									unzip(zip, entry, jinput);
								}
								extracted.add("jinput");
							}
							else if (entry.getName().equals("lib/slick.jar")){
								if (!slick.exists() || update){
									progress = "Extracting Slick";
									paintImmediately(0, 0, width, height);
									unzip(zip, entry, slick);
								}
								extracted.add("slick");
							}
							else if ((entry.getName().endsWith(osExt.get(os)) ||
									(osExt.containsKey(osExt) &&
											entry.getName().endsWith(osExt.get(os + "2")))) &&
											!entry.isDirectory()){
								File nativeFile = new File(nativeDir, entry.getName());
								if (!nativeFile.exists() || !update){
									progress = "Extracting natives";
									paintImmediately(0, 0, width, height);
									unzip(zip, entry, nativeFile);
								}
								extracted.add("native");
							}
						}
						if (!extracted.contains("lwjgl")){
							progress = "Failed to extract lwjgl.jar from library ZIP";
							fail = "Errors occurred; see log file for details";
						}
						else if (!extracted.contains("util")){
							progress = "Failed to extract lwjgl_util.jar from library ZIP";
							fail = "Errors occurred; see log file for details";
						}
						else if (!extracted.contains("jinput")){
							progress = "Failed to extract jinput.jar from library ZIP";
							fail = "Errors occurred; see log file for details";
						}
						else if (!extracted.contains("native")){
							progress = "Failed to extract natives from library ZIP";
							fail = "Errors occurred; see log file for details";
						}
						else if (!extracted.contains("slick")){
							progress = "Failed to extract slick.jar from library ZIP";
							fail = "Errors occurred; see log file for details";
						}
						repaint();
						zip.close();
						lwjglZip.delete();
					}
					catch (Exception ex){
						ex.printStackTrace();
						createExceptionLog(ex);
						progress = "Failed to extract libraries";
						fail = "Errors occurred; see log file for details";
						repaint();
					}
				}
			}
			if (fail == null){ // make sure something didn't get screwed up
				File versionFile = new File(appData(), FOLDER_NAME);
				if (!downloadDir.isEmpty())
					versionFile = new File(downloadDir, FOLDER_NAME);
				versionFile = new File(versionFile, "version");
				if (update || !versionFile.exists()){
					progress = "Creating version file"; // no one will ever see this -_-
					createVersionFile();
					progress = "Downloading " + JAR_NAME;
					try {
						main.delete();
						downloadMain(main);
					}
					catch (Exception ex){ // whoopsy daisy!
						ex.printStackTrace();
						createExceptionLog(ex);
						progress = "Failed to download main JAR";
						fail = "Error occurred; see log file for details";
					}
					launch();
				}
				else {
					try {
						if (versionFile.exists()){ // check that the main JAR is up to date
							BufferedReader currentVersionReader = new BufferedReader(
									new InputStreamReader(new FileInputStream(versionFile)));
							BufferedReader latestVersionReader = new BufferedReader(
									new InputStreamReader(new URL(VERSION_FILE_LOCATION)
									.openStream()));
							String currentStage = "";
							String currentVersion = "";
							String latestStage = "";
							String latestVersion = "";

							String line;
							while ((line = currentVersionReader.readLine()) != null){
								if (line.startsWith("stage: ")){
									currentStage = line.split(": ")[1];
								}
								else if (line.startsWith("version: ")){
									currentVersion = line.split(": ")[1];
								}
							}
							currentVersionReader.close();

							while ((line = latestVersionReader.readLine()) != null){
								if (line.startsWith("stage: ")){
									latestStage = line.split(": ")[1];
								}
								else if (line.startsWith("version: ")){
									latestVersion = line.split(": ")[1];
								}
							}
							latestVersionReader.close();

							boolean versionDifference = false;
							String[] currentVersionArray = currentVersion.split("\\.");
							String[] latestVersionArray = latestVersion.split("\\.");
							if (currentVersionArray.length == latestVersionArray.length){
								for (int i = 0; i < currentVersionArray.length; i++){
									if (!currentVersionArray[i].equals(latestVersionArray[i])){
										versionDifference = true;
										break;
									}
								}
							}
							else
								versionDifference = true;

							if (!currentStage.equals(latestStage) || versionDifference && !update){
								updateAvailable = true;
								updateMsg = "Would you like to update from version " +
										currentVersion + " " + currentStage + " to version " +
										latestVersion + " " + latestStage + "?";
							}
						}
					}
					catch (Exception ex){
						ex.printStackTrace();
						createExceptionLog(ex);
						progress = "Failed to get latest version";
						fail = "Errors occurred; see log file for details";
						repaint();
						try {
							Thread.sleep(2000L);
						}
						catch (Exception exc){ // wakey wakey little thread
							exc.printStackTrace();
						}
						launch();
					}
				}
			}

			if (fail == null){
				if (main.exists() && !update && updateAvailable){
					remove(play);
					remove(force);
					remove(quit);
					updateYes = new JButton("Update");
					updateYes.setVerticalTextPosition(AbstractButton.CENTER);
					updateYes.setHorizontalTextPosition(AbstractButton.CENTER);
					updateYes.setActionCommand("yesUpdate");
					updateYes.addActionListener(this);
					updateYes.setPreferredSize(new Dimension(btnWidth, btnHeight));
					updateYes.setBounds((width / 2) - (btnWidth / 2), 200, btnWidth, btnHeight);
					this.add(updateYes);

					updateNo = new JButton("Not Now");
					updateNo.setVerticalTextPosition(AbstractButton.CENTER);
					updateNo.setHorizontalTextPosition(AbstractButton.CENTER);
					updateNo.setActionCommand("noUpdate");
					updateNo.addActionListener(this);
					updateNo.setPreferredSize(new Dimension(btnWidth, btnHeight));
					updateNo.setBounds((width / 2) - (btnWidth / 2), 300, btnWidth, btnHeight);
					this.add(updateNo);

					this.paintImmediately(0, 0, width, height);
				}

				if (!updateAvailable){
					launch();
				}
			}
		}
		else if (e.getActionCommand().equals("force")){
			force.setEnabled(false);
			force.setText("Will Force!");
			update = true;
		}
		else if (e.getActionCommand().equals("quit")){
			pullThePlug();
		}
		else if (e.getActionCommand().equals("yesUpdate")){
			updateMsg = null;
			remove(updateYes);
			remove(updateNo);
			paintImmediately(0, 0, width, height);

			File nativeDir = new File(appData(), FOLDER_NAME);
			if (!downloadDir.isEmpty())
				nativeDir = new File(downloadDir, FOLDER_NAME);
			nativeDir = new File(nativeDir, "bin");
			File main = new File(nativeDir, JAR_NAME);
			nativeDir = new File(nativeDir, "natives");
			nativeDir = new File(nativeDir, os);

			progress = "Downloading " + JAR_NAME;
			paintImmediately(0, 0, width, height);
			try {
				downloadMain(main);
				createVersionFile();
				launch();
			}
			catch (Exception ex){
				ex.printStackTrace();
				createExceptionLog(ex);
				progress = "Failed to download main JAR";
				fail = "Errors occurred; see log file for details";
				paintImmediately(0, 0, width, height);
			}
		}

		else if (e.getActionCommand().equals("noUpdate")){

			updateMsg = null;
			remove(updateYes);
			remove(updateNo);
			paintImmediately(0, 0, width, height);

			launch();
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
					String s = (int)((double)aSize / 1024) + "/" + (int)((double)eSize / 1024) + " kb";
					g.drawString(s, centerText(g, s), height / 2 + 40);
					String sp = "@" + (int)speed + " KB/s";
					if (speed >= 1024)
						sp = "@" + String.format("%.2f", (speed / 1024)) + " MB/s";
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
			progress = "Failed to unzip LWJGL archive";
			fail = "Errors occurred; see log file for details";
			launcher.paintImmediately(0, 0, width, height);
		}
	}

	private static boolean createVersionFile(){
		try {
			File versionFile = new File(appData(), FOLDER_NAME);
			if (!downloadDir.isEmpty())
				versionFile = new File(downloadDir, FOLDER_NAME);
			versionFile = new File(versionFile, "version");
			if (versionFile.exists())
				versionFile.delete();
			versionFile.createNewFile();
			BufferedReader latestVersionReader = new BufferedReader(new InputStreamReader(
					new URL(VERSION_FILE_LOCATION).openStream()));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
					versionFile)));
			String line;
			while ((line = latestVersionReader.readLine()) != null){
				bw.append(line);
				bw.newLine();
			}
			bw.close();
			latestVersionReader.close();
			return true;
		}
		catch (Exception ex){
			ex.printStackTrace();
			createExceptionLog(ex);
			progress = "Failed to create new version file";
			fail = "Errors occurred; see log file for details";
			launcher.paintImmediately(0, 0, width, height);
		}
		return false;
	}

	private void launch(){
		File nativeDir = new File(appData(), FOLDER_NAME);
		if (!downloadDir.isEmpty())
			nativeDir = new File(downloadDir, FOLDER_NAME);
		nativeDir = new File(nativeDir, "bin");
		File main = new File(nativeDir, JAR_NAME);
		nativeDir = new File(nativeDir, "native");
		String os = "";
		if (System.getProperty("os.name").toUpperCase().contains("WIN"))
			os = "windows";
		else if (System.getProperty("os.name").toUpperCase().contains("MAC"))
			os = "macosx";
		else
			os = "linux";
		nativeDir = new File(nativeDir, os);

		progress = "Launching";
		paintImmediately(0, 0, width, height);
		try {
			InputStream errStream = Runtime.getRuntime().exec(
					new String[]{"java", "-Djava.library.path=" +
							nativeDir, "-jar", main.getPath()}, null,
							new File(appData(), FOLDER_NAME + File.separator + "bin")).getErrorStream();
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

	private void downloadMain(File main) throws MalformedURLException{
		if (updateAvailable)
			main.delete();
		Downloader dl = new Downloader(new URL(
				JAR_LOCATION),
				main.getPath(), JAR_NAME);
		eSize = getFileSize(new URL(JAR_LOCATION));
		aSize = 0;
		Thread t = new Thread(dl);
		t.start();
		while (t.isAlive()){
			aSize = (int)main.length();
			if (lastTime != -1){
				if (System.currentTimeMillis() - lastTime >= SPEED_UPDATE_INTERVAL){
					speed = ((aSize - lastSize) / 1000) / ((System.currentTimeMillis() - lastTime) /
							1000);
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
		lastTime = -1;
		lastSize = 0;
		speed = 0;
		aSize = -1;
		eSize = -1;
		createVersionFile();
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
		 * past 7. It's stupid, it really is.
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
			File versionFile = new File(appData(), FOLDER_NAME);
			if (!downloadDir.isEmpty())
				versionFile = new File(downloadDir, FOLDER_NAME);
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
}
