# Changelog
All notable changes to this project will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). The change log format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).


## 4.5.0 Release - 2019-11-20

Some small features and enhancements for the community and to support an update to the SNOMED International public browser.

### Features
- Pull request #85 New config flag to make concept bulk-load accessible when in read-only mode.
- API to list all active semantic tags including concept count.
- Descendants count option on concept, children and parents API endpoints.
- Type filter on Description search.
- API for deletion of single descriptions and relationships including force flag.
### Improvements
- Updates to documentation on extension management.
- FHIR API
  - Make JSON the default response.
  - Support expansion examination of the "compose" element.
- Fix #80 Human readable error when Snowstorm fails to connect to Elastic.
- Prevent description aggregation requests which take a very long time.
- Add conceptIds parameter to browser concept list API.
- Upgrade SNOMED Drools Engine.
### Fixes
- Fix URL mapping for bulk concept validation.
- Fix delta import marking unpublished reference set members as released.



## 4.4.0 Release - 2019-10-11 - International Authoring platform using Snowstorm

Since 4.1.0 we have made many minor and patch releases during preparation for another Snowstorm milestone.
I am very pleased to announce that we have now gone live with Snowstorm as the Terminology Server for the SNOMED International Edition Authoring Platform!

As usual we have also had plenty of engagement from the community with many questions, issues and pull requests coming through. Thank you.

Please note the new approach to importing and upgrading extensions. The Code System _migrate_ function is now deprecated in favour of the new _upgrade_ function. 
Code System branches should be created directly under `MAIN` rather than under a version branch. For example `MAIN/SNOMEDCT-US`. 
Using the _upgrade_ function Snowstorm will rebase the Code System branch to the a specific point on the timeline of the parent branch where the requested version 
was created, without having to use release branches like `MAIN/2019-07-31`.

I hope you find this release useful and as always please just reach out or raise an issue if you have questions.

### Features
- New approach to upgrading extensions using Code System upgrade function. (_Migrate_ function now deprecated).
  - This allows extensions to live directly under the main International Edition branch rather than under a version branch.
- Concept attributes and axioms sorted in all hierarchies using International Authoring Team's preferred order for best readability.
- Description search language filter.
- Pull request #77 AWS ElasticSearch request signing.
- Ability to import daily build RF2 automatically from S3.
- Daily build authoring statistics endpoint.
- Content report for concepts made inactive with no association.
  - GET /{branch}/report/inactive-concepts-without-association
- Authoring:
  - Admin function to rollback a commit.

### Improvements
- Implemented #75 Option to enable unlimited pagination of full concept format using `snowstorm.rest-api.allowUnlimitedConceptPagination=true` (disabled by default).
- Implemented #74 Set default upload size limit to 1024MB.
- FHIR:
  - Make json the default
  - Minor updates to FHIR readme.
- Ability to filter RF2 Snapshot Export by effectiveTime.
- Upgrade baked-in MRCM XML to 1.25.0 - matching the July 2019 International Edition.
- Search:
  -  Issue #41 Configure Danish, Norwegian and Finnish alphabet additional characters.
- Authoring:
  - Prevent duplicate historical associations and inactivation reasons during branch merge.
  - API to Load Concept version from base branch timepoint.
  - API to delete single descriptions including force option.
  - API to delete single relationships including force option.
  - Log more information while processing large classification results.
- Browser:
  - Add default defaultLanguageReferenceSets to Code Systems.
- Updated docker compose and readme.
- MRCM typeahead performance improvement.
- Stateless HTTP session management to prevent memory leak.
- Update IHTSDO maven repos to nexus3.

### Fixes
- Fix issue #62 Concept search using non-alphanumeric characters
- Fix issue #78 Branch path format validation.
- Fix search results totals not consistent for the three active status options.
- Changing code system default language no longer requires restart.
- Fix inferredNotStated lookup for classifications with over 1K results.
- Prevent duplicate results when using a concept id in the search "term" field.
- Minor load test harness fixes.
- Admin operation to remove duplicate versions of donated content in version control.
- Fixed search result sorting when descriptions grouped by concept.
- RF2 Import:
  - Fix clearing release status of components when new version imported via RF2.
  - Fix Unpublished refset components being marked as released during RF2 delta import when no effective time given.
- Authoring Validation:
  - Fix Incorrect Drools semantic tag warning related to inactive FSNs.
  - Fix Drools not detecting use of duplicate synonym in same hierarchy.
  - Fix Drools warning for duplicate FSNs which are inactive.
  - Fix Drools inbound relationship check to use axioms.
  - Fix validation endpoint JSON / URL mapping issue.
- Authoring:
  - Fix Rebase failing when branch has large number of semantic changes
  - Prevent duplicate concepts in semantic index during promotion.
- Fix thread safety issue in branch review function.
- Stop making inferred relationships inactive during concept inactivation. Classification must do this.
- Semantic index: improve depth counting, error reporting and handling.
- Fix Changing published inactivation indicator does not come back consistently.
- Fix Concept deletion orphaning active axioms.
- Allow transitive closure loop during rebase commit.



## 4.1.0 Release - 2019-08-07 - Public API supporting the SNOMED browser

This major version includes the API for the SNOMED International public SNOMEDCT browser!
The browser descriptions endpoint is now faster and includes the full set of aggregations and filters to support the browser search.

Another new feature is enhanced character matching for non-english languages. 
Diacritic characters which are considered as additional letters in the alphabet of a language can be added to configuration to have them indexed correctly for search. 
For example the Swedish language uses the characters 'å', 'ä' and 'ö' as additional letters in their alphabet, these letters are not just accented versions of 'a' and 'o'.
Thank you to Daniel Karlsson for educating us about this and providing an initial proof of concept. 

Thank you to everyone who asked questions and provided feedback during another great release.

_Note: The old public browser API project "sct-snapshot-rest-api" has now been archived in favour of the Snowstorm terminology server._

### Breaking
- Description index mapping has been updated with better support for non-english languages. 
Please migrate existing data to the new mapping using the [reindexing guide](docs/index-mapping-changes.md) then run 
the new admin "Rebuild the description index" function found in the swagger API docs.

### Features
- Search: Enhanced character matching for non-english languages (configured under "Search International Character Handling" in application.properties).
- Full set of aggregations and filters for browser description API endpoint.
- New concept references endpoint.
- New aggregated browser reference set members endpoint for refset summary view.
- FHIR:
  - Valueset maintenance / CRUD operations.
  - Language support - search appropriate language refset.

### Improvements
- Scalability:
  - Branch merge operation data now persisted in Elasticsearch. No other non-persistent operational data found. Ready for testing as multi-instance authoring server.
- Browser description search:
  - Faster aggregations.
  - New search parameters: active, semanticTag, module, conceptRefset.
  - New options: group by concept. 
  - New search mode for regular expressions.
- Browser:
  - Browser Concept JSON format made consistent with Snow Owl (minor changes made on both sides).
- Code system listing enhanced with languages, modules and latest release.
- OWL:
  - Use latest International stemming axioms to link Object Properties and Data Properties to the main Class hierarchy.
- Configuration:
  - Added extension module mapping for Estonia, Ireland, India and Norway extensions.
- FHIR:
  - Add Postman link to FHIR docs.
  - Upgrade to HAPI 3.8.0.
  - Add filter support for extensionally defined valuesets - on expansion.
- Authoring:
  - Add attribute grouped/ungrouped validation.
  - Semantic index processing of new object attribute axioms.
  - Automatically add description non-current indicators when inactive concept saved.
  - Automatically inactivate lang refset members when inactive description saved.
  - Validation: GCI must contain at least one parent and one attribute.
- Classification:
  - Equivalent concepts response format compatible with Snow Owl.
  - Add inferred not previously stated flag to relationship changes in classification report.
- Version Control:
  - Rebase no longer changes the base timepoint of the original branch version.
  - Allow loading concepts from branch base timepoint using GET /browser/{branch}@^/concept/{conceptId}.
  - Log commit duration.
- RF2 Import:
  - Change skipped components warning message to info.
- Code Build:
  - Allow Elasticsearch unit tests to run when disk low.
  - Replace Cobertura maven plugin with Jacoco.
  - Fix all lgtm.com automated code review suggestions.
- Deployment:
  - Debian package uses urandom for SecureRandom session IDs.

### Fixes
- Fix issue #16 Return complete list of code system versions.
- Fix issue #49 Correct total results count in simple concept search.
- Fix issue #53 Incorrect ECL ancestor count after delta import.
  - Account for multiple ancestors in semantic index update concept graph.
- Fix issue #55 Security fix - upgrade embedded Tomcat container to 8.5.41.
- FHIR:
  - Correct ICD-10 URI and allow reverse map lookup.
  - Don't show refset membership in system URI.
  - Protect against null pointer if VS is not found.
- Search:
  - Use translated FSN and PT in concept browser response.
  - Fix concept search when combining ECL and definition status.
  - Concept search using concept id can now return inactive concepts.
  - Fix active flag concept filter. 
- Version Control:
  - Fix branch rebase issue where multiple versions of a component could survive.   
  - Fix performance issue when promoting a large amount of changes to MAIN.
  - Branch merge review now checks changes on all ancestor branches not just the parent branch.
- Authoring validation:
  - Drools: Multiple hierarchy error should not use inferred form.
  - No traceability logging when concept updated with no change.
- Other:
  - Fix classification job date formats.
  - Concept browser format returns PT in PT field rather than FSN.



## 3.0.3 Release - 2019-05-17

This major version has support for Complete OWL SNOMED releases with no need for any active stated relationships.
It also supports authoring using OWL axioms.

Thanks again to everyone who got involved in the questions, issues and fixes during this release!

### Breaking
- Elasticsearch reindex is required.
  - Indices have been renamed to a simpler, more readable format. For example `es-rel` has been renamed to `relationship`.
  - Default number of shards per index has been changed to 1 (configurable in application.properties).
- Renamed concept additionalAxioms field to classAxioms.
- Rename classification branch metadata keys inline with RVF.

### Features
- OWL Axiom Support:
  - Import complete OWL versions of SNOMED CT.
  - Stated hierarchy navigation using axioms.
  - Stated ECL using axioms.
  - Authoring using only axioms without any active stated relationships.
  - Concept definition status set automatically using axiom definition status.
- Search:
  - Description search semantic tag counts and filtering.
  - New refset member search with aggregations (totals per reference set).
  - Search for refset members by a concept id within an OWL axiom expression.
  - Search for refset members containing OWL axiom class axioms or GCI axioms.
- FHIR:
  - Support for multiple languages
    - Accept-Language request header respected.
    - Valueset expand operation 'displayLanguage' and 'designations' parameters supported.
  - Add support for expand operation 'filter' parameter.
  - Add support for offset, count and total in ValueSet expand operation.
  - Add support for CodeSystem lookup properties.
  - Add support for all implicitly defined FHIR valuesets.
  - Add support for maps (ICD-10, CTV-3) including historical associations.
- Productionisation:
  - New multithreaded load test harness (ManualLoadTest.java) can be used to simulate many authoring users in order to evaluate different deployment configurations.
  - Concept searchAfter parameter allows scrolling through more than 10K results.
- Extensions:
  - Basic extension upgrade support (via POST /codesystems/{shortName}/migrate).
- Other:
  - Added reference set member create and update functionality to REST API.
  - Ability to load concepts from version control history within the same branch.

### Improvements
- Elasticsearch:
  - Recommended Elasticsearch version updated to 6.5.4.
  - Number of index shards and replicas is now configurable. Defaults to 1 shard and 0 replicas.
- FHIR:
  - Upgrade FHIR API from DSTU3 to R4.
  - Allow valueset expansion against other imported editions.
  - Allow FHIR requests to access MAIN (or equivalent) as well as versioned branches.
  - Automatically populate server capabilities version from maven pom.
  - Update documentation:  paging and filtering, valueset defined via refset.
- Authoring:
  - Automatically remove inactivation members when concept made active.
  - Automatically remove lang refset members which description made inactive.
  - Ensure refset member effectiveTime is updated during changes.
  - Exclude synonyms when finding conflicts during branch merge.
  - OWL Axioms included in integrity check functionality.
  - Branch lock metadata added to describe currently running commit.
  - New SNOMED Drools Engine validation engine with axiom support.
  - Many Drools validation fixes.
  - Ability to reload validation assertions and resources.
  - Updated baked in MRCM XML.
- Classification:
  - Classification save stale results check.
  - Allow saving classification results with greater than 10K changes.
  - Classification results can change existing relationships.
- Branch merging:
  - Details of integrity issues found during a promotion included in API response.
  - Exclude non-stated relationships.
  - Concepts can be manually deleted during branch merge review.
- RF2 Export:
  - New reference set types for RF2 export including: Description Type, Module Dependency and Simple Type. Note that Snowstorm does not yet calculate the Module Dependency members.
  - Carriage return line endings in RF2 export in line with RF2 specification.
  - Combine OWL Axiom and OWL Ontology refsets in RF2 export.
  - Add transientEffectiveTime option in RF2 export.
- Improved Docker instructions.
- ECL slow query logging.
- Branch metadata can contain objects.
- Binary for Elasticsearch unit tests cached in user home directory.
- Base, head and creation date stamps and epoch milliseconds on branch response.
- Authoring traceability logging of inferred changes capped to 100 (configurable).
- Moved semantic index rebuild endpoint to admin area in Swagger.
- Refset member search allows ECL in referenceSet parameter.
- Authoring traceability appended to separate log file.

### Fixes
- FHIR:
  - Fix finding latest code system version.
- Remove extra tab in header of some refset export files.
- Fix for attribute group disjunction in ECL.
- Clean up concept Elasticsearch auto-mapping, remove unpopulated fields.
- Automatically create snomed-drools-rules directory if missing.
- Additional axioms and GCI axioms included in branch merge.
- Identify branch merge conflict when concept deleted on either side of the merge.
- Prevent importing three bad relationships from international snapshot as stated and inferred.
- Version endpoint format corrected to JSON.



## 2.2.3 Fix Release - 2019-04-23

### Fixes
- Fix concept descendants endpoint for stated and inferred.



## 2.2.2 Fix Release - 2019-04-01

### Improvements
- Clarify documentation for extension loading
### Fixes
- UK Edition import fixes



## 2.2.1 Fix Release - 2019-03-29

### Fixes
- FHIR API fix, remove Accept-Language for now, incompatible annotation



## 2.2.0 Release - 2019-03-15
Maintenance release with fixes and enhancements.

Thanks to everyone who raised an issue or provided a pull request for this maintenance release.

_NOTICE - The next major release will be 3.x which will introduce support
for SNOMED CT Editions with a completely axiom based stated form._

### Breaking
- Removal of partial support for concept search using ESCG in favour of ECL.

### Features
- Issue #14 Language/Extension support in FHIR API (PR from @goranoe).
  - Added module to CodeSystem lookup table to support this.
- Issue #18 Command line --exit flag shuts down Snowstorm after loading data.
- Added Elasticsearch basic authentication configuration options.
- Support for latest RF2 OWL reference set file naming.
- Added low level single concept endpoint.
- Added concept search definition status filter.

### Improvements
- Issue #28 Better non-english character support in ECL parsing (by @danka74).
- Docker configuration improvements and documentation (PRs from @Zwordi and @kevinbayes).
- Many documentation updates.
- New documentation on Snowstorm FHIR support.
- New documentation on updating extensions.
- Semantic index updates are not logged if they take less than a second.
- Added "Snowstorm startup complete" log message.
- Refactoring recommendations from lgtm.com.
- Allow branch specific MRCM XML configuration.
- Removed unused feature which allowed mirrored authoring via traceability feed.
- New ascii banner on startup.
- Concept search uses stated form unless inferred ecl given (better during authoring and has no effect on released content).
- Fail faster when concept page is above 10K (ES does not support this with default config).

### Fixes
- Issue #29 Escape concept term quotes in search results.
- Fix concept parents listing.
- Fix ECL dot notation against empty set of concepts.
- Fix ECL conjunction with reverse flag.
- MRCM API domain attributes returns 'is a' attribute if no parents specified.
- MRCM API allows subtypes of MRCM attributes.
- Fix reloading MRCM rules API mapping.
- Catch classification save error when branch locked.
- Fix missing destination expansion in relationship endpoint
- Prevent crosstalk in Elasticsearch integration tests.



## 2.1.0 Release - 2018-10-22

Snowstorm is now production ready as a read-only terminology server.

### Features
- Running with latest Elasticsearch server (6.4.2) is now tested recommended.
- Include Preferred Term (PT) in concepts returned from API.
- Translated content support.
  - Translated Fully Specified Name (FSN) and Preferred Term (PT) are returned
  against all API responses when language is set in the Accept-Language header.
- Add conceptActive filter to description search API.
- Search reference set members by mapTarget.

### Improvements
- Performance improvement when holding large change sets in MAIN branch.
- ReferenceComponent concept included in reference set member response.
- Creating import configuration checks branch path and code system.
- Better date formatting in branch created date and code system versions.
- Make concept lookup performance logging quieter.
- New flag to create code system version automatically during import.

### Fixes
- Inactive relationships excluded from integrity check.
- Correct path of Relationship API.
- Correct full integrity check branch mapping.
- Correct reference set member lookup branch mapping.
- Concept parents endpoint excludes inactive parent relationships.
- Allow creating code system on branches other than MAIN.
- RF2 import time logging calculation.
- Export configuration conceptsAndRelationshipsOnly defaults to false.



## 2.0.0 Release Candidate - 2018-09-19

This major version brings support for the new SNOMED Axiom component as well as
many productionisation fixes.

This version is ready for testing.

### Breaking
- Elasticsearch indexes must be recreated due to changes in their format.


### Features
- Support for new Axiom component type.
  - CRUD operations via concept browser format.
  - RF2 Import/Export.
  - Classification Service integration.
  - Axioms used in ECL queries against the stated form.
- New integrity check functions with API endpoints.
  - Runs automatically before promotion.
- Description search aggregations similar to SNOMED CT public browser.
  - Aggregations for module, semantic tag, language, concept reference set membership.
- Create Code Systems via REST API.
- Create Code System versions via REST API.
- All reference set members imported without the need for configuration.
- ICD, CTV3 and four MRCM reference sets added to default configuration for RF2 export.
- New released content RF2 patch API endpoint.

### Improvements
- Concurrency and branch locking improvements.
- Performance improvement for branches containing wide impacting semantic changes.
- Concept description search algorithm improvement.
- Classification Service client authentication.
- Component Identifier Service client authentication.
- Added support for ECL ancestor of wildcard.
- Update to latest Snomed Drools Engine version.
- Added software version API endpoint.
- Limit traceability logging to first 300 inferred changes.
- Allow microservices within the Snomed Single Sign-On gateway to access Snowstorm directly.
- Rows in RF2 delta only imported if effectiveTime is blank or greater than existing component.
- Concept search TSV download.
- Classification results TSV download.

### Fixes
- Many ECL fixes.
- Semantic index update fix for non "is a" relationships.
- Fixes for complete semantic index rebuild feature.
- Remove irrelevant concepts from branch merge review.
- Changes to axioms and historical associations included in conflict check.
- Bring pagination parameters in line with Snow Owl 5.x.
- Fix Authoring Form endpoint.
- Allow very large classification results to be saved.
- Fix classification status during save.
- Fix change type of classification results.
- Prevent unnecessary new versions of inactive language refset members.
- Fix RF2 export download headers.
- Better grouping and naming of API endpoints in Swagger interface.
- Set ELK as default reasoner in Swagger interface.
- Log traceability activity for branch merges.



## 1.1.0 Alpha - 2018-05-29
This second alpha release is another preview of Snowstorm in read-only mode.

### Breaking
- Elasticsearch indexes must be recreated due to changes in their format.

### Features
- Docker container option, see [Using Docker](docs/using-docker.md).

### Improvements
- Improved lexical search matching and sorting.
- Upgrade Snomed Drools validation engine.
- Improved CIS authentication.

### Fixes
- ECL fixes (some attributes were missing due to relationship sorting bug).
- MRCM endpoint pagination fix.



## 1.0.0 Alpha - 2018-04-12
This alpha release gives people an early preview of Snowstorm in read-only mode.
Just follow the setup guide and import a snapshot.

### Features
- Browsing concepts including all descriptions and relationships in one response.
- Concept search:
  - ECL 1.3 using inferred or stated form
  - Term filter using FSN
- Reference set member search.
