package com.gfm.service.impl;

import com.gfm.service.UserService;

public class UserServiceImpl implements UserService {


    @Override
    public String showName(String name) {
        System.out.println("------"+name);
        return name;
    }
}
