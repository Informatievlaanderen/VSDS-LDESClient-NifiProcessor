package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class StateManagerTest {
    StateManager stateManager;
    Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    String fragmentToProcess = "localhost:8089/testData?1";
    String nextFragmentToProcess = "localhost:8089/testData?2";

    String memberIdToProcess = "localhost:8089/api/v1/data/10228974/2397";

    @BeforeEach
    public void init(){
        stateManager = new StateManager(fragmentToProcess, clock);
    }

    @Test
    void when_StateManagerIsInitialized_QueueHasOnlyOneItemAndThrowsExceptionOtherwise() {
        assertTrue(stateManager.hasFragmentsToProcess());
        assertEquals(fragmentToProcess, stateManager.getNextFragmentToProcess());

        assertFalse(stateManager.hasFragmentsToProcess());
        Assertions.assertThrows(LdesException.class, stateManager::getNextFragmentToProcess);
    }

    @Test
    void when_tryingToProcessTheSameFragmentTwice_FragmentDoesNotGetAddedToQueue() {
        assertTrue(stateManager.hasFragmentsToProcess());

        stateManager.processFragment(stateManager.getNextFragmentToProcess());

        stateManager.addNewFragmentToProcess(fragmentToProcess);
        stateManager.addNewFragmentToProcess(nextFragmentToProcess);

        assertEquals(nextFragmentToProcess, stateManager.getNextFragmentToProcess());
        assertFalse(stateManager.hasFragmentsToProcess());
    }

    @Test
     void when_tryingToProcessAnAlreadyProcessedLdesMember_MemberDoesNotGetProcessed() {
        assertTrue(stateManager.shouldProcessMember(memberIdToProcess));
        assertFalse(stateManager.shouldProcessMember(memberIdToProcess));
    }

    @Test
    void when_parsingImmutableFragment_saveAsProcessedPageWithEmptyExpireDate() {
        stateManager.processFragment(fragmentToProcess);

        assertNull(stateManager.processedFragments.get(fragmentToProcess).getExpireDate());
    }

    @Test
    void when_parsingFragment_saveAsProcessedPageWithCorrectExpireDate() {
        LocalDateTime dateTime = LocalDateTime.now(clock);

        stateManager.processFragment(fragmentToProcess, 6000L);

        assertNotNull(stateManager.processedFragments.get(fragmentToProcess).getExpireDate());
        assertEquals(stateManager.processedFragments.get(fragmentToProcess).getExpireDate(), dateTime.plusSeconds(6000L));
    }

    @Test
    void when_populateFragmentQueueWithNoInvalidFragments_AddNothingToQueue() {
        stateManager.fragmentsToProcessQueue.clear();

        stateManager.populateFragmentQueue();

        assertEquals(0, stateManager.fragmentsToProcessQueue.size());
    }

    @Test
    void when_populateFragmentQueueWith1InvalidFragment_InvalidFragmentGetsAddedToQueue() {
        stateManager.fragmentsToProcessQueue.clear();
        stateManager.processFragment(fragmentToProcess, -1L);

        stateManager.populateFragmentQueue();

        assertEquals(1, stateManager.fragmentsToProcessQueue.size());
    }
}
