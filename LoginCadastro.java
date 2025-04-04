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

    // Carrega o driver MySQL assim que a classe for carregada
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.err.println("Driver MySQL nao encontrado: " + ex.getMessage());
        }
    }

    // Inicia o servidor unificado na porta 8089
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
                        new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
                String requestLine = in.readLine();
                if (requestLine == null)
                    return;
                System.out.println("Request: " + requestLine);
                String[] tokens = requestLine.split(" ");
                if (tokens.length < 2)
                    return;
                String pathWithQuery = tokens[1];
                String path = pathWithQuery;
                String query = "";
                if (pathWithQuery.contains("?")) {
                    String[] parts = pathWithQuery.split("\\?", 2);
                    path = parts[0];
                    query = parts[1];
                }
                
                if (path.equals("/") || path.equals("/index")) {
                    sendMainPage(out, "");
                } else if (path.equals("/login")) {
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    String senha = params.get("senha");
                    if (email != null && senha != null) {
                        if (isValidAdmin(email, senha)) {
                            Adm.sendAdminPage(out, "");
                        } else if (validarLogin(email, senha)) {
                            sendPaginaUsuario(out, email);
                        } else {
                            sendMainPage(out, "Email ou senha incorretos para usuario!");
                        }
                    } else {
                        sendMainPage(out, "Dados invalidos para login.");
                    }
                } else if (path.equals("/cadastro")) {
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    String senha = params.get("senha");
                    if (email != null && senha != null) {
                        if (cadastrarUsuario(email, senha)) {
                            sendMainPage(out, "Cadastro realizado com sucesso! Faça login.");
                        } else {
                            sendMainPage(out, "Erro no cadastro (usuario ja existe?)");
                        }
                    } else {
                        sendMainPage(out, "Dados invalidos para cadastro.");
                    }
                } else if (path.equals("/saldo")) {
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    int saldo = PaginaUsuario.obterSaldo(email);
                    sendSaldoPage(out, email, saldo);
                } else if (path.equals("/adminForm")) {
                    sendAdminLoginForm(out, "");
                } else if (path.equals("/adminLogin")) {
                    Map<String, String> params = parseQuery(query);
                    String adminUser = params.get("adminUser");
                    String adminPass = params.get("adminPass");
                    if (isValidAdmin(adminUser, adminPass)) {
                        Adm.sendAdminPage(out, "");
                    } else {
                        sendAdminLoginForm(out, "Credenciais de administrador invalidas!");
                    }
                } else if (path.equals("/delete")) {
                    Map<String, String> params = parseQuery(query);
                    String email = params.get("email");
                    boolean success = Adm.excluirUsuario(email);
                    Adm.sendAdminPage(out, success ? "Usuario excluido com sucesso." : "Erro ao excluir usuario.");
                } else {
                    sendNotFoundPage(out);
                }
            } catch (IOException ex) {
                System.err.println("Erro no ClientHandler: " + ex.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException ex) { }
            }
        }
        
        private Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
            Map<String, String> params = new HashMap<>();
            if (query == null || query.isEmpty())
                return params;
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
        
        // Exibe a página principal com o card centralizado contendo os formulários de Login e Cadastro.
        // Os botões "Criar Cadastro" e "Ir para o Login" utilizarão a classe "tab-switch-btn"
        private void sendMainPage(PrintWriter out, String msg) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Banco DM - Login/Cadastro</title>");
            html.append("<link href='https://fonts.googleapis.com/css?family=Roboto&display=swap' rel='stylesheet'>");
            html.append("<style>");
            html.append("body { font-family: 'Roboto', sans-serif; background: linear-gradient(135deg, #ffffff, #f8f8f8); margin: 0; padding: 0; }");
            html.append(".navbar { background-color: #a3003d; color: #fff; padding: 15px; text-align: center; font-size: 26px; font-weight: bold; }");
            html.append(".container { max-width: 400px; margin: 40px auto; padding: 20px; background: #fff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
            html.append("input[type='radio'] { display: none; }");
            html.append(".tab-content { display: none; }");
            html.append("#tab-login:checked ~ .form-box #login-content { display: block; }");
            html.append("#tab-cadastro:checked ~ .form-box #cadastro-content { display: block; }");
            html.append(".tab-switch { text-align: center; margin-top: 10px; }");
            // Novos estilos para os botões de troca: borda vermelha, fundo branco, texto vermelho
            html.append(".tab-switch-btn { padding: 8px 12px; background-color: #fff; color: #a3003d; border: 1px solid #a3003d; border-radius: 4px; cursor: pointer; }");
            html.append(".tab-switch-btn:hover { background-color: #a3003d; color: #fff; }");
            html.append(".form-box form input[type='text'], .form-box form input[type='password'] { width: 100%; padding: 10px; margin: 6px 0; border: 1px solid #ddd; border-radius: 4px; }");
            html.append(".form-box form input[type='submit'] { width: 100%; padding: 10px; background-color: #a3003d; color: #fff; border: none; border-radius: 4px; cursor: pointer; margin-top: 10px; }");
            html.append(".form-box form input[type='submit']:hover { background-color: #80002a; }");
            html.append(".admin-btn { display: block; width: 200px; margin: 20px auto; padding: 10px; background-color: #a3003d; color: #fff; text-align: center; border: none; border-radius: 4px; text-decoration: none; cursor: pointer; }");
            html.append(".admin-btn:hover { background-color: #80002a; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM</div>");
            html.append("<div class='container'>");
            if (!msg.isEmpty()) {
                html.append("<p style='color:red; text-align:center;'>").append(msg).append("</p>");
            }
            // Botões de radio para alternar os formulários
            html.append("<input type='radio' name='tab' id='tab-login' checked>");
            html.append("<input type='radio' name='tab' id='tab-cadastro'>");
            html.append("<div class='form-box'>");
            // Conteúdo do Login
            html.append("<div id='login-content' class='tab-content'>");
            html.append("<h2>Login</h2>");
            html.append("<form action='/login' method='get'>");
            html.append("<input type='text' name='email' placeholder='Email'>");
            html.append("<input type='password' name='senha' placeholder='Senha'>");
            html.append("<input type='submit' value='Entrar'>");
            html.append("</form>");
            html.append("<div class='tab-switch'>");
            // Usando a nova classe para os botões de troca
            html.append("<label for='tab-cadastro' class='tab-switch-btn'>Criar Cadastro</label>");
            html.append("</div>");
            html.append("</div>");
            // Conteúdo do Cadastro
            html.append("<div id='cadastro-content' class='tab-content'>");
            html.append("<h2>Cadastro</h2>");
            html.append("<form action='/cadastro' method='get'>");
            html.append("<input type='text' name='email' placeholder='Email'>");
            html.append("<input type='password' name='senha' placeholder='Senha'>");
            html.append("<input type='submit' value='Cadastrar'>");
            html.append("</form>");
            html.append("<div class='tab-switch'>");
            html.append("<label for='tab-login' class='tab-switch-btn'>Ir para o Login</label>");
            html.append("</div>");
            html.append("</div>");
            html.append("</div>"); // fim da form-box
            html.append("</div>"); // fim do container
            // Botão centralizado para abrir o formulário de login do administrador
            html.append("<a class='admin-btn' href='#' onclick=\"document.getElementById('adminForm').style.display='block'; return false;\">Entrar como Administrador</a>");
            // Formulário de login do administrador (inicialmente oculto)
            html.append("<div id='adminForm' style='display:none;' class='container'>");
            html.append("<div class='form-box'>");
            html.append("<h2>Login Administrador</h2>");
            html.append("<form action='/adminLogin' method='get'>");
            html.append("<input type='text' name='adminUser' placeholder='Usuario'>");
            html.append("<input type='password' name='adminPass' placeholder='Senha'>");
            html.append("<input type='submit' value='Entrar'>");
            html.append("</form>");
            html.append("<button class='switch-btn' onclick=\"document.getElementById('adminForm').style.display='none';\">Fechar</button>");
            html.append("</div>");
            html.append("</div>");
            html.append("</body></html>");
            sendHttpResponse(out, html.toString());
        }
        
        private void sendAdminLoginForm(PrintWriter out, String msg) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Login Administrador</title>");
            html.append("<link href='https://fonts.googleapis.com/css?family=Roboto&display=swap' rel='stylesheet'>");
            html.append("<style>");
            html.append("body { font-family: 'Roboto', sans-serif; background: #f8f8f8; margin: 0; padding: 0; }");
            html.append(".navbar { background-color: #a3003d; color: #fff; padding: 15px; text-align: center; font-size: 26px; font-weight: bold; }");
            html.append(".container { max-width: 400px; margin: 40px auto; padding: 20px; background: #fff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
            html.append(".form-box { padding: 20px; }");
            html.append("input[type='text'], input[type='password'] { width: 100%; padding: 10px; margin: 6px 0; border: 1px solid #ddd; border-radius: 4px; }");
            html.append("input[type='submit'] { width: 100%; padding: 10px; background-color: #a3003d; color: #fff; border: none; border-radius: 4px; cursor: pointer; margin-top: 10px; }");
            html.append("input[type='submit']:hover { background-color: #80002a; }");
            html.append(".switch-btn { display: block; width: 100%; padding: 8px; background-color: #a3003d; color: #fff; border: none; border-radius: 4px; cursor: pointer; margin-top: 10px; }");
            html.append(".switch-btn:hover { background-color: #80002a; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM - Login Administrador</div>");
            html.append("<div class='container'>");
            if (!msg.isEmpty()){
                html.append("<p style='color:red; text-align:center;'>").append(msg).append("</p>");
            }
            html.append("<div class='form-box'>");
            html.append("<h2>Login Administrador</h2>");
            html.append("<form action='/adminLogin' method='get'>");
            html.append("<input type='text' name='adminUser' placeholder='Usuario'>");
            html.append("<input type='password' name='adminPass' placeholder='Senha'>");
            html.append("<input type='submit' value='Entrar'>");
            html.append("</form>");
            html.append("<button class='switch-btn' onclick=\"history.back();\">Voltar</button>");
            html.append("</div>");
            html.append("</div>");
            html.append("</body></html>");
            sendHttpResponse(out, html.toString());
        }
        
        private void sendPaginaUsuario(PrintWriter out, String email) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Pagina do Usuario</title>");
            html.append("<link href='https://fonts.googleapis.com/css?family=Roboto&display=swap' rel='stylesheet'>");
            html.append("<style>");
            html.append("body { font-family: 'Roboto', sans-serif; background: #f8f8f8; margin: 0; padding: 0; }");
            html.append(".navbar { background-color: #a3003d; color: #fff; padding: 15px; text-align: center; font-size: 26px; font-weight: bold; }");
            html.append(".container { max-width: 400px; margin: 40px auto; padding: 20px; background: #fff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
            html.append("input[type='submit'] { width: 100%; padding: 10px; background-color: #a3003d; color: #fff; border: none; border-radius: 4px; cursor: pointer; margin-top: 10px; }");
            html.append("input[type='submit']:hover { background-color: #80002a; }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM</div>");
            html.append("<div class='container'>");
            html.append("<h2>Pagina do Usuario</h2>");
            html.append("<p>Bem-vindo, ").append(email).append("!</p>");
            html.append("<form action='/saldo' method='get'>");
            html.append("<input type='hidden' name='email' value='").append(email).append("'/>");
            html.append("<input type='submit' value='Mostrar Saldo'/>");
            html.append("</form>");
            html.append("</div></body></html>");
            sendHttpResponse(out, html.toString());
        }
        
        private void sendSaldoPage(PrintWriter out, String email, int saldo) {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><title>Saldo</title>");
            html.append("<link href='https://fonts.googleapis.com/css?family=Roboto&display=swap' rel='stylesheet'>");
            html.append("<style>");
            html.append("body { font-family: 'Roboto', sans-serif; background: #f8f8f8; margin: 0; padding: 0; }");
            html.append(".navbar { background-color: #a3003d; color: #fff; padding: 15px; text-align: center; font-size: 26px; font-weight: bold; }");
            html.append(".container { max-width: 400px; margin: 40px auto; padding: 20px; background: #fff; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
            html.append("</style>");
            html.append("</head><body>");
            html.append("<div class='navbar'>Banco DM</div>");
            html.append("<div class='container'>");
            html.append("<h2>Saldo do Usuario</h2>");
            html.append("<p>Email: ").append(email).append("</p>");
            html.append("<p>Seu saldo: ").append(saldo).append("</p>");
            html.append("<p><a href='/'>Sair</a></p>");
            html.append("</div></body></html>");
            sendHttpResponse(out, html.toString());
        }
        
        private void sendNotFoundPage(PrintWriter out) {
            String html = "<html><head><meta charset='UTF-8'><title>404</title></head><body>" +
                          "<h2>404 - Pagina Não Encontrada</h2></body></html>";
            sendHttpResponse(out, html);
        }
        
        private void sendHttpResponse(PrintWriter out, String content) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Content-Length: " + content.getBytes(StandardCharsets.UTF_8).length);
            out.println();
            out.println(content);
        }
        
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

    public static boolean isValidAdmin(String email, String senha) {
        if (email == null || senha == null)
            return false;
        return (email.equals("as") && senha.equals("123")) || (email.equals("df") && senha.equals("456"));
    }
}
