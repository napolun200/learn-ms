package com.gfm.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @RequestMapping("queryUserDetail")
    public String queryUserDetail(String name){
        System.out.println("------name:"+ name);
        return "success";
    }

}
