# AHEAD of Time components

Let's build Ahead-of-Time components describing re-usable building blocks of an Ivy Project.

Why?
Ivy UI editors today enforce a heavy java-backend to be running, just to find out valid combinations callable-subs. This evaluation is resource expensive and requiring a full java stack up and runnning.
AI harnesses need can be made aware of valid data-structures with json-schemas. We have a lot of them for the basic data-models, but little to supply actual Ivy Project components.

What components?
- process
  - callable sub-process: process-path+signature. in-/output data in raw json-schema. + Tags (connector/tool/...)
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


