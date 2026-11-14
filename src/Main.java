public class Main {

    public static void main(String[] args) throws InterruptedException {
        SingleCoreSimulator.run(new String[]{"NSJF"});
    }

    public static void CPU(SharedThread task, int cycles) throws InterruptedException {
        if (task.assignCPU(cycles)) {
            task.waitForCompletion();
        }
    }
    public static void CPU(SharedThread task) throws InterruptedException {
        task.waitForCompletion();
    }
}
