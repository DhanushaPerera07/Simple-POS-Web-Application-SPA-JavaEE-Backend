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

        resp.setHeader("Access-Control-Allow-Origin",CommonConstants.FRONTEND_URL);
        resp.setContentType("application/xml");
        try (PrintWriter out = resp.getWriter();) {

            try {
                Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
                Connection connection = cp.getConnection();

                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM customer");

                out.println("<customers>");

                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    String address = rst.getString(3);
                    String email = rst.getString(4);
                    String contact = rst.getString(5);

                    /* Generate the table data row */
                    out.println("<customer>" +
                            "<id>" + id + "</id>" +
                            "<name>" + name + "</name>" +
                            "<address>" + address + "</address>" +
                            "<email>" + email + "</email>" +
                            "<contact>" + contact + "</contact>" +
                            "</customer>");
                }

                out.println("</customers>");
                connection.close();

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


        }


    }
}
