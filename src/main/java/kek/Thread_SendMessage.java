package kek;

public class Thread_SendMessage extends Thread {
    @Override
    public void run() {
        synchronized (this){
            while (true){
                System.out.println(123);
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void not(){
        synchronized (this){
            this.notify();
        }
    }
}
