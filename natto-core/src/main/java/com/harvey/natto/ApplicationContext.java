package com.harvey.natto;

import com.harvey.natto.aware.BeanNameAware;
import com.harvey.natto.aware.BeanPostProcessor;
import com.harvey.natto.aware.InitializingBean;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author harvey
 */
public class ApplicationContext {
    private final Class<?> configClass;
    
    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    
    private final Map<String, Object> singletonBeans = new ConcurrentHashMap<>();
    
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    
    public ApplicationContext(Class<?> configClass) {
        this.configClass = configClass;
        
        File[] files = getScannedFiles(configClass);
        
        addBeanDefinitions(files);
        
        createBeans();
    }
    
    private void addBeanDefinitions(File[] files) {
        for (File file : files) {
            String fileAbsolutePath = file.getAbsolutePath();
            String fullClassName = fileAbsolutePath.substring(fileAbsolutePath.indexOf("com"), fileAbsolutePath.indexOf(".class"));
            fullClassName = fullClassName.replace("/", ".");
            
            Class<?> beanClass;
            try {
                beanClass = Class.forName(fullClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            
            Component componentAnno = beanClass.getAnnotation(Component.class);
            if (componentAnno == null) {
                continue;
            }
            
            addBeanPostProcessor(beanClass);
            
            String beanName = componentAnno.value();
            if ("".equals(beanName)) {
                beanName = Introspector.decapitalize(beanClass.getSimpleName());
            }
            
            ScopeEnum scope = ScopeEnum.SINGLETON;
            Scope scopeAnno = beanClass.getAnnotation(Scope.class);
            if (scopeAnno != null) {
                scope = scopeAnno.value();
            }
            
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setBeanClass(beanClass);
            beanDefinition.setScope(scope);
            
            beanDefinitions.put(beanName, beanDefinition);
        }
    }
    
    private static File[] getScannedFiles(Class<?> configClass) {
        ComponentScan componentScanAnno = configClass.getAnnotation(ComponentScan.class);
        if (componentScanAnno == null) {
            throw new RuntimeException();
        }
        
        String componentScanPath = componentScanAnno.value();
        componentScanPath = componentScanPath.replace(".", "/");
        
        ClassLoader classLoader = ApplicationContext.class.getClassLoader();
        URL componentResource = classLoader.getResource(componentScanPath);
        if (componentResource == null) {
            throw new RuntimeException();
        }
        
        File componentDir = new File(componentResource.getFile());
        if (!componentDir.isDirectory()) {
            throw new RuntimeException();
        }
        
        File[] files = componentDir.listFiles();
        if (files == null) {
            throw new RuntimeException();
        }
        return files;
    }
    
    private void createBeans() {
        for (String beanName : beanDefinitions.keySet()) {
            BeanDefinition beanDefinition = beanDefinitions.get(beanName);
            ScopeEnum scope = beanDefinition.getScope();
            if (scope == ScopeEnum.SINGLETON) {
                Object singletonBean = createBean(beanName, beanDefinition);
                singletonBeans.put(beanName, singletonBean);
            }
        }
    }
    
    private void addBeanPostProcessor(Class<?> beanClass) {
        if (BeanPostProcessor.class.isAssignableFrom(beanClass)) {
            try {
                beanPostProcessors.add((BeanPostProcessor) beanClass.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public <T> T getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitions.get(beanName);
        if (beanDefinition == null) {
            throw new RuntimeException();
        }
        
        ScopeEnum scope = beanDefinition.getScope();
        if (scope == ScopeEnum.SINGLETON) {
            return (T) getSingletonBean(beanName, beanDefinition);
        } else if (scope == ScopeEnum.PROTOTYPE) {
            return (T) createBean(beanName, beanDefinition);
        }
        
        return null;
    }
    
    private Object getSingletonBean(String beanName, BeanDefinition beanDefinition) {
        Object singletonBean = singletonBeans.get(beanName);
        if (singletonBean != null) {
            return singletonBean;
        }
        singletonBean = createBean(beanName, beanDefinition);
        singletonBeans.put(beanName, singletonBean);
        return singletonBean;
    }
    
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class<?> beanClass = beanDefinition.getBeanClass();
        
        try {
            Object bean = beanClass.getDeclaredConstructor().newInstance();
            
            Field[] fields = beanClass.getDeclaredFields();
            
            injectDependencyBean(bean, fields);
            
            handleBeanNameAware(bean, beanName);
            
            bean = postProcessBeforeInitialization(beanName, bean);
            
            initialization(bean);
            
            bean = postProcessAfterInitialization(beanName, bean);
            
            return bean;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void injectDependencyBean(Object bean, Field[] fields) throws IllegalAccessException {
        for (Field field : fields) {
            Autowired autowiredAnno = field.getAnnotation(Autowired.class);
            if (autowiredAnno == null) {
                continue;
            }
            
            String dependencyBeanName = autowiredAnno.value();
            if ("".equals(dependencyBeanName)) {
                dependencyBeanName = field.getName();
            }
            
            field.setAccessible(true);
            field.set(bean, getBean(dependencyBeanName));
        }
    }
    
    private void handleBeanNameAware(Object bean, String beanName) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
    }
    
    private static void initialization(Object bean) throws Exception {
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }
    
    private Object postProcessBeforeInitialization(String beanName, Object bean) throws Exception {
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            bean = beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
        }
        return bean;
    }
    
    private Object postProcessAfterInitialization(String beanName, Object bean) throws Exception {
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            bean = beanPostProcessor.postProcessAfterInitialization(bean, beanName);
        }
        return bean;
    }
}
