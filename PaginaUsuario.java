import java.sql.*;

public class PaginaUsuario {
    public static int obterSaldo(String email) {
        int saldo = 0;
        try (Connection conn = DriverManager.getConnection(LoginCadastro.DB_URL, LoginCadastro.DB_USER, LoginCadastro.DB_PASS)) {
            String sql = "SELECT saldo FROM MainTableConjurDemo WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                saldo = rs.getInt("saldo");
            }
            rs.close();
            stmt.close();
        } catch (SQLException ex) {
            System.err.println("Erro ao obter saldo: " + ex.getMessage());
        }
        return saldo;
    }
}
