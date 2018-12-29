package me.shadorc.shadbot.data.database;

import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.utils.NumberUtils;

public class DBMember {

	private final long guildId;
	@JsonProperty("id")
	private final long memberId;
	@JsonProperty("coins")
	private final AtomicInteger coins;

	public DBMember(Snowflake guildId, Snowflake id) {
		this.guildId = guildId.asLong();
		this.memberId = id.asLong();
		this.coins = new AtomicInteger(0);
	}

	public DBMember() {
		this(Snowflake.of(0L), Snowflake.of(0L));
	}

	@JsonIgnore
	public Snowflake getGuildId() {
		return Snowflake.of(this.guildId);
	}

	@JsonIgnore
	public Snowflake getId() {
		return Snowflake.of(this.memberId);
	}

	@JsonIgnore
	public int getCoins() {
		return this.coins.get();
	}

	public void addCoins(int gains) {
		this.coins.set(NumberUtils.between(this.getCoins() + gains, 0, Config.MAX_COINS));
	}

	public void resetCoins() {
		this.coins.set(0);
	}

}
