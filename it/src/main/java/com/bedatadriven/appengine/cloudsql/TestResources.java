package com.bedatadriven.appengine.cloudsql;


import org.hibernate.annotations.QueryHints;
import org.hibernate.ejb.HibernateEntityManager;

import javax.persistence.*;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

@Path("/test")
public class TestResources {
    
    private static final Logger LOGGER = Logger.getLogger(TestResources.class.getName());
    
    
    @GET
    @Path("exception")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testException() {

        EntityManager em = Hibernate.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("select a from non_existant_table").getResultList();
            em.getTransaction().commit();
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Expected exception").build();
            
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            printWriter.flush();
            
            if(e instanceof PersistenceException) {
                return Response.ok(writer.toString()).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(writer.toString()).build();
            }
        }
    }

    @POST
    @Path("greeting")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createGreeting(@FormParam("author") String authorName, @FormParam("content") String content) {

        EntityManager em = Hibernate.createEntityManager();

        em.getTransaction().begin();
        Author author;
        try {
            author = em.createQuery("SELECT a FROM Author a WHERE a.name = :name", Author.class)
                    .setParameter("name", authorName)
                    .getSingleResult();
            
        } catch (NoResultException e) {
            author = new Author();
            author.setName(authorName);
            em.persist(author);
        }
        
        em.persist(new Greeting(author, new Date(), content));

        em.getTransaction().commit();
        em.close();
        
        return RequestStats.wrap(Response.ok());
    }

    @GET
    @Path("greetings")
    @Produces(MediaType.TEXT_PLAIN)
    public Response queryGreetings(@QueryParam("maxResults") int maxResults) {
        EntityManager em = Hibernate.createEntityManager();
        final List<Tuple> greetings = em
                .createQuery("SELECT g.id, g.date, g.author.name, g.content FROM Greeting g ", Tuple.class)
                .setHint(QueryHints.READ_ONLY, "true")
                .setMaxResults(maxResults == 0 ? 500 : maxResults)
                .getResultList();
        em.close();
        
        return RequestStats.wrap(Response.ok(toString(greetings)));
    }

    
    @GET
    @Path("greetings/latest")
    @Produces(MediaType.TEXT_PLAIN)
    public Response queryLatestGreetings(@QueryParam("maxResults") int maxResults) {

        EntityManager em = Hibernate.createEntityManager();
        try {
            final List<Tuple> greetings = em
                    .createQuery("SELECT g.id, g.date, g.author.name, g.content FROM Greeting g ORDER by g.date DESC", Tuple.class)
                    .setHint(QueryHints.READ_ONLY, "true")
                    .setMaxResults(maxResults == 0 ? 500 : maxResults)
                    .getResultList();
            em.close();


            return RequestStats.wrap(Response.ok(toString(greetings)));
            
        } catch (QueryTimeoutException e) {
            LOGGER.severe("Caught QueryTimeoutException");
            
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }
    }
    
    @GET
    @Path("numConnections")
    public String queryStats(@Context HttpServletRequest req) {
        Enumeration headerNames = req.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String header = (String)headerNames.nextElement();
            LOGGER.info("Header " +  header + " = " + req.getHeader(header));
        }
        EntityManager em = Hibernate.createEntityManager();
        int connectionCount = em.createNativeQuery("show processlist").getResultList().size();
        em.close();

        return Integer.toString(connectionCount);
    }
    
    
    private String toString(List<Tuple> greetings) {
        StringBuilder sb = new StringBuilder();
        for (Tuple greeting : greetings) {
            sb.append(String.format("[%d] On %tF %s wrote: %s\n",
                    (Long)greeting.get(0),
                    (Date)greeting.get(1),
                    (String)greeting.get(2),
                    (String)greeting.get(3)));
        }
        return sb.toString();
    }
}
