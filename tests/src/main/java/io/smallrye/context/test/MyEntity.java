package io.smallrye.context.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class MyEntity {
    @Id
    @GeneratedValue
    public Long id;
    
    public String name;
}
