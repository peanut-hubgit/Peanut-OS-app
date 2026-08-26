# XRpg 🎲

Um app Android leve para mesas de RPG com dados de **D4 a D100**.

## Recursos

- 🎲 D4, D6, D8, D10, D12, D20 e D100.
- 👤 Vários jogadores com seleção individual.
- 🔄 Cada dado pode ser rolado individualmente.
- 🛡️ Anti-zikamento configurável por jogador e por rodada.
- 🍀 Sorte e azar em pontos.
- 📊 Modo de sorte/azar por porcentagem.
- 2️⃣ Modo Dados 2D com animação dos números antes do resultado.
- 🧊 Modo Dados 3D nativo com OpenGL ES 2.0, múltiplos dados e animação de rolagem.
- ✨ Animações suaves sem engine 3D externa.
- 🎨 Interface de cores sólidas, cantos arredondados e visual inspirado no AOSP/Material.
- ❤️ Área de doação com chave PIX do projeto.
- Kotlin na interface + Java no motor matemático e renderizador 3D.

## Mecânica de sorte/azar

No modo de pontos, o resultado recebe `+ sorte - azar` e é limitado ao intervalo válido do dado.

No modo percentual, o ajuste é proporcional ao número de faces e também respeita o limite do dado.

O anti-zikamento evita o pior caso quando ativado para a rodada.

## Estrutura

- `app/src/main/java/com/peanut/xrpg/MainActivity.kt` — interface e fluxo do app.
- `app/src/main/java/com/peanut/xrpg/DiceEngine.java` — geração e aplicação dos modificadores.
- `app/src/main/java/com/peanut/xrpg/Dice2DView.java` — visualização 2D.
- `app/src/main/java/com/peanut/xrpg/Dice3DView.java` — renderizador OpenGL ES 2.0.
- `app/src/main/res/values/styles.xml` — tema visual.
