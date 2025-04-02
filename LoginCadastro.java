import java.io.*;
import java.net.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class LoginCadastro {
    public static final String DB_URL = "jdbc:mysql://maglev.proxy.rlwy.net:12233/railway";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "EdeIpjCqgYqgJLzKWaoJjxaicgWUgYum";

    // Carrega o driver MySQL assim que a classe é carregada
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.err.println("Driver MySQL não encontrado: " + ex.getMessage());
        }
    }

    // Método principal do servidor (porta 8089)
    public static void main(String[] args) {
        int port = 8089;
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Servidor LoginCadastro rodando na porta " + port);
            while (true) {
                Socket client = server.accept();
                new Thread(new ClientHandler(client)).start();
            }
        } catch (IOException ex) {
            System.err.println("Erro no servidor LoginCadastro: " + ex.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket client;
        public ClientHandler(Socket client) {
            this.client = client;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true))
            {
                String requestLine = in.readLine();
                if (requestLine == null) return;
                System.out.println("Request: " + requestLine);
                String[] tokens = requestLine.split(" ");
                if (tokens.length < 2) return;
                String pathWithQuery = tokens[1];
                String path = pathWithQuery;
                String query = "";
                if (pathWithQuery.contains("?")) {
                    String[] parts = pathWithQuery.split("\\?", 2);
                    path = parts[0];
                    query = parts[1];
                }

                // Rotas
                if (path.equals("/") || path.equals("/index")) {
                    sendMainPage(out, "");
                } else if (path.equals("/login")) {
                    // Processa o login de usuário
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    String senha = params.get("senha");
                    if (email != null && senha != null) {
                        // Se as credenciais forem de admin, chama o fluxo admin
                        if (isValidAdmin(email, senha)) {
                            // Fluxo de administrador: chama a página administrativa
                            Adm.sendAdminPage(out, "");
                        } else if (validarLogin(email, senha)) {
                            // Fluxo do usuário comum
                            sendPaginaUsuario(out, email);
                        } else {
                            sendMainPage(out, "Email ou senha incorretos para usuário!");
                        }
                    } else {
                        sendMainPage(out, "Dados inválidos para login.");
                    }
                } else if (path.equals("/cadastro")) {
                    // Processa o cadastro de novo usuário
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    String senha = params.get("senha");
                    if (email != null && senha != null) {
                        if (cadastrarUsuario(email, senha)) {
                            sendMainPage(out, "Cadastro realizado com sucesso! Faça login.");
                        } else {
                            sendMainPage(out, "Erro no cadastro (usuário já existe?)");
                        }
                    } else {
                        sendMainPage(out, "Dados inválidos para cadastro.");
                    }
                } else if (path.equals("/saldo")) {
                    // Exibe a página de saldo do usuário
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    int saldo = PaginaUsuario.obterSaldo(email);
                    sendSaldoPage(out, email, saldo);
                } else if (path.equals("/adminForm")) {
                    // Exibe o formulário de login para administrador (tela separada na mesma página)
                    sendAdminLoginForm(out, "");
                } else if (path.equals("/adminLogin")) {
                    // Processa o login de administrador
                    Map<String, String> params = parseQuery(query);
                    String adminUser = params.get("adminUser");
                    String adminPass = params.get("adminPass");
                    if (isValidAdmin(adminUser, adminPass)) {
                        Adm.sendAdminPage(out, "");
                    } else {
                        sendAdminLoginForm(out, "Credenciais de administrador inválidas!");
                    }
                } else if (path.equals("/delete")) {
                    // Exclusão de usuário (ação do administrador)
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    boolean success = Adm.excluirUsuario(email);
                    Adm.sendAdminPage(out, success ? "Usuário excluído com sucesso." : "Erro ao excluir usuário.");
                } else {
                    sendNotFoundPage(out);
                }

            } catch (IOException ex) {
                System.err.println("Erro no ClientHandler: " + ex.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException ex) { /* Ignorar */ }
            }
        }

        // Converte a query string para um mapa de parâmetros
        private Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
            Map<String, String> params = new HashMap<>();
            if (query == null || query.isEmpty()) return params;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                }
            }
            return params;
        }

        // Exibe a página principal (login e cadastro para usuários e botão para admin)
        private void sendMainPage(PrintWriter out, String msg) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Banco DM - Login/Cadastro</title>");
            html.append("<style>");
            html.append("body { font-family: Arial, sans-serif; background-color: white; margin: 0; padding: 0; }");
            html.append(".navbar { background-color: red; color: white; padding: 10px; text-align: center; font-size: 24px; }");
            html.append(".container { padding: 20px; }");
            html.append(".form-box { border: 1px solid #ccc; padding: 15px; margin-bottom: 20px; }");
            html.append("input[type='text'], input[type='password'] { padding: 5px; margin: 5px 0; width: 100%; box-sizing: border-box; }");
            html.append("input[type='submit'], button { padding: 10px 15px; background-color: red; color: white; border: none; cursor: pointer; }");
            html.append("input[type='submit']:hover, button:hover { background-color: darkred; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM</div>");
            html.append("<div class='container'>");
            if (!msg.isEmpty()) {
                html.append("<p style='color:red;'>").append(msg).append("</p>");
            }
            // Formulário de login de usuário
            html.append("<div class='form-box'>");
            html.append("<h2>Login de Usuário</h2>");
            html.append("<form action='/login' method='get'>");
            html.append("Email: <input type='text' name='email'/><br/>");
            html.append("Senha: <input type='password' name='senha'/><br/>");
            html.append("<input type='submit' value='Entrar'/>");
            html.append("</form>");
            html.append("</div>");
            // Formulário de cadastro
            html.append("<div class='form-box'>");
            html.append("<h2>Cadastro</h2>");
            html.append("<form action='/cadastro' method='get'>");
            html.append("Email: <input type='text' name='email'/><br/>");
            html.append("Senha: <input type='password' name='senha'/><br/>");
            html.append("<input type='submit' value='Cadastrar'/>");
            html.append("</form>");
            html.append("</div>");
            // Botão para abrir o formulário de login do administrador
            html.append("<div class='form-box'>");
            html.append("<button onclick='document.getElementById(\"adminForm\").style.display=\"block\";'>Entrar como Administrador</button>");
            html.append("</div>");
            // Formulário de login do administrador (inicialmente oculto)
            html.append("<div id='adminForm' style='display:none;' class='form-box'>");
            html.append("<h2>Login do Administrador</h2>");
            html.append("<form action='/adminLogin' method='get'>");
            html.append("Usuário: <input type='text' name='adminUser'/><br/>");
            html.append("Senha: <input type='password' name='adminPass'/><br/>");
            html.append("<input type='submit' value='Entrar'/>");
            html.append("</form>");
            html.append("<button onclick='document.getElementById(\"adminForm\").style.display=\"none\";'>Fechar</button>");
            html.append("</div>");
            html.append("</div>");
            html.append("</body></html>");
            sendHttpResponse(out, html.toString());
        }

        // Método foi adicionado para exibir o formulário de login para administrador
        private void sendAdminLoginForm(PrintWriter out, String msg) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Login Administrador</title>");
            html.append("<style>");
            html.append("body { font-family: Arial, sans-serif; background-color: white; margin: 0; padding: 0; }");
            html.append(".navbar { background-color: red; color: white; padding: 10px; text-align: center; font-size: 24px; }");
            html.append(".container { padding: 20px; }");
            html.append(".form-box { border: 1px solid #ccc; padding: 15px; margin: 20px; }");
            html.append("input[type='text'], input[type='password'] { padding: 5px; margin: 5px 0; width: 100%; box-sizing: border-box; }");
            html.append("input[type='submit'], button { padding: 10px 15px; background-color: red; color: white; border: none; cursor: pointer; }");
            html.append("input[type='submit']:hover, button:hover { background-color: darkred; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM - Login Administrador</div>");
            html.append("<div class='container'>");
            if (!msg.isEmpty()){
                html.append("<p style='color:red;'>").append(msg).append("</p>");
            }
            html.append("<div class='form-box'>");
            html.append("<h2>Login do Administrador</h2>");
            html.append("<form action='/adminLogin' method='get'>");
            html.append("Usuário: <input type='text' name='adminUser'/><br/>");
            html.append("Senha: <input type='password' name='adminPass'/><br/>");
            html.append("<input type='submit' value='Entrar'/>");
            html.append("</form>");
            html.append("<button onclick='history.back();'>Voltar</button>");
            html.append("</div>");
            html.append("</div>");
            html.append("</body></html>");
            sendHttpResponse(out, html.toString());
        }

        // Exibe a página do usuário (após login bem-sucedido)
        private void sendPaginaUsuario(PrintWriter out, String email) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Página do Usuário</title>");
            html.append("<style>");
            html.append("body { font-family: Arial, sans-serif; background-color: white; }");
            html.append(".navbar { background-color: red; color: white; padding: 10px; text-align: center; font-size: 24px; }");
            html.append(".container { padding: 20px; }");
            html.append("input[type='submit'] { padding: 10px 15px; background-color: red; color: white; border: none; cursor: pointer; }");
            html.append("input[type='submit']:hover { background-color: darkred; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM</div>");
            html.append("<div class='container'>");
            html.append("<h2>Página do Usuário</h2>");
            html.append("<p>Bem-vindo, ").append(email).append("!</p>");
            html.append("<form action='/saldo' method='get'>");
            html.append("<input type='hidden' name='email' value='").append(email).append("'/>");
            html.append("<input type='submit' value='Mostrar Saldo'/>");
            html.append("</form>");
            html.append("</div></body></html>");
            sendHttpResponse(out, html.toString());
        }

        // Exibe a página que mostra o saldo do usuário
        private void sendSaldoPage(PrintWriter out, String email, int saldo) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Saldo</title>");
            html.append("<style>");
            html.append("body { font-family: Arial, sans-serif; background-color: white; }");
            html.append(".navbar { background-color: red; color: white; padding: 10px; text-align: center; font-size: 24px; }");
            html.append(".container { padding: 20px; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM</div>");
            html.append("<div class='container'>");
            html.append("<h2>Saldo do Usuário</h2>");
            html.append("<p>Email: ").append(email).append("</p>");
            html.append("<p>Seu saldo é: ").append(saldo).append("</p>");
            html.append("<p><a href='/'>Sair</a></p>");
            html.append("</div></body></html>");
            sendHttpResponse(out, html.toString());
        }

        // Exibe uma página 404 simples
        private void sendNotFoundPage(PrintWriter out) {
            String html = "<html><head><meta charset='UTF-8'><title>404</title></head><body>" +
                          "<h2>404 - Página Não Encontrada</h2></body></html>";
            sendHttpResponse(out, html);
        }

        // Envia a resposta HTTP com o conteúdo fornecido
        private void sendHttpResponse(PrintWriter out, String content) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Content-Length: " + content.getBytes(StandardCharsets.UTF_8).length);
            out.println();
            out.println(content);
        }

        // Valida o login do usuário consultando o banco de dados
        private boolean validarLogin(String email, String senha) {
            boolean valid = false;
            try (Connection conn = DriverManager.getConnection(LoginCadastro.DB_URL, LoginCadastro.DB_USER, LoginCadastro.DB_PASS)) {
                String sql = "SELECT senha FROM MainTableConjurDemo WHERE email = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, email);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String senhaBD = rs.getString("senha");
                    if (senhaBD.equals(senha)) {
                        valid = true;
                    }
                }
                rs.close();
                stmt.close();
            } catch (SQLException ex) {
                System.err.println("Erro na validação do login: " + ex.getMessage());
            }
            return valid;
        }

        // Realiza o cadastro de um novo usuário com saldo aleatório (0 a 10000)
        private boolean cadastrarUsuario(String email, String senha) {
            boolean cadastrado = false;
            try (Connection conn = DriverManager.getConnection(LoginCadastro.DB_URL, LoginCadastro.DB_USER, LoginCadastro.DB_PASS)) {
                String checkSql = "SELECT id FROM MainTableConjurDemo WHERE email = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    rs.close();
                    checkStmt.close();
                    return false;
                }
                rs.close();
                checkStmt.close();
                int saldoInicial = (int) (Math.random() * 10001);
                String insertSql = "INSERT INTO MainTableConjurDemo (email, senha, saldo) VALUES (?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, email);
                insertStmt.setString(2, senha);
                insertStmt.setInt(3, saldoInicial);
                int rows = insertStmt.executeUpdate();
                cadastrado = rows > 0;
                insertStmt.close();
            } catch (SQLException ex) {
                System.err.println("Erro no cadastro: " + ex.getMessage());
            }
            return cadastrado;
        }
    } // Fim da classe ClientHandler

    // Método estático para validar as credenciais do administrador (hardcoded)
    public static boolean isValidAdmin(String email, String senha) {
        if (email == null || senha == null) return false;
        // Exemplos de credenciais administrativas
        return (email.equals("as") && senha.equals("123")) || (email.equals("df") && senha.equals("456"));
    }
}
