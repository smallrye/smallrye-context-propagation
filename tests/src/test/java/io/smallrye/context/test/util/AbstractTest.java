package io.smallrye.context.test.util;

import org.junit.BeforeClass;

import io.smallrye.context.SmallRyeContextManagerProvider;

/**
 * A parent test which should be extended by all test classes in this module.
 * <p/>
 * This class defines {@code @BeforeClass} callback making sure each test starts with a clean
 * {@code SmallryeContextManager} state. This is because {@code SmallRyeContextManagerProvider} keeps a map of
 * SR Context Managers per class loader and since all tests run in a single class loader, any optimization efforts
 * using caching may lead to issues.
 */
public abstract class AbstractTest {

    @BeforeClass
    public static void performContextManagerCleanup() {
        SmallRyeContextManagerProvider.instance().releaseContextManager(SmallRyeContextManagerProvider.getManager());
    }

}
