package org.tdar.nlp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;

public class PersonCleaner {

    public static void main(String[] args) throws IOException {
        File file = new File("people.txt");
        for (String line : FileUtils.readLines(file, Charset.defaultCharset())) {
            
        }
        

    }

}
