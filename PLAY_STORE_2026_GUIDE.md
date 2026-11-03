# 🚀 Guia Completo de Publicação na Google Play Store (2026) — **OndaMic**

> **Aplicativo**: **OndaMic — Seu Android vira um microfone**
> **Application ID**: `com.ondamic.app`
> **Target SDK**: Android 16 (`API 36`) | **Min SDK**: Android 9.0 (`API 28`)
> **Alinhamento de Página de Memória**: 16 KB (`useLegacyPackaging = false` configurado)
> **Permissão de Rede**: **Nenhuma** (`Zero-Internet` por design para aprovação expressa em Segurança dos Dados)

---

## 1. Requisitos Técnicos de 2026 Já Configurados no Projeto

1. **API Nível 36 (Android 16)**: A partir de 2026, todos os novos aplicativos e atualizações na Google Play devem ter `targetSdk = 36`.
2. **Suporte a Páginas de Memória de 16 KB**: O `app/build.gradle.kts` já inclui `packaging.jniLibs.useLegacyPackaging = false` para conformidade com dispositivos ARM64 de 16 KB.
3. **Foreground Service de Microfone (`FOREGROUND_SERVICE_MICROPHONE`)**: Configurado em `OndaMicForegroundService.kt` com `ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE`, iniciando **exclusivamente** após interação direta do usuário com o app aberto e exibindo notificação persistente com botão **"DESLIGAR MICROFONE"**.
4. **36+ Idiomas com `LocaleConfig`**: Configurado em `res/xml/locales_config.xml` para que o Android 13–16 exiba o seletor nativo de idiomas também nas Configurações do Sistema, além do seletor interno do app.

---

## 2. Geração do Android App Bundle (.AAB) Assinado para Produção

### Passo 2.1: Criar sua Keystore de Produção
Execute no terminal (substitua as senhas por senhas fortes e guarde o arquivo `.jks` em local seguro fora do repositório):
```powershell
keytool -genkey -v -keystore ondamic-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ondamic
```

### Passo 2.2: Compilar o Bundle `.aab` de Produção
No Android Studio: **Build > Generate Signed App Bundle / APK > Android App Bundle (.aab)**, ou via linha de comando:
```powershell
.\gradlew.bat bundleRelease
```
O arquivo gerado estará em `app/build/outputs/bundle/release/app-release.aab`.

---

## 3. Declaração Obrigatória de Permissão de Foreground Service (Play Console 2026)

Na seção **Política e programas > Conteúdo do app > Permissões de serviço em primeiro plano (Foreground Service)** do Google Play Console, marque **`MICROPHONE`** e cole o texto abaixo:

### Texto Pronto para Colar na Justificativa (PT-BR / EN):
> **PT-BR**:
> O OndaMic é uma ferramenta de acessibilidade vocal e amplificação de voz em tempo real (microfone sem fio/cabeado para palestras, professores, guias turísticos e eventos). O usuário toca explicitamente no botão principal "TOQUE PARA AMPLIFICAR" para capturar sua voz pelo microfone do dispositivo e reproduzi-la em tempo real em uma caixa de som conectada (via cabo P2, USB-C ou Bluetooth). O Foreground Service do tipo `microphone` é estritamente necessário para que a amplificação da voz do orador não seja interrompida abruptamente quando a tela do smartphone se apaga ou é bloqueada no bolso/púlpito durante uma palestra ou apresentação. Uma notificação persistente ("OndaMic está NO AR") permanece visível o tempo todo com um botão de ação direta ("DESLIGAR MICROFONE") que encerra a captura imediatamente.
>
> **EN (Recommended for Global Reviewers)**:
> OndaMic is a real-time voice amplification and live microphone utility for lecturers, teachers, tour guides, and public speakers. The user explicitly taps the "TAP TO AMPLIFY" button on the main screen to capture their voice via the device microphone and route it in real time to an external speaker (Wired Aux P2, USB-C DAC, or Bluetooth speaker). The `microphone` Foreground Service is strictly required so that live voice amplification is not abruptly cut off mid-speech when the device screen dims or locks while held in the speaker's hand or placed on a podium. A persistent notification ("OndaMic is ON AIR") is displayed at all times with an immediate "STOP MIC NOW" action button.

### Roteiro de 25 Segundos para o Vídeo de Demonstração Obrigatório (Link Não Listado do YouTube):
A Google Play exige um vídeo curto mostrando o fluxo do usuário:
1. **(0s–5s)**: Mostre o app **OndaMic** aberto na tela inicial em estado `PRONTO • EM ESPERA`.
2. **(5s–12s)**: Toque no botão central **TOQUE PARA AMPLIFICAR**. Mostre o indicador mudando para **NO AR • AO VIVO** e as barras do medidor VU reagindo à sua voz.
3. **(12s–18s)**: Bloqueie a tela do celular (ou deslize a barra de notificações do Android) mostrando a notificação persistente **"OndaMic está NO AR"** e demonstre que a voz continua sendo amplificada.
4. **(18s–25s)**: Toque no botão **"DESLIGAR MICROFONE"** diretamente na notificação e mostre que o serviço encerra na hora.

---

## 4. Formulário "Segurança dos Dados" (Data Safety) — Respostas Exatas

Como o **OndaMic** **NÃO solicita a permissão `android.permission.INTERNET`**, o preenchimento é 100% transparente:
- **O app coleta ou compartilha algum dos tipos de dados obrigatórios do usuário?** → **NÃO** (*No*).
- **Explicação técnica**: Todo o processamento de áudio (DSP, equalização, supressão de microfonia e gravação opcional em `.wav`) ocorre exclusivamente na memória local do dispositivo, sem qualquer conectividade de rede, SDK de anúncios ou telemetria externa.

---

## 5. Política de Privacidade Pronta (Hospede no GitHub Pages ou Google Sites)

```markdown
# Política de Privacidade — OndaMic ("Seu Android vira um microfone")
**Última atualização: Novembro de 2026**

O aplicativo **OndaMic** (`com.ondamic.app`) foi projetado sob o princípio de **Privacidade Absoluta por Design (100% Offline)**.

1. **Coleta e Transmissão de Dados**: O OndaMic **não possui permissão de acesso à Internet** (`android.permission.INTERNET`) em seu código-fonte. Portanto, é tecnicamente impossível que o aplicativo envie, colete, compartilhe ou armazene sua voz ou dados pessoais em servidores externos.
2. **Uso do Microfone (`RECORD_AUDIO`)**: A permissão de microfone é utilizada exclusivamente em tempo real na memória RAM do próprio aparelho para aplicar filtros acústicos (equalização, controle de ganho, redução de ruído e anti-microfonia) e encaminhar o som para a saída de áudio escolhida pelo usuário (cabo P2, USB ou Bluetooth).
3. **Gravações Locais Opcionais**: Caso o usuário ative manualmente a função "Gravação Local Simultânea em WAV", os arquivos de áudio são salvos exclusivamente no armazenamento interno privado do aplicativo no próprio telefone, podendo ser excluídos ou compartilhados a qualquer momento pelo próprio usuário.
4. **Zero Anúncios e Zero Rastreadores**: O OndaMic não contém SDKs de publicidade, analytics ou rastreamento de terceiros.
```

---

## 6. Fichas da Loja Otimizadas (ASO Multi-Idiomas)

> **Nota sobre Limites de Caracteres do Google Play Console**:
> - **Título do App**: Máximo de **30 caracteres**
> - **Descrição Curta**: Máximo de **80 caracteres**
> - **Descrição Completa**: Máximo de **4.000 caracteres**

### 🇧🇷 Português (Brasil) — `pt-BR`
- **Título (29 caracteres)**: `OndaMic: Android é Microfone`
- **Descrição Curta (78 caracteres)**: `Seu Android vira um microfone! Voz ao vivo em caixas P2/Bluetooth sem anúncios`
- **Descrição Completa**:
  > **OndaMic — Seu Android vira um microfone profissional de bolso!**
  >
  > Precisa dar uma aula, fazer uma palestra, conduzir um evento, anunciar em uma loja ou cantar no karaokê e não tem um microfone dedicado? O **OndaMic** transforma seu smartphone Android em um microfone sem fio ou cabeado versátil para caixas de som com entrada **P2 (Auxiliar), USB-C ou Bluetooth** — 100% gratuito, **sem propagandas invasivas** e **100% offline**.
  >
  > **🌟 Por que o OndaMic é diferente de tudo na Play Store?**
  > • **Modo Chamada Clara (Voice Focus) vs. Estúdio Baixa Latência**: Escolha usar o processador de chamadas do próprio aparelho (com cancelamento de eco, supressão de ruído e Foco de Voz da fabricante) ou a via rápida de baixa latência!
  > • **Modo Mono ou Estéreo Dual-Mic Real (Canal L + Canal R)**: Transmita dois microfones físicos do celular simultaneamente em canais separados (Esquerdo e Direito) com controle de balanço estéreo!
  > • **Escolha qual Microfone Físico usar**: Selecione entre o microfone inferior, superior/traseiro (câmera), fone com fio ou Bluetooth, além de ajustar a direção do feixe acústico (Frontal, Traseiro ou 360°).
  > • **Escudo Anti-Microfonia Inteligente**: Filtro Auto-Notch FFT que detecta e corta apitos automaticamente, Inversão de Fase de 180°, Noise Gate, Filtro Corta-Graves (HPF) e Limitador de proteção.
  > • **Brilho, Reverb e Eco Ajustáveis**: Controle individualmente (ON/OFF e nível de 0 a 100%) o Brilho Vocal, Reverb de Palco (Estúdio, Auditório, Arena) e Eco (Delay em ms), além de Equalizador de 5 Bandas.
  > • **Feito para o Palco**: Modos Contínuo, Segurar para Falar (Push-to-Talk na tela ou no botão físico de volume), Ativação por Voz (VOX), botão Tosse/Mute, Gongo de anúncio ("Ding-Dong"), Auto-Ducking para falar sobre música de fundo e Kill-Switch que muta na hora se o cabo P2 desconectar!
  > • **36+ Idiomas Nativos**: Inclui Português, Inglês, Espanhol, Francês, Crioulo Haitiano, Suaíli, Iorubá, Hauçá, Amárico, Zulu, Lingala e dezenas de outros idiomas.

### 🇺🇸 Inglês (Global) — `en-US`
- **Título (30 caracteres)**: `OndaMic: Android to Microphone`
- **Descrição Curta (79 caracteres)**: `Your Android becomes a microphone! Live voice amp for Aux/BT with zero ads.`

### 🇭🇹 Crioulo Haitiano — `ht`
- **Título (27 caracteres)**: `OndaMic: Mikwofòn Andwoyid`
- **Descrição Curta (76 caracteres)**: `Andwoyid ou tounen yon mikwofòn! Anplifye vwa ou sou bwat P2 ak Bluetooth.`
