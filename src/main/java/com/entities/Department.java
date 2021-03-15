package com.entities;

import com.annotations.Column;
import com.annotations.Entity;
import com.annotations.PrimaryKey;

@Entity(name = "departments")
public class Department {
    @PrimaryKey(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    public Department() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "|" + this.getId() + "|" + this.getName();
    }
}
