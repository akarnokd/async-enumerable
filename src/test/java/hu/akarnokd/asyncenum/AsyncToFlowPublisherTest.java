package hu.akarnokd.asyncenum;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.Test;

import java.util.concurrent.Flow;

@Test
public class AsyncToFlowPublisherTest extends FlowPublisherVerification<Integer> {

    public AsyncToFlowPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Integer> createFlowPublisher(long elements) {
        return AsyncEnumerable.range(0, (int)elements).toFlowPublisher();
    }

    @Override
    public Flow.Publisher<Integer> createFailedFlowPublisher() {
        return null;
    }
}
