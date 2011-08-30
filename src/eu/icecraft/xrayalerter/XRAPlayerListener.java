package eu.icecraft.xrayalerter;

import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class XRAPlayerListener extends PlayerListener {

	private XrayAlerter plugin;

	public XRAPlayerListener(XrayAlerter plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onPlayerQuit(PlayerQuitEvent ev) {
		plugin.onQuit(ev.getPlayer());
	}
	
}
