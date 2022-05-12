package be.vlaanderen.vsds.ldesclient.nifi.processors.services;

import be.vlaanderen.vsds.ldesclient.nifi.processors.models.TreeDirection;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class StateManager {
    private final Queue<String> pagesToProcess;
    protected final Set<String> processedPages;
    protected final Set<String> processedItems;
    private TreeDirection treeDirection;
    private boolean fullyReplayed;

    public StateManager(String initialPageToProcess) {
        this.pagesToProcess = new ArrayDeque<>();
        this.pagesToProcess.add(initialPageToProcess);
        this.processedItems = new HashSet<>();
        this.processedPages = new HashSet<>();
    }

    public StateManager(String treeDirection, String initialPageToProcess) {
        this.pagesToProcess = new ArrayDeque<>();
        this.pagesToProcess.add(initialPageToProcess);
        this.processedItems = new HashSet<>();
        this.processedPages = new HashSet<>();
        this.treeDirection = TreeDirection.valueOf(treeDirection);
    }

    public boolean processMember(String member) {
        return processedItems.add(member);
    }

    public boolean hasPagesToProcess() {
        return !pagesToProcess.isEmpty();
    }
    public String getNextPageToProcess() {
        if(!hasPagesToProcess()) {
            throw new RuntimeException("No more pages to process");
        }

        String pageUrl = pagesToProcess.poll();
        processedPages.add(pageUrl);
        return pageUrl;
    }

    public void addNewPageToProcess(String pageUrl) {
        if(!processedPages.contains(pageUrl)) {
            pagesToProcess.add(pageUrl);
        }
    }

    public String getTreeDirection() {
        return "tree:" + treeDirection.toString();
    }

    public boolean isFullyReplayed() {
        return fullyReplayed;
    }

    public void setFullyReplayed(boolean fullyReplayed) {
        this.fullyReplayed = fullyReplayed;
    }
}
