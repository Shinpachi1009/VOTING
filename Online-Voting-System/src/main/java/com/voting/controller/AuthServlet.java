package com.voting.controller;

import com.voting.dao.AuditLogDAO;
import com.voting.dao.UserDAO;
import com.voting.model.AuditLog;
import com.voting.model.User;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String action = request.getParameter("action");
        
        if ("login".equals(action)) {
            processLogin(request, response);
        } else if ("logout".equals(action)) {
            processLogout(request, response);
        } else {
            response.sendRedirect("login.jsp?error=Invalid action");
        }
    }
    
    private void processLogin(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String rememberMe = request.getParameter("rememberMe");
        
        try {
            Connection conn = (Connection) getServletContext().getAttribute("DBConnection");
            UserDAO userDAO = new UserDAO(conn);
            AuditLogDAO auditLogDAO = new AuditLogDAO(conn);
            
            User user = userDAO.getUserByUsername(username);
            
            if (user == null || !UserDAO.verifyPassword(password, user.getPasswordHash())) {
                // Log failed login attempt
                AuditLog auditLog = new AuditLog();
                auditLog.setActionType("LOGIN_FAILED");
                auditLog.setDetails("Failed login attempt for username: " + username);
                auditLog.setIpAddress(request.getRemoteAddr());
                auditLogDAO.logAction(auditLog);
                
                response.sendRedirect("login.jsp?error=Invalid username or password");
                return;
            }
            
            if (!"ACTIVE".equals(user.getStatus())) {
                response.sendRedirect("login.jsp?error=Your account is not active");
                return;
            }
            
            // Update last login time
            userDAO.updateLastLogin(user.getUserId());
            
            // Create session
            HttpSession session = request.getSession();
            session.setAttribute("user", user);
            
            // Set session timeout (30 minutes)
            session.setMaxInactiveInterval(30 * 60);
            
            // Log successful login
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(user.getUserId());
            auditLog.setActionType("LOGIN_SUCCESS");
            auditLog.setDetails("User logged in successfully");
            auditLog.setIpAddress(request.getRemoteAddr());
            auditLogDAO.logAction(auditLog);
            
            // Redirect based on role
            if ("ADMIN".equalsIgnoreCase(user.getRoleName())) {
                response.sendRedirect("admin/dashboard.jsp");
            } else if ("ELECTION_OFFICER".equalsIgnoreCase(user.getRoleName())) {
                response.sendRedirect("officer/dashboard.jsp");
            } else {
                response.sendRedirect("voter/dashboard.jsp");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("login.jsp?error=Database error");
        }
    }
    
    private void processLogout(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        if (session != null) {
            User user = (User) session.getAttribute("user");
            
            if (user != null) {
                try {
                    // Log logout action
                    Connection conn = (Connection) getServletContext().getAttribute("DBConnection");
                    AuditLogDAO auditLogDAO = new AuditLogDAO(conn);
                    
                    AuditLog auditLog = new AuditLog();
                    auditLog.setUserId(user.getUserId());
                    auditLog.setActionType("LOGOUT");
                    auditLog.setDetails("User logged out");
                    auditLog.setIpAddress(request.getRemoteAddr());
                    auditLogDAO.logAction(auditLog);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            
            session.invalidate();
        }
        
        response.sendRedirect("login.jsp?message=You have been logged out");
    }
}