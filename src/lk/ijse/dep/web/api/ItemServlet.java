package lk.ijse.dep.web.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lk.ijse.dep.web.commonConstants.CommonConstants;
import lk.ijse.dep.web.model.Item;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
        resp.setContentType("application/json");
        try (PrintWriter out = resp.getWriter();) {

            try {
                Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
                Connection connection = cp.getConnection();

                Statement stm = connection.createStatement();
                ResultSet rst = stm.executeQuery("SELECT * FROM item");

                /* List */
                List<Item> itemList = new ArrayList<>();

                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    int quantity = rst.getInt(3);
                    BigDecimal unitPrice = rst.getBigDecimal(4); // big decimal
                    String description = rst.getString(5);

                    itemList.add(new Item(Integer.toString(id),name,description,quantity,unitPrice));
                }

                // Create Jsonb and serialize
                Jsonb jsonb = JsonbBuilder.create();

                out.println(jsonb.toJson(itemList));

                connection.close();

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


        }
    }
}
