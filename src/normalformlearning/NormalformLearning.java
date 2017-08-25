package normalformlearning;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Hashtable;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

/**
 *
 * @author Denis
 */
public class NormalformLearning extends Application {
    private static final TableView<Entry> table= new TableView();
    private static final HashSet<Entry.Entrypart> primaryKey = new HashSet();
    private static final Hashtable<Integer, Dependency> dependencies = new Hashtable(); 
    private static HashSet<HashSet<Entry.Entrypart>> primarykeycandidates = null;
    private static HashSet<HashSet<Entry.Entrypart>> subsets = null;   
    private static final Entry entry = new Entry();
    private static final ObservableList<Entry> data = FXCollections.observableArrayList(entry);
    private static StackPane root;
    private static int dependencyIndex = 0;
    private static Entry.Entrypart draggedEntrypart;
    private static Connection conn = null;
    private static Circle outputlight = null;
    private static Label outputText = null;
    
    @Override
    public void start(Stage primaryStage) {
        //Creating a table with the first column
        // <editor-fold defaultstate="collapsed">  
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        entry.put(1);
        createEditingCell(1, true);
        table.setItems(data);
        // </editor-fold>
        //Implementing dragAndDrop into the table
        //<editor-fold defaultstate="collapsed">
        table.setRowFactory(tv -> {
            TableRow<Entry> row = new TableRow<>();
            
            row.setOnDragDetected(new EventHandler <MouseEvent>() {
                public void handle(MouseEvent event) {
                    Dragboard db = row.startDragAndDrop(TransferMode.ANY);

                    ClipboardContent content = new ClipboardContent();
                    try{
                        if(event.getTarget() instanceof EditingCell){
                            content.putString(row.getItem().getText(((EditingCell) event.getTarget()).index));
                            draggedEntrypart = entry.getEntrypart(((EditingCell) event.getTarget()).index);
                            db.setContent(content);
                        }
                    }catch(NullPointerException ignored){
                    }

                    event.consume();
                }
            });
            return row;
        });
        //</editor-fold>
        //Yellow box and Buttons
        //<editor-fold defaultstate="collapsed">
        Box box = new Box(1024, 758, 0);
        box.setTranslateY(54);
        box.setMaterial(new PhongMaterial(Color.LIGHTYELLOW));
        
        Button evaluate = new Button("evaluate");
        evaluate.setTranslateX(512-50);
        evaluate.setTranslateY(379-20);
        evaluate.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                outputlight.setVisible(true);
                Platform.runLater(()->outputlight.setFill(Color.RED));
                Platform.runLater(()->outputText.setTextFill(Color.WHITE));
                outputText.setVisible(true);
                outputText.setText("1NF");
                evaluade3NF();
            }
        });
        
        Button loadSQL = new Button("Lade Aufgabe");
        loadSQL.setTranslateX(512-50-100);
        loadSQL.setTranslateY(379-20);
        loadSQL.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                outputlight.setVisible(false);
                outputText.setVisible(false);
                sql();
                createDepLine();
            }
        });
        
        Button reset = new Button("Reset");
        reset.setTranslateX(512-50-200);
        reset.setTranslateY(379-20);
        reset.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                outputlight.setVisible(false);
                outputText.setVisible(false);
                table.getColumns().clear();
                for(int i = 0; i<dependencies.size(); i++){
                    root.getChildren().remove(dependencies.get(i).key);
                    root.getChildren().remove(dependencies.get(i).dependent);
                }
                dependencies.clear();
                dependencyIndex=0;
                entry.put(1);
                createEditingCell(1, true);
                createDepLine();
            }
        });
        //</editor-fold>
        //Output lights and text
        //<editor-fold defaultstate="collapsed">
        outputlight = new Circle(50);
        outputlight.setTranslateX(400);
        outputlight.setTranslateY(-250);
        outputlight.setVisible(false);
        
        outputText = new Label("");
        outputText.setTextAlignment(TextAlignment.CENTER);
        outputText.setTranslateX(400);
        outputText.setTranslateY(-250);
        outputText.setVisible(false);
        
        //</editor-fold>
        
        StackPane root = new StackPane(table,evaluate,loadSQL,outputlight,outputText,reset);
        this.root = root;
        createDepLine();
        Scene scene = new Scene(root, 1024, 758);
        
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                try{
                    if(conn!=null){
                        conn.close();
                    }
                }catch(SQLException e){
                    System.err.println(e.getMessage());
                }
            }
        });
        primaryStage.setTitle("");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        Pane header = (Pane) table.lookup("TableHeaderRow");
        header.setVisible(false);
        table.setLayoutY(-header.getHeight());
        table.autosize();
    }
    
    public void sql(){
        String sql = "SELECT COUNT(ExcersiseID) from Excersise";
        try{
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery(sql);
        sql(1+(int)(Math.random()*rs.getInt(1)));
        }catch(SQLException e){
            System.err.println(e.getMessage());
        }
        
    }
    
    public void sql(int excersiseNo){
        String sql;
        table.getColumns().clear();
        for(int i = 0; i<dependencies.size(); i++){
            root.getChildren().remove(dependencies.get(i).key);
            root.getChildren().remove(dependencies.get(i).dependent);
        }
        dependencies.clear();
        dependencyIndex=0;
        try{
        Statement statement = conn.createStatement();
        Statement statement2 = conn.createStatement();
        sql = "SELECT * FROM Excersise NATURAL JOIN Entry WHERE ExcersiseID = " + excersiseNo;
        ResultSet rs = statement.executeQuery(sql);
        int i = 1;
        while(rs.next()){
            entry.put(i, rs.getString("Name"));
            createEditingCell(i++,false);
            System.out.println(rs.getString("Name"));
        }
        entry.put(i);
        createEditingCell(i,true);
        
        sql = "SELECT * FROM Excersise NATURAL JOIN Dependency NATURAL JOIN asDependent NATURAL JOIN Entry WHERE ExcersiseID = " + excersiseNo + " order by DepID";
        rs = statement.executeQuery(sql);
        String sql2 = "SELECT * FROM Excersise NATURAL JOIN Dependency NATURAL JOIN asKey NATURAL JOIN Entry WHERE ExcersiseID = " + excersiseNo + " order by DepID";
        ResultSet rs2 = statement2.executeQuery(sql2);
        i = -1;
        Dependency current = null;
        while(rs.next()){
            if(i!=rs.getInt("DepID")){
                i=rs.getInt("DepID");
                current = createDepLine();
                current.createdDepLine = true;
            }
            if(current.dependent.getText().equals("")){
                current.dependent.setText(rs.getString("Name"));
            }
            else{
                current.dependent.setText(current.dependent.getText()+ ", " + rs.getString("Name"));
            }
            for(int j = 1; j<entry.getEntryparts().size(); j++){
                if(entry.getEntrypart(j).getText().equals(rs.getString("Name"))){
                    current.addDependent(entry.getEntrypart(j));
                }
            }
            if(current.key.getText().equals("")){
                while(rs2.next()){
                    if(rs.getInt("DepID") == rs2.getInt("DepID")){
                        if(current.key.getText().equals("")){
                            current.key.setText(rs2.getString("Name"));
                        }
                        else{
                        current.key.setText(current.key.getText()+ ", " + rs2.getString("Name"));
                        }
                        for(int j = 1; j<entry.getEntryparts().size(); j++){
                            if(entry.getEntrypart(j).getText().equals(rs2.getString("Name"))){
                                current.addKey(entry.getEntrypart(j));
                            }
                        }
                    }
                }
            }
            rs2 = statement2.executeQuery(sql2);
        }
        
        System.out.println("done");
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }
    
    public void evaluade3NF(){
        boolean is3NF = true;
        if(evaluade2NF()){
            Hashtable<Integer,Dependency> depContainingPKC = getDependenciesContainingPKC();
            HashSet<Entry.Entrypart> allAttributes = allAttributesHS();
            for(HashSet<Entry.Entrypart> pkc: primarykeycandidates){
                HashSet<Entry.Entrypart> pkcHull = createHull(pkc, depContainingPKC);
                if(!pkcHull.containsAll(allAttributes)){
                    int j = 0;
                    System.out.print("3NF liegt nicht vor, weil manche Attribute von {");
                    for(Entry.Entrypart e: pkc){
                        System.out.print(e.getText());
                        if(++j!=pkc.size()){
                            System.out.print(",");
                        }
                    }
                    System.out.println("} transitiv abhängen");
                    is3NF=false;
                }
            }
        }
        else{
            is3NF = false;
        }
        if(is3NF){
            outputText.setText("1NF");
            Platform.runLater(()->outputlight.setFill(Color.GREEN));
        }
    }
    
    public void createEditingCell(int index, boolean isExpanding){
        Callback<TableColumn, TableCell> cellFactory =
                new Callback<TableColumn, TableCell>() {
                    public TableCell call(TableColumn p){
                        EditingCell ec = null;
                        if(isExpanding){
                            ec = new EditingCellExpanding(table, index);
                        }
                        else{
                            ec = new EditingCell(table, index);
                        }
                        return ec;
                    }
                };
        TableColumn firstCol = new TableColumn("FirstCol");        
        firstCol.setCellValueFactory(new Callback<CellDataFeatures<Entry, String>, ObservableValue<String>>(){
            @Override
            public ObservableValue<String> call(CellDataFeatures<Entry, String> p) {
                return new SimpleStringProperty(p.getValue().getText(index));
            }
        });
        firstCol.setEditable(true);
        firstCol.setPrefWidth(1024);
        firstCol.setCellFactory(cellFactory);
        firstCol.setOnEditCommit(
            new EventHandler<CellEditEvent<Entry, String>>() {
                @Override
                public void handle(CellEditEvent<Entry, String> t) {
                    ((Entry) t.getTableView().getItems().get(
                        t.getTablePosition().getRow())
                        ).setText(t.getNewValue(), index);
                }
             }
        );
        table.getColumns().add(firstCol);
    }
    
    public Hashtable<Integer,Dependency> getDependenciesContainingPKC(){
        Hashtable<Integer,Dependency> result = new Hashtable<>();
        for(int i = 0; i<dependencies.size(); i++){
            HashSet<Entry.Entrypart> keys = dependencies.get(i).keys;
            for(HashSet<Entry.Entrypart> pkc: primarykeycandidates){
                if(keys.containsAll(pkc)){
                    result.put(result.size(), dependencies.get(i));
                }
            }
        }
        return result;
    }
    
    public boolean evaluade2NF(){
        Hashtable<Integer,HashSet<Entry.Entrypart>> wrongs = new Hashtable<>();
        createPrimaryKeyCandidates();
        testPrimeKeyCandidates();
        boolean is2NF = false;
        HashSet<Entry.Entrypart> depending = new HashSet<>();
        for(int i = 1; i<entry.getEntryparts().size()+1; i++){
            Entry.Entrypart checked = entry.getEntrypart(i);
            for(HashSet<Entry.Entrypart> pkc: primarykeycandidates){
                if(!pkc.contains(checked)){
                    HashSet<Entry.Entrypart> hull = createHull(pkc, dependencies);
                    if(hull.contains(checked)){
                        depending.add(checked);
                    }
                    HashSet<HashSet<Entry.Entrypart>> spkc = createSubsets(pkc);
                    spkc.remove(pkc);
                    for(HashSet<Entry.Entrypart> subpkc: spkc){
                        HashSet<Entry.Entrypart> subpkchull = createHull(subpkc, dependencies);
                        if(subpkchull.contains(checked)){
                            HashSet<Entry.Entrypart> checkedHS = new HashSet<>();
                            checkedHS.add(checked);
                            wrongs.put(wrongs.size(), checkedHS);
                            wrongs.put(wrongs.size(), subpkc);
                            wrongs.put(wrongs.size(), pkc);
                            is2NF = true;
                        }
                    }
                }
            }
        }
        HashSet<Entry.Entrypart> noPrimaryKeyAttributes = allAttributesHS();
        for(HashSet<Entry.Entrypart> pkc: primarykeycandidates){
            noPrimaryKeyAttributes.removeAll(pkc);
            depending.removeAll(pkc);
        }
        for(Entry.Entrypart e : depending){
            if(!noPrimaryKeyAttributes.contains(e)){
                is2NF = true;
                break;
            }
        }
        if(is2NF){
            for(int i = 0; i<wrongs.size(); i++){
                System.out.print("2NF liegt nicht vor, weil {");
                int j = 0;
                for(Entry.Entrypart e: wrongs.get(i)){
                    j++;
                    System.out.print(e.getText());
                    if(wrongs.get(i).size()!=j){
                        System.out.print(", ");
                    }
                }
                i++;
                j=0;
                System.out.print("} hängt nur von {");
                for(Entry.Entrypart e: wrongs.get(i)){
                    j++;
                    System.out.print(e.getText());
                    if(wrongs.get(i).size()!=j){
                        System.out.print(", ");
                    }
                }
                i++;
                j=0;
                System.out.print("} ab, was eine Teilmenge von {");
                for(Entry.Entrypart e: wrongs.get(i)){
                    j++;
                    System.out.print(e.getText());
                    if(wrongs.get(i).size()!=j){
                        System.out.print(", ");
                    }
                }
                System.out.println("} ist.");
            }
            return false;
        }
        else{
            outputText.setText("2NF");
            Platform.runLater(()->outputText.setTextFill(Color.BLACK));
            Platform.runLater(()->outputlight.setFill(Color.ORANGE));
            return true;
        }
    }
    
    private void createPrimaryKeyCandidates(){
        HashSet<HashSet<Entry.Entrypart>> result = new HashSet<>();
        HashSet<Entry.Entrypart> hs = new HashSet<>();
        HashSet<Entry.Entrypart> allAttributes = new HashSet<>();
        for(int i = 1; i<entry.getEntryparts().size()+1; i++){
            if(entry.getEntrypart(i).getText().equals("")){
                break;
            }
            hs.add(entry.getEntrypart(i));
            allAttributes.add(entry.getEntrypart(i));
        }
        int maxSize = hs.size();
        subsets = createSubsets(hs);
        for(int currentSize = 1; currentSize <= maxSize; currentSize++){
            for(HashSet<Entry.Entrypart> subset: subsets){
                if(subset.size()==currentSize){
                    boolean broken = false;
                    for(HashSet<Entry.Entrypart> toCompare: result){
                        if(subset.containsAll(toCompare)){
                            broken = true;
                            break;
                        }
                    }
                    if(!broken){
                        HashSet<Entry.Entrypart> hull = createHull(subset, dependencies);
                        if(hull.containsAll(allAttributes)){
                            result.add(subset);
                        }
                    }
                }
            }
        }
        primarykeycandidates = result;
    }
    
    public void testPrimeKeyCandidates(){
        for(HashSet<Entry.Entrypart> one : primarykeycandidates){
            for(Entry.Entrypart two: one){
                System.out.print(two.getText());
            }
            System.out.println();
        }
    }
    
    private HashSet<Entry.Entrypart> allAttributesHS(){
        HashSet<Entry.Entrypart> result = new HashSet<>();
        for(int i = 1; i<entry.getEntryparts().size()+1; i++){
            if(entry.getEntrypart(i).getText().equals("")){
                break;
            }
            result.add(entry.getEntrypart(i));
        }
        return result;
    }
    
    private HashSet<HashSet<Entry.Entrypart>> createSubsets(HashSet<Entry.Entrypart> hs){
        HashSet<Entry.Entrypart> input = new HashSet<>();
        input.addAll(hs);
        return createSubsetsRecursion(input);
        
    }
    
    private HashSet<HashSet<Entry.Entrypart>> createSubsetsRecursion(HashSet<Entry.Entrypart> hs){
        HashSet<HashSet<Entry.Entrypart>> result = new HashSet<>();
        if(hs.size()==1){
            result.add(hs);
            return result;
        }
        
        Entry.Entrypart ep = null;
        for(Entry.Entrypart e: hs){
            ep = e;
            hs.remove(e);
            break;
        }
        HashSet<Entry.Entrypart> epToAdd = new HashSet<>();
        epToAdd.add(ep);
        result.add(epToAdd);
        HashSet<HashSet<Entry.Entrypart>> a = createSubsets(hs);
         for(HashSet<Entry.Entrypart> b: a){
            result.add(b);
            HashSet<Entry.Entrypart> res = new HashSet<>();
            for(Entry.Entrypart e : b){
                res.add(e);
            }
            res.add(ep);
            result.add(res);
        }
         return result;
    }
    
//    public void testSubsets(int size){
//        System.out.println(subsets.size() == Math.pow(2, size)-1);
//    }

    public HashSet<Entry.Entrypart> createHull(Entry.Entrypart e, Hashtable<Integer,Dependency> dependencies){
        HashSet<Entry.Entrypart> hs = new HashSet<Entry.Entrypart>();
        hs.add(e);
        return createHull(hs, dependencies);
    }

    public HashSet<Entry.Entrypart> createHull(HashSet<Entry.Entrypart> hs, Hashtable<Integer,Dependency> dependencies){
        boolean restart = true;
        HashSet<Entry.Entrypart> hull = new HashSet<>();
        HashSet<Integer> added = new HashSet();
        for(Entry.Entrypart pk : hs){
            hull.add(pk);
        }
        
        while(restart){
            restart = false;
            for(int i = 0; i < dependencies.size(); i++){
                if(!added.contains(i)){
                    HashSet<Entry.Entrypart> keys = dependencies.get(i).getKeys();
                    HashSet<Entry.Entrypart> dependent = dependencies.get(i).getDependents();
                    boolean broke = false;
                    for(Entry.Entrypart p : keys){
                        if(!hull.contains(p)){
                            broke = true;
                            break;
                        }
                    }
                    if(!broke){
                        for(Entry.Entrypart d: dependent){
                            added.add(i);
                            hull.add(d);
                            restart = true;
                        }
                    }
                }
            }
        }
        return hull;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        conn = new SQLiteConnection().getConnection();
        launch(args);
        
    }
    
//    public static HashSet<EditingCell> getEditingCells(){
//        return editingCells;
//    }
    
    public static Entry getEntry(){
        return entry;
    }
    
    public static void togglePrim(int index){
        entry.togglePrim(index);
    }
    
    public static boolean isPrim(int index){
        return entry.isPrim(index);
    }
    
    private static Dependency createDepLine(){
        //Create two TextFields to show the dependencies. The right one (input2) is the depending one.
        TextField input1 = new TextField();
        TextField input2 = new TextField();
        input1.setTranslateX(-151);
        input2.setTranslateX(151);
        input1.setTranslateY(-300+dependencyIndex*27);
        input2.setTranslateY(-300+dependencyIndex*27);
        //double tableWidth = table.getColumns().get(1).getWidth();
        input1.setMaxWidth(300);
        input2.setMaxWidth(300);
        input1.setEditable(false);
        input2.setEditable(false);
        root.getChildren().addAll(input1,input2);
        
        //create the Drag and Drop function
        
        Dependency d = new Dependency(input1, input2);
        dependencyIndex++;
        return d;
    }
    
    public static class Entry {
        private final Hashtable<Integer, Entrypart> texts = new Hashtable();
        
        
        public Entry(){
        }
        
        public void put(int index){
            texts.put(index, new Entrypart());
        }
        
        public void put(int index, String text){
            texts.put(index, new Entrypart(text));
        }
        
        public String getText(int index){
            return texts.get(index).getText();
        }
        
        public void setText(String text, int index){
            texts.get(index).setText(text);
            if(text.equals("s")){
                
            }
                
        }
        
        public boolean isColumnCreated(int index){
            return texts.get(index).isColumnCreated();
        }
        
        public void setColumnCreated(int index){
            texts.get(index).setColumnCreated();
        }
        
        public void test1AllTexts(){
            for(Entrypart ep: texts.values()){
                System.out.println(ep.getText());
            }
        }
        
        public Entrypart getEntrypart(int index){
            return texts.get(index);
        }
        
        public boolean isPrim(int index){
            return texts.get(index).isPrim;
        }
        
        public void togglePrim(int index){
            texts.get(index).togglePrim();
        }
        
        public Hashtable<Integer, Entrypart> getEntryparts(){
            return texts;
        }
        
        public static class Entrypart{
            private String text;
            private boolean createdColumn;
            private boolean isPrim;
            public Entrypart(){
                this("");
            }
            
            public Entrypart(String text){
                this.text = text;
                createdColumn = false;  
            }
            
            public String getText(){
                return text;
            }
            public void setText(String newText){
                text = newText;
            }
            public boolean isColumnCreated(){
                return createdColumn;
            }
            public void setColumnCreated(){
                createdColumn = true;
            }
            
            public boolean isPrim(){
                return isPrim;
            }
            
            public void togglePrim(){
                isPrim = !isPrim;
                if(isPrim){
                    primaryKey.add(this);
                }
                else{
                    primaryKey.remove(this);
                }
            }
        }
    }
    
    public static class Dependency{
        private TextField key, dependent;
        private HashSet<Entry.Entrypart> keys = new HashSet<>();
        private HashSet<Entry.Entrypart> dependents = new HashSet<>();
        private boolean createdDepLine = false;
        public Dependency(TextField key, TextField dependent){
            this.key=key;
            this.dependent=dependent;
            setupDrop(key, keys);
            setupDrop(dependent,dependents);
            dependencies.put(dependencies.size(), this);
        }
        
        private HashSet<Entry.Entrypart> getKeys(){
            return keys;
        }
        
        private HashSet<Entry.Entrypart> getDependents(){
            return dependents;
        }
        
        public void addKey(Entry.Entrypart e){
            keys.add(e);
        }
        
        public void addDependent(Entry.Entrypart e){
            dependents.add(e);
        }
        
        private void setupDrop(TextField tf, HashSet resp){
            tf.setOnDragOver(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent event) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    event.consume();
                }
            });
            
            tf.setOnDragDropped(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent event) {
                    Dragboard db = event.getDragboard();
                    if(!db.getString().equals("") && !resp.contains(draggedEntrypart)){
                        if(tf.getText().equals("")){
                            tf.setText(db.getString());
                        }
                        else{
                            tf.setText(tf.getText()+", "+ db.getString());
                        }
                        resp.add(draggedEntrypart);
                        if(!dependents.isEmpty() && !keys.isEmpty() && !createdDepLine){
                        createDepLine();
                        createdDepLine=true;
                        }
                    }
                }
            });
        }
    }
}