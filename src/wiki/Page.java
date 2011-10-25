package thesis.wiki;

/**
 * Created by IntelliJ IDEA.
 * User: Ryan
 * Date: 10/15/11
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class Page {

	private String pageID;
	private String pageTitle;
	private String revisionID;
	private String textID;

	private String wikitext;


	public Page(String pid, String pt, String rid, String tid)      {
		this(pid,pt,rid,tid,"");
	}

	public Page(String pid, String pt, String rid, String tid, String wt)      {
		this.pageID = pid;
		this.pageTitle = pt;
		this.revisionID = rid;
		this.textID = tid;
		this.wikitext = wt;
	}

	public void setWikitext(String wt)   {
		this.wikitext = wt;
	}

}
