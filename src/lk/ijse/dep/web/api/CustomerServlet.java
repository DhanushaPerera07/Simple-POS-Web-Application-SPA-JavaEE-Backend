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
 * @since : 11/12/2020
 **/
@WebServlet(name = "CustomerServlet", urlPatterns = "/customers")
public class CustomerServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setContentType("application/json");
        try (PrintWriter out = resp.getWriter()) {

            try {
                Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
                Connection connection = cp.getConnection();

                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM customer");

                String json = ("[");

                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    String address = rst.getString(3);
                    String email = rst.getString(4);
                    String contact = rst.getString(5);

                    /* Generate the table data row */
                    json += ("{" +
                            "\"id\":\"" + id + "\"," +
                            "\"name\":\"" + name + "\"," +
                            "\"address\":\"" + address + "\"," +
                            "\"email\":\"" + email + "\"," +
                            "\"contact\":\"" + contact + "\"" +
                            "},");
                }

                json = json.substring(0,json.length()-1); // remove the last comma
                json += ("]");
                out.println(json);
                connection.close();

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


        }


    }
}
