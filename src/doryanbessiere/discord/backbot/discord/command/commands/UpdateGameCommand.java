package doryanbessiere.discord.backbot.discord.command.commands;

import doryanbessiere.discord.backbot.Backbot;
import doryanbessiere.discord.backbot.discord.command.ICommand;
import doryanbessiere.discord.backbot.update.UpdateManager;
import doryanbessiere.discord.backbot.version.VersionType;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class UpdateGameCommand implements ICommand {

	@Override
	public void handle(String[] args) {
		TextChannel channel = Backbot.getDiscordbot().getTextChannel();
		if(args.length == 0) {
			channel.sendMessage("Lancement de la mise à jour en cours...").queue();
			if(UpdateManager.updateGame(VersionType.RELEASE)){
				channel.sendMessage("Mise à jour terminé!").queue();	
			} else {
				channel.sendMessage("La mise à jour à échoué!").queue();
			}
		} else if(args.length == 1){
			VersionType versionType = VersionType.from(args[0]);
			if(versionType == null) {
				System.err.println("Cette version est inconnue!");
				return;
			}
			if(UpdateManager.updateGame(VersionType.RELEASE)){
				channel.sendMessage("Mise à jour terminé!").queue();	
			} else {
				channel.sendMessage("La mise à jour à échoué!").queue();
			}
		}
	}

	@Override
	public String getCommand() {
		return "update";
	}

	@Override
	public String getDescription() {
		return "Vous permet de mettre en ligne une nouvelle mise à jour du jeu!";
	}

}
