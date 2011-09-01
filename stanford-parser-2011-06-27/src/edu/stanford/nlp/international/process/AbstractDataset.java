package edu.stanford.nlp.international.process;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import edu.stanford.nlp.trees.TreeVisitor;
import edu.stanford.nlp.trees.Treebank;

/**
 * 
 * @author Spence Green
 *
 */
public abstract class AbstractDataset implements Dataset {

  protected final List<String> outputFileList;
  protected Mapper posMapper = null;
  protected Mapper lexMapper = null;
  protected Encoding encoding = Encoding.UTF8;
  protected final List<File> pathsToData;
  protected final List<File> pathsToMappings;
  protected FileFilter splitFilter = null;
  protected boolean addDeterminer = false;
  protected boolean removeDashTags = false;
  protected boolean addRoot = false;
  protected boolean removeEscapeTokens = false;
  protected String lexMapOptions = "";
  protected int maxLen = Integer.MAX_VALUE;
  protected String morphDelim = null;
  protected TreeVisitor customTreeVisitor = null;

  protected String outFileName;
  protected String flatFileName;
  protected boolean makeFlatFile = false;
  protected final Pattern fileNameNormalizer = Pattern.compile("\\s+");

  protected Treebank treebank;
  protected final Set<String> configuredOptions;
  protected final Set<String> requiredOptions;
  protected final StringBuilder toStringBuffer;

  protected String treeFileExtension = "tree";    //Current LDC releases use this extension

  /**
   * Provides access for sub-classes to the data set parameters 
   */
  protected StringMap options;

  public AbstractDataset() {
    outputFileList = new ArrayList<String>();
    pathsToData = new ArrayList<File>();
    pathsToMappings = new ArrayList<File>();
    toStringBuffer = new StringBuilder();

    //Read the raw file as UTF-8 irrespective of output encoding
//    treebank = new DiskTreebank(new ArabicTreeReaderFactory.ArabicRawTreeReaderFactory(true), "UTF-8");

    configuredOptions = new HashSet<String>();

    requiredOptions = new HashSet<String>();
    requiredOptions.add(ConfigParser.paramName);
    requiredOptions.add(ConfigParser.paramPath);
    requiredOptions.add(ConfigParser.paramEncode);
  }
  
  public abstract void build();
  
  @SuppressWarnings("unchecked")
  private Mapper loadMapper(String className) {
    Mapper m = null;
    try {
      Class c = ClassLoader.getSystemClassLoader().loadClass(className);  
      m = (Mapper) c.newInstance();
    } catch (ClassNotFoundException e) {
      System.err.printf("%s: Mapper type %s does not exist\n", this.getClass().getName(), className);
    } catch (InstantiationException e) {
      System.err.printf("%s: Unable to instantiate mapper type %s\n", this.getClass().getName(), className);
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      System.err.printf("%s: Unable to access mapper type %s\n", this.getClass().getName(), className);
    }

    return m;
  }

  public boolean setOptions(StringMap opts) {
    options = opts;

    for(String param : opts) {

      String value = opts.get(param);

      if(value == null) {
        System.err.printf("%s: Read parameter with null value (%s)\n", this.getClass().getName(),param);
        continue;
      }

      configuredOptions.add(param);

      //Make matchers for the pre-fix parameters
      Matcher pathMatcher = ConfigParser.matchPath.matcher(param);
      Matcher mapMatcher = ConfigParser.matchMapping.matcher(param);

      if(pathMatcher.lookingAt()) {
        pathsToData.add(new File(value));
        configuredOptions.add(ConfigParser.paramPath);
      }
      else if(mapMatcher.lookingAt()) {
        pathsToMappings.add(new File(value));
        configuredOptions.add(ConfigParser.paramMapping);
      }
      else if(param.equals(ConfigParser.paramEncode))
        encoding = Encoding.valueOf(value);
      else if(param.equals(ConfigParser.paramName)) {
        Matcher inThisFilename = fileNameNormalizer.matcher(value.trim());
        outFileName = inThisFilename.replaceAll("-");
        toStringBuffer.append(String.format("Dataset Name: %s\n",value.trim()));
      }
      else if(param.equals(ConfigParser.paramDT))
        addDeterminer = Boolean.parseBoolean(value);
      else if(param.equals(ConfigParser.paramSplit)) {
        StringMap sm = buildSplitMap(new File(value));
        splitFilter = new SplitFilter(sm);
      }
      else if(param.equals(ConfigParser.paramFlat) && Boolean.parseBoolean(value))
        makeFlatFile = true;
      else if(param.equals(ConfigParser.paramFileExt))
        treeFileExtension = value;
      else if(param.equals(ConfigParser.paramLexMapper))
        lexMapper = loadMapper(value);
      else if(param.equals(ConfigParser.paramNoDashTags))
        removeDashTags = Boolean.parseBoolean(value);
      else if(param.equals(ConfigParser.paramAddRoot))
        addRoot = Boolean.parseBoolean(value);
      else if(param.equals(ConfigParser.paramUnEscape))
        removeEscapeTokens = true;
      else if(param.equals(ConfigParser.paramLexMapOptions))
        lexMapOptions = value;
      else if(param.equals(ConfigParser.paramPosMapper))
        posMapper = loadMapper(value);
      else if(param.equals(ConfigParser.paramMaxLen))
        maxLen = Integer.parseInt(value);
      else if(param.equals(ConfigParser.paramMorph))
        morphDelim = value;
      else if(param.equals(ConfigParser.paramTransform))
        customTreeVisitor = loadTreeVistor(value);
    }

    if(!configuredOptions.containsAll(requiredOptions))
      return false;

    //Finalize the output file names
    if(encoding == Encoding.UTF8)
      outFileName += ".utf8";
    else
      outFileName += ".bw";

    if(makeFlatFile)
      flatFileName = outFileName + ".flat.txt";
    outFileName += ".txt";

    return true;
  }

  private TreeVisitor loadTreeVistor(String value) {
    try {
      Class c = ClassLoader.getSystemClassLoader().loadClass(value);
    
      return (TreeVisitor) c.newInstance();

    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    
    return null;
  }

  protected StringMap buildSplitMap(File path) {
    StringMap sm = new StringMap();
    int lineNum = 0;
    try {
      LineNumberReader reader = new LineNumberReader(new FileReader(path));

      while(reader.ready()) {
        String line = reader.readLine();
        lineNum = reader.getLineNumber();

        sm.put(line.trim(), "t"); //The value does not matter
      }

      reader.close();

    } catch (FileNotFoundException e) {
      System.err.printf("%s: Could not open split file %s\n", this.getClass().getName(),path.getPath());
    } catch (IOException e) {
      System.err.printf("%s: Error reading split file %s (line %d)\n",this.getClass().getName(),path.getPath(),lineNum);
    }

    return sm;
  }

  //Filenames of the stuff that was created
  public List<String> getFilenames() {
    return Collections.unmodifiableList(outputFileList);
  }

  @Override
  public String toString() {
    return toStringBuffer.toString();
  }

  /*
   * Accepts a filename if it is present in <code>filterMap</code>. Rejects the filename otherwise.
   */
  protected class SplitFilter implements FileFilter {
    private final StringMap filterMap;
    public SplitFilter(StringMap sm) {
      filterMap = sm;
    }

    public boolean accept(File f) {
      return filterMap.contains(f.getName());
    }
  }
}
