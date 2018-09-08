package logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class Logger {

	// This allows for a given class to log many things at once, all in different files.
	HashMap<String, File> csvFiles;
	public Logger() {
		csvFiles = new HashMap<String, File>();
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			@Override
//			public void run() {
//				closeWriters();
//				// TODO: Clean up the other stuff here.
//			}
//		});
	}
	
	/**
	 * Adds a file that we can log to.
	 * @param filename - Name of the file, including path
	 * @param createNew Create a new file if one of this name already exists.
	 */
	public void addFile(String filename, boolean createNew) {
		File f = new File(filename);
		if (f.isFile() && createNew) {
			f.delete();
//			try {
//				f.createNewFile();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
		csvFiles.put(filename, f);
	}
	
	public void addLineToFile(StringBuilder line, String filename) {
		try {
			File f = csvFiles.get(filename);
			FileWriter fw = new FileWriter(f, true);
			PrintWriter pw = new PrintWriter(fw);
			pw.println(line.toString());
			pw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
