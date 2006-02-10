/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.crawl;

import java.io.File;

import junit.framework.TestCase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.UTF8;
import org.apache.hadoop.io.Writable;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.MapWritable;
import org.apache.nutch.util.NutchConfiguration;

public class TestMapWritable extends TestCase {

  private Configuration configuration = NutchConfiguration.create();

  public void testMap() throws Exception {
    MapWritable map = new MapWritable();
    assertTrue(map.isEmpty());
    for (int i = 0; i < 100; i++) {
      UTF8 key = new UTF8("" + i);
      IntWritable value = new IntWritable(i);
      map.put(key, value);
      assertEquals(i + 1, map.size());
      assertTrue(map.containsKey(new UTF8("" + i)));
      assertTrue(map.containsValue(new IntWritable(i)));
      map.remove(key);
      assertEquals(i, map.size());
      map.put(key, value);
      assertEquals(value, map.get(key));
      assertFalse(map.isEmpty());
      assertTrue(map.keySet().contains(key));
      assertEquals(i + 1, map.values().size());
      assertTrue(map.values().contains(value));
    }
    testWritable(map);
    MapWritable map2 = new MapWritable();
    testWritable(map2);
    map2.putAll(map);
    assertEquals(100, map2.size());
    testWritable(map2);

    map.clear();
    assertTrue(map.isEmpty());
    assertEquals(0, map.size());
    assertFalse(map.containsKey(new UTF8("" + 1)));

  }

  public void testWritable() throws Exception {
    MapWritable datum1 = new MapWritable();
    for (int i = 0; i < 100; i++) {
      datum1.put(new LongWritable(i), new UTF8("" + 1));
    }
    assertEquals(100, datum1.size());
    testWritable(datum1);

    MapWritable datum2 = new MapWritable();
    for (int i = 0; i < 100; i++) {
      datum2.put(new DummyWritable(i), new DummyWritable(i));
    }
    assertEquals(100, datum2.size());
    testWritable(datum2);

    CrawlDatum c = new CrawlDatum(CrawlDatum.STATUS_DB_FETCHED, 1f);
    c.setMetaData(new MapWritable());
    for (int i = 0; i < 100; i++) {
      c.getMetaData().put(new LongWritable(i), new UTF8("" + 1));
    }
    testWritable(c);
  }

  public void testPerformance() throws Exception {
    File file = new File(System.getProperty("java.io.tmpdir"), "mapTestFile");
    file.delete();
    org.apache.hadoop.io.SequenceFile.Writer writer = new SequenceFile.Writer(
        FileSystem.get(configuration), file.getAbsolutePath(),
        IntWritable.class, MapWritable.class);
    // write map
    System.out.println("start writing map's");
    long start = System.currentTimeMillis();
    IntWritable key = new IntWritable();
    MapWritable map = new MapWritable();
    LongWritable mapValue = new LongWritable();
    for (int i = 0; i < 1000000; i++) {
      key.set(i);
      mapValue.set(i);
      map.put(key, mapValue);
      writer.append(key, map);
    }
    long needed = System.currentTimeMillis() - start;
    writer.close();
    System.out.println("needed time for writing map's: " + needed);

    // read map

    org.apache.hadoop.io.SequenceFile.Reader reader = new SequenceFile.Reader(
        FileSystem.get(configuration), file.getAbsolutePath(), configuration);
    System.out.println("start reading map's");
    start = System.currentTimeMillis();
    while (reader.next(key, map)) {

    }
    reader.close();
    needed = System.currentTimeMillis() - start;
    System.out.println("needed time for reading map's: " + needed);
    file.delete();

    // UTF8
    System.out.println("start writing utf8's");
    writer = new SequenceFile.Writer(FileSystem.get(configuration), file
        .getAbsolutePath(), IntWritable.class, UTF8.class);
    // write map
    start = System.currentTimeMillis();
    key = new IntWritable();
    UTF8 value = new UTF8();
    String s = "15726:15726";
    for (int i = 0; i < 1000000; i++) {
      key.set(i);
      value.set(s);
      writer.append(key, value);
    }
    needed = System.currentTimeMillis() - start;
    writer.close();
    System.out.println("needed time for writing utf8's: " + needed);

    // read map
    System.out.println("start reading utf8's");
    reader = new SequenceFile.Reader(FileSystem.get(configuration), file
        .getAbsolutePath(), configuration);
    start = System.currentTimeMillis();
    while (reader.next(key, value)) {

    }
    needed = System.currentTimeMillis() - start;
    System.out.println("needed time for reading utf8: " + needed);
    file.delete();

  }

  /** Utility method for testing writables, from hadoop code */
  public void testWritable(Writable before) throws Exception {
    DataOutputBuffer dob = new DataOutputBuffer();
    before.write(dob);

    DataInputBuffer dib = new DataInputBuffer();
    dib.reset(dob.getData(), dob.getLength());

    Writable after = (Writable) before.getClass().newInstance();
    after.readFields(dib);

    assertEquals(before, after);
  }

  public static void main(String[] args) throws Exception {
    TestMapWritable writable = new TestMapWritable();
    writable.testPerformance();
  }

}