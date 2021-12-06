[Ivy]
148CA74B16C580BF 9.3.1 #module
>Proto >Proto Collection #zClass
me0 myWebService Big #zClass
me0 WS #cInfo
me0 #process
me0 @TextInP .webServiceName .webServiceName #zField
me0 @TextInP .implementationClassName .implementationClassName #zField
me0 @TextInP .authenticationType .authenticationType #zField
me0 @TextInP .type .type #zField
me0 @TextInP .processKind .processKind #zField
me0 @AnnotationInP-0n ai ai #zField
me0 @MessageFlowInP-0n messageIn messageIn #zField
me0 @MessageFlowOutP-0n messageOut messageOut #zField
me0 @TextInP .xml .xml #zField
me0 @TextInP .responsibility .responsibility #zField
me0 @StartWS ws0 '' #zField
me0 @EndWS ws1 '' #zField
me0 @PushWFArc f0 '' #zField
me0 @InfoButton f1 '' #zField
>Proto me0 me0 myWebService #zField
me0 ws0 inParamDecl '<String myText> param;' #txt
me0 ws0 outParamDecl '<> result;' #txt
me0 ws0 actionDecl 'base.Data out;
' #txt
me0 ws0 callSignature call(String) #txt
me0 ws0 useUserDefinedException false #txt
me0 ws0 taskData TaskTriggered.PRI=2 #txt
me0 ws0 type base.Data #txt
me0 ws0 @C|.xml '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<elementInfo>
    <language>
        <name>call(String)</name>
    </language>
</elementInfo>
' #txt
me0 ws0 @C|.responsibility Everybody #txt
me0 ws0 81 49 30 30 -29 17 #rect
me0 ws1 337 49 30 30 0 15 #rect
me0 f0 expr out #txt
me0 f0 111 64 337 64 #arcP
me0 f1 @C|.xml '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<elementInfo>
    <language>
        <name>Maven CI/CD plugin Sprint 1
----------------------------------

Done:
- auto download ivyEngine (as dependency provider) from fixed URL or website that lists certain engine
- re-use installed &amp; configured engine
- compile DataClasses, WebServiceProcesses &amp; JavaClasses
- pack ivyProject as IAR and install it in Maven Repository
- build high level ivyProject with IAR dependencies resolved from POM.xml declaration

Future Stories:
- remove library.libraryconfig and replace it with POM.xml as single depdency source
- integrate M2E plugin into designer
- publish plugin to maven central (and make it Open Source)
- Auto deployment: to InMemoryTest- or Staging Server
- Start/stop Server goals (to run selenium or other integration tests)</name>
        <nameStyle>27,0,8
1,7
36,7
5,0,7
347,7
15,0,7
307,7
</nameStyle>
    </language>
</elementInfo>
' #txt
me0 f1 32 146 576 268 -279 -130 #rect
me0 f1 -5972572|-1|-16777216 #nodeStyle
>Proto me0 .webServiceName myWebService #txt
>Proto me0 .type base.Data #txt
>Proto me0 .processKind WEB_SERVICE #txt
>Proto me0 -8 -8 16 16 16 26 #rect
me0 ws0 mainOut f0 tail #connect
me0 f0 head ws1 mainIn #connect
