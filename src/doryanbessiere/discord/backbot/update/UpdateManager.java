package doryanbessiere.discord.backbot.update;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

import doryan.mbd.builder.MavenBuilderAPI;
import doryan.mbd.builder.MavenLogs;
import doryan.mbd.download.DownloadInfo;
import doryan.mbd.github.GithubAPI;
import doryanbessiere.discord.backbot.Backbot;
import doryanbessiere.discord.backbot.version.VersionType;
import doryanbessiere.isotopestudio.api.updater.FileFilesUpdate;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class UpdateManager {

	public static boolean updateGame(VersionType versionType) {
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
						FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/datas"),
								game_content_directory);
						FileUtils.copyFileToDirectory(new File(unzip_directory, "target/changelogs.log"),
								game_content_directory);

						channel.sendMessage("```[INFO] Reviewing Modified Files !```").queue();

						ArrayList<String> update_logs = new ArrayList<>();

						File files_update_file = new File(latest_directory.getParent(), "files.update");
						FileFilesUpdate fileFilesUpdate = null;
						
						if (files_update_file.exists()) {
							System.out.println(files_update_file.getPath()+"="+files_update_file.length());
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
						FileUtils.copyDirectoryToDirectory(new File(game_content_directory, "datas"), latest_directory);
						FileUtils.copyFileToDirectory(new File(game_content_directory, "changelogs.log"),
								latest_directory);
						
						File changelogs_file = new File(game_content_directory, "changelogs.log");
						HashMap<String, ArrayList<String>> logs = null;
						if (changelogs_file.length() != 0) {
							if (changelogs_file.exists()) {
								logs = new HashMap<String, ArrayList<String>>();
								BufferedReader input = new BufferedReader(new InputStreamReader(
										new FileInputStream(changelogs_file), Charset.forName("UTF-8")));
								String log = null;
								String target_title = null;
								while ((log = input.readLine()) != null) {
									if (log.endsWith(":")) {
										target_title = log;
										logs.put(target_title, new ArrayList<String>());
									} else {
										if (target_title != null) {
											logs.get(target_title).add(log);
										}
									}
								}
								input.close();
							} else {
								channel.sendMessage("```" + changelogs_file.getParent() + " not found```").queue();
							}
						}

						if (logs != null) {
							TextChannel changelogs_channel = Backbot.getDiscordbot().getJDA()
									.getTextChannelById(662833381280710689L);

							EmbedBuilder eb = new EmbedBuilder();
							eb.setColor(new Color(0x353535));
							eb.setThumbnail(
									"https://media.discordapp.net/attachments/699234758374457364/699274451048726588/Isotope_logo_wb.png");
							eb.setTitle(version, null);
							eb.setDescription(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
									.format(System.currentTimeMillis()));
							eb.addBlankField(false);
							for (Entry<String, ArrayList<String>> entries : logs.entrySet()) {
								String title = entries.getKey();
								String content = "";
								for (String log : entries.getValue()) {
									content += content == "" ? log : "\n" + log;
								}
								eb.addField(title, content, false);
								eb.addBlankField(false);
							}
							eb.setFooter("Backdoor, produit par IsotopeStudio",
									"https://cdn.discordapp.com/attachments/489417878861512704/700847654359269508/Logo_Backdoor.png");

							changelogs_channel.sendMessage("@everyone").queue();
							changelogs_channel.sendMessage(eb.build()).queue();
						}

						Backbot.getDatabase().setString(versionType.getTableSQL(), "name", "backdoor", "version",
								version);
						update_logs_writer.close();
						channel.sendFile(update_logs_file,
								new MessageBuilder().append("```" + update_logs_file.getName() + "```").build())
								.queue();

						return true;
					} else {
						channel.sendMessage("```[ERROR] Build failed!```").queue();
						update_logs_writer.close();
						channel.sendFile(update_logs_file,
								new MessageBuilder().append("```" + update_logs_file.getName() + "```").build())
								.queue();
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
