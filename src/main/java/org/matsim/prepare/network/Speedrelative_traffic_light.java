package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class Speedrelative_traffic_light implements FeatureRegressor {
    
    public static Speedrelative_traffic_light INSTANCE = new Speedrelative_traffic_light();
    public static final double[] DEFAULT_PARAMS = {0.02672967, 0.081900865, 0.053034972, 0.08446151, 0.03276493, 0.071407326, -0.023928678, 0.03410692, -0.042862535, 0.007113333, 0.0023266017, 0.042514097, -0.058245804, -0.013689547, 0.0049403505, -0.021806315, 0.039555844, 0.012205355, 0.055683292, 0.040495906, -0.018197138, 0.012440386, 0.032121252, 0.008326337, -0.042096734, -0.020887302, 0.0044887, -0.035267785, 0.010093981, -0.016060995, 0.022857327, -0.0062540593, 0.017679192, 0.04323236, -0.009998074, 0.036675263, -0.034326367, -0.0027236857, -0.029326094, 0.023139749, -0.02400434, 0.009349212, -0.06401877, 0.00803626, -0.03857581, -0.011204823, -0.05182662, -0.01122764, -0.04997249, 0.0016140289, 0.01590757, 0.03240009, -0.048695743, -0.025288235, -0.015816864, -0.0008354368, 0.028008368, 0.04024639, 0.00086700777, 0.025649697, -0.017507378, 0.015326313, 0.041393295, -0.045492128, -0.0139780585, 0.018288834, -0.007137278, 0.019313008, 0.006469012, 0.0047062146, 0.023600647, -0.010110819, -0.023493957, 0.010161423, -0.015696606, -0.03562217, 0.006692773, -0.018435616, 0.0038985629, -0.016864264, -0.001187908, 0.011493373, 0.0018385139, -0.016310629, 0.0013226686, -0.027069269, -0.011383642, -0.023405619, -0.009091406, 0.0012356937, -0.02051276, -0.0016820772, -0.037399866, 0.013381466, -0.03972806, 0.006310406, -0.004882459, -0.00018265756, 0.014005854, -0.0049224123, -0.019507458, -0.010337537, 0.019420264, -0.043424454, 0.01803482, -0.012511946, 0.029618748, -0.00330452, -0.024241628, 0.020220226, -0.0012113304, 0.0060021942, -0.010335434, -0.022662979, 0.008070686, -0.007909735, 0.0061049, -0.00754677, 0.009248198, 0.00042798033, -0.016113643, -0.004144019, -0.015797436, -0.036987927, 0.0052333786, -0.007865147, -0.00038512584, -0.019125879, 0.006921055, -0.004053284, 0.036284827, 0.013979893, -0.00031446968, -0.0014063935, -0.020877214, 0.008057659, -0.00344305, 0.0037369246, 0.030612065, -0.0050245672, 0.013907017, 0.014756328, -0.010173623, -0.011990903, 0.016641164, -0.023504745, 0.010938932, -0.04214916, 0.017000519, -0.031579223, -0.014754095, -0.0056909057, 0.0115929125, 0.0033608908, -0.0012129244, -0.006745956, 0.0005271181, 0.024115793, -0.002309813, -0.019186174, 0.004995935, -0.0032075713, -0.018796884, -0.005572311, -0.012635144, 0.014109028, 0.001250176, 0.0057716053, -0.020501874, -0.0006526424, 0.008763132, 0.012876768, -0.008461759, 0.004772153, -0.01012048, -0.0018596641, 0.0, 0.022573654, -0.01835205, 0.0046745646, 0.0, -0.03536958, 0.008444654, 0.0017268299, -0.011742718, 0.006219295, 0.015834937, -0.014433121, -0.0054902495, 0.012779403, -0.009488669, 0.00030039944, 0.00042476412, 0.0054790135, -0.0031896585, -0.021349462, 0.011214811, 0.036101967, -0.011064748, 0.00026978576, 0.020780737, 0.0021785772, 0.0032453018, -0.0124553535, 0.0061376505, -0.021886118, -0.009512777, 0.015981643, -0.008499147, -0.0005367195, -0.011721236, 0.0020560187, 0.017658614, 0.0069867484, -0.009386386, -0.001393256, -0.006149292, 0.008796969, 0.00023590519, -0.005488677, 0.011950189, -0.04180444, -0.010375813, -0.0049798763, 0.019866657, -0.0043466855, 0.00602803, 0.02591866, 0.019801212, -0.0017350526, 0.0052215727, -0.00015579026, -0.007276478, 0.011183358, 0.0064067603, 0.0061288537, -0.0071214447, 0.0020716758, -0.01603627, 0.0076239635, -0.017927235, 0.014956552, 0.010882739, -0.011055113, 0.01847202, -0.015520886, 0.005722581, -0.00868443, -0.010827428, 0.006549411, -0.014514054, -0.0024962898, 0.00061903097, -0.003289423, 0.003863995, -0.0015401262, 0.005169729, -0.00041814084, -0.007748396, 0.0011531163, 0.012569849, 0.016971465, 0.0018450491, -0.0008069037, -0.004156409, -0.024098692, -0.008196066, 0.0009979605, -0.032654915, 0.007038396, -0.017315391, -0.0065580253, 0.0149074355, 0.02341699, -0.0004702245, -0.018915761, 0.002015762, 5.8807153e-05, -0.010585102, 0.041828405, 0.0, -0.0355101, -0.010269751, 0.014998806, 0.018291168, -0.0028743467, 0.0174826, -0.0004413311, -0.017520329, -0.00425402, -0.019582683, 0.004545165, -0.01844734, 0.0005263567, 0.007093741, 0.0023567427, -0.0038803825, 0.016031163, -0.00978845, 0.014525503, 0.0007352337, -0.0077979243, 0.0023218181, -0.017925201, 0.0012097007, -0.0006081959, -0.00069398317, -0.03405387, -0.0055578984, 0.0010618606, -0.005371819, -0.023276644, -0.02600419, -0.002633595, -0.00010532885, -0.0028377676, 0.012748503, -0.00203033, -0.017263561, 0.0005562401, -0.017590124, 0.0006730753, 0.004691983, 0.012806608, -0.02300126, -0.0036971956, 0.0024646786, -0.0027914767, 0.0048576733, 0.0020022457, -0.0036141973, 0.00035840302, -0.0017047703, -0.008714578, -0.00038072825, -0.01033011, 0.00611105, -0.008052845, 0.0018109282, 0.0066358177, 0.03232777, -0.009639772, 0.021862363, -0.017429113, -0.01042969, -0.0016700295, 0.0001407893, -0.0031154866, 0.0017336892, -0.008227171, 0.007140533, -0.012689197, 0.0051840413, -0.0012753225, -0.0076941233, 0.005221435, 0.03517991, 0.008712071, -0.024997069, 0.0131085245, 0.0005197675, -0.0002524971, -0.016701791, 0.020968925, -0.0039766366, -0.03530954, 0.024028504, 0.016613068, -0.0052661067, 0.05225834, 9.833425e-05, 0.008169875, -0.0008635007, 0.005399532, -0.014806244, 0.0015388669, -0.010500554, -0.020747289, -0.0069364654, 0.002613798, 0.00013274471, 0.026170686, -0.029559238, 0.005938267, -0.006929883, 0.0002942946, 0.00029299964, -0.0084923105, 0.0016527828, 0.028015409, -0.016085954, 0.006183133, 0.033776708, 0.004530843, -0.011248405, 0.0039703366, 0.0019399134, -0.0075180284};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
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

        return data;
    }
    
    @Override
    public double predict(Object2DoubleMap<String> ft, double[] params) {

        double[] data = getData(ft);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);
    
        return score(data, params);
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[0] >= -0.12318505) {
            if (input[0] >= 0.6606475) {
                if (input[0] >= 1.4716873) {
                    if (input[1] >= 0.815915) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[1] >= -0.27201572) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[1] >= -1.3599465) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    if (input[4] >= 0.3712256) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                }
            }
        } else {
            if (input[0] >= -0.58559275) {
                if (input[3] >= -0.42304826) {
                    if (input[4] >= 0.14333406) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                } else {
                    if (input[1] >= -0.8159811) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[1] >= -0.8159811) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                } else {
                    if (input[0] >= -0.79623306) {
                        var0 = params[14];
                    } else {
                        var0 = params[15];
                    }
                }
            }
        }
        double var1;
        if (input[0] >= -0.21590818) {
            if (input[0] >= 0.44782144) {
                if (input[4] >= 1.0549002) {
                    if (input[0] >= 1.5108013) {
                        var1 = params[16];
                    } else {
                        var1 = params[17];
                    }
                } else {
                    if (input[0] >= 1.17839) {
                        var1 = params[18];
                    } else {
                        var1 = params[19];
                    }
                }
            } else {
                if (input[4] >= 0.59911716) {
                    if (input[3] >= 0.59700394) {
                        var1 = params[20];
                    } else {
                        var1 = params[21];
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var1 = params[22];
                    } else {
                        var1 = params[23];
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[3] >= -0.42304826) {
                    if (input[4] >= 0.14333406) {
                        var1 = params[24];
                    } else {
                        var1 = params[25];
                    }
                } else {
                    if (input[0] >= -0.8024453) {
                        var1 = params[26];
                    } else {
                        var1 = params[27];
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[0] >= -0.67089576) {
                        var1 = params[28];
                    } else {
                        var1 = params[29];
                    }
                } else {
                    if (input[0] >= -0.7430841) {
                        var1 = params[30];
                    } else {
                        var1 = params[31];
                    }
                }
            }
        }
        double var2;
        if (input[0] >= -0.07365987) {
            if (input[0] >= 0.4490294) {
                if (input[4] >= 0.59911716) {
                    if (input[1] >= -0.8159811) {
                        var2 = params[32];
                    } else {
                        var2 = params[33];
                    }
                } else {
                    if (input[1] >= 0.815915) {
                        var2 = params[34];
                    } else {
                        var2 = params[35];
                    }
                }
            } else {
                if (input[4] >= 1.0549002) {
                    if (input[4] >= 1.9664664) {
                        var2 = params[36];
                    } else {
                        var2 = params[37];
                    }
                } else {
                    if (input[1] >= 0.815915) {
                        var2 = params[38];
                    } else {
                        var2 = params[39];
                    }
                }
            }
        } else {
            if (input[0] >= -0.67336917) {
                if (input[4] >= 0.59911716) {
                    if (input[1] >= -0.8159811) {
                        var2 = params[40];
                    } else {
                        var2 = params[41];
                    }
                } else {
                    if (input[1] >= 1.9018891) {
                        var2 = params[42];
                    } else {
                        var2 = params[43];
                    }
                }
            } else {
                if (input[4] >= -0.08455747) {
                    if (input[1] >= -0.8159811) {
                        var2 = params[44];
                    } else {
                        var2 = params[45];
                    }
                } else {
                    if (input[1] >= 0.815915) {
                        var2 = params[46];
                    } else {
                        var2 = params[47];
                    }
                }
            }
        }
        double var3;
        if (input[1] >= -1.3599465) {
            if (input[0] >= 0.12116225) {
                if (input[1] >= 0.815915) {
                    if (input[4] >= 0.3712256) {
                        var3 = params[48];
                    } else {
                        var3 = params[49];
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var3 = params[50];
                    } else {
                        var3 = params[51];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[1] >= 0.815915) {
                        var3 = params[52];
                    } else {
                        var3 = params[53];
                    }
                } else {
                    if (input[4] >= 0.3712256) {
                        var3 = params[54];
                    } else {
                        var3 = params[55];
                    }
                }
            }
        } else {
            if (input[0] >= -0.8852173) {
                if (input[0] >= -0.4262033) {
                    if (input[10] >= 0.5) {
                        var3 = params[56];
                    } else {
                        var3 = params[57];
                    }
                } else {
                    if (input[4] >= 0.59911716) {
                        var3 = params[58];
                    } else {
                        var3 = params[59];
                    }
                }
            } else {
                if (input[4] >= -1.2240152) {
                    if (input[5] >= 0.5) {
                        var3 = params[60];
                    } else {
                        var3 = params[61];
                    }
                } else {
                    var3 = params[62];
                }
            }
        }
        double var4;
        if (input[0] >= -0.4053234) {
            if (input[1] >= 0.815915) {
                if (input[6] >= 0.5) {
                    if (input[1] >= 1.9018891) {
                        var4 = params[63];
                    } else {
                        var4 = params[64];
                    }
                } else {
                    if (input[4] >= -0.7682321) {
                        var4 = params[65];
                    } else {
                        var4 = params[66];
                    }
                }
            } else {
                if (input[1] >= -1.3599465) {
                    if (input[0] >= 1.0439644) {
                        var4 = params[67];
                    } else {
                        var4 = params[68];
                    }
                } else {
                    if (input[13] >= 1.5) {
                        var4 = params[69];
                    } else {
                        var4 = params[70];
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[1] >= -0.27201572) {
                    if (input[12] >= 0.5) {
                        var4 = params[71];
                    } else {
                        var4 = params[72];
                    }
                } else {
                    if (input[0] >= -0.9071902) {
                        var4 = params[73];
                    } else {
                        var4 = params[74];
                    }
                }
            } else {
                if (input[1] >= 1.9018891) {
                    var4 = params[75];
                } else {
                    if (input[0] >= -1.13756) {
                        var4 = params[76];
                    } else {
                        var4 = params[77];
                    }
                }
            }
        }
        double var5;
        if (input[0] >= -0.47975498) {
            if (input[4] >= 1.0549002) {
                if (input[10] >= 0.5) {
                    if (input[13] >= 1.5) {
                        var5 = params[78];
                    } else {
                        var5 = params[79];
                    }
                } else {
                    var5 = params[80];
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var5 = params[81];
                    } else {
                        var5 = params[82];
                    }
                } else {
                    if (input[4] >= -0.54034054) {
                        var5 = params[83];
                    } else {
                        var5 = params[84];
                    }
                }
            }
        } else {
            if (input[4] >= 1.0549002) {
                if (input[4] >= 1.5106833) {
                    var5 = params[85];
                } else {
                    if (input[2] >= 0.13679874) {
                        var5 = params[86];
                    } else {
                        var5 = params[87];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[4] >= -0.08455747) {
                        var5 = params[88];
                    } else {
                        var5 = params[89];
                    }
                } else {
                    if (input[4] >= -1.2240152) {
                        var5 = params[90];
                    } else {
                        var5 = params[91];
                    }
                }
            }
        }
        double var6;
        if (input[0] >= -0.8296525) {
            if (input[1] >= 1.9018891) {
                if (input[0] >= -0.059337243) {
                    if (input[4] >= 0.029388294) {
                        var6 = params[92];
                    } else {
                        var6 = params[93];
                    }
                } else {
                    var6 = params[94];
                }
            } else {
                if (input[1] >= -1.3599465) {
                    if (input[12] >= 0.5) {
                        var6 = params[95];
                    } else {
                        var6 = params[96];
                    }
                } else {
                    if (input[4] >= 1.0549002) {
                        var6 = params[97];
                    } else {
                        var6 = params[98];
                    }
                }
            }
        } else {
            if (input[4] >= -1.6797982) {
                if (input[1] >= -0.8159811) {
                    if (input[13] >= 0.5) {
                        var6 = params[99];
                    } else {
                        var6 = params[100];
                    }
                } else {
                    if (input[4] >= -0.7682321) {
                        var6 = params[101];
                    } else {
                        var6 = params[102];
                    }
                }
            } else {
                if (input[1] >= 0.815915) {
                    var6 = params[103];
                } else {
                    if (input[0] >= -1.1556215) {
                        var6 = params[104];
                    } else {
                        var6 = params[105];
                    }
                }
            }
        }
        double var7;
        if (input[0] >= -0.31329048) {
            if (input[1] >= 0.815915) {
                if (input[0] >= 2.0634587) {
                    var7 = params[106];
                } else {
                    if (input[8] >= 0.5) {
                        var7 = params[107];
                    } else {
                        var7 = params[108];
                    }
                }
            } else {
                if (input[0] >= 2.7587671) {
                    var7 = params[109];
                } else {
                    if (input[13] >= 0.5) {
                        var7 = params[110];
                    } else {
                        var7 = params[111];
                    }
                }
            }
        } else {
            if (input[1] >= 0.815915) {
                if (input[0] >= -1.1831162) {
                    if (input[8] >= 0.5) {
                        var7 = params[112];
                    } else {
                        var7 = params[113];
                    }
                } else {
                    var7 = params[114];
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[5] >= 0.5) {
                        var7 = params[115];
                    } else {
                        var7 = params[116];
                    }
                } else {
                    if (input[2] >= 0.13679874) {
                        var7 = params[117];
                    } else {
                        var7 = params[118];
                    }
                }
            }
        }
        double var8;
        if (input[1] >= -1.3599465) {
            if (input[4] >= -0.54034054) {
                if (input[13] >= -0.5) {
                    if (input[4] >= -0.312449) {
                        var8 = params[119];
                    } else {
                        var8 = params[120];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var8 = params[121];
                    } else {
                        var8 = params[122];
                    }
                }
            } else {
                if (input[1] >= 2.987863) {
                    var8 = params[123];
                } else {
                    if (input[7] >= 0.5) {
                        var8 = params[124];
                    } else {
                        var8 = params[125];
                    }
                }
            }
        } else {
            if (input[4] >= -0.7682321) {
                if (input[4] >= 1.0549002) {
                    if (input[4] >= 1.2827917) {
                        var8 = params[126];
                    } else {
                        var8 = params[127];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var8 = params[128];
                    } else {
                        var8 = params[129];
                    }
                }
            } else {
                if (input[13] >= 1.5) {
                    var8 = params[130];
                } else {
                    if (input[13] >= -0.5) {
                        var8 = params[131];
                    } else {
                        var8 = params[132];
                    }
                }
            }
        }
        double var9;
        if (input[0] >= -0.89390296) {
            if (input[13] >= 0.5) {
                if (input[8] >= 0.5) {
                    if (input[0] >= 0.024297638) {
                        var9 = params[133];
                    } else {
                        var9 = params[134];
                    }
                } else {
                    if (input[0] >= 1.0261331) {
                        var9 = params[135];
                    } else {
                        var9 = params[136];
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[0] >= -0.87469107) {
                        var9 = params[137];
                    } else {
                        var9 = params[138];
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var9 = params[139];
                    } else {
                        var9 = params[140];
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[13] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var9 = params[141];
                    } else {
                        var9 = params[142];
                    }
                } else {
                    if (input[0] >= -1.285963) {
                        var9 = params[143];
                    } else {
                        var9 = params[144];
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= -1.2283274) {
                        var9 = params[145];
                    } else {
                        var9 = params[146];
                    }
                } else {
                    var9 = params[147];
                }
            }
        }
        double var10;
        if (input[1] >= 0.815915) {
            if (input[0] >= 2.187588) {
                var10 = params[148];
            } else {
                if (input[9] >= 0.5) {
                    if (input[1] >= 2.987863) {
                        var10 = params[149];
                    } else {
                        var10 = params[150];
                    }
                } else {
                    var10 = params[151];
                }
            }
        } else {
            if (input[0] >= -0.7737425) {
                if (input[0] >= 2.1705046) {
                    var10 = params[152];
                } else {
                    if (input[12] >= 0.5) {
                        var10 = params[153];
                    } else {
                        var10 = params[154];
                    }
                }
            } else {
                if (input[2] >= -1.3740458) {
                    if (input[1] >= -0.8159811) {
                        var10 = params[155];
                    } else {
                        var10 = params[156];
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var10 = params[157];
                    } else {
                        var10 = params[158];
                    }
                }
            }
        }
        double var11;
        if (input[4] >= 0.59911716) {
            if (input[12] >= 0.5) {
                if (input[13] >= -0.5) {
                    if (input[3] >= 1.6170561) {
                        var11 = params[159];
                    } else {
                        var11 = params[160];
                    }
                } else {
                    if (input[0] >= -0.7426239) {
                        var11 = params[161];
                    } else {
                        var11 = params[162];
                    }
                }
            } else {
                if (input[0] >= -1.154471) {
                    if (input[0] >= -0.6638782) {
                        var11 = params[163];
                    } else {
                        var11 = params[164];
                    }
                } else {
                    var11 = params[165];
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[0] >= -1.064739) {
                    if (input[4] >= -0.54034054) {
                        var11 = params[166];
                    } else {
                        var11 = params[167];
                    }
                } else {
                    if (input[0] >= -1.1093749) {
                        var11 = params[168];
                    } else {
                        var11 = params[169];
                    }
                }
            } else {
                if (input[0] >= 0.93818414) {
                    var11 = params[170];
                } else {
                    if (input[3] >= 0.59700394) {
                        var11 = params[171];
                    } else {
                        var11 = params[172];
                    }
                }
            }
        }
        double var12;
        if (input[1] >= -1.3599465) {
            if (input[4] >= -1.4519067) {
                if (input[0] >= 1.4982617) {
                    var12 = params[173];
                } else {
                    if (input[4] >= 1.5106833) {
                        var12 = params[174];
                    } else {
                        var12 = params[175];
                    }
                }
            } else {
                if (input[0] >= -0.5758718) {
                    if (input[0] >= 0.81267434) {
                        var12 = params[176];
                    } else {
                        var12 = params[177];
                    }
                } else {
                    if (input[0] >= -0.72611547) {
                        var12 = params[178];
                    } else {
                        var12 = params[179];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[8] >= 0.5) {
                    if (input[3] >= -0.42304826) {
                        var12 = params[180];
                    } else {
                        var12 = params[181];
                    }
                } else {
                    if (input[0] >= 0.2536896) {
                        var12 = params[182];
                    } else {
                        var12 = params[183];
                    }
                }
            } else {
                if (input[3] >= 1.6170561) {
                    var12 = params[184];
                } else {
                    if (input[0] >= -0.30805612) {
                        var12 = params[185];
                    } else {
                        var12 = params[186];
                    }
                }
            }
        }
        double var13;
        if (input[0] >= -0.93761855) {
            if (input[1] >= 1.9018891) {
                if (input[6] >= 0.5) {
                    var13 = params[187];
                } else {
                    if (input[4] >= -1.4519067) {
                        var13 = params[188];
                    } else {
                        var13 = params[189];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var13 = params[190];
                    } else {
                        var13 = params[191];
                    }
                } else {
                    if (input[4] >= -0.9961236) {
                        var13 = params[192];
                    } else {
                        var13 = params[193];
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[6] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var13 = params[194];
                    } else {
                        var13 = params[195];
                    }
                } else {
                    if (input[4] >= -2.363473) {
                        var13 = params[196];
                    } else {
                        var13 = params[197];
                    }
                }
            } else {
                if (input[0] >= -1.2768748) {
                    if (input[4] >= -1.9076898) {
                        var13 = params[198];
                    } else {
                        var13 = params[199];
                    }
                } else {
                    if (input[4] >= -0.312449) {
                        var13 = params[200];
                    } else {
                        var13 = params[201];
                    }
                }
            }
        }
        double var14;
        if (input[1] >= -1.3599465) {
            if (input[8] >= 0.5) {
                if (input[3] >= -0.42304826) {
                    if (input[0] >= -1.0242445) {
                        var14 = params[202];
                    } else {
                        var14 = params[203];
                    }
                } else {
                    if (input[0] >= 0.95802873) {
                        var14 = params[204];
                    } else {
                        var14 = params[205];
                    }
                }
            } else {
                if (input[4] >= 0.59911716) {
                    if (input[0] >= -1.157117) {
                        var14 = params[206];
                    } else {
                        var14 = params[207];
                    }
                } else {
                    if (input[1] >= 0.815915) {
                        var14 = params[208];
                    } else {
                        var14 = params[209];
                    }
                }
            }
        } else {
            if (input[3] >= 1.6170561) {
                var14 = params[210];
            } else {
                if (input[10] >= 0.5) {
                    if (input[4] >= -1.4519067) {
                        var14 = params[211];
                    } else {
                        var14 = params[212];
                    }
                } else {
                    if (input[0] >= -1.1216843) {
                        var14 = params[213];
                    } else {
                        var14 = params[214];
                    }
                }
            }
        }
        double var15;
        if (input[4] >= -1.6797982) {
            if (input[0] >= -1.0676725) {
                if (input[4] >= 1.0549002) {
                    if (input[0] >= -0.584845) {
                        var15 = params[215];
                    } else {
                        var15 = params[216];
                    }
                } else {
                    if (input[3] >= 1.6170561) {
                        var15 = params[217];
                    } else {
                        var15 = params[218];
                    }
                }
            } else {
                if (input[4] >= -0.9961236) {
                    if (input[4] >= -0.7682321) {
                        var15 = params[219];
                    } else {
                        var15 = params[220];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var15 = params[221];
                    } else {
                        var15 = params[222];
                    }
                }
            }
        } else {
            if (input[13] >= -0.5) {
                if (input[6] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var15 = params[223];
                    } else {
                        var15 = params[224];
                    }
                } else {
                    if (input[0] >= -1.0703759) {
                        var15 = params[225];
                    } else {
                        var15 = params[226];
                    }
                }
            } else {
                if (input[4] >= -1.9076898) {
                    var15 = params[227];
                } else {
                    if (input[11] >= 0.5) {
                        var15 = params[228];
                    } else {
                        var15 = params[229];
                    }
                }
            }
        }
        double var16;
        if (input[1] >= -0.27201572) {
            if (input[7] >= 0.5) {
                if (input[13] >= -1.5) {
                    if (input[3] >= 1.6170561) {
                        var16 = params[230];
                    } else {
                        var16 = params[231];
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var16 = params[232];
                    } else {
                        var16 = params[233];
                    }
                }
            } else {
                if (input[0] >= 1.2212428) {
                    var16 = params[234];
                } else {
                    if (input[3] >= 0.59700394) {
                        var16 = params[235];
                    } else {
                        var16 = params[236];
                    }
                }
            }
        } else {
            if (input[4] >= -0.9961236) {
                if (input[2] >= -1.3740458) {
                    var16 = params[237];
                } else {
                    var16 = params[238];
                }
            } else {
                if (input[0] >= -0.96540093) {
                    var16 = params[239];
                } else {
                    if (input[2] >= -1.3740458) {
                        var16 = params[240];
                    } else {
                        var16 = params[241];
                    }
                }
            }
        }
        double var17;
        if (input[4] >= 1.5106833) {
            if (input[0] >= 0.3480808) {
                if (input[0] >= 0.56614125) {
                    if (input[0] >= 1.427454) {
                        var17 = params[242];
                    } else {
                        var17 = params[243];
                    }
                } else {
                    var17 = params[244];
                }
            } else {
                if (input[0] >= -0.13808289) {
                    var17 = params[245];
                } else {
                    if (input[0] >= -0.3474002) {
                        var17 = params[246];
                    } else {
                        var17 = params[247];
                    }
                }
            }
        } else {
            if (input[1] >= 0.815915) {
                if (input[0] >= -0.06146551) {
                    if (input[4] >= 0.14333406) {
                        var17 = params[248];
                    } else {
                        var17 = params[249];
                    }
                } else {
                    if (input[4] >= 0.3712256) {
                        var17 = params[250];
                    } else {
                        var17 = params[251];
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var17 = params[252];
                    } else {
                        var17 = params[253];
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var17 = params[254];
                    } else {
                        var17 = params[255];
                    }
                }
            }
        }
        double var18;
        if (input[0] >= 2.1705046) {
            var18 = params[256];
        } else {
            if (input[0] >= -1.3181746) {
                if (input[1] >= -1.3599465) {
                    if (input[0] >= -1.1492941) {
                        var18 = params[257];
                    } else {
                        var18 = params[258];
                    }
                } else {
                    if (input[0] >= -1.1124811) {
                        var18 = params[259];
                    } else {
                        var18 = params[260];
                    }
                }
            } else {
                var18 = params[261];
            }
        }
        double var19;
        if (input[4] >= -0.9961236) {
            if (input[7] >= 0.5) {
                if (input[13] >= -1.5) {
                    if (input[3] >= 0.59700394) {
                        var19 = params[262];
                    } else {
                        var19 = params[263];
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var19 = params[264];
                    } else {
                        var19 = params[265];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var19 = params[266];
                    } else {
                        var19 = params[267];
                    }
                } else {
                    var19 = params[268];
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[13] >= -0.5) {
                    if (input[2] >= -2.8848906) {
                        var19 = params[269];
                    } else {
                        var19 = params[270];
                    }
                } else {
                    if (input[4] >= -1.2240152) {
                        var19 = params[271];
                    } else {
                        var19 = params[272];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var19 = params[273];
                    } else {
                        var19 = params[274];
                    }
                } else {
                    if (input[4] >= -1.6797982) {
                        var19 = params[275];
                    } else {
                        var19 = params[276];
                    }
                }
            }
        }
        double var20;
        if (input[0] >= -1.2502427) {
            if (input[0] >= -1.1201887) {
                if (input[0] >= -1.1093749) {
                    if (input[0] >= -1.0791191) {
                        var20 = params[277];
                    } else {
                        var20 = params[278];
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var20 = params[279];
                    } else {
                        var20 = params[280];
                    }
                }
            } else {
                if (input[0] >= -1.1238701) {
                    var20 = params[281];
                } else {
                    if (input[13] >= -0.5) {
                        var20 = params[282];
                    } else {
                        var20 = params[283];
                    }
                }
            }
        } else {
            if (input[4] >= -0.312449) {
                var20 = params[284];
            } else {
                if (input[0] >= -1.330599) {
                    var20 = params[285];
                } else {
                    var20 = params[286];
                }
            }
        }
        double var21;
        if (input[0] >= -0.10316791) {
            if (input[4] >= -0.9961236) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -0.07475275) {
                        var21 = params[287];
                    } else {
                        var21 = params[288];
                    }
                } else {
                    if (input[3] >= -0.42304826) {
                        var21 = params[289];
                    } else {
                        var21 = params[290];
                    }
                }
            } else {
                if (input[2] >= 0.13679874) {
                    if (input[10] >= 0.5) {
                        var21 = params[291];
                    } else {
                        var21 = params[292];
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var21 = params[293];
                    } else {
                        var21 = params[294];
                    }
                }
            }
        } else {
            if (input[4] >= 1.0549002) {
                if (input[11] >= 0.5) {
                    var21 = params[295];
                } else {
                    var21 = params[296];
                }
            } else {
                if (input[3] >= 1.6170561) {
                    if (input[5] >= 0.5) {
                        var21 = params[297];
                    } else {
                        var21 = params[298];
                    }
                } else {
                    if (input[13] >= 1.5) {
                        var21 = params[299];
                    } else {
                        var21 = params[300];
                    }
                }
            }
        }
        double var22;
        if (input[7] >= 0.5) {
            if (input[4] >= 1.7385747) {
                if (input[8] >= 0.5) {
                    if (input[12] >= 0.5) {
                        var22 = params[301];
                    } else {
                        var22 = params[302];
                    }
                } else {
                    var22 = params[303];
                }
            } else {
                if (input[0] >= -1.1599929) {
                    if (input[13] >= -0.5) {
                        var22 = params[304];
                    } else {
                        var22 = params[305];
                    }
                } else {
                    if (input[4] >= -1.9076898) {
                        var22 = params[306];
                    } else {
                        var22 = params[307];
                    }
                }
            }
        } else {
            if (input[2] >= -2.8848906) {
                if (input[0] >= -0.89119947) {
                    if (input[4] >= -0.54034054) {
                        var22 = params[308];
                    } else {
                        var22 = params[309];
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var22 = params[310];
                    } else {
                        var22 = params[311];
                    }
                }
            } else {
                if (input[0] >= -0.6404674) {
                    var22 = params[312];
                } else {
                    var22 = params[313];
                }
            }
        }
        double var23;
        if (input[0] >= -0.18306395) {
            if (input[0] >= -0.17294034) {
                if (input[4] >= -1.6797982) {
                    if (input[0] >= 0.31678966) {
                        var23 = params[314];
                    } else {
                        var23 = params[315];
                    }
                } else {
                    if (input[4] >= -1.9076898) {
                        var23 = params[316];
                    } else {
                        var23 = params[317];
                    }
                }
            } else {
                var23 = params[318];
            }
        } else {
            if (input[0] >= -0.7737425) {
                if (input[10] >= 0.5) {
                    if (input[5] >= 0.5) {
                        var23 = params[319];
                    } else {
                        var23 = params[320];
                    }
                } else {
                    if (input[4] >= 0.14333406) {
                        var23 = params[321];
                    } else {
                        var23 = params[322];
                    }
                }
            } else {
                if (input[3] >= 1.6170561) {
                    if (input[5] >= 0.5) {
                        var23 = params[323];
                    } else {
                        var23 = params[324];
                    }
                } else {
                    if (input[0] >= -1.0423635) {
                        var23 = params[325];
                    } else {
                        var23 = params[326];
                    }
                }
            }
        }
        double var24;
        if (input[13] >= -1.5) {
            if (input[3] >= 0.59700394) {
                if (input[0] >= -0.5477443) {
                    if (input[11] >= 0.5) {
                        var24 = params[327];
                    } else {
                        var24 = params[328];
                    }
                } else {
                    if (input[2] >= 0.13679874) {
                        var24 = params[329];
                    } else {
                        var24 = params[330];
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    var24 = params[331];
                } else {
                    if (input[5] >= 0.5) {
                        var24 = params[332];
                    } else {
                        var24 = params[333];
                    }
                }
            }
        } else {
            if (input[3] >= 1.6170561) {
                var24 = params[334];
            } else {
                var24 = params[335];
            }
        }
        double var25;
        if (input[3] >= 1.6170561) {
            if (input[4] >= 1.2827917) {
                if (input[3] >= 2.6371083) {
                    var25 = params[336];
                } else {
                    if (input[12] >= 0.5) {
                        var25 = params[337];
                    } else {
                        var25 = params[338];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[0] >= -0.9272648) {
                        var25 = params[339];
                    } else {
                        var25 = params[340];
                    }
                } else {
                    if (input[4] >= -1.4519067) {
                        var25 = params[341];
                    } else {
                        var25 = params[342];
                    }
                }
            }
        } else {
            if (input[4] >= 2.1943579) {
                var25 = params[343];
            } else {
                if (input[1] >= 0.815915) {
                    if (input[4] >= 0.14333406) {
                        var25 = params[344];
                    } else {
                        var25 = params[345];
                    }
                } else {
                    if (input[0] >= -0.93865395) {
                        var25 = params[346];
                    } else {
                        var25 = params[347];
                    }
                }
            }
        }
        double var26;
        if (input[0] >= -0.18904608) {
            if (input[0] >= 1.2391891) {
                if (input[0] >= 1.364469) {
                    if (input[0] >= 1.4716873) {
                        var26 = params[348];
                    } else {
                        var26 = params[349];
                    }
                } else {
                    var26 = params[350];
                }
            } else {
                if (input[0] >= 1.1457758) {
                    var26 = params[351];
                } else {
                    if (input[0] >= 1.0254428) {
                        var26 = params[352];
                    } else {
                        var26 = params[353];
                    }
                }
            }
        } else {
            if (input[0] >= -0.28665847) {
                if (input[0] >= -0.2803312) {
                    if (input[3] >= 1.6170561) {
                        var26 = params[354];
                    } else {
                        var26 = params[355];
                    }
                } else {
                    if (input[0] >= -0.28326476) {
                        var26 = params[356];
                    } else {
                        var26 = params[357];
                    }
                }
            } else {
                if (input[0] >= -0.2903973) {
                    var26 = params[358];
                } else {
                    if (input[3] >= 2.6371083) {
                        var26 = params[359];
                    } else {
                        var26 = params[360];
                    }
                }
            }
        }
        double var27;
        if (input[0] >= -1.0976982) {
            if (input[0] >= -1.0897604) {
                if (input[0] >= -1.0423635) {
                    if (input[0] >= -1.0373592) {
                        var27 = params[361];
                    } else {
                        var27 = params[362];
                    }
                } else {
                    if (input[0] >= -1.0566286) {
                        var27 = params[363];
                    } else {
                        var27 = params[364];
                    }
                }
            } else {
                var27 = params[365];
            }
        } else {
            if (input[2] >= -1.3740458) {
                if (input[0] >= -1.1121935) {
                    var27 = params[366];
                } else {
                    if (input[4] >= 0.82700866) {
                        var27 = params[367];
                    } else {
                        var27 = params[368];
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[4] >= -2.363473) {
                        var27 = params[369];
                    } else {
                        var27 = params[370];
                    }
                } else {
                    var27 = params[371];
                }
            }
        }
        double var28;
        if (input[4] >= 0.59911716) {
            if (input[8] >= 0.5) {
                if (input[3] >= -0.42304826) {
                    if (input[3] >= 0.59700394) {
                        var28 = params[372];
                    } else {
                        var28 = params[373];
                    }
                } else {
                    var28 = params[374];
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[0] >= -0.7650569) {
                        var28 = params[375];
                    } else {
                        var28 = params[376];
                    }
                } else {
                    if (input[0] >= 0.022399459) {
                        var28 = params[377];
                    } else {
                        var28 = params[378];
                    }
                }
            }
        } else {
            if (input[0] >= -1.1557364) {
                if (input[0] >= -1.1488916) {
                    if (input[3] >= 0.59700394) {
                        var28 = params[379];
                    } else {
                        var28 = params[380];
                    }
                } else {
                    var28 = params[381];
                }
            } else {
                if (input[0] >= -1.1731652) {
                    var28 = params[382];
                } else {
                    if (input[6] >= 0.5) {
                        var28 = params[383];
                    } else {
                        var28 = params[384];
                    }
                }
            }
        }
        double var29;
        if (input[0] >= -0.93060106) {
            if (input[0] >= -0.9254817) {
                if (input[4] >= -1.2240152) {
                    var29 = params[385];
                } else {
                    if (input[0] >= -0.3976156) {
                        var29 = params[386];
                    } else {
                        var29 = params[387];
                    }
                }
            } else {
                if (input[3] >= 0.59700394) {
                    var29 = params[388];
                } else {
                    var29 = params[389];
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[4] >= -0.9961236) {
                    if (input[0] >= -1.0436289) {
                        var29 = params[390];
                    } else {
                        var29 = params[391];
                    }
                } else {
                    if (input[2] >= 0.13679874) {
                        var29 = params[392];
                    } else {
                        var29 = params[393];
                    }
                }
            } else {
                if (input[0] >= -1.0159616) {
                    if (input[1] >= -0.8159811) {
                        var29 = params[394];
                    } else {
                        var29 = params[395];
                    }
                } else {
                    if (input[3] >= -0.42304826) {
                        var29 = params[396];
                    } else {
                        var29 = params[397];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
