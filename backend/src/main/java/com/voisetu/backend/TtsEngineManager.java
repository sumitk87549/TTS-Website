package com.voisetu.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class TtsEngineManager implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(TtsEngineManager.class);
    private boolean isRunning = false;
    private Process engineProcess;

    @Override
    public void start() {
        log.info("Initializing Supertonic TTS Engine manager...");
        try {
            File currentDir = new File(System.getProperty("user.dir")); // backend directory
            File envDir = new File(currentDir.getParentFile(), "supertonic-env");
            
            if (!envDir.exists()) {
                log.info("Creating Python virtual environment at {}...", envDir.getAbsolutePath());
                runProcess(currentDir, "python3", "-m", "venv", envDir.getAbsolutePath());
                log.info("Installing supertonic[serve]. This will take a moment to download the HuggingFace model...");
                runProcess(currentDir, envDir.getAbsolutePath() + "/bin/pip", "install", "supertonic[serve]");
            }
            
            log.info("Starting local Supertonic TTS Engine on port 7788...");
            ProcessBuilder pb = new ProcessBuilder(
                envDir.getAbsolutePath() + "/bin/supertonic", 
                "serve", "--host", "127.0.0.1", "--port", "7788"
            );
            pb.directory(currentDir);
            pb.redirectErrorStream(true);
            
            File logFile = new File(currentDir.getParentFile(), "supertonic-engine.log");
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
            
            this.engineProcess = pb.start();
            this.isRunning = true;
            log.info("Supertonic TTS Engine started. Logs streaming to {}", logFile.getAbsolutePath());
            
        } catch (Exception e) {
            log.error("Failed to start embedded Supertonic TTS engine", e);
        }
    }

    private void runProcess(File dir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir);
        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + String.join(" ", command));
        }
    }

    @Override
    public void stop() {
        if (this.engineProcess != null) {
            log.info("Stopping embedded Supertonic TTS Engine...");
            this.engineProcess.destroy();
        }
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }
}
