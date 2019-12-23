package com.gfm.utils;

import com.gfm.service.UserService;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.*;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
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

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;

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
                Class<?> targetType = determineTargetType(beanName, mbd);
                if (targetType != null) {
                    bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
                    if (bean != null) {
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

}
