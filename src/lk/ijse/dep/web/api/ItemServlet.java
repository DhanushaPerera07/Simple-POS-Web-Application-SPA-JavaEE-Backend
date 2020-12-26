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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Dhanusha Perera
 * @since : 15/12/2020
 **/
@WebServlet(name = "ItemServlet", urlPatterns = "/items")
public class ItemServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* Let's the id from the request header */
        String itemID = req.getParameter("id");

        /* CORS policy */
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");

        try {
            PrintWriter out = resp.getWriter();
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
            try (Connection connection = cp.getConnection()) {
                /* if item id is passed in the GET request that means,
                 * record for that particular ID should be retrieved from the database, otherwise;
                 * all the items in the database are retrieved */
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM item" +
                        ((itemID != null) ? " WHERE id=?" : ""));
                if (itemID != null) {
                    pstm.setObject(1, itemID);
                }
                ResultSet rst = pstm.executeQuery();

                /* Let's take the result set to an item array */
                List<Item> itemList = new ArrayList<>();

                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    int quantity = rst.getInt(3);
                    BigDecimal unitPrice = rst.getBigDecimal(4); // big decimal
                    String description = rst.getString(5);

                    itemList.add(new Item(Integer.toString(id), name, description, quantity, unitPrice));
                }

                /* If itemID is not null, that means there is a item ID, somehow it is a valid one.
                 * But, itemList is empty; that means for that given ID no result found / no matching records found.
                 * So, it is good to let the client know that there is no result for that request.
                 * To do that, we can send "404 - Not Found" error */
                if (itemID != null && itemList.isEmpty()) {
//                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    /* Create Jsonb and serialize */
                    Jsonb jsonb = JsonbBuilder.create();
                    /* Let's make the customerList to a JSON format
                     * and, send the JSON to the client */
                    out.println(jsonb.toJson(itemList));
                }

            } catch (SQLException e) {
                e.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
