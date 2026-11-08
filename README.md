# Multiverse Shinies

Mod (NeoForge 1.21.1) para o [Cobblemon](https://cobblemon.com/) 1.8.1 que adiciona variantes **"Multiverse"** de alguns Pokémon, com texturas e modelos próprios. Funcionam como os shinies: qualquer Pokémon dessas espécies que nasce tem uma pequena chance de ser Multiverse.

Os modelos, texturas, animações e o sorteio ficam dentro do jar (em `src/main/resources/assets/` e `src/main/resources/data/`), então basta instalar o mod. O código Java cuida do que o Cobblemon não faz sozinho: a fúria do Infernape (aura, explosão, empurrão e sons), o cabelo que ilumina e o voo na nuvem.

## Pokémon incluídos

| Pokémon | Textura | Modelo | Resolver | Status |
|---|:-:|:-:|:-:|---|
| Magikarp (macho e fêmea) | ✅ | padrão | ✅ | Funcional |
| Gyarados | ✅ | padrão | ✅ | Funcional |
| Eevee (macho e fêmea) | ✅ | ✅ | ✅ | Funcional |
| Flareon (macho) — "Diluc" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Flareon (fêmea) — "Mavuika" | ✅ | ✅ | ✅ | Funcional (animações do original; pontas do cabelo brilham e óculos translúcidos via camadas) |
| Jolteon (macho) — "Flins" | ✅ | ✅ | ✅ | Funcional (lanterna animada no quadril: balança, chama azul brilha pelo vidro e reage nas batalhas) |
| Jolteon (fêmea) — "Fischl" | ✅ | ✅ | ✅ | Funcional (animações do original; arco nas costas) |
| Umbreon (macho) — "Zhongli" | ✅ | ✅ | ✅ | Funcional (animações do original; anéis vermelhos e gemas Geo brilham via camada) |
| Espeon (macho) — "Kaveh" | ✅ | ✅ | ✅ | Funcional (animações do original; capa em duas faixas, Visão Dendro e gemas brilham via camada) |
| Espeon (fêmea) — "Citlali" | ✅ | ✅ | ✅ | Funcional (animações do original; anéis flutuando atrás das orelhas, saia longa; joia da testa brilha via camada) |
| Sylveon (fêmea) — "Varesa" | ✅ | ✅ | ✅ | Funcional (animações do original; tranças nas fitas, chifres, saia e polainas) |
| Sylveon (macho) — "Dahlia" | ✅ | ✅ | ✅ | Funcional (livro mágico animado voando em volta; reage nas batalhas) |
| Vaporeon (fêmea) — "Furina" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Vaporeon (macho) — "Neuvillette" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Infernape — cosplay de Super Saiyajin 4 | ✅ | ✅ | ✅ | Funcional (animações do original; fogo do cabelo animado; jaqueta de pelo vermelho, faixa, calça de gi, munhequeiras e botas; jubinhas de fogo nos ombros; cabelo que ilumina; voa numa nuvem atrás do treinador; **fúria** com explosão de aura quando recebe buff na batalha) |
| Chimchar — "Goku criança" | ✅ | ✅ | ✅ | Funcional (animações do original; gi vermelho do Goku criança) |
| Monferno — cosplay do Goku | ✅ | ✅ | ✅ | Funcional (gi, faixa, munhequeiras, botas e o bastão Nyoibo nas costas; em batalha segura o bastão em pé como na figure, com animação própria; **vira Super Saiyajin** quando recebe buff: bate o bastão no chão, o pelo fica dourado, aura dourada com raios) |
| Rayquaza — "Slifer, o Dragão Celeste" | ✅ | ✅ | ✅ | Funcional (animações e poser próprios: voo em onda vertical batendo as asas; joia da testa brilha via camada) |
| Rayquaza — "Shenlong" | ✅ | ✅ | ✅ | Funcional (meio a meio com o Slifer quando vem Multiverse; esferas do dragão em volta; aparece na cena de evolução dos Multiverse) |
| Mega Rayquaza — "Mega Slifer" | ✅ | ✅ | ✅ | Opcional: só com o [Mega Showdown](https://modrinth.com/project/gUnQILfP) instalado (animações e poser próprios; asas maiores) |

> Os modelos usam somente **box UV**: o carregador de modelos do Cobblemon não aceita UV por face e, se encontrar um, o recarregamento dos recursos falha e o jogo trava.

## Como funciona

1. **Sorteio:** `data/cobblemon/species_features/multiverse_roll.json` é uma feature do tipo `choice` com `"default": "random"`. Toda vez que o Cobblemon cria um Pokémon dessas espécies (spawn natural, spawn de outros mods, comando sem a propriedade), ele sorteia um item da lista `choices`: 511 vezes `multiverse_none` e 1 vez `multiverseshinies`, ou seja, **1 chance em 512**.
2. **Atribuição:** `data/cobblemon/species_feature_assignments/multiverse_roll.json` liga o sorteio às espécies (Magikarp, Gyarados, Eevee, Vaporeon, Flareon, Jolteon, Espeon, Umbreon, Sylveon, Monferno, Infernape e Rayquaza). O resultado fica salvo no Pokémon e acompanha a evolução entre essas espécies.
3. **Visual:** quando o sorteio dá `multiverseshinies`, o Pokémon ganha esse aspect e os resolvers em `assets/cobblemon/bedrock/pokemon/resolvers/` trocam a textura e o modelo. Onde há duas versões, o gênero decide: Vaporeon fêmea = Furina, macho = Neuvillette; Flareon fêmea = Mavuika, macho = Diluc; Jolteon fêmea = Fischl, macho = Flins; Sylveon fêmea = Varesa, macho = Dahlia; Espeon fêmea = Citlali, macho = Kaveh; Umbreon macho = Zhongli (a fêmea ainda não tem variante). O Infernape (macho ou fêmea) veste o cosplay de Super Saiyajin 4. O Rayquaza vira o Slifer, o Dragão Celeste, e, se o mod Mega Showdown estiver instalado, o Slifer que mega evolui vira o Mega Slifer.

Não existe spawn próprio: os Pokémon aparecem onde o Cobblemon (ou outro mod) já os coloca. Para mudar a chance, ajuste quantas vezes `multiverse_none` aparece na lista `choices` (por exemplo, 1023 vezes = 1 em 1024).


### Infernape SSJ4: fogo animado e cabelo que ilumina

O fogo do cabelo e das jubinhas corre da raiz para as pontas o tempo todo: a camada brilhante (`infernape_multiverse_flame1.png` a `flame10.png`) é uma textura animada de 10 quadros a 10 por segundo, do mesmo jeito que a chama do Charmander no Cobblemon.

O cabelo também ilumina o lugar em volta dele, como uma tocha (`HairLight`): a cada tick o cliente coloca um bloco de luz invisível na altura da cabeça (nível 13, ou 15 durante a fúria) e tira quando ele sai dali. O bloco existe só na cópia do mundo do jogador: nada vai para o servidor nem é salvo.

### Infernape SSJ4: fúria

Quando o Infernape Multiverse recebe buff numa batalha, ele entra em fúria (inspirado no Super Saiyajin 4 do anime). A animação `animation.multiverse_infernape.rage` dura 5,4 s e é tocada pelo nome `multiverse_rage` do poser `infernape_multiverse.json`:

1. **Juntando força (0 a 2,6 s):** fecha os punhos, agacha devagar e treme cada vez mais forte.
2. **Grito (2,62 s):** explode de pé rugindo, com os braços abertos para cima. Nesse instante:
   - a aura explode para fora dele em duas ondas (os mesmos desenhos da aura, crescendo até umas 3 vezes e sumindo);
   - três anéis da aura correm pelo chão;
   - poeira, faíscas de ki e pedaços do chão são arremessados, e saem os pedregulhos da partícula `bedrock/particles/multiverse_rage_rocks.particle.json`;
   - a tela pisca vermelho-alaranjado, as bordas ficam vermelhas e a câmera treme, mais forte quanto mais perto o jogador estiver e quanto mais estiver olhando para ele (até 40 blocos);
   - uma onda de choque joga para trás os jogadores e criaturas por perto (os Pokémon da batalha não são afetados).
3. **Transformado:** a aura começa maior e mais forte e assenta em pouco mais de 1 segundo. Daí em diante ele fica com o cabelo vermelho (ossos `rage_*` do modelo), a aura vermelha animada, ondas e vento no chão, faíscas e pedras levitando, e empurra para longe quem chegar perto.

A explosão e a aura são desenhadas pelo mod (`RageBurst`, `RageAuraRenderer` e `AuraMesh`, texturas em `assets/multiverseshinies/textures/aura/`), e o empurrão roda no servidor (`RageAuraPush`).

**A aura é 3D** (`AuraMesh`), para funcionar de qualquer ângulo, inclusive de cima: uma casca em forma de gota em volta do corpo, com riscos de luz subindo e brilho na borda (forte onde a casca é vista de perfil, quase transparente onde fica de frente para a câmera, então ele aparece dentro dela), com pontas de chama no topo; e labaredas recortadas dos desenhos da aura subindo pelas laterais do corpo, cada uma virada para a câmera. A explosão da transformação é essa mesma aura estourando para fora em duas ondas.

**Como a fúria liga e desliga:**

- O mod substitui o `data/cobblemon/action_effects/misc/boost.json` do Cobblemon: os passos originais continuam iguais para todos os Pokémon, e só foi acrescentado um passo que toca a animação `multiverse_rage` e marca a variável `v.multiverse_rage` no Pokémon. Só tem efeito em Pokémon cujo poser tenha essa animação (hoje, só o Infernape Multiverse). Se uma atualização do Cobblemon mudar esse arquivo, ele precisa ser atualizado aqui também.
- Os debuffs usam o `unboost.json` do Cobblemon, que não foi alterado: status caindo nunca dispara a fúria.
- O visual (cabelo vermelho, aura, empurrão) só liga no grito, 52 ticks (2,62 s) depois do buff. O cliente escreve `v.multiverse_rage_on` para o poser trocar o cabelo.
- A fúria acaba quando a batalha termina, quando ele volta para a Poké Ball ou quando os buffs somem no meio da luta (Haze, status voltando a 0...). O servidor confere os buffs de verdade da batalha e avisa os jogadores (`RageNet`), então ele não começa a batalha seguinte já transformado.

**Uma transformação por batalha:** golpes que sobem dois status (Bulk Up, Dragon Dance) mandam dois buffs; assim que o primeiro entra, o servidor dá ao Pokémon o aspect temporário `multiverse_raging`, e o `boost.json` só toca a animação de transformação em quem ainda não o tem. O aspect sai quando a fúria acaba.

**Sons:** o mod registra três sons em `assets/multiverseshinies/sounds.json`: `rage.transform` (no grito, tocado pela própria animação), `rage.aura_loop` (zumbido da aura em loop enquanto ele estiver transformado, entrando baixinho uns 1,7 s depois do grito) e `rage.calm` (quando a fúria acaba com ele ainda em campo). Por padrão eles usam sons do Minecraft; um resource pack pode trocá-los por outros (com `"replace": true` no `sounds.json` do pack).

**Shaders:** os pedaços do chão são desenhados como entidades, e não como blocos de terreno, porque com shaders (Iris) blocos de terreno desenhados fora do chão saem esticados e retorcidos.

### Chimchar "Goku criança"

O Chimchar Multiverse veste o gi do Goku criança: gi vermelho sem manga com o símbolo do Kame no peito e um grande nas costas, faixa preta com as pontas penduradas, munhequeiras azuis, calça larga e sapatos azul-marinho. Só roupa (sem cabelo, sem bastão), com as animações e o poser do Chimchar original (`multiverse_chimchar.geo.json`, `chimchar_multiverse.png`, resolver `resolvers/0390_chimchar/1_chimchar_multiverse.json`). Evoluindo para Monferno, toca a cena do Shenlong.

### Monferno "Goku": bastão e Super Saiyajin

O Monferno Multiverse veste o gi do Goku e carrega o bastão Nyoibo atravessado nas costas (osso `power_pole`). Fora da batalha usa as animações do Monferno original; o poser próprio (`posers/0391_monferno/monferno_multiverse.json`) esconde o bastão da mão (`pole_hand_hidden`). Em batalha, a pose `multiverse_monferno.battle_idle` imita a figure do Goku: o bastão em pé seguro no punho esquerdo (osso `power_pole_hand`, apoiado no chão), o punho direito na cintura, respirando e olhando em volta num loop de 8 s; o bastão das costas some.

Quando recebe buff, a animação `multiverse_monferno.rage` levanta o bastão reto e bate no chão uma vez; nessa batida (0,5 s) ele vira Super Saiyajin:

- o pelo fica dourado: o servidor dá ao Pokémon o aspect `multiverse_ssj` e o resolver troca a textura por `monferno_multiverse_ssj.png` (o aspect sai quando a fúria acaba);
- aura dourada (a mesma do Infernape, recolorida), com a explosão de aura na batida;
- raios azul-claros e brancos estalando em volta dele o tempo todo (como no Super Saiyajin 2), e uma rajada maior na batida;
- flash e bordas da tela dourados; sem pedras, poeira ou vento no chão.

Cada espécie que se transforma tem a sua configuração em `RageSpecies` (quando a explosão acontece e se é dourada).

### Infernape SSJ4: nuvem voadora

Fora da batalha ele anda e corre em cima de uma nuvem amarela (ossos `nimbus*` do modelo; animações `cloud_walk` e `cloud_run`). O mod mede a velocidade dele no cliente e grava em `v.multiverse_running` para o poser escolher entre andar e correr (`CloudPoseDriver`).

Quando tem dono, ele voa de verdade atrás do treinador (`NimbusFlight`): decola quando o treinador se afasta uns 5 blocos ou sobe, mantém cerca de 1 bloco de ar embaixo da nuvem, acelera quando fica para trás, teleporta quando passa de 28 blocos e pousa depois de uns 3 segundos com o treinador parado no chão perto dele. Em batalha, na água, ocupado, dormindo ou preso num pasto ele não voa.

A posição no ar segue a direção em que o treinador **anda**, nunca para onde a câmera olha, então dá para virar e olhar para ele à vontade:

- Com o treinador se movendo, ele voa um pouco à frente e uns 2 blocos para o lado (o lado em que já estava), para aparecer na tela.
- Com o treinador parado (no chão ou no ar), ele fica onde está, entre 1,8 e 3,6 blocos de distância, virado para o treinador.

O servidor avisa os jogadores quando ele está voando (`NimbusNet`), e no ar o poser usa `cloud_fly` e `cloud_fly_fast` (`v.multiverse_flying`). Não dá para usar a falta de gravidade para isso, porque o Cobblemon a reinicia no cliente toda vez que a pose muda e a nuvem sumia com ele parado no ar.

## Estrutura

```
Multiverse Shinies/
├── build.gradle, settings.gradle, gradle.properties, gradlew   # Projeto NeoForge (Gradle)
├── libs/                                # Jars do Cobblemon e do Kotlin for Forge para compilar (fora do git)
├── src/main/
│   ├── java/com/multiverseshinies/      # Código do mod (fúria, aura, cabelo que ilumina, voo na nuvem)
│   ├── templates/META-INF/              # neoforge.mods.toml
│   └── resources/
│       ├── assets/cobblemon/
│       │   ├── textures/pokemon/        # Texturas das variantes
│       │   └── bedrock/
│       │       ├── particles/           # Pedregulhos da fúria do Infernape
│       │       └── pokemon/
│       │           ├── animations/      # Animações próprias
│       │           ├── models/          # Modelos geo customizados
│       │           ├── posers/          # Animações e poses
│       │           └── resolvers/       # Mapeamento aspect → textura/modelo
│       ├── assets/multiverseshinies/    # Texturas da aura e sons (sounds.json) do mod
│       └── data/cobblemon/
│           ├── species_features/        # Sorteio multiverse_roll (1 em 512)
│           ├── species_feature_assignments/ # Quais espécies participam do sorteio
│           └── action_effects/misc/     # boost.json com o passo da fúria
└── blockbench/                          # Arquivos-fonte .bbmodel (não vão no jar)
```

A pasta `blockbench/` guarda os arquivos-fonte `.bbmodel` dos modelos customizados (Rayquaza "Slifer" em `rayquaza_slifer.bbmodel` e "Mega Slifer" em `rayquaza_mega_slifer.bbmodel`, ambos com as animações; Vaporeon "Furina", Vaporeon "Neuvillette", Flareon "Diluc", Flareon "Mavuika", Jolteon "Fischl", Jolteon "Flins", Sylveon "Dahlia" em `sylveon_dahlia_male.bbmodel`, com as animações, Sylveon "Varesa" em `sylveon_varesa_female.bbmodel`, Espeon "Citlali" em `espeon_citlali_female.bbmodel`, Espeon "Kaveh" em `espeon_kaveh_male.bbmodel`, Umbreon "Zhongli" em `umbreon_zhongli_male.bbmodel`, Chimchar "Goku criança" em `chimchar_goku.bbmodel`, Monferno "Goku" em `monferno_goku.bbmodel`, Infernape SSJ4 em `infernape_ssj4.bbmodel` e Rayquaza "Shenlong" em `rayquaza_shenlong.bbmodel`, todos com as animações), para editar no [Blockbench](https://www.blockbench.net/).

## Instalação

1. Instale o **NeoForge 1.21.1** e o **Cobblemon 1.8.1**.
2. Coloque `multiverseshinies-1.0.0.jar` na pasta `mods/`.

Para compilar, coloque `Cobblemon-neoforge-1.8.1+1.21.1.jar` e `kotlinforforge-5.12.0-all.jar` em `libs/` e rode `./gradlew build` com o Java 21. O jar sai em `build/libs/`.

Para testar, invoque um Pokémon já com o resultado do sorteio:

```
/pokespawn eevee multiverse_roll=multiverseshinies
/pokespawn vaporeon multiverse_roll=multiverseshinies gender=female
/pokespawn vaporeon multiverse_roll=multiverseshinies gender=male
/pokespawn flareon multiverse_roll=multiverseshinies gender=male
/pokespawn flareon multiverse_roll=multiverseshinies gender=female
/pokespawn jolteon multiverse_roll=multiverseshinies gender=female
/pokespawn jolteon multiverse_roll=multiverseshinies gender=male
/pokespawn rayquaza multiverse_roll=multiverseshinies multiverse_rayquaza_style=multiverse_classic
/pokespawn rayquaza multiverse_roll=multiverseshinies multiverse_rayquaza_style=multiverse_shenlong
/pokespawn sylveon multiverse_roll=multiverseshinies gender=male
/pokespawn sylveon multiverse_roll=multiverseshinies gender=female
/pokespawn espeon multiverse_roll=multiverseshinies gender=female
/pokespawn espeon multiverse_roll=multiverseshinies gender=male
/pokespawn umbreon multiverse_roll=multiverseshinies gender=male
/pokespawn chimchar multiverse_roll=multiverseshinies
/pokespawn monferno multiverse_roll=multiverseshinies
/pokespawn infernape multiverse_roll=multiverseshinies
```

O Rayquaza "Slifer" tem animações próprias em `animations/0384_rayquaza/multiverse_rayquaza.animation.json` (`ground_idle`, `blink`, `eye_size`, `air_fly` e `air_idle`) e um poser próprio (`posers/0384_rayquaza/rayquaza_multiverse.json`): parado usa `ground_idle`, pairando usa `air_idle` e voando ou nadando usa `air_fly`, em que o corpo e a cauda ondulam para cima e para baixo enquanto as asas batem (o pescoço e a cabeça ficam firmes). A joia azul da testa fica em `rayquaza_multiverse_emissive.png` e brilha no escuro.

### Jolteon "Flins" e a lanterna

O Jolteon "Flins" usa as animações do Jolteon original e ganha animações próprias da lanterna em `animations/0135_jolteon/multiverse_jolteon.animation.json`, com poser próprio (`posers/0135_jolteon/jolteon_multiverse_male.json`):

- A lanterna fica pendurada pela alça (osso `lantern_hang`) e tem uma chama azul própria (`lantern_flame`) dentro do vidro. O vidro é uma camada translúcida (`jolteon_multiverse_male_glass.png`) e a chama brilha no escuro (`jolteon_multiverse_male_emissive.png`).
- `lantern_idle` (parado) e `lantern_walk` (andando): balanço de pêndulo e chama tremulando.
- Batalha e grito juntam a expressão do Jolteon com a lanterna: `cry` = susto (tranco e a chama explode), `physical_attack` = golpe com a lanterna, `special_attack` = a chama cresce e estala, `status` = balanço hipnótico com clarão no fim, `faint` = a chama engasga e apaga.

### Sylveon "Dahlia" e o livro mágico

O Sylveon "Dahlia" usa as animações do Sylveon original e ganha o livro mágico da Dahlia, com animações próprias em `animations/0700_sylveon/multiverse_sylveon.animation.json` e poser próprio (`posers/0700_sylveon/sylveon_multiverse.json`):

- `book_float` (parado e andando): o livro dá uma volta em torno do Sylveon, para na frente, abre, folheia o livro todo em 2 segundos e fecha (ciclo de 10 s). Roda junto com as animações do Sylveon, porque só mexe nas partes do livro.
- Batalha e grito: cada uma junta a expressão do rosto do Sylveon com uma "expressão" do livro. `cry` = susto (o livro escancara e as folhas tremulam), `physical_attack` = raiva (o livro treme e morde com a capa), `special_attack` = `book_cast` (o livro avança, abre e folheia em 1 segundo), `status` = desconfiado (o livro vira de lado e fica entreaberto), `faint` = machucado (o livro fecha, cai, quica e fica no chão).
- O arquivo também traz as outras expressões do livro (`book_neutral`, `book_happy`, `book_sad`, `book_bored`, `book_mouth_open`, `book_mouth_open_ooo` e as já citadas) para uso futuro.

A Sylveon "Varesa" (fêmea) usa as animações do Sylveon original; as fitas viram as tranças rosa dela e a saia envolve o quadril abrindo para trás. As texturas do Dahlia e da Varesa têm 512×512 (4 texels por unidade nas roupas).

### Rayquaza "Shenlong"

Um Rayquaza no visual do Shenlong. Todo Rayquaza sorteia também o traço `multiverse_rayquaza_style` (`multiverse_classic` ou `multiverse_shenlong`, meio a meio); quando ele vem Multiverse, esse traço decide se é o Slifer ou o Shenlong (resolver `resolvers/0384_rayquaza/3_rayquaza_shenlong.json`, ordem 3). O Shenlong não tem mega: com o Mega Showdown ele continua Shenlong.

- Modelo `multiverse_rayquaza_shenlong.geo.json`, texturas `rayquaza_shenlong.png` e `rayquaza_shenlong_emissive.png` (olhos vermelhos brilham no escuro), animações em `multiverse_rayquaza_shenlong.animation.json` e poser `posers/0384_rayquaza/rayquaza_shenlong.json`: parado e andando usa `ground_idle`, pairando usa `air_idle`, voando ou nadando usa `air_fly`.
- Corpo verde com escamas, barriga creme em placas com a linha vermelha, crista dentada nas costas, galhadas marrons, tufos de pelo nas bochechas, bigodes compridos e olhos todos vermelhos.
- As sete esferas do dragão (de 1 a 7 estrelas) flutuam em volta do corpo: voando (`air_fly`) giram em espiral; paradas (`ground_idle`, `air_idle`) só sobem e descem. Cada uma tem um locator (`dragonball_1` a `_7`).

### Cena de evolução dos Multiverse

Quando um Pokémon Multiverse evolui, o Shenlong aparece (cena só no cliente, todo mundo por perto vê):

- Um portal surge pequeno com um clarão e raios em volta e cresce; o Shenlong sai dele mergulhando, dá umas 2,75 voltas em espiral em volta do Pokémon e sai por um segundo portal logo depois da troca de forma (11,2 s). O corpo segue o caminho da cabeça, ondulando e inclinando nas curvas; as esferas deixam rastro.
- Enquanto ele circula, a luz sai do Pokémon e vai crescendo (núcleo brilhante, raios de luz girando, coluna de luz até o céu, brilhos sendo sugados e sinos subindo); na troca de forma, tela branca, ondas de choque, explosão de brilhos e tremor de câmera.
- O voo é uma animação só (`evolution_cutscene`, no osso `flight`), feita no Blockbench (`blockbench/rayquaza_shenlong.bbmodel`; a cena com portais e o Monferno de referência está em `blockbench/shenlong_evolution_preview.bbmodel`). Os portais e os tempos de cada osso ficam em `assets/multiverseshinies/cutscene/shenlong_evolution.json`.
- O dragão da cena é um Rayquaza só do cliente, com o aspect `multiverse_shenlong_cutscene` e o poser `rayquaza_shenlong_cutscene.json`. O servidor segura o Pokémon que está evoluindo no lugar durante a cena (`EvolutionLock`).

### Mega Slifer (opcional, precisa do Mega Showdown)

O Mega Slifer é a mega evolução do Slifer. Ele só funciona com o mod [Mega Showdown](https://modrinth.com/project/gUnQILfP) instalado: o resolver `resolvers/0384_rayquaza/2_rayquaza_multiverse_mega.json` (ordem 2, acima do Mega Rayquaza do Mega Showdown e do Slifer) só vale quando o Pokémon tem os aspects `multiverseshinies` e `mega` ao mesmo tempo, e quem dá o aspect `mega` é o Mega Showdown. Sem ele, os arquivos ficam no jar sem uso e não interferem em nada; o mod não inclui nem altera nada do Mega Showdown.

Ele usa o modelo `multiverse_megarayquaza.geo.json`, as texturas `rayquaza_multiverse_mega.png` e `rayquaza_multiverse_mega_emissive.png` (fitas, olhos e joia brilham) e o poser `rayquaza_multiverse_mega.json`. As animações (`multiverse_megarayquaza.animation.json`) são as do Mega Rayquaza do Mega Showdown com as asas batendo no `ground_idle` e no `ground_walk`, mais `air_fly`, `air_idle` e `eye_size` no estilo do Slifer.

A Flareon "Mavuika" usa duas camadas extras no resolver: `flareon_multiverse_female_emissive.png` (pontas neon do cabelo, brilham no escuro) e `flareon_multiverse_female_lenses.png` (lentes dos óculos, translúcidas).

## Problemas conhecidos

- O Cobblemon não faz Rayquaza nascer sozinho; o Rayquaza Multiverse só aparece se outro mod (ou um comando) criar um Rayquaza.
- As outras evoluções do Eevee (Leafeon e Glaceon) e a Umbreon fêmea ainda não têm variante.

## Próximas atualizações

- **Animações próprias para os Pokémon Multiverse.** O Rayquaza "Slifer" já tem as suas; os outros ainda usam as animações e o poser da espécie original. A ideia é criar animações que combinem com cada personagem, por exemplo:
  - Flareon "Diluc": balançar a espada nas costas no idle, golpe de espada no ataque.
  - Flareon "Mavuika": ajeitar os óculos no idle, chamas do cabelo mais vivas ao andar.
  - Vaporeon "Furina": pose teatral, girando e fazendo reverência com a cartola.
  - Vaporeon "Neuvillette": postura séria e elegante, abaixando a cabeça como cumprimento.
  - Cada animação nova precisa de um poser próprio em `posers/` apontado pelo resolver da variante.

## Licença

Ainda não definida, exceto pelos arquivos do Mega Slifer (veja abaixo).

Os modelos do Vaporeon "Furina" (fêmea), do Vaporeon "Neuvillette" (macho), do Flareon "Diluc" (macho) da Flareon "Mavuika" (fêmea) da Jolteon "Fischl" (fêmea) do Jolteon "Flins" (macho), do Sylveon "Dahlia" (macho), da Sylveon "Varesa" (fêmea), da Espeon "Citlali" (fêmea), do Espeon "Kaveh" (macho), do Umbreon "Zhongli" (macho), do Chimchar "Goku criança", do Monferno "Goku", do Infernape SSJ4 e do Rayquaza "Slifer" são adaptações dos modelos oficiais do Vaporeon, do Flareon, do Jolteon, do Espeon, do Umbreon, do Sylveon, do Chimchar, do Monferno, do Infernape e do Rayquaza do [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets) (licença Creative Commons, uso não comercial): foram adicionadas roupas e acessórios, e as texturas foram recoloridas. Os créditos do modelo original são dos autores do Cobblemon.

O Mega Rayquaza "Mega Slifer" (`multiverse_megarayquaza.geo.json`, `multiverse_megarayquaza.animation.json`, `rayquaza_multiverse_mega.png`, `rayquaza_multiverse_mega_emissive.png`, `rayquaza_multiverse_mega.json` e `blockbench/rayquaza_mega_slifer.bbmodel`) é uma adaptação do modelo, da textura e das animações do Mega Rayquaza do mod [Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown), de Yajat Kaul e colaboradores: foram adicionadas asas, a textura foi recolorida e foram criadas animações novas. Esses assets vêm do Mega Showdown e, como manda a [licença do Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown/blob/main/LICENSE.md), esses arquivos são distribuídos sob a licença [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) (uso não comercial, com crédito e mesma licença). Este mod não é afiliado ao Mega Showdown.

## Créditos

- **[Cobblemon](https://cobblemon.com/)** e o [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets): modelos, texturas e animações originais do Magikarp, Gyarados, Eevee, Vaporeon, Flareon, Jolteon, Espeon, Umbreon, Sylveon, Monferno, Infernape e Rayquaza.
- **[Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown)**, de Yajat Kaul e colaboradores: modelo, textura e animações originais do Mega Rayquaza, base do Mega Slifer ([licença](https://github.com/yajatkaul/CobblemonMegaShowdown/blob/main/LICENSE.md); assets sob CC BY-NC-SA 4.0).
