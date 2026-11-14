package top.leonam.jda;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import top.leonam.escutador.BotListener;
import top.leonam.service.MonitorService;

public class IniciaJda {
    public static void iniciaJda() {
        var token = System.getenv("DISCORD_TOKEN");

        if (token == null || token.isBlank()) {
            System.err.println("Erro: Variável de ambiente DISCORD_TOKEN não configurada.");
            System.exit(-1);
        }

        try {
            var listener = new BotListener();

            var jda = JDABuilder.createDefault(token)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Tamagotchi 🐾 | /ajuda"))
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(listener)
                    .build();

            jda.awaitReady();

            listener.onStartup();
            new MonitorService().iniciar(jda);

            IO.println("Bot iniciado com sucesso.");

        } catch (Exception e) {
            if (e.getCause() != null) e.getCause().printStackTrace();
            else e.printStackTrace();
        }
    }
}
