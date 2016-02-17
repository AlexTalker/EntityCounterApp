import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by alx on 17.02.16.
 */
public class TokenCounterTask implements Runnable {

    private String path;
    private StringBuilder out = new StringBuilder();

    public TokenCounterTask(String path) {
        this.path = path;
    }

    private void print(String s) {
        out.append(String.format("%s: %s\n", path, s));
    }

    @Override
    public void run() {
        InputStream in;
        long start = System.currentTimeMillis();
        try {
            URL url;
            try {
                url = new URL(path);
            } catch (MalformedURLException e) {
                url = new File(path).toURI().toURL();
            }
            in = url.openStream();
            print(new TokenCounter(in).toString());
        } catch (MalformedURLException error) {
            print("Fail to understand the path.");
        } catch (IOException e) {
            print("Fail to read the source.");
        } finally {
            synchronized (System.out) {
                print(String.format("Finished in %s milliseconds.", System.currentTimeMillis() - start));
                System.out.print(out.toString());
            }
        }
    }


    public static void main(String[] args) throws Exception {
        List<Future<?>> tasks = new LinkedList<>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (String task : args) {
            tasks.add(executorService.submit(new TokenCounterTask(task)));
        }
        try {
            for (Future<?> task : tasks) {
                task.get();
            }
        } finally {
            executorService.shutdownNow();
        }
    }
}
