# Appengine MySQL

## CloudSQL Hibernate

Specialized ConnectionProvider which limits query running to avoid exceeding the request time limit and 
sinking the whole VM, leading to a death spiral as AppEngine tries to bring up replacement instances.


