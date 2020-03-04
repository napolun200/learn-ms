package com.gfm.controller;

import com.gfm.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class UserController {

    @Resource
    private UserService userService;

    @RequestMapping("queryUserDetail")
    public String queryUserDetail(String name){
        System.out.println("------name:"+ name);
        userService.showName(name);
        return "success";
    }

}
