package com.swoag.logalong.entities;
/* Copyright (C) 2015 SWOAG Technology <www.swoag.com> */

public class LUser {
    private String name;
    private String fullName;
    private int id;

    public LUser() {
        name = "";
        fullName = "";
        id = 0;
    }

    public LUser(String name, String fullName, int id) {
        this.name = name;
        this.fullName = fullName;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
