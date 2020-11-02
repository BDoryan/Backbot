package doryanbessiere.discord.backbot.update;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import doryan.mbd.builder.MavenBuilderAPI;
import doryan.mbd.builder.MavenLogs;
import doryan.mbd.download.DownloadInfo;
import doryan.mbd.github.GithubAPI;
import doryanbessiere.discord.backbot.Backbot;
import doryanbessiere.discord.backbot.version.VersionType;
import doryanbessiere.isotopestudio.api.changelogs.ChangeLogsObject;
import doryanbessiere.isotopestudio.api.updater.FileFilesUpdate;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class UpdateManager {

	public static boolean updateGame(VersionType versionType, boolean enable_changelogs_message) {
		TextChannel channel = Backbot.getDiscordbot().getTextChannel();
		if (GithubAPI.getCacheDirectory().exists())
			try {
				FileUtils.deleteDirectory(GithubAPI.getCacheDirectory());
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		GithubAPI githubAPI = new GithubAPI(Backbot.getConfig().getProperty("github.username"),
				Backbot.getConfig().getProperty("github.token"));
		try {
			DownloadInfo download_info = new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```[INFO] Downloading source code...```").queue();
				}

				@Override
				public void finish() {
					channel.sendMessage("```[INFO] Downloading source code finish.```").queue();
				}

				@Override
				public void download() {
				}
			};
			githubAPI.download(GithubAPI.getCacheDirectory(), "BackdoorGame",
					versionType == VersionType.RELEASE ? "master" : versionType.getName(), download_info);
			channel.sendMessage("```[INFO] Decompressing the project archive...```").queue();
			File unzip_directory = githubAPI.unzip(download_info.getFile());
			channel.sendMessage("```[INFO] Decompressing finish```").queue();

			MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
			File update_logs_file = new File(GithubAPI.localDirectory(), "update.logs");
			if (update_logs_file.exists()) {
				update_logs_file.delete();
			}
			update_logs_file.createNewFile();
			FileWriter update_logs_writer = new FileWriter(update_logs_file);

			String[] arguments = Backbot.getConfig().getProperty("mavenbuildapi.game.arguments").split(",");

			try {
				String version = null;

				try {
					MavenXpp3Reader reader = new MavenXpp3Reader();
					Model model = reader.read(new FileInputStream(new File(unzip_directory, "pom.xml")));
					version = model.getVersion();

					channel.sendMessage("```[INFO] Building projects...```").queue();
					if (build.build(new MavenLogs() {
						@Override
						public void log(String log) {
							try {
								update_logs_writer.write(log + "\n");
								System.out.println("[Maven] " + log);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void error(String log) {
							try {
								update_logs_writer.write(log + "\n");
								System.err.println("[Maven] " + log);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}, arguments)) {
						File latest_directory = new File(
								Backbot.getConfig().getProperty("update.latest." + versionType.getName()));

						channel.sendMessage("```[INFO] Build success !```").queue();

						channel.sendMessage("```[INFO] Recovery of current game files !```").queue();

						File game_content_directory = new File(Backbot.localDirectory(), "game_content");
						if (game_content_directory.exists()) {
							FileUtils.deleteDirectory(game_content_directory);
						}
						game_content_directory.mkdirs();

						FileUtils.copyFileToDirectory(new File(unzip_directory, "target/backdoor.jar"),
								game_content_directory);
						FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/datapacks"),
								game_content_directory);
						FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/langs"),
								game_content_directory);
						FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/resources"),
								game_content_directory);
						FileUtils.copyFileToDirectory(new File(unzip_directory, "target/changelogs.log"),
								game_content_directory);
						FileUtils.copyFileToDirectory(new File(unzip_directory, "target/game.properties"),
								game_content_directory);

						channel.sendMessage("```[INFO] Reviewing Modified Files !```").queue();

						ArrayList<String> update_logs = new ArrayList<>();

						File files_update_file = new File(latest_directory.getParent(), "files.update");
						FileFilesUpdate fileFilesUpdate = null;

						if (files_update_file.exists()) {
							System.out.println(files_update_file.getPath() + "=" + files_update_file.length());
							fileFilesUpdate = new FileFilesUpdate(files_update_file);

							if (!fileFilesUpdate.read()) {
								channel.sendMessage("```[ERROR] files.update cannot be read!```").queue();
								return false;
							}

							List<String> last_version_files = search(latest_directory, latest_directory);
							List<String> new_version_files = search(game_content_directory, game_content_directory);

							for (String file : last_version_files) {
								if (!new_version_files.contains(file)) {
									fileFilesUpdate.removeFile(file);
									update_logs.add(file + " has been removed.");
								} else {
									if (!FileUtils.contentEquals(new File(latest_directory, file),
											new File(game_content_directory, file))) {
										fileFilesUpdate.setFile(file, version);
										update_logs.add(file + " has been changed.");
									}
								}
							}

							for (String file : new_version_files) {
								if (!last_version_files.contains(file)) {
									fileFilesUpdate.addFile(file, version);
									update_logs.add(file + " has been added.");
								}
							}

							if (files_update_file.exists())
								files_update_file.delete();
							files_update_file.createNewFile();

							if (!fileFilesUpdate.save()) {
								channel.sendMessage("```[ERROR] files.update cannot be saved!```").queue();
								return false;
							}
						} else {
							fileFilesUpdate = new FileFilesUpdate(files_update_file);

							List<String> new_version_files = search(game_content_directory, game_content_directory);
							for (String file : new_version_files) {
								fileFilesUpdate.addFile(file, version);
								update_logs.add(file + " has been implemented!");
							}

							if (!fileFilesUpdate.save()) {
								channel.sendMessage("```[ERROR] files.update cannot be saved!```").queue();
								return false;
							}
						}

						update_logs_writer.write("\n");
						update_logs_writer.write("## Files update ##\n");
						for (Entry<String, String> entries : fileFilesUpdate.getFiles().entrySet()) {
							update_logs_writer.write(entries.getKey() + "=" + entries.getValue() + "\n");
						}
						update_logs_writer.write("\n");
						update_logs_writer.write("## Update Logs ##\n");

						if (!updateLibrairies(latest_directory, channel, unzip_directory, game_content_directory,
								update_logs, version))
							return false;
						
						for (String log : update_logs) {
							update_logs_writer.write(log + "\n");
						}

						channel.sendMessage("```[INFO] Uploading the update...!```").queue();

						if (latest_directory.exists()) {
							FileUtils.deleteDirectory(latest_directory);
						}
						latest_directory.mkdirs();

						FileUtils.copyFileToDirectory(new File(game_content_directory, "backdoor.jar"),
								latest_directory);
						FileUtils.copyDirectoryToDirectory(new File(game_content_directory, "datapacks"),
								latest_directory);
						FileUtils.copyDirectoryToDirectory(new File(game_content_directory, "langs"), latest_directory);
						FileUtils.copyDirectoryToDirectory(new File(game_content_directory, "resources"),
								latest_directory);
						FileUtils.copyFileToDirectory(new File(game_content_directory, "changelogs.log"),
								latest_directory);
						FileUtils.copyFileToDirectory(new File(game_content_directory, "game.properties"),
								latest_directory);
						
						File changelogs_file = new File(game_content_directory, "changelogs.log");

						if (changelogs_file.exists() && enable_changelogs_message) {
							changelogs(changelogs_file, version);
						}

						Backbot.getIsotopeStudioDatabase().setString(versionType.getTableSQL(), "name", "backdoor",
								"version", version);

						update_logs_writer.close();
						channel.sendFile(update_logs_file, "```" + update_logs_file.getName() + "```").queue();

						return true;
					} else {
						channel.sendMessage("```[ERROR] Build failed!```").queue();
						update_logs_writer.close();
						channel.sendFile(update_logs_file, "```" + update_logs_file.getName() + "```").queue();
						return false;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				update_logs_writer.close();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static boolean updateLibrairies(File latest_directory, TextChannel channel, File unzip_directory,
			File game_content_directory, ArrayList<String> update_logs, String version) throws IOException {
		
		FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/libs"), game_content_directory);
		/*
		 * windows: 
		 * {x64}: 
		 * - [librairies] (peut contenir des librairies de x86) 
		 * {x86}: -
		 * [librairies]
		 * 
		 * macsos: - [librairies]
		 * 
		 * linux: {x64}: - [librairies] {x86}: - [librairies] {arm64}: - [librairies]
		 * {armhf}: - [librairies] {ppc64le}: - [librairies]
		 * 
		 */

		File game_content_libs_directory = new File(game_content_directory, "libs");

		HashMap<String, String[]> systems = new HashMap<>();
		systems.put("windows", new String[] { "64", "86" });
		systems.put("linux", new String[] { "64", "86", "arm64", "armhf", "ppc64le" });
		systems.put("macos", new String[] { "64", "86" });

		for (Entry<String, String[]> system : systems.entrySet()) {
			File parent = new File(game_content_directory, "librairies/"+system.getKey());
			if (!parent.exists())
				parent.mkdirs();
			
			for (String children : system.getValue()) {
				update_logs.add("");
				update_logs.add("## "+system.getKey()+"/"+children+" Update Logs ##");
				
				channel.sendMessage("`Updating operating system librairies : " + system.getKey() + " ["
						+ children + "]`").queue();
				File children_directory = new File(parent, children);
				if (!children_directory.exists())
					children_directory.mkdirs();

				File librairies_destination = new File(children_directory, "latest");

				if (!librairies_destination.exists())
					librairies_destination.mkdirs();

				for (File library : game_content_libs_directory.listFiles()) {
					String library_name = library.getName();
					if (library_name.contains("android") || library_name.contains("ios"))
						continue;

					if (library_name.contains(system.getKey())) {
						boolean contains_type = false;
						for (String types : system.getValue()) {
							if (library_name.contains(types)) {
								contains_type = true;
								break;
							}
						}

						if (!contains_type)
							FileUtils.copyFileToDirectory(library, librairies_destination);

						if (library_name.contains(children))
							FileUtils.copyFileToDirectory(library, librairies_destination);
					}
					
					boolean contains_os = false;
					for (String types : systems.keySet()) {
						if (library_name.contains(types)) {
							contains_os = true;
							break;
						}
					}
					
					if(!contains_os)
						FileUtils.copyFileToDirectory(library, librairies_destination);
				}

				File latest_librairies_directory = new File(latest_directory.getParent()+"/librairies/"+system.getKey(), children);
				latest_librairies_directory.mkdirs();

				File files_update_file = new File(latest_librairies_directory, "files.update");
				FileFilesUpdate fileFilesUpdate = null;

				if (files_update_file.exists()) {
					System.out.println(files_update_file.getPath() + "=" + files_update_file.length());
					fileFilesUpdate = new FileFilesUpdate(files_update_file);

					if (!fileFilesUpdate.read()) {
						channel.sendMessage("```[ERROR] files.update cannot be read!```").queue();
						return false;
					}

					List<String> last_version_files = search(new File(latest_librairies_directory, "latest"), new File(latest_librairies_directory, "latest"));
					List<String> new_version_files = search(librairies_destination, librairies_destination);

					for (String file : last_version_files) {
						if (!new_version_files.contains(file)) {
							fileFilesUpdate.removeFile(file);
							update_logs.add(file + " has been removed.");
						} else {
							/*
							 * Je n'utilise pas cette fonctionnalité car elle ne fonctionne pas pour les fichiers '.jar'
							 * De plus les fichiers concerner change de nom lors d'une mise à jour donc cela ne sert à rien
							 * de verifier des fichiers qui existerons plus par la suite.
							 */
							/*
							if (!FileUtils.contentEquals(new File(latest_librairies_directory, file),
									new File(librairies_destination, file))) {
								fileFilesUpdate.setFile(file, version);
								update_logs.add(file + " has been changed.");
							}
							*/
						}
					}

					for (String file : new_version_files) {
						if (!last_version_files.contains(file)) {
							fileFilesUpdate.addFile(file, version);
							update_logs.add(file + " has been added.");
						}
					}

					if (files_update_file.exists())
						files_update_file.delete();

					files_update_file.createNewFile();

					if (!fileFilesUpdate.save()) {
						channel.sendMessage("```[ERROR] files.update cannot be saved!```").queue();
						return false;
					}
				} else {
					fileFilesUpdate = new FileFilesUpdate(files_update_file);

					List<String> new_version_files = search(librairies_destination, librairies_destination);
					for (String file : new_version_files) {
						fileFilesUpdate.addFile(file, version);
						update_logs.add(file + " has been implemented!");
					}

					if (!fileFilesUpdate.save()) {
						channel.sendMessage("```[ERROR] files.update cannot be saved!```").queue();
						return false;
					}
				}
				
				FileUtils.copyDirectoryToDirectory(librairies_destination,
						latest_librairies_directory);
				
				for(File file : new File(latest_librairies_directory, "latest").listFiles()) {
					System.out.println("Checking: " +file.getPath());
					if(!fileFilesUpdate.getFiles().containsKey(file.getPath().substring(file.getPath().lastIndexOf("/")))){
						if(!file.delete()) {
							System.out.println(file.getPath()+" cannot be deleted!");
						} else {
							System.out.println(file.getPath()+" deleted!");
						}
					}
				}
			}
		}

		return true;
	}

	private static void changelogs(File changelogs_file, String version)
			throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		try {
			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();
			ChangeLogsObject changelogs = gson.fromJson(
					new InputStreamReader(new FileInputStream(changelogs_file), Charset.forName("UTF-8")),
					ChangeLogsObject.class);
			changelogs.setDate(new Date(System.currentTimeMillis()));
			changelogs.setVersion(version);

			String json = changelogs.toJson().replace("\"", "\\" + "\"").replace("'", "\\" + "'");
			Backbot.getBackdoorDatabase().create("changelogs", "json", json);

			TextChannel changelogs_channel = Backbot.getDiscordbot().getJDA().getTextChannelById(662833381280710689L);

			EmbedBuilder eb = new EmbedBuilder();
			eb.setColor(new Color(0x353535));
			eb.setThumbnail(
					"https://media.discordapp.net/attachments/699234758374457364/699274451048726588/Isotope_logo_wb.png");

			eb.setTitle(changelogs.getTitle(), null);
			eb.setDescription(changelogs.getSubtitle());
			eb.addField(
					changelogs.getVersion() + " - "
							+ new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(changelogs.getDate()),
					"", false);
			eb.addBlankField(false);
			for (Entry<String, ArrayList<String>> entries : changelogs.getContents().entrySet()) {
				String title = entries.getKey();
				String content = "";
				for (String log : entries.getValue()) {
					log = "  - " + log;
					content += content == "" ? log : "\n" + log;
				}
				eb.addField(title, content, false);
				eb.addBlankField(false);
			}
			eb.setFooter("Backdoor, produit par IsotopeStudio",
					"https://cdn.discordapp.com/attachments/489417878861512704/700847654359269508/Logo_Backdoor.png");

			changelogs_channel.sendMessage("@everyone").queue();
			changelogs_channel.sendMessage(eb.build()).queue();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static boolean updateServer(VersionType versionType, boolean enable_changelogs_message) {
		TextChannel channel = Backbot.getDiscordbot().getTextChannel();
		if (GithubAPI.getCacheDirectory().exists())
			try {
				FileUtils.deleteDirectory(GithubAPI.getCacheDirectory());
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		GithubAPI githubAPI = new GithubAPI(Backbot.getConfig().getProperty("github.username"),
				Backbot.getConfig().getProperty("github.token"));
		try {
			DownloadInfo download_info = new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```[INFO] Downloading source code...```").queue();
				}

				@Override
				public void finish() {
					channel.sendMessage("```[INFO] Downloading source code finish.```").queue();
				}

				@Override
				public void download() {
				}
			};
			githubAPI.download(GithubAPI.getCacheDirectory(), "BackdoorServer",
					versionType == VersionType.RELEASE ? "master" : versionType.getName(), download_info);
			channel.sendMessage("```[INFO] Decompressing the project archive...```").queue();
			File unzip_directory = githubAPI.unzip(download_info.getFile());
			channel.sendMessage("```[INFO] Decompressing finish```").queue();

			MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
			File update_logs_file = new File(GithubAPI.localDirectory(), "update.logs");
			if (update_logs_file.exists()) {
				update_logs_file.delete();
			}
			update_logs_file.createNewFile();
			FileWriter update_logs_writer = new FileWriter(update_logs_file);

			String[] arguments = Backbot.getConfig().getProperty("mavenbuildapi.server.arguments").split(",");

			StringBuilder arguments_string = new StringBuilder();
			for (String arg : arguments) {
				arguments_string.append(arg + " ");
			}

			try {
				String version = null;

				try {
					MavenXpp3Reader reader = new MavenXpp3Reader();
					Model model = reader.read(new FileInputStream(new File(unzip_directory, "pom.xml")));
					version = model.getVersion();

					channel.sendMessage("```[INFO] Building projects...```").queue();
					if (build.build(new MavenLogs() {
						@Override
						public void log(String log) {
							try {
								update_logs_writer.write(log + "\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void error(String log) {
							try {
								update_logs_writer.write(log + "\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}, arguments)) {
						File resources_directory = new File(Backbot.getConfig().getProperty("resources.directory"));

						channel.sendMessage("```[INFO] Build success !```").queue();

						channel.sendMessage("```[INFO] Recovery of current game files !```").queue();

						FileUtils.copyFileToDirectory(new File(unzip_directory, "target/server.jar"),
								new File(resources_directory, (versionType.getName() + "s") + "/"));

						channel.sendFile(update_logs_file, "```" + update_logs_file.getName() + "```").queue();

						return true;
					} else {
						channel.sendMessage("```[ERROR] Build failed!```").queue();
						update_logs_writer.close();
						channel.sendFile(update_logs_file, "```" + update_logs_file.getName() + "```").queue();
						return false;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				update_logs_writer.close();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static List<String> search(File parent, File directory) {
		List<String> files = new ArrayList<>();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				String path = file.getPath();
				path = path.replace(parent.getPath(), "");
				files.add(path);
			} else if (file.isDirectory()) {
				files.addAll(search(parent, file));
			}
		}
		return files;
	}
}
