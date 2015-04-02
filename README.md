# AppEngine Hibernate

A library of components to support Hibernate on AppEngine at scale.

## Functionality

### Query Timeouts

A few runaway queries; or queries that have an unexpectedly long running time, often only under specific
load conditions, can bring an otherwise well-engineered AppEngine application to its knees.

This is because, unlike Datastore queries or other calls to AppEngine services, `SoftDeadlineException`s are *not*
thrown during CloudSQL queries. This can lead to a vicious cycle under peak load:

1. Queries that exceed that 60-minutes request lead the AppEngine scheduler to terminate the VM, but without
   giving the application the opportunity to close the SQL connection.
   
2. As clients retry and load increases, new instances are started and the same runaway query is executed, but
   now within the context of a loading request where there is even less time to complete the query. More queries
   exceed the request time limit, more VMs are terminated, and the number of loading requests grow.
   
3. Meanwhile, the slow queries, continue to run on MySQL, starving the database for both connections and 
   processing power, increasing response latency, and increasing the chances that even normal queries are unable
   to finish on time. 
   
   
The `CloudSqlConnectionProvider` addresses this problem by running all queries in a seperate worker thread, and 
cancelling the queries if the request approaches the time limit.

### Connection Pooling

Existing connection pool libraries like C3P0 are poorly suited to the AppEngine context for two reasons:
1. AppEngine instances are short-lived and can be started or stopped at anytime, leaving no opportunity to 
   close idling connections before the instance shuts down.
 
2. In most cases, AppEngine does not allow the creation of 'background' threads that outlive requests, which
   means that connection pools aren't able to periodically inspect their idling connections and evict closed 
   connections.
   
   
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

