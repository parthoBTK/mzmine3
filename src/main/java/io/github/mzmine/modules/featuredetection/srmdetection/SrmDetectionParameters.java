/*
 * Copyright 2006-2016 The MZmine 3 Development Team
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
 * MZmine 3; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.featuredetection.srmdetection;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterValidator;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class SrmDetectionParameters extends ParameterSet {

    public static final RawDataFilesParameter rawDataFiles = new RawDataFilesParameter();

    public static final DoubleParameter minHeight = new DoubleParameter(
            "Min peak height",
            "Peaks with intensities less than this value are interpreted as noise",
            "Algorithm Parameters",
            MZmineCore.getConfiguration().getIntensityFormat(),
            ParameterValidator.createNonEmptyValidator(), 5000d);

    public static final PercentParameter intensityTolerance = new PercentParameter(
            "Intensity tolerance",
            "Maximum allowed deviation of the peak chromatogram from the expected /\\ shape.",
            "Algorithm Parameters",
            ParameterValidator.createNonEmptyValidator(), 0.15);

    public static final StringParameter nameSuffix = new StringParameter(
            "Name suffix",
            "Suffix to be added to the raw data file(s) when creating the feature table(s)",
            "Output", " msmsDetection");

    /**
     * Create the parameter set.
     */
    public SrmDetectionParameters() {
        super(rawDataFiles, minHeight, intensityTolerance, nameSuffix);
    }

}
