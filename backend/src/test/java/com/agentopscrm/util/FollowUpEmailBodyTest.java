package com.agentopscrm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowUpEmailBodyTest {

    @Test
    void splitsOneLineDraftIntoGreetingBodyAndClose() {
        String raw = "Dear Jayadileep, Thank you for your inquiry regarding a quote for AgentOps CRM. "
                + "We appreciate your interest and would like to discuss your specific requirements further. "
                + "Please let us know a suitable time for a brief call this month.";

        String plain = FollowUpEmailBody.plain(raw);

        assertEquals(
                "Dear Jayadileep,\n\n"
                        + "Thank you for your inquiry regarding a quote for AgentOps CRM. "
                        + "We appreciate your interest and would like to discuss your specific requirements further.\n\n"
                        + "Please let us know a suitable time for a brief call this month.",
                plain);
        String html = FollowUpEmailBody.html(raw);
        assertTrue(html.contains("<p style=\"margin:0 0 16px 0\">Dear Jayadileep,</p>"));
        assertTrue(html.contains("Please let us know a suitable time for a brief call this month."));
        assertTrue(html.contains("</div>"));
    }

    @Test
    void keepsExistingBlankLineParagraphs() {
        String raw = "Hi Ada,\n\nThanks for writing.\n\nWhen can we talk?";
        assertEquals("Hi Ada,\n\nThanks for writing.\n\nWhen can we talk?", FollowUpEmailBody.plain(raw));
    }
}
