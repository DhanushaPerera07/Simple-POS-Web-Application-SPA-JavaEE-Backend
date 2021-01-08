package lk.ijse.dep.web.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep.web.commonConstants.CommonConstants;
import lk.ijse.dep.web.model.Customer;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Dhanusha Pererḁ
 * @since : 11/12/2020
 **/
@WebServlet(name = "CustomerServlet", urlPatterns = "/customers")
public class CustomerServlet extends HttpServlet {

    /* Since, we set the required headers in the CorsFilter,
    * We do not have to config the doOption method */
/*    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE");
    }//doOptions*/

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* Let's the id from the request header */
        String customerID = req.getParameter("id");

        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");

        try {
            PrintWriter out = resp.getWriter();
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);

            try (Connection connection = cp.getConnection()) {
                /* if customer id is passed in the GET request that means,
                 * record for that particular ID should be retrieved from the database, otherwise;
                 * all the customers in the database are retrieved */
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM customer" +
                        ((customerID != null) ? " WHERE id=?" : ""));
                if (customerID != null) {
                    pstm.setObject(1, customerID);
                }
                ResultSet rst = pstm.executeQuery();

                /* Let's take the result set to a customer array */
                List<Customer> customerList = new ArrayList<Customer>();

                /* Let's go through the result set */
                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    String address = rst.getString(3);
                    String email = rst.getString(4);
                    String contact = rst.getString(5);

                    customerList.add(new Customer(Integer.toString(id), name, address, email, contact));
                }

                /* If customerID is not null, that means there is a customer ID, somehow it is a valid one.
                 * But, customerList is empty; that means for that given ID no result found / no matching records found.
                 * So, it is good to let the client know that there is no result for that request.
                 * To do that, we can send "404 - Not Found" error */
                if (customerID != null && customerList.isEmpty()) {
//                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    /* Create Jsonb and serialize */
                    Jsonb jsonb = JsonbBuilder.create();
                    /* Let's make the customerList to a JSON format
                     * and, send the JSON to the client */
                    out.println(jsonb.toJson(customerList));
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
/*        BufferedReader br = req.getReader();
        String line = null;
        String json = "";

        while ((line = br.readLine()) != null){
            json += line;
        }

        System.out.println(json);
        Jsonb jsonb = JsonbBuilder.create();
        Customer customer = jsonb.fromJson(json, Customer.class);*/

        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        resp.setContentType("application/json");
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);

                Customer customer;
            try (Connection connection = bds.getConnection()) {
                if (req.getContentType().equals("application/json")) {
                    /* application/x-www-form-urlencoded */
                    Jsonb jsonb = JsonbBuilder.create();
                    customer = jsonb.fromJson(req.getReader(), Customer.class);
                } else if (req.getContentType().equals("application/x-www-form-urlencoded")){
                    /* application/x-www-form-urlencoded */
                    customer = new Customer("", // ID is auto generated
                            req.getParameter("name"),
                            req.getParameter("address"),
                            req.getParameter("email"),
                            req.getParameter("contact"));

                } else {
                    /* other ContentTypes are not acceptable */
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                /* Validation part - check null */
                if (customer.getName() == null || customer.getAddress() == null || customer.getContact() == null || customer.getEmail() == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                /* Validation part */
                if (!customer.getEmail().matches("^(.+)@(.+)$") ||
                        customer.getName().trim().isEmpty() ||
                        customer.getAddress().trim().isEmpty() ||
                        !customer.getContact().trim().matches("\\d{10}")
                ) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                PreparedStatement pstm = connection.prepareStatement("INSERT INTO `customer` (`name`,`address`,`email`,`contact`) VALUES (?,?,?,?);");
//            pstm.setObject(1, customer.getId());
                pstm.setObject(1, customer.getName());
                pstm.setObject(2, customer.getAddress());
                pstm.setObject(3, customer.getEmail());
                pstm.setObject(4, customer.getContact());

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

    }//doPost

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        String id = req.getParameter("id");

        /* Validation */
        if (id == null || id.trim().isEmpty() || !id.trim().matches("\\d+")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
            try (Connection connection = cp.getConnection()) {

                /* Check if there is a record for the given ID */
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM customer WHERE id=?");
                preparedStatement.setObject(1, id);

                /* if customer does not exists for that ID, send NOT_FOUND error */
                if (!(preparedStatement.executeQuery().next())) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                /* Database operation - delete customer record */
                PreparedStatement pstm = connection.prepareStatement("DELETE FROM customer WHERE `id` = ?");

                pstm.setObject(1, id);
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

    }//doDelete

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* CORS policy */
        /*resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);*/

        String id = req.getParameter("id");

        /* Validation */
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
            Customer customer = jsonb.fromJson(req.getReader(), Customer.class);

            /* Validation part - check null */
            if (customer.getId() != null || customer.getName() == null || customer.getAddress() == null || customer.getContact() == null || customer.getEmail() == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Validation part */
            if (!customer.getEmail().matches("^(.+)@(.+)$") ||
                    customer.getName().trim().isEmpty() ||
                    customer.getAddress().trim().isEmpty() ||
                    !customer.getContact().trim().matches("\\d{10}")
            ) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            /* Check if there is a record for the given ID */
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM customer WHERE id=?");
            preparedStatement.setObject(1, id);

            /* if customer does not exists for that ID */
            if (!(preparedStatement.executeQuery().next())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            /* Database operation - update customer record */
            PreparedStatement pstm = connection.prepareStatement("UPDATE customer SET name=?,address=?, email=?,contact=? WHERE id=?");
            pstm.setObject(1, customer.getName());
            pstm.setObject(2, customer.getAddress());
            pstm.setObject(3, customer.getEmail());
            pstm.setObject(4, customer.getContact());
            pstm.setObject(5, id);

            /* result of the update operation */
            boolean success = pstm.executeUpdate() > 0;

            if (success) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
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
