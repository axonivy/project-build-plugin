# AHEAD of Time components

Let's build Ahead-of-Time components describing re-usable building blocks of an Ivy Project.

Why?
Ivy UI editors today enforce a heavy java-backend to be running, just to find out valid combinations callable-subs. This evaluation is resource expensive and requiring a full java stack up and runnning.
AI harnesses need can be made aware of valid data-structures with json-schemas. We have a lot of them for the basic data-models, but little to supply actual Ivy Project components.

What components?
- process
  - callable sub-process: process path + signature, caller-supplied `params` and returned `results` as JSON Schema object schemas; property names are param/result names and `required` lists mandatory properties
  - Example catalog: [subprocess.json](subprocess.json)
  - program-interface impl; and especially their data (mvp; manually contributed to smart-workflow product.zip)
  - triggerable starts: including input parameterse
  - signature starts: including payload/signal being listened to
- config
  - variables.yaml -> schema reflecting the current project settings, keys + defaults, descriptions, ...
- ui
  - form-components
  - jsf-components
  - full dialogs?
- connect
  - rest-client paths/payload -> benefit over openapi.json?
  - webservice-clients method+in/out data -> json-schema!
  - databases?
  - persistence-unit? -> rather a jandex thing?

Roll-out
1. Market end point for AI harness to fetch + Listing of components for the end-user on the frontend.
2. Primary source for light-variants of our editors: e.g. process-editor / variables

Role-models
1. smart-workflow: program elements, chat component
2. msgraph: callable subs

---

Considerations
- param/result data: only show full schema for data-class and basic types at first. JavaTypes from connector can be huge and are a sign that they need a better use-case driven cutting
- write the "caller" element 'type' hint > website needs to render it just like the "extension" palette
- create a generic schema for all process callables? (callsub, trigger-start, signal-start, program, rest, web)
- only list 'connector' and 'program' with icon types in a first version?!

Editor Feed
- for an editor-integration data feed, we would need all data though (all params, results, ...)
- we could/should design this like today in an extra 'type'/lsp service -> share the same information among multiple edito scenarios

Explore
- use jira-connector or another as reference beside microsoft. Pick one that doesn't expose internal data-structure from vendors.
