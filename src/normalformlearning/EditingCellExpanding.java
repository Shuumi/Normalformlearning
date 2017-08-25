/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package normalformlearning;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/**
 *
 * @author Denis
 */
public class EditingCellExpanding extends EditingCell{
    
    public EditingCellExpanding(TableView table, int index) {
        super(table, index);
    }
    
    @Override
    public String getString() {
            if(NormalformLearning.getEntry().isColumnCreated(index)){
                createdTable = true;
            }
            if(getItem() != null && !getItem().toString().equals("") && !createdTable){
                createdTable = true;
                if(index<19){
                    table.getColumns().add(newEditingCell());
                }
            }
            return getItem() == null ? "" : getItem().toString();
        }
    
    public TableColumn newEditingCell() {
            TableColumn Column = new TableColumn("");
            Column.setEditable(true);
            Callback<TableColumn, TableCell> cellFactory =
                new Callback<TableColumn, TableCell>() {
                    public TableCell call(TableColumn p){
                        EditingCellExpanding ec = new EditingCellExpanding(table, index+1);
                        NormalformLearning.getEntry().put(index+1);
                        return ec;
                    }
                };
            Column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<NormalformLearning.Entry, String>, ObservableValue<String>>(){
                @Override
                public ObservableValue<String> call(TableColumn.CellDataFeatures<NormalformLearning.Entry, String> p) {
                    return new SimpleStringProperty(p.getValue().getText(index+1));
                }
            });
            Column.setCellFactory(cellFactory);
            Column.setOnEditCommit(
                    new EventHandler<TableColumn.CellEditEvent<NormalformLearning.Entry, String>>() {
                @Override
                public void handle(TableColumn.CellEditEvent<NormalformLearning.Entry, String> t) {
                    ((NormalformLearning.Entry) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setText(t.getNewValue(), index+1);
                }
            }
            );
            return Column;
        }
}
