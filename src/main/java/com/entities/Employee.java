package com.entities;

import com.annotations.Column;
import com.annotations.Entity;
import com.annotations.PrimaryKey;

@Entity(name = "employees")
public class Employee {
    @PrimaryKey(name = "id")
    private long id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;

    public Employee() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format("%d|%s|%s",getId(),getFirstName(),getLastName());
    }
}
