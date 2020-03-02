package com.gfm.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AopConfig {

    @Pointcut("execution(public * com.gfm.controller.*.*(..))")
    public void pointCut(){

    }

    @Before("pointCut()")
    public void before(JoinPoint joinPoint) {
        System.out.println("------------before");
    }

    @After("pointCut()")
    public void after(JoinPoint joinPoint){
        System.out.println("-----------after");
    }

    /**
     * 环绕通知,环绕增强，相当于MethodInterceptor
     * @param pjp
     * @return
     */
    @Around("pointCut()")
    public Object around(ProceedingJoinPoint pjp) {
        System.out.println("-------around start");
        try{
            Object result = pjp.proceed();
            System.out.println("-------around end");
            return result;
        }catch (Throwable e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 后置异常通知
     * @param jp
     */
    @AfterThrowing("pointCut()")
    public void afterThrowing(JoinPoint jp){
        System.out.println("---------- afterThrowing");
    }

    /**
     * 后置返回通知
     * @param ret
     * @throws Throwable
     */
    @AfterReturning(returning = "ret", pointcut = "pointCut()")
    public void afterReturning(Object ret) throws Throwable {
        System.out.println("---------- afterReturning");
    }

}
