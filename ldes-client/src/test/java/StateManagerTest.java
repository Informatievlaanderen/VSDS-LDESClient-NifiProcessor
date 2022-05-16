import be.vlaanderen.informatievlaanderen.ldes.client.services.StateManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StateManagerTest {
    StateManager stateManager;
    String pageToProcess = "localhost:8089/testData?1";
    String nextPageToProcess = "localhost:8089/testData?2";

    String memberIdToProcess = "localhost:8089/api/v1/data/10228974/2397";

    @BeforeEach
    public void init(){
        stateManager = new StateManager(pageToProcess);
    }

    @Test
    public void when_StateManagerIsInitialized_QueueHasOnlyOneItem() {
        assertTrue(stateManager.hasPagesToProcess());
        assertEquals(pageToProcess, stateManager.getNextPageToProcess());
        assertFalse(stateManager.hasPagesToProcess());
        Assertions.assertThrows(RuntimeException.class, stateManager::getNextPageToProcess);
    }

    @Test
    public void when_tryingToProcessTheSamePageTwice_PageDoesNotGetAddedToQueue() {
        assertTrue(stateManager.hasPagesToProcess());
        assertEquals(pageToProcess, stateManager.getNextPageToProcess());

        stateManager.addNewPageToProcess(pageToProcess);
        stateManager.addNewPageToProcess(nextPageToProcess);

        assertEquals(nextPageToProcess, stateManager.getNextPageToProcess());
        assertFalse(stateManager.hasPagesToProcess());
    }

    @Test
    public void when_tryingToProcessAnAlreadyProcessedLdesMember_MemberDoesNotGetProcessed() {
        assertTrue(stateManager.processMember(memberIdToProcess));
        assertFalse(stateManager.processMember(memberIdToProcess));
    }
}
