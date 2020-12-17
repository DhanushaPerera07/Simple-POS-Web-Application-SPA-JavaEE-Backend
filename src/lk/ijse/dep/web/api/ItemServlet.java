package lk.ijse.dep.web.api;

import lk.ijse.dep.web.commonConstants.CommonConstants;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author : Dhanusha Perera
 * @since : 15/12/2020
 **/
@WebServlet(name = "ItemServlet", urlPatterns = "/items")
public class ItemServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setContentType("application/xml");
        try (PrintWriter out = resp.getWriter();) {

            try {
                Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
                Connection connection = cp.getConnection();

                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM item");

                out.println("<items>");

                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    int quantity = rst.getInt(3);
                    String unitPrice = rst.getBigDecimal(4).setScale(2).toString();
                    String description = rst.getString(5);

                    /* Generate the table data row */
                    out.println("<item>" +
                            "<id>" + id + "</id>" +
                            "<name>" + name + "</name>" +
                            "<quantity>" + quantity + "</quantity>" +
                            "<unitPrice>" + unitPrice + "</unitPrice>" +
                            "<description>" + description + "</description>" +
                            "</item>");
                }

                out.println("</items>");
                /* ./table body */

                connection.close();

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


        }
    }
}
