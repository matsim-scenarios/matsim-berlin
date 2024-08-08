package org.matsim.prepare.network;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;


/**
* Generated model, do not modify.
* Model: XGBRegressor(alpha=0.04777680241937814, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.3660101036581986,
             eval_metric='mae', feature_types=None, gamma=0.059964922360269186,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.02205835468417265,
             learning_rate=0.3660101, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=8, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...)
*/
public final class BerlinNetworkParams_capacity_traffic_light implements Predictor {

    public static BerlinNetworkParams_capacity_traffic_light INSTANCE = new BerlinNetworkParams_capacity_traffic_light();
    public static final double[] DEFAULT_PARAMS = {120.49426, 144.23634, 176.85863, 141.85364, 190.75696, 222.93301, 265.98166, 218.37746, 435.78235, 356.49872, 532.8341, 415.45447, 442.81064, 340.13422, 371.5444, 410.15225, 59.037926, 88.06354, 64.639946, 119.052216, 118.94829, 195.70755, 92.24206, 164.79692, 256.24506, 299.74417, 85.83028, 147.62825, 371.17477, 226.61607, 231.30916, 318.83243, 18.345224, 42.45009, 56.17625, 82.335266, 89.46965, 71.07942, 109.97693, 79.931526, 144.59903, 189.5599, 211.52019, 135.29271, 128.19968, 149.30782, 194.29205, 167.53056, 26.6483, 2.5114083, 37.2103, 55.092377, 53.847168, 32.690998, 72.3803, 28.17736, 113.514275, 152.52472, -31.19217, 33.795063, 108.2306, 82.351135, 118.39192, 94.24523, -73.696, -19.979353, 14.157042, 29.819204, 57.32583, 26.149326, 22.975346, -14.209185, 4.0672297, 56.446335, -38.535393, -27.19353, 36.364372, 88.063545, 65.1905, 15.053817, 60.38553, 8.744071, -8.494278, 11.646749, -33.347908, 31.109875, 15.37061, -17.577547, -36.250965, -14.885776, 40.35406, 61.205334, 97.15457, 61.102592, -5.312496, 4.6384983, 11.336888, -2.5808558, -18.864212, 13.407413, 29.133062, 23.825205, -25.057493, -13.955483, -70.86994, 22.903076, -26.5507, 39.624695, 25.703302, 0.821612, -65.60758, -1.9834079, -62.237267, -23.240376, -67.95218, 40.810192, 9.882693, 31.784784, 12.784874, -14.660674, 20.694582, -11.028931, 101.85731, -67.88605, 1.4346863, -6.9392357, -23.357416, 6.526499, -0.07597236, 33.960964, -80.98684, -0.5791053, 7.7659125, -47.35109, 18.062014, 3.833865, -18.151062, 7.149092, 4.649397, -5.3530445, -7.371597, -15.70551, 28.88867, 1.0832975, -6.995907, 33.08812, -45.303284, 6.500982, -72.95547, 60.977, 13.577403, 2.1199994, -7.716016, -1.1638446, -15.602898, 3.8238697, 41.293682, -43.65799, 9.463854, 31.95385, 6.3125377, 9.451338, 32.53724, -19.548334, 6.157807, -12.018502, 9.6463, -45.882168, -16.784565, -9.321689, 2.0157843, -1.4258658, -56.869373, 6.543343, 21.873459, -80.23896, -17.8595, 16.517387, -21.567091, -48.5633, -0.29963833, 3.344198, -7.094839, -29.622002, -88.97607, -1.6944087, 14.728632, -48.01166, -0.5523689, 19.333853, 70.26248, 30.643167, 4.346452, -10.339705, -24.976522, 51.634693, 25.314564, 66.712715, 0.2617382, 15.793596, -3.0511916, 24.23845, 16.968756, 1.3424442, -42.0059, -2.6236048, 3.9660504, -2.808516, 9.43076, -15.892035, -52.07929, -0.46310726, -99.18235, -11.007244, 19.825527, -14.813451, -2.9147213, 13.790986, 40.65582, -13.091324, -0.04984132, 41.59082, -14.52366, 1.0103111, -19.534575, 23.991581, 37.35831, -74.48797, 41.18364, 81.24579, -42.973347, 59.772606, -81.346466, -66.18567, 23.055445, -1.4219692, 5.7437778, -20.75848, 7.9285054, -63.45346, 1.7404125, -33.24766, 43.850113, -4.9093533, 5.236828, -35.42528, -2.8403587, 2.3255866, -40.707363, 36.78737, 3.2079086, -15.61722, 31.449936, -41.47577, -3.6103203, 24.344547, 74.75395, 38.4803, -53.510635, -0.7233074, -0.035281874, 27.485872, -39.864906, 50.7327, -70.79828, -22.159994, -15.875036, -0.06120714, -31.281004, 0.52760744, -12.105713, 8.977068, -2.2936924, -36.259823, 76.4563, 0.046459302, 9.12112, 17.27247, 4.34476, -1.3091905, 2.7716637, -74.35873, 6.3124847, -8.228346, -28.945152, -11.320066, 64.02686, -22.054539, 4.2219057, -11.238134, 7.1784983, -54.661884, 29.802792, -35.07111, -53.024155, -7.791357, 18.132824, -2.8491168, -25.672855, -1.5283177, 1.3678759, 23.01982, -11.022074, 2.4540086, -7.761553, 14.117198, 49.42193, 4.623291, -16.403389, 41.573673, -1.6961627, -7.989792, -1.081079, 3.9185448, -30.934427, 13.549328, 20.424013, -1.3499842, 1.3837997, -1.5665351, 6.136379, -7.948176, -67.2593, 1.7542174, -12.849709, -34.455315, -25.372015, 18.109741, 32.060146, -9.2019005, 1.7474107, -1.8326497, 39.67979, 17.258509, 6.7043233, 0.5433281, -131.06569, -11.020212, -43.662212, -20.632881, 6.9032445, 56.387096, 18.296072, -29.94616, 2.6680706, 11.377827, -27.659697, 13.2376585, 55.71633, -0.3144063, -7.0986767, 9.998999, -0.37414578, -64.27474, -4.3082685, 35.013187, 5.6364393, -47.026714, -0.17854747, 2.5423157, -0.92739356, -12.940792, -66.22313, -16.891813, 43.30377, -4.7439427, -20.530468, -22.655607, 15.102622, -35.644787, 36.95956, -35.41971, -7.7455697, -0.013062295, 12.402798, 3.2888505, -34.729126, 1.5855132, -16.979069, 29.092934, 10.36071, 13.937312, -5.101226, 38.229256, 32.026665, -1.6190432, 0.5351575, 10.357025, -30.88293, 3.1167831, -7.556241, 5.5443535, 0.2929742, 3.217742, 22.01282, 1.450663, -43.770924, -14.019111, -66.206505, 35.423653, -19.071333, 8.978611, -5.453257, -6.1721787, 18.963472, -0.25508207, -42.796616, 0.016214658, -19.092773, 2.2526999, 9.626387, -16.032166, 8.648903, -2.5534189, -19.078411, -11.62906, 2.8987596, 55.95079, 5.0766244, 0.7850271, -5.0118766, -0.073889196, -6.806319, 0.04729633, 7.1832604, 25.577148, -62.06377, -6.672181, -22.133873, 66.0882, -0.80976695, -38.63526, 19.23159, 35.614384, 27.911171, -2.0380037};

    @Override
    public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
        return predict(features, categories, DEFAULT_PARAMS);
    }

    @Override
    public double[] getData(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
        double[] data = new double[12];
		data[0] = features.getDouble("speed");
		data[1] = features.getDouble("num_lanes");
		data[2] = features.getDouble("num_to_links");
		data[3] = features.getDouble("junction_inc_lanes");
		data[4] = features.getDouble("num_conns");
		data[5] = features.getDouble("num_response");
		data[6] = features.getDouble("num_foes");
		data[7] = features.getDouble("is_primary_or_higher");
		data[8] = features.getDouble("is_secondary_or_higher");
		data[9] = features.getDouble("num_left");
		data[10] = features.getDouble("num_right");
		data[11] = features.getDouble("num_straight");

        return data;
    }

    @Override
    public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories, double[] params) {

        double[] data = getData(features, categories);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);

        double scale = 1;
        // The reference simulation is performed with equal green split
        // However this is usually not the case for major roads with higher priority,
        // there capacity for these roads is increased
        if (features.getDouble("is_primary_or_higher") == 1)
            scale = 1.5;
        else if (features.getDouble("is_secondary_or_higher") == 1)
            scale = 1.3;

        return score(data, params) * scale;
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[1] >= 1.5) {
            if (input[1] >= 2.5) {
                if (input[2] >= 2.5) {
                    if (input[8] >= 0.5) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[2] >= 2.5) {
                    if (input[6] >= 9.5) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                }
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[6] >= 9.5) {
                    if (input[0] >= 11.110001) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                }
            } else {
                if (input[0] >= 9.719999) {
                    if (input[4] >= 2.5) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                } else {
                    if (input[6] >= 9.5) {
                        var0 = params[14];
                    } else {
                        var0 = params[15];
                    }
                }
            }
        }
        double var1;
        if (input[1] >= 1.5) {
            if (input[1] >= 2.5) {
                if (input[2] >= 2.5) {
                    if (input[1] >= 3.5) {
                        var1 = params[16];
                    } else {
                        var1 = params[17];
                    }
                } else {
                    if (input[1] >= 3.5) {
                        var1 = params[18];
                    } else {
                        var1 = params[19];
                    }
                }
            } else {
                if (input[2] >= 2.5) {
                    if (input[3] >= 4.5) {
                        var1 = params[20];
                    } else {
                        var1 = params[21];
                    }
                } else {
                    if (input[3] >= 8.5) {
                        var1 = params[22];
                    } else {
                        var1 = params[23];
                    }
                }
            }
        } else {
            if (input[3] >= 3.5) {
                if (input[4] >= 1.5) {
                    if (input[6] >= 5.5) {
                        var1 = params[24];
                    } else {
                        var1 = params[25];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var1 = params[26];
                    } else {
                        var1 = params[27];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[6] >= 4.5) {
                        var1 = params[28];
                    } else {
                        var1 = params[29];
                    }
                } else {
                    if (input[4] >= 2.5) {
                        var1 = params[30];
                    } else {
                        var1 = params[31];
                    }
                }
            }
        }
        double var2;
        if (input[1] >= 1.5) {
            if (input[1] >= 2.5) {
                if (input[1] >= 3.5) {
                    if (input[1] >= 4.5) {
                        var2 = params[32];
                    } else {
                        var2 = params[33];
                    }
                } else {
                    if (input[6] >= 8.5) {
                        var2 = params[34];
                    } else {
                        var2 = params[35];
                    }
                }
            } else {
                if (input[6] >= 10.5) {
                    if (input[11] >= 2.5) {
                        var2 = params[36];
                    } else {
                        var2 = params[37];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var2 = params[38];
                    } else {
                        var2 = params[39];
                    }
                }
            }
        } else {
            if (input[0] >= 9.719999) {
                if (input[3] >= 4.5) {
                    if (input[6] >= 9.5) {
                        var2 = params[40];
                    } else {
                        var2 = params[41];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var2 = params[42];
                    } else {
                        var2 = params[43];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[5] >= 8.5) {
                        var2 = params[44];
                    } else {
                        var2 = params[45];
                    }
                } else {
                    if (input[10] >= 1.5) {
                        var2 = params[46];
                    } else {
                        var2 = params[47];
                    }
                }
            }
        }
        double var3;
        if (input[1] >= 1.5) {
            if (input[1] >= 2.5) {
                if (input[1] >= 3.5) {
                    if (input[2] >= 1.5) {
                        var3 = params[48];
                    } else {
                        var3 = params[49];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var3 = params[50];
                    } else {
                        var3 = params[51];
                    }
                }
            } else {
                if (input[6] >= 8.5) {
                    if (input[0] >= 11.110001) {
                        var3 = params[52];
                    } else {
                        var3 = params[53];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var3 = params[54];
                    } else {
                        var3 = params[55];
                    }
                }
            }
        } else {
            if (input[0] >= 9.719999) {
                if (input[4] >= 1.5) {
                    if (input[3] >= 3.5) {
                        var3 = params[56];
                    } else {
                        var3 = params[57];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var3 = params[58];
                    } else {
                        var3 = params[59];
                    }
                }
            } else {
                if (input[6] >= 9.5) {
                    if (input[3] >= 6.5) {
                        var3 = params[60];
                    } else {
                        var3 = params[61];
                    }
                } else {
                    if (input[3] >= 4.5) {
                        var3 = params[62];
                    } else {
                        var3 = params[63];
                    }
                }
            }
        }
        double var4;
        if (input[1] >= 1.5) {
            if (input[3] >= 5.5) {
                if (input[0] >= 20.83) {
                    if (input[4] >= 3.5) {
                        var4 = params[64];
                    } else {
                        var4 = params[65];
                    }
                } else {
                    if (input[1] >= 3.5) {
                        var4 = params[66];
                    } else {
                        var4 = params[67];
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[0] >= 11.110001) {
                        var4 = params[68];
                    } else {
                        var4 = params[69];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var4 = params[70];
                    } else {
                        var4 = params[71];
                    }
                }
            }
        } else {
            if (input[6] >= 11.5) {
                if (input[11] >= 0.5) {
                    if (input[11] >= 2.5) {
                        var4 = params[72];
                    } else {
                        var4 = params[73];
                    }
                } else {
                    var4 = params[74];
                }
            } else {
                if (input[0] >= 15.280001) {
                    if (input[0] >= 25.0) {
                        var4 = params[75];
                    } else {
                        var4 = params[76];
                    }
                } else {
                    if (input[0] >= 9.719999) {
                        var4 = params[77];
                    } else {
                        var4 = params[78];
                    }
                }
            }
        }
        double var5;
        if (input[1] >= 1.5) {
            if (input[2] >= 2.5) {
                if (input[0] >= 9.719999) {
                    if (input[3] >= 4.5) {
                        var5 = params[79];
                    } else {
                        var5 = params[80];
                    }
                } else {
                    if (input[3] >= 8.5) {
                        var5 = params[81];
                    } else {
                        var5 = params[82];
                    }
                }
            } else {
                if (input[3] >= 8.5) {
                    if (input[2] >= 1.5) {
                        var5 = params[83];
                    } else {
                        var5 = params[84];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var5 = params[85];
                    } else {
                        var5 = params[86];
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                if (input[0] >= 18.055) {
                    if (input[3] >= 5.5) {
                        var5 = params[87];
                    } else {
                        var5 = params[88];
                    }
                } else {
                    if (input[11] >= 2.5) {
                        var5 = params[89];
                    } else {
                        var5 = params[90];
                    }
                }
            } else {
                if (input[4] >= 2.5) {
                    if (input[3] >= 3.5) {
                        var5 = params[91];
                    } else {
                        var5 = params[92];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var5 = params[93];
                    } else {
                        var5 = params[94];
                    }
                }
            }
        }
        double var6;
        if (input[1] >= 1.5) {
            if (input[1] >= 2.5) {
                if (input[0] >= 9.719999) {
                    if (input[2] >= 2.5) {
                        var6 = params[95];
                    } else {
                        var6 = params[96];
                    }
                } else {
                    if (input[11] >= 2.5) {
                        var6 = params[97];
                    } else {
                        var6 = params[98];
                    }
                }
            } else {
                if (input[4] >= 2.5) {
                    if (input[10] >= 0.5) {
                        var6 = params[99];
                    } else {
                        var6 = params[100];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var6 = params[101];
                    } else {
                        var6 = params[102];
                    }
                }
            }
        } else {
            if (input[2] >= 3.5) {
                if (input[5] >= 11.5) {
                    var6 = params[103];
                } else {
                    var6 = params[104];
                }
            } else {
                if (input[6] >= 11.5) {
                    if (input[2] >= 2.5) {
                        var6 = params[105];
                    } else {
                        var6 = params[106];
                    }
                } else {
                    if (input[0] >= 9.719999) {
                        var6 = params[107];
                    } else {
                        var6 = params[108];
                    }
                }
            }
        }
        double var7;
        if (input[3] >= 4.5) {
            if (input[0] >= 18.055) {
                if (input[0] >= 20.83) {
                    if (input[3] >= 7.5) {
                        var7 = params[109];
                    } else {
                        var7 = params[110];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var7 = params[111];
                    } else {
                        var7 = params[112];
                    }
                }
            } else {
                if (input[2] >= 3.5) {
                    if (input[0] >= 9.719999) {
                        var7 = params[113];
                    } else {
                        var7 = params[114];
                    }
                } else {
                    if (input[9] >= 2.5) {
                        var7 = params[115];
                    } else {
                        var7 = params[116];
                    }
                }
            }
        } else {
            if (input[2] >= 1.5) {
                if (input[11] >= 0.5) {
                    if (input[0] >= 9.719999) {
                        var7 = params[117];
                    } else {
                        var7 = params[118];
                    }
                } else {
                    if (input[0] >= 12.5) {
                        var7 = params[119];
                    } else {
                        var7 = params[120];
                    }
                }
            } else {
                if (input[4] >= 2.5) {
                    if (input[1] >= 1.5) {
                        var7 = params[121];
                    } else {
                        var7 = params[122];
                    }
                } else {
                    if (input[3] >= 3.5) {
                        var7 = params[123];
                    } else {
                        var7 = params[124];
                    }
                }
            }
        }
        double var8;
        if (input[1] >= 2.5) {
            if (input[6] >= 1.5) {
                if (input[1] >= 4.5) {
                    if (input[7] >= 0.5) {
                        var8 = params[125];
                    } else {
                        var8 = params[126];
                    }
                } else {
                    if (input[3] >= 11.5) {
                        var8 = params[127];
                    } else {
                        var8 = params[128];
                    }
                }
            } else {
                var8 = params[129];
            }
        } else {
            if (input[0] >= 25.0) {
                if (input[6] >= 10.0) {
                    var8 = params[130];
                } else {
                    var8 = params[131];
                }
            } else {
                if (input[6] >= 6.5) {
                    if (input[2] >= 1.5) {
                        var8 = params[132];
                    } else {
                        var8 = params[133];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var8 = params[134];
                    } else {
                        var8 = params[135];
                    }
                }
            }
        }
        double var9;
        if (input[1] >= 1.5) {
            if (input[0] >= 9.719999) {
                if (input[0] >= 18.055) {
                    if (input[3] >= 6.5) {
                        var9 = params[136];
                    } else {
                        var9 = params[137];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var9 = params[138];
                    } else {
                        var9 = params[139];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var9 = params[140];
                    } else {
                        var9 = params[141];
                    }
                } else {
                    if (input[10] >= 1.5) {
                        var9 = params[142];
                    } else {
                        var9 = params[143];
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                if (input[3] >= 7.5) {
                    if (input[11] >= 1.5) {
                        var9 = params[144];
                    } else {
                        var9 = params[145];
                    }
                } else {
                    if (input[11] >= 2.5) {
                        var9 = params[146];
                    } else {
                        var9 = params[147];
                    }
                }
            } else {
                if (input[5] >= 7.5) {
                    var9 = params[148];
                } else {
                    if (input[4] >= 4.5) {
                        var9 = params[149];
                    } else {
                        var9 = params[150];
                    }
                }
            }
        }
        double var10;
        if (input[6] >= 11.5) {
            if (input[5] >= 2.5) {
                if (input[4] >= 5.5) {
                    if (input[10] >= 1.5) {
                        var10 = params[151];
                    } else {
                        var10 = params[152];
                    }
                } else {
                    if (input[3] >= 5.5) {
                        var10 = params[153];
                    } else {
                        var10 = params[154];
                    }
                }
            } else {
                if (input[4] >= 5.5) {
                    if (input[11] >= 1.5) {
                        var10 = params[155];
                    } else {
                        var10 = params[156];
                    }
                } else {
                    if (input[9] >= 1.5) {
                        var10 = params[157];
                    } else {
                        var10 = params[158];
                    }
                }
            }
        } else {
            if (input[10] >= 1.5) {
                if (input[4] >= 5.5) {
                    if (input[5] >= 1.5) {
                        var10 = params[159];
                    } else {
                        var10 = params[160];
                    }
                } else {
                    if (input[4] >= 3.5) {
                        var10 = params[161];
                    } else {
                        var10 = params[162];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[9] >= 1.5) {
                        var10 = params[163];
                    } else {
                        var10 = params[164];
                    }
                } else {
                    if (input[0] >= 12.5) {
                        var10 = params[165];
                    } else {
                        var10 = params[166];
                    }
                }
            }
        }
        double var11;
        if (input[2] >= 1.5) {
            if (input[10] >= 0.5) {
                if (input[2] >= 3.5) {
                    if (input[8] >= 0.5) {
                        var11 = params[167];
                    } else {
                        var11 = params[168];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var11 = params[169];
                    } else {
                        var11 = params[170];
                    }
                }
            } else {
                if (input[9] >= 1.5) {
                    if (input[11] >= 1.5) {
                        var11 = params[171];
                    } else {
                        var11 = params[172];
                    }
                } else {
                    if (input[3] >= 5.5) {
                        var11 = params[173];
                    } else {
                        var11 = params[174];
                    }
                }
            }
        } else {
            if (input[9] >= 1.5) {
                var11 = params[175];
            } else {
                if (input[4] >= 1.5) {
                    if (input[11] >= 1.5) {
                        var11 = params[176];
                    } else {
                        var11 = params[177];
                    }
                } else {
                    if (input[3] >= 2.5) {
                        var11 = params[178];
                    } else {
                        var11 = params[179];
                    }
                }
            }
        }
        double var12;
        if (input[5] >= 0.5) {
            if (input[6] >= 1.5) {
                if (input[6] >= 3.5) {
                    if (input[1] >= 1.5) {
                        var12 = params[180];
                    } else {
                        var12 = params[181];
                    }
                } else {
                    if (input[1] >= 1.5) {
                        var12 = params[182];
                    } else {
                        var12 = params[183];
                    }
                }
            } else {
                var12 = params[184];
            }
        } else {
            if (input[6] >= 4.5) {
                if (input[2] >= 1.5) {
                    if (input[10] >= 0.5) {
                        var12 = params[185];
                    } else {
                        var12 = params[186];
                    }
                } else {
                    var12 = params[187];
                }
            } else {
                if (input[3] >= 3.5) {
                    if (input[11] >= 1.5) {
                        var12 = params[188];
                    } else {
                        var12 = params[189];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var12 = params[190];
                    } else {
                        var12 = params[191];
                    }
                }
            }
        }
        double var13;
        if (input[9] >= 2.5) {
            if (input[1] >= 2.5) {
                if (input[10] >= 0.5) {
                    if (input[3] >= 11.0) {
                        var13 = params[192];
                    } else {
                        var13 = params[193];
                    }
                } else {
                    var13 = params[194];
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[3] >= 7.5) {
                        var13 = params[195];
                    } else {
                        var13 = params[196];
                    }
                } else {
                    var13 = params[197];
                }
            }
        } else {
            if (input[3] >= 4.5) {
                if (input[9] >= 1.5) {
                    if (input[6] >= 11.5) {
                        var13 = params[198];
                    } else {
                        var13 = params[199];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var13 = params[200];
                    } else {
                        var13 = params[201];
                    }
                }
            } else {
                if (input[2] >= 1.5) {
                    if (input[11] >= 1.5) {
                        var13 = params[202];
                    } else {
                        var13 = params[203];
                    }
                } else {
                    if (input[11] >= 1.5) {
                        var13 = params[204];
                    } else {
                        var13 = params[205];
                    }
                }
            }
        }
        double var14;
        if (input[6] >= 5.5) {
            if (input[3] >= 7.5) {
                if (input[1] >= 2.5) {
                    if (input[9] >= 1.5) {
                        var14 = params[206];
                    } else {
                        var14 = params[207];
                    }
                } else {
                    if (input[2] >= 2.5) {
                        var14 = params[208];
                    } else {
                        var14 = params[209];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[0] >= 20.83) {
                        var14 = params[210];
                    } else {
                        var14 = params[211];
                    }
                } else {
                    if (input[6] >= 8.5) {
                        var14 = params[212];
                    } else {
                        var14 = params[213];
                    }
                }
            }
        } else {
            if (input[11] >= 0.5) {
                if (input[9] >= 0.5) {
                    if (input[0] >= 11.110001) {
                        var14 = params[214];
                    } else {
                        var14 = params[215];
                    }
                } else {
                    if (input[1] >= 1.5) {
                        var14 = params[216];
                    } else {
                        var14 = params[217];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[9] >= 1.5) {
                        var14 = params[218];
                    } else {
                        var14 = params[219];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var14 = params[220];
                    } else {
                        var14 = params[221];
                    }
                }
            }
        }
        double var15;
        if (input[4] >= 1.5) {
            if (input[6] >= 1.5) {
                if (input[4] >= 2.5) {
                    if (input[2] >= 3.5) {
                        var15 = params[222];
                    } else {
                        var15 = params[223];
                    }
                } else {
                    if (input[0] >= 12.5) {
                        var15 = params[224];
                    } else {
                        var15 = params[225];
                    }
                }
            } else {
                if (input[11] >= 1.5) {
                    if (input[7] >= 0.5) {
                        var15 = params[226];
                    } else {
                        var15 = params[227];
                    }
                } else {
                    if (input[3] >= 4.5) {
                        var15 = params[228];
                    } else {
                        var15 = params[229];
                    }
                }
            }
        } else {
            if (input[6] >= 1.5) {
                if (input[3] >= 4.5) {
                    var15 = params[230];
                } else {
                    var15 = params[231];
                }
            } else {
                if (input[5] >= 0.5) {
                    var15 = params[232];
                } else {
                    if (input[3] >= 2.5) {
                        var15 = params[233];
                    } else {
                        var15 = params[234];
                    }
                }
            }
        }
        double var16;
        if (input[0] >= 9.719999) {
            if (input[11] >= 0.5) {
                if (input[3] >= 4.5) {
                    if (input[6] >= 7.5) {
                        var16 = params[235];
                    } else {
                        var16 = params[236];
                    }
                } else {
                    if (input[9] >= 1.5) {
                        var16 = params[237];
                    } else {
                        var16 = params[238];
                    }
                }
            } else {
                if (input[3] >= 3.5) {
                    if (input[3] >= 10.5) {
                        var16 = params[239];
                    } else {
                        var16 = params[240];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var16 = params[241];
                    } else {
                        var16 = params[242];
                    }
                }
            }
        } else {
            if (input[11] >= 0.5) {
                if (input[3] >= 5.5) {
                    if (input[6] >= 11.5) {
                        var16 = params[243];
                    } else {
                        var16 = params[244];
                    }
                } else {
                    if (input[3] >= 4.5) {
                        var16 = params[245];
                    } else {
                        var16 = params[246];
                    }
                }
            } else {
                if (input[5] >= 4.5) {
                    if (input[3] >= 4.5) {
                        var16 = params[247];
                    } else {
                        var16 = params[248];
                    }
                } else {
                    if (input[9] >= 1.5) {
                        var16 = params[249];
                    } else {
                        var16 = params[250];
                    }
                }
            }
        }
        double var17;
        if (input[9] >= 2.5) {
            if (input[5] >= 8.5) {
                if (input[1] >= 1.5) {
                    if (input[3] >= 8.5) {
                        var17 = params[251];
                    } else {
                        var17 = params[252];
                    }
                } else {
                    var17 = params[253];
                }
            } else {
                if (input[1] >= 1.5) {
                    if (input[5] >= 6.5) {
                        var17 = params[254];
                    } else {
                        var17 = params[255];
                    }
                } else {
                    if (input[0] >= 11.110001) {
                        var17 = params[256];
                    } else {
                        var17 = params[257];
                    }
                }
            }
        } else {
            if (input[4] >= 1.5) {
                if (input[0] >= 25.0) {
                    if (input[5] >= 2.5) {
                        var17 = params[258];
                    } else {
                        var17 = params[259];
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var17 = params[260];
                    } else {
                        var17 = params[261];
                    }
                }
            } else {
                if (input[6] >= 1.5) {
                    if (input[5] >= 1.5) {
                        var17 = params[262];
                    } else {
                        var17 = params[263];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var17 = params[264];
                    } else {
                        var17 = params[265];
                    }
                }
            }
        }
        double var18;
        if (input[3] >= 2.5) {
            if (input[0] >= 18.055) {
                if (input[6] >= 2.5) {
                    if (input[5] >= 3.5) {
                        var18 = params[266];
                    } else {
                        var18 = params[267];
                    }
                } else {
                    var18 = params[268];
                }
            } else {
                if (input[6] >= 5.5) {
                    if (input[4] >= 3.5) {
                        var18 = params[269];
                    } else {
                        var18 = params[270];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var18 = params[271];
                    } else {
                        var18 = params[272];
                    }
                }
            }
        } else {
            if (input[5] >= 0.5) {
                var18 = params[273];
            } else {
                var18 = params[274];
            }
        }
        double var19;
        if (input[6] >= 4.5) {
            if (input[3] >= 8.5) {
                if (input[11] >= 1.5) {
                    if (input[6] >= 8.5) {
                        var19 = params[275];
                    } else {
                        var19 = params[276];
                    }
                } else {
                    if (input[6] >= 11.5) {
                        var19 = params[277];
                    } else {
                        var19 = params[278];
                    }
                }
            } else {
                if (input[4] >= 2.5) {
                    if (input[6] >= 5.5) {
                        var19 = params[279];
                    } else {
                        var19 = params[280];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var19 = params[281];
                    } else {
                        var19 = params[282];
                    }
                }
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[0] >= 12.5) {
                    if (input[3] >= 4.5) {
                        var19 = params[283];
                    } else {
                        var19 = params[284];
                    }
                } else {
                    if (input[4] >= 2.5) {
                        var19 = params[285];
                    } else {
                        var19 = params[286];
                    }
                }
            } else {
                if (input[11] >= 3.5) {
                    var19 = params[287];
                } else {
                    if (input[0] >= 11.110001) {
                        var19 = params[288];
                    } else {
                        var19 = params[289];
                    }
                }
            }
        }
        double var20;
        if (input[2] >= 3.5) {
            if (input[9] >= 1.5) {
                if (input[11] >= 1.5) {
                    if (input[1] >= 2.5) {
                        var20 = params[290];
                    } else {
                        var20 = params[291];
                    }
                } else {
                    if (input[0] >= 9.719999) {
                        var20 = params[292];
                    } else {
                        var20 = params[293];
                    }
                }
            } else {
                var20 = params[294];
            }
        } else {
            if (input[0] >= 18.055) {
                if (input[0] >= 20.83) {
                    if (input[2] >= 2.5) {
                        var20 = params[295];
                    } else {
                        var20 = params[296];
                    }
                } else {
                    if (input[1] >= 1.5) {
                        var20 = params[297];
                    } else {
                        var20 = params[298];
                    }
                }
            } else {
                if (input[4] >= 2.5) {
                    if (input[1] >= 2.5) {
                        var20 = params[299];
                    } else {
                        var20 = params[300];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var20 = params[301];
                    } else {
                        var20 = params[302];
                    }
                }
            }
        }
        double var21;
        if (input[11] >= 2.5) {
            if (input[6] >= 7.5) {
                if (input[3] >= 5.5) {
                    if (input[0] >= 9.719999) {
                        var21 = params[303];
                    } else {
                        var21 = params[304];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var21 = params[305];
                    } else {
                        var21 = params[306];
                    }
                }
            } else {
                if (input[3] >= 3.5) {
                    if (input[7] >= 0.5) {
                        var21 = params[307];
                    } else {
                        var21 = params[308];
                    }
                } else {
                    var21 = params[309];
                }
            }
        } else {
            if (input[2] >= 1.5) {
                if (input[1] >= 1.5) {
                    if (input[0] >= 12.5) {
                        var21 = params[310];
                    } else {
                        var21 = params[311];
                    }
                } else {
                    if (input[6] >= 5.5) {
                        var21 = params[312];
                    } else {
                        var21 = params[313];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[6] >= 2.5) {
                        var21 = params[314];
                    } else {
                        var21 = params[315];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var21 = params[316];
                    } else {
                        var21 = params[317];
                    }
                }
            }
        }
        double var22;
        if (input[2] >= 1.5) {
            if (input[3] >= 3.5) {
                if (input[3] >= 4.5) {
                    if (input[3] >= 7.5) {
                        var22 = params[318];
                    } else {
                        var22 = params[319];
                    }
                } else {
                    if (input[6] >= 9.5) {
                        var22 = params[320];
                    } else {
                        var22 = params[321];
                    }
                }
            } else {
                if (input[6] >= 10.5) {
                    var22 = params[322];
                } else {
                    if (input[0] >= 9.719999) {
                        var22 = params[323];
                    } else {
                        var22 = params[324];
                    }
                }
            }
        } else {
            if (input[9] >= 1.5) {
                var22 = params[325];
            } else {
                if (input[8] >= 0.5) {
                    if (input[6] >= 6.5) {
                        var22 = params[326];
                    } else {
                        var22 = params[327];
                    }
                } else {
                    if (input[3] >= 5.5) {
                        var22 = params[328];
                    } else {
                        var22 = params[329];
                    }
                }
            }
        }
        double var23;
        if (input[2] >= 2.5) {
            if (input[5] >= 0.5) {
                if (input[6] >= 8.5) {
                    if (input[5] >= 7.5) {
                        var23 = params[330];
                    } else {
                        var23 = params[331];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var23 = params[332];
                    } else {
                        var23 = params[333];
                    }
                }
            } else {
                if (input[3] >= 5.5) {
                    if (input[6] >= 8.5) {
                        var23 = params[334];
                    } else {
                        var23 = params[335];
                    }
                } else {
                    var23 = params[336];
                }
            }
        } else {
            if (input[3] >= 8.5) {
                if (input[6] >= 8.5) {
                    if (input[11] >= 2.5) {
                        var23 = params[337];
                    } else {
                        var23 = params[338];
                    }
                } else {
                    if (input[11] >= 3.5) {
                        var23 = params[339];
                    } else {
                        var23 = params[340];
                    }
                }
            } else {
                if (input[5] >= 11.5) {
                    if (input[9] >= 1.5) {
                        var23 = params[341];
                    } else {
                        var23 = params[342];
                    }
                } else {
                    if (input[5] >= 9.5) {
                        var23 = params[343];
                    } else {
                        var23 = params[344];
                    }
                }
            }
        }
        double var24;
        if (input[4] >= 1.5) {
            if (input[9] >= 2.5) {
                if (input[4] >= 5.5) {
                    if (input[10] >= 1.5) {
                        var24 = params[345];
                    } else {
                        var24 = params[346];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var24 = params[347];
                    } else {
                        var24 = params[348];
                    }
                }
            } else {
                if (input[5] >= 3.5) {
                    if (input[6] >= 7.5) {
                        var24 = params[349];
                    } else {
                        var24 = params[350];
                    }
                } else {
                    if (input[4] >= 5.5) {
                        var24 = params[351];
                    } else {
                        var24 = params[352];
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var24 = params[353];
                    } else {
                        var24 = params[354];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var24 = params[355];
                    } else {
                        var24 = params[356];
                    }
                }
            } else {
                var24 = params[357];
            }
        }
        double var25;
        if (input[6] >= 3.5) {
            if (input[4] >= 2.5) {
                if (input[0] >= 9.719999) {
                    if (input[5] >= 3.5) {
                        var25 = params[358];
                    } else {
                        var25 = params[359];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var25 = params[360];
                    } else {
                        var25 = params[361];
                    }
                }
            } else {
                if (input[3] >= 7.5) {
                    var25 = params[362];
                } else {
                    if (input[7] >= 0.5) {
                        var25 = params[363];
                    } else {
                        var25 = params[364];
                    }
                }
            }
        } else {
            if (input[3] >= 4.5) {
                if (input[1] >= 2.5) {
                    if (input[4] >= 3.5) {
                        var25 = params[365];
                    } else {
                        var25 = params[366];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var25 = params[367];
                    } else {
                        var25 = params[368];
                    }
                }
            } else {
                if (input[0] >= 15.280001) {
                    if (input[2] >= 1.5) {
                        var25 = params[369];
                    } else {
                        var25 = params[370];
                    }
                } else {
                    if (input[11] >= 1.5) {
                        var25 = params[371];
                    } else {
                        var25 = params[372];
                    }
                }
            }
        }
        double var26;
        if (input[5] >= 7.5) {
            if (input[5] >= 8.5) {
                if (input[0] >= 11.110001) {
                    if (input[1] >= 1.5) {
                        var26 = params[373];
                    } else {
                        var26 = params[374];
                    }
                } else {
                    if (input[5] >= 9.5) {
                        var26 = params[375];
                    } else {
                        var26 = params[376];
                    }
                }
            } else {
                if (input[0] >= 11.110001) {
                    if (input[1] >= 1.5) {
                        var26 = params[377];
                    } else {
                        var26 = params[378];
                    }
                } else {
                    if (input[6] >= 9.5) {
                        var26 = params[379];
                    } else {
                        var26 = params[380];
                    }
                }
            }
        } else {
            if (input[9] >= 2.5) {
                if (input[1] >= 1.5) {
                    if (input[10] >= 1.5) {
                        var26 = params[381];
                    } else {
                        var26 = params[382];
                    }
                } else {
                    if (input[6] >= 9.5) {
                        var26 = params[383];
                    } else {
                        var26 = params[384];
                    }
                }
            } else {
                if (input[4] >= 1.5) {
                    if (input[6] >= 8.5) {
                        var26 = params[385];
                    } else {
                        var26 = params[386];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var26 = params[387];
                    } else {
                        var26 = params[388];
                    }
                }
            }
        }
        double var27;
        if (input[4] >= 3.5) {
            if (input[1] >= 1.5) {
                if (input[5] >= 11.5) {
                    if (input[3] >= 11.5) {
                        var27 = params[389];
                    } else {
                        var27 = params[390];
                    }
                } else {
                    if (input[5] >= 8.5) {
                        var27 = params[391];
                    } else {
                        var27 = params[392];
                    }
                }
            } else {
                if (input[11] >= 1.5) {
                    if (input[2] >= 2.5) {
                        var27 = params[393];
                    } else {
                        var27 = params[394];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var27 = params[395];
                    } else {
                        var27 = params[396];
                    }
                }
            }
        } else {
            if (input[6] >= 6.5) {
                if (input[9] >= 0.5) {
                    if (input[1] >= 1.5) {
                        var27 = params[397];
                    } else {
                        var27 = params[398];
                    }
                } else {
                    if (input[5] >= 4.5) {
                        var27 = params[399];
                    } else {
                        var27 = params[400];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[6] >= 4.5) {
                        var27 = params[401];
                    } else {
                        var27 = params[402];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var27 = params[403];
                    } else {
                        var27 = params[404];
                    }
                }
            }
        }
        double var28;
        if (input[3] >= 7.5) {
            if (input[1] >= 2.5) {
                if (input[10] >= 2.5) {
                    if (input[3] >= 11.5) {
                        var28 = params[405];
                    } else {
                        var28 = params[406];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var28 = params[407];
                    } else {
                        var28 = params[408];
                    }
                }
            } else {
                if (input[2] >= 2.5) {
                    if (input[10] >= 1.5) {
                        var28 = params[409];
                    } else {
                        var28 = params[410];
                    }
                } else {
                    if (input[5] >= 3.5) {
                        var28 = params[411];
                    } else {
                        var28 = params[412];
                    }
                }
            }
        } else {
            if (input[2] >= 2.5) {
                if (input[3] >= 4.5) {
                    if (input[3] >= 5.5) {
                        var28 = params[413];
                    } else {
                        var28 = params[414];
                    }
                } else {
                    if (input[9] >= 1.5) {
                        var28 = params[415];
                    } else {
                        var28 = params[416];
                    }
                }
            } else {
                if (input[4] >= 4.5) {
                    if (input[5] >= 11.5) {
                        var28 = params[417];
                    } else {
                        var28 = params[418];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var28 = params[419];
                    } else {
                        var28 = params[420];
                    }
                }
            }
        }
        double var29;
        if (input[4] >= 3.5) {
            if (input[3] >= 3.5) {
                if (input[1] >= 3.5) {
                    if (input[7] >= 0.5) {
                        var29 = params[421];
                    } else {
                        var29 = params[422];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var29 = params[423];
                    } else {
                        var29 = params[424];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    var29 = params[425];
                } else {
                    if (input[6] >= 10.5) {
                        var29 = params[426];
                    } else {
                        var29 = params[427];
                    }
                }
            }
        } else {
            if (input[6] >= 6.5) {
                if (input[5] >= 6.5) {
                    if (input[1] >= 1.5) {
                        var29 = params[428];
                    } else {
                        var29 = params[429];
                    }
                } else {
                    if (input[1] >= 1.5) {
                        var29 = params[430];
                    } else {
                        var29 = params[431];
                    }
                }
            } else {
                if (input[3] >= 8.5) {
                    if (input[2] >= 1.5) {
                        var29 = params[432];
                    } else {
                        var29 = params[433];
                    }
                } else {
                    if (input[2] >= 2.5) {
                        var29 = params[434];
                    } else {
                        var29 = params[435];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
