package fr.sazaju.mgqeditor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.logging.LogManager;

import org.apache.commons.io.IOUtils;

import fr.sazaju.mgqeditor.MGQProject.MGQEntry;
import fr.sazaju.mgqeditor.MGQProject.MGQMap;
import fr.sazaju.mgqeditor.MGQProject.MapID;
import fr.vergne.translation.editor.Editor;
import fr.vergne.translation.util.ProjectLoader;

@SuppressWarnings("serial")
public class MGQEditor extends Editor<MapID, MGQEntry, MGQMap, MGQProject> {

	public MGQEditor() {
		super(new ProjectLoader<MGQProject>() {

			@Override
			public MGQProject load(File directory) {
				return new MGQProject(directory);
			}
		});
	}

	public static void main(String[] args) {
		LogManager manager = LogManager.getLogManager();
		try {
			File file = new File("debug.properties");
			if (file.exists()) {
				FileInputStream fis = new FileInputStream(file);
				manager.readConfiguration(fis);
				fis.close();
			} else {
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

				printer.close();
				manager.readConfiguration(IOUtils.toInputStream(new String(
						stream.toByteArray(), Charset.forName("UTF-8"))));
			}
		} catch (SecurityException | IOException e) {
			throw new RuntimeException(e);
		}

		new Runnable() {
			public void run() {
				new MGQEditor().setVisible(true);
			}
		}.run();
	}
}
