package doryanbessiere.discord.backbot;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import javax.security.auth.login.LoginException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import doryan.mbd.builder.MavenBuilderAPI;
import doryan.mbd.builder.MavenLogs;
import doryan.mbd.download.DownloadInfo;
import doryan.mbd.github.GithubAPI;
import doryanbessiere.isotopestudio.api.mysql.SQL;
import doryanbessiere.isotopestudio.api.mysql.SQLDatabase;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;

public class Main {

	public static void main(String[] args) {
		SQLDatabase sql_database = new SQLDatabase(SQL.DEFAULT_SQL_DRIVER, SQL.DEFAULT_URLBASE, "localhost",
				"isotopestudio", "", "");
		if (!sql_database.connect()) {
			System.err.println("Database connection failed!");
		}

		try {
			JDA jda = new JDABuilder(AccountType.BOT)
					.setToken("").buildAsync();
			jda.addEventListener(new EventListener() {
				@Override
				public void onEvent(Event event) {
					TextChannel logs_channel = jda.getTextChannelById(700817282569601045L);
					if (event instanceof MessageReceivedEvent) {
						MessageReceivedEvent e = (MessageReceivedEvent) event;
						if (e.getAuthor().equals((jda.getSelfUser())))
							return;
						if (e.getTextChannel().getIdLong() == 700821824040337472L) {
							String[] args = e.getMessage().getContent().split(" ");
							if (e.getMessage().getContent().equalsIgnoreCase("!help")) {
								e.getTextChannel()
										.sendMessage("Liste des commandes disponible\n" + "```"
												+ "- !deletecache - pour supprimer la dernière version construite\n"
												+ "- !product <snapshot/release> - pour construire le jeu et le serveur\n"
												+ "- !buildmatchmaking - pour construire le serveur matchmaking\n"
												+ "- !productserver  <snapshot/release> - pour construire le serveur\n"
												+ "- !productgame <snapshot/release> - pour construire le jeu\n" + "```" + "\n"
												+ "Cette fonctionnalité à été développé par Doryan Bessiere\n"
												+ "https://github.com/BDoryan/")
										.complete();
							} else if (args[0].equalsIgnoreCase("!deletecache")) {
								clearCache();
								e.getTextChannel().sendMessage("Le dossier cache à été vidé :D").complete();
							} else if (args[0].equalsIgnoreCase("!buildmatchmaking")) {
								clearCache();
								e.getTextChannel()
								.sendMessage(new MessageBuilder()
										.append("Construction du serveur matchmaking").build())
								.complete();
								buildMatchmaking(jda, sql_database, logs_channel);
								e.getTextChannel()
								.sendMessage(new MessageBuilder()
										.append("Construction terminée!").build())
								.complete();
							} else if (args[0].equalsIgnoreCase("!product")) {
								clearCache();
								clear(logs_channel);
								if(args.length != 2) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Commande invalide!").build())
											.complete();
									return;
								}

								if(args[1].equalsIgnoreCase("snapshot")) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Construction du jeu et du server en cours [snapshot version]").build())
											.complete();
									buildSnapshot(jda, sql_database, logs_channel);
									e.getTextChannel()
									.sendMessage(new MessageBuilder()
											.append("Construction terminée!").build())
									.complete();
								} else if (args[1].equalsIgnoreCase("release")) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Construction du jeu et du server en cours [release version]").build())
											.complete();
									buildRelease(jda, sql_database, logs_channel);	
									e.getTextChannel()
									.sendMessage(new MessageBuilder()
											.append("Construction terminée!").build())
									.complete();
								}
							} else if (args[0].equalsIgnoreCase("!productserver")) {
								clearCache();
								clear(logs_channel);

								if(args.length != 2) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Commande invalide!").build())
											.complete();
									return;
								}
								
								if(args[1].equalsIgnoreCase("snapshot")) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Construction du server en cours [snapshot version]").build())
											.complete();
									buildSnapshotServer(jda, sql_database, logs_channel);
									e.getTextChannel()
									.sendMessage(new MessageBuilder()
											.append("Construction terminée!").build())
									.complete();
								} else if (args[1].equalsIgnoreCase("release")) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Construction du server en cours [release version]").build())
											.complete();
									buildReleaseServer(jda, sql_database, logs_channel);	
									e.getTextChannel()
									.sendMessage(new MessageBuilder()
											.append("Construction terminée!").build())
									.complete();
								}
							} else if (args[0].equalsIgnoreCase("!productgame")) {
								clearCache();
								clear(logs_channel);
								if(args.length != 2) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Commande invalide!").build())
											.complete();
									return;
								}
								
								if(args[1].equalsIgnoreCase("snapshot")) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Construction du jeu en cours [snapshot version]").build())
											.complete();
									buildSnapshotGame(jda, sql_database, logs_channel);
									e.getTextChannel()
									.sendMessage(new MessageBuilder()
											.append("Construction terminée!").build())
									.complete();
								} else if (args[1].equalsIgnoreCase("release")) {
									e.getTextChannel()
											.sendMessage(new MessageBuilder()
													.append("Construction du jeu en cours [release version]").build())
											.complete();
									buildReleaseGame(jda, sql_database, logs_channel);	
									e.getTextChannel()
									.sendMessage(new MessageBuilder()
											.append("Construction terminée!").build())
									.complete();
								}
							} else {
								e.getTextChannel().sendMessage("Cette commande n'existe pas, !help").complete();
							}
						}
					}
				}
			});
			System.out.println("Discord bot connected :D");
		} catch (LoginException e1) {
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (RateLimitedException e1) {
			e1.printStackTrace();
		}
	}

	private static void buildRelease(JDA jda, SQLDatabase sql_database, TextChannel channel) {
		clear(channel);
		buildReleaseServer(jda, sql_database, channel);
		buildReleaseGame(jda, sql_database, channel);
	}

	private static void buildSnapshot(JDA jda, SQLDatabase sql_database, TextChannel channel) {
		clear(channel);
		buildSnapshotServer(jda, sql_database, channel);
		buildSnapshotGame(jda, sql_database, channel);
	}

	private static void buildReleaseServer(JDA jda, SQLDatabase sql_database, TextChannel channel) {
		GithubAPI githubAPI = new GithubAPI("BDoryan", "");
		try {
			githubAPI.download(GithubAPI.getCacheDirectory(), "BackdoorServer", new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```[INFO] Downloading the Server release project...```").queue();
				}

				@Override
				public void finish() {
					channel.sendMessage("```[INFO] Downloading finish```").queue();
					channel.sendMessage("```[INFO] Decompressing the project archive...```").queue();
					try {
						File unzip_directory = githubAPI.unzip(getFile());
						channel.sendMessage("```[INFO] Decompressing finish```").queue();

						channel.sendMessage("```[INFO] Building projects...```").queue();
						MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
						try {
							File mavenlogs_file = new File(GithubAPI.localDirectory(), "maven-logs.log");
							if (mavenlogs_file.exists()) {
								mavenlogs_file.delete();
							}
							mavenlogs_file.createNewFile();
							FileWriter mavenlogs_writer = new FileWriter(mavenlogs_file);
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
							}, "-s", "/home/backdoor/settings.xml", "install", "-U")) {
								channel.sendMessage("```[INFO] Build success :D```").queue();

								File latest_directory = new File("/home/resources/release");

								if (latest_directory.exists()) {
									FileUtils.deleteDirectory(latest_directory);
								}
								latest_directory.mkdirs();

								FileUtils.copyFileToDirectory(new File(unzip_directory, "target/server.jar"),
										latest_directory);
							} else {
								channel.sendMessage("```[ERROR] Build failed!```").queue();
							}
							mavenlogs_writer.close();
							channel.sendFile(mavenlogs_file,
									new MessageBuilder().append("```" + mavenlogs_file.getName() + "```").build())
									.queue();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void download() {
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void buildSnapshotServer(JDA jda, SQLDatabase sql_database, TextChannel channel) {
		GithubAPI githubAPI = new GithubAPI("BDoryan", "");
		try {
			githubAPI.download(GithubAPI.getCacheDirectory(), "BackdoorServer", "snapshot", new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```[INFO] Downloading the Server snapshot project...```").queue();
				}

				@Override
				public void finish() {
					channel.sendMessage("```[INFO] Downloading finish```").queue();
					channel.sendMessage("```[INFO] Decompressing the project archive...```").queue();
					try {
						File unzip_directory = githubAPI.unzip(getFile());
						channel.sendMessage("```[INFO] Decompressing finish```").queue();

						channel.sendMessage("```[INFO] Building projects...```").queue();
						MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
						try {
							File mavenlogs_file = new File(GithubAPI.localDirectory(), "maven-logs.log");
							if (mavenlogs_file.exists()) {
								mavenlogs_file.delete();
							}
							mavenlogs_file.createNewFile();
							FileWriter mavenlogs_writer = new FileWriter(mavenlogs_file);
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
							}, "-s", "/home/backdoor/settings.xml", "install", "-U")) {
								channel.sendMessage("```[INFO] Build success :D```").queue();

								File latest_directory = new File("/home/resources/snapshot");

								if (latest_directory.exists()) {
									FileUtils.deleteDirectory(latest_directory);
								}
								latest_directory.mkdirs();

								FileUtils.copyFileToDirectory(new File(unzip_directory, "target/server.jar"),
										latest_directory);
							} else {
								channel.sendMessage("```[ERROR] Build failed!```").queue();
							}
							mavenlogs_writer.close();
							channel.sendFile(mavenlogs_file,
									new MessageBuilder().append("```" + mavenlogs_file.getName() + "```").build())
									.queue();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void download() {
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static void buildMatchmaking(JDA jda, SQLDatabase sql_database, TextChannel channel) {
		GithubAPI githubAPI = new GithubAPI("BDoryan", "");
		try {
			githubAPI.download(GithubAPI.getCacheDirectory(), "BackdoorMatchmaking", new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```[INFO] Downloading the Matchmaking Server project...```").queue();
				}

				@Override
				public void finish() {
					channel.sendMessage("```[INFO] Downloading finish```").queue();
					channel.sendMessage("```[INFO] Decompressing the project archive...```").queue();
					try {
						File unzip_directory = githubAPI.unzip(getFile());
						channel.sendMessage("```[INFO] Decompressing finish```").queue();

						channel.sendMessage("```[INFO] Building projects...```").queue();
						MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
						try {
							File mavenlogs_file = new File(GithubAPI.localDirectory(), "maven-logs.log");
							if (mavenlogs_file.exists()) {
								mavenlogs_file.delete();
							}
							mavenlogs_file.createNewFile();
							FileWriter mavenlogs_writer = new FileWriter(mavenlogs_file);
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
							}, "-s", "/home/backdoor/settings.xml", "install", "-U")) {
								channel.sendMessage("```[INFO] Build success :D```").queue();

								File latest_directory = new File("/home/resources/matchmaking");

								if (latest_directory.exists()) {
									FileUtils.deleteDirectory(latest_directory);
								}
								latest_directory.mkdirs();

								FileUtils.copyFileToDirectory(new File(unzip_directory, "target/matchmaking.jar"),
										latest_directory);
							} else {
								channel.sendMessage("```[ERROR] Build failed!```").queue();
							}
							mavenlogs_writer.close();
							channel.sendFile(mavenlogs_file,
									new MessageBuilder().append("```" + mavenlogs_file.getName() + "```").build())
									.queue();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void download() {
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void buildReleaseGame(JDA jda, SQLDatabase sql_database, TextChannel channel) {
		GithubAPI githubAPI = new GithubAPI("BDoryan", "");
		try {
			githubAPI.download(GithubAPI.getCacheDirectory(), "BackdoorGame", new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```[INFO] Downloading the Game release project...```").queue();
				} 

				@Override
				public void finish() {
					channel.sendMessage("```[INFO] Downloading finish```").queue();
					channel.sendMessage("```[INFO] Decompressing the project archive...```").queue();
					try {
						File unzip_directory = githubAPI.unzip(getFile());
						channel.sendMessage("```[INFO] Decompressing finish```").queue();

						channel.sendMessage("```[INFO] Building projects...```").queue();
						MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
						try {
							File mavenlogs_file = new File(GithubAPI.localDirectory(), "maven-logs.log");
							if (mavenlogs_file.exists()) {
								mavenlogs_file.delete();
							}
							mavenlogs_file.createNewFile();
							FileWriter mavenlogs_writer = new FileWriter(mavenlogs_file);
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
							}, "-Plwjgl-natives-linux-amd64", "-Plwjgl-natives-linux-amd64",
									"-Plwjgl-natives-linux-aarch64", "-Plwjgl-natives-linux-arm",
									"-Plwjgl-natives-linux-arm32", "-Plwjgl-natives-macos-amd64",
									"-Plwjgl-natives-windows-x86", "-Plwjgl-natives-windows-amd64", "package",
									"compile", "-U", "-s",  "/home/backdoor/settings.xml")) {
								channel.sendMessage("```[INFO] Build success :D```").queue();

								File latest_directory = new File("/var/www/html/games/backdoor/release/latest/");

								if (latest_directory.exists()) {
									FileUtils.deleteDirectory(latest_directory);
								}
								latest_directory.mkdirs();

								FileUtils.copyFileToDirectory(new File(unzip_directory, "target/backdoor.jar"),
										latest_directory);
								FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/datapacks"),
										latest_directory);
								FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/langs"),
										latest_directory);
								FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/datas"),
										latest_directory);
								FileUtils.copyFileToDirectory(new File(unzip_directory, "target/changelogs.log"),
										latest_directory);

								File pom_file = new File(unzip_directory, "pom.xml");
								File changelogs_file = new File(unzip_directory, "target/changelogs.log");
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
										channel.sendMessage("```" + changelogs_file.getParent() + " not found```")
												.queue();
									}
								}

								MavenXpp3Reader reader = new MavenXpp3Reader();
								Model model;
								try {
									model = reader.read(new FileInputStream(pom_file));

									String GAME_VERSION = model.getVersion();
									channel.sendMessage("```[INFO] BackdoorGame version -> " + GAME_VERSION + "```")
											.queue();

									if (logs != null) {
										TextChannel changelogs_channel = jda.getTextChannelById(662833381280710689L);

										EmbedBuilder eb = new EmbedBuilder();
										eb.setColor(new Color(0x353535));
										eb.setThumbnail(
												"https://media.discordapp.net/attachments/699234758374457364/699274451048726588/Isotope_logo_wb.png");
										eb.setTitle(GAME_VERSION, null);
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

									try {
										sql_database.setString("releases", "name", "backdoor", "version", GAME_VERSION);
										sql_database.disconnect();
										channel.sendMessage(
												"```[INFO] The production of the game is done successfully, the game has a new update ("
														+ GAME_VERSION + ") :D```")
												.queue();
									} catch (SQLException e) {
										e.printStackTrace();
									}
								} catch (XmlPullParserException e) {
									e.printStackTrace();
								}
							} else {
								channel.sendMessage("```[ERROR] Build failed!```").queue();
							}
							mavenlogs_writer.close();
							channel.sendFile(mavenlogs_file,
									new MessageBuilder().append("```" + mavenlogs_file.getName() + "```").build())
									.queue();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void download() {
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void buildSnapshotGame(JDA jda, SQLDatabase sql_database, TextChannel channel) {
		GithubAPI githubAPI = new GithubAPI("BDoryan", "");
		try {
			githubAPI.download(GithubAPI.getCacheDirectory(), "BackdoorGame", "snapshot", new DownloadInfo() {
				@Override
				public void start() {
					channel.sendMessage("```[INFO] Downloading the Game snapshot project...```").queue();
				} 

				@Override
				public void finish() {
					channel.sendMessage("```[INFO] Downloading finish```").queue();
					channel.sendMessage("```[INFO] Decompressing the project archive...```").queue();
					try {
						File unzip_directory = githubAPI.unzip(getFile());
						channel.sendMessage("```[INFO] Decompressing finish```").queue();

						channel.sendMessage("```[INFO] Building projects...```").queue();
						MavenBuilderAPI build = new MavenBuilderAPI(unzip_directory);
						try {
							File mavenlogs_file = new File(GithubAPI.localDirectory(), "maven-logs.log");
							if (mavenlogs_file.exists()) {
								mavenlogs_file.delete();
							}
							mavenlogs_file.createNewFile();
							FileWriter mavenlogs_writer = new FileWriter(mavenlogs_file);
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
							}, "-Plwjgl-natives-linux-amd64", "-Plwjgl-natives-linux-amd64",
									"-Plwjgl-natives-linux-aarch64", "-Plwjgl-natives-linux-arm",
									"-Plwjgl-natives-linux-arm32", "-Plwjgl-natives-macos-amd64",
									"-Plwjgl-natives-windows-x86", "-Plwjgl-natives-windows-amd64", "package",
									"compile", "-U", "-s", "/home/backdoor/settings.xml")) {
								channel.sendMessage("```[INFO] Build success :D```").queue();

								File latest_directory = new File("/var/www/html/games/backdoor/snapshot/latest/");

								if (latest_directory.exists()) {
									FileUtils.deleteDirectory(latest_directory);
								}
								latest_directory.mkdirs();

								FileUtils.copyFileToDirectory(new File(unzip_directory, "target/backdoor.jar"),
										latest_directory);
								FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/datapacks"),
										latest_directory);
								FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/langs"),
										latest_directory);
								FileUtils.copyDirectoryToDirectory(new File(unzip_directory, "target/datas"),
										latest_directory);
								FileUtils.copyFileToDirectory(new File(unzip_directory, "target/changelogs.log"),
										latest_directory);

								File pom_file = new File(unzip_directory, "pom.xml");
								File changelogs_file = new File(unzip_directory, "target/changelogs.log");
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
										channel.sendMessage("```" + changelogs_file.getParent() + " not found```")
												.queue();
									}
								}

								MavenXpp3Reader reader = new MavenXpp3Reader();
								Model model;
								try {
									model = reader.read(new FileInputStream(pom_file));

									String GAME_VERSION = model.getVersion();
									channel.sendMessage("```[INFO] BackdoorGame version -> " + GAME_VERSION + "```")
											.queue();

									if (logs != null) {
										TextChannel changelogs_channel = jda.getTextChannelById(662833381280710689L);

										EmbedBuilder eb = new EmbedBuilder();
										eb.setColor(new Color(0x353535));
										eb.setThumbnail(
												"https://media.discordapp.net/attachments/699234758374457364/699274451048726588/Isotope_logo_wb.png");
										eb.setTitle(GAME_VERSION, null);
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

									try {
										sql_database.setString("snapshots", "name", "backdoor", "version", GAME_VERSION);
										sql_database.disconnect();
										channel.sendMessage(
												"```[INFO] The production of the game is done successfully, the game has a new update ("
														+ GAME_VERSION + ") :D```")
												.queue();
									} catch (SQLException e) {
										e.printStackTrace();
									}
								} catch (XmlPullParserException e) {
									e.printStackTrace();
								}
							} else {
								channel.sendMessage("```[ERROR] Build failed!```").queue();
							}
							mavenlogs_writer.close();
							channel.sendFile(mavenlogs_file,
									new MessageBuilder().append("```" + mavenlogs_file.getName() + "```").build())
									.queue();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void download() {
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean isWorking = false;

	public static void clear(TextChannel channel) {
		if (isWorking) {
			return;
		}

		isWorking = true;

		OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minus(2, ChronoUnit.WEEKS);

		new Thread(() -> {
			while (isWorking) {
				List<Message> messages = channel.getHistory().retrievePast(50).complete();

				messages.removeIf(m -> m.getCreationTime().isBefore(twoWeeksAgo));

				if (messages.isEmpty()) {
					isWorking = false;
					return;
				}

				channel.deleteMessages(messages).complete();
			}
		}).run();
	}

	private static void clearCache() {
		if (GithubAPI.getCacheDirectory().exists()) {
			try {
				FileUtils.deleteDirectory(GithubAPI.getCacheDirectory());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		GithubAPI.getCacheDirectory().mkdirs();
	}
}
