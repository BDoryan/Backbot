package doryanbessiere.discord.backbot.discord.command.commands;

import doryanbessiere.discord.backbot.Backbot;
import doryanbessiere.discord.backbot.discord.command.ICommand;
import net.dv8tion.jda.api.entities.TextChannel;

public class HelpCommand implements ICommand {

	@Override
	public String getCommand() {
		return "help";
	}
	
	@Override
	public void handle(String[] args) {
		TextChannel channel = Backbot.getDiscordbot().getTextChannel();
		channel.sendMessage("Liste des commandes:").queue();
		for(ICommand command : ICommand.commands) {
			channel.sendMessage(" ").queue();
			channel.sendMessage("  - "+command.getCommand()).queue();
			channel.sendMessage("  > "+command.getDescription()).queue();
		}
		channel.sendMessage(" ").queue();
	}

	@Override
	public String getDescription() {
		return "Vous donne de l'aide :D";
	}
}
