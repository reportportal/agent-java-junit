# agent-java-junit
### Report Portal agent for JUnit 4

 [ ![Download](https://api.bintray.com/packages/epam/reportportal/agent-java-junit/images/download.svg) ](https://bintray.com/epam/reportportal/agent-java-junit/_latestVersion)
 
[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![UserVoice](https://img.shields.io/badge/uservoice-vote%20ideas-orange.svg?style=flat)](https://rpp.uservoice.com/forums/247117-report-portal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)

#### Overview: How to Add ReportPortal Logging to Your JUnit Java Project

1. [Configuration](https://github.com/reportportal/agent-java-junit#configuration): Create/update the **_reportportal.properties_** configuration file
2. [Logback Framework](https://github.com/reportportal/agent-java-junit#logback-framework): For the Logback framework:
   a. Create/update the **_logback.xml_** file
   b. Add ReportPortal / Logback dependencies to your project POM
3. [Log4J Framework](https://github.com/reportportal/agent-java-junit#log4j-framework): For the Log4J framework:
   a. Add ReportPortal / Log4J dependencies to your project POM
4. [Images and Files](https://github.com/reportportal/agent-java-junit#images-and-files): Logging images and files

### Configuration

#### Create/update the reportportal.properties configuration file:
Create or update a file named reportportal.properties in your Java project in source folder src/main/resources:

```
[reportportal.properties]
rp.endpoint = http://localhost:8080
rp.uuid = e0e541d8-b1cd-426a-ae18-b771173c545a
rp.launch = default_JUNIT_AGENT
rp.project = default_personal
```

* The value of the **rp.endpoint** property is the URL for the report portal server(actual link).
* The value of the **rp.uuid** property can be found on your report portal user profile page.
* The value of the **rp.project** property must be set to one of your assigned projects.
* The value of the **rp.launch** property is a user-selected identifier for the source of the report output (i.e. - the Java project)

### Logback Framework

#### Create/update the logback.xml file:

In your project, create or update a file named logback.xml in the src/main/resources folder, adding the ReportPortal elements:

```xml
[logback.xml]
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
 
    <!-- Send debug messages to System.out -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <!-- By default, encoders are assigned the type ch.qos.logback.classic.encoder.PatternLayoutEncoder -->
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{5} - %thread - %msg%n</pattern>
        </encoder>
    </appender>
 
    <appender name="RP" class="com.epam.reportportal.logback.appender.ReportPortalAppender">
        <encoder>
             <!--Best practice: don't put time and logging level to the final message. Appender do this for you-->
            <pattern>%d{HH:mm:ss.SSS} [%t] %-5level - %msg%n</pattern>
            <pattern>[%t] - %msg%n</pattern>
        </encoder>
    </appender>
 
     <!--'additivity' flag is important! Without it logback will double-log log messages-->
    <logger name="binary_data_logger" level="TRACE" additivity="false">
        <appender-ref ref="RP"/>
    </logger>
 
    <!-- By default, the level of the root level is set to DEBUG -->
    <root level="DEBUG">
        <appender-ref ref="RP"/>
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

#### Add ReportPortal / Logback dependencies to your project POM:

```xml
[pom.xml]
<project ...>
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.target>1.8</maven.compiler.target>
    <maven.compiler.source>1.8</maven.compiler.source>  	
  </properties>
  
  <repositories>
    <repository>
      <id>bintray</id>
      <url>http://dl.bintray.com/epam/reportportal</url>
    </repository>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
 
  <dependencies>
    <dependency>
      <groupId>ch.qos.logback</groupId>
      <artifactId>logback-classic</artifactId>
      <version>1.2.3</version>
    </dependency>
    <dependency>
      <groupId>com.epam.reportportal</groupId>
      <artifactId>agent-java-junit</artifactId>
      <version>3.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>com.epam.reportportal</groupId>
      <artifactId>logger-java-logback</artifactId>
      <version>4.0.0</version>
    </dependency>
  </dependencies>
 
  <build>
    <pluginManagement>
      <plugins>
        <!-- Add this if you plan to import into Eclipse -->
        <plugin>
          <groupId>org.eclipse.m2e</groupId>
          <artifactId>lifecycle-mapping</artifactId>
          <version>1.0.0</version>
          <configuration>
            <lifecycleMappingMetadata>
              <pluginExecutions>
                <pluginExecution>
                  <pluginExecutionFilter>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <versionRange>[1.0.0,)</versionRange>
                    <goals>
                      <goal>properties</goal>
                    </goals>
                  </pluginExecutionFilter>
                  <action>
                    <execute />
                  </action>
                </pluginExecution>
              </pluginExecutions>
            </lifecycleMappingMetadata>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
      <!-- This provides the path to the Java agent -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.1.1</version>
        <executions>
          <execution>
            <id>getClasspathFilenames</id>
            <goals>
              <goal>properties</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.22.0</version>
        <configuration>
          <argLine>-javaagent:${com.nordstrom.tools:junit-foundation:jar}</argLine>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### Log4J Framework

#### Add ReportPortal / Log4J dependencies to your project POM:

```xml
[pom.xml]
<project ...>
  <repositories>
    <repository>
      <id>bintray</id>
      <url>http://dl.bintray.com/epam/reportportal</url>
    </repository>
    <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
 
  <dependencies>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
      <version>2.11.0</version>
    </dependency>
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-api</artifactId>
      <version>2.11.0</version>
    </dependency>
    <dependency>
      <groupId>com.epam.reportportal</groupId>
      <artifactId>agent-java-junit</artifactId>
      <version>3.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>com.epam.reportportal</groupId>
      <artifactId>logger-java-log4j</artifactId>
      <version>4.0.1</version>
    </dependency>
  </dependencies>
 
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.6.0</version>
          <configuration>
            <source>1.8</source>
            <target>1.8</target>
          </configuration>
        </plugin>
        <!-- Add this if you plan to import into Eclipse -->
        <plugin>
          <groupId>org.eclipse.m2e</groupId>
          <artifactId>lifecycle-mapping</artifactId>
          <version>1.0.0</version>
          <configuration>
            <lifecycleMappingMetadata>
              <pluginExecutions>
                <pluginExecution>
                  <pluginExecutionFilter>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <versionRange>[1.0.0,)</versionRange>
                    <goals>
                      <goal>properties</goal>
                    </goals>
                  </pluginExecutionFilter>
                  <action>
                    <execute />
                  </action>
                </pluginExecution>
              </pluginExecutions>
            </lifecycleMappingMetadata>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
    <plugins>
      <!-- This provides the path to the Java agent -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.1.1</version>
        <executions>
          <execution>
            <id>getClasspathFilenames</id>
            <goals>
              <goal>properties</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.22.0</version>
        <configuration>
          <argLine>-javaagent:${com.nordstrom.tools:junit-foundation:jar}</argLine>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### Images and Files

http://reportportal.io/docs/Logging-Integration%3Elog-message-format

In addition to text log messages, ReportPortal has the ability to record images and file contents. The link above documents the formats supported by the report portal test listener for representing these artifacts.

# Copyright Notice

Licensed under the [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) license (see the LICENSE.md file).
