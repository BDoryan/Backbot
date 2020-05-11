package doryanbessiere.discord.backbot.discord.command.commands;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;

import doryanbessiere.discord.backbot.Backbot;
import doryanbessiere.discord.backbot.discord.command.ICommand;
import net.dv8tion.jda.core.EmbedBuilder;
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
			channel.sendMessage("Mise à jour terminé!").queue();
		} else {
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
