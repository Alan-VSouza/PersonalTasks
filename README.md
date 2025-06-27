# PersonalTasks

Aplicativo Android para gerenciamento de tarefas pessoais com autenticação e persistência remota via Firebase.

**Autor:** Alan Souza  
**Email:** alansouzeesi@gmail.com

## Descrição

PersonalTasks permite criar, visualizar, editar, concluir e excluir tarefas diárias, com sincronização em tempo real via Firebase Realtime Database e suporte offline nativo. Além da lista principal de tarefas, inclui um histórico de tarefas excluídas, permitindo reativá-las ou revisar detalhes em modo somente leitura.

## Funcionalidades

- **Autenticação Firebase**  
  Login e logout de usuários via Firebase Authentication.

- **Lista Principal de Tarefas**
    - Exibe tarefas com status **Ativo** ou **Concluído**.
    - Checkbox para marcar/desmarcar conclusão sem excluir.
    - Indicador de importância (Alta, Média, Baixa).
    - Ordenação por importância e data limite, com preferência salva.

- **CRUD de Tarefas**
    - **Criar** nova tarefa (título, descrição, data, importância).
    - **Editar** tarefa existente.
    - **Visualizar Detalhes** em modo somente leitura.
    - **Excluir** (soft delete, status `DELETED`).

- **Histórico de Tarefas Excluídas**
    - Tela dedicada que lista apenas tarefas com status `DELETED`.
    - **Clique longo** em um item abre menu de contexto:
        - **Reativar tarefa** (status volta a `ACTIVE`).
        - **Detalhes** (abre `TaskDetailActivity` em modo leitura).

- **Sincronização e Suporte Offline**
    - Persistência local e cache automático via `FirebaseDatabase.setPersistenceEnabled(true)`.
    - Operações de leitura/escrita funcionam mesmo sem conexão, sincronizando quando online.

- **Feedback e Validação**
    - Toasts e diálogos de confirmação para salvar, excluir ou descartar alterações.
    - Validação de campos: título (máx. 50 caracteres), descrição (máx. 250), data (não permite passado).

## Video Exemplo

[https://youtu.be/gLL4KoyGocI](https://youtu.be/xFnd6h0Tuuo)

## Screenshots

**Tela Principal (Sem tarefas)**  
![Lista de Tarefas Vazia](./readme_assets/principal_sem_tasks.png)

**Tela Principal (Com tarefas)**  
![Lista de Tarefas](./readme_assets/com_tasks.png)

**Adicionar Nova Tarefa**  
![Adicionar Nova Tarefa](./readme_assets/nova_task.png)

**Ordenar Tarefas**  
![Ordenar Tarefas](./readme_assets/ordenar_importancia.png)

**Menu de Ações (Context Menu)**  
![Menu de Ações](./readme_assets/pop_acoes.png)

**Detalhes de Tarefa**  
![Detalhes de Tarefa](./readme_assets/detalhes_tasks.png)

**Edição de Tarefa**  
![Editar Tarefa](./readme_assets/editar_task.png)

**Excluir Tarefa**  
![Excluir Tarefa](./readme_assets/excluir_task.png)

**Tela de Tarefas Excluídas**  
![Tarefas Excluídas](./readme_assets/deleted_tasks.png)

## Instruções de Execução

1. Clone o repositório  
   git clone https://github.com/Alan-VSouza/PersonalTasks.git

2. Abra o projeto no Android Studio (4.2+ recomendado).
3. Certifique-se de que o `minSdk` está em 26 ou superior.
4. Configure o arquivo `google-services.json` na pasta `app/`.
5. Sincronize o Gradle e execute em emulador ou dispositivo Android (API 26+).

## Instruções de Uso

1. **Login**  
   Cadastre-se ou faça login com e-mail e senha via Firebase.

2. **Tela Principal**
- Tarefas ativas e concluídas aparecem em `RecyclerView`.
- Ícone “+” para adicionar nova tarefa.
- Clique longo em tarefa → menu com “Detalhes”, “Editar” ou “Excluir”.
- Checkbox para marcar/desmarcar conclusão diretamente.

3. **Detalhes/Edição/Exclusão**
- **Detalhes**: modo somente leitura.
- **Editar**: campos habilitados para alteração.
- **Excluir**: confirmação antes de remover (soft delete).

4. **Histórico de Excluídas**
- Acesse pelo menu superior → “Tarefas Excluídas”.
- Clique longo em item → menu com “Reativar” ou “Detalhes”.

5. **Ordenação**
- Ícone de ordenação no menu superior: “Mais importante primeiro” ou “Menos importante primeiro”.
- Preferência salva em `SharedPreferences`.

## Arquitetura

O app adota **MVVM** com separação de camadas:

- **Model**: classe `Task` (com construtor vazio para Firebase).
- **ViewModel**: `TaskViewModel` expõe `LiveData<List<Task>>` para ativos e excluídos.
- **Repository**: `FirebaseTaskRepository` abstrai operações CRUD no Firebase.
- **View**: Activities (`MainActivity`, `TaskDetailActivity`, `DeletedTasksActivity`), usando ViewBinding.

## Regras de Segurança (Realtime Database)

No Console Firebase → Realtime Database → Regras:
```
{
  "rules": {
    "tasks": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid",
        ".indexOn": ["status"]
      }
    }
  }
}
```


## Contato

Dúvidas ou sugestões, envie e-mail para **alansouzeesi@gmail.com**.
