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
| Vaporeon (fêmea) — "Furina" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Vaporeon (macho) — "Neuvillette" | ✅ | ✅ | ✅ | Funcional (animações do original) |
| Rayquaza | ✅ | ✅ | ✅ | Funcional (animações do original) |

## Como funciona

1. **Sorteio:** `data/cobblemon/species_features/multiverse_roll.json` é uma feature do tipo `choice` com `"default": "random"`. Toda vez que o Cobblemon cria um Pokémon dessas espécies (spawn natural, spawn de outros mods, comando sem a propriedade), ele sorteia um item da lista `choices`: 511 vezes `multiverse_none` e 1 vez `multiverseshinies`, ou seja, **1 chance em 512**.
2. **Atribuição:** `data/cobblemon/species_feature_assignments/multiverse_roll.json` liga o sorteio às espécies (Magikarp, Gyarados, Eevee, Vaporeon, Flareon, Jolteon e Rayquaza). O resultado fica salvo no Pokémon e acompanha a evolução entre essas espécies.
3. **Visual:** quando o sorteio dá `multiverseshinies`, o Pokémon ganha esse aspect e os resolvers em `assets/cobblemon/bedrock/pokemon/resolvers/` trocam a textura e o modelo. Onde há duas versões, o gênero decide: Vaporeon fêmea = Furina, macho = Neuvillette; Flareon fêmea = Mavuika, macho = Diluc; Jolteon fêmea = Fischl, macho = Flins.

Não existe spawn próprio: os Pokémon aparecem onde o Cobblemon (ou outro mod) já os coloca. Para mudar a chance, ajuste quantas vezes `multiverse_none` aparece na lista `choices` (por exemplo, 1023 vezes = 1 em 1024).

## Estrutura

```
Multiverse Shinies/
├── pack.mcmeta
├── assets/cobblemon/
│   ├── textures/pokemon/           # Texturas das variantes
│   └── bedrock/pokemon/
│       ├── models/                 # Modelos geo customizados
│       ├── posers/                 # Animações e poses
│       └── resolvers/              # Mapeamento aspect → textura/modelo
├── data/cobblemon/
│   ├── species_features/           # Sorteio multiverse_roll (1 em 512)
│   └── species_feature_assignments/# Quais espécies participam do sorteio
└── blockbench/                     # Arquivos-fonte .bbmodel (o jogo ignora)
```

A pasta `blockbench/` guarda os arquivos-fonte `.bbmodel` dos modelos customizados (Rayquaza, Vaporeon "Furina", Vaporeon "Neuvillette", Flareon "Diluc", Flareon "Mavuika", Jolteon "Fischl" e Jolteon "Flins"), para editar no [Blockbench](https://www.blockbench.net/).

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
```

A Flareon "Mavuika" usa duas camadas extras no resolver: `flareon_multiverse_female_emissive.png` (pontas neon do cabelo, brilham no escuro) e `flareon_multiverse_female_lenses.png` (lentes dos óculos, translúcidas).

## Problemas conhecidos

- O Cobblemon não faz Rayquaza nascer sozinho; o Rayquaza Multiverse só aparece se outro mod (ou um comando) criar um Rayquaza.
- As outras evoluções do Eevee (Espeon, Umbreon, Leafeon, Glaceon e Sylveon) ainda não têm variante.
- O `pack_format` é `15` (Minecraft 1.20.2). Em versões mais novas o jogo pode mostrar um aviso de incompatibilidade.

## Próximas atualizações

- **Animações próprias para os Pokémon Multiverse.** Hoje todos usam as animações e o poser da espécie original. A ideia é criar animações que combinem com cada personagem, por exemplo:
  - Flareon "Diluc": balançar a espada nas costas no idle, golpe de espada no ataque.
  - Flareon "Mavuika": ajeitar os óculos no idle, chamas do cabelo mais vivas ao andar.
  - Vaporeon "Furina": pose teatral, girando e fazendo reverência com a cartola.
  - Vaporeon "Neuvillette": postura séria e elegante, abaixando a cabeça como cumprimento.
  - Rayquaza: idle e voo próprios.
  - Cada animação nova precisa de um poser próprio em `posers/` apontado pelo resolver da variante.

## Licença

Ainda não definida.

Os modelos do Vaporeon "Furina" (fêmea), do Vaporeon "Neuvillette" (macho), do Flareon "Diluc" (macho) da Flareon "Mavuika" (fêmea) da Jolteon "Fischl" (fêmea) e do Jolteon "Flins" (macho) são adaptações dos modelos oficiais do Vaporeon, do Flareon e do Jolteon do [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets) (licença Creative Commons, uso não comercial): foram adicionadas roupas e acessórios, e as texturas foram recoloridas. Os créditos do modelo original são dos autores do Cobblemon.
