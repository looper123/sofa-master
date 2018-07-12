import com.alipay.sofa.test.annotation.DelegateToRunner;
import com.alipay.sofa.test.runner.SofaJUnit4Runner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;


/**
 * 在编写测试用例时，有时需要指定特殊的 Runner，为了统一编码风格，可以借助注解 @DelegateToRunner 配合 SofaBootRunner 和 SofaJUnit4Runner 使用
 */
@RunWith(SofaJUnit4Runner.class)
@DelegateToRunner(BlockJUnit4ClassRunner.class)
//上面两个注解可以合并成一个@RunWith(BlockJUnit4ClassRunner.class)
public class UnitTestCaseWithoutArk {

    @Test
    public void test() {
        Assert.assertFalse(false);
    }

}
