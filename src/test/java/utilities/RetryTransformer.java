package utilities;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Annotation Transformer that automatically applies RetryAnalyzer to ALL @Test methods.
 * 
 * This eliminates the need to add retryAnalyzer=RetryAnalyzer.class on every test method.
 * Just register this listener in your suite XML:
 *     <listener class-name="utilities.RetryTransformer"/>
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
