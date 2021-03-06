package be.vlaanderen.informatievlaanderen.ldes.client.services;

import be.vlaanderen.informatievlaanderen.ldes.client.exceptions.LdesException;
import be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.FragmentSettings;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static be.vlaanderen.informatievlaanderen.ldes.client.valueobjects.FragmentSettings.IMMUTABLE;

public class StateManager {
    private Clock clock = Clock.systemDefaultZone();
    protected final Queue<String> fragmentsToProcessQueue;
    protected final Map<String, FragmentSettings> processedFragments;
    protected final Set<String> processedMembers;

    public StateManager(String initialFragmentToProcess) {
        this.fragmentsToProcessQueue = new ArrayDeque<>();
        this.fragmentsToProcessQueue.add(initialFragmentToProcess);
        this.processedMembers = new HashSet<>();
        this.processedFragments = new HashMap<>();
    }

    protected StateManager(String initialFragmentToProcess, Clock clock) {
        this(initialFragmentToProcess);
        this.clock = clock;
    }

    public boolean shouldProcessMember(String member) {
        return processedMembers.add(member);
    }

    public boolean hasFragmentsToProcess() {
        return fragmentsToProcessQueue.iterator().hasNext();
    }

    public String getNextFragmentToProcess() {
        if (!hasFragmentsToProcess()) {
            throw new LdesException("No more fragments to process");
        }

        return fragmentsToProcessQueue.poll();
    }

    public void addNewFragmentToProcess(String pageUrl) {
        if (!processedFragments.containsKey(pageUrl)) {
            fragmentsToProcessQueue.add(pageUrl);
        }
    }

    public void processFragment(String fragmentUrl, Long maxAge) {
        if (maxAge == null) {
            this.processedFragments.put(fragmentUrl, new FragmentSettings(IMMUTABLE));
        } else {
            this.processedFragments.put(fragmentUrl, new FragmentSettings(LocalDateTime.now(clock).plusSeconds(maxAge)));
        }
    }

    public void processFragment(String fragmentUrl) {
        processFragment(fragmentUrl, null);
    }

    public void populateFragmentQueue() {
        processedFragments.forEach((fragmentURL, fragmentSettings) -> {
            if (fragmentSettings.getExpireDate() != null && fragmentSettings.getExpireDate().isBefore(LocalDateTime.now(clock))) {
                fragmentsToProcessQueue.add(fragmentURL);
            }
        });
    }
}
