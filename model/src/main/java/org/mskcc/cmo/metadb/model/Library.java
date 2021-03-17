package org.mskcc.cmo.metadb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.mskcc.cmo.metadb.model.converter.RunStringConverter;
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

    public Library(){}

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((barcodeId == null) ? 0 : barcodeId.hashCode());
        result = prime * result + ((barcodeIndex == null) ? 0 : barcodeIndex.hashCode());
        result = prime * result + ((captureConcentrationNm == null) ? 0 : captureConcentrationNm.hashCode());
        result = prime * result + ((captureInputNg == null) ? 0 : captureInputNg.hashCode());
        result = prime * result + ((captureName == null) ? 0 : captureName.hashCode());
        result = prime * result + ((dnaInputNg == null) ? 0 : dnaInputNg.hashCode());
        result = prime * result + ((libraryConcentrationNgul == null) ? 0 : libraryConcentrationNgul.hashCode());
        result = prime * result + ((libraryIgoId == null) ? 0 : libraryIgoId.hashCode());
        result = prime * result + ((libraryVolume == null) ? 0 : libraryVolume.hashCode());
        result = prime * result + ((runs == null) ? 0 : runs.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Library other = (Library) obj;
        if (this.barcodeId == null ? other.barcodeId != null : !this.barcodeId.equals(other.barcodeId)) {
            return false;
        }
        if (this.barcodeIndex == null ? other.barcodeIndex != null : !this.barcodeIndex.equals(other.barcodeIndex)) {
            return false;
        }
        if (this.captureConcentrationNm == null ? other.captureConcentrationNm != null : !this.captureConcentrationNm.equals(other.captureConcentrationNm)) {
            return false;
        }
        if (this.captureInputNg == null ? other.captureInputNg != null : !this.captureInputNg.equals(other.captureInputNg)) {
            return false;
        }
        if (this.captureName == null ? other.captureName != null : !this.captureName.equals(other.captureName)) {
            return false;
        }
        if (this.dnaInputNg == null ? other.dnaInputNg != null : !this.dnaInputNg.equals(other.dnaInputNg)) {
            return false;
        }
        if (this.libraryConcentrationNgul == null ? other.libraryConcentrationNgul != null : 
            !this.libraryConcentrationNgul.equals(other.libraryConcentrationNgul)) {
            return false;
        }
        if (this.libraryIgoId == null ? other.libraryIgoId != null : !this.libraryIgoId.equals(other.libraryIgoId)) {
            return false;
        }
        if (this.libraryVolume == null ? other.libraryVolume != null : !this.libraryVolume.equals(other.libraryVolume)) {
            return false;
        }
        if (this.runs == null ? other.runs != null : !compareRunList(this.runs, other.runs)) {
            return false;
        }
        return true;
    }
    
    public boolean equalLists(List<Library> libraryList) {
        for (Library library: libraryList) {
            if(!this.equals(library)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean compareRunList(List<Run> foundList, List<Run> newList) {
        for (Run run: newList) {
           if (!run.equalLists(foundList)) {
                return false;
            }
        }
        return true;
    }
    
    
}
