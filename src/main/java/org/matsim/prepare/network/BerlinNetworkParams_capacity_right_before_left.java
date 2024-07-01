package org.matsim.prepare.network;
import org.matsim.application.prepare.network.params.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public final class BerlinNetworkParams_capacity_right_before_left implements FeatureRegressor {

    public static BerlinNetworkParams_capacity_right_before_left INSTANCE = new BerlinNetworkParams_capacity_right_before_left();
    public static final double[] DEFAULT_PARAMS = {328.5757, 269.49268, 231.0964, 232.20732, 238.96944, 217.52971, 245.93172, 196.2599, 130.11137, 162.67091, 163.88892, 168.91855, 177.93335, 120.14436, 164.4786, 162.06781, 139.9658, 110.37732, 114.853226, 122.946236, 104.39815, 115.12266, 119.465096, 79.75374, 124.89497, 87.860054, 29.94975, 71.75533, 135.55458, 111.58922, 81.732124, 63.05375, 83.79276, 88.973175, 59.21387, 86.27968, 54.50634, 43.036, 57.698544, 59.136612, 63.140118, 66.2636, 54.35065, 94.81921, 33.714684, 63.78595, 2.5854192, 40.278103, 67.96995, 65.76385, 51.57006, 39.75378, 41.452232, 44.53736, 31.997284, 10.905647, 42.840084, 27.180868, 41.26702, 44.15924, 62.49501, 33.340282, 34.941944, 20.610315, 61.56515, 26.562513, 17.145185, 42.70646, 29.333172, 32.076294, 16.195492, 47.306076, 0.038455993, 26.993008, 57.766266, 19.425892, 44.300034, 18.40526, 34.33802, 20.988033, 24.663631, 8.912071, 16.680994, 20.58277, -2.8025827, 17.442913, 27.01601, 13.931712, 15.10554, 19.0708, 7.64242, 19.289486, 26.840696, -7.763874, 16.059181, 38.44315, -12.641703, 15.14082, 9.366194, 14.997878, -10.614942, 7.670979, -2.6596265, 22.54426, -39.99585, 10.795762, 15.121941, -24.76626, 8.156524, 13.459406, 1.9710164, 25.679066, 32.778603, 7.3173504, 18.013678, -12.889844, 8.557115, -16.925081, 21.958729, 5.212196, 18.448833, -9.052041, 25.488205, -11.65606, 18.484278, -24.807718, 16.140015, 3.490814, 6.3571334, -22.313322, 1.9848174, -14.237209, 5.360275, 14.617204, -6.1242256, -21.332108, 13.842055, 4.7554398, -8.330782, -2.283184, 1.6876256, 2.2970202, 2.9211318, 11.132554, 8.222093, -3.9318573, 1.0153062, -5.786771, 1.9176867, 20.111982, -13.341735, -5.2488017, 18.932772, -2.2183902, 6.023973, 1.0027256, -11.451071, 2.4872222, -18.286674, 7.4326425, 1.363571, -8.058313, 2.9623373, 0.6662814, 2.0770974, -7.1936674, 5.135833, -1.0970511, 1.265644, 2.826718, 17.753006, 6.6504216, 1.1511834, -4.57029, 1.2588999, -9.778034, 10.277584, 0.7607379, -14.093642, -5.7878227, 25.867159, -12.429686, 5.3511887, 0.4285916, 10.114006, -17.682116, -0.6951186, -3.686415, 5.9004226, 13.114284, 6.1482863, -8.650964, -21.94511, 4.131482, -13.5670595, 0.4509796, -5.169152, 9.02604, 4.7153153, 11.034595, 0.36327785, -10.315573, 11.911259, -11.446489, 0.21937746, -1.9593282, -16.709671, 7.9414787, 10.704911, -15.123205, 0.19036125, -24.913687, 17.289347, 7.1274853, -6.104514, 0.09103825, -16.644897, 14.6652155, -9.731849, 9.392941, 0.027961692, 13.964792, -12.233078, 6.300041, -8.140979, 5.66955, 0.23326816, -8.987359, 24.763414, 0.5058552, 7.135163, -13.049223, 0.9535057, -20.696547, -0.0388047, 14.340578, -16.97962, 12.339516, -10.289911, -0.96544015, -0.017682046, -25.752195, -4.7804914, 1.9569407, 9.993857, -14.980678, -5.454724, 0.24939379, 0.6361772, -0.1201637, -1.6190526, -3.2766292, 0.441606, 12.357215, -1.7070338, 0.3730253, -0.013359121, -14.511282, 10.292064, 3.6254585, -16.11821, 4.9755793, -8.917786, 13.915958};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }

    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 143.222974798649) / 82.88768569254515;
		data[1] = (ft.getDouble("speed") - 8.335055858664587) / 0.16557690987318385;
		data[2] = (ft.getDouble("num_lanes") - 1.0017320516151382) / 0.04742011278858442;
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
        if (input[0] >= -1.5885108) {
            var0 = params[0];
        } else {
            var0 = params[1];
        }
        double var1;
        if (input[5] >= 1.5) {
            if (input[0] >= -0.15741754) {
                var1 = params[2];
            } else {
                if (input[0] >= -1.427751) {
                    if (input[5] >= 2.5) {
                        var1 = params[3];
                    } else {
                        var1 = params[4];
                    }
                } else {
                    var1 = params[5];
                }
            }
        } else {
            if (input[4] >= 0.5) {
                var1 = params[6];
            } else {
                if (input[0] >= -1.4446414) {
                    var1 = params[7];
                } else {
                    var1 = params[8];
                }
            }
        }
        double var2;
        if (input[0] >= -1.5099103) {
            if (input[5] >= 1.5) {
                if (input[0] >= 0.23528254) {
                    var2 = params[9];
                } else {
                    if (input[6] >= 3.5) {
                        var2 = params[10];
                    } else {
                        var2 = params[11];
                    }
                }
            } else {
                if (input[6] >= 2.5) {
                    if (input[4] >= 0.5) {
                        var2 = params[12];
                    } else {
                        var2 = params[13];
                    }
                } else {
                    var2 = params[14];
                }
            }
        } else {
            if (input[6] >= 3.5) {
                var2 = params[15];
            } else {
                if (input[0] >= -1.5885108) {
                    var2 = params[16];
                } else {
                    var2 = params[17];
                }
            }
        }
        double var3;
        if (input[5] >= 1.5) {
            if (input[6] >= 3.5) {
                if (input[0] >= -0.87417054) {
                    var3 = params[18];
                } else {
                    if (input[0] >= -1.3322604) {
                        var3 = params[19];
                    } else {
                        var3 = params[20];
                    }
                }
            } else {
                if (input[0] >= -1.5885108) {
                    if (input[0] >= 0.678171) {
                        var3 = params[21];
                    } else {
                        var3 = params[22];
                    }
                } else {
                    var3 = params[23];
                }
            }
        } else {
            if (input[6] >= 2.5) {
                if (input[0] >= -1.220362) {
                    if (input[4] >= 0.5) {
                        var3 = params[24];
                    } else {
                        var3 = params[25];
                    }
                } else {
                    if (input[0] >= -1.40887) {
                        var3 = params[26];
                    } else {
                        var3 = params[27];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    var3 = params[28];
                } else {
                    var3 = params[29];
                }
            }
        }
        double var4;
        if (input[6] >= 2.5) {
            if (input[5] >= 1.5) {
                if (input[6] >= 3.5) {
                    var4 = params[30];
                } else {
                    if (input[3] >= 1.385) {
                        var4 = params[31];
                    } else {
                        var4 = params[32];
                    }
                }
            } else {
                if (input[4] >= 0.5) {
                    var4 = params[33];
                } else {
                    var4 = params[34];
                }
            }
        } else {
            var4 = params[35];
        }
        double var5;
        if (input[5] >= 1.5) {
            if (input[0] >= -0.8861144) {
                if (input[0] >= 2.3567677) {
                    if (input[0] >= 2.481153) {
                        var5 = params[36];
                    } else {
                        var5 = params[37];
                    }
                } else {
                    if (input[0] >= -0.034600735) {
                        var5 = params[38];
                    } else {
                        var5 = params[39];
                    }
                }
            } else {
                if (input[0] >= -1.350357) {
                    if (input[0] >= -1.1719229) {
                        var5 = params[40];
                    } else {
                        var5 = params[41];
                    }
                } else {
                    if (input[0] >= -1.626152) {
                        var5 = params[42];
                    } else {
                        var5 = params[43];
                    }
                }
            }
        } else {
            if (input[6] >= 2.5) {
                if (input[0] >= -0.6688928) {
                    if (input[0] >= 0.13032123) {
                        var5 = params[44];
                    } else {
                        var5 = params[45];
                    }
                } else {
                    if (input[0] >= -0.7453215) {
                        var5 = params[46];
                    } else {
                        var5 = params[47];
                    }
                }
            } else {
                if (input[4] >= 0.5) {
                    var5 = params[48];
                } else {
                    if (input[0] >= 0.76696587) {
                        var5 = params[49];
                    } else {
                        var5 = params[50];
                    }
                }
            }
        }
        double var6;
        if (input[0] >= -0.5536163) {
            if (input[0] >= -0.5439647) {
                if (input[5] >= 1.5) {
                    if (input[0] >= 1.0400462) {
                        var6 = params[51];
                    } else {
                        var6 = params[52];
                    }
                } else {
                    if (input[0] >= 0.7685946) {
                        var6 = params[53];
                    } else {
                        var6 = params[54];
                    }
                }
            } else {
                if (input[0] >= -0.54547274) {
                    var6 = params[55];
                } else {
                    if (input[0] >= -0.5509621) {
                        var6 = params[56];
                    } else {
                        var6 = params[57];
                    }
                }
            }
        } else {
            if (input[0] >= -1.4796284) {
                if (input[5] >= 1.5) {
                    if (input[6] >= 3.5) {
                        var6 = params[58];
                    } else {
                        var6 = params[59];
                    }
                } else {
                    if (input[0] >= -0.6688928) {
                        var6 = params[60];
                    } else {
                        var6 = params[61];
                    }
                }
            } else {
                if (input[0] >= -1.6256092) {
                    if (input[0] >= -1.5250514) {
                        var6 = params[62];
                    } else {
                        var6 = params[63];
                    }
                } else {
                    var6 = params[64];
                }
            }
        }
        double var7;
        if (input[0] >= -1.3734607) {
            if (input[0] >= -0.8776693) {
                if (input[0] >= 2.3805952) {
                    if (input[0] >= 2.481153) {
                        var7 = params[65];
                    } else {
                        var7 = params[66];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var7 = params[67];
                    } else {
                        var7 = params[68];
                    }
                }
            } else {
                if (input[0] >= -1.3591642) {
                    if (input[0] >= -1.350357) {
                        var7 = params[69];
                    } else {
                        var7 = params[70];
                    }
                } else {
                    var7 = params[71];
                }
            }
        } else {
            if (input[0] >= -1.3811216) {
                var7 = params[72];
            } else {
                if (input[0] >= -1.427751) {
                    if (input[0] >= -1.4235284) {
                        var7 = params[73];
                    } else {
                        var7 = params[74];
                    }
                } else {
                    if (input[0] >= -1.626152) {
                        var7 = params[75];
                    } else {
                        var7 = params[76];
                    }
                }
            }
        }
        double var8;
        if (input[4] >= 0.5) {
            if (input[0] >= 0.63250685) {
                var8 = params[77];
            } else {
                var8 = params[78];
            }
        } else {
            if (input[5] >= 1.5) {
                if (input[0] >= -1.5896568) {
                    if (input[6] >= 2.5) {
                        var8 = params[79];
                    } else {
                        var8 = params[80];
                    }
                } else {
                    var8 = params[81];
                }
            } else {
                if (input[0] >= -1.0476583) {
                    if (input[6] >= 2.5) {
                        var8 = params[82];
                    } else {
                        var8 = params[83];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var8 = params[84];
                    } else {
                        var8 = params[85];
                    }
                }
            }
        }
        double var9;
        if (input[0] >= -0.9600096) {
            if (input[10] >= 0.5) {
                var9 = params[86];
            } else {
                if (input[5] >= 1.5) {
                    if (input[6] >= 3.5) {
                        var9 = params[87];
                    } else {
                        var9 = params[88];
                    }
                } else {
                    if (input[0] >= 0.7685946) {
                        var9 = params[89];
                    } else {
                        var9 = params[90];
                    }
                }
            }
        } else {
            if (input[5] >= 1.5) {
                if (input[0] >= -1.045728) {
                    if (input[0] >= -1.0361367) {
                        var9 = params[91];
                    } else {
                        var9 = params[92];
                    }
                } else {
                    if (input[0] >= -1.0510364) {
                        var9 = params[93];
                    } else {
                        var9 = params[94];
                    }
                }
            } else {
                if (input[0] >= -1.0476583) {
                    var9 = params[95];
                } else {
                    if (input[0] >= -1.1356087) {
                        var9 = params[96];
                    } else {
                        var9 = params[97];
                    }
                }
            }
        }
        double var10;
        if (input[0] >= 0.5457629) {
            if (input[5] >= 1.5) {
                if (input[0] >= 0.56307554) {
                    if (input[0] >= 0.5981833) {
                        var10 = params[98];
                    } else {
                        var10 = params[99];
                    }
                } else {
                    if (input[0] >= 0.55625904) {
                        var10 = params[100];
                    } else {
                        var10 = params[101];
                    }
                }
            } else {
                if (input[0] >= 0.7600891) {
                    if (input[0] >= 1.5583379) {
                        var10 = params[102];
                    } else {
                        var10 = params[103];
                    }
                } else {
                    var10 = params[104];
                }
            }
        } else {
            if (input[6] >= 2.5) {
                if (input[0] >= -1.3657997) {
                    if (input[0] >= -1.2385793) {
                        var10 = params[105];
                    } else {
                        var10 = params[106];
                    }
                } else {
                    if (input[0] >= -1.3758132) {
                        var10 = params[107];
                    } else {
                        var10 = params[108];
                    }
                }
            } else {
                if (input[0] >= -1.3198339) {
                    if (input[0] >= -1.1222774) {
                        var10 = params[109];
                    } else {
                        var10 = params[110];
                    }
                } else {
                    if (input[0] >= -1.438971) {
                        var10 = params[111];
                    } else {
                        var10 = params[112];
                    }
                }
            }
        }
        double var11;
        if (input[0] >= -1.626152) {
            if (input[0] >= -1.5797639) {
                if (input[0] >= -1.4796284) {
                    if (input[0] >= -1.451518) {
                        var11 = params[113];
                    } else {
                        var11 = params[114];
                    }
                } else {
                    if (input[0] >= -1.5135298) {
                        var11 = params[115];
                    } else {
                        var11 = params[116];
                    }
                }
            } else {
                var11 = params[117];
            }
        } else {
            var11 = params[118];
        }
        double var12;
        if (input[0] >= -1.5935779) {
            if (input[0] >= -1.4928994) {
                if (input[0] >= -1.4657543) {
                    if (input[0] >= -1.451518) {
                        var12 = params[119];
                    } else {
                        var12 = params[120];
                    }
                } else {
                    var12 = params[121];
                }
            } else {
                if (input[0] >= -1.5250514) {
                    var12 = params[122];
                } else {
                    if (input[0] >= -1.5439323) {
                        var12 = params[123];
                    } else {
                        var12 = params[124];
                    }
                }
            }
        } else {
            if (input[0] >= -1.6307969) {
                var12 = params[125];
            } else {
                var12 = params[126];
            }
        }
        double var13;
        if (input[0] >= -1.4657543) {
            if (input[6] >= 2.5) {
                if (input[0] >= -1.3218849) {
                    if (input[0] >= -1.0836107) {
                        var13 = params[127];
                    } else {
                        var13 = params[128];
                    }
                } else {
                    if (input[0] >= -1.328279) {
                        var13 = params[129];
                    } else {
                        var13 = params[130];
                    }
                }
            } else {
                if (input[0] >= 1.8478863) {
                    var13 = params[131];
                } else {
                    if (input[0] >= -1.2484722) {
                        var13 = params[132];
                    } else {
                        var13 = params[133];
                    }
                }
            }
        } else {
            if (input[0] >= -1.5159426) {
                if (input[0] >= -1.4796284) {
                    var13 = params[134];
                } else {
                    var13 = params[135];
                }
            } else {
                if (input[0] >= -1.5935779) {
                    if (input[0] >= -1.5311439) {
                        var13 = params[136];
                    } else {
                        var13 = params[137];
                    }
                } else {
                    var13 = params[138];
                }
            }
        }
        double var14;
        if (input[5] >= 1.5) {
            if (input[3] >= 1.385) {
                var14 = params[139];
            } else {
                if (input[6] >= 3.5) {
                    if (input[6] >= 4.5) {
                        var14 = params[140];
                    } else {
                        var14 = params[141];
                    }
                } else {
                    var14 = params[142];
                }
            }
        } else {
            if (input[4] >= 0.5) {
                if (input[6] >= 2.5) {
                    var14 = params[143];
                } else {
                    var14 = params[144];
                }
            } else {
                if (input[6] >= 2.5) {
                    var14 = params[145];
                } else {
                    var14 = params[146];
                }
            }
        }
        double var15;
        if (input[0] >= -1.611011) {
            if (input[0] >= -1.5099707) {
                if (input[0] >= -1.4928994) {
                    if (input[2] >= 10.507524) {
                        var15 = params[147];
                    } else {
                        var15 = params[148];
                    }
                } else {
                    var15 = params[149];
                }
            } else {
                if (input[0] >= -1.5284294) {
                    var15 = params[150];
                } else {
                    var15 = params[151];
                }
            }
        } else {
            var15 = params[152];
        }
        double var16;
        if (input[0] >= -1.5935779) {
            if (input[0] >= 3.066825) {
                if (input[6] >= 3.5) {
                    if (input[0] >= 3.3937736) {
                        var16 = params[153];
                    } else {
                        var16 = params[154];
                    }
                } else {
                    if (input[0] >= 3.3397846) {
                        var16 = params[155];
                    } else {
                        var16 = params[156];
                    }
                }
            } else {
                if (input[6] >= 4.5) {
                    if (input[5] >= 3.5) {
                        var16 = params[157];
                    } else {
                        var16 = params[158];
                    }
                } else {
                    if (input[4] >= 0.5) {
                        var16 = params[159];
                    } else {
                        var16 = params[160];
                    }
                }
            }
        } else {
            var16 = params[161];
        }
        double var17;
        if (input[0] >= -0.5547624) {
            if (input[5] >= 1.5) {
                if (input[6] >= 2.5) {
                    if (input[5] >= 3.5) {
                        var17 = params[162];
                    } else {
                        var17 = params[163];
                    }
                } else {
                    var17 = params[164];
                }
            } else {
                if (input[6] >= 2.5) {
                    var17 = params[165];
                } else {
                    if (input[4] >= 0.5) {
                        var17 = params[166];
                    } else {
                        var17 = params[167];
                    }
                }
            }
        } else {
            if (input[0] >= -0.7322315) {
                if (input[5] >= 1.5) {
                    if (input[5] >= 2.5) {
                        var17 = params[168];
                    } else {
                        var17 = params[169];
                    }
                } else {
                    var17 = params[170];
                }
            } else {
                if (input[5] >= 1.5) {
                    if (input[6] >= 4.5) {
                        var17 = params[171];
                    } else {
                        var17 = params[172];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var17 = params[173];
                    } else {
                        var17 = params[174];
                    }
                }
            }
        }
        double var18;
        if (input[1] >= 8.364355) {
            var18 = params[175];
        } else {
            if (input[0] >= -1.4246745) {
                if (input[0] >= -1.4068794) {
                    if (input[10] >= 0.5) {
                        var18 = params[176];
                    } else {
                        var18 = params[177];
                    }
                } else {
                    if (input[0] >= -1.4132736) {
                        var18 = params[178];
                    } else {
                        var18 = params[179];
                    }
                }
            } else {
                if (input[0] >= -1.4283543) {
                    var18 = params[180];
                } else {
                    if (input[0] >= -1.4378248) {
                        var18 = params[181];
                    } else {
                        var18 = params[182];
                    }
                }
            }
        }
        double var19;
        if (input[0] >= -1.2108912) {
            if (input[0] >= -1.1408567) {
                if (input[0] >= -1.0458486) {
                    if (input[0] >= -1.0361367) {
                        var19 = params[183];
                    } else {
                        var19 = params[184];
                    }
                } else {
                    if (input[0] >= -1.0512776) {
                        var19 = params[185];
                    } else {
                        var19 = params[186];
                    }
                }
            } else {
                if (input[6] >= 3.5) {
                    if (input[0] >= -1.1815746) {
                        var19 = params[187];
                    } else {
                        var19 = params[188];
                    }
                } else {
                    if (input[0] >= -1.1654683) {
                        var19 = params[189];
                    } else {
                        var19 = params[190];
                    }
                }
            }
        } else {
            if (input[0] >= -1.2362268) {
                if (input[0] >= -1.2230161) {
                    if (input[0] >= -1.2198191) {
                        var19 = params[191];
                    } else {
                        var19 = params[192];
                    }
                } else {
                    if (input[0] >= -1.2301946) {
                        var19 = params[193];
                    } else {
                        var19 = params[194];
                    }
                }
            } else {
                if (input[0] >= -1.626152) {
                    if (input[0] >= -1.4796284) {
                        var19 = params[195];
                    } else {
                        var19 = params[196];
                    }
                } else {
                    var19 = params[197];
                }
            }
        }
        double var20;
        if (input[0] >= -1.5935779) {
            if (input[3] >= 4.165) {
                if (input[0] >= -0.13805398) {
                    var20 = params[198];
                } else {
                    var20 = params[199];
                }
            } else {
                if (input[0] >= -1.5439323) {
                    if (input[0] >= -1.5247498) {
                        var20 = params[200];
                    } else {
                        var20 = params[201];
                    }
                } else {
                    var20 = params[202];
                }
            }
        } else {
            var20 = params[203];
        }
        double var21;
        if (input[0] >= -1.4271477) {
            if (input[0] >= -1.4095938) {
                if (input[0] >= -1.4035616) {
                    if (input[5] >= 1.5) {
                        var21 = params[204];
                    } else {
                        var21 = params[205];
                    }
                } else {
                    var21 = params[206];
                }
            } else {
                if (input[0] >= -1.4200296) {
                    var21 = params[207];
                } else {
                    var21 = params[208];
                }
            }
        } else {
            if (input[0] >= -1.474139) {
                if (input[0] >= -1.457309) {
                    if (input[0] >= -1.4399362) {
                        var21 = params[209];
                    } else {
                        var21 = params[210];
                    }
                } else {
                    var21 = params[211];
                }
            } else {
                if (input[6] >= 3.5) {
                    var21 = params[212];
                } else {
                    if (input[0] >= -1.5099707) {
                        var21 = params[213];
                    } else {
                        var21 = params[214];
                    }
                }
            }
        }
        double var22;
        if (input[0] >= -1.611011) {
            if (input[0] >= -1.5311439) {
                if (input[0] >= -1.5159426) {
                    if (input[0] >= -1.5049639) {
                        var22 = params[215];
                    } else {
                        var22 = params[216];
                    }
                } else {
                    var22 = params[217];
                }
            } else {
                var22 = params[218];
            }
        } else {
            var22 = params[219];
        }
        double var23;
        if (input[0] >= -1.6183101) {
            if (input[0] >= -1.5293946) {
                if (input[0] >= -1.5099707) {
                    if (input[0] >= -1.4928994) {
                        var23 = params[220];
                    } else {
                        var23 = params[221];
                    }
                } else {
                    var23 = params[222];
                }
            } else {
                var23 = params[223];
            }
        } else {
            var23 = params[224];
        }
        double var24;
        if (input[0] >= -1.5935779) {
            if (input[0] >= -1.4223219) {
                if (input[0] >= -1.4082668) {
                    if (input[10] >= 0.5) {
                        var24 = params[225];
                    } else {
                        var24 = params[226];
                    }
                } else {
                    var24 = params[227];
                }
            } else {
                if (input[0] >= -1.4271477) {
                    var24 = params[228];
                } else {
                    if (input[6] >= 2.5) {
                        var24 = params[229];
                    } else {
                        var24 = params[230];
                    }
                }
            }
        } else {
            var24 = params[231];
        }
        double var25;
        if (input[0] >= -1.626152) {
            if (input[0] >= -1.5473707) {
                if (input[6] >= 4.5) {
                    if (input[0] >= -0.46234822) {
                        var25 = params[232];
                    } else {
                        var25 = params[233];
                    }
                } else {
                    if (input[0] >= -1.5135298) {
                        var25 = params[234];
                    } else {
                        var25 = params[235];
                    }
                }
            } else {
                var25 = params[236];
            }
        } else {
            var25 = params[237];
        }
        double var26;
        if (input[0] >= 0.68209195) {
            if (input[0] >= 0.6971123) {
                if (input[4] >= 0.5) {
                    var26 = params[238];
                } else {
                    if (input[6] >= 3.5) {
                        var26 = params[239];
                    } else {
                        var26 = params[240];
                    }
                }
            } else {
                if (input[0] >= 0.68480647) {
                    var26 = params[241];
                } else {
                    var26 = params[242];
                }
            }
        } else {
            if (input[0] >= 0.6642365) {
                if (input[5] >= 2.5) {
                    var26 = params[243];
                } else {
                    var26 = params[244];
                }
            } else {
                if (input[0] >= 0.6590488) {
                    var26 = params[245];
                } else {
                    if (input[11] >= 0.5) {
                        var26 = params[246];
                    } else {
                        var26 = params[247];
                    }
                }
            }
        }
        double var27;
        if (input[5] >= 2.5) {
            if (input[5] >= 3.5) {
                var27 = params[248];
            } else {
                if (input[6] >= 3.5) {
                    var27 = params[249];
                } else {
                    var27 = params[250];
                }
            }
        } else {
            if (input[3] >= 1.385) {
                var27 = params[251];
            } else {
                if (input[6] >= 3.5) {
                    if (input[5] >= 1.5) {
                        var27 = params[252];
                    } else {
                        var27 = params[253];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var27 = params[254];
                    } else {
                        var27 = params[255];
                    }
                }
            }
        }
        double var28;
        if (input[0] >= -1.611011) {
            if (input[0] >= -1.4796284) {
                if (input[0] >= -1.438971) {
                    if (input[0] >= -1.42757) {
                        var28 = params[256];
                    } else {
                        var28 = params[257];
                    }
                } else {
                    if (input[0] >= -1.4554994) {
                        var28 = params[258];
                    } else {
                        var28 = params[259];
                    }
                }
            } else {
                if (input[0] >= -1.5135298) {
                    var28 = params[260];
                } else {
                    if (input[0] >= -1.5311439) {
                        var28 = params[261];
                    } else {
                        var28 = params[262];
                    }
                }
            }
        } else {
            var28 = params[263];
        }
        return 0.5 + (463.70804 + var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28);
    }
}
