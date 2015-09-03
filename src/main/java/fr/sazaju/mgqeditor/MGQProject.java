package fr.sazaju.mgqeditor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.NotImplementedException;

import fr.sazaju.mgqeditor.MGQProject.MGQEntry;
import fr.sazaju.mgqeditor.MGQProject.MGQMap;
import fr.sazaju.mgqeditor.MGQProject.MapID;
import fr.vergne.translation.TranslationEntry;
import fr.vergne.translation.TranslationMap;
import fr.vergne.translation.TranslationMetadata;
import fr.vergne.translation.TranslationMetadata.Field;
import fr.vergne.translation.TranslationProject;
import fr.vergne.translation.impl.NoTranslationFilter;
import fr.vergne.translation.impl.PatternFileMap;
import fr.vergne.translation.impl.PatternFileMap.PatternEntry;
import fr.vergne.translation.util.EntryFilter;
import fr.vergne.translation.util.Feature;
import fr.vergne.translation.util.MapNamer;

public class MGQProject implements TranslationProject<MGQEntry, MapID, MGQMap> {

	private static final Logger logger = Logger.getLogger(MGQProject.class
			.getName());
	public static final String REGEX_ENTRY = "(?<=\n{1,2})<<(?s:.*?)(?=\n\n<<|\n{0,3}$)";
	public static final String REGEX_CONTENT = "(?<=>>\n)(?s:.*+)$";
	public static final String REGEX_ABSENT = "(?<=>>)(?=\n)";
	public static final String REGEX_ID = "(?<=<<).*(?=>>)";

	public static enum MapID {
		DATABASE, DIALOGUES, SCRIPT_TEXT
	}

	private final File directory;
	private final Collection<MapNamer<MapID>> mapNamers;
	private final Collection<EntryFilter<MGQEntry>> filters;
	private final Field<String> idField = new Field<>("ID");

	public MGQProject(File directory) {
		logger.info("Creating Project on " + directory);
		this.directory = directory;
		this.mapNamers = new LinkedHashSet<>();
		this.mapNamers.add(new MapNamer<MapID>() {

			@Override
			public String getName() {
				return "Type";
			}

			@Override
			public String getDescription() {
				return "Use the type of data stored in the map to name it.";
			}

			@Override
			public String getNameFor(MapID id) {
				return id.name();
			}
		});

		this.filters = new LinkedList<EntryFilter<MGQEntry>>();
		this.filters.add(new NoTranslationFilter<MGQEntry>());
	}

	@Override
	public Iterator<MapID> iterator() {
		return Arrays.asList(MapID.values()).iterator();
	}

	@Override
	public MGQMap getMap(MapID id) {
		logger.info("Building map " + id + "...");

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

		logger.fine("Loading Japanese...");
		PatternFileMap japaneseMap = new PatternFileMap(japaneseFile,
				REGEX_ENTRY, REGEX_CONTENT, REGEX_ABSENT);
		japaneseMap.addFieldRegex(idField, REGEX_ID, false);
		logger.fine("Japanese loaded: " + japaneseMap.size() + " entries.");

		logger.fine("Loading English...");
		PatternFileMap englishMap = new PatternFileMap(englishFile, REGEX_ENTRY,
				REGEX_ABSENT, REGEX_CONTENT);
		englishMap.addFieldRegex(idField, REGEX_ID, false);
		logger.fine("English loaded: " + englishMap.size() + " entries.");

		List<String> sortedIds = new ArrayList<>(japaneseMap.size());
		Map<String, PatternEntry> japaneseEntries = new HashMap<>();
		for (PatternEntry entry : japaneseMap) {
			String entryId = entry.getMetadata().get(idField);
			sortedIds.add(entryId);
			japaneseEntries.put(entryId, entry);
		}

		Map<String, PatternEntry> englishEntries = new HashMap<>();
		for (PatternEntry entry : englishMap) {
			String entryId = entry.getMetadata().get(idField);
			if (japaneseEntries.containsKey(entryId)) {
				englishEntries.put(entryId, entry);
			} else {
				// obsolete English entry
			}
		}

		return new MGQMap(sortedIds, japaneseEntries, englishEntries);
	}

	@Override
	public Collection<MapNamer<MapID>> getMapNamers() {
		return mapNamers;
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

	@Override
	public Collection<EntryFilter<MGQEntry>> getEntryFilters() {
		return filters;
	}

	public class MGQMap implements TranslationMap<MGQEntry> {

		private final List<String> sortedIds;
		private final Map<String, PatternEntry> japaneseEntries;
		private final Map<String, PatternEntry> englishEntries;

		public MGQMap(List<String> sortedIds,
				Map<String, PatternEntry> japaneseEntries,
				Map<String, PatternEntry> englishEntries) {
			this.sortedIds = sortedIds;
			this.japaneseEntries = japaneseEntries;
			this.englishEntries = englishEntries;
		}

		@Override
		public Iterator<MGQEntry> iterator() {
			return new Iterator<MGQEntry>() {

				private final Iterator<String> iterator = sortedIds.iterator();

				@Override
				public boolean hasNext() {
					return iterator.hasNext();
				}

				@Override
				public MGQEntry next() {
					return getEntry(iterator.next());
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
			return getEntry(sortedIds.get(index));
		}

		private MGQEntry getEntry(String id) {
			PatternEntry japaneseEntry = japaneseEntries.get(id);
			PatternEntry englishEntry = englishEntries.get(id);
			return new MGQEntry(japaneseEntry, englishEntry);
		}

		@Override
		public int size() {
			return sortedIds.size();
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

	public class MGQEntry implements TranslationEntry<MGQMetadata> {

		private final PatternEntry japaneseEntry;
		private final PatternEntry englishEntry;

		public MGQEntry(PatternEntry japaneseEntry, PatternEntry englishEntry) {
			if (englishEntry == null) {
				throw new IllegalArgumentException("No English entry for "
						+ japaneseEntry.getMetadata().get(idField));
			} else {
				this.japaneseEntry = japaneseEntry;
				this.englishEntry = englishEntry;
			}
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
