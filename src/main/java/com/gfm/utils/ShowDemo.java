package com.gfm.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeansDtdResolver;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;

public class ShowDemo {
    public static void main(String[] args) {

    }

    //AbstractRefreshableApplicationContext:
    //告诉子类刷新内部bean工厂
    protected DefaultListableBeanFactory createBeanFactory() {
        return new DefaultListableBeanFactory(getInternalParentBeanFactory());
    }

    //AbstractApplicationContext:
    protected BeanFactory getInternalParentBeanFactory() {
        //如果实现了ConfigurableApplicationContext返回父上下文的beanFactory,
        //否则返回父上下文
        return (getParent() instanceof ConfigurableApplicationContext ?
                ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
    }

    //AbstractApplicationContext:
    public ApplicationContext getParent() {
        //ApplicationContext parent； 父上下文
        return this.parent;
    }

    //DefaultListableBeanFactory:
    //通过给定的parentBeanFactory创建DefaultListableBeanFactory实例
    public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    //AbstractAutowireCapableBeanFactory:
    //通过给定的parentBeanFactory创建AbstractAutowireCapableBeanFactory实例
    public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        this();
        setParentBeanFactory(parentBeanFactory);
    }

    //AbstractAutowireCapableBeanFactory:
    public AbstractAutowireCapableBeanFactory() {
        super();
        ignoreDependencyInterface(BeanNameAware.class);
        ignoreDependencyInterface(BeanFactoryAware.class);
        ignoreDependencyInterface(BeanClassLoaderAware.class);
    }

    //AbstractAutowireCapableBeanFactory:
    //忽略依赖的接口去自动装配
    public void ignoreDependencyInterface(Class<?> ifc) {
        //忽略依赖检查的集合，Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>()
        this.ignoredDependencyInterfaces.add(ifc);
    }

    //AbstractBeanFactory:
    public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
            throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
        }
        this.parentBeanFactory = parentBeanFactory;
    }


    //AbstractRefreshableApplicationContext:
    //设置一个用于序列化的id
    public void setSerializationId(@Nullable String serializationId) {
        if (serializationId != null) {
            //Map<String, Reference<DefaultListableBeanFactory>> serializableFactories=new ConcurrentHashMap<>(8)
            serializableFactories.put(serializationId, new WeakReference<>(this));
        }
        else if (this.serializationId != null) {
            serializableFactories.remove(this.serializationId);
        }
        this.serializationId = serializationId;
    }

    //AbstractRefreshableApplicationContext:
    //通过这个上下文自定义内部beanFactory
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        if (this.allowBeanDefinitionOverriding != null) {
            beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
        }
        if (this.allowCircularReferences != null) {
            beanFactory.setAllowCircularReferences(this.allowCircularReferences);
        }
    }




    //AbstractXmlApplicationContext:
    ///实现父类抽象的载入Bean定义方法
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
        //1>创建XmlBeanDefinitionReader，即创建Bean读取器，并通过回调设置到容器中去，容器使用该读取器读取Bean定义资源
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

        //使用此上下文的资源加载环境配置bean定义读取器
        beanDefinitionReader.setEnvironment(this.getEnvironment());
        //为Bean读取器设置Spring资源加载器，AbstractXmlApplicationContext的
        //祖先父类AbstractApplicationContext继承DefaultResourceLoader，因此，容器本身也是一个资源加载器
        beanDefinitionReader.setResourceLoader(this);
        //2>为Bean读取器设置SAX xml解析器
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

        //3>当Bean读取器读取Bean定义的Xml资源文件时，启用Xml的校验机制
        initBeanDefinitionReader(beanDefinitionReader);

        //4>Bean读取器真正实现加载的方法
        loadBeanDefinitions(beanDefinitionReader);
    }

    //XmlBeanDefinitionReader:
    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    //AbstractBeanDefinitionReader:
    protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
        this.registry = registry; //registry = BeanDefinitionRegistry

        // Determine ResourceLoader to use.
        if (this.registry instanceof ResourceLoader) {
            this.resourceLoader = (ResourceLoader) this.registry;
        }else {
            //创建路径匹配的资源加载器，实际上就是获取类加载器
            this.resourceLoader = new PathMatchingResourcePatternResolver();
        }

        // Inherit Environment if possible
        if (this.registry instanceof EnvironmentCapable) {
            this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
        }else {
            this.environment = new StandardEnvironment();
        }
    }

    //PathMatchingResourcePatternResolver:
    public PathMatchingResourcePatternResolver() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    //DefaultResourceLoader:
    public DefaultResourceLoader() {
        this.classLoader = ClassUtils.getDefaultClassLoader();
    }

    //ClassUtils:
    //获取类加载器
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        }catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = ClassUtils.class.getClassLoader();
            if (cl == null) {
                // getClassLoader() returning null indicates the bootstrap ClassLoader
                try {
                    cl = ClassLoader.getSystemClassLoader();
                }catch (Throwable ex) {
                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
                }
            }
        }
        return cl;
    }


    //ResourceEntityResolver:
    public ResourceEntityResolver(ResourceLoader resourceLoader) {
        super(resourceLoader.getClassLoader());
        this.resourceLoader = resourceLoader;
    }

    //DelegatingEntityResolver:
    public DelegatingEntityResolver(@Nullable ClassLoader classLoader) {
        this.dtdResolver = new BeansDtdResolver();
        this.schemaResolver = new PluggableSchemaResolver(classLoader);
    }




    //AbstractXmlApplicationContext:
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
        reader.setValidating(this.validating);
    }

    //XmlBeanDefinitionReader:
    public void setValidating(boolean validating) {
        this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
        this.namespaceAware = !validating;
    }


    //AbstractXmlApplicationContext:
    //XmlBeanDefinitionReader加载bean定义资源
    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
        //1>获取Bean定义资源的定位
        Resource[] configResources = getConfigResources();
        if (configResources != null) {
            //Xml Bean读取器调用其父类AbstractBeanDefinitionReader读取定位的Bean定义资源
            reader.loadBeanDefinitions(configResources);
        }
        //2>如果子类中获取的Bean定义资源定位为空，则获取ClassPathXmlApplicationContext构造
        //  方法中setConfigLocations方法设置的资源
        String[] configLocations = getConfigLocations();
        if (configLocations != null) {
            //3>Xml Bean读取器调用其父类AbstractBeanDefinitionReader读取定位的Bean定义资源
            reader.loadBeanDefinitions(configLocations);
        }
    }

    //ClassPathXmlApplicationContext:
    protected Resource[] getConfigResources() {
        // Resource[] configResources; 资源数组
        return this.configResources;
    }

    //AbstractRefreshableConfigApplicationContext:
    protected String[] getConfigLocations() {
        //1>String[] configLocations;  资源名字符串数组
        //2>getDefaultConfigLocations(); 是一个接口，使用了一个委托模式，调用子类的获取Bean定义资源定位的方法，
        //  该方法在ClassPathXmlApplicationContext中进行实现,本类中没有实现
        return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
    }

    //AbstractBeanDefinitionReader:
    public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
        Assert.notNull(locations, "Location array must not be null");
        int count = 0;
        for (String location : locations) {
            count += loadBeanDefinitions(location);
        }
        return count;
    }

    //AbstractBeanDefinitionReader:
    public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(location, null);
    }



    //AbstractBeanDefinitionReader:
    public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
        //1>获取在IoC容器初始化过程中设置的资源加载器
        ResourceLoader resourceLoader = getResourceLoader();
        if (resourceLoader == null) {
            throw new BeanDefinitionStoreException(
                    "Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
        }

        if (resourceLoader instanceof ResourcePatternResolver) {
            try {
                //2>将指定位置的Bean定义资源文件解析为Spring IoC容器封装的资源,加载多个指定位置的Bean定义资源文件
                Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
                //委派调用其子类XmlBeanDefinitionReader的方法，实现加载功能
                int count = loadBeanDefinitions(resources);
                if (actualResources != null) {
                    Collections.addAll(actualResources, resources);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
                }
                return count;
            }catch (IOException ex) {
                throw new BeanDefinitionStoreException(
                        "Could not resolve bean definition resource pattern [" + location + "]", ex);
            }
        }else {
            //将指定位置的Bean定义资源文件解析为Spring IoC容器封装的资源
            //加载单个指定位置的Bean定义资源文件
            Resource resource = resourceLoader.getResource(location);
            //委派调用其子类XmlBeanDefinitionReader的方法，实现加载功能
            int count = loadBeanDefinitions(resource);
            if (actualResources != null) {
                actualResources.add(resource);
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
            }
            return count;
        }
    }

    //AbstractBeanDefinitionReader:
    public ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    //PathMatchingResourcePatternResolver:
    public Resource[] getResources(String locationPattern) throws IOException {
        Assert.notNull(locationPattern, "Location pattern must not be null");
        //CLASSPATH_ALL_URL_PREFIX = "classpath*:"
        if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
            // a class path resource (multiple resources for same name possible)
            if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
                // a class path resource pattern
                return findPathMatchingResources(locationPattern);
            } else {
                // all class path resources with the given name
                return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
            }
        }
        else {
            // Generally only look for a pattern after a prefix here,
            // and on Tomcat only after the "*/" separator for its "war:" protocol.
            int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
                    locationPattern.indexOf(':') + 1);
            if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
                // a file pattern
                return findPathMatchingResources(locationPattern);
            }
            else {
                // a single resource with the given name
                return new Resource[] {getResourceLoader().getResource(locationPattern)};
            }
        }
    }






}
