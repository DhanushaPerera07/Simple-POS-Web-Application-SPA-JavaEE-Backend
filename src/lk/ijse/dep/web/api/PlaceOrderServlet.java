package lk.ijse.dep.web.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep.web.commonConstants.CommonConstants;
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
import java.sql.*;

/**
 * @author : Dhanusha Perera
 * @since : 15/12/2020
 **/
@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/place-orders")
public class PlaceOrderServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    }// doOptions

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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // place / save an order

        /* CORS policy */
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setContentType("application/json");
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);

            try (Connection connection = bds.getConnection()) {
                Jsonb jsonb = JsonbBuilder.create();
                OrderDetail orderDetail = jsonb.fromJson(req.getReader(), OrderDetail.class);

                /* Validation part - check customer obj is null or not */
                if (orderDetail.getCustomer() == null ||
                        orderDetail.getItemList() == null){
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                } else {
                    /* check the customer object is valid or not */

                    if (orderDetail.getCustomer().getId() == null){
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } else {
                        try{
                            Integer.parseInt(orderDetail.getCustomer().getId());
                        } catch (NumberFormatException ex){
                            ex.printStackTrace();
                            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                            return;
                        } catch (Exception ex){
                            ex.printStackTrace();
                            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            return;
                        }
                    }
                }

                /* Item related validations */
                for (Item item : orderDetail.getItemList()) {
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

                }// item related validations

                /* Set auto commit mode false */
                connection.setAutoCommit(false);

                PreparedStatement pstm = connection.prepareStatement(
                        "INSERT INTO `order` (`customer_id`) VALUES (?);",
                        Statement.RETURN_GENERATED_KEYS);
                pstm.setObject(1, Integer.parseInt(orderDetail.getCustomer().getId()));


                /* Check inserted successfully or not */
                if (pstm.executeUpdate() > 0) {
                        /* insertion successful */
//                        resp.setStatus(HttpServletResponse.SC_CREATED);
//                    resp.getWriter().println(jsonb.toJson(true));

                    try (ResultSet rs = pstm.getGeneratedKeys()){
                        if (rs.next()){
                            /* get the order id and set orderId*/
                            orderDetail.setOrderId(Integer.toString(rs.getInt(1)));

                            try {
                                /* insert all the items (in the cart) to order_detail table */
                                for (Item item : orderDetail.getItemList()) {
                                    PreparedStatement preparedStatement = connection.prepareStatement(
                                            "INSERT INTO `order_detail` " +
                                                    "(`order_id`,`item_id`,`qty`,`unit_price`) " +
                                                    "VALUES (?,?,?,?);");
                                    /* Insert record to the Order Detail Table in the database */
                                    preparedStatement.setObject(1,orderDetail.getOrderId());
                                    preparedStatement.setObject(2, item.getId());
                                    preparedStatement.setObject(3, item.getQuantity());
                                    preparedStatement.setObject(4, item.getUnitPrice());

                                    if (preparedStatement.executeUpdate() > 0){
                                        /* Committing point */
                                        connection.commit();
                                    } else {
                                        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                        connection.rollback();
                                        System.out.println("failed to insert data into order_detailed table");
                                    }
                                }


                            } catch (SQLIntegrityConstraintViolationException ex){
                                ex.printStackTrace();
                                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                                connection.rollback();
                            } catch (SQLException ex){
                                ex.printStackTrace();
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                connection.rollback();
                            } catch (Exception ex){
                                ex.printStackTrace();
                                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                connection.rollback();
                            }

                        } else {
                            /* Something went wrong */
                            connection.rollback();
                        }
                    }
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
    }
}
