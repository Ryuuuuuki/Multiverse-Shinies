# Multiverse Shinies

Mod (NeoForge 1.21.1) para o [Cobblemon](https://cobblemon.com/) 1.8.1 que adiciona variantes **"Multiverse"** de alguns Pokémon, com texturas e modelos próprios. Funcionam como os shinies: qualquer Pokémon dessas espécies que nasce tem uma pequena chance de ser Multiverse.

Os modelos, texturas, animações e o sorteio ficam dentro do jar (em `src/main/resources/assets/` e `src/main/resources/data/`), então basta instalar o mod. O código Java cuida do que o Cobblemon não faz sozinho: a aura da fúria do Infernape, o empurrão e o voo na nuvem.

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
| Infernape — cosplay de Super Saiyajin 4 | ✅ | ✅ | ✅ | Funcional (animações do original; fogo do cabelo animado; jaqueta de pelo vermelho, faixa, calça de gi, munhequeiras e botas; jubinhas de fogo nos ombros; **fúria** quando recebe buff na batalha) |
| Rayquaza — "Slifer, o Dragão Celeste" | ✅ | ✅ | ✅ | Funcional (animações e poser próprios: voo em onda vertical batendo as asas; joia da testa brilha via camada) |
| Mega Rayquaza — "Mega Slifer" | ✅ | ✅ | ✅ | Opcional: só com o [Mega Showdown](https://modrinth.com/project/gUnQILfP) instalado (animações e poser próprios; asas maiores) |

> Os modelos usam somente **box UV**: o carregador de modelos do Cobblemon não aceita UV por face e, se encontrar um, o recarregamento dos recursos falha e o jogo trava.

## Como funciona

1. **Sorteio:** `data/cobblemon/species_features/multiverse_roll.json` é uma feature do tipo `choice` com `"default": "random"`. Toda vez que o Cobblemon cria um Pokémon dessas espécies (spawn natural, spawn de outros mods, comando sem a propriedade), ele sorteia um item da lista `choices`: 511 vezes `multiverse_none` e 1 vez `multiverseshinies`, ou seja, **1 chance em 512**.
2. **Atribuição:** `data/cobblemon/species_feature_assignments/multiverse_roll.json` liga o sorteio às espécies (Magikarp, Gyarados, Eevee, Vaporeon, Flareon, Jolteon, Espeon, Umbreon, Sylveon, Infernape e Rayquaza). O resultado fica salvo no Pokémon e acompanha a evolução entre essas espécies.
3. **Visual:** quando o sorteio dá `multiverseshinies`, o Pokémon ganha esse aspect e os resolvers em `assets/cobblemon/bedrock/pokemon/resolvers/` trocam a textura e o modelo. Onde há duas versões, o gênero decide: Vaporeon fêmea = Furina, macho = Neuvillette; Flareon fêmea = Mavuika, macho = Diluc; Jolteon fêmea = Fischl, macho = Flins; Sylveon fêmea = Varesa, macho = Dahlia; Espeon fêmea = Citlali, macho = Kaveh; Umbreon macho = Zhongli (a fêmea ainda não tem variante). O Infernape (macho ou fêmea) veste o cosplay de Super Saiyajin 4. O Rayquaza vira o Slifer, o Dragão Celeste, e, se o mod Mega Showdown estiver instalado, o Slifer que mega evolui vira o Mega Slifer.

Não existe spawn próprio: os Pokémon aparecem onde o Cobblemon (ou outro mod) já os coloca. Para mudar a chance, ajuste quantas vezes `multiverse_none` aparece na lista `choices` (por exemplo, 1023 vezes = 1 em 1024).


### Infernape SSJ4: fogo animado e fúria

O fogo do cabelo e das jubinhas corre da raiz para as pontas o tempo todo: a camada brilhante (`infernape_multiverse_flame1.png` a `flame10.png`) é uma textura animada de 10 quadros a 10 por segundo, do mesmo jeito que a chama do Charmander no Cobblemon.

Quando o Infernape Multiverse recebe buff numa batalha, ele entra em fúria (inspirado no anime): agacha, treme juntando força e explode de pé rugindo, com pedregulhos saindo do chão (`animation.multiverse_infernape.rage`, tocada pelo nome `multiverse_rage` do poser `infernape_multiverse.json`). O mesmo passo marca a variável `v.multiverse_rage` no Pokémon: enquanto ela estiver ligada e ele estiver em batalha, o poser mostra a cópia vermelha do cabelo (ossos `rage_*` do modelo, um pouco maiores que o fogo normal) e usa a pose `rage`. A variável some sozinha quando ele volta para a Poké Ball. Enquanto ele estiver com buff na batalha, o mod desenha a aura vermelha em volta dele (`RageAuraRenderer`) e empurra para longe quem chegar perto (`RageAuraPush`).

Para isso o mod substitui o `data/cobblemon/action_effects/misc/boost.json` do Cobblemon: os passos originais continuam iguais para todos os Pokémon, e só foi acrescentado um passo de animação que só tem efeito em Pokémon cujo poser tenha a animação `multiverse_rage` (hoje, só o Infernape Multiverse). Se uma atualização do Cobblemon mudar esse arquivo, ele precisa ser atualizado aqui também. Os pedregulhos são a partícula `bedrock/particles/multiverse_rage_rocks.particle.json`.

### Infernape SSJ4: nuvem voadora

Fora da batalha ele anda e corre em cima de uma nuvem amarela (ossos `nimbus*` do modelo; animações `cloud_walk` e `cloud_run`). O mod mede a velocidade dele no cliente e grava em `v.multiverse_running` para o poser escolher entre andar e correr (`CloudPoseDriver`).

Quando tem dono, ele voa de verdade atrás do treinador (`NimbusFlight`): decola quando o treinador se afasta uns 5 blocos ou sobe, voa ao lado dele com cerca de 1 bloco de ar embaixo da nuvem, acelera quando fica para trás, teleporta quando passa de 28 blocos e pousa depois de uns 3 segundos com o treinador parado perto dele. Em batalha, na água, ocupado, dormindo ou preso num pasto ele não voa. No ar o poser usa `cloud_fly` e `cloud_fly_fast` (`v.multiverse_flying`).

## Estrutura

```
Multiverse Shinies/
├── build.gradle, settings.gradle, gradle.properties, gradlew   # Projeto NeoForge (Gradle)
├── libs/                                # Jars do Cobblemon e do Kotlin for Forge para compilar (fora do git)
├── src/main/
│   ├── java/com/multiverseshinies/      # Código do mod (aura, empurrão, voo na nuvem)
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
│       ├── assets/multiverseshinies/    # Texturas da aura desenhada pelo mod
│       └── data/cobblemon/
│           ├── species_features/        # Sorteio multiverse_roll (1 em 512)
│           ├── species_feature_assignments/ # Quais espécies participam do sorteio
│           └── action_effects/misc/     # boost.json com o passo da fúria
└── blockbench/                          # Arquivos-fonte .bbmodel (não vão no jar)
```

A pasta `blockbench/` guarda os arquivos-fonte `.bbmodel` dos modelos customizados (Rayquaza "Slifer" em `rayquaza_slifer.bbmodel` e "Mega Slifer" em `rayquaza_mega_slifer.bbmodel`, ambos com as animações; Vaporeon "Furina", Vaporeon "Neuvillette", Flareon "Diluc", Flareon "Mavuika", Jolteon "Fischl", Jolteon "Flins", Sylveon "Dahlia" em `sylveon_dahlia_male.bbmodel`, com as animações, Sylveon "Varesa" em `sylveon_varesa_female.bbmodel`, Espeon "Citlali" em `espeon_citlali_female.bbmodel`, Espeon "Kaveh" em `espeon_kaveh_male.bbmodel`, Umbreon "Zhongli" em `umbreon_zhongli_male.bbmodel` e Infernape SSJ4 em `infernape_ssj4.bbmodel`), para editar no [Blockbench](https://www.blockbench.net/).

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
/pokespawn rayquaza multiverse_roll=multiverseshinies
/pokespawn sylveon multiverse_roll=multiverseshinies gender=male
/pokespawn sylveon multiverse_roll=multiverseshinies gender=female
/pokespawn espeon multiverse_roll=multiverseshinies gender=female
/pokespawn espeon multiverse_roll=multiverseshinies gender=male
/pokespawn umbreon multiverse_roll=multiverseshinies gender=male
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

A Sylveon "Varesa" (fêmea) usa as animações do Sylveon original; as fitas viram as tranças rosa dela e a saia envolve o quadril abrindo para trás. As texturas do Dahlia e da Varesa têm 512×512 (4 texels por unidade nas roupas) e o modelo usa UV por face.

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

Os modelos do Vaporeon "Furina" (fêmea), do Vaporeon "Neuvillette" (macho), do Flareon "Diluc" (macho) da Flareon "Mavuika" (fêmea) da Jolteon "Fischl" (fêmea) do Jolteon "Flins" (macho), do Sylveon "Dahlia" (macho), da Sylveon "Varesa" (fêmea), da Espeon "Citlali" (fêmea), do Espeon "Kaveh" (macho), do Umbreon "Zhongli" (macho), do Infernape SSJ4 e do Rayquaza "Slifer" são adaptações dos modelos oficiais do Vaporeon, do Flareon, do Jolteon, do Espeon, do Umbreon, do Sylveon, do Infernape e do Rayquaza do [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets) (licença Creative Commons, uso não comercial): foram adicionadas roupas e acessórios, e as texturas foram recoloridas. Os créditos do modelo original são dos autores do Cobblemon.

O Mega Rayquaza "Mega Slifer" (`multiverse_megarayquaza.geo.json`, `multiverse_megarayquaza.animation.json`, `rayquaza_multiverse_mega.png`, `rayquaza_multiverse_mega_emissive.png`, `rayquaza_multiverse_mega.json` e `blockbench/rayquaza_mega_slifer.bbmodel`) é uma adaptação do modelo, da textura e das animações do Mega Rayquaza do mod [Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown), de Yajat Kaul e colaboradores: foram adicionadas asas, a textura foi recolorida e foram criadas animações novas. Esses assets vêm do Mega Showdown e, como manda a [licença do Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown/blob/main/LICENSE.md), esses arquivos são distribuídos sob a licença [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) (uso não comercial, com crédito e mesma licença). Este mod não é afiliado ao Mega Showdown.

## Créditos

- **[Cobblemon](https://cobblemon.com/)** e o [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets): modelos, texturas e animações originais do Magikarp, Gyarados, Eevee, Vaporeon, Flareon, Jolteon, Espeon, Umbreon, Sylveon, Infernape e Rayquaza.
- **[Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown)**, de Yajat Kaul e colaboradores: modelo, textura e animações originais do Mega Rayquaza, base do Mega Slifer ([licença](https://github.com/yajatkaul/CobblemonMegaShowdown/blob/main/LICENSE.md); assets sob CC BY-NC-SA 4.0).
