//package com.gfm.utils;
//
//import org.springframework.beans.*;
//import org.springframework.beans.factory.BeanDefinitionStoreException;
//import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
//import org.springframework.beans.factory.config.*;
//import org.springframework.beans.factory.parsing.*;
//import org.springframework.beans.factory.support.*;
//import org.springframework.beans.factory.xml.*;
//import org.springframework.context.annotation.AnnotationConfigUtils;
//import org.springframework.core.KotlinDetector;
//import org.springframework.core.annotation.AnnotationAttributes;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.ResourcePatternUtils;
//import org.springframework.core.type.AnnotationMetadata;
//import org.springframework.lang.Nullable;
//import org.springframework.util.*;
//import org.springframework.util.xml.DomUtils;
//import org.w3c.dom.*;
//
//import java.beans.Introspector;
//import java.io.IOException;
//import java.lang.reflect.Constructor;
//import java.lang.reflect.InvocationTargetException;
//import java.net.URISyntaxException;
//import java.util.*;
//
//public class BeanDemo {
//    public static void main(String[] args) {
//
//    }
//
//
//    //XmlBeanDefinitionReader:
//    //按照Spring的Bean语义要求将Bean定义资源解析并转换为容器内部数据结构
//    public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
//        //1>得到BeanDefinitionDocumentReader来对xml格式的BeanDefinition解析
//        BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
//        //2>获得容器中注册的Bean数量
//        int countBefore = getRegistry().getBeanDefinitionCount();
//        //3>解析过程入口，这里使用了委派模式，BeanDefinitionDocumentReader只是个接口，
//        //  具体的解析实现过程有实现类DefaultBeanDefinitionDocumentReader完成
//        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
//        //统计解析的Bean数量
//        return getRegistry().getBeanDefinitionCount() - countBefore;
//    }
//
//    //XmlBeanDefinitionReader:
//    protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
//        //Class<? extends BeanDefinitionDocumentReader> documentReaderClass =
//        //    DefaultBeanDefinitionDocumentReader.class;
//        return BeanUtils.instantiateClass(this.documentReaderClass);
//    }
//
//    //BeanUtils:
//    public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
//        Assert.notNull(clazz, "Class must not be null");
//        if (clazz.isInterface()) {
//            throw new BeanInstantiationException(clazz, "Specified class is an interface");
//        }
//        try {
//            return instantiateClass(clazz.getDeclaredConstructor());
//        }catch (NoSuchMethodException ex) {
//            Constructor<T> ctor = findPrimaryConstructor(clazz);
//            if (ctor != null) {
//                return instantiateClass(ctor);
//            }
//            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
//        }catch (LinkageError err) {
//            throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
//        }
//    }
//
//    //BeanUtils:
//    //用给定的构造函数便利的实例化一个类，使用java反射机制实现
//    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
//        Assert.notNull(ctor, "Constructor must not be null");
//        try {
//            ReflectionUtils.makeAccessible(ctor);
//            if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
//                return BeanUtils.KotlinDelegate.instantiateClass(ctor, args);
//            }else {
//                Class<?>[] parameterTypes = ctor.getParameterTypes();
//                Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
//                Object[] argsWithDefaultValues = new Object[args.length];
//                for (int i = 0 ; i < args.length; i++) {
//                    if (args[i] == null) {
//                        Class<?> parameterType = parameterTypes[i];
//                        argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
//                    }else {
//                        argsWithDefaultValues[i] = args[i];
//                    }
//                }
//                return ctor.newInstance(argsWithDefaultValues);
//            }
//        }catch (InstantiationException ex) {
//            throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
//        }catch (IllegalAccessException ex) {
//            throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
//        }catch (IllegalArgumentException ex) {
//            throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
//        }catch (InvocationTargetException ex) {
//            throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
//        }
//    }
//
//    //AbstractBeanDefinitionReader:
//    public final BeanDefinitionRegistry getRegistry() {
//        //BeanDefinitionRegistry registry
//        return this.registry;
//    }
//
//    //DefaultListableBeanFactory:
//    public int getBeanDefinitionCount() {
//        //Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256)
//        //key是bean的名称，value是BeanDefinition
//        return this.beanDefinitionMap.size();
//    }
//
//    //DefaultBeanDefinitionDocumentReader:
//    public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
//        //XmlReaderContext readerContext
//        this.readerContext = readerContext;
//        doRegisterBeanDefinitions(doc.getDocumentElement());
//    }
//
//    //DefaultBeanDefinitionDocumentReader:
//    //根据Spring DTD对Bean的定义规则解析Bean定义Document对象
//    protected void doRegisterBeanDefinitions(Element root) {
//        //任何嵌套的<beans>元素将导致该方法递归， 为了传播和保存<bean>默认属性的正确，跟踪当前(父)委托，
//        //这可能是null, 创建一个子委托引用父对象实现回调，最终重置this.delegate为它的父引用
//        BeanDefinitionParserDelegate parent = this.delegate;
//        //1>创建BeanDefinition解析委托器
//        this.delegate = createDelegate(getReaderContext(), root, parent);
//
//        if (this.delegate.isDefaultNamespace(root)) {
//            //String PROFILE_ATTRIBUTE = "profile"
//            String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
//            //如果存在profile引用文件
//            if (StringUtils.hasText(profileSpec)) {
//                String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
//                        profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
//                //profile文件校验
//                if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec +
//                                "] not matching: " + getReaderContext().getResource());
//                    }
//                    return;
//                }
//            }
//        }
//
//        //在解析Bean定义之前，进行自定义的解析，增强解析过程的可扩展性
//        preProcessXml(root);
//        //2>从Document的根元素开始进行Bean定义的Document对象解析
//        parseBeanDefinitions(root, this.delegate);
//        //在解析Bean定义之后，进行自定义的解析，增加解析过程的可扩展性
//        postProcessXml(root);
//
//        this.delegate = parent;
//    }
//
//    //DefaultBeanDefinitionDocumentReader:
//    protected BeanDefinitionParserDelegate createDelegate(
//            XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
//
//        //创建BeanDefinitionParserDelegate，用于完成真正的解析过程
//        BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
//        //初始化Document根元素
//        delegate.initDefaults(root, parentDelegate);
//        return delegate;
//    }
//
//    //BeanDefinitionParserDelegate:
//    public void initDefaults(Element root, @Nullable BeanDefinitionParserDelegate parent) {
//        populateDefaults(this.defaults, (parent != null ? parent.defaults : null), root);
//        this.readerContext.fireDefaultsRegistered(this.defaults);
//    }
//
//    //BeanDefinitionParserDelegate:
//    //为BeanDefinition元素设置默认值
//    protected void populateDefaults(DocumentDefaultsDefinition defaults, @Nullable DocumentDefaultsDefinition parentDefaults, Element root) {
//        String lazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE); //default-lazy-init
//        if (isDefaultValue(lazyInit)) {
//            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
//            lazyInit = (parentDefaults != null ? parentDefaults.getLazyInit() : FALSE_VALUE);
//        }
//        defaults.setLazyInit(lazyInit);
//
//        String merge = root.getAttribute(DEFAULT_MERGE_ATTRIBUTE); //default-merge
//        if (isDefaultValue(merge)) {
//            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
//            merge = (parentDefaults != null ? parentDefaults.getMerge() : FALSE_VALUE);
//        }
//        defaults.setMerge(merge);
//
//        String autowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE); //default-autowire
//        if (isDefaultValue(autowire)) {
//            // Potentially inherited from outer <beans> sections, otherwise falling back to 'no'.
//            autowire = (parentDefaults != null ? parentDefaults.getAutowire() : AUTOWIRE_NO_VALUE);
//        }
//        defaults.setAutowire(autowire);
//
//        if (root.hasAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE)) { //default-autowire-candidates
//            defaults.setAutowireCandidates(root.getAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE));
//        }else if (parentDefaults != null) {
//            defaults.setAutowireCandidates(parentDefaults.getAutowireCandidates());
//        }
//
//        if (root.hasAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE)) { //default-init-method
//            defaults.setInitMethod(root.getAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE));
//        }else if (parentDefaults != null) {
//            defaults.setInitMethod(parentDefaults.getInitMethod());
//        }
//
//        if (root.hasAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE)) { //default-destroy-method
//            defaults.setDestroyMethod(root.getAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE));
//        }else if (parentDefaults != null) {
//            defaults.setDestroyMethod(parentDefaults.getDestroyMethod());
//        }
//
//        defaults.setSource(this.readerContext.extractSource(root));
//    }
//
//    private boolean isDefaultValue(String value) {
//        return (DEFAULT_VALUE.equals(value) || "".equals(value));
//    }
//
//    //ReaderContext:
//    //激活一个默认的注册事件
//    public void fireDefaultsRegistered(DefaultsDefinition defaultsDefinition) {
//        this.eventListener.defaultsRegistered(defaultsDefinition);
//    }
//
//
//
//
//    //DefaultBeanDefinitionDocumentReader:
//    //使用Spring的Bean规则从Document的根元素开始进行Bean定义的Document对象
//    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
//        //1>Bean定义的Document对象使用了Spring默认的XML命名空间
//        if (delegate.isDefaultNamespace(root)) {
//            NodeList nl = root.getChildNodes();
//            for (int i = 0; i < nl.getLength(); i++) {
//                Node node = nl.item(i);
//                //获得Document节点是XML元素节点
//                if (node instanceof Element) {
//                    Element ele = (Element) node;
//                    //Bean定义的Document的元素节点使用的是Spring默认的XML命名空间
//                    if (delegate.isDefaultNamespace(ele)) {
//                        //2>使用Spring的Bean规则解析元素节点
//                        parseDefaultElement(ele, delegate);
//                    }
//                    else {
//                        delegate.parseCustomElement(ele);
//                    }
//                }
//            }
//        } else {
//            //3>Document的根节点没有使用Spring默认的命名空间，则使用用户自定义的解析规则解析Document根节点
//            delegate.parseCustomElement(root);
//        }
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public boolean isDefaultNamespace(Node node) {
//        return isDefaultNamespace(getNamespaceURI(node));
//    }
//
//    //BeanDefinitionParserDelegate:
//    public boolean isDefaultNamespace(@Nullable String namespaceUri) {
//        //String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans"
//        return (!StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri));
//    }
//
//
//    //DefaultBeanDefinitionDocumentReader:
//    //使用Spring的Bean规则解析Document元素节点
//    private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
//        //1>如果元素节点是<Import>导入元素，进行导入解析
//        if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
//            importBeanDefinitionResource(ele);
//        }
//        //2>如果元素节点是<Alias>别名元素，进行别名解析
//        else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
//            processAliasRegistration(ele);
//        }
//        //3>元素节点既不是导入元素，也不是别名元素，即普通的<Bean>元素,按照Spring的Bean规则解析元素
//        else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
//            processBeanDefinition(ele, delegate);
//        }
//        //String NESTED_BEANS_ELEMENT = "beans",
//        //如果是嵌套的beans元素，则递归调用doRegisterBeanDefinitions循环解析
//        else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
//            // recurse
//            doRegisterBeanDefinitions(ele);
//        }
//    }
//
//    //DefaultBeanDefinitionDocumentReader:
//    //解析<Import>导入元素，从给定的导入路径加载Bean定义资源到Spring IoC容器中
//    protected void importBeanDefinitionResource(Element ele) {
//        String location = ele.getAttribute(RESOURCE_ATTRIBUTE); //RESOURCE_ATTRIBUTE = "resource"
//        //如果导入元素的location属性值为空，则没有导入任何资源，直接返回
//        if (!StringUtils.hasText(location)) {
//            getReaderContext().error("Resource location must not be empty", ele);
//            return;
//        }
//
//        //使用系统变量值解析location属性值
//        location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);
//
//        Set<Resource> actualResources = new LinkedHashSet<>(4);
//
//        //标识给定的导入元素的location是否是绝对路径
//        boolean absoluteLocation = false;
//        try {
//            absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
//        }catch (URISyntaxException ex) {
//            //给定的导入元素的location不是绝对路径
//        }
//
//        //给定的导入元素的location是绝对路径
//        if (absoluteLocation) {
//            try {
//                //使用资源读入器加载给定路径的Bean定义资源
//                int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
//                if (logger.isTraceEnabled()) {
//                    logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
//                }
//            }catch (BeanDefinitionStoreException ex) {
//                getReaderContext().error(
//                        "Failed to import bean definitions from URL location [" + location + "]", ele, ex);
//            }
//        }
//        else {
//            //给定的导入元素的location是相对路径
//            try {
//                int importCount;
//                Resource relativeResource = getReaderContext().getResource().createRelative(location);
//                //封装的相对路径资源存在
//                if (relativeResource.exists()) {
//                    //使用资源读入器加载Bean定义资源
//                    importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
//                    actualResources.add(relativeResource);
//                }else {
//                    //封装的相对路径资源不存在
//
//                    //获取Spring IoC容器资源读入器的基本路径
//                    String baseLocation = getReaderContext().getResource().getURL().toString();
//                    //根据Spring IoC容器资源读入器的基本路径加载给定导入路径的资源
//                    importCount = getReaderContext().getReader().loadBeanDefinitions(
//                            StringUtils.applyRelativePath(baseLocation, location), actualResources);
//                }
//                if (logger.isTraceEnabled()) {
//                    logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
//                }
//            }catch (IOException ex) {
//                getReaderContext().error("Failed to resolve current resource location", ele, ex);
//            }catch (BeanDefinitionStoreException ex) {
//                getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",ele,ex);
//            }
//        }
//        Resource[] actResArray = actualResources.toArray(new Resource[0]);
//        //在解析完<Import>元素之后，发送容器导入其他资源处理完成事件
//        getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
//    }
//
//
//    //DefaultBeanDefinitionDocumentReader:
//    //解析<Alias>别名元素，为Bean向Spring IoC容器注册别名
//    protected void processAliasRegistration(Element ele) {
//        //获取<Alias>别名元素中name的属性值
//        String name = ele.getAttribute(NAME_ATTRIBUTE);
//        //获取<Alias>别名元素中alias的属性值
//        String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
//        boolean valid = true;
//        //<alias>别名元素的name属性值为空
//        if (!StringUtils.hasText(name)) {
//            getReaderContext().error("Name must not be empty", ele);
//            valid = false;
//        }
//        //<alias>别名元素的alias属性值为空
//        if (!StringUtils.hasText(alias)) {
//            getReaderContext().error("Alias must not be empty", ele);
//            valid = false;
//        }
//        if (valid) {
//            try {
//                //向容器的资源读入器注册别名
//                getReaderContext().getRegistry().registerAlias(name, alias);
//            }catch (Exception ex) {
//                getReaderContext().error("Failed to register alias '" + alias +
//                        "' for bean with name '" + name + "'", ele, ex);
//            }
//            //在解析完<Alias>元素之后，发送容器别名处理完成事件
//            getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
//        }
//    }
//
//    //SimpleAliasRegistry:
//    public void registerAlias(String name, String alias) {
//        Assert.hasText(name, "'name' must not be empty");
//        Assert.hasText(alias, "'alias' must not be empty");
//        //Map<String, String> aliasMap = new ConcurrentHashMap<>(16)
//        //key=别名，value=名字，别名到名字的映射map
//        synchronized (this.aliasMap) {
//            //别名和名字相同
//            if (alias.equals(name)) {
//                //移除别名
//                this.aliasMap.remove(alias);
//                if (logger.isDebugEnabled()) {
//                    logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
//                }
//            }
//            else {
//                String registeredName = this.aliasMap.get(alias);
//                if (registeredName != null) {
//                    //别名映射的名字已经存在，直接返回
//                    if (registeredName.equals(name)) {
//                        // An existing alias - no need to re-register
//                        return;
//                    }
//                    if (!allowAliasOverriding()) {
//                        throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" +
//                                name + "': It is already registered for name '" + registeredName + "'.");
//                    }
//                    if (logger.isDebugEnabled()) {
//                        logger.debug("Overriding alias '" + alias + "' definition for registered name '" +
//                                registeredName + "' with new target name '" + name + "'");
//                    }
//                }
//                //别名已经存在，直接抛出异常
//                checkForAliasCircle(name, alias);
//                //将新的别名存放在aliasMap中
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
//        //判断别名是否已经存在
//        if (hasAlias(alias, name)) {
//            throw new IllegalStateException("Cannot register alias '" + alias +
//                    "' for name '" + name + "': Circular reference - '" +
//                    name + "' is a direct or indirect alias for '" + alias + "' already");
//        }
//    }
//
//    //SimpleAliasRegistry:
//    public boolean hasAlias(String name, String alias) {
//        //Map<String, String> aliasMap = new ConcurrentHashMap<>(16)
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
//
//    //DefaultBeanDefinitionDocumentReader:
//    //解析Bean定义资源Document对象的普通元素
//    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
//        //1>BeanDefinitionHolder是对BeanDefinition的封装，即Bean定义的封装类
//        //对Document对象中<Bean>元素的解析由BeanDefinitionParserDelegate实现
//        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
//        if (bdHolder != null) {
//            //2>如果BeanDefinition定义了required属性,就装饰该BeanDefinition
//            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
//            try {
//                //3>向Spring IoC容器注册解析得到的Bean定义，这是Bean定义向IoC容器注册的入口
//                BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
//            }catch (BeanDefinitionStoreException ex) {
//                getReaderContext().error("Failed to register bean definition with name '" +
//                        bdHolder.getBeanName() + "'", ele, ex);
//            }
//            //4>在完成向Spring IoC容器注册解析得到的Bean定义之后，发送注册事件
//            getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
//        }
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
//            //String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; "   以 ,; 分割字符串成数组
//            String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
//            //将<Bean>元素中的所有name属性值存放到别名中
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
//
//        //检查<Bean>元素所配置的id或者name的唯一性，containingBean标识<Bean>元素中是否包含子<Bean>元素
//        if (containingBean == null) {
//            //1>查<Bean>元素所配置的id、name或者别名是否重复
//            checkNameUniqueness(beanName, aliases, ele);
//        }
//
//        //2>详细对<Bean>元素中配置的Bean定义进行解析的地方
//        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
//        if (beanDefinition != null) {
//            if (!StringUtils.hasText(beanName)) {
//                try {
//                    if (containingBean != null) {
//                        //3>如果<Bean>元素中没有配置id、别名或者name，且没有包含子<Bean>元素，为解析的Bean生成一个唯一beanName并注册
//                        beanName = BeanDefinitionReaderUtils.generateBeanName(
//                                beanDefinition, this.readerContext.getRegistry(), true);
//                    }else {
//                        //4>如果<Bean>元素中没有配置id、别名或者name，且包含了子<Bean>元素，为解析的Bean使用别名向IoC容器注册
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
//                }
//                catch (Exception ex) {
//                    error(ex.getMessage(), ele);
//                    return null;
//                }
//            }
//            String[] aliasesArray = StringUtils.toStringArray(aliases);
//            //5>创建处理BeanDefinition的 BeanDefinitionHolder
//            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
//        }
//
//        //当解析出错时，返回null
//        return null;
//    }
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
//        //Set<String> usedNames = new HashSet<>()
//        this.usedNames.add(beanName);
//        this.usedNames.addAll(aliases);
//    }
//
//
//    //BeanDefinitionReaderUtils:
//    //在BeanFactory里给BeanDefinition生成一个唯一的beanName
//    public static String generateBeanName(
//            BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
//            throws BeanDefinitionStoreException {
//
//        String generatedBeanName = definition.getBeanClassName();
//        if (generatedBeanName == null) {
//            if (definition.getParentName() != null) {
//                generatedBeanName = definition.getParentName() + "$child";
//            }
//            else if (definition.getFactoryBeanName() != null) {
//                generatedBeanName = definition.getFactoryBeanName() + "$created";
//            }
//        }
//        if (!StringUtils.hasText(generatedBeanName)) {
//            throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
//                    "'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
//        }
//
//        String id = generatedBeanName;
//        if (isInnerBean) {
//            //GENERATED_BEAN_NAME_SEPARATOR = "#"
//            id = generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
//        }
//        else {
//            // Top-level bean: use plain class name with unique suffix if necessary.
//            return uniqueBeanName(generatedBeanName, registry);
//        }
//        return id;
//    }
//
//    //BeanDefinitionReaderUtils:
//    //生成一个唯一的bean id
//    public static String uniqueBeanName(String beanName, BeanDefinitionRegistry registry) {
//        String id = beanName;
//        int counter = -1;
//
//        // Increase counter until the id is unique.
//        while (counter == -1 || registry.containsBeanDefinition(id)) {
//            counter++;
//            //GENERATED_BEAN_NAME_SEPARATOR = "#"
//            id = beanName + GENERATED_BEAN_NAME_SEPARATOR + counter;
//        }
//        return id;
//    }
//
//    //DefaultListableBeanFactory:
//    public boolean containsBeanDefinition(String beanName) {
//        Assert.notNull(beanName, "Bean name must not be null");
//        //Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256)
//        //key = beanName, value = BeanDefinition
//        return this.beanDefinitionMap.containsKey(beanName);
//    }
//
//    //-----------
//
//    //XmlReaderContext:
//    public String generateBeanName(BeanDefinition beanDefinition) {
//        return this.reader.getBeanNameGenerator().generateBeanName(beanDefinition, getRegistry());
//    }
//
//    //AnnotationBeanNameGenerator:
//    //注解驱动获取beanName
//    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
//        if (definition instanceof AnnotatedBeanDefinition) {
//            String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
//            if (StringUtils.hasText(beanName)) {
//                // Explicit bean name found.
//                return beanName;
//            }
//        }
//        // Fallback: generate a unique default bean name.
//        return buildDefaultBeanName(definition, registry);
//    }
//
//    //AnnotationBeanNameGenerator:
//    protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {
//        AnnotationMetadata amd = annotatedDef.getMetadata();
//        Set<String> types = amd.getAnnotationTypes();
//        String beanName = null;
//        for (String type : types) {
//            AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);
//            if (attributes != null && isStereotypeWithNameValue(type, amd.getMetaAnnotationTypes(type), attributes)) {
//                Object value = attributes.get("value");
//                if (value instanceof String) {
//                    String strVal = (String) value;
//                    if (StringUtils.hasLength(strVal)) {
//                        if (beanName != null && !strVal.equals(beanName)) {
//                            throw new IllegalStateException("Stereotype annotations suggest inconsistent " +
//                                    "component names: '" + beanName + "' versus '" + strVal + "'");
//                        }
//                        beanName = strVal;
//                    }
//                }
//            }
//        }
//        return beanName;
//    }
//
//    //AnnotationBeanNameGenerator:
//    protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
//        return buildDefaultBeanName(definition);
//    }
//
//    //AnnotationBeanNameGenerator:
//    protected String buildDefaultBeanName(BeanDefinition definition) {
//        String beanClassName = definition.getBeanClassName();
//        Assert.state(beanClassName != null, "No bean class name set");
//        String shortClassName = ClassUtils.getShortName(beanClassName);
//        return Introspector.decapitalize(shortClassName);
//    }
//
//    //ClassUtils:
//    public static String getShortName(String className) {
//        Assert.hasLength(className, "Class name must not be empty");
//        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR); //PACKAGE_SEPARATOR = '.'
//        int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR); //CGLIB_CLASS_SEPARATOR = "$$"
//        if (nameEndIndex == -1) {
//            nameEndIndex = className.length();
//        }
//        String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
//        //INNER_CLASS_SEPARATOR = '$'
//        shortName = shortName.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
//        return shortName;
//    }
//
//    //Introspector:
//    //将第一个字母小写
//    public static String decapitalize(String name) {
//        if (name == null || name.length() == 0) {
//            return name;
//        }
//        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
//                        Character.isUpperCase(name.charAt(0))){
//            return name;
//        }
//        char[] chars = name.toCharArray();
//        chars[0] = Character.toLowerCase(chars[0]);
//        return new String(chars);
//    }
//
//
//
//
//    //BeanDefinitionHolder:
//    public BeanDefinitionHolder(BeanDefinition beanDefinition, String beanName, @Nullable String[] aliases) {
//        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
//        Assert.notNull(beanName, "Bean name must not be null");
//        this.beanDefinition = beanDefinition; //BeanDefinition beanDefinition
//        this.beanName = beanName; //String beanName
//        this.aliases = aliases; //String[] aliases
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    //详细对<Bean>元素中配置的Bean定义其他属性进行解析，由于上面的方法中已经对Bean的id、name和别名等属性进行了处理，
//    //该方法中主要处理除这三个以外的其他属性数据
//    public AbstractBeanDefinition parseBeanDefinitionElement(
//            Element ele, String beanName, @Nullable BeanDefinition containingBean) {
//
//        //记录解析的<Bean>, ParseState.state ==> LinkedList<Entry> state;
//        this.parseState.push(new BeanEntry(beanName));
//
//        //这里只读取<Bean>元素中配置的class名字，然后载入到BeanDefinition中去
//        //只是记录配置的class名字，不做实例化，对象的实例化在依赖注入时完成
//        String className = null;
//        if (ele.hasAttribute(CLASS_ATTRIBUTE)) { //CLASS_ATTRIBUTE = "class"
//            className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
//        }
//        //如果<Bean>元素中配置了parent属性，则获取parent属性的值
//        String parent = null;
//        if (ele.hasAttribute(PARENT_ATTRIBUTE)) { //PARENT_ATTRIBUTE = "parent"
//            parent = ele.getAttribute(PARENT_ATTRIBUTE);
//        }
//
//        try {
//            //1>根据<Bean>元素配置的class名称和parent属性值创建BeanDefinition为载入Bean定义信息做准备
//            AbstractBeanDefinition bd = createBeanDefinition(className, parent);
//
//            //2>对当前的<Bean>元素中配置的一些属性进行解析和设置，如配置的单态(singleton)属性等
//            parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
//            //为<Bean>元素解析的Bean设置description信息
//            bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
//
//            //3>对<Bean>元素的meta(元信息)属性解析
//            parseMetaElements(ele, bd);
//            //4>对<Bean>元素的lookup-method属性解析
//            parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
//            //5>对<Bean>元素的replaced-method属性解析
//            parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
//
//            //6>解析<Bean>元素的构造方法设置
//            parseConstructorArgElements(ele, bd);
//            //7>解析<Bean>元素的<property>设置
//            parsePropertyElements(ele, bd);
//            //8>解析<Bean>元素的qualifier属性
//            parseQualifierElements(ele, bd);
//
//            //9>为当前解析的Bean设置所需的资源和依赖对象
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
//        //解析<Bean>元素出错时，返回null
//        return null;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    protected AbstractBeanDefinition createBeanDefinition(@Nullable String className, @Nullable String parentName)
//            throws ClassNotFoundException {
//
//        return BeanDefinitionReaderUtils.createBeanDefinition(
//                parentName, className, this.readerContext.getBeanClassLoader());
//    }
//
//    //BeanDefinitionReaderUtils:
//    public static AbstractBeanDefinition createBeanDefinition(
//            @Nullable String parentName, @Nullable String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
//
//        //创建一个通用的GenericBeanDefinition
//        // GenericBeanDefinition extends AbstractBeanDefinition
//        // AbstractBeanDefinition extends BeanMetadataAttributeAccessor implements BeanDefinition, Cloneable
//        GenericBeanDefinition bd = new GenericBeanDefinition();
//        bd.setParentName(parentName);
//        if (className != null) {
//            if (classLoader != null) {
//                bd.setBeanClass(ClassUtils.forName(className, classLoader));
//            }
//            else {
//                bd.setBeanClassName(className);
//            }
//        }
//        return bd;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
//                                                                @Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {
//
//        if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) { //SINGLETON_ATTRIBUTE = "singleton"
//            error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
//        }
//        else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) { //SCOPE_ATTRIBUTE = "scope"
//            bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
//        }
//        else if (containingBean != null) {
//            // Take default from containing bean in case of an inner bean definition.
//            bd.setScope(containingBean.getScope());
//        }
//
//        if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) { //ABSTRACT_ATTRIBUTE = "abstract"
//            bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
//        }
//
//        String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE); //LAZY_INIT_ATTRIBUTE = "lazy-init"
//        if (isDefaultValue(lazyInit)) {
//            lazyInit = this.defaults.getLazyInit();
//        }
//        bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
//
//        String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
//        bd.setAutowireMode(getAutowireMode(autowire));
//
//        if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) { //DEPENDS_ON_ATTRIBUTE = "depends-on"
//            String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
//            bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
//        }
//
//        String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE); //AUTOWIRE_CANDIDATE_ATTRIBUTE = "autowire-candidate"
//        if (isDefaultValue(autowireCandidate)) {
//            String candidatePattern = this.defaults.getAutowireCandidates();
//            if (candidatePattern != null) {
//                String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
//                bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
//            }
//        }
//        else {
//            bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
//        }
//
//        if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) { //PRIMARY_ATTRIBUTE = "primary"
//            bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
//        }
//
//        if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
//            String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
//            bd.setInitMethodName(initMethodName);
//        }
//        else if (this.defaults.getInitMethod() != null) {
//            bd.setInitMethodName(this.defaults.getInitMethod());
//            bd.setEnforceInitMethod(false);
//        }
//
//        if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
//            String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
//            bd.setDestroyMethodName(destroyMethodName);
//        }
//        else if (this.defaults.getDestroyMethod() != null) {
//            bd.setDestroyMethodName(this.defaults.getDestroyMethod());
//            bd.setEnforceDestroyMethod(false);
//        }
//
//        if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) { //FACTORY_METHOD_ATTRIBUTE = "factory-method"
//            bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
//        }
//        if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) { //FACTORY_BEAN_ATTRIBUTE = "factory-bean"
//            bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
//        }
//
//        return bd;
//    }
//
//    //BeanDefinitionParserDelegate:
//    public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
//        NodeList nl = ele.getChildNodes();
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            //解析的元素是meta信息，META_ELEMENT = "meta"
//            if (isCandidateElement(node) && nodeNameEquals(node, META_ELEMENT)) {
//                Element metaElement = (Element) node;
//                String key = metaElement.getAttribute(KEY_ATTRIBUTE); //KEY_ATTRIBUTE = "key"
//                String value = metaElement.getAttribute(VALUE_ATTRIBUTE); //VALUE_ATTRIBUTE = "value"
//                BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
//                attribute.setSource(extractSource(metaElement));
//                attributeAccessor.addMetadataAttribute(attribute);
//            }
//        }
//    }
//
//    //BeanDefinitionParserDelegate:
//    private boolean isCandidateElement(Node node) {
//        return (node instanceof Element && (isDefaultNamespace(node) || !isDefaultNamespace(node.getParentNode())));
//    }
//
//    //BeanDefinitionParserDelegate:
//    public boolean nodeNameEquals(Node node, String desiredName) {
//        return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
//    }
//
//    //BeanDefinitionParserDelegate:
//    public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
//        NodeList nl = beanEle.getChildNodes();
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            //LOOKUP_METHOD_ELEMENT = "lookup-method"
//            if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
//                Element ele = (Element) node;
//                String methodName = ele.getAttribute(NAME_ATTRIBUTE); //NAME_ATTRIBUTE = "name"
//                String beanRef = ele.getAttribute(BEAN_ELEMENT); //BEAN_ELEMENT = "bean"
//                LookupOverride override = new LookupOverride(methodName, beanRef);
//                override.setSource(extractSource(ele));
//                overrides.addOverride(override);
//            }
//        }
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
//        NodeList nl = beanEle.getChildNodes();
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            //REPLACED_METHOD_ELEMENT = "replaced-method"
//            if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
//                Element replacedMethodEle = (Element) node;
//                String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE); //NAME_ATTRIBUTE = "name"
//                String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE); //REPLACER_ATTRIBUTE = "replacer"
//                ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
//                // Look for arg-type match elements, ARG_TYPE_ELEMENT = "arg-type"
//                List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
//                for (Element argTypeEle : argTypeEles) {
//                    String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE); //ARG_TYPE_MATCH_ATTRIBUTE = "match"
//                    match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
//                    if (StringUtils.hasText(match)) {
//                        replaceOverride.addTypeIdentifier(match);
//                    }
//                }
//                replaceOverride.setSource(extractSource(replacedMethodEle));
//                overrides.addOverride(replaceOverride);
//            }
//        }
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
//                }
//                else {
//                    try {
//                        //ParseState.parseState = LinkedList<Entry>
//                        this.parseState.push(new ConstructorArgumentEntry(index));
//                        //解析元素的属性值
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
//                        }else {
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
//    //BeanDefinitionParserDelegate:
//    public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
//        String elementName = (propertyName != null ?
//                "<property> element for property '" + propertyName + "'" :
//                "<constructor-arg> element");
//
//        //获取<property>的所有子元素，只能是其中一种类型:ref,value,list等
//        NodeList nl = ele.getChildNodes();
//        Element subElement = null;
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            //子元素不是description和meta属性
//            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) &&
//                    !nodeNameEquals(node, META_ELEMENT)) {
//                // Child element is what we're looking for.
//                if (subElement != null) {
//                    error(elementName + " must not contain more than one sub-element", ele);
//                }else { //当前<property>元素包含有子元素
//                    subElement = (Element) node;
//                }
//            }
//        }
//
//        //判断property的属性值是ref还是value，不允许既是ref又是value
//        boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE); //REF_ATTRIBUTE = "ref"
//        boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE); //VALUE_ATTRIBUTE = "value"
//        if ((hasRefAttribute && hasValueAttribute) ||
//                ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
//            error(elementName +
//                    " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
//        }
//
//        //如果属性是ref，创建一个ref的数据对象RuntimeBeanReference，这个对象封装了ref信息
//        if (hasRefAttribute) {
//            String refName = ele.getAttribute(REF_ATTRIBUTE);
//            if (!StringUtils.hasText(refName)) {
//                error(elementName + " contains empty 'ref' attribute", ele);
//            }
//            //一个指向运行时所依赖对象的引用
//            RuntimeBeanReference ref = new RuntimeBeanReference(refName);
//            //设置这个ref的数据对象 被 当前的property对象所引用
//            ref.setSource(extractSource(ele));
//            return ref;
//        }
//        //如果属性是value，创建一个value的数据对象TypedStringValue，这个对象封装了value信息
//        else if (hasValueAttribute) {
//            TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
//            //设置这个value数据对象被当前的property对象所引用
//            valueHolder.setSource(extractSource(ele));
//            return valueHolder;
//        }
//        //如果当前<property>元素还有子元素
//        else if (subElement != null) {
//            //解析<property>的子元素
//            return parsePropertySubElement(subElement, bd);
//        }
//        else {
//            //propery属性中既不是ref，也不是value属性，解析出错返回null
//            error(elementName + " must specify a ref or value", ele);
//            return null;
//        }
//    }
//
//    //BeanDefinitionParserDelegate:
//    //解析<property>元素中ref,value或者集合等子元素
//    public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
//        //如果<property>没有使用Spring默认的命名空间，则使用用户自定义的规则解析内嵌元素
//        if (!isDefaultNamespace(ele)) {
//            return parseNestedCustomElement(ele, bd);
//        }
//        //如果子元素是bean，则使用解析<Bean>元素的方法解析
//        else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
//            BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
//            if (nestedBd != null) {
//                nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
//            }
//            return nestedBd;
//        }
//        //如果子元素是ref，ref中只能有以下3个属性：bean、local、parent
//        else if (nodeNameEquals(ele, REF_ELEMENT)) {
//            //获取<property>元素中的bean属性值，引用其他解析的Bean的名称
//            //可以不再同一个Spring配置文件中，具体请参考Spring对ref的配置规则
//            String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
//            boolean toParent = false;
//            if (!StringUtils.hasLength(refName)) {
//                //获取<property>元素中的local属性值，引用同一个Xml文件中配置
//                //的Bean的id，local和ref不同，local只能引用同一个配置文件中的Bean
//                refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
//                toParent = true;
//                if (!StringUtils.hasLength(refName)) {
//                    error("'bean' or 'parent' is required for <ref> element", ele);
//                    return null;
//                }
//            }
//            //没有配置ref的目标属性值
//            if (!StringUtils.hasText(refName)) {
//                error("<ref> element contains empty target attribute", ele);
//                return null;
//            }
//            //创建ref类型数据，指向被引用的对象
//            RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
//            //设置引用类型值是被当前子元素所引用
//            ref.setSource(extractSource(ele));
//            return ref;
//        }
//        //如果子元素是<idref>，使用解析ref元素的方法解析
//        else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
//            return parseIdRefElement(ele);
//        }
//        //如果子元素是<value>，使用解析value元素的方法解析
//        else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
//            return parseValueElement(ele, defaultValueType);
//        }
//        //如果子元素是null，为<property>设置一个封装null值的字符串数据
//        else if (nodeNameEquals(ele, NULL_ELEMENT)) {
//            TypedStringValue nullHolder = new TypedStringValue(null);
//            nullHolder.setSource(extractSource(ele));
//            return nullHolder;
//        }
//        //如果子元素是<array>，使用解析array集合子元素的方法解析
//        else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
//            return parseArrayElement(ele, bd);
//        }
//        //如果子元素是<list>，使用解析list集合子元素的方法解析
//        else if (nodeNameEquals(ele, LIST_ELEMENT)) {
//            return parseListElement(ele, bd);
//        }
//        //如果子元素是<set>，使用解析set集合子元素的方法解析
//        else if (nodeNameEquals(ele, SET_ELEMENT)) {
//            return parseSetElement(ele, bd);
//        }
//        //如果子元素是<map>，使用解析map集合子元素的方法解析
//        else if (nodeNameEquals(ele, MAP_ELEMENT)) {
//            return parseMapElement(ele, bd);
//        }
//        //如果子元素是<props>，使用解析props集合子元素的方法解析
//        else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
//            return parsePropsElement(ele);
//        }
//        //既不是ref，又不是value，也不是集合，则子元素配置错误，返回null
//        else {
//            error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
//            return null;
//        }
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public Object parseIdRefElement(Element ele) {
//        //BEAN_REF_ATTRIBUTE = "bean"
//        String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
//        if (!StringUtils.hasLength(refName)) {
//            error("'bean' is required for <idref> element", ele);
//            return null;
//        }
//        if (!StringUtils.hasText(refName)) {
//            error("<idref> element contains empty target attribute", ele);
//            return null;
//        }
//        RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
//        ref.setSource(extractSource(ele));
//        return ref;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public Object parseValueElement(Element ele, @Nullable String defaultTypeName) {
//        String value = DomUtils.getTextValue(ele);
//        //TYPE_ATTRIBUTE = "type"
//        String specifiedTypeName = ele.getAttribute(TYPE_ATTRIBUTE);
//        String typeName = specifiedTypeName;
//        if (!StringUtils.hasText(typeName)) {
//            typeName = defaultTypeName;
//        }
//        try {
//            TypedStringValue typedValue = buildTypedStringValue(value, typeName);
//            typedValue.setSource(extractSource(ele));
//            typedValue.setSpecifiedTypeName(specifiedTypeName);
//            return typedValue;
//        }
//        catch (ClassNotFoundException ex) {
//            error("Type class [" + typeName + "] not found for <value> element", ele, ex);
//            return value;
//        }
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public Object parseArrayElement(Element arrayEle, @Nullable BeanDefinition bd) {
//        //VALUE_TYPE_ATTRIBUTE = "value-type"
//        String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//        NodeList nl = arrayEle.getChildNodes();
//        ManagedArray target = new ManagedArray(elementType, nl.getLength());
//        target.setSource(extractSource(arrayEle));
//        target.setElementTypeName(elementType);
//        target.setMergeEnabled(parseMergeAttribute(arrayEle));
//        parseCollectionElements(nl, target, bd, elementType);
//        return target;
//    }
//
//    //BeanDefinitionParserDelegate:
//    protected void parseCollectionElements(
//            NodeList elementNodes, Collection<Object> target, @Nullable BeanDefinition bd, String defaultElementType) {
//        //遍历集合所有节点
//        for (int i = 0; i < elementNodes.getLength(); i++) {
//            Node node = elementNodes.item(i);
//            //节点不是description节点
//            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
//                //将解析的元素加入集合中，递归调用下一个子元素
//                target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
//            }
//        }
//    }
//
//    //BeanDefinitionParserDelegate:
//    public List<Object> parseListElement(Element collectionEle, @Nullable BeanDefinition bd) {
//        String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//        NodeList nl = collectionEle.getChildNodes();
//        ManagedList<Object> target = new ManagedList<>(nl.getLength());
//        target.setSource(extractSource(collectionEle));
//        target.setElementTypeName(defaultElementType);
//        target.setMergeEnabled(parseMergeAttribute(collectionEle));
//        parseCollectionElements(nl, target, bd, defaultElementType);
//        return target;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public Set<Object> parseSetElement(Element collectionEle, @Nullable BeanDefinition bd) {
//        String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//        NodeList nl = collectionEle.getChildNodes();
//        ManagedSet<Object> target = new ManagedSet<>(nl.getLength());
//        target.setSource(extractSource(collectionEle));
//        target.setElementTypeName(defaultElementType);
//        target.setMergeEnabled(parseMergeAttribute(collectionEle));
//        parseCollectionElements(nl, target, bd, defaultElementType);
//        return target;
//    }
//
//    //BeanDefinitionParserDelegate:
//    public Map<Object, Object> parseMapElement(Element mapEle, @Nullable BeanDefinition bd) {
//        String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE); //KEY_TYPE_ATTRIBUTE = "key-type"
//        String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE); //VALUE_TYPE_ATTRIBUTE = "value-type"
//
//        List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT); //ENTRY_ELEMENT = "entry"
//        ManagedMap<Object, Object> map = new ManagedMap<>(entryEles.size());
//        map.setSource(extractSource(mapEle));
//        map.setKeyTypeName(defaultKeyType);
//        map.setValueTypeName(defaultValueType);
//        map.setMergeEnabled(parseMergeAttribute(mapEle));
//
//        for (Element entryEle : entryEles) {
//            // Should only have one value child element: ref, value, list, etc.
//            // Optionally, there might be a key child element.
//            NodeList entrySubNodes = entryEle.getChildNodes();
//            Element keyEle = null;
//            Element valueEle = null;
//            for (int j = 0; j < entrySubNodes.getLength(); j++) {
//                Node node = entrySubNodes.item(j);
//                if (node instanceof Element) {
//                    Element candidateEle = (Element) node;
//                    //KEY_ELEMENT = "key"
//                    if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
//                        if (keyEle != null) {
//                            error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
//                        }
//                        else {
//                            keyEle = candidateEle;
//                        }
//                    }
//                    else {
//                        // Child element is what we're looking for.
//                        //DESCRIPTION_ELEMENT = "description"
//                        if (nodeNameEquals(candidateEle, DESCRIPTION_ELEMENT)) {
//                            // the element is a <description> -> ignore it
//                        }else if (valueEle != null) {
//                            error("<entry> element must not contain more than one value sub-element", entryEle);
//                        } else {
//                            valueEle = candidateEle;
//                        }
//                    }
//                }
//            }
//
//            // Extract key from attribute or sub-element.
//            Object key = null;
//            boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE); //KEY_ATTRIBUTE = "key"
//            boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE); //KEY_REF_ATTRIBUTE = "key-ref"
//            if ((hasKeyAttribute && hasKeyRefAttribute) ||
//                    (hasKeyAttribute || hasKeyRefAttribute) && keyEle != null) {
//                error("<entry> element is only allowed to contain either " +
//                        "a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
//            }
//            if (hasKeyAttribute) {
//                key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
//            }
//            else if (hasKeyRefAttribute) {
//                String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
//                if (!StringUtils.hasText(refName)) {
//                    error("<entry> element contains empty 'key-ref' attribute", entryEle);
//                }
//                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
//                ref.setSource(extractSource(entryEle));
//                key = ref;
//            }
//            else if (keyEle != null) {
//                key = parseKeyElement(keyEle, bd, defaultKeyType);
//            }
//            else {
//                error("<entry> element must specify a key", entryEle);
//            }
//
//            // Extract value from attribute or sub-element.
//            Object value = null;
//            boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE); //VALUE_ATTRIBUTE = "value"
//            boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE); //VALUE_REF_ATTRIBUTE = "value-ref"
//            boolean hasValueTypeAttribute = entryEle.hasAttribute(VALUE_TYPE_ATTRIBUTE); //VALUE_TYPE_ATTRIBUTE = "value-type"
//            if ((hasValueAttribute && hasValueRefAttribute) ||
//                    (hasValueAttribute || hasValueRefAttribute) && valueEle != null) {
//                error("<entry> element is only allowed to contain either " +
//                        "'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
//            }
//            if ((hasValueTypeAttribute && hasValueRefAttribute) ||
//                    (hasValueTypeAttribute && !hasValueAttribute) ||
//                    (hasValueTypeAttribute && valueEle != null)) {
//                error("<entry> element is only allowed to contain a 'value-type' " +
//                        "attribute when it has a 'value' attribute", entryEle);
//            }
//            if (hasValueAttribute) {
//                String valueType = entryEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
//                if (!StringUtils.hasText(valueType)) {
//                    valueType = defaultValueType;
//                }
//                value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), valueType, entryEle);
//            }
//            else if (hasValueRefAttribute) {
//                String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
//                if (!StringUtils.hasText(refName)) {
//                    error("<entry> element contains empty 'value-ref' attribute", entryEle);
//                }
//                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
//                ref.setSource(extractSource(entryEle));
//                value = ref;
//            }
//            else if (valueEle != null) {
//                value = parsePropertySubElement(valueEle, bd, defaultValueType);
//            }else {
//                error("<entry> element must specify a value", entryEle);
//            }
//
//            // Add final key and value to the Map.
//            map.put(key, value);
//        }
//
//        return map;
//    }
//
//    //BeanDefinitionParserDelegate:
//    public Properties parsePropsElement(Element propsEle) {
//        ManagedProperties props = new ManagedProperties();
//        props.setSource(extractSource(propsEle));
//        props.setMergeEnabled(parseMergeAttribute(propsEle));
//
//        //PROP_ELEMENT = "prop"
//        List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
//        for (Element propEle : propEles) {
//            //KEY_ATTRIBUTE = "key"
//            String key = propEle.getAttribute(KEY_ATTRIBUTE);
//            // Trim the text value to avoid unwanted whitespace
//            // caused by typical XML formatting.
//            String value = DomUtils.getTextValue(propEle).trim();
//            TypedStringValue keyHolder = new TypedStringValue(key);
//            keyHolder.setSource(extractSource(propEle));
//            TypedStringValue valueHolder = new TypedStringValue(value);
//            valueHolder.setSource(extractSource(propEle));
//            props.put(keyHolder, valueHolder);
//        }
//
//        return props;
//    }
//
//
//
//
//
//
//    //BeanDefinitionParserDelegate:
//    public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
//        NodeList nl = beanEle.getChildNodes();
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            //PROPERTY_ELEMENT = "property"
//            if (isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)) {
//                parsePropertyElement((Element) node, bd);
//            }
//        }
//    }
//
//    //BeanDefinitionParserDelegate:
//    public void parsePropertyElement(Element ele, BeanDefinition bd) {
//        String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
//        if (!StringUtils.hasLength(propertyName)) {
//            error("Tag 'property' must have a 'name' attribute", ele);
//            return;
//        }
//        this.parseState.push(new PropertyEntry(propertyName));
//        try {
//            if (bd.getPropertyValues().contains(propertyName)) {
//                error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
//                return;
//            }
//            Object val = parsePropertyValue(ele, bd, propertyName);
//            PropertyValue pv = new PropertyValue(propertyName, val);
//            parseMetaElements(ele, pv);
//            pv.setSource(extractSource(ele));
//            bd.getPropertyValues().addPropertyValue(pv);
//        }
//        finally {
//            this.parseState.pop();
//        }
//    }
//
//
//
//    //BeanDefinitionParserDelegate:
//    public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
//        NodeList nl = beanEle.getChildNodes();
//        for (int i = 0; i < nl.getLength(); i++) {
//            Node node = nl.item(i);
//            if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ELEMENT)) {
//                parseQualifierElement((Element) node, bd);
//            }
//        }
//    }
//
//    //BeanDefinitionParserDelegate:
//    //解析qualifier元素
//    public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
//        String typeName = ele.getAttribute(TYPE_ATTRIBUTE); //TYPE_ATTRIBUTE = "type"
//        if (!StringUtils.hasLength(typeName)) {
//            error("Tag 'qualifier' must have a 'type' attribute", ele);
//            return;
//        }
//        this.parseState.push(new QualifierEntry(typeName));
//        try {
//            AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
//            qualifier.setSource(extractSource(ele));
//            String value = ele.getAttribute(VALUE_ATTRIBUTE); // VALUE_ATTRIBUTE = "value"
//            if (StringUtils.hasLength(value)) {
//                //VALUE_KEY = "value"
//                qualifier.setAttribute(AutowireCandidateQualifier.VALUE_KEY, value);
//            }
//            NodeList nl = ele.getChildNodes();
//            for (int i = 0; i < nl.getLength(); i++) {
//                Node node = nl.item(i);
//                //QUALIFIER_ATTRIBUTE_ELEMENT = "attribute"
//                if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ATTRIBUTE_ELEMENT)) {
//                    Element attributeEle = (Element) node;
//                    String attributeName = attributeEle.getAttribute(KEY_ATTRIBUTE); //KEY_ATTRIBUTE = "key"
//                    String attributeValue = attributeEle.getAttribute(VALUE_ATTRIBUTE); //VALUE_ATTRIBUTE = "value"
//                    if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
//                        BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
//                        attribute.setSource(extractSource(attributeEle));
//                        qualifier.addMetadataAttribute(attribute);
//                    }else {
//                        error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
//                        return;
//                    }
//                }
//            }
//            bd.addQualifier(qualifier);
//        }finally {
//            this.parseState.pop();
//        }
//    }
//
//
//
//
//
//
//    //BeanDefinitionParserDelegate:
//    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder originalDef) {
//        return decorateBeanDefinitionIfRequired(ele, originalDef, null);
//    }
//
//    //BeanDefinitionParserDelegate:
//    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(
//            Element ele, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {
//
//        BeanDefinitionHolder finalDefinition = originalDef;
//
//        // Decorate based on custom attributes first.
//        NamedNodeMap attributes = ele.getAttributes();
//        for (int i = 0; i < attributes.getLength(); i++) {
//            Node node = attributes.item(i);
//            //获取处理BeanDefinition的holder
//            finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
//        }
//
//        // Decorate based on custom nested elements.
//        NodeList children = ele.getChildNodes();
//        for (int i = 0; i < children.getLength(); i++) {
//            Node node = children.item(i);
//            if (node.getNodeType() == Node.ELEMENT_NODE) {
//                finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
//            }
//        }
//        return finalDefinition;
//    }
//
//
//    //BeanDefinitionParserDelegate:
//    public BeanDefinitionHolder decorateIfRequired(
//            Node node, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {
//
//        String namespaceUri = getNamespaceURI(node);
//        if (namespaceUri != null && !isDefaultNamespace(namespaceUri)) {
//            NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
//            if (handler != null) {
//                //
//                BeanDefinitionHolder decorated =
//                        handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
//                if (decorated != null) {
//                    return decorated;
//                }
//            }else if (namespaceUri.startsWith("http://www.springframework.org/schema/")) {
//                error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
//            }else {
//                // A custom namespace, not to be handled by Spring - maybe "xml:...".
//                if (logger.isDebugEnabled()) {
//                    logger.debug("No Spring NamespaceHandler found for XML schema namespace [" + namespaceUri + "]");
//                }
//            }
//        }
//        return originalDef;
//    }
//
//
//    //NamespaceHandlerSupport:
//    public BeanDefinitionHolder decorate(
//            Node node, BeanDefinitionHolder definition, ParserContext parserContext) {
//
//        BeanDefinitionDecorator decorator = findDecoratorForNode(node, parserContext);
//        return (decorator != null ? decorator.decorate(node, definition, parserContext) : null);
//    }
//
//    //NamespaceHandlerSupport:
//    private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
//        //支持自定义内嵌标签的接口
//        BeanDefinitionDecorator decorator = null;
//        String localName = parserContext.getDelegate().getLocalName(node);
//        if (node instanceof Element) {
//            //Map<String, BeanDefinitionDecorator> decorators = new HashMap<>()
//            decorator = this.decorators.get(localName);
//        }
//        else if (node instanceof Attr) {
//            //Map<String, BeanDefinitionDecorator> attributeDecorators = new HashMap<>()
//            decorator = this.attributeDecorators.get(localName);
//        }
//        else {
//            parserContext.getReaderContext().fatal(
//                    "Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
//        }
//        if (decorator == null) {
//            parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " +
//                    (node instanceof Element ? "element" : "attribute") + " [" + localName + "]", node);
//        }
//        return decorator;
//    }
//
//
//    //ConstructorArgumentValues:
//    //在构造函数参数列表中查找与给定索引对应的参数值，或根据类型进行常规匹配的参数值
//    public ValueHolder getArgumentValue(int index, @Nullable Class<?> requiredType, @Nullable String requiredName,
//                                        @Nullable Set<ValueHolder> usedValueHolders) {
//        Assert.isTrue(index >= 0, "Index must not be negative");
//        ValueHolder valueHolder = getIndexedArgumentValue(index, requiredType, requiredName);
//        if (valueHolder == null) {
//            valueHolder = getGenericArgumentValue(requiredType, requiredName, usedValueHolders);
//        }
//        return valueHolder;
//    }
//
//    //ConstructorArgumentValues:
//    public ValueHolder getIndexedArgumentValue(int index, @Nullable Class<?> requiredType, @Nullable String requiredName) {
//        Assert.isTrue(index >= 0, "Index must not be negative");
//        //Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>()
//        ValueHolder valueHolder = this.indexedArgumentValues.get(index);
//        if (valueHolder != null &&
//                (valueHolder.getType() == null ||
//                        (requiredType != null && ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) &&
//                (valueHolder.getName() == null || "".equals(requiredName) ||
//                        (requiredName != null && requiredName.equals(valueHolder.getName())))) {
//            return valueHolder;
//        }
//        return null;
//    }
//
//    //ClassUtils:
//    public static boolean matchesTypeName(Class<?> clazz, @Nullable String typeName) {
//        return (typeName != null &&
//                (typeName.equals(clazz.getTypeName()) || typeName.equals(clazz.getSimpleName())));
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
//
//
//
//
//    //ConstructorArgumentValues:
//    //寻找下一个匹配给定类型的泛型参数值，忽略当前解析过程中已经使用的参数值
//    public ValueHolder getGenericArgumentValue(@Nullable Class<?> requiredType, @Nullable String requiredName,
//                                               @Nullable Set<ValueHolder> usedValueHolders) {
//        for (ValueHolder valueHolder : this.genericArgumentValues) {
//            if (usedValueHolders != null && usedValueHolders.contains(valueHolder)) {
//                continue;
//            }
//            if (valueHolder.getName() != null && !"".equals(requiredName) &&
//                    (requiredName == null || !valueHolder.getName().equals(requiredName))) {
//                continue;
//            }
//            if (valueHolder.getType() != null &&
//                    (requiredType == null || !ClassUtils.matchesTypeName(requiredType, valueHolder.getType()))) {
//                continue;
//            }
//            if (requiredType != null && valueHolder.getType() == null && valueHolder.getName() == null &&
//                    !ClassUtils.isAssignableValue(requiredType, valueHolder.getValue())) {
//                continue;
//            }
//            return valueHolder;
//        }
//        return null;
//    }
//
//
//
//
//
//
//
//    //ConstructorResolver:
//    //用于解析指定参数的模板方法，该参数应该是自动生成的
//    protected Object resolveAutowiredArgument(MethodParameter param, String beanName,
//                                              @Nullable Set<String> autowiredBeanNames, TypeConverter typeConverter, boolean fallback) {
//
//        Class<?> paramType = param.getParameterType();
//        if (InjectionPoint.class.isAssignableFrom(paramType)) {
//            //NamedThreadLocal<InjectionPoint> currentInjectionPoint = new NamedThreadLocal<>("Current injection point");
//            InjectionPoint injectionPoint = currentInjectionPoint.get();
//            if (injectionPoint == null) {
//                throw new IllegalStateException("No current InjectionPoint available for " + param);
//            }
//            return injectionPoint;
//        }
//        try {
//            //解析参数属性的依赖
//            return this.beanFactory.resolveDependency(
//                    new DependencyDescriptor(param, true), beanName, autowiredBeanNames, typeConverter);
//        }catch (NoUniqueBeanDefinitionException ex) {
//            throw ex;
//        }catch (NoSuchBeanDefinitionException ex) {
//            if (fallback) {
//                //让我们返回一个空数组/集合
//                if (paramType.isArray()) {
//                    return Array.newInstance(paramType.getComponentType(), 0);
//                }else if (CollectionFactory.isApproximableCollectionType(paramType)) {
//                    return CollectionFactory.createCollection(paramType, 0);
//                }else if (CollectionFactory.isApproximableMapType(paramType)) {
//                    return CollectionFactory.createMap(paramType, 0);
//                }
//            }
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
//    //ConstructorArgumentValues:
//    //在构造函数参数列表中为给定的索引添加一个参数值
//    public void addIndexedArgumentValue(int index, ValueHolder newValue) {
//        Assert.isTrue(index >= 0, "Index must not be negative");
//        Assert.notNull(newValue, "ValueHolder must not be null");
//        addOrMergeIndexedArgumentValue(index, newValue);
//    }
//
//    //ConstructorArgumentValues:
//    private void addOrMergeIndexedArgumentValue(Integer key, ValueHolder newValue) {
//        //Map<Integer, ValueHolder> indexedArgumentValues = new LinkedHashMap<>()
//        ValueHolder currentValue = this.indexedArgumentValues.get(key);
//        if (currentValue != null && newValue.getValue() instanceof Mergeable) {
//            Mergeable mergeable = (Mergeable) newValue.getValue();
//            if (mergeable.isMergeEnabled()) {
//                newValue.setValue(mergeable.merge(currentValue.getValue()));
//            }
//        }
//        this.indexedArgumentValues.put(key, newValue);
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
//
//
//
//    //BeanDefinitionValueResolver:
//    //解析属性值，对注入类型进行转换
//    public Object resolveValueIfNecessary(Object argName, @Nullable Object value) {
//        //对引用类型的属性进行解析
//        if (value instanceof RuntimeBeanReference) {
//            RuntimeBeanReference ref = (RuntimeBeanReference) value;
//            //调用引用类型属性的解析方法
//            return resolveReference(argName, ref);
//        }
//        //对属性值是引用容器中另一个Bean名称的解析
//        else if (value instanceof RuntimeBeanNameReference) {
//            String refName = ((RuntimeBeanNameReference) value).getBeanName();
//            refName = String.valueOf(doEvaluate(refName));
//            if (!this.beanFactory.containsBean(refName)) {
//                throw new BeanDefinitionStoreException(
//                        "Invalid bean name '" + refName + "' in bean reference for " + argName);
//            }
//            return refName;
//        }
//        //对Bean类型属性的解析，主要是Bean中的内部类
//        else if (value instanceof BeanDefinitionHolder) {
//            // Resolve BeanDefinitionHolder: contains BeanDefinition with name and aliases.
//            BeanDefinitionHolder bdHolder = (BeanDefinitionHolder) value;
//            return resolveInnerBean(argName, bdHolder.getBeanName(), bdHolder.getBeanDefinition());
//        }
//        else if (value instanceof BeanDefinition) {
//            // Resolve plain BeanDefinition, without contained name: use dummy name.
//            BeanDefinition bd = (BeanDefinition) value;
//            String innerBeanName = "(inner bean)" + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR +
//                    ObjectUtils.getIdentityHexString(bd);
//            return resolveInnerBean(argName, innerBeanName, bd);
//        }
//        //对依赖bean类型属性的解析
//        else if (value instanceof DependencyDescriptor) {
//            Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
//            Object result = this.beanFactory.resolveDependency(
//                    (DependencyDescriptor) value, this.beanName, autowiredBeanNames, this.typeConverter);
//            for (String autowiredBeanName : autowiredBeanNames) {
//                if (this.beanFactory.containsBean(autowiredBeanName)) {
//                    this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
//                }
//            }
//            return result;
//        }
//        //对集合数组类型的属性解析
//        else if (value instanceof ManagedArray) {
//            ManagedArray array = (ManagedArray) value;
//            //获取数组的类型
//            Class<?> elementType = array.resolvedElementType;
//            if (elementType == null) {
//                //获取数组元素的类型
//                String elementTypeName = array.getElementTypeName();
//                if (StringUtils.hasText(elementTypeName)) {
//                    try {
//                        //使用反射机制创建指定类型的对象
//                        elementType = ClassUtils.forName(elementTypeName, this.beanFactory.getBeanClassLoader());
//                        array.resolvedElementType = elementType;
//                    }catch (Throwable ex) {
//                        throw new BeanCreationException(
//                                this.beanDefinition.getResourceDescription(), this.beanName,
//                                "Error resolving array type for " + argName, ex);
//                    }
//                }else {
//                    //没有获取到数组的类型，也没有获取到数组元素的类型，则直接设置数组的类型为Object
//                    elementType = Object.class;
//                }
//            }
//            //创建指定类型的数组
//            return resolveManagedArray(argName, (List<?>) value, elementType);
//        }
//        //解析list类型的属性值
//        else if (value instanceof ManagedList) {
//            // May need to resolve contained runtime references.
//            return resolveManagedList(argName, (List<?>) value);
//        }
//        //解析set类型的属性值
//        else if (value instanceof ManagedSet) {
//            //可能需要解析包含的运行时引用
//            return resolveManagedSet(argName, (Set<?>) value);
//        }
//        //解析map类型的属性值
//        else if (value instanceof ManagedMap) {
//            // May need to resolve contained runtime references.
//            return resolveManagedMap(argName, (Map<?, ?>) value);
//        }
//        //解析props类型的属性值，props其实就是key和value均为字符串的map
//        else if (value instanceof ManagedProperties) {
//            Properties original = (Properties) value;
//            //创建一个拷贝，用于作为解析后的返回值
//            Properties copy = new Properties();
//            original.forEach((propKey, propValue) -> {
//                if (propKey instanceof TypedStringValue) {
//                    propKey = evaluate((TypedStringValue) propKey);
//                }
//                if (propValue instanceof TypedStringValue) {
//                    propValue = evaluate((TypedStringValue) propValue);
//                }
//                if (propKey == null || propValue == null) {
//                    throw new BeanCreationException(
//                            this.beanDefinition.getResourceDescription(), this.beanName,
//                            "Error converting Properties key/value pair for " + argName + ": resolved to null");
//                }
//                copy.put(propKey, propValue);
//            });
//            return copy;
//        }
//        //解析字符串类型的属性值
//        else if (value instanceof TypedStringValue) {
//            // Convert value to target type here.
//            TypedStringValue typedStringValue = (TypedStringValue) value;
//            Object valueObject = evaluate(typedStringValue);
//            try {
//                //获取属性的目标类型
//                Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
//                if (resolvedTargetType != null) {
//                    //对目标类型的属性进行解析，递归调用
//                    return this.typeConverter.convertIfNecessary(valueObject, resolvedTargetType);
//                }else {
//                    //没有获取到属性的目标对象，则按Object类型返回
//                    return valueObject;
//                }
//            }catch (Throwable ex) {
//                throw new BeanCreationException(
//                        this.beanDefinition.getResourceDescription(), this.beanName,
//                        "Error converting typed String value for " + argName, ex);
//            }
//        }else if (value instanceof NullBean) {
//            return null;
//        }else {
//            return evaluate(value);
//        }
//    }
//
//
//
//
//
//
//    //ConstructorArgumentValues:
//    //添加一个由类型或名称匹配的泛型参数值(如果可用)
//    public void addGenericArgumentValue(ValueHolder newValue) {
//        Assert.notNull(newValue, "ValueHolder must not be null");
//        //List<ValueHolder> genericArgumentValues = new ArrayList<>()
//        if (!this.genericArgumentValues.contains(newValue)) {
//            addOrMergeGenericArgumentValue(newValue);
//        }
//    }
//
//    //ConstructorArgumentValues:
//    private void addOrMergeGenericArgumentValue(ValueHolder newValue) {
//        if (newValue.getName() != null) {
//            //List<ValueHolder> genericArgumentValues = new ArrayList<>()
//            for (Iterator<ValueHolder> it = this.genericArgumentValues.iterator(); it.hasNext();) {
//                ValueHolder currentValue = it.next();
//                if (newValue.getName().equals(currentValue.getName())) {
//                    if (newValue.getValue() instanceof Mergeable) {
//                        Mergeable mergeable = (Mergeable) newValue.getValue();
//                        if (mergeable.isMergeEnabled()) {
//                            newValue.setValue(mergeable.merge(currentValue.getValue()));
//                        }
//                    }
//                    it.remove();
//                }
//            }
//        }
//        this.genericArgumentValues.add(newValue);
//    }
//
//
//
//
//
//
//
//
//    //BeanDefinitionValueResolver:
//    //解析引用类型的属性值, 对工厂中另一个bean的引用
//    private Object resolveReference(Object argName, RuntimeBeanReference ref) {
//        try {
//            Object bean;
//            //获取引用的Bean的类型
//            Class<?> beanType = ref.getBeanType();
//            //如果引用的对象在父类容器中，则从父类容器中获取指定的引用对象
//            if (ref.isToParent()) {
//                BeanFactory parent = this.beanFactory.getParentBeanFactory();
//                if (parent == null) {
//                    throw new BeanCreationException(
//                            this.beanDefinition.getResourceDescription(), this.beanName,
//                            "Cannot resolve reference to bean " + ref +
//                                    " in parent factory: no parent factory available");
//                }
//                if (beanType != null) {
//                    bean = parent.getBean(beanType);
//                }else {
//                    bean = parent.getBean(String.valueOf(doEvaluate(ref.getBeanName())));
//                }
//            }else {
//                //从当前的容器中获取指定的引用Bean对象，如果指定的Bean没有被实例化,
//                //则会递归触发引用Bean的初始化和依赖注入
//                String resolvedName;
//                if (beanType != null) {
//                    NamedBeanHolder<?> namedBean = this.beanFactory.resolveNamedBean(beanType);
//                    bean = namedBean.getBeanInstance();
//                    resolvedName = namedBean.getBeanName();
//                }else {
//                    resolvedName = String.valueOf(doEvaluate(ref.getBeanName()));
//                    bean = this.beanFactory.getBean(resolvedName);
//                }
//                //注册当前实例化对象的依赖引用对象
//                this.beanFactory.registerDependentBean(resolvedName, this.beanName);
//            }
//            if (bean instanceof NullBean) {
//                bean = null;
//            }
//            return bean;
//        }catch (BeansException ex) {
//            throw new BeanCreationException(
//                    this.beanDefinition.getResourceDescription(), this.beanName,
//                    "Cannot resolve reference to bean '" + ref.getBeanName() + "' while setting " + argName, ex);
//        }
//    }
//
//
//
//
//
//
//
//    //BeanDefinitionValueResolver:
//    //如果需要，将给定的字符串值解析为表达式
//    private Object doEvaluate(@Nullable String value) {
//        return this.beanFactory.evaluateBeanDefinitionString(value, this.beanDefinition);
//    }
//
//    //AbstractBeanFactory:
//    //计算bean定义中包含的给定字符串，可能将其解析为表达式。
//    protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
//        if (this.beanExpressionResolver == null) {
//            return value;
//        }
//
//        Scope scope = null;
//        if (beanDefinition != null) {
//            String scopeName = beanDefinition.getScope();
//            if (scopeName != null) {
//                scope = getRegisteredScope(scopeName);
//            }
//        }
//        return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
//    }
//
//
//
//
//
//
//
//    //BeanDefinitionValueResolver:
//    //解析内部bean
//    private Object resolveInnerBean(Object argName, String innerBeanName, BeanDefinition innerBd) {
//        RootBeanDefinition mbd = null;
//        try {
//            mbd = this.beanFactory.getMergedBeanDefinition(innerBeanName, innerBd, this.beanDefinition);
//            //检查给定的bean名称是否惟一。如果还不是唯一的，增加计数器直到名称是唯一的
//            String actualInnerBeanName = innerBeanName;
//            if (mbd.isSingleton()) {
//                actualInnerBeanName = adaptInnerBeanName(innerBeanName);
//            }
//            //在两个bean之间注册一个包容关系，例如在一个内部bean和它包含的外部bean之间
//            this.beanFactory.registerContainedBean(actualInnerBeanName, this.beanName);
//            //保证内部bean所依赖的bean必须已经初始化
//            String[] dependsOn = mbd.getDependsOn();
//            if (dependsOn != null) {
//                for (String dependsOnBean : dependsOn) {
//                    this.beanFactory.registerDependentBean(dependsOnBean, actualInnerBeanName);
//                    this.beanFactory.getBean(dependsOnBean);
//                }
//            }
//            //实际开始创建内部bean实例
//            Object innerBean = this.beanFactory.createBean(actualInnerBeanName, mbd, null);
//            if (innerBean instanceof FactoryBean) {
//                boolean synthetic = mbd.isSynthetic();
//                innerBean = this.beanFactory.getObjectFromFactoryBean(
//                        (FactoryBean<?>) innerBean, actualInnerBeanName, !synthetic);
//            }
//            if (innerBean instanceof NullBean) {
//                innerBean = null;
//            }
//            return innerBean;
//        }catch (BeansException ex) {
//            throw new BeanCreationException(
//                    this.beanDefinition.getResourceDescription(), this.beanName,
//                    "Cannot create inner bean '" + innerBeanName + "' " +
//                            (mbd != null && mbd.getBeanClassName() != null ? "of type [" + mbd.getBeanClassName() + "] " : "") +
//                            "while setting " + argName, ex);
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
//    //BeanDefinitionValueResolver:
//    //解析数组类型的属性值
//    private Object resolveManagedArray(Object argName, List<?> ml, Class<?> elementType) {
//        Object resolved = Array.newInstance(elementType, ml.size());
//        for (int i = 0; i < ml.size(); i++) {
//            Array.set(resolved, i, resolveValueIfNecessary(new BeanDefinitionValueResolver.KeyedArgName(argName, i), ml.get(i)));
//        }
//        return resolved;
//    }
//
//    //BeanDefinitionValueResolver:
//    //解析list类型的属性值
//    private List<?> resolveManagedList(Object argName, List<?> ml) {
//        List<Object> resolved = new ArrayList<>(ml.size());
//        for (int i = 0; i < ml.size(); i++) {
//            resolved.add(resolveValueIfNecessary(new BeanDefinitionValueResolver.KeyedArgName(argName, i), ml.get(i)));
//        }
//        return resolved;
//    }
//
//    //BeanDefinitionValueResolver:
//    //解析set类型的属性值
//    private Set<?> resolveManagedSet(Object argName, Set<?> ms) {
//        Set<Object> resolved = new LinkedHashSet<>(ms.size());
//        int i = 0;
//        for (Object m : ms) {
//            resolved.add(resolveValueIfNecessary(new BeanDefinitionValueResolver.KeyedArgName(argName, i), m));
//            i++;
//        }
//        return resolved;
//    }
//
//    //BeanDefinitionValueResolver:
//    //解析map类型的属性值
//    private Map<?, ?> resolveManagedMap(Object argName, Map<?, ?> mm) {
//        Map<Object, Object> resolved = new LinkedHashMap<>(mm.size());
//        mm.forEach((key, value) -> {
//            Object resolvedKey = resolveValueIfNecessary(argName, key);
//            Object resolvedValue = resolveValueIfNecessary(new BeanDefinitionValueResolver.KeyedArgName(argName, key), value);
//            resolved.put(resolvedKey, resolvedValue);
//        });
//        return resolved;
//    }
//
//
//    //BeanDefinitionValueResolver:
//    //解析给定TypedStringValue为目标类型
//    protected Class<?> resolveTargetType(TypedStringValue value) throws ClassNotFoundException {
//        if (value.hasTargetType()) {
//            return value.getTargetType();
//        }
//        return value.resolveTargetType(this.beanFactory.getBeanClassLoader());
//    }
//
//    //BeanDefinitionValueResolver:
//    //确定要转换为的类型，必要时从指定的类名解析它。在使用已解析的目标类型调用时，还将从其名称重新加载指定的类
//    public Class<?> resolveTargetType(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
//        String typeName = getTargetTypeName();
//        if (typeName == null) {
//            return null;
//        }
//        Class<?> resolvedClass = ClassUtils.forName(typeName, classLoader);
//        this.targetType = resolvedClass;
//        return resolvedClass;
//    }
//
//
//
//    //AbstractAutowireCapableBeanFactory:
//    //“自动装配构造函数”(按类型提供构造函数参数)行为。
//    //如果指定了显式构造函数参数值，将所有剩余的参数与bean工厂中的bean进行匹配。这对应于构造函数注入
//    protected BeanWrapper autowireConstructor(
//            String beanName, RootBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {
//        return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
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
//    //ConstructorResolver:
//    //“自动装配构造函数”(按类型提供构造函数参数)行为。如果指定了显式构造函数参数值，将所有剩余参数与bean工厂中的bean匹配，
//    //也可以应用。这与构造函数注入相对应:在这种模式下，Spring bean工厂能够承载期望基于构造函数的依赖项解析的组件
//    public BeanWrapper autowireConstructor(String beanName, RootBeanDefinition mbd,
//                                           @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {
//
//        BeanWrapperImpl bw = new BeanWrapperImpl();
//        this.beanFactory.initBeanWrapper(bw);
//
//        Constructor<?> constructorToUse = null;
//        ConstructorResolver.ArgumentsHolder argsHolderToUse = null;
//        Object[] argsToUse = null;
//
//        if (explicitArgs != null) {
//            argsToUse = explicitArgs;
//        }else {
//            Object[] argsToResolve = null;
//            synchronized (mbd.constructorArgumentLock) {
//                constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
//                if (constructorToUse != null && mbd.constructorArgumentsResolved) {
//                    //找到缓存的构造函数
//                    argsToUse = mbd.resolvedConstructorArguments;
//                    if (argsToUse == null) {
//                        argsToResolve = mbd.preparedConstructorArguments;
//                    }
//                }
//            }
//            if (argsToResolve != null) {
//                argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve, true);
//            }
//        }
//
//        if (constructorToUse == null || argsToUse == null) {
//            //以指定的构造函数为例(如果有)
//            Constructor<?>[] candidates = chosenCtors;
//            if (candidates == null) {
//                Class<?> beanClass = mbd.getBeanClass();
//                try {
//                    candidates = (mbd.isNonPublicAccessAllowed() ?
//                            beanClass.getDeclaredConstructors() : beanClass.getConstructors());
//                }catch (Throwable ex) {
//                    throw new BeanCreationException(mbd.getResourceDescription(), beanName,
//                            "Resolution of declared constructors on bean Class [" + beanClass.getName() +
//                                    "] from ClassLoader [" + beanClass.getClassLoader() + "] failed", ex);
//                }
//            }
//
//            if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
//                Constructor<?> uniqueCandidate = candidates[0];
//                if (uniqueCandidate.getParameterCount() == 0) {
//                    synchronized (mbd.constructorArgumentLock) {
//                        mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
//                        mbd.constructorArgumentsResolved = true;
//                        mbd.resolvedConstructorArguments = EMPTY_ARGS;
//                    }
//                    //实例化bean
//                    bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
//                    return bw;
//                }
//            }
//
//            //需要解析构造函数
//            boolean autowiring = (chosenCtors != null ||
//                    mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
//            ConstructorArgumentValues resolvedValues = null;
//
//            int minNrOfArgs;
//            if (explicitArgs != null) {
//                minNrOfArgs = explicitArgs.length;
//            }else {
//                ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
//                resolvedValues = new ConstructorArgumentValues();
//                //将此bean的构造函数参数解析为resolvedValues对象。这可能涉及到查找其他bean。
//                //此方法还用于处理静态工厂方法的调用
//                minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
//            }
//
//            AutowireUtils.sortConstructors(candidates);
//            int minTypeDiffWeight = Integer.MAX_VALUE;
//            Set<Constructor<?>> ambiguousConstructors = null;
//            LinkedList<UnsatisfiedDependencyException> causes = null;
//
//            for (Constructor<?> candidate : candidates) {
//                Class<?>[] paramTypes = candidate.getParameterTypes();
//
//                if (constructorToUse != null && argsToUse != null && argsToUse.length > paramTypes.length) {
//                    //已经找到贪婪的构造函数可以满足-->不寻找任何进一步，只有较少的贪婪构造函数留下
//                    break;
//                }
//                if (paramTypes.length < minNrOfArgs) {
//                    continue;
//                }
//
//                ConstructorResolver.ArgumentsHolder argsHolder;
//                if (resolvedValues != null) {
//                    try {
//                        //委托检查Java 6的ConstructorProperties注释
//                        String[] paramNames = ConstructorResolver.ConstructorPropertiesChecker.evaluate(candidate, paramTypes.length);
//                        if (paramNames == null) {
//                            ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
//                            if (pnd != null) {
//                                paramNames = pnd.getParameterNames(candidate);
//                            }
//                        }
//                        //给定已解析的构造函数参数值，创建参数数组来调用构造函数或工厂方法
//                        argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
//                                getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);
//                    }catch (UnsatisfiedDependencyException ex) {
//                        if (logger.isTraceEnabled()) {
//                            logger.trace("Ignoring constructor [" + candidate + "] of bean '" + beanName + "': " + ex);
//                        }
//                        //吞下并尝试下一个构造函数
//                        if (causes == null) {
//                            causes = new LinkedList<>();
//                        }
//                        causes.add(ex);
//                        continue;
//                    }
//                }else {
//                    //给定的显式参数-->参数长度必须精确匹配
//                    if (paramTypes.length != explicitArgs.length) {
//                        continue;
//                    }
//                    argsHolder = new ConstructorResolver.ArgumentsHolder(explicitArgs);
//                }
//
//                int typeDiffWeight = (mbd.isLenientConstructorResolution() ?
//                        argsHolder.getTypeDifferenceWeight(paramTypes) : argsHolder.getAssignabilityWeight(paramTypes));
//                //如果它表示最接近的匹配，则选择此构造函数
//                if (typeDiffWeight < minTypeDiffWeight) {
//                    constructorToUse = candidate;
//                    argsHolderToUse = argsHolder;
//                    argsToUse = argsHolder.arguments;
//                    minTypeDiffWeight = typeDiffWeight;
//                    ambiguousConstructors = null;
//                }else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
//                    if (ambiguousConstructors == null) {
//                        ambiguousConstructors = new LinkedHashSet<>();
//                        ambiguousConstructors.add(constructorToUse);
//                    }
//                    ambiguousConstructors.add(candidate);
//                }
//            }
//
//            if (constructorToUse == null) {
//                if (causes != null) {
//                    UnsatisfiedDependencyException ex = causes.removeLast();
//                    for (Exception cause : causes) {
//                        this.beanFactory.onSuppressedException(cause);
//                    }
//                    throw ex;
//                }
//                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
//                        "Could not resolve matching constructor " +
//                                "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities)");
//            }else if (ambiguousConstructors != null && !mbd.isLenientConstructorResolution()) {
//                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
//                        "Ambiguous constructor matches found in bean '" + beanName + "' " +
//                                "(hint: specify index/type/name arguments for simple parameters to avoid type ambiguities): " +
//                                ambiguousConstructors);
//            }
//
//            if (explicitArgs == null && argsHolderToUse != null) {
//                argsHolderToUse.storeCache(mbd, constructorToUse);
//            }
//        }
//
//        Assert.state(argsToUse != null, "Unresolved constructor arguments");
//        //实例化bean
//        bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
//        return bw;
//    }
//
//
//    //AbstractAutowireCapableBeanFactory:
//    //使用给定bean的默认构造函数实例化它
//    protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
//        try {
//            Object beanInstance;
//            final BeanFactory parent = this;
//            //获取系统的安全管理接口，JDK标准的安全管理API
//            if (System.getSecurityManager() != null) {
//                //这里是一个匿名内置类，根据实例化策略创建实例对象
//                beanInstance = AccessController.doPrivileged((PrivilegedAction<Object>) () ->
//                                getInstantiationStrategy().instantiate(mbd, beanName, parent),
//                        getAccessControlContext());
//            } else {
//                //将实例化的对象封装起来
//                beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
//            }
//            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
//            initBeanWrapper(bw);
//            return bw;
//        } catch (Throwable ex) {
//            throw new BeanCreationException(
//                    mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
//        }
//    }
//
//
//
//    //AbstractAutowireCapableBeanFactory:
//    //确定给定bean的候选构造函数，检查所有已注册的构造函数
//    protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName)
//            throws BeansException {
//
//        if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
//            for (BeanPostProcessor bp : getBeanPostProcessors()) {
//                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
//                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
//                    Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
//                    if (ctors != null) {
//                        return ctors;
//                    }
//                }
//            }
//        }
//        return null;
//    }
//
//
//
//
//    //DefaultListableBeanFactory:
//    //根据工厂中定义的bean解析指定的依赖项
//    public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
//                                    @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
//        //初始化基础方法参数的参数名称发现(如果有的话)
//        descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
//        if (Optional.class == descriptor.getDependencyType()) {
//            //为指定的依赖项创建一个{Optional}包装器
//            return createOptionalDependency(descriptor, requestingBeanName);
//        }else if (ObjectFactory.class == descriptor.getDependencyType() ||
//                ObjectProvider.class == descriptor.getDependencyType()) {
//            //序列化的ObjectFactory/ObjectProvider，用于依赖项的延迟解析
//            return new DefaultListableBeanFactory.DependencyObjectProvider(descriptor, requestingBeanName);
//        }else if (javaxInjectProviderClass == descriptor.getDependencyType()) {
//            //单独的内部类，以避免对javax.inject api的硬依赖
//            return new DefaultListableBeanFactory.Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
//        }else {
//            //解析依赖
//            Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(descriptor, requestingBeanName);
//            if (result == null) {
//                result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
//            }
//            return result;
//        }
//    }
//
//
//    //DefaultListableBeanFactory:
//    //为指定的依赖项创建一个{Optional}包装器
//    private Optional<?> createOptionalDependency(
//            DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {
//
//        DependencyDescriptor descriptorToUse = new DefaultListableBeanFactory.NestedDependencyDescriptor(descriptor) {
//            @Override
//            public boolean isRequired() {
//                return false;
//            }
//            @Override
//            public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
//                return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName, args) :
//                        super.resolveCandidate(beanName, requiredType, beanFactory));
//            }
//        };
//        //去处理bean依赖
//        Object result = doResolveDependency(descriptorToUse, beanName, null, null);
//        return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
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
//    //DefaultListableBeanFactory:
//    //处理bean依赖
//    public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
//                                      @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
//
//        InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
//        try {
//            Object shortcut = descriptor.resolveShortcut(this);
//            if (shortcut != null) {
//                return shortcut;
//            }
//
//            //确定包装参数/字段的声明(非泛型)类型
//            Class<?> type = descriptor.getDependencyType();
//            Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
//            if (value != null) {
//                if (value instanceof String) {
//                    //处理内嵌的属性值
//                    String strVal = resolveEmbeddedValue((String) value);
//                    BeanDefinition bd = (beanName != null && containsBean(beanName) ?
//                            getMergedBeanDefinition(beanName) : null);
//                    //计算bean定义中包含的给定字符串，可能将其解析为表达式
//                    value = evaluateBeanDefinitionString(strVal, bd);
//                }
//                TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
//                try {
//                    //将值转换为所需的类型(如果需要，从字符串转换)
//                    return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
//                }catch (UnsupportedOperationException ex) {
//                    //不支持类型描述符解析的自定义TypeConverter
//                    return (descriptor.getField() != null ?
//                            converter.convertIfNecessary(value, type, descriptor.getField()) :
//                            converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
//                }
//            }
//
//            //处理多重的bean
//            Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
//            if (multipleBeans != null) {
//                return multipleBeans;
//            }
//
//            //获取自动匹配候选的多个bean
//            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
//            if (matchingBeans.isEmpty()) {
//                if (isRequired(descriptor)) {
//                    //提高NoSuchBeanDefinitionException或BeanNotOfRequiredTypeException不肯舍弃的依赖
//                    raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
//                }
//                return null;
//            }
//
//            String autowiredBeanName;
//            Object instanceCandidate;
//
//            if (matchingBeans.size() > 1) {
//                //定给定的一组bean的自动装配候选人
//                autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
//                if (autowiredBeanName == null) {
//                    if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
//                        //处理指定的不是唯一的场景:默认情况下,抛一个{NoUniqueBeanDefinitionException}
//                        return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
//                    }else {
//                        return null;
//                    }
//                }
//                instanceCandidate = matchingBeans.get(autowiredBeanName);
//            }else {
//                //我们有一个匹配。
//                Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
//                autowiredBeanName = entry.getKey();
//                instanceCandidate = entry.getValue();
//            }
//
//            if (autowiredBeanNames != null) {
//                autowiredBeanNames.add(autowiredBeanName);
//            }
//            if (instanceCandidate instanceof Class) {
//                instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
//            }
//            Object result = instanceCandidate;
//            if (result instanceof NullBean) {
//                if (isRequired(descriptor)) {
//                    raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
//                }
//                result = null;
//            }
//            if (!ClassUtils.isAssignableValue(type, result)) {
//                throw new BeanNotOfRequiredTypeException(autowiredBeanName, type, instanceCandidate.getClass());
//            }
//            return result;
//        }finally {
//            ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
//        }
//    }
//
//
//
//
//
//
//    //AutowiredAnnotationBeanPostProcessor.ShortcutDependencyDescriptor:
//    public Object resolveShortcut(BeanFactory beanFactory) {
//        return beanFactory.getBean(this.shortcut, this.requiredType);
//    }
//
//    //AbstractBeanFactory:
//    public String resolveEmbeddedValue(@Nullable String value) {
//        if (value == null) {
//            return null;
//        }
//        String result = value;
//        for (StringValueResolver resolver : this.embeddedValueResolvers) {
//            result = resolver.resolveStringValue(result);
//            if (result == null) {
//                return null;
//            }
//        }
//        return result;
//    }
//
//    //EmbeddedValueResolver:
//    public String resolveStringValue(String strVal) {
//        //BeanExpressionContext exprContext
//        String value = this.exprContext.getBeanFactory().resolveEmbeddedValue(strVal);
//        //BeanExpressionResolver exprResolver
//        if (this.exprResolver != null && value != null) {
//            Object evaluated = this.exprResolver.evaluate(value, this.exprContext);
//            value = (evaluated != null ? evaluated.toString() : null);
//        }
//        return value;
//    }
//
//
//
//
//
//
//    //DependencyDescriptor:
//    //确定包装参数/字段的声明(非泛型)类型
//    public Class<?> getDependencyType() {
//        if (this.field != null) {
//            if (this.nestingLevel > 1) {
//                Type type = this.field.getGenericType();
//                for (int i = 2; i <= this.nestingLevel; i++) {
//                    if (type instanceof ParameterizedType) {
//                        Type[] args = ((ParameterizedType) type).getActualTypeArguments();
//                        type = args[args.length - 1];
//                    }
//                }
//                if (type instanceof Class) {
//                    return (Class<?>) type;
//                }else if (type instanceof ParameterizedType) {
//                    Type arg = ((ParameterizedType) type).getRawType();
//                    if (arg instanceof Class) {
//                        return (Class<?>) arg;
//                    }
//                }
//                return Object.class;
//            }else {
//                return this.field.getType();
//            }
//        }else {
//            return obtainMethodParameter().getNestedParameterType();
//        }
//    }
//
//    //MethodParameter:
//    //返回方法/构造函数参数的嵌套类型
//    public Class<?> getNestedParameterType() {
//        if (this.nestingLevel > 1) {
//            Type type = getGenericParameterType();
//            for (int i = 2; i <= this.nestingLevel; i++) {
//                if (type instanceof ParameterizedType) {
//                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
//                    Integer index = getTypeIndexForLevel(i);
//                    type = args[index != null ? index : args.length - 1];
//                }
//            // TODO: Object.class if unresolvable
//            }
//            if (type instanceof Class) {
//                return (Class<?>) type;
//            }else if (type instanceof ParameterizedType) {
//                Type arg = ((ParameterizedType) type).getRawType();
//                if (arg instanceof Class) {
//                    return (Class<?>) arg;
//                }
//            }
//            return Object.class;
//        }else {
//            return getParameterType();
//        }
//    }
//
//
//
//
//    //AbstractBeanFactory:
//    //计算bean定义中包含的给定字符串，可能将其解析为表达式
//    protected Object evaluateBeanDefinitionString(@Nullable String value, @Nullable BeanDefinition beanDefinition) {
//        if (this.beanExpressionResolver == null) {
//            return value;
//        }
//
//        Scope scope = null;
//        if (beanDefinition != null) {
//            String scopeName = beanDefinition.getScope();
//            if (scopeName != null) {
//                scope = getRegisteredScope(scopeName);
//            }
//        }
//        return this.beanExpressionResolver.evaluate(value, new BeanExpressionContext(this, scope));
//    }
//
//    //AbstractBeanFactory:
//    public Scope getRegisteredScope(String scopeName) {
//        Assert.notNull(scopeName, "Scope identifier must not be null");
//        //Map<String, Scope> scopes = new LinkedHashMap<>(8)
//        //从scope标识符字符串映射到相应的scope
//        return this.scopes.get(scopeName);
//    }
//
//
//
//
//    //DefaultListableBeanFactory:
//    private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
//                                        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {
//        //获取依赖bean类型
//        final Class<?> type = descriptor.getDependencyType();
//
//        //对多个元素的流访问的依赖描述符标记
//        if (descriptor instanceof DefaultListableBeanFactory.StreamDependencyDescriptor) {
//            //查找与所需类型匹配的bean实例。在为指定bean进行自动装配期间调用
//            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
//            if (autowiredBeanNames != null) {
//                autowiredBeanNames.addAll(matchingBeans.keySet());
//            }
//            Stream<Object> stream = matchingBeans.keySet().stream()
//                    .map(name -> descriptor.resolveCandidate(name, type, this))
//                    .filter(bean -> !(bean instanceof NullBean));
//            if (((DefaultListableBeanFactory.StreamDependencyDescriptor) descriptor).isOrdered()) {
//                //适配排序比较器
//                stream = stream.sorted(adaptOrderComparator(matchingBeans));
//            }
//            return stream;
//        }else if (type.isArray()) {
//            Class<?> componentType = type.getComponentType();
//            ResolvableType resolvableType = descriptor.getResolvableType();
//            Class<?> resolvedArrayType = resolvableType.resolve(type);
//            if (resolvedArrayType != type) {
//                componentType = resolvableType.getComponentType().resolve();
//            }
//            if (componentType == null) {
//                return null;
//            }
//            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
//                    new DefaultListableBeanFactory.MultiElementDescriptor(descriptor));
//            if (matchingBeans.isEmpty()) {
//                return null;
//            }
//            if (autowiredBeanNames != null) {
//                autowiredBeanNames.addAll(matchingBeans.keySet());
//            }
//            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
//            Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
//            if (result instanceof Object[]) {
//                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
//                if (comparator != null) {
//                    Arrays.sort((Object[]) result, comparator);
//                }
//            }
//            return result;
//        }else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
//            Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
//            if (elementType == null) {
//                return null;
//            }
//            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
//                    new DefaultListableBeanFactory.MultiElementDescriptor(descriptor));
//            if (matchingBeans.isEmpty()) {
//                return null;
//            }
//            if (autowiredBeanNames != null) {
//                autowiredBeanNames.addAll(matchingBeans.keySet());
//            }
//            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
//            Object result = converter.convertIfNecessary(matchingBeans.values(), type);
//            if (result instanceof List) {
//                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
//                if (comparator != null) {
//                    ((List<?>) result).sort(comparator);
//                }
//            }
//            return result;
//        }else if (Map.class == type) {
//            ResolvableType mapType = descriptor.getResolvableType().asMap();
//            Class<?> keyType = mapType.resolveGeneric(0);
//            if (String.class != keyType) {
//                return null;
//            }
//            Class<?> valueType = mapType.resolveGeneric(1);
//            if (valueType == null) {
//                return null;
//            }
//            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
//                    new DefaultListableBeanFactory.MultiElementDescriptor(descriptor));
//            if (matchingBeans.isEmpty()) {
//                return null;
//            }
//            if (autowiredBeanNames != null) {
//                autowiredBeanNames.addAll(matchingBeans.keySet());
//            }
//            return matchingBeans;
//        }else {
//            return null;
//        }
//    }
//
//
//    //DefaultListableBeanFactory；
//    private void raiseNoMatchingBeanFound(
//            Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {
//
//        checkBeanNotOfRequiredType(type, descriptor);
//
//        throw new NoSuchBeanDefinitionException(resolvableType,
//                "expected at least 1 bean which qualifies as autowire candidate. " +
//                        "Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
//    }
//
//    //DefaultListableBeanFactory:
//    //提高BeanNotOfRequiredTypeException不肯舍弃的依赖性,如果适用,即如果bean将匹配的目标类型,是一个不公开代理
//    private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
//        for (String beanName : this.beanDefinitionNames) {
//            RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
//            Class<?> targetType = mbd.getTargetType();
//            if (targetType != null && type.isAssignableFrom(targetType) &&
//                    isAutowireCandidate(beanName, mbd, descriptor, getAutowireCandidateResolver())) {
//                //可能一个代理干扰目标类型匹配-->抛出有意义的异常
//                Object beanInstance = getSingleton(beanName, false);
//                Class<?> beanType = (beanInstance != null && beanInstance.getClass() != NullBean.class ?
//                        beanInstance.getClass() : predictBeanType(beanName, mbd));
//                if (beanType != null && !type.isAssignableFrom(beanType)) {
//                    throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
//                }
//            }
//        }
//
//        BeanFactory parent = getParentBeanFactory();
//        if (parent instanceof DefaultListableBeanFactory) {
//            ((DefaultListableBeanFactory) parent).checkBeanNotOfRequiredType(type, descriptor);
//        }
//    }
//
//
//
//    //DefaultListableBeanFactory:
//    //确定指定的bean定义是否符合autowire候选，以便将其注入到声明匹配类型依赖项的其他bean中
//    protected boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
//            throws NoSuchBeanDefinitionException {
//
//        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
//        if (containsBeanDefinition(beanDefinitionName)) {
//            return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(beanDefinitionName), descriptor, resolver);
//        }else if (containsSingleton(beanName)) {
//            return isAutowireCandidate(beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
//        }
//
//        BeanFactory parent = getParentBeanFactory();
//        if (parent instanceof DefaultListableBeanFactory) {
//            //在这个工厂中没有找到bean定义——>委托给父类。
//            return ((DefaultListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
//        }else if (parent instanceof ConfigurableListableBeanFactory) {
//            //如果没有DefaultListableBeanFactory，就不能传递解析器
//            return ((ConfigurableListableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
//        }else {
//            return true;
//        }
//    }
//
//
//
//
//
//    //DefaultListableBeanFactory:
//    //确定给定的一组bean的自动装配候选人
//    protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
//        Class<?> requiredType = descriptor.getDependencyType();
//        //1>确定在给定的一组bean的主要候选人
//        String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
//        if (primaryCandidate != null) {
//            return primaryCandidate;
//        }
//        //2>确定给定的一组bean优先级最高的候选人
//        String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
//        if (priorityCandidate != null) {
//            return priorityCandidate;
//        }
//        // Fallback
//        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
//            String candidateName = entry.getKey();
//            Object beanInstance = entry.getValue();
//            //Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16)
//            //依赖类型到相应的自动注入值的映射
//            if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) ||
//                    matchesBeanName(candidateName, descriptor.getDependencyName())) {
//                return candidateName;
//            }
//        }
//        return null;
//    }
//
//
//
//
//    //DependencyDescriptor:
//    public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory)
//            throws BeansException {
//        //调用bean工厂的初始化bean方法
//        return beanFactory.getBean(beanName);
//    }
//
//    //ConstructorResolver:
//    static InjectionPoint setCurrentInjectionPoint(@Nullable InjectionPoint injectionPoint) {
//        //NamedThreadLocal<InjectionPoint> currentInjectionPoint=new NamedThreadLocal<>("Current injection point")
//        InjectionPoint old = currentInjectionPoint.get();
//        if (injectionPoint != null) {
//            currentInjectionPoint.set(injectionPoint);
//        }else {
//            currentInjectionPoint.remove();
//        }
//        return old;
//    }
//
//
//
//
//
//
//    //DefaultListableBeanFactory:
//    protected Map<String, Object> findAutowireCandidates(
//            @Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
//
//        //1>获取给定类型的所有bean名称，包括在祖先工厂中定义的名称。将在覆盖bean定义的情况下返回唯一的名称
//        String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
//                this, requiredType, true, descriptor.isEager());
//
//        Map<String, Object> result = new LinkedHashMap<>(candidateNames.length);
//        for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
//            Class<?> autowiringType = classObjectEntry.getKey();
//            if (autowiringType.isAssignableFrom(requiredType)) {
//                Object autowiringValue = classObjectEntry.getValue();
//                //2>根据给定的必需类型解析给定的自动装配值，例如{ObjectFactory}值到它的实际对象结果
//                autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
//                if (requiredType.isInstance(autowiringValue)) {
//                    result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
//                    break;
//                }
//            }
//        }
//
//        for (String candidate : candidateNames) {
//            //3.1>确定给定的beanName/candidateName对是否表示自引用
//            //3.2>确定指定的bean定义是否符合autowire候选，以便将其注入到声明匹配类型依赖项的其他bean中
//            if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
//                //4>在候选映射中添加一个entry:一个bean实例(如果可用)，或者只是解析类型
//                addCandidateEntry(result, candidate, descriptor, requiredType);
//            }
//        }
//        if (result.isEmpty()) {
//            boolean multiple = indicatesMultipleBeans(requiredType);
//            //如果第一次传递没有找到任何内容，则考虑回退匹配
//            DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
//            for (String candidate : candidateNames) {
//                if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
//                        (!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
//                    addCandidateEntry(result, candidate, descriptor, requiredType);
//                }
//            }
//            if (result.isEmpty() && !multiple) {
//                //将自引用视为最后一次传递，但在依赖项集合的情况下，不是完全相同的bean本身
//                for (String candidate : candidateNames) {
//                    if (isSelfReference(beanName, candidate) &&
//                            (!(descriptor instanceof DefaultListableBeanFactory.MultiElementDescriptor) || !beanName.equals(candidate)) &&
//                            isAutowireCandidate(candidate, fallbackDescriptor)) {
//                        addCandidateEntry(result, candidate, descriptor, requiredType);
//                    }
//                }
//            }
//        }
//        return result;
//    }
//
//
//
//
//
//    //DefaultListableBeanFactory:
//    private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
//        Comparator<Object> comparator = getDependencyComparator();
//        if (comparator instanceof OrderComparator) {
//            return ((OrderComparator) comparator).withSourceProvider(
//                    createFactoryAwareOrderSourceProvider(matchingBeans));
//        }else {
//            return comparator;
//        }
//    }
//
//    //DefaultListableBeanFactory:
//    private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
//        IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
//        beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
//        return new DefaultListableBeanFactory.FactoryAwareOrderSourceProvider(instancesToBeanNames);
//    }
//
//
//
//}
