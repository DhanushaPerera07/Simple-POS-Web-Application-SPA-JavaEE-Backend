package lk.ijse.dep.web.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
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

                List<Customer> customerList = new ArrayList<Customer>();


                while (rst.next()) {
                    int id = rst.getInt(1);
                    String name = rst.getString(2);
                    String address = rst.getString(3);
                    String email = rst.getString(4);
                    String contact = rst.getString(5);

                    customerList.add(new Customer(Integer.toString(id), name, address, email, contact));
                }

                // Create Jsonb and serialize
                Jsonb jsonb = JsonbBuilder.create();

                out.println(jsonb.toJson(customerList));
                connection.close();

            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


        }


    }//doGet

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

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

        Jsonb jsonb = JsonbBuilder.create();
        Customer customer = jsonb.fromJson(req.getReader(), Customer.class);

        resp.setHeader("Access-Control-Allow-Origin", CommonConstants.FRONTEND_URL);
        resp.setContentType("application/json");

        // TODO: save customer in the database
        BasicDataSource bds = (BasicDataSource) getServletContext().getAttribute("cp");

        try {
            Class.forName(CommonConstants.MYSQL_DRIVER_CLASS_NAME);
            Connection connection = bds.getConnection();
            PreparedStatement pstm = connection.prepareStatement("INSERT INTO `customer` (`name`,`address`,`email`,`contact`) VALUES (?,?,?,?);");
//            pstm.setObject(1, customer.getId());
            pstm.setObject(1, customer.getName());
            pstm.setObject(2, customer.getAddress());
            pstm.setObject(3, customer.getEmail());
            pstm.setObject(4, customer.getContact());

            boolean success = pstm.executeUpdate() > 0;

            if (success) {
                resp.getWriter().println(jsonb.toJson(true));
            } else {
                resp.getWriter().println(jsonb.toJson(false));
            }

            connection.close();
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
            resp.getWriter().println(jsonb.toJson(false));
        }

    }//doPost
}
