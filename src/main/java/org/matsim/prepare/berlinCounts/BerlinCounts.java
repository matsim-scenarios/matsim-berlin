package org.matsim.prepare.berlinCounts;

import java.util.Arrays;
import java.util.List;

public class BerlinCounts {

    public final List<String> informations = Arrays.asList("MQ_ID","DTVW_KFZ","DTVW_LKW","PERC_LKW","PERC_Q_KFZ_TYPE","PERC_Q_PKW_TYPE","PERC_Q_LKW_TYPE","linkid");

    private int MQ_ID;
    private int DTVW_KFZ;
    private int DTVW_LKW;
    private double PERC_LKW;
    private double[] PERC_Q_KFZ_TYPE = new double[24];
    private double[] PERC_Q_PKW_TYPE = new double[24];
    private double[] PERC_Q_LKW_TYPE = new double[24];
    private int linkid;

    public BerlinCounts(int MQ_ID) {
        this.MQ_ID = MQ_ID;
    }

    public int getMQ_ID() {
        return MQ_ID;
    }

    public void setMQ_ID(int MQ_ID) {
        this.MQ_ID = MQ_ID;
    }

    public int getDTVW_KFZ() {
        return DTVW_KFZ;
    }

    public void setDTVW_KFZ(int DTVW_KFZ) {
        this.DTVW_KFZ = DTVW_KFZ;
    }

    public int getDTVW_LKW() {
        return DTVW_LKW;
    }

    public void setDTVW_LKW(int DTVW_LKW) {
        this.DTVW_LKW = DTVW_LKW;
    }

    public double getPERC_LKW() {
        return PERC_LKW;
    }

    public void setPERC_LKW(double PERC_LKW) {
        this.PERC_LKW = PERC_LKW;
    }

    public double[] getPERC_Q_KFZ_TYPE() {
        return PERC_Q_KFZ_TYPE;
    }

    public void setPERC_Q_KFZ_TYPE(double[] PERC_Q_KFZ_TYPE) {
        this.PERC_Q_KFZ_TYPE = PERC_Q_KFZ_TYPE;
    }

    public double[] getPERC_Q_PKW_TYPE() {
        return PERC_Q_PKW_TYPE;
    }

    public void setPERC_Q_PKW_TYPE(double[] PERC_Q_PKW_TYPE) {
        this.PERC_Q_PKW_TYPE = PERC_Q_PKW_TYPE;
    }

    public double[] getPERC_Q_LKW_TYPE() {
        return PERC_Q_LKW_TYPE;
    }

    public void setPERC_Q_LKW_TYPE(double[] PERC_Q_LKW_TYPE) {
        this.PERC_Q_LKW_TYPE = PERC_Q_LKW_TYPE;
    }

    public int getLinkid() {
        return linkid;
    }

    public void setLinkid(int linkid) {
        this.linkid = linkid;
    }

    public void setArrays(int i, double PERC_Q_KFZ_TYPE, double PERC_Q_PKW_TYPE, double PERC_Q_LKW_TYPE) {
        this.PERC_Q_KFZ_TYPE[i] = PERC_Q_KFZ_TYPE;
        this.PERC_Q_PKW_TYPE[i] = PERC_Q_PKW_TYPE;
        this.PERC_Q_LKW_TYPE[i] = PERC_Q_LKW_TYPE;
    }
}
