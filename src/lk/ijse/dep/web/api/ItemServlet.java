package lk.ijse.dep.web.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
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
import java.sql.*;
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
    }// doOptions

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
                    /* Let's make the itemList to a JSON format
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
    }// doGet

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* CORS policy */
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);

        resp.setContentType("application/json");
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);

            try (Connection connection = bds.getConnection()) {
                Jsonb jsonb = JsonbBuilder.create();
                Item item = jsonb.fromJson(req.getReader(), Item.class);

                /* Validation part - check for null and negative numbers */
                if (item.getName() == null ||
                        item.getQuantity() < 0 ||
                        item.getUnitPrice() == null ||
                        item.getDescription() == null
                ) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                /* Validation part */
                if (!item.getUnitPrice().toString().matches("^\\d+|(\\d)+(.)\\d{2}|\\d$") ||
                        item.getName().trim().isEmpty() ||
                        item.getDescription().trim().isEmpty()
                ) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                PreparedStatement pstm = connection.prepareStatement("INSERT INTO `item` (`name`,`qty`,`unit_price`,`description`) VALUES (?,?,?,?);");
//            pstm.setObject(1, item.getId());
                pstm.setObject(1, item.getName());
                pstm.setObject(2, item.getQuantity());
                pstm.setObject(3, item.getUnitPrice());
                pstm.setObject(4, item.getDescription());

                /* Check inserted successfully or not */
                if (pstm.executeUpdate() > 0) {
                    /* insertion successful */
                    resp.setStatus(HttpServletResponse.SC_CREATED);
//                    resp.getWriter().println(jsonb.toJson(true));
                } else {
                    /* insertion unsuccessful */
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                    resp.getWriter().println(jsonb.toJson(false));
                }

            } catch (SQLIntegrityConstraintViolationException throwables) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                throwables.printStackTrace();
            } catch (SQLException throwables) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throwables.printStackTrace();
            } catch (JsonbException throwables) {
                throwables.printStackTrace();
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }// doPost

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        /* CORS policy */
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);

        String id = req.getParameter("id");

        /* ID Validation */
        if (id == null || id.trim().isEmpty() || !id.trim().matches("\\d+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        /* Connection Pool */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
            try (Connection connection = cp.getConnection()) {

                /* Check if there is a record for the given ID */
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `item` WHERE id=?");
                preparedStatement.setObject(1, id);

                /* if item does not exists for that ID, send NOT_FOUND error */
                if (!(preparedStatement.executeQuery().next())) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                /* Database operation - delete item record */
                PreparedStatement pstm = connection.prepareStatement("DELETE FROM `item` WHERE `id` = ?");
                pstm.setObject(1, id);

                /* get the result */
                boolean success = pstm.executeUpdate() > 0;
                if (success) {
                    /* deleted successfully ---> 204 status code */
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                } else {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }

            } catch (SQLIntegrityConstraintViolationException throwables) {
                throwables.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (ClassNotFoundException throwables) {
            throwables.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }// doDelete

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* CORS policy */
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);

        /* Get the id from the request header */
        String id = req.getParameter("id");

        /* ID Validation */
        if (id == null || id.trim().isEmpty() || !id.trim().matches("\\d+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        /* Database connection config - db connection pool */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try {

            /* Database connection */
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
            Connection connection = cp.getConnection();

            /* JSONb */
            Jsonb jsonb = JsonbBuilder.create();
            Item item = jsonb.fromJson(req.getReader(), Item.class);

            /* Validation part - check for null and negative numbers */
            if (item.getName() == null ||
                    item.getQuantity() < 0 ||
                    item.getUnitPrice() == null ||
                    item.getDescription() == null
            ) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part */
            if (!item.getUnitPrice().toString().matches("^\\d+|(\\d)+(.)\\d{2}|\\d$") ||
                    item.getName().trim().isEmpty() ||
                    item.getDescription().trim().isEmpty()
            ) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Check if there is a record for the given ID */
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `item` WHERE `id`=?");
            preparedStatement.setObject(1, id);

            /* if item does not exists for that ID */
            if (!(preparedStatement.executeQuery().next())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            /* Database operation - update item record */
            PreparedStatement pstm = connection.prepareStatement("UPDATE `item` SET `name`=?,`qty`=?, `unit_price`=?,`description`=? WHERE id=?");
            pstm.setObject(1, item.getName());
            pstm.setObject(2, item.getQuantity());
            pstm.setObject(3, item.getUnitPrice());
            pstm.setObject(4, item.getDescription());
            pstm.setObject(5, id); // the ID got from the request header

            /* result of the update operation */
            boolean success = pstm.executeUpdate() > 0;

            if (success) {
                /* Send success status code to the client */
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                /* That means something went wrong,
                send error status code to the client */
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (ClassNotFoundException throwables) {
            throwables.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (JsonbException throwables) {
            throwables.printStackTrace();
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }


    } //doPut
}
