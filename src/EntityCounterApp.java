import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Main entry point
 */
public class EntityCounterApp {
        public static void main(String[] args) throws Exception {
        List<Future<?>> tasks = new LinkedList<>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        Thread hook = new Thread() {
            // Hot implementation stage 3
            // Since finally not always executed by Ctrl-C
            @Override
            public void run() {
                executorService.shutdownNow();
            }
        };
        Runtime.getRuntime().addShutdownHook(hook);
        try {
            for (String task : args) {
                tasks.add(executorService.submit(new TokenCounterTask(task)));
            }
            for (Future<?> task : tasks) {
                task.get();
            }
        }
        finally { // This is repetitive but required to successful finish.
            hook.run();
        }
    }
}
