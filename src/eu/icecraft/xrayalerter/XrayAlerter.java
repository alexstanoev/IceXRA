package eu.icecraft.xrayalerter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import eu.icecraft.xrayalerter.configCompat.Configuration;

public class XrayAlerter extends JavaPlugin {

	private Map<String, XRAPlayerData> playerData = new HashMap<String, XRAPlayerData>();
	private boolean useLog = true;
	private BufferedLogger log;
	private Configuration conf;
	private List<Integer> watchOres;

	@Override
	public void onDisable() {
		if(useLog) log.disable();
		System.out.println(this.getDescription().getName() + " " + this.getDescription().getVersion() + " was disabled!");
	}

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(new XRAListener(), this);

		if(!this.getDataFolder().exists()) this.getDataFolder().mkdir();

		File confFile = new File(this.getDataFolder(), "config.yml");
		conf = new Configuration(confFile);
		if(!confFile.exists()) {
			conf.setProperty("minY", 50);
			conf.setProperty("minLightLevel", 4);
			conf.setProperty("warnAfter", 6);
			conf.setProperty("watchMinutes", 10);

			conf.setProperty("logToFile", true);
			conf.setProperty("logBuffer", 5);

			List<Integer> oreIDs = new ArrayList<Integer>();
			oreIDs.add(Material.IRON_ORE.getId());
			oreIDs.add(Material.GOLD_ORE.getId());
			oreIDs.add(Material.DIAMOND_ORE.getId());
			oreIDs.add(Material.LAPIS_ORE.getId());
			oreIDs.add(Material.REDSTONE_ORE.getId());

			conf.setProperty("watchOres", oreIDs);

			conf.save();
		}

		conf.load();

		useLog = conf.getBoolean("logToFile", true);

		if(useLog) {
			File logFile = new File(this.getDataFolder(), "log.txt");
			if(!logFile.exists()) {
				try {
					logFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			log = new BufferedLogger(logFile, conf.getInt("logBuffer", 5));
		}

		this.watchOres = conf.getIntList("watchOres", null);

		System.out.println(this.getDescription().getName() + " " + this.getDescription().getVersion() + " was loaded sucessfully!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		label = command.getName().toLowerCase();

		if (sender.hasPermission("icexra.warn")) {

			if(label.equals("icexra")) {
				conf.load();
				sender.sendMessage(ChatColor.AQUA + "[IceXRA] Config reloaded.");

				return true;
			}

			/* TODO
			if(label.equals("tpxra")) {
				Player player = null;
				if (sender instanceof Player) {
					player = (Player) sender;
				} else {
					sender.sendMessage(ChatColor.RED + "[IceXRA] You need to be a player!");
					return true;
				}

				List<String> keys = log.getKeys();
				String lastXra = log.getString(keys.get(keys.size() - 1));
				lastXra.replaceAll("\\([^)]*\\)", "").trim(); // remove date and time from the log string
				String[] split = lastXra.split(",");
				player.sendMessage(ChatColor.AQUA + "[IceXRA] Teleporting to last x-ray alert.");

				player.teleport(new Location(this.getServer().getWorlds().get(0), Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2])));

				return true;
			}

			int i = 0;
			List<String> keys = log.getKeys();
			for(String pl : keys) {
				i++;
				if(keys.size() - i <= 5 || args.length > 0) sender.sendMessage(ChatColor.AQUA + pl + " : " + log.getString(pl));
			}

			 */

		} else {
			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
		}
		return true;
	}

	private void reportPlayer(Player player) {
		XRAPlayerData xp = getXRAPlayer(player);
		int lastWarn = xp.getWarnTime();

		if(lastWarn - (int) (System.currentTimeMillis() / 1000L) > 2*60 || lastWarn == 0) {
			xp.setWarned();

			logWarn(player);

			String xmsg = ChatColor.RED + "POSSIBLE XRAY: " + ChatColor.AQUA + player.getName() + " at "+ player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ() + " (" + player.getLocation().getWorld().getName()+")";

			for(Player p : this.getServer().getOnlinePlayers()) {
				if(p.hasPermission("icexra.warn")) {
					p.sendMessage(xmsg);
				}
			}
		}
	}

	public void logWarn(Player player) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

		log.log(player.getName() + " : " + player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ() + " (at " + player.getLocation().getWorld().getName() + " on " + sdf.format(cal.getTime()) + ")");
	}

	public XRAPlayerData getXRAPlayer(Player player) {
		return this.playerData.get(player.getName());
	}

	public class XRAListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR)
		public void onBlockBreak(BlockBreakEvent event) {
			if(event.isCancelled()) return;

			Block block = event.getBlock();
			int typeID = block.getTypeId();
			Player player = event.getPlayer();

			int lightlevel = 10;

			ArrayList<Block> target = (ArrayList<Block>) player.getLastTwoTargetBlocks(null, 50);
			if (target.size() >= 2 &&!target.get(1).getType().equals(Material.matchMaterial("AIR"))) {
				lightlevel = target.get(0).getLightLevel();
			}

			if(player.getLocation().getY() < conf.getInt("minY", 50) && lightlevel <= conf.getInt("minLightLevel", 50) && watchOres.contains(typeID)) {

				XRAPlayerData xp = getXRAPlayer(player);

				if(xp != null) {
					if(xp.getBlockBroken() > conf.getInt("warnAfter", 6) && ((int) (System.currentTimeMillis() / 1000L) - xp.getFirstBreak() < conf.getInt("watchMinutes", 10)*60)) {
						reportPlayer(player);

						xp.clearBlockBroken();
						xp.setFirstBreak();
					} else {	
						xp.incBlockBroken();
					}
				} else {
					xp = new XRAPlayerData();
					playerData.put(player.getName(), xp);
				}
			}
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onPlayerQuit(PlayerQuitEvent event) {
			playerData.remove(event.getPlayer().getName());
		}
	}

	public class XRAPlayerData {

		private int breakTime = 0;
		private int blockBroken = 0;
		private int warned = 0;

		public XRAPlayerData() {
			this.setFirstBreak();
			this.incBlockBroken();
		}

		public int getFirstBreak() {
			return breakTime;
		}

		public void setFirstBreak() {
			this.breakTime = (int) (System.currentTimeMillis() / 1000L);
		}

		public int getBlockBroken() {
			return this.blockBroken;
		}

		public void incBlockBroken() {
			this.blockBroken++;
		}

		public void clearBlockBroken() {
			this.blockBroken = 0;
		}

		public void setWarned() {
			this.warned = (int) (System.currentTimeMillis() / 1000L);
		}

		public void unsetWarned() {
			this.warned = 0;
		}

		public boolean isWarned() {
			return this.warned > 0;
		}

		public int getWarnTime() {
			return this.warned;
		}
	}
}