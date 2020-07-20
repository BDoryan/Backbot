package doryanbessiere.discord.backbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import doryanbessiere.discord.backbot.discord.DiscordBot;
import doryanbessiere.isotopestudio.api.IsotopeStudioAPI;
import doryanbessiere.isotopestudio.api.mysql.SQL;
import doryanbessiere.isotopestudio.api.mysql.SQLDatabase;
import doryanbessiere.isotopestudio.api.mysql.SQLDatabase;
import doryanbessiere.isotopestudio.commons.LocalDirectory;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

/**
 * @author BESSIERE Doryan
 * @github https://www.github.com/DoryanBessiere/
 */
public class Backbot {
	
	private static Properties config = new Properties();
	private static DiscordBot discordbot;
	private static SQLDatabase isotopestudio;
	private static SQLDatabase backdoor;
	
	public static void main(String[] args) {
		File config_file = new File(localDirectory(), "config.properties");
		if(config_file.exists()) {
			try {
				FileInputStream inputstream = new FileInputStream(config_file);
				config.load(inputstream);
				inputstream.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(config_file.getPath()+" cannot be read!");
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			}
			
			try {
				discordbot = new DiscordBot(config.getProperty("discordbot.token"), Long.valueOf(config.getProperty("discordbot.channel")));
				System.out.println("BackBot login success :D");
				discordbot.sendMessage("*Heyy je suis connecté(e) bg ^^*");
			} catch (NumberFormatException e) {
				e.printStackTrace();
				System.err.println("discordbot.channel are not a long value !");
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (LoginException e) {
				e.printStackTrace();
				System.err.println("BackBot login failed!");
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (RateLimitedException e) {
				e.printStackTrace();
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(IsotopeStudioAPI.EXIT_CODE_CRASH);
				return;
			}

			isotopestudio = new SQLDatabase(SQL.DEFAULT_SQL_DRIVER, SQL.DEFAULT_URLBASE, config.getProperty("mysql.url"),
					config.getProperty("mysql.isotopestudio.database"),config.getProperty("mysql.username"),config.getProperty("mysql.password"));
			backdoor = new SQLDatabase(SQL.DEFAULT_SQL_DRIVER, SQL.DEFAULT_URLBASE, config.getProperty("mysql.url"),
					config.getProperty("mysql.backdoor.database"),config.getProperty("mysql.username"),config.getProperty("mysql.password"));

			if(isotopestudio.connect() && backdoor.connect()) {
				discordbot.sendMessage("*Ohhh ouii maître, je suis connecté aux bases de données :sunglasses:*");
			} else {
				discordbot.sendMessage("*FUCKKKKKKKKK, j'arrive pas à me connecté aux bases de données! :middle_finger:*");
			}
		} else {
			System.err.println("BackdoorMBD cannot be started, "+config_file.getPath()+" not found!");
		}
	}

	/**
	 * @return the config
	 */
	public static Properties getConfig() {
		return config;
	}
	
	/**
	 * @return the discordbot
	 */
	public static DiscordBot getDiscordbot() {
		return discordbot;
	}
	
	/**
	 * @return the isotopestudio database
	 */
	public static SQLDatabase getIsotopeStudioDatabase() {
		return isotopestudio;
	}
	
	/**
	 * @return the backdoor database
	 */
	public static SQLDatabase getBackdoorDatabase() {
		return backdoor;
	}
	
	/**
	 * @return the backdoor update directory
	 */
	public static File updateDirectory() {
		return new File(localDirectory(), "backdoor-update");
	}

	public static File localDirectory() {
		return LocalDirectory.toFile(Backbot.class);
	}
}
