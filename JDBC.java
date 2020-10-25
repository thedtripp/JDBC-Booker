//REFACTOR
import java.util.Scanner;
import java.sql.*;
public class JDBC {

    //TODO add uniquness constraint to DDL
    //Consider padding/truncating strings for uniform output
    //Consider printing result set in ~100 tuple blocks each headed with column labels

    static final String JDBC_DRIVER = "org.apache.derby.jdbc.ClientDriver";
    static final String DB_URL = "jdbc:derby://localhost:1527/<yourdatabasehere>";

    //  Database credentials
    static final String USER = "<user>";
    static final String PASS = "<passwd>";
    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement temp = null;

        try {
            //STEP 2: Register JDBC driver
            Class.forName(JDBC_DRIVER);
        
             //STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
                
            //STEP 4:
            menu(conn, stmt, temp);
        } catch(SQLException se) {  //Handle errors for JDBC
            se.printStackTrace();
        } catch(Exception e) {      //Handle errors for Class.forName
            e.printStackTrace();
        } finally {                 //Close resources
            try {
                if(stmt!=null)
                    stmt.close();
            } catch(SQLException se2) { // nothing we can do
                se2.printStackTrace();
            }
            try {
                if(temp!=null)
                    temp.close();
            } catch(SQLException se2) {
                se2.printStackTrace();
            }
            try {
                if(conn!=null)
                    conn.close();
            } catch(SQLException se) {
                se.printStackTrace();
            }   //end finally try
        }   //end try
    
        System.out.println("Excecution finished. Program terminating.");
    }   //end main

    public static void menu(Connection conn, Statement stmt, PreparedStatement temp) throws SQLException, Exception{
        
        int menuOption = 0;
        System.out.println(
            "Menu:\n"
            + "Please select an option:\n"
            + "1. List all writing groups.\n"
            + "2. List all the data for a group specified by the user.\n"
            + "3. List all publishers.\n"
            + "4. List all the data for a pubisher specified by the user.\n"
            + "5. List all book titles.\n"
            + "6. List all the data for a single book specified by the user.\n"
            + "7. Insert a new book.\n"
            + "8. Insert a new publisher and update all books "
            + "published by one publisher to be published by the new pubisher.\n"
            + "9. Remove a single book specified by the user.\n"
            + "10. Exit."
        );
        
        menuOption = getUserInt(1, 10, "Please select an option:");
        
        //System.out.println("You chose option: " + menuOption);
        //waitMiliseconds(1000);
        switch (menuOption) {
            case 1:
                listWritingGroups(conn, stmt);
                waitMiliseconds(1500);
                menu(conn, stmt, temp);
                break;
            case 2:
                try {
                    listGroupData(conn, stmt, temp);
                    waitMiliseconds(1500);
                    menu(conn, stmt, temp);
                } catch (Exception e) {
                   //e.printStackTrace();
                    menu(conn, stmt, temp);
                }
                break;
            case 3:
                listPublishers(conn, stmt);
                waitMiliseconds(1500);
                menu(conn, stmt, temp);
                break;
            case 4:
                try {
                    listPublisherData(conn, stmt, temp, "");
                    waitMiliseconds(1500);
                    menu(conn, stmt, temp);
                } catch (Exception e) {
                   //e.printStackTrace();
                    menu(conn, stmt, temp);
                }
                break;
            case 5:
                listBooks(conn, stmt);
                waitMiliseconds(1500);
                menu(conn, stmt, temp);
                break;
            case 6:
                try {
                    listBookData(conn, stmt, temp);
                    waitMiliseconds(1500);
                    menu(conn, stmt, temp);
                } catch (Exception e) {
                   //e.printStackTrace();
                    menu(conn, stmt, temp);
                }
                break;
            case 7:
                try {
                    insertBook(conn, stmt, temp);
                    waitMiliseconds(1500);
                    menu(conn, stmt, temp);
                } catch (Exception e) {
                   //e.printStackTrace();
                    menu(conn, stmt, temp);
                }
                break;
            case 8:
                try{
                    insertUpdatePublisher(conn, stmt, temp);
                    waitMiliseconds(1500);
                    menu(conn, stmt, temp);
                } catch (Exception e) {
                   //e.printStackTrace();
                    menu(conn, stmt, temp);
                }
                break;
            case 9:
                try{
                    removeBook(conn, stmt, temp);
                    waitMiliseconds(1500);
                    menu(conn, stmt, temp);
                } catch (Exception e) {
                   //e.printStackTrace();
                    menu(conn, stmt, temp);
                }
                break;
            case 10:
                System.out.println("Exiting Menu.");
                break;
            default:
                System.out.println("Invalid option.");
                waitMiliseconds(1500);
                menu(conn, stmt, temp);
        }
        
    } //END MENU

    //OPTION 1
    public static void listWritingGroups(Connection conn, Statement stmt) throws SQLException {
        stmt = conn.createStatement();
        String query = "SELECT * FROM WritingGroups ORDER BY GroupName";
        ResultSet rs = stmt.executeQuery(query);
        printResultSet(rs);
        rs.close();
    }

    //OPTION 2
    public static void listGroupData(Connection conn, Statement stmt, PreparedStatement temp) throws SQLException, Exception {
        Scanner input = new Scanner(System.in);
        System.out.println("Please enter a group name: ");
        String groupName = input.nextLine();
        String query = "SELECT * FROM WritingGroups "
                    + "NATURAL JOIN Books " // on (WritingGroups.GroupName = Books.GroupName) "
                    + "NATURAL JOIN Publishers "// ON (Publishers.PublisherName = Books.PublisherName) "
                    + "WHERE WritingGroups.GroupName = ?\n";
        temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
        temp.clearParameters();
        temp.setString(1, groupName);
        ResultSet rs = temp.executeQuery();

        if ( !rs.next() ) { //empty result set. No books found by WRITING GROUP
            if ( !checkWritingGroupExists(conn, stmt, temp, groupName) ) { // WRITING GROUP doesn't exist
                System.out.println("Writing Group not found."); 
                offerReturnToMenu();
                listGroupData(conn, stmt, temp);
            } else { // WRITING GROUP does exist
                System.out.println("Writing Group has no books.");
                printWritingGroupInfo(conn, stmt, temp, groupName);
            }
        } else { //publisher has books
            rs.previous();
            printResultSet(rs);
        }
        rs.close();
    }

    public static void printWritingGroupInfo(Connection conn, Statement stmt, PreparedStatement temp, String writingGroupName) throws SQLException {
        String query = "SELECT * " 
        + "FROM WritingGroups "
        + "WHERE GroupName = ?\n";
        temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
        temp.clearParameters();
        temp.setString(1, writingGroupName);
        ResultSet rs = temp.executeQuery();
        printResultSet(rs);
        rs.close();
    }

    //OPTION 3
    public static void listPublishers(Connection conn, Statement stmt) throws SQLException {
        stmt = conn.createStatement();
        String query = "SELECT * FROM Publishers ORDER BY PublisherName";
        ResultSet rs = stmt.executeQuery(query);
        printResultSet(rs);
        rs.close();
    }

    //OPTION 4
    public static void listPublisherData(Connection conn, Statement stmt, PreparedStatement temp, String publisherName) throws SQLException, Exception {
        Scanner input = new Scanner(System.in);
        if (publisherName.equals("")) {
            System.out.println("Please enter a publisher name: ");
            publisherName = input.nextLine();
        }
        String query = "SELECT Publishers.PublisherName, PublisherAddress, PublisherPhone, "
                    + "PublisherEmail, WritingGroups.GroupName, HeadWriter, YearFormed, Subject, "
                    + "BookTitle, YearPublished, NumberPages " 
                    + "FROM Publishers "
                    + "NATURAL JOIN Books "// on (Publishers.PublisherName = Books.PublisherName) "
                    + "NATURAL JOIN WritingGroups "// on (Publishers.PublisherName = Books.PublisherName) "
                    + "WHERE Publishers.PublisherName = ?\n";
        temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
        temp.clearParameters();
        temp.setString(1, publisherName);
        ResultSet rs = temp.executeQuery();
        
        if ( !rs.next() ) { //empty result set. No books found by publisher
            if ( !checkPublisherExists(conn, stmt, temp, publisherName) ) { // publisher doesn't exist
                System.out.println("Publisher not found.");
                offerReturnToMenu();
                listPublisherData(conn, stmt, temp, "");
            } else { // publisher does exist
                System.out.println( publisherName + " has no books. Showing contact info: ");
                printPublisherInfo(conn, stmt, temp, publisherName);
            }
        } else { //publisher has books
            rs.previous();
            printResultSet(rs);
        }
        rs.close();
    }

    public static void printPublisherInfo(Connection conn, Statement stmt, PreparedStatement temp, String publisherName) throws SQLException {
        String query = "SELECT * " 
        + "FROM Publishers "
        + "WHERE Publishers.PublisherName = ?\n";
        temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
        temp.clearParameters();
        temp.setString(1, publisherName);
        ResultSet rs = temp.executeQuery();
        printResultSet(rs);
        rs.close();
    }

    //OPTION 5
    public static void listBooks(Connection conn, Statement stmt) throws SQLException {
        stmt = conn.createStatement();
        String query = "SELECT BookTitle, GroupName, PublisherName, YearPublished, NumberPages FROM Books ORDER BY BookTitle, GroupName";
        ResultSet rs = stmt.executeQuery(query);
        printResultSet(rs);
        rs.close();
    }

    //OPTION 6
    public static void listBookData(Connection conn, Statement stmt, PreparedStatement temp) throws SQLException, Exception {
        Scanner input = new Scanner(System.in);
        System.out.println("Please enter a book title: ");
        String bookTitle = input.nextLine();
        System.out.println("Please enter a publisher or writing group name: ");
        String otherName = input.nextLine();

        //VALIDATE USER INPUT
        if ( !checkPublisherExists(conn, stmt, temp, otherName) && !checkWritingGroupExists(conn, stmt, temp, otherName)) {
            System.out.println("Bad publisher / group name.");
            offerReturnToMenu();
            listBookData(conn, stmt, temp);
        } else {
            String tempValue;
            //START SQL QUERY. COMPLETE THE QUERY IN IF-ELSE BLOCK.
            String query = "SELECT "
            + "BookTitle, YearPublished, NumberPages, " 
            + "Publishers.PublisherName, PublisherAddress, PublisherPhone, "
            + "PublisherEmail, WritingGroups.GroupName, HeadWriter, YearFormed, Subject "
            + "FROM WritingGroups "
            + "LEFT OUTER JOIN Books on (WritingGroups.GroupName = Books.GroupName) "
            + "LEFT OUTER JOIN Publishers ON (Publishers.PublisherName = Books.PublisherName) "
            + "WHERE Books.BookTitle = ? ";
            if ( checkPublisherExists(conn, stmt, temp, otherName) ) {    //GOOD PUBLISHER
                tempValue = otherName;
                query += "AND Books.PublisherName = ?\n";
            } else {    //GOOD WRITING GROUP
                tempValue = otherName;
                query += "AND Books.GroupName = ?\n";
            } 
            temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
            temp.clearParameters();
            temp.setString(1, bookTitle);
            temp.setString(2, tempValue);
            ResultSet rs = temp.executeQuery();
            
            if ( !rs.next() ) { //RESULT SET EMPTY BOOK NOT FOUND
                System.out.println("Book not found.");
                offerReturnToMenu();
                listBookData(conn, stmt, temp);
            } else {
                rs.previous();
                printResultSet(rs);
            }
            rs.close();
        }
    }

    //OPTION 7
    public static void insertBook(Connection conn, Statement stmt, PreparedStatement temp) throws SQLException, Exception {
        String[] userBookData = new String[5]; //0: groupname, 1: booktitle, 2: publishername, 3: yearpublished, 4: numberpages
        userBookData = getUserBookData(conn, stmt, temp, userBookData);
        String query = "INSERT INTO Books (GroupName, BookTitle, PublisherName,"
                        + "YearPublished, NumberPages) VALUES(?, ?, ?, ?, ?)";
        temp = conn.prepareStatement(query);
        temp.clearParameters();
        temp.setString(1, userBookData[0]);
        temp.setString(2, userBookData[1]);
        temp.setString(3, userBookData[2]);
        temp.setInt(4, Integer.parseInt(userBookData[3]));
        temp.setInt(5, Integer.parseInt(userBookData[4]));
        try {
            //int numRows = temp.executeUpdate();
            temp.executeUpdate();
            System.out.println("Success! Book has been inserted.");
        } catch (SQLException se) {
            System.out.println("Failure: Book not inserted. Error likely in input validation.");
            se.printStackTrace();
        }
    }

    public static String[] getUserBookData(Connection conn, Statement stmt, PreparedStatement temp, String[] userBookData) throws SQLException, Exception {
        //String[] userBookData = new String[5]; //0: groupname, 1: booktitle, 2: publishername, 3: yearpublished, 4: numberpages
        userBookData[0] = getUserWritingGroup(conn, stmt, temp);
        userBookData[2] = getUserPublisher(conn, stmt, temp, false);
        userBookData[3] = String.valueOf(getUserInt(1, 10000, "Enter number of pages:"));
        userBookData[4] = String.valueOf(getUserInt(0, 2020, "Enter year published:"));
        userBookData[1] = getUserBookTitle(conn, stmt, temp, userBookData, true);
        return userBookData;
    }

    public static String getUserBookTitle(Connection conn, Statement stmt, PreparedStatement temp, String[] userBookData, boolean isNew) throws SQLException, Exception {
        Scanner input = new Scanner(System.in);
        if ( !isNew ) { //DATA SHOULD BE IN THE TABLE ALREADY (NOT NEW)
            System.out.println("Enter a book title: ");
            String bookTitle = input.nextLine();
            if ( checkBookExists(conn, stmt, temp, bookTitle) ) {
                return bookTitle;
            } else {
                offerReturnToMenu();
                return getUserBookTitle(conn, stmt, temp, userBookData, isNew);
            }
        } else { //DATA SHOULD NOT BE IN THE TABLE YET (NEW)
            System.out.println("Enter a NEW book title: ");
            String bookTitle = input.nextLine();
            if ( !bookViolatesUniqnessConstraint(conn, stmt, temp, userBookData, bookTitle) ) {
                return bookTitle;
            } else {
                offerReturnToMenu();
                return getUserBookTitle(conn, stmt, temp, userBookData, isNew);
            }
        }
    }

    public static boolean bookViolatesUniqnessConstraint(Connection conn, Statement stmt, PreparedStatement temp, String[] userBookData, String bookTitle) throws SQLException, Exception {
        if ( !checkBookExists(conn, stmt, temp, bookTitle) ) { //new book title
            return false;
        } else if ( checkBookAndPublisherExists(conn, stmt, temp, userBookData, bookTitle) ) { //violation of uniquness constraint
            System.out.println( "This combination violates uniquness constraint on book table.\n" 
                                + "Unique Book Title and Publisher Name required." );
            return true;
        } else if ( checkBookAndWritingGroupExists(conn, stmt, temp, userBookData, bookTitle) ) { //violation of uniquness constraint
            System.out.println( "This combination violates uniquness constraint on book table.\n" 
                                + "Unique Book Title and Writing Group Name required." );
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkBookAndPublisherExists(Connection conn, Statement stmt, PreparedStatement temp, String[] userBookData, String bookTitle) throws SQLException { //violation of uniquness constraint
        String query = "SELECT * "
        + "FROM Books "
        + "WHERE Books.BookTitle = ? AND Books.PublisherName = ?\n";
        temp = conn.prepareStatement(query);
        temp.clearParameters();
        temp.setString(1, bookTitle);
        temp.setString(2, userBookData[2]);
        ResultSet rs = temp.executeQuery();
        if ( !rs.next() ) { //RESULT SET EMPTY. BOOK/PUBLISHER COMBO NOT FOUND
            rs.close();
            return false;
        } else {
            rs.close();
            return true;
        }
    }

    public static boolean checkBookAndWritingGroupExists(Connection conn, Statement stmt, PreparedStatement temp, String[] userBookData, String bookTitle) throws SQLException { //violation of uniquness constraint
        String query = "SELECT * "
        + "FROM Books "
        + "WHERE Books.BookTitle = ? AND Books.GroupName = ?\n";
        temp = conn.prepareStatement(query);
        temp.clearParameters();
        temp.setString(1, bookTitle);
        temp.setString(2, userBookData[0]);
        ResultSet rs = temp.executeQuery();
        if ( !rs.next() ) { //RESULT SET EMPTY. BOOK/WRITING GROUP COMBO NOT FOUND
            rs.close();
            return false;
        } else {
            rs.close();
            return true;
        }
    }

    public static String getUserPublisher(Connection conn, Statement stmt, PreparedStatement temp, boolean isNew) throws SQLException, Exception {
        Scanner input = new Scanner(System.in);
        String publisher;
        if ( !isNew ) { //DATA SHOULD BE IN THE TABLE ALREADY (NOT NEW)
            System.out.println("Enter a publisher name: ");
            publisher = input.nextLine();
            if ( checkPublisherExists(conn, stmt, temp, publisher) ) {
                return publisher;
            } else {
                System.out.println("Publisher: " + publisher + " not found.");
                //offer user to quit
                offerReturnToMenu();
                return getUserPublisher(conn, stmt, temp, isNew);
            }
        } else { //DATA SHOULD NOT BE IN THE TABLE YET (NEW)
            System.out.println("Enter a NEW publisher name: ");
            publisher = input.nextLine();
            if ( !checkPublisherExists(conn, stmt, temp, publisher) ) {
                return publisher;
            } else {
                System.out.println("Publisher: " + publisher + " already exists.");
                offerReturnToMenu();
                return getUserPublisher(conn, stmt, temp, isNew);
            }
        }
    }

    public static void offerReturnToMenu() throws Exception {
        Scanner input = new Scanner(System.in);
        System.out.println("Press ENTER to continue or 'M' to abort and return to main menu: ");
        String option = " ";
        option += input.nextLine();
        try {
            if ( Character.toLowerCase(option.charAt(1)) == 'm') {
                throw new Exception();
            } else {
                return;
            }
        } catch (StringIndexOutOfBoundsException s) {
            return;
        }
    }

    public static String getUserWritingGroup(Connection conn, Statement stmt, PreparedStatement temp) throws SQLException, Exception{
        Scanner input = new Scanner(System.in);
        System.out.println("Enter a writing group name: ");
        String writingGroup = input.nextLine();
        if ( checkWritingGroupExists(conn, stmt, temp, writingGroup) ) {
            return writingGroup;
        } else {
            System.out.println("Writing group: " + writingGroup + " not found.");
            offerReturnToMenu();
            return getUserWritingGroup(conn, stmt, temp);
        }
    }

    //OPTION 8
    public static void insertUpdatePublisher(Connection conn, Statement stmt, PreparedStatement temp) throws SQLException, Exception {
        String[] userPublisherData = new String[5]; //0: oldpublishername, 1: newpublishername, 2: publisheraddress, 3: publisherphone, 4: publisheremail
        userPublisherData = getUserPublisherData(conn, stmt, temp, userPublisherData);
        insertPublisher(conn, stmt, temp, userPublisherData);
        updatePublisher(conn, stmt, temp, userPublisherData);
        System.out.println("Printing New Publisher Data and Books (if any): ");
        listPublisherData(conn, stmt, temp, userPublisherData[1]);
    }

    public static void updatePublisher(Connection conn, Statement stmt, PreparedStatement temp, String[] userPublisherData) throws SQLException{
        String query = "UPDATE Books SET PublisherName = ? WHERE PublisherName = ?";
        temp = conn.prepareStatement(query);
        temp.clearParameters();
        temp.setString(1, userPublisherData[1]);
        temp.setString(2, userPublisherData[0]);
        try {
            temp.executeUpdate();
            System.out.println("Success! Book(s) updated.");
        } catch (SQLException se) {
            System.out.println("Failure: Books NOT updated.");
            se.printStackTrace();
        }
    }

    public static void insertPublisher(Connection conn, Statement stmt, PreparedStatement temp, String[] userPublisherData) throws SQLException{
        String query = "INSERT INTO Publishers (PublisherName, PublisherAddress, PublisherPhone,"
                        + "PublisherEmail) VALUES(?, ?, ?, ?)";
        temp = conn.prepareStatement(query);
        temp.clearParameters();
        temp.setString(1, userPublisherData[1]);
        temp.setString(2, userPublisherData[2]);
        temp.setString(3, userPublisherData[3]);
        temp.setString(4, userPublisherData[4]);
        try {
            temp.executeUpdate();
            System.out.println("Success! Publisher has been inserted.");
        } catch (SQLException se) {
            System.out.println("Failure: Publisher not inserted. Error likely in input validation.");
            se.printStackTrace();
        }
    }

    public static String[] getUserPublisherData( Connection conn, Statement stmt, PreparedStatement temp, String[] userPublisherData ) throws SQLException, Exception {
        System.out.print("Existing Publisher - ");
        userPublisherData[0] = getUserPublisher(conn, stmt, temp, false); //boolean notNew
        userPublisherData[1] = getUserPublisher(conn, stmt, temp, true); //boolean isNew
        userPublisherData[2] = getUserString("Enter publisher address: ");
        userPublisherData[3] = getUserString("Enter publisher phone number: ");
        userPublisherData[4] = getUserString("Enter publisher email: ");
        return userPublisherData;
    }

    //OPTION 9
    public static void removeBook(Connection conn, Statement stmt, PreparedStatement temp) throws SQLException, Exception {
        String[] userBookData = new String[3]; //0: groupname, 1: booktitle, 2: publishername
        userBookData = getUserBookKey(conn, stmt, temp, userBookData);
        if ( userBookData[0] == null && userBookData[2] == null ) { //CHECK IF THERE IS A BOOK WITH THIS KEY BEFORE REMOVING
            System.out.println("Error: Failed to get full key.");
        } else if (userBookData[0] == null) {
            //System.out.println("Use PUBLISHER.");
            if ( checkBookAndPublisherExists(conn, stmt, temp, userBookData, userBookData[1]) ) {
                removeBookDML(conn, stmt, temp, userBookData);
                listBooks(conn, stmt);
            } else {
                System.out.println("The key you entered was not found. Nothing happened.");
                offerReturnToMenu();
                removeBook(conn, stmt, temp);
            }
        } else { //userBookData[2] == null
            //System.out.println("Use WRITING GROUP.");
            if ( checkBookAndWritingGroupExists(conn, stmt, temp, userBookData, userBookData[1]) ) {
                removeBookDML(conn, stmt, temp, userBookData);
                listBooks(conn, stmt);
            } else {
                System.out.println("The key you entered was not found. Nothing happened.");
                offerReturnToMenu();
                removeBook(conn, stmt, temp);
            }
        }
    }

    public static void removeBookDML(Connection conn, Statement stmt, PreparedStatement temp, String[] userBookData) throws SQLException {
        String keyAttribute = (userBookData[0] == null) ? "PublisherName" : "GroupName";
        String query = "DELETE FROM Books WHERE BookTitle =  ? and "
                        + keyAttribute
                        + " = ?";
        temp = conn.prepareStatement(query);
        temp.clearParameters();
        temp.setString( 1, userBookData[1] );
        temp.setString( 2, (userBookData[0] == null) ? userBookData[2] : userBookData[0] );
        temp.executeUpdate();
        System.out.println("Book: [" + userBookData[1] + "] removed.");
    }

    public static String[] getUserBookKey(Connection conn, Statement stmt, PreparedStatement temp, String[] userBookData) throws SQLException, Exception {
        System.out.println("Remove a book Menu\n"
                            + "Key Selection:\n"
                            + "1. Remove by Title and Writing Group Name.\n"
                            + "2. Remove by Title and Publisher Name.");
        int option = getUserInt(1, 2, "Select an option: ");
        if (option == 1) {
            //WRITING GROUP
            userBookData[0] = getUserWritingGroup(conn, stmt, temp);
            userBookData[1] = getUserBookTitle(conn, stmt, temp, userBookData, false);
        } else if (option == 2) {
            //PUBLISHER
            userBookData[2] = getUserPublisher(conn, stmt, temp, false);
            userBookData[1] = getUserBookTitle(conn, stmt, temp, userBookData, false);
        } else {
            System.out.println("Invalid option. Error bounds checking user defined option.");
            waitMiliseconds(1500);
        }
        return userBookData;
    }

    //CHECK IF BOOK EXISTS
    public static Boolean checkBookExists(Connection conn, Statement stmt, PreparedStatement temp, String bookTitle) throws SQLException, Exception {
        String query = "SELECT * FROM Books WHERE BookTitle = ?\n";
        temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
        temp.clearParameters();
        temp.setString(1, bookTitle);
        ResultSet rs = temp.executeQuery();
        if ( !rs.next() ) { //BOOKTITLE NOT FOUND
            System.out.println("There is no book with the title: " + bookTitle + ".");
            rs.close();
            
            return false;
        } else { //BOOKTITLE FOUND
            //System.out.println(bookTitle + " found.");
            rs.close();
            return true;
        }
    }

    //DETERMINE IF PUBLISHER IS IN PUBLISHER TABLE
    public static Boolean checkPublisherExists(Connection conn, Statement stmt, PreparedStatement temp, String otherName) throws SQLException {
        String query = "SELECT * FROM Publishers WHERE Publishers.PublisherName = ?\n";
        temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
        temp.clearParameters();
        temp.setString(1, otherName);
        ResultSet rs = temp.executeQuery();
        if ( !rs.next() ) { //PUBLISHER NOT FOUND
            //System.out.println("String is NOT a valid Publisher Name.");
            rs.close();
            return false;
        } else {
            //System.out.println("String is a valid Publisher Name.");
            rs.close();
            return true;
        }
    }

    //DETERMINE IF WRITING GROUP IS IN WRITING GROUP TABLE
    public static Boolean checkWritingGroupExists(Connection conn, Statement stmt, PreparedStatement temp, String otherName) throws SQLException {
        String query = "SELECT * FROM WritingGroups WHERE WritingGroups.GroupName = ?\n";
        temp = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);        
        temp.clearParameters();
        temp.setString(1, otherName);
        ResultSet rs = temp.executeQuery();
        if ( !rs.next() ) { //WRITING GROUP NOT FOUND
            //System.out.println("String is NOT a valid Writing Group Name.");
            rs.close();
            return false;
        } else {
            //System.out.println("String is a valid Writing Group Name.");
            rs.close();
            return true;
        }
    }

    //PRINT RESULT SET
    public static void printResultSet (ResultSet rs) throws SQLException {
        int i = 1;
        while (rs.next()) {
            System.out.print(i + ": ");
            printTuple(rs);
            System.out.println();
            i++;
        }
    }

    //HELPER FUNCTION OF PRINT RESULT SET
    public static void printTuple (ResultSet rs) throws SQLException {
        for ( int i = 1; i <= rs.getMetaData().getColumnCount(); i++ ){
            System.out.print("[" + rs.getMetaData().getColumnName(i) + "]: " + rs.getString(rs.getMetaData().getColumnName(i)) + " " );
        }
    }

    //PAUSE EXECUTION FOR VARIABLE NUMBER OF MILISECONDS
    //PURPOSE: ALLOWS USER TO SEE OUTPUT FOR A BRIEF PERIOD BEFORE MENU POPS UP AGAIN
    public static void waitMiliseconds(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException i) {
            i.printStackTrace();
        }
    }

    // PROMPT USER TO ENTER A NUMBER WITHIN A RANGE. LOOPS UNTIL USER GETS IT RIGHT (ENTERS ANY INT).
    public static int getUserInt(int lowBound, int upBound, String message) {
        System.out.println( message + " ( " + lowBound + " - " + upBound + " ).");
        Scanner input = new Scanner(System.in);
        String line = input.nextLine();
        int choice = 0;
        try {
            choice = Integer.parseInt(line);
            if (choice >= lowBound && choice <= upBound) { //choice is within acceptable range
                return choice;
            } else {
                System.out.println("Number: " + choice + " outside of range. Try again.");
                choice = getUserInt(lowBound, upBound, message);
            }
        } catch (NumberFormatException e) {
            System.out.println("You've caused a number format exception. Try again.");
            choice = getUserInt(lowBound, upBound, message);
        }
        return choice;
    }

    public static String getUserString(String message) throws SQLException{
        Scanner input = new Scanner(System.in);
        System.out.println( message );
        String str = input.nextLine();
        return str;
    }

} //END CLASS
