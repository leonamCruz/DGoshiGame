package top.leonam.escutador;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import top.leonam.dao.BatalhaDAO;
import top.leonam.model.TipoItem;
import top.leonam.service.BatalhaService;
import top.leonam.service.LojaService;
import top.leonam.service.RankingService;
import top.leonam.service.TamagotchiService;

import java.awt.Color;

public class BotListener extends ListenerAdapter {
    private final TamagotchiService tamagotchiService = new TamagotchiService();
    private final BatalhaService    batalhaService    = new BatalhaService();
    private final LojaService       lojaService       = new LojaService();
    private final RankingService    rankingService    = new RankingService();
    private final BatalhaDAO        batalhaDAO        = new BatalhaDAO();

    // Chamado assim que o JDA estiver pronto — limpa desafios pendentes órfãos
    public void onStartup() {
        batalhaDAO.limparPendentesAntigos();
    }

    // ----------------------------------------------------------------- slash commands

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            handleSlash(event);
        } catch (Exception e) {
            System.err.println("[BotListener] Erro no comando " + event.getName() + ": " + e.getMessage());
            if (!event.isAcknowledged()) {
                event.reply("❌ Ocorreu um erro interno. Tente novamente.").setEphemeral(true).queue();
            }
        }
    }

    // ----------------------------------------------------------------- menção ao bot

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getAuthor().isBot()) return;

        // Responde com o guia de ajuda quando o bot é mencionado
        if (event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser())) {
            event.getMessage().replyEmbeds(construirEmbedAjuda()).queue();
        }
    }

    private void handleSlash(SlashCommandInteractionEvent event) {
        long donoId = event.getUser().getIdLong();

        switch (event.getName()) {

            case "adotar" -> {
                var nome = event.getOption("nome").getAsString();
                var tipo = event.getOption("tipo").getAsString();
                event.reply(tamagotchiService.adotar(donoId, nome, tipo)).queue();
            }

            case "status" -> {
                var alvo = event.getOption("jogador");
                if (alvo != null) {
                    event.replyEmbeds(construirEmbed(alvo.getAsUser().getIdLong(), alvo.getAsUser().getName())).queue();
                } else {
                    event.replyEmbeds(construirEmbed(donoId, event.getUser().getName())).queue();
                }
            }

            case "alimentar" -> {
                var opcao = event.getOption("item");
                TipoItem tipoItem = TipoItem.RACAO_COMUM;
                if (opcao != null) {
                    try {
                        tipoItem = TipoItem.valueOf(opcao.getAsString());
                    } catch (IllegalArgumentException ignored) {
                        event.reply("Item inválido.").setEphemeral(true).queue();
                        return;
                    }
                }
                event.reply(tamagotchiService.alimentar(donoId, tipoItem)).queue();
            }

            case "banho"   -> event.reply(tamagotchiService.darBanho(donoId)).queue();
            case "medicar" -> event.reply(tamagotchiService.medicar(donoId)).queue();
            case "vacinar" -> event.reply(tamagotchiService.vacinar(donoId)).queue();
            case "dormir"  -> event.reply(tamagotchiService.dormir(donoId)).queue();

            case "brincar" -> {
                var desafio = tamagotchiService.iniciarBrincar(donoId, timeoutDonoId -> {
                    // Timeout: tenta editar a mensagem original para indicar expiração
                    event.getHook().editOriginal("⏰ Tempo esgotado! **" + event.getUser().getName()
                            + "** não clicou a tempo. +5 moedas e +5 XP pela tentativa.")
                            .queue(null, err -> {}); // ignora se já foi deletada
                    tamagotchiService.resolverBrincar(timeoutDonoId, "", "", true);
                });

                if (!desafio.sucesso()) {
                    event.reply(desafio.mensagem()).setEphemeral(true).queue();
                    return;
                }

                // Precisa deferReply para que o timeout possa editar depois
                event.reply(desafio.mensagem())
                        .addComponents(ActionRow.of(
                                Button.primary("brincar_emoji:" + donoId + ":🌟", "🌟"),
                                Button.primary("brincar_emoji:" + donoId + ":🎾", "🎾"),
                                Button.primary("brincar_emoji:" + donoId + ":🧸", "🧸"),
                                Button.primary("brincar_emoji:" + donoId + ":🎪", "🎪"),
                                Button.primary("brincar_emoji:" + donoId + ":🎠", "🎠")
                        ))
                        .queue();
            }

            case "daily"     -> event.reply(tamagotchiService.resgatarDaily(donoId)).queue();
            case "loja"      -> event.reply(lojaService.listarLoja()).queue();
            case "inventario"-> event.reply(lojaService.listarInventario(donoId)).queue();

            case "comprar" -> {
                var item = event.getOption("item").getAsString();
                event.reply(lojaService.comprar(donoId, item)).queue();
            }

            case "desistir" -> event.reply(batalhaService.desistir(donoId)).queue();

            case "treinar" -> event.reply(tamagotchiService.treinar(donoId)).queue();
            case "girar"   -> event.reply(tamagotchiService.girar(donoId)).queue();

            case "presentear" -> {
                var alvo = event.getOption("jogador");
                var moedasOp = event.getOption("moedas");
                if (alvo == null || moedasOp == null) {
                    event.reply("Use `/presentear jogador:@alguém moedas:quantidade`.").setEphemeral(true).queue();
                    return;
                }
                long alvoId = alvo.getAsUser().getIdLong();
                long moedas = moedasOp.getAsLong();
                event.reply(tamagotchiService.presentear(donoId, alvoId, moedas)).queue();
            }

            case "usar" -> {
                var item = event.getOption("item");
                if (item == null) {
                    event.reply("Escolha um item para usar.").setEphemeral(true).queue();
                    return;
                }
                String itemStr = item.getAsString();
                String resultado = switch (itemStr) {
                    case "brinquedo"       -> tamagotchiService.usarBrinquedo(donoId);
                    case "super_brinquedo" -> tamagotchiService.usarSuperBrinquedo(donoId);
                    case "pocao"           -> tamagotchiService.usarPoçãoCuraTotal(donoId);
                    default -> "Item inválido. Use `/usar` e escolha uma opção do menu.";
                };
                event.reply(resultado).queue();
            }

            case "ranking" -> {
                var categoriaOp = event.getOption("categoria");
                String categoria = categoriaOp != null ? categoriaOp.getAsString() : "nivel";
                var resultado = rankingService.buscarRanking(categoria);
                if (resultado.erro()) {
                    event.reply(resultado.texto()).setEphemeral(true).queue();
                } else {
                    event.reply(resultado.texto()).queue();
                }
            }

            case "ajuda" -> event.replyEmbeds(construirEmbedAjuda()).queue();

            case "atacar" -> {
                var alvoOpcao = event.getOption("jogador");
                if (alvoOpcao == null) {
                    // Ataque durante batalha em curso
                    var jda = event.getJDA();
                    long canalId = event.getChannel().getIdLong();
                    String nomeAtacante = event.getUser().getName();

                    String resultado = batalhaService.atacar(donoId, perdedorId -> {
                        // Envia DM ao perdedor
                        jda.retrieveUserById(perdedorId).queue(user ->
                            user.openPrivateChannel().queue(ch ->
                                ch.sendMessage("💀 Seu bicho foi derrotado em batalha por **"
                                        + nomeAtacante + "**! Use `/adotar` para começar de novo.").queue()
                            )
                        );
                    }, (timeoutPerdedorId, timeoutVencedorId) -> {
                        // Timeout de turno: notifica no canal
                        var canal = jda.getTextChannelById(canalId);
                        if (canal != null) {
                            canal.sendMessage("⏰ <@" + timeoutPerdedorId + "> não atacou a tempo! <@"
                                    + timeoutVencedorId + "> vence por timeout! 🏆 +150 moedas, +300 XP").queue();
                        }
                    });
                    event.reply(resultado).queue();
                } else {
                    long desafiadoId = alvoOpcao.getAsUser().getIdLong();
                    long canalId     = event.getChannel().getIdLong();
                    var  resultado   = batalhaService.desafiar(donoId, desafiadoId, canalId);

                    if (!resultado.sucesso()) {
                        event.reply(resultado.mensagem()).setEphemeral(true).queue();
                        return;
                    }

                    event.reply(resultado.mensagem())
                            .addComponents(ActionRow.of(
                                    Button.success("batalha_aceitar:" + resultado.batalhaId() + ":" + desafiadoId, "✅ Aceitar"),
                                    Button.danger("batalha_recusar:" + resultado.batalhaId() + ":" + desafiadoId, "❌ Recusar")
                            ))
                            .queue();
                }
            }

            default -> event.reply("Comando desconhecido.").setEphemeral(true).queue();
        }
    }

    // ----------------------------------------------------------------- button interactions

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        try {
            handleButton(event);
        } catch (Exception e) {
            System.err.println("[BotListener] Erro no botão " + event.getComponentId() + ": " + e.getMessage());
            if (!event.isAcknowledged()) {
                event.reply("❌ Erro interno.").setEphemeral(true).queue();
            }
        }
    }

    private void handleButton(ButtonInteractionEvent event) {
        var componentId = event.getComponentId();
        long clicadorId = event.getUser().getIdLong();

        if (componentId.startsWith("brincar_emoji:")) {
            // formato: brincar_emoji:<donoId>:<emoji>
            var    partes  = componentId.split(":");
            long   donoId  = Long.parseLong(partes[1]);
            String emoji   = partes[2];

            if (clicadorId != donoId) {
                event.reply("Este não é o seu jogo!").setEphemeral(true).queue();
                return;
            }

            String emojiCorreto = tamagotchiService.removerMinijogoPendente(donoId);
            if (emojiCorreto == null) {
                // Timeout já disparou e limpou o mapa
                event.reply("⏰ Tempo esgotado! Você demorou demais.").setEphemeral(true).queue();
                return;
            }

            String resultado = tamagotchiService.resolverBrincar(donoId, emoji, emojiCorreto, false);
            event.reply(resultado).queue();
        }

        else if (componentId.startsWith("batalha_aceitar:")) {
            // formato: batalha_aceitar:<batalhaId>:<desafiadoId>
            var  partes      = componentId.split(":");
            long batalhaId   = Long.parseLong(partes[1]);
            long desafiadoId = Long.parseLong(partes[2]);

            if (clicadorId != desafiadoId) {
                event.reply("Você não foi desafiado nesta batalha!").setEphemeral(true).queue();
                return;
            }

            var jda = event.getJDA();
            long canalId = event.getChannel().getIdLong();

            event.reply(batalhaService.aceitarDesafio(batalhaId, desafiadoId, (timeoutPerdedorId, timeoutVencedorId) -> {
                var canal = jda.getTextChannelById(canalId);
                if (canal != null) {
                    canal.sendMessage("⏰ <@" + timeoutPerdedorId + "> não atacou a tempo! <@"
                            + timeoutVencedorId + "> vence por timeout! 🏆 +150 moedas, +300 XP").queue();
                }
            })).queue();
        }

        else if (componentId.startsWith("batalha_recusar:")) {
            var  partes      = componentId.split(":");
            long batalhaId   = Long.parseLong(partes[1]);
            long desafiadoId = Long.parseLong(partes[2]);

            if (clicadorId != desafiadoId) {
                event.reply("Você não foi desafiado nesta batalha!").setEphemeral(true).queue();
                return;
            }

            event.reply(batalhaService.recusarDesafio(batalhaId, desafiadoId)).queue();
        }
    }

    // ----------------------------------------------------------------- embed de ajuda

    private net.dv8tion.jda.api.entities.MessageEmbed construirEmbedAjuda() {
        var embed = new EmbedBuilder();
        embed.setTitle("🐾 DGoshi — Como Jogar");
        embed.setColor(new Color(130, 80, 200));
        embed.setDescription("Adote um bichinho virtual e cuide dele! Ele evolui, batalha e pode até morrer se você descuidar. Cada jogador tem **1 bicho por vez**.");
        embed.setFooter("DGoshi • Tamagotchi Bot • Marque o bot ou use /ajuda quando precisar!");

        embed.addField("🌱 Começando",
                "`/adotar` — escolha nome e espécie. Ganha 3 rações grátis.\n" +
                "`/status` — veja a saúde do seu bicho (ou de outro: `/status jogador:@alguém`)",
                false);

        embed.addField("🍖 Cuidando do bicho",
                "`/alimentar` — enche a fome (precisa de comida no inventário)\n" +
                "`/banho` — limpa o bicho (+40 limpeza, grátis)\n" +
                "`/medicar` — cura o bicho (+50 vida, precisa de Medicamento)\n" +
                "`/vacinar` — protege contra doença (+30 limpeza, precisa de Vacina)\n" +
                "`/dormir` — recupera vida e felicidade\n" +
                "`/usar` — usa Brinquedo, Super Brinquedo ou Poção de Cura Total",
                false);

        embed.addField("🎮 Entretenimento & Moedas",
                "`/brincar` — minijogo de reação: acerto = +20 moedas e +30 XP\n" +
                "`/girar` — caça-níqueis: custa 50 moedas, pode ganhar até 1000\n" +
                "`/daily` — recompensa diária de +100 moedas (1x a cada 24h)",
                false);

        embed.addField("💪 Progressão",
                "`/treinar` — +2 de ataque permanente por sessão (custa moedas, 1h cooldown)\n" +
                "`/loja` — veja os itens disponíveis\n" +
                "`/comprar` — compre itens com suas moedas\n" +
                "`/inventario` — veja o que você tem\n" +
                "🌟 Ao subir de nível, você ganha **200 × nível** em moedas!",
                false);

        embed.addField("⚔️ Batalha (estilo Pokémon)",
                "`/atacar jogador:@alguém` — desafia outro jogador (ele aceita ou recusa)\n" +
                "`/atacar` — ataca durante a batalha (use no seu turno, 5 min por turno)\n" +
                "`/desistir` — desiste da batalha (seu bicho não morre, oponente vence)\n" +
                "Vitória: +150 moedas e +300 XP | Bônus de felicidade e treino aumentam o dano",
                false);

        embed.addField("🎁 Social & Ranking",
                "`/presentear jogador:@alguém moedas:N` — presenteia moedas a outro jogador\n" +
                "`/ranking` — top 10 por nível, moedas ou bichos mais velhos",
                false);

        embed.addField("⚠️ Cuidado!",
                "Os atributos decaem em tempo real: fome -5/h, limpeza -2/h, felicidade -1/h.\n" +
                "Se a fome zerar, o bicho perde vida. Se a vida zerar, **ele morre**!\n" +
                "Você pode adotar outro imediatamente após a morte.",
                false);

        return embed.build();
    }

    // ----------------------------------------------------------------- embed de status

    private net.dv8tion.jda.api.entities.MessageEmbed construirEmbed(long donoId, String nomePlayer) {
        var statusTexto = tamagotchiService.status(donoId);

        // Detecta se o bicho está morto ou ausente pela mensagem retornada
        boolean semBicho = statusTexto.contains("não tem um bicho") || statusTexto.contains("morreu");

        var embed = new EmbedBuilder();
        embed.setTitle("🐾 Status de " + nomePlayer);
        embed.setDescription(statusTexto);

        if (semBicho) {
            embed.setColor(Color.GRAY);
        } else {
            // Cor baseada na vida do bicho (extraída do texto)
            embed.setColor(corPorStatus(statusTexto));
        }

        embed.setFooter("DGoshi • Tamagotchi Bot");
        return embed.build();
    }

    /**
     * Cor do embed com base nos indicadores de saúde no texto:
     * verde = tudo ok, amarelo = atenção, vermelho = crítico.
     */
    private Color corPorStatus(String statusTexto) {
        // Conta quantas barras estão em estado crítico (≤30 — 3 blocos ou menos)
        long criticos = statusTexto.lines()
                .filter(l -> l.contains("░░░░░░░") || l.contains("/100") && extrairValor(l) <= 30)
                .count();

        if (criticos >= 2) return new Color(220, 50, 50);   // vermelho
        if (criticos == 1) return new Color(230, 170, 30);  // amarelo
        return new Color(50, 180, 100);                      // verde
    }

    private int extrairValor(String linha) {
        try {
            var partes = linha.split("/100");
            if (partes.length == 0) return 100;
            String ultimo = partes[0].trim();
            return Integer.parseInt(ultimo.substring(ultimo.lastIndexOf(' ') + 1));
        } catch (Exception e) {
            return 100;
        }
    }

    // ----------------------------------------------------------------- guild ready

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        // Opções de autocomplete para alimentar
        var opcoesAlimentar = new OptionData(OptionType.STRING, "item", "Item a usar", false)
                .addChoices(
                        new Command.Choice(TipoItem.RACAO_COMUM.getNome(),    TipoItem.RACAO_COMUM.name()),
                        new Command.Choice(TipoItem.RACAO_PREMIUM.getNome(),  TipoItem.RACAO_PREMIUM.name()),
                        new Command.Choice(TipoItem.RACAO_GOURMET.getNome(),  TipoItem.RACAO_GOURMET.name()),
                        new Command.Choice(TipoItem.COMIDA_ESPECIAL.getNome(),TipoItem.COMIDA_ESPECIAL.name())
                );

        // Opções de autocomplete para comprar
        var opcoesComprar = new OptionData(OptionType.STRING, "item", "Nome do item", true)
                .addChoices(
                        new Command.Choice(TipoItem.RACAO_COMUM.getNome(),    TipoItem.RACAO_COMUM.name()),
                        new Command.Choice(TipoItem.RACAO_PREMIUM.getNome(),  TipoItem.RACAO_PREMIUM.name()),
                        new Command.Choice(TipoItem.RACAO_GOURMET.getNome(),  TipoItem.RACAO_GOURMET.name()),
                        new Command.Choice(TipoItem.COMIDA_ESPECIAL.getNome(),TipoItem.COMIDA_ESPECIAL.name()),
                        new Command.Choice(TipoItem.MEDICAMENTO.getNome(),    TipoItem.MEDICAMENTO.name()),
                        new Command.Choice(TipoItem.VACINA.getNome(),         TipoItem.VACINA.name()),
                        new Command.Choice(TipoItem.BRINQUEDO.getNome(),      TipoItem.BRINQUEDO.name()),
                        new Command.Choice(TipoItem.SUPER_BRINQUEDO.getNome(),TipoItem.SUPER_BRINQUEDO.name()),
                        new Command.Choice(TipoItem.POCAO_CURA_TOTAL.getNome(),TipoItem.POCAO_CURA_TOTAL.name())
                );

        event.getGuild().updateCommands().addCommands(

                Commands.slash("adotar", "Adote o seu primeiro pet virtual")
                        .addOption(OptionType.STRING, "nome", "Nome do seu bicho", true)
                        .addOptions(new OptionData(OptionType.STRING, "tipo", "Espécie do seu bicho", true)
                                .addChoices(
                                        new Command.Choice("🐱 Gato",     "Gato"),
                                        new Command.Choice("🐶 Cachorro",  "Cachorro"),
                                        new Command.Choice("🐉 Dragão",   "Dragão"),
                                        new Command.Choice("🦊 Raposa",   "Raposa"),
                                        new Command.Choice("🐺 Lobo",     "Lobo"),
                                        new Command.Choice("🐧 Pinguim",  "Pinguim"),
                                        new Command.Choice("🐰 Coelho",   "Coelho"),
                                        new Command.Choice("🦋 Borboleta","Borboleta"),
                                        new Command.Choice("🐸 Sapo",     "Sapo"),
                                        new Command.Choice("🦄 Unicórnio","Unicórnio")
                                )),

                Commands.slash("status", "Veja o status do seu bicho (ou de outro jogador)")
                        .addOption(OptionType.USER, "jogador", "Ver bicho de outro jogador", false),

                Commands.slash("alimentar", "Alimente o seu bicho")
                        .addOptions(opcoesAlimentar),

                Commands.slash("banho",   "Dê banho no seu bicho"),
                Commands.slash("medicar", "Medique o seu bicho (precisa de Medicamento no inventário)"),
                Commands.slash("vacinar", "Vacine o seu bicho para restaurar limpeza (precisa de Vacina)"),
                Commands.slash("dormir",  "Coloque o seu bicho pra descansar"),
                Commands.slash("brincar", "Jogue com o seu bicho e ganhe moedas"),

                Commands.slash("atacar", "Desafie ou ataque em batalha")
                        .addOption(OptionType.USER, "jogador",
                                "Jogador a desafiar (omita durante batalha para atacar)", false),

                Commands.slash("desistir", "Desiste da batalha atual (o oponente vence por desistência)"),

                Commands.slash("treinar", "Treine seu bicho: +2 de ataque permanente (custa moedas, 1h cooldown)"),
                Commands.slash("girar", "Caça-níqueis! Custa 50 moedas, pode ganhar até 1000"),

                Commands.slash("presentear", "Presenteie moedas para outro jogador")
                        .addOption(OptionType.USER, "jogador", "Jogador que vai receber", true)
                        .addOption(OptionType.INTEGER, "moedas", "Quantidade de moedas", true),

                Commands.slash("usar", "Use um item do inventário no seu bicho")
                        .addOptions(new OptionData(OptionType.STRING, "item", "Item a usar", true)
                                .addChoices(
                                        new Command.Choice("🧸 Brinquedo", "brinquedo"),
                                        new Command.Choice("✨ Super Brinquedo", "super_brinquedo"),
                                        new Command.Choice("🧪 Poção de Cura Total", "pocao")
                                )),

                Commands.slash("ranking", "Veja o ranking dos melhores jogadores")
                        .addOptions(new OptionData(OptionType.STRING, "categoria", "Categoria do ranking", false)
                                .addChoices(
                                        new Command.Choice("🏅 Nível", "nivel"),
                                        new Command.Choice("💰 Moedas", "moedas"),
                                        new Command.Choice("🐾 Bichos mais velhos", "bichos")
                                )),

                Commands.slash("daily",    "Resgate sua recompensa diária de moedas"),
                Commands.slash("ajuda",    "Saiba como jogar o DGoshi"),
                Commands.slash("loja",     "Veja os itens disponíveis para compra"),
                Commands.slash("comprar",  "Compre um item na loja").addOptions(opcoesComprar),
                Commands.slash("inventario", "Veja os itens no seu inventário")

        ).queue();
    }
}
