package thesis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 10/4/11
 * Time: 10:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class NLPController {

	public static void main(String[] args) {

		Connection con = null;
		Statement st = null;
		ResultSet rs = null;

		String url = "jdbc:mysql://localhost/testwiki";
		String user = "root";
		String password = "sqlr21X";

		try     {
			con = DriverManager.getConnection(url,user,password);
			st = con.createStatement();
			rs = st.executeQuery("SELECT VERSION()");

			if (rs.next())  {
				System.out.println("!" + rs.getString(1));
			}
		}       catch (SQLException ex) {
			//Logger lgr = Logger.getLogger(Version.class.getName());

		}       finally {
				try     {
					if (rs != null) {
						rs.close();
					}
					if (st != null) {
						st.close();
					}
					if (con != null) {
						con.close();
					}
				}       catch (SQLException ex) {
					//
				}
		}

	}

}
