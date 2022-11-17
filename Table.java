import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner; // Import the Scanner class to read text files

public class Table {
    List<Row> rows = null;
    String[] columns = null;
    List<BTree<String, List<Row>>> trees;

    Table(List<Row> rows, String[] columns){
        this.rows = rows;
        this.columns = columns;
    }
    Table(String path){
        reloadFromFile(path);
    }

    void populateHeader(String header){
        columns = header.split(",");
    }

    void populateData(Scanner myReader){
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            rows.add(new Row(data));
        }
    }

    Row getRow(int index){
        return rows.get(index);
    }

    public void reloadFromFile(String path){
        rows = new LinkedList<Row>(); //resets

        try {
            File myObj = new File(path);
            Scanner myReader = new Scanner(myObj);
            if(myReader.hasNextLine()) { //This is supposed to be the header
                populateHeader(myReader.nextLine());
                populateData(myReader);
            }
            myReader.close();
            buildIndexes();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    /***
     *  This will iterate over the columns and build BTree based indexes for all columns
     */

    private void buildIndexes() {
        trees = new LinkedList<>();
        BTree<String, List<Row>> tree;
        for (int i = 0; i < columns.length; i++) {
            tree = new BTree<>();
            for (int j = 0; j < rows.size(); j++) {
                Row value = getRow(j);
                String key = value.getColumnAtIndex(i);
                try{
                    tree.get(key).add(value);
                }
                catch(Exception e){
                    List<Row> newValues = new LinkedList<>();
                    newValues.add(value);
                    tree.put(key,newValues);
                }
            }
            trees.add(tree);
        }
    }

    /***
     *This method is supposed to parse the filtering statement
     * identify which rows will be filtered
     * apply filters using btree indices
     * collect rows from each btree and find the mutual intersection of all.
     * Statement Rules: ColumnName==ColumnValue AND ColumnName==ColumnValue AND ColumnName==ColumnValue
     * Can be chained by any number of "AND"s; 0 or more
     * sample filterStatement: first_name==Roberta AND id=3
     * Return Type: A new Table which consists of Rows that pass the filter test
     */
    public Table filter(String filterStatement){
        String[] filters = filterStatement.split("AND");
        String filter, key, value;
        BTree<String, List<Row>> tree;
        List<Row> row = null, temp;
        for (int i = 0; i < filters.length; i++) {
            filter = filters[i].strip();
            key = filter.split("==")[0];
            value = filter.split("==")[1];
            for (int j = 0; j < columns.length; j++) {
                if(columns[j].equals(key)){
                    tree = trees.get(j);
                    temp = tree.get(value);
                    if(i==0){
                        row = new LinkedList<>(temp);
                    }
                    else if(row!=null) {
                        if(temp == null){
                            row = new LinkedList<>();
                            row.add(new Row(""));
                            break;
                        }
                        else {
                            row.retainAll(temp);
                        }
                    }
                    break;
                }
            }
        }
        return new Table(row, this.columns); //change
    }
    /***
     * This method projects only set of columns from the table and forms a new table including all rows but only selected columns
     * columnsList is comma separated list of columns i.e., "id,email,ip_address"
     */
    public Table project(String columnsList){
        String[] newColumns = columnsList.split(",");
        ArrayList<Integer> indexes = new ArrayList<>();
        for (String column : newColumns) {
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equals(column)) {
                    indexes.add(i);
                }
            }
        }
        List<Row> values = new LinkedList<>();
        for (Row row : this.rows) {
            if(!row.toString().equals("")){
                StringBuilder s = new StringBuilder();
                s.append(row.getColumnAtIndex(indexes.get(0)));
                for (int i = 1; i < indexes.size(); ++i) {
                    s.append(",").append(row.getColumnAtIndex(indexes.get(i)));
                }
                values.add(new Row(s.toString()));
            }
            else {
                values.add(new Row("No Match!"));
                break;
            }
        }
        return new Table(values, newColumns);
    }

    /***
     *  Print column names in the first line
     *  Print the rest of the table
     */

    public void show(){
        System.out.println(String.join(",", columns) + "\n");
        for(Row rw:rows){
            System.out.println(rw.toString() + "\n");
        }
    }

    public static void main(String[] args) {
        Table tb = new Table("src/userLogs.csv");
        //tb.show(); // should print everything
        tb.filter("id==3").project("first_name").show();  // should print Aldon

        //This is suppposed to print Jobling,sjoblingi@w3.org
        tb.filter("id==19 AND ip_address==242.40.106.103").project("last_name,email").show();

        //amathewesg@slideshare.net
        //imathewesdx@ucoz.com
        tb.filter("last_name==Mathewes").project("email").show();

        //We might test with a different csv file with same format but different column count
        //Good luck!!
    }
}
