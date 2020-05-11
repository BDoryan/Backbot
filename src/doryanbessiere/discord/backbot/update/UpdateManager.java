package doryanbessiere.discord.backbot.update;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class UpdateManager {

	public static boolean updateGame(VersionType versionType) {
		TextChannel channel = Backbot.getDiscordbot().getTextChannel();
		GithubAPI githubAPI = new GithubAPI(Backbot.getConfig().getProperty("github.username"),
				Backbot.getConfig().getProperty("github.token"));
		try {
			DownloadInfo download_info = new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```Téléchargement des sources en cours...```").queue();
				}

				@Override
				public void finish() {
					channel.sendMessage("```Téléchargement des sources terminé```").queue();
				}

				@Override
				public void download() {
				}
			};
			githubAPI.download(Backbot.updateDirectory(), "BackdoorGame", versionType.getName(), download_info);
			channel.sendMessage("```Décompression des sources...```").queue();
			File unzip_directory = githubAPI.unzip(download_info.getFile());
			channel.sendMessage("```Décompression des sources terminé```").queue();

			MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
			File mavenlogs_file = new File(GithubAPI.localDirectory(), "maven-logs.log");
			if (mavenlogs_file.exists()) {
				mavenlogs_file.delete();
			}
			mavenlogs_file.createNewFile();
			FileWriter mavenlogs_writer = new FileWriter(mavenlogs_file);

			String[] arguments = Backbot.getConfig().getProperty("mavenbuildapi.game.arguments").split(",");

			try {
				File latest_directory = new File(
						Backbot.getConfig().getProperty("update.latest." + versionType.getName()));
				File target_directory = new File(unzip_directory, "target");

				String version = null;

				try {
					MavenXpp3Reader reader = new MavenXpp3Reader();
					Model model = reader.read(new FileInputStream(new File(unzip_directory, "pom.xml")));
					version = model.getVersion();

					if (build.build(new MavenLogs() {
						@Override
						public void log(String log) {
							try {
								mavenlogs_writer.write(log + "\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void error(String log) {
							try {
								mavenlogs_writer.write(log + "\n");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}, arguments)) {
						channel.sendMessage("```Examination des fichiers modifiés !```").queue();
						
						ArrayList<String> update_logs = new ArrayList<>();
						
						File files_update_file = new File(latest_directory, "files.update");
						FilesUpdate filesUpdate = new FilesUpdate(files_update_file);
						if (files_update_file.exists()) {
							filesUpdate.read();

							List<String> last_version_files = search(latest_directory, latest_directory);
							List<String> new_version_files = search(target_directory, target_directory);

							for (String file : last_version_files) {
								if (!new_version_files.contains(file)) {
									filesUpdate.removeFile(file);
									update_logs.add(file+" à été supprimé.");
								} else {
									if (!FileUtils.contentEquals(new File(latest_directory, file),
											new File(target_directory, file))) {
										filesUpdate.setFile(file, version);
										update_logs.add(file+" à été mis à jour.");
									}
								}
							}

							for (String file : new_version_files) {
								if (!last_version_files.contains(file)) {
									filesUpdate.addFile(file, version);
									update_logs.add(file+" à été ajouté.");
								}
							}

							filesUpdate.save();
						} else {
							files_update_file.createNewFile();
							
							List<String> new_version_files = search(unzip_directory, unzip_directory);
							for (String file : new_version_files) {
								filesUpdate.addFile(file, version);
								update_logs.add(file+" à été implémenter.");
							}
							filesUpdate.save();
						}
						
						EmbedBuilder update_log_embed = new EmbedBuilder();
						update_log_embed.setColor(new Color(0x353535));
						update_log_embed.setTitle(version, null);
						update_log_embed.setDescription(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
								.format(System.currentTimeMillis()));
						update_log_embed.addBlankField(false);
						
						String content = "";
						for (String log : update_logs) {
								content += content == "" ? log : "\n" + log;
						}
						update_log_embed.addField("Changement de la mise à jour", content, false);
						
						for (Entry<String, String> entries : filesUpdate.getFiles().entrySet()) {
							update_log_embed.addField(entries.getKey(), entries.getValue(), false);
						}
						update_log_embed.setFooter("Backdoor, produit par IsotopeStudio",
								"https://cdn.discordapp.com/attachments/489417878861512704/700847654359269508/Logo_Backdoor.png");
						channel.sendMessage(update_log_embed.build()).queue();
					} else {
						return false;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				mavenlogs_writer.close();
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
		List<String> files = Arrays.asList();
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				String path = file.getName();
				path = path.substring(path.indexOf(parent.getPath() + (parent.getPath().endsWith("/") ? "" : "/")),
						path.length());
				files.add(path);
			} else if (file.isDirectory()) {
				files.addAll(search(parent, file));
			}
		}
		return files;
	}
}
