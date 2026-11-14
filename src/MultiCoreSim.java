import java.util.*;
import java.util.concurrent.Semaphore;

public class MultiCoreSim {
    public static void run() throws InterruptedException {
        //Get input for alg type and # of cores
        String algorithm = "ALG";
        int n_cores = 4; //Make a string
        Random rand = new Random();
        int taskCount = rand.nextInt(25) + 1;

        //Initialize Ready Queue
        Queue<SharedThread> readyQueue = new LinkedList<>();
        Semaphore readyQueue_SEM = new Semaphore(1);

        //Fork random number of task threads [1,25]
        List<SharedThread> tasks = new ArrayList<>();
        for(int i = 1 ; i < taskCount; i++){
            //Fork Threads with random burst times
            int burstTime = rand.nextInt(50) + 1;
            SharedThread task = new SharedThread(i, burstTime);
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

        //Make Dispatcher threads which are held until all dispatchers are created

    }

    public static void dispatch(int dispatchID, String algorithm, Queue<SharedThread> readyQueue, Semaphore readyQueue_SEM){
        //Select task

        //Select Scheduling Alg
    }
    //Print Ready Queue
    public static void printReadyQueue(Queue<SharedThread> readyQueue, Semaphore readyQueue_SEM) throws InterruptedException{
        System.out.println("--------------- Ready Queue ---------------");
        readyQueue_SEM.acquire();

        for(SharedThread task: readyQueue)
            System.out.println("ID: " + task.getThreadID() + ", Max Burst:" + task.getBurstTime() + ", Current Burst:" + task.getTimeLeft());
        System.out.println("-------------------------------------------");
        readyQueue_SEM.release();
    }
}
