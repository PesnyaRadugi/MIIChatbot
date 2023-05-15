package io.project.MIIChatbot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import io.project.MIIChatbot.config.BotConfig;

@Component
public class TelegramBot extends TelegramLongPollingBot{

    private final BotConfig config;
    
    public TelegramBot(BotConfig config) {
        this.config = config;
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
                    break;
            }
        }
    }

    private void startMessage(long chatId, String firstName) {
        String answer = "Hey there, " + firstName + ", nice to see you!";

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
    
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    
    }

}
