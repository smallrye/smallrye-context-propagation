package io.smallrye.concurrency.test;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class MyBeanImpl implements MyBean {
    private long id;
    
    
    public MyBeanImpl() {
        System.err.println("CREATE "+this+" from "+Thread.currentThread());
    }
    
    @Override
    public long getId() {
        System.err.println("getId "+this+" from "+Thread.currentThread());
        return id;
    }

    @Override
    public void setId(long id) {
        System.err.println("setId "+this+" from "+Thread.currentThread());
        this.id = id;
    }
}
