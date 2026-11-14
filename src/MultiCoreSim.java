import java.util.*;
import java.util.concurrent.Semaphore;

public class MultiCoreSim {
    public static void run() throws InterruptedException {
        //Get input for alg type and # of cores
        String algorithm = "NSJF";
        int n_cores = 4; //Make a string

        //Delete after CLI
        int quantum = 3;
        //

        Random rand = new Random();
        int taskCount = rand.nextInt(25) + 1;

        //Initialize Ready Queue
        Queue<SharedThread> readyQueue = new LinkedList<>();
        Semaphore readyQueue_SEM = new Semaphore(1);

        //Fork random number of task threads [1,25]
        List<SharedThread> tasks = new ArrayList<>();

        for(int i = 1 ; i <= taskCount; i++){
            //Fork Threads with random burst times
            int burstTime = rand.nextInt(50) + 1;
            SharedThread task = new SharedThread(i, burstTime);
            tasks.add(task);
            //Add to ready queue list
            readyQueue_SEM.acquire();
            try {
                readyQueue.add(task);
            }
            finally {
                readyQueue_SEM.release();
            }

            task.start();
            System.out.println("Creating Process Thread: " + i + " W/ burst time: " + burstTime);

        }
        printReadyQueue(readyQueue, readyQueue_SEM);

        int[] completedTasks = {0};
        List<CPU> cpus = new ArrayList<>();
        for (int i = 1; i <= n_cores; i++){
            CPU cpu = new CPU(i, algorithm, readyQueue, quantum, readyQueue_SEM, completedTasks,
                    taskCount);
            cpus.add(cpu);
            System.out.println("Created CPU " + i);
        }
        for (CPU cpu : cpus){
            cpu.start();
        }
        for (CPU cpu : cpus){
            cpu.join();
        }
        for (SharedThread task : tasks){
            task.join();
        }
        System.out.println("All tasks finished");
    }

    public static void printReadyQueue(Queue<SharedThread> readyQueue, Semaphore readyQueue_SEM) throws InterruptedException{
        readyQueue_SEM.acquire();

        try {
            System.out.println("--------------- Ready Queue ---------------");

            for (SharedThread task : readyQueue) {
                System.out.println("ID: " + task.getThreadID() + ", Max Burst:" + task.getBurstTime() + ", Current Burst:" +
                        task.getTimeLeft());
            }
            System.out.println("-------------------------------------------");
        }
        finally {
            readyQueue_SEM.release();
        }
    }
    public static void main(String[] args) throws InterruptedException{
        run();
    }
}
