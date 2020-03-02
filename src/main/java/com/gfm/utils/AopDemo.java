package com.gfm.utils;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.AjType;
import org.aspectj.lang.reflect.AjTypeSystem;
import org.aspectj.lang.reflect.PerClauseKind;
import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.aspectj.*;
import org.springframework.aop.aspectj.annotation.*;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.Pointcuts;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConvertingComparator;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.InstanceComparator;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

public class AopDemo {

//    @Target(ElementType.TYPE)
//    @Retention(RetentionPolicy.RUNTIME)
//    @Documented
//    @Import(AspectJAutoProxyRegistrar.class)
//    public @interface EnableAspectJAutoProxy {
//
//        /**
//         * 声明使用CGLIB动态代理，还是JDK动态代理。默认是false,使用jdk动态代理
//         * @return
//         */
//        boolean proxyTargetClass() default false;
//
//        /**
//         * exposeProxy = true;表示通过aop框架暴露该代理对象，aopContext能够访问
//         * @return
//         */
//        boolean exposeProxy() default false;
//    }
//
//
//
//    class AspectJAutoProxyRegistrar implements ImportBeanDefinitionRegistrar {
//
//        /**
//         * 注册，升级和配置基于AspectJ的自动代理
//         */
//        @Override
//        public void registerBeanDefinitions(
//                AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
//
//            //调用registerBeanDefinitions方法，往Spring容器中注册AnnotationAwareAspectJAutoProxyCreator的Bean的定义信息
//            AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);
//
//            AnnotationAttributes enableAspectJAutoProxy =
//                    AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);
//            if (enableAspectJAutoProxy != null) {
//                if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {
//                    AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
//                }
//                if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {
//                    AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
//                }
//            }
//        }
//    }





//    //AopConfigUtils:
//    public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(BeanDefinitionRegistry registry) {
//        return registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry, null);
//    }
//
//    //AopConfigUtils:
//    public static BeanDefinition registerAspectJAnnotationAutoProxyCreatorIfNecessary(
//            BeanDefinitionRegistry registry, @Nullable Object source) {
//
//        return registerOrEscalateApcAsRequired(AnnotationAwareAspectJAutoProxyCreator.class, registry, source);
//    }
//
//    //AopConfigUtils:
//    private static BeanDefinition registerOrEscalateApcAsRequired(
//            Class<?> cls, BeanDefinitionRegistry registry, @Nullable Object source) {
//        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
//
//        //如果存在org.springframework.aop.config.internalAutoProxyCreator直接使用
//        //AUTO_PROXY_CREATOR_BEAN_NAME = "org.springframework.aop.config.internalAutoProxyCreator"
//        if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
//            BeanDefinition apcDefinition = registry.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
//            if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
//                int currentPriority = findPriorityForClass(apcDefinition.getBeanClassName());
//                int requiredPriority = findPriorityForClass(cls);
//                if (currentPriority < requiredPriority) {
//                    apcDefinition.setBeanClassName(cls.getName());
//                }
//            }
//            return null;
//        }
//
//        //没有就自己注册
//        RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
//        beanDefinition.setSource(source);
//        beanDefinition.getPropertyValues().add("order", Ordered.HIGHEST_PRECEDENCE);
//        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
//        registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME, beanDefinition);
//        return beanDefinition;
//    }
//
//    //AopConfigUtils:
//    private static int findPriorityForClass(@Nullable String className) {
//        /**
//         * List<Class<?>> APC_PRIORITY_LIST = new ArrayList<>(3)
//         * APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
//         * PC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
//         * APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
//         */
//        for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
//            Class<?> clazz = APC_PRIORITY_LIST.get(i);
//            if (clazz.getName().equals(className)) {
//                return i;
//            }
//        }
//        throw new IllegalArgumentException(
//                "Class name [" + className + "] is not a known auto-proxy creator class");
//    }
//






//    //AbstractAdvisorAutoProxyCreator:
//    public void setBeanFactory(BeanFactory beanFactory) {
//        super.setBeanFactory(beanFactory);
//        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
//            throw new IllegalArgumentException(
//                    "AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);
//        }
//        initBeanFactory((ConfigurableListableBeanFactory) beanFactory);
//    }
//
//    //AbstractAdvisorAutoProxyCreator:
//    protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {
//        this.advisorRetrievalHelper = new AbstractAdvisorAutoProxyCreator.BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);
//    }


//
//    //AbstractAutowireCapableBeanFactory:
//    protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
//        if (System.getSecurityManager() != null) {
//            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
//                invokeAwareMethods(beanName, bean);
//                return null;
//            }, getAccessControlContext());
//        }else {
//            invokeAwareMethods(beanName, bean);
//        }
//
//        Object wrappedBean = bean;
//        if (mbd == null || !mbd.isSynthetic()) {
//            wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
//        }
//
//        try {
//            invokeInitMethods(beanName, wrappedBean, mbd);
//        }catch (Throwable ex) {
//            throw new BeanCreationException(
//                    (mbd != null ? mbd.getResourceDescription() : null),
//                    beanName, "Invocation of init method failed", ex);
//        }
//        if (mbd == null || !mbd.isSynthetic()) {
//            //配置Aop的入口，应用BeanPostProcessorAfter实现
//            wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
//        }
//
//        return wrappedBean;
//    }



//    //AbstractAutowireCapableBeanFactory:
//    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
//            throws BeansException {
//
//        Object result = existingBean;
//        //遍历所有的BeanPostProcessor
//        for (BeanPostProcessor processor : getBeanPostProcessors()) {
//            //将AOP配置方法应用到Bean
//            Object current = processor.postProcessAfterInitialization(result, beanName);
//            if (current == null) {
//                return result;
//            }
//            result = current;
//        }
//        return result;
//    }
//
//    //AbstractAutoProxyCreator:
//    public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
//        if (bean != null) {
//            //通过自己封装的key获取（beanClassName_beanName）
//            Object cacheKey = getCacheKey(bean.getClass(), beanName);
//            //缓存中不包含
//            //Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16)
//            if (this.earlyProxyReferences.remove(cacheKey) != bean) {
//                //解析过程
//                return wrapIfNecessary(bean, beanName, cacheKey);
//            }
//        }
//        return bean;
//    }
//
//
//    //AbstractAutoProxyCreator:
//    //解析包装给定的bean
//    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
//        //Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16))
//        if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
//            return bean;
//        }
//        //Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256)
//        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
//            return bean;
//        }
//        //1>如果是基础公共bean, 或者是以 .ORIGINAL作为后缀的原始bean直接跳过
//        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
//            this.advisedBeans.put(cacheKey, Boolean.FALSE);
//            return bean;
//        }
//
//        //2>获取增强器
//        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
//        if (specificInterceptors != DO_NOT_PROXY) {
//            this.advisedBeans.put(cacheKey, Boolean.TRUE);
//            //3>创建代理
//            Object proxy = createProxy(
//                    bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
//            //Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16)
//            this.proxyTypes.put(cacheKey, proxy.getClass());
//            //返回代理过后的bean，加入容器
//            return proxy;
//        }
//
//        this.advisedBeans.put(cacheKey, Boolean.FALSE);
//        return bean;
//    }


//    //AbstractAutoProxyCreator:
//    protected boolean shouldSkip(Class<?> beanClass, String beanName) {
//        return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
//    }
//
//    //AutoProxyUtils:
//    //根据bean名称是否有后缀.ORIGINAL 判断是否是一个original instance，如果不是返回false
//    static boolean isOriginalInstance(String beanName, Class<?> beanClass) {
//        if (!StringUtils.hasLength(beanName) || beanName.length() !=
//                beanClass.getName().length() + AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX.length()) {
//            return false;
//        }
//        return (beanName.startsWith(beanClass.getName()) &&
//                beanName.endsWith(AutowireCapableBeanFactory.ORIGINAL_INSTANCE_SUFFIX));
//    }






    //AbstractAdvisorAutoProxyCreator:
    //获取所有通知增强器，增强器可以理解成对需要代理的类的增强,代理过程就是对需要的代理类进行相关处理
    protected Object[] getAdvicesAndAdvisorsForBean(
            Class<?> beanClass, String beanName, @Nullable TargetSource targetSource) {
        //获取满足条件的通知
        List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
        if (advisors.isEmpty()) {
            return DO_NOT_PROXY;
        }
        return advisors.toArray();
    }

    //AbstractAdvisorAutoProxyCreator:
    protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
        //1>查找所有增强器
        List<Advisor> candidateAdvisors = findCandidateAdvisors();
        //2>发现合适的增强器, 即有哪些通知advice可以应用
        List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
        extendAdvisors(eligibleAdvisors);
        if (!eligibleAdvisors.isEmpty()) {
            //排序通知，子类可以重写这个方法自定义排序策略
            eligibleAdvisors = sortAdvisors(eligibleAdvisors);
        }
        return eligibleAdvisors;
    }

    //AnnotationAwareAspectJAutoProxyCreator:
    protected List<Advisor> findCandidateAdvisors() {
        //发现父类是否存在通知advisor
        List<Advisor> advisors = super.findCandidateAdvisors();
        //添加新发现的通知advisor
        if (this.aspectJAdvisorsBuilder != null) {
            advisors.addAll(this.aspectJAdvisorsBuilder.buildAspectJAdvisors());
        }
        return advisors;
    }

    //AbstractAdvisorAutoProxyCreator:
    //获取所有使用自动代理的通知advisor
    protected List<Advisor> findCandidateAdvisors() {
        Assert.state(this.advisorRetrievalHelper != null, "No BeanFactoryAdvisorRetrievalHelper available");
        //BeanFactoryAdvisorRetrievalHelper advisorRetrievalHelper;
        return this.advisorRetrievalHelper.findAdvisorBeans();
    }

    //BeanFactoryAdvisorRetrievalHelper:
    //获取当前bean工厂中符合条件的通知bean,忽略FactoryBeans和正在创建的beans
    public List<Advisor> findAdvisorBeans() {
        //确定通知bean名称列表,如果没有缓存
        String[] advisorNames = this.cachedAdvisorBeanNames;
        if (advisorNames == null) {
            /**
             * AOP功能中在这里传入的是Object对象，代表去容器中获取到所有的组件的名称，然后再
             * 进行遍历，这个过程是十分的消耗性能的，所以说Spring会再这里加入了保存切面信息的缓存。
             * 但是事务功能不一样，事务模块的功能是直接去容器中获取Advisor类型的，选择范围小，且不消耗性能。
             * 所以Spring在事务模块中没有加入缓存来保存我们的事务相关的advisor
             */
            advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                    this.beanFactory, Advisor.class, true, false);
            this.cachedAdvisorBeanNames = advisorNames;
        }
        if (advisorNames.length == 0) {
            return new ArrayList<>();
        }

        List<Advisor> advisors = new ArrayList<>();
        for (String name : advisorNames) {
            if (isEligibleBean(name)) {
                if (this.beanFactory.isCurrentlyInCreation(name)) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Skipping currently created advisor '" + name + "'");
                    }
                }else {
                    try {
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
                                // Ignore: indicates a reference back to the bean we're trying to advise.
                                // We want to find advisors other than the currently created bean itself.
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




    //BeanFactoryAspectJAdvisorsBuilder:
    //去容器中获取到所有的切面信息保存到缓存中
    public List<Advisor> buildAspectJAdvisors() {
        List<String> aspectNames = this.aspectBeanNames;
        //缓存字段aspectNames没有值 注意实例化第一个单实例bean的时候就会触发解析切面
        if (aspectNames == null) {
            synchronized (this) {
                aspectNames = this.aspectBeanNames;
                if (aspectNames == null) {
                    //用于保存所有解析出来的Advisors集合对象
                    List<Advisor> advisors = new ArrayList<>();
                    //用于保存切面的名称的集合
                    aspectNames = new ArrayList<>();
                    /**
                     * 1>AOP功能中在这里传入的是Object对象，代表去容器中获取到所有的组件的名称，然后再
                     * 进行遍历，这个过程是十分的消耗性能的，所以说Spring会再这里加入了保存切面信息的缓存。
                     * 但是事务功能不一样，事务模块的功能是直接去容器中获取Advisor类型的，选择范围小，且不消耗性能。
                     * 所以Spring在事务模块中没有加入缓存来保存我们的事务相关的advisor
                     */
                    String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                            this.beanFactory, Object.class, true, false);
                    //遍历我们从IOC容器中获取处的所有Bean的名称
                    for (String beanName : beanNames) {
                        if (!isEligibleBean(beanName)) {
                            continue;
                        }
                        //通过beanName去容器中获取到对应class对象
                        Class<?> beanType = this.beanFactory.getType(beanName);
                        if (beanType == null) {
                            continue;
                        }
                        //根据class对象判断是不是切面 @Aspect
                        if (this.advisorFactory.isAspect(beanType)) {
                            //是切面类, 加入到缓存中
                            aspectNames.add(beanName);
                            //把beanName和class对象构建成为一个AspectMetadata
                            AspectMetadata amd = new AspectMetadata(beanType, beanName);
                            if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
                                //构建切面注解的实例工厂
                                MetadataAwareAspectInstanceFactory factory =
                                        new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
                                //2>真正的去获取我们的Advisor
                                //AspectJAdvisorFactory advisorFactory
                                List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
                                //加入到缓存中
                                if (this.beanFactory.isSingleton(beanName)) {
                                    //Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>()
                                    this.advisorsCache.put(beanName, classAdvisors);
                                }else {
                                    //Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>()
                                    this.aspectFactoryCache.put(beanName, factory);
                                }
                                advisors.addAll(classAdvisors);
                            }else {
                                // Per target or per this.
                                if (this.beanFactory.isSingleton(beanName)) {
                                    throw new IllegalArgumentException("Bean with name '" + beanName +
                                            "' is a singleton, but aspect instantiation model is not singleton");
                                }
                                MetadataAwareAspectInstanceFactory factory =
                                        new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
                                this.aspectFactoryCache.put(beanName, factory);
                                advisors.addAll(this.advisorFactory.getAdvisors(factory));
                            }
                        }
                    }
                    this.aspectBeanNames = aspectNames;
                    return advisors;
                }
            }
        }

        if (aspectNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<Advisor> advisors = new ArrayList<>();
        for (String aspectName : aspectNames) {
            List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
            if (cachedAdvisors != null) {
                advisors.addAll(cachedAdvisors);
            }else {
                MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
                advisors.addAll(this.advisorFactory.getAdvisors(factory));
            }
        }
        return advisors;
    }



    //BeanFactoryUtils:
    //得到给定类型的所有bean名称,包括那些在祖先中定义的工厂。将返回唯一名称的覆盖bean定义
    public static String[] beanNamesForTypeIncludingAncestors(
            ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

        Assert.notNull(lbf, "ListableBeanFactory must not be null");
        String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        if (lbf instanceof HierarchicalBeanFactory) {
            HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
            if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
                String[] parentResult = beanNamesForTypeIncludingAncestors(
                        (ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
                result = mergeNamesWithParent(result, parentResult, hbf);
            }
        }
        return result;
    }

    //BeanFactoryUtils:
    private static String[] mergeNamesWithParent(String[] result, String[] parentResult, HierarchicalBeanFactory hbf) {
        if (parentResult.length == 0) {
            return result;
        }
        List<String> merged = new ArrayList<>(result.length + parentResult.length);
        merged.addAll(Arrays.asList(result));
        for (String beanName : parentResult) {
            if (!merged.contains(beanName) && !hbf.containsLocalBean(beanName)) {
                merged.add(beanName);
            }
        }
        return StringUtils.toStringArray(merged);
    }



    //ReflectiveAspectJAdvisorFactory:
    public List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory) {
        //获取我们的标记为Aspect的类
        Class<?> aspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        //获取我们的切面类的名称
        String aspectName = aspectInstanceFactory.getAspectMetadata().getAspectName();
        //校验我们的切面类
        validate(aspectClass);

        //使用包装模式构建MetadataAwareAspectInstanceFactory
        MetadataAwareAspectInstanceFactory lazySingletonAspectInstanceFactory =
                new LazySingletonAspectInstanceFactoryDecorator(aspectInstanceFactory);

        List<Advisor> advisors = new ArrayList<>();
        //获取到切面类中的所有方法，但是该方法不会解析到标注了@PointCut注解的方法
        for (Method method : getAdvisorMethods(aspectClass)) {
            //循环解析我们切面中的方法
            Advisor advisor = getAdvisor(method, lazySingletonAspectInstanceFactory, advisors.size(), aspectName);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        //如果它是一个目标切面，创建仿真的实例切面
        if (!advisors.isEmpty() && lazySingletonAspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
            Advisor instantiationAdvisor = new ReflectiveAspectJAdvisorFactory.SyntheticInstantiationAdvisor(lazySingletonAspectInstanceFactory);
            advisors.add(0, instantiationAdvisor);
        }

        //获取所有引用字段
        for (Field field : aspectClass.getDeclaredFields()) {
            Advisor advisor = getDeclareParentsAdvisor(field);
            if (advisor != null) {
                advisors.add(advisor);
            }
        }

        return advisors;
    }



    //AbstractAspectJAdvisorFactory:
    public void validate(Class<?> aspectClass) throws AopConfigException {
        //如果父类有注解且不是Abstract, 就抛出异常
        if (aspectClass.getSuperclass().getAnnotation(Aspect.class) != null &&
                !Modifier.isAbstract(aspectClass.getSuperclass().getModifiers())) {
            throw new AopConfigException("[" + aspectClass.getName() + "] cannot extend concrete aspect [" +
                    aspectClass.getSuperclass().getName() + "]");
        }

        AjType<?> ajType = AjTypeSystem.getAjType(aspectClass);
        if (!ajType.isAspect()) {
            throw new NotAnAtAspectException(aspectClass);
        }
        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOW) {
            throw new AopConfigException(aspectClass.getName() + " uses percflow instantiation model: " +
                    "This is not supported in Spring AOP.");
        }
        if (ajType.getPerClause().getKind() == PerClauseKind.PERCFLOWBELOW) {
            throw new AopConfigException(aspectClass.getName() + " uses percflowbelow instantiation model: " +
                    "This is not supported in Spring AOP.");
        }
    }


    //ReflectiveAspectJAdvisorFactory:
    //获取切面上的通知方法，并按照规则排序
    private List<Method> getAdvisorMethods(Class<?> aspectClass) {
        final List<Method> methods = new ArrayList<>();
        ReflectionUtils.doWithMethods(aspectClass, method -> {
            // Exclude pointcuts
            if (AnnotationUtils.getAnnotation(method, Pointcut.class) == null) {
                methods.add(method);
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
        methods.sort(METHOD_COMPARATOR);
        return methods;
    }


    private static final Comparator<Method> METHOD_COMPARATOR;

    static {
        Comparator<Method> adviceKindComparator = new ConvertingComparator<>(
                new InstanceComparator<>(
                        Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class),
                (Converter<Method, Annotation>) method -> {
                    AbstractAspectJAdvisorFactory.AspectJAnnotation<?> annotation =
                            AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(method);
                    return (annotation != null ? annotation.getAnnotation() : null);
                });
        Comparator<Method> methodNameComparator = new ConvertingComparator<>(Method::getName);
        METHOD_COMPARATOR = adviceKindComparator.thenComparing(methodNameComparator);
    }


    //----------------

    //ReflectiveAspectJAdvisorFactory:
    public Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
                              int declarationOrderInAspect, String aspectName) {

        validate(aspectInstanceFactory.getAspectMetadata().getAspectClass());

        //切面的方法上构建切点表达式
        AspectJExpressionPointcut expressionPointcut = getPointcut(
                candidateAdviceMethod, aspectInstanceFactory.getAspectMetadata().getAspectClass());
        if (expressionPointcut == null) {
            return null;
        }
        //实例化我们的切面通知对象
        return new InstantiationModelAwarePointcutAdvisorImpl(expressionPointcut, candidateAdviceMethod,
                this, aspectInstanceFactory, declarationOrderInAspect, aspectName);
    }


    //ReflectiveAspectJAdvisorFactory:
    private AspectJExpressionPointcut getPointcut(Method candidateAdviceMethod, Class<?> candidateAspectClass) {
        //获取给定方法上的第一个aspectJ注解
        AspectJAnnotation<?> aspectJAnnotation =
                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        if (aspectJAnnotation == null) {
            return null;
        }

        //设置切点pointCut的表达式
        AspectJExpressionPointcut ajexp =
                new AspectJExpressionPointcut(candidateAspectClass, new String[0], new Class<?>[0]);
        ajexp.setExpression(aspectJAnnotation.getPointcutExpression());
        if (this.beanFactory != null) {
            ajexp.setBeanFactory(this.beanFactory);
        }
        return ajexp;
    }



    //AbstractAspectJAdvisorFactory:
    protected static AspectJAnnotation<?> findAspectJAnnotationOnMethod(Method method) {
        //Class<?>[] ASPECTJ_ANNOTATION_CLASSES = new Class<?>[] {
        //			Pointcut.class, Around.class, Before.class, After.class, AfterReturning.class, AfterThrowing.class}
        for (Class<?> clazz : ASPECTJ_ANNOTATION_CLASSES) {
            AspectJAnnotation<?> foundAnnotation = findAnnotation(method, (Class<Annotation>) clazz);
            if (foundAnnotation != null) {
                return foundAnnotation;
            }
        }
        return null;
    }

    //AbstractAspectJAdvisorFactory:
    private static <A extends Annotation> AspectJAnnotation<A> findAnnotation(Method method, Class<A> toLookFor) {
        A result = AnnotationUtils.findAnnotation(method, toLookFor);
        if (result != null) {
            return new AspectJAnnotation<>(result);
        }else {
            return null;
        }
    }

    //AnnotationUtils:
    public static <A extends Annotation> A findAnnotation(Method method, @Nullable Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }
        // 快捷方式:直接出现在元素,不需要合并吗?
        if (AnnotationFilter.PLAIN.matches(annotationType) ||
                AnnotationsScanner.hasPlainJavaAnnotationsOnly(method)) {
            return method.getDeclaredAnnotation(annotationType);
        }
        //详细的检索合并注释
        return MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
                .get(annotationType).withNonMergedAttributes()
                .synthesize(MergedAnnotation::isPresent).orElse(null);
    }




    //InstantiationModelAwarePointcutAdvisorImpl:
    public InstantiationModelAwarePointcutAdvisorImpl(AspectJExpressionPointcut declaredPointcut,
                                                      Method aspectJAdviceMethod, AspectJAdvisorFactory aspectJAdvisorFactory,
                                                      MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {

        //当前的切点表达式
        this.declaredPointcut = declaredPointcut;
        //切面的class对象
        this.declaringClass = aspectJAdviceMethod.getDeclaringClass();
        //切面方法的名称
        this.methodName = aspectJAdviceMethod.getName();
        //切面方法的参数类型
        this.parameterTypes = aspectJAdviceMethod.getParameterTypes();
        //切面方法对象
        this.aspectJAdviceMethod = aspectJAdviceMethod;
        //aspectj的通知工厂
        this.aspectJAdvisorFactory = aspectJAdvisorFactory;
        //aspect的实例工厂
        this.aspectInstanceFactory = aspectInstanceFactory;
        //切面的顺序
        this.declarationOrder = declarationOrder;
        //切面的名称
        this.aspectName = aspectName;

        //判断当前的切面对象是否需要延时加载
        if (aspectInstanceFactory.getAspectMetadata().isLazilyInstantiated()) {
            //pointcut的静态部分是一个lazy类型
            org.springframework.aop.Pointcut preInstantiationPointcut = Pointcuts.union(
                    aspectInstanceFactory.getAspectMetadata().getPerClausePointcut(), this.declaredPointcut);

            // Make it dynamic: must mutate from pre-instantiation to post-instantiation state.
            // If it's not a dynamic pointcut, it may be optimized out
            // by the Spring AOP infrastructure after the first evaluation.

            //使其动态，必须从pre-instantiation突变为post-instantiation状态，如果它不是一个动态的切点，
            ///可能是优化的Spring AOP基本组件在第一次解析
            this.pointcut = new PerTargetInstantiationModelPointcut(
                    this.declaredPointcut, preInstantiationPointcut, aspectInstanceFactory);
            this.lazy = true;
        }else {
            //一个单例切面
            this.pointcut = this.declaredPointcut;
            this.lazy = false;
            //将切面中的通知构造为advice通知对象
            this.instantiatedAdvice = instantiateAdvice(this.declaredPointcut);
        }
    }



    //InstantiationModelAwarePointcutAdvisorImpl:
    private Advice instantiateAdvice(AspectJExpressionPointcut pointcut) {
        Advice advice = this.aspectJAdvisorFactory.getAdvice(this.aspectJAdviceMethod, pointcut,
                this.aspectInstanceFactory, this.declarationOrder, this.aspectName);
        //Advice EMPTY_ADVICE = new Advice() {}
        return (advice != null ? advice : EMPTY_ADVICE);
    }


    //ReflectiveAspectJAdvisorFactory:
    public Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
                            MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName) {
        //获取我们的切面类的class对象
        Class<?> candidateAspectClass = aspectInstanceFactory.getAspectMetadata().getAspectClass();
        validate(candidateAspectClass);

        //获取切面方法上的注解
        AspectJAnnotation<?> aspectJAnnotation =
                AbstractAspectJAdvisorFactory.findAspectJAnnotationOnMethod(candidateAdviceMethod);
        //解析出来的注解信息是否为null
        if (aspectJAnnotation == null) {
            return null;
        }

        //再次判断是否是切面对象
        if (!isAspect(candidateAspectClass)) {
            throw new AopConfigException("Advice must be declared inside an aspect type: " +
                    "Offending method '" + candidateAdviceMethod + "' in class [" +
                    candidateAspectClass.getName() + "]");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Found AspectJ method: " + candidateAdviceMethod);
        }

        AbstractAspectJAdvice springAdvice;

        //判断标注在方法上的注解类型
        switch (aspectJAnnotation.getAnnotationType()) {
            //是PointCut注解 那么就抛出异常 因为在外面传递进来的方法已经排除了Pointcut的方法
            case AtPointcut:
                if (logger.isDebugEnabled()) {
                    logger.debug("Processing pointcut '" + candidateAdviceMethod.getName() + "'");
                }
                return null;
            //环绕通知 构建AspectJAroundAdvice
            case AtAround:
                springAdvice = new AspectJAroundAdvice(
                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                break;
            //前置通知  构建AspectJMethodBeforeAdvice
            case AtBefore:
                springAdvice = new AspectJMethodBeforeAdvice(
                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                break;
            //后置通知 AspectJAfterAdvice
            case AtAfter:
                springAdvice = new AspectJAfterAdvice(
                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                break;
            //返回通知 AspectJAfterReturningAdvice
            case AtAfterReturning:
                springAdvice = new AspectJAfterReturningAdvice(
                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                AfterReturning afterReturningAnnotation = (AfterReturning) aspectJAnnotation.getAnnotation();
                if (StringUtils.hasText(afterReturningAnnotation.returning())) {
                    springAdvice.setReturningName(afterReturningAnnotation.returning());
                }
                break;
            //异常通知   AspectJAfterThrowingAdvice
            case AtAfterThrowing:
                springAdvice = new AspectJAfterThrowingAdvice(
                        candidateAdviceMethod, expressionPointcut, aspectInstanceFactory);
                AfterThrowing afterThrowingAnnotation = (AfterThrowing) aspectJAnnotation.getAnnotation();
                if (StringUtils.hasText(afterThrowingAnnotation.throwing())) {
                    springAdvice.setThrowingName(afterThrowingAnnotation.throwing());
                }
                break;
            default:
                throw new UnsupportedOperationException(
                        "Unsupported advice type on method: " + candidateAdviceMethod);
        }

        //设置我们构建出来的通知对象的相关属性比如DeclarationOrder，在代理调用的时候，责任链顺序上会用到
        springAdvice.setAspectName(aspectName);
        springAdvice.setDeclarationOrder(declarationOrder);
        String[] argNames = this.parameterNameDiscoverer.getParameterNames(candidateAdviceMethod);
        if (argNames != null) {
            springAdvice.setArgumentNamesFromStringArray(argNames);
        }
        springAdvice.calculateArgumentBindings();

        return springAdvice;
    }


    //ReflectiveAspectJAdvisorFactory:
    //获取定义的父通知advisor,用给定引用字段构建一个 {org.springframework.aop.aspectj.DeclareParentsAdvisor}
    private Advisor getDeclareParentsAdvisor(Field introductionField) {
        DeclareParents declareParents = introductionField.getAnnotation(DeclareParents.class);
        if (declareParents == null) {
            //不是一个引用字段
            return null;
        }

        if (DeclareParents.class == declareParents.defaultImpl()) {
            throw new IllegalStateException("'defaultImpl' attribute must be set on DeclareParents");
        }

        return new DeclareParentsAdvisor(
                introductionField.getType(), declareParents.value(), declareParents.defaultImpl());
    }





    //AbstractAdvisorAutoProxyCreator:
    protected List<Advisor> findAdvisorsThatCanApply(
            List<Advisor> candidateAdvisors, Class<?> beanClass, String beanName) {

        ProxyCreationContext.setCurrentProxiedBeanName(beanName);
        try {
            //从候选的通知器中找到合适正在创建的实例对象的通知器
            return AopUtils.findAdvisorsThatCanApply(candidateAdvisors, beanClass);
        }finally {
            ProxyCreationContext.setCurrentProxiedBeanName(null);
        }
    }


    //AopUtils:
    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
        //若候选的通知集合为空 直接返回
        if (candidateAdvisors.isEmpty()) {
            return candidateAdvisors;
        }
        //定义一个合适的通知集合对象
        List<Advisor> eligibleAdvisors = new ArrayList<>();
        //循环我们候选的通知对象
        for (Advisor candidate : candidateAdvisors) {
            //判断我们的通知对象是不是实现了IntroductionAdvisor(很明显我们事务的没有实现 所以不会走下面的逻辑)
            if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
                eligibleAdvisors.add(candidate);
            }
        }
        //不为空
        boolean hasIntroductions = !eligibleAdvisors.isEmpty();
        for (Advisor candidate : candidateAdvisors) {
            //判断我们的增强器对象是不是实现了IntroductionAdvisor(很明显我们事务的没有实现,所以不会走下面的逻辑)
            if (candidate instanceof IntroductionAdvisor) {
                //在上面已经处理过 ，不需要处理
                continue;
            }
            //1>真正的判断通知advisor是否合适当前类型
            if (canApply(candidate, clazz, hasIntroductions)) {
                eligibleAdvisors.add(candidate);
            }
        }
        return eligibleAdvisors;
    }




}
