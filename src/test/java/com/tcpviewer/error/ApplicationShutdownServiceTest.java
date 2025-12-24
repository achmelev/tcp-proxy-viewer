package com.tcpviewer.error;

import com.tcpviewer.javafx.wrapper.PlatformWrapper;
import com.tcpviewer.model.ProxySession;
import com.tcpviewer.service.ProxyService;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApplicationShutdownService.
 * Note: Using test stubs instead of Mockito mocks due to JDK 25 compatibility issues.
 * Uses TestSystemWrapper to intercept System.exit() calls for safe testing.
 */
class ApplicationShutdownServiceTest {

    /**
     * Test stub for ProxyService.
     */
    private static class TestProxyService extends ProxyService {
        public boolean isSessionActive = false;
        public int stopProxySessionCallCount = 0;
        public boolean throwExceptionOnStop = false;

        public TestProxyService() {
            super(null, null, null, null);
        }

        @Override
        public boolean isSessionActive() {
            return isSessionActive;
        }

        @Override
        public void stopProxySession() {
            stopProxySessionCallCount++;
            if (throwExceptionOnStop) {
                throw new RuntimeException("Stop failed");
            }
        }
    }

    /**
     * Test stub for ConfigurableApplicationContext.
     */
    private static class TestApplicationContext implements ConfigurableApplicationContext {
        public int closeCallCount = 0;
        public boolean throwExceptionOnClose = false;

        @Override
        public void close() {
            closeCallCount++;
            if (throwExceptionOnClose) {
                throw new RuntimeException("Context close failed");
            }
        }

        // Minimal implementations for required interface methods
        @Override public String getId() { return "test"; }
        @Override public String getApplicationName() { return "test"; }
        @Override public String getDisplayName() { return "test"; }
        @Override public long getStartupDate() { return 0; }
        @Override public org.springframework.context.ApplicationContext getParent() { return null; }
        @Override public org.springframework.beans.factory.config.AutowireCapableBeanFactory getAutowireCapableBeanFactory() { return null; }
        @Override public void setId(String id) {}
        @Override public void setParent(org.springframework.context.ApplicationContext parent) {}
        @Override public void setEnvironment(org.springframework.core.env.ConfigurableEnvironment environment) {}
        @Override public org.springframework.core.env.ConfigurableEnvironment getEnvironment() { return null; }
        @Override public void addBeanFactoryPostProcessor(org.springframework.beans.factory.config.BeanFactoryPostProcessor postProcessor) {}
        @Override public void addApplicationListener(org.springframework.context.ApplicationListener<?> listener) {}
        @Override public void removeApplicationListener(org.springframework.context.ApplicationListener<?> listener) {}
        @Override public void setClassLoader(ClassLoader classLoader) {}
        @Override public void addProtocolResolver(org.springframework.core.io.ProtocolResolver resolver) {}
        @Override public void refresh() {}
        @Override public void registerShutdownHook() {}
        @Override public boolean isActive() { return false; }
        @Override public boolean isRunning() { return false; }
        @Override public void start() {}
        @Override public void stop() {}
        @Override public org.springframework.core.metrics.ApplicationStartup getApplicationStartup() { return null; }
        @Override public void setApplicationStartup(org.springframework.core.metrics.ApplicationStartup applicationStartup) {}
        @Override public org.springframework.beans.factory.config.ConfigurableListableBeanFactory getBeanFactory() { return null; }
        @Override public org.springframework.beans.factory.BeanFactory getParentBeanFactory() { return null; }
        @Override public boolean containsLocalBean(String name) { return false; }
        @Override public boolean containsBeanDefinition(String beanName) { return false; }
        @Override public int getBeanDefinitionCount() { return 0; }
        @Override public String[] getBeanDefinitionNames() { return new String[0]; }
        @Override public <T> org.springframework.beans.factory.ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) { return null; }
        @Override public <T> org.springframework.beans.factory.ObjectProvider<T> getBeanProvider(org.springframework.core.ResolvableType requiredType, boolean allowEagerInit) { return null; }
        @Override public String[] getBeanNamesForType(org.springframework.core.ResolvableType type) { return new String[0]; }
        @Override public String[] getBeanNamesForType(org.springframework.core.ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) { return new String[0]; }
        @Override public String[] getBeanNamesForType(Class<?> type) { return new String[0]; }
        @Override public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) { return new String[0]; }
        @Override public <T> java.util.Map<String, T> getBeansOfType(Class<T> type) { return java.util.Collections.emptyMap(); }
        @Override public <T> java.util.Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) { return java.util.Collections.emptyMap(); }
        @Override public String[] getBeanNamesForAnnotation(Class<? extends java.lang.annotation.Annotation> annotationType) { return new String[0]; }
        @Override public java.util.Map<String, Object> getBeansWithAnnotation(Class<? extends java.lang.annotation.Annotation> annotationType) { return java.util.Collections.emptyMap(); }
        @Override public <A extends java.lang.annotation.Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) { return null; }
        @Override public <A extends java.lang.annotation.Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) { return null; }
        @Override public <A extends java.lang.annotation.Annotation> java.util.Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) { return java.util.Collections.emptySet(); }
        @Override public Object getBean(String name) { return null; }
        @Override public <T> T getBean(String name, Class<T> requiredType) { return null; }
        @Override public Object getBean(String name, Object... args) { return null; }
        @Override public <T> T getBean(Class<T> requiredType) { return null; }
        @Override public <T> T getBean(Class<T> requiredType, Object... args) { return null; }
        @Override public <T> org.springframework.beans.factory.ObjectProvider<T> getBeanProvider(Class<T> requiredType) { return null; }
        @Override public <T> org.springframework.beans.factory.ObjectProvider<T> getBeanProvider(org.springframework.core.ResolvableType requiredType) { return null; }
        @Override public boolean containsBean(String name) { return false; }
        @Override public boolean isSingleton(String name) { return false; }
        @Override public boolean isPrototype(String name) { return false; }
        @Override public boolean isTypeMatch(String name, org.springframework.core.ResolvableType typeToMatch) { return false; }
        @Override public boolean isTypeMatch(String name, Class<?> typeToMatch) { return false; }
        @Override public Class<?> getType(String name) { return null; }
        @Override public Class<?> getType(String name, boolean allowFactoryBeanInit) { return null; }
        @Override public String[] getAliases(String name) { return new String[0]; }
        @Override public void publishEvent(Object event) {}
        @Override public void publishEvent(org.springframework.context.ApplicationEvent event) {}
        @Override public org.springframework.core.io.Resource[] getResources(String locationPattern) { return new org.springframework.core.io.Resource[0]; }
        @Override public org.springframework.core.io.Resource getResource(String location) { return null; }
        public ClassLoader getClassLoader() { return null; }
        public org.springframework.context.MessageSourceResolvable resolveMessage(org.springframework.context.MessageSourceResolvable resolvable, java.util.Locale locale) { return null; }
        @Override public String getMessage(String code, Object[] args, String defaultMessage, java.util.Locale locale) { return null; }
        @Override public String getMessage(String code, Object[] args, java.util.Locale locale) { return null; }
        @Override public String getMessage(org.springframework.context.MessageSourceResolvable resolvable, java.util.Locale locale) { return null; }
    }

    /**
     * Test stub for PlatformWrapper.
     */
    private static class TestPlatformWrapper implements PlatformWrapper {
        public List<Runnable> runnables = new ArrayList<>();

        @Override
        public void runLater(Runnable runnable) {
            runnables.add(runnable);
            // Don't execute - would try to call Platform.exit()
        }
    }

    /**
     * Test stub for SystemWrapper.
     */
    private static class TestSystemWrapper implements com.tcpviewer.lang.wrapper.SystemWrapper {
        public int exitCallCount = 0;
        public int lastExitStatus = -1;

        @Override
        public void exit(int status) {
            exitCallCount++;
            lastExitStatus = status;
            // Don't actually exit - just record the call
        }
    }

    private TestProxyService proxyService;
    private TestApplicationContext applicationContext;
    private TestPlatformWrapper platformWrapper;
    private TestSystemWrapper systemWrapper;
    private ApplicationShutdownService shutdownService;

    @BeforeEach
    void setUp() {
        proxyService = new TestProxyService();
        applicationContext = new TestApplicationContext();
        platformWrapper = new TestPlatformWrapper();
        systemWrapper = new TestSystemWrapper();
        shutdownService = new ApplicationShutdownService(proxyService, applicationContext, platformWrapper, systemWrapper);
    }

    @Test
    void testConstructor_allowsNullParameters() {
        // After removing validation, constructor should allow null parameters for testing
        assertDoesNotThrow(() ->
            new ApplicationShutdownService(null, null, null, null));
    }

    @Test
    void testInitiateGracefulShutdown_startsShutdownThread() throws InterruptedException {
        IOException exception = new IOException("Fatal error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.SYSTEM_RESOURCE)
                .severity(ErrorSeverity.FATAL)
                .userMessage("System error occurred")
                .technicalDetails("IOException: Fatal error")
                .build();

        proxyService.isSessionActive = false;

        // This test verifies the method returns quickly (doesn't block)
        long startTime = System.currentTimeMillis();
        shutdownService.initiateGracefulShutdown(errorContext);
        long duration = System.currentTimeMillis() - startTime;

        // Should return almost immediately (starts thread and returns)
        assertTrue(duration < 100, "initiate shutdown should return quickly");

        // Give the shutdown thread time to execute
        Thread.sleep(1500);

        // Verify System.exit() was called
        assertEquals(1, systemWrapper.exitCallCount, "System.exit() should be called once");
        assertEquals(1, systemWrapper.lastExitStatus, "Exit status should be 1");
    }

    @Test
    void testInitiateGracefulShutdown_withActiveSession() throws InterruptedException {
        IOException exception = new IOException("Fatal error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.PROXY_SERVER)
                .severity(ErrorSeverity.FATAL)
                .userMessage("Proxy server failed")
                .technicalDetails("IOException: Fatal error")
                .build();

        proxyService.isSessionActive = true;

        // Initiate shutdown
        shutdownService.initiateGracefulShutdown(errorContext);

        // Give the shutdown thread time to execute (needs at least 3 seconds for proxy session wait)
        Thread.sleep(4000);

        // Verify proxy session stop was attempted
        assertTrue(proxyService.stopProxySessionCallCount >= 1,
            "stopProxySession should be called at least once");

        // Verify System.exit() was called
        assertEquals(1, systemWrapper.exitCallCount, "System.exit() should be called once");
        assertEquals(1, systemWrapper.lastExitStatus, "Exit status should be 1");
    }

    @Test
    void testInitiateGracefulShutdown_handlesExceptionDuringProxyStop() throws InterruptedException {
        IOException exception = new IOException("Fatal error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.INITIALIZATION)
                .severity(ErrorSeverity.FATAL)
                .userMessage("Initialization failed")
                .technicalDetails("IOException: Fatal error")
                .build();

        proxyService.isSessionActive = true;
        proxyService.throwExceptionOnStop = true;

        // Should not throw exception even if proxy stop fails
        assertDoesNotThrow(() -> {
            shutdownService.initiateGracefulShutdown(errorContext);
            Thread.sleep(1500);
        });

        // Verify shutdown still completes and exits
        assertEquals(1, systemWrapper.exitCallCount, "System.exit() should still be called");
    }

    @Test
    void testInitiateGracefulShutdown_handlesExceptionDuringContextClose() throws InterruptedException {
        IOException exception = new IOException("Fatal error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.UI_OPERATION)
                .severity(ErrorSeverity.FATAL)
                .userMessage("UI failed")
                .technicalDetails("IOException: Fatal error")
                .build();

        proxyService.isSessionActive = false;
        applicationContext.throwExceptionOnClose = true;

        // Should not throw exception even if context close fails
        assertDoesNotThrow(() -> {
            shutdownService.initiateGracefulShutdown(errorContext);
            Thread.sleep(1500);
        });

        // Verify shutdown still completes and exits
        assertEquals(1, systemWrapper.exitCallCount, "System.exit() should still be called");
        assertEquals(1, applicationContext.closeCallCount, "Context close should be attempted");
    }

    @Test
    void testShutdownService_createsValidInstance() {
        assertNotNull(shutdownService);
    }

    @Test
    void testInitiateGracefulShutdown_callsPlatformWrapperRunLater() throws InterruptedException {
        IOException exception = new IOException("Test error");
        ErrorContext errorContext = ErrorContext.builder()
                .throwable(exception)
                .category(ErrorCategory.NETWORK_IO)
                .severity(ErrorSeverity.FATAL)
                .userMessage("Network error")
                .technicalDetails("IOException: Test error")
                .build();

        proxyService.isSessionActive = false;

        shutdownService.initiateGracefulShutdown(errorContext);

        // Wait for shutdown thread to execute
        Thread.sleep(1500);

        // Verify Platform.runLater was called for JavaFX exit
        assertTrue(platformWrapper.runnables.size() >= 1,
            "Expected at least one Platform.runLater call, but got " + platformWrapper.runnables.size());

        // Verify System.exit() was called
        assertEquals(1, systemWrapper.exitCallCount, "System.exit() should be called once");
    }
}
