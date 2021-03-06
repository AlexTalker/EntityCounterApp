import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.nio.file.FileVisitResult.CONTINUE;
// TODO: Possibly merge with TokenCounter
/**
 * Prepare Runnable object to handle console arguments with TokenCounter
 *
 * @see TokenCounter
 */
public class TokenCounterTask implements Runnable {

    private class DirectoryVisitor extends SimpleFileVisitor<Path>{
        private List<Future<?>> tasks = new LinkedList<>();
        private ExecutorService executorService = Executors.newCachedThreadPool();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException {
            if(Files.probeContentType(file).equals("text/html")){
                tasks.add(executorService.submit(new TokenCounterTask(file.toString())));
            }
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException error){
            // TODO: Notify or do not notify? That's a question!
            return CONTINUE;
        }
    }

    private String path;
    private StringBuilder out = new StringBuilder();
/**
 * Construct new task to handle file(s) by path.
 * @param path: path to file or directory, or URL otherwise
 * */
    public TokenCounterTask(String path) {
        this.path = path;
    }

    private void print(String s) {
        out.append(String.format("%s\n", s));
    }

    //* Simple synchronized printf wapper
    private static synchronized void printf(String format, Object... args){
        synchronized (System.out){
            System.out.printf(format, args);
        }
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        printf("Start handle source %s ...\n", path);
        print(String.format("<===> %s <===>", path));
        try {
            URL url;
            try {
                url = new URL(path);
            } catch (MalformedURLException e) {
                File file = new File(path);
                if(file.isDirectory()){
                    printf("%s is a directory, walk through...\n", path);
                    DirectoryVisitor visitor = new DirectoryVisitor();
                    Files.walkFileTree(file.toPath(), visitor);
                    try {
                        for (Future<?> task : visitor.tasks) {
                            task.get();
                        }
                    }
                    catch (InterruptedException error) {} // DIRTY
                    catch (ExecutionException error) {}
                    finally {
                        visitor.executorService.shutdownNow();
                    }
                    return;
                }
                url = file.toURI().toURL();
            }
            try(InputStream in = url.openStream()) { // This will fail on HTTP connections either than 200 OK
                TokenCounter tokenCounter = new TokenCounter(in);
                print(tokenCounter.toString());
                tokenCounter.errors.forEach(this::print);
            }
        } catch (SecurityException error) {
            print("Fail because no access to read the file or directory.");
        } catch (MalformedURLException error) {
            print("Fail to understand the path.");
        } catch (IOException error) {
            print("Fail to read the source.");
        } finally {
            synchronized (System.out) {
                print(String.format("Finished in %s seconds.",
                        ( System.currentTimeMillis() - start) / 1000.0 ));
                System.out.print(out.toString());
            }
        }
    }
}
