package com.harvey.demo.component;

import com.harvey.natto.Component;

/**
 * @author harvey
 */
@Component
public class StockService {
    public void incrStock() {
        System.out.println("increase stock");
    }
    
    public void decrStock() {
        System.out.println("decrease stock");
    }
}
