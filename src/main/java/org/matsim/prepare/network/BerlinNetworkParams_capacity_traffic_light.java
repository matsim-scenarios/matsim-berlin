package org.matsim.prepare.network;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;


/**
* Generated model, do not modify.
* Model: XGBRegressor(alpha=0.7805406663891017, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.42442351663908984,
             eval_metric='mae', feature_types=None, gamma=0.01514317414390719,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.03384760210486928,
             learning_rate=0.424423516, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=3, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...)
*/
public final class BerlinNetworkParams_capacity_traffic_light implements Predictor {

    public static BerlinNetworkParams_capacity_traffic_light INSTANCE = new BerlinNetworkParams_capacity_traffic_light();
    public static final double[] DEFAULT_PARAMS = {144.7419, 316.0572, 365.2319, 452.22885, 348.5643, 406.49893, 443.96057, 389.81857, 442.54373, 375.47406, 502.90494, 439.87012, 444.26117, 247.68372, 431.32706, 337.68777, 148.32056, 189.6219, 146.64378, 176.04637, 261.16693, 227.10551, 132.12212, 216.5601, 216.85416, 264.56506, 168.86957, 241.43185, 330.37442, 269.51257, 138.76718, 278.65323, 123.78207, 100.87378, 139.10155, 91.2437, 200.61545, 143.41835, 79.614105, 117.0278, 164.23444, 132.58234, 152.06537, 195.40454, 308.87073, 91.191666, 204.28485, -24.48445, 42.624, -172.78291, 57.17818, 57.412724, 91.18058, 70.12337, 81.93623, 79.560715, 102.338806, 146.94391, 68.535164, 49.92815, 113.62016, 103.66271, -21.787584, -9.841763, 61.01062, -1.9279529, -63.23056, 73.52335, -44.9745, 40.4048, 14.7982645, 76.12368, 26.324306, -19.274912, 73.19499, -42.73204, 27.876217, 37.712254, 79.2108, -79.836044, 13.984036, -59.32804, 4.8617444, 25.956951, 10.618834, 37.425037, 7.3425207, -53.15611, 47.22288, 30.745354, 20.435513, -25.240677, 81.78547, -59.77613, 39.57315, -130.02287, -10.315844, -185.39569, 26.00977, -4.066559, 13.924528, -4.6805153, 17.042557, -87.30621, -10.965356, 20.315994, 115.065475, -16.427855, 7.7498617, 26.250603, -13.993895, 7.4842067, -15.824703, 24.284616, 57.174995, -34.275654, 20.853027, -20.085798, 29.154709, 6.8081193, -72.43018, -4.687198, -23.857433, 94.27802, -4.02702, -24.663008, -40.835854, 29.11939, -14.232646, -147.14822, -4.5429554, 5.476316, 42.520573, -45.91149, 7.1114717, -21.164392, -72.119896, 17.85887, 18.71914, -73.87017, -19.999222, 119.56282, 14.336783, 39.0889, -72.24567, 3.583989, -5.8991866, -33.679443, -5.374173, 14.970844, -10.278076, 15.770914, 18.213566, -6.49932, 5.61372, -6.1237464, 100.49689, -9.682325, 11.556834, 3.902428, 2.8743289, -16.621174, 1.8534825, -25.90141, 59.709373, 26.24039, 3.2920973, 27.624142, 0.3191808, -19.226328, 5.5377994, -8.44518, -99.1421, 13.992535, -16.654991, -47.612392, -35.74691, 18.428408, 3.7624128, -93.18088, -64.29078, 14.384312, 1.4009901, 24.35726, -38.326103, -4.4788747, 38.188854, -36.398872, -89.16547, -31.31309, 56.16291, -40.16219, -42.89339, 26.792692, -51.060303, 46.202526, 97.225395, -7.236632, 0.28429842, -20.270956, 2.8033082, -2.7724712, 16.960049, 13.102278, -26.413202, -8.556882, -169.47963, 135.99623, -28.082775, -168.73352, -8.708141, -3.3867972, -88.47126, 11.776655, 68.71947, -4.4144053, 17.686052, 0.23092908, 8.085741, -46.154236, 10.046411, -10.040611, -112.033325, 6.924571, -11.978903, 3.509339, -14.377459, 19.298027, 50.905926, -68.82891, -24.113083, 1.2191393, -15.179295, -3.1667697, 7.23575, -0.65055, -47.058613, 9.200322, -68.092, -13.8293495, 2.697028, -31.612421, 2.60773, -114.92156, -9.929466, -0.40600422, -83.38374, 21.096144, 3.6258874, -53.756763, -3.684588, 13.722677, -6.2354665, -17.048956, -76.791245, 35.946224, 12.68691, -40.62043, -23.2364, 50.83999, -2.5947447, 29.21971, 15.58184, 0.16418731, -0.7296576, 7.1611238, -7.1022615, 2.497628, 39.476353, 2.2226183, 2.4097512, -6.612954, -184.23886, -127.48309, 18.226957, -27.240746, 17.247776, -50.25438, -3.2696674, 6.8128324, 22.682257, -51.954735, -23.56316, -81.289566, -2.5026717, -1.9747547, 6.311233, 1.7120445, -39.527615, -224.55634, 55.075863, 6.333592, -2.9023678, -5.0175695, -21.438726, -55.945484, 0.30082178, -17.678436, 26.914814, 53.17081, 13.61343, -7.551292, 40.042446, -34.088684, -185.50896, 2.5475948, -60.84544, 47.090107, 0.24586214, 41.13014, -9.937824, -3.9775712, 10.3061, 19.028816, 1.6497232, 49.222343, -102.58587, -26.499134, 44.834682, -16.388195, 28.216183, 12.448935, 63.147984, 2.6395628, 18.06975, -3.063298, -47.52427, 1.5665433, 64.75885, -100.29367, -8.102122, 9.215973, 3.9472368, -1.1561016, -0.29831287, -22.101206, -173.75294, -9.915586, 1.356128, 8.979831, -10.426495, 8.976558, -4.014847, 6.7051983, -25.800646, -62.766117, -1.4966645, -10.024456, 16.265709, -11.147039, 59.608746, 90.82452, 38.647015, -108.95293, 42.13027, 65.802345, -66.266754, -19.548021, -6.7297845, 22.97122, -0.033260107, 5.172136, 1.6767843, 10.636854, -6.024167, -86.48156, -42.742756, 2.4565015, 84.35425, 4.035032, -33.271095, -85.65453, -8.335147, 8.127773, -7.3146725, -0.2694614, 75.60134, 18.611898, -4.1973047, -1.6095215, 2.0197825, -1.6148685, -14.960697, -20.802738, 40.56988, -53.887302, 0.0, -27.92976, 17.765617, -76.494545, -76.32011, -1.1319474, 16.53474, 54.87108, 70.23639, 58.234364, -20.209349, 33.78283, -78.337875, -37.075375, 12.235175, -6.075058, -24.608728, -91.91919, -39.14803, 34.333168, -89.98896, -60.636383, -0.9281029, -10.56351, 1.5116483, 0.8390076, -20.224276, -38.704258, 10.074378, 64.30022, 24.76204, -6.047733, -62.442825, 20.99253, 103.306435, 2.1486325, -10.10403, 19.382902, -0.6242676, 12.3101635, -0.57169104, 0.17972001, -7.960878, 51.485157, 89.033264, -23.820639, 14.228744, -34.449787, 16.394014, -59.85167, -24.048498, 0.29620793};

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
		data[7] = features.getDouble("is_secondary_or_higher");
		data[8] = features.getDouble("num_left");
		data[9] = features.getDouble("num_right");
		data[10] = features.getDouble("num_straight");
		data[11] = features.getDouble("dir_exclusive");

        return data;
    }

    @Override
    public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories, double[] params) {

        double[] data = getData(features, categories);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);

		// Model is trained on total capacity, needs to return per lane here
        return score(data, params) / features.getDouble("num_lanes");
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[2] >= 2.5) {
            if (input[1] >= 1.5) {
                if (input[6] >= 9.5) {
                    if (input[0] >= 25.0) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[6] >= 7.5) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[10] >= 1.5) {
                    if (input[4] >= 5.5) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    if (input[0] >= 9.719999) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                }
            }
        } else {
            if (input[4] >= 1.5) {
                if (input[6] >= 8.5) {
                    if (input[4] >= 4.5) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var0 = params[14];
                    } else {
                        var0 = params[15];
                    }
                }
            }
        }
        double var1;
        if (input[2] >= 2.5) {
            if (input[1] >= 1.5) {
                if (input[0] >= 9.719999) {
                    if (input[0] >= 18.055) {
                        var1 = params[16];
                    } else {
                        var1 = params[17];
                    }
                } else {
                    if (input[4] >= 5.5) {
                        var1 = params[18];
                    } else {
                        var1 = params[19];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[0] >= 11.110001) {
                        var1 = params[20];
                    } else {
                        var1 = params[21];
                    }
                } else {
                    if (input[2] >= 3.5) {
                        var1 = params[22];
                    } else {
                        var1 = params[23];
                    }
                }
            }
        } else {
            if (input[1] >= 1.5) {
                if (input[10] >= 0.5) {
                    if (input[8] >= 1.5) {
                        var1 = params[24];
                    } else {
                        var1 = params[25];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var1 = params[26];
                    } else {
                        var1 = params[27];
                    }
                }
            } else {
                if (input[4] >= 1.5) {
                    if (input[11] >= 0.5) {
                        var1 = params[28];
                    } else {
                        var1 = params[29];
                    }
                } else {
                    if (input[0] >= 11.110001) {
                        var1 = params[30];
                    } else {
                        var1 = params[31];
                    }
                }
            }
        }
        double var2;
        if (input[6] >= 10.5) {
            if (input[3] >= 4.5) {
                if (input[1] >= 1.5) {
                    if (input[3] >= 11.5) {
                        var2 = params[32];
                    } else {
                        var2 = params[33];
                    }
                } else {
                    if (input[3] >= 5.5) {
                        var2 = params[34];
                    } else {
                        var2 = params[35];
                    }
                }
            } else {
                if (input[0] >= 11.110001) {
                    if (input[1] >= 1.5) {
                        var2 = params[36];
                    } else {
                        var2 = params[37];
                    }
                } else {
                    if (input[5] >= 10.5) {
                        var2 = params[38];
                    } else {
                        var2 = params[39];
                    }
                }
            }
        } else {
            if (input[4] >= 2.5) {
                if (input[1] >= 1.5) {
                    if (input[10] >= 1.5) {
                        var2 = params[40];
                    } else {
                        var2 = params[41];
                    }
                } else {
                    if (input[6] >= 6.5) {
                        var2 = params[42];
                    } else {
                        var2 = params[43];
                    }
                }
            } else {
                if (input[1] >= 2.5) {
                    var2 = params[44];
                } else {
                    if (input[0] >= 12.5) {
                        var2 = params[45];
                    } else {
                        var2 = params[46];
                    }
                }
            }
        }
        double var3;
        if (input[6] >= 8.5) {
            if (input[0] >= 18.055) {
                if (input[9] >= 0.5) {
                    if (input[0] >= 25.0) {
                        var3 = params[47];
                    } else {
                        var3 = params[48];
                    }
                } else {
                    if (input[5] >= 5.0) {
                        var3 = params[49];
                    } else {
                        var3 = params[50];
                    }
                }
            } else {
                if (input[9] >= 1.5) {
                    if (input[6] >= 11.5) {
                        var3 = params[51];
                    } else {
                        var3 = params[52];
                    }
                } else {
                    if (input[3] >= 4.5) {
                        var3 = params[53];
                    } else {
                        var3 = params[54];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[3] >= 3.5) {
                    if (input[5] >= 3.5) {
                        var3 = params[55];
                    } else {
                        var3 = params[56];
                    }
                } else {
                    if (input[4] >= 2.5) {
                        var3 = params[57];
                    } else {
                        var3 = params[58];
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[4] >= 2.5) {
                        var3 = params[59];
                    } else {
                        var3 = params[60];
                    }
                } else {
                    if (input[4] >= 2.5) {
                        var3 = params[61];
                    } else {
                        var3 = params[62];
                    }
                }
            }
        }
        double var4;
        if (input[6] >= 6.5) {
            if (input[2] >= 3.5) {
                if (input[0] >= 9.719999) {
                    if (input[5] >= 6.5) {
                        var4 = params[63];
                    } else {
                        var4 = params[64];
                    }
                } else {
                    if (input[5] >= 5.5) {
                        var4 = params[65];
                    } else {
                        var4 = params[66];
                    }
                }
            } else {
                if (input[8] >= 2.5) {
                    if (input[5] >= 2.5) {
                        var4 = params[67];
                    } else {
                        var4 = params[68];
                    }
                } else {
                    if (input[4] >= 3.5) {
                        var4 = params[69];
                    } else {
                        var4 = params[70];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[2] >= 1.5) {
                    if (input[0] >= 9.719999) {
                        var4 = params[71];
                    } else {
                        var4 = params[72];
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var4 = params[73];
                    } else {
                        var4 = params[74];
                    }
                }
            } else {
                if (input[0] >= 18.055) {
                    if (input[5] >= 1.5) {
                        var4 = params[75];
                    } else {
                        var4 = params[76];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var4 = params[77];
                    } else {
                        var4 = params[78];
                    }
                }
            }
        }
        double var5;
        if (input[1] >= 1.5) {
            if (input[11] >= 0.5) {
                if (input[8] >= 0.5) {
                    if (input[0] >= 33.61) {
                        var5 = params[79];
                    } else {
                        var5 = params[80];
                    }
                } else {
                    if (input[0] >= 18.055) {
                        var5 = params[81];
                    } else {
                        var5 = params[82];
                    }
                }
            } else {
                if (input[2] >= 2.5) {
                    if (input[8] >= 1.5) {
                        var5 = params[83];
                    } else {
                        var5 = params[84];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var5 = params[85];
                    } else {
                        var5 = params[86];
                    }
                }
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[3] >= 6.5) {
                    if (input[0] >= 16.665) {
                        var5 = params[87];
                    } else {
                        var5 = params[88];
                    }
                } else {
                    if (input[0] >= 9.719999) {
                        var5 = params[89];
                    } else {
                        var5 = params[90];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[6] >= 10.5) {
                        var5 = params[91];
                    } else {
                        var5 = params[92];
                    }
                } else {
                    if (input[5] >= 10.5) {
                        var5 = params[93];
                    } else {
                        var5 = params[94];
                    }
                }
            }
        }
        double var6;
        if (input[0] >= 18.055) {
            if (input[9] >= 1.5) {
                var6 = params[95];
            } else {
                if (input[5] >= 6.5) {
                    if (input[9] >= 0.5) {
                        var6 = params[96];
                    } else {
                        var6 = params[97];
                    }
                } else {
                    if (input[10] >= 1.5) {
                        var6 = params[98];
                    } else {
                        var6 = params[99];
                    }
                }
            }
        } else {
            if (input[6] >= 11.5) {
                if (input[9] >= 1.5) {
                    if (input[8] >= 1.5) {
                        var6 = params[100];
                    } else {
                        var6 = params[101];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var6 = params[102];
                    } else {
                        var6 = params[103];
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[1] >= 3.5) {
                        var6 = params[104];
                    } else {
                        var6 = params[105];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var6 = params[106];
                    } else {
                        var6 = params[107];
                    }
                }
            }
        }
        double var7;
        if (input[6] >= 6.5) {
            if (input[5] >= 1.5) {
                if (input[0] >= 9.719999) {
                    if (input[8] >= 0.5) {
                        var7 = params[108];
                    } else {
                        var7 = params[109];
                    }
                } else {
                    if (input[10] >= 1.5) {
                        var7 = params[110];
                    } else {
                        var7 = params[111];
                    }
                }
            } else {
                if (input[3] >= 6.5) {
                    if (input[6] >= 8.5) {
                        var7 = params[112];
                    } else {
                        var7 = params[113];
                    }
                } else {
                    if (input[1] >= 2.5) {
                        var7 = params[114];
                    } else {
                        var7 = params[115];
                    }
                }
            }
        } else {
            if (input[4] >= 2.5) {
                if (input[1] >= 2.5) {
                    if (input[8] >= 0.5) {
                        var7 = params[116];
                    } else {
                        var7 = params[117];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var7 = params[118];
                    } else {
                        var7 = params[119];
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[10] >= 1.5) {
                        var7 = params[120];
                    } else {
                        var7 = params[121];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var7 = params[122];
                    } else {
                        var7 = params[123];
                    }
                }
            }
        }
        double var8;
        if (input[0] >= 18.055) {
            if (input[6] >= 2.5) {
                if (input[9] >= 0.5) {
                    if (input[6] >= 8.0) {
                        var8 = params[124];
                    } else {
                        var8 = params[125];
                    }
                } else {
                    if (input[3] >= 6.5) {
                        var8 = params[126];
                    } else {
                        var8 = params[127];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    var8 = params[128];
                } else {
                    var8 = params[129];
                }
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[6] >= 2.5) {
                    if (input[1] >= 3.5) {
                        var8 = params[130];
                    } else {
                        var8 = params[131];
                    }
                } else {
                    if (input[3] >= 6.5) {
                        var8 = params[132];
                    } else {
                        var8 = params[133];
                    }
                }
            } else {
                if (input[6] >= 6.5) {
                    if (input[3] >= 6.5) {
                        var8 = params[134];
                    } else {
                        var8 = params[135];
                    }
                } else {
                    if (input[3] >= 10.5) {
                        var8 = params[136];
                    } else {
                        var8 = params[137];
                    }
                }
            }
        }
        double var9;
        if (input[10] >= 2.5) {
            if (input[5] >= 11.5) {
                if (input[3] >= 10.5) {
                    if (input[9] >= 0.5) {
                        var9 = params[138];
                    } else {
                        var9 = params[139];
                    }
                } else {
                    if (input[4] >= 5.5) {
                        var9 = params[140];
                    } else {
                        var9 = params[141];
                    }
                }
            } else {
                if (input[6] >= 8.5) {
                    if (input[9] >= 0.5) {
                        var9 = params[142];
                    } else {
                        var9 = params[143];
                    }
                } else {
                    if (input[10] >= 4.5) {
                        var9 = params[144];
                    } else {
                        var9 = params[145];
                    }
                }
            }
        } else {
            if (input[1] >= 1.5) {
                if (input[6] >= 7.5) {
                    if (input[9] >= 0.5) {
                        var9 = params[146];
                    } else {
                        var9 = params[147];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var9 = params[148];
                    } else {
                        var9 = params[149];
                    }
                }
            } else {
                if (input[3] >= 4.5) {
                    if (input[7] >= 0.5) {
                        var9 = params[150];
                    } else {
                        var9 = params[151];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var9 = params[152];
                    } else {
                        var9 = params[153];
                    }
                }
            }
        }
        double var10;
        if (input[0] >= 9.719999) {
            if (input[9] >= 1.5) {
                if (input[2] >= 1.5) {
                    if (input[10] >= 2.5) {
                        var10 = params[154];
                    } else {
                        var10 = params[155];
                    }
                } else {
                    if (input[4] >= 2.5) {
                        var10 = params[156];
                    } else {
                        var10 = params[157];
                    }
                }
            } else {
                if (input[6] >= 4.5) {
                    if (input[10] >= 1.5) {
                        var10 = params[158];
                    } else {
                        var10 = params[159];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var10 = params[160];
                    } else {
                        var10 = params[161];
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[2] >= 1.5) {
                    if (input[3] >= 8.5) {
                        var10 = params[162];
                    } else {
                        var10 = params[163];
                    }
                } else {
                    if (input[3] >= 4.5) {
                        var10 = params[164];
                    } else {
                        var10 = params[165];
                    }
                }
            } else {
                if (input[3] >= 4.5) {
                    if (input[10] >= 0.5) {
                        var10 = params[166];
                    } else {
                        var10 = params[167];
                    }
                } else {
                    if (input[2] >= 2.5) {
                        var10 = params[168];
                    } else {
                        var10 = params[169];
                    }
                }
            }
        }
        double var11;
        if (input[1] >= 2.5) {
            if (input[10] >= 1.5) {
                if (input[9] >= 0.5) {
                    if (input[8] >= 1.5) {
                        var11 = params[170];
                    } else {
                        var11 = params[171];
                    }
                } else {
                    if (input[8] >= 2.5) {
                        var11 = params[172];
                    } else {
                        var11 = params[173];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[6] >= 8.5) {
                        var11 = params[174];
                    } else {
                        var11 = params[175];
                    }
                } else {
                    if (input[6] >= 11.5) {
                        var11 = params[176];
                    } else {
                        var11 = params[177];
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[6] >= 6.5) {
                    if (input[2] >= 1.5) {
                        var11 = params[178];
                    } else {
                        var11 = params[179];
                    }
                } else {
                    if (input[8] >= 1.5) {
                        var11 = params[180];
                    } else {
                        var11 = params[181];
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[2] >= 1.5) {
                        var11 = params[182];
                    } else {
                        var11 = params[183];
                    }
                } else {
                    if (input[8] >= 1.5) {
                        var11 = params[184];
                    } else {
                        var11 = params[185];
                    }
                }
            }
        }
        double var12;
        if (input[2] >= 3.5) {
            if (input[4] >= 5.5) {
                if (input[3] >= 11.5) {
                    if (input[9] >= 3.5) {
                        var12 = params[186];
                    } else {
                        var12 = params[187];
                    }
                } else {
                    if (input[1] >= 2.5) {
                        var12 = params[188];
                    } else {
                        var12 = params[189];
                    }
                }
            } else {
                if (input[0] >= 9.719999) {
                    if (input[3] >= 4.5) {
                        var12 = params[190];
                    } else {
                        var12 = params[191];
                    }
                } else {
                    var12 = params[192];
                }
            }
        } else {
            if (input[8] >= 2.5) {
                if (input[4] >= 5.5) {
                    if (input[9] >= 1.5) {
                        var12 = params[193];
                    } else {
                        var12 = params[194];
                    }
                } else {
                    if (input[3] >= 8.5) {
                        var12 = params[195];
                    } else {
                        var12 = params[196];
                    }
                }
            } else {
                if (input[1] >= 1.5) {
                    if (input[11] >= 0.5) {
                        var12 = params[197];
                    } else {
                        var12 = params[198];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var12 = params[199];
                    } else {
                        var12 = params[200];
                    }
                }
            }
        }
        double var13;
        if (input[3] >= 7.5) {
            if (input[2] >= 1.5) {
                if (input[1] >= 2.5) {
                    if (input[9] >= 0.5) {
                        var13 = params[201];
                    } else {
                        var13 = params[202];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var13 = params[203];
                    } else {
                        var13 = params[204];
                    }
                }
            } else {
                if (input[5] >= 1.5) {
                    if (input[5] >= 6.5) {
                        var13 = params[205];
                    } else {
                        var13 = params[206];
                    }
                } else {
                    if (input[4] >= 3.0) {
                        var13 = params[207];
                    } else {
                        var13 = params[208];
                    }
                }
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[1] >= 3.5) {
                    if (input[8] >= 1.5) {
                        var13 = params[209];
                    } else {
                        var13 = params[210];
                    }
                } else {
                    if (input[4] >= 1.5) {
                        var13 = params[211];
                    } else {
                        var13 = params[212];
                    }
                }
            } else {
                if (input[5] >= 5.5) {
                    if (input[9] >= 1.5) {
                        var13 = params[213];
                    } else {
                        var13 = params[214];
                    }
                } else {
                    if (input[3] >= 3.5) {
                        var13 = params[215];
                    } else {
                        var13 = params[216];
                    }
                }
            }
        }
        double var14;
        if (input[0] >= 9.719999) {
            if (input[10] >= 0.5) {
                if (input[6] >= 5.5) {
                    if (input[8] >= 0.5) {
                        var14 = params[217];
                    } else {
                        var14 = params[218];
                    }
                } else {
                    if (input[9] >= 1.5) {
                        var14 = params[219];
                    } else {
                        var14 = params[220];
                    }
                }
            } else {
                if (input[6] >= 11.5) {
                    if (input[4] >= 5.5) {
                        var14 = params[221];
                    } else {
                        var14 = params[222];
                    }
                } else {
                    if (input[5] >= 4.5) {
                        var14 = params[223];
                    } else {
                        var14 = params[224];
                    }
                }
            }
        } else {
            if (input[8] >= 1.5) {
                if (input[6] >= 10.5) {
                    if (input[9] >= 1.5) {
                        var14 = params[225];
                    } else {
                        var14 = params[226];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var14 = params[227];
                    } else {
                        var14 = params[228];
                    }
                }
            } else {
                if (input[4] >= 5.5) {
                    if (input[9] >= 2.5) {
                        var14 = params[229];
                    } else {
                        var14 = params[230];
                    }
                } else {
                    if (input[2] >= 2.5) {
                        var14 = params[231];
                    } else {
                        var14 = params[232];
                    }
                }
            }
        }
        double var15;
        if (input[1] >= 1.5) {
            if (input[10] >= 1.5) {
                if (input[5] >= 1.5) {
                    if (input[9] >= 1.5) {
                        var15 = params[233];
                    } else {
                        var15 = params[234];
                    }
                } else {
                    if (input[3] >= 4.5) {
                        var15 = params[235];
                    } else {
                        var15 = params[236];
                    }
                }
            } else {
                if (input[3] >= 8.5) {
                    if (input[9] >= 0.5) {
                        var15 = params[237];
                    } else {
                        var15 = params[238];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var15 = params[239];
                    } else {
                        var15 = params[240];
                    }
                }
            }
        } else {
            if (input[6] >= 5.5) {
                if (input[4] >= 3.5) {
                    if (input[8] >= 2.5) {
                        var15 = params[241];
                    } else {
                        var15 = params[242];
                    }
                } else {
                    if (input[3] >= 6.5) {
                        var15 = params[243];
                    } else {
                        var15 = params[244];
                    }
                }
            } else {
                if (input[10] >= 1.5) {
                    if (input[6] >= 3.5) {
                        var15 = params[245];
                    } else {
                        var15 = params[246];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var15 = params[247];
                    } else {
                        var15 = params[248];
                    }
                }
            }
        }
        double var16;
        if (input[1] >= 3.5) {
            if (input[9] >= 1.5) {
                if (input[2] >= 3.5) {
                    var16 = params[249];
                } else {
                    if (input[5] >= 11.5) {
                        var16 = params[250];
                    } else {
                        var16 = params[251];
                    }
                }
            } else {
                if (input[2] >= 1.5) {
                    if (input[10] >= 2.5) {
                        var16 = params[252];
                    } else {
                        var16 = params[253];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var16 = params[254];
                    } else {
                        var16 = params[255];
                    }
                }
            }
        } else {
            if (input[8] >= 2.5) {
                if (input[5] >= 6.5) {
                    if (input[1] >= 1.5) {
                        var16 = params[256];
                    } else {
                        var16 = params[257];
                    }
                } else {
                    if (input[2] >= 2.5) {
                        var16 = params[258];
                    } else {
                        var16 = params[259];
                    }
                }
            } else {
                if (input[9] >= 1.5) {
                    if (input[2] >= 1.5) {
                        var16 = params[260];
                    } else {
                        var16 = params[261];
                    }
                } else {
                    if (input[4] >= 5.5) {
                        var16 = params[262];
                    } else {
                        var16 = params[263];
                    }
                }
            }
        }
        double var17;
        if (input[2] >= 1.5) {
            if (input[3] >= 5.5) {
                if (input[3] >= 7.5) {
                    if (input[7] >= 0.5) {
                        var17 = params[264];
                    } else {
                        var17 = params[265];
                    }
                } else {
                    if (input[1] >= 1.5) {
                        var17 = params[266];
                    } else {
                        var17 = params[267];
                    }
                }
            } else {
                if (input[10] >= 2.5) {
                    if (input[9] >= 0.5) {
                        var17 = params[268];
                    } else {
                        var17 = params[269];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var17 = params[270];
                    } else {
                        var17 = params[271];
                    }
                }
            }
        } else {
            if (input[3] >= 8.5) {
                if (input[1] >= 2.5) {
                    if (input[5] >= 4.5) {
                        var17 = params[272];
                    } else {
                        var17 = params[273];
                    }
                } else {
                    var17 = params[274];
                }
            } else {
                if (input[3] >= 4.5) {
                    if (input[5] >= 3.5) {
                        var17 = params[275];
                    } else {
                        var17 = params[276];
                    }
                } else {
                    if (input[10] >= 1.5) {
                        var17 = params[277];
                    } else {
                        var17 = params[278];
                    }
                }
            }
        }
        double var18;
        if (input[3] >= 11.5) {
            if (input[5] >= 1.5) {
                if (input[2] >= 2.5) {
                    if (input[7] >= 0.5) {
                        var18 = params[279];
                    } else {
                        var18 = params[280];
                    }
                } else {
                    var18 = params[281];
                }
            } else {
                if (input[6] >= 9.5) {
                    if (input[5] >= 0.5) {
                        var18 = params[282];
                    } else {
                        var18 = params[283];
                    }
                } else {
                    var18 = params[284];
                }
            }
        } else {
            if (input[6] >= 7.5) {
                if (input[9] >= 0.5) {
                    if (input[6] >= 9.5) {
                        var18 = params[285];
                    } else {
                        var18 = params[286];
                    }
                } else {
                    if (input[4] >= 4.5) {
                        var18 = params[287];
                    } else {
                        var18 = params[288];
                    }
                }
            } else {
                if (input[10] >= 4.5) {
                    if (input[5] >= 3.5) {
                        var18 = params[289];
                    } else {
                        var18 = params[290];
                    }
                } else {
                    if (input[3] >= 4.5) {
                        var18 = params[291];
                    } else {
                        var18 = params[292];
                    }
                }
            }
        }
        double var19;
        if (input[4] >= 1.5) {
            if (input[6] >= 3.5) {
                if (input[1] >= 3.5) {
                    if (input[9] >= 0.5) {
                        var19 = params[293];
                    } else {
                        var19 = params[294];
                    }
                } else {
                    if (input[9] >= 3.5) {
                        var19 = params[295];
                    } else {
                        var19 = params[296];
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[0] >= 11.110001) {
                        var19 = params[297];
                    } else {
                        var19 = params[298];
                    }
                } else {
                    if (input[9] >= 1.5) {
                        var19 = params[299];
                    } else {
                        var19 = params[300];
                    }
                }
            }
        } else {
            if (input[0] >= 18.055) {
                if (input[5] >= 1.5) {
                    var19 = params[301];
                } else {
                    var19 = params[302];
                }
            } else {
                if (input[0] >= 15.280001) {
                    if (input[5] >= 0.5) {
                        var19 = params[303];
                    } else {
                        var19 = params[304];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var19 = params[305];
                    } else {
                        var19 = params[306];
                    }
                }
            }
        }
        double var20;
        if (input[4] >= 1.5) {
            if (input[6] >= 5.5) {
                if (input[5] >= 1.5) {
                    if (input[9] >= 3.5) {
                        var20 = params[307];
                    } else {
                        var20 = params[308];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var20 = params[309];
                    } else {
                        var20 = params[310];
                    }
                }
            } else {
                if (input[1] >= 1.5) {
                    if (input[6] >= 3.5) {
                        var20 = params[311];
                    } else {
                        var20 = params[312];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var20 = params[313];
                    } else {
                        var20 = params[314];
                    }
                }
            }
        } else {
            if (input[1] >= 1.5) {
                var20 = params[315];
            } else {
                if (input[5] >= 0.5) {
                    if (input[7] >= 0.5) {
                        var20 = params[316];
                    } else {
                        var20 = params[317];
                    }
                } else {
                    if (input[6] >= 1.5) {
                        var20 = params[318];
                    } else {
                        var20 = params[319];
                    }
                }
            }
        }
        double var21;
        if (input[3] >= 11.5) {
            if (input[0] >= 15.280001) {
                if (input[6] >= 10.5) {
                    if (input[5] >= 6.5) {
                        var21 = params[320];
                    } else {
                        var21 = params[321];
                    }
                } else {
                    var21 = params[322];
                }
            } else {
                if (input[6] >= 10.5) {
                    if (input[5] >= 4.5) {
                        var21 = params[323];
                    } else {
                        var21 = params[324];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var21 = params[325];
                    } else {
                        var21 = params[326];
                    }
                }
            }
        } else {
            if (input[10] >= 4.5) {
                if (input[6] >= 5.5) {
                    if (input[3] >= 7.5) {
                        var21 = params[327];
                    } else {
                        var21 = params[328];
                    }
                } else {
                    var21 = params[329];
                }
            } else {
                if (input[5] >= 11.5) {
                    if (input[10] >= 1.5) {
                        var21 = params[330];
                    } else {
                        var21 = params[331];
                    }
                } else {
                    if (input[5] >= 7.5) {
                        var21 = params[332];
                    } else {
                        var21 = params[333];
                    }
                }
            }
        }
        double var22;
        if (input[0] >= 9.719999) {
            if (input[8] >= 1.5) {
                if (input[4] >= 2.5) {
                    if (input[3] >= 4.5) {
                        var22 = params[334];
                    } else {
                        var22 = params[335];
                    }
                } else {
                    var22 = params[336];
                }
            } else {
                if (input[3] >= 4.5) {
                    if (input[5] >= 11.5) {
                        var22 = params[337];
                    } else {
                        var22 = params[338];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var22 = params[339];
                    } else {
                        var22 = params[340];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[2] >= 2.5) {
                    if (input[3] >= 7.5) {
                        var22 = params[341];
                    } else {
                        var22 = params[342];
                    }
                } else {
                    if (input[5] >= 6.5) {
                        var22 = params[343];
                    } else {
                        var22 = params[344];
                    }
                }
            } else {
                if (input[3] >= 6.5) {
                    if (input[11] >= 0.5) {
                        var22 = params[345];
                    } else {
                        var22 = params[346];
                    }
                } else {
                    if (input[5] >= 4.5) {
                        var22 = params[347];
                    } else {
                        var22 = params[348];
                    }
                }
            }
        }
        double var23;
        if (input[10] >= 4.5) {
            if (input[6] >= 7.5) {
                if (input[3] >= 7.5) {
                    if (input[9] >= 0.5) {
                        var23 = params[349];
                    } else {
                        var23 = params[350];
                    }
                } else {
                    if (input[5] >= 7.5) {
                        var23 = params[351];
                    } else {
                        var23 = params[352];
                    }
                }
            } else {
                if (input[5] >= 3.5) {
                    var23 = params[353];
                } else {
                    var23 = params[354];
                }
            }
        } else {
            if (input[0] >= 20.83) {
                if (input[3] >= 8.5) {
                    var23 = params[355];
                } else {
                    if (input[5] >= 3.5) {
                        var23 = params[356];
                    } else {
                        var23 = params[357];
                    }
                }
            } else {
                if (input[8] >= 2.5) {
                    if (input[7] >= 0.5) {
                        var23 = params[358];
                    } else {
                        var23 = params[359];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var23 = params[360];
                    } else {
                        var23 = params[361];
                    }
                }
            }
        }
        double var24;
        if (input[10] >= 2.5) {
            if (input[1] >= 1.5) {
                if (input[7] >= 0.5) {
                    if (input[1] >= 2.5) {
                        var24 = params[362];
                    } else {
                        var24 = params[363];
                    }
                } else {
                    if (input[2] >= 1.5) {
                        var24 = params[364];
                    } else {
                        var24 = params[365];
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[2] >= 2.5) {
                        var24 = params[366];
                    } else {
                        var24 = params[367];
                    }
                } else {
                    if (input[5] >= 5.5) {
                        var24 = params[368];
                    } else {
                        var24 = params[369];
                    }
                }
            }
        } else {
            if (input[9] >= 3.5) {
                if (input[3] >= 6.5) {
                    var24 = params[370];
                } else {
                    var24 = params[371];
                }
            } else {
                if (input[1] >= 2.5) {
                    if (input[9] >= 0.5) {
                        var24 = params[372];
                    } else {
                        var24 = params[373];
                    }
                } else {
                    if (input[0] >= 15.280001) {
                        var24 = params[374];
                    } else {
                        var24 = params[375];
                    }
                }
            }
        }
        double var25;
        if (input[0] >= 33.61) {
            var25 = params[376];
        } else {
            if (input[0] >= 9.719999) {
                if (input[10] >= 3.5) {
                    if (input[5] >= 8.5) {
                        var25 = params[377];
                    } else {
                        var25 = params[378];
                    }
                } else {
                    if (input[3] >= 6.5) {
                        var25 = params[379];
                    } else {
                        var25 = params[380];
                    }
                }
            } else {
                if (input[4] >= 2.5) {
                    if (input[3] >= 3.5) {
                        var25 = params[381];
                    } else {
                        var25 = params[382];
                    }
                } else {
                    if (input[3] >= 5.5) {
                        var25 = params[383];
                    } else {
                        var25 = params[384];
                    }
                }
            }
        }
        double var26;
        if (input[6] >= 3.5) {
            if (input[4] >= 1.5) {
                if (input[6] >= 4.5) {
                    if (input[0] >= 33.61) {
                        var26 = params[385];
                    } else {
                        var26 = params[386];
                    }
                } else {
                    if (input[1] >= 1.5) {
                        var26 = params[387];
                    } else {
                        var26 = params[388];
                    }
                }
            } else {
                var26 = params[389];
            }
        } else {
            if (input[6] >= 1.5) {
                if (input[8] >= 0.5) {
                    if (input[0] >= 18.055) {
                        var26 = params[390];
                    } else {
                        var26 = params[391];
                    }
                } else {
                    if (input[0] >= 11.110001) {
                        var26 = params[392];
                    } else {
                        var26 = params[393];
                    }
                }
            } else {
                if (input[3] >= 6.5) {
                    var26 = params[394];
                } else {
                    if (input[8] >= 0.5) {
                        var26 = params[395];
                    } else {
                        var26 = params[396];
                    }
                }
            }
        }
        double var27;
        if (input[2] >= 3.5) {
            if (input[0] >= 9.719999) {
                if (input[10] >= 2.5) {
                    if (input[3] >= 11.5) {
                        var27 = params[397];
                    } else {
                        var27 = params[398];
                    }
                } else {
                    if (input[3] >= 11.5) {
                        var27 = params[399];
                    } else {
                        var27 = params[400];
                    }
                }
            } else {
                if (input[10] >= 1.5) {
                    var27 = params[401];
                } else {
                    if (input[9] >= 1.5) {
                        var27 = params[402];
                    } else {
                        var27 = params[403];
                    }
                }
            }
        } else {
            if (input[9] >= 2.5) {
                if (input[4] >= 4.5) {
                    if (input[3] >= 9.5) {
                        var27 = params[404];
                    } else {
                        var27 = params[405];
                    }
                } else {
                    var27 = params[406];
                }
            } else {
                if (input[9] >= 1.5) {
                    if (input[0] >= 15.280001) {
                        var27 = params[407];
                    } else {
                        var27 = params[408];
                    }
                } else {
                    if (input[5] >= 11.5) {
                        var27 = params[409];
                    } else {
                        var27 = params[410];
                    }
                }
            }
        }
        double var28;
        if (input[8] >= 0.5) {
            if (input[3] >= 3.5) {
                if (input[6] >= 6.5) {
                    if (input[4] >= 3.5) {
                        var28 = params[411];
                    } else {
                        var28 = params[412];
                    }
                } else {
                    if (input[3] >= 10.5) {
                        var28 = params[413];
                    } else {
                        var28 = params[414];
                    }
                }
            } else {
                if (input[2] >= 2.5) {
                    if (input[4] >= 5.5) {
                        var28 = params[415];
                    } else {
                        var28 = params[416];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var28 = params[417];
                    } else {
                        var28 = params[418];
                    }
                }
            }
        } else {
            if (input[2] >= 2.5) {
                if (input[1] >= 2.5) {
                    var28 = params[419];
                } else {
                    var28 = params[420];
                }
            } else {
                if (input[3] >= 3.5) {
                    if (input[10] >= 2.5) {
                        var28 = params[421];
                    } else {
                        var28 = params[422];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var28 = params[423];
                    } else {
                        var28 = params[424];
                    }
                }
            }
        }
        double var29;
        if (input[1] >= 1.5) {
            if (input[4] >= 1.5) {
                if (input[10] >= 1.5) {
                    if (input[0] >= 15.280001) {
                        var29 = params[425];
                    } else {
                        var29 = params[426];
                    }
                } else {
                    if (input[3] >= 6.5) {
                        var29 = params[427];
                    } else {
                        var29 = params[428];
                    }
                }
            } else {
                var29 = params[429];
            }
        } else {
            if (input[5] >= 11.5) {
                if (input[10] >= 2.5) {
                    if (input[3] >= 9.5) {
                        var29 = params[430];
                    } else {
                        var29 = params[431];
                    }
                } else {
                    if (input[4] >= 4.5) {
                        var29 = params[432];
                    } else {
                        var29 = params[433];
                    }
                }
            } else {
                if (input[3] >= 7.5) {
                    if (input[10] >= 0.5) {
                        var29 = params[434];
                    } else {
                        var29 = params[435];
                    }
                } else {
                    if (input[0] >= 18.055) {
                        var29 = params[436];
                    } else {
                        var29 = params[437];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
