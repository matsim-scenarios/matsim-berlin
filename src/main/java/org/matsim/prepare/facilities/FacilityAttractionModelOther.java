package org.matsim.prepare.facilities;
import org.matsim.application.prepare.Predictor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
    
/**
* Generated model, do not modify.
* Model: XGBRegressor(alpha=0.13415646738521442, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.4132954956004439,
             eval_metric='mae', feature_types=None, gamma=0.016655458319067392,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.4344413251581389,
             learning_rate=0.413295507, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=1, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...)
* Error: 0.038648
*/
public final class FacilityAttractionModelOther implements Predictor {
    
    public static FacilityAttractionModelOther INSTANCE = new FacilityAttractionModelOther();
    public static final double[] DEFAULT_PARAMS = {-0.15216102, -0.16628286, -0.17856602, -0.16805296, -0.16179523, -0.15005267, -0.13105828, -0.17257981, -0.14734165, -0.17745596, -0.19004343, -0.14746353, -0.16825481, -0.13972259, -0.11681784, -0.10444686, -0.09348584, -0.11706815, -0.09695546, -0.10507279, -0.09713114, -0.065364234, -0.08112048, -0.087821335, -0.12381576, -0.11150259, -0.0868856, -0.11168958, -0.10207733, -0.08416655, -0.04852397, -0.058724064, -0.071617946, -0.062864274, -0.060382165, -0.043563243, -0.041998755, -0.05247808, -0.053838454, -0.0641497, -0.074375734, -0.06684833, -0.04501793, -0.061897233, -0.053449888, -0.03763795, -0.045119785, 0.000503564, -0.017774345, 0.0040162923, -0.030806486, -0.039028987, -0.029838778, -0.005681061, -0.033948544, -0.029733833, -0.041004915, -0.034246974, -0.02795505, -0.037957128, -0.029643597, -0.020159263, -0.008118159, -0.015397975, -0.024727982, -0.013006277, -0.020875685, -0.0064059743, -0.01793386, 0.0, -0.014303434, -0.027678622, -0.02281956, -0.02108273, -0.017239245, -0.020336095, -0.0046864236, -0.021127038, -0.0039720945, 0.0070759524, -0.012192677, 0.0, -0.018906416, -0.0073086745, 0.013880123, -0.008871795, 0.004576532, -0.014845452, -0.012793347, -0.0004907091, 0.022769617, -0.014956246, -0.0012317413, -0.0057955747, 0.007561619, -0.003682303, 0.020226281, -0.014139287, 0.03745069, -0.016924202, -0.012310901, -0.008882259, -0.0045332387, 0.018187473, -0.007924355, 0.025998976, 0.0043698037, 0.0138370935, -0.0013884641, 0.0, -0.015053513, -0.0044727554, 0.003743835, 0.0014436024, 0.009697541, -0.007946342, 0.0010643335, -0.0036092214, -0.005393247, -0.0030547897, 0.0024472754, 0.0, 0.025644321, -0.0031320632, 0.004360804, 0.0048208307, -0.0015873025, 0.0058239326, -0.0025479118, -0.005664896, -0.003546555, 0.0005628377, 0.015281249, 0.0061570276, -0.0025763127, 0.009222, 0.038169082, 0.012852806, -0.009404195, -0.0057628085, 0.013181939, 0.014935939, -0.0032075013, -0.005383483, 0.009956505, -0.00089021836, 0.008416741, -0.003941664, -0.0014048903, 0.0102753, -0.00636565, 0.01863958, 4.52187e-05, -0.01204354, -0.008134882, 0.0061169397, 0.00010211873, -0.0038147063, 0.006495459, 0.0009645417, -0.0010914245, -4.0920168e-05, -0.0007203351, 0.01979621, -0.004656616, 0.0065098354, 0.013411708, -0.0027469576, 0.0025443574, 0.0017110389, 0.008185001, 0.002577436, -0.0022852, 0.0045666527, -0.0005116068, 0.0, -0.014132656, -0.001840841, 0.028261032, -0.0061117606, -0.0041626636, 0.004858744, -0.007761347, 0.007944089, 0.017073328, -0.00057777937, 0.00081146974, 0.0095629385, -0.00083660416, -5.875835e-05, 0.03256128, 0.0, 0.008172847, -0.005180259, -0.0013265943, 0.017737627, 0.052814998, 0.0, -0.00047561005, -0.004084047, -0.002493659, 0.01420486, 0.001211757, 0.013533606, 0.015082587, -0.0002537408, -0.0046758796, 0.003524741, -0.0042722644, -0.00024834776, 0.007708204, 0.0027625787, 0.0005098966, 0.016338924, -0.013994855, -0.00430675, 0.001126375, 0.010913855, 1.9702828e-05, 0.008020428, -0.00070991437, 0.00031716446, 0.011877658, 0.00047084398, -0.008840832, 0.00943859, 0.0066107092, -0.006633546, -0.00076388643, 5.2986077e-05, 0.012803967, -0.0075161103, 0.006253737, -0.0027735315, 0.033788588, -0.002577162, 0.009805766, -0.003325513, 0.005980905, -0.010077708, 0.026395539, -0.00817541, 0.0042598504, -0.00010952335, -0.010912333, 0.0, 0.006940519, -0.00013481517, -0.009599118, 0.0072412593, 0.0013126928, -0.001417684, 0.005715553, -0.0009917886, 3.355887e-05, 0.024194222, 0.0, -0.00070705696, -0.0034868943, 0.0063970597, 0.025671707, 0.002122948, -0.0001128266, -0.010408071, 0.009076207, 0.001776522, 0.024281276, -0.0016706667, 0.0011074817, -0.003206501, 0.012611808, 0.0, 0.030886251, 0.0, 0.0005614933, 0.0096550835, 0.011517035, -0.0017919404, 0.0020549991, -0.00077920343, 0.0056736483, -0.0072258417, 0.01615629, -0.016230963, 0.012166424, 3.192161e-05, -0.008082501, 0.0, -0.00016092649, 0.003118809, -0.000277953, 0.019238275, 0.0032932449, 0.033841334, 0.0073562087, -0.01422051, -0.00089586474, 0.010702279, 0.005422894, -1.8486526e-05, -0.0022895862, 0.010280889, -0.005396973, 0.019359354, 0.0, 0.011452112, -0.00027099787, 0.0045888596, 0.0004208389, 0.0055483356, -0.0054569133, 0.012569686, -0.001466627, 0.0005543249, -8.518171e-05, -0.0049442234, -0.030187769, 0.00042558767, -0.007226092, 0.0019770558, 0.00738568, -0.0055052508, 0.0034824796, 0.0023163185, -0.0011127826, -0.0023843655, -0.00012225388, 0.012715778, 0.0005868807, 0.0031442954, 2.2653856e-05, 0.009739125, -0.012979816, 0.016758624, -0.0037924708, 0.0040689064, -0.014333322, -0.0031932949, 0.018389959, 0.0025187747, -0.0037748518, 2.7818749e-05, -0.0031511902, 0.0023450637, 0.024577832, -0.0007048508, -0.0034227737, 0.0032817684, 0.0002810907, -0.0018574629, -5.5532375e-05, 0.043876186, 0.008807471, 0.0025866106, -0.0030347805, 0.019282335, 0.0, 0.0029591348, 0.00029000052, 8.214231e-05, -0.004656119, 0.0036433996, 0.0007933222, -0.005442655, -0.0012587027, 0.0059722187, -0.00019465231, -0.011768113, 0.0058383867, 0.024225164, -0.020380093, 0.0016472826, -0.00978623, -0.00019774091, 0.0027843087, -0.00159939, 0.0061408803, -0.0013505849, -0.005112886, 5.6863726e-05, 7.320677e-05, 0.009333496, -0.0041468847, -0.015277629, -0.004614459, -3.6356803e-06, 0.0031467294, 0.00013876976, 0.011200626, 0.0017009011, 0.0, 0.02283013, 0.00905502, -0.002442702, 0.018977566, 0.0, 0.002568793, -0.009118193, 0.0018206323, -0.0008226331, -0.0032939538, 0.009706387, -8.379586e-05, 0.0008980472, -0.004583928, -0.0003062728, 0.019309962, 0.0010106281, 0.021873923, -0.0050830483, 0.0005262823, 0.024333224, 0.0067784404, -0.0010780566, -0.0062900083, -0.0028856306, 3.220146e-05};

    @Override
    public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
        return predict(features, categories, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
        double[] data = new double[32];
		data[0] = features.getDouble("area");
		data[1] = features.getDouble("levels");
		data[2] = features.getDouble("landuse");
		data[3] = features.getDouble("landuse_residential_500m");
		data[4] = features.getDouble("landuse_residential_1500m");
		data[5] = features.getDouble("landuse_retail_500m");
		data[6] = features.getDouble("landuse_retail_1500m");
		data[7] = features.getDouble("landuse_commercial_500m");
		data[8] = features.getDouble("landuse_commercial_1500m");
		data[9] = features.getDouble("landuse_recreation_1500m");
		data[10] = features.getDouble("poi_leisure");
		data[11] = features.getDouble("poi_leisure_250m");
		data[12] = features.getDouble("poi_shop");
		data[13] = features.getDouble("poi_shop_250m");
		data[14] = features.getDouble("poi_dining");
		data[15] = features.getDouble("poi_dining_250m");
		data[16] = features.getDouble("delivery");
		data[17] = features.getDouble("depot");
		data[18] = features.getDouble("dining");
		data[19] = features.getDouble("edu_higher");
		data[20] = features.getDouble("edu_kiga");
		data[21] = features.getDouble("edu_other");
		data[22] = features.getDouble("edu_prim");
		data[23] = features.getDouble("leisure");
		data[24] = features.getDouble("medical");
		data[25] = features.getDouble("p_business");
		data[26] = features.getDouble("parking");
		data[27] = features.getDouble("religious");
		data[28] = features.getDouble("resident");
		data[29] = features.getDouble("shop");
		data[30] = features.getDouble("shop_daily");
		data[31] = features.getDouble("work");

        return data;
    }
    
    @Override
    public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories, double[] params) {

        double[] data = getData(features, categories);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);
    
        return Math.min(Math.max(score(data, params), 0.000000), 0.238576);
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[12] >= 0.5) {
            if (input[0] >= 235.385) {
                if (input[16] >= 0.5) {
                    if (input[12] >= 3.5) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[11] >= 28.5) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[0] >= 46.16) {
                    if (input[28] >= 0.5) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    var0 = params[6];
                }
            }
        } else {
            if (input[0] >= 36.905) {
                if (input[18] >= 0.5) {
                    if (input[0] >= 179.795) {
                        var0 = params[7];
                    } else {
                        var0 = params[8];
                    }
                } else {
                    if (input[26] >= 0.5) {
                        var0 = params[9];
                    } else {
                        var0 = params[10];
                    }
                }
            } else {
                if (input[0] >= 12.645) {
                    if (input[8] >= 0.16190001) {
                        var0 = params[11];
                    } else {
                        var0 = params[12];
                    }
                } else {
                    if (input[0] >= 7.4399996) {
                        var0 = params[13];
                    } else {
                        var0 = params[14];
                    }
                }
            }
        }
        double var1;
        if (input[15] >= 4.5) {
            if (input[10] >= 0.5) {
                if (input[16] >= 0.5) {
                    if (input[11] >= 23.5) {
                        var1 = params[15];
                    } else {
                        var1 = params[16];
                    }
                } else {
                    if (input[0] >= 286.33502) {
                        var1 = params[17];
                    } else {
                        var1 = params[18];
                    }
                }
            } else {
                if (input[0] >= 111.744995) {
                    if (input[0] >= 498.02502) {
                        var1 = params[19];
                    } else {
                        var1 = params[20];
                    }
                } else {
                    if (input[14] >= 0.5) {
                        var1 = params[21];
                    } else {
                        var1 = params[22];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[12] >= 0.5) {
                    var1 = params[23];
                } else {
                    if (input[0] >= 478.41498) {
                        var1 = params[24];
                    } else {
                        var1 = params[25];
                    }
                }
            } else {
                if (input[0] >= 59.905) {
                    if (input[30] >= 0.5) {
                        var1 = params[26];
                    } else {
                        var1 = params[27];
                    }
                } else {
                    if (input[0] >= 16.215) {
                        var1 = params[28];
                    } else {
                        var1 = params[29];
                    }
                }
            }
        }
        double var2;
        if (input[13] >= 5.5) {
            if (input[0] >= 408.91998) {
                if (input[30] >= 0.5) {
                    if (input[7] >= 0.01105) {
                        var2 = params[30];
                    } else {
                        var2 = params[31];
                    }
                } else {
                    if (input[0] >= 2387.44) {
                        var2 = params[32];
                    } else {
                        var2 = params[33];
                    }
                }
            } else {
                if (input[11] >= 23.5) {
                    if (input[0] >= 111.655) {
                        var2 = params[34];
                    } else {
                        var2 = params[35];
                    }
                } else {
                    if (input[14] >= 0.5) {
                        var2 = params[36];
                    } else {
                        var2 = params[37];
                    }
                }
            }
        } else {
            if (input[0] >= 85.895004) {
                if (input[1] >= 2.5) {
                    if (input[1] >= 9.5) {
                        var2 = params[38];
                    } else {
                        var2 = params[39];
                    }
                } else {
                    if (input[0] >= 1764.69) {
                        var2 = params[40];
                    } else {
                        var2 = params[41];
                    }
                }
            } else {
                if (input[0] >= 25.529999) {
                    if (input[7] >= 0.04515) {
                        var2 = params[42];
                    } else {
                        var2 = params[43];
                    }
                } else {
                    if (input[0] >= 8.95) {
                        var2 = params[44];
                    } else {
                        var2 = params[45];
                    }
                }
            }
        }
        double var3;
        if (input[12] >= 0.5) {
            if (input[5] >= 0.09365) {
                if (input[0] >= 5291.385) {
                    var3 = params[46];
                } else {
                    if (input[25] >= 0.5) {
                        var3 = params[47];
                    } else {
                        var3 = params[48];
                    }
                }
            } else {
                if (input[7] >= 0.01055) {
                    if (input[3] >= 0.69225) {
                        var3 = params[49];
                    } else {
                        var3 = params[50];
                    }
                } else {
                    if (input[11] >= 15.5) {
                        var3 = params[51];
                    } else {
                        var3 = params[52];
                    }
                }
            }
        } else {
            if (input[0] >= 344.03497) {
                if (input[1] >= 5.5) {
                    if (input[8] >= 1.08975) {
                        var3 = params[53];
                    } else {
                        var3 = params[54];
                    }
                } else {
                    if (input[25] >= 0.5) {
                        var3 = params[55];
                    } else {
                        var3 = params[56];
                    }
                }
            } else {
                if (input[4] >= 1.0788) {
                    if (input[0] >= 172.67001) {
                        var3 = params[57];
                    } else {
                        var3 = params[58];
                    }
                } else {
                    if (input[0] >= 20.365002) {
                        var3 = params[59];
                    } else {
                        var3 = params[60];
                    }
                }
            }
        }
        double var4;
        if (input[14] >= 0.5) {
            if (input[10] >= 0.5) {
                if (input[14] >= 2.5) {
                    if (input[6] >= 0.1343) {
                        var4 = params[61];
                    } else {
                        var4 = params[62];
                    }
                } else {
                    if (input[7] >= 0.05475) {
                        var4 = params[63];
                    } else {
                        var4 = params[64];
                    }
                }
            } else {
                if (input[11] >= 22.5) {
                    if (input[7] >= 0.017549999) {
                        var4 = params[65];
                    } else {
                        var4 = params[66];
                    }
                } else {
                    if (input[6] >= 0.0303) {
                        var4 = params[67];
                    } else {
                        var4 = params[68];
                    }
                }
            }
        } else {
            if (input[0] >= 93.5) {
                if (input[12] >= 2.5) {
                    if (input[25] >= 0.5) {
                        var4 = params[69];
                    } else {
                        var4 = params[70];
                    }
                } else {
                    if (input[0] >= 3358.44) {
                        var4 = params[71];
                    } else {
                        var4 = params[72];
                    }
                }
            } else {
                if (input[28] >= 0.5) {
                    if (input[0] >= 22.795) {
                        var4 = params[73];
                    } else {
                        var4 = params[74];
                    }
                } else {
                    if (input[3] >= 0.36285) {
                        var4 = params[75];
                    } else {
                        var4 = params[76];
                    }
                }
            }
        }
        double var5;
        if (input[25] >= 0.5) {
            if (input[0] >= 5323.165) {
                var5 = params[77];
            } else {
                if (input[13] >= 17.5) {
                    if (input[11] >= 29.5) {
                        var5 = params[78];
                    } else {
                        var5 = params[79];
                    }
                } else {
                    if (input[0] >= 314.705) {
                        var5 = params[80];
                    } else {
                        var5 = params[81];
                    }
                }
            }
        } else {
            if (input[30] >= 0.5) {
                if (input[0] >= 1875.47) {
                    var5 = params[82];
                } else {
                    if (input[3] >= 0.2369) {
                        var5 = params[83];
                    } else {
                        var5 = params[84];
                    }
                }
            } else {
                if (input[8] >= 1.1108501) {
                    if (input[0] >= 1051.02) {
                        var5 = params[85];
                    } else {
                        var5 = params[86];
                    }
                } else {
                    if (input[0] >= 485.525) {
                        var5 = params[87];
                    } else {
                        var5 = params[88];
                    }
                }
            }
        }
        double var6;
        if (input[5] >= 0.1326) {
            if (input[0] >= 1125.4099) {
                if (input[15] >= 54.0) {
                    if (input[15] >= 97.5) {
                        var6 = params[89];
                    } else {
                        var6 = params[90];
                    }
                } else {
                    if (input[16] >= 0.5) {
                        var6 = params[91];
                    } else {
                        var6 = params[92];
                    }
                }
            } else {
                if (input[3] >= 0.2733) {
                    if (input[5] >= 0.17469999) {
                        var6 = params[93];
                    } else {
                        var6 = params[94];
                    }
                } else {
                    if (input[17] >= 0.5) {
                        var6 = params[95];
                    } else {
                        var6 = params[96];
                    }
                }
            }
        } else {
            if (input[20] >= 0.5) {
                if (input[15] >= 138.5) {
                    if (input[0] >= 383.79) {
                        var6 = params[97];
                    } else {
                        var6 = params[98];
                    }
                } else {
                    if (input[15] >= 12.5) {
                        var6 = params[99];
                    } else {
                        var6 = params[100];
                    }
                }
            } else {
                if (input[7] >= 0.01305) {
                    if (input[0] >= 1107.875) {
                        var6 = params[101];
                    } else {
                        var6 = params[102];
                    }
                } else {
                    if (input[2] >= 0.5) {
                        var6 = params[103];
                    } else {
                        var6 = params[104];
                    }
                }
            }
        }
        double var7;
        if (input[26] >= 0.5) {
            if (input[13] >= 53.5) {
                if (input[3] >= 0.48855) {
                    if (input[11] >= 32.5) {
                        var7 = params[105];
                    } else {
                        var7 = params[106];
                    }
                } else {
                    if (input[5] >= 0.035099998) {
                        var7 = params[107];
                    } else {
                        var7 = params[108];
                    }
                }
            } else {
                if (input[1] >= 0.5) {
                    if (input[6] >= 0.2545) {
                        var7 = params[109];
                    } else {
                        var7 = params[110];
                    }
                } else {
                    if (input[0] >= 314.36) {
                        var7 = params[111];
                    } else {
                        var7 = params[112];
                    }
                }
            }
        } else {
            if (input[24] >= 0.5) {
                if (input[12] >= 0.5) {
                    if (input[11] >= 19.5) {
                        var7 = params[113];
                    } else {
                        var7 = params[114];
                    }
                } else {
                    if (input[11] >= 21.5) {
                        var7 = params[115];
                    } else {
                        var7 = params[116];
                    }
                }
            } else {
                if (input[0] >= 80.165) {
                    if (input[1] >= 2.5) {
                        var7 = params[117];
                    } else {
                        var7 = params[118];
                    }
                } else {
                    if (input[0] >= 14.605) {
                        var7 = params[119];
                    } else {
                        var7 = params[120];
                    }
                }
            }
        }
        double var8;
        if (input[1] >= 6.5) {
            if (input[13] >= 160.5) {
                if (input[0] >= 310.745) {
                    if (input[6] >= 0.30805) {
                        var8 = params[121];
                    } else {
                        var8 = params[122];
                    }
                } else {
                    var8 = params[123];
                }
            } else {
                if (input[1] >= 10.5) {
                    var8 = params[124];
                } else {
                    if (input[7] >= 0.01675) {
                        var8 = params[125];
                    } else {
                        var8 = params[126];
                    }
                }
            }
        } else {
            if (input[0] >= 418.31) {
                if (input[5] >= 0.00885) {
                    if (input[14] >= 2.5) {
                        var8 = params[127];
                    } else {
                        var8 = params[128];
                    }
                } else {
                    if (input[3] >= 0.27385) {
                        var8 = params[129];
                    } else {
                        var8 = params[130];
                    }
                }
            } else {
                if (input[14] >= 0.5) {
                    if (input[13] >= 5.5) {
                        var8 = params[131];
                    } else {
                        var8 = params[132];
                    }
                } else {
                    if (input[25] >= 0.5) {
                        var8 = params[133];
                    } else {
                        var8 = params[134];
                    }
                }
            }
        }
        double var9;
        if (input[11] >= 104.5) {
            if (input[4] >= 3.7175) {
                if (input[11] >= 151.5) {
                    if (input[3] >= 0.5819) {
                        var9 = params[135];
                    } else {
                        var9 = params[136];
                    }
                } else {
                    if (input[13] >= 116.5) {
                        var9 = params[137];
                    } else {
                        var9 = params[138];
                    }
                }
            } else {
                if (input[11] >= 113.0) {
                    var9 = params[139];
                } else {
                    var9 = params[140];
                }
            }
        } else {
            if (input[11] >= 45.5) {
                if (input[26] >= 0.5) {
                    if (input[3] >= 0.46195) {
                        var9 = params[141];
                    } else {
                        var9 = params[142];
                    }
                } else {
                    if (input[3] >= 0.2999) {
                        var9 = params[143];
                    } else {
                        var9 = params[144];
                    }
                }
            } else {
                if (input[13] >= 78.5) {
                    if (input[3] >= 0.47785) {
                        var9 = params[145];
                    } else {
                        var9 = params[146];
                    }
                } else {
                    if (input[11] >= 23.5) {
                        var9 = params[147];
                    } else {
                        var9 = params[148];
                    }
                }
            }
        }
        double var10;
        if (input[27] >= 0.5) {
            if (input[1] >= 1.5) {
                if (input[0] >= 220.38) {
                    if (input[3] >= 0.5823) {
                        var10 = params[149];
                    } else {
                        var10 = params[150];
                    }
                } else {
                    var10 = params[151];
                }
            } else {
                if (input[3] >= 0.6473) {
                    var10 = params[152];
                } else {
                    var10 = params[153];
                }
            }
        } else {
            if (input[24] >= 0.5) {
                if (input[0] >= 3655.945) {
                    var10 = params[154];
                } else {
                    if (input[7] >= 0.0103) {
                        var10 = params[155];
                    } else {
                        var10 = params[156];
                    }
                }
            } else {
                if (input[14] >= 2.5) {
                    if (input[4] >= 4.559) {
                        var10 = params[157];
                    } else {
                        var10 = params[158];
                    }
                } else {
                    if (input[26] >= 0.5) {
                        var10 = params[159];
                    } else {
                        var10 = params[160];
                    }
                }
            }
        }
        double var11;
        if (input[4] >= 5.24965) {
            if (input[13] >= 14.5) {
                if (input[1] >= 3.5) {
                    var11 = params[161];
                } else {
                    if (input[0] >= 2410.51) {
                        var11 = params[162];
                    } else {
                        var11 = params[163];
                    }
                }
            } else {
                if (input[4] >= 5.2786503) {
                    if (input[0] >= 263.36) {
                        var11 = params[164];
                    } else {
                        var11 = params[165];
                    }
                } else {
                    var11 = params[166];
                }
            }
        } else {
            if (input[7] >= 0.09395) {
                if (input[6] >= 0.12605) {
                    if (input[0] >= 586.695) {
                        var11 = params[167];
                    } else {
                        var11 = params[168];
                    }
                } else {
                    if (input[3] >= 0.12645) {
                        var11 = params[169];
                    } else {
                        var11 = params[170];
                    }
                }
            } else {
                if (input[11] >= 17.5) {
                    if (input[12] >= 2.5) {
                        var11 = params[171];
                    } else {
                        var11 = params[172];
                    }
                } else {
                    if (input[30] >= 0.5) {
                        var11 = params[173];
                    } else {
                        var11 = params[174];
                    }
                }
            }
        }
        double var12;
        if (input[17] >= 0.5) {
            if (input[13] >= 15.5) {
                if (input[8] >= 0.4733) {
                    var12 = params[175];
                } else {
                    if (input[7] >= 0.0085) {
                        var12 = params[176];
                    } else {
                        var12 = params[177];
                    }
                }
            } else {
                if (input[8] >= 0.2723) {
                    if (input[15] >= 20.5) {
                        var12 = params[178];
                    } else {
                        var12 = params[179];
                    }
                } else {
                    if (input[0] >= 650.095) {
                        var12 = params[180];
                    } else {
                        var12 = params[181];
                    }
                }
            }
        } else {
            if (input[5] >= 0.13865) {
                if (input[7] >= 0.03845) {
                    if (input[4] >= 3.2779) {
                        var12 = params[182];
                    } else {
                        var12 = params[183];
                    }
                } else {
                    if (input[4] >= 2.6822) {
                        var12 = params[184];
                    } else {
                        var12 = params[185];
                    }
                }
            } else {
                if (input[1] >= 5.5) {
                    if (input[28] >= 0.5) {
                        var12 = params[186];
                    } else {
                        var12 = params[187];
                    }
                } else {
                    if (input[0] >= 264.505) {
                        var12 = params[188];
                    } else {
                        var12 = params[189];
                    }
                }
            }
        }
        double var13;
        if (input[13] >= 207.5) {
            if (input[11] >= 62.5) {
                if (input[4] >= 4.5937) {
                    if (input[6] >= 0.12385) {
                        var13 = params[190];
                    } else {
                        var13 = params[191];
                    }
                } else {
                    if (input[11] >= 84.5) {
                        var13 = params[192];
                    } else {
                        var13 = params[193];
                    }
                }
            } else {
                if (input[31] >= 0.5) {
                    if (input[6] >= 0.24575001) {
                        var13 = params[194];
                    } else {
                        var13 = params[195];
                    }
                } else {
                    if (input[8] >= 0.19365) {
                        var13 = params[196];
                    } else {
                        var13 = params[197];
                    }
                }
            }
        } else {
            if (input[11] >= 33.5) {
                if (input[3] >= 0.21055) {
                    if (input[13] >= 75.5) {
                        var13 = params[198];
                    } else {
                        var13 = params[199];
                    }
                } else {
                    if (input[5] >= 0.03515) {
                        var13 = params[200];
                    } else {
                        var13 = params[201];
                    }
                }
            } else {
                if (input[13] >= 25.5) {
                    if (input[8] >= 0.04965) {
                        var13 = params[202];
                    } else {
                        var13 = params[203];
                    }
                } else {
                    if (input[2] >= 0.5) {
                        var13 = params[204];
                    } else {
                        var13 = params[205];
                    }
                }
            }
        }
        double var14;
        if (input[1] >= 2.5) {
            if (input[0] >= 246.235) {
                if (input[7] >= 0.03925) {
                    if (input[0] >= 1873.88) {
                        var14 = params[206];
                    } else {
                        var14 = params[207];
                    }
                } else {
                    if (input[4] >= 4.20195) {
                        var14 = params[208];
                    } else {
                        var14 = params[209];
                    }
                }
            } else {
                if (input[0] >= 163.545) {
                    if (input[5] >= 0.02115) {
                        var14 = params[210];
                    } else {
                        var14 = params[211];
                    }
                } else {
                    if (input[0] >= 34.754997) {
                        var14 = params[212];
                    } else {
                        var14 = params[213];
                    }
                }
            }
        } else {
            if (input[14] >= 0.5) {
                if (input[0] >= 1279.0449) {
                    if (input[0] >= 6803.185) {
                        var14 = params[214];
                    } else {
                        var14 = params[215];
                    }
                } else {
                    if (input[28] >= 0.5) {
                        var14 = params[216];
                    } else {
                        var14 = params[217];
                    }
                }
            } else {
                if (input[30] >= 0.5) {
                    if (input[28] >= 0.5) {
                        var14 = params[218];
                    } else {
                        var14 = params[219];
                    }
                } else {
                    if (input[0] >= 104.595) {
                        var14 = params[220];
                    } else {
                        var14 = params[221];
                    }
                }
            }
        }
        double var15;
        if (input[0] >= 6.5699997) {
            if (input[9] >= 0.1014) {
                if (input[4] >= 3.0679002) {
                    if (input[15] >= 19.5) {
                        var15 = params[222];
                    } else {
                        var15 = params[223];
                    }
                } else {
                    if (input[4] >= 1.3215001) {
                        var15 = params[224];
                    } else {
                        var15 = params[225];
                    }
                }
            } else {
                if (input[9] >= 0.017749999) {
                    if (input[13] >= 66.5) {
                        var15 = params[226];
                    } else {
                        var15 = params[227];
                    }
                } else {
                    if (input[4] >= 3.2696) {
                        var15 = params[228];
                    } else {
                        var15 = params[229];
                    }
                }
            }
        } else {
            var15 = params[230];
        }
        double var16;
        if (input[25] >= 0.5) {
            if (input[0] >= 1396.94) {
                if (input[8] >= 0.5277) {
                    var16 = params[231];
                } else {
                    if (input[11] >= 17.5) {
                        var16 = params[232];
                    } else {
                        var16 = params[233];
                    }
                }
            } else {
                if (input[1] >= 4.5) {
                    if (input[3] >= 0.6626) {
                        var16 = params[234];
                    } else {
                        var16 = params[235];
                    }
                } else {
                    if (input[11] >= 3.5) {
                        var16 = params[236];
                    } else {
                        var16 = params[237];
                    }
                }
            }
        } else {
            if (input[14] >= 3.5) {
                if (input[0] >= 495.59998) {
                    if (input[13] >= 32.5) {
                        var16 = params[238];
                    } else {
                        var16 = params[239];
                    }
                } else {
                    var16 = params[240];
                }
            } else {
                if (input[7] >= 0.48970002) {
                    var16 = params[241];
                } else {
                    if (input[12] >= 3.5) {
                        var16 = params[242];
                    } else {
                        var16 = params[243];
                    }
                }
            }
        }
        double var17;
        if (input[1] >= 10.5) {
            if (input[9] >= 0.00095) {
                var17 = params[244];
            } else {
                if (input[0] >= 859.245) {
                    var17 = params[245];
                } else {
                    var17 = params[246];
                }
            }
        } else {
            if (input[14] >= 1.5) {
                if (input[0] >= 2472.97) {
                    if (input[15] >= 28.5) {
                        var17 = params[247];
                    } else {
                        var17 = params[248];
                    }
                } else {
                    if (input[15] >= 137.5) {
                        var17 = params[249];
                    } else {
                        var17 = params[250];
                    }
                }
            } else {
                if (input[10] >= 1.5) {
                    if (input[22] >= 0.5) {
                        var17 = params[251];
                    } else {
                        var17 = params[252];
                    }
                } else {
                    if (input[11] >= 10.5) {
                        var17 = params[253];
                    } else {
                        var17 = params[254];
                    }
                }
            }
        }
        double var18;
        if (input[28] >= 0.5) {
            if (input[15] >= 32.5) {
                if (input[11] >= 156.5) {
                    if (input[8] >= 0.47125) {
                        var18 = params[255];
                    } else {
                        var18 = params[256];
                    }
                } else {
                    if (input[15] >= 64.5) {
                        var18 = params[257];
                    } else {
                        var18 = params[258];
                    }
                }
            } else {
                if (input[13] >= 90.0) {
                    if (input[15] >= 25.5) {
                        var18 = params[259];
                    } else {
                        var18 = params[260];
                    }
                } else {
                    if (input[5] >= 0.00915) {
                        var18 = params[261];
                    } else {
                        var18 = params[262];
                    }
                }
            }
        } else {
            if (input[15] >= 16.5) {
                if (input[26] >= 0.5) {
                    if (input[7] >= 0.1665) {
                        var18 = params[263];
                    } else {
                        var18 = params[264];
                    }
                } else {
                    if (input[8] >= 0.020350002) {
                        var18 = params[265];
                    } else {
                        var18 = params[266];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[5] >= 0.02905) {
                        var18 = params[267];
                    } else {
                        var18 = params[268];
                    }
                } else {
                    if (input[0] >= 25.595) {
                        var18 = params[269];
                    } else {
                        var18 = params[270];
                    }
                }
            }
        }
        double var19;
        if (input[24] >= 0.5) {
            if (input[3] >= 0.66455) {
                if (input[0] >= 639.08997) {
                    var19 = params[271];
                } else {
                    if (input[13] >= 9.5) {
                        var19 = params[272];
                    } else {
                        var19 = params[273];
                    }
                }
            } else {
                if (input[0] >= 213.47) {
                    if (input[8] >= 0.04495) {
                        var19 = params[274];
                    } else {
                        var19 = params[275];
                    }
                } else {
                    if (input[0] >= 153.055) {
                        var19 = params[276];
                    } else {
                        var19 = params[277];
                    }
                }
            }
        } else {
            if (input[3] >= 0.40525) {
                if (input[0] >= 58.405) {
                    if (input[3] >= 0.65365) {
                        var19 = params[278];
                    } else {
                        var19 = params[279];
                    }
                } else {
                    if (input[11] >= 25.5) {
                        var19 = params[280];
                    } else {
                        var19 = params[281];
                    }
                }
            } else {
                if (input[13] >= 163.0) {
                    if (input[0] >= 198.435) {
                        var19 = params[282];
                    } else {
                        var19 = params[283];
                    }
                } else {
                    if (input[2] >= 0.5) {
                        var19 = params[284];
                    } else {
                        var19 = params[285];
                    }
                }
            }
        }
        double var20;
        if (input[8] >= 0.60605) {
            if (input[0] >= 931.665) {
                if (input[12] >= 1.5) {
                    if (input[0] >= 1867.595) {
                        var20 = params[286];
                    } else {
                        var20 = params[287];
                    }
                } else {
                    var20 = params[288];
                }
            } else {
                if (input[8] >= 0.66725004) {
                    if (input[7] >= 0.0903) {
                        var20 = params[289];
                    } else {
                        var20 = params[290];
                    }
                } else {
                    if (input[15] >= 77.5) {
                        var20 = params[291];
                    } else {
                        var20 = params[292];
                    }
                }
            }
        } else {
            if (input[15] >= 167.5) {
                if (input[5] >= 0.01355) {
                    if (input[0] >= 389.515) {
                        var20 = params[293];
                    } else {
                        var20 = params[294];
                    }
                } else {
                    var20 = params[295];
                }
            } else {
                if (input[11] >= 14.5) {
                    if (input[0] >= 22.46) {
                        var20 = params[296];
                    } else {
                        var20 = params[297];
                    }
                } else {
                    if (input[15] >= 16.5) {
                        var20 = params[298];
                    } else {
                        var20 = params[299];
                    }
                }
            }
        }
        double var21;
        if (input[24] >= 0.5) {
            if (input[6] >= 0.19795) {
                if (input[6] >= 0.27534997) {
                    if (input[6] >= 0.41415) {
                        var21 = params[300];
                    } else {
                        var21 = params[301];
                    }
                } else {
                    if (input[4] >= 2.243) {
                        var21 = params[302];
                    } else {
                        var21 = params[303];
                    }
                }
            } else {
                if (input[5] >= 0.021249998) {
                    if (input[3] >= 0.5718) {
                        var21 = params[304];
                    } else {
                        var21 = params[305];
                    }
                } else {
                    if (input[4] >= 3.3671) {
                        var21 = params[306];
                    } else {
                        var21 = params[307];
                    }
                }
            }
        } else {
            if (input[25] >= 0.5) {
                if (input[15] >= 0.5) {
                    if (input[12] >= 0.5) {
                        var21 = params[308];
                    } else {
                        var21 = params[309];
                    }
                } else {
                    var21 = params[310];
                }
            } else {
                if (input[4] >= 4.4977503) {
                    if (input[9] >= 0.099649996) {
                        var21 = params[311];
                    } else {
                        var21 = params[312];
                    }
                } else {
                    if (input[1] >= 3.5) {
                        var21 = params[313];
                    } else {
                        var21 = params[314];
                    }
                }
            }
        }
        double var22;
        if (input[26] >= 0.5) {
            if (input[1] >= 0.5) {
                if (input[11] >= 24.5) {
                    if (input[0] >= 742.04504) {
                        var22 = params[315];
                    } else {
                        var22 = params[316];
                    }
                } else {
                    if (input[11] >= 7.5) {
                        var22 = params[317];
                    } else {
                        var22 = params[318];
                    }
                }
            } else {
                if (input[13] >= 2.5) {
                    if (input[0] >= 301.40503) {
                        var22 = params[319];
                    } else {
                        var22 = params[320];
                    }
                } else {
                    if (input[3] >= 0.3309) {
                        var22 = params[321];
                    } else {
                        var22 = params[322];
                    }
                }
            }
        } else {
            if (input[11] >= 9.5) {
                if (input[13] >= 80.5) {
                    if (input[12] >= 1.5) {
                        var22 = params[323];
                    } else {
                        var22 = params[324];
                    }
                } else {
                    if (input[4] >= 3.6124501) {
                        var22 = params[325];
                    } else {
                        var22 = params[326];
                    }
                }
            } else {
                if (input[15] >= 12.5) {
                    if (input[5] >= 0.00225) {
                        var22 = params[327];
                    } else {
                        var22 = params[328];
                    }
                } else {
                    if (input[14] >= 0.5) {
                        var22 = params[329];
                    } else {
                        var22 = params[330];
                    }
                }
            }
        }
        double var23;
        if (input[2] >= 0.5) {
            if (input[4] >= 0.28955) {
                if (input[13] >= 21.0) {
                    var23 = params[331];
                } else {
                    var23 = params[332];
                }
            } else {
                var23 = params[333];
            }
        } else {
            if (input[7] >= 0.24035001) {
                if (input[7] >= 0.34105) {
                    if (input[7] >= 0.4385) {
                        var23 = params[334];
                    } else {
                        var23 = params[335];
                    }
                } else {
                    if (input[12] >= 1.5) {
                        var23 = params[336];
                    } else {
                        var23 = params[337];
                    }
                }
            } else {
                if (input[7] >= 0.21855) {
                    if (input[5] >= 0.04095) {
                        var23 = params[338];
                    } else {
                        var23 = params[339];
                    }
                } else {
                    if (input[6] >= 0.42514998) {
                        var23 = params[340];
                    } else {
                        var23 = params[341];
                    }
                }
            }
        }
        double var24;
        if (input[17] >= 0.5) {
            if (input[7] >= 0.00865) {
                var24 = params[342];
            } else {
                if (input[7] >= 0.0056) {
                    if (input[6] >= 0.12605) {
                        var24 = params[343];
                    } else {
                        var24 = params[344];
                    }
                } else {
                    var24 = params[345];
                }
            }
        } else {
            if (input[27] >= 0.5) {
                var24 = params[346];
            } else {
                if (input[5] >= 0.00325) {
                    if (input[3] >= 0.6217) {
                        var24 = params[347];
                    } else {
                        var24 = params[348];
                    }
                } else {
                    if (input[4] >= 4.3227997) {
                        var24 = params[349];
                    } else {
                        var24 = params[350];
                    }
                }
            }
        }
        double var25;
        if (input[1] >= 3.5) {
            if (input[4] >= 4.21925) {
                if (input[9] >= 0.00885) {
                    if (input[15] >= 27.5) {
                        var25 = params[351];
                    } else {
                        var25 = params[352];
                    }
                } else {
                    if (input[7] >= 0.022750001) {
                        var25 = params[353];
                    } else {
                        var25 = params[354];
                    }
                }
            } else {
                if (input[3] >= 0.6351) {
                    if (input[6] >= 0.06515) {
                        var25 = params[355];
                    } else {
                        var25 = params[356];
                    }
                } else {
                    if (input[15] >= 65.5) {
                        var25 = params[357];
                    } else {
                        var25 = params[358];
                    }
                }
            }
        } else {
            if (input[6] >= 0.03925) {
                if (input[6] >= 0.08975) {
                    if (input[8] >= 0.09775) {
                        var25 = params[359];
                    } else {
                        var25 = params[360];
                    }
                } else {
                    if (input[6] >= 0.07575) {
                        var25 = params[361];
                    } else {
                        var25 = params[362];
                    }
                }
            } else {
                if (input[8] >= 0.01805) {
                    if (input[28] >= 0.5) {
                        var25 = params[363];
                    } else {
                        var25 = params[364];
                    }
                } else {
                    if (input[5] >= 0.0009) {
                        var25 = params[365];
                    } else {
                        var25 = params[366];
                    }
                }
            }
        }
        double var26;
        if (input[13] >= 218.5) {
            if (input[6] >= 0.31875) {
                var26 = params[367];
            } else {
                if (input[6] >= 0.1264) {
                    if (input[15] >= 78.0) {
                        var26 = params[368];
                    } else {
                        var26 = params[369];
                    }
                } else {
                    if (input[24] >= 0.5) {
                        var26 = params[370];
                    } else {
                        var26 = params[371];
                    }
                }
            }
        } else {
            if (input[11] >= 54.5) {
                if (input[6] >= 0.23615) {
                    if (input[5] >= 0.01265) {
                        var26 = params[372];
                    } else {
                        var26 = params[373];
                    }
                } else {
                    if (input[15] >= 106.5) {
                        var26 = params[374];
                    } else {
                        var26 = params[375];
                    }
                }
            } else {
                if (input[9] >= 0.07275) {
                    if (input[11] >= 16.5) {
                        var26 = params[376];
                    } else {
                        var26 = params[377];
                    }
                } else {
                    if (input[9] >= 0.01305) {
                        var26 = params[378];
                    } else {
                        var26 = params[379];
                    }
                }
            }
        }
        double var27;
        if (input[22] >= 0.5) {
            if (input[0] >= 205.05) {
                if (input[10] >= 0.5) {
                    var27 = params[380];
                } else {
                    if (input[6] >= 0.2983) {
                        var27 = params[381];
                    } else {
                        var27 = params[382];
                    }
                }
            } else {
                var27 = params[383];
            }
        } else {
            if (input[0] >= 14.755) {
                if (input[28] >= 0.5) {
                    if (input[26] >= 0.5) {
                        var27 = params[384];
                    } else {
                        var27 = params[385];
                    }
                } else {
                    if (input[15] >= 43.5) {
                        var27 = params[386];
                    } else {
                        var27 = params[387];
                    }
                }
            } else {
                if (input[15] >= 0.5) {
                    var27 = params[388];
                } else {
                    var27 = params[389];
                }
            }
        }
        double var28;
        if (input[4] >= 5.2238503) {
            if (input[13] >= 34.5) {
                if (input[5] >= 0.0124) {
                    var28 = params[390];
                } else {
                    var28 = params[391];
                }
            } else {
                if (input[8] >= 0.03765) {
                    if (input[5] >= 0.0193) {
                        var28 = params[392];
                    } else {
                        var28 = params[393];
                    }
                } else {
                    if (input[6] >= 0.02935) {
                        var28 = params[394];
                    } else {
                        var28 = params[395];
                    }
                }
            }
        } else {
            if (input[0] >= 992.525) {
                if (input[12] >= 4.5) {
                    if (input[7] >= 0.0716) {
                        var28 = params[396];
                    } else {
                        var28 = params[397];
                    }
                } else {
                    if (input[13] >= 49.5) {
                        var28 = params[398];
                    } else {
                        var28 = params[399];
                    }
                }
            } else {
                if (input[12] >= 3.5) {
                    if (input[6] >= 0.1294) {
                        var28 = params[400];
                    } else {
                        var28 = params[401];
                    }
                } else {
                    if (input[28] >= 0.5) {
                        var28 = params[402];
                    } else {
                        var28 = params[403];
                    }
                }
            }
        }
        double var29;
        if (input[6] >= 0.19895) {
            if (input[6] >= 0.21865001) {
                if (input[4] >= 2.15455) {
                    if (input[15] >= 109.5) {
                        var29 = params[404];
                    } else {
                        var29 = params[405];
                    }
                } else {
                    if (input[15] >= 12.5) {
                        var29 = params[406];
                    } else {
                        var29 = params[407];
                    }
                }
            } else {
                if (input[8] >= 1.08325) {
                    var29 = params[408];
                } else {
                    if (input[7] >= 0.00495) {
                        var29 = params[409];
                    } else {
                        var29 = params[410];
                    }
                }
            }
        } else {
            if (input[5] >= 0.068) {
                if (input[13] >= 29.5) {
                    if (input[8] >= 0.238) {
                        var29 = params[411];
                    } else {
                        var29 = params[412];
                    }
                } else {
                    var29 = params[413];
                }
            } else {
                if (input[9] >= 0.10295) {
                    var29 = params[414];
                } else {
                    if (input[5] >= 0.04845) {
                        var29 = params[415];
                    } else {
                        var29 = params[416];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
