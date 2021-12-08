package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.converter.RunStringConverter;
import org.mskcc.cmo.metadb.model.igo.IgoLibrary;
import org.mskcc.cmo.metadb.model.igo.IgoRun;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/**
 *
 * @author ochoaa
 */
@JsonIgnoreProperties({"numFastQs"})
public class Library implements Serializable {
    @Id @GeneratedValue
    private Long id;
    private String barcodeId;
    private String barcodeIndex;
    private String libraryIgoId;
    private Double libraryVolume; // [uL]
    private Double libraryConcentrationNgul; // ng/uL
    private Double dnaInputNg;
    private String captureConcentrationNm;
    private String captureInputNg;
    private String captureName;
    @Convert(RunStringConverter.class)
    private List<Run> runs;

    public Library() {}

    /**
     * Library constructor.
     * TODO: Decide whether to keep or just
     * make the List of 'Library' a string for
     * SampleMetadata.libraries since we are
     * storing in the graph db as a string anyway.
     * Replacing 'Library' with string will allow us to remove
     * the LibrariesStringConverter
     * @param igoLibrary
     */
    public Library(IgoLibrary igoLibrary) {
        this.barcodeId = igoLibrary.getBarcodeId();
        this.barcodeIndex = igoLibrary.getBarcodeIndex();
        this.libraryIgoId = igoLibrary.getLibraryIgoId();
        this.libraryVolume = igoLibrary.getLibraryVolume();
        this.libraryConcentrationNgul = igoLibrary.getLibraryConcentrationNgul();
        this.dnaInputNg = igoLibrary.getDnaInputNg();
        this.captureConcentrationNm = igoLibrary.getCaptureConcentrationNm();
        this.captureInputNg = igoLibrary.getCaptureInputNg();
        this.captureName = igoLibrary.getCaptureName();
        this.runs = new ArrayList<>();
        for (IgoRun r : igoLibrary.getRuns()) {
            runs.add(new Run(r));
        }
    }

    /**
     * Library constructor.
     * @param libraryIgoId
     * @param libraryVolume
     * @param libraryConcentrationNgul
     * @param dnaInputNg
     */
    public Library(String libraryIgoId, Double libraryVolume,
            Double libraryConcentrationNgul, Double dnaInputNg) {
        this.libraryIgoId = libraryIgoId;
        this.libraryVolume = libraryVolume;
        this.libraryConcentrationNgul = libraryConcentrationNgul;
        this.dnaInputNg = dnaInputNg;
    }

    /**
     * Library constructor.
     * @param barcodeId
     * @param barcodeIndex
     * @param libraryIgoId
     * @param libraryVolume
     * @param libraryConcentrationNgul
     * @param dnaInputNg
     * @param captureConcentrationNm
     * @param captureInputNg
     * @param captureName
     * @param runs
     */
    public Library(String barcodeId, String barcodeIndex, String libraryIgoId, Double libraryVolume,
            Double libraryConcentrationNgul, Double dnaInputNg, String captureConcentrationNm,
            String captureInputNg, String captureName, List<Run> runs) {
        this.barcodeId = barcodeId;
        this.barcodeIndex = barcodeIndex;
        this.libraryIgoId = libraryIgoId;
        this.libraryVolume = libraryVolume;
        this.libraryConcentrationNgul = libraryConcentrationNgul;
        this.dnaInputNg = dnaInputNg;
        this.captureConcentrationNm = captureConcentrationNm;
        this.captureInputNg = captureInputNg;
        this.captureName = captureName;
        this.runs = runs;
    }

    public String getBarcodeId() {
        return barcodeId;
    }

    public void setBarcodeId(String barcodeId) {
        this.barcodeId = barcodeId;
    }

    public String getBarcodeIndex() {
        return barcodeIndex;
    }

    public void setBarcodeIndex(String barcodeIndex) {
        this.barcodeIndex = barcodeIndex;
    }

    public String getLibraryIgoId() {
        return libraryIgoId;
    }

    public void setLibraryIgoId(String libraryIgoId) {
        this.libraryIgoId = libraryIgoId;
    }

    public Double getLibraryVolume() {
        return libraryVolume;
    }

    public void setLibraryVolume(Double libraryVolume) {
        this.libraryVolume = libraryVolume;
    }

    public Double getLibraryConcentrationNgul() {
        return libraryConcentrationNgul;
    }

    public void setLibraryConcentrationNgul(Double libraryConcentrationNgul) {
        this.libraryConcentrationNgul = libraryConcentrationNgul;
    }

    public Double getDnaInputNg() {
        return dnaInputNg;
    }

    public void setDnaInputNg(Double dnaInputNg) {
        this.dnaInputNg = dnaInputNg;
    }

    public String getCaptureConcentrationNm() {
        return captureConcentrationNm;
    }

    public void setCaptureConcentrationNm(String captureConcentrationNm) {
        this.captureConcentrationNm = captureConcentrationNm;
    }

    public String getCaptureInputNg() {
        return captureInputNg;
    }

    public void setCaptureInputNg(String captureInputNg) {
        this.captureInputNg = captureInputNg;
    }

    public String getCaptureName() {
        return captureName;
    }

    public void setCaptureName(String captureName) {
        this.captureName = captureName;
    }

    /**
     * Returns empty array list if field is null.
     * @return
     */
    public List<Run> getRuns() {
        if (runs == null) {
            this.runs = new ArrayList<>();
        }
        return runs;
    }

    public void setRuns(List<Run> runs) {
        this.runs = runs;
    }

    /**
     * Adds Run to list.
     * @param run
     */
    public void addRun(Run run) {
        if (runs == null) {
            this.runs = new ArrayList<>();
        }
        this.runs.add(run);
    }

    /**
     * Determines whether library has any fastqs.
     * @return
     */
    public boolean hasFastqs() {
        for (Run run : runs) {
            if (run.getFastqs() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns num of non-null fastqs.
     * @return
     */
    public Integer getNumFastQs() {
        Integer numFastqs = 0;
        for (Run run : runs) {
            if (run.getFastqs() != null) {
                numFastqs += run.getFastqs().size();
            }
        }
        return numFastqs;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
