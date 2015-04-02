package com.bedatadriven.appengine.cloudsql;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;

import java.util.logging.Logger;


public class TimingInterceptor extends EmptyInterceptor {

    private static final Logger LOGGER = Logger.getLogger(TimingInterceptor.class.getName());
    
    public TimingInterceptor() {
        LOGGER.info("Instantiating TimingInterceptor");
    }

    @Override
    public void afterTransactionBegin(Transaction tx) {
        RequestStats.current().getTransactionStopwatch().start();
    }

    @Override
    public void afterTransactionCompletion(Transaction tx) {
        RequestStats.current().getTransactionStopwatch().stop();
    }
    
    
}
