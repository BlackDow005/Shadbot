package me.shadorc.shadbot.listener;

import java.util.List;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import me.shadorc.shadbot.data.database.DatabaseManager;

public class ChannelListener {

	/**
	 * Remove deleted text channels from allowed text channels setting.
	 * 
	 * @param event - the event
	 */
	public static void onTextChannelDelete(TextChannelDeleteEvent event) {
		final DBGuild dbGuild = DatabaseManager.getDBGuild(event.getChannel().getGuildId());
		final List<Snowflake> allowedChannelIds = dbGuild.getAllowedTextChannels();
		// If the channel was an allowed channel...
		if(allowedChannelIds.remove(event.getChannel().getId())) {
			// ...update settings to remove the deleted one
			dbGuild.setSetting(SettingEnum.ALLOWED_TEXT_CHANNELS, allowedChannelIds);
		}
	}

}
