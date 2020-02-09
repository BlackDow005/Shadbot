package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class UserInfoCmd extends BaseCmd {

    private final DateTimeFormatter dateFormatter;

    public UserInfoCmd() {
        super(CommandCategory.INFO, List.of("user_info", "user-info", "userinfo"));
        this.setDefaultRateLimiter();

        this.dateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu - HH'h'mm", Locale.ENGLISH);
    }

    @Override
    public Mono<Void> execute(Context context) {
        final Mono<Member> getMember = context.getMessage()
                .getUserMentions()
                .switchIfEmpty(context.getGuild()
                        .flatMapMany(guild -> DiscordUtils.extractMembers(guild, context.getContent())))
                .defaultIfEmpty(context.getAuthor())
                .next()
                .flatMap(user -> user.asMember(context.getGuildId()));

        return Mono.zip(getMember, getMember.flatMapMany(Member::getRoles).collectList())
                .map(tuple -> this.getEmbed(tuple.getT1(), tuple.getT2(), context.getAvatarUrl()))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    private Consumer<EmbedCreateSpec> getEmbed(Member member, List<Role> roles, String avatarUrl) {
        final String creationDate = String.format("%s%n(%s)",
                TimeUtils.toLocalDate(member.getId().getTimestamp()).format(this.dateFormatter),
                FormatUtils.longDuration(member.getId().getTimestamp()));

        final String joinDate = String.format("%s%n(%s)",
                TimeUtils.toLocalDate(member.getJoinTime()).format(this.dateFormatter),
                FormatUtils.longDuration(member.getJoinTime()));

        final StringBuilder usernameBuilder = new StringBuilder(member.getUsername());
        if (member.isBot()) {
            usernameBuilder.append(" (Bot)");
        }
        if (member.getPremiumTime().isPresent()) {
            usernameBuilder.append(" (Booster)");
        }

        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> {
                    embed.setAuthor(String.format("User Info: %s", usernameBuilder), null, avatarUrl)
                            .setThumbnail(member.getAvatarUrl())
                            .addField("Display name", member.getDisplayName(), false)
                            .addField("User ID", member.getId().asString(), false)
                            .addField("Creation date", creationDate, false)
                            .addField("Join date", joinDate, false);

                    if (!roles.isEmpty()) {
                        embed.addField("Roles", FormatUtils.format(roles, Role::getMention, "\n"), true);
                    }
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Show info about a user.")
                .addArg("@user", "if not specified, it will show your info", true)
                .build();
    }

}
