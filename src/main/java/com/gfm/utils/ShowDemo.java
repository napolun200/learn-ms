//package com.gfm.utils;
//
//import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
//import jdk.internal.reflect.Reflection;
//import org.aopalliance.aop.Advice;
//import org.aopalliance.intercept.MethodInterceptor;
//import org.aopalliance.intercept.MethodInvocation;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.springframework.aop.*;
//import org.springframework.aop.framework.*;
//import org.springframework.aop.framework.adapter.AdvisorAdapter;
//import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
//import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
//import org.springframework.aop.support.AopUtils;
//import org.springframework.aop.support.DefaultPointcutAdvisor;
//import org.springframework.beans.BeanUtils;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.CachedIntrospectionResults;
//import org.springframework.beans.FatalBeanException;
//import org.springframework.beans.factory.*;
//import org.springframework.beans.factory.config.*;
//import org.springframework.beans.factory.parsing.*;
//import org.springframework.beans.factory.support.*;
//import org.springframework.beans.factory.xml.*;
//import org.springframework.beans.support.ResourceEditorRegistrar;
//import org.springframework.cglib.core.*;
//import org.springframework.cglib.proxy.Callback;
//import org.springframework.cglib.proxy.CallbackFilter;
//import org.springframework.cglib.proxy.Enhancer;
//import org.springframework.context.*;
//import org.springframework.context.event.ApplicationEventMulticaster;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.context.event.SimpleApplicationEventMulticaster;
//import org.springframework.context.expression.StandardBeanExpressionResolver;
//import org.springframework.context.support.*;
//import org.springframework.context.weaving.LoadTimeWeaverAware;
//import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
//import org.springframework.core.*;
//import org.springframework.core.annotation.AnnotationTypeMappings;
//import org.springframework.core.annotation.AnnotationUtils;
//import org.springframework.core.annotation.AnnotationsScanner;
//import org.springframework.core.convert.ConversionService;
//import org.springframework.core.env.EnvironmentCapable;
//import org.springframework.core.env.StandardEnvironment;
//import org.springframework.core.io.*;
//import org.springframework.core.io.support.EncodedResource;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.core.io.support.PropertiesLoaderUtils;
//import org.springframework.core.io.support.ResourcePatternResolver;
//import org.springframework.core.log.LogMessage;
//import org.springframework.lang.Nullable;
//import org.springframework.util.*;
//import org.springframework.util.CollectionUtils;
//import org.springframework.util.xml.DomUtils;
//import org.springframework.util.xml.XmlValidationModeDetector;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.xml.sax.*;
//import org.xml.sax.ErrorHandler;
//
//import javax.management.MBeanServer;
//import javax.management.ObjectName;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.FactoryFinder;
//import javax.xml.parsers.ParserConfigurationException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.lang.management.ManagementFactory;
//import java.lang.ref.WeakReference;
//import java.lang.reflect.*;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.security.AccessController;
//import java.security.PrivilegedExceptionAction;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executor;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
//public class ShowDemo {
//    public static void main(String[] args) {
//
//    }
//
//    //AbstractRefreshableApplicationContext:
//    //告诉子类刷新内部bean工厂
//    protected DefaultListableBeanFactory createBeanFactory() {
//        return new DefaultListableBeanFactory(getInternalParentBeanFactory());
//    }
//
//    //AbstractApplicationContext:
//    protected BeanFactory getInternalParentBeanFactory() {
//        //如果实现了ConfigurableApplicationContext返回父上下文的beanFactory,
//        //否则返回父上下文
//        return (getParent() instanceof ConfigurableApplicationContext ?
//                ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
//    }
//
//    //AbstractApplicationContext:
//    public ApplicationContext getParent() {
//        //ApplicationContext parent； 父上下文
//        return this.parent;
//    }
//
//    //DefaultListableBeanFactory:
//    //通过给定的parentBeanFactory创建DefaultListableBeanFactory实例
//    public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
//        super(parentBeanFactory);
//    }
//
//    //AbstractAutowireCapableBeanFactory:
//    //通过给定的parentBeanFactory创建AbstractAutowireCapableBeanFactory实例
//    public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
//        this();
//        setParentBeanFactory(parentBeanFactory);
//    }
//
//    //AbstractAutowireCapableBeanFactory:
//    public AbstractAutowireCapableBeanFactory() {
//        super();
//        ignoreDependencyInterface(BeanNameAware.class);
//        ignoreDependencyInterface(BeanFactoryAware.class);
//        ignoreDependencyInterface(BeanClassLoaderAware.class);
//    }
//
//    //AbstractAutowireCapableBeanFactory:
//    //忽略依赖的接口去自动装配
//    public void ignoreDependencyInterface(Class<?> ifc) {
//        //忽略依赖检查的集合，Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>()
//        this.ignoredDependencyInterfaces.add(ifc);
//    }
//
//    //AbstractBeanFactory:
//    public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
//        if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
//            throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
//        }
//        this.parentBeanFactory = parentBeanFactory;
//    }
//
//
//    //AbstractRefreshableApplicationContext:
//    //设置一个用于序列化的id
//    public void setSerializationId(@Nullable String serializationId) {
//        if (serializationId != null) {
//            //Map<String, Reference<DefaultListableBeanFactory>> serializableFactories=new ConcurrentHashMap<>(8)
//            serializableFactories.put(serializationId, new WeakReference<>(this));
//        }
//        else if (this.serializationId != null) {
//            serializableFactories.remove(this.serializationId);
//        }
//        this.serializationId = serializationId;
//    }
//
//    //AbstractRefreshableApplicationContext:
//    //通过这个上下文自定义内部beanFactory
//    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
//        if (this.allowBeanDefinitionOverriding != null) {
//            beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
//        }
//        if (this.allowCircularReferences != null) {
//            beanFactory.setAllowCircularReferences(this.allowCircularReferences);
//        }
//    }
//
//
//
//
//    //AbstractXmlApplicationContext:
//    ///实现父类抽象的载入Bean定义方法
//    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
//        //1>创建XmlBeanDefinitionReader，即创建Bean读取器，并通过回调设置到容器中去，容器使用该读取器读取Bean定义资源
//        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
//
//        //使用此上下文的资源加载环境配置bean定义读取器
//        beanDefinitionReader.setEnvironment(this.getEnvironment());
//        //为Bean读取器设置Spring资源加载器，AbstractXmlApplicationContext的
//        //祖先父类AbstractApplicationContext继承DefaultResourceLoader，因此，容器本身也是一个资源加载器
//        beanDefinitionReader.setResourceLoader(this);
//        //2>为Bean读取器设置SAX xml解析器
//        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
//
//        //3>当Bean读取器读取Bean定义的Xml资源文件时，启用Xml的校验机制
//        initBeanDefinitionReader(beanDefinitionReader);
//
//        //4>Bean读取器真正实现加载的方法
//        loadBeanDefinitions(beanDefinitionReader);
//    }
//
//    //XmlBeanDefinitionReader:
//    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
//        super(registry);
//    }
//
//    //AbstractBeanDefinitionReader:
//    protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
//        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
//        this.registry = registry; //registry = BeanDefinitionRegistry
//
//        // Determine ResourceLoader to use.
//        if (this.registry instanceof ResourceLoader) {
//            this.resourceLoader = (ResourceLoader) this.registry;
//        }else {
//            //创建路径匹配的资源加载器，实际上就是获取类加载器
//            this.resourceLoader = new PathMatchingResourcePatternResolver();
//        }
//
//        // Inherit Environment if possible
//        if (this.registry instanceof EnvironmentCapable) {
//            this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
//        }else {
//            this.environment = new StandardEnvironment();
//        }
//    }
//
//    //PathMatchingResourcePatternResolver:
//    public PathMatchingResourcePatternResolver() {
//        this.resourceLoader = new DefaultResourceLoader();
//    }
//
//    //DefaultResourceLoader:
//    public DefaultResourceLoader() {
//        this.classLoader = ClassUtils.getDefaultClassLoader();
//    }
//
//    //ClassUtils:
//    //获取类加载器
//    public static ClassLoader getDefaultClassLoader() {
//        ClassLoader cl = null;
//        try {
//            cl = Thread.currentThread().getContextClassLoader();
//        }catch (Throwable ex) {
//            // Cannot access thread context ClassLoader - falling back...
//        }
//        if (cl == null) {
//            // No thread context class loader -> use class loader of this class.
//            cl = ClassUtils.class.getClassLoader();
//            if (cl == null) {
//                // getClassLoader() returning null indicates the bootstrap ClassLoader
//                try {
//                    cl = ClassLoader.getSystemClassLoader();
//                }catch (Throwable ex) {
//                    // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
//                }
//            }
//        }
//        return cl;
//    }
//
//
//    //ResourceEntityResolver:
//    public ResourceEntityResolver(ResourceLoader resourceLoader) {
//        super(resourceLoader.getClassLoader());
//        this.resourceLoader = resourceLoader;
//    }
//
//    //DelegatingEntityResolver:
//    public DelegatingEntityResolver(@Nullable ClassLoader classLoader) {
//        this.dtdResolver = new BeansDtdResolver();
//        this.schemaResolver = new PluggableSchemaResolver(classLoader);
//    }
//
//
//
//
//    //AbstractXmlApplicationContext:
//    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
//        reader.setValidating(this.validating);
//    }
//
//    //XmlBeanDefinitionReader:
//    public void setValidating(boolean validating) {
//        this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
//        this.namespaceAware = !validating;
//    }
//
//
//    //AbstractXmlApplicationContext:
//    //XmlBeanDefinitionReader加载bean定义资源
//    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
//        //1>获取Bean定义资源的定位
//        Resource[] configResources = getConfigResources();
//        if (configResources != null) {
//            //Xml Bean读取器调用其父类AbstractBeanDefinitionReader读取定位的Bean定义资源
//            reader.loadBeanDefinitions(configResources);
//        }
//        //2>如果子类中获取的Bean定义资源定位为空，则获取ClassPathXmlApplicationContext构造
//        //  方法中setConfigLocations方法设置的资源
//        String[] configLocations = getConfigLocations();
//        if (configLocations != null) {
//            //3>Xml Bean读取器调用其父类AbstractBeanDefinitionReader读取定位的Bean定义资源
//            reader.loadBeanDefinitions(configLocations);
//        }
//    }
//
//    //ClassPathXmlApplicationContext:
//    protected Resource[] getConfigResources() {
//        // Resource[] configResources; 资源数组
//        return this.configResources;
//    }
//
//    //AbstractRefreshableConfigApplicationContext:
//    protected String[] getConfigLocations() {
//        //1>String[] configLocations;  资源名字符串数组
//        //2>getDefaultConfigLocations(); 是一个接口，使用了一个委托模式，调用子类的获取Bean定义资源定位的方法，
//        //  该方法在ClassPathXmlApplicationContext中进行实现,本类中没有实现
//        return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
//    }
//
//    //AbstractBeanDefinitionReader:
//    public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
//        Assert.notNull(locations, "Location array must not be null");
//        int count = 0;
//        for (String location : locations) {
//            count += loadBeanDefinitions(location);
//        }
//        return count;
//    }
//
//    //AbstractBeanDefinitionReader:
//    public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
//        return loadBeanDefinitions(location, null);
//    }
//
//
//
//    //AbstractBeanDefinitionReader:
//    public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
//        //获取在IoC容器初始化过程中设置的资源加载器
//        ResourceLoader resourceLoader = getResourceLoader();
//        if (resourceLoader == null) {
//            throw new BeanDefinitionStoreException(
//                    "Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
//        }
//
//        if (resourceLoader instanceof ResourcePatternResolver) {
//            try {
//                //1>将指定位置的Bean定义资源文件解析为Spring IoC容器封装的资源,加载多个指定位置的Bean定义资源文件
//                Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
//                //2>委派调用其子类XmlBeanDefinitionReader的方法，实现加载功能
//                int count = loadBeanDefinitions(resources);
//                if (actualResources != null) {
//                    Collections.addAll(actualResources, resources);
//                }
//                if (logger.isTraceEnabled()) {
//                    logger.trace("Loaded " + count + " bean definitions from location pattern [" + location + "]");
//                }
//                return count;
//            }catch (IOException ex) {
//                throw new BeanDefinitionStoreException(
//                        "Could not resolve bean definition resource pattern [" + location + "]", ex);
//            }
//        }else {
//            //将指定位置的Bean定义资源文件解析为Spring IoC容器封装的资源
//            //加载单个指定位置的Bean定义资源文件
//            Resource resource = resourceLoader.getResource(location);
//            //委派调用其子类XmlBeanDefinitionReader的方法，实现加载功能
//            int count = loadBeanDefinitions(resource);
//            if (actualResources != null) {
//                actualResources.add(resource);
//            }
//            if (logger.isTraceEnabled()) {
//                logger.trace("Loaded " + count + " bean definitions from location [" + location + "]");
//            }
//            return count;
//        }
//    }
//
//    //AbstractBeanDefinitionReader:
//    public ResourceLoader getResourceLoader() {
//        return this.resourceLoader;
//    }
//
//    //PathMatchingResourcePatternResolver:
//    public Resource[] getResources(String locationPattern) throws IOException {
//        Assert.notNull(locationPattern, "Location pattern must not be null");
//        //CLASSPATH_ALL_URL_PREFIX = "classpath*:"
//        if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
//            //一个类路径资源，多个资源可以使用相同的名字
//            if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
//                // 获取一个匹配的类路径资源
//                return findPathMatchingResources(locationPattern);
//            } else {
//                // all class path resources with the given name
//                return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
//            }
//        }
//        else {
//            // 通常的模式时查找前缀后面的内容
//            // and on Tomcat only after the "*/" separator for its "war:" protocol.
//            int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
//                    locationPattern.indexOf(':') + 1);
//            if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
//                // a file pattern
//                return findPathMatchingResources(locationPattern);
//            }
//            else {
//                // a single resource with the given name
//                return new Resource[] {getResourceLoader().getResource(locationPattern)};
//            }
//        }
//    }
//
//    //PathMatchingResourcePatternResolver:
//    protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
//        String rootDirPath = determineRootDir(locationPattern);
//        String subPattern = locationPattern.substring(rootDirPath.length());
//        Resource[] rootDirResources = getResources(rootDirPath);
//        Set<Resource> result = new LinkedHashSet<>(16);
//        for (Resource rootDirResource : rootDirResources) {
//            rootDirResource = resolveRootDirResource(rootDirResource);
//            URL rootDirUrl = rootDirResource.getURL();
//            if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
//                URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
//                if (resolvedUrl != null) {
//                    rootDirUrl = resolvedUrl;
//                }
//                rootDirResource = new UrlResource(rootDirUrl);
//            }
//            if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
//                result.addAll(PathMatchingResourcePatternResolver.VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
//            }
//            else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
//                result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
//            }
//            else {
//                result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
//            }
//        }
//        if (logger.isTraceEnabled()) {
//            logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
//        }
//        return result.toArray(new Resource[0]);
//    }
//
//    //PathMatchingResourcePatternResolver:
//    protected String determineRootDir(String location) {
//        int prefixEnd = location.indexOf(':') + 1;
//        int rootDirEnd = location.length();
//        while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
//            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
//        }
//        if (rootDirEnd == 0) {
//            rootDirEnd = prefixEnd;
//        }
//        return location.substring(0, rootDirEnd);
//    }
//
//
//    //DefaultResourceLoader:
//    public Resource getResource(String location) {
//        Assert.notNull(location, "Location must not be null");
//        for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
//            Resource resource = protocolResolver.resolve(location, this);
//            if (resource != null) {
//                return resource;
//            }
//        }
//
//        if (location.startsWith("/")) {
//            return getResourceByPath(location);
//        }//如果是类路径的方式，那需要使用ClassPathResource 来得到bean 文件的资源对象
//        else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
//            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
//        } else {
//            try {
//                //如果是URL 方式，使用UrlResource 作为bean 文件的资源对象
//                URL url = new URL(location);
//                return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
//            }catch (MalformedURLException ex) {
//                //如果既不是classpath标识，又不是URL标识的Resource定位，则调用
//                //容器本身的getResourceByPath方法获取Resource
//                return getResourceByPath(location);
//            }
//        }
//    }
//
//
//
//
//
//
//
//
//
//
//    //AbstractBeanDefinitionReader:
//    public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
//        Assert.notNull(resources, "Resource array must not be null");
//        int count = 0;
//        for (Resource resource : resources) {
//            count += loadBeanDefinitions(resource);
//        }
//        return count;
//    }
//
//    //XmlBeanDefinitionReader:
//    //XmlBeanDefinitionReader加载资源的入口方法
//    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
//        //将读入的XML资源进行特殊编码处理
//        return loadBeanDefinitions(new EncodedResource(resource));
//    }
//
//    //XmlBeanDefinitionReader:
//    //这里是载入XML形式Bean定义资源文件方法
//    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
//        Assert.notNull(encodedResource, "EncodedResource must not be null");
//        if (logger.isTraceEnabled()) {
//            logger.trace("Loading XML bean definitions from " + encodedResource);
//        }
//        //ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
//        //			new NamedThreadLocal<>("XML bean definition resources currently being loaded")
//        Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
//        if (currentResources == null) {
//            currentResources = new HashSet<>(4);
//            this.resourcesCurrentlyBeingLoaded.set(currentResources);
//        }
//        if (!currentResources.add(encodedResource)) {
//            throw new BeanDefinitionStoreException(
//                    "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
//        }
//        try {
//            //将资源文件转为InputStream的IO流
//            InputStream inputStream = encodedResource.getResource().getInputStream();
//            try {
//                //从InputStream中得到XML的解析源
//                InputSource inputSource = new InputSource(inputStream);
//                if (encodedResource.getEncoding() != null) {
//                    inputSource.setEncoding(encodedResource.getEncoding());
//                }
//                //1>这里是具体的读取过程
//                return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
//            }finally {
//                //关闭从Resource中得到的IO流
//                inputStream.close();
//            }
//        }catch (IOException ex) {
//            throw new BeanDefinitionStoreException(
//                    "IOException parsing XML document from " + encodedResource.getResource(), ex);
//        }finally {
//            currentResources.remove(encodedResource);
//            if (currentResources.isEmpty()) {
//                this.resourcesCurrentlyBeingLoaded.remove();
//            }
//        }
//    }
//
//    //XmlBeanDefinitionReader:
//    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
//            throws BeanDefinitionStoreException {
//
//        try {
//            //1>将XML文件转换为DOM对象，解析过程由documentLoader实现
//            Document doc = doLoadDocument(inputSource, resource);
//            //2>这里是启动对Bean定义解析的详细过程，该解析过程会用到Spring的Bean配置规则
//            int count = registerBeanDefinitions(doc, resource);
//            if (logger.isDebugEnabled()) {
//                logger.debug("Loaded " + count + " bean definitions from " + resource);
//            }
//            return count;
//        }catch (BeanDefinitionStoreException ex) {
//            throw ex;
//        }catch (SAXParseException ex) {
//            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
//                    "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
//        }catch (SAXException ex) {
//            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
//                    "XML document from " + resource + " is invalid", ex);
//        }catch (ParserConfigurationException ex) {
//            throw new BeanDefinitionStoreException(resource.getDescription(),
//                    "Parser configuration exception parsing XML from " + resource, ex);
//        }catch (IOException ex) {
//            throw new BeanDefinitionStoreException(resource.getDescription(),
//                    "IOException parsing XML document from " + resource, ex);
//        }catch (Throwable ex) {
//            throw new BeanDefinitionStoreException(resource.getDescription(),
//                    "Unexpected exception parsing XML document from " + resource, ex);
//        }
//    }
//
//
//    //XmlBeanDefinitionReader:
//    protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
//        /**
//         * 1.ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger)
//         * 2.protected EntityResolver getEntityResolver() {
//         *      if (this.entityResolver == null) {
//         *          //先使用默认的EntityResolver
//         *          ResourceLoader resourceLoader = getResourceLoader();
//         *          if (resourceLoader != null) {
//         *              this.entityResolver = new ResourceEntityResolver(resourceLoader);
//         *          }
//         *          else {
//         *              this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
//         *          }
//         *      }
//         *      return this.entityResolver;
//         * }
//         *
//         * 3.public boolean isNamespaceAware() {
//         *    return this.namespaceAware;
//         * }
//         */
//        return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
//                getValidationModeForResource(resource), isNamespaceAware());
//    }
//
//    //DefaultDocumentLoader:
//    //使用标准的JAXP将载入的Bean定义资源转换成document对象
//    public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
//                                 ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
//        //1>创建文件解析器工厂
//        DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
//        if (logger.isTraceEnabled()) {
//            logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
//        }
//        //2>创建文档解析器
//        DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
//        //3>解析Spring的Bean定义资源
//        return builder.parse(inputSource);
//    }
//
//    //DefaultDocumentLoader:
//    protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
//            throws ParserConfigurationException {
//        //创建文档解析工厂
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        factory.setNamespaceAware(namespaceAware);
//        //设置解析XML的校验
//        if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
//            factory.setValidating(true);
//            if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
//                // Enforce namespace aware for XSD...
//                factory.setNamespaceAware(true);
//                try {
//                    factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
//                }catch (IllegalArgumentException ex) {
//                    ParserConfigurationException pcex = new ParserConfigurationException("Unable to validate using XSD");
//                    pcex.initCause(ex);
//                    throw pcex;
//                }
//            }
//        }
//        return factory;
//    }
//
//    //DocumentBuilderFactory:
//    public static DocumentBuilderFactory newInstance() {
//        return FactoryFinder.find(
//                /* The default property name according to the JAXP spec */
//                DocumentBuilderFactory.class, // "javax.xml.parsers.DocumentBuilderFactory"
//                /* The fallback implementation class name */
//                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
//    }
//
//
//    //DefaultDocumentLoader:
//    protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory,
//                                                    @Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
//            throws ParserConfigurationException {
//
//        DocumentBuilder docBuilder = factory.newDocumentBuilder();
//        if (entityResolver != null) {
//            docBuilder.setEntityResolver(entityResolver);
//        }
//        if (errorHandler != null) {
//            docBuilder.setErrorHandler(errorHandler);
//        }
//        return docBuilder;
//    }
//
//
//
//
//    //DocumentBuilderImpl:
//    //将输入流解析成Document对象
//    public Document parse(InputSource is) throws SAXException, IOException {
//        if (is == null) {
//            throw new IllegalArgumentException(
//                    DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN,
//                            "jaxp-null-input-source", null));
//        }
//        //XMLComponent fSchemaValidator;
//        if (fSchemaValidator != null) {
//            //ValidationManager fSchemaValidationManager
//            if (fSchemaValidationManager != null) {
//                fSchemaValidationManager.reset();
//                fUnparsedEntityHandler.reset();
//            }
//            resetSchemaValidator();
//        }
//        //DOMParser domParser
//        domParser.parse(is);
//        Document doc = domParser.getDocument();
//        //清除document引用，实际上就是将其置为null, 能够被GC回收
//        domParser.dropDocumentReferences();
//        return doc;
//    }
//
//    //AbstractDOMParser
//    public final void dropDocumentReferences() {
//        fDocument = null;
//        fDocumentImpl = null;
//        fDeferredDocumentImpl = null;
//        fDocumentType = null;
//        fCurrentNode = null;
//        fCurrentCDATASection = null;
//        fCurrentEntityDecl = null;
//        fRoot = null;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
//        //解析<Bean>元素的入口
//        return parseBeanDefinitionElement(ele, null);
//    }
//
//    //BeanDefinitionParserDelegate:
//    //解析Bean定义资源文件中的<Bean>元素，这个方法中主要处理<Bean>元素的id，name和别名属性
//    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
//        String id = ele.getAttribute(ID_ATTRIBUTE); //获取<Bean>元素中的id属性值
//        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE); //获取<Bean>元素中的name属性值
//
//        //获取<Bean>元素中的alias属性值
//        List<String> aliases = new ArrayList<>();
//        if (StringUtils.hasLength(nameAttr)) {
//            //String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; "
//            String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
//            aliases.addAll(Arrays.asList(nameArr));
//        }
//
//        String beanName = id;
//        //如果<Bean>元素中没有配置id属性时，将别名中的第一个值赋值给beanName
//        if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
//            beanName = aliases.remove(0);
//            if (logger.isTraceEnabled()) {
//                logger.trace("No XML 'id' specified - using '" + beanName +
//                        "' as bean name and " + aliases + " as aliases");
//            }
//        }
//        //检查<Bean>元素所配置的id或者name的唯一性，containingBean标识<Bean>元素中是否包含子<Bean>元素
//        if (containingBean == null) {
//            checkNameUniqueness(beanName, aliases, ele);
//        }
//
//        //详细对<Bean>元素中配置的Bean定义进行解析的地方
//        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
//        if (beanDefinition != null) {
//            if (!StringUtils.hasText(beanName)) {
//                try {
//                    if (containingBean != null) {
//                        //如果<Bean>元素中没有配置id、别名或者name，且没有包含子<Bean>元素，为解析的Bean生成一个唯一beanName并注册
//                        beanName = BeanDefinitionReaderUtils.generateBeanName(
//                                beanDefinition, this.readerContext.getRegistry(), true);
//                    }else {
//                        //如果<Bean>元素中没有配置id、别名或者name，且包含了子<Bean>元素，为解析的Bean使用别名向IoC容器注册
//                        beanName = this.readerContext.generateBeanName(beanDefinition);
//                        //为解析的Bean使用别名注册时，为了向后兼容 Spring1.2/2.0，给别名添加类名后缀
//                        String beanClassName = beanDefinition.getBeanClassName();
//                        if (beanClassName != null &&
//                                beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
//                                !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
//                            aliases.add(beanClassName);
//                        }
//                    }
//                    if (logger.isTraceEnabled()) {
//                        logger.trace("Neither XML 'id' nor 'name' specified - " +
//                                "using generated bean name [" + beanName + "]");
//                    }
//                }catch (Exception ex) {
//                    error(ex.getMessage(), ele);
//                    return null;
//                }
//            }
//            String[] aliasesArray = StringUtils.toStringArray(aliases);
//            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
//        }
//
//        return null;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
//        String foundName = null;
//
//        if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
//            foundName = beanName;
//        }
//        if (foundName == null) {
//            foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
//        }
//        if (foundName != null) {
//            error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
//        }
//
//        //Set<String> usedNames = new HashSet<>(), 存储所有bean的名称，包括别名
//        this.usedNames.add(beanName);
//        this.usedNames.addAll(aliases);
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public AbstractBeanDefinition parseBeanDefinitionElement(
//            Element ele, String beanName, @Nullable BeanDefinition containingBean) {
//
//        this.parseState.push(new BeanEntry(beanName));
//
//        String className = null;
//        if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
//            className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
//        }
//        String parent = null;
//        if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
//            parent = ele.getAttribute(PARENT_ATTRIBUTE);
//        }
//
//        try {
//            AbstractBeanDefinition bd = createBeanDefinition(className, parent);
//
//            parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
//            bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
//
//            parseMetaElements(ele, bd);
//            parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
//            parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
//
//            parseConstructorArgElements(ele, bd);
//            parsePropertyElements(ele, bd);
//            parseQualifierElements(ele, bd);
//
//            bd.setResource(this.readerContext.getResource());
//            bd.setSource(extractSource(ele));
//
//            return bd;
//        }catch (ClassNotFoundException ex) {
//            error("Bean class [" + className + "] not found", ele, ex);
//        }catch (NoClassDefFoundError err) {
//            error("Class that bean class [" + className + "] depends on not found", ele, err);
//        }catch (Throwable ex) {
//            error("Unexpected failure during bean definition parsing", ele, ex);
//        }finally {
//            this.parseState.pop();
//        }
//
//        return null;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
//        NodeList nl = beanEle.getChildNodes();
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            //CONSTRUCTOR_ARG_ELEMENT = "constructor-arg"
//            if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
//                parseConstructorArgElement((Element) node, bd);
//            }
//        }
//    }
//
//    //BeanDefinitionParserDelegate:
//    public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
//        String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE); //INDEX_ATTRIBUTE = "index"
//        String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE); //TYPE_ATTRIBUTE = "type"
//        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE); //NAME_ATTRIBUTE = "name"
//        if (StringUtils.hasLength(indexAttr)) {
//            try {
//                int index = Integer.parseInt(indexAttr);
//                if (index < 0) {
//                    error("'index' cannot be lower than 0", ele);
//                }else {
//                    try {
//                        //parseState ==> ParseState.state = LinkedList<Entry>
//                        this.parseState.push(new ConstructorArgumentEntry(index));
//                        Object value = parsePropertyValue(ele, bd, null);
//                        ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
//                        if (StringUtils.hasLength(typeAttr)) {
//                            valueHolder.setType(typeAttr);
//                        }
//                        if (StringUtils.hasLength(nameAttr)) {
//                            valueHolder.setName(nameAttr);
//                        }
//                        valueHolder.setSource(extractSource(ele));
//                        if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
//                            error("Ambiguous constructor-arg entries for index " + index, ele);
//                        }
//                        else {
//                            bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
//                        }
//                    }finally {
//                        this.parseState.pop();
//                    }
//                }
//            }catch (NumberFormatException ex) {
//                error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
//            }
//        }else {
//            try {
//                this.parseState.push(new ConstructorArgumentEntry());
//                Object value = parsePropertyValue(ele, bd, null);
//                ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
//                if (StringUtils.hasLength(typeAttr)) {
//                    valueHolder.setType(typeAttr);
//                }
//                if (StringUtils.hasLength(nameAttr)) {
//                    valueHolder.setName(nameAttr);
//                }
//                valueHolder.setSource(extractSource(ele));
//                bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
//            }finally {
//                this.parseState.pop();
//            }
//        }
//    }
//
//
//    public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
//        String elementName = (propertyName != null ?
//                "<property> element for property '" + propertyName + "'" :
//                "<constructor-arg> element");
//
//        // Should only have one child element: ref, value, list, etc.
//        NodeList nl = ele.getChildNodes();
//        Element subElement = null;
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
//                    !nodeNameEquals(node, META_ELEMENT)) {
//                // Child element is what we're looking for.
//                if (subElement != null) {
//                    error(elementName + " must not contain more than one sub-element", ele);
//                }else {
//                    subElement = (Element) node;
//                }
//            }
//        }
//
//        boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
//        boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
//        if ((hasRefAttribute && hasValueAttribute) ||
//                ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
//            error(elementName +
//                    " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
//        }
//
//        if (hasRefAttribute) {
//            String refName = ele.getAttribute(REF_ATTRIBUTE);
//            if (!StringUtils.hasText(refName)) {
//                error(elementName + " contains empty 'ref' attribute", ele);
//            }
//            RuntimeBeanReference ref = new RuntimeBeanReference(refName);
//            ref.setSource(extractSource(ele));
//            return ref;
//        } else if (hasValueAttribute) {
//            TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
//            valueHolder.setSource(extractSource(ele));
//            return valueHolder;
//        }
//        else if (subElement != null) {
//            return parsePropertySubElement(subElement, bd);
//        }
//        else {
//            // Neither child element nor "ref" or "value" attribute found.
//            error(elementName + " must specify a ref or value", ele);
//            return null;
//        }
//    }
//
//
//
//
//    //BeanDefinitionReaderUtils:
//    //注册BeanDefinition到BeanFactory
//    public static void registerBeanDefinition(
//            BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
//            throws BeanDefinitionStoreException {
//
//        //在主名称下注册bean定义
//        String beanName = definitionHolder.getBeanName();
//        registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());
//
//        //为bean名称注册别名(如果有)
//        String[] aliases = definitionHolder.getAliases();
//        if (aliases != null) {
//            for (String alias : aliases) {
//                registry.registerAlias(beanName, alias);
//            }
//        }
//    }
//
//    //DefaultListableBeanFactory:
//    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
//            throws BeanDefinitionStoreException {
//        Assert.hasText(beanName, "Bean name must not be empty");
//        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
//
//        if (beanDefinition instanceof AbstractBeanDefinition) {
//            try {
//                //校验BeanDefinition
//                ((AbstractBeanDefinition) beanDefinition).validate();
//            }catch (BeanDefinitionValidationException ex) {
//                throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
//                        "Validation of bean definition failed", ex);
//            }
//        }
//
//        //根据beanName获取BeanDefinition容器里的BeanDefinition
//        //Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256), key=beanName, value=BeanDefinition
//        BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
//        if (existingDefinition != null) { //如果此BeanDefinition已经存在
//            //如果不允许覆盖同名的BeanDefinition, 就抛出异常
//            if (!isAllowBeanDefinitionOverriding()) {
//                throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
//            }else if (existingDefinition.getRole() < beanDefinition.getRole()) {
//                // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
//                if (logger.isInfoEnabled()) {
//                    logger.info("Overriding user-defined bean definition for bean '" + beanName +
//                            "' with a framework-generated bean definition: replacing [" +
//                            existingDefinition + "] with [" + beanDefinition + "]");
//                }
//            }else if (!beanDefinition.equals(existingDefinition)) {
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Overriding bean definition for bean '" + beanName +
//                            "' with a different definition: replacing [" + existingDefinition +
//                            "] with [" + beanDefinition + "]");
//                }
//            }else {
//                if (logger.isTraceEnabled()) {
//                    logger.trace("Overriding bean definition for bean '" + beanName +
//                            "' with an equivalent definition: replacing [" + existingDefinition +
//                            "] with [" + beanDefinition + "]");
//                }
//            }
//            //允许覆盖同名BeanDefinition, 就用新的BeanDefinition替换原来的
//            this.beanDefinitionMap.put(beanName, beanDefinition);
//        }else { //如果此BeanDefinition在容器里不存在
//            if (hasBeanCreationStarted()) { //bean已经在工厂里创建
//                //防止其他线程同步修改BeanDefinition容器
//                synchronized (this.beanDefinitionMap) {
//                    this.beanDefinitionMap.put(beanName, beanDefinition);
//                    List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
//                    //将新创建的bean定义名称添加到已有的bean定义名称列表中
//                    //List<String> beanDefinitionNames = new ArrayList<>(256), bean定义名称列表，按注册顺序排列.
//                    updatedDefinitions.addAll(this.beanDefinitionNames);
//                    updatedDefinitions.add(beanName);
//                    this.beanDefinitionNames = updatedDefinitions;
//                    removeManualSingletonName(beanName);
//                }
//            }
//            else {
//                //仍在启动注册阶段
//                this.beanDefinitionMap.put(beanName, beanDefinition);
//                this.beanDefinitionNames.add(beanName);
//                removeManualSingletonName(beanName);
//            }
//            this.frozenBeanDefinitionNames = null;
//        }
//
//        if (existingDefinition != null || containsSingleton(beanName)) {
//            resetBeanDefinition(beanName);
//        }
//    }
//
//
//    //AbstractBeanDefinition:
//    //校验BeanDefinition
//    public void validate() throws BeanDefinitionValidationException {
//        if (hasMethodOverrides() && getFactoryMethodName() != null) {
//            throw new BeanDefinitionValidationException(
//                    "Cannot combine factory method with container-generated method overrides: " +
//                            "the factory method must create the concrete bean instance.");
//        }
//        if (hasBeanClass()) {
//            prepareMethodOverrides();
//        }
//    }
//
//    //AbstractBeanDefinition:
//    public void prepareMethodOverrides() throws BeanDefinitionValidationException {
//        //校验查找方法是否存在并确定它们的重载状态
//        if (hasMethodOverrides()) {
//            getMethodOverrides().getOverrides().forEach(this::prepareMethodOverride);
//        }
//    }
//
//    //AbstractBeanDefinition:
//    protected void prepareMethodOverride(MethodOverride mo) throws BeanDefinitionValidationException {
//        int count = ClassUtils.getMethodCountForName(getBeanClass(), mo.getMethodName());
//        if (count == 0) {
//            throw new BeanDefinitionValidationException(
//                    "Invalid method override: no method with name '" + mo.getMethodName() +
//                            "' on class [" + getBeanClassName() + "]");
//        }else if (count == 1) {
//            //标记此方法没有被重载
//            mo.setOverloaded(false);
//        }
//    }
//
//
//    //---------------------------------
//
//    //AbstractBeanFactory:
//    //校验这个工厂的bean是否已经开始创建
//    protected boolean hasBeanCreationStarted() {
//        //至少已经创建过一次的bean的名称的集合
//        //Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256))
//        return !this.alreadyCreated.isEmpty();
//    }
//
//    //DefaultListableBeanFactory:
//    private void removeManualSingletonName(String beanName) {
//        updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
//    }
//
//
//    //DefaultListableBeanFactory:
//    //更新工厂的内部手动单例名称集
//    private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
//        if (hasBeanCreationStarted()) {
//            // Cannot modify startup-time collection elements anymore (for stable iteration)
//            synchronized (this.beanDefinitionMap) {
//                //Set<String> manualSingletonNames = new LinkedHashSet<>(16)；手动注册的单例名称集，按注册顺序排列
//                if (condition.test(this.manualSingletonNames)) {
//                    Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
//                    action.accept(updatedSingletons);
//                    this.manualSingletonNames = updatedSingletons;
//                }
//            }
//        }
//        else {
//            //还处于启动注册阶段
//            if (condition.test(this.manualSingletonNames)) {
//                action.accept(this.manualSingletonNames);
//            }
//        }
//    }
//
//
//    //DefaultListableBeanFactory:
//    public boolean containsSingleton(String beanName) {
//        //Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256)
//        //单例对象的缓存:key=bean名, value=bean实例
//        return this.singletonObjects.containsKey(beanName);
//    }
//
//
//    //DefaultListableBeanFactory:
//    //重置给定bean的所有bean定义缓存，包括从它派生的bean的缓存
//    protected void resetBeanDefinition(String beanName) {
//        // 删除已创建的给定bean的合并bean定义
//        clearMergedBeanDefinition(beanName);
//
//        //从单例缓存中删除对应的bean(如果有的话)。通常不应该
//        //是必需的，而不仅仅意味着要覆盖上下文的默认bean
//        destroySingleton(beanName);
//
//        // 通知所有post-processors指定的bean定义已被重置
//        for (BeanPostProcessor processor : getBeanPostProcessors()) {
//            if (processor instanceof MergedBeanDefinitionPostProcessor) {
//                ((MergedBeanDefinitionPostProcessor) processor).resetBeanDefinition(beanName);
//            }
//        }
//
//        // 重置所有将给定bean作为父bean的bean定义(递归地)
//        for (String bdName : this.beanDefinitionNames) {
//            if (!beanName.equals(bdName)) {
//                BeanDefinition bd = this.beanDefinitionMap.get(bdName);
//                if (bd != null && beanName.equals(bd.getParentName())) {
//                    resetBeanDefinition(bdName);
//                }
//            }
//        }
//    }
//
//    //AbstractBeanFactory:
//    //删除指定bean的合并bean定义，在next acces上重新创建它
//    protected void clearMergedBeanDefinition(String beanName) {
//        //Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256)
//        //从bean名称映射到合并的RootBeanDefinition
//        RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
//        if (bd != null) {
//            //boolean stale; 确定是否需要重新合并定义
//            bd.stale = true;
//        }
//    }
//
//    //DefaultListableBeanFactory:
//    public void destroySingleton(String beanName) {
//        super.destroySingleton(beanName);
//        removeManualSingletonName(beanName);
//        clearByTypeCache();
//    }
//
//    //DefaultListableBeanFactory:
//    private void clearByTypeCache() {
//        //Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64)
//        //Bean类型对应的所有beanName数组，key=ClassType, value = beanName数组
//        this.allBeanNamesByType.clear();
//        //Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64)
//        //通过Bean 类型依赖的所有所有单例Bean数组map, key=ClassType, value=所有单例BeanName数组
//        this.singletonBeanNamesByType.clear();
//    }
//
//    //DefaultSingletonBeanRegistry:
//    public void destroySingleton(String beanName) {
//        //删除已注册的给定名称单例(如果有的话)
//        removeSingleton(beanName);
//
//        //销毁相应的DisposableBean实例
//        DisposableBean disposableBean;
//        // Map<String, Object> disposableBeans = new LinkedHashMap<>()
//        //包含处理bean实例的map， 映射为：beanName --> disposableBean
//        synchronized (this.disposableBeans) {
//            disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
//        }
//        destroyBean(beanName, disposableBean);
//    }
//
//    //DefaultSingletonBeanRegistry:
//    protected void removeSingleton(String beanName) {
//        //Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256)
//        //缓存的单例对象，beanName --> bean instance
//        synchronized (this.singletonObjects) {
//            this.singletonObjects.remove(beanName);
//            //Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16)
//            //单例工厂的缓存: beanName --> ObjectFactory
//            this.singletonFactories.remove(beanName);
//            //Map<String, Object> earlySingletonObjects = new HashMap<>(16)
//            //早期单例对象的缓存: beanName --> bean instance
//            this.earlySingletonObjects.remove(beanName);
//            //Set<String> registeredSingletons = new LinkedHashSet<>(256)
//            //已注册的单例集合，包含按注册顺序排列的bean名称
//            this.registeredSingletons.remove(beanName);
//        }
//    }
//
//    //DefaultSingletonBeanRegistry:
//    //销毁给定的bean。必须在bean销毁之前销毁依赖于它本身的bean。不应该抛出任何异常。
//    protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
//        //首先触发从属bean的销毁
//        Set<String> dependencies;
//        //Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64)
//        //beanName之间依赖关系的集合, key=beanName, value=依赖这个Bean的beanName集合
//        synchronized (this.dependentBeanMap) {
//            dependencies = this.dependentBeanMap.remove(beanName);
//        }
//        if (dependencies != null) {
//            if (logger.isTraceEnabled()) {
//                logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
//            }
//            for (String dependentBeanName : dependencies) {
//                //递归调用，删除bean的依赖BeanName集合
//                destroySingleton(dependentBeanName);
//            }
//        }
//
//        // 真正开始销毁bean
//        if (bean != null) {
//            try {
//                bean.destroy();
//            }catch (Throwable ex) {
//                if (logger.isWarnEnabled()) {
//                    logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
//                }
//            }
//        }
//
//        // 触发销毁包含的beans
//        Set<String> containedBeans;
//        //Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16)
//        //一个Bean包含的所有Bean的名字集合map， key=beanName, value=包含的所有bean的name集合
//        synchronized (this.containedBeanMap) {
//            // Within full synchronization in order to guarantee a disconnected Set
//            containedBeans = this.containedBeanMap.remove(beanName);
//        }
//        if (containedBeans != null) {
//            for (String containedBeanName : containedBeans) {
//                destroySingleton(containedBeanName);
//            }
//        }
//
//        //从其他beans的依赖里移除销毁的bean
//        synchronized (this.dependentBeanMap) {
//            for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
//                Map.Entry<String, Set<String>> entry = it.next();
//                Set<String> dependenciesToClean = entry.getValue();
//                dependenciesToClean.remove(beanName);
//                if (dependenciesToClean.isEmpty()) {
//                    it.remove();
//                }
//            }
//        }
//
//        //删除销毁bean的准备依赖信息
//        //Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64)
//        //依赖于指定bean的其他BeanName集合， key=beanName, value=依赖此bean的其他BeanName集合
//        this.dependenciesForBeanMap.remove(beanName);
//    }
//
//    //DisposableBeanAdapter:
//    public void destroy() {
//        //如果实现了DestructionAwareBeanPostProcessor， 就调用它的postProcessBeforeDestruction方法
//        if (!CollectionUtils.isEmpty(this.beanPostProcessors)) {
//            for (DestructionAwareBeanPostProcessor processor : this.beanPostProcessors) {
//                processor.postProcessBeforeDestruction(this.bean, this.beanName);
//            }
//        }
//
//        //调用bean的默认destroy()方法
//        if (this.invokeDisposableBean) {
//            if (logger.isTraceEnabled()) {
//                logger.trace("Invoking destroy() on bean with name '" + this.beanName + "'");
//            }
//            try {
//                if (System.getSecurityManager() != null) {
//                    AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
//                        ((DisposableBean) this.bean).destroy();
//                        return null;
//                    }, this.acc);
//                }else {
//                    ((DisposableBean) this.bean).destroy();
//                }
//            }catch (Throwable ex) {
//                String msg = "Invocation of destroy method failed on bean with name '" + this.beanName + "'";
//                if (logger.isDebugEnabled()) {
//                    logger.warn(msg, ex);
//                }else {
//                    logger.warn(msg + ": " + ex);
//                }
//            }
//        }
//
//        //如果有实现自定义的destroy方法，就调用该destroy方法
//        if (this.destroyMethod != null) {
//            invokeCustomDestroyMethod(this.destroyMethod);
//        } else if (this.destroyMethodName != null) {
//            Method methodToInvoke = determineDestroyMethod(this.destroyMethodName);
//            if (methodToInvoke != null) {
//                invokeCustomDestroyMethod(ClassUtils.getInterfaceMethodIfPossible(methodToInvoke));
//            }
//        }
//    }
//
//
//
//
//    //GenericApplicationContext:
//    public void registerAlias(String beanName, String alias) {
//        this.beanFactory.registerAlias(beanName, alias);
//    }
//
//    //SimpleAliasRegistry:
//    public void registerAlias(String name, String alias) {
//        Assert.hasText(name, "'name' must not be empty");
//        Assert.hasText(alias, "'alias' must not be empty");
//        //Map<String, String> aliasMap = new ConcurrentHashMap<>(16)
//        //从别名映射到规范名称的map
//        synchronized (this.aliasMap) {
//            if (alias.equals(name)) {
//                this.aliasMap.remove(alias);
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
//                }
//            } else {
//                String registeredName = this.aliasMap.get(alias);
//                if (registeredName != null) {
//                    if (registeredName.equals(name)) {
//                        //别名已经存在，不需要重新注册
//                        return;
//                    }
//                    //如果不允许别名覆盖，就抛出异常
//                    if (!allowAliasOverriding()) {
//                        throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" +
//                                name + "': It is already registered for name '" + registeredName + "'.");
//                    }
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("Overriding alias '" + alias + "' definition for registered name '" +
//                                registeredName + "' with new target name '" + name + "'");
//                    }
//                }
//                //循环调用判断别名是否已经存在
//                checkForAliasCircle(name, alias);
//                this.aliasMap.put(alias, name);
//                if (logger.isTraceEnabled()) {
//                    logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
//                }
//            }
//        }
//    }
//
//    //SimpleAliasRegistry:
//    protected void checkForAliasCircle(String name, String alias) {
//        if (hasAlias(alias, name)) {
//            throw new IllegalStateException("Cannot register alias '" + alias +
//                    "' for name '" + name + "': Circular reference - '" +
//                    name + "' is a direct or indirect alias for '" + alias + "' already");
//        }
//    }
//
//    //SimpleAliasRegistry:
//    public boolean hasAlias(String name, String alias) {
//        for (Map.Entry<String, String> entry : this.aliasMap.entrySet()) {
//            String registeredName = entry.getValue();
//            if (registeredName.equals(name)) {
//                String registeredAlias = entry.getKey();
//                if (registeredAlias.equals(alias) || hasAlias(registeredAlias, alias)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//
////-------------------------------------------
//
//    //BeanDefinitionParserDelegate:
//    //解析自定义元素
//    public BeanDefinition parseCustomElement(Element ele) {
//        return parseCustomElement(ele, null);
//    }
//
//    //BeanDefinitionParserDelegate:
//    //解析自定义元素(在默认名称空间之外)
//    public BeanDefinition parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
//        String namespaceUri = getNamespaceURI(ele);  //==> ele.getNamespaceURI()
//        if (namespaceUri == null) {
//            return null;
//        }
//        //用名称空间解析器解析namespaceUri，获取名称空间处理器
//        //readerContext.getNamespaceHandlerResolver() ==> (NamespaceHandlerResolver)XmlReaderContext.namespaceHandlerResolver
//        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
//        if (handler == null) {
//            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
//            return null;
//        }
//        return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
//    }
//
//    //DefaultNamespaceHandlerResolver:
//    //从配置的映射中找到支持的名称空间URI
//    public NamespaceHandler resolve(String namespaceUri) {
//        //获取处理器映射容器map, 如果此URI已经有处理器就直接返回
//        Map<String, Object> handlerMappings = getHandlerMappings();
//        Object handlerOrClassName = handlerMappings.get(namespaceUri);
//        if (handlerOrClassName == null) {
//            return null;
//        }else if (handlerOrClassName instanceof NamespaceHandler) {
//            return (NamespaceHandler) handlerOrClassName;
//        }else {
//            String className = (String) handlerOrClassName;
//            try {
//                Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
//                if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
//                    throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
//                            "] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
//                }
//                //实例化NamespaceHandler
//                NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
//                namespaceHandler.init();
//                handlerMappings.put(namespaceUri, namespaceHandler);
//                return namespaceHandler;
//            }catch (ClassNotFoundException ex) {
//                throw new FatalBeanException("Could not find NamespaceHandler class [" + className +
//                        "] for namespace [" + namespaceUri + "]", ex);
//            }catch (LinkageError err) {
//                throw new FatalBeanException("Unresolvable class definition for NamespaceHandler class [" +
//                        className + "] for namespace [" + namespaceUri + "]", err);
//            }
//        }
//    }
//
//    //DefaultNamespaceHandlerResolver:
//    //懒加载指定的NamespaceHandler映射
//    private Map<String, Object> getHandlerMappings() {
//        //Map<String, Object> handlerMappings, 名称空间URI到处理器的映射map
//        Map<String, Object> handlerMappings = this.handlerMappings;
//        if (handlerMappings == null) {
//            synchronized (this) {
//                handlerMappings = this.handlerMappings;
//                if (handlerMappings == null) {
//                    if (logger.isTraceEnabled()) {
//                        logger.trace("Loading NamespaceHandler mappings from [" + this.handlerMappingsLocation + "]");
//                    }
//                    try {
//                        Properties mappings = PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
//                        if (logger.isTraceEnabled()) {
//                            logger.trace("Loaded NamespaceHandler mappings: " + mappings);
//                        }
//                        handlerMappings = new ConcurrentHashMap<>(mappings.size());
//                        CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
//                        this.handlerMappings = handlerMappings;
//                    }catch (IOException ex) {
//                        throw new IllegalStateException(
//                                "Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
//                    }
//                }
//            }
//        }
//        return handlerMappings;
//    }
//
//
//
//    //NamespaceHandlerSupport:
//    public BeanDefinition parse(Element element, ParserContext parserContext) {
//        BeanDefinitionParser parser = findParserForElement(element, parserContext);
//        return (parser != null ? parser.parse(element, parserContext) : null);
//    }
//
//    //NamespaceHandlerSupport:
//    private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
//        //BeanDefinitionParserDelegate.getLocalName(element)
//        String localName = parserContext.getDelegate().getLocalName(element);
//        //Map<String, BeanDefinitionParser> parsers = new HashMap<>()
//        BeanDefinitionParser parser = this.parsers.get(localName);
//        if (parser == null) {
//            parserContext.getReaderContext().fatal(
//                    "Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
//        }
//        return parser;
//    }
//
//    //AbstractBeanDefinitionParser:
//    public final BeanDefinition parse(Element element, ParserContext parserContext) {
//        //将Element解析成BeanDefinition
//        AbstractBeanDefinition definition = parseInternal(element, parserContext);
//        if (definition != null && !parserContext.isNested()) {
//            try {
//                String id = resolveId(element, definition, parserContext);
//                if (!StringUtils.hasText(id)) {
//                    parserContext.getReaderContext().error(
//                            "Id is required for element '" + parserContext.getDelegate().getLocalName(element)
//                                    + "' when used as a top-level tag", element);
//                }
//                String[] aliases = null;
//                if (shouldParseNameAsAliases()) {
//                    String name = element.getAttribute(NAME_ATTRIBUTE);
//                    if (StringUtils.hasLength(name)) {
//                        aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
//                    }
//                }
//                //构建处理BeanDefinition的BeanDefinitionHolder
//                BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id, aliases);
//                //向BeanFactory注册BeanDefinition
//                registerBeanDefinition(holder, parserContext.getRegistry());
//                if (shouldFireEvents()) {
//                    BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
//                    postProcessComponentDefinition(componentDefinition);
//                    //注册组件
//                    parserContext.registerComponent(componentDefinition);
//                }
//            } catch (BeanDefinitionStoreException ex) {
//                String msg = ex.getMessage();
//                parserContext.getReaderContext().error((msg != null ? msg : ex.toString()), element);
//                return null;
//            }
//        }
//        return definition;
//    }
//
//    //AbstractSingleBeanDefinitionParser:
//    protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
//        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
//        String parentName = getParentName(element);
//        if (parentName != null) {
//            builder.getRawBeanDefinition().setParentName(parentName);
//        }
//        Class<?> beanClass = getBeanClass(element);
//        if (beanClass != null) {
//            builder.getRawBeanDefinition().setBeanClass(beanClass);
//        } else {
//            String beanClassName = getBeanClassName(element);
//            if (beanClassName != null) {
//                builder.getRawBeanDefinition().setBeanClassName(beanClassName);
//            }
//        }
//        builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
//        // (BeanDefinition) ParserContext.containingBeanDefinition
//        BeanDefinition containingBd = parserContext.getContainingBeanDefinition();
//        if (containingBd != null) {
//            //内部bean定义必须接收与包含bean相同的作用域
//            builder.setScope(containingBd.getScope());
//        }
//        if (parserContext.isDefaultLazyInit()) {
//            //设置默认开启懒加载
//            builder.setLazyInit(true);
//        }
//        //真正开始解析配xml的Element
//        doParse(element, parserContext, builder);
//        return builder.getBeanDefinition();
//    }
//
//    //AbstractSingleBeanDefinitionParser:
//    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
//        doParse(element, builder);
//    }
//
//    //AbstractSingleBeanDefinitionParser:
//    //有自定义的需求就子类继承并实现此方法
//    protected void doParse(Element element, BeanDefinitionBuilder builder) {
//    }
//
//    //ParserContext:
//    public void registerComponent(ComponentDefinition component) {
//        CompositeComponentDefinition containingComponent = getContainingComponent();
//        if (containingComponent != null) {
//            //List<ComponentDefinition> nestedComponents = new ArrayList<>()
//            containingComponent.addNestedComponent(component);
//        }else {
//            //触发一个组件注册事件
//            this.readerContext.fireComponentRegistered(component);
//        }
//    }
//
//    //CompositeComponentDefinition:
//    public void fireComponentRegistered(ComponentDefinition componentDefinition) {
//        //ReaderEventListener eventListener
//        this.eventListener.componentRegistered(componentDefinition);
//    }
//
//    /**
//     * ---------------------------------------------------------------------
//     */
//    //AbstractApplicationContext.obtainFreshBeanFactory
//
//    //AbstractRefreshableApplicationContext:
//    public final ConfigurableListableBeanFactory getBeanFactory() {
//        synchronized (this.beanFactoryMonitor) {
//            if (this.beanFactory == null) {
//                throw new IllegalStateException("BeanFactory not initialized or already closed - " +
//                        "call 'refresh' before accessing beans via the ApplicationContext");
//            }
//            //DefaultListableBeanFactory beanFactory
//            return this.beanFactory;
//        }
//    }
//
//    //AbstractApplicationContext:
//    //配置工厂的标准上下文特征，比如上下文的类加载器和后处理程序
//    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
//        //告诉内部bean工厂使用上下文的类装入器等
//        beanFactory.setBeanClassLoader(getClassLoader());
//        beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
//        //Set<PropertyEditorRegistrar> propertyEditorRegistrars = new LinkedHashSet<>(4)
//        beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));
//
//        //使用上下文回调配置bean工厂
//        beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
//        beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
//        beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
//        beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
//        beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
//        beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
//        beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
//
//        //忽略给定的自动装配依赖接口,在普通工厂中BeanFactory接口作为用于解析的类型不注册
//        //MessageSource作为一个Bean注册
//        beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
//        beanFactory.registerResolvableDependency(ResourceLoader.class, this);
//        beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
//        beanFactory.registerResolvableDependency(ApplicationContext.class, this);
//
//        // Register early post-processor for detecting inner beans as ApplicationListeners.
//        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));
//
//        // Detect a LoadTimeWeaver and prepare for weaving, if found.
//        if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
//            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
//            // Set a temporary ClassLoader for type matching.
//            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
//        }
//
//        //注册默认环境bean
//        if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
//            //ENVIRONMENT_BEAN_NAME = "environment"
//            beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
//        }
//        //SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties"
//        if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
//            beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
//        }
//        //SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment"
//        if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
//            beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
//        }
//    }
//
//    //AbstractBeanFactory:
//    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
//        Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
//        // List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>()
//        //移除老的BeanPostProcessor(如果有的话)
//        this.beanPostProcessors.remove(beanPostProcessor);
//        //跟踪它是否支持实例化/销毁
//        if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
//            this.hasInstantiationAwareBeanPostProcessors = true;
//        }
//        if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
//            this.hasDestructionAwareBeanPostProcessors = true;
//        }
//        //重新添加到列表的尾部
//        this.beanPostProcessors.add(beanPostProcessor);
//    }
//
//    //AbstractAutowireCapableBeanFactory:
//    //忽略给定的自动装配依赖接口。这通常会被应用程序上下文用来注册
//    //以其他方式解析的依赖项，如BeanFactory.
//    //默认情况下，只有BeanFactoryAware接口被忽略。
//    //若要忽略其他类型，请为每个类型调用此方法
//    public void ignoreDependencyInterface(Class<?> ifc) {
//        //Set<Class<?>> ignoredDependencyInterfaces = new HashSet<>()
//        this.ignoredDependencyInterfaces.add(ifc);
//    }
//
//
//    //---------------------------------
//    //AbstractBeanFactory:
//    //在ApplicationContext初始化之后修改它的内部bean工厂。所有beanDefinition都已加载，但还没有实例化bean。
//    //允许在特定的ApplicationContext实现中注册特殊的beanpostprocessor。
//    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
//    }
//
//
//    //AbstractBeanFactory:
//    //实例化并调用所有已注册的BeanFactoryPostProcessor，如果设置了排序。必须在单例bean初始化之前调用。
//    protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
//        PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());
//
//        //检测LoadTimeWeaver并准备织入
//        if (beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
//            beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
//            beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
//        }
//    }
//
//    //AbstractBeanFactory:
//    protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
//        PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
//    }
//
//    //PostProcessorRegistrationDelegate:
//    public static void registerBeanPostProcessors(
//            ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
//
//        String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
//
//        // 注册BeanPostProcessorChecker，当一个bean在BeanPostProcessor实例化过程中被创建时，
//        // 即当一个bean没有资格被所有BeanPostProcessor处理时，它记录一个信息消息。
//        int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
//        beanFactory.addBeanPostProcessor(new PostProcessorRegistrationDelegate.BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));
//
//        //在实现priorityor、Ordered和其他操作的beanpostprocessor之间进行分离。
//        List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
//        List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
//        List<String> orderedPostProcessorNames = new ArrayList<>();
//        List<String> nonOrderedPostProcessorNames = new ArrayList<>();
//        for (String ppName : postProcessorNames) {
//            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
//                BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
//                priorityOrderedPostProcessors.add(pp);
//                if (pp instanceof MergedBeanDefinitionPostProcessor) {
//                    internalPostProcessors.add(pp);
//                }
//            }else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
//                orderedPostProcessorNames.add(ppName);
//            }else {
//                nonOrderedPostProcessorNames.add(ppName);
//            }
//        }
//
//        // 首先，注册实现了PriorityOrdered的BeanPostProcessors
//        sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
//        registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);
//
//        //接下来，注册实现了Ordered的BeanPostProcessors
//        List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
//        for (String ppName : orderedPostProcessorNames) {
//            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
//            orderedPostProcessors.add(pp);
//            if (pp instanceof MergedBeanDefinitionPostProcessor) {
//                internalPostProcessors.add(pp);
//            }
//        }
//        sortPostProcessors(orderedPostProcessors, beanFactory);
//        registerBeanPostProcessors(beanFactory, orderedPostProcessors);
//
//        //现在，注册所有匹配的BeanPostProcessors
//        List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
//        for (String ppName : nonOrderedPostProcessorNames) {
//            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
//            nonOrderedPostProcessors.add(pp);
//            if (pp instanceof MergedBeanDefinitionPostProcessor) {
//                internalPostProcessors.add(pp);
//            }
//        }
//        registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);
//
//        //最后，重新注册所有内部的BeanPostProcessors
//        sortPostProcessors(internalPostProcessors, beanFactory);
//        registerBeanPostProcessors(beanFactory, internalPostProcessors);
//
//        //重新注册post-processor作为ApplicationListeners去探测内部的bean，移动它到链表尾
//        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
//    }
//
//    //PostProcessorRegistrationDelegate:
//    private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
//        Comparator<Object> comparatorToUse = null;
//        if (beanFactory instanceof DefaultListableBeanFactory) {
//            comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
//        }
//        if (comparatorToUse == null) {
//            comparatorToUse = OrderComparator.INSTANCE;
//        }
//        postProcessors.sort(comparatorToUse);
//    }
//
//    //PostProcessorRegistrationDelegate:
//    private static void registerBeanPostProcessors(
//            ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {
//
//        for (BeanPostProcessor postProcessor : postProcessors) {
//            beanFactory.addBeanPostProcessor(postProcessor);
//        }
//    }
//
//    //AbstractBeanFactory:
//    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
//        Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
//        // Remove from old position, if any
//        this.beanPostProcessors.remove(beanPostProcessor);
//        // Track whether it is instantiation/destruction aware
//        if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
//            this.hasInstantiationAwareBeanPostProcessors = true;
//        }
//        if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
//            this.hasDestructionAwareBeanPostProcessors = true;
//        }
//        // Add to end of list
//        this.beanPostProcessors.add(beanPostProcessor);
//    }
//
//    //------------------------------
//    //AbstractApplicationContext:
//    protected void initMessageSource() {
//        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
//        //MESSAGE_SOURCE_BEAN_NAME = "messageSource", BeanFactory里MessageSource bean的名字
//        if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
//            this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
//            //使MessageSource知道父MessageSource
//            if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
//                HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
//                if (hms.getParentMessageSource() == null) {
//                    //如果父消息源没有注册，则只将父上下文设置为父消息源
//                    hms.setParentMessageSource(getInternalParentMessageSource());
//                }
//            }
//            if (logger.isTraceEnabled()) {
//                logger.trace("Using MessageSource [" + this.messageSource + "]");
//            }
//        }else {
//            //使用空的MessageSource来接受getMessage调用
//            //DelegatingMessageSource extends MessageSourceSupport implements HierarchicalMessageSource
//            DelegatingMessageSource dms = new DelegatingMessageSource();
//            dms.setParentMessageSource(getInternalParentMessageSource());
//            this.messageSource = dms;
//            //将MessageSource bean注册到BeanFactory
//            beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
//            if (logger.isTraceEnabled()) {
//                logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
//            }
//        }
//    }
//
//    //-------------------------------
//    //AbstractApplicationContext:
//    protected void initApplicationEventMulticaster() {
//        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
//        //APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster"
//        //如果ApplicationEventMulticaster在BeanFactory里已经存在，就直接用
//        if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
//            this.applicationEventMulticaster =
//                    beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
//            if (logger.isTraceEnabled()) {
//                logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
//            }
//        }else {
//            //如果不存在，就创建一个SimpleApplicationEventMulticaster，并注册到BeanFactory
//            this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
//            beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
//            if (logger.isTraceEnabled()) {
//                logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
//                        "[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
//            }
//        }
//    }
//
//    //--------------------------
//    //AbstractApplicationContext:
//    //模板方法，可以覆盖该方法以添加自定义方法完成上下文的刷新工作。
//    //在单例实例化之前，在初始化特殊bean时调用。
//    protected void onRefresh() throws BeansException {
//        // For subclasses: do nothing by default.
//    }
//
//    //-----------------------------
//    //AbstractApplicationContext:
//    //添加一个实现了ApplicationListener的listener, 不影响其他监听器，
//    //可以在不添加bean的情况下添加
//    protected void registerListeners() {
//        //首先注册静态指定的侦听器
//        //Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>()
//        for (ApplicationListener<?> listener : getApplicationListeners()) {
//            getApplicationEventMulticaster().addApplicationListener(listener);
//        }
//
//        // Do not initialize FactoryBeans here: We need to leave all regular beans
//        // uninitialized to let post-processors apply to them!
//        String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
//        for (String listenerBeanName : listenerBeanNames) {
//            getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
//        }
//
//        // Publish early application events now that we finally have a multicaster...
//        Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
//        this.earlyApplicationEvents = null;
//        if (earlyEventsToProcess != null) {
//            for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
//                getApplicationEventMulticaster().multicastEvent(earlyEvent);
//            }
//        }
//    }
//
//    //AbstractApplicationContext:
//    ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
//        // ApplicationEventMulticaster applicationEventMulticaster
//        if (this.applicationEventMulticaster == null) {
//            throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
//                    "call 'refresh' before multicasting events via the context: " + this);
//        }
//        return this.applicationEventMulticaster;
//    }
//
//    //AbstractApplicationEventMulticaster:
//    public void addApplicationListener(ApplicationListener<?> listener) {
//        synchronized (this.retrievalMutex) {
//            //如果已经注册，则显式删除代理的目标，以避免对同一侦听器的重复调用
//            Object singletonTarget = AopProxyUtils.getSingletonTarget(listener);
//            if (singletonTarget instanceof ApplicationListener) {
//                this.defaultRetriever.applicationListeners.remove(singletonTarget);
//            }
//            //Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>()
//            this.defaultRetriever.applicationListeners.add(listener);
//            //Map<ListenerCacheKey, ListenerRetriever> retrieverCache = new ConcurrentHashMap<>(64)
//            this.retrieverCache.clear();
//        }
//    }
//
//    //AbstractApplicationContext：
//    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
//        assertBeanFactoryActive();
//        return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
//    }
//
//    //DefaultListableBeanFactory：
//    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
//        if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
//            return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
//        }
//        Map<Class<?>, String[]> cache =
//                (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
//        String[] resolvedBeanNames = cache.get(type);
//        if (resolvedBeanNames != null) {
//            return resolvedBeanNames;
//        }
//        resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
//        if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
//            cache.put(type, resolvedBeanNames);
//        }
//        return resolvedBeanNames;
//    }
//
//    //DefaultListableBeanFactory：
//    private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
//        List<String> result = new ArrayList<>();
//
//        //校验所有的 bean definitions.
//        //List<String> beanDefinitionNames = new ArrayList<>(256)
//        for (String beanName : this.beanDefinitionNames) {
//            // Only consider bean as eligible if the bean name
//            // is not defined as alias for some other bean.
//            if (!isAlias(beanName)) {
//                try {
//                    RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
//                    // Only check bean definition if it is complete.
//                    if (!mbd.isAbstract() && (allowEagerInit ||
//                            (mbd.hasBeanClass() || !mbd.isLazyInit() || isAllowEagerClassLoading()) &&
//                                    !requiresEagerInitForType(mbd.getFactoryBeanName()))) {
//                        boolean isFactoryBean = isFactoryBean(beanName, mbd);
//                        BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
//                        boolean matchFound = false;
//                        boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
//                        boolean isNonLazyDecorated = dbd != null && !mbd.isLazyInit();
//                        if (!isFactoryBean) {
//                            if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
//                                matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
//                            }
//                        }else {
//                            if (includeNonSingletons || isNonLazyDecorated ||
//                                    (allowFactoryBeanInit && isSingleton(beanName, mbd, dbd))) {
//                                matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
//                            }
//                            if (!matchFound) {
//                                // In case of FactoryBean, try to match FactoryBean instance itself next.
//                                beanName = FACTORY_BEAN_PREFIX + beanName;
//                                matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
//                            }
//                        }
//                        if (matchFound) {
//                            result.add(beanName);
//                        }
//                    }
//                } catch (CannotLoadBeanClassException | BeanDefinitionStoreException ex) {
//                    if (allowEagerInit) {
//                        throw ex;
//                    }
//                    // Probably a placeholder: let's ignore it for type matching purposes.
//                    LogMessage message = (ex instanceof CannotLoadBeanClassException) ?
//                            LogMessage.format("Ignoring bean class loading failure for bean '%s'", beanName) :
//                            LogMessage.format("Ignoring unresolvable metadata in bean definition '%s'", beanName);
//                    logger.trace(message, ex);
//                    onSuppressedException(ex);
//                }
//            }
//        }
//
//        // Check manually registered singletons too.
//        //Set<String> manualSingletonNames = new LinkedHashSet<>(16)
//        for (String beanName : this.manualSingletonNames) {
//            try {
//                // In case of FactoryBean, match object created by FactoryBean.
//                if (isFactoryBean(beanName)) {
//                    if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
//                        result.add(beanName);
//                        // Match found for this bean: do not match FactoryBean itself anymore.
//                        continue;
//                    }
//                    // In case of FactoryBean, try to match FactoryBean itself next.
//                    beanName = FACTORY_BEAN_PREFIX + beanName;
//                }
//                // Match raw bean instance (might be raw FactoryBean).
//                if (isTypeMatch(beanName, type)) {
//                    result.add(beanName);
//                }
//            }catch (NoSuchBeanDefinitionException ex) {
//                // Shouldn't happen - probably a result of circular reference resolution...
//                logger.trace(LogMessage.format("Failed to check manually registered singleton with name '%s'", beanName), ex);
//            }
//        }
//        return StringUtils.toStringArray(result);
//    }
//
//
//    //--------------------------------------------------
//    //AbstractApplicationContext:
//    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
//        //为此上下文初始化转换服务
//        //CONVERSION_SERVICE_BEAN_NAME = "conversionService"
//        if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
//                beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
//            beanFactory.setConversionService(
//                    beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
//        }
//
//        //如果以前没有任何bean后处理器(如PropertyPlaceholderConfigurer bean)注册，
//        //则注册一个默认的嵌入式值解析器:此时，主要用于解析注释属性值
//        if (!beanFactory.hasEmbeddedValueResolver()) {
//            beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
//        }
//
//        //尽早初始化LoadTimeWeaverAware bean，以便尽早注册它们的转换器
//        String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
//        for (String weaverAwareName : weaverAwareNames) {
//            getBean(weaverAwareName);
//        }
//
//        //停止使用临时类加载器进行类型匹配
//        beanFactory.setTempClassLoader(null);
//
//        //允许缓存所有的bean定义元数据，不希望未来发生更改
//        beanFactory.freezeConfiguration();
//
//        //实例化所有剩余的(非懒加载)单例
//        beanFactory.preInstantiateSingletons();
//    }
//
//    //DefaultListableBeanFactory:
//    public void freezeConfiguration() {
//        this.configurationFrozen = true;
//        this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
//    }
//
//
//    //----------------------------------
//    //AbstractApplicationContext:
//    protected void finishRefresh() {
//        //清除上下文级的资源缓存(例如扫描的ASM元数据)
//        clearResourceCaches();
//
//        //为此上下文初始化生命周期处理器
//        initLifecycleProcessor();
//
//        //首先将refresh传播到生命周期处理器
//        getLifecycleProcessor().onRefresh();
//
//        //发布最终事件
//        publishEvent(new ContextRefreshedEvent(this));
//
//        // Participate in LiveBeansView MBean, if active.
//        LiveBeansView.registerApplicationContext(this);
//    }
//
//    //DefaultResourceLoader:
//    public void clearResourceCaches() {
//        //Map<Class<?>, Map<Resource, ?>> resourceCaches = new ConcurrentHashMap<>(4)
//        this.resourceCaches.clear();
//    }
//
//    //AbstractApplicationContext:
//    protected void initLifecycleProcessor() {
//        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
//        //LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor"
//        if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
//            this.lifecycleProcessor =
//                    beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
//            if (logger.isTraceEnabled()) {
//                logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
//            }
//        }else {
//            DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
//            defaultProcessor.setBeanFactory(beanFactory);
//            this.lifecycleProcessor = defaultProcessor;
//            beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
//            if (logger.isTraceEnabled()) {
//                logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
//                        "[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
//            }
//        }
//    }
//
//    //DefaultLifecycleProcessor:
//    public void onRefresh() {
//        startBeans(true);
//        this.running = true;
//    }
//
//    //DefaultLifecycleProcessor:
//    private void startBeans(boolean autoStartupOnly) {
//        //1>获取所有的生命周期bean
//        Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
//        Map<Integer, DefaultLifecycleProcessor.LifecycleGroup> phases = new HashMap<>();
//        lifecycleBeans.forEach((beanName, bean) -> {
//            if (!autoStartupOnly || (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {
//                int phase = getPhase(bean);
//                DefaultLifecycleProcessor.LifecycleGroup group = phases.get(phase);
//                if (group == null) {
//                    group = new DefaultLifecycleProcessor.LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly);
//                    phases.put(phase, group);
//                }
//                group.add(beanName, bean);
//            }
//        });
//        if (!phases.isEmpty()) {
//            List<Integer> keys = new ArrayList<>(phases.keySet());
//            Collections.sort(keys);
//            for (Integer key : keys) {
//                //2>启动生命周期阶段
//                phases.get(key).start();
//            }
//        }
//    }
//
//    //DefaultLifecycleProcessor:
//    protected Map<String, Lifecycle> getLifecycleBeans() {
//        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
//        Map<String, Lifecycle> beans = new LinkedHashMap<>();
//        String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
//        for (String beanName : beanNames) {
//            String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
//            boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
//            //FACTORY_BEAN_PREFIX = "&"
//            String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
//            if ((beanFactory.containsSingleton(beanNameToRegister) &&
//                    (!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))) ||
//                    matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
//                Object bean = beanFactory.getBean(beanNameToCheck);
//                if (bean != this && bean instanceof Lifecycle) {
//                    beans.put(beanNameToRegister, (Lifecycle) bean);
//                }
//            }
//        }
//        return beans;
//    }
//
//    //DefaultLifecycleProcessor:
//    public void start() {
//        if (this.members.isEmpty()) {
//            return;
//        }
//        if (logger.isDebugEnabled()) {
//            logger.debug("Starting beans in phase " + this.phase);
//        }
//        Collections.sort(this.members);
//        //List<LifecycleGroupMember> members = new ArrayList<>()
//        for (DefaultLifecycleProcessor.LifecycleGroupMember member : this.members) {
//            //开始启动
//            doStart(this.lifecycleBeans, member.name, this.autoStartupOnly);
//        }
//    }
//
//    //DefaultLifecycleProcessor:
//    //启动指定的bean作为给定的生命周期bean集合的一部分，确保首先启动它所依赖的任何bean
//    private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
//        Lifecycle bean = lifecycleBeans.remove(beanName);
//        if (bean != null && bean != this) {
//            String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
//            for (String dependency : dependenciesForBean) {
//                doStart(lifecycleBeans, dependency, autoStartupOnly);
//            }
//            if (!bean.isRunning() &&
//                    (!autoStartupOnly || !(bean instanceof SmartLifecycle) || ((SmartLifecycle) bean).isAutoStartup())) {
//                if (logger.isTraceEnabled()) {
//                    logger.trace("Starting bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
//                }
//                try {
//                    bean.start();
//                }
//                catch (Throwable ex) {
//                    throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
//                }
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Successfully started bean '" + beanName + "'");
//                }
//            }
//        }
//    }
//
//    //AbstractApplicationContext:
//    public void publishEvent(ApplicationEvent event) {
//        publishEvent(event, null);
//    }
//
//    //AbstractApplicationContext:
//    protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
//        Assert.notNull(event, "Event must not be null");
//
//        //必要时将事件装饰为ApplicationEvent
//        ApplicationEvent applicationEvent;
//        if (event instanceof ApplicationEvent) {
//            applicationEvent = (ApplicationEvent) event;
//        }else {
//            applicationEvent = new PayloadApplicationEvent<>(this, event);
//            if (eventType == null) {
//                eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
//            }
//        }
//
//        //如果可能的话，开始广播事件或者在初始化之后懒加载
//        if (this.earlyApplicationEvents != null) {
//            //Set<ApplicationEvent> earlyApplicationEvents
//            this.earlyApplicationEvents.add(applicationEvent);
//        } else {
//            //广播事件
//            getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
//        }
//
//        //可以通过父上下文发布event
//        if (this.parent != null) {
//            if (this.parent instanceof AbstractApplicationContext) {
//                //递归调用广播Event的方法
//                ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
//            }else {
//                //递归调用广播Event的方法
//                this.parent.publishEvent(event);
//            }
//        }
//    }
//
//    //SimpleApplicationEventMulticaster:
//    public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
//        ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
//        //用一个线程池去启动事件监听器
//        Executor executor = getTaskExecutor();
//        for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
//            //启动实现了ApplicationListener接口的监听器Listener
//            if (executor != null) {
//                executor.execute(() -> invokeListener(listener, event));
//            }else {
//                invokeListener(listener, event);
//            }
//        }
//    }
//
//    //SimpleApplicationEventMulticaster:
//    protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
//        org.springframework.util.ErrorHandler errorHandler = getErrorHandler();
//        if (errorHandler != null) {
//            try {
//                doInvokeListener(listener, event);
//            }catch (Throwable err) {
//                errorHandler.handleError(err);
//            }
//        }else {
//            doInvokeListener(listener, event);
//        }
//    }
//
//    //SimpleApplicationEventMulticaster:
//    private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
//        try {
//            //这是个接口方法，处理注册的监听器Listener，
//            //自定义的Listener实现接口ApplicationListener，覆盖此方法
//            listener.onApplicationEvent(event);
//        }catch (ClassCastException ex) {
//            String msg = ex.getMessage();
//            if (msg == null || matchesClassCastMessage(msg, event.getClass())) {
//                Log logger = LogFactory.getLog(getClass());
//                if (logger.isTraceEnabled()) {
//                    logger.trace("Non-matching event type for listener: " + listener, ex);
//                }
//            } else {
//                throw ex;
//            }
//        }
//    }
//
//
//    //LiveBeansView:
//    static void registerApplicationContext(ConfigurableApplicationContext applicationContext) {
//        //MBEAN_DOMAIN_PROPERTY_NAME = "spring.liveBeansView.mbeanDomain"
//        String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
//        if (mbeanDomain != null) {
//            //Set<ConfigurableApplicationContext> applicationContexts = new LinkedHashSet<>()
//            synchronized (applicationContexts) {
//                if (applicationContexts.isEmpty()) {
//                    try {
//                        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
//                        applicationName = applicationContext.getApplicationName();
//                        server.registerMBean(new LiveBeansView(),
//                                new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
//                    }catch (Throwable ex) {
//                        throw new ApplicationContextException("Failed to register LiveBeansView MBean", ex);
//                    }
//                }
//                applicationContexts.add(applicationContext);
//            }
//        }
//    }
//
//
//    //--------------------------------------------------------
//
//    //AbstractApplicationContext:
//    protected void resetCommonCaches() {
//        ReflectionUtils.clearCache();
//        AnnotationUtils.clearCache();
//        ResolvableType.clearCache();
//        CachedIntrospectionResults.clearClassLoader(getClassLoader());
//    }
//
//    //ReflectionUtils:
//    public static void clearCache() {
//        // Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentReferenceHashMap<>(256)
//        declaredMethodsCache.clear();
//        //Map<Class<?>, Field[]> declaredFieldsCache = new ConcurrentReferenceHashMap<>(256)
//        declaredFieldsCache.clear();
//    }
//
//    //AnnotationUtils:
//    public static void clearCache() {
//        //1>清除注解类型映射缓存
//        AnnotationTypeMappings.clearCache();
//        //2>清除注解扫描
//        AnnotationsScanner.clearCache();
//    }
//
//    //AnnotationTypeMappings:
//    static void clearCache() {
//        //Map<AnnotationFilter, Cache> standardRepeatablesCache = new ConcurrentReferenceHashMap<>()
//        standardRepeatablesCache.clear();
//        //Map<AnnotationFilter, Cache> noRepeatablesCache = new ConcurrentReferenceHashMap<>()
//        noRepeatablesCache.clear();
//    }
//
//    //AnnotationsScanner:
//    static void clearCache() {
//        //Map<AnnotatedElement, Annotation[]> declaredAnnotationCache =
//        //                              new ConcurrentReferenceHashMap<>(256)
//        declaredAnnotationCache.clear();
//        //Map<Class<?>, Method[]> baseTypeMethodsCache =
//        //			          new ConcurrentReferenceHashMap<>(256)
//        baseTypeMethodsCache.clear();
//    }
//
//    //ResolvableType:
//    public static void clearCache() {
//        //ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
//        //			      new ConcurrentReferenceHashMap<>(256)
//        cache.clear();
//        //ConcurrentReferenceHashMap<Type, Type> cache = new ConcurrentReferenceHashMap<>(256)
//        SerializableTypeWrapper.cache.clear();
//    }
//
//    //CachedIntrospectionResults:
//    //清除给定类装入器的默认缓存，删除该类装入器下的所有类的默认结果，并从接受列表中删除类装入器(及其子类)
//    public static void clearClassLoader(@Nullable ClassLoader classLoader) {
//        //Set<ClassLoader> acceptedClassLoaders = Collections.newSetFromMap(new ConcurrentHashMap<>(16))
//        acceptedClassLoaders.removeIf(registeredLoader ->
//                isUnderneathClassLoader(registeredLoader, classLoader));
//        //ConcurrentMap<Class<?>, CachedIntrospectionResults> strongClassCache = new ConcurrentHashMap<>(64)
//        strongClassCache.keySet().removeIf(beanClass ->
//                isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
//        //ConcurrentMap<Class<?>, CachedIntrospectionResults> softClassCache = new ConcurrentReferenceHashMap<>(64)
//        softClassCache.keySet().removeIf(beanClass ->
//                isUnderneathClassLoader(beanClass.getClassLoader(), classLoader));
//    }
//
//    //CachedIntrospectionResults:
//    private static boolean isUnderneathClassLoader(@Nullable ClassLoader candidate, @Nullable ClassLoader parent) {
//        if (candidate == parent) {
//            return true;
//        }
//        if (candidate == null) {
//            return false;
//        }
//        ClassLoader classLoaderToCheck = candidate;
//        while (classLoaderToCheck != null) {
//            classLoaderToCheck = classLoaderToCheck.getParent();
//            if (classLoaderToCheck == parent) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//
//
//    //========================================
//
//
//
//    //AopUtils:
//    public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
//        //判断通知是否是IntroductionAdvisor
//        if (advisor instanceof IntroductionAdvisor) {
//            return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
//        }
//        //判断事务的通知BeanFactoryTransactionAttributeSourceAdvisor是否实现了PointcutAdvisor
//        else if (advisor instanceof PointcutAdvisor) {
//            //转为PointcutAdvisor类型
//            PointcutAdvisor pca = (PointcutAdvisor) advisor;
//            //找到真正能用的通知
//            return canApply(pca.getPointcut(), targetClass, hasIntroductions);
//        }else {
//            //它没有切入点，所以我们假设它适用
//            return true;
//        }
//    }
//
//    //AopUtils:
//    //判断目标类是否可以应用此切点，如果返回true就表示匹配，添加到合适的集合eligibleAdvisors中
//    public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
//        Assert.notNull(pc, "Pointcut must not be null");
//        if (!pc.getClassFilter().matches(targetClass)) {
//            return false;
//        }
//        //通过切点获取到一个方法匹配器对象
//        MethodMatcher methodMatcher = pc.getMethodMatcher();
//        if (methodMatcher == MethodMatcher.TRUE) {
//            //如果匹配到任何方法，就不需要进行迭代
//            return true;
//        }
//        //判断匹配器是不是IntroductionAwareMethodMatcher
//        IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
//        if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
//            introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
//        }
//
//        //创建一个集合用于保存targetClass的class对象
//        Set<Class<?>> classes = new LinkedHashSet<>();
//        //判断当前class是不是代理的class对象
//        if (!Proxy.isProxyClass(targetClass)) {
//            //加入到集合中去
//            classes.add(ClassUtils.getUserClass(targetClass));
//        }
//        //获取到targetClass所实现的接口的class对象，然后加入到集合中
//        classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
//
//        //循环所有的class对象
//        for (Class<?> clazz : classes) {
//            //通过class获取到所有的方法
//            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
//            //循环我们的方法
//            for (Method method : methods) {
//                //通过methodMatcher.matches来匹配我们的方法
//                if (introductionAwareMethodMatcher != null ?
//                        introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) :
//                        //通过方法匹配器进行匹配
//                        methodMatcher.matches(method, targetClass)) {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }
//
//
//
//    //AbstractAutoProxyCreator:
//    //给指定的aop创建一个AOP代理
//    protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
//                                 @Nullable Object[] specificInterceptors, TargetSource targetSource) {
//
//        if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
//            //1>为指定的bean公开给定的目标类
//            AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
//        }
//
//        //创建一个代理对象工厂
//        ProxyFactory proxyFactory = new ProxyFactory();
//        proxyFactory.copyFrom(this);
//
//        //为proxyFactory设置创建jdk还是cglib代理
//        if (!proxyFactory.isProxyTargetClass()) {
//            //2>确定给定的bean是否应该使用其目标类而不是其接口进行代理
//            if (shouldProxyTargetClass(beanClass, beanName)) {
//                proxyFactory.setProxyTargetClass(true);
//            }else {
//                //3>检查给定bean类的接口，并将它们应用于{ProxyFactory}(如果合适的话)
//                evaluateProxyInterfaces(beanClass, proxyFactory);
//            }
//        }
//
//        //把我们的specificInterceptors数组中的Advisor转化为数组形式的
//        Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
//        //为我们的代理工厂加入通知器
//        proxyFactory.addAdvisors(advisors);
//        //设置targetSource对象
//        proxyFactory.setTargetSource(targetSource);
//        customizeProxyFactory(proxyFactory);
//
//        proxyFactory.setFrozen(this.freezeProxy);
//        if (advisorsPreFiltered()) {
//            proxyFactory.setPreFiltered(true);
//        }
//        //真正创建代理对象
//        return proxyFactory.getProxy(getProxyClassLoader());
//    }
//
//
//    //ProxyFactory:
//    public Object getProxy(@Nullable ClassLoader classLoader) {
//        //1>createAopProxy() 决定使用jdk还是cglib进行动态代理
//        //2>getProxy(classLoader) 具体获取代理对象实例
//        return createAopProxy().getProxy(classLoader);
//    }
//
//    //ProxyCreatorSupport:
//    //子类应该调用这个新的AOP代理,
//    protected final synchronized AopProxy createAopProxy() {
//        //如果没有激活，激活这个代理配置
//        if (!this.active) {
//            activate();
//        }
//        return getAopProxyFactory().createAopProxy(this);
//    }
//
//
//    //ProxyCreatorSupport:
//    private void activate() {
//        this.active = true;
//        for (AdvisedSupportListener listener : this.listeners) {
//            listener.activated(this);
//        }
//    }
//
//    //DefaultAopProxyFactory:
//    public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
//        //判断我们是否指定使用cglib代理ProxyTargetClass =true  默认false
//        if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
//            Class<?> targetClass = config.getTargetClass();
//            if (targetClass == null) {
//                throw new AopConfigException("TargetSource cannot determine target class: " +
//                        "Either an interface or a target is required for proxy creation.");
//            }
//            //targetClass是接口使用的就是jdk代理
//            if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
//                return new JdkDynamicAopProxy(config);
//            }
//            //cglib代理
//            return new ObjenesisCglibAopProxy(config);
//        }else {
//            return new JdkDynamicAopProxy(config);
//        }
//    }
//
//
//    //JdkDynamicAopProxy:
//    //使用jdk动态aop代理
//    public Object getProxy(@Nullable ClassLoader classLoader) {
//        if (logger.isTraceEnabled()) {
//            logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
//        }
//        //确定接口,为给定的AOP配置代理
//        Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
//        //获取定义了equals和hashCode的方法
//        findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
//        //创建代理实例
//        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
//    }
//
//
//    //AopProxyUtils:
//    static Class<?>[] completeProxiedInterfaces(AdvisedSupport advised, boolean decoratingProxy) {
//        Class<?>[] specifiedInterfaces = advised.getProxiedInterfaces();
//        if (specifiedInterfaces.length == 0) {
//            //没有指定的接口:检查是目标类是否是一个接口
//            Class<?> targetClass = advised.getTargetClass();
//            if (targetClass != null) {
//                if (targetClass.isInterface()) {
//                    advised.setInterfaces(targetClass);
//                }else if (Proxy.isProxyClass(targetClass)) {
//                    advised.setInterfaces(targetClass.getInterfaces());
//                }
//                specifiedInterfaces = advised.getProxiedInterfaces();
//            }
//        }
//        boolean addSpringProxy = !advised.isInterfaceProxied(SpringProxy.class);
//        boolean addAdvised = !advised.isOpaque() && !advised.isInterfaceProxied(Advised.class);
//        boolean addDecoratingProxy = (decoratingProxy && !advised.isInterfaceProxied(DecoratingProxy.class));
//        int nonUserIfcCount = 0;
//        if (addSpringProxy) {
//            nonUserIfcCount++;
//        }
//        if (addAdvised) {
//            nonUserIfcCount++;
//        }
//        if (addDecoratingProxy) {
//            nonUserIfcCount++;
//        }
//        Class<?>[] proxiedInterfaces = new Class<?>[specifiedInterfaces.length + nonUserIfcCount];
//        System.arraycopy(specifiedInterfaces, 0, proxiedInterfaces, 0, specifiedInterfaces.length);
//        int index = specifiedInterfaces.length;
//        if (addSpringProxy) {
//            proxiedInterfaces[index] = SpringProxy.class;
//            index++;
//        }
//        if (addAdvised) {
//            proxiedInterfaces[index] = Advised.class;
//            index++;
//        }
//        if (addDecoratingProxy) {
//            proxiedInterfaces[index] = DecoratingProxy.class;
//        }
//        return proxiedInterfaces;
//    }
//
//
//    //JdkDynamicAopProxy:
//    private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
//        for (Class<?> proxiedInterface : proxiedInterfaces) {
//            Method[] methods = proxiedInterface.getDeclaredMethods();
//            for (Method method : methods) {
//                if (AopUtils.isEqualsMethod(method)) {
//                    this.equalsDefined = true;
//                }
//                if (AopUtils.isHashCodeMethod(method)) {
//                    this.hashCodeDefined = true;
//                }
//                if (this.equalsDefined && this.hashCodeDefined) {
//                    return;
//                }
//            }
//        }
//    }
//
//
//    //java.lang.reflect.Proxy:
//    public static Object newProxyInstance(ClassLoader loader,
//                                          Class<?>[] interfaces,
//                                          InvocationHandler h) {
//        Objects.requireNonNull(h);
//        final Class<?> caller = System.getSecurityManager() == null
//                ? null
//                : Reflection.getCallerClass();
//
//        //查找或生成指定的代理类的构造函数
//        Constructor<?> cons = getProxyConstructor(caller, loader, interfaces);
//
//        return newProxyInstance(caller, cons, h);
//    }
//
//    //java.lang.reflect.Proxy:
//    private static Constructor<?> getProxyConstructor(Class<?> caller,
//                                                      ClassLoader loader,
//                                                      Class<?>... interfaces){
//        //优化单一接口
//        if (interfaces.length == 1) {
//            Class<?> intf = interfaces[0];
//            if (caller != null) {
//                checkProxyAccess(caller, loader, intf);
//            }
//            //ClassLoaderValue<Constructor<?>> proxyCache = new ClassLoaderValue<>()
//            return proxyCache.sub(intf).computeIfAbsent(
//                    loader,
//                    (ld, clv) -> new ProxyBuilder(ld, clv.key()).build()
//            );
//        } else {
//            // interfaces cloned
//            final Class<?>[] intfsArray = interfaces.clone();
//            if (caller != null) {
//                checkProxyAccess(caller, loader, intfsArray);
//            }
//            final List<Class<?>> intfs = Arrays.asList(intfsArray);
//            return proxyCache.sub(intfs).computeIfAbsent(
//                    loader,
//                    (ld, clv) -> new ProxyBuilder(ld, clv.key()).build()
//            );
//        }
//    }
//
//
//    //java.lang.reflect.Proxy:
//    private static Object newProxyInstance(Class<?> caller,
//                                           Constructor<?> cons,
//                                           InvocationHandler h) {
//        //调用指定了invocation handler的构造函数
//        try {
//            if (caller != null) {
//                checkNewProxyPermission(caller, cons.getDeclaringClass());
//            }
//            //使用构造函数实例化一个类
//            return cons.newInstance(new Object[]{h});
//        } catch (IllegalAccessException | InstantiationException e) {
//            throw new InternalError(e.toString(), e);
//        } catch (InvocationTargetException e) {
//            Throwable t = e.getCause();
//            if (t instanceof RuntimeException) {
//                throw (RuntimeException) t;
//            } else {
//                throw new InternalError(t.toString(), t);
//            }
//        }
//    }
//
//
//
//
//
//
//
//    //CglibAopProxy:
//    //使用cglib动态aop代理
//    public Object getProxy(@Nullable ClassLoader classLoader) {
//        if (logger.isTraceEnabled()) {
//            logger.trace("Creating CGLIB proxy: " + this.advised.getTargetSource());
//        }
//
//        try {
//            Class<?> rootClass = this.advised.getTargetClass();
//            Assert.state(rootClass != null, "Target class must be available for creating a CGLIB proxy");
//
//            Class<?> proxySuperClass = rootClass;
//            //String CGLIB_CLASS_SEPARATOR = "$$"
//            if (rootClass.getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)) {
//                proxySuperClass = rootClass.getSuperclass();
//                Class<?>[] additionalInterfaces = rootClass.getInterfaces();
//                for (Class<?> additionalInterface : additionalInterfaces) {
//                    this.advised.addInterface(additionalInterface);
//                }
//            }
//
//            //验证类,编写必要的日志消息
//            validateClassIfNecessary(proxySuperClass, classLoader);
//
//            // Configure CGLIB Enhancer...
//            Enhancer enhancer = createEnhancer();
//            if (classLoader != null) {
//                enhancer.setClassLoader(classLoader);
//                if (classLoader instanceof SmartClassLoader &&
//                        ((SmartClassLoader) classLoader).isClassReloadable(proxySuperClass)) {
//                    enhancer.setUseCache(false);
//                }
//            }
//            enhancer.setSuperclass(proxySuperClass);
//            enhancer.setInterfaces(AopProxyUtils.completeProxiedInterfaces(this.advised));
//            enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
//            enhancer.setStrategy(new ClassLoaderAwareGeneratorStrategy(classLoader));
//
//            Callback[] callbacks = getCallbacks(rootClass);
//            Class<?>[] types = new Class<?>[callbacks.length];
//            for (int x = 0; x < types.length; x++) {
//                types[x] = callbacks[x].getClass();
//            }
//            // fixedInterceptorMap only populated at this point, after getCallbacks call above
//            enhancer.setCallbackFilter(new CglibAopProxy.ProxyCallbackFilter(
//                    this.advised.getConfigurationOnlyCopy(), this.fixedInterceptorMap, this.fixedInterceptorOffset));
//            enhancer.setCallbackTypes(types);
//
//            //生成代理类同时创建一个代理实例
//            return createProxyClassAndInstance(enhancer, callbacks);
//        }catch (CodeGenerationException | IllegalArgumentException ex) {
//            throw new AopConfigException("Could not generate CGLIB subclass of " + this.advised.getTargetClass() +
//                    ": Common causes of this problem include using a final class or a non-visible class",ex);
//        }catch (Throwable ex) {
//            // TargetSource.getTarget() failed
//            throw new AopConfigException("Unexpected AOP exception", ex);
//        }
//    }
//
//
//
//
//    //CglibAopProxy:
//    protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
//        enhancer.setInterceptDuringConstruction(false);
//        enhancer.setCallbacks(callbacks);
//        return (this.constructorArgs != null && this.constructorArgTypes != null ?
//                enhancer.create(this.constructorArgTypes, this.constructorArgs) :
//                enhancer.create());
//    }
//
//
//    //Enhancer
//    //必要时生成一个新类,并使用指定的回调函数(如果有的话)创建一个新的对象实例。
//    public Object create(Class[] argumentTypes, Object[] arguments) {
//        classOnly = false;
//        if (argumentTypes == null || arguments == null || argumentTypes.length != arguments.length) {
//            throw new IllegalArgumentException("Arguments must be non-null and of equal length");
//        }
//        this.argumentTypes = argumentTypes;
//        this.arguments = arguments;
//        return createHelper();
//    }
//
//    //Enhancer:
//    private Object createHelper() {
//        preValidate();
//        Object key = KEY_FACTORY.newInstance((superclass != null) ? superclass.getName() : null,
//                ReflectUtils.getNames(interfaces),
//                filter == ALL_ZERO ? null : new WeakCacheKey<CallbackFilter>(filter),
//                callbackTypes,
//                useFactory,
//                interceptDuringConstruction,
//                serialVersionUID);
//        this.currentKey = key;
//        //创建代理实例对象
//        Object result = super.create(key);
//        return result;
//    }
//
//
//    //AbstractClassGenerator:
//    protected Object create(Object key) {
//        try {
//            ClassLoader loader = getClassLoader();
//            Map<ClassLoader, ClassLoaderData> cache = CACHE;
//            ClassLoaderData data = cache.get(loader);
//            if (data == null) {
//                synchronized (AbstractClassGenerator.class) {
//                    cache = CACHE;
//                    data = cache.get(loader);
//                    if (data == null) {
//                        Map<ClassLoader, ClassLoaderData> newCache = new WeakHashMap<ClassLoader, ClassLoaderData>(cache);
//                        data = new AbstractClassGenerator.ClassLoaderData(loader);
//                        newCache.put(loader, data);
//                        CACHE = newCache;
//                    }
//                }
//            }
//            this.key = key;
//            Object obj = data.get(this, getUseCache());
//            if (obj instanceof Class) {
//                return firstInstance((Class) obj);
//            }
//            return nextInstance(obj);
//        }catch (RuntimeException | Error ex) {
//            throw ex;
//        }catch (Exception ex) {
//            throw new CodeGenerationException(ex);
//        }
//    }
//
//
//
//
//    //AutoProxyUtils:
//    static void exposeTargetClass(
//            ConfigurableListableBeanFactory beanFactory, @Nullable String beanName, Class<?> targetClass) {
//
//        if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
//            //ORIGINAL_TARGET_CLASS_ATTRIBUTE ==> AutoProxyUtils.class.getName() + ".originalTargetClass"
//            beanFactory.getMergedBeanDefinition(beanName).setAttribute(ORIGINAL_TARGET_CLASS_ATTRIBUTE, targetClass);
//        }
//    }
//
//    //AbstractAutoProxyCreator:
//    //确定给定的bean是否应该使用其目标类而不是其接口进行代理
//    protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
//        return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
//                AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
//    }
//
//    //AutoProxyUtils:
//    public static boolean shouldProxyTargetClass(
//            ConfigurableListableBeanFactory beanFactory, @Nullable String beanName) {
//
//        if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
//            BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
//            //PRESERVE_TARGET_CLASS_ATTRIBUTE ==> AutoProxyUtils.class.getName() + '.' + preserveTargetClass
//            return Boolean.TRUE.equals(bd.getAttribute(PRESERVE_TARGET_CLASS_ATTRIBUTE));
//        }
//        return false;
//    }
//
//
//    //ProxyProcessorSupport:
//    //检查给定bean类的接口，并将它们应用于{ProxyFactory}(如果合适的话)
//    protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
//        Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
//        boolean hasReasonableProxyInterface = false;
//        for (Class<?> ifc : targetInterfaces) {
//            if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
//                    ifc.getMethods().length > 0) {
//                hasReasonableProxyInterface = true;
//                break;
//            }
//        }
//        if (hasReasonableProxyInterface) {
//            //必须允许相互引用;不能只将接口设置为目标的接口
//            for (Class<?> ifc : targetInterfaces) {
//                proxyFactory.addInterface(ifc);
//            }
//        }else {
//            proxyFactory.setProxyTargetClass(true);
//        }
//    }
//
//
//    //AbstractAutoProxyCreator:
//    //确定给定bean的advisor通知，包括特定的拦截器和通用的拦截器，所有这些都适应于Advisor接口
//    protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
//        //1>将指定的拦截器名称解析为Advisor对象
//        Advisor[] commonInterceptors = resolveInterceptorNames();
//
//        List<Object> allInterceptors = new ArrayList<>();
//        if (specificInterceptors != null) {
//            allInterceptors.addAll(Arrays.asList(specificInterceptors));
//            if (commonInterceptors.length > 0) {
//                if (this.applyCommonInterceptorsFirst) {
//                    allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
//                }else {
//                    allInterceptors.addAll(Arrays.asList(commonInterceptors));
//                }
//            }
//        }
//        if (logger.isTraceEnabled()) {
//            int nrOfCommonInterceptors = commonInterceptors.length;
//            int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
//            logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
//                    " common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
//        }
//
//        Advisor[] advisors = new Advisor[allInterceptors.size()];
//        for (int i = 0; i < allInterceptors.size(); i++) {
//            advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
//        }
//        return advisors;
//    }
//
//    //AbstractAutoProxyCreator:
//    private Advisor[] resolveInterceptorNames() {
//        BeanFactory bf = this.beanFactory;
//        ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
//        List<Advisor> advisors = new ArrayList<>();
//        for (String beanName : this.interceptorNames) {
//            if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
//                Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
//                Object next = bf.getBean(beanName);
//                advisors.add(this.advisorAdapterRegistry.wrap(next));
//            }
//        }
//        return advisors.toArray(new Advisor[0]);
//    }
//
//    //DefaultAdvisorAdapterRegistry:
//    public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
//        if (adviceObject instanceof Advisor) {
//            return (Advisor) adviceObject;
//        }
//        if (!(adviceObject instanceof Advice)) {
//            throw new UnknownAdviceTypeException(adviceObject);
//        }
//        Advice advice = (Advice) adviceObject;
//        if (advice instanceof MethodInterceptor) {
//            // So well-known it doesn't even need an adapter.
//            return new DefaultPointcutAdvisor(advice);
//        }
//        for (AdvisorAdapter adapter : this.adapters) {
//            // Check that it is supported.
//            if (adapter.supportsAdvice(advice)) {
//                return new DefaultPointcutAdvisor(advice);
//            }
//        }
//        throw new UnknownAdviceTypeException(advice);
//    }
//
//
//
//    @org.aspectj.lang.annotation.Pointcut("execution(public * com.gfm.controller.*.*(..))")
//    public void pointCut(){
//    }
//
//    /**
//     * 环绕通知,环绕增强，相当于MethodInterceptor
//     * @param pjp
//     * @return
//     */
//    @Around("pointCut()")
//    public Object around(ProceedingJoinPoint pjp) {
//        System.out.println("-------around start");
//        try{
//            Object result = pjp.proceed();
//            System.out.println("-------around end");
//            return result;
//        }catch (Throwable e){
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//
//
//    //JdkDynamicAopProxy:
//    //jdk动态代理的invoke方法
//    //必须实现{InvocationHandler.invoke} 方法
//    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//        Object oldProxy = null;
//        boolean setProxyContext = false;
//
//        //获取到我们的目标对象
//        TargetSource targetSource = this.advised.targetSource;
//        Object target = null;
//
//        try {
//            //若是equals方法不需要代理
//            if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
//                // The target does not implement the equals(Object) method itself.
//                return equals(args[0]);
//            }
//            //若是hashCode方法不需要代理
//            else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
//                // The target does not implement the hashCode() method itself.
//                return hashCode();
//            }
//            //若是DecoratingProxy也不要拦截器执行
//            else if (method.getDeclaringClass() == DecoratingProxy.class) {
//                // There is only getDecoratedClass() declared -> dispatch to proxy config.
//                return AopProxyUtils.ultimateTargetClass(this.advised);
//            }
//            else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
//                    method.getDeclaringClass().isAssignableFrom(Advised.class)) {
//                // Service invocations on ProxyConfig with the proxy config...
//                return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
//            }
//
//            Object retVal;
//
//            /**
//             * 这个配置是暴露我们的代理对象到线程变量中，需要搭配@EnableAspectJAutoProxy(exposeProxy = true)一起使用
//             * 比如在目标对象方法中再次获取代理对象可以使用这个AopContext.currentProxy()
//             * 还有的就是事务方法调用事务方法的时候也是用到这个
//             */
//            if (this.advised.exposeProxy) {
//                //把我们的代理对象暴露到线程变量中
//                oldProxy = AopContext.setCurrentProxy(proxy);
//                setProxyContext = true;
//            }
//
//            //获取我们的目标对象
//            target = targetSource.getTarget();
//            //获取我们目标对象的class
//            Class<?> targetClass = (target != null ? target.getClass() : null);
//
//            //把aop的advisor转化为拦截器链
//            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
//
//            ////如果拦截器链为空
//            if (chain.isEmpty()) {
//                //通过反射直接调用执行
//                Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
//                retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
//            } else {
//                //创建一个方法调用对象
//                MethodInvocation invocation =
//                        new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
//                //调用执行
//                retVal = invocation.proceed();
//            }
//
//            // Massage return value if necessary.
//            Class<?> returnType = method.getReturnType();
//            if (retVal != null && retVal == target &&
//                    returnType != Object.class && returnType.isInstance(proxy) &&
//                    !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
//                //特殊情况:它返回的“this”,方法的返回类型是兼容的。请注意,目标集本身不能是另一个返回的对象的引用。
//                retVal = proxy;
//            }else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
//                throw new AopInvocationException(
//                        "Null return value from advice does not match primitive return type for: " + method);
//            }
//            return retVal;
//        }finally {
//            if (target != null && !targetSource.isStatic()) {
//                // Must have come from TargetSource.
//                targetSource.releaseTarget(target);
//            }
//            if (setProxyContext) {
//                // Restore old proxy.
//                AopContext.setCurrentProxy(oldProxy);
//            }
//        }
//    }
//
//
//}
