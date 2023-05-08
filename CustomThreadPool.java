import java.util.LinkedList;

public class CustomThreadPool {
    private final int poolSize;
    private final WorkerThread[] threads;
    private final LinkedList<Runnable> taskQueue;

    public CustomThreadPool(int poolSize) {
        this.poolSize = poolSize;
        this.threads = new WorkerThread[poolSize];
        this.taskQueue = new LinkedList<Runnable>();

        for (int i = 0; i < poolSize; i++) {
            threads[i] = new WorkerThread();
            threads[i].start();
        }
    }

    public void execute(Runnable task) {
        synchronized (taskQueue) {
            taskQueue.addLast(task);
            taskQueue.notify();
        }
    }

    private class WorkerThread extends Thread {
        public void run() {
            Runnable task;

            while (true) {
                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                            taskQueue.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                    task = taskQueue.removeFirst();
                }

                try {
                    task.run();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void shutdown() {
        for (WorkerThread thread : threads) {
            thread.interrupt();
        }
    }

    public static void main(String[] args) {
        CustomThreadPool threadPool = new CustomThreadPool(5);

        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            threadPool.execute(new Runnable() {
                public void run() {
                    System.out.println("Task " + taskId + " started");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("Task " + taskId + " finished");
                }
            });
        }

        threadPool.shutdown();
    }
}
