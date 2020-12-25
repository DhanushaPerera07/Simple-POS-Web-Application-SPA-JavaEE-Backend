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
 * @author : Dhanusha Perera
 * @since : 11/12/2020
 **/
@WebServlet(name = "CustomerServlet", urlPatterns = "/customers")
public class CustomerServlet extends HttpServlet {
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }//doOptions

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String customerID = req.getParameter("id");

        /* CORS policy */
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);

        /* Let's get the connection pool using the created key value pair */
        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");
        resp.setContentType("application/json");

        try {
            PrintWriter out = resp.getWriter();
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);

            try (Connection connection = cp.getConnection()) {
                PreparedStatement pstm = connection.prepareStatement("SELECT * FROM customer" + ((customerID != null) ? " WHERE id=?" : ""));
                if (customerID != null) {
                    pstm.setObject(1, customerID);
                }
                ResultSet rst = pstm.executeQuery();

                List<Customer> customerList = new ArrayList<Customer>();


                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    String address = rst.getString(3);
                    String email = rst.getString(4);
                    String contact = rst.getString(5);

                    customerList.add(new Customer(Integer.toString(id), name, address, email, contact));
                }

                if (customerID != null && customerList.isEmpty()) {
//                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    // Create Jsonb and serialize
                    Jsonb jsonb = JsonbBuilder.create();
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
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);

        resp.setContentType("application/json");
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);

            try (Connection connection = bds.getConnection()) {
                Jsonb jsonb = JsonbBuilder.create();
                Customer customer = jsonb.fromJson(req.getReader(), Customer.class);

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
        String id = req.getParameter("id");

        BasicDataSource cp = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
            Connection connection = cp.getConnection();
            PreparedStatement pstm = connection.prepareStatement("DELETE FROM customer WHERE `id` = ?");

            pstm.setObject(1, id);
            boolean success = pstm.executeUpdate() > 0;
            if (success) {
                /* deleted successfully ---> 204 status code */
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            connection.close();
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
        }

    }//doDelete

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        /* CORS policy */
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);

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
            if (!(preparedStatement.executeQuery().next())){
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
