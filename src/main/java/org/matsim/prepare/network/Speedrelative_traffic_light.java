package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public class Speedrelative_traffic_light implements FeatureRegressor {

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 123.77791684254963) / 86.92545218615102;
		data[1] = (ft.getDouble("speed") - 13.195084423807513) / 2.5553097705928556;
		data[2] = (ft.getDouble("numFoes") - 2.4094554664415364) / 0.6618814394678828;
		data[3] = (ft.getDouble("numLanes") - 1.9147319544111439) / 0.9803419977659901;
		data[4] = (ft.getDouble("junctionSize") - 13.871042634022794) / 4.3880523696095075;
		data[5] = ft.getDouble("dir_l");
		data[6] = ft.getDouble("dir_r");
		data[7] = ft.getDouble("dir_s");
		data[8] = ft.getDouble("dir_multiple_s");
		data[9] = ft.getDouble("dir_exclusive");
		data[10] = ft.getDouble("priority_lower");
		data[11] = ft.getDouble("priority_equal");
		data[12] = ft.getDouble("priority_higher");
		data[13] = ft.getDouble("changeNumLanes");

        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);

        return score(data);
    }
    public static double score(double[] input) {
        double var0;
        if (input[0] >= -0.14153412) {
            if (input[0] >= 0.52553177) {
                if (input[0] >= 1.4986644) {
                    if (input[1] >= 0.815915) {
                        var0 = 0.029490458;
                    } else {
                        var0 = 0.1190194;
                    }
                } else {
                    if (input[1] >= 0.815915) {
                        var0 = 0.0;
                    } else {
                        var0 = 0.09091397;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[1] >= -1.3599465) {
                        var0 = 0.04412549;
                    } else {
                        var0 = 0.096885815;
                    }
                } else {
                    if (input[4] >= 0.3712256) {
                        var0 = -0.03209078;
                    } else {
                        var0 = 0.035963815;
                    }
                }
            }
        } else {
            if (input[1] >= -0.27201572) {
                if (input[6] >= 0.5) {
                    if (input[0] >= -0.6357507) {
                        var0 = -0.042446688;
                    } else {
                        var0 = -0.08748933;
                    }
                } else {
                    if (input[0] >= -0.67083824) {
                        var0 = 0.024084387;
                    } else {
                        var0 = -0.029583162;
                    }
                }
            } else {
                if (input[0] >= -0.6508786) {
                    if (input[4] >= 0.59911716) {
                        var0 = 0.014977028;
                    } else {
                        var0 = 0.063402615;
                    }
                } else {
                    if (input[4] >= -0.08455747) {
                        var0 = -0.029922005;
                    } else {
                        var0 = 0.010027146;
                    }
                }
            }
        }
        double var1;
        if (input[0] >= -0.31628156) {
            if (input[13] >= -0.5) {
                if (input[4] >= 1.0549002) {
                    if (input[0] >= 0.7022924) {
                        var1 = 0.02709447;
                    } else {
                        var1 = -0.013372531;
                    }
                } else {
                    if (input[0] >= 0.29608226) {
                        var1 = 0.05618545;
                    } else {
                        var1 = 0.030756027;
                    }
                }
            } else {
                if (input[4] >= 0.3712256) {
                    if (input[0] >= -0.12433547) {
                        var1 = -0.02326971;
                    } else {
                        var1 = -0.043846425;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var1 = 0.040683817;
                    } else {
                        var1 = 0.0035930374;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[3] >= -0.42304826) {
                        var1 = -0.054659463;
                    } else {
                        var1 = -0.01477933;
                    }
                } else {
                    if (input[0] >= -0.8741734) {
                        var1 = 0.004473643;
                    } else {
                        var1 = -0.0332256;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[0] >= -1.13756) {
                        var1 = -0.0031809928;
                    } else {
                        var1 = -0.06053098;
                    }
                } else {
                    if (input[0] >= -0.7972684) {
                        var1 = 0.021572154;
                    } else {
                        var1 = 0.0;
                    }
                }
            }
        }
        double var2;
        if (input[0] >= -0.4053234) {
            if (input[0] >= 1.17839) {
                if (input[0] >= 2.7646341) {
                    var2 = 0.051456366;
                } else {
                    if (input[1] >= -0.8159811) {
                        var2 = 0.027362822;
                    } else {
                        var2 = 0.057962716;
                    }
                }
            } else {
                if (input[4] >= 0.59911716) {
                    if (input[1] >= -0.8159811) {
                        var2 = -0.011733956;
                    } else {
                        var2 = 0.03705633;
                    }
                } else {
                    if (input[1] >= 0.815915) {
                        var2 = -0.03598883;
                    } else {
                        var2 = 0.023215426;
                    }
                }
            }
        } else {
            if (input[4] >= 1.0549002) {
                if (input[0] >= -1.1138616) {
                    if (input[1] >= -0.8159811) {
                        var2 = -0.048199065;
                    } else {
                        var2 = -0.009274079;
                    }
                } else {
                    var2 = 0.0;
                }
            } else {
                if (input[0] >= -0.8276392) {
                    if (input[1] >= 1.9018891) {
                        var2 = -0.084019125;
                    } else {
                        var2 = -0.00022610933;
                    }
                } else {
                    if (input[4] >= -1.6797982) {
                        var2 = -0.029786518;
                    } else {
                        var2 = 0.012504144;
                    }
                }
            }
        }
        double var3;
        if (input[1] >= -1.3599465) {
            if (input[0] >= 0.1188039) {
                if (input[1] >= 0.815915) {
                    if (input[4] >= 0.14333406) {
                        var3 = -0.056885105;
                    } else {
                        var3 = 0.0036205829;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var3 = -0.0013903168;
                    } else {
                        var3 = 0.015619044;
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var3 = -0.034804918;
                    } else {
                        var3 = -0.011236131;
                    }
                } else {
                    if (input[1] >= 1.9018891) {
                        var3 = -0.053893827;
                    } else {
                        var3 = -0.0036080636;
                    }
                }
            }
        } else {
            if (input[0] >= -0.90673006) {
                if (input[10] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var3 = 0.025076004;
                    } else {
                        var3 = -0.0111059565;
                    }
                } else {
                    if (input[4] >= 1.2827917) {
                        var3 = 0.0;
                    } else {
                        var3 = 0.043416068;
                    }
                }
            } else {
                if (input[4] >= -0.54034054) {
                    var3 = -0.016651165;
                } else {
                    if (input[0] >= -1.0775084) {
                        var3 = 0.0;
                    } else {
                        var3 = 0.06372058;
                    }
                }
            }
        }
        double var4;
        if (input[0] >= -0.5491247) {
            if (input[1] >= 1.9018891) {
                if (input[0] >= 0.17511652) {
                    if (input[4] >= 0.029388294) {
                        var4 = -0.037035447;
                    } else {
                        var4 = 0.0048056273;
                    }
                } else {
                    var4 = -0.045184795;
                }
            } else {
                if (input[4] >= -0.9961236) {
                    if (input[1] >= -1.3599465) {
                        var4 = 0.0020904546;
                    } else {
                        var4 = 0.014702606;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var4 = 0.02414328;
                    } else {
                        var4 = 0.0021657785;
                    }
                }
            }
        } else {
            if (input[4] >= -0.08455747) {
                if (input[1] >= -0.8159811) {
                    if (input[0] >= -1.2680166) {
                        var4 = -0.023969335;
                    } else {
                        var4 = 0.0;
                    }
                } else {
                    if (input[3] >= 0.59700394) {
                        var4 = -0.018654618;
                    } else {
                        var4 = 0.0038141233;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[13] >= 1.5) {
                        var4 = 0.0752051;
                    } else {
                        var4 = 0.0022844395;
                    }
                } else {
                    if (input[0] >= -0.89281005) {
                        var4 = -0.0015232898;
                    } else {
                        var4 = -0.039258547;
                    }
                }
            }
        }
        double var5;
        if (input[0] >= -0.7765035) {
            if (input[4] >= 0.59911716) {
                if (input[12] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var5 = 0.009015309;
                    } else {
                        var5 = -0.0048238;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var5 = -0.0032266492;
                    } else {
                        var5 = -0.01929081;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var5 = 0.0098273;
                    } else {
                        var5 = 0.0024667524;
                    }
                } else {
                    if (input[4] >= -0.54034054) {
                        var5 = -0.0113310395;
                    } else {
                        var5 = 0.0;
                    }
                }
            }
        } else {
            if (input[4] >= -0.7682321) {
                if (input[4] >= 0.59911716) {
                    var5 = -0.02364062;
                } else {
                    var5 = -0.012452081;
                }
            } else {
                if (input[4] >= -1.9076898) {
                    if (input[13] >= 0.5) {
                        var5 = 0.007848122;
                    } else {
                        var5 = -0.0073744804;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var5 = 0.040147293;
                    } else {
                        var5 = 0.0;
                    }
                }
            }
        }
        double var6;
        if (input[1] >= 0.815915) {
            if (input[8] >= 0.5) {
                if (input[0] >= 0.46962175) {
                    var6 = 0.012525513;
                } else {
                    var6 = -0.012558079;
                }
            } else {
                if (input[0] >= -1.1197286) {
                    if (input[0] >= 2.0185351) {
                        var6 = 0.0;
                    } else {
                        var6 = -0.029225264;
                    }
                } else {
                    var6 = 0.0;
                }
            }
        } else {
            if (input[0] >= -0.93060106) {
                if (input[13] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var6 = -0.016757203;
                    } else {
                        var6 = -0.0015721841;
                    }
                } else {
                    if (input[4] >= -0.54034054) {
                        var6 = 0.001991249;
                    } else {
                        var6 = 0.009226198;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var6 = 0.022477696;
                    } else {
                        var6 = -0.0064138677;
                    }
                } else {
                    if (input[2] >= -1.3740458) {
                        var6 = -0.019335784;
                    } else {
                        var6 = 0.011705646;
                    }
                }
            }
        }
        double var7;
        if (input[1] >= -1.3599465) {
            if (input[10] >= 0.5) {
                if (input[0] >= 1.032978) {
                    if (input[1] >= 0.815915) {
                        var7 = -0.0067937407;
                    } else {
                        var7 = 0.008048057;
                    }
                } else {
                    if (input[3] >= 1.6170561) {
                        var7 = 0.0057743383;
                    } else {
                        var7 = -0.011110299;
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var7 = -0.008061428;
                    } else {
                        var7 = 0.0033060298;
                    }
                } else {
                    if (input[0] >= -0.8431698) {
                        var7 = 0.002789319;
                    } else {
                        var7 = -0.00843837;
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[0] >= 0.7110355) {
                    var7 = 0.014519562;
                } else {
                    if (input[0] >= -1.0971806) {
                        var7 = -0.0034323188;
                    } else {
                        var7 = 0.04553311;
                    }
                }
            } else {
                if (input[0] >= -1.1303699) {
                    var7 = 0.013361479;
                } else {
                    var7 = -0.0067050634;
                }
            }
        }
        double var8;
        if (input[4] >= 1.5106833) {
            var8 = -0.0107417945;
        } else {
            if (input[4] >= -0.54034054) {
                if (input[13] >= -0.5) {
                    if (input[4] >= -0.312449) {
                        var8 = 0.0034241104;
                    } else {
                        var8 = -0.01396882;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var8 = -0.016821267;
                    } else {
                        var8 = 0.0;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[9] >= 0.5) {
                        var8 = 0.0036452485;
                    } else {
                        var8 = -0.0032509796;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var8 = -0.0016765237;
                    } else {
                        var8 = 0.02119612;
                    }
                }
            }
        }
        double var9;
        if (input[0] >= 1.0283189) {
            if (input[6] >= 0.5) {
                if (input[5] >= 0.5) {
                    var9 = 0.0056427987;
                } else {
                    if (input[11] >= 0.5) {
                        var9 = -0.016077224;
                    } else {
                        var9 = 0.0;
                    }
                }
            } else {
                var9 = 0.01010602;
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[13] >= -1.5) {
                    if (input[0] >= -1.2539241) {
                        var9 = -0.00036556588;
                    } else {
                        var9 = 0.014990362;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var9 = -0.008582896;
                    } else {
                        var9 = 0.0061845053;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var9 = -0.023744458;
                    } else {
                        var9 = -0.006563261;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var9 = 0.023096181;
                    } else {
                        var9 = 0.000060549068;
                    }
                }
            }
        }
        double var10;
        if (input[1] >= 0.815915) {
            if (input[3] >= 0.59700394) {
                var10 = 0.0;
            } else {
                var10 = -0.012502556;
            }
        } else {
            if (input[0] >= -1.1492941) {
                if (input[0] >= -1.1087997) {
                    if (input[3] >= 0.59700394) {
                        var10 = 0.0032620477;
                    } else {
                        var10 = 0.0;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var10 = 0.0;
                    } else {
                        var10 = 0.038789637;
                    }
                }
            } else {
                if (input[0] >= -1.2357476) {
                    if (input[13] >= 0.5) {
                        var10 = 0.0;
                    } else {
                        var10 = -0.032159526;
                    }
                } else {
                    var10 = 0.0032687003;
                }
            }
        }
        double var11;
        if (input[0] >= 2.145483) {
            var11 = 0.008928878;
        } else {
            if (input[12] >= 0.5) {
                if (input[3] >= 0.59700394) {
                    if (input[4] >= 0.3712256) {
                        var11 = -0.0036463249;
                    } else {
                        var11 = 0.012925497;
                    }
                } else {
                    if (input[0] >= -1.1090298) {
                        var11 = -0.0010116892;
                    } else {
                        var11 = 0.015117263;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var11 = -0.0022269012;
                    } else {
                        var11 = 0.006304135;
                    }
                } else {
                    if (input[0] >= 0.29671496) {
                        var11 = 0.0030651372;
                    } else {
                        var11 = -0.008171257;
                    }
                }
            }
        }
        double var12;
        if (input[1] >= -1.3599465) {
            if (input[8] >= 0.5) {
                if (input[0] >= -1.0413857) {
                    if (input[3] >= -0.42304826) {
                        var12 = 0.003173658;
                    } else {
                        var12 = -0.014153413;
                    }
                } else {
                    if (input[0] >= -1.1323831) {
                        var12 = -0.035691578;
                    } else {
                        var12 = 0.0;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[4] >= 0.59911716) {
                        var12 = -0.011996664;
                    } else {
                        var12 = 0.0016237525;
                    }
                } else {
                    if (input[4] >= 0.3712256) {
                        var12 = -0.011159485;
                    } else {
                        var12 = -0.00041155014;
                    }
                }
            }
        } else {
            if (input[0] >= -0.89654887) {
                if (input[0] >= -0.81136096) {
                    if (input[0] >= 0.08595967) {
                        var12 = 0.0070070177;
                    } else {
                        var12 = 0.000010880499;
                    }
                } else {
                    if (input[4] >= 0.3712256) {
                        var12 = 0.0;
                    } else {
                        var12 = 0.037796747;
                    }
                }
            } else {
                if (input[0] >= -1.0978134) {
                    var12 = -0.007793947;
                } else {
                    if (input[0] >= -1.1461306) {
                        var12 = 0.029682375;
                    } else {
                        var12 = 0.0;
                    }
                }
            }
        }
        double var13;
        if (input[0] >= -0.18916112) {
            if (input[4] >= 0.3712256) {
                if (input[0] >= -0.053125024) {
                    if (input[1] >= 1.9018891) {
                        var13 = -0.01650922;
                    } else {
                        var13 = 0.0020702286;
                    }
                } else {
                    var13 = -0.00927764;
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -0.9961236) {
                        var13 = -0.012918497;
                    } else {
                        var13 = 0.001291424;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var13 = -0.0024110004;
                    } else {
                        var13 = 0.019455008;
                    }
                }
            }
        } else {
            if (input[4] >= 0.59911716) {
                if (input[0] >= -0.9077654) {
                    var13 = -0.001231096;
                } else {
                    var13 = -0.015027608;
                }
            } else {
                if (input[0] >= -1.0626683) {
                    if (input[13] >= -0.5) {
                        var13 = 0.006788305;
                    } else {
                        var13 = 0.0;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var13 = 0.009579637;
                    } else {
                        var13 = -0.010352489;
                    }
                }
            }
        }
        double var14;
        if (input[1] >= 0.815915) {
            if (input[3] >= -0.42304826) {
                if (input[4] >= 0.59911716) {
                    var14 = -0.009292987;
                } else {
                    if (input[0] >= 0.36355385) {
                        var14 = 0.0151312705;
                    } else {
                        var14 = -0.0025033564;
                    }
                }
            } else {
                var14 = -0.02148139;
            }
        } else {
            if (input[4] >= -1.6797982) {
                if (input[2] >= -2.8848906) {
                    if (input[1] >= -1.3599465) {
                        var14 = -0.000235066;
                    } else {
                        var14 = 0.0029681602;
                    }
                } else {
                    if (input[4] >= -0.7682321) {
                        var14 = 0.0;
                    } else {
                        var14 = -0.027235057;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var14 = 0.0;
                    } else {
                        var14 = 0.021192512;
                    }
                } else {
                    if (input[2] >= -1.3740458) {
                        var14 = -0.008649706;
                    } else {
                        var14 = 0.012276167;
                    }
                }
            }
        }
        double var15;
        if (input[3] >= 1.6170561) {
            if (input[4] >= 0.82700866) {
                if (input[12] >= 0.5) {
                    if (input[0] >= -0.23477493) {
                        var15 = 0.002657212;
                    } else {
                        var15 = -0.008385643;
                    }
                } else {
                    var15 = 0.0029613855;
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[0] >= -0.268597) {
                        var15 = 0.0;
                    } else {
                        var15 = 0.033220284;
                    }
                } else {
                    if (input[0] >= -0.53485966) {
                        var15 = 0.01503703;
                    } else {
                        var15 = -0.022285087;
                    }
                }
            }
        } else {
            if (input[0] >= 2.5343218) {
                var15 = 0.006219776;
            } else {
                if (input[4] >= 1.5106833) {
                    if (input[10] >= 0.5) {
                        var15 = 0.0;
                    } else {
                        var15 = -0.014271205;
                    }
                } else {
                    if (input[0] >= -0.1851347) {
                        var15 = -0.0018547821;
                    } else {
                        var15 = 0.00053511304;
                    }
                }
            }
        }
        double var16;
        if (input[1] >= 2.987863) {
            var16 = -0.024698442;
        } else {
            if (input[0] >= -1.2194693) {
                if (input[0] >= -0.93060106) {
                    var16 = 0.00033383848;
                } else {
                    if (input[3] >= 1.6170561) {
                        var16 = 0.005721742;
                    } else {
                        var16 = -0.004121052;
                    }
                }
            } else {
                if (input[3] >= -0.42304826) {
                    if (input[4] >= -0.9961236) {
                        var16 = 0.02919372;
                    } else {
                        var16 = 0.0;
                    }
                } else {
                    var16 = 0.0;
                }
            }
        }
        double var17;
        if (input[0] >= 0.9651038) {
            var17 = 0.0029100361;
        } else {
            if (input[0] >= 0.50954103) {
                if (input[1] >= -0.8159811) {
                    if (input[6] >= 0.5) {
                        var17 = -0.011561808;
                    } else {
                        var17 = 0.0;
                    }
                } else {
                    var17 = 0.00062538707;
                }
            } else {
                if (input[1] >= -1.3599465) {
                    if (input[6] >= 0.5) {
                        var17 = 0.00060313684;
                    } else {
                        var17 = -0.002821201;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var17 = 0.0;
                    } else {
                        var17 = 0.0063120164;
                    }
                }
            }
        }
        double var18;
        if (input[4] >= 1.7385747) {
            var18 = -0.0039201863;
        } else {
            if (input[13] >= -1.5) {
                if (input[3] >= 0.59700394) {
                    if (input[2] >= 0.13679874) {
                        var18 = 0.0059075286;
                    } else {
                        var18 = 0.0;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var18 = 0.00074355985;
                    } else {
                        var18 = -0.0033155004;
                    }
                }
            } else {
                var18 = -0.0040286873;
            }
        }
        double var19;
        if (input[4] >= -0.7682321) {
            if (input[12] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var19 = 0.0029154844;
                    } else {
                        var19 = -0.0005763117;
                    }
                } else {
                    if (input[4] >= 0.14333406) {
                        var19 = -0.020601235;
                    } else {
                        var19 = 0.0;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[3] >= -0.42304826) {
                        var19 = -0.0035387932;
                    } else {
                        var19 = -0.021177646;
                    }
                } else {
                    if (input[1] >= -1.3599465) {
                        var19 = -0.0033716615;
                    } else {
                        var19 = 0.0005439212;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[5] >= 0.5) {
                    var19 = -0.0011243206;
                } else {
                    if (input[2] >= -2.8848906) {
                        var19 = 0.006373667;
                    } else {
                        var19 = -0.0038904496;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[2] >= -1.3740458) {
                        var19 = -0.0003481494;
                    } else {
                        var19 = 0.017374424;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var19 = 0.0030225108;
                    } else {
                        var19 = -0.02074998;
                    }
                }
            }
        }
        double var20;
        if (input[1] >= -1.3599465) {
            if (input[0] >= 1.5535965) {
                var20 = 0.0021352612;
            } else {
                if (input[0] >= 0.4733606) {
                    if (input[0] >= 0.66386867) {
                        var20 = -0.0010665552;
                    } else {
                        var20 = -0.009997006;
                    }
                } else {
                    var20 = -0.00050490594;
                }
            }
        } else {
            if (input[0] >= 1.5186815) {
                var20 = -0.0067281285;
            } else {
                if (input[9] >= 0.5) {
                    if (input[0] >= -0.8613463) {
                        var20 = 0.0023288596;
                    } else {
                        var20 = 0.01411553;
                    }
                } else {
                    if (input[4] >= -0.7682321) {
                        var20 = -0.0077248975;
                    } else {
                        var20 = 0.009636239;
                    }
                }
            }
        }
        double var21;
        if (input[4] >= -1.6797982) {
            if (input[13] >= -1.5) {
                if (input[3] >= 1.6170561) {
                    if (input[2] >= 0.13679874) {
                        var21 = 0.009573484;
                    } else {
                        var21 = 0.0;
                    }
                } else {
                    var21 = 0.00006518043;
                }
            } else {
                var21 = -0.0043071094;
            }
        } else {
            if (input[4] >= -1.9076898) {
                if (input[2] >= -2.8848906) {
                    if (input[5] >= 0.5) {
                        var21 = 0.0;
                    } else {
                        var21 = 0.030569553;
                    }
                } else {
                    var21 = 0.0;
                }
            } else {
                if (input[2] >= -2.8848906) {
                    if (input[1] >= 0.815915) {
                        var21 = -0.04040078;
                    } else {
                        var21 = 0.0;
                    }
                } else {
                    if (input[0] >= -1.0305142) {
                        var21 = 0.0;
                    } else {
                        var21 = 0.04437657;
                    }
                }
            }
        }
        double var22;
        if (input[0] >= -0.30955166) {
            if (input[0] >= 0.04782354) {
                var22 = 0.0003347591;
            } else {
                if (input[0] >= 0.003820321) {
                    var22 = -0.013791659;
                } else {
                    var22 = -0.0024036793;
                }
            }
        } else {
            if (input[0] >= -0.43822512) {
                if (input[13] >= -0.5) {
                    if (input[12] >= 0.5) {
                        var22 = 0.01980261;
                    } else {
                        var22 = 0.0022518686;
                    }
                } else {
                    if (input[4] >= 0.3712256) {
                        var22 = -0.007226361;
                    } else {
                        var22 = 0.0046313955;
                    }
                }
            } else {
                if (input[0] >= -0.44460988) {
                    var22 = -0.017412832;
                } else {
                    if (input[6] >= 0.5) {
                        var22 = 0.0009489473;
                    } else {
                        var22 = -0.0024808373;
                    }
                }
            }
        }
        double var23;
        if (input[0] >= -1.0317221) {
            if (input[0] >= -0.94659173) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -0.8700894) {
                        var23 = 0.0;
                    } else {
                        var23 = 0.005835842;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var23 = -0.011400672;
                    } else {
                        var23 = -0.0003028639;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    var23 = 0.0;
                } else {
                    if (input[4] >= 0.82700866) {
                        var23 = 0.0;
                    } else {
                        var23 = -0.022405645;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0566286) {
                if (input[9] >= 0.5) {
                    if (input[11] >= 0.5) {
                        var23 = 0.0;
                    } else {
                        var23 = 0.03439975;
                    }
                } else {
                    var23 = 0.0;
                }
            } else {
                if (input[3] >= -0.42304826) {
                    if (input[2] >= -1.3740458) {
                        var23 = 0.0;
                    } else {
                        var23 = 0.024976192;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var23 = 0.0034575192;
                    } else {
                        var23 = -0.01216727;
                    }
                }
            }
        }
        double var24;
        if (input[2] >= 0.13679874) {
            if (input[0] >= -0.9789183) {
                if (input[3] >= 1.6170561) {
                    var24 = 0.004242736;
                } else {
                    if (input[13] >= -0.5) {
                        var24 = -0.00043313828;
                    } else {
                        var24 = -0.0057176463;
                    }
                }
            } else {
                if (input[3] >= -0.42304826) {
                    if (input[12] >= 0.5) {
                        var24 = 0.0;
                    } else {
                        var24 = 0.023512993;
                    }
                } else {
                    var24 = 0.0;
                }
            }
        } else {
            if (input[0] >= -0.6696878) {
                if (input[0] >= -0.62579966) {
                    if (input[13] >= -0.5) {
                        var24 = 0.00003877023;
                    } else {
                        var24 = 0.004008682;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var24 = 0.0;
                    } else {
                        var24 = 0.024858354;
                    }
                }
            } else {
                if (input[0] >= -0.727611) {
                    var24 = -0.008389792;
                } else {
                    var24 = 0.00027786585;
                }
            }
        }
        double var25;
        if (input[0] >= -0.896894) {
            if (input[0] >= -0.8736557) {
                if (input[1] >= 1.9018891) {
                    var25 = -0.0021557768;
                } else {
                    if (input[0] >= -0.47756916) {
                        var25 = 0.0011867295;
                    } else {
                        var25 = -0.0011927321;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[8] >= 0.5) {
                        var25 = 0.0;
                    } else {
                        var25 = 0.027444318;
                    }
                } else {
                    var25 = 0.0;
                }
            }
        } else {
            if (input[0] >= -1.0213685) {
                if (input[13] >= -0.5) {
                    if (input[0] >= -0.9376761) {
                        var25 = 0.0;
                    } else {
                        var25 = -0.015438838;
                    }
                } else {
                    var25 = 0.0;
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[0] >= -1.0626683) {
                        var25 = 0.02407604;
                    } else {
                        var25 = 0.0;
                    }
                } else {
                    if (input[0] >= -1.2865958) {
                        var25 = -0.003734924;
                    } else {
                        var25 = 0.009176313;
                    }
                }
            }
        }
        double var26;
        if (input[0] >= -0.18686031) {
            if (input[0] >= -0.05427544) {
                var26 = -0.00043309305;
            } else {
                if (input[0] >= -0.10828723) {
                    if (input[3] >= 0.59700394) {
                        var26 = 0.0;
                    } else {
                        var26 = -0.018202767;
                    }
                } else {
                    if (input[3] >= 0.59700394) {
                        var26 = -0.00963576;
                    } else {
                        var26 = 0.0004999104;
                    }
                }
            }
        } else {
            if (input[4] >= 0.82700866) {
                if (input[0] >= -0.73117733) {
                    if (input[0] >= -0.60848594) {
                        var26 = -0.0031602827;
                    } else {
                        var26 = 0.0039789006;
                    }
                } else {
                    var26 = -0.005904702;
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var26 = -0.0028417227;
                    } else {
                        var26 = 0.0037819787;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var26 = -0.004197456;
                    } else {
                        var26 = 0.0037399814;
                    }
                }
            }
        }
        double var27;
        if (input[6] >= 0.5) {
            if (input[2] >= 0.13679874) {
                if (input[4] >= -0.312449) {
                    var27 = 0.0010385126;
                } else {
                    if (input[0] >= -0.13181314) {
                        var27 = -0.0140458355;
                    } else {
                        var27 = 0.0024815197;
                    }
                }
            } else {
                if (input[0] >= -0.88774824) {
                    if (input[0] >= 0.54157996) {
                        var27 = -0.0040947045;
                    } else {
                        var27 = 0.0;
                    }
                } else {
                    if (input[4] >= -0.7682321) {
                        var27 = -0.018991543;
                    } else {
                        var27 = 0.0;
                    }
                }
            }
        } else {
            if (input[2] >= 0.13679874) {
                if (input[0] >= -1.0134882) {
                    if (input[0] >= 0.03166027) {
                        var27 = 0.0;
                    } else {
                        var27 = -0.019020448;
                    }
                } else {
                    if (input[0] >= -1.11668) {
                        var27 = 0.043069042;
                    } else {
                        var27 = -0.0052575315;
                    }
                }
            } else {
                if (input[4] >= -0.9961236) {
                    if (input[0] >= -1.1186932) {
                        var27 = 0.010951615;
                    } else {
                        var27 = 0.0;
                    }
                } else {
                    var27 = 0.0009990524;
                }
            }
        }
        double var28;
        if (input[4] >= -1.6797982) {
            if (input[5] >= 0.5) {
                if (input[0] >= -1.1199586) {
                    if (input[0] >= -1.0971806) {
                        var28 = 0.000043378997;
                    } else {
                        var28 = 0.04829652;
                    }
                } else {
                    if (input[0] >= -1.1857047) {
                        var28 = -0.018238626;
                    } else {
                        var28 = 0.0;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -0.54034054) {
                        var28 = -0.0051829545;
                    } else {
                        var28 = 0.0018612961;
                    }
                } else {
                    if (input[2] >= -1.3740458) {
                        var28 = -0.0031733203;
                    } else {
                        var28 = -0.04174672;
                    }
                }
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[4] >= -1.9076898) {
                    var28 = 0.0;
                } else {
                    if (input[12] >= 0.5) {
                        var28 = -0.031303264;
                    } else {
                        var28 = 0.0;
                    }
                }
            } else {
                if (input[0] >= -1.0133157) {
                    if (input[2] >= 0.13679874) {
                        var28 = -0.0024392405;
                    } else {
                        var28 = 0.016774917;
                    }
                } else {
                    var28 = 0.0;
                }
            }
        }
        double var29;
        if (input[0] >= -1.0624957) {
            if (input[4] >= -1.9076898) {
                if (input[0] >= -1.0317221) {
                    var29 = 0.00024884494;
                } else {
                    if (input[12] >= 0.5) {
                        var29 = 0.0;
                    } else {
                        var29 = 0.018248256;
                    }
                }
            } else {
                if (input[2] >= -2.8848906) {
                    if (input[0] >= -0.087982476) {
                        var29 = 0.0;
                    } else {
                        var29 = -0.016361184;
                    }
                } else {
                    var29 = 0.0;
                }
            }
        } else {
            if (input[0] >= -1.0962027) {
                var29 = -0.022251539;
            } else {
                if (input[0] >= -1.1201887) {
                    if (input[13] >= 0.5) {
                        var29 = 0.035314206;
                    } else {
                        var29 = 0.0;
                    }
                } else {
                    if (input[0] >= -1.1291045) {
                        var29 = -0.019847313;
                    } else {
                        var29 = 0.001390385;
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
