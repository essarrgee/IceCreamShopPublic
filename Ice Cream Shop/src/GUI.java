import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.*;
import java.awt.EventQueue;
import java.awt.event.*;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.*;
import java.awt.Color;
import java.math.BigDecimal;

public class GUI {
	static String DBLocation = "jdbc:sqlite:./IceCreamShop.db";
	//static String DBLocation = "jdbc:sqlite:/.../IceCreamShop.db"; // IceCreamShop.db path goes here
	static Connection connection = null;
	static int custkey = 0;
	static String custname = "";
	static String custfavflavorname = "none";
	static int custfavflavorkey = 0;
	static String storename = "";
	static int orderkey = 0;
	static boolean confirmed = false;
	static private JFrame framewelcome;
	static private JFrame frame;
	static private JFrame framestore;
	static private JList listflavor;
	static private JList listtopping;
	static private JList listcustomer;
	static private JList listorder;
	static private JScrollPane paneflavor;
	static private JScrollPane panetopping;
	static private JScrollPane panecustomer;
	static private JScrollPane paneorder;
	static private JButton buttonorder;
	static private JButton buttonconfirm;
	static private JLabel labeltotalprice;
	//Stores, Flavors, Toppings, and Cones are from the database
	static ArrayList<String> Stores;
	static ArrayList<String> FavFlavors;
	static ArrayList<String> Flavors; // = new ArrayList<String>();
	static ArrayList<String> FlavorsText;
	static ArrayList<String> Toppings;
	static ArrayList<String> ToppingsText;
	static ArrayList<String> Cones;
	static ArrayList<String> ConesText;
	//CurrentFlavors, CurrentToppings is a temporary list for cust's order
	static DefaultListModel CurrentFlavors; // = new DefaultListModel();
	static DefaultListModel CurrentToppings;
	static ArrayList<String> CurrentCustomers;
	static ArrayList<Integer> OrdersKeys;
	static ArrayList<String> CurrentOrders;
	static String CurrentCone;
	//total price
	static double totalprice;
	
	public static boolean AddCustomer() {
		try {
        	connection = DriverManager.getConnection(DBLocation);
            Statement stat = connection.createStatement();
            //get max of customers
            ResultSet result = stat.executeQuery("select max(c_custkey) as cno "
                + "from customer ");
            if (result.next()) {
            	int resultint = result.getInt("cno");
            	custkey = resultint + 1;
            }
            //check custkey doesn't exist yet
            result = stat.executeQuery("select c_custkey "
                + "from customer "
                + "where c_custkey = '" + custkey + "'");
            //check favoriteflavorkey
            ResultSet result2 = stat.executeQuery("select f_flavorkey "
                + "from flavor "
                + "where f_name = '" + custfavflavorname + "'");
            //assign fav flavor
            if (result2.next())
            	custfavflavorkey = result2.getInt("f_flavorkey");
            else {
            	custfavflavorname = "none";
            	custfavflavorkey = 0;
            }
            if (!result.next() && !custname.isEmpty()) {
            	//if not exists, insert into customer table
            	stat.executeUpdate("insert into customer "
            		+ "values('" + custkey + "', '" + custname + "', '" + custfavflavorkey + "')");
            	return true;
            }
        }
        catch(SQLException e) {
        	System.err.println(e.getMessage());
        }
		return false;
	}
	
	public static double GetTotalPrice() {
		try {
        	connection = DriverManager.getConnection(DBLocation);
            Statement stat = connection.createStatement();
            double flavorsprice = 0.0;
            double toppingsprice = 0.0;
            double coneprice = 0.0;
            //get total flavor price
            for (int i=0; i<CurrentFlavors.size(); i++) {
	            ResultSet result = stat.executeQuery("select f_price "
	                + "from flavor "
	                + "where f_name = '" + CurrentFlavors.get(i) +"' ");
	            if (result.next()) {
	            	double resultd = result.getDouble("f_price");
	            	flavorsprice += resultd;
	            }
            }
            //get total topping price
            for (int i=0; i<CurrentToppings.size(); i++) {
	            ResultSet result = stat.executeQuery("select t_price "
	                + "from topping "
	                + "where t_name = '" + CurrentToppings.get(i) +"' ");
	            if (result.next()) {
	            	double resultd = result.getDouble("t_price");
	            	toppingsprice += resultd;
	            }
            }
            //get cup price
            ResultSet result = stat.executeQuery("select co_price "
                + "from cone "
                + "where co_name = '" + CurrentCone + "' ");
            if (result.next()) {
            	double resultd = result.getDouble("co_price");
            	coneprice += resultd;
            }
            return (flavorsprice + toppingsprice + coneprice);
        }
        catch(SQLException e) {
        	System.err.println(e.getMessage());
        }
		finally {
		  try {
		    if(connection != null) {
		      connection.close();
		    }
		  }
		  catch(SQLException e) {
		    System.err.println(e.getMessage());
		  }
        }
		return 0.0;
	}
	
	public static void UpdateTotalPriceLabel() {
		if (labeltotalprice != null) {
			totalprice = GetTotalPrice();
			labeltotalprice.setText("Total Price: $" + BigDecimal.valueOf((double) Math.round(totalprice*100)/100));
		}
	}
	
	public static void MakeOrder() {
		UpdateTotalPriceLabel();
		try {
        	connection = DriverManager.getConnection(DBLocation);
            Statement stat = connection.createStatement();
            int conekey = 1;
            int storekey = 1;
            //create new orderkey
            ResultSet result = stat.executeQuery("select max(o_orderkey) as ono "
                    + "from orders ");
                if (result.next()) {
                	int resultint = result.getInt("ono");
                	orderkey = resultint + 1;
                }
            //get conekey
            result = stat.executeQuery("select co_conekey "
                    + "from cone "
                    + "where co_name = '" + CurrentCone + "' ");
                if (result.next()) {
                	conekey = result.getInt("co_conekey");
                }
            //get storekey
            result = stat.executeQuery("select s_storekey "
                    + "from store "
                    + "where s_name = '" + storename + "' ");
                if (result.next()) {
                	storekey = result.getInt("s_storekey");
                }
            //insert new order into orders
            stat.executeUpdate("insert into orders "
                    + "values('" + orderkey + "', '" + custkey + "', '" + conekey +"',"
                    + "'" + storekey + "')");
            //insert into orderflavor
            for (int i=0; i<CurrentFlavors.size(); i++) {
            	int flavorkey = 0;
            	result = stat.executeQuery("select f_flavorkey "
                        + "from flavor "
                        + "where f_name = '" + CurrentFlavors.get(i) + "' ");
                    if (result.next()) {
                    	flavorkey = result.getInt("f_flavorkey");
                    }
            	stat.executeUpdate("insert into orderflavor "
                        + "values('" + orderkey + "', '" + flavorkey + "')");
            }
            //insert into ordertopping
            for (int i=0; i<CurrentToppings.size(); i++) {
            	int toppingkey = 0;
            	result = stat.executeQuery("select t_toppingkey "
                        + "from topping "
                        + "where t_name = '" + CurrentToppings.get(i) + "' ");
                    if (result.next()) {
                    	toppingkey = result.getInt("t_toppingkey");
                    }
            	stat.executeUpdate("insert into ordertopping "
                        + "values('" + orderkey + "', '" + toppingkey + "')");
            }
            labeltotalprice.setText("Order successful! Your order is #" + orderkey + ".");
        }
        catch(SQLException e) {
        	System.err.println(e.getMessage());
        }
		finally {
			  try {
			    if(connection != null) {
			      connection.close();
			    }
			  }
			  catch(SQLException e) {
			    System.err.println(e.getMessage());
			  }
	        }
	}
	
	public static void RemoveOrder(int index) {
		try {
			if (CurrentOrders != null) {
        	connection = DriverManager.getConnection(DBLocation);
            Statement stat = connection.createStatement();
            //check order exists
            ResultSet result = stat.executeQuery("select o_orderkey "
                    + "from orders "
                    + "where o_orderkey = '" + OrdersKeys.get(index) + "' ");
                if (result.next()) {
                	//delete from db
                	stat.executeUpdate("delete from orders "
                            + "where o_orderkey = '" + OrdersKeys.get(index) + "' ");
                	stat.executeUpdate("delete from orderflavor "
                            + "where of_orderkey = '" + OrdersKeys.get(index) + "' ");
                	stat.executeUpdate("delete from ordertopping "
                            + "where ot_orderkey = '" + OrdersKeys.get(index) + "' ");
                }
			}
		}
        catch(SQLException e) {
        	System.err.println(e.getMessage());
        }
		finally {
			  try {
			    if(connection != null) {
			      connection.close();
			    }
			  }
			  catch(SQLException e) {
			    System.err.println(e.getMessage());
			  }
	        }
	}
	
	public static void ResetOrder() {
		
	}
	
	//limit orders up to 8 for each (glitches out if more)
	public static void AddToCurrentList(String list, String item) {
		if (list == "Flavor") {
			if (CurrentFlavors.size() <= 7) {
				CurrentFlavors.addElement(item);
				//refresh list display
				UpdateList("CurrentFlavor");
			}
		}
		else if (list == "Topping") {
			if (CurrentToppings.size() <= 7) {
				CurrentToppings.addElement(item);
				//refresh list display
				UpdateList("CurrentTopping");
			}
		}
	}
	
	public static void RemoveFromCurrentList(String list, int index) {
		if (list == "Flavor") {
			if (CurrentFlavors.size() > 0 && index < CurrentFlavors.size() && CurrentFlavors.get(index) != null) {
				CurrentFlavors.remove(index);
				//refresh list display
				UpdateList("CurrentFlavor");
			}
		}
		else if (list == "Topping") {
			if (CurrentToppings.size() > 0 && index < CurrentToppings.size() && CurrentToppings.get(index) != null) {
				CurrentToppings.remove(index);
				//refresh list display
				UpdateList("CurrentTopping");
			}
		}
	}
	
	public static void UpdateList(String list) {
        try {
        	connection = DriverManager.getConnection(DBLocation);
            Statement stat = connection.createStatement();
            if (list == "Store") {
            	if (Stores != null) {
		            ResultSet result = stat.executeQuery("select s_storekey, s_name, s_address "
		                    + "from store ");
		            	Stores.clear();
		            int i = 0;
		            while (result.next()) {
		            	String resultstring = result.getString("s_name");
		            	Stores.add(resultstring);
		            	i++;
		            }
            	}
            }
            else if (list == "FavFlavor") {
            	if (FavFlavors != null) {
            		FavFlavors.clear();
		            //check that "none" flavor exists
		            ResultSet result2 = stat.executeQuery("select f_name "
		                    + "from flavor "
		                    + "where f_name = 'none' ");
		            //if doesn't exist, add "none" to list
		            if (!result2.next()) {
		            	FavFlavors.add("none");
		            }
		            //query for each flavor
		            ResultSet result = stat.executeQuery("select f_flavorkey, f_name, f_price "
		                    + "from flavor ");
		            int i = 0;
		            while (result.next()) {
		            	String resultstring = result.getString("f_name");
		            	FavFlavors.add(resultstring);
		            	i++;
		            }
            	}
            }
            else if (list == "Flavor") {
            	if (Flavors != null	&& FlavorsText != null) {
		            ResultSet result = stat.executeQuery("select f_flavorkey, f_name, f_price "
		                    + "from flavor ");
		            	Flavors.clear();
		            	FlavorsText.clear();
		            int i = 0;
		            while (result.next()) {
		            	String resultstring = result.getString("f_name");
		            	String resultstring2 = result.getString("f_price");
		            	Flavors.add(resultstring);
		            	FlavorsText.add(resultstring + " - $" + resultstring2);
		            	i++;
		            }
            	}
            }
            else if (list == "Topping") {
            	if (Toppings != null && ToppingsText != null) {
	            	ResultSet result = stat.executeQuery("select t_toppingkey, t_name, t_price "
	                        + "from topping ");
	            		Toppings.clear();
	                	ToppingsText.clear();
	                int i = 0;
	                while (result.next()) {
	                	String resultstring = result.getString("t_name");
	                	String resultstring2 = result.getString("t_price");
	                	Toppings.add(resultstring);
	                	ToppingsText.add(resultstring + " - $" + resultstring2);
	                	i++;
	                }
            	}
            }
            else if (list == "Cone") {
            	if (Cones != null) {
	            	ResultSet result = stat.executeQuery("select co_conekey, co_name, co_price "
	                        + "from cone ");
	            		Cones.clear();
	                int i = 0;
	                while (result.next()) {
	                	String resultstring = result.getString("co_name");
	                	String resultstring2 = result.getString("co_price");
	                	Cones.add(resultstring);
	                	ConesText.add(resultstring + " - $" + resultstring2);
	                	i++;
	                }
            	}
            }
            else if (list == "CurrentFlavor") {
            	if (CurrentFlavors != null && listflavor != null && paneflavor != null && buttonorder != null) {
	            	int height = 30 + (CurrentFlavors.size()*10);
	            	int scroll = 30 + ((CurrentFlavors.size())*10);
	        		if (scroll >= 120)
	        			scroll = 120;
	        		listflavor.setModel(CurrentFlavors);
	        		listflavor.setSelectedIndex(0);
	            	listflavor.setBounds(30, 210, 250, height);
	            	paneflavor.setBounds(30, 210, 250, scroll);
	            	if (CurrentFlavors.size() <= 0)
	            		buttonorder.setEnabled(false);
	            	else
	            		buttonorder.setEnabled(true);
	            	UpdateTotalPriceLabel();
            	}
            }
            else if (list == "CurrentTopping") {
            	if (CurrentToppings != null && listtopping != null && panetopping != null) {
	            	int height = 30 + (CurrentToppings.size()*10);
	            	int scroll = 30 + (CurrentToppings.size()*10);
	            	if (scroll >= 120)
	        			scroll = 120;
	            	listtopping.setModel(CurrentToppings);
	            	listtopping.setSelectedIndex(0);
	            	listtopping.setBounds(360, 210, 250, height);
	            	panetopping.setBounds(360, 210, 250, scroll);
	            	UpdateTotalPriceLabel();
            	}
            }
            else if (list == "CurrentCustomer") {
            	if (CurrentCustomers != null) {
		            CurrentCustomers.clear();
		            ResultSet result = stat.executeQuery("select c_custkey, c_name, c_favoriteflavorkey, count(o_orderkey) as countorder "
		                    + "from customer, orders, store "
		                    + "where o_custkey = c_custkey and s_storekey = o_storekey and s_name = '"+ storename +"' "
		                    + "group by c_custkey ");
		            int i = 0;
		            while (result.next()) {
		            	int resultint = result.getInt("countorder");
		            	int resultint2 = result.getInt("c_favoriteflavorkey");
		            	String resultstring = result.getString("c_name");
		            	CurrentCustomers.add(resultstring + " - " + resultint2 + " - " + resultint);
		            	i++;
		            }
            	}
            }
            else if (list == "CurrentOrder") {
            	if (CurrentOrders != null && OrdersKeys != null) {
		            CurrentOrders.clear();
		            ResultSet result = stat.executeQuery("select o_orderkey, c_name, s_name "
		                    + "from orders, customer, store "
		                    + "where o_custkey = c_custkey and s_storekey = o_storekey and s_name = '"+ storename +"' ");
		            int i = 0;
		            while (result.next()) {
		            	int resultint = result.getInt("o_orderkey");
		            	String resultstring = result.getString("c_name");
		            	CurrentOrders.add(resultint + " - " + resultstring);
		            	OrdersKeys.add(resultint);
		            	i++;
		            }
            	}
            }
        }
        catch(SQLException e) {
        	System.err.println(e.getMessage());
        }
    }

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					connection = DriverManager.getConnection(DBLocation);
					GUI window = new GUI();
				} catch(SQLException e) {
					System.err.println(e.getMessage());
		        }
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		//initialize();
		open();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	
	//create new customer id
	//select store
		//add flavor (+ icon)
			//set flavor (drop down menu)
				//favorite flavor(if exists) at top of menu
		//add topping (+ icon)
			//set topping (drop down menu)
		//set cone (drop down menu)
		//make order
			//checks to make sure >= 1 flavor exists
			//insert into orders, orderflavor, ordertopping with settings above
	//set favorite flavor (drop down menu)
		//update customer
	
	private void open() {
		Stores = new ArrayList<String>();
		FavFlavors = new ArrayList<String>();
		
		UpdateList("Store");
		UpdateList("FavFlavor");
		
		framewelcome = new JFrame("Welcome");
		framewelcome.setBounds(50, 50, 800, 600);
		framewelcome.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		framewelcome.setSize(800,600);
		framewelcome.setResizable(false);
		
		//enter name label
		JLabel lbcustname = new JLabel("Please enter your name:");
		lbcustname.setHorizontalAlignment(SwingConstants.CENTER);
		lbcustname.setFont(new Font("Tahoma", Font.BOLD, 16));
		lbcustname.setBounds(250, 50, 300, 40);
		framewelcome.getContentPane().add(lbcustname);
		
		//enter name text field
		final JTextField entryname = new JTextField();
		entryname.setBounds(325, 90, 150, 40);
		framewelcome.getContentPane().add(entryname);
		
		//select store label
		JLabel lbstoreselect = new JLabel("Please select a shop:");
		lbstoreselect.setHorizontalAlignment(SwingConstants.CENTER);
		lbstoreselect.setFont(new Font("Tahoma", Font.BOLD, 16));
		lbstoreselect.setBounds(250, 150, 300, 40);
		framewelcome.getContentPane().add(lbstoreselect);
		
		//store select combo box
		final JComboBox cbstoreselect = new JComboBox(Stores.toArray());
		cbstoreselect.setBounds(275, 190, 250, 30);
		framewelcome.getContentPane().add(cbstoreselect);
		
		//welcome button
		JButton buttonstorelogin = new JButton("Shop Data");
		buttonstorelogin.setBounds(550, 190, 150, 40);
		framewelcome.getContentPane().add(buttonstorelogin);
		buttonstorelogin.addActionListener(new ActionListener() {  
			public void actionPerformed(ActionEvent e) {
				storename = Stores.get(cbstoreselect.getSelectedIndex());
				edit();
			}
		});
		
		//select fav flavor label
		JLabel lbfavselect = new JLabel("Favorite flavor?");
		lbfavselect.setHorizontalAlignment(SwingConstants.CENTER);
		lbfavselect.setFont(new Font("Tahoma", Font.BOLD, 16));
		lbfavselect.setBounds(250, 250, 300, 40);
		framewelcome.getContentPane().add(lbfavselect);
		
		//store select combo box
		final JComboBox cbfavselect = new JComboBox(FavFlavors.toArray());
		cbfavselect.setBounds(275, 290, 250, 30);
		framewelcome.getContentPane().add(cbfavselect);
		
		//welcome button
		JButton buttonenter = new JButton("Enter Store");
		buttonenter.setBounds(325, 400, 150, 40);
		framewelcome.getContentPane().add(buttonenter);
		buttonenter.addActionListener(new ActionListener() {  
			public void actionPerformed(ActionEvent e) {
				custname = entryname.getText();
				custfavflavorname = FavFlavors.get(cbfavselect.getSelectedIndex());
				storename = Stores.get(cbstoreselect.getSelectedIndex());
				boolean canenter = AddCustomer();
				if (canenter) {
					initialize();
				}
				//framewelcome.dispatchEvent(new WindowEvent(framewelcome, WindowEvent.WINDOW_CLOSING));
			}
		});
		
		framewelcome.getContentPane().setLayout(null);
		framewelcome.setVisible(true);
	}
	
	private void edit() {
		if (framestore != null)
			framestore.dispatchEvent(new WindowEvent(framestore, WindowEvent.WINDOW_CLOSING));
		if (frame != null)
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		Flavors = new ArrayList<String>();
		FlavorsText = new ArrayList<String>();
		Toppings = new ArrayList<String>();
		ToppingsText = new ArrayList<String>();
		Cones = new ArrayList<String>();
		ConesText = new ArrayList<String>();
		CurrentCustomers = new ArrayList<String>();
		OrdersKeys = new ArrayList<Integer>();
		CurrentOrders = new ArrayList<String>();
		totalprice = 0.0;
		
		UpdateList("Flavor");
		UpdateList("Topping");
		UpdateList("Cone");
		UpdateList("CurrentCustomer");
		UpdateList("CurrentOrder");
		
		framestore = new JFrame("Store");
		framestore.setBounds(50, 50, 800, 600);
		framestore.setSize(800,600);
		framestore.setResizable(false);
		
		//store label
		JLabel lbstore = new JLabel(storename + " Management");
		lbstore.setFont(new Font("Tahoma", Font.BOLD, 16));
		lbstore.setBounds(20, 20, 500, 30);
		framestore.getContentPane().add(lbstore);
		
		//customer name label
		JLabel lbcustname = new JLabel("Customer List: (Name - Fav Flavor Key - # of Orders)");
		lbcustname.setFont(new Font("Tahoma", Font.BOLD, 14));
		lbcustname.setBounds(30, 60, 400, 30);
		framestore.getContentPane().add(lbcustname);
		
		//customer list
		listcustomer = new JList(CurrentCustomers.toArray());
		framestore.getContentPane().add(listcustomer);
		listcustomer.setBounds(30, 90, 400, 1000);
		panecustomer = new JScrollPane(listcustomer);
		panecustomer.setBounds(30, 90, 400, 150);
		panecustomer.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		framestore.getContentPane().add(panecustomer);
		
		//orders label
		JLabel lbordername = new JLabel("Order List: (Order# - Customer#)");
		lbordername.setFont(new Font("Tahoma", Font.BOLD, 14));
		lbordername.setBounds(30, 270, 400, 30);
		framestore.getContentPane().add(lbordername);
		
		//order list
		listorder = new JList(CurrentOrders.toArray());
		framestore.getContentPane().add(listorder);
		listorder.setSelectedIndex(0);
		listorder.setBounds(30,300, 400, 1000);
		paneorder = new JScrollPane(listorder);
		paneorder.setBounds(30, 300, 400, 150);
		paneorder.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		framestore.getContentPane().add(paneorder);
		
		//remove order
		JButton buttonremove = new JButton("Remove Order");
		buttonremove.setBounds(450, 300, 150, 40);
		//buttonremove.setEnabled(false);
		framestore.getContentPane().add(buttonremove);
		//make order function
		buttonremove.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				RemoveOrder(listorder.getSelectedIndex());
				edit();
			}  
		});
		
		framestore.getContentPane().setLayout(null);
		framestore.setVisible(true);
	}
	
	private void initialize() {
		if (frame != null)
			frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
		if (framestore != null)
			framestore.dispatchEvent(new WindowEvent(framestore, WindowEvent.WINDOW_CLOSING));
		Flavors = new ArrayList<String>();
		FlavorsText = new ArrayList<String>();
		Toppings = new ArrayList<String>();
		ToppingsText = new ArrayList<String>();
		Cones = new ArrayList<String>();
		ConesText = new ArrayList<String>();
		CurrentFlavors = new DefaultListModel();
		CurrentToppings = new DefaultListModel();
		CurrentCone = "";
		totalprice = 0.0;
		
		UpdateList("Flavor");
		UpdateList("Topping");
		UpdateList("Cone");
		
		frame = new JFrame("Ice Cream Shop");
		frame.setBounds(50, 50, 800, 600);
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800,600);
		frame.setResizable(false);
		
		//menu label
		JLabel lbmenu = new JLabel(storename + "'s Menu:");
		lbmenu.setFont(new Font("Tahoma", Font.BOLD, 16));
		lbmenu.setBounds(20, 20, 500, 30);
		frame.getContentPane().add(lbmenu);
		
		//customer name label
		JLabel lbcustname = new JLabel("Welcome, " + custname + "!");
		lbcustname.setHorizontalAlignment(SwingConstants.TRAILING);
		lbcustname.setFont(new Font("Tahoma", Font.BOLD, 14));
		lbcustname.setBounds(480, 20, 300, 15);
		frame.getContentPane().add(lbcustname);
		
		//customer key label
		JLabel lbcustkey = new JLabel("You are Customer#" + custkey + ".");
		lbcustkey.setHorizontalAlignment(SwingConstants.TRAILING);
		lbcustkey.setFont(new Font("Tahoma", Font.BOLD, 14));
		lbcustkey.setBounds(480, 35, 300, 15);
		frame.getContentPane().add(lbcustkey);
		
		//fav flavor key label
		JLabel lbffkey = new JLabel("Your favorite flavor is " + custfavflavorname + ".");
		lbffkey.setHorizontalAlignment(SwingConstants.TRAILING);
		lbffkey.setFont(new Font("Tahoma", Font.BOLD, 14));
		lbffkey.setBounds(280, 50, 500, 15);
		frame.getContentPane().add(lbffkey);
		
		//add flavor
		//add flavor label
		JLabel lbaddflavor = new JLabel("Add Flavor:");
		lbaddflavor.setBounds(30, 60, 250, 30);
		frame.getContentPane().add(lbaddflavor);
		//flavor select combo box
		final JComboBox cbflavorselect = new JComboBox(FlavorsText.toArray());
		cbflavorselect.setBounds(30, 90, 250, 30);
		frame.getContentPane().add(cbflavorselect);
		//flavor list label
		JLabel lbflavor = new JLabel("Current Flavors:");
		lbflavor.setBounds(30, 170, 250, 30);
		frame.getContentPane().add(lbflavor);
		//flavor list
		listflavor = new JList(CurrentFlavors.toArray());
		frame.getContentPane().add(listflavor);
		listflavor.setBounds(30, 210, 250, 100);
		paneflavor = new JScrollPane(listflavor);
		paneflavor.setBounds(30, 210, 250, 30);
		paneflavor.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.getContentPane().add(paneflavor);
		//add flavor button
		final JButton buttonflavor = new JButton("Add to Order");
		buttonflavor.setBounds(30, 130, 250, 30);
		frame.getContentPane().add(buttonflavor);
		//remove flavor button
		final JButton buttonremoveflavor = new JButton("Remove from Order");
		//buttonremoveflavor.setBackground(Color.PINK);
		buttonremoveflavor.setBounds(30, 330, 250, 30);
		buttonremoveflavor.setEnabled(false);
		frame.getContentPane().add(buttonremoveflavor);
		//add flavor button function
		buttonflavor.addActionListener(new ActionListener() {  
			public void actionPerformed(ActionEvent e) {       
				AddToCurrentList("Flavor", Flavors.get(cbflavorselect.getSelectedIndex()));
				buttonremoveflavor.setEnabled(true);
				if (CurrentFlavors.size() > 7)
					buttonflavor.setEnabled(false);
			}  
		});
		//remove flavor button function
		buttonremoveflavor.addActionListener(new ActionListener() {  
			public void actionPerformed(ActionEvent e) {
				RemoveFromCurrentList("Flavor", listflavor.getSelectedIndex());
				buttonflavor.setEnabled(true);
				if (CurrentFlavors.size() <= 0)
					buttonremoveflavor.setEnabled(false);
			}  
		});
		
		//add topping
		//add topping label
		JLabel lbaddtopping = new JLabel("Add Topping:");
		lbaddtopping.setBounds(360, 60, 250, 30);
		frame.getContentPane().add(lbaddtopping);
		//topping select combo box
		final JComboBox cbtoppingselect = new JComboBox(ToppingsText.toArray());
		cbtoppingselect.setBounds(360, 90, 250, 30);
		frame.getContentPane().add(cbtoppingselect);
		//topping list label
		JLabel lbtopping = new JLabel("Current Toppings:");
		lbtopping.setBounds(360, 170, 250, 30);
		frame.getContentPane().add(lbtopping);
		//topping list
		listtopping = new JList(CurrentToppings.toArray());
		frame.getContentPane().add(listtopping);
		listtopping.setBounds(360, 210, 250, 100);
		panetopping = new JScrollPane(listtopping);
		panetopping.setBounds(360, 210, 250, 30);
		panetopping.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.getContentPane().add(panetopping);
		//add topping button
		final JButton buttontopping = new JButton("Add to Order");
		buttontopping.setBounds(360, 130, 250, 30);
		frame.getContentPane().add(buttontopping);
		//remove topping button
		final JButton buttonremovetopping = new JButton("Remove from Order");
		//buttonremovetopping.setBackground(Color.PINK);
		buttonremovetopping.setBounds(360, 330, 250, 30);
		buttonremovetopping.setEnabled(false);
		frame.getContentPane().add(buttonremovetopping);
		//add topping button
		buttontopping.addActionListener(new ActionListener() {  
			public void actionPerformed(ActionEvent e) {       
				AddToCurrentList("Topping", Toppings.get(cbtoppingselect.getSelectedIndex()));
				buttonremovetopping.setEnabled(true);
				if (CurrentToppings.size() > 7)
					buttontopping.setEnabled(false);
			}  
		});
		//remove topping button function
		buttonremovetopping.addActionListener(new ActionListener() {  
			public void actionPerformed(ActionEvent e) {
				RemoveFromCurrentList("Topping", listtopping.getSelectedIndex());
				buttontopping.setEnabled(true);
				if (CurrentToppings.size() <= 0)
					buttonremovetopping.setEnabled(false);
			}  
		});
		
		//select cone
		//cone select label
		JLabel lbconeselect = new JLabel("Select Cone:");
		lbconeselect.setBounds(30, 360, 400, 30);
		frame.getContentPane().add(lbconeselect);
		//cone select combo box
		final JComboBox cbconeselect = new JComboBox(ConesText.toArray());
		cbconeselect.setBounds(30, 390, 400, 30);
		frame.getContentPane().add(cbconeselect);
		CurrentCone = Cones.get(cbconeselect.getSelectedIndex());
		cbconeselect.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		    	CurrentCone = Cones.get(cbconeselect.getSelectedIndex());
		    	UpdateTotalPriceLabel();
		    }
		});
		
		//total price label
		labeltotalprice = new JLabel("Total Price: $" + totalprice);
		labeltotalprice.setHorizontalAlignment(SwingConstants.TRAILING);
		labeltotalprice.setFont(new Font("Tahoma", Font.BOLD, 16));
		labeltotalprice.setBounds(260, 470, 500, 20);
		frame.getContentPane().add(labeltotalprice);
		UpdateTotalPriceLabel();
		
		//make orders
		buttonorder = new JButton("Make Order!");
		buttonorder.setBounds(610, 500, 150, 40);
		buttonorder.setEnabled(false);
		frame.getContentPane().add(buttonorder);
		//make order function
		buttonorder.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) {
				CurrentCone = Cones.get(cbconeselect.getSelectedIndex());
				MakeOrder();
			}  
		});
		
		frame.getContentPane().setLayout(null);
		frame.setVisible(true);
	}

}
