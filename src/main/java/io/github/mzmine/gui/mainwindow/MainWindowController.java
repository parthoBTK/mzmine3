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

package io.github.mzmine.gui.mainwindow;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;

import org.controlsfx.control.StatusBar;
import org.controlsfx.control.TaskProgressView;
import org.dockfx.DockNode;
import org.dockfx.DockPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.featuretable.FeatureTableModule;
import io.github.mzmine.modules.featuretable.FeatureTableModuleParameters;
import io.github.mzmine.modules.plots.chromatogram.ChromatogramPlotModule;
import io.github.mzmine.modules.plots.chromatogram.ChromatogramPlotParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureTablesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureTablesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.project.MZmineProject;
import io.github.mzmine.taskcontrol.MSDKTask;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * This class is the main window of application
 * 
 */
public class MainWindowController implements Initializable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @FXML
    private DockPane mainDockPane;
    private DockNode visualizerDock;

    @FXML
    private TreeView<RawDataTreeItem> rawDataTree;

    @FXML
    private TreeView<FeatureTableTreeItem> featureTree;

    @FXML
    private TaskProgressView<Task<?>> tasksView;

    @FXML
    private StatusBar statusBar;

    @FXML
    private ProgressBar memoryBar;

    @FXML
    private Label memoryBarLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        rawDataTree.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);
        rawDataTree.setShowRoot(false);

        // Add mouse clicked event handler
        rawDataTree.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // Sample Line Chart
                    NumberAxis xAxis = new NumberAxis();
                    NumberAxis yAxis = new NumberAxis();
                    xAxis.setLabel("Label X");
                    final LineChart<Number, Number> lineChart = new LineChart<Number, Number>(
                            xAxis, yAxis);
                    lineChart.setTitle("Line Chart Example 2");
                    XYChart.Series series = new XYChart.Series();
                    series.setName("Sample X");
                    series.getData().add(new XYChart.Data(1, 23));
                    series.getData().add(new XYChart.Data(2, 14));
                    series.getData().add(new XYChart.Data(3, 15));
                    series.getData().add(new XYChart.Data(4, 24));
                    series.getData().add(new XYChart.Data(5, 34));
                    series.getData().add(new XYChart.Data(6, 36));
                    series.getData().add(new XYChart.Data(7, 22));
                    series.getData().add(new XYChart.Data(8, 45));
                    series.getData().add(new XYChart.Data(9, 43));
                    series.getData().add(new XYChart.Data(10, 17));
                    series.getData().add(new XYChart.Data(11, 29));
                    series.getData().add(new XYChart.Data(12, 25));
                    lineChart.getData().addAll(series);

                    // Add new window with chart
                    MZmineGUI.addWindow(lineChart, "Example Line Chart 2");
                }
            }
        });

        featureTree.getSelectionModel()
                .setSelectionMode(SelectionMode.MULTIPLE);
        featureTree.setShowRoot(false);

        // Add mouse clicked event handler
        featureTree.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    // Show feature table for selected row
                    ParameterSet moduleParameters = MZmineCore
                            .getConfiguration()
                            .getModuleParameters(FeatureTableModule.class);
                    FeatureTablesParameter inputTablesParam = moduleParameters
                            .getParameter(
                                    FeatureTableModuleParameters.featureTables);
                    inputTablesParam.switchType(
                            FeatureTablesSelectionType.GUI_SELECTED_FEATURE_TABLES);
                    FeatureTableModule moduleInstance = MZmineCore
                            .getModuleInstance(FeatureTableModule.class);
                    MZmineProject currentProject = MZmineCore
                            .getCurrentProject();
                    List<Task<?>> newTasks = new ArrayList<>();
                    moduleInstance.runModule(currentProject, moduleParameters,
                            newTasks);
                    MZmineCore.submitTasks(newTasks);
                }
            }
        });

        statusBar.setText("Welcome to MZmine " + MZmineCore.getMZmineVersion());

        // Setup the Timeline to update the memory indicator periodically
        final Timeline memoryUpdater = new Timeline();
        int UPDATE_FREQUENCY = 500; // ms
        memoryUpdater.setCycleCount(Animation.INDEFINITE);
        memoryUpdater.getKeyFrames().add(new KeyFrame(
                Duration.millis(UPDATE_FREQUENCY), (ActionEvent e) -> {

                    final long freeMemMB = Runtime.getRuntime().freeMemory()
                            / (1024 * 1024);
                    final long totalMemMB = Runtime.getRuntime().totalMemory()
                            / (1024 * 1024);
                    final double memory = ((double) (totalMemMB - freeMemMB))
                            / totalMemMB;

                    memoryBar.setProgress(memory);
                    memoryBarLabel
                            .setText(freeMemMB + "/" + totalMemMB + " MB free");
                }));
        memoryUpdater.play();

        // Setup the Timeline to update the MSDK tasks periodically
        final Timeline msdkTaskUpdater = new Timeline();
        UPDATE_FREQUENCY = 50; // ms
        msdkTaskUpdater.setCycleCount(Animation.INDEFINITE);
        msdkTaskUpdater.getKeyFrames().add(new KeyFrame(
                Duration.millis(UPDATE_FREQUENCY), (ActionEvent e) -> {

                    Collection<Task<?>> tasks = tasksView.getTasks();
                    for (Task<?> task : tasks) {
                        if (task instanceof MSDKTask) {
                            MSDKTask msdkTask = (MSDKTask) task;
                            msdkTask.refreshStatus();
                        }
                    }
                }));
        msdkTaskUpdater.play();
    }

    @FXML
    public void memoryBarClicked(MouseEvent e) {
        // Run garbage collector on a new thread, so it does not block the GUI
        new Thread(() -> {
            logger.info("Running garbage collector");
            System.gc();
        }).start();

    }

    public DockPane getMainDockPane() {
        return mainDockPane;
    }

    public DockNode getVisualizerDock() {
        return visualizerDock;
    }

    public void setVisualizerDock(@Nonnull DockNode visualizerDock) {
        this.visualizerDock = visualizerDock;
    }

    public TreeView<RawDataTreeItem> getRawDataTree() {
        return rawDataTree;
    }

    public TreeView<FeatureTableTreeItem> getFeatureTree() {
        return featureTree;
    }

    public TaskProgressView<Task<?>> getTaskTable() {
        return tasksView;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    @FXML
    protected void handleShowTIC(ActionEvent event) {
        logger.debug("Activated Show chromatogram menu item");
        ParameterSet chromPlotParams = MZmineCore.getConfiguration()
                .getModuleParameters(ChromatogramPlotModule.class);
        RawDataFilesParameter inputFilesParam = chromPlotParams
                .getParameter(ChromatogramPlotParameters.inputFiles);
        inputFilesParam
                .switchType(RawDataFilesSelectionType.GUI_SELECTED_FILES);
        MZmineGUI.setupAndRunModule(ChromatogramPlotModule.class);
    }

    @FXML
    protected void removeRawData(ActionEvent event) {
        // Get selected tree items
        ObservableList<TreeItem<RawDataTreeItem>> rows = null;
        if (rawDataTree.getSelectionModel() != null) {
            rows = rawDataTree.getSelectionModel().getSelectedItems();
        }

        // Loop through all selected tree items
        if (rows != null) {
            for (int i = rows.size() - 1; i >= 0; i--) {
                TreeItem<RawDataTreeItem> row = rows.get(i);

                // Remove feature table from current project
                RawDataTreeItem rawDataTreeItem = row.getValue();
                MZmineCore.getCurrentProject()
                        .removeFile(rawDataTreeItem.getRawDataFile());

                // Remove raw data file from tree table view
                TreeItem<?> parent = row.getParent();
                parent.getChildren().remove(row);
            }
            rawDataTree.getSelectionModel().clearSelection();
        }
    }

    @FXML
    protected void removeFeatureTable(ActionEvent event) {
        // Get selected tree items
        ObservableList<TreeItem<FeatureTableTreeItem>> rows = null;
        if (featureTree.getSelectionModel() != null) {
            rows = featureTree.getSelectionModel().getSelectedItems();
        }

        // Loop through all selected tree items
        if (rows != null) {
            for (int i = rows.size() - 1; i >= 0; i--) {
                TreeItem<FeatureTableTreeItem> row = rows.get(i);

                // Remove feature table from current project
                FeatureTableTreeItem featureTableTreeItem = row.getValue();
                MZmineCore.getCurrentProject().removeFeatureTable(
                        featureTableTreeItem.getFeatureTable());

                // Remove feature table from tree table view
                TreeItem<?> parent = row.getParent();
                parent.getChildren().remove(row);
            }
            featureTree.getSelectionModel().clearSelection();
        }
    }

}
