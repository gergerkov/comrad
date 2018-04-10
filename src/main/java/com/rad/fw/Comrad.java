package com.rad.fw;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Comrad {

    private static final Map<Class, Object> commanderInstances = new HashMap<>();

    class Cmd {

        final String name;
        final String help;
        final Method method;
        final int parameterCount;

        private Object parentInstance;

        private List<String> parameters;

        Cmd(Method m, Command c) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
            method = m;
            name = c.name();
            help = c.help();
            parameterCount = m.getParameterCount();
            parameters = new ArrayList<>(parameterCount);
            createCommanderInstance();
        }

        private void addParameter(String parameter) {
            parameters.add(parameter);
            print("\t\t" + this + " :: " + parameters);
            if (parameters.size() > parameterCount)
                throw new IllegalStateException("Too many parameters for " + name + "[" + parameterCount + "]: " + parameters);
        }

        private void invoke() throws IllegalAccessException, InvocationTargetException {
            Object[] params = new Object[parameters.size()];
            for (int i = 0; i < params.length; i++) params[i] = parameters.get(i);
            print("Invoking " + parentInstance.getClass() + "." + method.getName() + " (" + parameters + ")...");
            method.invoke(parentInstance, params);
        }

        @Override
        public String toString() {
            return name + ": " + help + "[" + parameterCount + "]";
        }

        private void createCommanderInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            Class parent = method.getDeclaringClass();
            parentInstance = commanderInstances.get(parent);
            if (parentInstance == null) {
                parentInstance = parent.getConstructor().newInstance();
                commanderInstances.put(parent, parentInstance);
            }
        }

    }

    private List<Cmd> availableCommands = new ArrayList<>();
    private List<Cmd> activeCommands = new ArrayList<>();

    public Comrad() throws IOException, URISyntaxException {
        buildCommands();
    }

    public void enableDebug(boolean b) {
        debugEnabled = b;
    }

    public void invokeAll(String[] args) {
        parse(args);
        StringJoiner sj = new StringJoiner("\n");
        activeCommands.stream().forEach(cmd -> sj.add(cmd.name + " " + cmd.parameters));

        print(sj.toString());

        activeCommands.stream().forEach(cmd -> {
            try {
                print("Invoking " + cmd);
                cmd.invoke();
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }

    public String getHelpMessage() {
        StringJoiner sj = new StringJoiner("\n");
        availableCommands.forEach(cmd -> sj.add(cmd.toString()));
        return sj.toString();
    }

    private void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            Cmd cmd = getCommandByName(a);
            if (cmd != null) {
                print("Found command " + cmd.name);
                activeCommands.add(cmd);
                for (int j = 0; j < cmd.parameterCount; j++) {
                    String param = args[++i];
                    cmd.addParameter(param);
                    print("\tAdding parameter " + param);
                }
            }
        }
    }

    private Cmd getCommandByName(String commandName) {
        for (Cmd c : availableCommands) {
            if (c.name.equals(commandName)) return c;
        }
        return null;
    }

    private void buildCommands() throws IOException, URISyntaxException {
        Collector c = new Collector();
        c.getCommandControllers().forEach(this::addCommands);
    }

    private void addCommands(Class src) {
        for (Method m : src.getDeclaredMethods()) {
            Command cmd = m.getAnnotation(Command.class);
            if (cmd != null) {
                try {
                    availableCommands.add(new Cmd(m, cmd));
                } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean debugEnabled = false;

    private void print(String msg) {
        if (debugEnabled) System.out.println("[COMRAD]: " + msg);
    }

}
