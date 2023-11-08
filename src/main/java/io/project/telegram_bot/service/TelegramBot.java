package io.project.telegram_bot.service;

import com.vdurmont.emoji.EmojiParser;
import io.project.telegram_bot.config.BotConfig;
import io.project.telegram_bot.model.entity.Ads;
import io.project.telegram_bot.model.repository.AdsRepository;
import io.project.telegram_bot.model.repository.UserRepository;
import io.project.telegram_bot.model.entity.User;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final String YOU_NOT_ADMIN = "Вы не являетесь администратором бота и данная команда вам не доступна";
    private final String YES_BUTTON = "yes";
    private final String NO_BUTTON = "no";
    private final String COMMAND_START_EN = "/start";
    private final String COMMAND_START_RU = "старт";
    private final String COMMAND_MY_DATA_EN = "/mydata";
    private final String COMMAND_MY_DATA_RU = "мои данные";
    private final String COMMAND_DELETE_DATA_EN = "/deletedata";
    private final String COMMAND_DELETE_DATA_RU = "удали мои данные";
    private final String COMMAND_HELP_EN = "/help";
    private final String COMMAND_HELP_RU = "помощь";
    private final String COMMAND_REGISTER_EN = "/register";
    private final String COMMAND_REGISTER_RU = "регистрация";
    private final String COMMAND_SEND_EN = "/send";
    private final String COMMAND_NOT_FOUND = "Простите, комманда не найдена";
    private final String ERROR_MESSAGE = "Возникла ошибка: ";
    private static final String HELP_INFORMATION = """
            Краткая информация о том что умеет данный бот;
            Команда "/start" или "старт", запускает бота и приветствует пользователя;
            Команда "/mydata" или "мои данные", показывае пользователю тут информацию, которую бот хранит о нем;
            Команда "/deletedata" или "удали мои данные", удаляет данные пользователя из базы данных;
            Команда "/help" или "помощь", выводит пользователю краткий справочник по командам бота;
            Команда "/register" или "регистрация", пока что демонстрирует работу кнопок под сообщением;
            """;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdsRepository adsRepository;
    final BotConfig config;

    public TelegramBot(BotConfig config, UserRepository userRepository) {
        /*
            Меню команд
         */
        this.config = config;
        List<BotCommand> listCommands = new ArrayList<>();
        listCommands.add(new BotCommand(COMMAND_START_EN,"запуск бота"));
        listCommands.add(new BotCommand(COMMAND_MY_DATA_EN, "общая информация о пользователе"));
        listCommands.add(new BotCommand(COMMAND_DELETE_DATA_EN, "удаление информации о пользователе"));
        listCommands.add(new BotCommand(COMMAND_HELP_EN, "информация о боте и командах"));
        listCommands.add(new BotCommand(COMMAND_REGISTER_EN, "регистрация пользователя - функционал будет добавлен позже"));

        try {
            this.execute(new SetMyCommands(listCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException tae) {
            log.error("Возникла ошибка в списке команд " + tae.getMessage());
        }

    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    /**
     * Метод обработки команд ботом
     * @param update type of Update
     */
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText().toLowerCase();
            long chatId = update.getMessage().getChatId();
            String userFirstName = update.getMessage().getChat().getFirstName();

            if(message.contains(COMMAND_SEND_EN)) {
                if(config.getOwnerId() == chatId) {
                    var textToSend = EmojiParser.parseToUnicode(message.substring(message.indexOf(" ")));
                    var users = userRepository.findAll();

                    for (User user : users) {
                        prepareAndSendMessage(user.getChatId(), textToSend);
                    }
                }
                message = COMMAND_SEND_EN;
            }

            switch (message) {
                case COMMAND_START_EN, COMMAND_START_RU -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, userFirstName);
                }
                case COMMAND_HELP_EN, COMMAND_HELP_RU -> prepareAndSendMessage(chatId, HELP_INFORMATION);

                case COMMAND_MY_DATA_EN,COMMAND_MY_DATA_RU -> userInfo(chatId, update.getMessage());

                case COMMAND_DELETE_DATA_EN, COMMAND_DELETE_DATA_RU -> deleteUserInfo(chatId, update.getMessage());

                case COMMAND_REGISTER_EN, COMMAND_REGISTER_RU -> register(chatId);

                case COMMAND_SEND_EN -> {
                    if (config.getOwnerId() != chatId){
                        prepareAndSendMessage(chatId, YOU_NOT_ADMIN);
                    }
                }

                default -> prepareAndSendMessage(chatId, COMMAND_NOT_FOUND );
            }
        }
        else if(update.hasCallbackQuery()) {

            String callbackData = update.getCallbackQuery().getData();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON)){
                String text = "Вы нажали кнопку да";
                executeEditMessageText(text, chatId, messageId);

            }
            else if (callbackData.equals(NO_BUTTON)){
                String text = "Вы нажали кнопку нет";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы хотите зарегистрироваться?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessageText(message);
    }

    /**
     * Удаление данных о пользователе и БД
     * @param chatId  type of long
     * @param message type of Message
     */
    private void deleteUserInfo(long chatId, Message message) {

        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            prepareAndSendMessage(chatId, "Все данные о вас " + message.getChat().getUserName()+ " удалены из базы данных!");
            log.error("Данные о пользователе с ID=" + chatId + " не найдены!");
        }
        String answer;
        if (user != null) {
            answer = EmojiParser.parseToUnicode("Удаленны данные о пользователе: "
                    + user.getUserName() + ":ok_hand:");
            prepareAndSendMessage(chatId, answer);
            userRepository.delete(user);
        }

        log.info("Удалены данные о пользователе: " + user);
    }

    /**
     * Метод показывает данные которые хранятся о пользователе в БД
     * @param chatId type of long
     * @param message type of Message
     */
    private void userInfo(long chatId, Message message) {

        User user = userRepository.findByChatId(chatId);
        if (user == null) {
            prepareAndSendMessage(chatId, "Данные о пользователе " + message.getChat().getUserName() + " не найдены!");
            log.error("Данные о пользователе с ID=" + chatId + " не найдены!");
        }
        assert user != null;
        String answer = EmojiParser.parseToUnicode("Данные которые хранятся в базе данных о тебе "
                + user.getFirstName() + " :thinking::" + user);
        prepareAndSendMessage(chatId, answer);
        log.info("Данные которые хранятся о пользователе: " + user);
    }

    /**
     * Внесение пользователя в БД, если его там нет
     * @param message type of Message
     */
    private void registerUser(Message message) {

        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegistrationTime(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("Сохранены данные пользователя: " + user);
        }
    }

    /**
     * Приветствие пользователя
     * @param chatId type of long
     * @param userFirstName type of String
     */
    private void startCommandReceived(long chatId, String userFirstName) {

        String answer = EmojiParser.parseToUnicode("Привет, " + userFirstName + ", приятно с тобой познакомиться" + " :blush:");
        log.info("ответил пользователю: " + userFirstName);
        sendMessageKeyboardStart(chatId, answer);
    }

    /**
     * Отправка сообщения и набор экранных кнопок
     * @param chatId type of long
     * @param textToSend type of String
     */
    private void sendMessageKeyboardStart(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("старт");
        row.add("мои данные");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("удали мои данные");
        row.add("помощь");
        row.add("регистрация");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
        executeMessageText(message);
    }

    /**
     * Метод изменения текста в сообщение при выборе варианта ответа под текстом
     * @param text type of String
     * @param chatId type of long
     * @param messageId type of int
     */
    private void executeEditMessageText(String text, long chatId, int messageId) {
        EditMessageText messageText = new EditMessageText();
        messageText.setChatId(String.valueOf(chatId));
        messageText.setText(text);
        messageText.setMessageId(messageId);
        executeMessageText(messageText);
    }

    /**
     * Метод для отправки ответа пользователю
     * @param message type of SendMessage
     */
    private void executeMessageText(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException tae) {
            log.error(ERROR_MESSAGE + tae.getMessage());
            System.out.println(ERROR_MESSAGE + tae);
        }
    }

    /**
     * Метод для отправки ответа пользователю
     * @param message type of EditMessageText
     */
    private void executeMessageText(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException tae) {
            log.error(ERROR_MESSAGE + tae.getMessage());
            System.out.println(ERROR_MESSAGE + tae);
        }
    }

    /**
     * метод подготовки и отправки сообщения
     * @param chatId type of long
     * @param textToSend type of String
     */
    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessageText(message);
    }

    /**
     * Выгрузка текста из БД в бот в заданное время
     */
//    @Scheduled(cron = "${cron.scheduler}")
//    private void sendAds() {
//        var ads = adsRepository.findAll();
//        var users = userRepository.findAll();
//
//        for(Ads ad: ads) {
//            for (User user : users) {
//                prepareAndSendMessage(user.getChatId(), ad.getAd());
//            }
//        }
//    }
}
