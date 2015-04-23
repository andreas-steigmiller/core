SemFacet
=====

SemFacet is a semantic facet based search system. The instruction how to use it can be found on the following website: http://www.cs.ox.ac.uk/isg/tools/SemFacet/ 


Project dependencies/requirements
=====

- JavaSE 8
- Maven
- Apache Tomcat 7.0
- Eclipse Luna


How to compile/run the project from source in Eclipse?
=====

- In order to compile the project make sure that you have JavaSE 8 and Eclipse Luna installed on your computer.
- After cloning the repository by command git clone https://github.com/semfacet/core.git, you need to load the project into Eclipse and convert it into Maven project.
- The project depends on two libraries that are not in central maven repository: JRDFox and PAGOdA. These libraries can be found in lib directory. You need to copy both into WebContent/WEB-INF/lib directory and add them to the build path. Alternative, if you have maven installed on your computer, you could execute the scripts provided in WebContent/WEB-INF/lib/install_3dparty_libs_to_maven to add both libraries into your local maven repository. In this case, you need to add both of them to maven dependency and remove them in the build path. Please make sure that you choose the correct version of JRDFox.jar. If the provided JRDFox.jar doesn't work, you might need to follow the instruction in https://github.com/yujiaoz/PAGOdA to compile the project JRDFox on your machine.
- In order to run the project make sure that you have Apache Tomcat 7.0 installed. Open the Server view in Eclipse, create a new server of type Apache Tomcat 7.0 and add the project to the server.
- Start the server and now you should be able to visit http://localhost:8080/semFacet/. 


How to package the project?
===

Once the project compiles it can be packaged as a war file. In order to do that you should execute the following script in the root directory:
mvn clean package.

Then, the war file can be found in target/ directory. By default the auto generated file will have a version number attached to it (i.e. semFacet-0.0.1-SNAPSHOT.war). It is recommended to rename this file into semFacet.war before loading it on the tomcat server.

Alternatively, you can export the project as an external War file in Eclipse. 


