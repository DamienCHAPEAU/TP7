package banking;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;


public class BankingTest {
	private static DataSource myDataSource; // La source de données à utiliser
	private static Connection myConnection ;	
	private BankingDAO myDAO;
	
	@Before
	public void setUp() throws SQLException, IOException, SqlToolError {
		// On crée la connection vers la base de test "in memory"
		myDataSource = getDataSource();
		myConnection = myDataSource.getConnection();
		// On initialise la base avec le contenu d'un fichier de test
		String sqlFilePath = BankingTest.class.getResource("testdata.sql").getFile();
		SqlFile sqlFile = new SqlFile(new File(sqlFilePath));
		sqlFile.setConnection(myConnection);
		sqlFile.execute();
		sqlFile.closeReader();	
		// On crée l'objet à tester
		myDAO = new BankingDAO(myDataSource);
	}
	
	@After
	public void tearDown() throws SQLException {
		myConnection.close();		
		myDAO = null; // Pas vraiment utile
	}

	
	@Test
	public void findExistingCustomer() throws SQLException {
		float balance = myDAO.balanceForCustomer(0);
		// attention à la comparaison des nombres à virgule flottante !
		// Le dernier paramètre correspond à la marge d'erreur tolérable
		assertEquals("Balance incorrecte !", 100.0f, balance, 0.001f);
	}

	@Test
	public void successfulTransfer() throws Exception {
		float amount = 10.0f;
		int fromCustomer = 0; // Le client 0 dispose de 100€ dans le jeu de tests
		int toCustomer = 1;
		// On mémorise les balances dans les deux comptes avant la transaction
		float before0 = myDAO.balanceForCustomer(fromCustomer);
		float before1 = myDAO.balanceForCustomer(toCustomer);
		// On exécute la transaction, qui doit réussir
		myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
		// Les balances doivent avoir été mises à jour dans les 2 comptes
		assertEquals("Balance incorrecte !", before0 - amount, myDAO.balanceForCustomer(fromCustomer), 0.001f);
		assertEquals("Balance incorrecte !", before1 + amount, myDAO.balanceForCustomer(toCustomer), 0.001f);				
	}
        
        @Test
	public void balanceIncorrecte() throws SQLException {
		float solde1 = myDAO.balanceForCustomer(0);
		float solde2 = myDAO.balanceForCustomer(1);
		try {	
			myDAO.bankTransferTransaction(0, 1, solde1 + 50f);
			fail("Exception solde négatif");
		} catch (Exception e) { }		
		assertEquals("balance incorrecte", solde1, myDAO.balanceForCustomer(0), 0.001f);
		assertEquals("balance incorrecte", solde2, myDAO.balanceForCustomer(1), 0.001f);		
	}
	
        
	@Test
	public void fromIDInexistant() throws SQLException {
		float solde = myDAO.balanceForCustomer(1);
		try {   // On essaie débiter un client inconnu
			myDAO.bankTransferTransaction(10000, 1, 10f);
			fail("fromId inexistant");
		} catch (Exception e) {}
		assertEquals("balance incorrecte", solde, myDAO.balanceForCustomer(1), 0.001f);		
	}

	@Test
	public void toIDInexistant() throws SQLException {
		float solde = myDAO.balanceForCustomer(1);
		try {  
			myDAO.bankTransferTransaction(1, 10000, 10f);
			fail("toId inexistant");
		} catch (Exception e) {}
		assertEquals("balance incorrecte", solde, myDAO.balanceForCustomer(1), 0.001f);		
	}
	

	public static DataSource getDataSource() throws SQLException {
		org.hsqldb.jdbc.JDBCDataSource ds = new org.hsqldb.jdbc.JDBCDataSource();
		ds.setDatabase("jdbc:hsqldb:mem:testcase;shutdown=true");
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}	
}