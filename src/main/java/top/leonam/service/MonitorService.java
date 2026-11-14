package top.leonam.service;

import net.dv8tion.jda.api.JDA;
import top.leonam.dao.BichoDAO;
import top.leonam.model.Bicho;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Roda a cada 15 minutos em uma thread virtual e verifica bichos que morreram
 * por descuido (fome/limpeza zeradas). Envia DM ao dono quando isso ocorrer.
 *
 * O decaimento real é calculado lazily (atualizarStatusPeloTempo) a cada checagem,
 * sem guardar estado intermediário — econômico e correto.
 */
public class MonitorService {
    private static final int INTERVALO_MINUTOS = 15;

    private final BichoDAO bichoDAO = new BichoDAO();
    private JDA jda;

    public void iniciar(JDA jda) {
        this.jda = jda;

        var executor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = Thread.ofVirtual().unstarted(r);
            t.setName("monitor-bichos");
            return t;
        });

        executor.scheduleAtFixedRate(this::verificarMortes, INTERVALO_MINUTOS, INTERVALO_MINUTOS, TimeUnit.MINUTES);
    }

    private void verificarMortes() {
        var bichos = bichoDAO.buscarTodos();

        for (Bicho bicho : bichos) {
            bicho.atualizarStatusPeloTempo();

            if (bicho.isMorto()) {
                bichoDAO.marcarMorto(bicho.getId());
                enviarDmMorte(bicho);
            } else {
                // Persiste o decaimento acumulado sem matar
                bichoDAO.salvar(bicho);
            }
        }
    }

    private void enviarDmMorte(Bicho bicho) {
        if (jda == null) return;

        jda.retrieveUserById(bicho.getDonoId()).queue(user -> {
            user.openPrivateChannel().queue(canal -> {
                canal.sendMessage(
                        "💀 **" + bicho.getNome() + " faleceu...**\n\n"
                        + "Seu bicho morreu por falta de cuidados enquanto você estava fora. 😢\n"
                        + "Use `/adotar` para adotar um novo companheiro."
                ).queue();
            });
        });
    }
}
