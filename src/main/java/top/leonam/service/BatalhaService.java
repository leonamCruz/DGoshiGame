package top.leonam.service;

import top.leonam.dao.BatalhaDAO;
import top.leonam.dao.BichoDAO;
import top.leonam.dao.DonoDAO;
import top.leonam.model.BatalhaAtiva;
import top.leonam.model.Bicho;
import top.leonam.model.Dono;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Gerencia desafios e batalhas entre bichos estilo Pokémon.
 *
 * Fluxo:
 *   1. desafiar()       — desafiante envia /atacar @player
 *   2. aceitarDesafio() — desafiado clica em "Aceitar"
 *   3. atacar()         — jogador do turno ataca (máx 5 min por turno)
 *   4. desistir()       — jogador desiste, o outro ganha
 *   5. Batalha encerra quando um bicho chega a vida=0
 */
public class BatalhaService {
    private static final double CHANCE_ERRAR          = 0.20;
    private static final double CHANCE_CRITICO        = 0.10;
    private static final double MULTIPLICADOR_CRITICO = 1.5;
    private static final long   MOEDAS_VITORIA        = 150;
    private static final long   XP_VITORIA            = 300;
    /** Tempo máximo por turno sem atacar antes de cancelar automaticamente. */
    private static final long   TIMEOUT_TURNO_MINUTOS = 5;

    private final BatalhaDAO batalhaDAO = new BatalhaDAO();
    private final BichoDAO   bichoDAO   = new BichoDAO();
    private final DonoDAO    donoDAO    = new DonoDAO();
    private final Random     random     = new Random();

    // Futures dos timeouts de turno, por batalhaId
    private final Map<Long, ScheduledFuture<?>> timeoutsTurno = new ConcurrentHashMap<>();
    private final ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = Thread.ofVirtual().unstarted(r);
        t.setName("batalha-timeout");
        return t;
    });

    /**
     * Inicia um desafio. Retorna a mensagem que deve ser postada no canal com botões
     * "Aceitar" e "Recusar".
     *
     * @param desafianteId  ID Discord do desafiante
     * @param desafiadoId   ID Discord do desafiado
     * @param canalId       ID do canal onde a batalha ocorre
     */
    public ResultadoDesafio desafiar(long desafianteId, long desafiadoId, long canalId) {
        if (desafianteId == desafiadoId) {
            return ResultadoDesafio.erro("Você não pode desafiar a si mesmo!");
        }

        var bichoDesafiante = bichoDAO.buscarPorDonoId(desafianteId);
        if (bichoDesafiante == null) {
            return ResultadoDesafio.erro("Você não tem um bicho vivo para batalhar!");
        }

        var bichoDesafiado = bichoDAO.buscarPorDonoId(desafiadoId);
        if (bichoDesafiado == null) {
            return ResultadoDesafio.erro("O jogador desafiado não tem um bicho vivo!");
        }

        if (batalhaDAO.buscarPorJogador(desafianteId) != null) {
            return ResultadoDesafio.erro("Você já está em uma batalha!");
        }
        if (batalhaDAO.buscarPorJogador(desafiadoId) != null) {
            return ResultadoDesafio.erro("O jogador desafiado já está em batalha!");
        }

        // Grava o desafio pendente — turno começa com o desafiante mas só depois do aceite
        var batalha = new BatalhaAtiva();
        batalha.setDesafianteId(desafianteId);
        batalha.setDesafiadoId(desafiadoId);
        batalha.setCanalId(canalId);
        batalha.setTurnoDe(desafianteId);
        batalha.setCriadoEm(OffsetDateTime.now());
        batalhaDAO.salvar(batalha);

        return ResultadoDesafio.desafio(batalha.getId(),
                "⚔️ **Desafio de batalha!**\n"
                + "<@" + desafianteId + "> desafiou <@" + desafiadoId + "> para uma batalha!\n"
                + "🐾 **" + bichoDesafiante.getNome() + "** (" + bichoDesafiante.getEstagio() + ")"
                + "  vs  "
                + "**" + bichoDesafiado.getNome() + "** (" + bichoDesafiado.getEstagio() + ")\n\n"
                + "<@" + desafiadoId + ">, você aceita?");
    }

    /**
     * Desafiado recusa. Remove a batalha pendente.
     */
    public String recusarDesafio(long batalhaId, long desafiadoId) {
        var batalha = batalhaDAO.buscarPorJogador(desafiadoId);
        if (batalha == null || batalha.getId() != batalhaId) {
            return "Nenhum desafio pendente encontrado.";
        }
        batalhaDAO.deletar(batalhaId);
        return "❌ <@" + desafiadoId + "> recusou o desafio. Galinha! 🐔";
    }

    /**
     * Desafiado aceita. Batalha inicia: turno do desafiante primeiro.
     * Agenda timeout de 5 minutos — se ninguém atacar, a batalha é cancelada.
     *
     * @param notificarTimeout  callback chamado com (atacanteId, oponenteId) quando o tempo esgotar
     */
    public String aceitarDesafio(long batalhaId, long desafiadoId, BiConsumer<Long, Long> notificarTimeout) {
        var batalha = batalhaDAO.buscarPorJogador(desafiadoId);
        if (batalha == null || batalha.getId() != batalhaId) {
            return "Nenhum desafio pendente encontrado.";
        }

        batalhaDAO.marcarAceita(batalhaId);

        var bichoDesafiante = bichoDAO.buscarPorDonoId(batalha.getDesafianteId());
        var bichoDesafiado  = bichoDAO.buscarPorDonoId(desafiadoId);

        // Agenda timeout de turno
        agendarTimeoutTurno(batalhaId, batalha.getDesafianteId(), desafiadoId, notificarTimeout);

        return "✅ **A batalha começou!**\n\n"
             + construirStatus(bichoDesafiante, bichoDesafiado)
             + "\n⚔️ É o turno de <@" + batalha.getDesafianteId() + ">! Use `/atacar` para atacar."
             + "\n⏱️ Você tem **" + TIMEOUT_TURNO_MINUTOS + " minutos** por turno.";
    }

    /** Versão sem callback de timeout — usada nos testes. */
    public String aceitarDesafio(long batalhaId, long desafiadoId) {
        return aceitarDesafio(batalhaId, desafiadoId, (a, b) -> {});
    }

    /**
     * Jogador desiste da batalha. O oponente ganha as recompensas.
     * O bicho do desistente NÃO morre — apenas o oponente é recompensado.
     */
    public String desistir(long jogadorId) {
        var batalha = batalhaDAO.buscarPorJogador(jogadorId);
        if (batalha == null) {
            return "Você não está em uma batalha!";
        }

        long oponenteId = (jogadorId == batalha.getDesafianteId())
                ? batalha.getDesafiadoId()
                : batalha.getDesafianteId();

        cancelarTimeoutTurno(batalha.getId());
        var levelUpMsg = recompensarVencedor(oponenteId);
        batalhaDAO.deletar(batalha.getId());

        return "🏳️ <@" + jogadorId + "> desistiu da batalha!\n"
             + "🏆 <@" + oponenteId + "> vence por desistência! +"
             + MOEDAS_VITORIA + " moedas, +" + XP_VITORIA + " XP"
             + levelUpMsg;
    }

    /**
     * Executa um ataque do jogador do turno.
     * Só funciona se a batalha estiver aceita (não pendente).
     *
     * @param atacanteId       ID Discord de quem ataca
     * @param notificarMorte   callback chamado com donoId do perdedor quando um bicho morre
     * @param notificarTimeout callback chamado com (atacanteId, oponenteId) se o timeout expirar no próximo turno
     */
    public String atacar(long atacanteId, Consumer<Long> notificarMorte,
                         BiConsumer<Long, Long> notificarTimeout) {
        var batalha = batalhaDAO.buscarPorJogador(atacanteId);
        if (batalha == null) {
            return "Você não está em uma batalha!";
        }
        if (!batalha.isAceita()) {
            return "O seu desafio ainda não foi aceito! Aguarde o outro jogador responder.";
        }
        if (batalha.getTurnoDe() != atacanteId) {
            return "Não é o seu turno!";
        }

        // Cancela o timeout do turno anterior
        cancelarTimeoutTurno(batalha.getId());

        long defensoreId = (atacanteId == batalha.getDesafianteId())
                ? batalha.getDesafiadoId()
                : batalha.getDesafianteId();

        var bichoAtacante = bichoDAO.buscarPorDonoId(atacanteId);
        var bichoDefensor = bichoDAO.buscarPorDonoId(defensoreId);

        if (bichoAtacante == null || bichoDefensor == null) {
            batalhaDAO.deletar(batalha.getId());
            return "⚠️ Um dos bichos não foi encontrado. Batalha encerrada.";
        }

        var sb = new StringBuilder();

        // Rola o ataque
        boolean errou   = random.nextDouble() < CHANCE_ERRAR;
        boolean critico = !errou && random.nextDouble() < CHANCE_CRITICO;

        if (errou) {
            sb.append("💨 **").append(bichoAtacante.getNome()).append("** errou o ataque!\n");
        } else {
            int danoBase = bichoAtacante.calcularAtaqueBase();
            int dano = critico ? (int)(danoBase * MULTIPLICADOR_CRITICO) : danoBase;
            int novaVida = Math.max(Bicho.MIN_STATUS, bichoDefensor.getVida() - dano);
            bichoDefensor.setVida(novaVida);

            if (critico) {
                sb.append("💥 **Golpe crítico!** ");
            }
            sb.append("**").append(bichoAtacante.getNome()).append("** atacou **")
              .append(bichoDefensor.getNome()).append("** causando **").append(dano).append(" de dano**!\n");
        }

        sb.append("\n").append(construirStatus(
                atacanteId == batalha.getDesafianteId() ? bichoAtacante : bichoDefensor,
                atacanteId == batalha.getDesafianteId() ? bichoDefensor : bichoAtacante));

        // Checa morte
        if (bichoDefensor.getVida() <= Bicho.MIN_STATUS) {
            bichoDefensor.setMorto(true);
            bichoDAO.salvar(bichoDefensor);
            var levelUpMsg = recompensarVencedor(atacanteId);
            batalhaDAO.deletar(batalha.getId());

            sb.append("\n\n💀 **").append(bichoDefensor.getNome()).append("** foi derrotado!\n");
            sb.append("🏆 <@").append(atacanteId).append("> venceu a batalha! +")
              .append(MOEDAS_VITORIA).append(" moedas, +").append(XP_VITORIA).append(" XP");
            sb.append(levelUpMsg);

            notificarMorte.accept(defensoreId);

        } else {
            bichoDAO.salvar(bichoDefensor);
            batalhaDAO.atualizarTurno(batalha.getId(), defensoreId);

            // Agenda timeout para o próximo turno
            agendarTimeoutTurno(batalha.getId(), defensoreId, atacanteId, notificarTimeout);

            sb.append("\n⚔️ É o turno de <@").append(defensoreId)
              .append(">! Use `/atacar`. _(").append(TIMEOUT_TURNO_MINUTOS).append(" min)_");
        }

        return sb.toString();
    }

    /** Versão sem callbacks — usada nos testes. */
    public String atacar(long atacanteId, Consumer<Long> notificarMorte) {
        return atacar(atacanteId, notificarMorte, (a, b) -> {});
    }

    private void agendarTimeoutTurno(long batalhaId, long jogadorDoTurno, long oponente,
                                      BiConsumer<Long, Long> notificar) {
        var future = agendador.schedule(() -> {
            // Verifica se a batalha ainda existe (pode ter encerrado normalmente)
            var batalha = batalhaDAO.buscarPorJogador(jogadorDoTurno);
            if (batalha != null && batalha.getId() == batalhaId) {
                recompensarVencedor(oponente);
                batalhaDAO.deletar(batalhaId);                notificar.accept(jogadorDoTurno, oponente); // quem perdeu por timeout, quem ganhou
            }
            timeoutsTurno.remove(batalhaId);
        }, TIMEOUT_TURNO_MINUTOS, TimeUnit.MINUTES);

        timeoutsTurno.put(batalhaId, future);
    }

    private void cancelarTimeoutTurno(long batalhaId) {
        var future = timeoutsTurno.remove(batalhaId);
        if (future != null) future.cancel(false);
    }

    private String recompensarVencedor(long vencedorId) {
        var dono = donoDAO.buscarPorId(vencedorId);
        if (dono == null) return "";
        dono.adicionarMoedas(MOEDAS_VITORIA);
        dono.adicionarExperiencia(XP_VITORIA);
        donoDAO.salvar(dono);
        if (dono.subiuNivel()) {
            return "\n🌟 **LEVEL UP!** <@" + vencedorId + "> alcançou o nível " + dono.getNivel()
                 + "! +" + (200 * dono.getNivel()) + " moedas de bônus!";
        }
        return "";
    }

    private String construirStatus(Bicho esquerda, Bicho direita) {
        return String.format("```\n%-20s  %s\n❤️ %-3d  🍖 %-3d  🛁 %-3d    ❤️ %-3d  🍖 %-3d  🛁 %-3d\n```",
                esquerda.getNome() + " (" + esquerda.getEstagio() + ")",
                direita.getNome()  + " (" + direita.getEstagio()  + ")",
                esquerda.getVida(), esquerda.getFome(), esquerda.getLimpeza(),
                direita.getVida(),  direita.getFome(),  direita.getLimpeza());
    }

    /** DTO para o resultado do desafio (pode ser erro, pode precisar de ID para botões). */
    public record ResultadoDesafio(boolean sucesso, long batalhaId, String mensagem) {
        static ResultadoDesafio erro(String msg) {
            return new ResultadoDesafio(false, 0, msg);
        }
        static ResultadoDesafio desafio(long id, String msg) {
            return new ResultadoDesafio(true, id, msg);
        }
    }
}
