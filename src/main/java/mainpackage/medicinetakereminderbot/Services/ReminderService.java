package mainpackage.medicinetakereminderbot.Services;

import mainpackage.medicinetakereminderbot.Bot;
import mainpackage.medicinetakereminderbot.Models.Medicine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.concurrent.*;

@Service
public class ReminderService {

    private final MedicineService medicineService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>(); // Хранение задач

    @Autowired
    public ReminderService(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    public void scheduleReminder(String medicineName, Long chatId, int perDay, int intervalInHours, Bot bot, Long medicineId) {
        for (int i = 0; i < perDay; i++) {
            long delay = i * intervalInHours;

            ScheduledFuture<?> task = scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        Medicine medicine = medicineService.findMedicineById(medicineId);
                        if (medicine == null) {
                            System.out.println("Лекарство с ID " + medicineId + " не найдено. Остановка напоминания.");
                            cancelTask(medicineId); // Отменяем задачу
                            return;
                        }

                        bot.sendMessageToChat("Пора принять лекарство: " + medicineName, chatId);
                        bot.takeMedicin(chatId, medicineId);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }, delay, TimeUnit.HOURS);

            // Добавляем проверку: не заменяем существующую задачу
            if (!scheduledTasks.containsKey(medicineId)) {
                scheduledTasks.put(medicineId, task);
            }
        }
    }

    public void scheduleDailyReminders(String medicineName, Long chatId, int perDay, int intervalInHours, Bot bot, Long medicineId) {
        // Проверяем, существует ли уже задача, если да - не создаём новую
        if (scheduledTasks.containsKey(medicineId)) {
            System.out.println("Напоминание для ID " + medicineId + " уже существует.");
            return;
        }

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Medicine medicine = medicineService.findMedicineById(medicineId);
                    if (medicine == null) {
                        System.out.println("Лекарство с ID " + medicineId + " не найдено. Остановка ежедневного напоминания.");
                        cancelTask(medicineId); // Останавливаем задачу
                        return;
                    }

                    medicine.setCounter(0L);
                    medicineService.save(medicine);
                    scheduleReminder(medicineName, chatId, perDay, intervalInHours, bot, medicineId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 24, TimeUnit.SECONDS);

        scheduledTasks.put(medicineId, task);
    }

    public void cancelTask(Long medicineId) {
        ScheduledFuture<?> task = scheduledTasks.get(medicineId);
        if (task != null) {
            task.cancel(true); // Отменяем задачу
            scheduledTasks.remove(medicineId); // Удаляем из коллекции
        }
    }
}

