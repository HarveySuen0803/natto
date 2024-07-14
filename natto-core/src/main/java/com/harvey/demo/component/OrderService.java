package com.harvey.demo.component;

import com.harvey.natto.Autowired;
import com.harvey.natto.Component;
import com.harvey.natto.aware.BeanNameAware;
import com.harvey.natto.aware.InitializingBean;

/**
 * @author harvey
 */
@Component
public class OrderService implements BeanNameAware, InitializingBean {
    @Autowired
    private StockService stockService;
    
    private String beanName;
    
    public void genOrder() {
        stockService.decrStock();
        System.out.println("generate order");
        
        System.out.println(beanName);
    }
    
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    @Override
    public void afterPropertiesSet() {
        System.out.println("InitializingBean: Properties are set. Bean is initialized.");
    }
}
