package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class LdesStateManagerTest {
    LdesStateManager stateManager;
    Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    String fragmentToProcess = "localhost:10101/testData?1";
    String nextFragmentToProcess = "localhost:10101/testData?2";

    String memberIdToProcess = "localhost:10101/api/v1/data/10228974/2397";

    @BeforeEach
    public void init() {
        stateManager = new LdesStateManager();
        stateManager.queueFragment(fragmentToProcess);
    }

//    @Test
//    void when_StateManagerIsInitialized_QueueHasOnlyOneItemAndThrowsExceptionOtherwise() {
//        assertTrue(stateManager.hasNext());
//        assertEquals(fragmentToProcess, stateManager.next());

//        assertFalse(stateManager.hasNext());
//        Assertions.assertThrows(LdesException.class, stateManager::next);
//    }

//    @Test
//    void when_tryingToProcessTheSameFragmentTwice_FragmentDoesNotGetAddedToQueue() {
//        assertTrue(stateManager.hasFragmentsToProcess());
//
//        stateManager.processedFragment(new LdesFragment(stateManager.getNextFragment(), null));
//
//        stateManager.queueFragment(fragmentToProcess);
//        stateManager.queueFragment(nextFragmentToProcess);
//
//        assertEquals(nextFragmentToProcess, stateManager.getNextFragment());
//        assertFalse(stateManager.hasFragmentsToProcess());
//    }

//    @Test
//     void when_tryingToProcessAnAlreadyProcessedLdesMember_MemberDoesNotGetProcessed() {
//    	stateManager.processedMember()
//        assertTrue(stateManager.shouldProcessMember(memberIdToProcess));
//        assertFalse(stateManager.shouldProcessMember(memberIdToProcess));
//    }
//
//    @Test
//    void when_parsingImmutableFragment_saveAsProcessedPageWithEmptyExpireDate() {
//        stateManager.processedFragment(fragmentToProcess);
//
//        assertNull(stateManager.processedFragments.get(fragmentToProcess).getExpireDate());
//    }
//
//    @Test
//    void when_parsingFragment_saveAsProcessedPageWithCorrectExpireDate() {
//        LocalDateTime dateTime = LocalDateTime.now(clock);
//
//        stateManager.fragmentProcessed(fragmentToProcess, 6000L);
//
//        assertNotNull(stateManager.processedFragments.get(fragmentToProcess).getExpireDate());
//        assertEquals(stateManager.processedFragments.get(fragmentToProcess).getExpireDate(), dateTime.plusSeconds(6000L));
//    }
//
//    @Test
//    void when_populateFragmentQueueWithNoInvalidFragments_AddNothingToQueue() {
//        stateManager.fragmentsToProcess.clear();
//
//        stateManager.populateFragmentQueue();
//
//        assertEquals(0, stateManager.fragmentsToProcess.size());
//    }
//
//    @Test
//    void when_populateFragmentQueueWith1InvalidFragment_InvalidFragmentGetsAddedToQueue() {
//        stateManager.fragmentsToProcess.clear();
//        stateManager.fragmentProcessed(fragmentToProcess, -1L);
//
//        stateManager.populateFragmentQueue();
//
//        assertEquals(1, stateManager.fragmentsToProcess.size());
//    }
}
