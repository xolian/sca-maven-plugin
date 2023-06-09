# sca-maven-plugin
Fortify SCA Maven Plugin: sca-maven-plugin

#### Maven Goals
If you need to resolve your pom properties and other values `mvn help:effective-pom`.  Below are default goals to deploy your plugin.

```
mvn clean compile package deploy 
```

#### M2 Settings
Only use a personal access token in `settings.xml` that is scoped to the __repo__ and __user:email__, and includes

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <pluginGroups>
    <pluginGroup>com.fortify.sca.plugins.maven</pluginGroup>
  </pluginGroups>
    <servers>
        <server>
            <id>github</id>
            <password>INSERT GITHUB PERSONAL ACCESS TOKEN</password>
        </server>
    </servers>
</settings>
```

#### Download and install plugin

`mvn dependency:get -DrepositoryUrl=https://maven.pkg.github.com/xolian -Dartifact=com.fortify.sca.plugins.maven:sca-maven-plugin:22.2.1`

## TODO:
* [Deploying JAR files on GitLab](https://medium.com/credera-engineering/deploying-jar-files-to-gitlab-using-the-maven-release-plugin-via-ci-cd-d17c64c2ea3)

* [GitLab as Maven Registry](https://kb.wisc.edu/shared-tools/page.php?id=114481)
