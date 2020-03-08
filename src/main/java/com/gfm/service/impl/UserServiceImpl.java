package com.gfm.service.impl;

import com.gfm.entity.EcOrderCustomsLog;
import com.gfm.mapper.EcOrderCustomsLogMapper;
import com.gfm.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public String showName(String name) {
        System.out.println("------"+name);
        return name;
    }


    @Resource
    private EcOrderCustomsLogMapper customsLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void saveUserData(){
        EcOrderCustomsLog customsLog = new EcOrderCustomsLog();
        customsLog.setId(1L);
        customsLog.setUserId(556L);
        customsLogMapper.insert(customsLog);
    }

    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        //1>开始触发事务处理
        userService.saveUserData();
    }

}
