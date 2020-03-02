package com.gfm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication
public class LearnApplication {

//    public static void main(String[] args) {
//        ApplicationContext application =  new ClassPathXmlApplicationContext("classpath:application.xml.bak");
//        UserService userService = application.getBean(UserService.class);
//        String name = userService.showName("jack");
//        System.out.println("----spring "+name);
//    }

    public static void main(String[] args) {
        SpringApplication.run(LearnApplication.class, args);
    }

}
