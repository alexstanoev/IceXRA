package eu.icecraft.xrayalerter;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

import eu.icecraft.xrayalerter.XrayAlerter;

public class XRABlockListener extends BlockListener {


    private XrayAlerter plugin;
    public XRABlockListener(XrayAlerter plugin) {
        this.plugin = plugin;    
    }


    @Override
    public void onBlockBreak(BlockBreakEvent event) {
    	
    	if(plugin.isEnabled()) plugin.onBreak(event);
    	
    }
    

}