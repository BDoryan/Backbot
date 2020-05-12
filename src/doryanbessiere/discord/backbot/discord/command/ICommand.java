package doryanbessiere.discord.backbot.discord.command;

import java.util.Scanner;

import doryanbessiere.discord.backbot.discord.command.commands.ClearCommand;
import doryanbessiere.discord.backbot.discord.command.commands.HelpCommand;
import doryanbessiere.discord.backbot.discord.command.commands.UpdateGameCommand;

public interface ICommand {

	public static ICommand[] commands = new ICommand[] { 
		new ClearCommand(),
		new HelpCommand(),
		new UpdateGameCommand()
	};

	public abstract void handle(String[] args);

	public abstract String getCommand();

	public abstract String getDescription();

	public static boolean command(String line) {
		String[] args = line.split(" ");
		String target = args[0].substring(1);

		for (ICommand command : commands) {
			if (command.getCommand().equalsIgnoreCase(target)) {
				String[] arguments = new String[args.length - 1];
				if (args.length > 1) {
					for (int i = 1; i < args.length; i++) {
						arguments[i - 1] = args[i];
					}
				}
				command.handle(arguments);
				return true;
			}
		}
		return false;
	}
}
