# AppEngine Hibernate

A library of components to support Hibernate on AppEngine at scale.


## Usage

Include the appengine-hibernate library as a dependency:

```
<dependencies>
    <dependency>
        <groupId>com.bedatadriven.appengine</groupId>
        <artifactId>appengine-hibernate</artifactId>
        <version>0.4</version>
    </dependency>
</dependencies>
<repositories>

</repositories>
```

To use the CloudSqlConnectionProvider, add the following to your persistence.xml descriptor:

```
  <property name="hibernate.connection.provider_class" 
            value="com.bedatadriven.appengine.cloudsql.CloudSqlConnectionProvider"/>
```

