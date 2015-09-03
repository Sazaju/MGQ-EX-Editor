package fr.sazaju.mgqupdater;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import fr.sazaju.mgqeditor.MGQProject;
import fr.vergne.translation.TranslationMetadata.Field;
import fr.vergne.translation.impl.PatternFileMap;
import fr.vergne.translation.impl.PatternFileMap.PatternEntry;

public class MGQUpdater {

	private static final Logger logger = Logger.getLogger(MGQUpdater.class
			.getName());
	private final Field<String> idField = new Field<>("ID");
	private List<String> sortedIds;
	private Map<String, PatternEntry> japaneseEntries;
	private Map<String, PatternEntry> englishEntries;

	private void loadJapanese(File file) {
		logger.info("Loading Japanese file " + file + "...");
		PatternFileMap japaneseMap = new PatternFileMap(file,
				MGQProject.REGEX_ENTRY, MGQProject.REGEX_CONTENT,
				MGQProject.REGEX_ABSENT);
		japaneseMap.addFieldRegex(idField, MGQProject.REGEX_ID, false);
		sortedIds = new ArrayList<>(japaneseMap.size());
		japaneseEntries = new HashMap<>();
		for (PatternEntry entry : japaneseMap) {
			String entryId = entry.getMetadata().get(idField);
			sortedIds.add(entryId);
			japaneseEntries.put(entryId, entry);
			logger.finest("- Entry retrieved: " + entryId);
		}
		logger.info("Japanese loaded: " + japaneseMap.size() + " entries.");
	}

	private void loadEnglish(File file) {
		logger.info("Loading English file " + file + "...");
		PatternFileMap englishMap = new PatternFileMap(file,
				MGQProject.REGEX_ENTRY, MGQProject.REGEX_ABSENT,
				MGQProject.REGEX_CONTENT);
		englishMap.addFieldRegex(idField, MGQProject.REGEX_ID, false);
		englishEntries = new LinkedHashMap<>();
		for (PatternEntry entry : englishMap) {
			String entryId = entry.getMetadata().get(idField);
			englishEntries.put(entryId, entry);
			logger.finest("- Entry retrieved: " + entryId);
		}
		logger.info("English loaded: " + englishMap.size() + " entries.");
	}

	private void writeNewEnglish(File file) {
		logger.info("Writing new English file " + file + "...");
		PrintStream printer;
		try {
			printer = new PrintStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		int done = 0;
		for (String id : sortedIds) {
			logger.finest("- Entry " + id);
			PatternEntry japaneseEntry = japaneseEntries.get(id);
			PatternEntry englishEntry = englishEntries.get(id);

			String printString;
			if (englishEntry != null) {
				printString = englishEntry.rebuildString();
				logger.finest("- Store English version");
			} else {
				printString = japaneseEntry.rebuildString();
				logger.finest("- Store Japanese version");
			}
			printer.print("\n\n" + printString);
			done++;
		}
		printer.close();
		logger.info("New English file written: " + done + " entries");
	}

	private void writeUnusedEnglish(File file) {
		logger.info("Writing unused English file " + file + "...");
		PrintStream printer;
		try {
			printer = new PrintStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		int done = 0;
		for (PatternEntry englishEntry : englishEntries.values()) {
			String id = englishEntry.getMetadata().get(idField);
			if (japaneseEntries.containsKey(id)) {
				// used, so ignore
			} else {
				logger.finest("- Entry " + id);
				printer.print("\n\n" + englishEntry.rebuildString());
				done++;
			}
		}
		printer.close();
		logger.info("Unused English file written: " + done + " entries");
	}

	protected boolean hasUnused() {
		return !japaneseEntries.keySet().containsAll(englishEntries.keySet());
	}

	public static void main(String[] args) throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		PrintStream printer = new PrintStream(stream);
		printer.println(".level = INFO");
		printer.println("java.level = OFF");
		printer.println("javax.level = OFF");
		printer.println("sun.level = OFF");

		printer.println("handlers = java.util.logging.FileHandler, java.util.logging.ConsoleHandler");

		printer.println("java.util.logging.FileHandler.pattern = vh-editor.%u.%g.log");
		printer.println("java.util.logging.FileHandler.level = ALL");
		printer.println("java.util.logging.FileHandler.formatter = fr.vergne.logging.OneLineFormatter");

		printer.println("java.util.logging.ConsoleHandler.level = ALL");
		printer.println("java.util.logging.ConsoleHandler.formatter = fr.vergne.logging.OneLineFormatter");

		File file = new File("logging.properties");
		if (file.exists()) {
			try {
				printer.println(FileUtils.readFileToString(file));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			// use only default configuration
		}
		printer.close();

		LogManager manager = LogManager.getLogManager();
		try {
			manager.readConfiguration(IOUtils.toInputStream(new String(stream
					.toByteArray(), Charset.forName("UTF-8"))));
		} catch (SecurityException | IOException e) {
			throw new RuntimeException(e);
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					File directory = new File("MGQ-EX");
					File japaneseFile = new File(directory,
							"ScriptTextJapanese.rvtext");
					File englishFile = new File(directory,
							"ScriptTextEnglish.rvtext");
					File newEnglishFile = new File(englishFile.getParentFile(),
							englishFile.getName() + ".new");
					File unusedEnglishFile = new File(
							englishFile.getParentFile(), englishFile.getName()
									+ ".unused");

					MGQUpdater updater = new MGQUpdater();
					updater.loadJapanese(japaneseFile);
					updater.loadEnglish(englishFile);
					updater.writeNewEnglish(newEnglishFile);
					if (updater.hasUnused()) {
						updater.writeUnusedEnglish(unusedEnglishFile);
					} else {
						logger.info("No unused English entries.");
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
		}).start();
	}
}
