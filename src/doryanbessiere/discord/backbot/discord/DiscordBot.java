package doryanbessiere.discord.backbot.discord;

import javax.security.auth.login.LoginException;

import doryanbessiere.discord.backbot.discord.command.ICommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class DiscordBot {

	private long channel;
	private JDA jda;

	public DiscordBot(String token, long channel)
			throws LoginException, IllegalArgumentException, RateLimitedException, InterruptedException {
		this.channel = channel;

		jda = JDABuilder.createDefault(token).build();
		jda.awaitReady();
		System.out.println("JDA ready");
		jda.addEventListener(new ListenerAdapter() {
			@Override
			public void onMessageReceived(MessageReceivedEvent e) {
				if (e.getAuthor().equals((jda.getSelfUser())))
					return;
				if (e.getTextChannel().getIdLong() == channel) {
					String message = e.getMessage().getContentRaw();
					if(!message.startsWith("!"))return;
					if(!ICommand.command(message)) {
						e.getTextChannel().sendMessage("Cette commande est inconnue!").complete();
					}
				}
			}
		});
	}

	public void sendMessage(String message) {
		MessageAction action = getTextChannel().sendMessage(message);
		action.complete();
	}

	/**
	 * @return the channel
	 */
	public long getChannel() {
		return channel;
	}

	/**
	 * @return
	 */
	public JDA getJDA() {
		return jda;
	}

	/**
	 * @return
	 */
	public TextChannel getTextChannel() {
		return jda.getTextChannelById(channel);
	}
}
