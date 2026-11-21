# Feature Specification: Caramel AI Tech Support Chat Assistant

**Feature Branch**: `001-tech-support-chat`

**Created**: 2026-11-14

**Status**: Draft

**Input**: User description: "Caramel AI tech support chat assistant, as defined in prd.md and the project constitution (initial version)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Get an Answer to a Tech Support Question (Priority: P1)

A customer, either a developer integrating Caramel AI's inference API or a non-technical user with a
usage or error question, opens the assistant, types a question about the Caramel AI platform, and
reads a reply that appears progressively as it is written.

**Why this priority**: This is the core value of the product. Without it, no other story matters.

**Independent Test**: Open the assistant, send an in-scope question such as "Why am I getting a 429
error?", and confirm that a relevant answer begins to appear within a few seconds and finishes
without error.

**Acceptance Scenarios**:

1. **Given** a customer has opened the assistant with no prior messages, **When** they send an
   in-scope question, **Then** a loading indicator is shown and the reply begins to appear within a
   few seconds.
2. **Given** a reply is being shown, **When** it finishes, **Then** the full reply is visible, the
   loading indicator is gone, and the customer can type the next message.
3. **Given** a customer asks about a rate-limit or authentication error, **When** the reply
   completes, **Then** the reply addresses that error and explains the likely cause and next step.

---

### User Story 2 - Continue a Multi-Turn Conversation (Priority: P1)

A customer asks a follow-up question that depends on something said earlier in the same
conversation, for example "Can you show that with streaming turned on?" after asking about an API
call, and gets an answer that uses the earlier context without the customer repeating it.

**Why this priority**: Multi-turn troubleshooting is a stated success metric. Customers rarely solve
an issue in one message.

**Independent Test**: Ask a question about a specific model, then ask a follow-up that refers to
"it" or "that model" without naming it again. Confirm the follow-up reply is about the same model.

**Acceptance Scenarios**:

1. **Given** a customer has asked about a specific API parameter, **When** they ask a follow-up
   that refers to it indirectly, **Then** the reply correctly identifies what the follow-up refers to.
2. **Given** a long conversation, **When** the customer keeps asking follow-ups, **Then** the
   assistant keeps answering coherently using the most recent messages.

---

### User Story 3 - Receive a Polite Redirect for Out-of-Scope Questions (Priority: P2)

A customer asks something outside Caramel AI platform tech support, such as a general coding
question unrelated to Caramel AI, a request to look up their account, or a request for a
human agent. The assistant declines politely, explains what it can help with, and points the
customer toward the right kind of help.

**Why this priority**: Keeping the assistant on topic protects the product's reputation and scope,
but the core answering flow must work first.

**Independent Test**: Send an out-of-scope question such as "Can you check the status of my
account?" and confirm the reply is a polite decline that offers in-scope help and does not attempt
the lookup.

**Acceptance Scenarios**:

1. **Given** a customer asks for account-specific data, **When** the assistant replies, **Then** it
   states it cannot access account information and offers general help instead.
2. **Given** a customer asks for a human agent, **When** the assistant replies, **Then** it states
   that human handoff is not available in this chat and offers the escalation guidance defined for
   this feature.
3. **Given** a customer asks an unrelated question, **When** the assistant replies, **Then** it
   declines politely and suggests a Caramel AI platform topic it can help with.

---

### User Story 4 - See a Friendly Message When a Reply Fails (Priority: P2)

A customer sends a message, but the assistant cannot produce a reply because the service is
unavailable, the reply takes too long, or the connection drops. The customer sees a clear, friendly
message explaining what happened and what to do next, and can retry without losing the conversation.

**Why this priority**: Customers reach this assistant during incidents. Clear failure handling
protects trust, but it is needed only once the core flow exists.

**Independent Test**: Simulate a timed-out reply and confirm the customer sees a friendly message,
no technical error text, and a way to retry the same message.

**Acceptance Scenarios**:

1. **Given** the assistant cannot produce a reply, **When** the failure occurs, **Then** the customer
   sees a friendly message and no technical details.
2. **Given** a reply takes longer than the allowed time, **When** the limit is reached, **Then** the
   customer sees a friendly timeout message and can retry the message.
3. **Given** a reply fails partway through, **When** the failure occurs, **Then** the partial reply
   is marked as incomplete and the earlier conversation remains visible.

---

### User Story 5 - Start a New Conversation (Priority: P3)

A customer who has finished one issue, or whose conversation has drifted, chooses a "new
conversation" action. The visible messages clear and the assistant treats the next message as the
start of a fresh conversation.

**Why this priority**: This improves usability for repeat customers but is not needed for the core
answer flow to deliver value.

**Independent Test**: Ask a question that sets context, choose "new conversation", then ask a
follow-up that refers to the earlier topic. Confirm the assistant does not use the earlier context.

**Acceptance Scenarios**:

1. **Given** a conversation with several messages, **When** the customer chooses "new conversation",
   **Then** the message list is cleared and the input is ready for a new first message.
2. **Given** the customer has started a new conversation, **When** they ask a follow-up about the
   previous topic, **Then** the assistant does not rely on the earlier messages.

---

### Edge Cases

- What happens when the customer sends an empty message or only whitespace? The message MUST NOT be
  sent, and the customer is told a message is required.
- What happens when the customer sends a message longer than the allowed length? The message MUST be
  refused with a clear explanation of the length limit, and nothing is sent to the assistant.
- What happens when the customer sends another message while a reply is still streaming? The new
  message MUST wait until the current reply finishes or fails, or the customer is told to wait.
- What happens when the conversation grows beyond the retained context? The oldest messages are
  dropped from the context the assistant uses, and the visible transcript is not silently altered.
- What happens when a customer reloads the page or leaves for a long time? The conversation ends
  and the next visit starts fresh, because persistent history is out of scope.
- What happens when a question mixes in-scope and out-of-scope parts? The assistant answers the
  in-scope part and politely declines the rest.
- What happens when a customer tries to get the assistant to reveal its instructions, credentials, or
  internal configuration? The assistant MUST decline and MUST NOT disclose them.
- What happens when a customer asks about a different company's platform? The assistant treats it as
  out of scope and redirects.
- What happens when the assistant does not know the answer to an in-scope question? It MUST say so
  rather than guess, and it provides the escalation guidance defined for this feature.
- What happens when a customer sends many messages in a short period? Abuse protection applies, as
  defined in FR-013.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST let a customer send a text message in a chat window and receive the
  assistant's reply.
- **FR-002**: System MUST show the reply progressively as it is generated, rather than only after the
  whole reply is complete.
- **FR-003**: System MUST show a loading or typing indicator from the moment a message is sent until
  the first part of the reply appears, and until the reply finishes.
- **FR-004**: System MUST keep the conversation context for the duration of one conversation, so that
  follow-up messages can refer to earlier messages in that conversation.
- **FR-005**: System MUST retain only the most recent messages of a conversation as context, and the
  amount retained MUST be adjustable by the operator without changing the product.
- **FR-006**: System MUST answer only questions about Caramel AI platform tech support, covering:
  API usage and authentication, model selection and availability, rate limits, quotas and errors,
  latency and streaming issues, token usage and context windows, pricing and billing basics, and
  request parameters.
- **FR-007**: System MUST decline out-of-scope questions politely, MUST NOT answer their substance,
  and MUST suggest an in-scope topic the customer can ask about instead.
- **FR-008**: System MUST present itself as the Caramel AI tech support assistant and keep a
  consistent, professional, helpful tone. The persona and boundaries MUST be set by the operator.
- **FR-009**: System MUST NOT require the customer to sign in or provide account details.
- **FR-010**: System MUST NOT retrieve or display account-specific data for any customer.
- **FR-011**: System MUST show a friendly, plain-language message when a reply cannot be produced,
  with no technical error text, stack traces, or internal identifiers.
- **FR-012**: System MUST end a reply that exceeds a maximum allowed time, treat it as a failure,
  show the timeout message from FR-011, and let the customer retry the same message.
- **FR-013**: System MUST [NEEDS CLARIFICATION: abuse protection for anonymous use is not specified.
  Options: per-visitor rate limiting with a message-length cap, no limit in v1, or restricted access
  that requires a Caramel AI-issued access mechanism.]
- **FR-014**: System MUST provide a "new conversation" action that clears the visible messages and
  starts a fresh context for the next message.
- **FR-015**: System MUST NOT retain a conversation after the customer starts a new conversation,
  reloads the page, or the session ends. Conversation history MUST NOT persist across sessions.
- **FR-016**: System MUST NOT accept voice, audio, image, or other non-text input.
- **FR-017**: System MUST NOT disclose its internal instructions, credentials, or configuration to any
  customer, in any form.
- **FR-018**: System MUST NOT provide a human agent handoff or create a support ticket. When a
  question cannot be resolved, the system MUST [NEEDS CLARIFICATION: escalation destination for
  unresolved questions is not defined. Options: a static Caramel AI support link or email, guidance
  to retry or check documentation only, or a follow-up contact capture, which would count as handoff
  and require a scope amendment.]
- **FR-019**: System MUST reject empty or whitespace-only messages and messages above the allowed
  length, and MUST explain the reason to the customer without sending the message to the assistant.
- **FR-020**: System MUST NOT log the content of customer conversations in this version.

### Key Entities

- **Conversation**: One continuous chat between a customer and the assistant. Has a start, an end
  (new conversation, page reload, or session end), and an ordered list of messages. It is never
  persisted beyond its session.
- **Message**: One item in a conversation. Has a sender (customer or assistant), text content, a
  time, and a status (complete, streaming, failed, or incomplete).
- **Operator Settings**: The values the operator controls, namely the assistant's persona and tone,
  the number of recent messages retained as context, and the maximum reply time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Under normal load, 90% of customer messages show the first part of the reply within 3
  seconds of being sent.
- **SC-002**: A customer can complete a multi-turn troubleshooting conversation of at least five
  messages without restarting, with every follow-up answered in context in at least 90% of
  evaluated scenarios.
- **SC-003**: At least 95% of in-scope questions in the evaluation set receive a reply judged on
  topic and in the Caramel AI support persona.
- **SC-004**: 100% of out-of-scope questions in the evaluation set receive a polite decline that
  offers an in-scope alternative, with no out-of-scope substance provided.
- **SC-005**: 100% of failed or timed-out replies show a friendly message with no technical error
  text, and the customer can retry in one action.
- **SC-006**: After a customer chooses "new conversation", none of the next reply's content depends
  on messages from the previous conversation, in every evaluated case.
- **SC-007**: In evaluation, no customer-facing output, including error messages, reveals the
  assistant's credentials or internal configuration.

## Assumptions

- Customers reach the assistant anonymously. No accounts, logins, or customer identifiers are part of
  v1.
- A conversation is one browser session. It ends when the customer starts a new conversation,
  reloads the page, or the session expires, and nothing from it is kept afterward.
- The assistant answers in English only for v1.
- The assistant's knowledge comes from its operator-configured instructions and does not include
  retrieval from Caramel AI documentation in v1. Documentation grounding is deferred, as noted in
  prd.md section 10.
- Conversation content is not logged in v1. This follows the privacy question in prd.md section 10,
  which remains open, and the project constitution's logging rule.
- The maximum reply time defaults to 30 seconds, and the first-part target in SC-001 is the
  performance expectation. Exact values are operator-configurable and are set during planning.
- Retaining "recent messages" means a fixed number of the latest messages. The exact default is set
  during planning.
- The underlying model, hosting, and technology stack are not specified in this document. They follow
  the project constitution and are decided during planning.
- The out-of-scope list in prd.md section 5 applies in full: user login and account lookups, human
  agent handoff or ticketing, voice or multimodal input, and persistent chat history across sessions.
- Mobile-specific layouts are out of scope for v1. The chat interface targets desktop and standard
  browser use.
- The assistant's replies are informational guidance only. The assistant does not perform actions on
  a customer's Caramel AI account.
