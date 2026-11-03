# Guia de Contribuição — OndaMic

Ficamos muito felizes pelo seu interesse em contribuir com o **OndaMic**! Este projeto nasceu para fornecer uma ferramenta de amplificação de voz madura, moderna, 100% offline, gratuita e acessível para qualquer pessoa no planeta.

---

## 🎯 Princípios Fundamentais do Projeto

1. **Privacidade Absoluta (Zero-Internet)**: O OndaMic **NUNCA** deve declarar a permissão `android.permission.INTERNET`. Todo processamento de áudio, gravação e inteligência de filtragem deve ser local no aparelho.
2. **Desempenho de Áudio e Zero-GC**: O pipeline de DSP roda em threads de prioridade urgente (`Process.THREAD_PRIORITY_URGENT_AUDIO`). Evite alocações de objetos (`new`, cópias desnecessárias de buffers) dentro de loops de áudio para evitar pausas do Garbage Collector do Android.
3. **Inclusão Linguística Global**: O suporte a idiomas menos assistidos pelas Big Techs (Crioulo Haitiano, idiomas de nações africanas e povos originários da América do Sul) é prioridade máxima.

---

## 🚀 Principais Áreas para Contribuição

Buscamos contribuições especialmente nas seguintes frentes:

### 1. Tradução e Revisão Linguística (L10n)
- Revisão por falantes nativos dos arquivos `strings.xml` existentes (especialmente Crioulo Haitiano `ht`, Suaíli `sw`, Iorubá `yo`, Hauçá `ha`, Zulu `zu`, Lingala `ln`, Guarani `gn`, etc.).
- Adição de novos dialetos ou línguas regionais.
- Ajuste de termos técnicos de áudio em cada cultura para melhor compreensão por leigos.

### 2. Algoritmos de Áudio e DSP
- Aprimoramento do detector de microfonia (FFT / Goertzel).
- Otimizações no motor de Reverb e novos modelos acústicos de sala.
- Implementação de cancelamento adaptativo ou rotinas em C++ (Oboe / AAudio NDK) se houver ganho comprovado de latência em aparelhos antigos.

### 3. Compatibilidade com Fabricantes (OEMs)
- Testes e mapeamento de comportamento específico de APIs de áudio em marcas com personalizações pesadas (Samsung One UI, Xiaomi HyperOS, Transsion/Infinix, Motorola, Google Pixel).
- Relatos de mapeamento de cápsulas físicas (`preferredDevice` / `MicrophoneInfo`).

### 4. Acessibilidade (a11y) e Ergonomia
- Melhorias na navegação por TalkBack e leitores de tela.
- Otimização do contraste visual para uso sob luz solar forte ou em palcos escuros.
- Suporte a novos controles externos (pedais Bluetooth HID, fones com botão PTT).

---

## 🛠️ Como Configurar o Ambiente de Desenvolvimento

1. **Pré-requisitos**:
   - JDK 17 ou 21
   - Android Studio (versão Ladybug / Meerkat ou superior)
   - Android SDK Platforms 35 e 36 (`compileSdk = 36`)
   - Git

2. **Passo a Passo**:
   ```bash
   git clone https://github.com/thepreiss/OndaMic.git
   cd OndaMic
   ```
3. **Rodar os Testes Unitários**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Compilar o APK de Debug**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📋 Padrões de Código e Commits

- Utilize Kotlin idiomático e Jetpack Compose com padrões Material 3.
- Formatação de commits baseada em [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat: adiciona suporte ao idioma X`
  - `fix: corrige threshold do noise gate no modo VOX`
  - `docs: atualiza guia de publicação da Play Store`
  - `test: adiciona caso de teste para inversão de fase estéreo`

---

## 📬 Como Enviar sua Contribuição (Pull Request)

1. Crie um Fork do projeto.
2. Crie uma branch para sua funcionalidade (`git checkout -b feature/minha-melhoria`).
3. Faça suas alterações e garanta que `./gradlew testDebugUnitTest` passe sem falhas.
4. Faça commit das mudanças (`git commit -m 'feat: minha melhoria'`).
5. Envie para o seu fork (`git push origin feature/minha-melhoria`).
6. Abra um **Pull Request** detalhando as motivações e screenshots/vídeos (se aplicável).
