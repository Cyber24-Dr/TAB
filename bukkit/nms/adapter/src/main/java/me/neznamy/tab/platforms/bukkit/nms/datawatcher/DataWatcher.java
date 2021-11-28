package me.neznamy.tab.platforms.bukkit.nms.datawatcher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import me.neznamy.tab.platforms.bukkit.nms.AdapterProvider;

/**
 * A class representing the n.m.s.DataWatcher class to make work with it much easier
 */
public class DataWatcher {

	//datawatcher data
	private final Map<Integer, DataWatcherItem> dataValues = new HashMap<>();
	
	//a helper for easier data write
	private final DataWatcherHelper helper = new DataWatcherHelper(this);

	/**
	 * Sets value into data values
	 * @param type - type of value
	 * @param value - value
	 */
	public void setValue(DataWatcherObject type, Object value){
		dataValues.put(type.getPosition(), new DataWatcherItem(type, value));
	}

	/**
	 * Removes value by position
	 * @param position - position of value to remove
	 */
	public void removeValue(int position) {
		dataValues.remove(position);
	}

	/**
	 * Returns item with given position
	 * @param position - position of item
	 * @return item or null if not set
	 */
	public DataWatcherItem getItem(int position) {
		return dataValues.get(position);
	}

	/**
	 * Returns helper created by this instance
	 * @return data write helper
	 */
	public DataWatcherHelper helper() {
		return helper;
	}

	public Collection<DataWatcherItem> getItems() {
		return dataValues.values();
	}
}
