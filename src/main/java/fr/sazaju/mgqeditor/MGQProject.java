package fr.sazaju.mgqeditor;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.apache.commons.lang3.NotImplementedException;

import fr.sazaju.mgqeditor.MGQProject.MGQMap;
import fr.sazaju.mgqeditor.MGQProject.MapID;
import fr.vergne.translation.TranslationEntry;
import fr.vergne.translation.TranslationMap;
import fr.vergne.translation.TranslationMetadata;
import fr.vergne.translation.TranslationMetadata.Field;
import fr.vergne.translation.TranslationProject;
import fr.vergne.translation.impl.PatternFileMap;
import fr.vergne.translation.impl.PatternFileMap.PatternEntry;
import fr.vergne.translation.util.EntryFilter;
import fr.vergne.translation.util.Feature;

public class MGQProject implements TranslationProject<MapID, MGQMap> {

	private static final Logger logger = Logger.getLogger(MGQProject.class
			.getName());

	public static enum MapID {
		DATABASE, DIALOGUES, SCRIPT_TEXT
	}

	private final File directory;

	public MGQProject(File directory) {
		logger.info("Creating Project on " + directory);
		this.directory = directory;
	}

	@Override
	public Iterator<MapID> iterator() {
		return Arrays.asList(MapID.values()).iterator();
	}

	@Override
	public MGQMap getMap(MapID id) {
		logger.info("Building map " + id + "...");
		String entryRegex = "(?<=\n\n)<<(?s:.*?)(?=\n\n<<|\n{0,3}$)";
		String contentRegex = "(?<=>>\n)(?s:.*+)$";
		String absentRegex = "(?<=>>)(?=\n)";

		File japaneseFile;
		File englishFile;
		if (id == MapID.DATABASE) {
			japaneseFile = new File(directory, "DatabaseTextJapanese.rvtext");
			englishFile = new File(directory, "DatabaseTextEnglish.rvtext");
		} else if (id == MapID.DIALOGUES) {
			japaneseFile = new File(directory, "DialoguesJapanese.rvtext");
			englishFile = new File(directory, "DialoguesEnglish.rvtext");
		} else if (id == MapID.SCRIPT_TEXT) {
			japaneseFile = new File(directory, "ScriptTextJapanese.rvtext");
			englishFile = new File(directory, "ScriptTextEnglish.rvtext");
		} else {
			throw new RuntimeException("Unmanaged map id: " + id);
		}

		Field<String> idField = new Field<>("ID");
		String idRegex = "(?<=<<).*(?=>>)";

		logger.fine("Loading Japanese...");
		PatternFileMap japaneseMap = new PatternFileMap(japaneseFile,
				entryRegex, contentRegex, absentRegex);
		japaneseMap.addFieldRegex(idField, idRegex, false);
		logger.fine("Japanese loaded: " + japaneseMap.size() + " entries.");

		logger.fine("Loading English...");
		PatternFileMap englishMap = new PatternFileMap(englishFile, entryRegex,
				absentRegex, contentRegex);
		englishMap.addFieldRegex(idField, idRegex, false);
		logger.fine("English loaded: " + englishMap.size() + " entries.");

		int size = Math.min(japaneseMap.size(), englishMap.size());
		for (int i = 0; i < size; i++) {
			PatternEntry jap = japaneseMap.getEntry(i);
			PatternEntry eng = englishMap.getEntry(i);
			String idJap = jap.getMetadata().get(idField);
			String idEng = eng.getMetadata().get(idField);

			String equal = "";
			if (idJap.equals(idEng)) {
				equal += "O";
			} else {
				equal += "X";
			}

			System.out.println(equal + " " + idJap + " > " + idEng);
		}

		return new MGQMap(japaneseMap, englishMap);
	}

	@Override
	public String getMapName(MapID id) {
		return id.name();
	}

	@Override
	public int size() {
		return MapID.values().length;
	}

	@Override
	public void saveAll() {
		// TODO Auto-generated method stub
		throw new NotImplementedException("Not implemented yet.");
	}

	@Override
	public void resetAll() {
		// TODO Auto-generated method stub
		throw new NotImplementedException("Not implemented yet.");
	}

	@Override
	public Collection<Feature> getFeatures() {
		return Collections.emptyList();
	}

	public static class MGQMap implements TranslationMap<MGQEntry> {

		private final PatternFileMap japaneseMap;
		private final PatternFileMap englishMap;

		public MGQMap(PatternFileMap japaneseMap, PatternFileMap englishMap) {
			this.japaneseMap = japaneseMap;
			this.englishMap = englishMap;
		}

		@Override
		public Iterator<MGQEntry> iterator() {
			return new Iterator<MGQEntry>() {

				private final int maxIndex = englishMap.size() - 1;
				private int index = -1;

				@Override
				public boolean hasNext() {
					return index < maxIndex;
				}

				@Override
				public MGQEntry next() {
					if (hasNext()) {
						index++;
						return getEntry(index);
					} else {
						throw new NoSuchElementException();
					}
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException(
							"You cannot remove entries from this map.");
				}
			};
		}

		@Override
		public MGQEntry getEntry(int index) {
			PatternEntry japaneseEntry = japaneseMap.getEntry(index);
			PatternEntry englishEntry = englishMap.getEntry(index);
			return new MGQEntry(japaneseEntry, englishEntry);
		}

		@Override
		public int size() {
			return englishMap.size();
		}

		@Override
		public void saveAll() {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public void resetAll() {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public Collection<EntryFilter<MGQEntry>> getEntryFilters() {
			return Collections.emptyList();
		}

	}

	public static class MGQEntry implements TranslationEntry<MGQMetadata> {

		private final PatternEntry japaneseEntry;
		private final PatternEntry englishEntry;

		public MGQEntry(PatternEntry japaneseEntry, PatternEntry englishEntry) {
			this.japaneseEntry = japaneseEntry;
			this.englishEntry = englishEntry;
		}

		@Override
		public String getOriginalContent() {
			return japaneseEntry.getOriginalContent();
		}

		@Override
		public String getStoredTranslation() {
			return englishEntry.getStoredTranslation();
		}

		@Override
		public String getCurrentTranslation() {
			return englishEntry.getCurrentTranslation();
		}

		@Override
		public void setCurrentTranslation(String translation) {
			englishEntry.setCurrentTranslation(translation);
		}

		@Override
		public void saveTranslation() {
			englishEntry.saveTranslation();
		}

		@Override
		public void resetTranslation() {
			englishEntry.resetTranslation();
		}

		@Override
		public void saveAll() {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public void resetAll() {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public MGQMetadata getMetadata() {
			// TODO Auto-generated method stub
			return new MGQMetadata();
		}

		@Override
		public void addTranslationListener(TranslationListener listener) {
			englishEntry.addTranslationListener(listener);
		}

		@Override
		public void removeTranslationListener(TranslationListener listener) {
			englishEntry.removeTranslationListener(listener);
		}

	}

	public static class MGQMetadata implements TranslationMetadata {

		@Override
		public Iterator<Field<?>> iterator() {
			// TODO Auto-generated method stub
			return Collections.<Field<?>> emptyList().iterator();
		}

		@Override
		public <T> T getStored(Field<T> field) {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public <T> T get(Field<T> field) {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public <T> boolean isEditable(Field<T> field) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public <T> void set(Field<T> field, T value)
				throws UneditableFieldException {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public void addFieldListener(FieldListener listener) {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public void removeFieldListener(FieldListener listener) {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public <T> void save(Field<T> field) {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public <T> void reset(Field<T> field) {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public void saveAll() {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

		@Override
		public void resetAll() {
			// TODO Auto-generated method stub
			throw new NotImplementedException("Not implemented yet.");
		}

	}
}
