package com.bedatadriven.appengine.cloudsql;


import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;
import org.hibernate.annotations.QueryHints;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Tuple;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

@Path("/test")
public class TestResources {
    
    private static final Logger LOGGER = Logger.getLogger(TestResources.class.getName());
    
    @POST
    @Path("reset")
    public Response resetDatabase() {

        EntityManager em = Hibernate.createEntityManager();
        em.getTransaction().begin();
        em.createNativeQuery("delete from greeting").executeUpdate();
        em.createNativeQuery("delete from author").executeUpdate();
        em.getTransaction().commit();
        return Response.ok().build();
    }

    @POST
    @Path("greeting")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createGreeting(@FormParam("author") String authorName, @FormParam("content") String content) {

        LOGGER.fine("Hit endpoint");
        
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

        // now list the most recent posts
        
        
        
        
        
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
//
//    @GET
//    @Path("query")
//    @Produces(MediaType.TEXT_PLAIN)
//    public Response queryLatest

    @GET
    @Path("greetings/latest")
    @Produces(MediaType.TEXT_PLAIN)
    public Response queryLatestGreetings(@QueryParam("maxResults") int maxResults) {
        EntityManager em = Hibernate.createEntityManager();
        final List<Tuple> greetings = em
                .createQuery("SELECT g.id, g.date, g.author.name, g.content FROM Greeting g ORDER by g.date DESC", Tuple.class)
                .setHint(QueryHints.READ_ONLY, "true")
                .setMaxResults(maxResults == 0 ? 500 : maxResults)
                .getResultList();
        
        em.close();
        return RequestStats.wrap(Response.ok(toString(greetings)));
    }
    
    @GET
    @Path("stats")
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
