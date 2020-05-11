package doryanbessiere.discord.backbot.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import doryanbessiere.isotopestudio.commons.GsonInstance;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class FilesUpdate {
	private HashMap<String, String> files = new HashMap<>();

	private File file;

	public FilesUpdate(File file) {
		this.file = file;
	}

	public boolean save() {
		try {
			if (file.exists()) {
				if(file.delete()) {
					return false;
				}
			}
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			file.createNewFile();
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(GsonInstance.instance().toJson(files));
			fileWriter.flush();
			fileWriter.close();
			
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public boolean read() {
		String json = null;
		String line = "";
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while((line = reader.readLine())!= null) {
				json += line;
			}
			reader.close();
			
			files = GsonInstance.instance().fromJson(json, HashMap.class);
			return true;
		} catch (IOException e) {
		}
		return false;
	}
	
	public void addFile(String path, String version) {
		files.put(path, version);
	}
	
	public void setFile(String path, String version) {
		if(files.containsKey(path))
			files.remove(path);
		files.put(path, version);
	}
	
	public void removeFile(String path) {
		files.remove(path);
	}

	public void clear() {
		files.clear();
	}
	
	public HashMap<String, String> getFiles() {
		return files;
	}
}
