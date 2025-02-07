package org.manta;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Main {
    public static void main(String[] args) {
        try {
            String token = System.getProperty("token", "tokenDefault");
            String mp3Path = System.getProperty("media", "mediaDefault").concat("/mp3");

            System.out.println("token: ".concat(token));
            System.out.println("mp3Path: ".concat(mp3Path));
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            VoiceChatTeleBot bot = new VoiceChatTeleBot(token);
            botsApi.registerBot(bot);

            List<BotCommand> commandList = new ArrayList<>();
            commandList.add(new BotCommand("/start", "Start the bot"));

            Objects.requireNonNull(VoiceChatTeleBot.getListNameFileIncludeExt(mp3Path)).forEach(command -> commandList.add(new BotCommand("/" + command, "Voice message")));
            try {
                bot.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
