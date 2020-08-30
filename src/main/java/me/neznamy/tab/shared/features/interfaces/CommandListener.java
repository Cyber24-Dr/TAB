package me.neznamy.tab.shared.features.interfaces;

import me.neznamy.tab.shared.ITabPlayer;

/**
 * Classes implementing this interface will receive player commands
 */
public interface CommandListener extends Feature {

	public boolean onCommand(ITabPlayer sender, String message);
}
