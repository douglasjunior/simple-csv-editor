/*
The MIT License (MIT)

Copyright (c) 2016 Douglas Nassif Roma Junior

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.github.douglasjunior.simpleCSVEditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javax.annotation.processing.FilerException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

public class FXMLController implements Initializable {

    @FXML
    private TableView<CSVRow> tableView;

    @FXML
    private AnchorPane root;

    private FileChooser fileChooser;

    private CSVFormat csvFormat;
    private Integer numbeColumns = 0;
    private File file;
    private boolean saved = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        file = new File("");
        csvFormat = CSVFormat.DEFAULT.withIgnoreEmptyLines(false);

        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);
        MenuItem inserirLinha = new MenuItem("Inserir linha");
        inserirLinha.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                addNewRow();
                setNotSaved();
            }
        });

        contextMenu.getItems().add(inserirLinha);
        MenuItem removerLinha = new MenuItem("Remover linha");
        removerLinha.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                deleteRow();
                setNotSaved();
            }
        });
        contextMenu.getItems().add(removerLinha);

        contextMenu.getItems().add(new SeparatorMenuItem());

        MenuItem inserirColuna = new MenuItem("Inserir coluna");
        inserirColuna.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                addNewColumn();
                setNotSaved();
            }
        });
        contextMenu.getItems().add(inserirColuna);

        MenuItem removerColuna = new MenuItem("Remover coluna");
        removerColuna.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                deleteColumn();
                setNotSaved();
            }
        });
        contextMenu.getItems().add(removerColuna);

        tableView.setContextMenu(contextMenu);
    }

    @FXML
    private void onSalvarActionEvent(ActionEvent event) {
        try (PrintWriter pw = new PrintWriter(file); CSVPrinter print = csvFormat.print(pw)) {
            for (CSVRow row : tableView.getItems()) {
                if (row.isEmpty()) {
                    print.println();
                } else {
                    for (SimpleStringProperty column : row.getColumns()) {
                        print.print(column.getValue());
                    }
                    print.println();
                }
            }
            print.flush();
            setSaved();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert d = new Alert(Alert.AlertType.ERROR);
            d.setHeaderText("Ooops, não foi possível salvar o arquivo " + (file != null ? file.getName() : "."));
            d.setContentText(ex.getMessage());
            d.setTitle("Erro");
            d.initOwner(root.getScene().getWindow());
            d.show();
        }
    }

    @FXML
    private void onAbrirActionEvent(ActionEvent event) {
        File csvFile = null;
        try {
            if (!saved) {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
                a.setHeaderText("Deseja descartar as alterações??");
                a.initOwner(root.getScene().getWindow());
                Optional<ButtonType> result = a.showAndWait();
                if (result.get() != ButtonType.YES) {
                    return;
                }
            }
            csvFile = openFileChooser();
            if (csvFile == null || !csvFile.exists()) {
                throw new FileNotFoundException("O arquivo selecionado não existe!");
            }
            ObservableList<CSVRow> rows = readFile(csvFile);
            if (rows == null || rows.isEmpty()) {
                throw new FilerException("O arquivo selecionado está vazio!");
            }
            updateTable(rows);
            this.file = csvFile;
            setSaved();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert d = new Alert(Alert.AlertType.ERROR);
            d.setHeaderText("Ooops, não foi possível abrir o arquivo " + (csvFile != null ? csvFile.getName() : "."));
            d.setContentText(ex.getMessage());
            d.setTitle("Erro");
            d.initOwner(root.getScene().getWindow());
            d.show();
        }
    }

    private void addNewRow() {
        Integer current = tableView.getSelectionModel().getSelectedIndex();
        tableView.getItems().add(current, new CSVRow());
        tableView.getSelectionModel().select(current);
    }

    private void deleteRow() {
        tableView.getItems().remove(tableView.getSelectionModel().getSelectedIndex());
    }

    private void addNewColumn() {
        List<TablePosition> cells = tableView.getSelectionModel().getSelectedCells();
        int columnIndex = cells.get(0).getColumn();
        for (CSVRow row : tableView.getItems()) {
            row.addColumn(columnIndex);
        }
        numbeColumns++;
        tableView.getColumns().add(createColumn(numbeColumns - 1));
        tableView.refresh();
    }

    private void deleteColumn() {
        List<TablePosition> cells = tableView.getSelectionModel().getSelectedCells();
        int columnIndex = cells.get(0).getColumn();
        for (CSVRow row : tableView.getItems()) {
            row.removeColumn(columnIndex);
        }
        numbeColumns--;
        tableView.getColumns().remove(tableView.getColumns().size() - 1);
        tableView.refresh();
    }

    private File openFileChooser() {
        if (fileChooser == null) {
            fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir Arquivo");
            fileChooser.setInitialDirectory(
                    new File(System.getProperty("user.home"))
            );
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSV", "*.csv")
            );
        }
        return fileChooser.showOpenDialog(root.getScene().getWindow());
    }

    private ObservableList<CSVRow> readFile(File csvFile) throws IOException {
        ObservableList<CSVRow> rows = FXCollections.observableArrayList();
        Integer maxColumns = 0;
        try (Reader in = new InputStreamReader(new FileInputStream(csvFile));) {
            CSVParser parse = csvFormat.parse(in);
            for (CSVRecord record : parse.getRecords()) {
                if (maxColumns < record.size()) {
                    maxColumns = record.size();
                }
                CSVRow row = new CSVRow();
                for (int i = 0; i < record.size(); i++) {
                    row.getColumns().add(new SimpleStringProperty(record.get(i)));
                }
                rows.add(row);
            }
            this.numbeColumns = maxColumns;
        }
        return rows;
    }

    private void updateTable(ObservableList<CSVRow> rows) {
        tableView.getColumns().clear();
        for (int i = 0; i < numbeColumns; i++) {
            TableColumn<CSVRow, String> col = createColumn(i);
            tableView.getColumns().add(col);
        }
        tableView.setItems(rows);
        tableView.setEditable(true);
        tableView.getSelectionModel().setCellSelectionEnabled(true);
    }

    private void setNotSaved() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setTitle(file.getName() + " (Não salvo) - " + MainApp.TITLE);
        saved = false;
    }

    private void setSaved() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setTitle(file.getName() + " - " + MainApp.TITLE);
        saved = true;
    }

    @FXML
    private void onSairActionEvent(ActionEvent event) {
        if (saved) {
            sair();
        } else {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
            a.setHeaderText("Deseja sair sem salvar?");
            a.initOwner(root.getScene().getWindow());
            Optional<ButtonType> result = a.showAndWait();
            if (result.get() == ButtonType.YES) {
                sair();
            }
        }
    }

    private void sair() {
        System.exit(0);
    }

    private TableColumn<CSVRow, String> createColumn(int index) {
        TableColumn<CSVRow, String> col = new TableColumn<>((index + 1) + "");
        col.setSortable(false);
        col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<CSVRow, String>, ObservableValue<String>>() {
            @Override
            public ObservableValue<String> call(TableColumn.CellDataFeatures<CSVRow, String> param) {
                adjustColumns(param.getValue().getColumns());
                return param.getValue().getColumns().get(index);
            }
        });
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(new EventHandler<CellEditEvent<CSVRow, String>>() {
            @Override
            public void handle(CellEditEvent<CSVRow, String> event) {
                adjustColumns(event.getRowValue().getColumns());
                event.getRowValue().getColumns().get(index).set(event.getNewValue());
                setNotSaved();
            }
        });
        col.setEditable(true);
        return col;
    }

    private void adjustColumns(List<SimpleStringProperty> columns) {
        int dif = numbeColumns - columns.size();
        for (int i = 0; i < dif; i++) {
            columns.add(new SimpleStringProperty());
        }
    }
}
