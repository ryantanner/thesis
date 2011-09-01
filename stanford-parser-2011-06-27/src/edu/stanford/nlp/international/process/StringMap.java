package edu.stanford.nlp.international.process;

import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.Collections;
import java.util.Iterator;

/**
 * A map from strings to strings that provides type-checking and limited access control.
 * 
 * @author Spence Green
 *
 */
public class StringMap implements Iterable<String> {

  private final Map<String,String> paramMap;
  
  public StringMap()  {
    paramMap = new TreeMap<String,String>();
  }
  
  /**
   * Indicates whether the map contains the specified key or not.
   * @param param The query key
   * @return True if the map contains <code>param</code>. False otherwise.
   */
  public boolean contains(String param) {
    return paramMap.containsKey(param);
  }
  
  /**
   * Returns the value associated with the specified key, or null if the key does
   * not exist.
   * @param param The query key
   * @return The value associated with <code>param</code>, if it exists. Returns null otherwise.
   */
  public String get(String param) {
    if (contains(param))
      return paramMap.get(param);
    
    return null;
  }
  
  /**
   * Removes the value associated with the specified key. This method silently ignores keys not present
   * in the map.
   * @param param The query key
   * @return The value removed from the map, or null if the key does not exist.
   */
  public String remove(String param) {
    if(contains(param)) 
      return paramMap.remove(param);
    
    return null;
  }
  
  /**
   * Adds a key to the map with an associated value. If the key already exists in the map, then the value
   * is updated with <code>value</code>.
   * @param param The key to add
   * @param value The value associated with the key
   */
  public void put(String param, String value) {
    paramMap.put(param, value);
  }
  
  /**
   * Returns an unmodifiable set of keys.
   * @return The set of keys
   */
  public Set<String> keySet() {
    return Collections.unmodifiableSet(paramMap.keySet());
  }
  
  /**
   * Returns an iterator over the key set.
   */
  public Iterator<String> iterator() {
    Iterator<String> itr = keySet().iterator();
    return itr;
  }
    
  public static void main(String[] args) {
    StringMap dp = new StringMap();
    
    dp.put("key1", "AAA");
    dp.put("key2", "BBBB");
    dp.put("key3", "CCC");
    
    for(String key : dp)
      System.out.printf("%s: %s\n", key,dp.get(key));
  }

}
