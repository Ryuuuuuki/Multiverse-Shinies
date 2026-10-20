# Multiverse Shinies

Addon para o mod [Cobblemon](https://cobblemon.com/) que adiciona variantes **"Multiverse"** de alguns Pokémon, com texturas e modelos próprios. Funcionam como os shinies: qualquer Pokémon dessas espécies que nasce tem uma pequena chance de ser Multiverse.

É um pack combinado: funciona como **datapack** (pasta `data/`) e como **resourcepack** (pasta `assets/`).

## Pokémon incluídos

| Pokémon | Textura | Modelo | Resolver | Status |
|---|:-:|:-:|:-:|---|
| Magikarp (macho e fêmea) | ✅ | padrão | ✅ | Funcional |
| Gyarados | ✅ | padrão | ✅ | Funcional |
| Eevee (macho e fêmea) | ✅ | ✅ | ✅ | Funcional |
| Flareon (macho) — "Diluc" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Flareon (fêmea) — "Mavuika" | ✅ | ✅ | ✅ | Funcional (animações do original; pontas do cabelo brilham e óculos translúcidos via camadas) |
| Jolteon (macho) — "Flins" | ✅ | ✅ | ✅ | Funcional (animações do original; lanterna presa no quadril) |
| Jolteon (fêmea) — "Fischl" | ✅ | ✅ | ✅ | Funcional (animações do original; arco nas costas) |
| Sylveon (macho) — "Dahlia" | ✅ | ✅ | ✅ | Funcional (livro mágico animado voando em volta; reage nas batalhas) |
| Vaporeon (fêmea) — "Furina" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Vaporeon (macho) — "Neuvillette" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Rayquaza — "Slifer, o Dragão Celeste" | ✅ | ✅ | ✅ | Funcional (animações e poser próprios: voo em onda vertical batendo as asas; joia da testa brilha via camada) |
| Mega Rayquaza — "Mega Slifer" | ✅ | ✅ | ✅ | Opcional: só com o [Mega Showdown](https://modrinth.com/project/gUnQILfP) instalado (animações e poser próprios; asas maiores) |

## Como funciona

1. **Sorteio:** `data/cobblemon/species_features/multiverse_roll.json` é uma feature do tipo `choice` com `"default": "random"`. Toda vez que o Cobblemon cria um Pokémon dessas espécies (spawn natural, spawn de outros mods, comando sem a propriedade), ele sorteia um item da lista `choices`: 511 vezes `multiverse_none` e 1 vez `multiverseshinies`, ou seja, **1 chance em 512**.
2. **Atribuição:** `data/cobblemon/species_feature_assignments/multiverse_roll.json` liga o sorteio às espécies (Magikarp, Gyarados, Eevee, Vaporeon, Flareon, Jolteon, Sylveon e Rayquaza). O resultado fica salvo no Pokémon e acompanha a evolução entre essas espécies.
3. **Visual:** quando o sorteio dá `multiverseshinies`, o Pokémon ganha esse aspect e os resolvers em `assets/cobblemon/bedrock/pokemon/resolvers/` trocam a textura e o modelo. Onde há duas versões, o gênero decide: Vaporeon fêmea = Furina, macho = Neuvillette; Flareon fêmea = Mavuika, macho = Diluc; Jolteon fêmea = Fischl, macho = Flins; Sylveon macho = Dahlia (a fêmea ainda não tem variante). O Rayquaza vira o Slifer, o Dragão Celeste, e, se o mod Mega Showdown estiver instalado, o Slifer que mega evolui vira o Mega Slifer.

Não existe spawn próprio: os Pokémon aparecem onde o Cobblemon (ou outro mod) já os coloca. Para mudar a chance, ajuste quantas vezes `multiverse_none` aparece na lista `choices` (por exemplo, 1023 vezes = 1 em 1024).

## Estrutura

```
Multiverse Shinies/
├── pack.mcmeta
├── assets/cobblemon/
│   ├── textures/pokemon/           # Texturas das variantes
│   └── bedrock/pokemon/
│       ├── animations/             # Animações próprias (Rayquaza "Slifer", "Mega Slifer" e Sylveon "Dahlia")
│       ├── models/                 # Modelos geo customizados
│       ├── posers/                 # Animações e poses
│       └── resolvers/              # Mapeamento aspect → textura/modelo
├── data/cobblemon/
│   ├── species_features/           # Sorteio multiverse_roll (1 em 512)
│   └── species_feature_assignments/# Quais espécies participam do sorteio
└── blockbench/                     # Arquivos-fonte .bbmodel (o jogo ignora)
```

A pasta `blockbench/` guarda os arquivos-fonte `.bbmodel` dos modelos customizados (Rayquaza "Slifer" em `rayquaza_slifer.bbmodel` e "Mega Slifer" em `rayquaza_mega_slifer.bbmodel`, ambos com as animações; o Rayquaza Multiverse antigo continua em `rayquaza_multiverse.bbmodel`; Vaporeon "Furina", Vaporeon "Neuvillette", Flareon "Diluc", Flareon "Mavuika", Jolteon "Fischl", Jolteon "Flins" e Sylveon "Dahlia" em `sylveon_dahlia_male.bbmodel`, com as animações), para editar no [Blockbench](https://www.blockbench.net/).

## Instalação

1. Instale o **Cobblemon**.
2. **Datapack:** copie a pasta (ou um `.zip` dela) para `saves/<seu mundo>/datapacks/`.
3. **Resourcepack:** copie a mesma pasta (ou `.zip`) para `resourcepacks/` e ative-a em *Opções → Pacotes de recursos*.

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
```

O Rayquaza "Slifer" tem animações próprias em `animations/0384_rayquaza/multiverse_rayquaza.animation.json` (`ground_idle`, `blink`, `eye_size`, `air_fly` e `air_idle`) e um poser próprio (`posers/0384_rayquaza/rayquaza_multiverse.json`): parado usa `ground_idle`, pairando usa `air_idle` e voando ou nadando usa `air_fly`, em que o corpo e a cauda ondulam para cima e para baixo enquanto as asas batem (o pescoço e a cabeça ficam firmes). A joia azul da testa fica em `rayquaza_multiverse_emissive.png` e brilha no escuro.

### Sylveon "Dahlia" e o livro mágico

O Sylveon "Dahlia" usa as animações do Sylveon original e ganha o livro mágico da Dahlia, com animações próprias em `animations/0700_sylveon/multiverse_sylveon.animation.json` e poser próprio (`posers/0700_sylveon/sylveon_multiverse.json`):

- `book_float` (parado e andando): o livro dá uma volta em torno do Sylveon, para na frente, abre, folheia o livro todo em 2 segundos e fecha (ciclo de 10 s). Roda junto com as animações do Sylveon, porque só mexe nas partes do livro.
- Batalha e grito: cada uma junta a expressão do rosto do Sylveon com uma "expressão" do livro. `cry` = susto (o livro escancara e as folhas tremulam), `physical_attack` = raiva (o livro treme e morde com a capa), `special_attack` = `book_cast` (o livro avança, abre e folheia em 1 segundo), `status` = desconfiado (o livro vira de lado e fica entreaberto), `faint` = machucado (o livro fecha, cai, quica e fica no chão).
- O arquivo também traz as outras expressões do livro (`book_neutral`, `book_happy`, `book_sad`, `book_bored`, `book_mouth_open`, `book_mouth_open_ooo` e as já citadas) para uso futuro.

A textura do Dahlia tem 512×512 (4 texels por unidade nas roupas) e o modelo usa UV por face.

### Mega Slifer (opcional, precisa do Mega Showdown)

O Mega Slifer é a mega evolução do Slifer. Ele só funciona com o mod [Mega Showdown](https://modrinth.com/project/gUnQILfP) instalado: o resolver `resolvers/0384_rayquaza/2_rayquaza_multiverse_mega.json` (ordem 2, acima do Mega Rayquaza do Mega Showdown e do Slifer) só vale quando o Pokémon tem os aspects `multiverseshinies` e `mega` ao mesmo tempo, e quem dá o aspect `mega` é o Mega Showdown. Sem ele, os arquivos ficam no pack sem uso e não interferem em nada; o addon não inclui nem altera nada do Mega Showdown.

Ele usa o modelo `multiverse_megarayquaza.geo.json`, as texturas `rayquaza_multiverse_mega.png` e `rayquaza_multiverse_mega_emissive.png` (fitas, olhos e joia brilham) e o poser `rayquaza_multiverse_mega.json`. As animações (`multiverse_megarayquaza.animation.json`) são as do Mega Rayquaza do Mega Showdown com as asas batendo no `ground_idle` e no `ground_walk`, mais `air_fly`, `air_idle` e `eye_size` no estilo do Slifer.

A Flareon "Mavuika" usa duas camadas extras no resolver: `flareon_multiverse_female_emissive.png` (pontas neon do cabelo, brilham no escuro) e `flareon_multiverse_female_lenses.png` (lentes dos óculos, translúcidas).

## Problemas conhecidos

- O Cobblemon não faz Rayquaza nascer sozinho; o Rayquaza Multiverse só aparece se outro mod (ou um comando) criar um Rayquaza.
- As outras evoluções do Eevee (Espeon, Umbreon, Leafeon e Glaceon) e a Sylveon fêmea ainda não têm variante.
- O `pack_format` é `15` (Minecraft 1.20.2). Em versões mais novas o jogo pode mostrar um aviso de incompatibilidade.

## Próximas atualizações

- **Animações próprias para os Pokémon Multiverse.** O Rayquaza "Slifer" já tem as suas; os outros ainda usam as animações e o poser da espécie original. A ideia é criar animações que combinem com cada personagem, por exemplo:
  - Flareon "Diluc": balançar a espada nas costas no idle, golpe de espada no ataque.
  - Flareon "Mavuika": ajeitar os óculos no idle, chamas do cabelo mais vivas ao andar.
  - Vaporeon "Furina": pose teatral, girando e fazendo reverência com a cartola.
  - Vaporeon "Neuvillette": postura séria e elegante, abaixando a cabeça como cumprimento.
  - Cada animação nova precisa de um poser próprio em `posers/` apontado pelo resolver da variante.

## Licença

Ainda não definida, exceto pelos arquivos do Mega Slifer (veja abaixo).

Os modelos do Vaporeon "Furina" (fêmea), do Vaporeon "Neuvillette" (macho), do Flareon "Diluc" (macho) da Flareon "Mavuika" (fêmea) da Jolteon "Fischl" (fêmea) do Jolteon "Flins" (macho), do Sylveon "Dahlia" (macho) e do Rayquaza "Slifer" são adaptações dos modelos oficiais do Vaporeon, do Flareon, do Jolteon, do Sylveon e do Rayquaza do [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets) (licença Creative Commons, uso não comercial): foram adicionadas roupas e acessórios, e as texturas foram recoloridas. Os créditos do modelo original são dos autores do Cobblemon.

O Mega Rayquaza "Mega Slifer" (`multiverse_megarayquaza.geo.json`, `multiverse_megarayquaza.animation.json`, `rayquaza_multiverse_mega.png`, `rayquaza_multiverse_mega_emissive.png`, `rayquaza_multiverse_mega.json` e `blockbench/rayquaza_mega_slifer.bbmodel`) é uma adaptação do modelo, da textura e das animações do Mega Rayquaza do mod [Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown), de Yajat Kaul e colaboradores: foram adicionadas asas, a textura foi recolorida e foram criadas animações novas. Esses assets vêm do Mega Showdown e, como manda a [licença do Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown/blob/main/LICENSE.md), esses arquivos são distribuídos sob a licença [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/) (uso não comercial, com crédito e mesma licença). Este addon não é afiliado ao Mega Showdown.

## Créditos

- **[Cobblemon](https://cobblemon.com/)** e o [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets): modelos, texturas e animações originais do Magikarp, Gyarados, Eevee, Vaporeon, Flareon, Jolteon, Sylveon e Rayquaza.
- **[Mega Showdown](https://github.com/yajatkaul/CobblemonMegaShowdown)**, de Yajat Kaul e colaboradores: modelo, textura e animações originais do Mega Rayquaza, base do Mega Slifer ([licença](https://github.com/yajatkaul/CobblemonMegaShowdown/blob/main/LICENSE.md); assets sob CC BY-NC-SA 4.0).
