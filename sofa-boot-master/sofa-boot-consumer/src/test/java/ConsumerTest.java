import com.quark.boot.ConsumerApplicationRun;
import com.quark.boot.consumer.JvmServiceConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试消费者对服务者的调用
 */
@SpringBootTest(classes = ConsumerApplicationRun.class)
@RunWith(SpringRunner.class)
public class ConsumerTest {
    @Autowired
    private JvmServiceConsumer consumer;

    @Test
    public void test() {
        consumer.init();
    }
}
