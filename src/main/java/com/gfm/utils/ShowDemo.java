package com.gfm.utils;

import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.parsing.BeanEntry;
import org.springframework.beans.factory.parsing.ConstructorArgumentEntry;
import org.springframework.beans.factory.support.*;
import org.springframework.beans.factory.xml.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.*;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.xml.DomUtils;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.*;
import org.xml.sax.ErrorHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryFinder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
        //获取在IoC容器初始化过程中设置的资源加载器
        ResourceLoader resourceLoader = getResourceLoader();
        if (resourceLoader == null) {
            throw new BeanDefinitionStoreException(
                    "Cannot load bean definitions from location [" + location + "]: no ResourceLoader available");
        }

        if (resourceLoader instanceof ResourcePatternResolver) {
            try {
                //1>将指定位置的Bean定义资源文件解析为Spring IoC容器封装的资源,加载多个指定位置的Bean定义资源文件
                Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
                //2>委派调用其子类XmlBeanDefinitionReader的方法，实现加载功能
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
            //一个类路径资源，多个资源可以使用相同的名字
            if (getPathMatcher().isPattern(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()))) {
                // 获取一个匹配的类路径资源
                return findPathMatchingResources(locationPattern);
            } else {
                // all class path resources with the given name
                return findAllClassPathResources(locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length()));
            }
        }
        else {
            // 通常的模式时查找前缀后面的内容
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

    //PathMatchingResourcePatternResolver:
    protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
        String rootDirPath = determineRootDir(locationPattern);
        String subPattern = locationPattern.substring(rootDirPath.length());
        Resource[] rootDirResources = getResources(rootDirPath);
        Set<Resource> result = new LinkedHashSet<>(16);
        for (Resource rootDirResource : rootDirResources) {
            rootDirResource = resolveRootDirResource(rootDirResource);
            URL rootDirUrl = rootDirResource.getURL();
            if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
                URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
                if (resolvedUrl != null) {
                    rootDirUrl = resolvedUrl;
                }
                rootDirResource = new UrlResource(rootDirUrl);
            }
            if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
                result.addAll(PathMatchingResourcePatternResolver.VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
            }
            else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
                result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
            }
            else {
                result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
        }
        return result.toArray(new Resource[0]);
    }

    //PathMatchingResourcePatternResolver:
    protected String determineRootDir(String location) {
        int prefixEnd = location.indexOf(':') + 1;
        int rootDirEnd = location.length();
        while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }


    //DefaultResourceLoader:
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
            Resource resource = protocolResolver.resolve(location, this);
            if (resource != null) {
                return resource;
            }
        }

        if (location.startsWith("/")) {
            return getResourceByPath(location);
        }//如果是类路径的方式，那需要使用ClassPathResource 来得到bean 文件的资源对象
        else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
        } else {
            try {
                //如果是URL 方式，使用UrlResource 作为bean 文件的资源对象
                URL url = new URL(location);
                return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));
            }catch (MalformedURLException ex) {
                //如果既不是classpath标识，又不是URL标识的Resource定位，则调用
                //容器本身的getResourceByPath方法获取Resource
                return getResourceByPath(location);
            }
        }
    }










    //AbstractBeanDefinitionReader:
    public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
        Assert.notNull(resources, "Resource array must not be null");
        int count = 0;
        for (Resource resource : resources) {
            count += loadBeanDefinitions(resource);
        }
        return count;
    }

    //XmlBeanDefinitionReader:
    //XmlBeanDefinitionReader加载资源的入口方法
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        //将读入的XML资源进行特殊编码处理
        return loadBeanDefinitions(new EncodedResource(resource));
    }

    //XmlBeanDefinitionReader:
    //这里是载入XML形式Bean定义资源文件方法
    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
        Assert.notNull(encodedResource, "EncodedResource must not be null");
        if (logger.isTraceEnabled()) {
            logger.trace("Loading XML bean definitions from " + encodedResource);
        }
        //ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
        //			new NamedThreadLocal<>("XML bean definition resources currently being loaded")
        Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
        if (currentResources == null) {
            currentResources = new HashSet<>(4);
            this.resourcesCurrentlyBeingLoaded.set(currentResources);
        }
        if (!currentResources.add(encodedResource)) {
            throw new BeanDefinitionStoreException(
                    "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
        }
        try {
            //将资源文件转为InputStream的IO流
            InputStream inputStream = encodedResource.getResource().getInputStream();
            try {
                //从InputStream中得到XML的解析源
                InputSource inputSource = new InputSource(inputStream);
                if (encodedResource.getEncoding() != null) {
                    inputSource.setEncoding(encodedResource.getEncoding());
                }
                //1>这里是具体的读取过程
                return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
            }finally {
                //关闭从Resource中得到的IO流
                inputStream.close();
            }
        }catch (IOException ex) {
            throw new BeanDefinitionStoreException(
                    "IOException parsing XML document from " + encodedResource.getResource(), ex);
        }finally {
            currentResources.remove(encodedResource);
            if (currentResources.isEmpty()) {
                this.resourcesCurrentlyBeingLoaded.remove();
            }
        }
    }

    //XmlBeanDefinitionReader:
    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
            throws BeanDefinitionStoreException {

        try {
            //1>将XML文件转换为DOM对象，解析过程由documentLoader实现
            Document doc = doLoadDocument(inputSource, resource);
            //2>这里是启动对Bean定义解析的详细过程，该解析过程会用到Spring的Bean配置规则
            int count = registerBeanDefinitions(doc, resource);
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded " + count + " bean definitions from " + resource);
            }
            return count;
        }catch (BeanDefinitionStoreException ex) {
            throw ex;
        }catch (SAXParseException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                    "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
        }catch (SAXException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(),
                    "XML document from " + resource + " is invalid", ex);
        }catch (ParserConfigurationException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "Parser configuration exception parsing XML from " + resource, ex);
        }catch (IOException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "IOException parsing XML document from " + resource, ex);
        }catch (Throwable ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(),
                    "Unexpected exception parsing XML document from " + resource, ex);
        }
    }


    //XmlBeanDefinitionReader:
    protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
        /**
         * 1.ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger)
         * 2.protected EntityResolver getEntityResolver() {
         *      if (this.entityResolver == null) {
         *          //先使用默认的EntityResolver
         *          ResourceLoader resourceLoader = getResourceLoader();
         *          if (resourceLoader != null) {
         *              this.entityResolver = new ResourceEntityResolver(resourceLoader);
         *          }
         *          else {
         *              this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
         *          }
         *      }
         *      return this.entityResolver;
         * }
         *
         * 3.public boolean isNamespaceAware() {
         *    return this.namespaceAware;
         * }
         */
        return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
                getValidationModeForResource(resource), isNamespaceAware());
    }

    //DefaultDocumentLoader:
    //使用标准的JAXP将载入的Bean定义资源转换成document对象
    public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
                                 ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {
        //1>创建文件解析器工厂
        DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
        if (logger.isTraceEnabled()) {
            logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
        }
        //2>创建文档解析器
        DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
        //3>解析Spring的Bean定义资源
        return builder.parse(inputSource);
    }

    //DefaultDocumentLoader:
    protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
            throws ParserConfigurationException {
        //创建文档解析工厂
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAware);
        //设置解析XML的校验
        if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
            factory.setValidating(true);
            if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
                // Enforce namespace aware for XSD...
                factory.setNamespaceAware(true);
                try {
                    factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
                }catch (IllegalArgumentException ex) {
                    ParserConfigurationException pcex = new ParserConfigurationException("Unable to validate using XSD");
                    pcex.initCause(ex);
                    throw pcex;
                }
            }
        }
        return factory;
    }

    //DocumentBuilderFactory:
    public static DocumentBuilderFactory newInstance() {
        return FactoryFinder.find(
                /* The default property name according to the JAXP spec */
                DocumentBuilderFactory.class, // "javax.xml.parsers.DocumentBuilderFactory"
                /* The fallback implementation class name */
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }


    //DefaultDocumentLoader:
    protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory,
                                                    @Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
            throws ParserConfigurationException {

        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        if (entityResolver != null) {
            docBuilder.setEntityResolver(entityResolver);
        }
        if (errorHandler != null) {
            docBuilder.setErrorHandler(errorHandler);
        }
        return docBuilder;
    }




    //DocumentBuilderImpl:
    //将输入流解析成Document对象
    public Document parse(InputSource is) throws SAXException, IOException {
        if (is == null) {
            throw new IllegalArgumentException(
                    DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN,
                            "jaxp-null-input-source", null));
        }
        //XMLComponent fSchemaValidator;
        if (fSchemaValidator != null) {
            //ValidationManager fSchemaValidationManager
            if (fSchemaValidationManager != null) {
                fSchemaValidationManager.reset();
                fUnparsedEntityHandler.reset();
            }
            resetSchemaValidator();
        }
        //DOMParser domParser
        domParser.parse(is);
        Document doc = domParser.getDocument();
        //清除document引用，实际上就是将其置为null, 能够被GC回收
        domParser.dropDocumentReferences();
        return doc;
    }

    //AbstractDOMParser
    public final void dropDocumentReferences() {
        fDocument = null;
        fDocumentImpl = null;
        fDeferredDocumentImpl = null;
        fDocumentType = null;
        fCurrentNode = null;
        fCurrentCDATASection = null;
        fCurrentEntityDecl = null;
        fRoot = null;
    }


    //BeanDefinitionParserDelegate:
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
        //解析<Bean>元素的入口
        return parseBeanDefinitionElement(ele, null);
    }

    //BeanDefinitionParserDelegate:
    //解析Bean定义资源文件中的<Bean>元素，这个方法中主要处理<Bean>元素的id，name和别名属性
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
        String id = ele.getAttribute(ID_ATTRIBUTE); //获取<Bean>元素中的id属性值
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE); //获取<Bean>元素中的name属性值

        //获取<Bean>元素中的alias属性值
        List<String> aliases = new ArrayList<>();
        if (StringUtils.hasLength(nameAttr)) {
            //String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; "
            String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
            aliases.addAll(Arrays.asList(nameArr));
        }

        String beanName = id;
        //如果<Bean>元素中没有配置id属性时，将别名中的第一个值赋值给beanName
        if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
            beanName = aliases.remove(0);
            if (logger.isTraceEnabled()) {
                logger.trace("No XML 'id' specified - using '" + beanName +
                        "' as bean name and " + aliases + " as aliases");
            }
        }
        //检查<Bean>元素所配置的id或者name的唯一性，containingBean标识<Bean>元素中是否包含子<Bean>元素
        if (containingBean == null) {
            checkNameUniqueness(beanName, aliases, ele);
        }

        //详细对<Bean>元素中配置的Bean定义进行解析的地方
        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
        if (beanDefinition != null) {
            if (!StringUtils.hasText(beanName)) {
                try {
                    if (containingBean != null) {
                        //如果<Bean>元素中没有配置id、别名或者name，且没有包含子<Bean>元素，为解析的Bean生成一个唯一beanName并注册
                        beanName = BeanDefinitionReaderUtils.generateBeanName(
                                beanDefinition, this.readerContext.getRegistry(), true);
                    }else {
                        //如果<Bean>元素中没有配置id、别名或者name，且包含了子<Bean>元素，为解析的Bean使用别名向IoC容器注册
                        beanName = this.readerContext.generateBeanName(beanDefinition);
                        //为解析的Bean使用别名注册时，为了向后兼容 Spring1.2/2.0，给别名添加类名后缀
                        String beanClassName = beanDefinition.getBeanClassName();
                        if (beanClassName != null &&
                                beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
                                !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                            aliases.add(beanClassName);
                        }
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Neither XML 'id' nor 'name' specified - " +
                                "using generated bean name [" + beanName + "]");
                    }
                }catch (Exception ex) {
                    error(ex.getMessage(), ele);
                    return null;
                }
            }
            String[] aliasesArray = StringUtils.toStringArray(aliases);
            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
        }

        return null;
    }


    //BeanDefinitionParserDelegate:
    protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
        String foundName = null;

        if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
            foundName = beanName;
        }
        if (foundName == null) {
            foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
        }
        if (foundName != null) {
            error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
        }

        //Set<String> usedNames = new HashSet<>(), 存储所有bean的名称，包括别名
        this.usedNames.add(beanName);
        this.usedNames.addAll(aliases);
    }


    //BeanDefinitionParserDelegate:
    public AbstractBeanDefinition parseBeanDefinitionElement(
            Element ele, String beanName, @Nullable BeanDefinition containingBean) {

        this.parseState.push(new BeanEntry(beanName));

        String className = null;
        if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
            className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
        }
        String parent = null;
        if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
            parent = ele.getAttribute(PARENT_ATTRIBUTE);
        }

        try {
            AbstractBeanDefinition bd = createBeanDefinition(className, parent);

            parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
            bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));

            parseMetaElements(ele, bd);
            parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
            parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

            parseConstructorArgElements(ele, bd);
            parsePropertyElements(ele, bd);
            parseQualifierElements(ele, bd);

            bd.setResource(this.readerContext.getResource());
            bd.setSource(extractSource(ele));

            return bd;
        }catch (ClassNotFoundException ex) {
            error("Bean class [" + className + "] not found", ele, ex);
        }catch (NoClassDefFoundError err) {
            error("Class that bean class [" + className + "] depends on not found", ele, err);
        }catch (Throwable ex) {
            error("Unexpected failure during bean definition parsing", ele, ex);
        }finally {
            this.parseState.pop();
        }

        return null;
    }


    //BeanDefinitionParserDelegate:
    public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            //CONSTRUCTOR_ARG_ELEMENT = "constructor-arg"
            if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
                parseConstructorArgElement((Element) node, bd);
            }
        }
    }

    //BeanDefinitionParserDelegate:
    public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
        String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE); //INDEX_ATTRIBUTE = "index"
        String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE); //TYPE_ATTRIBUTE = "type"
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE); //NAME_ATTRIBUTE = "name"
        if (StringUtils.hasLength(indexAttr)) {
            try {
                int index = Integer.parseInt(indexAttr);
                if (index < 0) {
                    error("'index' cannot be lower than 0", ele);
                }else {
                    try {
                        //parseState ==> ParseState.state = LinkedList<Entry>
                        this.parseState.push(new ConstructorArgumentEntry(index));
                        Object value = parsePropertyValue(ele, bd, null);
                        ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                        if (StringUtils.hasLength(typeAttr)) {
                            valueHolder.setType(typeAttr);
                        }
                        if (StringUtils.hasLength(nameAttr)) {
                            valueHolder.setName(nameAttr);
                        }
                        valueHolder.setSource(extractSource(ele));
                        if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
                            error("Ambiguous constructor-arg entries for index " + index, ele);
                        }
                        else {
                            bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
                        }
                    }finally {
                        this.parseState.pop();
                    }
                }
            }catch (NumberFormatException ex) {
                error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
            }
        }else {
            try {
                this.parseState.push(new ConstructorArgumentEntry());
                Object value = parsePropertyValue(ele, bd, null);
                ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                if (StringUtils.hasLength(typeAttr)) {
                    valueHolder.setType(typeAttr);
                }
                if (StringUtils.hasLength(nameAttr)) {
                    valueHolder.setName(nameAttr);
                }
                valueHolder.setSource(extractSource(ele));
                bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
            }finally {
                this.parseState.pop();
            }
        }
    }


    public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
        String elementName = (propertyName != null ?
                "<property> element for property '" + propertyName + "'" :
                "<constructor-arg> element");

        // Should only have one child element: ref, value, list, etc.
        NodeList nl = ele.getChildNodes();
        Element subElement = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
                    !nodeNameEquals(node, META_ELEMENT)) {
                // Child element is what we're looking for.
                if (subElement != null) {
                    error(elementName + " must not contain more than one sub-element", ele);
                }else {
                    subElement = (Element) node;
                }
            }
        }

        boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
        boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
        if ((hasRefAttribute && hasValueAttribute) ||
                ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
            error(elementName +
                    " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
        }

        if (hasRefAttribute) {
            String refName = ele.getAttribute(REF_ATTRIBUTE);
            if (!StringUtils.hasText(refName)) {
                error(elementName + " contains empty 'ref' attribute", ele);
            }
            RuntimeBeanReference ref = new RuntimeBeanReference(refName);
            ref.setSource(extractSource(ele));
            return ref;
        } else if (hasValueAttribute) {
            TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
            valueHolder.setSource(extractSource(ele));
            return valueHolder;
        }
        else if (subElement != null) {
            return parsePropertySubElement(subElement, bd);
        }
        else {
            // Neither child element nor "ref" or "value" attribute found.
            error(elementName + " must specify a ref or value", ele);
            return null;
        }
    }




    //BeanDefinitionReaderUtils:
    //注册BeanDefinition到BeanFactory
    public static void registerBeanDefinition(
            BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
            throws BeanDefinitionStoreException {

        //在主名称下注册bean定义
        String beanName = definitionHolder.getBeanName();
        registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

        //为bean名称注册别名(如果有)
        String[] aliases = definitionHolder.getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                registry.registerAlias(beanName, alias);
            }
        }
    }

    //DefaultListableBeanFactory:
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
            throws BeanDefinitionStoreException {
        Assert.hasText(beanName, "Bean name must not be empty");
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");

        if (beanDefinition instanceof AbstractBeanDefinition) {
            try {
                //校验BeanDefinition
                ((AbstractBeanDefinition) beanDefinition).validate();
            }catch (BeanDefinitionValidationException ex) {
                throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
                        "Validation of bean definition failed", ex);
            }
        }

        //根据beanName获取BeanDefinition容器里的BeanDefinition
        //Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256), key=beanName, value=BeanDefinition
        BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
        if (existingDefinition != null) { //如果此BeanDefinition已经存在
            //如果不允许覆盖同名的BeanDefinition, 就抛出异常
            if (!isAllowBeanDefinitionOverriding()) {
                throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
            }else if (existingDefinition.getRole() < beanDefinition.getRole()) {
                // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
                if (logger.isInfoEnabled()) {
                    logger.info("Overriding user-defined bean definition for bean '" + beanName +
                            "' with a framework-generated bean definition: replacing [" +
                            existingDefinition + "] with [" + beanDefinition + "]");
                }
            }else if (!beanDefinition.equals(existingDefinition)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Overriding bean definition for bean '" + beanName +
                            "' with a different definition: replacing [" + existingDefinition +
                            "] with [" + beanDefinition + "]");
                }
            }else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Overriding bean definition for bean '" + beanName +
                            "' with an equivalent definition: replacing [" + existingDefinition +
                            "] with [" + beanDefinition + "]");
                }
            }
            //允许覆盖同名BeanDefinition, 就用新的BeanDefinition替换原来的
            this.beanDefinitionMap.put(beanName, beanDefinition);
        }else { //如果此BeanDefinition在容器里不存在
            if (hasBeanCreationStarted()) { //bean已经在工厂里创建
                //防止其他线程同步修改BeanDefinition容器
                synchronized (this.beanDefinitionMap) {
                    this.beanDefinitionMap.put(beanName, beanDefinition);
                    List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
                    //将新创建的bean定义名称添加到已有的bean定义名称列表中
                    //List<String> beanDefinitionNames = new ArrayList<>(256), bean定义名称列表，按注册顺序排列.
                    updatedDefinitions.addAll(this.beanDefinitionNames);
                    updatedDefinitions.add(beanName);
                    this.beanDefinitionNames = updatedDefinitions;
                    removeManualSingletonName(beanName);
                }
            }
            else {
                //仍在启动注册阶段
                this.beanDefinitionMap.put(beanName, beanDefinition);
                this.beanDefinitionNames.add(beanName);
                removeManualSingletonName(beanName);
            }
            this.frozenBeanDefinitionNames = null;
        }

        if (existingDefinition != null || containsSingleton(beanName)) {
            resetBeanDefinition(beanName);
        }
    }


    //AbstractBeanDefinition:
    //校验BeanDefinition
    public void validate() throws BeanDefinitionValidationException {
        if (hasMethodOverrides() && getFactoryMethodName() != null) {
            throw new BeanDefinitionValidationException(
                    "Cannot combine factory method with container-generated method overrides: " +
                            "the factory method must create the concrete bean instance.");
        }
        if (hasBeanClass()) {
            prepareMethodOverrides();
        }
    }

    //AbstractBeanDefinition:
    public void prepareMethodOverrides() throws BeanDefinitionValidationException {
        //校验查找方法是否存在并确定它们的重载状态
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
            //标记此方法没有被重载
            mo.setOverloaded(false);
        }
    }


    //---------------------------------

    //AbstractBeanFactory:
    //校验这个工厂的bean是否已经开始创建
    protected boolean hasBeanCreationStarted() {
        //至少已经创建过一次的bean的名称的集合
        //Set<String> alreadyCreated = Collections.newSetFromMap(new ConcurrentHashMap<>(256))
        return !this.alreadyCreated.isEmpty();
    }

    //DefaultListableBeanFactory:
    private void removeManualSingletonName(String beanName) {
        updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
    }


    //DefaultListableBeanFactory:
    //更新工厂的内部手动单例名称集
    private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
        if (hasBeanCreationStarted()) {
            // Cannot modify startup-time collection elements anymore (for stable iteration)
            synchronized (this.beanDefinitionMap) {
                //Set<String> manualSingletonNames = new LinkedHashSet<>(16)；手动注册的单例名称集，按注册顺序排列
                if (condition.test(this.manualSingletonNames)) {
                    Set<String> updatedSingletons = new LinkedHashSet<>(this.manualSingletonNames);
                    action.accept(updatedSingletons);
                    this.manualSingletonNames = updatedSingletons;
                }
            }
        }
        else {
            //还处于启动注册阶段
            if (condition.test(this.manualSingletonNames)) {
                action.accept(this.manualSingletonNames);
            }
        }
    }


    //DefaultListableBeanFactory:
    public boolean containsSingleton(String beanName) {
        //Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256)
        //单例对象的缓存:key=bean名, value=bean实例
        return this.singletonObjects.containsKey(beanName);
    }


    //DefaultListableBeanFactory:
    //重置给定bean的所有bean定义缓存，包括从它派生的bean的缓存
    protected void resetBeanDefinition(String beanName) {
        // 删除已创建的给定bean的合并bean定义
        clearMergedBeanDefinition(beanName);

        //从单例缓存中删除对应的bean(如果有的话)。通常不应该
        //是必需的，而不仅仅意味着要覆盖上下文的默认bean
        destroySingleton(beanName);

        // 通知所有post-processors指定的bean定义已被重置
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            if (processor instanceof MergedBeanDefinitionPostProcessor) {
                ((MergedBeanDefinitionPostProcessor) processor).resetBeanDefinition(beanName);
            }
        }

        // 重置所有将给定bean作为父bean的bean定义(递归地)
        for (String bdName : this.beanDefinitionNames) {
            if (!beanName.equals(bdName)) {
                BeanDefinition bd = this.beanDefinitionMap.get(bdName);
                if (bd != null && beanName.equals(bd.getParentName())) {
                    resetBeanDefinition(bdName);
                }
            }
        }
    }

    //AbstractBeanFactory:
    //删除指定bean的合并bean定义，在next acces上重新创建它
    protected void clearMergedBeanDefinition(String beanName) {
        //Map<String, RootBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256)
        //从bean名称映射到合并的RootBeanDefinition
        RootBeanDefinition bd = this.mergedBeanDefinitions.get(beanName);
        if (bd != null) {
            //boolean stale; 确定是否需要重新合并定义
            bd.stale = true;
        }
    }

    //DefaultListableBeanFactory:
    public void destroySingleton(String beanName) {
        super.destroySingleton(beanName);
        removeManualSingletonName(beanName);
        clearByTypeCache();
    }

    //DefaultSingletonBeanRegistry:
    public void destroySingleton(String beanName) {
        //删除已注册的给定名称单例(如果有的话)
        removeSingleton(beanName);

        //销毁相应的DisposableBean实例
        DisposableBean disposableBean;
        // Map<String, Object> disposableBeans = new LinkedHashMap<>()
        //包含处理bean实例的map， 映射为：beanName --> disposableBean
        synchronized (this.disposableBeans) {
            disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
        }
        destroyBean(beanName, disposableBean);
    }

    //DefaultSingletonBeanRegistry:
    protected void removeSingleton(String beanName) {
        //Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256)
        //缓存的单例对象，beanName --> bean instance
        synchronized (this.singletonObjects) {
            this.singletonObjects.remove(beanName);
            //Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16)
            //单例工厂的缓存: beanName --> ObjectFactory
            this.singletonFactories.remove(beanName);
            //Map<String, Object> earlySingletonObjects = new HashMap<>(16)
            //早期单例对象的缓存: beanName --> bean instance
            this.earlySingletonObjects.remove(beanName);
            //Set<String> registeredSingletons = new LinkedHashSet<>(256)
            //已注册的单例集合，包含按注册顺序排列的bean名称
            this.registeredSingletons.remove(beanName);
        }
    }

    //DefaultSingletonBeanRegistry:
    //销毁给定的bean。必须在bean销毁之前销毁依赖于它本身的bean。不应该抛出任何异常。
    protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
        //首先触发从属bean的销毁
        Set<String> dependencies;
        synchronized (this.dependentBeanMap) {
            // Within full synchronization in order to guarantee a disconnected Set
            dependencies = this.dependentBeanMap.remove(beanName);
        }
        if (dependencies != null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
            }
            for (String dependentBeanName : dependencies) {
                destroySingleton(dependentBeanName);
            }
        }

        // Actually destroy the bean now...
        if (bean != null) {
            try {
                bean.destroy();
            }
            catch (Throwable ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Destruction of bean with name '" + beanName + "' threw an exception", ex);
                }
            }
        }

        // Trigger destruction of contained beans...
        Set<String> containedBeans;
        synchronized (this.containedBeanMap) {
            // Within full synchronization in order to guarantee a disconnected Set
            containedBeans = this.containedBeanMap.remove(beanName);
        }
        if (containedBeans != null) {
            for (String containedBeanName : containedBeans) {
                destroySingleton(containedBeanName);
            }
        }

        // Remove destroyed bean from other beans' dependencies.
        synchronized (this.dependentBeanMap) {
            for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Set<String>> entry = it.next();
                Set<String> dependenciesToClean = entry.getValue();
                dependenciesToClean.remove(beanName);
                if (dependenciesToClean.isEmpty()) {
                    it.remove();
                }
            }
        }

        // Remove destroyed bean's prepared dependency information.
        this.dependenciesForBeanMap.remove(beanName);
    }

}
