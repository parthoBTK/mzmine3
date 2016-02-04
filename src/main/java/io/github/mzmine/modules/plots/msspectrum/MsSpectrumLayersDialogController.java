/*
 * Copyright 2006-2015 The MZmine 3 Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 3; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.plots.msspectrum;

import io.github.msdk.datamodel.msspectra.MsSpectrumType;
import io.github.mzmine.main.MZmineCore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;

/**
 * MS spectrum layer setup dialog controller
 */
public class MsSpectrumLayersDialogController {

    @FXML
    private Stage dialogStage;

    @FXML
    private TableView<MsSpectrumDataSet> layersTable;

    @FXML
    private TableColumn<MsSpectrumDataSet, MsSpectrumType> renderingTypeColumn;

    @FXML
    private TableColumn<MsSpectrumDataSet, Color> colorColumn;

    @FXML
    private TableColumn<MsSpectrumDataSet, Double> mzShiftColumn;

    @FXML
    private TableColumn<MsSpectrumDataSet, Number> intensityScaleColumn;

    @FXML
    private TableColumn<MsSpectrumDataSet, Integer> lineThicknessColumn;

    @FXML
    private TableColumn<MsSpectrumDataSet, Boolean> showDataPointsColumn;

    @FXML
    public void initialize() {
        ObservableList<MsSpectrumType> choices = FXCollections
                .observableArrayList(MsSpectrumType.CENTROIDED,
                        MsSpectrumType.PROFILE);
        renderingTypeColumn
                .setCellFactory(ChoiceBoxTableCell.forTableColumn(choices));

        colorColumn.setCellFactory(column -> {
            TableCell<MsSpectrumDataSet, Color> cell = new TableCell<MsSpectrumDataSet, Color>() {
                private final ColorPicker colorPicker = new ColorPicker();

                {

                    tableRowProperty().addListener(e -> {
                        TableRow row = getTableRow();
                        if (row != null) {
                            MsSpectrumDataSet dataSet = (MsSpectrumDataSet) row
                                    .getItem();
                            if (dataSet != null)
                                colorPicker.valueProperty().bindBidirectional(
                                        dataSet.colorProperty());
                        }
                    });
                }

                @Override
                protected void updateItem(Color c, boolean empty) {

                    super.updateItem(c, empty);
                    if (empty || c == null) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    colorPicker.setValue(c);

                    setGraphic(colorPicker);
                }
            };
            return cell;
        });

        mzShiftColumn.setCellFactory(
                TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        intensityScaleColumn.setCellFactory(
                TextFieldTableCell.forTableColumn(new NumberStringConverter(
                        MZmineCore.getConfiguration().getIntensityFormat())));
        lineThicknessColumn.setCellFactory(TextFieldTableCell
                .forTableColumn(new IntegerStringConverter()));
        showDataPointsColumn.setCellFactory(
                CheckBoxTableCell.forTableColumn(showDataPointsColumn));

    }

    @FXML
    public void handleClose(Event event) {
        dialogStage.close();
    }

    public void setItems(ObservableList<MsSpectrumDataSet> items) {
        layersTable.setItems(items);
    }

}