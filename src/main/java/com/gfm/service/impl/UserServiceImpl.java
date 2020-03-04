package com.gfm.service.impl;

import com.gfm.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public String showName(String name) {
        System.out.println("------"+name);
        return name;
    }

    @Transactional
    public void saveUserData(){

    }
}
