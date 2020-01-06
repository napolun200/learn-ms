package com.gfm.utils;

import com.gfm.service.UserService;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.propertyeditors.*;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.NoOp;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.*;
import org.springframework.core.convert.*;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.log.LogMessage;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.jndi.TypeMismatchNamingException;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.xml.sax.InputSource;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BeanInstanceDemo {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.xml");
        UserService userService = context.getBean(UserService.class);
    }

    //AbstractApplicationContext：
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        //1>断言此上下文的BeanFactory当前处于激活状态，如果没有抛出异常
        assertBeanFactoryActive();
        //2>通过类型从beanFactory容器中获取Bean实例
        return getBeanFactory().getBean(requiredType);
    }

    //AbstractApplicationContext:
    protected void assertBeanFactoryActive() {
        if (!this.active.get()) {
            if (this.closed.get()) {
                throw new IllegalStateException(getDisplayName() + " has been closed already");
            }else {
                throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
            }
        }
    }

    //DefaultListableBeanFactory:
    public <T> T getBean(Class<T> requiredType) throws BeansException {
        return getBean(requiredType, (Object[]) null);
    }

    //DefaultListableBeanFactory:
    public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
        Assert.notNull(requiredType, "Required type must not be null");
        //解析Bean类型，对Bean进行实例化返回
        Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
        if (resolved == null) {
            throw new NoSuchBeanDefinitionException(requiredType);
        }
        return (T) resolved;
    }

    //ResolvableType:
    public static ResolvableType forRawClass(@Nullable Class<?> clazz) {
        return new ResolvableType(clazz) {
            @Override
            public ResolvableType[] getGenerics() {
                return EMPTY_TYPES_ARRAY;
            }
            @Override
            public boolean isAssignableFrom(Class<?> other) {
                return (clazz == null || ClassUtils.isAssignable(clazz, other));
            }
            @Override
            public boolean isAssignableFrom(ResolvableType other) {
                Class<?> otherClass = other.resolve();
                return (otherClass != null && (clazz == null || ClassUtils.isAssignable(clazz, otherClass)));
            }
        };
    }

    //ClassUtils:
    public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
        Assert.notNull(lhsType, "Left-hand side type must not be null");
        Assert.notNull(rhsType, "Right-hand side type must not be null");
        if (lhsType.isAssignableFrom(rhsType)) {
            return true;
        }
        if (lhsType.isPrimitive()) {
            //Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(8)
            //以基本包装类型为键，相应的基本类型为值的映射，例如:Integer.class -> int.class
            Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
            if (lhsType == resolvedPrimitive) {
                return true;
            }
        } else {
            //Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(8)
            //以基本类型为键和相应包装器类型为值的映射，例如:int.class -> Integer.class
            Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
            if (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper)) {
                return true;
            }
        }
        return false;
    }


    //DefaultListableBeanFactory:
    private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
        //1>获取名称bean支持
        NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
        if (namedBean != null) {
            return namedBean.getBeanInstance();
        }
        BeanFactory parent = getParentBeanFactory();
        if (parent instanceof DefaultListableBeanFactory) {
            return ((DefaultListableBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
        }
        else if (parent != null) {
            ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
            if (args != null) {
                return parentProvider.getObject(args);
            }
            else {
                return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
            }
        }
        return null;
    }

    //A
    //DefaultListableBeanFactory:
    private <T> NamedBeanHolder<T> resolveNamedBean(
            ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {

        Assert.notNull(requiredType, "Required type must not be null");
        //通过类型获取候选的beanName数组
        String[] candidateNames = getBeanNamesForType(requiredType);

        if (candidateNames.length > 1) {
            List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
            for (String beanName : candidateNames) {
                //containsBeanDefinition(beanName) ==> beanDefinitionMap.containsKey(beanName);
                if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
                    autowireCandidates.add(beanName);
                }
            }
            if (!autowireCandidates.isEmpty()) {
                candidateNames = StringUtils.toStringArray(autowireCandidates);
            }
        }

        if (candidateNames.length == 1) {
            String beanName = candidateNames[0];
            //根据bean名称和类型获取bean实例
            //getBean(beanName, requiredType.toClass(), args)
            return new NamedBeanHolder<>(beanName, (T) getBean(beanName, requiredType.toClass(), args));
        }else if (candidateNames.length > 1) {
            Map<String, Object> candidates = new LinkedHashMap<>(candidateNames.length);
            for (String beanName : candidateNames) {
                if (containsSingleton(beanName) && args == null) {
                    Object beanInstance = getBean(beanName);
                    candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
                }else {
                    candidates.put(beanName, getType(beanName));
                }
            }
            String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
            if (candidateName == null) {
                candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
            }
            if (candidateName != null) {
                Object beanInstance = candidates.get(candidateName);
                if (beanInstance == null || beanInstance instanceof Class) {
                    beanInstance = getBean(candidateName, requiredType.toClass(), args);
                }
                return new NamedBeanHolder<>(candidateName, (T) beanInstance);
            }
            if (!nonUniqueAsNull) {
                throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
            }
        }
        return null;
    }

    //DefaultListableBeanFactory:
    public String[] getBeanNamesForType(ResolvableType type) {
        return getBeanNamesForType(type, true, true);
    }

    //DefaultListableBeanFactory:
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        //将此类型解析为{@link java.lang类}，如果类型无法解析，则返回null
        Class<?> resolved = type.resolve();
        if (resolved != null && !type.hasGenerics()) {
            //1>如果可以将type解析成类
            return getBeanNamesForType(resolved, includeNonSingletons, includeNonSingletons);
        }else {
            //2>不能将type解析成类
            return doGetBeanNamesForType(type, includeNonSingletons, includeNonSingletons);
        }
    }

    //DefaultListableBeanFactory:
    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
            return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
        }
        Map<Class<?>, String[]> cache =
                (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
        String[] resolvedBeanNames = cache.get(type);
        if (resolvedBeanNames != null) {
            return resolvedBeanNames;
        }
        resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
        if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
            cache.put(type, resolvedBeanNames);
        }
        return resolvedBeanNames;
    }

    //DefaultListableBeanFactory:
    private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        List<String> result = new ArrayList<>();

        // Check all bean definitions.
        for (String beanName : this.beanDefinitionNames) {
            //只有当bean名称没有定义为其他bean的别名时，才认为bean是合格的
            if (!isAlias(beanName)) { //不是其他bean的别名
                try {
                    RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                    //只有在bean定义完成时才检查它
                    if (!mbd.isAbstract() && (allowEagerInit ||
                            (mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
                                    !requiresEagerInitForType(mbd.getFactoryBeanName()))) {
                        //判断是否是FactoryBean
                        boolean isFactoryBean = isFactoryBean(beanName, mbd);
                        BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
                        boolean matchFound = false;
                        boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
                        boolean isNonLazyDecorated = dbd != null && !mbd.isLazyInit();
                        if (!isFactoryBean) {
                            if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
                                matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                            }
                        }else  {
                            if (includeNonSingletons || isNonLazyDecorated ||
                                    (allowFactoryBeanInit && isSingleton(beanName, mbd, dbd))) {
                                matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                            }
                            if (!matchFound) {
                                //对于FactoryBean，接下来尝试匹配FactoryBean实例本身
                                beanName = FACTORY_BEAN_PREFIX + beanName;
                                matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                            }
                        }
                        if (matchFound) {
                            result.add(beanName);
                        }
                    }
                }catch (CannotLoadBeanClassException | BeanDefinitionStoreException ex) {
                    if (allowEagerInit) {
                        throw ex;
                    }
                    // Probably a placeholder: let's ignore it for type matching purposes.
                    LogMessage message = (ex instanceof CannotLoadBeanClassException) ?
                            LogMessage.format("Ignoring bean class loading failure for bean '%s'", beanName) :
                            LogMessage.format("Ignoring unresolvable metadata in bean definition '%s'", beanName);
                    logger.trace(message, ex);
                    onSuppressedException(ex);
                }
            }
        }

        //也检查手动注册的单例
        for (String beanName : this.manualSingletonNames) {
            try {
                //对于FactoryBean，匹配FactoryBean创建的对象
                if (isFactoryBean(beanName)) {
                    if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
                        result.add(beanName);
                        //为这个bean找到匹配:不再匹配FactoryBean本身
                        continue;
                    }
                    //对于FactoryBean，接下来尝试匹配FactoryBean本身
                    beanName = FACTORY_BEAN_PREFIX + beanName;
                }
                //匹配原始bean实例(可能是原始的FactoryBean)
                if (isTypeMatch(beanName, type)) {
                    result.add(beanName);
                }
            }catch (NoSuchBeanDefinitionException ex) {
                // Shouldn't happen - probably a result of circular reference resolution...
                logger.trace(LogMessage.format("Failed to check manually registered singleton with name '%s'", beanName), ex);
            }
        }

        return StringUtils.toStringArray(result);
    }

    //AbstractBeanFactory:
    protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
            throws NoSuchBeanDefinitionException {

        String beanName = transformedBeanName(name);
        boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

        //检查手动注册的单例
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
            if (beanInstance instanceof FactoryBean) {
                if (!isFactoryDereference) {
                    Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
                    return (type != null && typeToMatch.isAssignableFrom(type));
                }else {
                    return typeToMatch.isInstance(beanInstance);
                }
            }else if (!isFactoryDereference) {
                if (typeToMatch.isInstance(beanInstance)) {
                    // Direct match for exposed instance?
                    return true;
                }else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
                    //泛型可能只匹配目标类，而不匹配代理
                    RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                    Class<?> targetType = mbd.getTargetType();
                    if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
                        //还要检查原始类匹配，确保它在代理上是公开的。
                        Class<?> classToMatch = typeToMatch.resolve();
                        if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
                            return false;
                        }
                        if (typeToMatch.isAssignableFrom(targetType)) {
                            return true;
                        }
                    }
                    ResolvableType resolvableType = mbd.targetType;
                    if (resolvableType == null) {
                        resolvableType = mbd.factoryMethodReturnType;
                    }
                    return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
                }
            }
            return false;
        } else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
            //空实例注册直接返回false
            return false;
        }

        //为发现单例实例，校验bean definition
        BeanFactory parentBeanFactory = getParentBeanFactory();
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            //在这个工厂中没有找到bean定义——>委托给父类
            return parentBeanFactory.isTypeMatch(originalBeanName(name), typeToMatch);
        }

        //检索相应的bean定义。
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();

        //设置我们想要匹配的类型
        Class<?> classToMatch = typeToMatch.resolve();
        if (classToMatch == null) {
            classToMatch = FactoryBean.class;
        }
        Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
                new Class<?>[] {classToMatch} : new Class<?>[] {FactoryBean.class, classToMatch});

        //尝试预测bean类型
        Class<?> predictedType = null;

        //我们正在寻找一个常规的引用，但是我们是一个工厂bean，它有一个修饰过的bean定义。
        //目标bean应该与FactoryBean最终返回的类型相同
        if (!isFactoryDereference && dbd != null && isFactoryBean(beanName, mbd)) {
            //只有当用户显式地将lazy-init设置为true, 并且我们知道合并的bean定义是针对工厂bean时，我们才应该尝试。
            if (!mbd.isLazyInit() || allowFactoryBeanInit) {
                RootBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
                Class<?> targetType = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
                if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
                    predictedType = targetType;
                }
            }
        }

        //如果我们不能使用目标类型，请尝试常规预测
        if (predictedType == null) {
            predictedType = predictBeanType(beanName, mbd, typesToMatch);
            if (predictedType == null) {
                return false;
            }
        }

        //尝试为bean获取实际的ResolvableType。
        ResolvableType beanType = null;

        //如果它是一个FactoryBean，我们希望查看它创建了什么，而不是factory类
        if (FactoryBean.class.isAssignableFrom(predictedType)) {
            if (beanInstance == null && !isFactoryDereference) {
                beanType = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit);
                predictedType = beanType.resolve();
                if (predictedType == null) {
                    return false;
                }
            }
        }else if (isFactoryDereference) {
            //特殊情况:一个SmartInstantiationAwareBeanPostProcessor返回了一个非FactoryBean类型，
            //但是我们仍然被要求取消对一个FactoryBean的引用
            predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
            if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
                return false;
            }
        }

        //我们没有确切的类型，但是如果bean定义目标类型或工厂方法返回类型与预测类型匹配，那么我们就可以使用它
        if (beanType == null) {
            ResolvableType definedType = mbd.targetType;
            if (definedType == null) {
                definedType = mbd.factoryMethodReturnType;
            }
            if (definedType != null && definedType.resolve() == predictedType) {
                beanType = definedType;
            }
        }

        //如果我们有一个bean类型，请使用它，以便考虑泛型
        if (beanType != null) {
            return typeToMatch.isAssignableFrom(beanType);
        }

        //如果没有bean类型，则退回到预测类型
        return typeToMatch.isAssignableFrom(predictedType);
    }



    //SimpleAliasRegistry:
    public boolean isAlias(String name) {
        // Map<String, String> aliasMap = new ConcurrentHashMap<>(16)
        //从别名映射到规范名称
        return this.aliasMap.containsKey(name);
    }

    //AbstractBeanFactory:
    //返回合并的RootBeanDefinition，如果指定的bean对应于子bean定义，则遍历父bean定义
    protected RootBeanDefinition getMergedLocalBeanDefinition(String beanName) throws BeansException {
        //首先快速检查并发映射，并使用最少的锁
        //Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256)
        RootBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
        if (mbd != null && !mbd.stale) {
            return mbd;
        }
        //如果RootBeanDefinition为空，就重新获取
        return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
    }

    //AbstractBeanFactory:
    protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd)
            throws BeanDefinitionStoreException {

        return getMergedBeanDefinition(beanName, bd, null);
    }

    //AbstractBeanFactory:
    protected RootBeanDefinition getMergedBeanDefinition(
            String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)
            throws BeanDefinitionStoreException {

        synchronized (this.mergedBeanDefinitions) {
            RootBeanDefinition mbd = null;
            RootBeanDefinition previous = null;

            //现在使用full lock进行检查，以执行相同的合并实例
            if (containingBd == null) {
                //Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256)
                mbd = this.mergedBeanDefinitions.get(beanName);
            }

            if (mbd == null || mbd.stale) {
                previous = mbd;
                mbd = null;
                if (bd.getParentName() == null) {
                    //使用给定 根bean定义 的副本
                    if (bd instanceof RootBeanDefinition) {
                        mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();
                    }else {
                        mbd = new RootBeanDefinition(bd);
                    }
                }else {
                    //子bean定义:需要与父bean合并
                    BeanDefinition pbd;
                    try {
                        //获取规范的beanName
                        String parentBeanName = transformedBeanName(bd.getParentName());
                        if (!beanName.equals(parentBeanName)) {
                            pbd = getMergedBeanDefinition(parentBeanName);
                        }else {
                            //获取父的beanFactory
                            BeanFactory parent = getParentBeanFactory();
                            if (parent instanceof ConfigurableBeanFactory) {
                                pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
                            }else {
                                throw new NoSuchBeanDefinitionException(parentBeanName,
                                        "Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +
                                                "': cannot be resolved without an AbstractBeanFactory parent");
                            }
                        }
                    }catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
                                "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
                    }
                    //深拷贝覆盖已有的值
                    mbd = new RootBeanDefinition(pbd);
                    mbd.overrideFrom(bd);
                }

                //设置默认的单例范围，如果之前没有配置
                if (!StringUtils.hasLength(mbd.getScope())) {
                    mbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
                }

                //非单例bean中包含的bean本身不能是单例的。让我们在这里动态地修正一下，因为这可能是外部bean的父-子合并的结果，
                //在这种情况下，原始的内部bean定义将不会继承合并后的外部bean的单例状态。
                if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
                    mbd.setScope(containingBd.getScope());
                }

                //暂时缓存合并的bean定义(为了获取元数据的更改，它可能在以后重新合并)
                if (containingBd == null && isCacheBeanMetadata()) {
                    //Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256)
                    this.mergedBeanDefinitions.put(beanName, mbd);
                }
            }
            if (previous != null) {
                copyRelevantMergedBeanDefinitionCaches(previous, mbd);
            }
            return mbd;
        }
    }

    //AbstractBeanFactory:
    protected String transformedBeanName(String name) {
        return canonicalName(BeanFactoryUtils.transformedBeanName(name));
    }

    //SimpleAliasRegistry:
    //将别名转换成符合规范的beanName
    public String canonicalName(String name) {
        String canonicalName = name;
        // Handle aliasing...
        String resolvedName;
        do {
            resolvedName = this.aliasMap.get(canonicalName);
            if (resolvedName != null) {
                canonicalName = resolvedName;
            }
        }
        while (resolvedName != null);
        return canonicalName;
    }

    //BeanFactoryUtils:
    //返回实际的bean名称，去掉工厂Bean的引用前缀&
    public static String transformedBeanName(String name) {
        Assert.notNull(name, "'name' must not be null");
        //FACTORY_BEAN_PREFIX = "&", 如果beanName没有"&"前缀，就直接返回
        if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
            return name;
        }
        //如果beanName是工厂bean, 含有"&"前缀，就去除这个前缀后返回
        return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
            do {
                beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
            }while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));

            return beanName;
        });
    }

    //AbstractBeanFactory:
    private void copyRelevantMergedBeanDefinitionCaches(RootBeanDefinition previous, RootBeanDefinition mbd) {
        if (ObjectUtils.nullSafeEquals(mbd.getBeanClassName(), previous.getBeanClassName()) &&
                ObjectUtils.nullSafeEquals(mbd.getFactoryBeanName(), previous.getFactoryBeanName()) &&
                ObjectUtils.nullSafeEquals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
            ResolvableType targetType = mbd.targetType;
            ResolvableType previousTargetType = previous.targetType;
            if (targetType == null || targetType.equals(previousTargetType)) {
                mbd.targetType = previousTargetType;
                mbd.isFactoryBean = previous.isFactoryBean;
                mbd.resolvedTargetType = previous.resolvedTargetType;
                mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
                mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
            }
        }
    }

    //DefaultListableBeanFactory:
    protected boolean isFactoryBean(String beanName, RootBeanDefinition mbd) {
        Boolean result = mbd.isFactoryBean;
        if (result == null) {
            //判定
            Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
            result = (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
            mbd.isFactoryBean = result;
        }
        return result;
    }

    //AbstractBeanFactory:
    protected Class<?> predictBeanType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = mbd.getTargetType();
        if (targetType != null) {
            return targetType;
        }
        if (mbd.getFactoryMethodName() != null) {
            return null;
        }
        return resolveBeanClass(mbd, beanName, typesToMatch);
    }

    //AbstractBeanFactory:
    //为指定的bean定义解析bean类，将bean类名解析为一个类引用(如果需要)，并将解析后的类存储在bean定义中以供进一步使用
    protected Class<?> resolveBeanClass(final RootBeanDefinition mbd, String beanName, final Class<?>... typesToMatch)
            throws CannotLoadBeanClassException {

        try {
            if (mbd.hasBeanClass()) {
                return mbd.getBeanClass();
            }
            if (System.getSecurityManager() != null) {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>) () ->
                        doResolveBeanClass(mbd, typesToMatch), getAccessControlContext());
            }else {
                return doResolveBeanClass(mbd, typesToMatch);
            }
        }catch (PrivilegedActionException pae) {
            ClassNotFoundException ex = (ClassNotFoundException) pae.getException();
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
        }catch (ClassNotFoundException ex) {
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), ex);
        }catch (LinkageError err) {
            throw new CannotLoadBeanClassException(mbd.getResourceDescription(), beanName, mbd.getBeanClassName(), err);
        }
    }

    //AbstractBeanFactory:
    private Class<?> doResolveBeanClass(RootBeanDefinition mbd, Class<?>... typesToMatch)
            throws ClassNotFoundException {

        ClassLoader beanClassLoader = getBeanClassLoader();
        ClassLoader dynamicLoader = beanClassLoader;
        boolean freshResolve = false;

        if (!ObjectUtils.isEmpty(typesToMatch)) {
            //当只是执行类型检查时(例如，还没有创建实际的实例)，使用指定的临时类装入器(例如，在织入场景中)
            ClassLoader tempClassLoader = getTempClassLoader();
            if (tempClassLoader != null) {
                dynamicLoader = tempClassLoader;
                freshResolve = true;
                if (tempClassLoader instanceof DecoratingClassLoader) {
                    DecoratingClassLoader dcl = (DecoratingClassLoader) tempClassLoader;
                    for (Class<?> typeToMatch : typesToMatch) {
                        dcl.excludeClass(typeToMatch.getName());
                    }
                }
            }
        }

        String className = mbd.getBeanClassName();
        if (className != null) {
            //计算bean定义中包含的给定字符串，可能将其解析为表达式
            Object evaluated = evaluateBeanDefinitionString(className, mbd);
            if (!className.equals(evaluated)) {
                // A dynamically resolved expression, supported as of 4.2...
                if (evaluated instanceof Class) {
                    return (Class<?>) evaluated;
                }else if (evaluated instanceof String) {
                    className = (String) evaluated;
                    freshResolve = true;
                }else {
                    throw new IllegalStateException("Invalid class name expression result: " + evaluated);
                }
            }

            if (freshResolve) {
                //在针对临时类装入器进行解析时，请尽早退出，以避免将解析后的类存储在bean定义中
                if (dynamicLoader != null) {
                    try {
                        return dynamicLoader.loadClass(className);
                    }catch (ClassNotFoundException ex) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Could not load class [" + className + "] from " + dynamicLoader + ": " + ex);
                        }
                    }
                }
                return ClassUtils.forName(className, dynamicLoader);
            }
        }

        //定期解析，将结果缓存到bean定义中
        return mbd.resolveBeanClass(beanClassLoader);
    }

    //AbstractBeanFactory:
    protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
        if (this.beanExpressionResolver == null) {
            return value;
        }

        Scope scope = null;
        if (beanDefinition != null) {
            String scopeName = beanDefinition.getScope();
            if (scopeName != null) {
                scope = getRegisteredScope(scopeName);
            }
        }
        //解析的字符串是个表达式，用bean表达式处理器进行评估
        return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
    }

    //StandardBeanExpressionResolver:
    public Object evaluate(@Nullable String value, BeanExpressionContext evalContext) throws BeansException {
        if (!StringUtils.hasLength(value)) {
            return value;
        }
        try {
            // Map<String, Expression> expressionCache = new ConcurrentHashMap<>(256)
            Expression expr = this.expressionCache.get(value);
            if (expr == null) {
                //解析表达式字符串
                expr = this.expressionParser.parseExpression(value, this.beanExpressionParserContext);
                this.expressionCache.put(value, expr);
            }
            //Map<BeanExpressionContext, StandardEvaluationContext> evaluationCache = new ConcurrentHashMap<>(8)
            StandardEvaluationContext sec = this.evaluationCache.get(evalContext);
            if (sec == null) {
                sec = new StandardEvaluationContext(evalContext);
                sec.addPropertyAccessor(new BeanExpressionContextAccessor());
                sec.addPropertyAccessor(new BeanFactoryAccessor());
                sec.addPropertyAccessor(new MapAccessor());
                sec.addPropertyAccessor(new EnvironmentAccessor());
                sec.setBeanResolver(new BeanFactoryResolver(evalContext.getBeanFactory()));
                sec.setTypeLocator(new StandardTypeLocator(evalContext.getBeanFactory().getBeanClassLoader()));
                ConversionService conversionService = evalContext.getBeanFactory().getConversionService();
                if (conversionService != null) {
                    sec.setTypeConverter(new StandardTypeConverter(conversionService));
                }
                customizeEvaluationContext(sec);
                this.evaluationCache.put(evalContext, sec);
            }
            return expr.getValue(sec);
        }
        catch (Throwable ex) {
            throw new BeanExpressionException("Expression parsing failed", ex);
        }
    }

    //TemplateAwareExpressionParser:
    public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
        if (context != null && context.isTemplate()) {
            return parseTemplate(expressionString, context);
        }else {
            return doParseExpression(expressionString, context);
        }
    }

    //AbstractBeanDefinition:
    public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
        String className = getBeanClassName();
        if (className == null) {
            return null;
        }
        //用类加载器加载类对象，初始化给定的类
        Class<?> resolvedClass = ClassUtils.forName(className, classLoader);
        this.beanClass = resolvedClass;
        return resolvedClass;
    }

    //ClassUtils:
    public static Class<?> forName(String name, @Nullable ClassLoader classLoader)
            throws ClassNotFoundException, LinkageError {

        Assert.notNull(name, "Name must not be null");

        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz == null) {
            clazz = commonClassCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }

        // "java.lang.String[]" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[Ljava.lang.String;" style arrays
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[[I" or "[[Ljava.lang.String;" style arrays
        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        ClassLoader clToUse = classLoader;
        if (clToUse == null) {
            clToUse = getDefaultClassLoader();
        }
        try {
            return Class.forName(name, false, clToUse);
        }
        catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
            if (lastDotIndex != -1) {
                String innerClassName =
                        name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
                try {
                    return Class.forName(innerClassName, false, clToUse);
                }
                catch (ClassNotFoundException ex2) {
                    // Swallow - let original exception get through
                }
            }
            throw ex;
        }
    }


    //A-1
    //DefaultListableBeanFactory:
    public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }
        return bd;
    }

    //A-2
    //AbstractBeanFactory:
    public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args)
            throws BeansException {

        return doGetBean(name, requiredType, args, false);
    }

    //B
    //AbstractBeanFactory:
    //真正实现向IoC容器获取Bean的功能，也是触发依赖注入功能的地方
    protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
                              @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

        //1>根据指定的名称获取被管理Bean的名称，剥离指定名称中对容器的相关依赖
        //如果指定的是别名，将别名转换为规范的Bean名称
        final String beanName = transformedBeanName(name);
        Object bean;

        //2>先从缓存中取是否已经有被创建过的单态类型的Bean，对于单态模式的Bean整
        //个IoC容器中只创建一次，不需要重复创建
        Object sharedInstance = getSingleton(beanName);
        //IoC容器创建单态模式Bean实例对象
        if (sharedInstance != null && args == null) {
            if (logger.isTraceEnabled()) {
                //如果指定名称的Bean在容器中已有单态模式的Bean被创建，直接返回已经创建的Bean
                if (isSingletonCurrentlyInCreation(beanName)) {
                    logger.trace("Returning eagerly cached instance of singleton bean '" + beanName +
                            "' that is not fully initialized yet - a consequence of a circular reference");
                }else {
                    logger.trace("Returning cached instance of singleton bean '" + beanName + "'");
                }
            }
            //3>获取给定Bean的实例对象，主要是完成FactoryBean的相关处理
            //注意：BeanFactory是管理容器中Bean的工厂，而FactoryBean是创建Bean对象的工厂Bean，两者之间有区别
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
        }else {
            //缓存没有正在创建的单态模式Bean
            //缓存中已经有已经创建的原型模式Bean，但是由于循环引用的问题导致实例化对象失败
            if (isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeanCurrentlyInCreationException(beanName);
            }

            //对IoC容器中是否存在指定名称的BeanDefinition进行检查，首先检查是否
            //能在当前的BeanFactory中获取的所需要的Bean，如果不能则委托当前容器
            //的父级容器去查找，如果还是找不到则沿着容器的继承体系向父级容器查找
            BeanFactory parentBeanFactory = getParentBeanFactory();
            //当前容器的父级容器存在，且当前容器中不存在指定名称的Bean
            if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
                //4>解析指定Bean名称的原始名称
                String nameToLookup = originalBeanName(name);
                if (parentBeanFactory instanceof AbstractBeanFactory) {
                    //递归委派父容器根据原始名称，类型，参数查找Bean实例
                    return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                            nameToLookup, requiredType, args, typeCheckOnly);
                }else if (args != null) {
                    //委派父级容器根据指定名称和显式的参数查找
                    return (T) parentBeanFactory.getBean(nameToLookup, args);
                }
                else if (requiredType != null) {
                    //5>委派父级容器根据指定名称和类型查找
                    return parentBeanFactory.getBean(nameToLookup, requiredType);
                }else {
                    //委派父级容器根据Bean原始名称查找
                    return (T) parentBeanFactory.getBean(nameToLookup);
                }
            }

            //创建的Bean是否需要进行类型验证，一般不需要
            if (!typeCheckOnly) {
                //6>向容器标记指定的Bean已经被创建
                markBeanAsCreated(beanName);
            }

            try {
                //根据指定Bean名称获取其父级的Bean定义，主要解决Bean继承时子类合并父类公共属性问题
                final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                checkMergedBeanDefinition(mbd, beanName, args);

                //获取当前Bean所有依赖Bean的名称
                String[] dependsOn = mbd.getDependsOn();
                //如果当前Bean有依赖Bean
                if (dependsOn != null) {
                    for (String dep : dependsOn) {
                        //7>如果当前bean和依赖bean之间具有循环依赖关系，抛出异常
                        if (isDependent(beanName, dep)) {
                            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                    "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                        }
                        //8>把被依赖Bean注册给当前依赖的Bean
                        registerDependentBean(dep, beanName);
                        try {
                            //递归调用getBean方法，获取当前Bean的依赖Bean
                            getBean(dep);
                        }catch (NoSuchBeanDefinitionException ex) {
                            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                    "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                        }
                    }
                }

                //创建单态模式Bean的实例对象
                if (mbd.isSingleton()) {
                    //这里使用了一个匿名内部类，创建Bean实例对象，并且注册给所依赖的对象
                    sharedInstance = getSingleton(beanName, () -> {
                        try {
                            //9>创建一个指定Bean实例对象，如果有父级继承，则合并子类和父类的定义
                            return createBean(beanName, mbd, args);
                        }catch (BeansException ex) {
                            //显式地从容器单态模式Bean缓存中清除实例对象
                            destroySingleton(beanName);
                            throw ex;
                        }
                    });
                    //获取给定Bean的实例对象
                    bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
                }else if (mbd.isPrototype()) { //IoC容器创建原型模式Bean实例对象
                    //原型模式(Prototype)是每次都会创建一个新的对象
                    Object prototypeInstance = null;
                    try {
                        //回调beforePrototypeCreation方法，默认的功能是注册当前创建的原型对象
                        beforePrototypeCreation(beanName);
                        //创建指定Bean对象实例
                        prototypeInstance = createBean(beanName, mbd, args);
                    }finally {
                        //回调afterPrototypeCreation方法，默认的功能告诉IoC容器指定Bean的原型对象不再创建了
                        afterPrototypeCreation(beanName);
                    }
                    //获取给定Bean的实例对象
                    bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
                }else {
                    //要创建的Bean既不是单态模式，也不是原型模式，则根据Bean定义资源中配置的生命周期范围，
                    //选择实例化Bean的合适方法，这种在Web应用程序中比较常用，如：request、session、application等生命周期

                    String scopeName = mbd.getScope();
                    final Scope scope = this.scopes.get(scopeName);
                    //Bean定义资源中没有配置生命周期范围，则Bean定义不合法
                    if (scope == null) {
                        throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                    }
                    try {
                        //这里又使用了一个匿名内部类，获取一个指定生命周期范围的实例
                        Object scopedInstance = scope.get(beanName, () -> {
                            beforePrototypeCreation(beanName);
                            try {
                                return createBean(beanName, mbd, args);
                            }finally {
                                afterPrototypeCreation(beanName);
                            }
                        });
                        //获取给定Bean的实例对象
                        bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                    }catch (IllegalStateException ex) {
                        throw new BeanCreationException(beanName,
                                "Scope '" + scopeName + "' is not active for the current thread; consider " +
                                        "defining a scoped proxy for this bean if you intend to refer to it from a singleton",ex);
                    }
                }
            }catch (BeansException ex) {
                cleanupAfterBeanCreationFailure(beanName);
                throw ex;
            }
        }

        //检查所需的类型是否与实际bean实例的类型匹配
        if (requiredType != null && !requiredType.isInstance(bean)) {
            try {
                T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
                if (convertedBean == null) {
                    throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
                }
                return convertedBean;
            }catch (TypeMismatchException ex) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Failed to convert bean '" + name + "' to required type '" +
                            ClassUtils.getQualifiedName(requiredType) + "'", ex);
                }
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
        }
        return (T) bean;
    }


    //StaticListableBeanFactory:
    public Object getBean(String name, Object... args) throws BeansException {
        if (!ObjectUtils.isEmpty(args)) {
            throw new UnsupportedOperationException(
                    "StaticListableBeanFactory does not support explicit bean creation arguments");
        }
        return getBean(name);
    }

    //StaticListableBeanFactory:
    public <T> T getBean(String name, @Nullable Class<T> requiredType) throws BeansException {
        Object bean = getBean(name);
        if (requiredType != null && !requiredType.isInstance(bean)) {
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
        return (T) bean;
    }

    //StaticListableBeanFactory:
    public Object getBean(String name) throws BeansException {
        String beanName = BeanFactoryUtils.transformedBeanName(name);
        //Map<String, Object> beans; 从bean名称映射到bean实例
        Object bean = this.beans.get(beanName);

        if (bean == null) {
            throw new NoSuchBeanDefinitionException(beanName,
                    "Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
        }

        //如果bean不是工厂，不要让调用代码尝试取消对bean工厂的引用
        if (BeanFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryBean)) {
            throw new BeanIsNotAFactoryException(beanName, bean.getClass());
        }

        if (bean instanceof FactoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
            try {
                Object exposedObject = ((FactoryBean<?>) bean).getObject();
                if (exposedObject == null) {
                    throw new BeanCreationException(beanName, "FactoryBean exposed null object");
                }
                return exposedObject;
            }catch (Exception ex) {
                throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
            }
        }else {
            return bean;
        }
    }




    //JndiLocatorSupport:
    //通过JndiTemplate对给定名称执行实际的JNDI查找。
    //如果名称不是以“java:comp/env/”开头，则在“resourceRef”设置为“true”时添加此前缀。
    protected <T> T lookup(String jndiName, @Nullable Class<T> requiredType) throws NamingException {
        Assert.notNull(jndiName, "'jndiName' must not be null");
        String convertedName = convertJndiName(jndiName);
        T jndiObject;
        try {
            jndiObject = getJndiTemplate().lookup(convertedName, requiredType);
        }catch (NamingException ex) {
            if (!convertedName.equals(jndiName)) {
                //尝试退回到最初指定的名称
                if (logger.isDebugEnabled()) {
                    logger.debug("Converted JNDI name [" + convertedName +
                            "] not found - trying original name [" + jndiName + "]. " + ex);
                }
                jndiObject = getJndiTemplate().lookup(jndiName, requiredType);
            }else {
                throw ex;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Located object with JNDI name [" + convertedName + "]");
        }
        return jndiObject;
    }

    //DefaultSingletonBeanRegistry:
    public Object getSingleton(String beanName) {
        return getSingleton(beanName, true);
    }

    //DefaultSingletonBeanRegistry:
    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        //Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256)
        //单例对象的缓存:从bean名到bean实例
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                //Map<String, Object> earlySingletonObjects = new HashMap<>(16)
                //早期单例对象的缓存:从bean名到bean实例
                singletonObject = this.earlySingletonObjects.get(beanName);
                if (singletonObject == null && allowEarlyReference) {
                    //Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16)
                    //单例工厂的缓存:从bean名到ObjectFactory
                    ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        singletonObject = singletonFactory.getObject();
                        this.earlySingletonObjects.put(beanName, singletonObject);
                        this.singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return singletonObject;
    }


    //B-3
    //AbstractBeanFactory:
    //获取给定bean实例的对象，对于FactoryBean，要么是bean实例本身，要么是它创建的对象
    protected Object getObjectForBeanInstance(
            Object beanInstance, String name, String beanName, @Nullable RootBeanDefinition mbd) {

        //1>如果bean不是工厂，不要让调用代码尝试取消对工厂的引用
        if (BeanFactoryUtils.isFactoryDereference(name)) {
            if (beanInstance instanceof NullBean) {
                return beanInstance;
            }
            if (!(beanInstance instanceof FactoryBean)) {
                throw new BeanIsNotAFactoryException(beanName, beanInstance.getClass());
            }
            if (mbd != null) {
                mbd.isFactoryBean = true;
            }
            return beanInstance;
        }

        //现在我们有了bean实例，它可以是一个普通的bean，也可以是一个FactoryBean。
        //如果它是一个FactoryBean，我们将使用它来创建一个bean实例，除非调用者确实需要对工厂的引用
        if (!(beanInstance instanceof FactoryBean)) {
            return beanInstance;
        }

        Object object = null;
        if (mbd != null) {
            mbd.isFactoryBean = true;
        }else {
            //2>从给定FactoryBean获取缓存的对象(如果以缓存形式可用)。快速检查最小同步
            object = getCachedObjectForFactoryBean(beanName);
        }
        if (object == null) {
            //从工厂里返回bean实例
            FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
            //缓存从FactoryBean获得的对象(如果它是单例的话)
            if (mbd == null && containsBeanDefinition(beanName)) {
                mbd = getMergedLocalBeanDefinition(beanName);
            }
            boolean synthetic = (mbd != null && mbd.isSynthetic());
            //3>从FactoryBean获取对象
            object = getObjectFromFactoryBean(factory, beanName, !synthetic);
        }
        return object;
    }

    //BeanFactoryUtils:
    public static boolean isFactoryDereference(@Nullable String name) {
        //FACTORY_BEAN_PREFIX = "&"
        return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
    }

    //FactoryBeanRegistrySupport:
    protected Object getCachedObjectForFactoryBean(String beanName) {
        //Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16)
        //由FactoryBean创建的单例对象的缓存:从FactoryBean名称到对象
        return this.factoryBeanObjectCache.get(beanName);
    }

    //FactoryBeanRegistrySupport:
    //获取要从给定的FactoryBean公开的对象
    protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
        if (factory.isSingleton() && containsSingleton(beanName)) {
            synchronized (getSingletonMutex()) {
                //Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16)
                //由FactoryBean创建的单例对象的缓存:从FactoryBean名称到对象
                Object object = this.factoryBeanObjectCache.get(beanName);
                if (object == null) {
                    //
                    object = doGetObjectFromFactoryBean(factory, beanName);
                    //如果在上面的getObject()调用期间还没有将数据存储到内存中，则只对数据进行后处理
                    //(例如，由于自定义getBean调用触发的循环引用处理)
                    Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
                    if (alreadyThere != null) {
                        object = alreadyThere;
                    } else {
                        if (shouldPostProcess) {
                            if (isSingletonCurrentlyInCreation(beanName)) {
                                // Temporarily return non-post-processed object, not storing it yet..
                                return object;
                            }
                            //在单例创建之前回调
                            beforeSingletonCreation(beanName);
                            try {
                                //对从FactoryBean获得的给定对象进行后处理。结果对象为bean引用
                                object = postProcessObjectFromFactoryBean(object, beanName);
                            }catch (Throwable ex) {
                                throw new BeanCreationException(beanName,
                                        "Post-processing of FactoryBean's singleton object failed", ex);
                            }finally {
                                //在单例创建之后回调
                                afterSingletonCreation(beanName);
                            }
                        }
                        if (containsSingleton(beanName)) {
                            this.factoryBeanObjectCache.put(beanName, object);
                        }
                    }
                }
                return object;
            }
        }else {
            Object object = doGetObjectFromFactoryBean(factory, beanName);
            if (shouldPostProcess) {
                try {
                    //对从FactoryBean获得的给定对象进行后处理。结果对象为bean引用
                    object = postProcessObjectFromFactoryBean(object, beanName);
                } catch (Throwable ex) {
                    throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
                }
            }
            return object;
        }
    }

    //FactoryBeanRegistrySupport:
    private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
            throws BeanCreationException {
        Object object;
        try {
            if (System.getSecurityManager() != null) {
                AccessControlContext acc = getAccessControlContext();
                try {
                    object = AccessController.doPrivileged((PrivilegedExceptionAction<Object>) factory::getObject, acc);
                }catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            }else {
                object = factory.getObject();
            }
        }catch (FactoryBeanNotInitializedException ex) {
            throw new BeanCurrentlyInCreationException(beanName, ex.toString());
        }catch (Throwable ex) {
            throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
        }

        //不接受还没有完全初始化的FactoryBean的空值:许多FactoryBean只返回null
        if (object == null) {
            if (isSingletonCurrentlyInCreation(beanName)) {
                throw new BeanCurrentlyInCreationException(
                        beanName, "FactoryBean which is currently in creation returned null from getObject");
            }
            object = new NullBean();
        }
        return object;
    }

    //B-4
    //AbstractBeanFactory:
    protected String originalBeanName(String name) {
        String beanName = transformedBeanName(name);
        //FACTORY_BEAN_PREFIX = "&"
        if (name.startsWith(FACTORY_BEAN_PREFIX)) {
            beanName = FACTORY_BEAN_PREFIX + beanName;
        }
        return beanName;
    }

    //B-5
    //SimpleJndiBeanFactory:
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        try {
            if (isSingleton(name)) {
                return doGetSingleton(name, requiredType);
            }else {
                return lookup(name, requiredType);
            }
        }catch (NameNotFoundException ex) {
            throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
        }catch (TypeMismatchNamingException ex) {
            throw new BeanNotOfRequiredTypeException(name, ex.getRequiredType(), ex.getActualType());
        }catch (NamingException ex) {
            throw new BeanDefinitionStoreException("JNDI environment", name, "JNDI lookup failed", ex);
        }
    }

    //SimpleJndiBeanFactory:
    private <T> T doGetSingleton(String name, @Nullable Class<T> requiredType) throws NamingException {
        synchronized (this.singletonObjects) {
            //Map<String, Object> singletonObjects = new HashMap<>()
            //可共享单例对象的缓存:bean名称到bean实例
            Object singleton = this.singletonObjects.get(name);
            if (singleton != null) {
                if (requiredType != null && !requiredType.isInstance(singleton)) {
                    throw new TypeMismatchNamingException(convertJndiName(name), requiredType, singleton.getClass());
                }
                return (T) singleton;
            }
            T jndiObject = lookup(name, requiredType);
            this.singletonObjects.put(name, jndiObject);
            return jndiObject;
        }
    }

    //B-6
    //AbstractBeanFactory:
    //将指定的bean标记为已经创建(或即将创建)。这允许bean工厂对其缓存进行优化，以重复创建指定的bean
    protected void markBeanAsCreated(String beanName) {
        if (!this.alreadyCreated.contains(beanName)) {
            synchronized (this.mergedBeanDefinitions) {
                //Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256))
                //至少已经创建过一次的bean的名称
                if (!this.alreadyCreated.contains(beanName)) {
                    //让bean定义重新合并，现在我们实际上是在创建bean…以防它的一些元数据同时改变
                    clearMergedBeanDefinition(beanName);
                    this.alreadyCreated.add(beanName);
                }
            }
        }
    }

    //AbstractBeanFactory:
    protected void clearMergedBeanDefinition(String beanName) {
        //Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256)
        //从bean名称映射到合并的RootBeanDefinition
        RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
        if (bd != null) {
            bd.stale = true;
        }
    }

    //DefaultSingletonBeanRegistry:
    protected boolean isDependent(String beanName, String dependentBeanName) {
        synchronized (this.dependentBeanMap) {
            return isDependent(beanName, dependentBeanName, null);
        }
    }

    //DefaultSingletonBeanRegistry:
    private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
        if (alreadySeen != null && alreadySeen.contains(beanName)) {
            return false;
        }
        String canonicalName = canonicalName(beanName);
        //Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64)
        //依赖bean名称之间的映射:从bean名称映射到依赖bean名称集
        Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
        if (dependentBeans == null) {
            return false;
        }
        if (dependentBeans.contains(dependentBeanName)) {
            return true;
        }
        for (String transitiveDependency : dependentBeans) {
            if (alreadySeen == null) {
                alreadySeen = new HashSet<>();
            }
            alreadySeen.add(beanName);
            if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
                return true;
            }
        }
        return false;
    }

    //DefaultSingletonBeanRegistry:
    //为给定bean注册一个从属bean，在销毁给定bean之前销毁它
    public void registerDependentBean(String beanName, String dependentBeanName) {
        String canonicalName = canonicalName(beanName);

        //Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64)
        //依赖bean名称之间的映射:从bean名称映射到依赖bean名称集
        synchronized (this.dependentBeanMap) {
            Set<String> dependentBeans =
                    this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
            if (!dependentBeans.add(dependentBeanName)) {
                return;
            }
        }

        //Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64)
        //依赖bean名称之间映射:bean名称到bean依赖项的bean名称集
        synchronized (this.dependenciesForBeanMap) {
            Set<String> dependenciesForBean =
                    this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
            dependenciesForBean.add(canonicalName);
        }
    }


    //B-9
    //AbstractAutowireCapableBeanFactory:
    //创建Bean实例对象
    protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
            throws BeanCreationException {

        if (logger.isTraceEnabled()) {
            logger.trace("Creating instance of bean '" + beanName + "'");
        }
        RootBeanDefinition mbdToUse = mbd;

        //判断需要创建的Bean是否可以实例化，即是否可以通过当前的类加载器加载
        Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            mbdToUse = new RootBeanDefinition(mbd);
            mbdToUse.setBeanClass(resolvedClass);
        }

        //1>校验和准备Bean中的方法覆盖
        try {
            mbdToUse.prepareMethodOverrides();
        }catch (BeanDefinitionValidationException ex) {
            throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                    beanName, "Validation of method overrides failed", ex);
        }

        try {
            //2>如果Bean配置了初始化前和初始化后的处理器，则试图返回一个需要创建Bean的代理对象
            Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
            if (bean != null) {
                return bean;
            }
        }catch (Throwable ex) {
            throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                    "BeanPostProcessor before instantiation of bean failed", ex);
        }

        try {
            //3>创建Bean的入口
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
            if (logger.isTraceEnabled()) {
                logger.trace("Finished creating instance of bean '" + beanName + "'");
            }
            return beanInstance;
        }catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
            // A previously detected exception with proper bean creation context already,
            // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
            throw ex;
        }catch (Throwable ex) {
            throw new BeanCreationException(
                    mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
        }
    }

    //B-9-1
    //AbstractBeanDefinition:
    //验证并准备为该bean定义的方法覆盖。检查是否存在具有指定名称的方法。
    public void prepareMethodOverrides() throws BeanDefinitionValidationException {
        // Check that lookup methods exist and determine their overloaded status.
        if (hasMethodOverrides()) {
            getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
        }
    }

    //AbstractBeanDefinition:
    protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
        int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
        if (count == 0) {
            throw new BeanDefinitionValidationException(
                    "Invalid method override: no method with name '" + mo.getMethodName() +
                            "' on class [" + getBeanClassName() + "]");
        }else if (count == 1) {
            //将override标记为未重载，以避免arg类型检查的开销
            mo.setOverloaded(false);
        }
    }

    //ClassUtils:
    public static int getMethodCountForName(Class<?> clazz, String methodName) {
        Assert.notNull(clazz, "Class must not be null");
        Assert.notNull(methodName, "Method name must not be null");
        int count = 0;
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (Method method : declaredMethods) {
            if (methodName.equals(method.getName())) {
                count++;
            }
        }
        Class<?>[] ifcs = clazz.getInterfaces();
        for (Class<?> ifc : ifcs) {
            count += getMethodCountForName(ifc, methodName);
        }
        if (clazz.getSuperclass() != null) {
            count += getMethodCountForName(clazz.getSuperclass(), methodName);
        }
        return count;
    }

    //B-9-2
    //AbstractAutowireCapableBeanFactory:
    //应用实例化前后处理程序，解决指定bean是否有实例化前快捷方式
    protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {
        Object bean = null;
        if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {
            //确保此时bean类已经被解析
            if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
                //1>预测bean的类型
                Class<?> targetType = determineTargetType(beanName, mbd);
                if (targetType != null) {
                    //2>应用InstantiationAwareBeanPostProcessors,调用它的postProcessBeforeInstantiation方法
                    bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                    if (bean != null) {
                        //应用BeanPostProcessor的初始化之后方法
                        bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
                    }
                }
            }
            mbd.beforeInstantiationResolved = (bean != null);
        }
        return bean;
    }

    //AbstractAutowireCapableBeanFactory:
    protected Class<?> determineTargetType(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = mbd.getTargetType();
        if (targetType == null) {
            targetType = (mbd.getFactoryMethodName() != null ?
                    getTypeForFactoryMethod(beanName, mbd, typesToMatch) :
                    resolveBeanClass(mbd, beanName, typesToMatch));
            if (ObjectUtils.isEmpty(typesToMatch) || getTempClassLoader() == null) {
                mbd.resolvedTargetType = targetType;
            }
        }
        return targetType;
    }

    //AbstractAutowireCapableBeanFactory:
    //确定基于工厂方法的给定bean定义的目标类型。只有在没有为目标bean注册单例实例时才调用
    protected Class<?> getTypeForFactoryMethod(String beanName, RootBeanDefinition mbd, Class<?>... typesToMatch) {
        ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
        if (cachedReturnType != null) {
            return cachedReturnType.resolve();
        }

        Class<?> commonType = null;
        Method uniqueCandidate = mbd.factoryMethodToIntrospect;//缺省的工厂方法

        if (uniqueCandidate == null) {
            Class<?> factoryClass;
            boolean isStatic = true;

            String factoryBeanName = mbd.getFactoryBeanName();
            if (factoryBeanName != null) {
                if (factoryBeanName.equals(beanName)) {
                    throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                            "factory-bean reference points back to the same bean definition");
                }
                //检查工厂类上声明的工厂方法返回类型
                factoryClass = getType(factoryBeanName);
                isStatic = false;
            } else {
                //检查bean类的声明工厂方法返回类型
                factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
            }

            if (factoryClass == null) {
                return null;
            }
            factoryClass = ClassUtils.getUserClass(factoryClass);

            //如果所有工厂方法具有相同的返回类型，则返回该类型。由于类型转换/自动装配，无法清楚地找出准确的方法!
            int minNrOfArgs =
                    (mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
            Method[] candidates = this.factoryMethodCandidateCache.computeIfAbsent(factoryClass,
                    clazz -> ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS));

            for (Method candidate : candidates) {
                if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) &&
                        candidate.getParameterCount() >= minNrOfArgs) {
                    // Declared type variables to inspect?
                    if (candidate.getTypeParameters().length > 0) {
                        try {
                            //完全解析参数名和参数值。
                            Class<?>[] paramTypes = candidate.getParameterTypes();
                            String[] paramNames = null;
                            ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
                            if (pnd != null) {
                                paramNames = pnd.getParameterNames(candidate);
                            }
                            ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
                            Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
                            Object[] args = new Object[paramTypes.length];
                            for (int i = 0; i < args.length; i++) {
                                ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
                                        i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
                                if (valueHolder == null) {
                                    valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
                                }
                                if (valueHolder != null) {
                                    args[i] = valueHolder.getValue();
                                    usedValueHolders.add(valueHolder);
                                }
                            }
                            Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(candidate, args, getBeanClassLoader());
                            uniqueCandidate = (commonType == null && returnType == candidate.getReturnType() ? candidate : null);
                            commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
                            if (commonType == null) {
                                //发现模糊的返回类型:返回null表示“不确定”
                                return null;
                            }
                        }catch (Throwable ex) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Failed to resolve generic return type for factory method: " + ex);
                            }
                        }
                    }else {
                        uniqueCandidate = (commonType == null ? candidate : null);
                        commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
                        if (commonType == null) {
                            //发现模糊的返回类型:返回null表示“不确定”
                            return null;
                        }
                    }
                }
            }

            mbd.factoryMethodToIntrospect = uniqueCandidate;
            if (commonType == null) {
                return null;
            }
        }

        //发现的公共返回类型:所有工厂方法返回相同的类型。对于非参数化唯一候选对象，缓存目标工厂方法的完整类型声明上下文
        cachedReturnType = (uniqueCandidate != null ?
                ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
        mbd.factoryMethodReturnType = cachedReturnType;
        return cachedReturnType.resolve();
    }

    //AbstractAutowireCapableBeanFactory:
    //将InstantiationAwareBeanPostProcessors应用到指定的bean定义(通过类和名称)，调用它的{postProcessBeforeInstantiation}方法。
    //任何返回的对象都将被用作bean，而不是实际实例化目标bean。来自后处理器的{@code null}返回值将导致目标bean被实例化。
    protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    //AbstractAutowireCapableBeanFactory:
    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
            throws BeansException {
        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    //AbstractBeanFactory:
    //返回将应用于此工厂创建的bean的beanpostprocessor列表
    public List<BeanPostProcessor> getBeanPostProcessors() {
        //List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>()
        //应用于创建bean的BeanPostProcessors列表
        return this.beanPostProcessors;
    }

    //B-9-3
    //AbstractAutowireCapableBeanFactory:
    //真正创建Bean的方法
    protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
            throws BeanCreationException {

        //封装被创建的Bean对象
        BeanWrapper instanceWrapper = null;
        if (mbd.isSingleton()) { //单态模式的Bean，先从容器缓存中获取同名Bean
            //ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>()
            //未完成的FactoryBean实例的缓存:从FactoryBean名称到BeanWrapper
            instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
        }
        if (instanceWrapper == null) {
            //1>创建实例对象
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }
        final Object bean = instanceWrapper.getWrappedInstance();
        //获取实例化对象的类型
        Class<?> beanType = instanceWrapper.getWrappedClass();
        if (beanType != NullBean.class) {
            mbd.resolvedTargetType = beanType;
        }

        //调用PostProcessor后置处理器
        synchronized (mbd.postProcessingLock) {
            if (!mbd.postProcessed) {
                try {
                    applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
                }catch (Throwable ex) {
                    throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                            "Post-processing of merged bean definition failed", ex);
                }
                mbd.postProcessed = true;
            }
        }

        //向容器中缓存单态模式的Bean对象，以防循环引用
        boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                isSingletonCurrentlyInCreation(beanName));
        if (earlySingletonExposure) {
            if (logger.isTraceEnabled()) {
                logger.trace("Eagerly caching bean '" + beanName +
                        "' to allow for resolving potential circular references");
            }
            //这里是一个匿名内部类，为了防止循环引用，尽早持有对象的引用
            addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
        }

        //Bean对象的初始化，依赖注入在此触发
        //这个exposedObject在初始化完成之后返回作为依赖注入完成后的Bean
        Object exposedObject = bean;
        try {
            //将Bean实例对象封装，并且Bean定义中配置的属性值赋值给实例对象
            populateBean(beanName, mbd, instanceWrapper);
            //初始化Bean对象
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        }catch (Throwable ex) {
            if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
                throw (BeanCreationException) ex;
            }else {
                throw new BeanCreationException(
                        mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
            }
        }

        if (earlySingletonExposure) {
            //获取指定名称的已注册的单态模式Bean对象
            Object earlySingletonReference = getSingleton(beanName, false);
            if (earlySingletonReference != null) {
                ////根据名称获取的已注册的Bean和正在实例化的Bean是同一个
                if (exposedObject == bean) {
                    exposedObject = earlySingletonReference;
                }
                //当前Bean依赖其他Bean，并且当发生循环引用时不允许新创建实例对象
                else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                    //获取当前Bean所依赖的其他Bean
                    String[] dependentBeans = getDependentBeans(beanName);
                    Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                    for (String dependentBean : dependentBeans) {
                        //对依赖Bean进行类型检查
                        if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                            actualDependentBeans.add(dependentBean);
                        }
                    }
                    if (!actualDependentBeans.isEmpty()) {
                        throw new BeanCurrentlyInCreationException(beanName,
                                "Bean with name '" + beanName + "' has been injected into other beans [" +
                                        StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                                        "] in its raw version as part of a circular reference, but has eventually been " +
                                        "wrapped. This means that said other beans do not use the final version of the " +
                                        "bean. This is often the result of over-eager type matching - consider using " +
                                        "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                    }
                }
            }
        }

        //注册完成依赖注入的Bean
        try {
            registerDisposableBeanIfNecessary(beanName, bean, mbd);
        }catch (BeanDefinitionValidationException ex) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
        }

        return exposedObject;
    }

    //B-9-3-1
    //AbstractAutowireCapableBeanFactory:
    //使用适当的实例化策略为指定的bean创建一个新实例:工厂方法、构造函数自动装配或简单实例化
    protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
        //检查确认Bean是可实例化的
        Class<?> beanClass = resolveBeanClass(mbd, beanName);

        //使用工厂方法对Bean进行实例化
        if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                    "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
        }

        //1>获取创建bean实例的回调
        Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
        if (instanceSupplier != null) {
            //从给定的供应者获取一个bean实例
            return obtainFromSupplier(instanceSupplier, beanName);
        }

        if (mbd.getFactoryMethodName() != null) {
            //调用工厂方法实例化
            return instantiateUsingFactoryMethod(beanName, mbd, args);
        }

        //使用容器的自动装配方法进行实例化
        boolean resolved = false;
        boolean autowireNecessary = false;
        if (args == null) {
            synchronized (mbd.constructorArgumentLock) {
                if (mbd.resolvedConstructorOrFactoryMethod != null) {
                    resolved = true;
                    autowireNecessary = mbd.constructorArgumentsResolved;
                }
            }
        }
        if (resolved) {
            if (autowireNecessary) {
                //配置了自动装配属性，使用容器的自动装配实例化,容器的自动装配是根据参数类型匹配Bean的构造方法
                return autowireConstructor(beanName, mbd, null, null);
            }else {
                //使用默认的无参构造方法实例化
                return instantiateBean(beanName, mbd);
            }
        }

        //使用Bean的构造方法进行实例化
        Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
        if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
                mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
            //使用容器的自动装配特性，调用匹配的构造方法实例化
            return autowireConstructor(beanName, mbd, ctors, args);
        }

        // Preferred constructors for default construction?
        ctors = mbd.getPreferredConstructors();
        if (ctors != null) {
            //使用容器的自动装配特性，调用默认首选的构造方法
            return autowireConstructor(beanName, mbd, ctors, null);
        }

        //使用默认的无参构造方法实例化
        return instantiateBean(beanName, mbd);
    }

    //返回创建bean实例的回调(如果有的话)
    public Supplier<?> getInstanceSupplier() {
        //Supplier<?> instanceSupplier
        return this.instanceSupplier;
    }

    //AbstractAutowireCapableBeanFactory:
    protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {
        Object instance;

        //NamedThreadLocal<String> currentlyCreatedBean = new NamedThreadLocal<>("Currently created bean")
        //当前创建的bean的名称，用于从用户指定的供应者回调中触发的getBean等调用的隐式依赖项注册
        String outerBean = this.currentlyCreatedBean.get();
        this.currentlyCreatedBean.set(beanName);
        try {
            instance = instanceSupplier.get();
        }finally {
            if (outerBean != null) {
                this.currentlyCreatedBean.set(outerBean);
            }else {
                this.currentlyCreatedBean.remove();
            }
        }

        if (instance == null) {
            instance = new NullBean();
        }
        //1>初始化给定的BeanWrapper
        BeanWrapper bw = new BeanWrapperImpl(instance);
        initBeanWrapper(bw);
        return bw;
    }

    //AbstractBeanFactory:
    //使用在该工厂注册的自定义编辑器初始化给定的BeanWrapper。调用将创建和填充bean实例的BeanWrappers
    protected void initBeanWrapper(BeanWrapper bw) {
        bw.setConversionService(getConversionService());
        registerCustomEditors(bw);
    }

    //AbstractBeanFactory:
    //使用在这个BeanFactory中注册的自定义编辑器初始化给定的PropertyEditorRegistry。用于创建和填充bean实例的BeanWrappers，
    //以及用于构造函数参数和工厂方法类型转换的SimpleTypeConverter。
    protected void registerCustomEditors(PropertyEditorRegistry registry) {
        PropertyEditorRegistrySupport registrySupport =
                (registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
        if (registrySupport != null) {
            registrySupport.useConfigValueEditors();
        }
        if (!this.propertyEditorRegistrars.isEmpty()) {
            //Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4)
            //应用在这个工厂bean的自定义编辑注册器
            for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
                try {
                    registrar.registerCustomEditors(registry);
                }catch (BeanCreationException ex) {
                    Throwable rootCause = ex.getMostSpecificCause();
                    if (rootCause instanceof BeanCurrentlyInCreationException) {
                        BeanCreationException bce = (BeanCreationException) rootCause;
                        String bceBeanName = bce.getBeanName();
                        if (bceBeanName != null && isCurrentlyInCreation(bceBeanName)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("PropertyEditorRegistrar [" + registrar.getClass().getName() +
                                        "] failed because it tried to obtain currently created bean '" +
                                        ex.getBeanName() + "': " + ex.getMessage());
                            }
                            onSuppressedException(ex);
                            continue;
                        }
                    }
                    throw ex;
                }
            }
        }
        if (!this.customEditors.isEmpty()) {
            this.customEditors.forEach((requiredType, editorClass) ->
                    registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass)));
        }
    }

    //ResourceEditorRegistrar:
    //使用以下资源编辑器填充给定的{registry}: ResourceEditor、InputStreamEditor、
    //InputSourceEditor、FileEditor、URLEditor、URIEditor、ClassEditor、ClassArrayEditor
    //如果此注册器配置了{ResourcePatternResolver}，则还将注册一个ResourceArrayPropertyEditor
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
        doRegisterEditor(registry, Resource.class, baseEditor);
        doRegisterEditor(registry, ContextResource.class, baseEditor);
        doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
        doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
        doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
        doRegisterEditor(registry, Path.class, new PathEditor(baseEditor));
        doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
        doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

        ClassLoader classLoader = this.resourceLoader.getClassLoader();
        doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
        doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
        doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

        if (this.resourceLoader instanceof ResourcePatternResolver) {
            doRegisterEditor(registry, Resource[].class,
                    new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
        }
    }

    //ResourceEditorRegistrar:
    private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
        if (registry instanceof PropertyEditorRegistrySupport) {
            //覆盖默认的编辑器
            ((PropertyEditorRegistrySupport) registry).overrideDefaultEditor(requiredType, editor);
        }else {
            //注册自定义的编辑器
            registry.registerCustomEditor(requiredType, editor);
        }
    }

    //PropertyEditorRegistrySupport:
    //使用给定的属性编辑器覆盖指定类型的默认编辑器。注意，这与注册自定义编辑器不同，因为编辑器在语义上
    //仍然是默认编辑器。ConversionService将覆盖此类默认编辑器，而自定义编辑器通常会覆盖ConversionService
    public void overrideDefaultEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        if (this.overriddenDefaultEditors == null) {
            this.overriddenDefaultEditors = new HashMap<>();
        }
        this.overriddenDefaultEditors.put(requiredType, propertyEditor);
    }

    //PropertyEditorRegistrySupport:
    public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        registerCustomEditor(requiredType, null, propertyEditor);
    }

    //PropertyEditorRegistrySupport:
    public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor) {
        if (requiredType == null && propertyPath == null) {
            throw new IllegalArgumentException("Either requiredType or propertyPath is required");
        }
        if (propertyPath != null) {
            if (this.customEditorsForPath == null) {
                this.customEditorsForPath = new LinkedHashMap<>(16);
            }
            this.customEditorsForPath.put(propertyPath, new PropertyEditorRegistrySupport.CustomEditorHolder(propertyEditor, requiredType));
        }else {
            if (this.customEditors == null) {
                this.customEditors = new LinkedHashMap<>(16);
            }
            this.customEditors.put(requiredType, propertyEditor);
            this.customEditorCache = null;
        }
    }


    //ConstructorResolver:
    //使用命名工厂方法实例化bean。如果mbd参数指定了一个类，而不是factoryBean或使用依赖项注入配置的
    //工厂对象本身上的实例变量，则该方法可能是静态的
    protected BeanWrapper instantiateUsingFactoryMethod(
            String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {
        /**
         * public ConstructorResolver(AbstractAutowireCapableBeanFactory beanFactory) {
         * 		this.beanFactory = beanFactory;
         * 		this.logger = beanFactory.getLogger();
         * }
         */
        return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
    }


    //ConstructorResolver:
    //使用命名工厂方法实例化bean。如果bean定义参数指定了一个类，而不是“factory-bean”，或使用依赖项注入配置的工厂对象本身的
    //实例变量，则该方法可能是静态的。实现需要使用在RootBeanDefinition中指定的名称迭代静态方法或实例方法(方法可能被重载)，并尝试与参数匹配。
    //我们没有将类型附加到构造函数args，因此尝试和错误是唯一的方法。explicitArgs数组可以包含通过相应的getBean方法以编程方式传入的参数值。
    public BeanWrapper instantiateUsingFactoryMethod(
            String beanName, RootBeanDefinition mbd, @Nullable Object[] explicitArgs) {

        BeanWrapperImpl bw = new BeanWrapperImpl();
        this.beanFactory.initBeanWrapper(bw);

        Object factoryBean;
        Class<?> factoryClass;
        boolean isStatic;

        String factoryBeanName = mbd.getFactoryBeanName();
        if (factoryBeanName != null) {
            //工厂bean引用名称和beanName相同时，抛出异常
            if (factoryBeanName.equals(beanName)) {
                throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                        "factory-bean reference points back to the same bean definition");
            }
            //递归调用BeanFactory的getBean()方法
            factoryBean = this.beanFactory.getBean(factoryBeanName);
            if (mbd.isSingleton() && this.beanFactory.containsSingleton(beanName)) {
                throw new ImplicitlyAppearedSingletonException();
            }
            factoryClass = factoryBean.getClass();
            isStatic = false;
        }else {
            //它是bean类上的一个静态工厂方法
            if (!mbd.hasBeanClass()) {
                throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName,
                        "bean definition declares neither a bean class nor a factory-bean reference");
            }
            factoryBean = null;
            factoryClass = mbd.getBeanClass();
            isStatic = true;
        }

        Method factoryMethodToUse = null;
        ConstructorResolver.ArgumentsHolder argsHolderToUse = null;
        Object[] argsToUse = null;

        if (explicitArgs != null) {
            argsToUse = explicitArgs;
        } else {
            Object[] argsToResolve = null;
            synchronized (mbd.constructorArgumentLock) {
                factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod;
                if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) {
                    //发现一个缓存的工厂方法
                    argsToUse = mbd.resolvedConstructorArguments;
                    if (argsToUse == null) {
                        argsToResolve = mbd.preparedConstructorArguments;
                    }
                }
            }
            if (argsToResolve != null) {
                //1>解析准备好的参数
                argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve, true);
            }
        }

        if (factoryMethodToUse == null || argsToUse == null) {
            //需要确定工厂方，尝试使用此名称的所有方法，看看它们是否与给定的参数匹配。
            factoryClass = ClassUtils.getUserClass(factoryClass);

            List<Method> candidateList = null;
            if (mbd.isFactoryMethodUnique) {
                if (factoryMethodToUse == null) {
                    factoryMethodToUse = mbd.getResolvedFactoryMethod();
                }
                if (factoryMethodToUse != null) {
                    candidateList = Collections.singletonList(factoryMethodToUse);
                }
            }
            if (candidateList == null) {
                candidateList = new ArrayList<>();
                Method[] rawCandidates = getCandidateMethods(factoryClass, mbd);
                for (Method candidate : rawCandidates) {
                    if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
                        candidateList.add(candidate);
                    }
                }
            }

            if (candidateList.size() == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
                Method uniqueCandidate = candidateList.get(0);
                if (uniqueCandidate.getParameterCount() == 0) {
                    mbd.factoryMethodToIntrospect = uniqueCandidate;
                    synchronized (mbd.constructorArgumentLock) {
                        mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
                        mbd.constructorArgumentsResolved = true;
                        mbd.resolvedConstructorArguments = EMPTY_ARGS;
                    }
                    //实例化bean
                    bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, uniqueCandidate, EMPTY_ARGS));
                    return bw;
                }
            }

            Method[] candidates = candidateList.toArray(new Method[0]);
            AutowireUtils.sortFactoryMethods(candidates);

            ConstructorArgumentValues resolvedValues = null;
            boolean autowiring = (mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
            int minTypeDiffWeight = Integer.MAX_VALUE;
            Set<Method> ambiguousFactoryMethods = null;

            int minNrOfArgs;
            if (explicitArgs != null) {
                minNrOfArgs = explicitArgs.length;
            }
            else {
                //我们没有以编程方式传递参数，因此需要解析bean定义中构造函数参数中指定的参数
                if (mbd.hasConstructorArgumentValues()) {
                    ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
                    resolvedValues = new ConstructorArgumentValues();
                    //将此bean的构造函数参数解析为resolvedValues对象。这可能涉及到查找其他bean。
                    //此方法还用于处理静态工厂方法的调用
                    minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
                }else {
                    minNrOfArgs = 0;
                }
            }

            LinkedList<UnsatisfiedDependencyException> causes = null;

            for (Method candidate : candidates) {
                Class<?>[] paramTypes = candidate.getParameterTypes();

                if (paramTypes.length >= minNrOfArgs) {
                    ConstructorResolver.ArgumentsHolder argsHolder;

                    if (explicitArgs != null) {
                        //给定的显式参数->参数长度必须精确匹配
                        if (paramTypes.length != explicitArgs.length) {
                            continue;
                        }
                        argsHolder = new ConstructorResolver.ArgumentsHolder(explicitArgs);
                    }else {
                        //解析构造函数参数:类型转换和或必要的自动装配
                        try {
                            String[] paramNames = null;
                            ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                            if (pnd != null) {
                                paramNames = pnd.getParameterNames(candidate);
                            }
                            //给定已解析的构造函数参数值，创建参数数组来调用构造函数或工厂方法
                            argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw,
                                    paramTypes, paramNames, candidate, autowiring, candidates.length == 1);
                        }catch (UnsatisfiedDependencyException ex) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("Ignoring factory method [" + candidate + "] of bean '" + beanName + "': " + ex);
                            }
                            //吞下并尝试下一个重载工厂方法
                            if (causes == null) {
                                causes = new LinkedList<>();
                            }
                            causes.add(ex);
                            continue;
                        }
                    }

                    int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
                            argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
                    //如果它表示最接近的匹配，则选择此工厂方法
                    if (typeDiffWeight < minTypeDiffWeight) {
                        factoryMethodToUse = candidate;
                        argsHolderToUse = argsHolder;
                        argsToUse = argsHolder.arguments;
                        minTypeDiffWeight = typeDiffWeight;
                        ambiguousFactoryMethods = null;
                    }
                    //找出歧义:如果具有相同参数数量的方法具有相同的类型差异权重，则收集这些候选对象并最终引发歧义异常。但是，只在非宽
                    //松构造函数解析模式下执行检查，并显式地忽略覆盖的方法(具有相同的参数签名)
                    else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&
                            !mbd.isLenientConstructorResolution() &&
                            paramTypes.length == factoryMethodToUse.getParameterCount() &&
                            !Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
                        if (ambiguousFactoryMethods == null) {
                            ambiguousFactoryMethods = new LinkedHashSet<>();
                            ambiguousFactoryMethods.add(factoryMethodToUse);
                        }
                        ambiguousFactoryMethods.add(candidate);
                    }
                }
            }

            if (factoryMethodToUse == null || argsToUse == null) {
                if (causes != null) {
                    UnsatisfiedDependencyException ex = causes.removeLast();
                    for (Exception cause : causes) {
                        this.beanFactory.onSuppressedException(cause);
                    }
                    throw ex;
                }
                List<String> argTypes = new ArrayList<>(minNrOfArgs);
                if (explicitArgs != null) {
                    for (Object arg : explicitArgs) {
                        argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
                    }
                }else if (resolvedValues != null) {
                    Set<ValueHolder> valueHolders = new LinkedHashSet<>(resolvedValues.getArgumentCount());
                    valueHolders.addAll(resolvedValues.getIndexedArgumentValues().values());
                    valueHolders.addAll(resolvedValues.getGenericArgumentValues());
                    for (ValueHolder value : valueHolders) {
                        String argType = (value.getType() != null ? ClassUtils.getShortName(value.getType()) :
                                (value.getValue() != null ? value.getValue().getClass().getSimpleName() : "null"));
                        argTypes.add(argType);
                    }
                }
                String argDesc = StringUtils.collectionToCommaDelimitedString(argTypes);
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "No matching factory method found: " +
                                (mbd.getFactoryBeanName() != null ?
                                        "factory bean '" + mbd.getFactoryBeanName() + "'; " : "") +
                                "factory method '" + mbd.getFactoryMethodName() + "(" + argDesc + ")'. " +
                                "Check that a method with the specified name " +
                                (minNrOfArgs > 0 ? "and arguments " : "") +
                                "exists and that it is " +
                                (isStatic ? "static" : "non-static") + ".");
            }else if (void.class == factoryMethodToUse.getReturnType()) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Invalid factory method '" + mbd.getFactoryMethodName() +
                                "': needs to have a non-void return type!");
            }else if (ambiguousFactoryMethods != null) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Ambiguous factory method matches found in bean '" + beanName + "' " +
                                "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                                ambiguousFactoryMethods);
            }

            if (explicitArgs == null && argsHolderToUse != null) {
                mbd.factoryMethodToIntrospect = factoryMethodToUse;
                //存储缓存
                argsHolderToUse.storeCache(mbd, factoryMethodToUse);
            }
        }

        //实例化bean
        bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, factoryMethodToUse, argsToUse));
        return bw;
    }

    //ConstructorResolver:
    //解析存储在给定bean定义中的准备好的参数
    private Object[] resolvePreparedArguments(String beanName, RootBeanDefinition mbd, BeanWrapper bw,
                                              Executable executable, Object[] argsToResolve, boolean fallback) {

        TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
        TypeConverter converter = (customConverter != null ? customConverter : bw);
        BeanDefinitionValueResolver valueResolver =
                new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);
        Class<?>[] paramTypes = executable.getParameterTypes();

        Object[] resolvedArgs = new Object[argsToResolve.length];
        for (int argIndex = 0; argIndex < argsToResolve.length; argIndex++) {
            Object argValue = argsToResolve[argIndex];
            //1>为method/constructor创建一个MethodParameter
            MethodParameter methodParam = MethodParameter.forExecutable(executable, argIndex);
            if (argValue == autowiredArgumentMarker) { //2>解析自动生成的参数
                argValue = resolveAutowiredArgument(methodParam, beanName, null, converter, fallback);
            }else if (argValue instanceof BeanMetadataElement) { //2>解析Bean元数据
                argValue = valueResolver.resolveValueIfNecessary("constructor argument", argValue);
            }else if (argValue instanceof String) { //3>解析字符串类型的参数值
                argValue = this.beanFactory.evaluateBeanDefinitionString((String) argValue, mbd);
            }
            Class<?> paramType = paramTypes[argIndex];
            try {
                resolvedArgs[argIndex] = converter.convertIfNecessary(argValue, paramType, methodParam);
            }catch (TypeMismatchException ex) {
                throw new UnsatisfiedDependencyException(
                        mbd.getResourceDescription(), beanName, new InjectionPoint(methodParam),
                        "Could not convert argument value of type [" + ObjectUtils.nullSafeClassName(argValue) +
                                "] to required type [" + paramType.getName() + "]: " + ex.getMessage());
            }
        }
        return resolvedArgs;
    }

    //MethodParameter:
    //为给定的方法或构造函数创建一个MethodParameter
    public static MethodParameter forExecutable(Executable executable, int parameterIndex) {
        if (executable instanceof Method) {
            return new MethodParameter((Method) executable, parameterIndex);
        }else if (executable instanceof Constructor) {
            return new MethodParameter((Constructor<?>) executable, parameterIndex);
        }else {
            throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
        }
    }

    //ConstructorResolver:
    protected Object resolveAutowiredArgument(MethodParameter param, String beanName,
                                              @Nullable Set<String> autowiredBeanNames, TypeConverter typeConverter, boolean fallback) {

        Class<?> paramType = param.getParameterType();
        if (InjectionPoint.class.isAssignableFrom(paramType)) {
            //NamedThreadLocal<InjectionPoint> currentInjectionPoint = new NamedThreadLocal<>("Current injection point");
            InjectionPoint injectionPoint = currentInjectionPoint.get();
            if (injectionPoint == null) {
                throw new IllegalStateException("No current InjectionPoint available for " + param);
            }
            return injectionPoint;
        }
        try {
            //解析参数属性的依赖
            return this.beanFactory.resolveDependency(
                    new DependencyDescriptor(param, true), beanName, autowiredBeanNames, typeConverter);
        }catch (NoUniqueBeanDefinitionException ex) {
            throw ex;
        }catch (NoSuchBeanDefinitionException ex) {
            if (fallback) {
                //让我们返回一个空数组/集合
                if (paramType.isArray()) {
                    return Array.newInstance(paramType.getComponentType(), 0);
                }else if (CollectionFactory.isApproximableCollectionType(paramType)) {
                    return CollectionFactory.createCollection(paramType, 0);
                }else if (CollectionFactory.isApproximableMapType(paramType)) {
                    return CollectionFactory.createMap(paramType, 0);
                }
            }
            throw ex;
        }
    }

    //DefaultListableBeanFactory:
    //根据工厂中定义的bean解析指定的依赖项
    public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
                                    @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

        descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
        if (Optional.class == descriptor.getDependencyType()) {
            return createOptionalDependency(descriptor, requestingBeanName);
        }else if (ObjectFactory.class == descriptor.getDependencyType() ||
                ObjectProvider.class == descriptor.getDependencyType()) {
            return new DefaultListableBeanFactory.DependencyObjectProvider(descriptor, requestingBeanName);
        }else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
            return new DefaultListableBeanFactory.Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
        }else {
            Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
                    descriptor, requestingBeanName);
            if (result == null) {
                result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
            }
            return result;
        }
    }

    //ContextAnnotationAutowireCandidateResolver:
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
        return (isLazy(descriptor) ? buildLazyResolutionProxy(descriptor, beanName) : null);
    }

    //ContextAnnotationAutowireCandidateResolver:
    protected Object buildLazyResolutionProxy(final DependencyDescriptor descriptor, final @Nullable String beanName) {
        Assert.state(getBeanFactory() instanceof DefaultListableBeanFactory,
                "BeanFactory needs to be a DefaultListableBeanFactory");
        final DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) getBeanFactory();
        TargetSource ts = new TargetSource() {
            @Override
            public Class<?> getTargetClass() {
                return descriptor.getDependencyType();
            }
            @Override
            public boolean isStatic() {
                return false;
            }
            @Override
            public Object getTarget() {
                Object target = beanFactory.doResolveDependency(descriptor, beanName, null, null);
                if (target == null) {
                    Class<?> type = getTargetClass();
                    if (Map.class == type) {
                        return Collections.emptyMap();
                    }
                    else if (List.class == type) {
                        return Collections.emptyList();
                    }
                    else if (Set.class == type || Collection.class == type) {
                        return Collections.emptySet();
                    }
                    throw new NoSuchBeanDefinitionException(descriptor.getResolvableType(),
                            "Optional dependency not present for lazy injection point");
                }
                return target;
            }
            @Override
            public void releaseTarget(Object target) {
            }
        };
        ProxyFactory pf = new ProxyFactory();
        pf.setTargetSource(ts);
        Class<?> dependencyType = descriptor.getDependencyType();
        if (dependencyType.isInterface()) {
            pf.addInterface(dependencyType);
        }
        return pf.getProxy(beanFactory.getBeanClassLoader());
    }

    //DefaultListableBeanFactory.Jsr330Factory:
    public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
        return new DefaultListableBeanFactory.Jsr330Factory.Jsr330Provider(descriptor, beanName);
    }


    //DependencyDescriptor:
    //初始化基础方法参数的参数名称发现(如果有的话)
    public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
        if (this.methodParameter != null) {
            this.methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
        }
    }

    //MethodParameter:
    public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
        //ParameterNameDiscoverer parameterNameDiscoverer
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    //DefaultListableBeanFactory:
    //为指定的依赖项创建一个{Optional}包装器
    private Optional<?> createOptionalDependency(
            DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

        DependencyDescriptor descriptorToUse = new DefaultListableBeanFactory.NestedDependencyDescriptor(descriptor) {
            @Override
            public boolean isRequired() {
                return false;
            }
            @Override
            public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
                return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) :
                        super.resolveCandidate(beanName, requiredType, beanFactory));
            }
        };
        //去处理bean依赖
        Object result = doResolveDependency(descriptorToUse, beanName, null, null);
        return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
    }

    //DependencyDescriptor:
    public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
            throws BeansException {
        //调用bean工厂的初始化bean方法
        return beanFactory.getBean(beanName);
    }

    //DefaultListableBeanFactory:
    //处理bean依赖
    public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
                                      @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

        InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
        try {
            Object shortcut = descriptor.resolveShortcut(this);
            if (shortcut != null) {
                return shortcut;
            }

            //确定包装参数/字段的声明(非泛型)类型
            Class<?> type = descriptor.getDependencyType();
            Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
            if (value != null) {
                if (value instanceof String) {
                    //处理内嵌的属性值
                    String strVal = resolveEmbeddedValue((String) value);
                    BeanDefinition bd = (beanName != null && containsBean(beanName) ?
                            getMergedBeanDefinition(beanName) : null);
                    //计算bean定义中包含的给定字符串，可能将其解析为表达式
                    value = evaluateBeanDefinitionString(strVal, bd);
                }
                TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
                try {
                    //将值转换为所需的类型(如果需要，从字符串转换)
                    return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
                }catch (UnsupportedOperationException ex) {
                    //不支持类型描述符解析的自定义TypeConverter
                    return (descriptor.getField() != null ?
                            converter.convertIfNecessary(value, type, descriptor.getField()) :
                            converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
                }
            }

            //处理多重的bean
            Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
            if (multipleBeans != null) {
                return multipleBeans;
            }

            //获取自动匹配候选的多个bean
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
            if (matchingBeans.isEmpty()) {
                if (isRequired(descriptor)) {
                    //提高NoSuchBeanDefinitionException或BeanNotOfRequiredTypeException不肯舍弃的依赖
                    raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
                }
                return null;
            }

            String autowiredBeanName;
            Object instanceCandidate;

            if (matchingBeans.size() > 1) {
                //定给定的一组bean的自动装配候选人
                autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
                if (autowiredBeanName == null) {
                    if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
                        //处理指定的不是唯一的场景:默认情况下,抛一个{NoUniqueBeanDefinitionException}
                        return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
                    }else {
                        return null;
                    }
                }
                instanceCandidate = matchingBeans.get(autowiredBeanName);
            }else {
                //我们有一个匹配。
                Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
                autowiredBeanName = entry.getKey();
                instanceCandidate = entry.getValue();
            }

            if (autowiredBeanNames != null) {
                autowiredBeanNames.add(autowiredBeanName);
            }
            if (instanceCandidate instanceof Class) {
                instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
            }
            Object result = instanceCandidate;
            if (result instanceof NullBean) {
                if (isRequired(descriptor)) {
                    raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
                }
                result = null;
            }
            if (!ClassUtils.isAssignableValue(type, result)) {
                throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
            }
            return result;
        }
        finally {
            ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
        }
    }

    //AutowiredAnnotationBeanPostProcessor.ShortcutDependencyDescriptor:
    public Object resolveShortcut(BeanFactory beanFactory) {
        return beanFactory.getBean(this.shortcut, this.requiredType);
    }

    //AbstractBeanFactory:
    public String resolveEmbeddedValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        for (StringValueResolver resolver : this.embeddedValueResolvers) {
            result = resolver.resolveStringValue(result);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    //EmbeddedValueResolver:
    public String resolveStringValue(String strVal) {
        //BeanExpressionContext exprContext
        String value = this.exprContext.getBeanFactory().resolveEmbeddedValue(strVal);
        //BeanExpressionResolver exprResolver
        if (this.exprResolver != null && value != null) {
            Object evaluated = this.exprResolver.evaluate(value, this.exprContext);
            value = (evaluated != null ? evaluated.toString() : null);
        }
        return value;
    }

    //DependencyDescriptor:
    //确定包装参数/字段的声明(非泛型)类型
    public Class<?> getDependencyType() {
        if (this.field != null) {
            if (this.nestingLevel > 1) {
                Type type = this.field.getGenericType();
                for (int i = 2; i <= this.nestingLevel; i++) {
                    if (type instanceof ParameterizedType) {
                        Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                        type = args[args.length - 1];
                    }
                }
                if (type instanceof Class) {
                    return (Class<?>) type;
                }else if (type instanceof ParameterizedType) {
                    Type arg = ((ParameterizedType) type).getRawType();
                    if (arg instanceof Class) {
                        return (Class<?>) arg;
                    }
                }
                return Object.class;
            }else {
                return this.field.getType();
            }
        }else {
            return obtainMethodParameter().getNestedParameterType();
        }
    }

    //MethodParameter:
    //返回方法/构造函数参数的嵌套类型
    public Class<?> getNestedParameterType() {
        if (this.nestingLevel > 1) {
            Type type = getGenericParameterType();
            for (int i = 2; i <= this.nestingLevel; i++) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    Integer index = getTypeIndexForLevel(i);
                    type = args[index != null ? index : args.length - 1];
                }
                // TODO: Object.class if unresolvable
            }
            if (type instanceof Class) {
                return (Class<?>) type;
            }else if (type instanceof ParameterizedType) {
                Type arg = ((ParameterizedType) type).getRawType();
                if (arg instanceof Class) {
                    return (Class<?>) arg;
                }
            }
            return Object.class;
        }else {
            return getParameterType();
        }
    }

    //AbstractBeanFactory:
    //计算bean定义中包含的给定字符串，可能将其解析为表达式
    protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
        if (this.beanExpressionResolver == null) {
            return value;
        }

        Scope scope = null;
        if (beanDefinition != null) {
            String scopeName = beanDefinition.getScope();
            if (scopeName != null) {
                scope = getRegisteredScope(scopeName);
            }
        }
        return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
    }

    //AbstractBeanFactory:
    public Scope getRegisteredScope(String scopeName) {
        Assert.notNull(scopeName, "Scope identifier must not be null");
        //Map<String, Scope> scopes = new LinkedHashMap<>(8)
        //从scope标识符字符串映射到相应的scope
        return this.scopes.get(scopeName);
    }

    //TypeConverterSupport:
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                    @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

        Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
        try {
            return this.typeConverterDelegate.convertIfNecessary(null, null, value, requiredType, typeDescriptor);
        }catch (ConverterNotFoundException | IllegalStateException ex) {
            throw new ConversionNotSupportedException(value, requiredType, ex);
        }catch (ConversionException | IllegalArgumentException ex) {
            throw new TypeMismatchException(value, requiredType, ex);
        }
    }

    //TypeConverterDelegate:
    //将指定属性的值转换为所需的类型(如果需要从字符串转换)
    public <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue, @Nullable Object newValue,
                                    @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws IllegalArgumentException {

        //获取此类型的自定义编辑器
        PropertyEditor editor = this.propertyEditorRegistry.findCustomEditor(requiredType, propertyName);

        ConversionFailedException conversionAttemptEx = null;

        //没有自定义编辑器，但指定了自定义转换服务
        ConversionService conversionService = this.propertyEditorRegistry.getConversionService();
        if (editor == null && conversionService != null && newValue != null && typeDescriptor != null) {
            TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
            if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
                try {
                    return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
                }catch (ConversionFailedException ex) {
                    // fallback to default conversion logic below
                    conversionAttemptEx = ex;
                }
            }
        }

        Object convertedValue = newValue;

        //值的类型不是必需的
        if (editor != null || (requiredType != null && !ClassUtils.isAssignableValue(requiredType, convertedValue))) {
            if (typeDescriptor != null && requiredType != null && Collection.class.isAssignableFrom(requiredType) &&
                    convertedValue instanceof String) {
                TypeDescriptor elementTypeDesc = typeDescriptor.getElementTypeDescriptor();
                if (elementTypeDesc != null) {
                    Class<?> elementType = elementTypeDesc.getType();
                    if (Class.class == elementType || Enum.class.isAssignableFrom(elementType)) {
                        convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
                    }
                }
            }
            if (editor == null) {
                editor = findDefaultEditor(requiredType);
            }
            convertedValue = doConvertValue(oldValue, convertedValue, requiredType, editor);
        }

        boolean standardConversion = false;

        if (requiredType != null) {
            //如果合适的话，尝试应用一些标准类型转换规则
            if (convertedValue != null) {
                if (Object.class == requiredType) {
                    return (T) convertedValue;
                }else if (requiredType.isArray()) {
                    //数组要求->应用适当的元素转换。
                    if (convertedValue instanceof String && Enum.class.isAssignableFrom(requiredType.getComponentType())) {
                        convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
                    }
                    return (T) convertToTypedArray(convertedValue, propertyName, requiredType.getComponentType());
                }else if (convertedValue instanceof Collection) {
                    // Convert elements to target type, if determined.
                    convertedValue = convertToTypedCollection(
                            (Collection<?>) convertedValue, propertyName, requiredType, typeDescriptor);
                    standardConversion = true;
                }else if (convertedValue instanceof Map) {
                    //如果确定，将元素转换为目标类型。
                    convertedValue = convertToTypedMap(
                            (Map<?, ?>) convertedValue, propertyName, requiredType, typeDescriptor);
                    standardConversion = true;
                }
                if (convertedValue.getClass().isArray() && Array.getLength(convertedValue) == 1) {
                    convertedValue = Array.get(convertedValue, 0);
                    standardConversion = true;
                }
                if (String.class == requiredType && ClassUtils.isPrimitiveOrWrapper(convertedValue.getClass())) {
                    //我们可以限制任何原始值
                    return (T) convertedValue.toString();
                }else if (convertedValue instanceof String && !requiredType.isInstance(convertedValue)) {
                    if (conversionAttemptEx == null && !requiredType.isInterface() && !requiredType.isEnum()) {
                        try {
                            Constructor<T> strCtor = requiredType.getConstructor(String.class);
                            return BeanUtils.instantiateClass(strCtor, convertedValue);
                        }catch (NoSuchMethodException ex) {
                            // proceed with field lookup
                            if (logger.isTraceEnabled()) {
                                logger.trace("No String constructor found on type [" + requiredType.getName() + "]", ex);
                            }
                        }catch (Exception ex) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Construction via String failed for type [" + requiredType.getName() + "]", ex);
                            }
                        }
                    }
                    String trimmedValue = ((String) convertedValue).trim();
                    if (requiredType.isEnum() && trimmedValue.isEmpty()) {
                        //它是一个空的enum标识符:将enum值重置为null
                        return null;
                    }
                    convertedValue = attemptToConvertStringToEnum(requiredType, trimmedValue, convertedValue);
                    standardConversion = true;
                }else if (convertedValue instanceof Number && Number.class.isAssignableFrom(requiredType)) {
                    convertedValue = NumberUtils.convertNumberToTargetClass(
                            (Number) convertedValue, (Class<Number>) requiredType);
                    standardConversion = true;
                }
            }else {
                // convertedValue == null
                if (requiredType == Optional.class) {
                    convertedValue = Optional.empty();
                }
            }

            if (!ClassUtils.isAssignableValue(requiredType, convertedValue)) {
                if (conversionAttemptEx != null) {
                    //来自前面的ConversionService调用的原始异常
                    throw conversionAttemptEx;
                }else if (conversionService != null && typeDescriptor != null) {
                    //以前没有尝试使用ConversionService，可能找到了自定义编辑器，但编辑器无法生成所需的类型
                    TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
                    if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
                        return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
                    }
                }

                //绝对不匹配: throw IllegalArgumentException/IllegalStateException
                StringBuilder msg = new StringBuilder();
                msg.append("Cannot convert value of type '").append(ClassUtils.getDescriptiveType(newValue));
                msg.append("' to required type '").append(ClassUtils.getQualifiedName(requiredType)).append("'");
                if (propertyName != null) {
                    msg.append(" for property '").append(propertyName).append("'");
                }
                if (editor != null) {
                    msg.append(": PropertyEditor [").append(editor.getClass().getName()).append(
                            "] returned inappropriate value of type '").append(
                            ClassUtils.getDescriptiveType(convertedValue)).append("'");
                    throw new IllegalArgumentException(msg.toString());
                }else {
                    msg.append(": no matching editors or conversion strategy found");
                    throw new IllegalStateException(msg.toString());
                }
            }
        }

        if (conversionAttemptEx != null) {
            if (editor == null && !standardConversion && requiredType != null && Object.class != requiredType) {
                throw conversionAttemptEx;
            }
            logger.debug("Original ConversionService attempt failed - ignored since " +
                    "PropertyEditor based conversion eventually succeeded", conversionAttemptEx);
        }

        return (T) convertedValue;
    }

    //TypeConverterDelegate:
    private Object convertToTypedArray(Object input, @Nullable String propertyName, Class<?> componentType) {
        if (input instanceof Collection) {
            //将集合元素转换为数组元素
            Collection<?> coll = (Collection<?>) input;
            Object result = Array.newInstance(componentType, coll.size());
            int i = 0;
            for (Iterator<?> it = coll.iterator(); it.hasNext(); i++) {
                Object value = convertIfNecessary(
                        buildIndexedPropertyName(propertyName, i), null, it.next(), componentType);
                Array.set(result, i, value);
            }
            return result;
        }else if (input.getClass().isArray()) {
            //转换数组元素，如果必要的话
            if (componentType.equals(input.getClass().getComponentType()) &&
                    !this.propertyEditorRegistry.hasCustomEditorForElement(componentType, propertyName)) {
                return input;
            }
            int arrayLength = Array.getLength(input);
            Object result = Array.newInstance(componentType, arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                Object value = convertIfNecessary(
                        buildIndexedPropertyName(propertyName, i), null, Array.get(input, i), componentType);
                Array.set(result, i, value);
            }
            return result;
        }else {
            //普通值:将其转换为具有单个组件的数组
            Object result = Array.newInstance(componentType, 1);
            Object value = convertIfNecessary(
                    buildIndexedPropertyName(propertyName, 0), null, input, componentType);
            Array.set(result, 0, value);
            return result;
        }
    }

    //PropertyEditorRegistrySupport:
    //确定此注册表是否包含指定数组/集合元素的自定义编辑器。
    public boolean hasCustomEditorForElement(@Nullable Class<?> elementType, @Nullable String propertyPath) {
        if (propertyPath != null && this.customEditorsForPath != null) {
            for (Map.Entry<String, PropertyEditorRegistrySupport.CustomEditorHolder> entry : this.customEditorsForPath.entrySet()) {
                if (PropertyAccessorUtils.matchesProperty(entry.getKey(), propertyPath) &&
                        entry.getValue().getPropertyEditor(elementType) != null) {
                    return true;
                }
            }
        }
        // No property-specific editor -> check type-specific editor.
        return (elementType != null && this.customEditors != null && this.customEditors.containsKey(elementType));
    }

    //TypeConverterDelegate:
    private String buildIndexedPropertyName(@Nullable String propertyName, int index) {
        //1>PROPERTY_KEY_PREFIX = "["
        //2>PROPERTY_KEY_SUFFIX = "]"
        return (propertyName != null ?
                propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + index + PropertyAccessor.PROPERTY_KEY_SUFFIX :
                null);
    }

    //TypeConverterDelegate:
    //使用给定的属性编辑器将值转换为所需的类型(如果需要从字符串转换)
    private Object doConvertValue(@Nullable Object oldValue, @Nullable Object newValue,
                                  @Nullable Class<?> requiredType, @Nullable PropertyEditor editor) {

        Object convertedValue = newValue;

        if (editor != null && !(convertedValue instanceof String)) {
            //不是字符串->使用PropertyEditor的setValue。对于标准的propertyeditor，它将返回相同的对象;我们
            //只是想让特殊的propertyeditor覆盖setValue，以便将类型从非字符串值转换为所需的类型。
            try {
                editor.setValue(convertedValue);
                Object newConvertedValue = editor.getValue();
                if (newConvertedValue != convertedValue) {
                    convertedValue = newConvertedValue;
                    //重置PropertyEditor:它已经做了正确的转换。不要在setAsText调用中再次使用它。
                    editor = null;
                }
            }catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
                }
            }
        }

        Object returnValue = convertedValue;

        if (requiredType != null && !requiredType.isArray() && convertedValue instanceof String[]) {
            //将字符串数组转换为逗号分隔的字符串。仅当之前没有PropertyEditor转换字符串数组时才适用。CSV字符
            //串将被传递到PropertyEditor的setAsText方法中(如果有的话)。
            if (logger.isTraceEnabled()) {
                logger.trace("Converting String array to comma-delimited String [" + convertedValue + "]");
            }
            convertedValue = StringUtils.arrayToCommaDelimitedString((String[]) convertedValue);
        }

        if (convertedValue instanceof String) {
            if (editor != null) {
                //对于字符串值，使用PropertyEditor的setAsText
                if (logger.isTraceEnabled()) {
                    logger.trace("Converting String to [" + requiredType + "] using property editor [" + editor + "]");
                }
                String newTextValue = (String) convertedValue;
                return doConvertTextValue(oldValue, newTextValue, editor);
            }else if (String.class == requiredType) {
                returnValue = convertedValue;
            }
        }

        return returnValue;
    }

    private Object doConvertTextValue(@Nullable Object oldValue, String newTextValue, PropertyEditor editor) {
        try {
            editor.setValue(oldValue);
        }catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
            }
            //Swallow and proceed. 忍受并进行下去
        }
        editor.setAsText(newTextValue);
        return editor.getValue();
    }

    //TypeConverterDelegate:
    //查找给定类型的默认编辑器
    private PropertyEditor findDefaultEditor(@Nullable Class<?> requiredType) {
        PropertyEditor editor = null;
        if (requiredType != null) {
            // No custom editor -> check BeanWrapperImpl's default editors.
            editor = this.propertyEditorRegistry.getDefaultEditor(requiredType);
            if (editor == null && String.class != requiredType) {
                // No BeanWrapper default editor -> check standard JavaBean editor.
                editor = BeanUtils.findEditorByConvention(requiredType);
            }
        }
        return editor;
    }

    //DefaultListableBeanFactory:
    private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
                                        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
        //获取依赖bean类型
        final Class<?> type = descriptor.getDependencyType();

        //对多个元素的流访问的依赖描述符标记
        if (descriptor instanceof DefaultListableBeanFactory.StreamDependencyDescriptor) {
            //查找与所需类型匹配的bean实例。在为指定bean进行自动装配期间调用
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            Stream<Object> stream = matchingBeans.keySet().stream()
                    .map(name -> descriptor.resolveCandidate(name, type, this))
                    .filter(bean -> !(bean instanceof NullBean));
            if (((DefaultListableBeanFactory.StreamDependencyDescriptor) descriptor).isOrdered()) {
                //适配排序比较器
                stream = stream.sorted(adaptOrderComparator(matchingBeans));
            }
            return stream;
        }else if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            ResolvableType resolvableType = descriptor.getResolvableType();
            Class<?> resolvedArrayType = resolvableType.resolve(type);
            if (resolvedArrayType != type) {
                componentType = resolvableType.getComponentType().resolve();
            }
            if (componentType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
                    new DefaultListableBeanFactory.MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
            if (result instanceof Object[]) {
                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
                if (comparator != null) {
                    Arrays.sort((Object[]) result, comparator);
                }
            }
            return result;
        }else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
            Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
            if (elementType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
                    new DefaultListableBeanFactory.MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), type);
            if (result instanceof List) {
                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
                if (comparator != null) {
                    ((List<?>) result).sort(comparator);
                }
            }
            return result;
        }else if (Map.class == type) {
            ResolvableType mapType = descriptor.getResolvableType().asMap();
            Class<?> keyType = mapType.resolveGeneric(0);
            if (String.class != keyType) {
                return null;
            }
            Class<?> valueType = mapType.resolveGeneric(1);
            if (valueType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
                    new DefaultListableBeanFactory.MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            return matchingBeans;
        }else {
            return null;
        }
    }


    //DefaultListableBeanFactory:
    protected Map<String, Object> findAutowireCandidates(
            @Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

        //1>获取给定类型的所有bean名称，包括在祖先工厂中定义的名称。将在覆盖bean定义的情况下返回唯一的名称
        String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                this, requiredType, true, descriptor.isEager());

        Map<String, Object> result = new LinkedHashMap<>(candidateNames.length);
        for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
            Class<?> autowiringType = classObjectEntry.getKey();
            if (autowiringType.isAssignableFrom(requiredType)) {
                Object autowiringValue = classObjectEntry.getValue();
                //2>根据给定的必需类型解析给定的自动装配值，例如{ObjectFactory}值到它的实际对象结果
                autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
                if (requiredType.isInstance(autowiringValue)) {
                    result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
                    break;
                }
            }
        }

        for (String candidate : candidateNames) {
            //3.1>确定给定的beanName/candidateName对是否表示自引用
            //3.2>确定指定的bean定义是否符合autowire候选，以便将其注入到声明匹配类型依赖项的其他bean中
            if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
                //4>在候选映射中添加一个entry:一个bean实例(如果可用)，或者只是解析类型
                addCandidateEntry(result, candidate, descriptor, requiredType);
            }
        }
        if (result.isEmpty()) {
            boolean multiple = indicatesMultipleBeans(requiredType);
            //如果第一次传递没有找到任何内容，则考虑回退匹配
            DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
            for (String candidate : candidateNames) {
                if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
                        (!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
                    addCandidateEntry(result, candidate, descriptor, requiredType);
                }
            }
            if (result.isEmpty() && !multiple) {
                //将自引用视为最后一次传递，但在依赖项集合的情况下，不是完全相同的bean本身
                for (String candidate : candidateNames) {
                    if (isSelfReference(beanName, candidate) &&
                            (!(descriptor instanceof DefaultListableBeanFactory.MultiElementDescriptor) || !beanName.equals(candidate)) &&
                            isAutowireCandidate(candidate, fallbackDescriptor)) {
                        addCandidateEntry(result, candidate, descriptor, requiredType);
                    }
                }
            }
        }
        return result;
    }

    //BeanFactoryUtils:
    public static String[] beanNamesForTypeIncludingAncestors(
            ListableBeanFactory lbf, Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {

        Assert.notNull(lbf, "ListableBeanFactory must not be null");
        String[] result = lbf.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        if (lbf instanceof HierarchicalBeanFactory) {
            HierarchicalBeanFactory hbf = (HierarchicalBeanFactory) lbf;
            if (hbf.getParentBeanFactory() instanceof ListableBeanFactory) {
                String[] parentResult = beanNamesForTypeIncludingAncestors(
                        (ListableBeanFactory) hbf.getParentBeanFactory(), type, includeNonSingletons, allowEagerInit);
                //将给定的bean名称结果与给定的父结果合并
                result = mergeNamesWithParent(result, parentResult, hbf);
            }
        }
        return result;
    }

    //BeanFactoryUtils:
    //将给定的bean名称结果与给定的父结果合并
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

    //AutowireUtils:
    //根据给定的必需类型解析给定的自动装配值，例如{ObjectFactory}值到它的实际对象结果
    public static Object resolveAutowiringValue(Object autowiringValue, Class<?> requiredType) {
        if (autowiringValue instanceof ObjectFactory && !requiredType.isInstance(autowiringValue)) {
            ObjectFactory<?> factory = (ObjectFactory<?>) autowiringValue;
            if (autowiringValue instanceof Serializable && requiredType.isInterface()) {
                autowiringValue = Proxy.newProxyInstance(requiredType.getClassLoader(),
                        new Class<?>[] {requiredType}, new AutowireUtils.ObjectFactoryDelegatingInvocationHandler(factory));
            }
            else {
                return factory.getObject();
            }
        }
        return autowiringValue;
    }

    //DefaultListableBeanFactory:
    //确定给定的beanName/candidateName对是否表示自引用，
    //即候选对象是指向原始bean还是指向原始bean上的工厂方法
    private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
        return (beanName != null && candidateName != null &&
                (beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
                        beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
    }

    //DefaultListableBeanFactory:
    public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
            throws NoSuchBeanDefinitionException {

        return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
    }

    //DefaultListableBeanFactory:
    //确定指定的bean定义是否符合autowire候选，以便将其注入到声明匹配类型依赖项的其他bean中
    protected boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
            throws NoSuchBeanDefinitionException {

        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
        if (containsBeanDefinition(beanDefinitionName)) {
            return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(beanDefinitionName), descriptor, resolver);
        }else if (containsSingleton(beanName)) {
            return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
        }

        BeanFactory parent = getParentBeanFactory();
        if (parent instanceof DefaultListableBeanFactory) {
            //在这个工厂中没有找到bean定义——>委托给父类。
            return ((DefaultListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
        }else if (parent instanceof ConfigurableListableBeanFactory) {
            //如果没有DefaultListableBeanFactory，就不能传递解析器
            return ((ConfigurableListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
        }else {
            return true;
        }
    }

    //DefaultListableBeanFactory:
    //在候选映射中添加一个entry:一个bean实例(如果可用)，或者只是解析类型，这样可以在主候选选择之前防止bean的早期初始化
    private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
                                   DependencyDescriptor descriptor, Class<?> requiredType) {

        if (descriptor instanceof DefaultListableBeanFactory.MultiElementDescriptor) {
            Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
            if (!(beanInstance instanceof NullBean)) {
                candidates.put(candidateName, beanInstance);
            }
        }else if (containsSingleton(candidateName) || (descriptor instanceof DefaultListableBeanFactory.StreamDependencyDescriptor &&
                ((DefaultListableBeanFactory.StreamDependencyDescriptor) descriptor).isOrdered())) {
            Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
            candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
        }else {
            candidates.put(candidateName, getType(candidateName));
        }
    }

    //DefaultListableBeanFactory:
    private boolean indicatesMultipleBeans(Class<?> type) {
        return (type.isArray() || (type.isInterface() &&
                (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
    }

    //DefaultListableBeanFactory:
    private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).withSourceProvider(
                    createFactoryAwareOrderSourceProvider(matchingBeans));
        }else {
            return comparator;
        }
    }

    //DefaultListableBeanFactory:
    private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
        IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
        beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
        return new DefaultListableBeanFactory.FactoryAwareOrderSourceProvider(instancesToBeanNames);
    }

    //DefaultListableBeanFactory；
    private void raiseNoMatchingBeanFound(
            Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {

        checkBeanNotOfRequiredType(type, descriptor);

        throw new NoSuchBeanDefinitionException(resolvableType,
                "expected at least 1 bean which qualifies as autowire candidate. " +
                        "Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
    }

    //DefaultListableBeanFactory:
    //提高BeanNotOfRequiredTypeException不肯舍弃的依赖性,如果适用,即如果bean将匹配的目标类型,是一个不公开代理
    private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
        for (String beanName : this.beanDefinitionNames) {
            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            Class<?> targetType = mbd.getTargetType();
            if (targetType != null && type.isAssignableFrom(targetType) &&
                    isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
                //可能一个代理干扰目标类型匹配-->抛出有意义的异常
                Object beanInstance = getSingleton(beanName, false);
                Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ?
                        beanInstance.getClass() : predictBeanType(beanName, mbd));
                if (beanType != null && !type.isAssignableFrom(beanType)) {
                    throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
                }
            }
        }

        BeanFactory parent = getParentBeanFactory();
        if (parent instanceof DefaultListableBeanFactory) {
            ((DefaultListableBeanFactory) parent).checkBeanNotOfRequiredType(type, descriptor);
        }
    }


    //DefaultListableBeanFactory:
    //确定给定的一组bean的自动装配候选人
    protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
        Class<?> requiredType = descriptor.getDependencyType();
        //1>确定在给定的一组bean的主要候选人
        String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
        if (primaryCandidate != null) {
            return primaryCandidate;
        }
        //2>确定给定的一组bean优先级最高的候选人
        String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
        if (priorityCandidate != null) {
            return priorityCandidate;
        }
        // Fallback
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateName = entry.getKey();
            Object beanInstance = entry.getValue();
            //Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16)
            //依赖类型到相应的自动注入值的映射
            if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) ||
                    matchesBeanName(candidateName, descriptor.getDependencyName())) {
                return candidateName;
            }
        }
        return null;
    }

    //DefaultListableBeanFactory:
    //确定在给定的一组bean的主要候选人
    protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String primaryBeanName = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (isPrimary(candidateBeanName, beanInstance)) {
                if (primaryBeanName != null) {
                    boolean candidateLocal = containsBeanDefinition(candidateBeanName);
                    boolean primaryLocal = containsBeanDefinition(primaryBeanName);
                    if (candidateLocal && primaryLocal) {
                        throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                                "more than one 'primary' bean found among candidates: " + candidates.keySet());
                    } else if (candidateLocal) {
                        primaryBeanName = candidateBeanName;
                    }
                }else {
                    primaryBeanName = candidateBeanName;
                }
            }
        }
        return primaryBeanName;
    }


    //DefaultListableBeanFactory:
    //确定给定的一组bean优先级最高的候选人
    protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String highestPriorityBeanName = null;
        Integer highestPriority = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (beanInstance != null) {
                Integer candidatePriority = getPriority(beanInstance);
                if (candidatePriority != null) {
                    if (highestPriorityBeanName != null) {
                        if (candidatePriority.equals(highestPriority)) {
                            throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                                    "Multiple beans found with the same priority ('" + highestPriority +
                                            "') among candidates: " + candidates.keySet());
                        }else if (candidatePriority < highestPriority) {
                            highestPriorityBeanName = candidateBeanName;
                            highestPriority = candidatePriority;
                        }
                    }else {
                        highestPriorityBeanName = candidateBeanName;
                        highestPriority = candidatePriority;
                    }
                }
            }
        }
        return highestPriorityBeanName;
    }


    //DefaultListableBeanFactory:
    //确定给定的候选人的名字与bean名称或别名匹配存储在这个bean定义
    protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
        return (candidateName != null &&
                (candidateName.equals(beanName) || ObjectUtils.containsElement(getAliases(beanName), candidateName)));
    }

    //DependencyDescriptor:
    //处理指定的不是唯一的场景:默认情况下,抛一个{NoUniqueBeanDefinitionException}
    public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) throws BeansException {
        throw new NoUniqueBeanDefinitionException(type, matchingBeans.keySet());
    }

    //ConstructorResolver:
    static InjectionPoint setCurrentInjectionPoint(@Nullable InjectionPoint injectionPoint) {
        InjectionPoint old = currentInjectionPoint.get();
        if (injectionPoint != null) {
            currentInjectionPoint.set(injectionPoint);
        }
        else {
            currentInjectionPoint.remove();
        }
        return old;
    }











    //--------------------------
    //AbstractAutowireCapableBeanFactory:
    //“自动装配构造函数”(按类型提供构造函数参数)行为。
    //如果指定了显式构造函数参数值，将所有剩余的参数与bean工厂中的bean进行匹配。这对应于构造函数注入
    protected BeanWrapper autowireConstructor(
            String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

        return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
    }


    //ConstructorResolver:
    //“自动装配构造函数”(按类型提供构造函数参数)行为。如果指定了显式构造函数参数值，将所有剩余参数与bean工厂中的bean匹配，
    //也可以应用。这与构造函数注入相对应:在这种模式下，Spring bean工厂能够承载期望基于构造函数的依赖项解析的组件
    public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
                                           @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

        BeanWrapperImpl bw = new BeanWrapperImpl();
        this.beanFactory.initBeanWrapper(bw);

        Constructor<?> constructorToUse = null;
        ConstructorResolver.ArgumentsHolder argsHolderToUse = null;
        Object[] argsToUse = null;

        if (explicitArgs != null) {
            argsToUse = explicitArgs;
        }else {
            Object[] argsToResolve = null;
            synchronized (mbd.constructorArgumentLock) {
                constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
                if (constructorToUse != null && mbd.constructorArgumentsResolved) {
                    //找到缓存的构造函数
                    argsToUse = mbd.resolvedConstructorArguments;
                    if (argsToUse == null) {
                        argsToResolve = mbd.preparedConstructorArguments;
                    }
                }
            }
            if (argsToResolve != null) {
                argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve, true);
            }
        }

        if (constructorToUse == null || argsToUse == null) {
            //以指定的构造函数为例(如果有)
            Constructor<?>[] candidates = chosenCtors;
            if (candidates == null) {
                Class<?> beanClass = mbd.getBeanClass();
                try {
                    candidates = (mbd.isNonPublicAccessAllowed() ?
                            beanClass.getDeclaredConstructors() : beanClass.getConstructors());
                }catch (Throwable ex) {
                    throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                            "Resolution of declared constructors on bean Class [" + beanClass.getName() +
                                    "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
                }
            }

            if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
                Constructor<?> uniqueCandidate = candidates[0];
                if (uniqueCandidate.getParameterCount() == 0) {
                    synchronized (mbd.constructorArgumentLock) {
                        mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
                        mbd.constructorArgumentsResolved = true;
                        mbd.resolvedConstructorArguments = EMPTY_ARGS;
                    }
                    //实例化bean
                    bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
                    return bw;
                }
            }

            //需要解析构造函数
            boolean autowiring = (chosenCtors != null ||
                    mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
            ConstructorArgumentValues resolvedValues = null;

            int minNrOfArgs;
            if (explicitArgs != null) {
                minNrOfArgs = explicitArgs.length;
            }else {
                ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
                resolvedValues = new ConstructorArgumentValues();
                //将此bean的构造函数参数解析为resolvedValues对象。这可能涉及到查找其他bean。
                //此方法还用于处理静态工厂方法的调用
                minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
            }

            AutowireUtils.sortConstructors(candidates);
            int minTypeDiffWeight = Integer.MAX_VALUE;
            Set<Constructor<?>> ambiguousConstructors = null;
            LinkedList<UnsatisfiedDependencyException> causes = null;

            for (Constructor<?> candidate : candidates) {
                Class<?>[] paramTypes = candidate.getParameterTypes();

                if (constructorToUse != null && argsToUse != null && argsToUse.length > paramTypes.length) {
                    //已经找到贪婪的构造函数可以满足-->不寻找任何进一步，只有较少的贪婪构造函数留下
                    break;
                }
                if (paramTypes.length < minNrOfArgs) {
                    continue;
                }

                ConstructorResolver.ArgumentsHolder argsHolder;
                if (resolvedValues != null) {
                    try {
                        //委托检查Java 6的ConstructorProperties注释
                        String[] paramNames = ConstructorResolver.ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
                        if (paramNames == null) {
                            ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                            if (pnd != null) {
                                paramNames = pnd.getParameterNames(candidate);
                            }
                        }
                        //给定已解析的构造函数参数值，创建参数数组来调用构造函数或工厂方法
                        argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
                                getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
                    }catch (UnsatisfiedDependencyException ex) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
                        }
                        //吞下并尝试下一个构造函数
                        if (causes == null) {
                            causes = new LinkedList<>();
                        }
                        causes.add(ex);
                        continue;
                    }
                }else {
                    //给定的显式参数-->参数长度必须精确匹配
                    if (paramTypes.length != explicitArgs.length) {
                        continue;
                    }
                    argsHolder = new ConstructorResolver.ArgumentsHolder(explicitArgs);
                }

                int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
                        argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
                //如果它表示最接近的匹配，则选择此构造函数
                if (typeDiffWeight < minTypeDiffWeight) {
                    constructorToUse = candidate;
                    argsHolderToUse = argsHolder;
                    argsToUse = argsHolder.arguments;
                    minTypeDiffWeight = typeDiffWeight;
                    ambiguousConstructors = null;
                }else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
                    if (ambiguousConstructors == null) {
                        ambiguousConstructors = new LinkedHashSet<>();
                        ambiguousConstructors.add(constructorToUse);
                    }
                    ambiguousConstructors.add(candidate);
                }
            }

            if (constructorToUse == null) {
                if (causes != null) {
                    UnsatisfiedDependencyException ex = causes.removeLast();
                    for (Exception cause : causes) {
                        this.beanFactory.onSuppressedException(cause);
                    }
                    throw ex;
                }
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Could not resolve matching constructor " +
                                "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
            }else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Ambiguous constructor matches found in bean '" + beanName + "' " +
                                "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
                                ambiguousConstructors);
            }

            if (explicitArgs == null && argsHolderToUse != null) {
                argsHolderToUse.storeCache(mbd, constructorToUse);
            }
        }

        Assert.state(argsToUse != null, "Unresolved constructor arguments");
        //实例化bean
        bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
        return bw;
    }

    //AbstractAutowireCapableBeanFactory:
    //使用给定bean的默认构造函数实例化它
    protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
        try {
            Object beanInstance;
            final BeanFactory parent = this;
            //获取系统的安全管理接口，JDK标准的安全管理API
            if (System.getSecurityManager() != null) {
                ////这里是一个匿名内置类，根据实例化策略创建实例对象
                beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
                                getInstantiationStrategy().instantiate(mbd, beanName, parent),
                        getAccessControlContext());
            } else {
                //将实例化的对象封装起来
                beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
            }
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw);
            return bw;
        } catch (Throwable ex) {
            throw new BeanCreationException(
                    mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
        }
    }

    //SimpleInstantiationStrategy:
    //使用初始化策略实例化Bean对象
    public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
        //如果Bean定义中没有方法覆盖，则就不需要CGLIB父类类的方法
        if (!bd.hasMethodOverrides()) {
            Constructor<?> constructorToUse;
            synchronized (bd.constructorArgumentLock) {
                //获取对象的构造方法或工厂方法
                constructorToUse = (Constructor<?>) bd.resolvedConstructorOrFactoryMethod;
                //如果没有构造方法且没有工厂方法
                if (constructorToUse == null) {
                    //使用JDK的反射机制，判断要实例化的Bean是否是接口
                    final Class<?> clazz = bd.getBeanClass();
                    if (clazz.isInterface()) {
                        throw new BeanInstantiationException(clazz, "Specified class is an interface");
                    }
                    try {
                        if (System.getSecurityManager() != null) {
                            //这里是一个匿名内置类，使用反射机制获取Bean的构造方法
                            constructorToUse = AccessController.doPrivileged(
                                    (PrivilegedExceptionAction<Constructor<?>>) clazz::getDeclaredConstructor);
                        }else {
                            constructorToUse = clazz.getDeclaredConstructor();
                        }
                        bd.resolvedConstructorOrFactoryMethod = constructorToUse;
                    }catch (Throwable ex) {
                        throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                    }
                }
            }
            //使用BeanUtils实例化，通过反射机制调用”构造方法.newInstance(arg)”来进行实例化
            return BeanUtils.instantiateClass(constructorToUse);
        }else {
            //使用CGLIB来实例化对象
            return instantiateWithMethodInjection(bd, beanName, owner);
        }
    }

    //BeanUtils:
    //使用给定构造函数实例化类的便利方法。注意，如果给定不可访问的构造函数(即非公共的)，
    //此方法将尝试设置可访问的构造函数，并支持带有可选参数和默认值的Kotlin类。
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            ReflectionUtils.makeAccessible(ctor);
            if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
                return BeanUtils.KotlinDelegate.instantiateClass(ctor, args);
            }else {
                Class<?>[] parameterTypes = ctor.getParameterTypes();
                Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
                Object[] argsWithDefaultValues = new Object[args.length];
                for (int i = 0 ; i < args.length; i++) {
                    if (args[i] == null) {
                        Class<?> parameterType = parameterTypes[i];
                        argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
                    }
                    else {
                        argsWithDefaultValues[i] = args[i];
                    }
                }
                return ctor.newInstance(argsWithDefaultValues);
            }
        }catch (InstantiationException ex) {
            throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
        }catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
        }catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
        }catch (InvocationTargetException ex) {
            throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
        }
    }

    //CglibSubclassingInstantiationStrategy:
    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
        return instantiateWithMethodInjection(bd, beanName, owner, null);
    }

    //CglibSubclassingInstantiationStrategy:
    protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
                                                    @Nullable Constructor<?> ctor, Object... args) {
        //必须生成CGLIB子类
        return new CglibSubclassingInstantiationStrategy.CglibSubclassCreator(bd, owner).instantiate(ctor, args);
    }

    ////CglibSubclassingInstantiationStrategy:
    //创建动态生成的实现所需查找的子类的新实例
    public Object instantiate(@Nullable Constructor<?> ctor, Object... args) {
        Class<?> subclass = createEnhancedSubclass(this.beanDefinition);
        Object instance;
        if (ctor == null) {
            instance = BeanUtils.instantiateClass(subclass);
        }else {
            try {
                Constructor<?> enhancedSubclassConstructor = subclass.getConstructor(ctor.getParameterTypes());
                instance = enhancedSubclassConstructor.newInstance(args);
            }catch (Exception ex) {
                throw new BeanInstantiationException(this.beanDefinition.getBeanClass(),
                        "Failed to invoke constructor for CGLIB enhanced subclass [" + subclass.getName() + "]", ex);
            }
        }
        //直接在实例上而不是在增强类中设置回调(通过增强器)，以避免内存泄漏。
        Factory factory = (Factory) instance;
        factory.setCallbacks(new Callback[] {NoOp.INSTANCE,
                new CglibSubclassingInstantiationStrategy.LookupOverrideMethodInterceptor(this.beanDefinition, this.owner),
                new CglibSubclassingInstantiationStrategy.ReplaceOverrideMethodInterceptor(this.beanDefinition, this.owner)});
        return instance;
    }












    //-----------------------------
    //AbstractAutowireCapableBeanFactory:
    //用bean定义中的属性值填充给定BeanWrapper中的bean实例。
    protected void populateBean(String beanName, RootBeanDefinition mbd, @Nullable BeanWrapper bw) {
        if (bw == null) {
            if (mbd.hasPropertyValues()) {
                throw new BeanCreationException(
                        mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
            }else {
                //跳过空实例的属性填充阶段
                return;
            }
        }

        //在设置属性之前调用Bean的PostProcessor后置处理器
        boolean continueWithPropertyPopulation = true;
        if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                        continueWithPropertyPopulation = false;
                        break;
                    }
                }
            }
        }

        if (!continueWithPropertyPopulation) {
            return;
        }

        //获取容器在解析Bean定义资源时为BeanDefiniton中设置的属性值
        PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);
        //依赖注入开始，首先处理autowire自动装配的注入
        int resolvedAutowireMode = mbd.getResolvedAutowireMode();
        if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
            //1>对autowire自动装配的处理，根据Bean名称自动装配注入
            if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }
            //2>根据Bean类型自动装配注入
            if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }
            pvs = newPvs;
        }

        //检查容器是否持有用于处理单态模式Bean关闭时的后置处理器
        boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
        //Bean实例对象没有依赖，即没有继承基类
        boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

        PropertyDescriptor[] filteredPds = null;
        if (hasInstAwareBpps) {
            if (pvs == null) {
                pvs = mbd.getPropertyValues();
            }
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
                    if (pvsToUse == null) {
                        if (filteredPds == null) {
                            //从实例对象中提取属性描述符
                            filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
                        }
                        //使用BeanPostProcessor处理器处理属性值
                        pvsToUse = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                        if (pvsToUse == null) {
                            return;
                        }
                    }
                    pvs = pvsToUse;
                }
            }
        }
        if (needsDepCheck) {
            if (filteredPds == null) {
                filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
            }
            //为要设置的属性进行依赖检查
            checkDependencies(beanName, mbd, filteredPds, pvs);
        }

        if (pvs != null) {
            //对属性进行注入
            applyPropertyValues(beanName, mbd, bw, pvs);
        }
    }


    //AbstractAutowireCapableBeanFactory:
    //如果autowire设置为“byName”，则使用对工厂中其他bean的引用来填充任何缺失的属性值
    protected void autowireByName(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            if (containsBean(propertyName)) {
                //当属性也是一个bean时，递归调用AbstractBeanFactory.getBean()方法, 去实例化bean
                Object bean = getBean(propertyName);
                pvs.add(propertyName, bean);
                //为当前bean注册一个从属bean，在销毁当前bean之前销毁它
                registerDependentBean(propertyName, beanName);
                if (logger.isTraceEnabled()) {
                    logger.trace("Added autowiring by name from bean name '" + beanName +
                            "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
                }
            }else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                            "' by name: no matching bean found");
                }
            }
        }
    }


    //AbstractAutowireCapableBeanFactory:
    //返回一个不满足的非简单bean属性数组。这些可能是对工厂中其他bean的不满意的引用。不包括基本类型或字符串等简单属性
    protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
        Set<String> result = new TreeSet<>();
        PropertyValues pvs = mbd.getPropertyValues();
        PropertyDescriptor[] pds = bw.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName()) &&
                    !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
                result.add(pd.getName());
            }
        }
        return StringUtils.toStringArray(result);
    }

    //AbstractAutowireCapableBeanFactory:
    //确定给定的bean属性是否被排除在依赖项检查之外。此实现排除由CGLIB定义的属性，
    //以及其类型与被忽略的依赖项类型相匹配或由被忽略的依赖项接口定义的属性。
    protected boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
        return (AutowireUtils.isExcludedFromDependencyCheck(pd) ||
                this.ignoredDependencyTypes.contains(pd.getPropertyType()) ||
                AutowireUtils.isSetterDefinedInInterface(pd, this.ignoredDependencyInterfaces));
    }


    //AbstractAutowireCapableBeanFactory:
    //抽象方法定义“按类型自动装配”(按类型定义bean属性)行为。这类似于PicoContainer默认设置，其中bean工厂中必须恰好有一个
    //属性类型的bean。这使得bean工厂很容易配置为较小的名称空间，但是对于较大的应用程序，它的工作效果不如标准的Spring行为
    protected void autowireByType(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }

        Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            try {
                //根据属性名获取属性描述
                PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
                //不要尝试通过类型对象的类型进行自动装配:这是没有意义的，即使它在技术上是一个不满足的、非简单的属性
                if (Object.class != pd.getPropertyType()) {
                    //获取指定属性的写方法
                    MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                    //如果是优先后处理器，则不允许立即初始化类型匹配
                    boolean eager = !PriorityOrdered.class.isInstance(bw.getWrappedInstance());
                    DependencyDescriptor desc = new AbstractAutowireCapableBeanFactory.AutowireByTypeDependencyDescriptor(methodParam, eager);
                    //解析处理属性bean依赖
                    Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                    if (autowiredArgument != null) {
                        pvs.add(propertyName, autowiredArgument);
                    }
                    for (String autowiredBeanName : autowiredBeanNames) {
                        registerDependentBean(autowiredBeanName, beanName);
                        if (logger.isTraceEnabled()) {
                            logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
                                    propertyName + "' to bean named '" + autowiredBeanName + "'");
                        }
                    }
                    autowiredBeanNames.clear();
                }
            }catch (BeansException ex) {
                throw new UnsatisfiedDependencyException(mbd.getResourceDescription(), beanName, propertyName, ex);
            }
        }
    }

    //BeanWrapperImpl:
    public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
        BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
        String finalPath = getFinalPath(nestedBw, propertyName);
        PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName,
                    "No property '" + propertyName + "' found");
        }
        return pd;
    }

    //AbstractNestablePropertyAccessor:
    //递归导航以返回嵌套属性路径的属性访问器
    protected AbstractNestablePropertyAccessor getPropertyAccessorForPropertyPath(String propertyPath) {
        int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
        // Handle nested properties recursively.
        if (pos > -1) {
            String nestedProperty = propertyPath.substring(0, pos);
            String nestedPath = propertyPath.substring(pos + 1);
            AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(nestedProperty);
            return nestedPa.getPropertyAccessorForPropertyPath(nestedPath);
        } else {
            return this;
        }
    }

    //BeanUtils:
    //获取指定属性的写方法的新方法参数对象。
    public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
        if (pd instanceof GenericTypeAwarePropertyDescriptor) {
            return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
        }
        else {
            Method writeMethod = pd.getWriteMethod();
            Assert.state(writeMethod != null, "No write method available");
            return new MethodParameter(writeMethod, 0);
        }
    }

}
