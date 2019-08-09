package org.snomed.snowstorm.fhir.services;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.ValueSet.ConceptReferenceDesignationComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.snomed.snowstorm.core.data.domain.*;
import org.snomed.snowstorm.fhir.config.FHIRConstants;

public class HapiValueSetMapper implements FHIRConstants {
	
	public ValueSet mapToFHIR(ValueSet vs, List<ConceptMini> concepts, String url, Map<String, Concept> conceptDetails, List<String> languageCodes, String displayLanguage, Boolean includeDesignations) {
		if (vs == null) {
			vs = getStandardValueSet(url);
		}
		addExpansion(vs, concepts, conceptDetails, languageCodes, displayLanguage, includeDesignations);
		return vs;
	}
	
	private ValueSet getStandardValueSet(String url) {
		ValueSet v = new ValueSet();
		v.setUrl(url);
		return v;
	}

	private void addExpansion(ValueSet vs, List<ConceptMini> concepts, Map<String, Concept> conceptDetails, List<String> languageCodes, String displayLanguage, Boolean includeDesignations) {
		ValueSetExpansionComponent expansion = vs.getExpansion();  //Will autocreate
		for (ConceptMini concept : concepts) {
			ValueSetExpansionContainsComponent component = expansion.addContains()
				.setCode(concept.getConceptId())
				.setDisplay(concept.getFsnTerm())
				.setSystem(SNOMED_URI);
			
			if (conceptDetails != null && conceptDetails.containsKey(concept.getConceptId())) {
				Concept c = conceptDetails.get(concept.getConceptId());
				for (Description d : c.getDescriptions(true, null, null, null)) {
					if (includeDesignations && languageCodes.contains(d.getLanguageCode())) {
						component.addDesignation(asDesignation(d));
					}
					//Are we mising the FSN in the conceptMini?
					if (component.getDisplay() == null && d.getTypeId().equals(Concepts.FSN)) {
						component.setDisplay(d.getTerm());
					}
					
					//Was request for a specific display language?
					//Use the preferred term in that language if so.
					if (d.getLanguageCode().equalsIgnoreCase(displayLanguage) &&
							d.hasAcceptability(Concepts.PREFERRED) &&
							d.getTypeId().equals(Concepts.SYNONYM)) {
						component.setDisplay(d.getTerm());
					}
				}
			}
		}
	}


	private ConceptReferenceDesignationComponent asDesignation(Description d) {
		ConceptReferenceDesignationComponent designation = new ConceptReferenceDesignationComponent();
		designation.setLanguage(d.getLanguageCode());
		designation.setValue(d.getTerm());
		Coding use = new Coding(SNOMED_URI, d.getTypeId(), FHIRHelper.translateDescType(d.getTypeId()));
		designation.setUse(use);
		return designation;
	}
}
