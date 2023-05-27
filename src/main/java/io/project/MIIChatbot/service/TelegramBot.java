package io.project.MIIChatbot.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import io.project.MIIChatbot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot{

    private final BotConfig config;
    
    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> botCommands = new ArrayList<>();

        botCommands.add(new BotCommand("/start", "Получить приветственное сообщение"));
        botCommands.add(new BotCommand("/help", "Список команд и информация о боте"));

        try {
            execute(new SetMyCommands(botCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot commands: " + e.getMessage());
        }
    
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken() ;
    }

    @Override 
    public void onUpdateReceived(Update update) {
        
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startMessage(chatId, update.getMessage().getChat().getFirstName());
                    break;       
                default:
                    sendMessage(chatId, "Command is not recognized, try again");
                    log.error("User " + update.getMessage().getChat().getFirstName() + " used invalid command");
                    break;
            }
        }
    }

    private void startMessage(long chatId, String firstName) {
        String answer = "Hey there, " + firstName + ", nice to see you!";
        log.info("Replied to user " + firstName);

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
    
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured", e.getMessage());
        }
    
    }

}
