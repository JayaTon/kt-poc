package com.bs.ktdocument;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class KtdocumentApplication implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(KtdocumentApplication.class);

	private static final String WATCH_FOLDER = "E:\\Learning\\SAP\\transcript";  // Watches for .vtt files
	private static final String CODE_FOLDER = "E:\\Learning\\SAP\\code";        // Watches for code files
	private static final String PROCESSED_LOG = "E:\\Learning\\SAP\\processed_files.log"; // File to track processed files
	private static final Set<String> processedFiles = ConcurrentHashMap.newKeySet();

	public static void main(String[] args) {
		SpringApplication.run(KtdocumentApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		loadProcessedFiles();
		watchFolders();
	}

	private void loadProcessedFiles() {
		try {
			Path logPath = Paths.get(PROCESSED_LOG);
			if (Files.exists(logPath)) {
				Files.lines(logPath).forEach(processedFiles::add);
				logger.info("Loaded {} processed files.", processedFiles.size());
			}
		} catch (IOException e) {
			logger.error("Error loading processed files", e);
		}
	}

	private void saveProcessedFile(String fileName) {
		try {
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			Files.write(Paths.get(PROCESSED_LOG), (fileName + "_" + timeStamp + "\n").getBytes(),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			processedFiles.add(fileName);
			logger.info("Processed file saved: {}", fileName);
		} catch (IOException e) {
			logger.error("Error saving processed file", e);
		}
	}

	private void watchFolders() throws IOException, InterruptedException {
		WatchService watchService = FileSystems.getDefault().newWatchService();

		// Register both directories
		registerFolder(watchService, Paths.get(WATCH_FOLDER));
		registerFolder(watchService, Paths.get(CODE_FOLDER));

		logger.info("Watching folders: {}, {}", WATCH_FOLDER, CODE_FOLDER);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				watchService.close();
				logger.info("Watch service closed gracefully.");
			} catch (IOException e) {
				logger.error("Error closing watch service", e);
			}
		}));

		while (true) {
			WatchKey key = watchService.take();
			Path folderPath = (Path) key.watchable();

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent<Path> ev = (WatchEvent<Path>) event;
				Path newFilePath = folderPath.resolve(ev.context());
				String fileName = newFilePath.getFileName().toString();

				if (Files.isRegularFile(newFilePath) && !processedFiles.contains(fileName)) {
					if (folderPath.toString().equals(WATCH_FOLDER) && fileName.endsWith(".vtt")) {
						logger.info("New VTT file detected: {}", fileName);
						new VttProcessor().processVTTFile(newFilePath.toString());
						saveProcessedFile(fileName);
					} else if (folderPath.toString().equals(CODE_FOLDER) && isCodeFile(fileName)) {
						logger.info("New Code file detected: {}", fileName);
						new CodeProcessor().processCodeFile(newFilePath.toString());
						saveProcessedFile(fileName);
					} else {
						logger.info("Skipping unsupported file: {}", fileName);
					}
				}
			}
			key.reset();
		}
	}

	private void registerFolder(WatchService watchService, Path folder) throws IOException {
		if (Files.exists(folder)) {
			folder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
		} else {
			logger.warn("Folder does not exist: {}", folder);
		}
	}

	private boolean isCodeFile(String fileName) {
		return fileName.endsWith(".java") || fileName.endsWith(".kt") || fileName.endsWith(".py")
				|| fileName.endsWith(".js") || fileName.endsWith(".ts") || fileName.endsWith(".cpp")
				|| fileName.endsWith(".c") || fileName.endsWith(".cs");
	}
}
