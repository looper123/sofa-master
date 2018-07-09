import com.alipay.sofa.test.runner.SofaJUnit4Runner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SofaJUnit4Runner 同样会检测应用是否引入 sofa-ark-springboot-starter 依赖；根据 Spring Boot 依赖即服务的原则，如果检测到
 * sofa-ark-springboot-starter 依赖，SofaJUnit4Runner 会使用 SOFABoot 类隔离能力，否则和原生的 JUnit4 无异；
 */
@RunWith(SofaJUnit4Runner.class)
public class UnitTestCaseWithIsolation {

    public static final String testClassloader = "com.alipay.sofa.ark.container.test.TestClassLoader";

    @Test
    public void test() {
        ClassLoader currentClassLoader = this.getClass().getClassLoader();
        Assert.assertTrue(currentClassLoader.getClass().getCanonicalName().equals(testClassloader));
    }

}
