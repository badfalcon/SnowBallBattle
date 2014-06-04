package com.github.badfalcon.SnowBallBattle;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class Spectator {

	SnowBallBattle plugin;

	public Spectator(SnowBallBattle plugin) {
		this.plugin = plugin;
	}

	public void setSpectate(Player player) {
		player.setAllowFlight(true);
		player.setFlying(true);
		player.getInventory().clear();
		player.setCanPickupItems(false);
		if (plugin.getConfig().contains("Spectator.Height")) {
			Location location = player.getLocation();
			location.setY(location.getY()
					+ plugin.getConfig().getInt("Spectator.Height"));
			player.teleport(location);
		}
		player.setMetadata("spectating", new FixedMetadataValue(plugin, true));
	}

	public void removeSpectate(Player player) {
		player.setAllowFlight(false);
		player.setFlying(false);
		player.setCanPickupItems(true);
		player.removeMetadata("spectating", plugin);
	}

	public boolean addSpectator(String player) {
		FileConfiguration config = plugin.getConfig();
		List<String> spectators = config.getStringList("Spectator.List");
		if (!isSpectator(player)) {
			spectators.add(player);
			config.set("Spectator.List", spectators);
			plugin.saveConfig();
			return true;
		} else {
			return false;
		}
	}

	public boolean removeSpectator(String player) {
		FileConfiguration config = plugin.getConfig();
		List<String> spectators = config.getStringList("Spectator.List");
		if (isSpectator(player)) {
			spectators.remove(player);
			config.set("Spectator.List", spectators);
			plugin.saveConfig();
			return true;
		} else {
			return false;
		}
	}

	public boolean isSpectator(String player) {
		FileConfiguration config = plugin.getConfig();
		List<String> spectators = config.getStringList("Spectator.List");
		if (spectators.contains(player)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isSpectating(Player player) {
		if (player.hasMetadata("spectating")) {
			return true;
		} else {
			return false;
		}
	}

}