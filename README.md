# XRpg 🎲

Um app Android leve para mesas de RPG com dados de **D4 a D100**.

## Recursos

- 🎲 D4, D6, D8, D10, D12, D20 e D100.
- 👤 Vários jogadores com seleção individual.
- 🔄 Cada dado é rolado individualmente por toque.
- 🛡️ Anti-zikamento configurável por jogador e por rodada.
- 🍀 Sorte e azar em pontos.
- 📊 Modo de sorte/azar por porcentagem.
- 2️⃣ Modo Dados 2D: dois dados no mesmo toque, com soma automática.
- ✨ Animações curtas e suaves, sem bibliotecas pesadas.
- 🎨 Interface de cores sólidas, alto contraste e visual próprio.
- Kotlin na interface + Java no motor matemático dos dados.

## Mecânica de sorte/azar

No modo de pontos, o resultado recebe `+ sorte - azar` e é limitado ao intervalo válido do dado.

No modo percentual, o resultado recebe um ajuste proporcional ao número de faces. Ex.: +10% em um D20 equivale a +2 antes do limite.

O anti-zikamento evita que um resultado ajustado termine no mínimo absoluto em dados com mais de duas faces. Ele não garante resultado alto: apenas corta o pior caso.

## Estrutura

- `app/src/main/java/com/peanut/xrpg/MainActivity.kt` — interface e fluxo do app.
- `app/src/main/java/com/peanut/xrpg/DiceEngine.java` — geração e aplicação dos modificadores.
- `app/src/main/res/values/styles.xml` — tema visual.
