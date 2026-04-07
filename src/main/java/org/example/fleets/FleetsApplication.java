package org.example.fleets;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FleetsApplication {

    public static void main(String[] args) {
        initDirectories();
        SpringApplication.run(FleetsApplication.class, args);
    }

    private static void initDirectories() {
        String[] dirs = {"C:/fleets/upload", "C:/fleets/temp"};
        for (String path : dirs) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

}
