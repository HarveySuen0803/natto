package com.harvey.demo.component;

import com.harvey.natto.Component;
import com.harvey.natto.aware.BeanPostProcessor;

import java.lang.reflect.Proxy;

/**
 * @author harvey
 */
@Component
public class BaseServicePostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
        if ("userServiceImpl".equals(beanName)) {
            return Proxy.newProxyInstance(BaseServicePostProcessor.class.getClassLoader(), bean.getClass().getInterfaces(), (proxy, method, args) -> {
                System.out.println("Do something before method invocation");
                Object result = method.invoke(bean, args);
                System.out.println("Do something after method invocation");
                return result;
            });
        }
        
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }
}
