package com.agentopscrm.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatServiceLeadCaptureAnswerTest {

    @Test
    void appendsContactAskToAUsefulServiceAnswer() {
        String merged = ChatService.applyLeadCaptureAnswer(
                "We offer SEO and PPC.",
                "Sure — please share your name along with a phone number or email so our team can contact you.",
                false,
                false);

        assertEquals(
                "We offer SEO and PPC.\n\nSure — please share your name along with a phone number or email so our team can contact you.",
                merged);
    }

    @Test
    void replacesAKnowledgeMissWithTheContactAsk() {
        String merged = ChatService.applyLeadCaptureAnswer(
                "I do not have confirmed information about that in the knowledge base.",
                "Sure — please share your name along with a phone number or email so our team can contact you.",
                false,
                true);

        assertEquals(
                "Sure — please share your name along with a phone number or email so our team can contact you.",
                merged);
    }

    @Test
    void showsLeadConfirmationInsteadOfTheRagAnswer() {
        String merged = ChatService.applyLeadCaptureAnswer(
                "We offer SEO and PPC.",
                "Thanks Jaya, your details have been saved. Our team will contact you soon.",
                true,
                false);

        assertEquals(
                "Thanks Jaya, your details have been saved. Our team will contact you soon.",
                merged);
    }

    @Test
    void extraServiceNamesAreNotAHardSafetyBlock() {
        assertFalse(ChatService.isHardSafetyBlock(
                "The answer includes 'Content Marketing' and 'Email Marketing' which are not explicitly supported by the retrieved chunks."));
        assertTrue(ChatService.isHardSafetyBlock(
                "Answer mentioned pricing but pricing was not found in retrieved chunks."));
    }

    @Test
    void contactReplyIsNotTreatedAsAKnowledgeQuestion() {
        assertFalse(ChatService.isKnowledgeQuestion("jayadileepb@gmail.com and +918328270668"));
        assertTrue(ChatService.isKnowledgeQuestion("what are the service you will provide"));
    }
}
