package com.harvey.demo.component;

import com.harvey.natto.Component;

/**
 * @author harvey
 */
@Component
public class UserServiceImpl implements UserService {
    @Override
    public void sayHello() {
        System.out.println("hello world");
    }
}
