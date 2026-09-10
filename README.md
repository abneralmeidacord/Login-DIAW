# Login-DIAW

Sistema de autenticação e cadastro de usuários desenvolvido em **Spring Boot**, com controle de acesso por perfil (usuário/administrador) usando **Spring Security**. Projeto acadêmico desenvolvido para a disciplina de DIAW.

## 🚀 Tecnologias

- **Java 17**
- **Spring Boot 3.3.5**
  - Spring Web
  - Spring Security
  - Spring Boot Test
- **Thymeleaf** — motor de templates para as páginas HTML
- **Maven** — gerenciamento de dependências e build
- **BCrypt** — codificação de senhas

## 📁 Estrutura do projeto

```
src/main/java/com/example/Login_DIAW/
├── LoginDiawApplication.java       # Classe principal (entry point)
├── config/
│   ├── SecurityConfig.java         # Configuração do Spring Security (rotas, login, roles)
│   └── UserConfig.java             # Carrega credenciais de usuário/admin do application.properties
└── controller/
    └── LoginDIAWController.java    # Rotas de login, registro, recuperação de senha, home e admin

src/main/resources/
├── templates/                      # Páginas HTML (Thymeleaf)
│   ├── login.html
│   ├── register.html
│   └── recoverpassword.html
├── static/css/                     # Estilos das páginas
└── application.properties          # Configurações e credenciais da aplicação
```

## ▶️ Como executar

### Pré-requisitos
- Java 17 ou superior instalado
- Maven (ou use o wrapper `mvnw` incluído no projeto, que não exige instalação)

### Passos

1. Clone o repositório:
   ```bash
   git clone https://github.com/<seu-usuario>/Login-DIAW.git
   cd Login-DIAW
   ```

2. Compile e rode os testes:
   ```bash
   ./mvnw clean install
   ```

3. Inicie a aplicação:
   ```bash
   ./mvnw spring-boot:run
   ```

4. Acesse no navegador:
   ```
   http://localhost:8080/login
   ```
   
## 🧭 Rotas principais

| Método | Rota | Descrição | Acesso |
|--------|------|-----------|--------|
| GET/POST | `/login` | Página e envio do formulário de login | Público |
| GET/POST | `/register` | Página e envio do formulário de cadastro | Público |
| GET/POST | `/recoverpassword` | Página e envio de recuperação de senha | Público |
| GET | `/home` | Página inicial após login | Autenticado |
| GET | `/admin` | Painel administrativo | Apenas `ROLE_ADMIN` |
| GET | `/error` | Página de erro | Público |
