import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class Adm {
    // Reutiliza as constantes definidas em LoginCadastro
    public static final String DB_URL = LoginCadastro.DB_URL;
    public static final String DB_USER = LoginCadastro.DB_USER;
    public static final String DB_PASS = LoginCadastro.DB_PASS;
    
    // Gera a página administrativa com a lista de usuários e opção para excluir
    public static void sendAdminPage(PrintWriter out, String msg) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'><title>Administração</title>");
        // CSS para a página administrativa
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; background-color: white; }");
        html.append(".navbar { background-color: red; color: white; padding: 10px; text-align: center; font-size: 24px; }");
        html.append(".container { padding: 20px; }");
        html.append("table { width: 100%; border-collapse: collapse; }");
        html.append("table, th, td { border: 1px solid #ccc; }");
        html.append("th, td { padding: 8px; text-align: left; }");
        html.append("input[type='submit'] { padding: 5px 10px; background-color: red; color: white; border: none; cursor: pointer; }");
        html.append("input[type='submit']:hover { background-color: darkred; }");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<div class='navbar'>Banco DM - Administração</div>");
        html.append("<div class='container'>");
        html.append("<h2>Página do Administrador</h2>");
        if (!msg.isEmpty()) {
            html.append("<p style='color:green;'>").append(msg).append("</p>");
        }
        html.append("<table>");
        html.append("<tr><th>ID</th><th>Email</th><th>Senha</th><th>Saldo</th><th>Ação</th></tr>");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "SELECT id, email, senha, saldo FROM MainTableConjurDemo";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                html.append("<tr>");
                html.append("<td>").append(rs.getInt("id")).append("</td>");
                html.append("<td>").append(rs.getString("email")).append("</td>");
                html.append("<td>").append(rs.getString("senha")).append("</td>");
                html.append("<td>").append(rs.getInt("saldo")).append("</td>");
                html.append("<td>");
                html.append("<form action='/delete' method='get'>");
                html.append("<input type='hidden' name='email' value='").append(rs.getString("email")).append("'/>");
                html.append("<input type='submit' value='Excluir'/>");
                html.append("</form>");
                html.append("</td>");
                html.append("</tr>");
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            html.append("<tr><td colspan='5'>Erro ao obter usuários: ").append(ex.getMessage()).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p><a href='/'>Sair</a></p>");
        html.append("</div>");
        html.append("</body></html>");
        sendHttpResponse(out, html.toString());
    }
    
    // Exclui um usuário a partir do email
    public static boolean excluirUsuario(String email) {
        boolean success = false;
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String sql = "DELETE FROM MainTableConjurDemo WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            int rows = stmt.executeUpdate();
            success = rows > 0;
            stmt.close();
        } catch (SQLException ex) {
            System.err.println("Erro ao excluir usuário: " + ex.getMessage());
        }
        return success;
    }
    
    // Envia a resposta HTTP com o conteúdo fornecido
    public static void sendHttpResponse(PrintWriter out, String content) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html; charset=UTF-8");
        out.println("Content-Length: " + content.getBytes(StandardCharsets.UTF_8).length);
        out.println();
        out.println(content);
    }
}
