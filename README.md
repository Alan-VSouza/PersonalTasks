# PersonalTasks

Aplicativo Android para gerenciamento de tarefas pessoais.

**Autor:** Alan Souza <br>
**Email:** alansouzeesi@gmail.com

## Descrição

O PersonalTasks permite aos usuários criar, visualizar, editar, ordenar e excluir suas tarefas diárias, com persistência local de dados e uma interface de usuário intuitiva.

## Funcionalidades

*   Adicionar novas tarefas com título, descrição, data limite e nível de importância.
*   Listar todas as tarefas cadastradas, ordenadas por importância e data.
*   Editar tarefas existentes.
*   Excluir tarefas.
*   Visualizar detalhes de uma tarefa.
*   Menu de Opções para fácil acesso à criação de tarefas.
*   Menu de Contexto para ações rápidas em itens específicos da lista.
*   Indicador visual de importância na lista de tarefas.
*   Mensagem de estado vazio quando não há tarefas.

## Video Exemplo
https://github.com/user-attachments/assets/01ee6704-1374-4c88-849e-0f4388e467d3
###### Link alternativo: https://youtu.be/gLL4KoyGocI

## Screenshots

### Tela Principal (Sem tarefas)
![Lista de Tarefas Vazia](./readme_assets/principal_sem_tasks.png)

---

### Tela Principal (Com tarefas)
![Lista de Tarefas](./readme_assets/com_tasks.png)

---

### Adicionar Nova Tarefa
![Adicionar Nova Tarefa](./readme_assets/nova_task.png)

---

### Ordenar Tarefas
###### (Nota: A ordenação é automática por importância)
![Ordenar Tarefas](./readme_assets/ordenar_importancia.png)

---


### Pop-Up para ações
![Seleção de Data](./readme_assets/pop_acoes.png)

---

### Detalhes das Tasks
![Detalhes para tasks](./readme_assets/detalhes_tasks.png)

---

### Edição de Task
![Edicao de Tarefa](./readme_assets/editar_task.png)

---

### Excluir Task
![Excluir Tarefa](./readme_assets/excluir_task.png)

---
## Instruções de Execução

1.  Clone este repositório: `git clone https://github.com/Alan-VSouza/PersonalTasks.git`
2.  Abra o projeto no Android Studio (versão LadyBug ou superior).
3.  Certifique-se de que o `minSdk` está configurado para API 26 (Android 8.0) ou superior.
4.  Sincronize o projeto com os arquivos Gradle.
5.  Execute o aplicativo em um emulador ou dispositivo Android (API 26+).

## Instruções de Uso

O PersonalTasks foi projetado para ser intuitivo e fácil de usar. Aqui está um guia rápido sobre como interagir com o aplicativo:

**1. Tela Principal (Lista de Tarefas):**
*   Ao abrir o aplicativo, você verá a lista de todas as suas tarefas cadastradas.
*   Se não houver tarefas, uma mensagem indicará que a lista está vazia (como visto na screenshot ["Tela Principal (Sem tarefas)"](#tela-principal-sem-tarefas)).
*   As tarefas são ordenadas de acordo com a preferência selecionada no menu de ordenação (por importância, e secundariamente por data limite).
*   Cada item da lista exibe o título, uma prévia da descrição, a data limite e o nível de importância com um indicador visual. ([Veja screenshot da lista com tarefas](#tela-principal-com-tarefas))

**2. Adicionar uma Nova Tarefa:**
*   Na tela principal, toque no ícone de + na barra superior.
*   Isso redirecionará para a tela de cadastro. ([Veja screenshot de adicionar nova tarefa](#adicionar-nova-tarefa))
*   Preencha os campos:
    *   **Título:** Um nome curto para sua tarefa (obrigatório, máximo de 50 caracteres).
    *   **Descrição:** Detalhes adicionais sobre a tarefa (obrigatório, máximo de 250 caracteres).
    *   **Data Limite:** Toque no campo para abrir um calendário (DatePicker) e escolha a data (obrigatório, não permite datas passadas).
    *   **Importância:** Selecione o nível de importância (Alta, Média ou Baixa) usando o seletor.
*   Toque em "Salvar" para adicionar a tarefa à sua lista. A nova tarefa aparecerá na tela principal, e uma mensagem de confirmação será exibida.
*   Toque em "Cancelar" para descartar as informações e voltar à tela principal. Se houver alterações não salvas, um diálogo de confirmação será exibido.

**3. Interagindo com Tarefas Existentes (Menu de Contexto):**
*   Na tela principal, pressione e segure (clique longo) sobre uma tarefa na lista para abrir o Menu de Contexto (como visto na screenshot ["Pop-Up para ações"](#pop-up-para-ações)).
*   Este menu oferece as seguintes opções, que levarão à segunda tela para a ação correspondente:
    *   **Detalhes:** Abre a tela de detalhes da tarefa, onde você pode ver todas as informações da tarefa em modo de visualização (campos não editáveis). ([Veja screenshot de detalhes](#detalhes-das-tasks)).
    *   **Editar tarefa:** Abre a tela de edição da tarefa, com os campos preenchidos com os dados atuais da tarefa, prontos para serem modificados. Faça as alterações desejadas (respeitando os limites de caracteres e a validação de data) e toque em "Salvar". Uma mensagem de confirmação será exibida. Se tentar sair com alterações não salvas, um diálogo de confirmação será exibido. ([Veja screenshot de edição](#edição-de-task)).
    *   **Excluir tarefa:** Abre a tela de confirmação de exclusão da tarefa. Os detalhes da tarefa são exibidos para sua revisão. Toque em "Excluir" para remover permanentemente a tarefa (uma mensagem de confirmação será exibida na tela principal) ou "Cancelar" para voltar. ([Veja screenshot de exclusão](#excluir-task)).

**4. Ordenação das Tarefas:**
*   Na tela principal, toque no ícone de ordenação (representado por linha na horizonatal) no menu de opções na barra superior.
*   Escolha entre **"Mais Importante Primeiro"** ou **"Menos Importante Primeiro"**. A lista será atualizada instantaneamente.
*   A preferência de ordenação selecionada é salva e será aplicada sempre que você abrir o aplicativo. ([Veja screenshot de ordenar tarefas](#ordenar-tarefas)).

**5. Tela de Cadastro/Edição/Detalhes/Exclusão (Segunda Tela):**
*   Esta tela é multifuncional e se adapta dependendo da ação iniciada na tela principal.
*   Para **Nova Tarefa** ou **Editar Tarefa**, os campos de formulário estarão ativos para entrada de dados.
*   Para **Detalhes** ou **Confirmar Exclusão**, os campos são exibidos, mas desabilitados para edição.
*   Use o botão "Salvar" (ou "Excluir") para aplicar as mudanças e retornar à tela principal. O botão "Cancelar" (ou o botão "Voltar" do dispositivo/toolbar) permitirá retornar, se houver alterações não salvas, um diálogo de confirmação será exibido.


## Arquitetura

O aplicativo utiliza a arquitetura **MVP (Model-View-Presenter)** para separação de responsabilidades, com os seguintes componentes principais:
*   **Model:** Entidade `Task` e classes de acesso a dados (Room DAO e Database).
*   **View:** Activities (`MainActivity`, `TaskDetailActivity`) e interfaces de contrato que definem como a UI deve ser atualizada.
*   **Presenter:** Classes (`MainPresenter`, `TaskDetailPresenter`) que contêm a lógica de apresentação, interagem com o Model e atualizam a View.
*   **LiveData** é utilizado para observar mudanças nos dados e atualizar a UI de forma reativa.

---
