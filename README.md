SemFacet
=====

SemFacet is a semantic facet based search system.


Project dependencies/requirements
=====

- JavaSE 8
- Maven
- Apache Tomcat

How to compile the project?
=====

- In order to compile the project make sure that you have JavaSE 8 and the latest Maven installed on your computer.
- If you use Eclipse IDE, then you will need to convert the project into Maven project.
- The project depends on 2 libraries that are not in central maven repository: JRDFox and Pagoda. These libraries can be found in WebContent/WEB-INF/lib directory. You need to install those libraries in the local maven repository. This can be done by executing the script which is provided in WebContent/WEB-INF/lib/install_3dparty_libs_to_maven .


How to package the project?
===

Once the project compiles it can be packaged as a war file. In order to do that you should execute the following script in the root directory:
mvn clean package.

Then, the war file can be found in target/ directory. By default the auto generated file will have a version number attached to it (i.e. semFacet-0.0.1-SNAPSHOT.war). It is recommended to rename this file into semFacet.war before loading it on the tomcat server.
