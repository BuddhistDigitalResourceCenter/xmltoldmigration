package io.bdrc.xmltoldmigration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExceptionHelper {
    // Exception types
    public static final int ET_EWTS = 0;
    
    public static final Map<Integer, String> logNames = new HashMap<Integer, String>();
    static {
        logNames.put(ET_EWTS, "errors-ewts.log");
    }
    
    public static final Map<Integer, FileWriter> writers = new HashMap<Integer, FileWriter>();
    
    public static FileWriter getFileWriter(int type) {
        FileWriter res = writers.get(type);
        if (res == null) {
            String fileName = logNames.get(type);
            try {
                res = new FileWriter(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writers.put(type, res);
        }
        return res;
    }
    
    public static void closeAll() {
        for (FileWriter fw : writers.values()) {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void logEwtsException(String RID, String propIndication, String original, List<String> warnings) {
        FileWriter f = getFileWriter(ET_EWTS);
        try {
            f.write("- [ ] ["+RID+"](https://www.tbrc.org/#!rid="+RID+") ");
            f.write("has EWTS conversion problems on property `"+propIndication+"`");
            f.write(", original string: `"+original+"`:\n");
            for (String warning : warnings) {
                f.write("  - "+warning.replace('"', '`').replace("line1: ", "")+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}