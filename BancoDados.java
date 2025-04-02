import java.sql.*;

public class BancoDados {
    private static final String URL = "jdbc:mysql://maglev.proxy.rlwy.net:12233/railway";
    private static final String USUARIO = "root";
    private static final String SENHA = "EdeIpjCqgYqgJLzKWaoJjxaicgWUgYum";

    // Carrega o driver uma única vez ao iniciar a classe
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.err.println("Driver MySQL não encontrado: " + ex.getMessage());
        }
    }

    // Valida se existe um usuário com o email e senha informados.
    public static boolean validarLogin(String email, String senhaInput) {
        boolean valid = false;
        try (Connection conexao = DriverManager.getConnection(URL, USUARIO, SENHA)) {
            String sql = "SELECT senha FROM MainTableConjurDemo WHERE email = ?";
            PreparedStatement stmt = conexao.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String senhaBD = rs.getString("senha");
                if (senhaBD.equals(senhaInput)) {
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

    // Retorna o saldo do usuário cadastrado com o email informado.
    public static int obterSaldo(String email) {
        int saldo = 0;
        try (Connection conexao = DriverManager.getConnection(URL, USUARIO, SENHA)) {
            String sql = "SELECT saldo FROM MainTableConjurDemo WHERE email = ?";
            PreparedStatement stmt = conexao.prepareStatement(sql);
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
