package hu.akarnokd.asyncenum;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.SubmissionPublisher;

import static org.junit.Assert.assertEquals;

public class AsyncFromFlowPublisherTest {

    @Test
    public void simple() {
        SubmissionPublisher<Integer> sp = new SubmissionPublisher<>();

        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(100);
                } catch (InterruptedException ex) {
                    sp.closeExceptionally(ex);
                    return;
                }
                for (int i = 0; i < 1000; i++) {
                    sp.submit(i);
                }
                sp.close();
            }
        }.start();

        List<Integer> list = AsyncEnumerable.fromFlowPublisher(sp)
                .toList()
                .blockingFirst();

        assertEquals(1000, list.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }
}
