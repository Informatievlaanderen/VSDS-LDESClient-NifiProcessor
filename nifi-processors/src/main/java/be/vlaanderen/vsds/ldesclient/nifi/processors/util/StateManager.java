package be.vlaanderen.vsds.ldesclient.nifi.processors.util;

import be.vlaanderen.vsds.ldesclient.nifi.processors.config.TreeDirection;

import java.util.*;

public class StateManager {
    private String nextPageToProcess;
    private final Set<String> processedItems;
    private final TreeDirection treeDirection;
    private boolean fullyReplayed;

    public StateManager(String treeDirection, String initialPageToProcess) {
        this.processedItems = new HashSet<>();
        this.treeDirection = TreeDirection.valueOf(treeDirection);
        this.nextPageToProcess = initialPageToProcess;
    }

    public boolean processItem(String item) {
        return processedItems.add(item);
    }

    public String getNextPageToProcess() {
        return nextPageToProcess;
    }

    public String getTreeDirection() {
        return "tree:" + treeDirection.toString();
    }

    public void lookupNextPageToProcess(LdesPage page) {
        Optional<String> nextRelationId = page.getRelationId(getTreeDirection());

        if (nextRelationId.isPresent()) {
            nextPageToProcess = nextRelationId.get();
        } else {
            //TODO implement different cycle with polling interval when stream is up to date
            fullyReplayed = true;
        }
    }
}
