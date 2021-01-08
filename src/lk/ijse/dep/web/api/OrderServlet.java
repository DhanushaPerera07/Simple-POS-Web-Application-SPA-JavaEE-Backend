package lk.ijse.dep.web.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lk.ijse.dep.web.commonConstants.CommonConstants;
import lk.ijse.dep.web.model.Customer;
import lk.ijse.dep.web.model.Item;
import lk.ijse.dep.web.model.Order;
import lk.ijse.dep.web.model.OrderDetail;
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
import java.util.Date;
import java.util.List;

/**
 * @author : Dhanusha Perera
 * @since : 05/01/2021
 **/

@WebServlet(name = "OrderServlet", urlPatterns = "/orders")
public class OrderServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");

        try {
            PrintWriter out = resp.getWriter();
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);

            /* Create OrderDetails arrayList */
            List<OrderDetail> orderDetailArrayList = new ArrayList<>();

            try (Connection connection = cp.getConnection()) {

                /* Getting ORDER info */
                PreparedStatement pstm = connection.prepareStatement("SELECT `order`.id,`order`.ordered_date,customer.*\n" +
                        "FROM `order`\n" +
                        "INNER JOIN customer ON `order`.customer_id = customer.id;");

                ResultSet rst = pstm.executeQuery();

                /* Let's go through the result set */
                while (rst.next()) {

                    OrderDetail orderDetail = new OrderDetail();

                    int orderId = rst.getInt(1); // order ID
                    orderDetail.setOrderId(Integer.toString(orderId));
                    orderDetail.setOrderedDate(rst.getDate(2));
                    orderDetail.setCustomer(new Customer(
                            Integer.toString(rst.getInt(3)),
                            rst.getString(4),
                            rst.getString(5),
                            rst.getString(6),
                            rst.getString(7)
                    ));

                    /* Getting ORDER DETAILS info */
                    PreparedStatement preparedStatement = connection.prepareStatement("SELECT `order_detail`.*,`item`.name,`customer`.name\n" +
                            "FROM `order_detail`\n" +
                            "INNER JOIN `order` ON order_detail.order_id = `order`.id\n" +
                            "INNER JOIN `item` ON order_detail.item_id = `item`.id\n" +
                            "INNER JOIN `customer` ON order.customer_id = customer.id\n" +
                            "where `order`.id = "+ orderId +";"); // order ID

                    ResultSet resultSet = preparedStatement.executeQuery();

                    ArrayList<Item> items = new ArrayList<>();

                    while (resultSet.next()){
                        int orderDetailId = resultSet.getInt(1);
//                        int orderIdFk = resultSet.getInt(2);
//                        int itemId = resultSet.getInt(3);
//                        int orderingQuantity = resultSet.getInt(4);
//                        BigDecimal orderingUnitPrice = resultSet.getBigDecimal(5);
//                        String orderingItemName = resultSet.getString(6);

                        orderDetail.setOrderDetailId(Integer.toString(orderDetailId));

                        /* Item */
                        Item item = new Item();
                        item.setId(Integer.toString(resultSet.getInt(3)));
                        item.setName(resultSet.getString(6));
                        item.setQuantity(resultSet.getInt(4));
                        item.setUnitPrice(resultSet.getBigDecimal(5));

                        items.add(item);

                    }

                    /* set items arrayList to orderDetail */
                    orderDetail.setItemList(items);

                    orderDetailArrayList.add(orderDetail);
                }

                if (orderDetailArrayList == null) {
//                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    /* Create Jsonb and serialize */
                    Jsonb jsonb = JsonbBuilder.create();
                    /* Let's make the orderDetailArrayList to a JSON format
                     * and, send the JSON to the client */
                    out.println(jsonb.toJson(orderDetailArrayList));
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }


    }//doGet

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

}
