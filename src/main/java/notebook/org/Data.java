package notebook.org;

import functionalj.types.Struct;

public class Data {
    
    enum ChunkType {
        HTML,
        JAVA
    }
    
    @Struct
    static String CodeChunk(String source, ChunkType type) {
        if (source == null)
            return "CodeChunk: Source cannot be null.";
        
        if (type == null)
            return "Chunk: Type cannot be null.";
        
        return null;
    }
    
}
