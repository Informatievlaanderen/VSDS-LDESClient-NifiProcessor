package be.vlaanderen.informatievlaanderen.ldes.client.services;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class StateManager {
    private final Queue<String> fragmentsToProcessQueue;
    protected final Set<String> processedFragments;
    protected final Set<String> processedMembers;
    private String treeDirection;
    private boolean fullyReplayed;

    public StateManager(String initialFragmentToProcess) {
        this.fragmentsToProcessQueue = new ArrayDeque<>();
        this.fragmentsToProcessQueue.add(initialFragmentToProcess);
        this.processedMembers = new HashSet<>();
        this.processedFragments = new HashSet<>();
    }

    public StateManager(String treeDirection, String initialFragmentToProcess) {
        this.fragmentsToProcessQueue = new ArrayDeque<>();
        this.fragmentsToProcessQueue.add(initialFragmentToProcess);
        this.processedMembers = new HashSet<>();
        this.processedFragments = new HashSet<>();
        this.treeDirection = treeDirection;
    }

    public boolean processMember(String member) {
        return processedMembers.add(member);
    }

    public boolean hasPagesToProcess() {
        return fragmentsToProcessQueue.iterator().hasNext();
    }
    public String getNextPageToProcess() {
        if(!hasPagesToProcess()) {
            throw new RuntimeException("No more pages to process");
        }

        String pageUrl = fragmentsToProcessQueue.poll();
        processedFragments.add(pageUrl);
        return pageUrl;
    }

    public void addNewPageToProcess(String pageUrl) {
        if(!processedFragments.contains(pageUrl)) {
            fragmentsToProcessQueue.add(pageUrl);
        }
    }

    public String getTreeDirection() {
        return treeDirection;
    }

    public boolean isFullyReplayed() {
        return fullyReplayed;
    }

    public void setFullyReplayed(boolean fullyReplayed) {
        this.fullyReplayed = fullyReplayed;
    }
}
