package com.agentopscrm.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary response for a bulk import operation (F-010).
 *
 * @author AgentOps Team
 * @version 0.10.0
 */
public class BulkImportResultResponse {

    private int requested;
    private int imported;
    private int skippedDuplicates;
    private int failed;
    private List<String> messages = new ArrayList<>();

    public BulkImportResultResponse() {
    }

    public int getRequested() {
        return requested;
    }

    public void setRequested(int requested) {
        this.requested = requested;
    }

    public int getImported() {
        return imported;
    }

    public void setImported(int imported) {
        this.imported = imported;
    }

    public int getSkippedDuplicates() {
        return skippedDuplicates;
    }

    public void setSkippedDuplicates(int skippedDuplicates) {
        this.skippedDuplicates = skippedDuplicates;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
