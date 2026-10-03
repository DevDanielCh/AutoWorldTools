# AutoWorldTools

Plugin para Spigot/Paper que **reseta e cria backup de mundos automaticamente** em dias e horários definidos, com opção de aviso prévio (contagem regressiva), portais de retorno, integração com DiscordSRV e limpeza de mapas do Dynmap.

Desenvolvido por [rypengu23](https://github.com/rypengu23).

---

## Índice

- [Requisitos](#requisitos)
- [Instalação](#instalação)
- [Funcionalidades](#funcionalidades)
  - [Reset automático](#reset-automático)
  - [Regeneração dos portais (warp gates)](#regeneração-dos-portais-warp-gates)
  - [Backup automático](#backup-automático)
  - [Reinício automático do servidor](#reinício-automático-do-servidor)
  - [Integração com DiscordSRV](#integração-com-discordsrv)
  - [Integração com Dynmap](#integração-com-dynmap)
- [Comandos](#comandos)
- [Permissões](#permissões)
- [Configuração](#configuração)
- [Compatibilidade](#compatibilidade)
- [Compilando a partir do código](#compilando-a-partir-do-código)
- [Licença](#licença)

---

## Requisitos

| Item | Versão |
| --- | --- |
| Servidor | Spigot / Paper 1.16+ (testado até 1.18.2) |
| Java | **17+** no servidor (requisito do Multiverse 5.x) |
| Multiverse-Core | **Opcional** — não é mais necessário para o reset (veja [Compatibilidade](#compatibilidade)) |
| Multiverse-Portals | **Opcional** — necessário apenas para reconfigurar portais / gerar warp gates (versão 5.x) |
| DiscordSRV | **Opcional** — envia avisos no Discord |
| dynmap | **Opcional** — permite apagar os mapas dos mundos resetados |

Plugins opcionais são carregados via `softdepend`, portanto não quebram a inicialização caso não estejam instalados.

---

## Instalação

1. Baixe o `.jar` na página de releases do projeto.
2. Copie o arquivo para a pasta `plugins/` do servidor.
3. Inicie o servidor. O plugin gera `plugins/AutoWorldTools/config.yml`.
4. Edite o `config.yml`, ajustando os mundos que serão resetados e/ou salvos em backup.
5. Use `/awt reload` para aplicar as mudanças.

> O reset acontece **fora do Bukkit**, ou seja, a pasta do mundo é apagada e o mundo é recriado do zero. Todo o conteúdo dos mundos listados em `resetWorldInfo` **será perdido**.

---

## Funcionalidades

### Reset automático

No horário configurado, todos os mundos listados no `config.yml` são recriados:

1. Os jogadores presentes no mundo são teleportados para o spawn do mundo `world`.
2. O mundo é descarregado (`Bukkit.unloadWorld`) e a pasta do mundo é apagada.
3. O mundo é recriado com o `WorldCreator`, já com a seed definida.
4. A borda do mundo (world border) é centralizada em `0,0` e redimensionada para o diâmetro configurado.
5. Os portais e os warp gates são reconstruídos (se o Multiverse-Portals estiver habilitado).

Recursos adicionais:

- **Seed por mundo**: `nomeDoMundo:1234567890` define uma seed fixa. Sem a seed, um valor aleatório é sorteado a cada reset.
- **Aviso prévio**: contagem regressiva configurable (`300,60,30,10,5,4,3,2,1` = 5 minutos, 1 minuto, 30s, 10s, 5s… até 1s).
- **Múltiplos dias e horários**: ex. `sun,wed` às `00:00,12:00`.

### Regeneração dos portais (warp gates)

Com o Multiverse-Portals habilitado (`gateInfo.useMultiversePortals: true`), o plugin reconfigura o portal de cada mundo logo após o reset, para que o portal volte a existir no mundo recém-criado.

Com `gateAutoBuildOf*` habilitado, o plugin também **constrói uma estrutura (gate)** ao redor do portal:

- O tamanho do portal é redefinido para **1×1×2**.
- A borda externa é construída com `bedrock`, o chão com `bedrock` e o contorno com `glass`.
- Uma tocha é colocada no centro, no ponto de teleporte.
- O spawn do mundo é reposicionado logo à frente do gate.

Ao final, o plugin **reinicia o Multiverse-Portals** (`onDisable()` + `onEnable()`) para que os portais sejam relidos do disco.

> ⚠️ Se você usa Multiverse-Portals, mantenha `useMultiversePortals: true` no config. A lista de nomes de mundos e de portais deve ter **a mesma quantidade de itens**; caso contrário a geração é ignorada.

### Backup automático

No horário configurado, os mundos listados em `backupWorldName` são salvos em arquivos `.zip` na pasta `backupLocation.fileLocation`.

- O backup roda de forma **assíncrona**, sem travar o servidor.
- O nome do arquivo inclui a data e a hora do backup.
- Arquivos antigos são removidos automaticamente, mantendo apenas a quantidade definida em `backupLimit`.
- Também é possível fazer backup manual de um mundo específico via comando.

### Reinício automático do servidor

No horário configurado, o servidor é reiniciado usando `Bukkit.spigot().restart()`, com mensagem de aviso e contagem regressiva (mesmo esquema do reset).

### Integração com DiscordSRV

Com `useDiscordSRV: true`, o plugin envia ao canal principal do Discord as mensagens de início/ fim do reset, do backup e do reinício, incluindo a contagem regressiva. As mensagens são configuráveis em `message_xx.yml`.

### Integração com Dynmap

Com `useDynmap: true`, o plugin executa `dynmap purgeworld <nomeDoMundo>` para apagar os tiles já renderizados dos mundos que foram resetados, evitando que os mapas continuem exibindo o terreno antigo.

Você pode definir isso por tipo de mundo (`mapPurgeAtResetOfNormal/Nether/End`) ou informar mundo por mundo em `mapPurgeAtResetWorldName`.

---

## Comandos

Alias principal: `/awt` (ou `/autoworldtools`).

| Comando | Descrição | Permissão |
| --- | --- | --- |
| `/awt reset normal` | Reseta os mundos Overworld do config | `autoWorldTools.reset` |
| `/awt reset nether` | Reseta os mundos Nether do config | `autoWorldTools.reset` |
| `/awt reset end` | Reseta os mundos The End do config | `autoWorldTools.reset` |
| `/awt reset all` | Reseta todos os tipos de mundo | `autoWorldTools.reset` |
| `/awt reset info` | Mostra os horários de reset automático | `autoWorldTools.resetInfo` |
| `/awt backup <nomeDoMundo>` | Faz backup do mundo informado | `autoWorldTools.backup` |
| `/awt backup info` | Mostra os horários de backup automático | `autoWorldTools.backupInfo` |
| `/awt restart info` | Mostra os horários de reinício automático | `autoWorldTools.restartInfo` |
| `/awt reload` | Recarrega o `config.yml` | `autoWorldTools.admin` |
| `/awt help [página]` | Exibe o guia de comandos | — |

---

## Permissões

| Permissão | Descrição | Padrão |
| --- | --- | --- |
| `autoWorldTools.*` | Acesso a todas as permissões | op |
| `autoWorldTools.admin` | Reset manual, backup manual e reload do config | op |
| `autoWorldTools.reset` | Reset manual de mundos | op |
| `autoWorldTools.backup` | Backup manual de mundos | op |
| `autoWorldTools.resetInfo` | Consultar horários de reset | todos |
| `autoWorldTools.backupInfo` | Consultar horários de backup | todos |
| `autoWorldTools.restartInfo` | Consultar horários de reinício | todos |

`autoWorldTools.admin` já inclui as permissões de `reset`, `backup`, `resetInfo`, `backupInfo` e `restartInfo`.

---

## Configuração

Arquivo: `plugins/AutoWorldTools/config.yml`.

Principais seções:

```yaml
setting:
  language: "en"              # idioma das mensagens no chat (en/ja)
  consoleLanguage: "en"       # idioma das mensagens no console (en/ja)
  autoReset: true             # habilita o reset automático
  autoBackup: true            # habilita o backup automático
  autoRestart: true           # habilita o reinício automático
  useDiscordSRV: false        # envia mensagens para o Discord
  backupLimit: 30             # quantidade máxima de backups mantidos por mundo

resetTime:
  resetDayOfTheWeek: "sun"    # sun, mon, tue, wed, thu, fri, sat (aceita múltiplos)
  resetTime: "00:00"          # horário(s) do reset
  resetNotifyTime: "300,60,30,10,5,4,3,2,1"   # avisos prévios em segundos

resetWorldInfo:
  worldNameOfNormal: "material,material2:0123456789"  # "mundo:seed"
  worldNameOfNether: "material_nether"
  worldNameOfEnd: "material_the_end"

border:
  worldOfNormalSize: 2000     # diâmetro da borda após o reset
  worldOfNetherSize: 2000
  worldOfEndSize: 6000

gateInfo:
  useMultiversePortals: false
  portalNameOfNormal: "material_spawn"
  portalNameOfNether: "materialnether_spawn"
  portalNameOfEnd: "materialend_spawn"
  gateAutoBuildOfNormal: true # gera a estrutura do gate
  gateAutoBuildOfNether: true
  gateAutoBuildOfEnd: true

dynmap:
  useDynmap: false
  mapPurgeAtResetOfNormal: false
  mapPurgeAtResetOfNether: false
  mapPurgeAtResetOfEnd: false
  mapPurgeAtResetWorldName: "world_test"

backupWorldInfo:
  backupWorldName: "world,world_nether"

backupTime:
  backupDayOfTheWeek: "sun"
  backupTime: "00:00"
  backupNotifyTime: "300,60,30,10,5,4,3,2,1"

backupLocation:
  fileLocation: "C:/worldBackup/"

restartTime:
  restartDayOfTheWeek: "sun"
  restartTime: "00:00"
  restartNotifyTime: "300,60,30,10,5,4,3,2,1"
```

> Os dias da semana também aceitam os nomes em japonês (`日, 月, 火, 水, 木, 金, 土`).

---

## Compatibilidade

### Multiverse-Core

**A partir da versão 1.8 deste plugin, o Multiverse-Core deixa de ser necessário.** O reset de mundos passou a ser feito diretamente pela API do Bukkit (`Bukkit.unloadWorld` + `WorldCreator`), sem nenhuma chamada à API do Multiverse-Core.

Consequências:

- Não há dependência de classes do Multiverse-Core em tempo de execução, portanto o plugin funciona com qualquer versão do Multiverse-Core — inclusive a **5.8.1**.
- O Multiverse-Core 5.x continua enxergando os mundos normalmente: ele escuta `WorldUnloadEvent`/`WorldLoadEvent` e sincroniza o próprio gerenciador de mundos quando o mundo é descarregado e recriado pelo AutoWorldTools.
- O `softdepend: ['Multiverse-Core']` no `plugin.yml` permanece apenas para garantir a ordem de carregamento e não tem efeito funcional.

### Multiverse-Portals

A integração com portais usa a API do **Multiverse-Portals 5.x** (`MultiversePortalsApi`, `MVPortal`, `PortalLocation`, `PortalManager`), compatível com as versões 5.x atuais. Se você utiliza portais, instale o Multiverse-Portals 5.x junto com o Multiverse-Core 5.x.

> **Compilado e verificado** contra Multiverse-Core **5.8.1** + Multiverse-Portals **5.3.0** (as versões mais recentes na data desta tradução).
>
> **Aviso de depreciação:** `MultiversePortalsUtil.java:112` usa `PortalLocation#getMVWorld()`, que está marcado como *deprecated for removal* desde o Multiverse-Portals 5.3 e será removido no 6.0. Funciona normalmente em todas as versões 5.x; quando o Multiverse-Portals 6.0 for lançado, o método deverá ser trocado por `getMultiverseWorld()`.

### Observações gerais

- O reset de um mundo **não** apaga o `level.dat` gerenciado por outros plugins, mas todo o conteúdo da pasta do mundo é removido.
- O plugin pressupõe que existe um mundo chamado `world` para onde os jogadores são teleportados antes do reset.
- Faça backup do servidor antes de configurar resets automáticos em produção.

---

## Compilando a partir do código

Requisitos: **JDK 17+** e Maven. O build usa `<release>17</release>` porque o Multiverse 5.x é compilado para Java 17.

Os jars do Multiverse 5.x **não estão publicados em repositórios Maven públicos** (a Multiverse publica apenas no GitHub Packages, que exige autenticação). Por isso o `pom.xml` usa escopo `system` com o caminho dos jars definido por propriedade:

| Propriedade | Valor padrão |
| --- | --- |
| `multiverse.core.jar` | `${project.basedir}/libs/multiverse-core.jar` |
| `multiverse.portals.jar` | `${project.basedir}/libs/multiverse-portals.jar` |

Coloque os jars na pasta `libs/` do projeto (ela é ignorada pelo Git, já que `*.jar` está no `.gitignore`) e rode:

```bash
mvn clean package
```

Ou aponte para outro caminho sem alterar o `pom.xml`:

```bash
mvn clean package \
  -Dmultiverse.core.jar=/caminho/multiverse-core-5.8.1.jar \
  -Dmultiverse.portals.jar=/caminho/multiverse-portals-5.3.0.jar
```

O artefato é gerado em `target/AutoWorldTools-1.8.jar`.

> Os jars podem ser obtidos nas releases oficiais: [Multiverse-Core](https://github.com/Multiverse/Multiverse-Core/releases) e [Multiverse-Portals](https://github.com/Multiverse/Multiverse-Portals/releases). O `multiverse-core.jar` é necessário apenas para compilar (os tipos do Core aparecem nas assinaturas de `PortalLocation`/`MultiverseRegion` do Portals); em runtime ele é opcional.

### Build automatizado (GitHub Actions)

O workflow `.github/workflows/build.yml` faz o build do jar no próprio GitHub:

| Evento | O que acontece |
| --- | --- |
| `push` na branch `master` | Compila e publica o jar como artefato da execução |
| `pull_request` | Compila para validar as alterações |
| `workflow_dispatch` | Compila manualmente pelo botão *Run workflow* |
| `git push v1.8.0` (tag `v*`) | Além do build, cria/atualiza o GitHub Release com o jar e o `sha256` |

Etapas do job:

1. Baixa os jars do Multiverse do [Modrinth](https://modrinth.com/mod/multiverse-core) direto para `./libs` (as versões estão fixadas em `MV_CORE_VERSION` e `MV_PORTALS_VERSION` no topo do workflow).
2. `mvn -B clean package` com Java 17.
3. Valida o jar gerado (existe, tem `plugin.yml` e a classe principal) e grava o `sha256`.
4. Envia o artefato para a aba *Actions* e, em tags, anexa ao release.

Basta ir em **Actions → Build → Run workflow** para baixar o jar sem precisar de Maven local.

---

## Licença

Distributed under the terms of the [GNU General Public License v3.0](LICENSE).