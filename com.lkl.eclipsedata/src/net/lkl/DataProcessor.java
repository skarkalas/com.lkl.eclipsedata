package net.lkl;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.codehaus.jettison.json.JSONArray;

@Path("/service")
public class DataProcessor
{
	private String _corsHeaders="Content-Type,X-Requested-With,Host,User-Agent,Accept,Accept-Language,Accept-Encoding,Accept-Charset,Keep-Alive,Connection,Referer,Origin";

	private Response makeCORS(ResponseBuilder req, String returnMethod)
	{
		ResponseBuilder rb = req.header("Access-Control-Allow-Origin", "*")
	      .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

		if (returnMethod.equals("")==false)
		{
			rb.header("Access-Control-Allow-Headers", returnMethod);
		}

		return rb.build();
	}

	private Response makeCORS(ResponseBuilder req)
	{
		return makeCORS(req, _corsHeaders);
	}
	
	private DataSource getDataSource(String dataSourceLocation) throws NamingException
	{
		// Get a context for the JNDI look up
		Context ctx = new InitialContext();
		Context envContext=(Context)ctx.lookup("java:/comp/env");
	      
		// Look up a data source
		DataSource ds=(DataSource)envContext.lookup(dataSourceLocation); 
	    
		return ds;
	}
	
	private Connection getConnection()
	{
		Connection connection=null;
		
		try
		{
			DataSource ds=getDataSource("jdbc/ORAPool");
			connection=ds.getConnection();
		}
		catch(NamingException e)
		{
			System.err.println("Naming Exception");
			e.printStackTrace();			
		}
		catch(SQLException e)
		{
			System.err.println("SQL Exception");
			e.printStackTrace();			
		}
		catch(Exception e)
		{
			System.err.println("General Exception");
			e.printStackTrace();			
		}

		return connection;
	}
	
	private String[] analyseInput(String input) throws Exception
	{
		String[] output=new String[3];
		int i=0,start=0;
		
		for(;i<output.length-1;i++)
		{
			int position=input.indexOf("@",start);
			
			if(position==-1)
			{
				throw new Exception("DataProcessor: invalid input format");
			}
		
			output[i]=new String(input.substring(start, position));
			start=position+1;
		}
		
		output[i]=new String(input.substring(start));
		
		return output;
	}
	
	@PUT
	@Consumes(MediaType.TEXT_PLAIN)
	public Response UpdateStudentActivity(String studentDetails)
	{
		String[] details=null;
		
		try
		{
			details=analyseInput(studentDetails);
		}
		catch(Exception e)
		{
			e.printStackTrace();			
		}
		
		String username=details[0];
		String machine=details[1];
		int active=0;
		
		if(details[2].equals("active"))
		{
			active=1;
		}
		
		Connection connection=getConnection();
		PreparedStatement statement=null;
		String message="status for student "+username+" updated ";
		
		try
		{
			String query="insert into useractivity values (?,?,systimestamp,?)";
			statement=connection.prepareStatement(query);
			statement.setString(1,username);
			statement.setString(2,machine);
			statement.setInt(3,active);
			
			int rows=statement.executeUpdate();
			
			if(rows==1)
			{
				message+="successfully";
			}
		}
		catch(SQLException e)
		{
			System.err.println("UpdateStudentActivity:SQL Exception");
			message+="unsuccessfully (SQLException)";						
		}
		catch(Exception e)
		{
			System.err.println("UpdateStudentActivity:General Exception");
			message+="unsuccessfully (Exception)";			
		}

		System.out.println("** updating: "+username);
		Response response=Response.ok(message,MediaType.TEXT_PLAIN).build();
		return response;
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response inactiveUsers()
	{
		String output="";
		Connection connection=getConnection();
		PreparedStatement statement=null;
		
		try
		{
			connection=getConnection();	
			String query="select * from inactiveusers";
			statement=connection.prepareStatement(query);
			
			ResultSet rs=statement.executeQuery();
			JSONArray json = ResultSetConverter.convert(rs);
			output = json.toString();
		}
		catch(SQLException e)
		{
			System.err.println("inactiveUsers:SQL Exception");
			e.printStackTrace();							
		}
		catch(Exception e)
		{
			System.err.println("inactiveUsers:General Exception");
			e.printStackTrace();				
		}

		ResponseBuilder response=Response.ok(output,MediaType.APPLICATION_JSON);
		return makeCORS(response);
	}
}
