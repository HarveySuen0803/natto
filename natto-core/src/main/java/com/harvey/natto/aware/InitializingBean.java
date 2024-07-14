package com.harvey.natto.aware;

/**
 * @author harvey
 */
public interface InitializingBean {
    void afterPropertiesSet() throws Exception;
}
