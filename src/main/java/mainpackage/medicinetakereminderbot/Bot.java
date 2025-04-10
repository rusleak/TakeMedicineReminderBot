package mainpackage.medicinetakereminderbot;

import lombok.NonNull;
import mainpackage.medicinetakereminderbot.Models.Medicine;
import mainpackage.medicinetakereminderbot.Models.User;
import mainpackage.medicinetakereminderbot.Repositories.MedicineRepo;
import mainpackage.medicinetakereminderbot.Services.MedicineService;
import mainpackage.medicinetakereminderbot.Services.ReminderService;
import mainpackage.medicinetakereminderbot.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

import static mainpackage.medicinetakereminderbot.Enums.MedicineState.*;

@Component
public class Bot extends TelegramLongPollingBot {
    private final UserService userService;
    private final MedicineService medicineService;
    private final MedicineRepo medicineRepo;
    private final ReminderService reminderService;

    @Autowired
    public Bot(UserService userService, MedicineService medicineService,
               MedicineRepo medicineRepo, ReminderService reminderService) {
        this.userService = userService;
        this.medicineService = medicineService;
        this.medicineRepo = medicineRepo;
        this.reminderService = reminderService;
    }


    @Override
    public String getBotUsername() {
        return "MedicineTakeReminder";
    }

    @Override
    public String getBotToken() {
        return "YourBotToken";
    }

    @Override
    public void onUpdateReceived(Update update) {


        if (update.hasMessage() && update.getMessage().hasText()) {
            // Handle text messages
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            org.telegram.telegrambots.meta.api.objects.User user = update.getMessage().getFrom();
            Optional<User> verificateUser = userService.findByTgId(user.getId());
            if (verificateUser.isEmpty() && !text.equals("/start")) {
                sendMessageToChat("Нажмите : /start для корректной работы бота", chatId);
                return;
            }


            if (text.equals("/start")) {
                if (userService.findByTgId(user.getId()).isEmpty()) {
                    User user1 = new User();
                    user1.setUserName(user.getUserName());
                    user1.setUserTgId(user.getId());
                    userService.save(user1);
                    showMenu("Меню :", chatId);
                } else {
                    showMenu("Меню :", chatId);
                }

            } else if (text.equals("/new")) {
                if (ifMedicineIsFinished(user.getId())) {
                    Medicine medicine = new Medicine();
                    medicine.setPersonUsername(user.getUserName());
                    medicine.setPersonTgId(user.getId());
                    medicine.setState(WAITING_FOR_MEDICINE_NAME);
                    medicineService.save(medicine);
                    sendMessageToChat("Введите название вашего лекарства : ", chatId);
                } else {
                    handleMedicineStates(text, chatId, user.getId());
                }

            } else if (text.equals("/menu")) {
                showMenu("Меню : ", chatId);
            } else {
                // Process the medicine state updates
                handleMedicineStates(text, chatId, user.getId());
            }
        } else if (update.hasCallbackQuery()) {
            // Handle callback queries
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Long personTgId = update.getCallbackQuery().getFrom().getId();

            if ("show_all".equals(callbackData)) {
                // Show all medicines
                showAllMedicines(chatId, personTgId);
            } else if (callbackData.startsWith("take_")) {
                String medicineId = callbackData.substring(5); // Извлечение ID лекарства
                Medicine medicine = medicineService.findMedicineById(Long.valueOf(medicineId));
                Long counter = medicine.getCounter();
                counter += 1;
                medicine.setCounter(counter);
                System.out.println(counter);
                medicineService.save(medicine);
                if (medicine.getCounter() >= medicine.getPerDay()) {
                    sendMessageToChat("На сегодня хватит !!!\nСегодня вы выпили лекарство " + medicine.getMedicineName() + " : " + counter + " из " + medicine.getPerDay() + " запланнированых раз", chatId);
                    showMenu("Меню : ", chatId);
                } else {
                    sendMessageToChat("Сегодня вы приняли лекарство" + medicine.getMedicineName() + " : " + counter + " из " + medicine.getPerDay() + " запланнированых раз", chatId);
                    showMenu("Меню : ", chatId);
                }
            } else if (callbackData.startsWith("edit_")) {
                Optional<User> verificateUser = userService.findByTgId(update.getCallbackQuery().getFrom().getId());
                if (verificateUser.isEmpty() && !callbackData.equals("/start")) {
                    sendMessageToChat("Нажмите : /start для корректной работы бота", chatId);
                    return;
                }
                // Extract the medicine ID and start the edit process
                String medicineId = callbackData.substring(5);
                editOneMedicineById(medicineId, chatId); // Pass chatId for further communication
            } else if (callbackData.startsWith("selected_medicine")) {
                String medicineId = callbackData.substring(17);
                System.out.println(medicineId);
                showMedicineDetails(chatId, Long.valueOf(medicineId));
            } else if (callbackData.startsWith("delete_")) {
                String medicineId = callbackData.substring(7);
                deleteMedicine(medicineId, chatId);
            } else if (callbackData.equals("/new")) {
                if (ifMedicineIsFinished(update.getCallbackQuery().getFrom().getId())) {
                    org.telegram.telegrambots.meta.api.objects.User user = update.getCallbackQuery().getFrom();
                    Medicine medicine = new Medicine();
                    medicine.setPersonUsername(user.getUserName());
                    medicine.setPersonTgId(user.getId());
                    medicine.setState(WAITING_FOR_MEDICINE_NAME);
                    medicineService.save(medicine);
                    sendMessageToChat("Введите название вашего лекарства :", chatId);
                } else {
                    sendMessageToChat("Сначала закончите предыдущее лекарство\nНапишите /new в чат", chatId);
                }

            } else if (callbackData.equals("/menu")) {
                showMenu("Меню : ", chatId);
            } else {
                sendMessageToChat("Неизвестная команда : " + callbackData, chatId);
            }
        } else {
            System.out.println("Необработанный тип события : " + update);
        }
    }

    public void takeMedicin(Long chatId, Long medicineId) throws TelegramApiException {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Принял");
        button.setCallbackData("take_" + medicineId); // callback для кнопки

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(button);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);

        inlineKeyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Нажмите кнопку, чтобы подтвердить принятие лекарства.");
        message.setReplyMarkup(inlineKeyboardMarkup);
        execute(message); // отправка сообщения с кнопкой
    }

    public void editOneMedicineById(String id, Long chatId) {
        Long medicineId = Long.valueOf(id);
        Medicine medicine = medicineService.findMedicineById(medicineId);

        if (medicine == null) {
            sendMessageToChat("Лекарство не найдено.", chatId);
            return;
        }

        // Ask for new medicine name
        sendMessageToChat("Введите новое название для лекарства :", chatId);

        // Set the state to waiting for name update
        medicine.setState(WAITING_FOR_MEDICINE_NAME);
        medicineService.save(medicine);
    }


    public void deleteMedicine(String id, Long chatId) {
        Long medicineId = Long.valueOf(id);
        medicineService.deleteMedicineById(medicineId);
        sendMessageToChat("Лекарство удалено!", chatId);
        showMenu("Меню : ", chatId); // Показать меню после удаления
    }


    public boolean ifMedicineIsFinished(Long userTgId) {
        Optional<User> user = userService.findByTgId(userTgId);
        if (user.isPresent()) {
            List<Medicine> medicines = medicineRepo.findMedicinesByPersonTgId(userTgId);
            // Указываем правильный тип для ArrayList
            ArrayList<Boolean> answers = new ArrayList<>();
            for (Medicine m : medicines) {
                // Проверяем, если состояние лекарства не равно null
                if (m.getState() != null) {
                    answers.add(false);
                } else {
                    answers.add(true);
                }
            }
            if (answers.contains(false)) {
                return false;
            } else {
                return true;
            }

        }
        return true;  // Если лекарства не найдены или все завершены
    }


    public void handleMedicineStates(String text, Long chatId, Long userId) {
        List<Medicine> medicines = medicineService.findMedicinesByPersonTgId(userId);
        Medicine currentMedicine = null;

        for (Medicine m : medicines) {
            if (m.getState() != null) {
                currentMedicine = m;
                break;
            }
        }

        if (currentMedicine != null) {
            switch (currentMedicine.getState()) {
                case WAITING_FOR_MEDICINE_NAME:
                    // Step 1: Update the medicine name
                    currentMedicine.setMedicineName(text);
                    currentMedicine.setState(WAITING_FOR_PER_DAY);
                    medicineRepo.save(currentMedicine);

                    sendMessageToChat("Сколько раз в день вы принимаете это лекарство?", chatId);
                    break;

                case WAITING_FOR_PER_DAY:
                    // Step 2: Update the number of times per day
                    try {
                        int perDay = Math.abs(Integer.parseInt(text));
                        currentMedicine.setPerDay(perDay);
                        currentMedicine.setState(WAITING_FOR_HOW_OFTEN);
                        medicineRepo.save(currentMedicine);

                        sendMessageToChat("С какой периодичностью (в часах) вы принимаете это лекарство?", chatId);
                    } catch (NumberFormatException e) {
                        sendMessageToChat("Пожалуйста, введите число для количества приемов в день.", chatId);
                    }
                    break;

                case WAITING_FOR_HOW_OFTEN:
                    // Step 3: Update the frequency of medicine
                    try {
                        int howOften = Math.abs(Integer.parseInt(text));
                        currentMedicine.setHowOftenPerDay(howOften);
                        currentMedicine.setState(null); // End editing process
                        medicineRepo.save(currentMedicine);

                        sendMessageToChat("Лекарство успешно обновлено!", chatId);

                        // Optionally, you can set reminders again after editing.
                        reminderService.scheduleDailyReminders(
                                currentMedicine.getMedicineName(),
                                chatId,
                                currentMedicine.getPerDay(),
                                currentMedicine.getHowOftenPerDay(),
                                this, currentMedicine.getId()
                        );
                    } catch (NumberFormatException e) {
                        sendMessageToChat("Пожалуйста, введите число для периодичности (в часах).", chatId);
                    }
                    break;

                default:
                    sendMessageToChat("Неизвестное состояние редактирования лекарства.", chatId);
                    break;
            }
        } else {
            sendMessageToChat("Нет активного процесса редактирования лекарства. Попробуйте еще раз.", chatId);
        }
    }


    public void showAllMedicines(Long chatId, Long personTgId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Ваши лекарства : ");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<Medicine> medicines = medicineService.findMedicinesByPersonTgId(personTgId);
        for (Medicine m : medicines) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            try {
                button.setText(m.getMedicineName());
            } catch (NullPointerException e) {
                button.setText("Без названия");
            }

            button.setCallbackData("selected_medicine" + String.valueOf(m.getId()));
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        markupInline.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(markupInline);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showMedicineDetails(Long chatId, Long medicineId) {
        System.out.println(medicineId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        Medicine medicine = medicineService.findMedicineById(medicineId);
        sendMessage.setText("Информация о лекарстве : \n" +
                "Название лекарства : " + medicine.getMedicineName() +
                "\nСколько раз в день : " + medicine.getPerDay() +
                "\nПереодичность в часах : " + medicine.getHowOftenPerDay() +
                "\nСегодня вы приняли это лекарство : " + medicine.getCounter() + " из " + medicine.getPerDay() + " раз ");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();


        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Редактировать");
        button.setCallbackData("edit_" + medicineId);
        rowInline.add(button);


        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Удалить");
        button1.setCallbackData("delete_" + medicineId);
        rowInline.add(button1);


        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Меню");
        button2.setCallbackData("/menu");
        rowInline.add(button2);


        rowsInline.add(rowInline);


        markupInline.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(markupInline);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToChat(String message, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void showMenu(String message, Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(message);
        try {
            InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            List<InlineKeyboardButton> rowInline = new ArrayList<>();

            // Создаем кнопку и настраиваем ее
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("Мои лекартсва");
            button.setCallbackData("show_all");
            // Добавляем кнопку в строку


            InlineKeyboardButton buttonNew = new InlineKeyboardButton();
            buttonNew.setText("Новое напоминание");
            buttonNew.setCallbackData("/new");
            // Добавляем кнопку в строку
            rowInline.add(buttonNew);
            rowInline.add(button);
            // Добавляем строку в клавиатуру
            rowsInline.add(rowInline);
            // Устанавливаем клавиатуру в сообщение
            markupInline.setKeyboard(rowsInline);
            sendMessage.setReplyMarkup(markupInline);
            // Отправляем сообщение с клавиатурой
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

