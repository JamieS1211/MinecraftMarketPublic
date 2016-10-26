package com.minecraftmarket.minecraftmarket;

import com.google.inject.Inject;
import com.minecraftmarket.minecraftmarket.command.CommandTask;
import com.minecraftmarket.minecraftmarket.mcommands.Commands;
import com.minecraftmarket.minecraftmarket.recentgui.RecentListener;
import com.minecraftmarket.minecraftmarket.shop.ShopListener;
import com.minecraftmarket.minecraftmarket.shop.ShopTask;
import com.minecraftmarket.minecraftmarket.signs.SignListener;
import com.minecraftmarket.minecraftmarket.signs.SignUpdate;
import com.minecraftmarket.minecraftmarket.signs.Signs;
import com.minecraftmarket.minecraftmarket.util.Chat;
import com.minecraftmarket.minecraftmarket.util.Log;
import com.minecraftmarket.minecraftmarket.util.Settings;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;

import java.io.File;

@Plugin(name = "MinecraftMarket", id = "minecraftmarket", version = Market.version)
public class Market {
	
	private Long interval;

	public Long getInterval() {
		return this.interval;
	}

	public void setInterval(Long interval) {
		this.interval = interval;
	}

	private String shopCommand;

	public String getShopCommand() {
		return this.shopCommand;
	}

	public void setShopCommand(String shopCommand) {
		this.shopCommand = shopCommand;
	}

	private boolean update;

	private boolean isBoardEnabled;

	public boolean getIsBoardEnabled() {
		return this.isBoardEnabled;
	}

	public void setIsBoardEnabled(boolean isBoardEnabled) {
		this.isBoardEnabled = isBoardEnabled;
	}


	private boolean isSignEnabled;

	public boolean getIsSignEnabled() {
		return this.isSignEnabled;
	}

	public void setIsSignEnabled(boolean isSignEnabled) {
		this.isSignEnabled = isSignEnabled;
	}


	private boolean isGuiEnabled;

	public boolean getIsGuiEnabled() {
		return this.isGuiEnabled;
	}

	public void setIsGuiEnabled(boolean isGuiEnabled) {
		this.isGuiEnabled = isGuiEnabled;
	}


	private static Market plugin;

	public static Market getPlugin() {
		return plugin;
	}

	public void setPlugin(Market plugin) {
		this.plugin = plugin;
	}


	private CommandTask commandTask;

	public CommandTask getCommandTask() {
		return this.commandTask;
	}

	public void setCommandTask(CommandTask commandTask) {
		this.commandTask = commandTask;
	}


	private SignUpdate signUpdate;

	public SignUpdate getSignUpdate() {
		return this.signUpdate;
	}

	public void setSignUpdate(SignUpdate signUpdate) {
		this.signUpdate = signUpdate;
	}


    private String color;

	public String getString() {
		return this.color;
	}

	public void setColor(String color) {
		this.color = color;
	}


	@Inject
	public Game game;

	public Game getGame() {
		return this.game;
	}

	@Inject
	private org.slf4j.Logger logger;

	public org.slf4j.Logger getLogger() {
		return this.logger;
	}


	public final static String version = "2.1.0";

	public static String getVersion() {
		return version;
	}

	@Listener
	public void onServerStop(GameStoppingServerEvent event) {
		stopTasks();
	}

	@Listener
	public void onServerInit(GameInitializationEvent event) {
        plugin = this;
		try {
			registerCommands();
			saveDefaultSettings();
			registerEvents();
			reload();
			startTasks();
		} catch (Exception e) {
			e.printStackTrace();
			Log.log(e);
		}
	}

	public void reload() {
		try {
            Settings.get().reloadMainConfig();
            Settings.get().reloadLanguageConfig();
            
			loadConfigOptions();
			if (authApi()) {
				startGUI();
				startSignTasks();
			}
		} catch (Exception e) {
			Log.log(e);
		}
	}

	private void loadConfigOptions() {
		Chat.get().SetupDefaultLanguage();
        ConfigurationNode config = Settings.get().getMainConfig();
        Api.setApi(config.getNode("ApiKey").getString("Apikey here"));
		this.interval = Math.max(config.getNode("Interval").getLong(90L), 10L);
		this.isGuiEnabled = config.getNode("Enabled-GUI").getBoolean(true);
		this.shopCommand = config.getNode("Shop-Command").getString("/shop");
		this.update = config.getNode("auto-update").getBoolean(true);
		this.isSignEnabled = config.getNode("Enabled-signs").getBoolean(true);
		this.color = config.getNode("Color").getString("&0");
		Log.setDebugging(config.getNode("Debug").getBoolean(false));
	}

	private void registerEvents() {
		getGame().getEventManager().registerListeners(this, new ShopListener());
		getGame().getEventManager().registerListeners(this, new ShopCommand());
		getGame().getEventManager().registerListeners(this, new SignListener());
		getGame().getEventManager().registerListeners(this, new RecentListener());
	}

	private boolean authApi() {
		if (Api.authAPI(Api.getKey())) {
			getLogger().info("Using API Key: " + Api.getKey());
			return true;
		} else {
			getLogger().warn("Invalid API Key! Use \"/MM APIKEY <APIKEY>\" to setup your APIKEY");
			return false;
		}
	}

	private void startGUI() {
		if (isGuiEnabled) {
			game.getScheduler().createTaskBuilder().delayTicks(20).execute(new ShopTask()).submit(this);
		}
	}

	private void runCommandChecker() {
		commandTask = new CommandTask();
		game.getScheduler().createTaskBuilder().async().delayTicks(600L).intervalTicks(interval * 20L).execute(commandTask).submit(this);
	}

	private void startSignTasks() {
		if (getIsSignEnabled()) {
			Signs.getSigns().setup();
			signUpdate = new SignUpdate();
			signUpdate.startSignTask();
		}
	}

	private void startTasks() {
		runCommandChecker();
	}

	private void registerCommands() {
		CommandManager commandService = getGame().getCommandManager();
		commandService.register(this, new Commands(), "mm");
	}

	private void saveDefaultSettings() {
		Settings.get().LoadSettings();
	}

	private void stopTasks() {
		try {
			SignUpdate.task.cancel();
			Market.getPlugin().getGame().getScheduler().getScheduledTasks(this).forEach(Task::cancel);
		} catch (Exception e) {
			Log.log(e);
		}
	}

	public File getDataFolder() {
		File file = new File("./config/minecraftmarket/");
		file.mkdirs();
		return file;
	}

}
