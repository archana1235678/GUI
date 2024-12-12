 import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

// Pizza class
class Pizza {
    private String size;
    private Vector<String> toppings;
    private double price;

    public Pizza(String size) {
        this.size = size;
        this.toppings = new Vector<>();
        this.price = calculatePrice(size);
    }

    private double calculatePrice(String size) {
        double basePrice;
        switch (size.toLowerCase()) {
            case "small":
                basePrice = 100;
                break;
            case "medium":
                basePrice = 150;
                break;
            case "large":
                basePrice = 200;
                break;
            default:
                basePrice = 0;
                break;
        }
        return basePrice;
    }

    public void addTopping(String topping) {
        toppings.add(topping);
        this.price += 1.50; // Each topping costs 1.50
    }

    public double getPrice() {
        return price;
    }

    public String getSize() {
        return size;
    }

    public Vector<String> getToppings() {
        return toppings;
    }

    @Override
    public String toString() {
        return "Size: " + size + ", Toppings: " + String.join(", ", toppings) + " | Price: ₹" + (int) price;
    }
}

class PizzaShop {
    private Connection conn;
    private Vector<Vector<Pizza>> orders;

    public PizzaShop() {
        // Initialize the orders vector
        orders = new Vector<>();

        // Establish a connection to the database
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Updated driver class
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/pizza_shop_db", "root", "");
            System.out.println("Connected to the database!");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    public void addOrder(Vector<Pizza> order) {
        if (orders.size() < 100) {
            orders.add(order);

            try {
                // Insert the order into the database
                String insertOrderSQL = "INSERT INTO orders (order_time) VALUES (NOW())";
                try (PreparedStatement pst = conn.prepareStatement(insertOrderSQL, Statement.RETURN_GENERATED_KEYS)) {
                    pst.executeUpdate();
                    ResultSet rs = pst.getGeneratedKeys();
                    if (rs.next()) {
                        int orderId = rs.getInt(1);
                        // Now save each pizza in the order
                        for (Pizza pizza : order) {
                            savePizza(pizza, orderId);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            JOptionPane.showMessageDialog(null, "Order placed successfully.");
        } else {
            JOptionPane.showMessageDialog(null, "Maximum order limit reached!");
        }
    }

    private void savePizza(Pizza pizza, int orderId) throws SQLException {
        String insertPizzaSQL = "INSERT INTO pizzas (size, toppings, price, order_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(insertPizzaSQL)) {
            pst.setString(1, pizza.getSize());
            pst.setString(2, String.join(", ", pizza.getToppings())); // Join toppings as a comma-separated string
            pst.setDouble(3, pizza.getPrice());
            pst.setInt(4, orderId);
            pst.executeUpdate();
        }
    }

    public void displayOrders() {
        if (orders.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No orders placed.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orders.size(); i++) {
            sb.append("Order #").append(i + 1).append(":\n");
            Vector<Pizza> order = orders.get(i);
            int orderTotal = 0;
            for (int j = 0; j < order.size(); j++) {
                sb.append("  Pizza #").append(j + 1).append(": ").append(order.get(j)).append("\n");
                orderTotal += order.get(j).getPrice();
            }
            sb.append("  Total for Order #").append(i + 1).append(": ₹").append(orderTotal).append("\n\n");
        }
        JOptionPane.showMessageDialog(null, sb.toString());
    }

    // Delete pizza from the order
    public void deletePizzaFromOrder(int orderIndex, int pizzaIndex) {
        if (orderIndex >= 0 && orderIndex < orders.size()) {
            Vector<Pizza> order = orders.get(orderIndex);
            if (pizzaIndex >= 0 && pizzaIndex < order.size()) {
                order.remove(pizzaIndex);
                JOptionPane.showMessageDialog(null, "Pizza removed from order.");
            } else {
                JOptionPane.showMessageDialog(null, "Pizza index is invalid.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Order index is invalid.");
        }
    }

    // Delete an entire order
    public void deleteOrder(int orderIndex) {
        if (orderIndex >= 0 && orderIndex < orders.size()) {
            orders.remove(orderIndex);
            JOptionPane.showMessageDialog(null, "Order deleted successfully.");
        } else {
            JOptionPane.showMessageDialog(null, "Order index is invalid.");
        }
    }

    // Add a pizza to an existing order
    public void addPizzaToOrder(int orderIndex, Pizza pizza) {
        if (orderIndex >= 0 && orderIndex < orders.size()) {
            Vector<Pizza> order = orders.get(orderIndex);
            order.add(pizza);  // Add the pizza to the order
            JOptionPane.showMessageDialog(null, "Pizza added to order.");
        } else {
            JOptionPane.showMessageDialog(null, "Order index is invalid.");
        }
    }
}

// PizzaAppGUI class
public class PizzaAppGUI extends JFrame {
    private PizzaShop pizzaShop;
    private static final String[] VALID_SIZES = {"small", "medium", "large"};
    private static final String[] VALID_TOPPINGS = {"Onions", "Black Olives", "Mushrooms", "Extra Cheese", "Pepperoni", "Sausage", "Bell Pepper"};

    public PizzaAppGUI() {
        pizzaShop = new PizzaShop();

        setTitle("Pizza Shop");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(6, 1));

        JButton placeOrderButton = new JButton("Place a new order");
        JButton addPizzaButton = new JButton("Add a pizza to an existing order");
        JButton deletePizzaButton = new JButton("Delete a pizza from an existing order");
        JButton deleteOrderButton = new JButton("Delete an existing order");
        JButton displayOrdersButton = new JButton("Display all orders");
        JButton exitButton = new JButton("Exit");

        placeOrderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                placeOrder();
            }
        });

        addPizzaButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addPizzaToOrder();
            }
        });

        deletePizzaButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deletePizzaFromOrder();
            }
        });

        deleteOrderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteOrder();
            }
        });

        displayOrdersButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pizzaShop.displayOrders();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        add(placeOrderButton);
        add(addPizzaButton);
        add(deletePizzaButton);
        add(deleteOrderButton);
        add(displayOrdersButton);
        add(exitButton);
    }

    private void placeOrder() {
        Vector<Pizza> currentOrder = new Vector<>();
        
        while (true) {
            String size = getValidSizeInput();
            
            if (size == null || size.equalsIgnoreCase("done")) {
                if (currentOrder.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No pizzas selected, order not placed.");
                }
                break;
            }

            Pizza pizza = new Pizza(size);
            while (true) {
                String topping = getValidToppingInput();
                if (topping == null || topping.equalsIgnoreCase("done")) {
                    break;
                }
                pizza.addTopping(topping);
            }

            currentOrder.add(pizza);
        }

        if (!currentOrder.isEmpty()) {
            pizzaShop.addOrder(currentOrder);
        }
    }

    private String getValidSizeInput() {
        while (true) {
            String size = JOptionPane.showInputDialog(this, "Enter pizza size (small, medium, large) or 'done' to finish this order:");
            if (size == null || size.equalsIgnoreCase("done")) {
                return size;
            }

            for (String validSize : VALID_SIZES) {
                if (validSize.equalsIgnoreCase(size)) {
                    return size;
                }
            }

            JOptionPane.showMessageDialog(this, "Invalid size. Please choose from: small, medium, or large.");
        }
    }

    private String getValidToppingInput() {
        while (true) {
            String topping = JOptionPane.showInputDialog(this, "Enter a topping from " + String.join(", ", VALID_TOPPINGS) + " or 'done' to finish:");
            if (topping == null || topping.equalsIgnoreCase("done")) {
                return topping;
            }

            for (String validTopping : VALID_TOPPINGS) {
                if (validTopping.equalsIgnoreCase(topping)) {
                    return topping;
                }
            }

            JOptionPane.showMessageDialog(this, "Invalid topping. Please choose a valid topping.");
        }
    }

    private void addPizzaToOrder() {
        String orderIndexString = JOptionPane.showInputDialog(this, "Enter order index to add a pizza to:");
        int orderIndex = Integer.parseInt(orderIndexString);
        String size = getValidSizeInput();
        Pizza pizza = new Pizza(size);

        while (true) {
            String topping = getValidToppingInput();
            if (topping == null || topping.equalsIgnoreCase("done")) {
                break;
            }
            pizza.addTopping(topping);
        }

        pizzaShop.addPizzaToOrder(orderIndex, pizza);
    }

    private void deletePizzaFromOrder() {
        String orderIndexString = JOptionPane.showInputDialog(this, "Enter order index to delete a pizza from:");
        int orderIndex = Integer.parseInt(orderIndexString);
        String pizzaIndexString = JOptionPane.showInputDialog(this, "Enter pizza index to delete:");
        int pizzaIndex = Integer.parseInt(pizzaIndexString);
        
        pizzaShop.deletePizzaFromOrder(orderIndex, pizzaIndex);
    }

    private void deleteOrder() {
        String orderIndexString = JOptionPane.showInputDialog(this, "Enter order index to delete:");
        int orderIndex = Integer.parseInt(orderIndexString);

        pizzaShop.deleteOrder(orderIndex);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new PizzaAppGUI().setVisible(true);
            }
        });
    }
}

