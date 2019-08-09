package org.snomed.snowstorm.core.data.services;

import com.google.common.collect.Lists;
import io.kaicode.elasticvc.api.BranchService;
import io.kaicode.elasticvc.domain.Branch;
import io.kaicode.elasticvc.domain.Commit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.snomed.snowstorm.AbstractTest;
import org.snomed.snowstorm.TestConfig;
import org.snomed.snowstorm.core.data.domain.Concept;
import org.snomed.snowstorm.core.data.domain.Description;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * This class checks that making updates within a commit happen atomically.
 * If several concepts are being changed at once and one fails the whole operation should not be persisted.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class AtomicCommitTest extends AbstractTest {

	@Autowired
	private BranchService branchService;

	@Autowired
	private BranchMetadataHelper branchMetadataHelper;

	@Autowired
	private ConceptService conceptService;

	@Before
	public void setup() {
		branchService.create("MAIN/task");
	}

	@Test
	public void testMultipleConceptCreationRollback() throws ServiceException {
		String branch = "MAIN/task";
		Branch branchBefore = branchService.findLatest(branch);
		assertEquals("Branch should be up to date before commit.", Branch.BranchState.UP_TO_DATE, branchBefore.getState());
		assertFalse(branchBefore.isLocked());
		long headTimestampBefore = branchBefore.getHeadTimestamp();

		assertNull("Concept 1 should not exist before the attempted commit.", conceptService.find("1", branch));

		try {
			conceptService.batchCreate(Lists.newArrayList(
					new Concept("1").addDescription(new Description("one")),
					new Concept("2").addDescription(new Description("two")),
					new Concept("3").addDescription(new Description("three")).setInactivationIndicator("DOES_NOT_EXIST")
			), branch);
		} catch (IllegalArgumentException e) {
			// java.lang.IllegalArgumentException: Concept inactivation indicator not recognised 'DOES_NOT_EXIST'.
		}

		Branch branchAfter = branchService.findLatest(branch);
		// Branch should still be up to date after failed commit
		assertEquals(Branch.BranchState.UP_TO_DATE, branchAfter.getState());
		assertEquals("Head timestamp should be the same before and after a failed commit.", headTimestampBefore, branchAfter.getHeadTimestamp());
		assertFalse("Branch should be unlocked as part of commit rollback.", branchAfter.isLocked());

		assertNull("Concept 1 should not exist after the attempted commit " +
				"because although there is nothing wrong with that concepts the whole commit should be rolled back.", conceptService.find("1", branch));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBranchLockMetadata() {
		try (Commit commit = branchService.openCommit("MAIN/task", branchMetadataHelper.getBranchLockMetadata("Testing lock metadata"))) {
			Branch branch = branchService.findLatest("MAIN/task");
			Map<String, Object> metadata = branchMetadataHelper.expandObjectValues(branch.getMetadata());
			Map<String, Object> lockMetadata = (Map<String, Object>) metadata.get("lock");
			Map<String, Object> lockMetadataContext = (Map<String, Object>) lockMetadata.get("context");
			assertEquals("Testing lock metadata", lockMetadataContext.get("description"));
		}
	}

}
