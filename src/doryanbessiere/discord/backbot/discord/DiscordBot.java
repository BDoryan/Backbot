package doryanbessiere.discord.backbot.discord;

import javax.security.auth.login.LoginException;

import doryanbessiere.discord.backbot.discord.command.ICommand;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;

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
		
		jda = new JDABuilder(AccountType.BOT).setToken(token).buildBlocking();
		jda.addEventListener(new EventListener() {
			@Override
			public void onEvent(Event event) {
				if (event instanceof MessageReceivedEvent) {
					MessageReceivedEvent e = (MessageReceivedEvent) event;
					if (e.getAuthor().equals((jda.getSelfUser())))
						return;
					if (e.getTextChannel().getIdLong() == channel) {
						String message = e.getMessage().getContent();
						if(!message.startsWith("!"))return;
						if(!ICommand.command(message)) {
							e.getTextChannel().sendMessage("Cette commande est inconnue!").complete();
						}
					}
				}
			}
		});
	}
	
	public void sendMessage(String message) {
		getTextChannel().sendMessage(message).complete();
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
