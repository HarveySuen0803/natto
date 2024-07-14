package com.harvey.demo;

import com.harvey.demo.component.UserService;
import com.harvey.natto.ApplicationConfig;
import com.harvey.natto.ApplicationContext;

/**
 * @author harvey
 */
public class Application {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new ApplicationContext(ApplicationConfig.class);
        
        // OrderService orderService = applicationContext.getBean("orderService");
        // orderService.genOrder();
        
        UserService userService = applicationContext.getBean("userServiceImpl");
        userService.sayHello();
    }
}
