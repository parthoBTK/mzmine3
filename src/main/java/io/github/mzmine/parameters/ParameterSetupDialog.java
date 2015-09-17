/*
 * Copyright 2006-2015 The MZmine 3 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.parameters;

import java.net.URL;

import org.controlsfx.property.editor.PropertyEditor;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;

import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;

/**
 * Parameter setup dialog
 */
public class ParameterSetupDialog extends Alert {

    /**
     * Help window for this setup dialog. Initially null, until the user clicks
     * the Help button.
     */
    private HelpWindow helpWindow = null;

    ParameterSetupDialog(ParameterSet parameters) {

        super(AlertType.CONFIRMATION);

        setTitle("Parameters");
        setHeaderText("Please set parameter values");
        setResizable(true);

        // Add Help button
        final URL helpURL = parameters.getClass().getResource("help/help.html");
        setupHelpButton(helpURL);

        // Add validation support
        ValidationSupport validationSupport = new ValidationSupport();

        // Add ParmeterSheetView to edit the parameters
        ParameterSheetView sheet = new ParameterSheetView(parameters,
                validationSupport);
        getDialogPane().setContent(sheet);

        // When the user presses the OK button, we need to commit all the
        // changes in the editors to the actual parameters
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(ActionEvent.ACTION, e -> {
            for (Parameter<?> parameter : parameters) {
                PropertyEditor<?> editor = sheet
                        .getEditorForParameter(parameter);
                Object value = editor.getValue();
                parameter.setValue(value);
            }
            if (validationSupport.isInvalid()) {
                e.consume();
                ValidationResult vr = validationSupport.getValidationResult();
                StringBuilder message = new StringBuilder(
                        "Please check the parameter settings:\n\n");
                for (ValidationMessage m : vr.getMessages()) {
                    message.append(m.getText());
                    message.append("\n");
                }
                MZmineGUI.displayMessage(message.toString());
            }

        });

    }

    private void setupHelpButton(URL helpURL) {

        // Add a Help button
        ButtonType helpButtonType = new ButtonType("Help", ButtonData.HELP);
        getDialogPane().getButtonTypes().add(helpButtonType);
        Button helpButton = (Button) getDialogPane()
                .lookupButton(helpButtonType);

        // If there is no help file, disable the Help button
        if (helpURL == null) {
            helpButton.setDisable(true);
            return;
        }

        // Prevent closing this dialog by the Help button by adding an event
        // filter
        helpButton.addEventFilter(ActionEvent.ACTION, event -> {
            if ((helpWindow != null) && (!helpWindow.isShowing())) {
                helpWindow = null;
            }

            if (helpWindow != null) {
                helpWindow.toFront();
            } else {
                helpWindow = new HelpWindow(helpURL.toString());
                helpWindow.show();
            }
            event.consume();
        });

        // Close the help window when we close this dialog
        setOnHidden(e -> {
            if ((helpWindow != null) && (helpWindow.isShowing())) {
                helpWindow.close();
            }
        });

    }

}