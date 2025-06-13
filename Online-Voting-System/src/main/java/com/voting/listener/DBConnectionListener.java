package com.voting.listener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class DBConnectionListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Load Derby driver
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            
            // Create connection
            Connection conn = DriverManager.getConnection("jdbc:derby:C:\\Users\\uriur\\VotingDB;create=true");
            
            // Store connection in application scope
            ServletContext ctx = sce.getServletContext();
            ctx.setAttribute("DBConnection", conn);
            
            System.out.println("Database connection initialized for Online Voting System");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Connection conn = (Connection) sce.getServletContext().getAttribute("DBConnection");
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed");
                
                // Shutdown Derby
                try {
                    DriverManager.getConnection("jdbc:derby:;shutdown=true");
                } catch (SQLException e) {
                    // Expected exception on shutdown
                    if (!"XJ015".equals(e.getSQLState())) {
                        throw e;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}