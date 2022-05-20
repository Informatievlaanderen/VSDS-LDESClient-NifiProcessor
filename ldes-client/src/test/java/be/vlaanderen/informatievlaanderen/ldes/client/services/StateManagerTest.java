package be.vlaanderen.informatievlaanderen.ldes.client.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StateManagerTest {
    StateManager stateManager;
    String fragmentToProcess = "localhost:8089/testData?1";
    String nextFragmentToProcess = "localhost:8089/testData?2";

    String memberIdToProcess = "localhost:8089/api/v1/data/10228974/2397";

    @BeforeEach
    public void init(){
        stateManager = new StateManager(fragmentToProcess);
    }

    @Test
    public void when_StateManagerIsInitialized_QueueHasOnlyOneItem() {
        assertTrue(stateManager.hasFragmentsToProcess());
        assertEquals(fragmentToProcess, stateManager.getNextFragmentToProcess());
        assertFalse(stateManager.hasFragmentsToProcess());
        Assertions.assertThrows(RuntimeException.class, stateManager::getNextFragmentToProcess);
    }

    @Test
    public void when_tryingToProcessTheSameFragmentTwice_FragmentDoesNotGetAddedToQueue() {
        assertTrue(stateManager.hasFragmentsToProcess());
        assertEquals(fragmentToProcess, stateManager.getNextFragmentToProcess());

        stateManager.addNewFragmentToProcess(fragmentToProcess);
        stateManager.addNewFragmentToProcess(nextFragmentToProcess);

        assertEquals(nextFragmentToProcess, stateManager.getNextFragmentToProcess());
        assertFalse(stateManager.hasFragmentsToProcess());
    }

    @Test
    public void when_tryingToProcessAnAlreadyProcessedLdesMember_MemberDoesNotGetProcessed() {
        assertTrue(stateManager.processMember(memberIdToProcess));
        assertFalse(stateManager.processMember(memberIdToProcess));
    }
}
