package org.discord;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.interactions.component.StringSelectMenuImpl;
import org.discord.utils.korean.KoreanUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptProcesser {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void accept(String script, SlashCommandInteractionEvent e, User u) {
        PlayerData playerData = DiscordListener.data.get(u.getId());
        script = convertLitheral(script, u);
        String[] actions = Arrays.stream(script.split("⇒")).map(String::strip).toArray(String[]::new);
        ReplyCallbackAction action = getReplyMessage(actions[0], e.reply(""));
        for (int i = 1; i < actions.length; i++) {
            String singleAction = actions[i];
            if (singleAction.startsWith("Actions")) {
                for (String actionRow : singleAction.substring("Actions[".length(), singleAction.length() - 1).split(",")) {
                    actionRow = actionRow.strip();
                    switch (actionRow.substring(0, actionRow.indexOf("["))) {
                        case "Buttons":
                            action.addActionRow(Arrays.stream(actionRow.substring("Buttons[".length(), actionRow.length() - 1).split("/"))
                                    .map(s -> s.strip().split("\\|"))
                                    .map(data -> new ButtonImpl(data[0], data[1], ButtonStyle.SECONDARY, false, Emoji.fromUnicode(data[2])))
                                    .collect(Collectors.toList()));
                            break;
                            /* not implemented yet
                        case "EntitySelectMenu":
                            action.addActionRow();
                            break;
                             */
                        case "StringSelectMenu":
                            String[] data = actionRow.substring("StringSelectMenu[".length(), actionRow.length() - 1).split("\\|");
                            List<SelectOption> options = new ArrayList<>();
                            for (int j = 2; j < data.length; j++) {
                                String[] selectOpt = data[j].split("/");
                                options.add(SelectOption.of(selectOpt[1], selectOpt[2]).withDescription(selectOpt[2]));
                            }
                            action.addActionRow(new StringSelectMenuImpl(data[0], data[1], 1, 1, false, options));
                            break;
                        case "Implemented":
                            switch (actionRow.substring(actionRow.indexOf("[") + 1, actionRow.length() - 1)) {
                                case "jobs":
                                    action.addActionRow(DiscordListener.getJobButtons(u));
                                    break;
                                case "stats":
                                    action.addActionRow(DiscordListener.getStatPointUseButtons(u));
                                    break;
                                default:
                                    break;
                            }
                            break;
                        default:
                            break;
                    }
                }
            } else if (singleAction.equals("story++")) playerData.storyIndex++;
            else if (singleAction.equals("Ephemeral")) action.setEphemeral(true);
        }
        action.queue();
    }

    private static ReplyCallbackAction getReplyMessage(String message, ReplyCallbackAction action) {
        while (message.contains("[postposition:")) {
            String temp = message.substring(0, message.indexOf("[postposition:"));
            message = message.substring(message.indexOf("[postposition:") + 14);
            String[] postPositionAction = message.substring(0, message.indexOf("]")).split("\\|");
            String last = postPositionAction.length == 2 ? "" : postPositionAction[2];
            message = temp + KoreanUtils.getPostposition(postPositionAction[0], postPositionAction[1], last) + message.substring(message.indexOf("]") + 1);
        }
        while (message.contains("[resources:")) {
            action.addContent(message.substring(0, message.indexOf("[resources:")));
            message = message.substring(message.indexOf("[resources:") + 11);
            String[] fileAction = message.substring(0, message.indexOf("]")).split("\\|");
            InputStream resource = Main.class.getClassLoader().getResourceAsStream(fileAction[0]);
            if (resource != null)
                action.addFiles(FileUpload.fromData(resource, STR."resource\{fileAction[0].substring(fileAction[0].lastIndexOf("."))}"));
            else
                action.addContent(fileAction[1]);
            message = message.substring(message.indexOf("]") + 1);
        }
        action.addContent(message);
        return action;
    }

    private static String convertLitheral(String text, User u) {
        PlayerData playerData = DiscordListener.data.get(u.getId());
        return text
                .replace("{user.name}", u.getEffectiveName())
                .replace("{user.job.name}", playerData.job.getName())
                .replace("{user.id}", u.getId());
    }
}
