package mineflat;

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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 
 * @author Maxim Roncac�
 * 
 * THIS SOFTWARE IS LICENSED UNDER THE GNU GENERAL PUBLIC LICENSE VERSION 3, AND AS SUCH,
 * ALL DERIVATIVES MUST BE RELEASED UNDER THE SAME LICENSE. THE FULL LICENSE TEXT MAY BE
 * VIEWED IN THE FILE ENTITLED "LICENSE" IN THE ROOT DIRECTORY OF THIS JAR. THIS FILE MUST
 * BE INCLUDED IN ALL DERIVATIVES OF THIS SOFTWARE.
 *
 */

public class Launcher extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	public static JFrame f;

	protected JButton play, force, quit, updateYes, updateNo;

	private int btnWidth = 200;
	private int btnHeight = 50;
	private static int width = 800;
	private static int height = 500;
	private boolean update = false;
	boolean updateAvailable = false;
	public static String progress = null;
	public static String fail = null;
	public static String updateMsg = null;

	Font font = new Font("Verdana", Font.BOLD, 30);
	Font smallFont = new Font("Verdana", Font.BOLD, 16);

	public Launcher(){
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
	}

	public void actionPerformed(ActionEvent e){
		if (e.getActionCommand().equals("play")){
			this.remove(play);
			this.remove(force);
			this.remove(quit);
			File dir = new File(appData(), "MineFlat");
			dir.mkdir();
			File bin = new File (dir, "bin");
			bin.mkdir();
			File main = new File(bin, "mineflat.jar");
			File lwjgl = new File(bin, "lwjgl.jar");
			File lwjgl_util = new File(bin, "lwjgl_util.jar");
			File jinput = new File(bin, "jinput.jar");
			String os = "";
			if (System.getProperty("os.name").toUpperCase().contains("WIN"))
				os = "windows";
			else if (System.getProperty("os.name").toUpperCase().contains("MAC"))
				os = "macosx";
			else
				os = "linux";
			File nativeDir = new File(bin, "native");
			nativeDir = new File(nativeDir, os);
			if (!lwjgl.exists() || !lwjgl_util.exists() || !jinput.exists() || !nativeDir.exists() || update){
				File lwjglZip = new File(bin, "lwjgl.zip");
				if (!lwjglZip.exists() || update){
					progress = "Downloading LWJGL";
					paintImmediately(0, 0, width, height);
					try {
						Downloader dl = new Downloader(new URL(
								"http://downloads.sourceforge.net/project/java-game-lib/Official%20Releases/LWJGL%202.8.5/lwjgl-2.8.5.zip"),
								lwjglZip.getPath());
						Thread t = new Thread(dl);
						t.start();
						t.join();
					}
					catch (Exception ex){
						ex.printStackTrace();
						progress = "Failed to download LWJGL";
						fail = "Errors occurred; see console for details";
						repaint();
					}
				}
				if (!new File(bin, "native").exists())
					new File(bin, "native").mkdir();
				if (!nativeDir.exists())
					nativeDir.mkdir();
				try {
					ZipFile zip = new ZipFile(new File(bin, "lwjgl.zip"));
					@SuppressWarnings("rawtypes")
					Enumeration en = zip.entries();
					while (en.hasMoreElements()){
						ZipEntry entry = (ZipEntry)en.nextElement();
						if (entry.getName().equals("lwjgl-2.8.5/jar/lwjgl.jar") && (!lwjgl.exists() || update)){
							progress = "Extracting LWJGL";
							paintImmediately(0, 0, width, height);
							unzip(zip, entry, lwjgl);
						}
						else if (entry.getName().equals("lwjgl-2.8.5/jar/lwjgl_util.jar") && (!lwjgl_util.exists() || update)){
							progress = "Extracting LWJGL Util";
							paintImmediately(0, 0, width, height);
							unzip(zip, entry, lwjgl_util);
						}
						else if (entry.getName().equals("lwjgl-2.8.5/jar/jinput.jar") && (!jinput.exists() || update)){
							progress = "Extracting JInput";
							paintImmediately(0, 0, width, height);
							unzip(zip, entry, jinput);
						}
						else if (entry.getName().startsWith("lwjgl-2.8.5/native/" + os) && !entry.isDirectory()){
							progress = "Extracting natives";
							paintImmediately(0, 0, width, height);
							unzip(zip, entry, new File(nativeDir, entry.getName().replace("lwjgl-2.8.5/native/" + os, "")));
						}
					}
					zip.close();
					lwjglZip.delete();
				}
				catch (Exception ex){
					ex.printStackTrace();
					progress = "Failed to extract LWJGL";
					fail = "Errors occurred; see console for details";
					repaint();
				}
			}

			File slick = new File(bin, "slick.jar");
			if (!slick.exists() || update){
				progress = "Downloading Slick";
				paintImmediately(0, 0, width, height);
				try {
					slick.createNewFile();
					Downloader dl = new Downloader(new URL("http://www.amigocraft.net/slick/slick.jar"), slick.getPath());
					Thread t = new Thread(dl);
					t.start();
					t.join();
				}
				catch (Exception ex){
					ex.printStackTrace();
					progress = "Failed to download Slick";
					fail = "Errors occurred; see console for details";
					repaint();
				}
			}

			if (!main.exists() || update){
				progress = "Downloading mineflat.jar";
				paintImmediately(0, 0, width, height);
				try {
					createVersionFile();
					Downloader dl = new Downloader(new URL(
							"http://www.amigocraft.net/mineflat/mineflat.jar"),
							main.getPath());
					Thread t = new Thread(dl);
					t.start();
					t.join();
				}
				catch (Exception ex){
					ex.printStackTrace();
					progress = "Failed to download mineflat.jar";
					fail = "Errors occurred; see console for details";
					repaint();
				}
			}
			
			File versionFile = new File(appData(), "MineFlat");
			versionFile = new File(versionFile, "version");
			try {
				if (versionFile.exists()){
					BufferedReader currentVersionReader = new BufferedReader(new InputStreamReader(new FileInputStream(versionFile)));
					BufferedReader latestVersionReader = new BufferedReader(new InputStreamReader(new URL("http://www.amigocraft.net/mineflat/version").openStream()));
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

					if (!currentStage.equals(latestStage) || versionDifference){
						updateAvailable = true;
						updateMsg = "Would you like to update from version " + currentVersion + " " + currentStage + " to version " + latestVersion + " " + latestStage + "?";
					}

				}
				else {
					updateAvailable = true;
					updateMsg = "No version file detected! Press \"Update\" to automatically begin an update.";
				}
			}
			catch (Exception ex){
				ex.printStackTrace();
				progress = "Failed to get latest version";
				fail = "Errors occurred; see console for details";
				repaint();
			}

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

			File nativeDir = new File(appData(), "MineFlat");
			nativeDir = new File(nativeDir, "bin");
			File main = new File(nativeDir, "mineflat.jar");
			nativeDir = new File(nativeDir, "natives");
			String os = "";
			if (System.getProperty("os.name").toUpperCase().contains("WIN"))
				os = "windows";
			else if (System.getProperty("os.name").toUpperCase().contains("MAC"))
				os = "macosx";
			else
				os = "linux";
			nativeDir = new File(nativeDir, os);

			progress = "Downloading mineflat.jar";
			paintImmediately(0, 0, width, height);
			try {
				Downloader dl = new Downloader(new URL(
						"http://www.amigocraft.net/mineflat/mineflat.jar"),
						main.getPath());
				Thread t = new Thread(dl);
				t.start();
				t.join();

				createVersionFile();

				launch();
			}
			catch (Exception ex){
				ex.printStackTrace();
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

	private String getOutputStream(Process p){

		StringBuffer cmd_out = new StringBuffer("");
		if (p != null){
			BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String buf = "";
			try {
				while ((buf = is.readLine()) != null){
					cmd_out.append(buf);
					cmd_out.append (System.getProperty("line.separator"));
				}
				is.close();
			}
			catch (Exception ex){
				ex.printStackTrace();
			}
		} 
		return cmd_out.toString();
	}

	private String getErrorStream(Process p){

		StringBuffer cmd_out = new StringBuffer("");
		if (p != null){
			BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String buf = "";
			try {
				is = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				while ((buf = is.readLine()) != null){
					cmd_out.append(buf);
					cmd_out.append("\n");
				}
				is.close();
			}
			catch (Exception ex){
				ex.printStackTrace();
			}
		} 
		return cmd_out.toString();
	}

	private static void createAndShowGUI() {

		f = new JFrame("MineFlat Launcher");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Launcher l = new Launcher();
		l.setOpaque(true);

		f.setContentPane(l);
		f.pack();
		f.setVisible(true);
		f.setSize(width, height);
		f.setResizable(false);
		try {
			f.setIconImage(ImageIO.read(Launcher.class.getResourceAsStream("/images/icon.png")));
		}
		catch (Exception ex){}
	}

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
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
			g.drawString("MineFlat Launcher", centerText(g, "MineFlat Launcher"), 50);

		else {
			g.drawString(progress, centerText(g, progress), height / 2);
			if (fail != null)
				g.drawString(fail, centerText(g, fail), height / 2 + 50);
		}
	}

	private int centerText(Graphics g, String text){
		int stringLen = (int)
				g.getFontMetrics().getStringBounds(text, g).getWidth();
		return this.getWidth() / 2 - stringLen / 2;
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
		else if (OS.contains("NUX"))
			return System.getProperty("user.home");
		return System.getProperty("user.dir");
	}

	public static void slow (int n){
		long t0, t1;
		t0 = System.currentTimeMillis();
		do {
			t1 = System.currentTimeMillis();
		}
		while (t1 - t0 < n);
	}

	public void unzip(ZipFile zip, ZipEntry entry, File dest){
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
		}
	}

	private boolean createVersionFile(){
		try {
			File versionFile = new File(appData(), "MineFlat");
			versionFile = new File(versionFile, "version");
			if (versionFile.exists())
				versionFile.delete();
			versionFile.createNewFile();
			BufferedReader latestVersionReader = new BufferedReader(new InputStreamReader(new URL("http://www.amigocraft.net/mineflat/version").openStream()));
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(versionFile)));
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
		}
		return false;
	}

	private void launch(){
		File nativeDir = new File(appData(), "MineFlat");
		nativeDir = new File(nativeDir, "bin");
		File main = new File(nativeDir, "mineflat.jar");
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
			Process p = Runtime.getRuntime().exec(new String[]{"java", "-Djava.library.path=\"" + nativeDir + "\"", "-jar", main.getPath()});
			System.out.println(getOutputStream(p));
			System.err.println(getErrorStream(p));
			p.waitFor();
			pullThePlug();
		}
		catch (Exception ex){
			ex.printStackTrace();
			progress = "Failed to launch game";
			fail = "Errors occurred; see console for details";
		}
	}
}