package be.vlaanderen.informatievlaanderen.ldes.client.services;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class StateManager {
    protected final Queue<String> fragmentsToProcessQueue;
    protected final Set<String> processedFragments;
    protected final Set<String> processedMembers;
    private boolean fullyReplayed;

    public StateManager(String initialFragmentToProcess) {
        this.fragmentsToProcessQueue = new ArrayDeque<>();
        this.fragmentsToProcessQueue.add(initialFragmentToProcess);
        this.processedMembers = new HashSet<>();
        this.processedFragments = new HashSet<>();
    }

    public boolean processMember(String member) {
        return processedMembers.add(member);
    }

    public boolean hasFragmentsToProcess() {
        return fragmentsToProcessQueue.iterator().hasNext();
    }
    public String getNextFragmentToProcess() {
        if(!hasFragmentsToProcess()) {
            throw new RuntimeException("No more pages to process");
        }

        String pageUrl = fragmentsToProcessQueue.poll();
        processedFragments.add(pageUrl);
        return pageUrl;
    }

    public void addNewFragmentToProcess(String pageUrl) {
        if(!processedFragments.contains(pageUrl)) {
            fragmentsToProcessQueue.add(pageUrl);
        }
    }

    public boolean isFullyReplayed() {
        return fullyReplayed;
    }

    public void setFullyReplayed(boolean fullyReplayed) {
        this.fullyReplayed = fullyReplayed;
    }
}
