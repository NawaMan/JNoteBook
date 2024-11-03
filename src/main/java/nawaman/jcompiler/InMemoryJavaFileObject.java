package nawaman.jcompiler;

import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

class InMemoryJavaFileObject extends SimpleJavaFileObject {
    
    private static URI javaFileUri(String className) {
        var fileName  = className.replace('.', '/');
        var fileExt   = JavaFileObject.Kind.SOURCE.extension;
        var uriString = "string:///" + fileName + fileExt;
        return URI.create(uriString);
    }
    
    private final String code;
    
    InMemoryJavaFileObject(String className, String code) {
        super(javaFileUri(className), JavaFileObject.Kind.SOURCE);
        this.code = code;
    }
    
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
    
}
