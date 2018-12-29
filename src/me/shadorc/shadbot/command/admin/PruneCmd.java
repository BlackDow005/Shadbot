package me.shadorc.shadbot.command.admin;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.CommandPermission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingPermissionException.UserType;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

@Command(category = CommandCategory.ADMIN, permission = CommandPermission.ADMIN, names = { "prune" })
public class PruneCmd extends AbstractCommand {

	private static final int MAX_MESSAGES = 250;

	@Override
	public Mono<Void> execute(Context context) {
		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		return context.getChannel()
				.flatMap(channel -> DiscordUtils.requirePermissions(channel, context.getSelfId(), UserType.BOT,
						Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY)
						.then(DiscordUtils.requirePermissions(channel, context.getAuthorId(), UserType.NORMAL, Permission.MANAGE_MESSAGES))
						.then(context.getMessage().getUserMentions().collectList())
						.flatMap(mentions -> {
							final String arg = context.getArg().orElse("");
							final List<String> quotedElements = StringUtils.getQuotedElements(arg);

							if(arg.contains("\"") && quotedElements.isEmpty() || quotedElements.size() > 1) {
								throw new CommandException("You have forgotten a quote or have specified several quotes in quotation marks.");
							}

							final String words = quotedElements.isEmpty() ? null : quotedElements.get(0);

							// Remove everything from argument (users mentioned and quoted words) to keep only count if specified
							final String argCleaned = StringUtils.remove(arg,
									FormatUtils.format(mentions, User::getMention, " "),
									String.format("\"%s\"", words))
									.trim();

							Integer count = NumberUtils.asPositiveInt(argCleaned);
							if(!argCleaned.isEmpty() && count == null) {
								loadingMsg.stopTyping();
								throw new CommandException(String.format("`%s` is not a valid number. If you want to specify a word or a sentence, "
										+ "please include them in quotation marks. See `%shelp %s` for more information.",
										argCleaned, context.getPrefix(), this.getName()));
							}

							count = count == null ? MAX_MESSAGES : Math.min(MAX_MESSAGES, count);

							final List<Snowflake> mentionIds = mentions.stream().map(User::getId).collect(Collectors.toList());

							return channel.getMessagesBefore(Snowflake.of(Instant.now()))
									.take(count)
									.filter(message -> mentions.isEmpty()
											|| message.getAuthorId().map(mentionIds::contains).orElse(false))
									.filter(message -> words == null
											|| message.getContent().map(content -> content.contains(words)).orElse(false)
											|| this.getEmbedContent(message).contains(words))
									.collectList();
						})
						.flatMap(messages -> DiscordUtils.bulkDelete((TextChannel) channel, messages))
						.flatMap(deletedMessages -> loadingMsg.send(String.format(Emoji.CHECK_MARK + " (Requested by **%s**) %s deleted.",
								context.getUsername(), StringUtils.pluralOf(deletedMessages, "message"))))
						.then());
	}

	private String getEmbedContent(Message message) {
		final StringBuilder strBuilder = new StringBuilder();
		for(Embed embed : message.getEmbeds()) {
			for(Field field : embed.getFields()) {
				strBuilder.append(field.getName() + "\n" + field.getValue() + "\n");
			}
		}
		return strBuilder.toString();
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Delete messages.")
				.addArg("@user(s)", "from these users", true)
				.addArg("\"words\"", "containing these words", true)
				.addArg("number", "number of messages to delete (max: 100)", true)
				.setExample(String.format("Delete **15** messages from user **@Shadbot** containing **hi guys**:"
						+ "%n`%s%s @Shadbot \"hi guys\" 15`", context.getPrefix(), this.getName()))
				.addField("Info", "Messages older than 2 weeks cannot be deleted.", false)
				.build();
	}

}
