# A descriptive title

Briefly describe changes proposed in this pull request:
- a
- b 

---
## Troubleshooting

Fix # (see https://help.github.com/en/articles/closing-issues-using-keywords)

**Expected behavior**

**Actual behavior**

**Logs, error output, or stacktrace**

**Steps to reproduce the behavior**

**Tasks**

Include specific tasks in the order they need to be done in. Include links to specific lines of code where the task should happen at. 

- [ ] task 1
- [ ] task 2
- [ ] task 3

---
## Crossing T's and dotting I's

Please follow these checklists to help prevent any unexpected issues from being introduced by the changes in this pull request. If an item does not apply then indicate so by surrounding the line item with `~~` to strikethrough the text. See [basic writing and formatting syntax](https://docs.github.com/en/github/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax) for more information.

### I. Web service and data model checklist

Please follow these checks if any changes were made to any classes in the web, service, or persistence layers.

**Code checks:**
- [ ] Endpoints were tested to ensure their integrity.
- [ ] Unit tests were updated in relation to updates to the mocked test data.

If no unit tests were updated or added, then please explain why: [insert details here]

### II. Neo4j models and database schema checklist:
- [ ] Neo4j persistence models were changed.
- [ ] The graph database produces the expected changes to models, relationships, and/or property names. [provide screenshot of updated elements in graph db below]

### III. Message handlers checklist:
- [ ] Changes in this PR affect the workflow of incoming messages.
- [ ] Messages are following the expected workflow when published to the topic(s) changed or introduced in this pull request.

Please describe how the workflow and messaging was tested/simulated:

**Describe your testing environment:**

- NATS: [local, local docker, dev server, production]
- Neo4j: [local, local docker, dev server, production]
- SMILE Server: [local, local docker, dev server, production]
- Message publishing simulation: [nats cli, docker nats cli, smile publisher tool, other (describe below)]

Other: [insert details on how messages were published or simulated for testing]

### IV. Configuration and/or permissions checklist:
- [ ] New topics were introduced.
- [ ] The topics and appropriate permissions were updated in [smile-configuration](https://github.mskcc.org/cmo/smile-configuration).
- [ ] If applicable, a new account was set up and the account credentials and keys are checked into [smile-configuration](https://github.mskcc.org/cmo/smile-configuration).
- [ ] Account credentials and keys were shared with the appropriate parties.

---
### Screenshots

---
### General checklist:
- [ ] All requested changes and comments have been resolved.

