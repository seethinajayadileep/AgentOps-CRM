package com.agentopscrm.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationQueryEscapeTest {

    @Test
    void escapeLikeEscapesWildcards() {
        assertEquals("final retest qa", ConversationService.escapeLike("final retest qa"));
        assertEquals("100\\%", ConversationService.escapeLike("100%"));
        assertEquals("a\\_b", ConversationService.escapeLike("a_b"));
        assertEquals("foo\\\\bar", ConversationService.escapeLike("foo\\bar"));
    }
}
