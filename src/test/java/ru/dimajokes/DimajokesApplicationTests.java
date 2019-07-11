package ru.dimajokes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(initializers = RedisInitializer.class)
public class DimajokesApplicationTests {

	@Test
	public void contextLoads() {
	}

}
