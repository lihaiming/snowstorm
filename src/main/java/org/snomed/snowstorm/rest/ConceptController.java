package org.snomed.snowstorm.rest;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonView;
import io.kaicode.elasticvc.api.BranchCriteria;
import io.kaicode.elasticvc.api.VersionControlHelper;
import io.kaicode.rest.util.branchpathrewrite.BranchPathUriUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.elasticsearch.common.Strings;
import org.snomed.snowstorm.core.data.domain.Concept;
import org.snomed.snowstorm.core.data.domain.ConceptMini;
import org.snomed.snowstorm.core.data.domain.ConceptView;
import org.snomed.snowstorm.core.data.domain.Relationship;
import org.snomed.snowstorm.core.data.domain.expression.Expression;
import org.snomed.snowstorm.core.data.services.*;
import org.snomed.snowstorm.core.data.services.pojo.AsyncConceptChangeBatch;
import org.snomed.snowstorm.core.data.services.pojo.MapPage;
import org.snomed.snowstorm.core.data.services.pojo.ResultMapPage;
import org.snomed.snowstorm.core.pojo.BranchTimepoint;
import org.snomed.snowstorm.core.util.PageHelper;
import org.snomed.snowstorm.core.util.TimerUtil;
import org.snomed.snowstorm.rest.converter.SearchAfterHelper;
import org.snomed.snowstorm.rest.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchAfterPage;
import org.springframework.data.elasticsearch.core.SearchAfterPageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.util.*;

import static io.kaicode.elasticvc.api.ComponentService.LARGE_PAGE;
import static org.snomed.snowstorm.core.pojo.BranchTimepoint.BRANCH_CREATION_TIMEPOINT;
import static org.snomed.snowstorm.rest.ControllerHelper.parseBranchTimepoint;

@RestController
@Api(tags = "Concepts", description = "-")
@RequestMapping(produces = "application/json")
public class ConceptController {

	@Autowired
	private ConceptService conceptService;

	@Autowired
	private RelationshipService relationshipService;

	@Autowired
	private QueryService queryService;

	@Autowired
	private SemanticIndexService semanticIndexService;

	@Autowired
	private ExpressionService expressionService;

	@Autowired
	private VersionControlHelper versionControlHelper;

	@Value("${snowstorm.rest-api.allowUnlimitedConceptPagination:false}")
	private boolean allowUnlimitedConceptPagination;

	@RequestMapping(value = "/{branch}/concepts", method = RequestMethod.GET, produces = {"application/json", "text/csv"})
	@ResponseBody
	public ItemsPage<ConceptMini> findConcepts(
			@PathVariable String branch,
			@RequestParam(required = false) Boolean activeFilter,
			@RequestParam(required = false) String definitionStatusFilter,
			@RequestParam(required = false) String term,
			@RequestParam(required = false) Boolean termActive,
			@RequestParam(required = false) String ecl,
			@RequestParam(required = false) String statedEcl,
			@RequestParam(required = false) Set<String> conceptIds,
			@RequestParam(required = false, defaultValue = "0") int offset,
			@RequestParam(required = false, defaultValue = "50") int limit,
			@RequestParam(required = false) String searchAfter,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		// Parameter validation
		int logicalMethods = 0;
		if (ecl != null) {
			logicalMethods++;
		}
		if (statedEcl != null) {
			logicalMethods++;
		}
		if (conceptIds != null) {
			logicalMethods++;
		}
		if (logicalMethods > 1) {
			throw new IllegalArgumentException("Parameters ecl, statedEcl and conceptIds can not be combined.");
		}

		if ((ecl != null || statedEcl != null) && activeFilter != null && !activeFilter) {
			throw new IllegalArgumentException("ECL search can not be used on inactive concepts.");
		}

		boolean stated = true;
		if (ecl != null && !ecl.isEmpty()) {
			stated = false;
		} else {
			ecl = statedEcl;
		}

		QueryService.ConceptQueryBuilder queryBuilder = queryService.createQueryBuilder(stated)
				.activeFilter(activeFilter)
				.termActive(termActive)
				.definitionStatusFilter(definitionStatusFilter)
				.ecl(ecl)
				.termMatch(term)
				.languageCodes(ControllerHelper.getLanguageCodes(acceptLanguageHeader))
				.conceptIds(conceptIds);

		ControllerHelper.validatePageSize(offset, limit);

		return new ItemsPage<>(queryService.search(queryBuilder, BranchPathUriUtil.decodePath(branch), ControllerHelper.getPageRequest(offset, searchAfter, limit)));
	}

	@RequestMapping(value = "/{branch}/concepts/{conceptId}", method = RequestMethod.GET, produces = {"application/json", "text/csv"})
	@ResponseBody
	public ConceptMini findConcept(
			@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		ResultMapPage<String, ConceptMini> conceptMinis = conceptService.findConceptMinis(BranchPathUriUtil.decodePath(branch), Collections.singleton(conceptId), ControllerHelper.getLanguageCodes(acceptLanguageHeader));
		ConceptMini concept = conceptMinis.getTotalElements() > 0 ? conceptMinis.getResultsMap().values().iterator().next() : null;
		return ControllerHelper.throwIfNotFound("Concept", concept);
	}

	@RequestMapping(value = "/{branch}/concepts/search", method = RequestMethod.POST, produces = {"application/json", "text/csv"})
	@ResponseBody
	public ItemsPage<ConceptMini> search(
			@PathVariable String branch,
			@RequestBody ConceptSearchRequest searchRequest,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		return findConcepts(BranchPathUriUtil.decodePath(branch),
				searchRequest.getActiveFilter(),
				searchRequest.getDefinitionStatusFilter(),
				searchRequest.getTermFilter(),
				searchRequest.getTermActive(),
				searchRequest.getEclFilter(),
				searchRequest.getStatedEclFilter(),
				searchRequest.getConceptIds(),
				searchRequest.getOffset(),
				searchRequest.getLimit(),
				searchRequest.getSearchAfter(),
				acceptLanguageHeader);
	}

	@ApiOperation(value = "Load concepts in the browser format.",
			notes = "When enabled 'searchAfter' can be used for unlimited pagination. " +
					"Load the first page then take the 'searchAfter' value from the response and use that " +
					"as a parameter in the next page request instead of 'number'.")
	@RequestMapping(value = "/browser/{branch}/concepts", method = RequestMethod.GET)
	@ResponseBody
	@JsonView(value = View.Component.class)
	public ItemsPage<Concept> getBrowserConcepts(
			@PathVariable String branch,
			@RequestParam(required = false) List<Long> conceptIds,
			@RequestParam(defaultValue = "0") int number,
			@RequestParam(defaultValue = "100") int size,
			@RequestParam(required = false) String searchAfter,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		PageRequest pageRequest;
		Integer conceptIdsSize = conceptIds != null ? conceptIds.size() : null;
		if (!Strings.isNullOrEmpty(searchAfter)) {
			if (!allowUnlimitedConceptPagination) {
				throw new IllegalArgumentException("Unlimited pagination of the full concept representation is disabled in this deployment.");
			}
			pageRequest = SearchAfterPageRequest.of(SearchAfterHelper.fromSearchAfterToken(searchAfter), size);
		} else {
			pageRequest = PageRequest.of(number, size);
			ControllerHelper.validatePageSize(pageRequest.getOffset(), pageRequest.getPageSize());
			conceptIds = PageHelper.subList(conceptIds, number, size);
		}

		Page<Concept> page = conceptService.find(conceptIds, ControllerHelper.getLanguageCodes(acceptLanguageHeader), BranchPathUriUtil.decodePath(branch), pageRequest);
		SearchAfterPage<Concept> concepts = PageHelper.toSearchAfterPage(page, concept -> SearchAfterHelper.convertToTokenAndBack(new Object[]{concept.getId()}), conceptIdsSize);
		return new ItemsPage<>(concepts);
	}

	@RequestMapping(value = "/browser/{branch}/concepts/bulk-load", method = RequestMethod.POST)
	@ResponseBody
	@JsonView(value = View.Component.class)
	public Collection<Concept> getBrowserConcepts(
			@PathVariable String branch,
			@RequestBody ConceptIdsPojo request,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		return conceptService.find(BranchPathUriUtil.decodePath(branch), request.getConceptIds(), ControllerHelper.getLanguageCodes(acceptLanguageHeader));
	}

	@ApiOperation(value = "Load a concept in the browser format.",
			notes = "During content authoring previous versions of the concept can be loaded from version control.\n" +
					"To do this use the branch path format {branch@" + BranchTimepoint.DATE_FORMAT_STRING + "} or {branch@epoch_milliseconds}.\n" +
					"The version of the concept when the branch was created can be loaded using {branch@" + BRANCH_CREATION_TIMEPOINT + "}.")
	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts/{conceptId}", method = RequestMethod.GET)
	@JsonView(value = View.Component.class)
	public ConceptView findBrowserConcept(
			@PathVariable String branch,
			@PathVariable String conceptId,
			@ApiParam("If this parameter is set a descendantCount will be included in the response using stated/inferred as requested.")
			@RequestParam(required = false) Relationship.CharacteristicType descendantCountForm,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		List<String> languageCodes = ControllerHelper.getLanguageCodes(acceptLanguageHeader);
		BranchTimepoint branchTimepoint = parseBranchTimepoint(branch);
		Concept concept = conceptService.find(conceptId, languageCodes, branchTimepoint);
		if (descendantCountForm != null) {
			queryService.joinDescendantCount(concept, descendantCountForm, languageCodes, branchTimepoint);
		}
		return ControllerHelper.throwIfNotFound("Concept", concept);
	}

	@ResponseBody
	@RequestMapping(value = "/{branch}/concepts/{conceptId}/descriptions", method = RequestMethod.GET)
	@JsonView(value = View.Component.class)
	public ConceptDescriptionsResult findConceptDescriptions(
			@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		Concept concept = ControllerHelper.throwIfNotFound("Concept", conceptService.find(conceptId, ControllerHelper.getLanguageCodes(acceptLanguageHeader), BranchPathUriUtil.decodePath(branch)));
		return new ConceptDescriptionsResult(concept.getDescriptions());
	}

	@ResponseBody
	@RequestMapping(value = "/{branch}/concepts/{conceptId}/descendants", method = RequestMethod.GET)
	@JsonView(value = View.Component.class)
	public ItemsPage<ConceptMini> findConceptDescendants(@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestParam(required = false, defaultValue = "false") boolean stated,
			@RequestParam(required = false, defaultValue = "0") int offset,
			@RequestParam(required = false, defaultValue = "50") int limit,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		String ecl = "<" + conceptId;
		return findConcepts(branch, null, null, null, null, !stated ? ecl : null, stated ? ecl : null, null, offset, limit, null, acceptLanguageHeader);
	}

	@ResponseBody
	@RequestMapping(value = "/{branch}/concepts/{conceptId}/inbound-relationships", method = RequestMethod.GET)
	@JsonView(value = View.Component.class)
	public InboundRelationshipsResult findConceptInboundRelationships(@PathVariable String branch, @PathVariable String conceptId) {
		List<Relationship> inboundRelationships = relationshipService.findInboundRelationships(conceptId, BranchPathUriUtil.decodePath(branch), null).getContent();
		return new InboundRelationshipsResult(inboundRelationships);
	}

	@ApiOperation(value = "Find concepts which reference this concept in the inferred or stated form (including stated axioms).",
			notes = "Pagination works on the referencing concepts. A referencing concept may have one or more references of different types.")
	@ResponseBody
	@RequestMapping(value = "/{branch}/concepts/{conceptId}/references", method = RequestMethod.GET)
	public ConceptReferencesResult findConceptReferences(
			@PathVariable String branch,
			@PathVariable Long conceptId,
			@RequestParam(defaultValue = "false") boolean stated,
			@RequestParam(required = false, defaultValue = "0") int offset,
			@RequestParam(required = false, defaultValue = "1000") int limit,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		branch = BranchPathUriUtil.decodePath(branch);
		ControllerHelper.validatePageSize(offset, limit);

		MapPage<Long, Set<Long>> conceptReferencesPage = semanticIndexService.findConceptReferences(branch, conceptId, stated, ControllerHelper.getPageRequest(offset, limit));
		Map<Long, Set<Long>> conceptReferences = conceptReferencesPage.getMap();

		// Join concept minis with FSN and PT
		Set<Long> allConceptIds = new LongOpenHashSet();
		for (Long typeId : conceptReferences.keySet()) {
			allConceptIds.add(typeId);
			allConceptIds.addAll(conceptReferences.get(typeId));
		}
		Map<String, ConceptMini> conceptMiniMap = conceptService.findConceptMinis(branch, allConceptIds, ControllerHelper.getLanguageCodes(acceptLanguageHeader)).getResultsMap();
		Set<TypeReferences> typeSets = new TreeSet<>(Comparator.comparing((type) -> type.getReferenceType().getFsnTerm()));
		for (Long typeId : conceptReferences.keySet()) {
			ArrayList<ConceptMini> referencingConcepts = new ArrayList<>();
			typeSets.add(new TypeReferences(conceptMiniMap.get(typeId.toString()), referencingConcepts));
			for (Long referencingConceptId : conceptReferences.get(typeId)) {
				referencingConcepts.add(conceptMiniMap.get(referencingConceptId.toString()));
			}
		}
		return new ConceptReferencesResult(typeSets, conceptReferencesPage.getPageable(), conceptReferencesPage.getTotalElements());
	}

	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts/{conceptId}", method = RequestMethod.PUT)
	@JsonView(value = View.Component.class)
	public ConceptView updateConcept(
			@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestBody @Valid ConceptView concept,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) throws ServiceException {

		Assert.isTrue(concept.getConceptId() != null && conceptId != null && concept.getConceptId().equals(conceptId), "The conceptId in the " +
				"path must match the one in the request body.");
		return conceptService.update((Concept) concept, ControllerHelper.getLanguageCodes(acceptLanguageHeader), BranchPathUriUtil.decodePath(branch));
	}

	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts", method = RequestMethod.POST)
	@JsonView(value = View.Component.class)
	public ConceptView createConcept(
			@PathVariable String branch,
			@RequestBody @Valid ConceptView concept,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) throws ServiceException {

		return conceptService.create((Concept) concept, ControllerHelper.getLanguageCodes(acceptLanguageHeader), BranchPathUriUtil.decodePath(branch));
	}

	@RequestMapping(value = "/{branch}/concepts/{conceptId}", method = RequestMethod.DELETE)
	public void deleteConcept(@PathVariable String branch, @PathVariable String conceptId) {
		conceptService.deleteConceptAndComponents(conceptId, BranchPathUriUtil.decodePath(branch), false);
	}

	@ApiOperation(value = "Start a bulk concept change.", notes = "Concepts can be created or updated using this endpoint.")
	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts/bulk", method = RequestMethod.POST)
	public ResponseEntity createConceptBulkChange(@PathVariable String branch, @RequestBody @Valid List<ConceptView> concepts, UriComponentsBuilder uriComponentsBuilder) {
		List<Concept> conceptList = new ArrayList<>();
		concepts.forEach(conceptView -> conceptList.add((Concept) conceptView));
		AsyncConceptChangeBatch batchConceptChange = new AsyncConceptChangeBatch();
		conceptService.createUpdateAsync(conceptList, BranchPathUriUtil.decodePath(branch), batchConceptChange, SecurityContextHolder.getContext());
		return ResponseEntity.created(uriComponentsBuilder.path("/browser/{branch}/concepts/bulk/{bulkChangeId}")
				.buildAndExpand(branch, batchConceptChange.getId()).toUri()).build();
	}

	@ApiOperation("Fetch the status of a bulk concept creation or update.")
	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts/bulk/{bulkChangeId}", method = RequestMethod.GET)
	public AsyncConceptChangeBatch getConceptBulkChange(@PathVariable String branch, @PathVariable String bulkChangeId) {
		return ControllerHelper.throwIfNotFound("Bulk Change", conceptService.getBatchConceptChange(bulkChangeId));
	}

	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts/{conceptId}/children", method = RequestMethod.GET)
	@JsonView(value = View.Component.class)
	public Collection<ConceptMini> findConceptChildren(@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestParam(defaultValue = "inferred") Relationship.CharacteristicType form,
			@RequestParam(required = false, defaultValue = "false") Boolean includeDescendantCount,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) throws ServiceException {

		List<String> languageCodes = ControllerHelper.getLanguageCodes(acceptLanguageHeader);
		String branchPath = BranchPathUriUtil.decodePath(branch);

		TimerUtil timer = new TimerUtil("Child listing: " + conceptId, Level.INFO, 5);

		QueryService.ConceptQueryBuilder conceptQuery = queryService.createQueryBuilder(form)
				.ecl("<!" + conceptId)
				.languageCodes(languageCodes);
		List<ConceptMini> children = queryService.search(conceptQuery, branchPath, LARGE_PAGE).getContent();
		timer.checkpoint("Find children");

		BranchCriteria branchCriteria = versionControlHelper.getBranchCriteria(branchPath);
		if (!includeDescendantCount) {
			queryService.joinIsLeafFlag(children, form, branchCriteria);
			timer.checkpoint("Join leaf flag");
		} else {
			queryService.joinDescendantCountAndLeafFlag(children, form, branchPath, branchCriteria);
			timer.checkpoint("Join descendant count and leaf flag");
		}
		return children;
	}

	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts/{conceptId}/parents", method = RequestMethod.GET)
	@JsonView(value = View.Component.class)
	public Collection<ConceptMini> findConceptParents(@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestParam(defaultValue = "inferred") Relationship.CharacteristicType form,
			@RequestParam(required = false, defaultValue = "false") Boolean includeDescendantCount,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) throws ServiceException {

		branch = BranchPathUriUtil.decodePath(branch);
		List<String> languageCodes = ControllerHelper.getLanguageCodes(acceptLanguageHeader);

		BranchCriteria branchCriteria = versionControlHelper.getBranchCriteria(branch);
		Set<Long> parentIds = queryService.findParentIds(branchCriteria, form == Relationship.CharacteristicType.stated, conceptId);
		Collection<ConceptMini> parents = conceptService.findConceptMinis(branchCriteria, parentIds, languageCodes).getResultsMap().values();
		if (includeDescendantCount) {
			queryService.joinDescendantCountAndLeafFlag(parents, form, branch, branchCriteria);
		}
		return parents;
	}

	@ResponseBody
	@RequestMapping(value = "/browser/{branch}/concepts/{conceptId}/ancestors", method = RequestMethod.GET)
	@JsonView(value = View.Component.class)
	public Collection<ConceptMini> findConceptAncestors(@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestParam(defaultValue = "inferred") Relationship.CharacteristicType form,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		String branchPath = BranchPathUriUtil.decodePath(branch);
		Set<Long> ancestorIds = queryService.findAncestorIds(conceptId, branchPath, form == Relationship.CharacteristicType.stated);
		return conceptService.findConceptMinis(branchPath, ancestorIds, ControllerHelper.getLanguageCodes(acceptLanguageHeader)).getResultsMap().values();
	}

	@ResponseBody
	@RequestMapping(value = "/{branch}/concepts/{conceptId}/authoring-form", method = RequestMethod.GET)
	public Expression getConceptAuthoringForm(
			@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		List<String> languageCodes = ControllerHelper.getLanguageCodes(acceptLanguageHeader);
		return expressionService.getConceptAuthoringForm(conceptId, languageCodes, BranchPathUriUtil.decodePath(branch));
	}
	
	@ResponseBody
	@RequestMapping(value = "/{branch}/concepts/{conceptId}/normal-form", method = RequestMethod.GET)
	public ExpressionStringPojo getConceptNormalForm(
			@PathVariable String branch,
			@PathVariable String conceptId,
			@RequestParam(defaultValue="false") boolean statedView,
			@RequestParam(defaultValue="false") boolean includeTerms,
			@RequestHeader(value = "Accept-Language", defaultValue = ControllerHelper.DEFAULT_ACCEPT_LANG_HEADER) String acceptLanguageHeader) {

		List<String> languageCodes = ControllerHelper.getLanguageCodes(acceptLanguageHeader);
		Expression expression =  expressionService.getConceptNormalForm(conceptId, languageCodes, BranchPathUriUtil.decodePath(branch), statedView);
		return new ExpressionStringPojo(expression.toString(includeTerms));
	}

}
