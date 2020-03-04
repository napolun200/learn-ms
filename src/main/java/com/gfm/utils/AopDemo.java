package com.gfm.utils;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.*;
import org.springframework.aop.framework.*;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.Nullable;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.TransactionAnnotationParser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.*;
import org.springframework.transaction.support.CallbackPreferringPlatformTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.io.Flushable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

//
//import org.aopalliance.aop.Advice;
//import org.aopalliance.intercept.Interceptor;
//import org.aopalliance.intercept.MethodInterceptor;
//import org.aopalliance.intercept.MethodInvocation;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.*;
//import org.aspectj.lang.annotation.Pointcut;
//import org.aspectj.lang.reflect.AjType;
//import org.aspectj.lang.reflect.AjTypeSystem;
//import org.aspectj.lang.reflect.PerClauseKind;
//import org.aspectj.weaver.tools.JoinPointMatch;
//import org.springframework.aop.*;
//import org.springframework.aop.aspectj.*;
//import org.springframework.aop.aspectj.annotation.*;
//import org.springframework.aop.config.AopConfigUtils;
//import org.springframework.aop.framework.Advised;
//import org.springframework.aop.framework.AdvisedSupport;
//import org.springframework.aop.framework.AopConfigException;
//import org.springframework.aop.framework.InterceptorAndDynamicMethodMatcher;
//import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
//import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
//import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
//import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
//import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
//import org.springframework.aop.support.AopUtils;
//import org.springframework.aop.support.Pointcuts;
//import org.springframework.aop.target.SingletonTargetSource;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.*;
//import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
//import org.springframework.beans.factory.config.BeanDefinition;
//import org.springframework.beans.factory.config.BeanPostProcessor;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.beans.factory.support.RootBeanDefinition;
//import org.springframework.cglib.proxy.MethodProxy;
//import org.springframework.context.annotation.*;
//import org.springframework.core.Ordered;
//import org.springframework.core.annotation.*;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.core.convert.converter.ConvertingComparator;
//import org.springframework.core.type.AnnotationMetadata;
//import org.springframework.lang.Nullable;
//import org.springframework.util.Assert;
//import org.springframework.util.ObjectUtils;
//import org.springframework.util.ReflectionUtils;
//import org.springframework.util.StringUtils;
//import org.springframework.util.comparator.InstanceComparator;
//
//import java.lang.annotation.*;
//import java.lang.reflect.*;
//import java.security.AccessController;
//import java.security.PrivilegedAction;
//import java.util.*;
//
public class AopDemo {
//
////    @Target(ElementType.TYPE)
////    @Retention(RetentionPolicy.RUNTIME)
////    @Documented
////    @Import(AspectJAutoProxyRegistrar.class)
////    public @interface EnableAspectJAutoProxy {
////
////        /**
////         * 声明使用CGLIB动态代理，还是JDK动态代理。默认是false,使用jdk动态代理
////         * @return
////         */
////        boolean proxyTargetClass() default false;
////
////        /**
////         * exposeProxy = true;表示通过aop框架暴露该代理对象，aopContext能够访问
////         * @return
////         */
////        boolean exposeProxy() default false;
////    }
////
////
////
////    class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
////
////        /**
////         * 注册，升级和配置基于AspectJ的自动代理
////         */
////        @Override
////        public void registerBeanDefinitions(
////                AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
////
////            //调用registerBeanDefinitions方法，往Spring容器中注册AnnotationAwareAspectJAutoProxyCreator的Bean的定义信息
////            AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
////
////            AnnotationAttributes enableAspectJAutoProxy =
////                    AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
////            if (enableAspectJAutoProxy != null) {
////                if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
////                    AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
////                }
////                if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
////                    AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
////                }
////            }
////        }
////    }
//
//
//
//
//
////    //AopConfigUtils:
////    public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
////        return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
////    }
////
////    //AopConfigUtils:
////    public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
////            BeanDefinitionRegistry registry, @Nullable Object source) {
////
////        return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
////    }
////
////    //AopConfigUtils:
////    private static BeanDefinition registerOrEscalateApcAsRequired(
////            Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {
////        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
////
////        //如果存在org.springframework.aop.config.internalAutoProxyCreator直接使用
////        //AUTO_PROXY_CREATOR_BEAN_NAME = "org.springframework.aop.config.internalAutoProxyCreator"
////        if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
////            BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
////            if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
////                int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
////                int requiredPriority = findPriorityForClass(cls);
////                if (currentPriority < requiredPriority) {
////                    apcDefinition.setBeanClassName(cls.getName());
////                }
////            }
////            return null;
////        }
////
////        //没有就自己注册
////        RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
////        beanDefinition.setSource(source);
////        beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
////        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
////        registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
////        return beanDefinition;
////    }
////
////    //AopConfigUtils:
////    private static int findPriorityForClass(@Nullable String className) {
////        /**
////         * List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3)
////         * APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
////         * PC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
////         * APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
////         */
////        for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
////            Class<?> clazz = APC_PRIORITY_LIST.get(i);
////            if (clazz.getName().equals(className)) {
////                return i;
////            }
////        }
////        throw new IllegalArgumentException(
////                "Class name [" + className + "] is not a known auto-proxy creator class");
////    }
////
//
//
//
//
//
//
////    //AbstractAdvisorAutoProxyCreator:
////    public void setBeanFactory(BeanFactory beanFactory) {
////        super.setBeanFactory(beanFactory);
////        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
////            throw new IllegalArgumentException(
////                    "AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
////        }
////        initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
////    }
////
////    //AbstractAdvisorAutoProxyCreator:
////    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
////        this.advisorRetrievalHelper = new AbstractAdvisorAutoProxyCreator.BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
////    }
//
//
////
////    //AbstractAutowireCapableBeanFactory:
////    protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
////        if (System.getSecurityManager() != null) {
////            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
////                invokeAwareMethods(beanName, bean);
////                return null;
////            }, getAccessControlContext());
////        }else {
////            invokeAwareMethods(beanName, bean);
////        }
////
////        Object wrappedBean = bean;
////        if (mbd == null || !mbd.isSynthetic()) {
////            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
////        }
////
////        try {
////            invokeInitMethods(beanName, wrappedBean, mbd);
////        }catch (Throwable ex) {
////            throw new BeanCreationException(
////                    (mbd != null ? mbd.getResourceDescription() : null),
////                    beanName, "Invocation of init method failed", ex);
////        }
////        if (mbd == null || !mbd.isSynthetic()) {
////            //配置Aop的入口，应用BeanPostProcessorAfter实现
////            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
////        }
////
////        return wrappedBean;
////    }
//
//
//
////    //AbstractAutowireCapableBeanFactory:
////    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
////            throws BeansException {
////
////        Object result = existingBean;
////        //遍历所有的BeanPostProcessor
////        for (BeanPostProcessor processor : getBeanPostProcessors()) {
////            //将AOP配置方法应用到Bean
////            Object current = processor.postProcessAfterInitialization(result, beanName);
////            if (current == null) {
////                return result;
////            }
////            result = current;
////        }
////        return result;
////    }
////
////    //AbstractAutoProxyCreator:
////    public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
////        if (bean != null) {
////            //通过自己封装的key获取（beanClassName_beanName）
////            Object cacheKey = getCacheKey(bean.getClass(), beanName);
////            //缓存中不包含
////            //Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16)
////            if (this.earlyProxyReferences.remove(cacheKey) != bean) {
////                //解析过程
////                return wrapIfNecessary(bean, beanName, cacheKey);
////            }
////        }
////        return bean;
////    }
////
////
////    //AbstractAutoProxyCreator:
////    //解析包装给定的bean
////    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
////        //Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16))
////        if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
////            return bean;
////        }
////        //Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256)
////        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
////            return bean;
////        }
////        //1>如果是基础公共bean, 或者是以 .ORIGINAL作为后缀的原始bean直接跳过
////        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
////            this.advisedBeans.put(cacheKey, Boolean.FALSE);
////            return bean;
////        }
////
////        //2>获取增强器
////        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
////        if (specificInterceptors != DO_NOT_PROXY) {
////            this.advisedBeans.put(cacheKey, Boolean.TRUE);
////            //3>创建代理
////            Object proxy = createProxy(
////                    bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
////            //Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16)
////            this.proxyTypes.put(cacheKey, proxy.getClass());
////            //返回代理过后的bean，加入容器
////            return proxy;
////        }
////
////        this.advisedBeans.put(cacheKey, Boolean.FALSE);
////        return bean;
////    }
//
//
////    //AbstractAutoProxyCreator:
////    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
////        return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
////    }
////
////    //AutoProxyUtils:
////    //根据bean名称是否有后缀.ORIGINAL 判断是否是一个original instance，如果不是返回false
////    static boolean isOriginalInstance(String beanName, Class<?> beanClass) {
////        if (!StringUtils.hasLength(beanName) || beanName.length() !=
////                beanClass.getName().length() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX.length()) {
////            return false;
////        }
////        return (beanName.startsWith(beanClass.getName()) &&
////                beanName.endsWith(AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX));
////    }
//
//
//
//
//
//
//    //AbstractAdvisorAutoProxyCreator:
//    //获取所有通知增强器，增强器可以理解成对需要代理的类的增强,代理过程就是对需要的代理类进行相关处理
//    protected Object[] getAdvicesAndAdvisorsForBean(
//            Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
//        //获取满足条件的通知
//        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
//        if (advisors.isEmpty()) {
//            return DO_NOT_PROXY;
//        }
//        return advisors.toArray();
//    }
//
//    //AbstractAdvisorAutoProxyCreator:
//    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
//        //1>查找所有增强器
//        List<Advisor> candidateAdvisors = findCandidateAdvisors();
//        //2>发现合适的增强器, 即有哪些通知advice可以应用
//        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
//        extendAdvisors(eligibleAdvisors);
//        if (!eligibleAdvisors.isEmpty()) {
//            //排序通知，子类可以重写这个方法自定义排序策略
//            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
//        }
//        return eligibleAdvisors;
//    }
//
//    //AnnotationAwareAspectJAutoProxyCreator:
//    protected List<Advisor> findCandidateAdvisors() {
//        //发现父类是否存在通知advisor
//        List<Advisor> advisors = super.findCandidateAdvisors();
//        //添加新发现的通知advisor
//        if (this.aspectJAdvisorsBuilder != null) {
//            advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
//        }
//        return advisors;
//    }
//
//    //AbstractAdvisorAutoProxyCreator:
//    //获取所有使用自动代理的通知advisor
//    protected List<Advisor> findCandidateAdvisors() {
//        Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
//        //BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;
//        return this.advisorRetrievalHelper.findAdvisorBeans();
//    }
//
//    //BeanFactoryAdvisorRetrievalHelper:
//    //获取当前bean工厂中符合条件的通知bean,忽略FactoryBeans和正在创建的beans
//    public List<Advisor> findAdvisorBeans() {
//        //确定通知bean名称列表,如果没有缓存
//        String[] advisorNames = this.cachedAdvisorBeanNames;
//        if (advisorNames == null) {
//            /**
//             * AOP功能中在这里传入的是Object对象，代表去容器中获取到所有的组件的名称，然后再
//             * 进行遍历，这个过程是十分的消耗性能的，所以说Spring会再这里加入了保存切面信息的缓存。
//             * 但是事务功能不一样，事务模块的功能是直接去容器中获取Advisor类型的，选择范围小，且不消耗性能。
//             * 所以Spring在事务模块中没有加入缓存来保存我们的事务相关的advisor
//             */
//            advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
//                    this.beanFactory, Advisor.class, true, false);
//            this.cachedAdvisorBeanNames = advisorNames;
//        }
//        if (advisorNames.length == 0) {
//            return new ArrayList<>();
//        }
//
//        List<Advisor> advisors = new ArrayList<>();
//        for (String name : advisorNames) {
//            if (isEligibleBean(name)) {
//                if (this.beanFactory.isCurrentlyInCreation(name)) {
//                    if (logger.isTraceEnabled()) {
//                        logger.trace("Skipping currently created advisor '" + name + "'");
//                    }
//                }else {
//                    try {
//                        advisors.add(this.beanFactory.getBean(name, Advisor.class));
//                    }catch (BeanCreationException ex) {
//                        Throwable rootCause = ex.getMostSpecificCause();
//                        if (rootCause instanceof BeanCurrentlyInCreationException) {
//                            BeanCreationException bce = (BeanCreationException) rootCause;
//                            String bceBeanName = bce.getBeanName();
//                            if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {
//                                if (logger.isTraceEnabled()) {
//                                    logger.trace("Skipping advisor '" + name +
//                                            "' with dependency on currently created bean: " + ex.getMessage());
//                                }
//                                // Ignore: indicates a reference back to the bean we're trying to advise.
//                                // We want to find advisors other than the currently created bean itself.
//                                continue;
//                            }
//                        }
//                        throw ex;
//                    }
//                }
//            }
//        }
//        return advisors;
//    }
//
//
//
//
//    //BeanFactoryAspectJAdvisorsBuilder:
//    //去容器中获取到所有的切面信息保存到缓存中
//    public List<Advisor> buildAspectJAdvisors() {
//        List<String> aspectNames = this.aspectBeanNames;
//        //缓存字段aspectNames没有值 注意实例化第一个单实例bean的时候就会触发解析切面
//        if (aspectNames == null) {
//            synchronized (this) {
//                aspectNames = this.aspectBeanNames;
//                if (aspectNames == null) {
//                    //用于保存所有解析出来的Advisors集合对象
//                    List<Advisor> advisors = new ArrayList<>();
//                    //用于保存切面的名称的集合
//                    aspectNames = new ArrayList<>();
//                    /**
//                     * 1>AOP功能中在这里传入的是Object对象，代表去容器中获取到所有的组件的名称，然后再
//                     * 进行遍历，这个过程是十分的消耗性能的，所以说Spring会再这里加入了保存切面信息的缓存。
//                     * 但是事务功能不一样，事务模块的功能是直接去容器中获取Advisor类型的，选择范围小，且不消耗性能。
//                     * 所以Spring在事务模块中没有加入缓存来保存我们的事务相关的advisor
//                     */
//                    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
//                            this.beanFactory, Object.class, true, false);
//                    //遍历我们从IOC容器中获取处的所有Bean的名称
//                    for (String beanName : beanNames) {
//                        if (!isEligibleBean(beanName)) {
//                            continue;
//                        }
//                        //通过beanName去容器中获取到对应class对象
//                        Class<?> beanType = this.beanFactory.getType(beanName);
//                        if (beanType == null) {
//                            continue;
//                        }
//                        //根据class对象判断是不是切面 @Aspect
//                        if (this.advisorFactory.isAspect(beanType)) {
//                            //是切面类, 加入到缓存中
//                            aspectNames.add(beanName);
//                            //把beanName和class对象构建成为一个AspectMetadata
//                            AspectMetadata amd = new AspectMetadata(beanType, beanName);
//                            if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
//                                //构建切面注解的实例工厂
//                                MetadataAwareAspectInstanceFactory factory =
//                                        new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
//                                //2>真正的去获取我们的Advisor
//                                //AspectJAdvisorFactory advisorFactory
//                                List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
//                                //加入到缓存中
//                                if (this.beanFactory.isSingleton(beanName)) {
//                                    //Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>()
//                                    this.advisorsCache.put(beanName, classAdvisors);
//                                }else {
//                                    //Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>()
//                                    this.aspectFactoryCache.put(beanName, factory);
//                                }
//                                advisors.addAll(classAdvisors);
//                            }else {
//                                // Per target or per this.
//                                if (this.beanFactory.isSingleton(beanName)) {
//                                    throw new IllegalArgumentException("Bean with name '" + beanName +
//                                            "' is a singleton, but aspect instantiation model is not singleton");
//                                }
//                                MetadataAwareAspectInstanceFactory factory =
//                                        new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
//                                this.aspectFactoryCache.put(beanName, factory);
//                                advisors.addAll(this.advisorFactory.getAdvisors(factory));
//                            }
//                        }
//                    }
//                    this.aspectBeanNames = aspectNames;
//                    return advisors;
//                }
//            }
//        }
//
//        if (aspectNames.isEmpty()) {
//            return Collections.emptyList();
//        }
//        List<Advisor> advisors = new ArrayList<>();
//        for (String aspectName : aspectNames) {
//            List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
//            if (cachedAdvisors != null) {
//                advisors.addAll(cachedAdvisors);
//            }else {
//                MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
//                advisors.addAll(this.advisorFactory.getAdvisors(factory));
//            }
//        }
//        return advisors;
//    }
//
//
//
//    //BeanFactoryUtils:
//    //得到给定类型的所有bean名称,包括那些在祖先中定义的工厂。将返回唯一名称的覆盖bean定义
//    public static String[] beanNamesForTypeIncludingAncestors(
//            ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
//
//        Assert.notNull(lbf, "ListableBeanFactory must not be null");
//        String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
//        if (lbf instanceof HierarchicalBeanFactory) {
//            HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
//            if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
//                String[] parentResult = beanNamesForTypeIncludingAncestors(
//                        (ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
//                result = mergeNamesWithParent(result, parentResult, hbf);
//            }
//        }
//        return result;
//    }
//
//    //BeanFactoryUtils:
//    private static String[] mergeNamesWithParent(String[] result, String[] parentResult, HierarchicalBeanFactory hbf) {
//        if (parentResult.length == 0) {
//            return result;
//        }
//        List<String> merged = new ArrayList<>(result.length + parentResult.length);
//        merged.addAll(Arrays.asList(result));
//        for (String beanName : parentResult) {
//            if (!merged.contains(beanName) && !hbf.containsLocalBean(beanName)) {
//                merged.add(beanName);
//            }
//        }
//        return StringUtils.toStringArray(merged);
//    }
//
//
//
//    //ReflectiveAspectJAdvisorFactory:
//    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
//        //获取我们的标记为Aspect的类
//        Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
//        //获取我们的切面类的名称
//        String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
//        //校验我们的切面类
//        validate(aspectClass);
//
//        //使用包装模式构建MetadataAwareAspectInstanceFactory
//        MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
//                new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);
//
//        List<Advisor> advisors = new ArrayList<>();
//        //获取到切面类中的所有方法，但是该方法不会解析到标注了@PointCut注解的方法
//        for (Method method : getAdvisorMethods(aspectClass)) {
//            //循环解析我们切面中的方法
//            Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
//            if (advisor != null) {
//                advisors.add(advisor);
//            }
//        }
//
//        //如果它是一个目标切面，创建仿真的实例切面
//        if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
//            Advisor instantiationAdvisor = new ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
//            advisors.add(0, instantiationAdvisor);
//        }
//
//        //获取所有引用字段
//        for (Field field : aspectClass.getDeclaredFields()) {
//            Advisor advisor = getDeclareParentsAdvisor(field);
//            if (advisor != null) {
//                advisors.add(advisor);
//            }
//        }
//
//        return advisors;
//    }
//
//
//
//    //AbstractAspectJAdvisorFactory:
//    public void validate(Class<?> aspectClass) throws AopConfigException {
//        //如果父类有注解且不是Abstract, 就抛出异常
//        if (aspectClass.getSuperclass().getAnnotation(Aspect.class) != null &&
//                !Modifier.isAbstract(aspectClass.getSuperclass().getModifiers())) {
//            throw new AopConfigException("[" + aspectClass.getName() + "] cannot extend concrete aspect [" +
//                    aspectClass.getSuperclass().getName() + "]");
//        }
//
//        AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
//        if (!ajType.isAspect()) {
//            throw new NotAnAtAspectException(aspectClass);
//        }
//        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
//            throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
//                    "This is not supported in Spring AOP.");
//        }
//        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
//            throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
//                    "This is not supported in Spring AOP.");
//        }
//    }
//
//
//    //ReflectiveAspectJAdvisorFactory:
//    //获取切面上的通知方法，并按照规则排序
//    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
//        final List<Method> methods = new ArrayList<>();
//        ReflectionUtils.doWithMethods(aspectClass, method -> {
//            // Exclude pointcuts
//            if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
//                methods.add(method);
//            }
//        }, ReflectionUtils.USER_DECLARED_METHODS);
//        methods.sort(METHOD_COMPARATOR);
//        return methods;
//    }
//
//
//    private static final Comparator<Method> METHOD_COMPARATOR;
//
//    static {
//        Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
//                new InstanceComparator<>(
//                        Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
//                (Converter<Method, Annotation>) method -> {
//                    AbstractAspectJAdvisorFactory.AspectJAnnotation<?> annotation =
//                            AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
//                    return (annotation != null ? annotation.getAnnotation() : null);
//                });
//        Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
//        METHOD_COMPARATOR = adviceKindComparator.thenComparing(methodNameComparator);
//    }
//
//
//    //----------------
//
//    //ReflectiveAspectJAdvisorFactory:
//    public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
//                              int declarationOrderInAspect, String aspectName) {
//
//        validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());
//
//        //切面的方法上构建切点表达式
//        AspectJExpressionPointcut expressionPointcut = getPointcut(
//                candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
//        if (expressionPointcut == null) {
//            return null;
//        }
//        //实例化我们的切面通知对象
//        return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
//                this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
//    }
//
//
//    //ReflectiveAspectJAdvisorFactory:
//    private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
//        //获取给定方法上的第一个aspectJ注解
//        AspectJAnnotation<?> aspectJAnnotation =
//                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
//        if (aspectJAnnotation == null) {
//            return null;
//        }
//
//        //设置切点pointCut的表达式
//        AspectJExpressionPointcut ajexp =
//                new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
//        ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
//        if (this.beanFactory != null) {
//            ajexp.setBeanFactory(this.beanFactory);
//        }
//        return ajexp;
//    }
//
//
//
//    //AbstractAspectJAdvisorFactory:
//    protected static AspectJAnnotation<?> findAspectJAnnotationOnMethod(Method method) {
//        //Class<?>[] ASPECTJ_ANNOTATION_CLASSES = new Class<?>[] {
//        //			Pointcut.class, Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class}
//        for (Class<?> clazz : ASPECTJ_ANNOTATION_CLASSES) {
//            AspectJAnnotation<?> foundAnnotation = findAnnotation(method, (Class<Annotation>) clazz);
//            if (foundAnnotation != null) {
//                return foundAnnotation;
//            }
//        }
//        return null;
//    }
//
//    //AbstractAspectJAdvisorFactory:
//    private static <A extends Annotation> AspectJAnnotation<A> findAnnotation(Method method, Class<A> toLookFor) {
//        A result = AnnotationUtils.findAnnotation(method, toLookFor);
//        if (result != null) {
//            return new AspectJAnnotation<>(result);
//        }else {
//            return null;
//        }
//    }
//
//    //AnnotationUtils:
//    public static <A extends Annotation> A findAnnotation(Method method, @Nullable Class<A> annotationType) {
//        if (annotationType == null) {
//            return null;
//        }
//        // 快捷方式:直接出现在元素,不需要合并吗?
//        if (AnnotationFilter.PLAIN.matches(annotationType) ||
//                AnnotationsScanner.hasPlainJavaAnnotationsOnly(method)) {
//            return method.getDeclaredAnnotation(annotationType);
//        }
//        //详细的检索合并注释
//        return MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
//                .get(annotationType).withNonMergedAttributes()
//                .synthesize(MergedAnnotation::isPresent).orElse(null);
//    }
//
//
//
//
//    //InstantiationModelAwarePointcutAdvisorImpl:
//    public InstantiationModelAwarePointcutAdvisorImpl(AspectJExpressionPointcut declaredPointcut,
//                                                      Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
//                                                      MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
//
//        //当前的切点表达式
//        this.declaredPointcut = declaredPointcut;
//        //切面的class对象
//        this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
//        //切面方法的名称
//        this.methodName = aspectJAdviceMethod.getName();
//        //切面方法的参数类型
//        this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
//        //切面方法对象
//        this.aspectJAdviceMethod = aspectJAdviceMethod;
//        //aspectj的通知工厂
//        this.aspectJAdvisorFactory = aspectJAdvisorFactory;
//        //aspect的实例工厂
//        this.aspectInstanceFactory = aspectInstanceFactory;
//        //切面的顺序
//        this.declarationOrder = declarationOrder;
//        //切面的名称
//        this.aspectName = aspectName;
//
//        //判断当前的切面对象是否需要延时加载
//        if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
//            //pointcut的静态部分是一个lazy类型
//            org.springframework.aop.Pointcut preInstantiationPointcut = Pointcuts.union(
//                    aspectInstanceFactory.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);
//
//            // Make it dynamic: must mutate from pre-instantiation to post-instantiation state.
//            // If it's not a dynamic pointcut, it may be optimized out
//            // by the Spring AOP infrastructure after the first evaluation.
//
//            //使其动态，必须从pre-instantiation突变为post-instantiation状态，如果它不是一个动态的切点，
//            ///可能是优化的Spring AOP基本组件在第一次解析
//            this.pointcut = new PerTargetInstantiationModelPointcut(
//                    this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
//            this.lazy = true;
//        }else {
//            //一个单例切面
//            this.pointcut = this.declaredPointcut;
//            this.lazy = false;
//            //将切面中的通知构造为advice通知对象
//            this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
//        }
//    }
//
//
//
//    //InstantiationModelAwarePointcutAdvisorImpl:
//    private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {
//        Advice advice = this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pointcut,
//                this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
//        //Advice EMPTY_ADVICE = new Advice() {}
//        return (advice != null ? advice : EMPTY_ADVICE);
//    }
//
//
//    //ReflectiveAspectJAdvisorFactory:
//    public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
//                            MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
//        //获取我们的切面类的class对象
//        Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
//        validate(candidateAspectClass);
//
//        //获取切面方法上的注解
//        AspectJAnnotation<?> aspectJAnnotation =
//                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
//        //解析出来的注解信息是否为null
//        if (aspectJAnnotation == null) {
//            return null;
//        }
//
//        //再次判断是否是切面对象
//        if (!isAspect(candidateAspectClass)) {
//            throw new AopConfigException("Advice must be declared inside an aspect type: " +
//                    "Offending method '" + candidateAdviceMethod + "' in class [" +
//                    candidateAspectClass.getName() + "]");
//        }
//
//        if (logger.isDebugEnabled()) {
//            logger.debug("Found AspectJ method: " + candidateAdviceMethod);
//        }
//
//        AbstractAspectJAdvice springAdvice;
//
//        //判断标注在方法上的注解类型
//        switch (aspectJAnnotation.getAnnotationType()) {
//            //是PointCut注解 那么就抛出异常 因为在外面传递进来的方法已经排除了Pointcut的方法
//            case AtPointcut:
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
//                }
//                return null;
//            //环绕通知 构建AspectJAroundAdvice
//            case AtAround:
//                springAdvice = new AspectJAroundAdvice(
//                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
//                break;
//            //前置通知  构建AspectJMethodBeforeAdvice
//            case AtBefore:
//                springAdvice = new AspectJMethodBeforeAdvice(
//                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
//                break;
//            //后置通知 AspectJAfterAdvice
//            case AtAfter:
//                springAdvice = new AspectJAfterAdvice(
//                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
//                break;
//            //返回通知 AspectJAfterReturningAdvice
//            case AtAfterReturning:
//                springAdvice = new AspectJAfterReturningAdvice(
//                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
//                AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
//                if (StringUtils.hasText(afterReturningAnnotation.returning())) {
//                    springAdvice.setReturningName(afterReturningAnnotation.returning());
//                }
//                break;
//            //异常通知   AspectJAfterThrowingAdvice
//            case AtAfterThrowing:
//                springAdvice = new AspectJAfterThrowingAdvice(
//                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
//                AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
//                if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
//                    springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
//                }
//                break;
//            default:
//                throw new UnsupportedOperationException(
//                        "Unsupported advice type on method: " + candidateAdviceMethod);
//        }
//
//        //设置我们构建出来的通知对象的相关属性比如DeclarationOrder，在代理调用的时候，责任链顺序上会用到
//        springAdvice.setAspectName(aspectName);
//        springAdvice.setDeclarationOrder(declarationOrder);
//        String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
//        if (argNames != null) {
//            springAdvice.setArgumentNamesFromStringArray(argNames);
//        }
//        springAdvice.calculateArgumentBindings();
//
//        return springAdvice;
//    }
//
//
//    //ReflectiveAspectJAdvisorFactory:
//    //获取定义的父通知advisor,用给定引用字段构建一个 {org.springframework.aop.aspectj.DeclareParentsAdvisor}
//    private Advisor getDeclareParentsAdvisor(Field introductionField) {
//        DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
//        if (declareParents == null) {
//            //不是一个引用字段
//            return null;
//        }
//
//        if (DeclareParents.class == declareParents.defaultImpl()) {
//            throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
//        }
//
//        return new DeclareParentsAdvisor(
//                introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
//    }
//
//
//
//
//
//    //AbstractAdvisorAutoProxyCreator:
//    protected List<Advisor> findAdvisorsThatCanApply(
//            List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {
//
//        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
//        try {
//            //从候选的通知器中找到合适正在创建的实例对象的通知器
//            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
//        }finally {
//            ProxyCreationContext.setCurrentProxiedBeanName(null);
//        }
//    }
//
//
//    //AopUtils:
//    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
//        //若候选的通知集合为空 直接返回
//        if (candidateAdvisors.isEmpty()) {
//            return candidateAdvisors;
//        }
//        //定义一个合适的通知集合对象
//        List<Advisor> eligibleAdvisors = new ArrayList<>();
//        //循环我们候选的通知对象
//        for (Advisor candidate : candidateAdvisors) {
//            //判断我们的通知对象是不是实现了IntroductionAdvisor(很明显我们事务的没有实现 所以不会走下面的逻辑)
//            if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
//                eligibleAdvisors.add(candidate);
//            }
//        }
//        //不为空
//        boolean hasIntroductions = !eligibleAdvisors.isEmpty();
//        for (Advisor candidate : candidateAdvisors) {
//            //判断我们的增强器对象是不是实现了IntroductionAdvisor(很明显我们事务的没有实现,所以不会走下面的逻辑)
//            if (candidate instanceof IntroductionAdvisor) {
//                //在上面已经处理过 ，不需要处理
//                continue;
//            }
//            //1>真正的判断通知advisor是否合适当前类型
//            if (canApply(candidate, clazz, hasIntroductions)) {
//                eligibleAdvisors.add(candidate);
//            }
//        }
//        return eligibleAdvisors;
//    }
//
//
//    //AopProxyUtils:
//    //确定给定bean实例的最终目标类，不仅要遍历顶级代理，还要遍历任意数量的嵌套代理,只针对单例目标
//    public static Class<?> ultimateTargetClass(Object candidate) {
//        Assert.notNull(candidate, "Candidate object must not be null");
//        Object current = candidate;
//        Class<?> result = null;
//        while (current instanceof TargetClassAware) {
//            result = ((TargetClassAware) current).getTargetClass();
//            current = getSingletonTarget(current);
//        }
//        if (result == null) {
//            result = (AopUtils.isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
//        }
//        return result;
//    }
//
//    //AopProxyUtils:
//    public static Object getSingletonTarget(Object candidate) {
//        if (candidate instanceof Advised) {
//            TargetSource targetSource = ((Advised) candidate).getTargetSource();
//            if (targetSource instanceof SingletonTargetSource) {
//                return ((SingletonTargetSource) targetSource).getTarget();
//            }
//        }
//        return null;
//    }
//
//    //AopUtils:
//    //通过反射调用给定的目标，作为AOP方法调用的一部分
//    public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args)
//            throws Throwable {
//        //使用反射调用方法
//        try {
//            ReflectionUtils.makeAccessible(method);
//            return method.invoke(target, args);
//        }catch (InvocationTargetException ex) {
//            throw ex.getTargetException();
//        }catch (IllegalArgumentException ex) {
//            throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" +
//                    method + "] on target [" + target + "]", ex);
//        }catch (IllegalAccessException ex) {
//            throw new AopInvocationException("Could not access method [" + method + "]", ex);
//        }
//    }
//
//    //AopContext:
//    static Object setCurrentProxy(@Nullable Object proxy) {
//        //ThreadLocal<Object> currentProxy=new NamedThreadLocal<>("Current AOP proxy")
//        Object old = currentProxy.get();
//        if (proxy != null) {
//            currentProxy.set(proxy);
//        }else {
//            currentProxy.remove();
//        }
//        return old;
//    }
//
//    //AdvisedSupport:
//    //基于这种配置,确定给定方法的{org.aopalliance.intercept.MethodInterceptor} 拦截器列表对象
//    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(Method method, @Nullable Class<?> targetClass) {
//        MethodCacheKey cacheKey = new MethodCacheKey(method);
//        // Map<MethodCacheKey, List<Object>> methodCache, 以Method为键，advisor链表为值进行缓存
//        List<Object> cached = this.methodCache.get(cacheKey);
//        if (cached == null) {
//            cached = this.advisorChainFactory.getInterceptorsAndDynamicInterceptionAdvice(
//                    this, method, targetClass);
//            this.methodCache.put(cacheKey, cached);
//        }
//        return cached;
//    }
//
//    //DefaultAdvisorChainFactory:
//    //给出一个{advise}对象，用于计算方法的advisor链。总是重建每个advisor链;缓存可以由子类提供
//    public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
//            Advised config, Method method, @Nullable Class<?> targetClass) {
//
//        //首先处理介绍introductions，需要在最终的列表中保持顺序
//        AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
//        Advisor[] advisors = config.getAdvisors();
//        List<Object> interceptorList = new ArrayList<>(advisors.length);
//        Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
//        Boolean hasIntroductions = null;
//
//        for (Advisor advisor : advisors) {
//            if (advisor instanceof PointcutAdvisor) {
//                //将它添加条件
//                PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
//                if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
//                    MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
//                    boolean match;
//                    if (mm instanceof IntroductionAwareMethodMatcher) {
//                        if (hasIntroductions == null) {
//                            hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
//                        }
//                        match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
//                    }else {
//                        match = mm.matches(method, actualClass);
//                    }
//                    if (match) {
//                        MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
//                        if (mm.isRuntime()) {
//                            //在getInterceptors()方法中创建一个新的对象实例不是问题，因为我们通常会缓存创建的链
//                            for (MethodInterceptor interceptor : interceptors) {
//                                interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
//                            }
//                        }else {
//                            interceptorList.addAll(Arrays.asList(interceptors));
//                        }
//                    }
//                }
//            }else if (advisor instanceof IntroductionAdvisor) {
//                IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
//                if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
//                    Interceptor[] interceptors = registry.getInterceptors(advisor);
//                    interceptorList.addAll(Arrays.asList(interceptors));
//                }
//            }else {
//                Interceptor[] interceptors = registry.getInterceptors(advisor);
//                interceptorList.addAll(Arrays.asList(interceptors));
//            }
//        }
//
//        return interceptorList;
//    }
//
//    //AopProxyUtils:
//    //如果给定的vararg参数数组与方法中声明的vararg参数的数组类型不匹配时, 将给定的参数调整为给定方法中的目标签名
//    static Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
//        if (ObjectUtils.isEmpty(arguments)) {
//            return new Object[0];
//        }
//        if (method.isVarArgs()) {
//            Class<?>[] paramTypes = method.getParameterTypes();
//            if (paramTypes.length == arguments.length) {
//                int varargIndex = paramTypes.length - 1;
//                Class<?> varargType = paramTypes[varargIndex];
//                if (varargType.isArray()) {
//                    Object varargArray = arguments[varargIndex];
//                    if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
//                        Object[] newArguments = new Object[arguments.length];
//                        System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
//                        Class<?> targetElementType = varargType.getComponentType();
//                        int varargLength = Array.getLength(varargArray);
//                        Object newVarargArray = Array.newInstance(targetElementType, varargLength);
//                        System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
//                        newArguments[varargIndex] = newVarargArray;
//                        return newArguments;
//                    }
//                }
//            }
//        }
//        return arguments;
//    }
//
//    //ReflectiveMethodInvocation:
//    public Object proceed() throws Throwable {
//        //从-1开始,下标=拦截器的长度-1的条件满足表示执行到了最后一个拦截器的时候，此时执行目标方法
//        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
//            return invokeJoinpoint();
//        }
//        //获取第一个方法拦截器使用的是前++
//        Object interceptorOrInterceptionAdvice =
//                this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
//        if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
//            //评估动态方法匹配器这里:静态部分将已经被评估，并找到匹配
//            InterceptorAndDynamicMethodMatcher dm =
//                    (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
//            Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
//            if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
//                return dm.interceptor.invoke(this);
//            }else {
//                //动态匹配失败。跳过这个拦截器并调用链中的下一个
//                return proceed();
//            }
//        }else {
//            //它是一个拦截器，所以我们只是调用它:在构造这个对象之前，切入点pointcut将被静态地评估
//            //责任链模式，执行顺序如下:
//            /**
//             * 1.around start
//             * 2.before
//             * 3.business running
//             * 4.around end
//             * 5.after
//             * 6.afterReturning
//             */
//            return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
//        }
//    }
//
//    //ExposeInvocationInterceptor:[1]
//    public Object invoke(MethodInvocation mi) throws Throwable {
//        MethodInvocation oldInvocation = invocation.get();
//        invocation.set(mi);
//        try {
//            return mi.proceed();
//        }finally {
//            invocation.set(oldInvocation);
//        }
//    }
//
//    //AspectJAfterThrowingAdvice:[2]
//    public Object invoke(MethodInvocation mi) throws Throwable {
//        try {
//            //执行下一个通知/拦截器
//            return mi.proceed();
//        }catch (Throwable ex) {
//            //抛出异常
//            if (shouldInvokeOnThrowing(ex)) {
//                //执行异常通知
//                invokeAdviceMethod(getJoinPointMatch(), null, ex);
//            }
//            throw ex;
//        }
//    }
//
//
//    //AfterReturningAdviceInterceptor:[3]
//    public Object invoke(MethodInvocation mi) throws Throwable {
//        //执行下一个通知/拦截器
//        Object retVal = mi.proceed();
//        //返回通知方法
//        this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
//        return retVal;
//    }
//
//    //AspectJAfterAdvice:[4]
//    public Object invoke(MethodInvocation mi) throws Throwable {
//        try {
//            //执行下一个通知/拦截器
//            return mi.proceed();
//        }finally {
//            //后置通知的方法总是会被执行 原因就在这finally
//            invokeAdviceMethod(getJoinPointMatch(), null, null);
//        }
//    }
//
//    //AspectJAroundAdvice:[5]
//    public Object invoke(MethodInvocation mi) throws Throwable {
//        if (!(mi instanceof ProxyMethodInvocation)) {
//            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
//        }
//        ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
//        //返回当前调用的ProceedingJoinPoint，如果还没有绑定到线程，则延迟实例化它
//        ProceedingJoinPoint pjp = lazyGetProceedingJoinPoint(pmi);
//        //获取匹配的连接点
//        JoinPointMatch jpm = getJoinPointMatch(pmi);
//        return invokeAdviceMethod(pjp, jpm, null, null);
//    }
//
//    //AbstractAspectJAdvice:
//    protected Object invokeAdviceMethod(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
//                                        @Nullable Object returnValue, @Nullable Throwable t) throws Throwable {
//        return invokeAdviceMethodWithGivenArgs(argBinding(jp, jpMatch, returnValue, t));
//    }
//
//    //AbstractAspectJAdvice:
//    protected Object invokeAdviceMethodWithGivenArgs(Object[] args) throws Throwable {
//        Object[] actualArgs = args;
//        if (this.aspectJAdviceMethod.getParameterCount() == 0) {
//            actualArgs = null;
//        }
//        try {
//            ReflectionUtils.makeAccessible(this.aspectJAdviceMethod);
//            // TODO AopUtils.invokeJoinpointUsingReflection
//            //this.aspectJAdviceMethod ==> Method aspectJAdviceMethod
//            return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance(), actualArgs);
//        }catch (IllegalArgumentException ex) {
//            throw new AopInvocationException("Mismatch on arguments to advice method [" +
//                    this.aspectJAdviceMethod + "]; pointcut expression [" +
//                    this.pointcut.getPointcutExpression() + "]", ex);
//        }catch (InvocationTargetException ex) {
//            throw ex.getTargetException();
//        }
//    }
//
//    //MethodBeforeAdviceInterceptor:[6]
//    public Object invoke(MethodInvocation mi) throws Throwable {
//        //执行前置通知的方法
//        this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis());
//        //执行下一个通知/拦截器，但是该拦截器是最后一个了，所以会调用目标方法
//        return mi.proceed();
//    }
//
//
//    //org.springframework.cglib.proxy.MethodProxy:
//    //在相同类型的不同对象上调用原始方法
//    public Object invoke(Object obj, Object[] args) throws Throwable {
//        try {
//            init();
//            MethodProxy.FastClassInfo fci = fastClassInfo;
//            return fci.f1.invoke(fci.i1, obj, args);
//        }catch (InvocationTargetException ex) {
//            throw ex.getTargetException();
//        }catch (IllegalArgumentException ex) {
//            if (fastClassInfo.i1 < 0)
//                throw new IllegalArgumentException("Protected method: " + sig1);
//            throw ex;
//        }
//    }
//
//
//
//
//
//
//


    public interface TransactionDefinition {

        //支持当前事物，若当前没有事物就创建一个事物
        int PROPAGATION_REQUIRED = 0;

        //如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务的方式运行
        int PROPAGATION_SUPPORTS = 1;

        //如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常
        int PROPAGATION_MANDATORY = 2;

        //创建一个新的事务，如果当前存在事务，则把当前事务挂起
        int PROPAGATION_REQUIRES_NEW = 3;

        //以非事务方式运行，如果当前存在事务，则把当前事务挂起
        int PROPAGATION_NOT_SUPPORTED = 4;

        //以非事务方式运行，如果当前存在事务，则抛出异常
        int PROPAGATION_NEVER = 5;

        //如果外层存在事务，就以嵌套事务运行，被嵌套的事务可以独立于外层事务进行提交或者回滚(保存点)，
        //如果外层不存在事务,行为跟PROPAGATION_REQUIRES_NEW
        int PROPAGATION_NESTED = 6;

        //使用数据库默认的隔离级别
        int ISOLATION_DEFAULT = -1;

        //读未提交
        int ISOLATION_READ_UNCOMMITTED = 1;

        //读已提交
        int ISOLATION_READ_COMMITTED = 2;

        //可重复读
        int ISOLATION_REPEATABLE_READ = 4;

        //可串行化
        int ISOLATION_SERIALIZABLE = 8;

        //使用默认的超时时间, 如果不支持超时，则不支持.
        int TIMEOUT_DEFAULT = -1;

        //获取事物的传播行为,默认是“支持当前事物，若当前没有事物就创建一个事物”
        default int getPropagationBehavior() {
            return PROPAGATION_REQUIRED;
        }

        //获取事物的隔离级别, 默认是使用数据库的隔离级别，
        //mysql默认是可重复读，oracle/sql server默认是读已提交
        default int getIsolationLevel() {
            return ISOLATION_DEFAULT;
        }

        //获取事物的超时时间
        default int getTimeout() {
            return TIMEOUT_DEFAULT;
        }

        //是否为只读事物
        default boolean isReadOnly() {
            return false;
        }

        //获取当前事物的名称
        default String getName() {
            return null;
        }

        //返回一个默认的不可修改的{TransactionDefinition}。出于定制的目的，使用可修改的
        static org.springframework.transaction.TransactionDefinition withDefaults() {
            return StaticTransactionDefinition.INSTANCE;
        }
    }


    public interface TransactionStatus extends TransactionExecution,SavepointManager,Flushable {
        //是否为新事务
        boolean isNewTransaction();

        //是否有保存点
        boolean hasSavepoint();

        //设置为只回滚
        void setRollbackOnly();

        //是否为只回滚
        boolean isRollbackOnly();

        //将会话刷新到数据库中
        void flush();

        //当前事务是否已经完成
        boolean isCompleted();

        //创建保存点
        Object createSavepoint() throws TransactionException;

        //回滚到保存点
        void rollbackToSavepoint(Object savepoint) throws TransactionException;

        //释放保存点
        void releaseSavepoint(Object savepoint) throws TransactionException;
    }



    //AbstractAutoProxyCreator:
    //实现了InstantiationAwareBeanPostProcessor接口，该接口有2个方法postProcessBeforeInstantiation
    //和postProcessAfterInstantiation，其中实例化之前会执行postProcessBeforeInstantiation方法
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        //构建我们的缓存key
        Object cacheKey = getCacheKey(beanClass, beanName);

        if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
            //如果被解析过直接返回
            if (this.advisedBeans.containsKey(cacheKey)) {
                return null;
            }
            /**
             * 判断是不是基础的bean
             * 判断是不是应该跳过 (此处Spring Aop解析直接解析出我们的切面信息(并且把我们的切面信息进行缓存)，
             * 而事务在这里是不会解析的，为什么？原因事务的话已经把事务拦截器通过@Bean得到，而Aop的需要寻找)
             */
            if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
                this.advisedBeans.put(cacheKey, Boolean.FALSE);
                return null;
            }
        }

        //如果我们有一个自定义的TargetSource，在这里创建代理。
        //防止不必要的目标bean的默认实例化:TargetSource将以自定义方式处理目标实例
        TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
        if (targetSource != null) {
            if (StringUtils.hasLength(beanName)) {
                this.targetSourcedBeans.add(beanName);
            }
            Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
            //创建代理对象
            Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
            this.proxyTypes.put(cacheKey, proxy.getClass());
            return proxy;
        }
        return null;
    }


    //AbstractAutoProxyCreator:
    //实现了BeanPostProcessor接口, 该接口有2个方法postProcessBeforeInitialization
    //和postProcessAfterInitialization，其中组件初始化之后会执行
    //postProcessAfterInitialization(该方法创建"Aop/事务"的代理对象)
    public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
        if (bean != null) {
            //获取缓存key
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (this.earlyProxyReferences.remove(cacheKey) != bean) {
                //如果有必要就代理
                return wrapIfNecessary(bean, beanName, cacheKey);
            }
        }
        return bean;
    }


    //AbstractAutoProxyCreator:
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        //已经被处理过
        if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        }
        //不需要增强的
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        }
        //是不是基础的bean, 是不是需要跳过的
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }

        //如果有匹配的通知,就创建代理对象
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        //如果不为空，表述需要代理
        if (specificInterceptors != DO_NOT_PROXY) {
            //设置当前的对象已处理
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            //创建我们的真正的代理对象
            Object proxy = createProxy(
                    bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
            //加入到缓存
            this.proxyTypes.put(cacheKey, proxy.getClass());
            return proxy;
        }

        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        return bean;
    }

    //AbstractAdvisorAutoProxyCreator:
    protected Object[] getAdvicesAndAdvisorsForBean(
            Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
        //找合适的增强器对象
        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
        //若合适的通知器为空
        if (advisors.isEmpty()) {
            //Object[] DO_NOT_PROXY = null
            return DO_NOT_PROXY;
        }
        return advisors.toArray();
    }

    //AbstractAdvisorAutoProxyCreator:
    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        //找到Spring IoC容器中所有的候选通知,包括Aop的和事务的
        List<Advisor> candidateAdvisors = findCandidateAdvisors();
        //判断找到的通知能不能作用到当前的类上
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        //可以使用继承的子类扩展额外的通知advisor
        extendAdvisors(eligibleAdvisors);
        //对我们的advisor进行排序
        if (!eligibleAdvisors.isEmpty()) {
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }

    //AbstractAdvisorAutoProxyCreator:
    protected List<Advisor> findCandidateAdvisors() {
        Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
        return this.advisorRetrievalHelper.findAdvisorBeans();
    }

    //BeanFactoryAdvisorRetrievalHelper:
    public List<Advisor> findAdvisorBeans() {
        //确定advisor bean的名称列表(如果还没有缓存的话)。
        String[] advisorNames = this.cachedAdvisorBeanNames;
        if (advisorNames == null) {
            /**
             * 去容器中获取到实现了Advisor接口的实现类 我们的事务注解@EnableTransactionManagement导入了一个叫
             * ProxyTransactionManagementConfiguration配置类。
             * 而在这个配置类中配置了：
             * @Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)
             * @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
             * public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor();
             * 然后把他的名字获取出来保存到 本类的属性变量cachedAdvisorBeanNames中
             */
            advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    this.beanFactory, Advisor.class, true, false);
            this.cachedAdvisorBeanNames = advisorNames;
        }
        //若在容器中没有找到，直接返回一个空的集合
        if (advisorNames.length == 0) {
            return new ArrayList<>();
        }

        List<Advisor> advisors = new ArrayList<>();
        //容器中找到了我们事务配置的BeanFactoryTransactionAttributeSourceAdvisor
        for (String name : advisorNames) {
            //判断他是不是一个合适的, 如果是合适的就保存在advisors中并返回
            if (isEligibleBean(name)) {
                //BeanFactoryTransactionAttributeSourceAdvisor是不是正在创建的Bean
                if (this.beanFactory.isCurrentlyInCreation(name)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Skipping currently created advisor '" + name + "'");
                    }
                }else { //不是的话
                    try {
                        //显示的调用getBean方法方法创建我们的BeanFactoryTransactionAttributeSourceAdvisor返回去
                        advisors.add(this.beanFactory.getBean(name, Advisor.class));
                    }catch (BeanCreationException ex) {
                        Throwable rootCause = ex.getMostSpecificCause();
                        if (rootCause instanceof BeanCurrentlyInCreationException) {
                            BeanCreationException bce = (BeanCreationException) rootCause;
                            String bceBeanName = bce.getBeanName();
                            if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {
                                if (logger.isTraceEnabled()) {
                                    logger.trace("Skipping advisor '" + name +
                                            "' with dependency on currently created bean: " + ex.getMessage());
                                }
                                continue;
                            }
                        }
                        throw ex;
                    }
                }
            }
        }
        return advisors;
    }

    //AbstractAdvisorAutoProxyCreator.BeanFactoryAdvisorRetrievalHelperAdapter:
    protected boolean isEligibleBean(String beanName) {
        return AbstractAdvisorAutoProxyCreator.this.isEligibleAdvisorBean(beanName);
    }

    //InfrastructureAdvisorAutoProxyCreator:
    protected boolean isEligibleAdvisorBean(String beanName) {
        //容器中包含了这个Bean定义，并且Bean定义的角色为BeanDefinition.ROLE_INFRASTRUCTURE
        return (this.beanFactory != null && this.beanFactory.containsBeanDefinition(beanName) &&
                this.beanFactory.getBeanDefinition(beanName).getRole() == BeanDefinition.ROLE_INFRASTRUCTURE);
    }

    //AbstractAdvisorAutoProxyCreator:
    protected List<Advisor> findAdvisorsThatCanApply(
            List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
        try {
            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        }finally {
            ProxyCreationContext.setCurrentProxiedBeanName(null);
        }
    }

    //AopUtils:
    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
        //若候选的增强器集合为空直接返回
        if (candidateAdvisors.isEmpty()) {
            return candidateAdvisors;
        }
        //定义一个合适的增强器集合对象
        List<Advisor> eligibleAdvisors = new ArrayList<>();
        //循环我们候选的增强器对象
        for (Advisor candidate : candidateAdvisors) {
            //判断我们的增强器对象是不是实现了IntroductionAdvisor (我们事务的没有实现,所以不会走下面的逻辑)
            if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
                eligibleAdvisors.add(candidate);
            }
        }
        //不为空
        boolean hasIntroductions = !eligibleAdvisors.isEmpty();
        for (Advisor candidate : candidateAdvisors) {
            //判断我们的增强器对象是不是实现了IntroductionAdvisor (我们事务的没有实现,所以不会走下面的逻辑)
            if (candidate instanceof IntroductionAdvisor) {
                //在上面已经处理过，不需要处理
                continue;
            }
            //真正的判断增强器是否合适当前类
            if (canApply(candidate, clazz, hasIntroductions)) {
                eligibleAdvisors.add(candidate);
            }
        }
        return eligibleAdvisors;
    }

    //AopUtils:
    public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
        //判断我们的增强器 IntroductionAdvisor
        if (advisor instanceof IntroductionAdvisor) {
            return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
        }
        //判断我们事务的增强器BeanFactoryTransactionAttributeSourceAdvisor是否实现了PointcutAdvisor
        else if (advisor instanceof PointcutAdvisor) {
            //转为PointcutAdvisor类型
            PointcutAdvisor pca = (PointcutAdvisor) advisor;
            //找到真正能用的增强器
            return canApply(pca.getPointcut(), targetClass, hasIntroductions);
        }else {
            // It doesn't have a pointcut so we assume it applies.
            return true;
        }
    }

    //AopUtils:
    public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
        Assert.notNull(pc, "Pointcut must not be null");
        if (!pc.getClassFilter().matches(targetClass)) {
            return false;
        }

        //通过切点获取到一个方法匹配器对象
        MethodMatcher methodMatcher = pc.getMethodMatcher();
        if (methodMatcher == MethodMatcher.TRUE) {
            // No need to iterate the methods if we're matching any method anyway...
            return true;
        }

        //判断匹配器是不是IntroductionAwareMethodMatcher
        IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
        if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
            introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
        }

        //创建一个集合用于保存targetClass的class对象
        Set<Class<?>> classes = new LinkedHashSet<>();
        //判断当前class是不是代理的class对象
        if (!Proxy.isProxyClass(targetClass)) {
            //加入到集合中去
            classes.add(ClassUtils.getUserClass(targetClass));
        }
        //获取到targetClass所实现的接口的class对象，然后加入到集合中
        classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));

        //循环所有的class对象
        for (Class<?> clazz : classes) {
            //通过class获取到所有的方法
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            for (Method method : methods) {
                //通过methodMatcher.matches来匹配我们的方法
                if (introductionAwareMethodMatcher != null ?
                        introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
                        //1>通过方法匹配器进行匹配
                        methodMatcher.matches(method, targetClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    //TransactionAttributeSourcePointcut:
    public boolean matches(Method method, Class<?> targetClass) {
        /**
         * 获取我们@EnableTransactionManagement注解为我们容器中导入的
         * ProxyTransactionManagementConfiguration配置类中的TransactionAttributeSource对象
         */
        TransactionAttributeSource tas = getTransactionAttributeSource();
        //若事务属性原为null或者 解析出来的事务注解属性不为空,表示方法匹配
        return (tas == null || tas.getTransactionAttribute(method, targetClass) != null);
    }

    //AbstractFallbackTransactionAttributeSource:
    public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        //判断method所在的class 是不是Object类型
        if (method.getDeclaringClass() == Object.class) {
            return null;
        }

        //构建我们的缓存key
        Object cacheKey = getCacheKey(method, targetClass);
        //先去我们的缓存中获取
        //Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024)
        TransactionAttribute cached = this.attributeCache.get(cacheKey);
        //缓存中不为空
        if (cached != null) {
            //判断缓存中的对象是不是空事务属性的对象
            if (cached == NULL_TRANSACTION_ATTRIBUTE) {
                return null;
            }else {
                return cached;
            }
        }else {
            //1>查找我们的事务注解
            TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
            //若解析出来的事务注解属性为空
            if (txAttr == null) {
                //往缓存中存放空事务注解属性
                this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
            }else {
                //我们执行方法的描述符 包名+类名+方法名
                String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
                //把方法描述设置到事务属性上去
                if (txAttr instanceof DefaultTransactionAttribute) {
                    ((DefaultTransactionAttribute) txAttr).setDescriptor(methodIdentification);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
                }
                //加入到缓存
                this.attributeCache.put(cacheKey, txAttr);
            }
            return txAttr;
        }
    }

    //AbstractFallbackTransactionAttributeSource:
    protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        //判断我们的事务方法上的修饰符是不是public的
        if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
            return null;
        }

        //获取指定方法，方法可能在接口上，但是我们需要目标类的属性。如果目标类为空，则该方法将保持不变
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);

        //第一：先去目标对象的方法上去找我们的事务注解
        TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
        if (txAttr != null) {
            return txAttr;
        }

        //第二：去目标对象上找事务注解
        txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
        if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
            return txAttr;
        }

        if (specificMethod != method) {
            //第三：去我们的实现类的接口上的方法去找事务注解
            txAttr = findTransactionAttribute(method);
            if (txAttr != null) {
                return txAttr;
            }
            //第四：去我们的实现类的接口上去找事务注解
            txAttr = findTransactionAttribute(method.getDeclaringClass());
            if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
                return txAttr;
            }
        }
        return null;
    }

    //AnnotationTransactionAttributeSource:
    protected TransactionAttribute findTransactionAttribute(Method method) {
        return determineTransactionAttribute(method);
    }

    //AnnotationTransactionAttributeSource:
    protected TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
        //获取我们的注解解析器
        for (TransactionAnnotationParser parser : this.annotationParsers) {
            //1>通过注解解析器去解析我们的元素(方法或者类)上的注解
            TransactionAttribute attr = parser.parseTransactionAnnotation(element);
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

    //SpringTransactionAnnotationParser:
    public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
        //从element对象中获取@Transactional注解 然后把注解属性封装到了AnnotationAttributes
        AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
                element, Transactional.class, false, false);
        if (attributes != null) {
            //1>解析出真正的事务属性对象
            return parseTransactionAnnotation(attributes);
        }else {
            return null;
        }
    }

    //SpringTransactionAnnotationParser:
    protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
        //创建一个基础规则的事务属性对象
        RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();

        //解析@Transactionl上的传播行为
        Propagation propagation = attributes.getEnum("propagation");
        rbta.setPropagationBehavior(propagation.value());

        //解析@Transactionl上的隔离级别
        Isolation isolation = attributes.getEnum("isolation");
        rbta.setIsolationLevel(isolation.value());

        //解析@Transactionl上的事务超时事件
        rbta.setTimeout(attributes.getNumber("timeout").intValue());
        rbta.setReadOnly(attributes.getBoolean("readOnly"));

        //解析@Transactionl上的事务管理器的名称
        rbta.setQualifier(attributes.getString("value"));

        //解析针对哪种异常回滚
        List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
        for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {
            rollbackRules.add(new RollbackRuleAttribute(rbRule));
        }
        //对哪种异常进行回滚
        for (String rbRule : attributes.getStringArray("rollbackForClassName")) {
            rollbackRules.add(new RollbackRuleAttribute(rbRule));
        }
        //对哪种异常不回滚
        for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {
            rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
        }
        //对哪种类型不回滚
        for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {
            rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
        }
        rbta.setRollbackRules(rollbackRules);

        return rbta;
    }


    /**
     * =================================================================
     * Spring事务代理调用过程
     * 调用执行方法，实际上是调用 org.springframework.aop.framework.JdkDynamicAopProxy#invoke
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object oldProxy = null;
        boolean setProxyContext = false;

        //获取到我们的目标对象
        TargetSource targetSource = this.advised.targetSource;
        Object target = null;

        try {
            //若是equals方法不需要代理
            if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
                // The target does not implement the equals(Object) method itself.
                return equals(args[0]);
            }
            //若是hashCode方法不需要代理
            else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
                // The target does not implement the hashCode() method itself.
                return hashCode();
            }
            //若是DecoratingProxy也不要拦截器执行
            else if (method.getDeclaringClass() == DecoratingProxy.class) {
                // There is only getDecoratedClass() declared -> dispatch to proxy config.
                return AopProxyUtils.ultimateTargetClass(this.advised);
            }else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
                    method.getDeclaringClass().isAssignableFrom(Advised.class)) {
                // Service invocations on ProxyConfig with the proxy config...
                return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
            }

            Object retVal;
            /**
             * 这个配置是暴露我们的代理对象到线程变量中，需要搭配@EnableAspectJAutoProxy(exposeProxy = true)一起使用
             * 比如在目标对象方法中再次获取代理对象可以使用这个AopContext.currentProxy()
             * 还有的就是事务方法调用事务方法的时候也是用到这个
             */
            if (this.advised.exposeProxy) {
                //把我们的代理对象暴露到线程变量中
                oldProxy = AopContext.setCurrentProxy(proxy);
                setProxyContext = true;
            }

            //获取我们的目标对象
            target = targetSource.getTarget();
            //获取我们目标对象的class
            Class<?> targetClass = (target != null ? target.getClass() : null);

            //把aop的advisor转化为拦截器链
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

            //如果拦截器链为空
            if (chain.isEmpty()) {
                //通过反射直接调用执行
                Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
                retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
            }else {
                //创建一个方法调用对象
                MethodInvocation invocation =
                        new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
                //1>调用执行
                retVal = invocation.proceed();
            }

            // Massage return value if necessary.
            Class<?> returnType = method.getReturnType();
            if (retVal != null && retVal == target &&
                    returnType != Object.class && returnType.isInstance(proxy) &&
                    !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
                retVal = proxy;
            }else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
                throw new AopInvocationException(
                        "Null return value from advice does not match primitive return type for: " + method);
            }
            return retVal;
        }finally {
            if (target != null && !targetSource.isStatic()) {
                // Must have come from TargetSource.
                targetSource.releaseTarget(target);
            }
            if (setProxyContext) {
                // Restore old proxy.
                AopContext.setCurrentProxy(oldProxy);
            }
        }
    }

    //ReflectiveMethodInvocation:
    public Object proceed() throws Throwable {
        //从-1开始,下标=拦截器的长度-1的条件满足表示执行到了最后一个拦截器的时候，此时执行目标方法
        if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
            return invokeJoinpoint();
        }

        //获取第一个方法拦截器使用的是前++
        Object interceptorOrInterceptionAdvice =
                this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
        if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
            InterceptorAndDynamicMethodMatcher dm =
                    (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
            Class<?> targetClass = (this.targetClass != null ? this.targetClass : this.method.getDeclaringClass());
            if (dm.methodMatcher.matches(this.method, targetClass, this.arguments)) {
                return dm.interceptor.invoke(this);
            }else {
                //动态匹配失败。跳过这个拦截器并调用链中的下一个
                return proceed();
            }
        }else {
            //真正的开始调用，它是一个拦截器，所以我们只是调用它:在构造这个对象之前，切入点将被静态地评估。
            return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
        }
    }

    //TransactionInterceptor:
    public Object invoke(MethodInvocation invocation) throws Throwable {
        //获取我们的代理对象的class属性
        Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);

        //1>以事务的方式调用目标方法, 在这埋了一个钩子函数, 用来回调目标方法的
        return invokeWithinTransaction(invocation.getMethod(), targetClass, invocation::proceed);
    }


    //TransactionAspectSupport:
    //用于around-advice-based子类的委托，委托给该类上的其他几个模板方法
    protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,
                                             final TransactionAspectSupport.InvocationCallback invocation) throws Throwable {

        //获取我们的事务属源对象
        TransactionAttributeSource tas = getTransactionAttributeSource();
        //通过事务属性源对象获取到我们的事务属性信息
        final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);
        final TransactionManager tm = determineTransactionManager(txAttr);

        //如果使用的是响应式的事务管理
        if (this.reactiveAdapterRegistry != null && tm instanceof ReactiveTransactionManager) {
            ReactiveTransactionSupport txSupport = this.transactionSupportCache.computeIfAbsent(method, key -> {
                if (KotlinDetector.isKotlinType(method.getDeclaringClass()) && KotlinDelegate.isSuspend(method)) {
                    throw new TransactionUsageException(
                            "Unsupported annotated transaction on suspending function detected: " + method +
                                    ". Use TransactionalOperator.transactional extensions instead.");
                }
                ReactiveAdapter adapter = this.reactiveAdapterRegistry.getAdapter(method.getReturnType());
                if (adapter == null) {
                    throw new IllegalStateException("Cannot apply reactive transaction to non-reactive return type: " +
                            method.getReturnType());
                }
                return new ReactiveTransactionSupport(adapter);
            });
            return txSupport.invokeWithinTransaction(
                    method, targetClass, invocation, txAttr, (ReactiveTransactionManager) tm);
        }

        //获取我们配置的事务管理器对象
        PlatformTransactionManager ptm = asPlatformTransactionManager(tm);
        //从tx属性对象中获取出标注了@Transactionl的方法描述符
        final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);

        //处理声明式事务
        if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager)) {
            //有没有必要创建事务
            TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);

            Object retVal;
            try {
                //调用钩子函数进行回调目标方法
                retVal = invocation.proceedWithInvocation();
            }catch (Throwable ex) {
                //抛出异常进行回滚处理
                completeTransactionAfterThrowing(txInfo, ex);
                throw ex;
            }finally {
                //清空我们的线程变量中transactionInfo的值
                cleanupTransactionInfo(txInfo);
            }

            if (vavrPresent && VavrDelegate.isVavrTry(retVal)) {
                // Set rollback-only in case of Vavr failure matching our rollback rules...
                org.springframework.transaction.TransactionStatus status = txInfo.getTransactionStatus();
                if (status != null && txAttr != null) {
                    retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
                }
            }
            //提交事务
            commitTransactionAfterReturning(txInfo);
            return retVal;
        } else { //编程式事务
            final ThrowableHolder throwableHolder = new ThrowableHolder();

            //这是一个CallbackPreferringPlatformTransactionManager:通过TransactionCallback回调进入
            try {
                Object result = ((CallbackPreferringPlatformTransactionManager) ptm).execute(txAttr, status -> {
                    TransactionAspectSupport.TransactionInfo txInfo = prepareTransactionInfo(ptm, txAttr, joinpointIdentification, status);
                    try {
                        Object retVal = invocation.proceedWithInvocation();
                        if (vavrPresent && VavrDelegate.isVavrTry(retVal)) {
                            //仅在Vavr匹配回滚规则失败的情况下设置回滚
                            retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);
                        }
                        return retVal;
                    } catch (Throwable ex) {
                        if (txAttr.rollbackOn(ex)) {
                            // A RuntimeException: will lead to a rollback.
                            if (ex instanceof RuntimeException) {
                                throw (RuntimeException) ex;
                            }else {
                                throw new ThrowableHolderException(ex);
                            }
                        }else {
                            //一个正常的返回值:将导致提交
                            throwableHolder.throwable = ex;
                            return null;
                        }
                    }finally {
                        cleanupTransactionInfo(txInfo);
                    }
                });

                // Check result state: It might indicate a Throwable to rethrow.
                if (throwableHolder.throwable != null) {
                    throw throwableHolder.throwable;
                }
                return result;
            }catch (ThrowableHolderException ex) {
                throw ex.getCause();
            }catch (TransactionSystemException ex2) {
                if (throwableHolder.throwable != null) {
                    logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
                    ex2.initApplicationException(throwableHolder.throwable);
                }
                throw ex2;
            }catch (Throwable ex2) {
                if (throwableHolder.throwable != null) {
                    logger.error("Application exception overridden by commit exception", throwableHolder.throwable);
                }
                throw ex2;
            }
        }
    }


    //TransactionAspectSupport:
    protected TransactionInfo createTransactionIfNecessary(@Nullable PlatformTransactionManager tm,
                                                                                    @Nullable TransactionAttribute txAttr, final String joinpointIdentification) {
        //把我们的方法描述符作为一个事务名称
        if (txAttr != null && txAttr.getName() == null) {
            txAttr = new DelegatingTransactionAttribute(txAttr) {
                @Override
                public String getName() {
                    return joinpointIdentification;
                }
            };
        }

        TransactionStatus status = null;
        if (txAttr != null) {
            if (tm != null) {
                //获取一个事务状态
                status = tm.getTransaction(txAttr);
            }else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping transactional joinpoint [" + joinpointIdentification +
                            "] because no transaction manager has been configured");
                }
            }
        }
        //把事务状态和事务属性等信息封装成一个TransactionInfo对象
        return prepareTransactionInfo(tm, txAttr, joinpointIdentification, status);
    }




}
