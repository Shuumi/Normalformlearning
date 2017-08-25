/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package normalformlearning;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.util.Callback;

/**
 *
 * @author Denis
 */
class EditingCell extends TableCell<NormalformLearning.Entry, String> {
        private TextField textField;
        public final TableView<NormalformLearning.Entry> table;
        public int index;
        public boolean createdTable = false;
        private boolean isCtrlHeld = false;
        
 
        public EditingCell(TableView table, int index) {
            this.table = table;
            this.index = index;
            /*for(EditingCell e : NormalformLearning.getEditingCells()){
                if(e.index == this.index){
                    createdTable = true;
                }
            }*/
        }
 
        @Override
        public void startEdit() {
            if (!isEmpty()) {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                textField.selectAll();
            }
        }
 
        @Override
        public void cancelEdit() {
            super.cancelEdit();
 
            setText((String) getItem());
            setGraphic(null);
        }
 
        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
 
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());        
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }
 
        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()* 2);
            textField.focusedProperty().addListener(new ChangeListener<Boolean>(){
                @Override
                public void changed(ObservableValue<? extends Boolean> arg0, 
                    Boolean arg1, Boolean arg2) {
                        if (!arg2) {
                            commitEdit(textField.getText());
                        }
                }
            });
            
            textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if(event.getCode().equals(event.getCode().ENTER)){
                        commitEdit(textField.getText());
                    }
                    
                    if(event.getCode().equals(event.getCode().CONTROL)){
                        isCtrlHeld = false;
                    }
                }
            });
            
            textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if(event.getCode().equals(event.getCode().CONTROL)){
                        isCtrlHeld = true;
                    }
                    
                    if(event.getCode().equals(event.getCode().A) && isCtrlHeld){
                        NormalformLearning.togglePrim(index);
                        System.out.println(index +(". Entrypart's Prim is set to " + NormalformLearning.isPrim(index)));
                    }
                }
            });
        }
        
        public String getString() {
//            if(NormalformLearning.getEntry().isColumnCreated(index)){
//                createdTable = true;
//            }
//            if(getItem() != null && !getItem().toString().equals("") && !createdTable){
//                createdTable = true;
//                if(index<19){
//                    table.getColumns().add(newEditingCell());
//                }
//            }
            return getItem() == null ? "" : getItem().toString();
        }
        
//        public TableColumn newEditingCell() {
//            TableColumn Column = new TableColumn("");
//            Column.setEditable(true);
//            Callback<TableColumn, TableCell> cellFactory =
//                new Callback<TableColumn, TableCell>() {
//                    public TableCell call(TableColumn p){
//                        EditingCell ec = new EditingCell(table, index+1);
//                        NormalformLearning.editingCells.add(ec);
//                        NormalformLearning.getEntry().put(index+1);
//                        return ec;
//                    }
//                };
//            Column.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<NormalformLearning.Entry, String>, ObservableValue<String>>(){
//                @Override
//                public ObservableValue<String> call(TableColumn.CellDataFeatures<NormalformLearning.Entry, String> p) {
//                    return new SimpleStringProperty(p.getValue().getText(index+1));
//                }
//            });
//            Column.setCellFactory(cellFactory);
//            Column.setOnEditCommit(
//                    new EventHandler<TableColumn.CellEditEvent<NormalformLearning.Entry, String>>() {
//                @Override
//                public void handle(TableColumn.CellEditEvent<NormalformLearning.Entry, String> t) {
//                    ((NormalformLearning.Entry) t.getTableView().getItems().get(
//                            t.getTablePosition().getRow())).setText(t.getNewValue(), index+1);
//                }
//            }
//            );
//            return Column;
//        }
    }
