# Instruções do projeto AMCash

Estas regras valem para todo o repositório. Instruções mais específicas em diretórios internos também devem ser seguidas.

## Design System

Antes de criar ou modificar qualquer interface:

1. Consulte `docs/DESIGN_SYSTEM.md`.
2. Identifique os componentes existentes relacionados.
3. Reutilize componentes antes de criar novos.
4. Utilize os tokens definidos.
5. Preserve a identidade visual do AMCash.
6. Não introduza padrões visuais diferentes sem solicitação explícita.
7. Considere Home, Trabalho e Metas como referências visuais prioritárias.

## Arquitetura

- Respeite a arquitetura e as convenções existentes.
- Evite duplicação e abstrações sem benefício real.
- Não adicione dependências sem necessidade.
- Não altere código fora do escopo solicitado.
- Não modifique contratos de APIs ou regras de negócio durante refatorações visuais.
- Priorize soluções simples, compostas e sustentáveis.

## Economia de tokens

1. Seja objetivo nas respostas e não repita o contexto estabelecido.
2. Não apresente grandes blocos de código quando os arquivos já foram alterados.
3. Informe apenas os arquivos modificados, o resumo, os testes e eventuais pendências.
4. Comece por buscas direcionadas e leia apenas os arquivos relevantes.
5. Evite reler arquivos e analisar módulos fora do escopo.
6. Execute somente comandos e testes necessários.
7. Agrupe operações relacionadas quando apropriado.
8. Não gere documentação ou arquivos temporários não solicitados.
9. Não explique conceitos básicos sem solicitação.
10. Reutilize o contexto disponível na sessão.

Economia de tokens nunca deve comprometer correção, segurança, qualidade, testes necessários ou entendimento dos requisitos.

## Validação

Após alterações:

- Execute as verificações relevantes e verifique erros de compilação.
- Verifique possíveis regressões e preserve comportamentos existentes.
- Não declare equivalência visual sem comparar as interfaces.
- Quando não houver infraestrutura de comparação visual, informe a limitação.

## Resposta final

Responda brevemente com:

- o que foi feito;
- arquivos alterados;
- testes executados;
- pendências, se existirem.
