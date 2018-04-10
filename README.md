# comrad

# Usage:

package com.cmd;

import com.rad.fw.Command;
import com.rad.fw.CommandController;

@CommandController
public class Controller {

    private String anything;

    @Command(name = "sum", help = "Calculates and prints the sum of two numbers.")
    public void sum(String n1, String n2) {
        System.out.println(Double.parseDouble(n1) + Double.parseDouble(n2));
    }

    @Command(name = "set", help = "Sets Controller.something to anything.")
    public void setSomething(String anything) {
        this.anything = anything;
    }

    @Command(name = "print", help = "Prints anything.")
    public void printAnything() {
        System.out.println(anything);
    }

}

package com.cmd;

import com.rad.fw.Comrad;

import java.io.IOException;
import java.net.URISyntaxException;

public class App {

    public static void main(String[] args) throws IOException, URISyntaxException {
        Comrad comrad = new Comrad();
        System.out.println(comrad.getHelpMessage());
        String cmdArgs = "set this sum 1 2 print";
        comrad.invokeAll(cmdArgs.split(" "));
    }

}

# prints: this 3.0
