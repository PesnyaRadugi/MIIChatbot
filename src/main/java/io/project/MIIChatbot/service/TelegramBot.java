package io.project.MIIChatbot.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    sendPresentationFormToUser(chatId);
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
                    speakersInstructions(chatId);
                    break;
                case Const.SIGN_IN:
                    signInToStudio(chatId);;
                    break;
                case Const.SIGNIN_PREPARATION:
                    giveAdvice(chatId);
                    break;
                case Const.GET_PRESENTATION_INSTRUCTION:
                    presentationMenu(chatId);
                    break;
                case Const.NEED_HELP:
                
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
        
        // Generate Button
        HashMap<String, String> buttons = new HashMap<>();
        buttons.put("Записаться в видеостудию", Const.SIGN_IN);
        buttons.put("Получить инструкции для спикеров", Const.GET_INSTRUCTIONS);

        // Message part
        String reply = """
            Здравствуйте! Рады приветствовать Вас в нашей видео-студии ЦДО МИИГАиК.

            Новые реалии требуют новых образовательных форматов, поэтому мы надеемся, что участие в цифровизации курсов покажется Вам интересным и полезным опытом.

            Нажмите “Получить инструкции для спикеров”, чтобы ознакомиться с регламентом нашего с Вами взаимодействия.

            Нажмите “Записаться в видеостудию”, если Вы уже ознакомились со всеми организационными моментами и готовы действовать
            
            Если у Вас остались вопросы, запросите помощь администратора.
            """;

        log.info("Replied to user " + firstName);

        sendMessage(chatId, reply, generateInlineKeyboard(buttons));
    }

    private void speakersInstructions(long chatId) {
        
        HashMap<String, String> buttons = new HashMap<>();
        buttons.put("Нужна помощь", Const.NEED_HELP);
        buttons.put("Работа с презентацией", Const.GET_PRESENTATION_INSTRUCTION);
        buttons.put("Подготовка к записи", Const.SIGNIN_PREPARATION);

        String reply = """
            Краткая инструкция:
            1. Определитесь с темой для записи и подготовьте презентацию. Шаблон вы найдете в отделе “работа с презентацией” . Обратите внимание, мы записываем презентации, подготовленные строго по шаблону. Презентация не должна быть слишком длинной, одна готовая запись длится ХХ минут
            
            2. Отправьте презентацию с помощью этого бота, после чего Вы сможете оформить запись.
            
            3. Подготовьтесь к записи (с советами вы можете ознакомиться в соответствующем разделе). Приходите вовремя или заранее предупредите администратора об опоздании или переносе записи
            
            4. Проведите запись. Не волнуйтесь, наш опытный режиссер поможет вам настроиться, также видеостудия оснащена суфлером. Готовую запись вы получите после двух этапов монтажа - это займет около ХХ дней
            
            5. С регламентом работы Вы можете ознакомиться в прикрепленном документе
            
            6. Если у Вас остались вопросы, запросите помощь.
           
            """;
        
        log.info("Sent speaker instructions!");
        sendMessage(chatId, reply, generateInlineKeyboard(buttons));
    }

    private void giveAdvice(long chatId) {

        String reply = """
            Мы убеждены, что каждый преподаватель МИИГАиК - уже настоящая звезда. А чтобы сиять на видеолекции еще ярче, мы подготовили для Вас некоторые рекомендации:
            
            1. Запись проходит на белом фоне, поэтому не надевайте белую одежду. Также откажитесь от мелких принтов в пользу однотонной одежды.
            
            2. В день записи проведите с утра речевые разминки: чистоговорки, скороговорки, упражнения для интонации и дикции. Мы не отрицаем Ваше ораторское мастерство, но аппаратура студии может значительно усилить недостатки речи. Советуем также сделать зарядку для отсутствия скованности в кадре
            
            3. Не волнуйтесь: Вас поддержит режиссер студии, имеющий более 30 лет опыта руководством театра. Запись проходит в спокойной атмосфере.
            
            4. Настройтесь на успех. Вы - профессионал своего дела! 
            """;

        log.info("Sent advice to user!");
        sendMessage(chatId, reply);
    }

    private void signInToStudio(long chatId) {
        HashMap<String, String> buttons = new HashMap<>();
        buttons.put("Да", Const.YES_READ_INSTRUCTIONS);
        buttons.put("Нет", Const.NO_DIDNT_READ_INSTRUCTIONS);

        String reply = """
            Вы ознакомились с информацией для спикеров ?
            """;
        
        log.info("Sent speaker instructions!");
        sendMessage(chatId, reply, generateInlineKeyboard(buttons));
    }

    private void presentationMenu(long chatId) {
        HashMap<String, String> buttons = new HashMap<>();
        buttons.put("Скачать шаблон презентации", "Download_presentation_form");
        buttons.put("Загрузить презнтацию", "Upload_presentation");

        String reply = """
            В этом разделе Вы можете скачать шаблон презентации и загрузить готовую. Обратите внимание, мы записываем презентации, подготовленные строго по шаблону.    
            """;

        sendMessage(chatId, reply, generateInlineKeyboard(buttons));
    }

    private void sendPresentationFormToUser(Long chatId) {
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

    private InlineKeyboardMarkup generateInlineKeyboard(HashMap<String, String> buttons) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardRows = new ArrayList<>();

        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();

            button.setText(entry.getKey());
            button.setCallbackData(entry.getValue());
            row.add(button);

            keyboardRows.add(row);
        }

        inlineKeyboardMarkup.setKeyboard(keyboardRows);

        return inlineKeyboardMarkup;
    }

}
