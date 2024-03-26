package org.matsim.prepare.network;
import org.matsim.application.prepare.network.params.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public final class BerlinNetworkParams_speedRelative_priority implements FeatureRegressor {

    public static BerlinNetworkParams_speedRelative_priority INSTANCE = new BerlinNetworkParams_speedRelative_priority();
    public static final double[] DEFAULT_PARAMS = {0.15724492, 0.19273548, 0.18403086, 0.19157858, 0.16120821, 0.1980568, 0.17025721, 0.14275415, 0.11048176, 0.102709346, 0.06792478, 0.10821823, 0.09011285, 0.08277143, 0.09243622, 0.09845659, 0.06138229, 0.06789871, 0.057023786, 0.045561712, 0.040391147, 0.054062303, 0.0, 0.040396813, 0.035386518, 0.0, 0.042849354, 0.0466025, 0.035123896, 0.0, 0.016435765, 0.0, 0.0, 0.019191166, 0.040614914, 0.027222177, 0.0048989994, 0.035677213, 0.021704927, 0.015032333, 0.031955387, 0.01666849, 0.0074288645, 0.014110131, 0.024576068, 0.010005573, 0.041455485, 0.025570804, 0.017918263, 0.0, 0.0, 0.01256823, 0.015104672, -0.012481432, 0.00536652, 0.009893488, 0.009986547, 0.018633667, 0.012301514, 0.0055723884, -0.0042632725, 0.009677288, 0.0015550333, 0.006243969, -0.0037499985, 0.0032326714, 0.0217748, 0.0048877522, -0.01280412, 0.0, 0.011345619, 0.0, 0.0, 0.0035977655, -0.008411235, 0.0, 0.01370504, -0.0006115317, 0.007350211, 0.0014637669, 0.005347326, 0.0020510554, 0.005052541, -0.0014409187, 0.0018602, -0.0016169732, 0.0031055745, -0.0056220083, 0.011374512, 0.0, 0.001966972, -0.0028674304, 0.0020517444, 0.00076617644, -0.0046325005, -0.004758692, 0.0012792036, -0.008385347, 0.0, 0.002726265, 0.00020829745, 0.0, 0.0, -0.0082345735, 0.0, -0.035001624, -0.0050901906, 0.0, -0.0017592426, 0.00027184488, -0.0030666916, -0.013880559, 0.0053953347, 0.0035018784, 0.00045144613, 8.061571e-05, -0.0033695176, 0.0030606918, 0.00033529464, -0.004963807, 0.010418295, -0.00029751126, -0.006895263, 0.00044846663, 0.0026161217, -0.021911642, -0.0044968612, 0.0, -0.0012206174, -0.025058791, 0.0042696362, -0.0005126147, 0.0004962817, -0.00077095797, 0.002565097, 0.0, -0.006931564, 0.0, -0.022092693, -0.007780278, 0.0007043617, -0.0008171179, -0.002066162, 0.008384691, 0.0011571822, 0.00027108335, -0.0009287675, 0.006082932, 0.0028573114, -0.00846794, 0.00018975107, 0.0045378944, -0.0011339803, 0.005224601, -6.513751e-05, 0.0, 0.0, -0.018269084, 0.0034695654, -0.00026458103, -0.0027475485, 0.0068913423, 0.0, -0.012815217, 0.00026853607, -0.0027989408, 0.00024086394, 0.0066861873, 0.0, 6.4727988e-06, -0.0014501911, -0.002494975, 0.0015008535, -0.00049155444, 0.0067627244, -0.0014077883, 0.0015698029, 0.0, -0.010308943, -0.0009068087, 0.0, 0.0027998113, -0.0046737837, 0.00012720711, -0.0042179585, 0.003662557, -0.0012615945, 0.00192968, 3.7585372e-07, 9.2846225e-05, 0.013026616, -0.008578217, -0.005248636, -0.0036968153, 0.0003156052, 0.0024093715, -0.0003495667, -0.00030448436, -3.8445793e-05, 0.0, -0.024849607, 0.00016108007, 0.015603923, 0.0, -0.0035782477, -2.18532e-05, -0.009707966, 0.0, 0.0041826395, -0.0035536666, 0.004105262, 0.010043072, 0.00020426886, 3.1741417e-06, 0.00572087, -0.0007401902, 0.0, -0.0108033465, -0.013462666, -0.001430584, -0.00095125375, 4.312593e-05, 0.00028308886, 0.0037140378, 0.00079021766, -0.005122026, -0.0013027412, -0.0047874786, 0.0041786814, -0.0005791032, -0.008750576, 0.0137136765, 4.2379565e-06, 0.0030340564, -0.021573383, -0.0009175329, 0.00065593724, -0.0048078895, 0.00050423876, 0.0017348679, -0.010162599, 0.00016265023, 0.0029644673, -0.008102837, 0.0024075173, -1.6770764e-05, 0.00025584106, -3.6568855e-07, -0.0074612647, 0.011033748, 0.0, 0.007791925, -0.019453187, 0.0005933511, 0.0024033622, -0.0007407156, -0.0063573257, 0.0012771342, 0.0044823387, -0.0023863865, 4.71596e-05, 0.000489205, -0.0005635803, -3.7119185e-05, 0.0027739953, -0.0083763795, -0.02207759, -0.002804457, 0.015720833, 0.0, 0.0007142104, -3.4262026e-05, 0.005516721, -0.0006166697, 0.0, 0.0132253505, 0.0, -0.0045134304, 0.0060599926, -0.00045232018};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }

    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 142.5375147043097) / 99.88133624783757;
		data[1] = (ft.getDouble("speed") - 12.960856860228855) / 3.818676471858858;
		data[2] = (ft.getDouble("num_lanes") - 1.2051384878622606) / 0.6153877429557003;
		data[3] = ft.getDouble("change_speed");
		data[4] = ft.getDouble("change_num_lanes");
		data[5] = ft.getDouble("num_to_links");
		data[6] = ft.getDouble("junction_inc_lanes");
		data[7] = ft.getDouble("priority_lower");
		data[8] = ft.getDouble("priority_equal");
		data[9] = ft.getDouble("priority_higher");
		data[10] = ft.getDouble("is_secondary_or_higher");
		data[11] = ft.getDouble("is_primary_or_higher");
		data[12] = ft.getDouble("is_motorway");
		data[13] = ft.getDouble("is_link");

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
        if (input[3] >= -3.4699998) {
            if (input[1] >= -0.12068497) {
                if (input[7] >= 0.5) {
                    var0 = params[0];
                } else {
                    if (input[0] >= -0.30794054) {
                        var0 = params[1];
                    } else {
                        var0 = params[2];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -1.0670915) {
                        var0 = params[3];
                    } else {
                        var0 = params[4];
                    }
                } else {
                    var0 = params[5];
                }
            }
        } else {
            if (input[0] >= -0.22288963) {
                var0 = params[6];
            } else {
                var0 = params[7];
            }
        }
        double var1;
        if (input[2] >= 0.47914752) {
            if (input[11] >= 0.5) {
                var1 = params[8];
            } else {
                var1 = params[9];
            }
        } else {
            if (input[1] >= -0.12068497) {
                if (input[3] >= 4.165) {
                    if (input[5] >= 1.5) {
                        var1 = params[10];
                    } else {
                        var1 = params[11];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var1 = params[12];
                    } else {
                        var1 = params[13];
                    }
                }
            } else {
                if (input[5] >= 1.5) {
                    var1 = params[14];
                } else {
                    if (input[3] >= -1.385) {
                        var1 = params[15];
                    } else {
                        var1 = params[16];
                    }
                }
            }
        }
        double var2;
        if (input[2] >= 0.47914752) {
            if (input[0] >= -0.5885235) {
                if (input[0] >= 1.1001804) {
                    var2 = params[17];
                } else {
                    if (input[4] >= -0.5) {
                        var2 = params[18];
                    } else {
                        var2 = params[19];
                    }
                }
            } else {
                if (input[2] >= 2.1041393) {
                    if (input[5] >= 1.5) {
                        var2 = params[20];
                    } else {
                        var2 = params[21];
                    }
                } else {
                    if (input[6] >= 8.5) {
                        var2 = params[22];
                    } else {
                        var2 = params[23];
                    }
                }
            }
        } else {
            if (input[3] >= -1.385) {
                if (input[1] >= 1.3340075) {
                    if (input[0] >= -1.3194408) {
                        var2 = params[24];
                    } else {
                        var2 = params[25];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var2 = params[26];
                    } else {
                        var2 = params[27];
                    }
                }
            } else {
                if (input[0] >= -0.65860665) {
                    if (input[3] >= -12.775) {
                        var2 = params[28];
                    } else {
                        var2 = params[29];
                    }
                } else {
                    if (input[0] >= -1.1179017) {
                        var2 = params[30];
                    } else {
                        var2 = params[31];
                    }
                }
            }
        }
        double var3;
        if (input[0] >= -0.82680625) {
            if (input[2] >= 0.47914752) {
                if (input[1] >= 4.607393) {
                    if (input[5] >= 1.5) {
                        var3 = params[32];
                    } else {
                        var3 = params[33];
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var3 = params[34];
                    } else {
                        var3 = params[35];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var3 = params[36];
                    } else {
                        var3 = params[37];
                    }
                } else {
                    if (input[3] >= -2.775) {
                        var3 = params[38];
                    } else {
                        var3 = params[39];
                    }
                }
            }
        } else {
            if (input[1] >= -0.84868586) {
                if (input[0] >= -1.1422806) {
                    if (input[3] >= 1.385) {
                        var3 = params[40];
                    } else {
                        var3 = params[41];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var3 = params[42];
                    } else {
                        var3 = params[43];
                    }
                }
            } else {
                if (input[6] >= 2.5) {
                    if (input[9] >= 0.5) {
                        var3 = params[44];
                    } else {
                        var3 = params[45];
                    }
                } else {
                    if (input[3] >= 1.385) {
                        var3 = params[46];
                    } else {
                        var3 = params[47];
                    }
                }
            }
        }
        double var4;
        if (input[0] >= 0.67307353) {
            if (input[2] >= 0.47914752) {
                if (input[3] >= -7.2250004) {
                    var4 = params[48];
                } else {
                    var4 = params[49];
                }
            } else {
                if (input[1] >= 3.1527004) {
                    var4 = params[50];
                } else {
                    var4 = params[51];
                }
            }
        } else {
            if (input[1] >= -0.12068497) {
                if (input[7] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var4 = params[52];
                    } else {
                        var4 = params[53];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var4 = params[54];
                    } else {
                        var4 = params[55];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    var4 = params[56];
                } else {
                    if (input[2] >= 0.47914752) {
                        var4 = params[57];
                    } else {
                        var4 = params[58];
                    }
                }
            }
        }
        double var5;
        if (input[0] >= -0.085927114) {
            if (input[1] >= 3.1527004) {
                if (input[0] >= 1.0653391) {
                    var5 = params[59];
                } else {
                    var5 = params[60];
                }
            } else {
                if (input[2] >= 0.47914752) {
                    var5 = params[61];
                } else {
                    if (input[1] >= 1.3340075) {
                        var5 = params[62];
                    } else {
                        var5 = params[63];
                    }
                }
            }
        } else {
            if (input[0] >= -1.2767903) {
                if (input[5] >= 2.5) {
                    if (input[8] >= 0.5) {
                        var5 = params[64];
                    } else {
                        var5 = params[65];
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var5 = params[66];
                    } else {
                        var5 = params[67];
                    }
                }
            } else {
                if (input[5] >= 1.5) {
                    if (input[10] >= 0.5) {
                        var5 = params[68];
                    } else {
                        var5 = params[69];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var5 = params[70];
                    } else {
                        var5 = params[71];
                    }
                }
            }
        }
        double var6;
        if (input[3] >= 1.385) {
            if (input[5] >= 1.5) {
                if (input[0] >= -1.1402783) {
                    if (input[8] >= 0.5) {
                        var6 = params[72];
                    } else {
                        var6 = params[73];
                    }
                } else {
                    var6 = params[74];
                }
            } else {
                if (input[0] >= 0.38107705) {
                    var6 = params[75];
                } else {
                    if (input[8] >= 0.5) {
                        var6 = params[76];
                    } else {
                        var6 = params[77];
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                if (input[10] >= 0.5) {
                    if (input[2] >= 2.1041393) {
                        var6 = params[78];
                    } else {
                        var6 = params[79];
                    }
                } else {
                    if (input[0] >= -0.7905632) {
                        var6 = params[80];
                    } else {
                        var6 = params[81];
                    }
                }
            } else {
                if (input[0] >= 1.1777724) {
                    var6 = params[82];
                } else {
                    if (input[5] >= 1.5) {
                        var6 = params[83];
                    } else {
                        var6 = params[84];
                    }
                }
            }
        }
        double var7;
        if (input[2] >= 2.1041393) {
            if (input[1] >= 3.1527004) {
                var7 = params[85];
            } else {
                if (input[4] >= -0.5) {
                    if (input[0] >= -1.0910699) {
                        var7 = params[86];
                    } else {
                        var7 = params[87];
                    }
                } else {
                    if (input[0] >= -1.1057372) {
                        var7 = params[88];
                    } else {
                        var7 = params[89];
                    }
                }
            }
        } else {
            if (input[4] >= -0.5) {
                if (input[1] >= 1.3340075) {
                    if (input[11] >= 0.5) {
                        var7 = params[90];
                    } else {
                        var7 = params[91];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var7 = params[92];
                    } else {
                        var7 = params[93];
                    }
                }
            } else {
                var7 = params[94];
            }
        }
        double var8;
        if (input[0] >= 0.18489426) {
            if (input[3] >= 9.725) {
                var8 = params[95];
            } else {
                var8 = params[96];
            }
        } else {
            if (input[3] >= -4.855) {
                if (input[7] >= 0.5) {
                    if (input[1] >= -0.84868586) {
                        var8 = params[97];
                    } else {
                        var8 = params[98];
                    }
                } else {
                    if (input[3] >= 1.385) {
                        var8 = params[99];
                    } else {
                        var8 = params[100];
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    var8 = params[101];
                } else {
                    if (input[6] >= 3.5) {
                        var8 = params[102];
                    } else {
                        var8 = params[103];
                    }
                }
            }
        }
        double var9;
        if (input[6] >= 7.5) {
            if (input[0] >= -0.9916018) {
                if (input[2] >= 3.729131) {
                    var9 = params[104];
                } else {
                    if (input[8] >= 0.5) {
                        var9 = params[105];
                    } else {
                        var9 = params[106];
                    }
                }
            } else {
                var9 = params[107];
            }
        } else {
            if (input[5] >= 2.5) {
                if (input[0] >= -0.72153133) {
                    if (input[8] >= 0.5) {
                        var9 = params[108];
                    } else {
                        var9 = params[109];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var9 = params[110];
                    } else {
                        var9 = params[111];
                    }
                }
            } else {
                if (input[6] >= 5.5) {
                    var9 = params[112];
                } else {
                    if (input[13] >= 0.5) {
                        var9 = params[113];
                    } else {
                        var9 = params[114];
                    }
                }
            }
        }
        double var10;
        if (input[3] >= -5.835) {
            if (input[0] >= -0.895688) {
                if (input[5] >= 1.5) {
                    if (input[4] >= -0.5) {
                        var10 = params[115];
                    } else {
                        var10 = params[116];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var10 = params[117];
                    } else {
                        var10 = params[118];
                    }
                }
            } else {
                if (input[0] >= -0.9304292) {
                    var10 = params[119];
                } else {
                    if (input[3] >= 9.725) {
                        var10 = params[120];
                    } else {
                        var10 = params[121];
                    }
                }
            }
        } else {
            var10 = params[122];
        }
        double var11;
        if (input[2] >= 0.47914752) {
            if (input[10] >= 0.5) {
                if (input[5] >= 1.5) {
                    var11 = params[123];
                } else {
                    var11 = params[124];
                }
            } else {
                if (input[6] >= 6.5) {
                    var11 = params[125];
                } else {
                    if (input[6] >= 3.5) {
                        var11 = params[126];
                    } else {
                        var11 = params[127];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[5] >= 1.5) {
                    if (input[6] >= 1.5) {
                        var11 = params[128];
                    } else {
                        var11 = params[129];
                    }
                } else {
                    if (input[3] >= 1.385) {
                        var11 = params[130];
                    } else {
                        var11 = params[131];
                    }
                }
            } else {
                if (input[5] >= 1.5) {
                    var11 = params[132];
                } else {
                    if (input[6] >= 1.5) {
                        var11 = params[133];
                    } else {
                        var11 = params[134];
                    }
                }
            }
        }
        double var12;
        if (input[9] >= 0.5) {
            if (input[1] >= 0.60731596) {
                if (input[0] >= 1.7070005) {
                    var12 = params[135];
                } else {
                    var12 = params[136];
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[0] >= 0.7755952) {
                        var12 = params[137];
                    } else {
                        var12 = params[138];
                    }
                } else {
                    if (input[5] >= 3.5) {
                        var12 = params[139];
                    } else {
                        var12 = params[140];
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[6] >= 2.5) {
                    var12 = params[141];
                } else {
                    if (input[3] >= 1.39) {
                        var12 = params[142];
                    } else {
                        var12 = params[143];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    var12 = params[144];
                } else {
                    if (input[3] >= 1.385) {
                        var12 = params[145];
                    } else {
                        var12 = params[146];
                    }
                }
            }
        }
        double var13;
        if (input[1] >= 3.1527004) {
            if (input[4] >= 0.5) {
                var13 = params[147];
            } else {
                if (input[1] >= 6.1341) {
                    var13 = params[148];
                } else {
                    if (input[3] >= -2.775) {
                        var13 = params[149];
                    } else {
                        var13 = params[150];
                    }
                }
            }
        } else {
            if (input[12] >= 0.5) {
                var13 = params[151];
            } else {
                if (input[6] >= 5.5) {
                    if (input[6] >= 6.5) {
                        var13 = params[152];
                    } else {
                        var13 = params[153];
                    }
                } else {
                    var13 = params[154];
                }
            }
        }
        double var14;
        if (input[6] >= 3.5) {
            if (input[13] >= 0.5) {
                if (input[1] >= 2.060699) {
                    var14 = params[155];
                } else {
                    if (input[4] >= 2.5) {
                        var14 = params[156];
                    } else {
                        var14 = params[157];
                    }
                }
            } else {
                if (input[1] >= 0.60731596) {
                    var14 = params[158];
                } else {
                    if (input[0] >= -0.8955879) {
                        var14 = params[159];
                    } else {
                        var14 = params[160];
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[0] >= -1.1934414) {
                    if (input[4] >= 0.5) {
                        var14 = params[161];
                    } else {
                        var14 = params[162];
                    }
                } else {
                    var14 = params[163];
                }
            } else {
                if (input[1] >= 0.60731596) {
                    if (input[0] >= 0.20576903) {
                        var14 = params[164];
                    } else {
                        var14 = params[165];
                    }
                } else {
                    var14 = params[166];
                }
            }
        }
        double var15;
        if (input[0] >= -0.28436258) {
            if (input[0] >= -0.20201486) {
                if (input[7] >= 0.5) {
                    if (input[4] >= 0.5) {
                        var15 = params[167];
                    } else {
                        var15 = params[168];
                    }
                } else {
                    if (input[1] >= -0.84868586) {
                        var15 = params[169];
                    } else {
                        var15 = params[170];
                    }
                }
            } else {
                var15 = params[171];
            }
        } else {
            if (input[0] >= -0.4798946) {
                var15 = params[172];
            } else {
                if (input[1] >= -0.84868586) {
                    if (input[0] >= -1.3518293) {
                        var15 = params[173];
                    } else {
                        var15 = params[174];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var15 = params[175];
                    } else {
                        var15 = params[176];
                    }
                }
            }
        }
        double var16;
        if (input[5] >= 2.5) {
            if (input[1] >= -0.48468542) {
                if (input[7] >= 0.5) {
                    if (input[0] >= 1.2699318) {
                        var16 = params[177];
                    } else {
                        var16 = params[178];
                    }
                } else {
                    var16 = params[179];
                }
            } else {
                if (input[0] >= -1.0003622) {
                    if (input[3] >= 5.5550003) {
                        var16 = params[180];
                    } else {
                        var16 = params[181];
                    }
                } else {
                    var16 = params[182];
                }
            }
        } else {
            if (input[0] >= -1.2026021) {
                if (input[0] >= -1.1579993) {
                    if (input[0] >= -1.1423306) {
                        var16 = params[183];
                    } else {
                        var16 = params[184];
                    }
                } else {
                    var16 = params[185];
                }
            } else {
                var16 = params[186];
            }
        }
        double var17;
        if (input[3] >= -9.725) {
            if (input[0] >= 2.9220424) {
                var17 = params[187];
            } else {
                if (input[5] >= 0.5) {
                    var17 = params[188];
                } else {
                    if (input[0] >= -1.0536754) {
                        var17 = params[189];
                    } else {
                        var17 = params[190];
                    }
                }
            }
        } else {
            var17 = params[191];
        }
        double var18;
        if (input[3] >= 15.275) {
            var18 = params[192];
        } else {
            if (input[9] >= 0.5) {
                if (input[1] >= 1.3340075) {
                    var18 = params[193];
                } else {
                    var18 = params[194];
                }
            } else {
                if (input[1] >= 0.60731596) {
                    if (input[6] >= 2.5) {
                        var18 = params[195];
                    } else {
                        var18 = params[196];
                    }
                } else {
                    var18 = params[197];
                }
            }
        }
        double var19;
        if (input[0] >= -1.3374121) {
            if (input[0] >= -1.3175887) {
                var19 = params[198];
            } else {
                if (input[6] >= 2.5) {
                    var19 = params[199];
                } else {
                    if (input[1] >= -0.48468542) {
                        var19 = params[200];
                    } else {
                        var19 = params[201];
                    }
                }
            }
        } else {
            if (input[0] >= -1.3404157) {
                var19 = params[202];
            } else {
                var19 = params[203];
            }
        }
        double var20;
        if (input[0] >= -0.39899862) {
            if (input[0] >= -0.3775732) {
                if (input[3] >= 8.335) {
                    var20 = params[204];
                } else {
                    var20 = params[205];
                }
            } else {
                if (input[0] >= -0.3868342) {
                    var20 = params[206];
                } else {
                    var20 = params[207];
                }
            }
        } else {
            if (input[3] >= 5.975) {
                var20 = params[208];
            } else {
                if (input[3] >= 5.5550003) {
                    if (input[6] >= 2.5) {
                        var20 = params[209];
                    } else {
                        var20 = params[210];
                    }
                } else {
                    if (input[3] >= 4.165) {
                        var20 = params[211];
                    } else {
                        var20 = params[212];
                    }
                }
            }
        }
        double var21;
        if (input[3] >= -5.835) {
            if (input[3] >= -2.775) {
                var21 = params[213];
            } else {
                if (input[0] >= 1.1943421) {
                    var21 = params[214];
                } else {
                    var21 = params[215];
                }
            }
        } else {
            if (input[0] >= 0.8305104) {
                var21 = params[216];
            } else {
                var21 = params[217];
            }
        }
        double var22;
        if (input[1] >= -0.84868586) {
            if (input[5] >= 1.5) {
                if (input[3] >= 1.385) {
                    if (input[8] >= 0.5) {
                        var22 = params[218];
                    } else {
                        var22 = params[219];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var22 = params[220];
                    } else {
                        var22 = params[221];
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    var22 = params[222];
                } else {
                    var22 = params[223];
                }
            }
        } else {
            if (input[5] >= 1.5) {
                if (input[0] >= -1.2628736) {
                    var22 = params[224];
                } else {
                    var22 = params[225];
                }
            } else {
                if (input[0] >= -1.0537255) {
                    var22 = params[226];
                } else {
                    if (input[4] >= 0.5) {
                        var22 = params[227];
                    } else {
                        var22 = params[228];
                    }
                }
            }
        }
        double var23;
        if (input[1] >= -0.84868586) {
            if (input[7] >= 0.5) {
                if (input[0] >= -0.9870964) {
                    if (input[0] >= -0.10605099) {
                        var23 = params[229];
                    } else {
                        var23 = params[230];
                    }
                } else {
                    var23 = params[231];
                }
            } else {
                if (input[0] >= -0.87711596) {
                    if (input[0] >= -0.84332585) {
                        var23 = params[232];
                    } else {
                        var23 = params[233];
                    }
                } else {
                    if (input[0] >= -0.87846756) {
                        var23 = params[234];
                    } else {
                        var23 = params[235];
                    }
                }
            }
        } else {
            if (input[0] >= 1.6461782) {
                if (input[7] >= 0.5) {
                    var23 = params[236];
                } else {
                    var23 = params[237];
                }
            } else {
                var23 = params[238];
            }
        }
        double var24;
        if (input[6] >= 4.5) {
            if (input[10] >= 0.5) {
                var24 = params[239];
            } else {
                if (input[8] >= 0.5) {
                    var24 = params[240];
                } else {
                    var24 = params[241];
                }
            }
        } else {
            if (input[1] >= 4.607393) {
                if (input[2] >= 2.1041393) {
                    if (input[4] >= -0.5) {
                        var24 = params[242];
                    } else {
                        var24 = params[243];
                    }
                } else {
                    var24 = params[244];
                }
            } else {
                var24 = params[245];
            }
        }
        double var25;
        if (input[0] >= -1.2263803) {
            if (input[3] >= 1.385) {
                var25 = params[246];
            } else {
                if (input[0] >= -1.2086093) {
                    var25 = params[247];
                } else {
                    var25 = params[248];
                }
            }
        } else {
            if (input[11] >= 0.5) {
                if (input[0] >= -1.3157866) {
                    var25 = params[249];
                } else {
                    var25 = params[250];
                }
            } else {
                if (input[1] >= 0.60731596) {
                    var25 = params[251];
                } else {
                    if (input[13] >= 0.5) {
                        var25 = params[252];
                    } else {
                        var25 = params[253];
                    }
                }
            }
        }
        double var26;
        if (input[3] >= 5.5550003) {
            if (input[9] >= 0.5) {
                var26 = params[254];
            } else {
                var26 = params[255];
            }
        } else {
            if (input[3] >= 1.385) {
                if (input[1] >= -0.84868586) {
                    if (input[5] >= 1.5) {
                        var26 = params[256];
                    } else {
                        var26 = params[257];
                    }
                } else {
                    var26 = params[258];
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[5] >= 2.5) {
                        var26 = params[259];
                    } else {
                        var26 = params[260];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var26 = params[261];
                    } else {
                        var26 = params[262];
                    }
                }
            }
        }
        double var27;
        if (input[0] >= -1.0221381) {
            if (input[0] >= -1.005168) {
                if (input[6] >= 1.5) {
                    var27 = params[263];
                } else {
                    if (input[0] >= -0.9433446) {
                        var27 = params[264];
                    } else {
                        var27 = params[265];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    var27 = params[266];
                } else {
                    var27 = params[267];
                }
            }
        } else {
            if (input[0] >= -1.0292464) {
                if (input[1] >= -0.48468542) {
                    var27 = params[268];
                } else {
                    var27 = params[269];
                }
            } else {
                var27 = params[270];
            }
        }
        double var28;
        if (input[0] >= -1.0642881) {
            if (input[0] >= -1.0552773) {
                var28 = params[271];
            } else {
                var28 = params[272];
            }
        } else {
            var28 = params[273];
        }
        double var29;
        if (input[6] >= 1.5) {
            var29 = params[274];
        } else {
            if (input[13] >= 0.5) {
                if (input[4] >= 0.5) {
                    var29 = params[275];
                } else {
                    var29 = params[276];
                }
            } else {
                if (input[5] >= 1.5) {
                    var29 = params[277];
                } else {
                    if (input[10] >= 0.5) {
                        var29 = params[278];
                    } else {
                        var29 = params[279];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
