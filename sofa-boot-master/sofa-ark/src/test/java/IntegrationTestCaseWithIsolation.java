import com.alipay.sofa.test.runner.SofaBootRunner;
import com.quark.ark.isolation.SofaArkApplication;
import com.quark.ark.isolation.facade.SampleService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * SofaBootRunner 会检测应用是否引入 sofa-ark-springboot-starter 依赖 根据 Spring Boot 依赖即服务的原则，如果检测到
 * sofa-ark-springboot-starter 依赖，SofaBootRunner 会使用 SOFABoot 类隔离能力，否则和原生的 SpringRunner 无异；
 */
@RunWith(SofaBootRunner.class)
@SpringBootTest(classes = SofaArkApplication.class)
public class IntegrationTestCaseWithIsolation {
    @Autowired
    private SampleService sampleService;
    @Test
    public void test() {
        Assert.assertTrue("service".equals(sampleService.service()));
    }
}

