package doryanbessiere.discord.backbot.discord.command.commands;

import doryanbessiere.discord.backbot.Backbot;
import doryanbessiere.discord.backbot.discord.command.ICommand;
import doryanbessiere.discord.backbot.update.UpdateManager;
import doryanbessiere.discord.backbot.version.VersionType;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class UpdateGameCommand implements ICommand {

	@Override
	public void handle(String[] args) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				TextChannel channel = Backbot.getDiscordbot().getTextChannel();
				if (args.length == 0) {
					channel.sendMessage("Lancement de la mise à jour du jeu (" + VersionType.RELEASE + ", par défaut) en cours...")
							.queue();
					if (UpdateManager.updateGame(VersionType.RELEASE, true)) {
						channel.sendMessage("Mise à jour terminé!").queue();
					} else {
						channel.sendMessage("La mise à jour à échoué!").queue();
					}
					channel.sendMessage("Lancement de la mise à jour du serveur (" + VersionType.RELEASE + ", par défaut) en cours...")
							.queue();
					if (UpdateManager.updateServer(VersionType.RELEASE, true)) {
						channel.sendMessage("Mise à jour terminé!").queue();
					} else {
						channel.sendMessage("La mise à jour à échoué!").queue();
					}
				} else if (args.length == 1) {
					VersionType versionType = VersionType.from(args[0]);
					if (versionType == null) {
						System.err.println("Cette version est inconnue!");
						return;
					}
					channel.sendMessage("Lancement de la mise à jour du jeu (" + versionType.getName() + ") en cours...")
							.queue();
					if (UpdateManager.updateGame(versionType, true)) {
						channel.sendMessage("Mise à jour terminé!").queue();
					} else {
						channel.sendMessage("La mise à jour à échoué!").queue();
					}
					channel.sendMessage("Lancement de la mise à jour du serveur (" + versionType.getName() + ") en cours...")
							.queue();
					if (UpdateManager.updateServer(versionType, true)) {
						channel.sendMessage("Mise à jour terminé!").queue();
					} else {
						channel.sendMessage("La mise à jour à échoué!").queue();
					}
				} else if (args.length == 2) {
					VersionType versionType = VersionType.from(args[0]);
					if (versionType == null) {
						System.err.println("Cette version est inconnue!");
						return;
					}

					Boolean enable_changelogs_message = args[1].equalsIgnoreCase("true") ? true : false;

					channel.sendMessage("Lancement de la mise à jour du jeu (" + versionType.getName() + ") en cours...").queue();
					if (UpdateManager.updateGame(versionType, enable_changelogs_message)) {
						channel.sendMessage("Mise à jour terminé!").queue();
					} else {
						channel.sendMessage("La mise à jour à échoué!").queue();
					}

					channel.sendMessage("Lancement de la mise à jour du serveur (" + versionType.getName() + ") en cours...").queue();
					if (UpdateManager.updateServer(versionType, enable_changelogs_message)) {
						channel.sendMessage("Mise à jour terminé!").queue();
					} else {
						channel.sendMessage("La mise à jour à échoué!").queue();
					}
				}				
			}
		}).start();
	}

	@Override
	public String getCommand() {
		return "updateall";
	}

	@Override
	public String getDescription() {
		return "Vous permet de mettre en ligne une nouvelle mise à jour du jeu!";
	}

}
