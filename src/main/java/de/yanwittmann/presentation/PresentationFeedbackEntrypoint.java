package de.yanwittmann.presentation;

import lombok.Getter;
import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.SecureRandom;
import java.util.Random;
import java.util.Scanner;

@SpringBootApplication
public class PresentationFeedbackEntrypoint {

    @Getter
    private static String passwordValue;

    public static void main(String[] args) {
        parseCommandLineArguments(args);
        startCommandLineThread();
        SpringApplication.run(PresentationFeedbackEntrypoint.class, args);
    }

    private static void startCommandLineThread() {
        new Thread(() -> {
            final Scanner scanner = new Scanner(System.in);
            while (true) {
                final String command = scanner.nextLine();
                if (command.equalsIgnoreCase("exit")) {
                    System.exit(0);
                } else if (command.equalsIgnoreCase("password")) {
                    System.out.println("Password: " + passwordValue);
                } else {
                    System.out.println("Unknown command. Available commands: exit, password, path, wsPort, port");
                }
            }
        }).start();
    }

    private static void parseCommandLineArguments(String[] args) {
        Options options = new Options();

        Option password = Option.builder("pwd")
                .longOpt("password")
                .hasArg()
                .desc("Password for super user authentication, random otherwise")
                .build();

        final Option help = new Option("h", "help", false, "Print this message");

        options.addOption(password)
                .addOption(help);

        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();

        try {
            final CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                formatter.printHelp("CommandLineApp", options);
                System.exit(0);
            }

            passwordValue = cmd.getOptionValue("password", generatePassword(16));

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("CommandLineApp", options);
            System.exit(1);
        }
    }

    private static String generatePassword(int strength) {
        final char[] characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+".toCharArray();

        final Random random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < strength; i++) {
            password.append(characters[random.nextInt(characters.length)]);
        }

        return password.toString();
    }
}
