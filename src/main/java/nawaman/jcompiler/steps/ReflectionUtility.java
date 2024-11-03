package nawaman.jcompiler.steps;

import static java.lang.reflect.Modifier.isPublic;

public class ReflectionUtility {
    
    public static boolean isPublicFirstLevelClass(String className, ClassLoader classLoader) {
        try {
            var clazz = classForClass(className, classLoader);
            return isFirstLevelClass(clazz) && isPublic(clazz.getModifiers());
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + className);
            return false;
        }
    }
    
    public static boolean isFirstLevelClass(Class<?> clazz) {
        return !(clazz.isMemberClass() || clazz.isLocalClass() || clazz.isAnonymousClass());
    }
    
    public static Class<?> classForClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);
            return clazz;
        } catch (ClassNotFoundException e) {
            int lastDotIndex = className.lastIndexOf('.');
            if (lastDotIndex > 0) {
                String outerClassName = className.substring(0, lastDotIndex);
                String innerClassName = className.substring(lastDotIndex + 1);
                try {
                    Class<?> outerClass = Class.forName(outerClassName, false, classLoader);
                    for (Class<?> innerClass : outerClass.getDeclaredClasses()) {
                        if (innerClass.getSimpleName().equals(innerClassName)) {
                            return innerClass;
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    throw e;
                }
            }
            throw e;
        }
    }
    
    public static String toImportFormat(String compiledClassName) {
        // Remove .class extension if present
        if (compiledClassName.endsWith(".class")) {
            compiledClassName = compiledClassName.substring(0, compiledClassName.length() - 6);
        }
        
        // Replace $ with . for inner classes
        compiledClassName = compiledClassName.replace('$', '.');
        
        // Replace / with . for package structure (in case of fully qualified names)
        compiledClassName = compiledClassName.replace('/', '.');
        
        return compiledClassName;
    }
}
