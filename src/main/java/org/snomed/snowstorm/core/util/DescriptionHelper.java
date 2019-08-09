package org.snomed.snowstorm.core.util;

import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.snomed.snowstorm.core.data.domain.Concepts;
import org.snomed.snowstorm.core.data.domain.Description;
import org.snomed.snowstorm.core.pojo.TermLangPojo;

import java.util.*;
import java.util.function.Predicate;

public class DescriptionHelper {

	public static final Set<String> EN_LANGUAGE_CODE = Collections.singleton("en");

	public static TermLangPojo getFsnDescriptionTermAndLang(Set<Description> descriptions, Collection<String> languageCodes) {
		return getFsnDescription(descriptions, languageCodes).map(d -> new TermLangPojo(d.getTerm(), d.getLang())).orElse(new TermLangPojo());
	}

	public static Optional<Description> getFsnDescription(Set<Description> descriptions, Collection<String> languageCodes) {
		return Optional.ofNullable(getBestDescription(descriptions, languageCodes, description -> Concepts.FSN.equals(description.getTypeId())));
	}

	public static TermLangPojo getPtDescriptionTermAndLang(Set<Description> descriptions, Collection<String> languageCodes) {
		return getPtDescription(descriptions, languageCodes).map(d -> new TermLangPojo(d.getTerm(), d.getLang())).orElse(new TermLangPojo());
	}

	public static Optional<Description> getPtDescription(Set<Description> descriptions, Collection<String> languageCodes) {
		return Optional.ofNullable(getBestDescription(descriptions, languageCodes, description -> Concepts.SYNONYM.equals(description.getTypeId()) &&
				// Preferred in any language refset
				description.getAcceptabilityMap().values().contains(Concepts.PREFERRED_CONSTANT)));
	}

	private static Description getBestDescription(Set<Description> descriptions, Collection<String> languageCodes, Predicate<Description> descriptionPredicate) {
		Map<String, Description> descriptionsByLanguageCode = new HashMap<>();
		for (Description description : descriptions) {
			if (description.isActive() && descriptionPredicate.test(description)) {
				descriptionsByLanguageCode.put(description.getLanguageCode(), description);
			}
		}
		if (languageCodes != null) {
			for (String languageCode : languageCodes) {
				if (descriptionsByLanguageCode.containsKey(languageCode)) {
					return descriptionsByLanguageCode.get(languageCode);
				}
			}
		}
		return null;
	}

	public static String foldTerm(String term, Set<Character> charactersNotFolded) {
		if (charactersNotFolded == null) {
			return term;
		}
		char[] chars = term.toLowerCase().toCharArray();
		char[] charsFolded = new char[chars.length * 2];

		// Fold all characters
		int charsFoldedOffset = 0;
		try {
			for (int i = 0; i < chars.length; i++) {
				if (charactersNotFolded.contains(chars[i])) {
					charsFolded[charsFoldedOffset] = chars[i];
				} else {
					int length = ASCIIFoldingFilter.foldToASCII(chars, i, charsFolded, charsFoldedOffset, 1);
					if (length != charsFoldedOffset + 1) {
						charsFoldedOffset = length - 1;
					}
				}
				charsFoldedOffset++;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw e;
		}
		return new String(charsFolded, 0, charsFoldedOffset);
	}

}
