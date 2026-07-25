package info.kgeorgiy.ja.goge.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class WebCrawler implements AdvancedCrawler {

    private final Downloader downloader;
    private final int perHost;
    private final Map<String, Semaphore> perHostSemaphore = new ConcurrentHashMap<>();
    private final ExecutorService downloadersExecutor;
    private final ExecutorService extractorsExecutor;

    /**
     * Creates a WebCrawler with the given downloader and given number of threads to download,
     * threads to extract links and maximum downloaded websites at one time per host.
     *
     * @param downloader downloader to download websites
     * @param downloaders maximum number of threads to download
     * @param extractors maximum number of threads to extract
     * @param perHost maximum downloaded websites at one time per host
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        this.downloadersExecutor = newFixedThreadPool(downloaders);
        this.extractorsExecutor = newFixedThreadPool(extractors);
    }

    /**
     * Creates WebCrawler with given arguments and downloads website up to specified depth.
     * @param args parameters
     * @throws IOException if an error occurred.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Not enough arguments");
        }
        if (args.length > 5) {
            throw new IllegalArgumentException("To many arguments");
        }
        int depth = args.length < 2 ? 1 : Integer.parseInt(args[1]);
        int downloaders = args.length < 3 ? 1 : Integer.parseInt(args[2]);
        int extractors = args.length < 4 ? 1 : Integer.parseInt(args[3]);
        int perHost = args.length < 5 ? 1 : Integer.parseInt(args[4]);
        try (Crawler webCrawler = new WebCrawler(new CachingDownloader(0), downloaders, extractors, perHost)) {
            System.out.println(webCrawler.download(args[0], depth));
        }
    }



    @Override
    public Result download(String url, int depth, Set<String> excludes) {
        return download(url, depth, excludes, null);
    }

    @Override
    public Result advancedDownload(String url, int depth, List<String> hosts) {
        Set<String> newHosts = new HashSet<>();
        newHosts.addAll(hosts);
        return download(url, depth, null, newHosts);
    }

    @Override
    public Result download(String url, int depth) {
        return download(url, depth, null, null);
    }

    private Result download(String url, int depth, Set<String> excludes, Set<String> hosts) {
        Set<String> allUrls = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        try {
            if ((excludes == null || excludes.stream().noneMatch(url::contains)) &&
                    (hosts == null || hosts.contains(URLUtils.getHost(url)))) {
                List<String> urls = downloader.download(url).extractLinks().stream().toList();
                downloaded.add(url);
                allUrls.add(url);
                download(Set.copyOf(urls), depth, allUrls, errors, downloaded, excludes, hosts);
            }
        } catch (IOException e) {
            errors.put(url, e);
        } catch (ExecutionException e) {
            System.err.println("ExecutionException is thrown");
        } catch (InterruptedException e) {
            System.err.println("Thread was interrupted");
        }
        return new Result(new ArrayList<>(allUrls), errors);
    }

    private void download(Set<String> urls, int depth, Set<String> allUrls,
                          Map<String, IOException> errors,
                          Set<String> downloaded,
                          Set<String> excludes,
                          Set<String> hosts)
            throws ExecutionException, InterruptedException {
        if (depth > 1) {
            List<Future<List<String>>> futureList = urls.stream()
                    .filter(x ->!downloaded.contains(x) && !errors.containsKey(x))
                    .map(x -> downloadersExecutor.submit(() -> downloaderSubmitFunction(x, errors, downloaded, excludes, hosts)))
                    .map(x -> extractorsExecutor.submit(() -> extractorSubmitFunction(x, errors))).toList();
            List<List<String>> list = new ArrayList<>();
            for (Future<List<String>> listFuture : futureList) {
                list.add(listFuture.get());
            }
            allUrls.addAll(urls.stream().filter(x -> !errors.containsKey(x) && downloaded.contains(x)).collect(Collectors.toSet()));
            download(list.stream()
                            .filter(Objects::nonNull)
                            .flatMap(List::stream)
                            .collect(Collectors.toSet()),
                    depth - 1, allUrls, errors, downloaded, excludes, hosts);
        }
    }

    private DocumentURL downloaderSubmitFunction(String url, Map<String, IOException> errors,
                                                 Set<String> downloaded,
                                                 Set<String> excludes, Set<String> hosts)
            throws InterruptedException {
        if (excludes != null && excludes.stream().anyMatch(url::contains)) {
            return null;
        }
        String host;
        try {
            host = URLUtils.getHost(url);
        } catch (MalformedURLException e) {
            errors.put(url, e);
            return null;
        }
        if (hosts != null && !hosts.contains(host)) {
            return null;
        }
        downloaded.add(url);
        try {
            perHostSemaphore.computeIfAbsent(host, (x) -> new Semaphore(perHost));
            perHostSemaphore.get(host).acquire();
            return new DocumentURL(downloader.download(url), url);
        } catch (IOException e) {
            errors.put(url, e);
            return null;
        } finally {
            perHostSemaphore.get(host).release();
        }
    }

    private List<String> extractorSubmitFunction(Future<DocumentURL> future, Map<String, IOException> errors)
            throws ExecutionException, InterruptedException {
        DocumentURL document = future.get();
        if (document == null) {
            return null;
        }
        try {
            return document.document().extractLinks();
        } catch (IOException e) {
            errors.put(document.url(), e);
            return null;
        }
    }

    @Override
    public void close() {
        downloadersExecutor.close();
        extractorsExecutor.close();
    }

    private record DocumentURL(Document document, String url) {}
}
