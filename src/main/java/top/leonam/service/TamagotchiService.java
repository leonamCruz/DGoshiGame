package top.leonam.service;

import top.leonam.dao.BichoDAO;
import top.leonam.dao.DonoDAO;
import top.leonam.dao.InventarioDAO;
import top.leonam.model.Bicho;
import top.leonam.model.Dono;
import top.leonam.model.ItemInventario;
import top.leonam.model.TipoItem;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TamagotchiService {
    private static final long MOEDAS_BRINCAR_ACERTO = 20;
    private static final long MOEDAS_BRINCAR_ERRO   = 5;
    private static final long XP_BRINCAR_ACERTO     = 30;
    private static final long XP_BRINCAR_ERRO       = 5;
    private static final int  TIMEOUT_BRINCAR_SEGS  = 15;

    private final DonoDAO       donoDAO       = new DonoDAO();
    private final BichoDAO      bichoDAO      = new BichoDAO();
    private final InventarioDAO inventarioDAO = new InventarioDAO();

    /**
     * Armazena o emoji correto do minijogo pendente por donoId.
     * Chave: donoId. Valor: emoji correto.
     * Acesso via getMinijogoPendente/removerMinijogoPendente/registrarMinijogo
     * para que o BotListener e o timeout possam coordenar sem estado duplicado.
     */
    private final Map<Long, String> minijogosPendentes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService agendador    = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = Thread.ofVirtual().unstarted(r);
        t.setName("minijogo-timeout");
        return t;
    });

    // ------------------------------------------------------------------ adotar

    public String adotar(long donoId, String nomeBicho, String tipo) {
        var dono = donoDAO.buscarPorId(donoId);
        if (dono == null) {
            dono = new Dono(donoId);
            donoDAO.salvar(dono);
        }

        if (bichoDAO.buscarPorDonoId(donoId) != null) {
            return "Você já tem um bicho! Use `/status` para vê-lo.";
        }

        var bicho = new Bicho();
        bicho.setDonoId(donoId);
        bicho.setNome(nomeBicho);
        bicho.setTipo(tipo);
        bicho.setEstagio("Ovo");
        bicho.setNascimento(OffsetDateTime.now());
        bicho.setUltimaInteracao(OffsetDateTime.now());
        bicho.setFome(100);
        bicho.setVida(100);
        bicho.setLimpeza(100);
        bicho.setFelicidade(100);

        bichoDAO.salvar(bicho);

        // Kit inicial
        inventarioDAO.salvarOuAtualizar(new ItemInventario(donoId, TipoItem.RACAO_COMUM, 3));

        return "🥚 **" + nomeBicho + "** foi adotado com sucesso! Cuide bem dele.\n"
             + "Kit inicial: 3x Ração Comum adicionadas ao inventário.";
    }

    // ----------------------------------------------------------------- status

    public String status(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho. Use `/adotar` para começar!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 **" + bicho.getNome() + "** morreu enquanto você verificava... 😢\nUse `/adotar` para começar de novo.";
        }
        bichoDAO.salvar(bicho);

        var dono = donoDAO.buscarPorId(donoId);

        return "🐾 **" + bicho.getNome() + "** (" + bicho.getTipo() + " — " + bicho.getEstagio() + ")\n"
             + barraStatus("❤️ Vida",       bicho.getVida())
             + barraStatus("🍖 Fome",       bicho.getFome())
             + barraStatus("🛁 Limpeza",    bicho.getLimpeza())
             + barraStatus("😄 Felicidade", bicho.getFelicidade())
             + "\n💰 Moedas: " + (dono != null ? dono.getMoedas() : "?")
             + "  |  ✨ Nível: " + (dono != null ? dono.getNivel() : "?");
    }

    // ---------------------------------------------------------------- alimentar

    public String alimentar(long donoId, TipoItem tipoItem) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        if (bicho.getFome() == Bicho.MAX_STATUS) {
            return "🍖 **" + bicho.getNome() + "** já está com a barriga cheia!";
        }

        var itens = inventarioDAO.listarPorDono(donoId);
        var itemEncontrado = itens.stream()
                .filter(i -> i.getTipoItem() == tipoItem && i.getQuantidade() > 0)
                .findFirst()
                .orElse(null);

        if (itemEncontrado == null) {
            return "Você não tem **" + tipoItem.getNome() + "** no inventário! Use `/loja` para comprar.";
        }

        var novaFome = Math.min(Bicho.MAX_STATUS, bicho.getFome() + tipoItem.getPoder());
        bicho.setFome(novaFome);

        // COMIDA_ESPECIAL também restaura vida
        if (tipoItem.getEfeito() == TipoItem.Efeito.FOME_E_VIDA) {
            bicho.setVida(Math.min(Bicho.MAX_STATUS, bicho.getVida() + 10));
        }
        // RACAO_GOURMET também dá felicidade
        if (tipoItem.getEfeito() == TipoItem.Efeito.FOME_E_FELICIDADE) {
            bicho.setFelicidade(Math.min(Bicho.MAX_STATUS, bicho.getFelicidade() + 10));
        }

        bicho.setUltimaInteracao(OffsetDateTime.now());
        itemEncontrado.remover(1);
        inventarioDAO.salvarOuAtualizar(itemEncontrado);
        bichoDAO.salvar(bicho);

        return "🍖 Você alimentou **" + bicho.getNome() + "** com " + tipoItem.getNome()
             + "!\n" + barraStatus("Fome", bicho.getFome());
    }

    // ------------------------------------------------------------------ banho

    public String darBanho(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        if (bicho.getLimpeza() == Bicho.MAX_STATUS) {
            return "🛁 **" + bicho.getNome() + "** já está limpinho!";
        }

        bicho.setLimpeza(Math.min(Bicho.MAX_STATUS, bicho.getLimpeza() + 40));
        bicho.setUltimaInteracao(OffsetDateTime.now());
        bichoDAO.salvar(bicho);

        return "🛁 **" + bicho.getNome() + "** tomou banho! "
             + barraStatus("Limpeza", bicho.getLimpeza());
    }

    // ----------------------------------------------------------------- medicar

    public String medicar(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        if (bicho.getVida() == Bicho.MAX_STATUS) {
            return "💊 **" + bicho.getNome() + "** está com saúde plena!";
        }

        var itens = inventarioDAO.listarPorDono(donoId);
        var medicamento = itens.stream()
                .filter(i -> i.getTipoItem() == TipoItem.MEDICAMENTO && i.getQuantidade() > 0)
                .findFirst()
                .orElse(null);

        if (medicamento == null) {
            return "Você não tem **Medicamento** no inventário! Use `/loja` para comprar.";
        }

        bicho.setVida(Math.min(Bicho.MAX_STATUS, bicho.getVida() + TipoItem.MEDICAMENTO.getPoder()));
        bicho.setUltimaInteracao(OffsetDateTime.now());
        medicamento.remover(1);
        inventarioDAO.salvarOuAtualizar(medicamento);
        bichoDAO.salvar(bicho);

        return "💊 **" + bicho.getNome() + "** foi medicado! "
             + barraStatus("Vida", bicho.getVida());
    }

    // ----------------------------------------------------------------- dormir

    public String dormir(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        // Dormir restaura vida moderadamente e melhora felicidade
        bicho.setVida(Math.min(Bicho.MAX_STATUS, bicho.getVida() + 15));
        bicho.setFelicidade(Math.min(Bicho.MAX_STATUS, bicho.getFelicidade() + 10));
        bicho.setUltimaInteracao(OffsetDateTime.now());
        bichoDAO.salvar(bicho);

        return "😴 **" + bicho.getNome() + "** dormiu um pouco e se recuperou!\n"
             + barraStatus("Vida",       bicho.getVida())
             + barraStatus("Felicidade", bicho.getFelicidade());
    }

    // ------------------------------------------------------------- ver bicho alheio

    public String verBichoDeOutro(long donoIdAlvo) {
        var bicho = buscarBichoVivo(donoIdAlvo);
        if (bicho == null) return "Este jogador não tem um bicho vivo.";

        bicho.atualizarStatusPeloTempo();
        bichoDAO.salvar(bicho);

        return "👀 **" + bicho.getNome() + "** (" + bicho.getTipo() + " — " + bicho.getEstagio() + ")\n"
             + barraStatus("❤️ Vida",       bicho.getVida())
             + barraStatus("🍖 Fome",       bicho.getFome())
             + barraStatus("🛁 Limpeza",    bicho.getLimpeza())
             + barraStatus("😄 Felicidade", bicho.getFelicidade());
    }

    // ----------------------------------------------------------------- brincar

    /**
     * Fase 1 do minijogo: gera um desafio de reação e agenda um timeout de 15 segundos.
     * Se o player não clicar a tempo, o mapa é limpo automaticamente.
     * O callback onTimeout é chamado pelo agendador para notificar o canal.
     */
    public MinijogoDesafio iniciarBrincar(long donoId, Consumer<Long> onTimeout) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return MinijogoDesafio.erro("Você não tem um bicho vivo!");

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return MinijogoDesafio.erro("💀 Seu bicho morreu...");
        }

        // 6 emojis disponíveis, 5 mostrados como botões, 1 é o correto
        String[] opcoes  = {"🌟", "🎾", "🧸", "🎪", "🎠", "🏆"};
        String correto   = opcoes[(int)(Math.random() * opcoes.length)];

        minijogosPendentes.put(donoId, correto);

        // Agenda limpeza automática após timeout
        agendador.schedule(() -> {
            if (minijogosPendentes.remove(donoId) != null) {
                // Ainda estava pendente — deu timeout
                onTimeout.accept(donoId);
            }
        }, TIMEOUT_BRINCAR_SEGS, TimeUnit.SECONDS);

        String mensagem = "🎮 **Minijogo!** Qual emoji **" + bicho.getNome() + "** quer brincar?\n"
                        + "Clique no emoji correto em **" + TIMEOUT_BRINCAR_SEGS + " segundos**!";

        return MinijogoDesafio.ok(correto, mensagem);
    }

    /** Versão sem callback de timeout — usada nos testes. */
    public MinijogoDesafio iniciarBrincar(long donoId) {
        return iniciarBrincar(donoId, id -> {});
    }

    public String getMinijogoPendente(long donoId) {
        return minijogosPendentes.get(donoId);
    }

    public String removerMinijogoPendente(long donoId) {
        return minijogosPendentes.remove(donoId);
    }

    /**
     * Fase 2: o player clicou no emoji. Verifica se acertou e dá a recompensa.
     */
    public String resolverBrincar(long donoId, String emojiClicado, String emojiCorreto, boolean timeoutOuErro) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        var dono = donoDAO.buscarPorId(donoId);
        if (dono == null) return "Jogador não encontrado.";

        boolean acertou = !timeoutOuErro && emojiClicado.equals(emojiCorreto);

        if (acertou) {
            bicho.setFelicidade(Math.min(Bicho.MAX_STATUS, bicho.getFelicidade() + 5));
            bicho.setUltimaInteracao(OffsetDateTime.now());
            bichoDAO.salvar(bicho);
            dono.adicionarMoedas(MOEDAS_BRINCAR_ACERTO);
            dono.adicionarExperiencia(XP_BRINCAR_ACERTO);
            donoDAO.salvar(dono);
            var msg = "🎉 **Acertou!** **" + bicho.getNome() + "** adorou! +20 moedas, +30 XP";
            if (dono.subiuNivel()) {
                msg += "\n\n🌟 **LEVEL UP!** Você alcançou o nível " + dono.getNivel()
                     + "! +" + (200 * dono.getNivel()) + " moedas de bônus!";
            }
            return msg;
        } else {
            bicho.setUltimaInteracao(OffsetDateTime.now());
            bichoDAO.salvar(bicho);
            dono.adicionarMoedas(MOEDAS_BRINCAR_ERRO);
            dono.adicionarExperiencia(XP_BRINCAR_ERRO);
            donoDAO.salvar(dono);
            var msg = "😅 **Errou!** Mas tudo bem, **" + bicho.getNome() + "** ainda gostou um pouco. +5 moedas, +5 XP";
            if (dono.subiuNivel()) {
                msg += "\n\n🌟 **LEVEL UP!** Você alcançou o nível " + dono.getNivel()
                     + "! +" + (200 * dono.getNivel()) + " moedas de bônus!";
            }
            return msg;
        }
    }

    // ------------------------------------------------------------------ usar brinquedo

    public String usarBrinquedo(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        if (bicho.getFelicidade() == Bicho.MAX_STATUS) {
            return "🧸 **" + bicho.getNome() + "** já está super feliz!";
        }

        var itens = inventarioDAO.listarPorDono(donoId);
        var brinquedo = itens.stream()
                .filter(i -> i.getTipoItem() == TipoItem.BRINQUEDO && i.getQuantidade() > 0)
                .findFirst()
                .orElse(null);

        if (brinquedo == null) {
            return "Você não tem **Brinquedo** no inventário! Use `/loja` para comprar.";
        }

        bicho.setFelicidade(Math.min(Bicho.MAX_STATUS, bicho.getFelicidade() + TipoItem.BRINQUEDO.getPoder()));
        bicho.setUltimaInteracao(OffsetDateTime.now());
        brinquedo.remover(1);
        inventarioDAO.salvarOuAtualizar(brinquedo);
        bichoDAO.salvar(bicho);

        return "🧸 **" + bicho.getNome() + "** brincou com o brinquedo e está mais feliz! "
             + barraStatus("Felicidade", bicho.getFelicidade());
    }

    // ------------------------------------------------------------------ vacinar

    public String vacinar(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        if (bicho.getLimpeza() == Bicho.MAX_STATUS) {
            return "💉 **" + bicho.getNome() + "** já está vacinado e limpo!";
        }

        var itens = inventarioDAO.listarPorDono(donoId);
        var vacina = itens.stream()
                .filter(i -> i.getTipoItem() == TipoItem.VACINA && i.getQuantidade() > 0)
                .findFirst()
                .orElse(null);

        if (vacina == null) {
            return "Você não tem **Vacina** no inventário! Use `/loja` para comprar.";
        }

        bicho.setLimpeza(Math.min(Bicho.MAX_STATUS, bicho.getLimpeza() + TipoItem.VACINA.getPoder()));
        bicho.setUltimaInteracao(OffsetDateTime.now());
        vacina.remover(1);
        inventarioDAO.salvarOuAtualizar(vacina);
        bichoDAO.salvar(bicho);

        return "💉 **" + bicho.getNome() + "** foi vacinado! "
             + barraStatus("Limpeza", bicho.getLimpeza());
    }

    // ------------------------------------------------------------------ treinar

    private static final long COOLDOWN_TREINO_HORAS = 1;
    private static final int  BONUS_TREINO_POR_SESSAO = 2;

    public String treinar(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        // Cooldown de 1 hora
        if (bicho.getUltimoTreino() != null) {
            long minutosDesde = Duration.between(bicho.getUltimoTreino(), OffsetDateTime.now()).toMinutes();
            if (minutosDesde < 60) {
                long minRestantes = 60 - minutosDesde;
                return "⏳ **" + bicho.getNome() + "** ainda está recuperando do último treino! Aguarde **"
                     + minRestantes + " min**.";
            }
        }

        var dono = donoDAO.buscarPorId(donoId);
        if (dono == null) return "Jogador não encontrado.";

        long custo = 50 + (long) dono.getNivel() * 25;
        if (dono.getMoedas() < custo) {
            return "Você precisa de **" + custo + " moedas** para treinar! Tem: " + dono.getMoedas() + ".";
        }

        dono.removerMoedas(custo);
        bicho.setBonusAtaqueTreino(bicho.getBonusAtaqueTreino() + BONUS_TREINO_POR_SESSAO);
        bicho.setUltimoTreino(OffsetDateTime.now());
        bicho.setUltimaInteracao(OffsetDateTime.now());
        donoDAO.salvar(dono);
        bichoDAO.salvar(bicho);

        return "💪 **" + bicho.getNome() + "** treinou duro! +2 de ataque permanente "
             + "(total: +" + bicho.getBonusAtaqueTreino() + ").\n"
             + "Custo: " + custo + " moedas. Saldo: " + dono.getMoedas() + " moedas.\n"
             + "⏱️ Próximo treino disponível em 1 hora.";
    }

    // ------------------------------------------------------------------ girar (caça-níqueis)

    private static final long CUSTO_GIRAR = 50;
    private static final String[] SIMBOLOS_GIRAR = {"🍒", "🍋", "🔔", "💎", "7️⃣"};
    private static final long[] PREMIOS_GIRAR = {200, 300, 400, 500, 1000};
    private static final long PREMIO_2_IGUAIS = 75;

    public String girar(long donoId) {
        var dono = donoDAO.buscarPorId(donoId);
        if (dono == null) return "Você ainda não tem um bicho! Use `/adotar` primeiro.";

        if (dono.getMoedas() < CUSTO_GIRAR) {
            return "Você precisa de **" + CUSTO_GIRAR + " moedas** para girar! Tem: " + dono.getMoedas() + ".";
        }

        dono.removerMoedas(CUSTO_GIRAR);

        int s1 = (int)(Math.random() * SIMBOLOS_GIRAR.length);
        int s2 = (int)(Math.random() * SIMBOLOS_GIRAR.length);
        int s3 = (int)(Math.random() * SIMBOLOS_GIRAR.length);

        long ganho = 0;
        String msg;

        if (s1 == s2 && s2 == s3) {
            ganho = PREMIOS_GIRAR[s1];
            msg = "🎉 **JACKPOT!** 3x " + SIMBOLOS_GIRAR[s1] + " = **+" + ganho + " moedas!**";
        } else if (s1 == s2 || s2 == s3 || s1 == s3) {
            ganho = PREMIO_2_IGUAIS;
            msg = "🤝 Dois iguais! +" + ganho + " moedas.";
        } else {
            msg = "💸 Nenhum match. Você perdeu " + CUSTO_GIRAR + " moedas.";
        }

        if (ganho > 0) dono.adicionarMoedas(ganho);
        donoDAO.salvar(dono);

        return "🎰 **Caça-Níqueis**\n" + SIMBOLOS_GIRAR[s1] + " | " + SIMBOLOS_GIRAR[s2] + " | " + SIMBOLOS_GIRAR[s3]
             + "\n" + msg + "\n💰 Saldo: " + dono.getMoedas() + " moedas.";
    }

    // ------------------------------------------------------------------ presentear

    public String presentear(long origemId, long destinoId, long moedas) {
        if (origemId == destinoId) return "Você não pode presentear a si mesmo!";
        if (moedas <= 0) return "A quantidade deve ser positiva.";

        var origem = donoDAO.buscarPorId(origemId);
        if (origem == null) return "Você ainda não tem um bicho! Use `/adotar` primeiro.";

        if (origem.getMoedas() < moedas) {
            return "Você não tem moedas suficientes! Tem: " + origem.getMoedas() + ", precisa: " + moedas + ".";
        }

        var destino = donoDAO.buscarPorId(destinoId);
        if (destino == null) return "O jogador de destino ainda não tem um bicho.";

        origem.removerMoedas(moedas);
        destino.adicionarMoedas(moedas);
        donoDAO.salvar(origem);
        donoDAO.salvar(destino);

        return "🎁 Você presenteou <@" + destinoId + "> com **" + moedas + " moedas**!\n"
             + "Seu saldo: " + origem.getMoedas() + " moedas.";
    }

    // ------------------------------------------------------------------ usar super brinquedo

    public String usarSuperBrinquedo(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        if (bicho.getFelicidade() == Bicho.MAX_STATUS) {
            return "✨ **" + bicho.getNome() + "** já está super feliz!";
        }

        var itens = inventarioDAO.listarPorDono(donoId);
        var brinquedo = itens.stream()
                .filter(i -> i.getTipoItem() == TipoItem.SUPER_BRINQUEDO && i.getQuantidade() > 0)
                .findFirst()
                .orElse(null);

        if (brinquedo == null) {
            return "Você não tem **Super Brinquedo** no inventário! Use `/loja` para comprar.";
        }

        bicho.setFelicidade(Math.min(Bicho.MAX_STATUS, bicho.getFelicidade() + TipoItem.SUPER_BRINQUEDO.getPoder()));
        bicho.setUltimaInteracao(OffsetDateTime.now());
        brinquedo.remover(1);
        inventarioDAO.salvarOuAtualizar(brinquedo);
        bichoDAO.salvar(bicho);

        return "✨ **" + bicho.getNome() + "** brincou com o Super Brinquedo e ficou MUITO feliz! "
             + barraStatus("Felicidade", bicho.getFelicidade());
    }

    // ------------------------------------------------------------------ usar poção de cura total

    public String usarPoçãoCuraTotal(long donoId) {
        var bicho = buscarBichoVivo(donoId);
        if (bicho == null) return "Você não tem um bicho vivo!";

        bicho.atualizarStatusPeloTempo();
        if (bicho.isMorto()) {
            bichoDAO.marcarMorto(bicho.getId());
            return "💀 Seu bicho morreu...";
        }

        var itens = inventarioDAO.listarPorDono(donoId);
        var pocao = itens.stream()
                .filter(i -> i.getTipoItem() == TipoItem.POCAO_CURA_TOTAL && i.getQuantidade() > 0)
                .findFirst()
                .orElse(null);

        if (pocao == null) {
            return "Você não tem **Poção de Cura Total** no inventário! Use `/loja` para comprar.";
        }

        bicho.setVida(Math.min(Bicho.MAX_STATUS, bicho.getVida() + 50));
        bicho.setFome(Math.min(Bicho.MAX_STATUS, bicho.getFome() + 50));
        bicho.setLimpeza(Math.min(Bicho.MAX_STATUS, bicho.getLimpeza() + 50));
        bicho.setFelicidade(Math.min(Bicho.MAX_STATUS, bicho.getFelicidade() + 50));
        bicho.setUltimaInteracao(OffsetDateTime.now());
        pocao.remover(1);
        inventarioDAO.salvarOuAtualizar(pocao);
        bichoDAO.salvar(bicho);

        return "🧪 **" + bicho.getNome() + "** tomou a Poção de Cura Total e está revigorado!\n"
             + barraStatus("❤️ Vida",       bicho.getVida())
             + barraStatus("🍖 Fome",       bicho.getFome())
             + barraStatus("🛁 Limpeza",    bicho.getLimpeza())
             + barraStatus("😄 Felicidade", bicho.getFelicidade());
    }

    // ------------------------------------------------------------------ daily

    public String resgatarDaily(long donoId) {
        var dono = donoDAO.buscarPorId(donoId);
        if (dono == null) dono = new Dono(donoId);

        if (dono.getUltimoDaily() != null) {
            var horasPassadas = Duration.between(dono.getUltimoDaily(), OffsetDateTime.now()).toHours();
            if (horasPassadas < 24) {
                var horasRestantes = 24 - horasPassadas;
                return "⏳ Você já resgatou o diário hoje! Volte em **" + horasRestantes + "h**.";
            }
        }

        dono.adicionarMoedas(100);
        dono.setUltimoDaily(OffsetDateTime.now());
        donoDAO.salvar(dono);

        return "🎁 Recompensa diária resgatada! **+100 moedas**. Saldo atual: " + dono.getMoedas();
    }

    // ----------------------------------------------------------------- helpers

    private Bicho buscarBichoVivo(long donoId) {
        return bichoDAO.buscarPorDonoId(donoId);
    }

    private String barraStatus(String label, int valor) {
        int blocos     = valor / 10;
        String barra   = "█".repeat(blocos) + "░".repeat(10 - blocos);
        return label + ": `" + barra + "` " + valor + "/100\n";
    }

    // ------------------------------------------------------------------ DTOs

    public record MinijogoDesafio(boolean sucesso, String emojiCorreto, String mensagem) {
        static MinijogoDesafio erro(String msg) {
            return new MinijogoDesafio(false, null, msg);
        }
        static MinijogoDesafio ok(String emoji, String msg) {
            return new MinijogoDesafio(true, emoji, msg);
        }
    }
}
