# Multiverse Shinies

Addon para o mod [Cobblemon](https://cobblemon.com/) que adiciona variantes **"Multiverse"** de alguns Pokémon, com texturas e modelos próprios e spawns raros no mundo.

É um pack combinado: funciona como **datapack** (pasta `data/`) e como **resourcepack** (pasta `assets/`).

## Pokémon incluídos

| Pokémon | Textura | Modelo | Resolver | Spawn | Status |
|---|:-:|:-:|:-:|:-:|---|
| Magikarp (macho e fêmea) | ✅ | padrão | ✅ | ✅ | Funcional |
| Gyarados | ✅ | padrão | ✅ | ✅ | Funcional |
| Eevee (macho e fêmea) | ✅ | ✅ | ✅ | ✅ | Funcional |
| Flareon | ✅ | ✅ | ✅ | via Eevee | Funcional (animações do original) |
| Vaporeon (fêmea) — "Maestro" | ✅ | ✅ | ✅ | via Eevee | Funcional (animações do original) |
| Vaporeon (macho) — "Neuvillette" | ✅ | ✅ | ✅ | via Eevee | Funcional (animações do original) |
| Rayquaza | ✅ | ✅ | ✅ | ✅ | Funcional (animações do original) |

## Como funciona

1. **Feature:** `data/cobblemon/species_features/multiverseshinies.json` define a flag `multiverseshinies` (um aspect, desligado por padrão).
2. **Atribuição:** `data/cobblemon/species_feature_assignments/` liga a flag às espécies (Magikarp, Gyarados, Eevee e todas as evoluções, Rayquaza).
3. **Spawn:** `data/cobblemon/spawn_pool_world/` faz os Pokémon nascerem com `multiverseshinies=true`.
4. **Visual:** os resolvers em `assets/cobblemon/bedrock/pokemon/resolvers/` escolhem a textura e o modelo de acordo com o aspect.

### Spawns

| Pokémon | Bucket | Nível | Peso | Onde |
|---|---|---|---|---|
| Magikarp (macho) | rare | 1–20 | 5.0 | Água doce e oceano, submerso |
| Magikarp (fêmea) | rare | 1–20 | 2.0 | Água doce e oceano, submerso |
| Gyarados | rare | 20–50 | 1.0 | Água doce e oceano, submerso |
| Eevee | ultra-rare | 5–33 | 4.8 | Overworld, no chão, com luz do céu entre 8 e 15 |
| Rayquaza | ultra-rare | 70–90 | 0.1 | Overworld, no chão, céu aberto, altura Y ≥ 150 |

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
└── data/cobblemon/
    ├── species_features/           # Definição da flag multiverseshinies
    ├── species_feature_assignments/# Quais espécies recebem a flag
    └── spawn_pool_world/           # Regras de spawn
```

## Instalação

1. Instale o **Cobblemon**.
2. **Datapack:** copie a pasta (ou um `.zip` dela) para `saves/<seu mundo>/datapacks/`.
3. **Resourcepack:** copie a mesma pasta (ou `.zip`) para `resourcepacks/` e ative-a em *Opções → Pacotes de recursos*.

Para testar, invoque um Pokémon pelo comando:

```
/pokespawn eevee multiverseshinies=true shiny=true
/pokespawn vaporeon multiverseshinies=true female=true
/pokespawn vaporeon multiverseshinies=true male=true
/pokespawn flareon multiverseshinies=true
```

## Problemas conhecidos

- As outras evoluções do Eevee (Jolteon, Espeon, Umbreon, Leafeon, Glaceon e Sylveon) recebem a flag, mas ainda não têm textura e usam a aparência padrão.
- Os resolvers do Eevee exigem o aspect `shiny`, então a variante só aparece se o Pokémon também for shiny.
- O resolver do Magikarp macho exige `shiny`, mas o da fêmea não, então o comportamento entre os dois é diferente.
- O `pack_format` é `15` (Minecraft 1.20.2). Em versões mais novas o jogo pode mostrar um aviso de incompatibilidade.

## Licença

Ainda não definida.

Os modelos do Vaporeon "Maestro" (fêmea) e "Neuvillette" (macho) são adaptações do modelo oficial do Vaporeon do [cobblemon-assets](https://gitlab.com/cable-mc/cobblemon-assets) (licença Creative Commons, uso não comercial): foram adicionadas roupas e acessórios, e as texturas foram recoloridas. Os créditos do modelo original são dos autores do Cobblemon.
