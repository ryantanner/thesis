package thesis.wiki;

import java.sql.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 10/14/11
 * Time: 2:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParserController {

	/* Get list of articles from database
	 * Take first article ID, get text
	 * Strip text of wikimarkup with bliki
	 * Send output to Stanford
	 * Save output to ???? File for now
	 */

	private Connection con;


	public static void main(String[] args)  {

		ParserController pc = new ParserController();

		System.out.println(pc.getArticleIDList());
	}

	public ParserController()       {
		con = getConnection();
	}

	private Connection getConnection()     {
		String url = "jdbc:mysql://localhost/testwiki";
		String user = "root";
		String password = "sqlr21X";

		Connection con = null;
		try {
			con = DriverManager.getConnection(url,user,password);
		} catch (SQLException ex)       {
			System.out.println(ex.getMessage());
		}
		return con;
	}

	private String getArticleIDList() {
		// Return article ID list from database
		try     {
			Statement st = con.createStatement();

			ResultSet rs = st.executeQuery("SELECT\n" +
				"    p.page_id AS \"Page ID\",\n" +
				"    p.page_title AS \"Page Title\",\n" +
				"    r.rev_text_id AS \"Revision ID\",\n" +
				"    t.old_id AS \"Text ID\"\n" +
				"FROM\n" +
				"    wikitest.page p\n" +
				"        INNER JOIN wikitest.revision r\n" +
				"            ON p.page_latest = r.rev_id\n" +
				"        INNER JOIN wikitest.text t\n" +
				"            ON r.rev_text_id = t.old_id;");

			return rs.getString(1);
		} catch (SQLException ex)       {
			System.out.println(ex.getMessage());
		}
		return null;

	}

	private String getArticle(String id)    {
		// Get article from list

		return "";
	}

	private String strip(String article)    {
		// strip with bliki
		return article;
	}

	private void parse(String article)      {
		// parse with stanford

	}

	private boolean save(String article)    {
		return false;
	}

}
