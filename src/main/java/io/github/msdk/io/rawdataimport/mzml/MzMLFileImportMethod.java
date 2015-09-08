/* 
 * (C) Copyright 2015 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */

package io.github.msdk.io.rawdataimport.mzml;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;

import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.datamodel.chromatograms.Chromatogram;
import io.github.msdk.datamodel.impl.MSDKObjectBuilder;
import io.github.msdk.datamodel.msspectra.MsSpectrumDataPointList;
import io.github.msdk.datamodel.msspectra.MsSpectrumType;
import io.github.msdk.datamodel.rawdata.ChromatographyInfo;
import io.github.msdk.datamodel.rawdata.FragmentationInfo;
import io.github.msdk.datamodel.rawdata.IsolationInfo;
import io.github.msdk.datamodel.rawdata.MsFunction;
import io.github.msdk.datamodel.rawdata.MsScan;
import io.github.msdk.datamodel.rawdata.MsScanType;
import io.github.msdk.datamodel.rawdata.PolarityType;
import io.github.msdk.datamodel.rawdata.RawDataFile;
import io.github.msdk.datamodel.util.MsSpectrumUtil;
import io.github.msdk.io.spectrumtypedetection.SpectrumTypeDetectionMethod;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.xml.io.MzMLObjectIterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;
import uk.ac.ebi.pride.tools.jmzreader.JMzReaderException;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLParsingException;

/**
 * This class reads mzML data format using the jmzml library.
 */
public class MzMLFileImportMethod implements MSDKMethod<RawDataFile> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final @Nonnull File sourceFile;

    private boolean canceled = false;

    private JMzMLRawDataFile newRawFile;
    private long totalScans = 0, totalChromatograms = 0, parsedScans,
            parsedChromatograms;
    private int lastScanNumber = 0;

    private Map<String, Integer> scanIdTable = new Hashtable<String, Integer>();

    public MzMLFileImportMethod(@Nonnull File sourceFile) {
        this.sourceFile = sourceFile;
    }

    /**
     * @throws JMzReaderException
     * @throws MzXMLParsingException
     * @throws MSDKException
     */
    @SuppressWarnings("null")
    @Override
    public RawDataFile execute() throws MSDKException {

        logger.info("Started parsing file " + sourceFile);

        MzMLUnmarshaller parser = new MzMLUnmarshaller(sourceFile);

        totalScans = parser
                .getObjectCountForXpath("/run/spectrumList/spectrum");
        totalChromatograms = parser
                .getObjectCountForXpath("/run/chromatogramList/chromatogram");

        // Prepare data structures
        List<MsFunction> msFunctionsList = new ArrayList<>();
        List<MsScan> scansList = new ArrayList<>();
        List<Chromatogram> chromatogramsList = new ArrayList<>();
        MsSpectrumDataPointList dataPoints = MSDKObjectBuilder
                .getMsSpectrumDataPointList();

        // Create the XMLBasedRawDataFile object
        newRawFile = new JMzMLRawDataFile(sourceFile, parser, msFunctionsList,
                scansList, chromatogramsList);

        MzMLObjectIterator<Spectrum> iterator = parser
                .unmarshalCollectionFromXpath("/run/spectrumList/spectrum",
                        Spectrum.class);

        while (iterator.hasNext()) {

            if (canceled)
                return null;

            Spectrum spectrum = iterator.next();

            // Get the scan number
            String spectrumId = spectrum.getId();
            Integer scanNumber = convertSpectrumIdToScanNumber(spectrumId);

            // Get the MS function
            MsFunction msFunction = JMzMLUtil.extractMsFunction(spectrum);
            msFunctionsList.add(msFunction);

            // Store the chromatography data
            ChromatographyInfo chromData = JMzMLUtil
                    .extractChromatographyData(spectrum);

            // Extract the scan data points, so we can check the m/z range and
            // detect the spectrum type (profile/centroid)
            JMzMLUtil.extractDataPoints(spectrum, dataPoints);

            // Get the m/z range
            Range<Double> mzRange = MsSpectrumUtil.getMzRange(dataPoints);

            // Get the instrument scanning range
            Range<Double> scanningRange = null;

            // Get the TIC
            Float tic = MsSpectrumUtil.getTIC(dataPoints);

            // Auto-detect whether this scan is centroided
            SpectrumTypeDetectionMethod detector = new SpectrumTypeDetectionMethod(
                    dataPoints);
            MsSpectrumType spectrumType = detector.execute();

            // Get the MS scan type
            MsScanType scanType = JMzMLUtil.extractScanType(spectrum);

            // Get the polarity
            PolarityType polarity = JMzMLUtil.extractPolarity(spectrum);

            // Get the in-source fragmentation
            FragmentationInfo sourceFragmentation = JMzMLUtil
                    .extractSourceFragmentation(spectrum);

            // Get the in-source fragmentation
            List<IsolationInfo> isolations = JMzMLUtil
                    .extractIsolations(spectrum);

            // Create a new MsScan instance
            JMzMLMsScan scan = new JMzMLMsScan(newRawFile, spectrumId,
                    spectrumType, msFunction, chromData, scanType, mzRange,
                    scanningRange, scanNumber, tic, polarity,
                    sourceFragmentation, isolations);

            // Add the scan to the final raw data file
            scansList.add(scan);

            parsedScans++;

        }

        parsedChromatograms = totalChromatograms;
        logger.info("Finished importing " + sourceFile + ", parsed "
                + parsedScans + " scans");

        return newRawFile;

    }

    private Integer convertSpectrumIdToScanNumber(String spectrumId) {

        if (scanIdTable.containsKey(spectrumId))
            return scanIdTable.get(spectrumId);

        final Pattern pattern = Pattern.compile("scan=([0-9]+)");
        final Matcher matcher = pattern.matcher(spectrumId);
        boolean scanNumberFound = matcher.find();

        // Some vendors include scan=XX in the ID, some don't, such as
        // mzML converted from WIFF files. See the definition of nativeID in
        // http://psidev.cvs.sourceforge.net/viewvc/psidev/psi/psi-ms/mzML/controlledVocabulary/psi-ms.obo
        if (scanNumberFound) {
            Integer scanNumber = Integer.parseInt(matcher.group(1));
            lastScanNumber = scanNumber;
            scanIdTable.put(spectrumId, scanNumber);
            return scanNumber;
        }

        Integer scanNumber = lastScanNumber + 1;
        lastScanNumber++;
        scanIdTable.put(spectrumId, scanNumber);
        return scanNumber;
    }

    @Override
    public Float getFinishedPercentage() {
        return totalScans == 0 ? null
                : (float) (parsedScans + parsedChromatograms)
                        / (totalScans + totalChromatograms);
    }

    @Override
    @Nullable
    public RawDataFile getResult() {
        return newRawFile;
    }

    @Override
    public void cancel() {
        this.canceled = true;
    }

}