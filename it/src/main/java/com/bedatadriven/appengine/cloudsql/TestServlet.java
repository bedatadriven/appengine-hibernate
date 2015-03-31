/**
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bedatadriven.appengine.cloudsql;

import com.google.appengine.api.utils.SystemProperty;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import javafx.scene.paint.Stop;
import org.hibernate.annotations.QueryHints;
import org.hibernate.cfg.AvailableSettings;

import javax.persistence.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class TestServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(TestServlet.class.getName());

    private EntityManagerFactory emf;

    @Override
    public void init() throws ServletException {
        super.init();

        Stopwatch stopwatch = Stopwatch.createStarted();
        emf = JPA.newFactory();
        LOGGER.info("EntityManagerFactory created in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if("true".equals(req.getParameter("reset"))) {
            resetDatabase(resp);

        } else {
            String authorName = req.getParameter("author");
            String content = req.getParameter("content");

            // Insert a few rows.
            EntityManager em = emf.createEntityManager();
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

            // No need to commit read-only transaction - avoid hibernate flushing cycle
            em.getTransaction().rollback();

            em.close();
        }
    }

    private void resetDatabase(HttpServletResponse resp) {
        try {
            CloudSqlConnectionProvider connectionProvider = new CloudSqlConnectionProvider();
            Connection connection = connectionProvider.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("DELETE DATABASE IF EXISTS hibernate");
            statement.executeUpdate("CREATE DATABASE hibernate");
            statement.close();
            connection.close();

            JPA.newAutoUpdatingFactory();
        } catch (SQLException e) {
            resp.setStatus(500);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

        res.setContentType("text/plain");


        // List all the rows.
        EntityManager em = emf.createEntityManager();

        if(!Strings.isNullOrEmpty(req.getParameter("deadlock"))) {
            triggerDeadlockToTestQueryTimeLimits(em);
        }

        List result = query(em, req);

        for (Object g : result) {
            res.getWriter().println(g.toString());
        }
        em.close();
    }

    private void triggerDeadlockToTestQueryTimeLimits(EntityManager em) {
        if(ThreadLocalRandom.current().nextBoolean()) {
            em.createNativeQuery("GET_LOCK('lock1', 90)").executeUpdate();
            em.createNativeQuery("GET_LOCK('lock2', 90)").executeUpdate();
        } else {
            em.createNativeQuery("GET_LOCK('lock2', 90)").executeUpdate();
            em.createNativeQuery("GET_LOCK('lock1', 90)").executeUpdate();            
        }
    }

    private List query(EntityManager em, HttpServletRequest req) {
        if(req.getParameterMap().containsKey("totals")) {
            // This should trigger writing a temporary table, which is always good fun.
            return em.createNativeQuery("select a.name, sum(d.char_count) from author a left join " +
                    "(select b.name, length(g.content) char_count from greeting g left join author b on (g.author_id = b.id)) as d " +
                    " on (a.name = d.name) " +
                    "group by a.name")
                    .getResultList();

        } else {
            // This can also be a nice long query given that we haven't indexed date.
            return em
                    .createQuery("SELECT g.id, g.date, g.author.name, g.content FROM Greeting g " +
                            "ORDER BY g.date DESC", Tuple.class)
                    .setHint(QueryHints.READ_ONLY, "true")
                    .setMaxResults(maxResults(req))
                    .getResultList();
        }

    }

    private int maxResults(HttpServletRequest req) {
        if(req.getParameterMap().containsKey("maxResults")) {
            return Integer.parseInt(req.getParameter("maxResults"));
        }
        return 500;
    }
}