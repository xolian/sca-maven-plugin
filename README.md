# sca-maven-plugin
Fortify SCA Maven Plugin: sca-maven-plugin

#### Maven Goals
If you need to resolve your pom properties and other values `mvn help:effective-pom`.  Below are default goals to deploy your plugin.

```
mvn clean compile package deploy 
```

#### M2 Settings
Only use a personal access token in `settings.xml` that is scoped to the __repo__ and __user:email__, and includes
