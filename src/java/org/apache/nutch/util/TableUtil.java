package org.apache.nutch.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

import org.apache.avro.util.Utf8;

public class TableUtil {

  public static final ByteBuffer YES_VAL = ByteBuffer.wrap(new byte[] { 'y' });

  /**
   * Reverses a url's domain. This form is better for storing in hbase. Because
   * scans within the same domain are faster.
   * <p>
   * E.g. "http://bar.foo.com:8983/to/index.html?a=b" becomes
   * "com.foo.bar:8983:http/to/index.html?a=b".
   *
   * @param url
   *          url to be reversed
   * @return Reversed url
   * @throws MalformedURLException
   */
  public static String reverseUrl(String urlString)
      throws MalformedURLException {
    return reverseUrl(new URL(urlString));
  }

  /**
   * Reverses a url's domain. This form is better for storing in hbase. Because
   * scans within the same domain are faster.
   * <p>
   * E.g. "http://bar.foo.com:8983/to/index.html?a=b" becomes
   * "com.foo.bar:http:8983/to/index.html?a=b".
   *
   * @param url
   *          url to be reversed
   * @return Reversed url
   */
  public static String reverseUrl(URL url) {
    String host = url.getHost();
    String file = url.getFile();
    String protocol = url.getProtocol();
    int port = url.getPort();

    StringBuilder buf = new StringBuilder();

    /* reverse host */
    reverseAppendSplits(host.split("\\."), buf);

    /* add protocol */
    buf.append(':');
    buf.append(protocol);

    /* add port if necessary */
    if (port != -1) {
      buf.append(':');
      buf.append(port);
    }

    /* add path */
    if (file.length() > 0 && '/' != file.charAt(0)) {
      buf.append('/');
    }
    buf.append(file);

    return buf.toString();
  }

  public static String unreverseUrl(String reversedUrl) {
    StringBuilder buf = new StringBuilder(reversedUrl.length() + 2);

    int pathBegin = reversedUrl.indexOf('/');
    if (pathBegin == -1)
      pathBegin = reversedUrl.length();
    String sub = reversedUrl.substring(0, pathBegin);

    String[] splits = sub.split(":"); // {<reversed host>, <port>, <protocol>}

    buf.append(splits[1]); // add protocol
    buf.append("://");
    reverseAppendSplits(splits[0].split("\\."), buf); // splits[0] is reversed
    // host
    if (splits.length == 3) { // has a port
      buf.append(':');
      buf.append(splits[2]);
    }
    buf.append(reversedUrl.substring(pathBegin));
    return buf.toString();
  }

  /**
   * Given a reversed url, returns the reversed host E.g
   * "com.foo.bar:http:8983/to/index.html?a=b" -> "com.foo.bar"
   *
   * @param reversedUrl
   *          Reversed url
   * @return Reversed host
   */
  public static String getReversedHost(String reversedUrl) {
    return reversedUrl.substring(0, reversedUrl.indexOf(':'));
  }

  private static void reverseAppendSplits(String[] splits, StringBuilder buf) {
    for (int i = splits.length - 1; i > 0; i--) {
      buf.append(splits[i]);
      buf.append('.');
    }
    buf.append(splits[0]);
  }

  /**
   * Convert given Utf8 instance to String
   *
   * @param utf8
   *          Utf8 object
   * @return string-ifed Utf8 object or null if Utf8 instance is null
   */
  public static String toString(Utf8 utf8) {
    return (utf8 == null ? null : utf8.toString());
  }

}
