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

	public MGQUpdater(File japaneseFile, File englishFile)
			throws FileNotFoundException {
		final Field<String> idField = new Field<>("ID");

		logger.info("Loading Japanese file " + japaneseFile + "...");
		PatternFileMap japaneseMap = new PatternFileMap(japaneseFile,
				MGQProject.REGEX_ENTRY, MGQProject.REGEX_CONTENT,
				MGQProject.REGEX_ABSENT);
		japaneseMap.addFieldRegex(idField, MGQProject.REGEX_ID, false);
		List<String> sortedIds = new ArrayList<>(japaneseMap.size());
		Map<String, PatternEntry> japaneseEntries = new HashMap<>();
		for (PatternEntry entry : japaneseMap) {
			String entryId = entry.getMetadata().get(idField);
			sortedIds.add(entryId);
			japaneseEntries.put(entryId, entry);
			logger.finest("- Entry retrieved: " + entryId);
		}
		logger.info("Japanese loaded: " + japaneseMap.size() + " entries.");

		logger.info("Loading English file " + englishFile + "...");
		PatternFileMap englishMap = new PatternFileMap(englishFile,
				MGQProject.REGEX_ENTRY, MGQProject.REGEX_ABSENT,
				MGQProject.REGEX_CONTENT);
		englishMap.addFieldRegex(idField, MGQProject.REGEX_ID, false);
		Map<String, PatternEntry> englishEntries = new LinkedHashMap<>();
		for (PatternEntry entry : englishMap) {
			String entryId = entry.getMetadata().get(idField);
			englishEntries.put(entryId, entry);
			logger.finest("- Entry retrieved: " + entryId);
		}
		logger.info("English loaded: " + englishMap.size() + " entries.");

		File newEnglishFile = new File(englishFile.getParentFile(),
				englishFile.getName() + ".new");
		logger.info("Writing new English file " + newEnglishFile + "...");
		PrintStream newStream = new PrintStream(newEnglishFile);
		for (String id : sortedIds) {
			logger.finest("- Entry " + id);
			PatternEntry japaneseEntry = japaneseEntries.remove(id);
			PatternEntry englishEntry = englishEntries.remove(id);

			String printString;
			if (englishEntry != null) {
				printString = englishEntry.rebuildString();
				logger.finest("- Store English version");
			} else {
				printString = japaneseEntry.rebuildString();
				logger.finest("- Store Japanese version");
			}
			newStream.print("\n\n" + printString);
		}
		newStream.close();
		logger.info("New English file written: " + englishEntries.size()
				+ " entries remaining");

		if (englishEntries.size() <= 0) {
			logger.info("No unused English entries.");
		} else {
			File unusedEnglishFile = new File(englishFile.getParentFile(),
					englishFile.getName() + ".unused");
			logger.info("Writing unused English file " + unusedEnglishFile
					+ "...");
			PrintStream unusedStream = new PrintStream(unusedEnglishFile);
			for (PatternEntry englishEntry : englishEntries.values()) {
				logger.finest("- Entry "
						+ englishEntry.getMetadata().get(idField));
				unusedStream.print("\n\n" + englishEntry.rebuildString());
			}
			unusedStream.close();
			logger.info("Unused English file written: " + englishEntries.size()
					+ " entries");
		}
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
					new MGQUpdater(japaneseFile, englishFile);
				} catch (Exception e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
		}).start();
	}
}
