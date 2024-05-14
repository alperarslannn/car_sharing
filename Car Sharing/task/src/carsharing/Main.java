package carsharing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static final String JDBC_DRIVER = "org.h2.Driver";
    static String DB_URL;

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-databaseFileName".equals(args[i]) && i + 1 < args.length) {
                DB_URL = "jdbc:h2:./src/carsharing/db/" + args[i + 1];
            }
        }

        try {
            Class.forName(JDBC_DRIVER);
            createCompanyTable();
            createCarTable();
            createCustomerTable();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1. Log in as a manager");
            System.out.println("2. Log in as a customer");
            System.out.println("3. Create a customer");
            System.out.println("0. Exit");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 0:
                    return;
                case 1:
                    managerMenu(scanner);
                    break;
                case 2:
                    customerLogin(scanner);
                    break;
                case 3:
                    createCustomer(scanner);
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        }
    }

    private static void createCustomer(Scanner scanner) {
        System.out.println("Enter the customer name:");
        String customerName = scanner.nextLine();

        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO CUSTOMER (NAME) VALUES (?)")) {
            connection.setAutoCommit(true);

            preparedStatement.setString(1, customerName);
            int insertedRows = preparedStatement.executeUpdate();

            if (insertedRows > 0) {
                System.out.println("Customer '" + customerName + "' created successfully!");
            } else {
                System.out.println("Failed to create customer.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void customerLogin(Scanner scanner) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
             ResultSet resultSet = statement.executeQuery("SELECT ID, NAME FROM CUSTOMER")) {
            connection.setAutoCommit(true);

            if (!resultSet.isBeforeFirst()) {
                System.out.println("The customer list is empty!");
                return;
            }

            System.out.println("Choose a customer:");
            int index = 1;
            while (resultSet.next()) {
                System.out.println(index + ". " + resultSet.getString("NAME"));
                index++;
            }
            System.out.println("0. Back");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                return;
            } else if (choice > 0 && choice < index) {
                String customerName = "";
                int customerId = 0;
                resultSet.absolute(choice);
                customerName = resultSet.getString("NAME");
                customerId = resultSet.getInt("ID");
                customerMenu(scanner, customerName, customerId);
            } else {
                System.out.println("Invalid choice, please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void customerMenu(Scanner scanner, String customerName, int customerId) {
        while (true) {
            System.out.println("'" + customerName + "' menu:");
            System.out.println("1. Rent a car");
            System.out.println("2. Return a rented car");
            System.out.println("3. My rented car");
            System.out.println("0. Back");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 0:
                    return;
                case 1:
                    if(checkAlreadyRentedCar(customerId)) break;
                    rentCar(customerId, scanner);
                    break;
                case 2:
                    returnRentedCar(customerId);
                    break;
                case 3:
                    displayRentedCar(customerId);
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        }
    }

    private static boolean checkAlreadyRentedCar(int customerId) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT CAR.NAME AS CAR_NAME, COMPANY.NAME AS COMPANY_NAME " +
                             "FROM CUSTOMER " +
                             "JOIN CAR ON CUSTOMER.RENTED_CAR_ID = CAR.ID " +
                             "JOIN COMPANY ON CAR.COMPANY_ID = COMPANY.ID " +
                             "WHERE CUSTOMER.ID = ?")) {
            connection.setAutoCommit(true);

            preparedStatement.setInt(1, customerId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                System.out.println("You've already rented a car!");
                return true;
            } else {
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }


    private static void displayRentedCar(int customerId) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT CAR.NAME AS CAR_NAME, COMPANY.NAME AS COMPANY_NAME " +
                             "FROM CUSTOMER " +
                             "JOIN CAR ON CUSTOMER.RENTED_CAR_ID = CAR.ID " +
                             "JOIN COMPANY ON CAR.COMPANY_ID = COMPANY.ID " +
                             "WHERE CUSTOMER.ID = ?")) {
            connection.setAutoCommit(true);

            preparedStatement.setInt(1, customerId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String carName = resultSet.getString("CAR_NAME");
                String companyName = resultSet.getString("COMPANY_NAME");
                System.out.println("Your rented car:");
                System.out.println(carName);
                System.out.println("Company:");
                System.out.println(companyName);
            } else {
                System.out.println("You didn't rent a car!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void returnRentedCar(int customerId) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "UPDATE CUSTOMER SET RENTED_CAR_ID = NULL WHERE ID = ? AND RENTED_CAR_ID IS NOT NULL")) {
            connection.setAutoCommit(true);

            preparedStatement.setInt(1, customerId);
            int updatedRows = preparedStatement.executeUpdate();

            if (updatedRows > 0) {
                System.out.println("You've returned a rented car!");
            } else {
                System.out.println("You didn't rent a car!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void rentCar(int customerId, Scanner scanner) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
             ResultSet resultSet = statement.executeQuery(
                     "SELECT ID, NAME FROM COMPANY WHERE ID NOT IN (SELECT COMPANY_ID FROM CAR WHERE ID IN (Select RENTED_CAR_ID from CUSTOMER))")) {
            connection.setAutoCommit(true);

            if (!resultSet.isBeforeFirst()) {
                System.out.println("The company list is empty!");
                return;
            }

            System.out.println("Choose a company to rent a car from:");
            int index = 1;
            while (resultSet.next()) {
                System.out.println(index + ". " + resultSet.getString("NAME"));
                index++;
            }

            System.out.println("0. Back");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                return;
            }
            if (choice > 0 && choice < index) {
                resultSet.absolute(choice);
                int companyId = resultSet.getInt("ID");
                String companyName = resultSet.getString("NAME");
                rentCarFromCompany(customerId, companyId, companyName, scanner);
            } else {
                System.out.println("Invalid choice, please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void rentCarFromCompany(int customerId, int companyId, String companyName, Scanner scanner) {
        try (Connection connection = DriverManager.getConnection(DB_URL)) {
            connection.setAutoCommit(true);

            List<String> availableCars = getAvailableCarsInCompany(connection, companyId);

            if (availableCars.isEmpty()) {
                System.out.println("No available cars in the '" + companyName + "' company.");
                return;
            }

            System.out.println("Choose a car from '" + companyName + "' company:");
            int index = 1;
            for (String car : availableCars) {
                System.out.println(index + ". " + car);
                index++;
            }
            System.out.println("0. Back");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                return;
            } else if (choice > 0 && choice < index) {
                String chosenCar = availableCars.get(choice - 1);
                rentSelectedCar(connection, customerId, companyId, chosenCar);
            } else {
                System.out.println("Invalid choice, please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static List<String> getAvailableCarsInCompany(Connection connection, int companyId) throws SQLException {
        List<String> availableCars = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT NAME FROM CAR WHERE COMPANY_ID = ? ORDER BY ID")) {

            connection.setAutoCommit(true);

            preparedStatement.setInt(1, companyId);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                availableCars.add(resultSet.getString("NAME"));
            }
        }
        return availableCars;
    }

    private static void rentSelectedCar(Connection connection, int customerId, int companyId, String chosenCar) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE CUSTOMER SET RENTED_CAR_ID = (SELECT ID FROM CAR WHERE COMPANY_ID = ? AND NAME = ? AND RENTED_CAR_ID IS NULL) WHERE ID = ?")) {
            connection.setAutoCommit(true);

            preparedStatement.setInt(1, companyId);
            preparedStatement.setString(2, chosenCar);
            preparedStatement.setInt(3, customerId);
            int updatedRows = preparedStatement.executeUpdate();

            if (updatedRows > 0) {
                System.out.println("You rented '" + chosenCar + "'!");
            } else {
                System.out.println("An error occurred while renting the car.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    private static void managerMenu(Scanner scanner) {
        while (true) {
            System.out.println("1. Company list");
            System.out.println("2. Create a company");
            System.out.println("0. Back");
            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                break;
            } else if (choice == 1) {
                chooseCompany(scanner);
            } else if (choice == 2) {
                createCompany(scanner);
            } else {
                System.out.println("Invalid choice, please try again.");
            }
        }
    }

    private static void createCompanyTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            connection.setAutoCommit(true);

            String sql = "CREATE TABLE IF NOT EXISTS COMPANY (" +
                    "ID INTEGER AUTO_INCREMENT PRIMARY KEY, " +
                    "NAME VARCHAR(255) UNIQUE NOT NULL)";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createCarTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            connection.setAutoCommit(true);

            String sql = "CREATE TABLE IF NOT EXISTS CAR (" +
                    "ID INTEGER AUTO_INCREMENT PRIMARY KEY, " +
                    "NAME VARCHAR(255) UNIQUE NOT NULL, " +
                    "COMPANY_ID INTEGER NOT NULL, " +
                    "FOREIGN KEY (COMPANY_ID) REFERENCES COMPANY(ID))";
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createCustomerTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            connection.setAutoCommit(true);

            String sql = "CREATE TABLE IF NOT EXISTS CUSTOMER ( " +
                    "    ID INT AUTO_INCREMENT PRIMARY KEY, " +
                    "    NAME VARCHAR(255) UNIQUE NOT NULL, " +
                    "    RENTED_CAR_ID INT, " +
                    "    FOREIGN KEY (RENTED_CAR_ID) REFERENCES CAR(ID) " +
                    ")";
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void chooseCompany(Scanner scanner) {
        try (Connection connection = DriverManager.getConnection(DB_URL);
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
             ResultSet resultSet = statement.executeQuery("SELECT ID, NAME FROM COMPANY")) {
            connection.setAutoCommit(true);

            if (!resultSet.isBeforeFirst()) {
                System.out.println("The company list is empty!");
                return;
            }

            System.out.println("Choose a company:");
            int index = 1;
            while (resultSet.next()) {
                System.out.println(index + ". " + resultSet.getString("NAME"));
                index++;
            }
            System.out.println("0. Back");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                return;
            } else if (choice > 0 && choice < index) {
                resultSet.absolute(choice);
                String companyName = resultSet.getString("NAME");
                int companyId = resultSet.getInt("ID");
                companyMenu(scanner, companyName, companyId);
            } else {
                System.out.println("Invalid choice, please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void companyMenu(Scanner scanner, String companyName, int companyId) {
        while (true) {
            System.out.println("'" + companyName + "' company:");
            System.out.println("1. Car list");
            System.out.println("2. Create a car");
            System.out.println("0. Back");

            int choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                break;
            } else if (choice == 1) {
                listCars(companyName, companyId);
            } else if (choice == 2) {
                createCar(scanner, companyId);
            } else {
                System.out.println("Invalid choice, please try again.");
            }
        }
    }

    private static void createCar(Scanner scanner, int companyId) {
        System.out.println("Enter the car name:");
        String name = scanner.nextLine();

        String sql = "INSERT INTO CAR (NAME, COMPANY_ID) VALUES (?, ?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(true);

            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, companyId);
            preparedStatement.executeUpdate();
            System.out.println("The car was created!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void listCars(String companyName, int companyId) {
        String sql = "SELECT ID, NAME FROM CAR WHERE COMPANY_ID = ? ORDER BY ID";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(true);

            preparedStatement.setInt(1, companyId);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.isBeforeFirst()) {
                System.out.println("The car list is empty!");
            } else {
                System.out.println("'" + companyName + "' cars:");
                int index = 1;
                while (resultSet.next()) {
                    System.out.println(index + ". " + resultSet.getString("NAME"));
                    index++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createCompany(Scanner scanner) {
        System.out.println("Enter the company name:");
        String name = scanner.nextLine();

        String sql = "INSERT INTO COMPANY (NAME) VALUES (?)";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(true);

            preparedStatement.setString(1, name);
            preparedStatement.executeUpdate();
            System.out.println("The company was created!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}