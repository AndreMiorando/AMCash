# Design System do AMCash

> Estado documentado do frontend em 19 de outubro de 2026. Este documento descreve a implementação existente; não propõe redesign. Home, Trabalho e Metas são as referências visuais prioritárias.

## 1. Princípios visuais

- **Financeiro direto:** valores e estados financeiros ocupam a maior hierarquia visual.
- **Mobile first:** telas usam altura do viewport, navegação inferior fixa no fluxo e conteúdo rolável.
- **Superfícies leves:** fundo quase branco, cards brancos, bordas azul-acinzentadas e sombras de baixa opacidade.
- **Azul como ação e progresso:** ações primárias, seleção, navegação ativa e metas usam azul.
- **Semântica financeira consistente:** verde representa resultado positivo/coberto; rosa-vermelho representa despesas, déficit, remoção e erro.
- **Agrupamento por composição:** KPIs relacionados aparecem em um único card, separados por divisórias internas.
- **Hierarquia compacta:** rótulos pequenos em caixa alta e com espaçamento entre letras precedem títulos e valores.
- **Ícones lineares:** SVGs usam traço arredondado; logos de plataformas e dois indicadores de trabalho usam imagens existentes.

## 2. Fontes da verdade

| Área | Localização |
| --- | --- |
| Tokens e estilos globais | `amcash-web/src/app/globals.css` |
| Fonte e metadados | `amcash-web/src/app/layout.tsx` |
| Home, Metas, navegação e ícones gerais | `amcash-web/src/app/page.tsx` |
| Trabalho e telas de registro/edição | `amcash-web/src/app/components/WorkScreen.tsx` |
| Cabeçalho compartilhado | `amcash-web/src/app/components/AppHeader.tsx` |
| Categorias de custo | `amcash-web/src/app/components/CostCategoryGrid.tsx` |
| Controles compartilhados | `amcash-web/src/app/components/ui/` |
| Logos e imagens de indicadores | `amcash-web/public/` |

Não há biblioteca externa de componentes, Storybook ou suíte de regressão visual configurada. Tailwind está instalado, mas o visual das telas analisadas é implementado principalmente com classes semânticas em `globals.css`.

## 3. Tokens existentes

Tokens globais atualmente declarados em `:root`:

```css
--background: #ffffff;
--foreground: #062762;
--brand-blue: #075cf4;
--brand-navy: #062762;
--app-page-background: #fafbfc;
--app-shell-width: 100%;
--app-content-width: 100%;
--surface-card: #ffffff;
--text-primary: #183253;
--text-muted: #8a9caf;
--border-subtle: #edf1f6;
--border-control: #dce5f0;
--control-height: 2.7rem;
--control-radius: 0.7rem;
--control-border: #dfe6ef;
--control-background: #ffffff;
--focus-border: #78aafa;
--focus-ring: 0 0 0 0.2rem rgb(7 92 244 / 10%);
--button-height: 3rem;
--button-radius: 0.8rem;
--segmented-height: 2.5rem;
--segmented-radius: 0.8rem;
--expense-surface: #fff0f2;
--shadow-card: 0 0.4rem 1.2rem rgb(38 68 104 / 5%);
```

Os aliases semânticos foram introduzidos apenas para valores idênticos já repetidos. Os demais valores abaixo continuam como literais e devem ser tratados como referências antes de qualquer centralização.

### 3.1 Paleta

| Papel observado | Valor atual | Uso atual |
| --- | --- | --- |
| Azul principal | `#075cf4` | marca, navegação ativa, botões e variáveis globais |
| Azul de controles | `#086cf3` | botões, seleções e destaques da Home |
| Azul claro | `#edf4ff`, `#eaf2ff`, `#eef4ff` | fundos de ícones, badges e ações secundárias |
| Azul de seleção | `#e9f1ff`, `#f0f6ff`, `#f1f6ff` | calendários, categoria selecionada e resultados |
| Azul escuro da marca | `#062762` | texto da marca e foreground global |
| Texto principal | `#183253`, `#173557`, `#183555` | títulos, valores e conteúdo de cards |
| Texto secundário | `#71859b`, `#7489a1`, `#7f92a8`, `#8295aa` | labels, descrições e datas |
| Texto terciário | `#8a9caf`, `#8da0b5`, `#91a2b5` | eyebrows, metadados e navegação inativa |
| Fundo da aplicação | `#fafbfc` | Home, Trabalho, Metas e telas internas |
| Superfície | `#fff`, `#ffffff` | cards, cabeçalhos, campos e modais |
| Divisória principal | `#edf1f6` | separação interna de KPIs, listas e menus |
| Borda de campo | `#dce5f0` | inputs e seletores |
| Borda de card | `#dfe7f2`, `#e2e9f2`, `#e3eaf4` | cards e agrupamentos |
| Positivo | `#13ae69`, `#15a565`, `#18ac6b` | saldo positivo e resultado líquido |
| Positivo forte | gradientes `#12ae77 → #0b9d6b` e `#0d9d61 → #18b874` | meta coberta/concluída |
| Negativo/despesa | `#f24f68`, `#ef5368`, `#ed5267`, `#fa5367` | custos, déficit e valores negativos |
| Fundo negativo | `#fff0f2`, `#fff5f6`, `#fff7f8` | ícones de despesa, erro e remoção |
| Branco translúcido | `rgb(255 255 255 / 72%–96%)` | texto sobre gradiente e navegação inferior |

Gradientes principais encontrados:

- Home/meta diária: `linear-gradient(145deg, #0875fa, #0753e6)`.
- Trabalho/líquido: `linear-gradient(135deg, #075cf4, #1677f8)`.
- Metas: `linear-gradient(145deg, #0d61ee, #1d72f6)`.
- Estados cobertos: `linear-gradient(145deg, #12ae77, #0b9d6b)` e `linear-gradient(145deg, #0d9d61, #18b874)`.
- Dia de folga: `linear-gradient(145deg, #3478d8, #245ba9)`.

### 3.2 Tipografia

- Fonte carregada: **Geist**, via `next/font/google` e `--font-geist-sans`.
- Fallback das telas principais: `Arial, sans-serif`.
- `body` ainda declara `Arial, Helvetica, sans-serif`; Home, Trabalho, Metas e demais shells internos sobrescrevem com Geist.
- Fonte monoespaçada carregada: Geist Mono; não há uso visual relevante nas telas de referência.

Hierarquia observada:

| Nível | Valores atuais | Exemplos |
| --- | --- | --- |
| Valor hero | `1.65rem–2rem`, peso padrão/herdado, tracking `-0.04em` a `-0.055em` | meta diária, líquido do período |
| Título de modal | `1.45rem`, peso `780`, tracking `-0.035em` | Editar trabalho |
| Título de tela/card | `1rem–1.55rem`, pesos `620–780` | Home, Trabalho, estados vazios |
| Valor de KPI | `0.88rem–1.25rem` | faturamento, custos, meta semanal |
| Texto de campo | `0.82rem–1rem`, pesos `520–700` | inputs monetários e quilômetros |
| Corpo/descrição | `0.62rem–0.78rem`, line-height `1.35–1.55` | ajuda, legendas e mensagens |
| Eyebrow/label | `0.52rem–0.62rem`, pesos `750–800`, tracking `0.08em–0.17em` | META DIÁRIA, HISTÓRICO |
| Navegação | `0.65rem`, peso `700` | barra inferior |

O projeto usa pesos intermediários de fonte variável (`620`, `650`, `680`, `720`, `750`, `760`, `780`) além de `500`, `600`, `700` e `800`.

### 3.3 Espaçamento

Não existe uma escala nomeada. Os valores dominantes encontrados são:

- Microespaços: `0.15rem`, `0.22rem`, `0.25rem`, `0.28rem`, `0.3rem`, `0.35rem`.
- Espaços de controle: `0.4rem`, `0.45rem`, `0.5rem`, `0.55rem`, `0.6rem`, `0.65rem`, `0.7rem`, `0.75rem`.
- Espaços de seção/card: `0.8rem`, `0.85rem`, `0.9rem`, `1rem`, `1.05rem`, `1.15rem`, `1.2rem`, `1.25rem`, `1.5rem`.
- Padding horizontal de conteúdo: Home `1rem`; Trabalho e Metas `1.05rem`; conteúdo ampliado em desktop `1.25rem`.
- Gap vertical principal: Home `0.9rem`; Metas `0.85rem`; Trabalho usa margens locais de `0.7rem–1rem`.

### 3.4 Bordas e raios

- Divisórias: normalmente `1px solid #edf1f6`.
- Campos: normalmente `1px solid #dce5f0`.
- Cards: `1px solid #dfe7f2`, `#e2e9f2` ou `#e3eaf4`.
- Raios mais frequentes: `0.65rem`, `0.75rem`, `0.8rem`, `0.85rem`, `0.9rem`, `1rem`.
- Cards de destaque: `1.15rem–1.25rem`.
- Modais: `1.35rem–1.75rem`.
- Pílulas e círculos: `99px`, `999px` ou `50%`.

### 3.5 Sombras

| Uso | Valor atual |
| --- | --- |
| Card padrão A | `0 0.4rem 1.2rem rgb(38 68 104 / 5%)` |
| Card padrão B | `0 0.45rem 1.3rem rgb(27 65 112 / 5%)` |
| Ação primária | `0 0.5rem 1rem rgb(8 108 243 / 18%)` |
| Hero de Trabalho | `0 0.9rem 2rem rgb(7 92 244 / 18%)` |
| Modal de meta | `0 1.2rem 3rem rgb(14 31 52 / 24%)` |
| Shell em telas amplas | `0 1.5rem 5rem rgb(24 52 99 / 9%)` |

## 4. Componentes e padrões atuais

### 4.1 Shell e cabeçalho

**Finalidade:** delimitar a aplicação e manter marca/contexto no topo.

**Quando usar:** em todas as telas autenticadas.

**Componente:** `AppHeader`, com `children` opcional para o conteúdo à direita e `className` opcional para a variação da tela. O DOM preserva `dashboard-header` e `header-brand`.

**Variações:** data + avatar (Home), label textual (Trabalho/Metas/Projeção), ação primária (Finanças).

```tsx
<AppHeader className="work-header">
  <span className="work-header-label">TRABALHO</span>
</AppHeader>
```

**Localização:** `components/AppHeader.tsx`; estilos em `globals.css` (`dashboard-header`, `header-brand`).

### 4.2 Navegação inferior (`BottomNavigation`)

**Finalidade:** navegação principal mobile-first.

**Quando usar:** nas telas autenticadas de primeiro nível.

**Props:** `active`, `onHome`, `onFinance`, `onRadar`, `onGoals`, `onWork`, `showWorkFeatures`.

**Variações:** item ativo; ocultação de Trabalho/Metas no perfil Pessoal.

```tsx
<BottomNavigation active="home" {...handlers} showWorkFeatures />
```

**Localização:** `page.tsx` (`BottomNavigation`); `globals.css` (`bottom-nav`, `nav-item`).

### 4.3 Card hero financeiro

**Finalidade:** destacar o principal resultado/meta da tela.

**Quando usar:** uma única vez, próximo ao topo.

**Variações atuais:** azul padrão, verde coberto/concluído, azul dessaturado para folga, valor negativo em rosa claro.

**Implementações:** `daily-goal-card`, `work-net-card`, `goal-main-card`, `custom-goal-main`.

```tsx
<section className="work-net-card">...</section>
```

**Localização:** Home/Metas em `page.tsx`; Trabalho em `WorkScreen.tsx`; estilos em `globals.css`.

### 4.4 Grupo de KPIs

**Finalidade:** comparar de dois a quatro indicadores relacionados.

**Quando usar:** métricas curtas que pertencem ao mesmo contexto.

**Variações:** duas colunas com divisória (`home-today-summary`, `goal-targets`); grade 2×2 (`work-metrics`); positivo, negativo e azul.

```tsx
<section className="home-today-summary">
  <article>...</article>
  <article>...</article>
</section>
```

**Localização:** `page.tsx`, `WorkScreen.tsx`; `globals.css`.

### 4.5 Card de navegação financeira

**Finalidade:** resumir uma informação e navegar para o detalhe.

**Quando usar:** resumo de despesas e próxima despesa na Home.

**Estrutura:** ícone tonal, label, conteúdo principal, valor à direita e chevron circular.

**Variações:** despesa mensal em vermelho; próxima despesa em azul neutro.

**Implementações:** `home-month-expenses`, `home-next-expense`.

### 4.6 Card/lista de registro de trabalho

**Finalidade:** abrir um dia para consulta e edição.

**Dados exibidos:** data, dia da semana, quilômetros, bruto, custos e líquido.

**Variações:** líquido positivo ou negativo; layout compacto abaixo de `30rem`.

**Localização:** `WorkScreen.tsx` (`DateBadge` e linha `work-day-row`); `globals.css`.

### 4.7 Botões

**Primário:** componente `Button`, altura `3rem`, raio `0.8rem`, gradiente azul, texto branco e sombra azul suave.

**Secundário:** superfície branca/azul muito clara, borda azul-acinzentada e texto azul.

**Texto/destrutivo:** fundo transparente ou rosa muito claro, texto `#e9546b`, `#ef4f69` ou `#ff3654`.

**Ícone circular:** `2rem–2.4rem`, fundo `#f1f5fa`/`#f3f6fa`.

**Estados:** `disabled`, `loading` e foco visível são centralizados; ícones e largura total são opcionais.

**Variações compartilhadas:** `primary`, `secondary` e `danger`. Ações especiais mantêm classes próprias quando sua função visual é diferente.

### 4.8 Campos monetários e de formulário

**Finalidade:** entrada direta de valores, distâncias e texto.

**Padrão comum:** componentes `Input` e `InputField`, altura `2.7rem`, borda `#dfe6ef`, raio `0.7rem`, fundo branco `#ffffff` e foco `#78aafa` com anel externo de `0.2rem`. Campos digitáveis compostos da Home e Trabalho reutilizam o mesmo token de fundo; seletores e áreas informativas mantêm superfícies próprias.

**Variações:** prefixos `R$` e `km`, logo de fonte, descrição opcional. A Home e a edição de Trabalho permitem fontes de faturamento adicionais.

**Componentes existentes:**

- `Input`: input base com estados nativo, inválido e desabilitado.
- `InputField`: label, prefixo/sufixo e mensagem de erro opcionais.
- `MoneyField`: composição de Trabalho sobre `InputField`.
- `BrandedMoneyField`: `label`, `logo`, `value`, `onChange`.

Campos de quilometragem, fontes com logomarca e composições compactas da Home permanecem variantes especiais, mas usam os mesmos tokens de foco.

### 4.9 Seletor segmentado

**Finalidade:** alternar entre poucas opções mutuamente exclusivas.

**Componente:** `SegmentedControl`, com opções dinâmicas, valor controlado, estado desabilitado e navegação por setas, Home/End.

**Usos:** Todos/Pendentes/Pagas, Dia/Semana/Mês, Despesas/Personalizada, periodicidade de meta e frequências de repetição.

**Padrão:** contêiner azul-cinza claro, padding interno pequeno, opção ativa branca com sombra e texto azul.

### 4.10 Grade de categorias de custo

**Finalidade:** selecionar visualmente a categoria do custo.

**Categorias atuais:** Combustível, Manutenção, Pedágio, Lavagem, Alimentação e Outro.

**Variações:** estado padrão e `selected`; grade de três colunas.

**Props:** `value`, `onChange` e `ariaLabel` no componente `CostCategoryGrid`. `CostCategoryIcon` e `workCostCategoryLabels` são exportados para os resumos compactos.

**Localização:** `components/CostCategoryGrid.tsx`; estilos compartilhados em `cost-category-grid`.

### 4.11 Telas de fluxo, modais e sheets

**Finalidade:** edição/criação em fluxos dedicados ou sobre o contexto atual.

**Padrão:** fluxos principais usam tela branca interna com cabeçalho de retorno, conteúdo rolável e ações inferiores; operações secundárias podem usar backdrop e sheet.

**Variações:**

- Trabalho: `work-record-screen`; tela interna de registro/edição com formulário compartilhado, largura útil máxima de `32rem` e retorno pelo cabeçalho.
- Meta personalizada: `custom-goal-backdrop` + `custom-goal-dialog`; largura máxima `27rem`.
- Finanças: família `sheet-backdrop`, pickers e diálogos próprios.

No fluxo de Finanças, despesa e receita abrem telas próprias em vez de modais, preservando a mesma estrutura visual e retornando pelo cabeçalho. Os atalhos usam somente os ícones tonalizados de despesa e receita. A seleção de categoria abre uma etapa própria, com busca de borda única, lista completa, ícones e retorno imediato após a escolha. A forma de pagamento/recebimento aparece logo abaixo da categoria e usa o seletor próprio do AMCash, com a mesma superfície dos campos e lista de opções estilizada. O detalhamento também aparece antes de valor e data e funciona como botão seletor centralizado, sem checkbox; os campos editáveis usam superfície branca para se diferenciarem das áreas informativas. O tipo de lançamento é apresentado como escolha entre Única e Recorrente; frequência e quantidade aparecem progressivamente, com as três frequências na mesma linha. A forma de pagamento/recebimento e as observações são opcionais e servem apenas para consulta e auditoria; não alteram as regras de detalhamento da despesa.

**Acessibilidade atual:** as telas de Trabalho usam `aria-labelledby` e botão de retorno com `aria-label`; diálogos preservam `role="dialog"` e `aria-modal`.

### 4.12 Ícones

**Padrão:** SVG inline, `fill="none"`, `stroke="currentColor"`, `strokeWidth` próximo de `1.8–2`, caps/joins arredondados.

**Componente existente:** `Icon` em `page.tsx`, com nomes de navegação, transação e ações.

**Exceções de imagem:** `amcash-logo.png`, `uber-logo.jpeg`, `99-logo.png`, `road-icon.png`, `earnings-per-km-icon.png`.

### 4.13 Estados

**Loading:** spinner circular `.loader`; containers centralizados como `work-loading`, `goals-loading`, `radar-loading` e `dashboard-loading`.

**Erro:** caixa rosa `.auth-error`, erro central da Home com botão de tentar novamente e alerta compacto `.work-error`.

**Vazio:** ícone tonal circular, título, descrição curta e ação opcional (`home-empty-state`, `goal-setup-empty`, `custom-goal-empty`, `work-empty`).

**Sucesso/coberto:** gradiente verde e ícone/check; não existe toast global.

## 5. Padrões de interação

- Cards navegáveis são implementados como `button`, preservando toda a área clicável.
- Chevron indica navegação ou expansão; chevron rotacionado indica conteúdo aberto.
- Seletores usam `aria-pressed`, `aria-selected` ou `aria-expanded` conforme o contexto.
- Valores monetários são formatados em `pt-BR` e os campos aplicam máscara enquanto o usuário digita.
- Campos textuais respeitam os limites do domínio também no frontend: lançamentos e itens `40`, observações `120`, metas `40`, fontes adicionais `30` e descrições de custo `60`. Observações exibem contador discreto abaixo do campo.
- A edição de lançamentos e subdespesas reutiliza os seletores `Única`/`Recorrente` e a frequência da criação, sem oferecer conversão posterior para despesa detalhada.
- O detalhamento de despesas é uma escolha independente da categoria e da forma de pagamento.
- A Home edita dados do dia diretamente; Trabalho consulta períodos e edita registros em modal.
- Ações destrutivas usam confirmação nativa antes da exclusão no fluxo de Trabalho.
- Overlays fecham ao clicar fora; o conteúdo interrompe a propagação do clique.
- Navegação inferior usa `aria-current="page"` no item ativo.
- As barras visuais dos contêineres de tela e modal ficam ocultas; rolagem por toque, roda do mouse e teclado permanece habilitada.

## 6. Responsividade

- Base mobile com `100dvh`, `min-height: 36rem` e conteúdo interno rolável.
- Inputs passam a `16px` abaixo de `47.999rem` para evitar zoom automático em dispositivos móveis.
- Abaixo de `25rem`, formulários e ações da Home passam de duas colunas para uma; grades compactam gaps.
- Trabalho possui ajustes adicionais abaixo de `40rem`, `30rem` e `22rem`.
- Entre `48rem` e `63.999rem`, e em tablets retrato até `79.999rem`, a faixa de tablet usa shell fluido com respiro externo de `1rem` por lado e limite de `56rem`; o conteúdo mantém `2rem` de respiro total e limite de `52rem`.
- Cabeçalhos e conteúdos principais usam `1.5rem` de padding horizontal em tablets.
- A partir de `64rem` em landscape, o shell passa a `58rem` e Home, Trabalho, Metas, Finanças e Projeção compartilham o limite interno de `48rem`, sempre centralizado.
- Modais e backdrops acompanham `--app-shell-width` e não ocupam toda a janela desktop.
- `env(safe-area-inset-top/bottom)` é usado em cabeçalhos e navegação inferior.
- Há tratamento de `prefers-reduced-motion`, reduzindo/removendo animações em pontos específicos.

## 7. Acessibilidade observada

Padrões presentes:

- Documento em `pt-BR`.
- `aria-label` em ícones, navegação e controles sem texto.
- `role="dialog"`, `aria-modal` e associação de título em modais principais.
- `aria-current`, `aria-selected`, `aria-pressed` e `aria-expanded` em controles de estado.
- Classe `.sr-only` para conteúdo exclusivo de leitores de tela.
- Elementos interativos são majoritariamente `button`, não `div` clicável.
- Estados de erro usam `role="alert"` em fluxos relevantes.

Limitações atuais a preservar/avaliar antes de refatorar:

- Estilos de foco visível não são uniformes em todos os botões e cards.
- Alguns textos e alvos do calendário usam dimensões muito compactas.
- Fechamento e retenção de foco não estão centralizados em um único componente de modal.
- Contraste não possui validação automatizada no projeto.

## 8. Boas práticas de reutilização

1. Consultar este documento antes de alterar interfaces.
2. Usar Home, Trabalho e Metas como referências prioritárias.
3. Reutilizar classes e componentes atuais antes de criar variantes.
4. Não escolher um tom “parecido”; usar o valor exato já presente no padrão correspondente.
5. Manter ícones em `currentColor` para que o contexto controle a cor.
6. Preferir composição: shell + header + card + KPI + divisória.
7. Não transformar diferenças semânticas reais em uma única prop genérica.
8. Não mover regras de negócio para componentes exclusivamente visuais.
9. Não alterar textos, máscaras, payloads ou chamadas de API durante extrações visuais.
10. Extrair somente padrões com pelo menos dois usos claros ou forte probabilidade de reutilização.

## 9. Inconsistências identificadas — não corrigidas

- Dois azuis principais muito próximos (`#075cf4` e `#086cf3`) são usados sem regra documentada explícita.
- Há várias variações próximas de navy, cinza de texto, borda e superfície.
- `#fff` e `#ffffff` coexistem.
- `body` usa Arial, enquanto shells internos usam Geist.
- Cards equivalentes usam duas sombras quase idênticas.
- Raios de cards e controles especiais ainda variam entre `0.65rem` e `0.95rem` por contexto.
- Backdrops, estados vazios e campos compostos especiais ainda possuem marcação/CSS contextual.
- Formatação/máscara monetária possui implementações separadas em `page.tsx` e `WorkScreen.tsx`.
- `page.tsx` concentra telas e componentes de domínios diferentes, reduzindo a descoberta e reutilização.

Esses pontos são inventário técnico. Nenhum deve ser normalizado sem comparação visual e aprovação.

## 10. Baseline e validação visual

O repositório não possui infraestrutura detectada para screenshots automatizados, Storybook ou comparação pixel a pixel. Nesta fase, a referência disponível é:

- implementação atual em `page.tsx`, `WorkScreen.tsx` e `globals.css`;
- imagens fornecidas durante a evolução das telas;
- execução manual da aplicação.

Antes da primeira refatoração, registrar capturas dos estados afetados em pelo menos:

- Home: dia com meta, dia de folga, formulário de trabalho aberto e perfil Pessoal;
- Trabalho: Semana, histórico preenchido, vazio e tela de edição com custo aberto;
- Metas: Despesas, Personalizada, mês coberto e calendário;
- larguras abaixo de `25rem`, mobile padrão, `48rem` e landscape a partir de `64rem`, quando aplicável.

Se o ambiente autenticado/dados não estiverem disponíveis, a ausência de comparação deve ser declarada; build e TypeScript não comprovam equivalência visual.

## 11. Plano de refatoração

### Lote 1 — utilitários sem mudança visual

- Centralizar formatação monetária, parsing/máscaras e datas compartilhadas.
- Catálogo, ícones e grade de categorias de custo centralizados em `CostCategoryGrid.tsx`.
- CSS legado dos dropdowns de categoria removido após confirmação de ausência de consumidores.

### Lote 2 — primitives de baixo risco

- `AppHeader` extraído e aplicado às telas autenticadas.
- `Button`, `Input`, `InputField` e `SegmentedControl` extraídos em `components/ui` e aplicados aos usos equivalentes.
- Próximos candidatos: `IconButton`, `StatusMessage` e `ModalShell`, somente quando a marcação e o comportamento forem equivalentes.

### Lote 3 — composições financeiras

- Extrair `KpiGroup`, `KpiItem`, `FinancialNavigationCard` e um shell de card financeiro.
- Migrar primeiro um uso, comparar visualmente e só então migrar os demais.

### Lote 4 — estados e estrutura de telas

- Compartilhar Loading/Error/EmptyState onde a marcação e o visual forem equivalentes.
- Dividir `page.tsx` por domínio sem alterar contratos ou estado de negócio.

### Critérios para cada lote

- Captura antes/depois dos estados afetados.
- Sem alteração de texto, fluxo, API, medidas, cores ou breakpoints.
- ESLint, TypeScript e build do frontend.
- Testes funcionais relevantes do fluxo alterado.
- Interromper o lote se a comparação visual revelar diferença.
