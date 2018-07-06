FROM openjdk:10-jre

LABEL project="Linksmart(R) IoT Agents"
LABEL "project.code"=LA
LABEL organization="Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V."
LABEL institute=FIT
LABEL department=UCC
LABEL group=UCUC
LABEL "org.code"="FhG.FIT.UCC.UCUC"
LABEL version="2.0"
LABEL distribution=standard
LABEL maintainer="Jose Angel Carvajal Soto <carvajal@fit.fhg.de>"


ADD target/*.jar ./

# starting the agent
ENTRYPOINT ["java","--add-modules", "java.xml.bind", "-cp","./*", "org.springframework.boot.loader.PropertiesLauncher"]