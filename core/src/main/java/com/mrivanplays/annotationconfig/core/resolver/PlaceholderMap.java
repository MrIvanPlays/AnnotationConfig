package com.mrivanplays.annotationconfig.core.resolver;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

class PlaceholderMap implements Iterable<Entry<String, String>> {

  private List<Entry<String, String>> entries;

  PlaceholderMap() {
    this.entries = new LinkedList<>();
  }

  public int size() {
    return this.entries.size();
  }

  public boolean empty() {
    return this.entries.isEmpty();
  }

  public void put(String key, String value) {
    this.entries.add(new AbstractMap.SimpleEntry<>(key, value));
  }

  public Entry<String, String> first() {
    return this.entries.get(0);
  }

  @Override
  public Iterator<Entry<String, String>> iterator() {
    return this.entries.iterator();
  }
}
