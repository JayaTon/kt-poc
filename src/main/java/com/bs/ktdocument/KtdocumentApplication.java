package com.bs.ktdocument;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KtdocumentApplication implements CommandLineRunner {
	private static final String WATCH_FOLDER = "E:\\Learning\\SAP\\transcript";  // Change this path
	private static final String PROCESSED_LOG = "E:\\Learning\\SAP\\processed_files.log"; // File to track processed files
	private static final Set<String> processedFiles = new HashSet<>();

	public static void main(String[] args) {
		SpringApplication.run(KtdocumentApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		loadProcessedFiles();
		watchFolder();
	}

	private void loadProcessedFiles() {
		try {
			Path logPath = Paths.get(PROCESSED_LOG);
			if (Files.exists(logPath)) {
				Files.lines(logPath).forEach(processedFiles::add);
			}
		} catch (IOException e) {
			System.err.println("Error loading processed files: " + e.getMessage());
		}
	}

	private void saveProcessedFile(String fileName) {
		try {
			Files.write(Paths.get(PROCESSED_LOG), (fileName + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			processedFiles.add(fileName);
		} catch (IOException e) {
			System.err.println("Error saving processed file: " + e.getMessage());
		}
	}

	private void watchFolder() throws IOException, InterruptedException {
		WatchService watchService = FileSystems.getDefault().newWatchService();
		Path path = Paths.get(WATCH_FOLDER);
		path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

		System.out.println("Watching folder: " + WATCH_FOLDER);

		while (true) {
			WatchKey key = watchService.take();
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path newFilePath = path.resolve(ev.context());
				String fileName = newFilePath.getFileName().toString();

				if (fileName.endsWith(".vtt") && !processedFiles.contains(fileName)) {
					System.out.println("New VTT file detected: " + fileName);
					new VttProcessor().processVTTFile(newFilePath.toString());
					saveProcessedFile(fileName);
				} else {
					System.out.println("Skipping already processed file: " + fileName);
				}
			}
			key.reset();
		}
	}
}
