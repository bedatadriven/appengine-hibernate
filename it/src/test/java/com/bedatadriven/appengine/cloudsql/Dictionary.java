package com.bedatadriven.appengine.cloudsql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


public class Dictionary {

    public static final Dictionary INSTANCE = new Dictionary();
    
    private List<String> dictionary;
    private List<String> authors;
    private final AtomicLong authorIndex = new AtomicLong(0);

    public Dictionary() {
        try {
            dictionary = loadList("dict.txt");
            authors = loadList("authors.txt");
        } catch (Exception e) {
            System.err.println("Failed to load resources");
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    
    private List<String> loadList(String resourceName) throws IOException {
        return Resources.readLines(Resources.getResource(LoadTester.class, resourceName), Charsets.UTF_8);
    }

    public String randomContent() {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<5;++i) {
            if(i>0) {
                sb.append(' ');
            }
            sb.append(dictionary.get(ThreadLocalRandom.current().nextInt(0, dictionary.size())));
        }
        return sb.toString();
    }
    
    public String nextAuthor() {
        long nextAuthor = authorIndex.getAndIncrement() % authors.size();
        return authors.get((int)nextAuthor);
    }
   
}
