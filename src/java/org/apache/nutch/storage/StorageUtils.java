package org.apache.nutch.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.gora.mapreduce.GoraMapper;
import org.gora.mapreduce.GoraOutputFormat;
import org.gora.mapreduce.GoraReducer;
import org.gora.persistency.Persistent;
import org.gora.query.Query;
import org.gora.store.DataStore;
import org.gora.store.DataStoreFactory;

public class StorageUtils {

  @SuppressWarnings("unchecked")
  public static <K, V extends Persistent> DataStore<K, V> createDataStore(Configuration conf,
      Class<K> keyClass, Class<V> persistentClass) throws ClassNotFoundException {
    Class<? extends DataStore<K, V>> dataStoreClass =
      (Class<? extends DataStore<K, V>>) getDataStoreClass(conf);
    return DataStoreFactory.createDataStore(dataStoreClass,
            keyClass, persistentClass);
  }

  @SuppressWarnings("unchecked")
  public static <K, V extends Persistent> Class<? extends DataStore<K, V>>
  getDataStoreClass(Configuration conf)  throws ClassNotFoundException {
    return (Class<? extends DataStore<K, V>>)
      Class.forName(conf.get("storage.data.store.class",
          "org.gora.hbase.store.HBaseStore"));
  }

  public static <K, V> void initMapperJob(Job job,
      Collection<WebPage.Field> fields,
      Class<K> outKeyClass, Class<V> outValueClass,
      Class<? extends GoraMapper<String, WebPage, K, V>> mapperClass, boolean reuseObjects)
  throws ClassNotFoundException, IOException {
    initMapperJob(job, fields, outKeyClass, outValueClass, mapperClass, null, reuseObjects);
  }

  public static <K, V> void initMapperJob(Job job,
      Collection<WebPage.Field> fields,
      Class<K> outKeyClass, Class<V> outValueClass,
      Class<? extends GoraMapper<String, WebPage, K, V>> mapperClass)
  throws ClassNotFoundException, IOException {
    initMapperJob(job, fields, outKeyClass, outValueClass, mapperClass, null, true);
  }

  public static <K, V> void initMapperJob(Job job,
      Collection<WebPage.Field> fields,
      Class<K> outKeyClass, Class<V> outValueClass,
      Class<? extends GoraMapper<String, WebPage, K, V>> mapperClass,
      Class<? extends Partitioner<K, V>> partitionerClass)
  throws ClassNotFoundException, IOException {
    initMapperJob(job, fields, outKeyClass, outValueClass, mapperClass, partitionerClass, true);
  }

  public static <K, V> void initMapperJob(Job job,
      Collection<WebPage.Field> fields,
      Class<K> outKeyClass, Class<V> outValueClass,
      Class<? extends GoraMapper<String, WebPage, K, V>> mapperClass,
      Class<? extends Partitioner<K, V>> partitionerClass, boolean reuseObjects)
  throws ClassNotFoundException, IOException {
    DataStore<String, WebPage> store =
      createDataStore(job.getConfiguration(), String.class, WebPage.class);
    if (store==null) throw new RuntimeException("Could not create datastore");
    Query<String, WebPage> query = store.newQuery();
    query.setFields(toStringArray(fields));
    GoraMapper.initMapperJob(job, query, store,
        outKeyClass, outValueClass, mapperClass, partitionerClass, reuseObjects);
    GoraOutputFormat.setOutput(job, store, true);
  }

  public static <K, V> void initReducerJob(Job job,
      Class<? extends GoraReducer<K, V, String, WebPage>> reducerClass)
  throws ClassNotFoundException {
    Configuration conf = job.getConfiguration();
    DataStore<String, WebPage> store =
      StorageUtils.createDataStore(conf, String.class, WebPage.class);
    GoraReducer.initReducerJob(job, store, reducerClass);
    GoraOutputFormat.setOutput(job, store, true);
  }

  private static String[] toStringArray(Collection<WebPage.Field> fields) {
    String[] arr = new String[fields.size()];
    Iterator<WebPage.Field> iter = fields.iterator();
    for (int i = 0; i < arr.length; i++) {
      arr[i] = iter.next().getName();
    }
    return arr;
  }
}
