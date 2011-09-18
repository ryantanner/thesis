package edu.stanford.nlp.process;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.*;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.objectbank.TokenizerFactory;
import edu.stanford.nlp.objectbank.XMLBeginEndIterator;
import edu.stanford.nlp.util.Function;

//TODO wsg may 2010: Need to ensure that the tag delimiter is not a regex special character

/**
 * Produces a list of sentences from either a plain text or XML document. 
 * <p>
 * Tokenization: The default tokenizer is {@link PTBTokenizer}. If null is passed to
 * <code>setTokenizerFactory</code>, then whitespace tokenization is assumed.
 * <p>
 * Adding a new document type requires two steps:
 * <ol>
 * <li> Add a new DocType.
 * <li> Create an iterator for the new DocType and modify the iterator() function to return the new iterator.
 * </ol>
 * <p>
 * NOTE: This implementation should <em>not</em> use external libraries since it is used in the parser.
 *
 * @author Spence Green
 */
public class DocumentPreprocessor implements Iterable<List<HasWord>> {

  public static enum DocType {Plain, XML};

  private Reader inputReader = null;
  private String inputPath = null;
  private DocType docType = DocType.Plain;

  //Configurable options
  private TokenizerFactory<? extends HasWord> tokenizerFactory = PTBTokenizer.factory();
  private String encoding = null;
  private String[] sentenceFinalPuncWords = {".", "?", "!"};
  private Function<List<HasWord>,List<HasWord>> escaper = null;
  private String sentenceDelimiter = null;
  /**
   * Example: if the words are already POS tagged and look like
   * foo_VB, you want to set the tagDelimiter to "_"
   */
  private String tagDelimiter = null;
  /**
   * When doing XML parsing, only accept text in between tags that
   * match this regular expression.  Defaults to everything.
   */
  private String elementDelimiter = ".*";

  //From PTB conventions
  private final String[] sentenceFinalFollowers = {")", "]", "\"", "\'", "''", "-RRB-", "-RSB-", "-RCB-"};

  /**
   * Constructs a preprocessor from an existing input stream.
   * 
   * @param input An existing reader
   */
  public DocumentPreprocessor(Reader input) {
    this(input,DocType.Plain);
  }

  public DocumentPreprocessor(Reader input, DocType t) {
    if (input == null)
      throw new RuntimeException("Cannot read from null object!");

    docType = t;
    inputReader = input;
  }

  /**
   * Constructs a preprocessor from a file at a path, which can be either
   * a filesystem location or a URL.
   * 
   * @param docPath
   */
  public DocumentPreprocessor(String docPath) {
    this(docPath,DocType.Plain);
  }

  public DocumentPreprocessor(String docPath, DocType t) {
    if (docPath == null)
      throw new RuntimeException("Cannot open null document path!");

    docType = t;
    inputPath = docPath;
  }

  /**

  /**
   * Set the character encoding.
   *
   * @param encoding The character encoding used by Readers
   * @throws IllegalCharsetNameException If the JVM does not support the named character set.
   */
  public void setEncoding(String encoding) throws IllegalCharsetNameException {
    if (Charset.isSupported(encoding))
      this.encoding = encoding;
  }

  /**
   * Sets the end-of-sentence delimiters.
   * 
   * @param sentenceFinalPuncWords
   */
  public void setSentenceFinalPuncWords(String[] sentenceFinalPuncWords) {
    this.sentenceFinalPuncWords = sentenceFinalPuncWords;
  }

  /**
   * Sets the factory from which to produce a {@link Tokenizer}.  The default is
   * {@link PTBTokenizer}.
   * <p>
   * NOTE: If a null argument is used, then the document is assumed to be tokenized
   * and DocumentPreprocessor performs no tokenization.
   *
   */
  public void setTokenizerFactory(TokenizerFactory<? extends HasWord> newTokenizerFactory) { 
    tokenizerFactory = newTokenizerFactory; 
  }

  /**
   * Set an escaper.
   * 
   * @param e The escaper
   */
  public void setEscaper(Function<List<HasWord>,List<HasWord>> e) { escaper = e; }

  /**
   * Make the processor assume that the document is already delimited
   * by the supplied parameter.
   * 
   * @param s The sentence delimiter
   */
  public void setSentenceDelimiter(String s) { sentenceDelimiter = s; }

  /**
   * Split POS tags from tokens.
   * 
   * @param s POS tag delimiter
   */
  public void setTagDelimiter(String s) { tagDelimiter = s; }

  /**
   * Only read text from between these XML tokens if in XML mode.
   * Otherwise, will read from all tokens.
   */
  public void setElementDelimiter(String s) { elementDelimiter = s; }


  /**
   * Returns sentences until the document is exhausted. Calls close() if the end of the document
   * is reached. Otherwise, the user is required to close the stream.
   */
  public Iterator<List<HasWord>> iterator() {
    try {
      if (inputReader == null) 
        inputReader = getReaderFromPath(inputPath);

      //Add new document types here
      if (docType == DocType.Plain) {
        return new PlainTextIterator();
      } else if (docType == DocType.XML) {
        return new XMLIterator();  
      }

    } catch (IOException e) {
      System.err.printf("%s: Could not open path %s\n", this.getClass().getName(), inputPath);
    }

    return new Iterator<List<HasWord>>() {
      public boolean hasNext() { return false; }
      public List<HasWord> next() { return null; }
      public void remove() {}
    };
  }

  /**
   * Closes the underlying reader.
   */
  public void close() {
    try {
      if (inputReader != null) {
        inputReader.close();
        inputReader = null;
      }
    } catch (IOException e) {
      //Silently ignore
    }
  }

  private Reader getReaderFromPath(String path) throws IOException {
    //Check if it is a URL first, otherwise look for a file
    try {
      URL url = new URL(path);
      URLConnection connection = url.openConnection();
      return new BufferedReader(new InputStreamReader(connection.getInputStream()));

    } catch(MalformedURLException e) {
      //Do nothing: the path may be a file
    }

    File file = new File(path);
    if (file.exists())
      return (encoding == null) ? new FileReader(path) : new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));

    throw new IOException("Unable to open " + path);
  }

  private class PlainTextIterator implements Iterator<List<HasWord>> {

    private Tokenizer<? extends HasWord> tokenizer;
    private Set<String> sentDelims;
    private Set<String> delimFollowers = new HashSet<String>(Arrays.asList(sentenceFinalFollowers));
    private Function<String,String> splitTag;
    private List<HasWord> nextSent = null;
    private List<HasWord> nextSentCarryover = new ArrayList<HasWord>();

    public PlainTextIterator() {
      //Setup the tokenizer
      tokenizer = (tokenizerFactory == null) ? new WhitespaceTokenizer(inputReader, false) : 
        tokenizerFactory.getTokenizer(inputReader);

      //Establish how to find sentence boundaries
      sentDelims = new HashSet<String>();
      if (sentenceDelimiter == null) {
        if (sentenceFinalPuncWords != null)
          sentDelims = new HashSet<String>(Arrays.asList(sentenceFinalPuncWords));

      } else {
        sentDelims.add(sentenceDelimiter);
        delimFollowers = new HashSet<String>();

        if (sentenceDelimiter.matches("\\s+")) //Must use a WhitespaceTokenizer in this case
          tokenizer = new WhitespaceTokenizer(inputReader, true);
      }

      //If tokens are tagged, then we must split them
      if (tagDelimiter != null) {
        splitTag = new Function<String,String>() {
          private final String splitRegex = String.format("%s(?!.*%s)",tagDelimiter,tagDelimiter);
          public String apply(String in) {
            final String[] splits = in.split(splitRegex);
            return (splits.length > 0) ? splits[0] : in;
          }
        };
      }

      primeNext();
    }

    private void primeNext() {
      nextSent = new ArrayList<HasWord>(nextSentCarryover);
      nextSentCarryover = new ArrayList<HasWord>();
      boolean seenBoundary = false;

      while (tokenizer.hasNext()) {

        HasWord token = (HasWord) tokenizer.next();
        if (splitTag != null) {
          token.setWord(splitTag.apply(token.word()));
        }

        if (sentDelims.contains(token.word())) {
          seenBoundary = true;
        } else if (seenBoundary && !delimFollowers.contains(token.word())) {
          nextSentCarryover.add(token);
          break;
        }

        if ( ! token.word().matches("\\s+")) {
          nextSent.add(token);
        }
      }

      if (nextSent.size() == 0) {
        close();
        nextSent = null;
      } else if (escaper != null) {
        nextSent = escaper.apply(nextSent);
      }
    }

    public boolean hasNext() { return nextSent != null; }

    public List<HasWord> next() {
      List<HasWord> thisIteration = nextSent;
      primeNext();
      return thisIteration;
    }

    public void remove() { throw new UnsupportedOperationException(); }
  }

  private class XMLIterator implements Iterator<List<HasWord>> {

    private final XMLBeginEndIterator<String> xmlItr;
    private final Reader originalDocReader;
    private PlainTextIterator plainItr = null;
    private List<HasWord> nextSent = null;

    public XMLIterator() {
      xmlItr = new XMLBeginEndIterator<String>(inputReader, elementDelimiter);
      originalDocReader = inputReader;
      primeNext();
    }

    private void primeNext() {
      // It is necessary to loop because if a document has a pattern
      // that goes: <tag></tag> the xmlItr will return an empty
      // string, which the plainItr will process to null.  If we
      // didn't loop to find the next tag, the iterator would stop.
      do {
        if (plainItr != null && plainItr.hasNext()) {
          nextSent = plainItr.next();
        } else if (xmlItr.hasNext()) {
          String block = xmlItr.next();
          inputReader = new BufferedReader(new StringReader(block));
          plainItr = new PlainTextIterator();
          nextSent = plainItr.next();
        } else {
          try {
            originalDocReader.close();
          } catch (IOException e) {}
          nextSent = null;
          break;
        }
      } while (nextSent == null);
    }

    public boolean hasNext() { return nextSent != null; }

    public List<HasWord> next() {
      List<HasWord> thisSentence = nextSent;
      primeNext();
      return thisSentence;
    }

    public void remove() { throw new UnsupportedOperationException(); }
  }


  /**
   * This provides a simple test method for DocumentPreprocessor2. <br/>
   * Usage:
   * java
   * DocumentPreprocessor2 -file filename [-xml tag] [-noSplitSentence]
   * <p>
   * A filename is required. The code doesn't run as a filter currently.
   * <p>
   * tag is the element name of the XML from which to extract text.  It can
   * be a regular expression which is called on the element with the
   * matches() method, such as 'TITLE|P'.
   *
   * @param args Command-line arguments
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("usage: DocumentPreprocessor2 filename [OPTS]");
      System.exit(-1);
    }

    DocumentPreprocessor docPreprocessor = new DocumentPreprocessor(args[0]);

    for (int i = 1; i < args.length; i++) {
      if (args[i].equals("-xml")) {
        docPreprocessor = new DocumentPreprocessor(args[0], DocType.XML);
        docPreprocessor.setTagDelimiter(args[++i]);

      } else if (args[i].equals("-suppressEscaping")) {
        String options = "ptb3Escaping=false";
        docPreprocessor.setTokenizerFactory(PTBTokenizer.factory(new WordTokenFactory(),options));

      } else if (args[i].equals("-noTokenization")) {
        docPreprocessor.setTokenizerFactory(null);
        docPreprocessor.setSentenceDelimiter(System.getProperty("line.separator"));

      } else if (args[i].equals("-tag")) {
        docPreprocessor.setTagDelimiter(args[++i]);
      }
    }

    docPreprocessor.setEncoding("UTF-8");

    int numSents = 0;
    for (List<HasWord> sentence : docPreprocessor) {
      numSents++;
      System.err.println("Length: " + sentence.size());
      boolean printSpace = false;
      for (HasWord word : sentence) {
        if (printSpace) System.out.print(" ");
        printSpace = true;
        System.out.print(word.word());
      }
      System.out.println();
    }
    System.err.println("Read in " + numSents + " sentences.");
  }

}
