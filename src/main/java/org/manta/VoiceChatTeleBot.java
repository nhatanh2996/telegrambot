package org.manta;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class VoiceChatTeleBot extends TelegramLongPollingBot {

    public VoiceChatTeleBot(String botToken) {
        super(botToken);

    }

    private static final String DEFAULT_PATH_MP3 = "src/main/resources/media/mp3";
    private static final String DEFAULT_PATH_VOICE_CHAT = "src/main/resources/media/voice_chat";

    @Override
    public String getBotUsername() {
        return "MantaVoiceChatBot" ;
    }


    @Override
    public void onUpdateReceived(Update update) {
        // Check if the update contains a message with text
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Check if the message is a command (begins with "/")
            if (messageText.startsWith("/")) {
                // Extract the command (ignoring any extra parameters)
                String command = messageText.split(" ")[0];
                if (command.contains("@")) {
                    command = command.substring(0, command.indexOf("@"));
                }
                switch (command) {

                    case "/start":
                        sendTextMessage(chatId, "Welcome to the Voice chat bot!");
                        return;
                    default:
                        sendVoiceMessageWithMp3Conversion(chatId, command);
                }
            } else {
                sendTextMessage(chatId, "Echo: " + messageText);
            }
        }
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendVoiceMessageWithMp3Conversion(long chatId, String command) {
        File mp3File = new File(DEFAULT_PATH_MP3.concat(command).concat(".mp3"));
        File oggFile = new File(DEFAULT_PATH_VOICE_CHAT.concat(command).concat(".ogg"));


        if (!oggFile.exists()) {
            boolean conversionSuccessful = convertMp3ToOgg(mp3File, oggFile);
            if (!conversionSuccessful) {
                sendTextMessage(chatId, "Conversion failed. Unable to send voice message.");
                return;
            }
        }
        SendVoice sendVoice = new SendVoice();
        sendVoice.setChatId(String.valueOf(chatId));
        InputFile voiceInput = new InputFile(oggFile, oggFile.getName());
        sendVoice.setVoice(voiceInput);
        try {
            execute(sendVoice);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendTextMessage(chatId, "Failed to send voice message.");
        }
    }

    private boolean convertMp3ToOgg(File inputMp3, File outputOgg) {
        if (!inputMp3.exists()) {
            System.err.println("Input file does not exist: " + inputMp3.getAbsolutePath());
            return false;
        }

        ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-y", "-i", inputMp3.getAbsolutePath(), outputOgg.getAbsolutePath());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            // Optionally, read and print FFmpeg output.
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Wait for the conversion process to complete.
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Conversion successful: " + outputOgg.getAbsolutePath());
                return true;
            } else {
                System.err.println("Conversion failed with exit code " + exitCode);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static List<String> getListNameFileIncludeExt(String folderPath) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            return Arrays.stream(folder.listFiles()).map(file -> file.getName().substring(0, file.getName().lastIndexOf("."))).toList();
        } else {
            return null;
        }
    }

    public static void main(String[] args) {

        try {
            String configPath = "src/main/resources/config.yml";
            InputStream inputStream = Files.newInputStream(Paths.get(configPath));
           Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            Map<String, Object> botConfig = (Map<String, Object>) config.get("bot");
            String token = (String) botConfig.get("token");
            if(token == null || token.isEmpty()){
                throw new RuntimeException("token is empty");
            }

            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            VoiceChatTeleBot bot = new VoiceChatTeleBot(token);
            botsApi.registerBot(bot);

            List<BotCommand> commandList = new ArrayList<>();
            commandList.add(new BotCommand("/start", "Start the bot"));
            Objects.requireNonNull(getListNameFileIncludeExt(DEFAULT_PATH_MP3)).forEach(command -> commandList.add(new BotCommand("/" + command, "Voice message")));
            try {
                bot.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
