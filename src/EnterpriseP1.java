/* Name: Michael Gilday
 Course: CNT 4714 – Fall 2023
 Assignment title: Project 1 – Event-driven Enterprise Simulation
 Date: Sunday September 17, 2023
*/

/* Java application that creates a standalone GUI application that simulates an
e-store, henceforth known as Nile Dot Com, in which the user adds in stock items to a shopping cart. Once all items are included, total costs
(including tax), production of an invoice, and the appending of information to a transaction log file occurs.
*/

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class EnterpriseP1 {
    private final List<StoreItem> cart = new ArrayList<>();
    private final List<CSVRecord> csvRecords = new ArrayList<>();
    private final DefaultListModel<StoreItem> itemListModel;
    private final Map<String, Integer> availableQuantityMap = new HashMap<>();
    private int quantity; //This variable will be used to track user-entered quantities.
    private double totalCost = 0.0;
    private int items = 1; //This is the counter tracking the number of items in the cart.
    private double subtotal = 0.0;
    private StoreItem foundItem;

    private final JFrame frame;
    private static JLabel totalLabel;
    private static JTextArea detailsTextArea;
    private static JTextArea subtotalTextArea;
    private final JTextField IDTextField;
    private final JTextField quantityTextField;
    private final JButton addToCartButton;
    private final JButton viewCartButton;
    private final JButton checkoutButton;
    private final JButton findItemButton;
    private final JLabel IDLabel;
    private final JLabel quantityLabel;
    private final JLabel detailsLabel;
    private final JLabel subtotalLabel;

    public EnterpriseP1(JLabel totalLabel) {
        EnterpriseP1.totalLabel = totalLabel;
        frame = new JFrame("Nile.Com - Fall 2023");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(750, 450);
        frame.setLayout(new BorderLayout());

        //New content panel for the GUI that will be using the color gray.
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.GRAY);
        frame.setContentPane(contentPanel);

        itemListModel = new DefaultListModel<>();
        JList<StoreItem> itemList = new JList<>(itemListModel);
        frame.add(new JScrollPane(itemList), BorderLayout.CENTER);

        //Creating a transparent center panel of the GUI, which will hold the text boxes.
        JPanel upperPanel = new JPanel(new GridLayout(4, 2));
        upperPanel.setOpaque(false);
        frame.add(upperPanel, BorderLayout.CENTER);

        //Establishing the text displayed in the GUI.
        IDLabel = new JLabel("Enter Item ID for Item #" + items + ":");
        quantityLabel = new JLabel("Enter quantity for Item #" + items + ":");
        detailsLabel = new JLabel("Details for Item #0:");
        subtotalLabel = new JLabel("Order subtotal for " + cart.size() + " item(s):");

        //Setting the text color to yellow for the first two text displays.
        IDLabel.setForeground(Color.YELLOW);
        quantityLabel.setForeground(Color.YELLOW);

        //Setting the text color to red for the last two text displays.
        detailsLabel.setForeground(Color.RED);
        subtotalLabel.setForeground(Color.RED);

        //Creating the text Fields and text areas.
        IDTextField = createRoundedTextField(new JTextField());
        quantityTextField = createRoundedTextField(new JTextField());
        detailsTextArea = (JTextArea) createRoundedTextArea(new JTextArea());
        detailsTextArea.setEditable(false);
        subtotalTextArea = (JTextArea) createRoundedTextArea(new JTextArea());
        subtotalTextArea.setEditable(false);

        //Adding different initiated objects to the upper section of the GUI.
        upperPanel.add(IDLabel);
        upperPanel.add(IDTextField);
        upperPanel.add(quantityLabel);
        upperPanel.add(quantityTextField);
        upperPanel.add(detailsLabel);
        upperPanel.add(detailsTextArea);
        upperPanel.add(subtotalLabel);
        upperPanel.add(subtotalTextArea);

        //Creating the panel for lower half of the GUI.
        JPanel controlPanel = new JPanel(new GridLayout(3, 2));
        frame.add(controlPanel, BorderLayout.SOUTH);

        //Creating the buttons for the GUI, setting the font size, and whether certain buttons are disabled upon startup.
        Font buttonFont = new Font("Arial", Font.PLAIN, 16);
        findItemButton = new JButton("Find Item");
        findItemButton.setFont(buttonFont);
        addToCartButton = new JButton("Add Item To Cart");
        addToCartButton.setFont(buttonFont);
        addToCartButton.setEnabled(false);
        viewCartButton = new JButton("View Cart");
        viewCartButton.setFont(buttonFont);
        viewCartButton.setEnabled(false);
        checkoutButton = new JButton("Checkout");
        checkoutButton.setFont(buttonFont);
        checkoutButton.setEnabled(false);
        JButton emptyCartButton = new JButton("Empty Cart - Start A New Order");
        emptyCartButton.setFont(buttonFont);
        JButton exitButton = new JButton("Exit (Close App)");
        exitButton.setFont(buttonFont);

        //Adding the initiated buttons to the lower half of the GUI.
        controlPanel.add(findItemButton);
        controlPanel.add(addToCartButton);
        controlPanel.add(viewCartButton);
        controlPanel.add(checkoutButton);
        controlPanel.add(emptyCartButton);
        controlPanel.add(exitButton);

        //Calling the "readInventory" function.
        readInventory();

        frame.setVisible(true);

        //Functionality for the "Find Item" button".
        findItemButton.addActionListener(e -> {
            //The below block attempts to read the two user entry text fields. With spaces trimmed for program readability.
            String itemID = IDTextField.getText().trim();
            String quantityStr = quantityTextField.getText().trim();

            //This 'if' statement is used to prevent an error from occurring, which happens when the user does not enter a value into at least one of the user text-entry fields.
            if (itemID.isEmpty() || quantityStr.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please enter both item ID and quantity.");
                return;
            }

            //Now that values are confirmed to exist in the text entry spaces, they can be parsed for reading by the program.
            quantity = Integer.parseInt(quantityStr);
            foundItem = findItemByID(itemID);

            if (foundItem == null) { //This 'if' statement is entered if the entered value does not match any ID in the csv file.
                JOptionPane.showMessageDialog(frame, "Item ID " + itemID + " not found in the file.");
                IDTextField.setText("");
                quantityTextField.setText("");
            } else if (!foundItem.isAvailable()) { //This is determined by the true/false value associated with the item. False indicates that there is none currently.
                JOptionPane.showMessageDialog(frame, "Sorry... that item is out of stock, please try another item.");
                IDTextField.setText("");
                quantityTextField.setText("");
            } else if (quantity <= 0) { //This is entered if the input quantity value is below the valid amount of 1.
                JOptionPane.showMessageDialog(frame, "Please enter a valid quantity.");
                quantityTextField.setText("");
            } else if (quantity > availableQuantityMap.getOrDefault(foundItem.itemId(), 0)) { //This is entered when the provided user input quantity is above what is in stock.
                JOptionPane.showMessageDialog(frame, "Insufficient stock. Only " + availableQuantityMap.getOrDefault(foundItem.itemId(), 0) + " on hand. Please reduce the quantity.");
                quantityTextField.setText("");
            } else { //This is entered when the quantity and ID are valid, and there's an adequate amount of supply for the user.
                detailsTextArea.setText(formatCartItem(foundItem, quantity));
                detailsLabel.setText("Details for Item #" + items + ":");
                IDTextField.setEditable(false);
                quantityTextField.setEditable(false);
                findItemButton.setEnabled(false);
                addToCartButton.setEnabled(true);
            }
        });

        //Functionality for the "Add Item to Cart" button.
        addToCartButton.addActionListener(e -> {
            //Grabbing the user input value for quantity and parsing it for program readability.
            String quantityStr = quantityTextField.getText();
            quantity = Integer.parseInt(quantityStr);

            //Updating various views, adding the item to the cart, and incrementing the items in cart amount.
            addToCart(foundItem, quantity);
            updateTotalLabel();
            updateCartView();
            items++;

            //Updating text display in the GUI.
            IDLabel.setText("Enter Item ID for Item #" + items + ":");
            quantityLabel.setText("Enter quantity for Item #" + items + ":");
            subtotalLabel.setText("Order subtotal for " + cart.size() + " item(s):");

            //Emptying the text fields so that a user can add another item.
            IDTextField.setText("");
            quantityTextField.setText("");

            //Enabling most button functionalities, and disabling the 'Add to Cart' button.
            IDTextField.setEditable(true);
            quantityTextField.setEditable(true);
            findItemButton.setEnabled(true);
            addToCartButton.setEnabled(false);
            viewCartButton.setEnabled(true);
            checkoutButton.setEnabled(true);
        });

        //Functionality for the "View Cart" button.
        viewCartButton.addActionListener(e -> {
            //The cart should not be empty, but just in case code functionality goes haywire, then the else statement will save the program from bugs.
            if (!cart.isEmpty()) {
                StringBuilder cartDetails = new StringBuilder();
                int itemNumber = 1;

                //Adding objects that have been added to the cart to the cart view.
                for (StoreItem item : cart) {
                    cartDetails.append(itemNumber).append(". ").append(formatCartItem(item, item.quantity())).append("\n");
                    itemNumber++;
                }
                JOptionPane.showMessageDialog(frame, cartDetails.toString(), "Nile Dot Com - Current Shopping Cart Status", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "The cart is empty.", "Nile Dot Com - Current Shopping Cart Status", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        //Functionality for the "Checkout" button.
        checkoutButton.addActionListener(e -> checkout());

        //Functionality for the "Empty Cart" button.
        emptyCartButton.addActionListener(e -> clearCart());

        //Functionality for the "Exit" button. Not entirely necessary, but added to match project requirements.
        exitButton.addActionListener(e -> System.exit(0));
    }

    //Necessary function for clearing the cart, which also includes updating various variables to their initial settings.
    private void clearCart() {
        cart.clear();
        totalCost = 0.0;
        subtotal = 0.0;
        updateCartView();
        updateTotalLabel();

        //Resetting the item tracking value as well as the various GUI displayed text.
        items = 1;
        IDLabel.setText("Enter Item ID for Item #" + items + ":");
        quantityLabel.setText("Enter quantity for Item #" + items + ":");
        detailsLabel.setText("Details for Item #0:");
        subtotalLabel.setText("Order subtotal for " + cart.size() + " item(s):");

        //Resetting the various text fields.
        IDTextField.setText("");
        quantityTextField.setText("");
        detailsTextArea.setText("");
        subtotalTextArea.setText("");

        //Setting the buttons to their default values.
        IDTextField.setEditable(true);
        quantityTextField.setEditable(true);
        findItemButton.setEnabled(true);
        addToCartButton.setEnabled(false);
        viewCartButton.setEnabled(false);
        checkoutButton.setEnabled(false);
    }

    //Simple function used to change the line borders for the text areas to rounded.
    private Component createRoundedTextArea(JTextArea textArea) {
        textArea.setBorder(new LineBorder(Color.BLACK, 1, true));
        return textArea;
    }

    //Simple function used to change the line borders for the text fields to rounded.
    private JTextField createRoundedTextField(JTextField textField) {
        textField.setBorder(new LineBorder(Color.BLACK, 1, true));
        return textField;
    }

    //The function below reads the 'inventory' from the "inventory.csv" text file.
    private void readInventory() {
        //Attempting to read the "inventory.csv" file, and then storing it.
        try (FileReader fileReader = new FileReader("inventory.csv");
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT)) {

            for (CSVRecord record : csvParser) {
                csvRecords.add(record); //Storing the CSV information.
                String itemId = record.get(0);
                int quantity = Integer.parseInt(record.get(3).trim());

                availableQuantityMap.put(itemId, quantity); //Storing the available quantity associated with specific IDs.
            }

        } catch (IOException e) {
            //e.printStackTrace(); //Commenting out the printStackTrace so that a warning stops appearing.
        }
    }

    //The function below is used to store each object found in the associated csv file in the constructor "StoreItems".
    private StoreItem findItemByID(String itemID) {
        for (CSVRecord record : csvRecords) {
            if (record.get(0).trim().equals(itemID.trim())) {
                String itemId = record.get(0).trim();
                String itemDescription = record.get(1).trim();
                boolean isAvailable = Boolean.parseBoolean(record.get(2).trim());
                int quantity = Integer.parseInt(record.get(3).trim());
                double itemPrice = Double.parseDouble(record.get(4).trim());

                return new StoreItem(itemId, itemDescription, isAvailable, quantity, itemPrice);
            }
        }
        return null; //Items not found.
    }

    //The function below compares the provided quantity value to hard-coded discounts.
    //If the value is between 1 and 4 then no discount is provided. 5~9 is a 10% discount. 10~14 is a 15% discount. And 15+ items is a 20% discount.
    private double calcDiscount(int quantity) {
        if (quantity >= 1 && quantity <= 4) {
            return 0.0;
        } else if (quantity >= 5 && quantity <= 9) {
            return 10.0;
        } else if (quantity >= 10 && quantity <= 14) {
            return 15.0;
        } else {
            return 20.0;
        }
    }

    //The function below is used to calculate the total price for a single item purchase, based on the quantity and associated discount found in the calcDiscount function.
    private double itemPriceTotal(int quantity, double price) {
        double discount = calcDiscount(quantity);
        double discountedPrice = price * (1.0 - (discount/100.0));
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        return Double.parseDouble(decimalFormat.format(quantity * discountedPrice));
    }

    //The function below is used to return specific items in the format required by the project.
    private String formatCartItem(StoreItem item, int quantity) {
        return  item.itemId() + " " + item.itemDescription() + " $" + item.price() + " " + quantity + " " + calcDiscount(quantity) + "% $" + itemPriceTotal(quantity, item.price());
    }

    //The function below is used to manage the total, subtotal, and what are currently found in the cart.
    private void addToCart(StoreItem item, int quantity) {
        //The line below is used to add an item to the cart.
        cart.add(new StoreItem(item.itemId(), item.itemDescription(), item.isAvailable(), quantity, item.price()));

        //This value is used to calculate the total cost by calling "itemPriceTotal".
        totalCost += itemPriceTotal(quantity, item.price());

        //The two lines below are used to update the subTotal value and the associated GUI display.
        subtotal += itemPriceTotal(quantity, item.price());
        subtotalTextArea.setText("$" + String.format("%.2f", subtotal));

        updateTotalLabel();
    }

    //The function below is used to format the checkout display.
    private void checkout() {
        //The two doubles below are used to calculate the separate tax value of the whole order, as well as the grand total.
        double tax = totalCost * 0.06;
        double grandTotal = totalCost + tax;

        tax = Double.parseDouble(String.format("%.2f", tax));
        grandTotal = Double.parseDouble(String.format("%.2f", grandTotal));

        //Creating a timestamp for the invoice.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm:ss zzz", Locale.US);
        ZonedDateTime zdt = ZonedDateTime.now();

        //Necessary display strings for the invoice.
        StringBuilder invoice = new StringBuilder();
        invoice.append("Date: ").append(dtf.format(zdt)).append("\n\n");
        invoice.append("Number of line items: ").append(cart.size()).append("\n\n");
        invoice.append("Item# / ID / Title / Price / Qty / Disc % / Subtotal:\n\n");

        int itemNumber = 1;

        //Formatting the items in the required format for the display pop-up.
        for (StoreItem item : cart) {
            invoice.append(itemNumber).append(". ").append(item.itemId()).append(" ").append(item.itemDescription())
                    .append(" $").append(item.price()).append(" ").append(item.quantity()).append(" ")
                    .append(calcDiscount(item.quantity())).append("% $").append(itemPriceTotal(item.quantity(), item.price()))
                    .append("\n");
            itemNumber++;
        }

        //Appending more necessary display strings for the invoice.
        invoice.append("\n\nOrder Subtotal: $").append(totalCost).append("\n\n");
        invoice.append("Tax rate: 6%\n\n");
        invoice.append("Tax amount: $").append(tax).append("\n\n");
        invoice.append("ORDER TOTAL: $").append(grandTotal).append("\n");
        invoice.append("\nThanks for Shopping at Nile Dot Com!");

        JOptionPane.showMessageDialog(frame, invoice.toString(), "Nile Dot Com - FINAL Invoice", JOptionPane.INFORMATION_MESSAGE);

        //Writing transactions to the 'transactions.csv' log file.
        appendTransaction(cart);
        clearCart();
    }

    //The function below is used to write the properly formatted information in a "transactions.csv" file.
    private void appendTransaction(List<StoreItem> items) {
        //The code block below is used to get the current time, and format it in two different ways needed for the output file.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ZonedDateTime zdt = ZonedDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm:ss zzz", Locale.US);

        //The program will attempt to write to, or create (if the file does not exist), to a "transactions.csv", following the format specified by the project.
        try (PrintWriter writer = new PrintWriter(new FileWriter("transactions.csv", true))) {
            for (StoreItem item : items) {
                String transaction = formatter.format(zdt) + ", " + item.itemId() + ", " + item.itemDescription() + ", " + item.price()
                        + ", " + item.quantity() + ", " + calcDiscount(item.quantity()) + ", $"
                        + itemPriceTotal(item.quantity(), item.price()) + ", " + ", " + dtf.format(zdt);
                writer.println(transaction);
            }
            writer.println(); //This extra new line is used to separate entire orders from each other in the csv file.
        } catch (IOException e) {
            //e.printStackTrace(); //Commenting out the printStackTrace so that a warning stops appearing.
        }
    }

    //The function below simply updates the totalLabel w/ the correct values.
    private void updateTotalLabel() {
        totalLabel.setText("Total: $" + String.format("%.2f", totalCost));
    }

    //The function below is simply used to update the cart view.
    private void updateCartView() {
        itemListModel.clear();
        for (StoreItem item : cart) {
            itemListModel.addElement(item);
        }
    }

    public static void main(String[] args) {
        JLabel totalLabel = new JLabel("Total: $0.00"); //Used to prevent JLabel from being null. That creates some errors.
        SwingUtilities.invokeLater(() -> new EnterpriseP1(totalLabel)); //Initiating the GUI program.
    }
}

//Constructor function.
record StoreItem(String itemId, String itemDescription, boolean isAvailable, int quantity, double price) {

    @Override
    public String toString() {
        return itemDescription;
    }
}