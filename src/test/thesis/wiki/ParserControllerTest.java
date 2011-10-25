package thesis.test.thesis.wiki;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import thesis.wiki.ParserController;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ParserController Tester.
 *
 * @author <Authors name>
 * @since <pre>10/15/2011</pre>
 * @version 1.0
 */
public class ParserControllerTest extends TestCase {

    private ParserController pc;

    private static final Object[] EMPTY = {};


    public ParserControllerTest(String name) {
        super(name);
	pc = new ParserController();
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     *
     * Method: main(String[] args)
     *
     */
    public void testMain() throws Exception {
        //TODO: Test goes here...
    }


    /**
     *
     * Method: getConnection()
     *
     */
    public void testGetConnection() throws Exception {
	try {
           Method method = ParserController.class.getMethod("getConnection");
           method.setAccessible(true);
           Object ret = method.invoke(pc, EMPTY);

	   assertNotNull(ret);
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(InvocationTargetException e) {
        }

     }

    /**
     *
     * Method: getArticleIDList()
     *
     */
    public void testGetArticleIDList() throws Exception {
        //TODO: Test goes here...
        /*
        try {
           Method method = ParserController.class.getMethod("getArticleIDList");
           method.setAccessible(true);
           method.invoke(<Object>, <Parameters>);
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(InvocationTargetException e) {
        }
        */
        }

    /**
     *
     * Method: getArticle(String id)
     *
     */
    public void testGetArticle() throws Exception {
        //TODO: Test goes here...
        /*
        try {
           Method method = ParserController.class.getMethod("getArticle", String.class);
           method.setAccessible(true);
           method.invoke(<Object>, <Parameters>);
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(InvocationTargetException e) {
        }
        */
        }

    /**
     *
     * Method: strip(String article)
     *
     */
    public void testStrip() throws Exception {
        //TODO: Test goes here...
        /*
        try {
           Method method = ParserController.class.getMethod("strip", String.class);
           method.setAccessible(true);
           method.invoke(<Object>, <Parameters>);
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(InvocationTargetException e) {
        }
        */
        }

    /**
     *
     * Method: parse(String article)
     *
     */
    public void testParse() throws Exception {
        //TODO: Test goes here...
        /*
        try {
           Method method = ParserController.class.getMethod("parse", String.class);
           method.setAccessible(true);
           method.invoke(<Object>, <Parameters>);
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(InvocationTargetException e) {
        }
        */
        }

    /**
     *
     * Method: save(String article)
     *
     */
    public void testSave() throws Exception {
        //TODO: Test goes here...
        /*
        try {
           Method method = ParserController.class.getMethod("save", String.class);
           method.setAccessible(true);
           method.invoke(<Object>, <Parameters>);
        } catch(NoSuchMethodException e) {
        } catch(IllegalAccessException e) {
        } catch(InvocationTargetException e) {
        }
        */
        }


    public static Test suite() {
        return new TestSuite(ParserControllerTest.class);
    }
}
