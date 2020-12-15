/**
 * MIT License
 * <p>
 * Copyright (c) 2020 Dhanusha Perera
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author : Dhanusha Perera
 * @since : 12/12/2020
 **/
/**
 * @author : Dhanusha Perera
 * @since : 12/12/2020
 **/
package lk.ijse.dep.web.listener;

import lk.ijse.dep.web.commonConstants.CommonConstants;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.SQLException;

/*
 * Annotation should be set here,
 * We have to introduced this class to tomcat.
 * OLD way: web.xml through we have to introduced (deployment descriptor),
 * NEW way: we can do it by Annotation ---> @WebListener
 * */

@WebListener
public class MyContextListener implements ServletContextListener {

    /* When Context is initialized */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
//        System.out.println("Context is being initialized");
        BasicDataSource bds = new BasicDataSource();
        bds.setUsername(CommonConstants.MYSQL_USER_NAME);
        bds.setPassword(CommonConstants.MYSQL_PASSWORD);
        bds.setUrl(CommonConstants.MYSQL_URL);
        bds.setInitialSize(5);
        bds.setMaxTotal(5);
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute("cp",bds);

    }

    /* When Context is destroyed */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
//        System.out.println("Context is being destroyed");
        BasicDataSource cp = (BasicDataSource) sce.getServletContext().getAttribute("cp");

        try {
            cp.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }
}
