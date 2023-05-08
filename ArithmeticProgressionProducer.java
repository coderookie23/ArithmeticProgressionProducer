//Producer/Consumer
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ArithmeticProgressionProducer implements Runnable {
    private final BlockingQueue<Integer> queue;
    private final int start;
    private final int increment;
    private final long minIntervalMillis;
    private final long maxIntervalMillis;
    private final Random random = new Random();

    public ArithmeticProgressionProducer(BlockingQueue<Integer> queue, int start, int increment, long minIntervalMillis, long maxIntervalMillis) {
        this.queue = queue;
        this.start = start;
        this.increment = increment;
        this.minIntervalMillis = minIntervalMillis;
        this.maxIntervalMillis = maxIntervalMillis;
    }

    @Override
    public void run() {
        int currentValue = start;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("Produced: " + currentValue);
                queue.put(currentValue);
                currentValue += increment;
                Thread.sleep(random.nextInt((int) (maxIntervalMillis - minIntervalMillis)) + minIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    private static class Printer implements Runnable {
        private final BlockingQueue<Integer> queue;
        private final long minPrintWindowMillis;
        private final long maxPrintWindowMillis;
        private final AtomicBoolean isPrinting = new AtomicBoolean(false);

        public Printer(BlockingQueue<Integer> queue, long minPrintWindowMillis, long maxPrintWindowMillis) {
            this.queue = queue;
            this.minPrintWindowMillis = minPrintWindowMillis;
            this.maxPrintWindowMillis = maxPrintWindowMillis;
        }

        @Override
        public void run() {
            Stack<Integer> stack = new Stack<Integer>();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isPrinting.compareAndSet(false, true)) {
                        // start printing window
                        long printWindowMillis = ThreadLocalRandom.current().nextLong(minPrintWindowMillis, maxPrintWindowMillis + 1);
                        long endTimeMillis = System.currentTimeMillis() + printWindowMillis;
                        while (System.currentTimeMillis() < endTimeMillis && !queue.isEmpty()) {
                            Integer num = queue.poll();
                            if (num != null) {
                                stack.push(num);
                                System.out.println("Consumed: " + num);
                            }
                        }
                        isPrinting.set(false);
                        // print in LIFO order
                        while (!stack.isEmpty()) {
                            Integer num = stack.pop();
                            System.out.println("Printed: " + num);
                        }
                    } else {
                        // wait until printing window is over
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    public static void main(String[] args) {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<Integer>();
        ArithmeticProgressionProducer producer1 = new ArithmeticProgressionProducer(queue, 3, 3, 1000, 2000);
        ArithmeticProgressionProducer producer2 = new ArithmeticProgressionProducer(queue, 5, 5, 1500, 2500);
        Printer printer = new Printer(queue, 5000, 10000);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.execute(producer1);
        executor.execute(producer2);
        executor.execute(printer);

        // Wait for some time to allow the producers to generate numbers
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Shut down the executor
        executor.shutdownNow();
    }
}
