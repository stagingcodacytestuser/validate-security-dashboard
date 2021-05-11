import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Add;

class DBConnectionClass {
  def readDataBase() = {
   try {
        Class.forName("com.mysql.jdbc.Driver")
        val connection: Connection = DriverManager.getConnection("jdbc:mysql://172.16.40.5/scalatest", "user", "pass");

   } 
   catch {
      case _: Throwable => println("Could not connect to database")
  } 

}
