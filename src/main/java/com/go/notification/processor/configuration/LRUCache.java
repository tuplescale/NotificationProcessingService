package com.go.notification.processor.configuration;
import java.util.LinkedHashMap;
import java.util.Map;


public class LRUCache<K, V> extends LinkedHashMap<K, V>  {
	
	  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int cacheSize;
	
	  public LRUCache(int cacheSize) {
	    super(16, 0.75F);
	    this.cacheSize = cacheSize;
	  }
	
	  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
	    return size() >= cacheSize;
	  }
	  

}

