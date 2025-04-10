package mainpackage.medicinetakereminderbot;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@SpringBootApplication
public class MedicineTakeReminderBotApplication {

    private final Bot bot;

    public MedicineTakeReminderBotApplication(Bot bot) {
        this.bot = bot;
    }

    public static void main(String[] args) {
        SpringApplication.run(MedicineTakeReminderBotApplication.class, args);
    }

    @PostConstruct
    public void initBot() throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
    }
}
