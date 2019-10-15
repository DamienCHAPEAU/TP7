package banking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

public class BankingDAO {
	private final DataSource myDataSource;
	
	public BankingDAO(DataSource datasource) {
		myDataSource = datasource;
	}

	/**
	 * Renvoie le nom d'un client à partir de son ID
	 * @param id la clé du client à chercher
	 * @return le solde du compte du client
	 * @throws SQLException 
	 */
	public float balanceForCustomer(int id) throws SQLException {
		float result = 0.0f;
		String sql = "SELECT Total FROM Account WHERE CustomerID = ?";
		try ( 	Connection myConnection = myDataSource.getConnection(); 
			PreparedStatement statement = myConnection.prepareStatement(sql)) {
			statement.setInt(1, id); // On fixe le 1° paramètre de la requête
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) { // est-ce qu'il y a un résultat ? (pas besoin de "while", il y a au plus un enregistrement)
					// On récupère les champs de l'enregistrement courant
					result = resultSet.getFloat("Total");
				}
			}
		}
		return result;
	}
	
	/**
	 * Transfère amount € du compte du client fromID vers le compte du client toID
	 * @param fromID l'ID du client à débiter
	 * @param toID l'ID du client à créditer
	 * @param amount le montant à trasférer (positif ou nul)
	 * @throws java.lang.Exception si quelque chose ne marche pas
	 */
	public void bankTransferTransaction(int fromID, int toID, float amount) throws Exception {
		if (amount < 0)
			throw new IllegalArgumentException("Le montant ne doit pas être négatif");
	
		String sql = "UPDATE Account SET Total = Total + ? WHERE CustomerID = ?";
		try (	Connection myConnection = myDataSource.getConnection();
			PreparedStatement statement = myConnection.prepareStatement(sql)) {
			
			myConnection.setAutoCommit(false); // On démarre une transaction
			try {
				// On débite le 1° client
				statement.setFloat( 1, amount * -1);
				statement.setInt(2, fromID);
				int numberUpdated = statement.executeUpdate();
                                System.out.println(numberUpdated);
                                if(numberUpdated == 0){
                                    throw new Exception("Le client "+fromID+" n'existe pas."); 
                                }
				// On crédite le 2° client
				statement.clearParameters();
				statement.setFloat( 1, amount);
				statement.setInt(2, toID);
				numberUpdated = statement.executeUpdate();
                                if(numberUpdated == 0){
                                    throw new Exception("Le client "+toID+" n'existe pas."); 
                                }
				// Tout s'est bien passé, on peut valider la transaction
				myConnection.commit();
			} catch (Exception ex) {
				myConnection.rollback(); // On annule la transaction
				throw ex;       
			} finally {
				 // On revient au mode de fonctionnement sans transaction
				myConnection.setAutoCommit(true);				
			}
		}
	}
}
