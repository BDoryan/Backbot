package doryanbessiere.discord.backbot.discord.command.commands;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import doryanbessiere.discord.backbot.Backbot;
import doryanbessiere.discord.backbot.discord.DiscordBot;
import doryanbessiere.discord.backbot.discord.command.ICommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class ClearCommand implements ICommand {

	@Override
	public void handle(String[] args) {
		DiscordBot discordbot = Backbot.getDiscordbot();	
		clear(discordbot.getJDA().getTextChannelById(discordbot.getChannel()));
	}

	private boolean isWorking = false;

	public void clear(TextChannel channel) {
		if (isWorking) {
			return;
		}

		isWorking = true;

		OffsetDateTime twoWeeksAgo = OffsetDateTime.now().minus(4, ChronoUnit.WEEKS);

		new Thread(() -> {
			while (isWorking) {
				List<Message> messages = channel.getHistory().retrievePast(50).complete();
				if(messages.size() == 0)break;

				messages.removeIf(m -> m.getTimeCreated().isBefore(twoWeeksAgo));

				if (messages.isEmpty()) {
					isWorking = false;
					return;
				}

				channel.deleteMessages(messages).queue();
			}
		}).run();
	}

	@Override
	public String getCommand() {
		return "clear";
	}

	@Override
	public String getDescription() {
		return "Vous permet de nettoyer le channel";
	}
}
