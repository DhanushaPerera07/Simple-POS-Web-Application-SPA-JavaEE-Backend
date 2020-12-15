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
@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/place-orders")
public class PlaceOrderServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setContentType("text/html");
        try (PrintWriter out = resp.getWriter();) {
            out.println("<div>");

            out.println("<h1>Place Order servlet !</h1>");
//            out.println("<h2>"+ getServletContext().getAttribute("cp") +"</h2>");


            try {
                Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
                Connection connection = cp.getConnection();
//                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ijse", "root", "root");
                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT *" +
                        "FROM `order` AS o, `order_detail` AS od" +
                        "WHERE o.id = od.order_id;");

                /* table */
                out.println("<table style='border-collapse: collapse; border: 1px solid black;'>");

                /* table header */
                out.println("<thead>");
                out.println("<tr>");
                out.println("<th>ID</th>");
                out.println("<th>Name</th>");
                out.println("<th>Quantity</th>");
                out.println("<th>Unit Price</th>");
                out.println("<th>Description</th>");
                out.println("</tr>");
                out.println("</thead>");
                /* ./table header */



                /* table body */
                out.println("<tbody>");
                while (rst.next()) {
                    String id = rst.getString(1);
                    String name = rst.getString(2);
                    String quantity = rst.getString(3);
                    String unitPrice = rst.getString(4);
                    String description = rst.getString(5);

                    /* Generate the table data row */
                    out.println("<tr>" +
                            "<td>" + id + "</td>" +
                            "<td>" + name + "</td>" +
                            "<td>" + quantity + "</td>" +
                            "<td>" + unitPrice + "</td>" +
                            "<td>" + description + "</td>" +
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
