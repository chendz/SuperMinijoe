package com.google.minijoe.sys;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.NamedRunnable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Fetches HTML from a requested URL, follows the links, and repeats.
 */
public final class Crawler {
  private final OkHttpClient client;
  private final Set<URL> fetchedUrls = Collections.synchronizedSet(new LinkedHashSet<URL>());
  private final LinkedBlockingQueue<URL> queue = new LinkedBlockingQueue<>();
  private final ConcurrentHashMap<String, AtomicInteger> hostnames = new ConcurrentHashMap<>();

  public Crawler(OkHttpClient client) {
    this.client = client;
  }

  private void parallelDrainQueue(int threadCount) {
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    for (int i = 0; i < threadCount; i++) {
      executor.execute(new NamedRunnable("Crawler %s", i) {
        @Override protected void execute() {
          try {
            drainQueue();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
    }
    executor.shutdown();
  }

  private void drainQueue() throws Exception {
    for (URL url; (url = queue.take()) != null; ) {
      if (!fetchedUrls.add(url)) {
        continue;
      }

      try {
        fetch(url);
      } catch (IOException e) {
        System.out.printf("XXX: %s %s%n", url, e);
      }
    }
  }

  public void fetch(URL url) throws IOException {
    // Skip hosts that we've visited many times.
    AtomicInteger hostnameCount = new AtomicInteger();
    AtomicInteger previous = hostnames.putIfAbsent(url.getHost(), hostnameCount);
    if (previous != null) hostnameCount = previous;
    if (hostnameCount.incrementAndGet() > 100) return;

    Request request = new Request.Builder()
        .url(url)
        .build();
    Response response = client.newCall(request).execute();
    String responseSource = response.networkResponse() != null
        ? ("(network: " + response.networkResponse().code() + " over " + response.protocol() + ")")
        : "(cache)";
    int responseCode = response.code();

    System.out.printf("%03d: %s %s%n", responseCode, url, responseSource);

    String contentType = response.header("Content-Type");
    if (responseCode != 200 || contentType == null) {
      response.body().close();
      return;
    }

    Document document = Jsoup.parse(response.body().string(), url.toString());
    for (Element element : document.select("a[href]")) {
      String href = element.attr("href");
      URL link = parseUrl(response.request().url(), href);
      if (link != null) queue.add(link);
    }
  }

  private URL parseUrl(URL url, String href) {
    try {
      URL result = new URL(url, href);
      return result.getProtocol().equals("http") || result.getProtocol().equals("https")
          ? result
          : null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Usage: Crawler <cache dir> <root>");
      return;
    }

    int threadCount = 20;
    long cacheByteCount = 1024L * 1024L * 100L;

    OkHttpClient client = new OkHttpClient();
    Cache cache = new Cache(new File(args[0]), cacheByteCount);
    client.setCache(cache);

    Crawler crawler = new Crawler(client);
    crawler.queue.add(new URL(args[1]));
    crawler.parallelDrainQueue(threadCount);
  }
  
  
  public static void startCrawler(String url) throws IOException {


	    int threadCount = 20;
	    long cacheByteCount = 1024L * 1024L * 100L;

	    OkHttpClient client = new OkHttpClient();
	    Cache cache = new Cache(new File(System.getProperty("user.dir")+"/cache"), cacheByteCount);
	    client.setCache(cache);

	    Crawler crawler = new Crawler(client);
	    crawler.queue.add(new URL(url));
	    crawler.parallelDrainQueue(threadCount);
	  }  
  
}