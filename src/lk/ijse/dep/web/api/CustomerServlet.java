package lk.ijse.dep.web.api;

import lk.ijse.dep.web.commonConstants.CommonConstants;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

/**
 * @author : Dhanusha Perera
 * @since : 11/12/2020
 **/
@WebServlet(name = "CustomerServlet", urlPatterns = "/customers")
public class CustomerServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        /* To get no of active connections */
//        System.out.println(cp.getNumActive());
//        System.out.println(cp.getNumIdle());

        resp.setHeader("Access-Control-Allow-Origin",CommonConstants.FRONTEND_URL);
        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter();) {
            out.println("<div>");

            out.println("<h1>Customer servlet !</h1>");
//            out.println("<h2>"+ getServletContext().getAttribute("cp") +"</h2>");


            try {
                Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
                Connection connection = cp.getConnection();
//                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ijse", "root", "root");
                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM customer");

                /* table */
                out.println("<table style='border-collapse: collapse; border: 1px solid black;'>");

                /* table header */
                out.println("<thead>");
                out.println("<tr>");
                out.println("<th>ID</th>");
                out.println("<th>Name</th>");
                out.println("<th>Address</th>");
                out.println("<th>Email</th>");
                out.println("<th>Contact</th>");
                out.println("</tr>");
                out.println("</thead>");
                /* ./table header */



                /* table body */
                out.println("<tbody>");
                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    String address = rst.getString(3);
                    String email = rst.getString(4);
                    String contact = rst.getString(5);

                    /* Generate the table data row */
                    out.println("<tr>" +
                            "<td>" + id + "</td>" +
                            "<td>" + name + "</td>" +
                            "<td>" + address + "</td>" +
                            "<td>" + email + "</td>" +
                            "<td>" + contact + "</td>" +
                            "</tr>");
                }

                out.println("</tbody>");
                /* ./table body */

                connection.close();
                out.println("</table>");
                /* ./table */

                out.println("</div>");

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


        }


    }
}
