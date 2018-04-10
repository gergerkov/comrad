package com.rad.fw;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Collector {

    private List<Class> commandControllers = new ArrayList<>();

    public Collector() throws IOException, URISyntaxException {
        init();
    }

    public List<Class> getCommandControllers() {
        return commandControllers;
    }

    private void init() throws URISyntaxException, IOException {
        URL[] urls = ((URLClassLoader) Collector.class.getClassLoader()).getURLs();
        for (URL url : urls) {
            Path p  = Paths.get(url.toURI());
            if (p.toFile().isDirectory()) {
                commandControllers = getClassesUnderDir(p).stream()
                        .filter(c -> c.getAnnotation(CommandController.class) != null)
                        .collect(Collectors.toList());
            }
        }
    }

    private List<Class> getClassesUnderDir(Path dir) throws IOException {
        return Files.walk(dir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toFile().getName().endsWith(".class"))
                .map(Collector::loadClass).collect(Collectors.toList());
    }

    private static Class loadClass(Path classFile) {
        String rootDirectory = classFile.toFile().getParent();
        String className = getFileNameWithoutExtension(classFile);

        URL[] urls;
        ClassLoader cl = null;

        Class dynamicClass = null;

        try {
            urls = new URL[]{new File(rootDirectory).toURI().toURL()};
            cl = new URLClassLoader(urls);

            dynamicClass = cl.loadClass(className);
        } catch (MalformedURLException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoClassDefFoundError e) {
            String classPackage = getClassPackage(e.getMessage());
            try {
                dynamicClass = cl.loadClass(classPackage);
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return dynamicClass;
    }

    private static String getClassPackage(String errorMsg) {
        int startIndex = errorMsg.lastIndexOf(" ") + 1;
        int endIndex = errorMsg.length() - 1;
        String classPackage = errorMsg.substring(startIndex, endIndex);
        classPackage = classPackage.replace('/', '.');
        return classPackage;
    }

    private static String getFileNameWithoutExtension(Path p) {
        String path = p.toFile().getAbsolutePath();
        int start = path.lastIndexOf(File.separator) + 1;
        int end = path.lastIndexOf(".");
        end = start < end ? end : path.length();
        return path.substring(start, end);
    }

}
