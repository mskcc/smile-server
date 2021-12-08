package org.mskcc.cmo.metadb.model.igo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 *
 * @author ochoaa
 */
@JsonIgnoreProperties({"numFastQs"})
public class IgoLibrary {
    private String barcodeId;
    private String barcodeIndex;
    private String libraryIgoId;
    private Double libraryVolume;
    private Double libraryConcentrationNgul;
    private Double dnaInputNg;
    private String captureConcentrationNm;
    private String captureInputNg;
    private String captureName;
    private List<IgoRun> runs;

    public IgoLibrary() {}

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

    public List<IgoRun> getRuns() {
        return runs;
    }

    public void setRuns(List<IgoRun> runs) {
        this.runs = runs;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
