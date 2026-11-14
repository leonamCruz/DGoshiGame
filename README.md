# 🐾 DGoshi — Tamagotchi Bot para Discord

Bot de Discord que simula um Tamagotchi. Cada jogador pode ter **1 bicho** por vez. Cuide bem dele — ele pode morrer se você não interagir!

Feito com **Java 25**, **JDA 6** e **SQLite**.

---

## Mecânicas

### Atributos do bicho

| Atributo | Decaimento | Consequência se zerar |
|---|---|---|
| ❤️ Vida | — | Morte |
| 🍖 Fome | −5 / hora | Drena −3 vida/hora quando = 0 |
| 🛁 Limpeza | −2 / hora | Drena −2 vida/hora quando = 0 |
| 😄 Felicidade | −1 / hora (mínimo 10) | Apenas cosmético |

> Os atributos só são calculados quando o jogador interage. Um monitor roda a cada 15 minutos em background e envia DM ao dono se o bicho morrer por descuido.

### Estágios de vida

| Estágio | Idade |
|---|---|
| 🥚 Ovo | 0–1 dia |
| 🐣 Filhote | 1–7 dias |
| 🐾 Jovem | 7–30 dias |
| 💪 Adulto | 30+ dias |

Bichos mais velhos causam mais dano em batalha.

### Batalha (estilo Pokémon)

- Chance de **errar**: 20%
- Chance de **crítico**: 10% (+50% dano)
- Dano base: 10–24 (Ovo/Filhote) | 15–29 (Jovem) | 25–39 (Adulto)
- **Bônus de felicidade**: bichos com felicidade ≥ 70 causam +5 de dano extra
- **Bônus de treino**: cada treino adiciona +2 de dano permanente ao bicho
- Vitória: **+150 moedas** e **+300 XP**
- Morte em batalha: notificação no canal + DM ao perdedor
- Timeout de turno: 5 minutos sem atacar = oponente vence por W.O.

---

## Comandos

### 🌱 Começar

| Comando | Descrição |
|---|---|
| `/adotar nome: tipo:` | Adota o seu primeiro bicho. Escolha a espécie no menu. Ganha 3x Ração Comum de bônus. |
| `/ajuda` | Mostra um guia completo de como jogar (também funciona marcando o bot). |
| `/status` | Mostra o status do seu bicho (vida, fome, limpeza, felicidade, moedas e nível). |
| `/status jogador:@alguém` | Vê o bicho de outro jogador. |

### 🍖 Cuidados

| Comando | Descrição |
|---|---|
| `/alimentar` | Alimenta o bicho com Ração Comum (padrão). Escolha outro item no menu. |
| `/alimentar item:Ração Premium` | Alimenta com um item específico do inventário. |
| `/banho` | Dá banho no bicho. Restaura +40 de Limpeza. Sem custo. |
| `/medicar` | Usa um **Medicamento** do inventário. Restaura +50 de Vida. |
| `/vacinar` | Usa uma **Vacina** do inventário. Restaura +30 de Limpeza. |
| `/dormir` | Coloca o bicho para descansar. Restaura +15 Vida e +10 Felicidade. |
| `/usar brinquedo` | Usa um **Brinquedo** do inventário. Restaura +20 de Felicidade. |
| `/usar super_brinquedo` | Usa um **Super Brinquedo** do inventário. Restaura +50 de Felicidade. |
| `/usar pocao` | Usa uma **Poção de Cura Total** do inventário. Restaura +50 em Vida, Fome, Limpeza e Felicidade. |

### 🎮 Entretenimento

| Comando | Descrição |
|---|---|
| `/brincar` | Minijogo de reação: clique no emoji correto em 15 segundos. Acerto = +20 moedas e +30 XP. Erro = +5 moedas e +5 XP. |
| `/girar` | Caça-níqueis! Custa 50 moedas. 3 iguais = até 1000 moedas. 2 iguais = 75 moedas. |
| `/daily` | Resgata a recompensa diária. **+100 moedas**. Disponível 1x por 24 horas. |

### 💪 Treino

| Comando | Descrição |
|---|---|
| `/treinar` | Treina o bicho: **+2 de ataque permanente** por sessão. Custo: 50 + (nível × 25) moedas. Cooldown: 1 hora. |

### 🎁 Social

| Comando | Descrição |
|---|---|
| `/presentear jogador:@alguém moedas:100` | Transfere moedas para outro jogador. |

### ⚔️ Batalha

| Comando | Descrição |
|---|---|
| `/atacar jogador:@alguém` | Desafia outro jogador para batalha. Ele recebe botões de Aceitar/Recusar. |
| `/atacar` | (Sem argumento) Ataca durante uma batalha em curso. Use no seu turno. |
| `/desistir` | Desiste da batalha atual. O oponente vence por desistência (+150 moedas, +300 XP). Seu bicho não morre. |

> ⏱️ Cada turno tem **5 minutos** de limite. Se ninguém atacar a tempo, a batalha é cancelada e o oponente vence por timeout.

### 🏪 Loja & Inventário

| Comando | Descrição |
|---|---|
| `/loja` | Lista todos os itens disponíveis com preços. |
| `/comprar item:` | Compra um item com suas moedas. Escolha no menu. |
| `/inventario` | Mostra os itens que você tem e as quantidades. |

### 🏆 Ranking

| Comando | Descrição |
|---|---|
| `/ranking` | Top 10 jogadores por nível (padrão). |
| `/ranking categoria:Nível` | Ranking por maior nível/XP. |
| `/ranking categoria:Moedas` | Ranking por maior quantidade de moedas. |
| `/ranking categoria:Bichos mais velhos` | Ranking dos bichos vivos mais antigos. |

---

## Itens da Loja

| Item | Preço | Efeito |
|---|---|---|
| 🌾 Ração Comum | 50 🪙 | +15 Fome |
| ⭐ Ração Premium | 150 🪙 | +40 Fome |
| 🍖 Ração Gourmet | 100 🪙 | +30 Fome e +10 Felicidade |
| 🥩 Comida Especial | 300 🪙 | +60 Fome e +10 Vida |
| 💊 Medicamento | 200 🪙 | +50 Vida |
| 💉 Vacina | 180 🪙 | +30 Limpeza |
| 🧸 Brinquedo | 80 🪙 | +20 Felicidade |
| ✨ Super Brinquedo | 150 🪙 | +50 Felicidade |
| 🧪 Poção de Cura Total | 500 🪙 | +50 em Vida, Fome, Limpeza e Felicidade |

---

## Progressão

- **Moedas**: ganhas jogando (`/brincar`, `/girar`), no `/daily`, vencendo batalhas, e presenteando
- **XP**: ganho ao brincar e em batalhas
- **Nível**: sobe a cada 1000 XP (nível = XP ÷ 1000 + 1)
- **Recompensa de level-up**: ao subir de nível, recebe **200 × novo nível** em moedas automaticamente
- **Treino**: gaste moedas em `/treinar` para aumentar o ataque do bicho permanentemente (+2 por sessão)
- **Moedas iniciais**: 100 🪙 ao criar conta

---

## Espécies disponíveis

Ao adotar, escolha entre: 🐱 Gato, 🐶 Cachorro, 🐉 Dragão, 🦊 Raposa, 🐺 Lobo, 🐧 Pinguim, 🐰 Coelho, 🦋 Borboleta, 🐸 Sapo, 🦄 Unicórnio

---

## Setup

### Pré-requisitos

- Java 25+ (ou GraalVM 25 para build nativo)
- Maven 3.8+

### Variável de ambiente

```env
DISCORD_TOKEN=seu_token_aqui
```

> **Importante:** O bot usa o intent privilegiado `MESSAGE_CONTENT` (para detectar menções). Ative-o em **Discord Developer Portal → Bot → Privileged Gateway Intents → Message Content Intent**.

### Build JAR (padrão)

```bash
# Via Maven direto
mvn clean package -DskipTests

# Ou via script
.\build.ps1 jar        # Windows
```

Gera `target/DGoshi-0.0.1.jar` (fat JAR com todas as dependências). Rode com:

```bash
java -jar target/DGoshi-0.0.1.jar
```

### Build Nativo (GraalVM)

Gera um executável nativo sem precisar de JVM. Startup instantâneo e menor consumo de memória.

**Pré-requisitos:**
- **GraalVM 25** com `native-image` instalado
- **Windows:** Visual Studio 2022 (ou Build Tools) com workload "Desktop development with C++"
- **Linux:** `gcc` e `build-essential` (`sudo apt install build-essential`)

```bash
# Via Maven
mvn -Pnative clean package -DskipTests

# Ou via script
.\build.ps1 native       # Windows
```

Gera `target/dgoshi` (ou `dgoshi.exe` no Windows). Rode diretamente:

```bash
.\target\dgoshi.exe     # Windows
./target/dgoshi         # Linux
```

> **Cross-compilation:** o `native-image` compila para o SO atual. Para um binário Linux, faça o build no Linux. Para Windows, no Windows.

### Banco de dados

O banco `dados.db` (SQLite) é criado automaticamente na raiz do projeto na primeira execução. Não precisa de configuração.

---

## Estrutura do projeto

```
src/main/java/top/leonam/
├── Main.java
├── bd/          → Inicialização e configuração do banco
├── dao/         → Acesso a dados (BichoDAO, DonoDAO, InventarioDAO, BatalhaDAO, RankingDAO)
├── escutador/   → BotListener — processa slash commands e botões
├── jda/         → Inicialização do JDA
├── model/       → Entidades (Bicho, Dono, ItemInventario, BatalhaAtiva, TipoItem)
└── service/     → Lógica de negócio (TamagotchiService, BatalhaService, LojaService, MonitorService, RankingService)

src/main/resources/META-INF/native-image/top.leonam/DGoshi/
├── native-image.properties  → Args do GraalVM (resources, init-time, protocols)
├── reflect-config.json      → Reflection (SQLite JDBC + models)
├── resource-config.json     → Resources incluídos no binário nativo
└── jni-config.json           → Classes JNI do SQLite
```
