package io.project.MIIChatbot.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import io.project.MIIChatbot.config.BotConfig;
import io.project.MIIChatbot.config.Const;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot{

    private final BotConfig config;
    private static final String FORM_FOLDER_PATH = "Documents\\Form";
    
    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> botCommands = new ArrayList<>();

        botCommands.add(new BotCommand("/start", "Получить приветственное сообщение"));
        botCommands.add(new BotCommand("/help", "Список команд и информация о боте"));
        botCommands.add(new BotCommand("/form", "Get a form to fill"));

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
                case "/form":
                    sendFormToUser(chatId);
                    break;
                default:
                    defaultMessage(chatId);
                    log.error("User " + update.getMessage().getChat().getFirstName() + " used invalid command");
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String callbackData = callbackQuery.getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case Const.GET_INSTRUCTIONS:
                    defaultMessage(chatId);
                    break;
                case Const.SIGN_IN:
                    sendFormToUser(chatId);
                    break;
            }

            AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
            answerCallbackQuery.setCallbackQueryId(callbackQuery.getId());
            try {
                execute(answerCallbackQuery);    
            } catch (TelegramApiException e) {
                log.error("Error sending callbackQueryAnswer: ", e.getMessage());
            }
            
        }
    }

    private void startMessage(long chatId, String firstName) {
        // Inline keyboard controls
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        List<InlineKeyboardButton> firstRow = new ArrayList<>();
        InlineKeyboardButton instructionsButton = new InlineKeyboardButton();
        instructionsButton.setText("Получить инструкции для спикеров");
        instructionsButton.setCallbackData(Const.GET_INSTRUCTIONS);
        firstRow.add(instructionsButton);

        List<InlineKeyboardButton> secondRow = new ArrayList<>();
        InlineKeyboardButton signInButton = new InlineKeyboardButton();
        signInButton.setText("Записаться в видеостудию");
        signInButton.setCallbackData(Const.SIGN_IN);
        secondRow.add(signInButton);

        keyboardRows.add(firstRow);
        keyboardRows.add(secondRow);
        inlineKeyboardMarkup.setKeyboard(keyboardRows);

        // Message part
        String reply = """
            Здравствуйте! Рады приветствовать Вас в нашей видео-студии ЦДО МИИГАиК.

            Новые реалии требуют новых образовательных форматов, поэтому мы надеемся, что участие в цифровизации курсов покажется Вам интересным и полезным опытом.

            Нажмите “Получить инструкции для спикеров”, чтобы ознакомиться с регламентом нашего с Вами взаимодействия.

            Нажмите “Записаться в видеостудию”, если Вы уже ознакомились со всеми организационными моментами и готовы действовать
            
            Если у Вас остались вопросы, запросите помощь администратора.            
                """;
        log.info("Replied to user " + firstName);

        sendMessage(chatId, reply, inlineKeyboardMarkup);
    }

    private void sendFormToUser(Long chatId) {
        File formFolder = new File(FORM_FOLDER_PATH);
        File[] files = formFolder.listFiles();

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId.toString());

        if (files != null && files.length > 0) {
            File firstFile = files[0];

            try {
                sendDocument.setDocument(new InputFile(firstFile));
                execute(sendDocument);
            } catch (TelegramApiException e) {
                log.error("Error when sending form file: ", e.getMessage());
            }

        }

    }

    private void defaultMessage(Long chatId) {
        sendMessage(chatId, "Command is not recognized, try again" + chatId.toString());
    }

    /**
     * Method to send simple text reply
     * @param chatId - Id of chat
     * @param textToSend - String value for reply text
     */
    private void sendMessage(long chatId, String textToSend) {
    
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured:", e.getMessage());
        }
    
    }

    /**
     * sendMessage method overload.
     * Allows to send message with inline keyboard.
     * @param chatId - id of a chat
     * @param textToSend - string value, stands for a bot reply text
     * @param inlineKeyboardMarkup - your inline keyboard object with buttons
     */
    private void sendMessage(long chatId, String textToSend, InlineKeyboardMarkup inlineKeyboardMarkup) {
        
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured:", e.getMessage());
        }
    
    }

}
