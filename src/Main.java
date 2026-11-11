public class Main {

    public static void main(String[] args) throws InterruptedException {

    }

    public static void CPU(SharedThread task, int cycles) throws InterruptedException {
        if (task.assignCPU(cycles)) {
            task.waitForCompletion();
        }
    }
}
