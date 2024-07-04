package org.matsim.prepare.facilities;
import org.matsim.application.prepare.Predictor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
    
/**
* Generated model, do not modify.
* Model: XGBRegressor(alpha=0.1491509012548297, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.4531277098423365,
             eval_metric='mae', feature_types=None, gamma=0.034321311924908296,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.015230729860715594,
             learning_rate=0.453127712, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=2, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...)
* Error: 0.080080
*/
public final class FacilityAttractionModelOther implements Predictor {
    
    public static FacilityAttractionModelOther INSTANCE = new FacilityAttractionModelOther();
    public static final double[] DEFAULT_PARAMS = {-0.15773548, -0.063673854, -0.15752967, -0.06291148, -0.17241873, -0.0935872, -0.21541238, -0.19548933, -0.14534456, 0.020626921, -0.10064369, -0.0048545552, 0.023303475, -0.16616772, -0.13042909, -0.048884686, -0.07557888, -0.031447947, -0.10183693, -0.08576271, -0.029562064, 0.027142545, -0.040778752, -0.107682765, -0.021134058, -0.104367524, -0.11237294, -0.1028285, 0.012165038, -0.08450655, -0.007497731, -0.055066627, -0.08414083, -0.05138273, -0.031697437, -0.015737373, -0.087703444, -0.11793857, 0.021675078, -0.053410288, -0.024896204, -0.023752825, -0.059034012, 0.10457431, -0.020419575, -0.04256721, 0.010556376, -0.027754432, -0.040224466, -0.0074449526, -0.018812349, 0.062368147, -0.031578794, -0.02075476, -0.036530342, 0.02294408, -0.021654123, -0.040951878, 0.09426138, 0.009573344, -0.04197548, -0.03134391, -0.04307008, -0.033301834, -0.004574249, 0.0, -0.028322795, -0.012108415, 0.0030044946, -0.017663427, 0.007667208, 0.0, 0.07649852, -0.018147798, -0.008222865, -0.017173653, -0.003027344, 0.060326513, 0.009743657, 0.10610175, -0.014935938, 0.044140626, -0.009732546, -0.013013501, -0.007409421, -0.014783988, -0.0098274145, 0.017263576, 0.051532704, -0.00319842, 0.018245218, 0.0059866603, 0.02414072, 0.08690798, -0.011617567, 0.07525694, -0.0122354785, -0.015397626, 0.047320716, -0.002831519, 0.023119606, 0.0021645508, -0.010858987, 0.08008925, -0.0058534197, 0.045448516, -2.0209616e-05, 0.026390215, 0.001367693, -0.0068995836, -0.0020171888, 0.028762916, -0.000914382, -0.009608554, 0.013191611, 0.0, 0.043837693, -0.0061899163, -0.0022640962, -0.029538391, 0.06713373, 0.06470669, 0.021442087, -0.008688387, 0.027308118, -0.0026079756, 0.014739256, 0.034438122, 0.0063779326, 0.015377231, -0.00022864589, -0.0061305254, 0.14920723, 0.11119088, 0.011358049, -0.014687488, -0.0029486048, -0.002632513, 0.032328714, 0.078808725, 0.0088956095, 0.015293169, -0.0057477592, 0.038603146, 0.16271034, 0.01788566, 0.13961957, -0.003967554, 0.0066375923, -0.0014492854, -0.0045495206, 0.035961103, 0.012849233, 0.09357974, 0.00024204001, 0.096954145, 0.01657323, 0.1603168, 0.032115094, 0.114863425, -0.023642525, -0.012700851, 0.009489032, 0.0047793426, -0.012452338, 0.020823572, -0.00061502284, 0.0914505, -0.002357629, 0.026377842, 0.0039923904, -0.0017133944, 0.04356291, -0.0724382, 0.0010881218, 0.008756297, 0.035649356, -0.011954582, -0.006478151, -0.027693674, -0.0012610562, 0.019586608, -0.067476496, -0.007502447, -0.011280721, 0.04819015, -0.0065260446, 0.03371862, 0.0017093901, -0.005475427, -0.014017439, -0.0052980385, 0.048837997, 0.013472815, 0.012774257, 0.0, 0.02401029, 0.0017639636, -0.0069675134, -5.7998695e-06, -0.026378984, -0.00783126, 0.025625806, 0.00023505079, -0.009740133, 0.014883403, 0.094410546, -0.0063205077, -0.025523894, -0.009037307, 0.03707988, 0.0020848303, 0.0035471737, 0.024690764, 0.066594705, -0.0002595451, 0.021542806, -0.0026475762, -0.015606223, 0.007771523, 0.19203894, 0.035120483, 0.019798057, 0.0056845127, 0.0034877823, 0.028489394, -0.005180513, -0.0005455521, 0.0013704484, -0.0043489574, 0.009960251, 0.001075938, 0.0018773504, -0.011236678, 0.006102028, -0.00016552625, 0.07817339, 0.009830503, -0.005808812, 0.0038867488, 0.072771974, 0.0, 0.008427596, -0.01341282, 0.021753429, -0.015027221, 0.09983382, -4.9791357e-05, 0.006646704, -0.009143565, 0.014872922, 0.04029301, 0.14171116, -0.0042411117, 0.08228601, 0.00012020674, 0.057914685, 0.017240716, 0.0, 0.008139578, -0.00044618195, 0.059871797, 0.013480583, -0.00770694, 0.0032978973, -0.079208694, 0.018594721, -0.011455154, 0.033858445, -0.0018347108, 0.0007939317, 0.090592146, -0.0070478725, -0.0010072042, -0.012700405, 0.051814422, -0.006076862, -0.0033754995, 0.015651342, -0.0037630594, 0.0009544621, 3.0756866e-05, 0.015295331, -0.010212924, 0.09481881, -0.020273348, -0.0062214676, 0.0002725903, -0.0038534729, -0.028784404, -0.004446802, -0.048176438, -0.00065450044, -0.04740498, 0.0, -0.14805137, -0.032121383, 0.0, 0.061419897, 0.011964108, 0.0007705908, 0.01406273, 0.0046175355, -0.00046595573, -0.0016357189, 0.013142575, -0.0150031755, 0.05802763, 0.122372955, -0.0046170834, 0.036574166, -0.008379121, -0.034502454, -0.00071603886, 0.044105615, 0.064981654, 5.293463e-05, -0.033073433, 0.0, -0.00089660246, 0.0154301375, -0.0008506584, -0.01466735, 0.052983817, -0.005330001, 0.0014417404, -5.4732125e-05, -0.0031580108, -0.018534642, 0.00081960845, 0.013871846, 0.014725611, -0.0044276156, -0.0025894276, -0.017087756, -0.002324986, 0.042615023, -0.013945633, -0.0026781007, 0.15849514, 0.00601869, 0.0017727563, -0.00010748525, -0.0018174375, 0.017547078, -0.000826828, 0.00029452675, 0.14208613, -0.06761882, 0.0, 0.08434974, -0.0039775334, -0.002856609, -0.021715593, 0.07277253, -0.0015363286, -8.2712635e-05, -0.0054267664, 0.05327552, 0.0046971436, 0.09548657, -0.01967202, -0.0031136558, -0.026856098, 0.03162408, 0.035382964, 0.15857963, -0.03109688, 0.00024123678, 0.15240332, 0.0033340326, -0.019753488, 0.08873925, 0.0028288488, 0.018680753, -0.00073100935, -0.0083520515, 0.002975146, 0.010793633, 3.552884e-05, -0.014092967, 0.117820226, -0.0033780425, 0.005026666, 0.13003501, -0.0130093675, 0.0745645, -0.0017171134, 0.021001425, 0.06169818, -0.0019867723, -0.00018735394, 0.001069669, -0.013332601, 0.11719391, 0.022066562, 0.017699946, 0.0008086213, 0.0014267727, -0.0011659449, -0.004884082, 0.05571611, 0.10198606, 0.012667798, 0.016447518, -0.0061274646, 0.0014109488, -0.00063293806, 0.14481418, 0.001676948, 0.003631729, -0.009415969, 0.19636299, 0.041257005, 0.068791516, 0.008061615, 0.10048408, 0.0, -0.0112323165, 0.007755606, -0.0001872087, 0.00143287, 0.0052706725, -0.0007890529, 0.008849923, 0.0005674049, 0.042534426, 0.010650248, 0.0009999194, -0.0061222073, 0.081625186, 0.008789717, -0.000878019, 0.007568995, 0.04061468, 0.0007155539, 0.062350903, 0.004201035, 0.009785261, 0.00014053471};

    @Override
    public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
        return predict(features, categories, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
        double[] data = new double[31];
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
		data[26] = features.getDouble("religious");
		data[27] = features.getDouble("resident");
		data[28] = features.getDouble("shop");
		data[29] = features.getDouble("shop_daily");
		data[30] = features.getDouble("work");

        return data;
    }
    
    @Override
    public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories, double[] params) {

        double[] data = getData(features, categories);
        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);
    
        return Math.min(Math.max(score(data, params), 0.000000), 0.610552);
    }
    public static double score(double[] input, double[] params) {
        double var0;
        if (input[0] >= 42.425) {
            if (input[12] >= 1.5) {
                if (input[27] >= 0.5) {
                    if (input[0] >= 103.8) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[0] >= 994.88) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[14] >= 0.5) {
                    if (input[0] >= 121.305) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    if (input[0] >= 1640.88) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                }
            }
        } else {
            if (input[30] >= 0.5) {
                if (input[12] >= 1.5) {
                    if (input[3] >= 0.624) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                } else {
                    if (input[0] >= 15.684999) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                }
            } else {
                if (input[0] >= 20.96) {
                    if (input[2] >= 0.5) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                } else {
                    if (input[0] >= 8.965) {
                        var0 = params[14];
                    } else {
                        var0 = params[15];
                    }
                }
            }
        }
        double var1;
        if (input[12] >= 1.5) {
            if (input[0] >= 132.035) {
                if (input[16] >= 0.5) {
                    if (input[0] >= 239.81) {
                        var1 = params[16];
                    } else {
                        var1 = params[17];
                    }
                } else {
                    if (input[11] >= 29.5) {
                        var1 = params[18];
                    } else {
                        var1 = params[19];
                    }
                }
            } else {
                if (input[16] >= 0.5) {
                    if (input[11] >= 32.0) {
                        var1 = params[20];
                    } else {
                        var1 = params[21];
                    }
                } else {
                    if (input[7] >= 0.01375) {
                        var1 = params[22];
                    } else {
                        var1 = params[23];
                    }
                }
            }
        } else {
            if (input[0] >= 50.965) {
                if (input[13] >= 9.5) {
                    if (input[2] >= 0.5) {
                        var1 = params[24];
                    } else {
                        var1 = params[25];
                    }
                } else {
                    if (input[0] >= 88.725006) {
                        var1 = params[26];
                    } else {
                        var1 = params[27];
                    }
                }
            } else {
                if (input[0] >= 14.559999) {
                    if (input[14] >= 0.5) {
                        var1 = params[28];
                    } else {
                        var1 = params[29];
                    }
                } else {
                    if (input[15] >= 7.5) {
                        var1 = params[30];
                    } else {
                        var1 = params[31];
                    }
                }
            }
        }
        double var2;
        if (input[12] >= 1.5) {
            if (input[0] >= 106.625) {
                if (input[0] >= 3220.525) {
                    var2 = params[32];
                } else {
                    if (input[11] >= 22.5) {
                        var2 = params[33];
                    } else {
                        var2 = params[34];
                    }
                }
            } else {
                if (input[27] >= 0.5) {
                    if (input[5] >= 0.00055) {
                        var2 = params[35];
                    } else {
                        var2 = params[36];
                    }
                } else {
                    if (input[4] >= 5.12475) {
                        var2 = params[37];
                    } else {
                        var2 = params[38];
                    }
                }
            }
        } else {
            if (input[0] >= 31.515) {
                if (input[18] >= 0.5) {
                    if (input[11] >= 21.5) {
                        var2 = params[39];
                    } else {
                        var2 = params[40];
                    }
                } else {
                    if (input[5] >= 0.13855001) {
                        var2 = params[41];
                    } else {
                        var2 = params[42];
                    }
                }
            } else {
                if (input[2] >= 0.5) {
                    var2 = params[43];
                } else {
                    if (input[11] >= 10.5) {
                        var2 = params[44];
                    } else {
                        var2 = params[45];
                    }
                }
            }
        }
        double var3;
        if (input[7] >= 0.00765) {
            if (input[12] >= 1.5) {
                if (input[11] >= 33.5) {
                    if (input[7] >= 0.11415) {
                        var3 = params[46];
                    } else {
                        var3 = params[47];
                    }
                } else {
                    if (input[0] >= 1659.155) {
                        var3 = params[48];
                    } else {
                        var3 = params[49];
                    }
                }
            } else {
                if (input[14] >= 0.5) {
                    if (input[0] >= 37.825) {
                        var3 = params[50];
                    } else {
                        var3 = params[51];
                    }
                } else {
                    if (input[0] >= 80.455) {
                        var3 = params[52];
                    } else {
                        var3 = params[53];
                    }
                }
            }
        } else {
            if (input[0] >= 473.99) {
                if (input[12] >= 3.5) {
                    if (input[0] >= 766.06) {
                        var3 = params[54];
                    } else {
                        var3 = params[55];
                    }
                } else {
                    if (input[1] >= 6.5) {
                        var3 = params[56];
                    } else {
                        var3 = params[57];
                    }
                }
            } else {
                if (input[2] >= 0.5) {
                    if (input[4] >= 3.7488499) {
                        var3 = params[58];
                    } else {
                        var3 = params[59];
                    }
                } else {
                    if (input[3] >= 0.71155) {
                        var3 = params[60];
                    } else {
                        var3 = params[61];
                    }
                }
            }
        }
        double var4;
        if (input[0] >= 2401.98) {
            if (input[0] >= 6517.92) {
                if (input[16] >= 0.5) {
                    var4 = params[62];
                } else {
                    var4 = params[63];
                }
            } else {
                if (input[12] >= 3.5) {
                    var4 = params[64];
                } else {
                    if (input[1] >= 10.5) {
                        var4 = params[65];
                    } else {
                        var4 = params[66];
                    }
                }
            }
        } else {
            if (input[16] >= 0.5) {
                if (input[5] >= 0.01565) {
                    if (input[0] >= 442.425) {
                        var4 = params[67];
                    } else {
                        var4 = params[68];
                    }
                } else {
                    if (input[0] >= 180.275) {
                        var4 = params[69];
                    } else {
                        var4 = params[70];
                    }
                }
            } else {
                if (input[2] >= 0.5) {
                    if (input[0] >= 532.875) {
                        var4 = params[71];
                    } else {
                        var4 = params[72];
                    }
                } else {
                    if (input[3] >= 0.27945) {
                        var4 = params[73];
                    } else {
                        var4 = params[74];
                    }
                }
            }
        }
        double var5;
        if (input[25] >= 0.5) {
            if (input[0] >= 195.83) {
                if (input[0] >= 1621.855) {
                    if (input[0] >= 5178.18) {
                        var5 = params[75];
                    } else {
                        var5 = params[76];
                    }
                } else {
                    if (input[5] >= 0.11235) {
                        var5 = params[77];
                    } else {
                        var5 = params[78];
                    }
                }
            } else {
                if (input[1] >= 0.5) {
                    if (input[3] >= 0.2575) {
                        var5 = params[79];
                    } else {
                        var5 = params[80];
                    }
                } else {
                    if (input[7] >= 0.0064000003) {
                        var5 = params[81];
                    } else {
                        var5 = params[82];
                    }
                }
            }
        } else {
            if (input[0] >= 22.695) {
                if (input[15] >= 0.5) {
                    if (input[11] >= 36.5) {
                        var5 = params[83];
                    } else {
                        var5 = params[84];
                    }
                } else {
                    if (input[0] >= 130.705) {
                        var5 = params[85];
                    } else {
                        var5 = params[86];
                    }
                }
            } else {
                if (input[11] >= 23.5) {
                    if (input[5] >= 0.00385) {
                        var5 = params[87];
                    } else {
                        var5 = params[88];
                    }
                } else {
                    if (input[3] >= 0.3583) {
                        var5 = params[89];
                    } else {
                        var5 = params[90];
                    }
                }
            }
        }
        double var6;
        if (input[12] >= 2.5) {
            if (input[0] >= 232.88) {
                if (input[13] >= 31.5) {
                    if (input[13] >= 61.5) {
                        var6 = params[91];
                    } else {
                        var6 = params[92];
                    }
                } else {
                    if (input[15] >= 28.5) {
                        var6 = params[93];
                    } else {
                        var6 = params[94];
                    }
                }
            } else {
                if (input[4] >= 3.6826) {
                    if (input[6] >= 0.025899999) {
                        var6 = params[95];
                    } else {
                        var6 = params[96];
                    }
                } else {
                    if (input[11] >= 20.0) {
                        var6 = params[97];
                    } else {
                        var6 = params[98];
                    }
                }
            }
        } else {
            if (input[0] >= 535.125) {
                if (input[29] >= 0.5) {
                    if (input[15] >= 6.5) {
                        var6 = params[99];
                    } else {
                        var6 = params[100];
                    }
                } else {
                    if (input[1] >= 5.5) {
                        var6 = params[101];
                    } else {
                        var6 = params[102];
                    }
                }
            } else {
                if (input[3] >= 0.39485002) {
                    if (input[14] >= 3.5) {
                        var6 = params[103];
                    } else {
                        var6 = params[104];
                    }
                } else {
                    if (input[1] >= 10.5) {
                        var6 = params[105];
                    } else {
                        var6 = params[106];
                    }
                }
            }
        }
        double var7;
        if (input[6] >= 0.05785) {
            if (input[0] >= 272.425) {
                if (input[14] >= 2.5) {
                    if (input[8] >= 0.38905) {
                        var7 = params[107];
                    } else {
                        var7 = params[108];
                    }
                } else {
                    if (input[11] >= 22.5) {
                        var7 = params[109];
                    } else {
                        var7 = params[110];
                    }
                }
            } else {
                if (input[27] >= 0.5) {
                    if (input[12] >= 2.5) {
                        var7 = params[111];
                    } else {
                        var7 = params[112];
                    }
                } else {
                    if (input[17] >= 0.5) {
                        var7 = params[113];
                    } else {
                        var7 = params[114];
                    }
                }
            }
        } else {
            if (input[0] >= 12.655) {
                if (input[14] >= 2.5) {
                    if (input[4] >= 4.0263) {
                        var7 = params[115];
                    } else {
                        var7 = params[116];
                    }
                } else {
                    if (input[0] >= 91.735) {
                        var7 = params[117];
                    } else {
                        var7 = params[118];
                    }
                }
            } else {
                if (input[4] >= 4.627) {
                    if (input[0] >= 8.405) {
                        var7 = params[119];
                    } else {
                        var7 = params[120];
                    }
                } else {
                    if (input[11] >= 5.5) {
                        var7 = params[121];
                    } else {
                        var7 = params[122];
                    }
                }
            }
        }
        double var8;
        if (input[15] >= 3.5) {
            if (input[11] >= 14.5) {
                if (input[12] >= 3.5) {
                    if (input[0] >= 1644.975) {
                        var8 = params[123];
                    } else {
                        var8 = params[124];
                    }
                } else {
                    if (input[3] >= 0.17304999) {
                        var8 = params[125];
                    } else {
                        var8 = params[126];
                    }
                }
            } else {
                if (input[13] >= 30.5) {
                    if (input[4] >= 5.26085) {
                        var8 = params[127];
                    } else {
                        var8 = params[128];
                    }
                } else {
                    if (input[15] >= 17.5) {
                        var8 = params[129];
                    } else {
                        var8 = params[130];
                    }
                }
            }
        } else {
            if (input[5] >= 0.1936) {
                if (input[3] >= 0.2666) {
                    if (input[5] >= 0.197) {
                        var8 = params[131];
                    } else {
                        var8 = params[132];
                    }
                } else {
                    if (input[0] >= 111.165) {
                        var8 = params[133];
                    } else {
                        var8 = params[134];
                    }
                }
            } else {
                if (input[8] >= 0.92620003) {
                    if (input[4] >= 2.65095) {
                        var8 = params[135];
                    } else {
                        var8 = params[136];
                    }
                } else {
                    if (input[0] >= 7.5150003) {
                        var8 = params[137];
                    } else {
                        var8 = params[138];
                    }
                }
            }
        }
        double var9;
        if (input[24] >= 0.5) {
            if (input[0] >= 229.48999) {
                if (input[13] >= 23.5) {
                    if (input[3] >= 0.66935) {
                        var9 = params[139];
                    } else {
                        var9 = params[140];
                    }
                } else {
                    if (input[11] >= 18.5) {
                        var9 = params[141];
                    } else {
                        var9 = params[142];
                    }
                }
            } else {
                if (input[0] >= 225.08) {
                    if (input[0] >= 226.805) {
                        var9 = params[143];
                    } else {
                        var9 = params[144];
                    }
                } else {
                    if (input[6] >= 0.0056999996) {
                        var9 = params[145];
                    } else {
                        var9 = params[146];
                    }
                }
            }
        } else {
            if (input[0] >= 95.58) {
                if (input[1] >= 5.5) {
                    if (input[4] >= 4.1864) {
                        var9 = params[147];
                    } else {
                        var9 = params[148];
                    }
                } else {
                    if (input[6] >= 0.03035) {
                        var9 = params[149];
                    } else {
                        var9 = params[150];
                    }
                }
            } else {
                if (input[15] >= 19.5) {
                    if (input[30] >= 0.5) {
                        var9 = params[151];
                    } else {
                        var9 = params[152];
                    }
                } else {
                    if (input[26] >= 0.5) {
                        var9 = params[153];
                    } else {
                        var9 = params[154];
                    }
                }
            }
        }
        double var10;
        if (input[5] >= 0.11655) {
            if (input[13] >= 89.5) {
                if (input[15] >= 24.5) {
                    if (input[6] >= 0.28825) {
                        var10 = params[155];
                    } else {
                        var10 = params[156];
                    }
                } else {
                    if (input[11] >= 16.5) {
                        var10 = params[157];
                    } else {
                        var10 = params[158];
                    }
                }
            } else {
                if (input[7] >= 0.16964999) {
                    if (input[6] >= 0.4951) {
                        var10 = params[159];
                    } else {
                        var10 = params[160];
                    }
                } else {
                    if (input[9] >= 0.0001) {
                        var10 = params[161];
                    } else {
                        var10 = params[162];
                    }
                }
            }
        } else {
            if (input[0] >= 3.5349998) {
                if (input[17] >= 0.5) {
                    if (input[12] >= 1.5) {
                        var10 = params[163];
                    } else {
                        var10 = params[164];
                    }
                } else {
                    if (input[2] >= 0.5) {
                        var10 = params[165];
                    } else {
                        var10 = params[166];
                    }
                }
            } else {
                var10 = params[167];
            }
        }
        double var11;
        if (input[13] >= 2.5) {
            if (input[0] >= 264.285) {
                if (input[8] >= 1.1108501) {
                    if (input[0] >= 808.935) {
                        var11 = params[168];
                    } else {
                        var11 = params[169];
                    }
                } else {
                    if (input[29] >= 0.5) {
                        var11 = params[170];
                    } else {
                        var11 = params[171];
                    }
                }
            } else {
                if (input[2] >= 0.5) {
                    if (input[27] >= 0.5) {
                        var11 = params[172];
                    } else {
                        var11 = params[173];
                    }
                } else {
                    if (input[3] >= 0.23304999) {
                        var11 = params[174];
                    } else {
                        var11 = params[175];
                    }
                }
            }
        } else {
            if (input[3] >= 0.72005) {
                if (input[15] >= 2.5) {
                    if (input[4] >= 4.4611998) {
                        var11 = params[176];
                    } else {
                        var11 = params[177];
                    }
                } else {
                    if (input[0] >= 42.665) {
                        var11 = params[178];
                    } else {
                        var11 = params[179];
                    }
                }
            } else {
                if (input[27] >= 0.5) {
                    if (input[0] >= 12.040001) {
                        var11 = params[180];
                    } else {
                        var11 = params[181];
                    }
                } else {
                    if (input[12] >= 1.5) {
                        var11 = params[182];
                    } else {
                        var11 = params[183];
                    }
                }
            }
        }
        double var12;
        if (input[11] >= 46.5) {
            if (input[14] >= 2.5) {
                if (input[15] >= 110.5) {
                    if (input[13] >= 186.0) {
                        var12 = params[184];
                    } else {
                        var12 = params[185];
                    }
                } else {
                    if (input[0] >= 272.58002) {
                        var12 = params[186];
                    } else {
                        var12 = params[187];
                    }
                }
            } else {
                if (input[8] >= 0.22325) {
                    if (input[4] >= 3.7043) {
                        var12 = params[188];
                    } else {
                        var12 = params[189];
                    }
                } else {
                    if (input[16] >= 0.5) {
                        var12 = params[190];
                    } else {
                        var12 = params[191];
                    }
                }
            }
        } else {
            if (input[9] >= 0.05755) {
                if (input[7] >= 0.01765) {
                    if (input[4] >= 4.0085497) {
                        var12 = params[192];
                    } else {
                        var12 = params[193];
                    }
                } else {
                    if (input[13] >= 51.5) {
                        var12 = params[194];
                    } else {
                        var12 = params[195];
                    }
                }
            } else {
                if (input[25] >= 0.5) {
                    if (input[13] >= 56.5) {
                        var12 = params[196];
                    } else {
                        var12 = params[197];
                    }
                } else {
                    if (input[7] >= 0.2465) {
                        var12 = params[198];
                    } else {
                        var12 = params[199];
                    }
                }
            }
        }
        double var13;
        if (input[14] >= 1.5) {
            if (input[10] >= 0.5) {
                if (input[0] >= 1400.825) {
                    if (input[3] >= 0.21880001) {
                        var13 = params[200];
                    } else {
                        var13 = params[201];
                    }
                } else {
                    if (input[7] >= 0.105849996) {
                        var13 = params[202];
                    } else {
                        var13 = params[203];
                    }
                }
            } else {
                if (input[6] >= 0.047650002) {
                    if (input[0] >= 1665.98) {
                        var13 = params[204];
                    } else {
                        var13 = params[205];
                    }
                } else {
                    if (input[1] >= 7.5) {
                        var13 = params[206];
                    } else {
                        var13 = params[207];
                    }
                }
            }
        } else {
            if (input[14] >= 0.5) {
                if (input[11] >= 14.5) {
                    if (input[12] >= 1.5) {
                        var13 = params[208];
                    } else {
                        var13 = params[209];
                    }
                } else {
                    if (input[13] >= 54.5) {
                        var13 = params[210];
                    } else {
                        var13 = params[211];
                    }
                }
            } else {
                if (input[29] >= 0.5) {
                    if (input[12] >= 1.5) {
                        var13 = params[212];
                    } else {
                        var13 = params[213];
                    }
                } else {
                    if (input[12] >= 5.5) {
                        var13 = params[214];
                    } else {
                        var13 = params[215];
                    }
                }
            }
        }
        double var14;
        if (input[24] >= 0.5) {
            if (input[6] >= 0.16005) {
                if (input[6] >= 0.21595001) {
                    if (input[3] >= 0.4449) {
                        var14 = params[216];
                    } else {
                        var14 = params[217];
                    }
                } else {
                    if (input[3] >= 0.43535) {
                        var14 = params[218];
                    } else {
                        var14 = params[219];
                    }
                }
            } else {
                if (input[4] >= 5.1549997) {
                    if (input[5] >= 0.038850002) {
                        var14 = params[220];
                    } else {
                        var14 = params[221];
                    }
                } else {
                    if (input[6] >= 0.1178) {
                        var14 = params[222];
                    } else {
                        var14 = params[223];
                    }
                }
            }
        } else {
            if (input[3] >= 0.48245) {
                if (input[9] >= 0.11505) {
                    if (input[3] >= 0.53545) {
                        var14 = params[224];
                    } else {
                        var14 = params[225];
                    }
                } else {
                    if (input[12] >= 1.5) {
                        var14 = params[226];
                    } else {
                        var14 = params[227];
                    }
                }
            } else {
                if (input[0] >= 848.27) {
                    if (input[1] >= 0.5) {
                        var14 = params[228];
                    } else {
                        var14 = params[229];
                    }
                } else {
                    if (input[1] >= 6.5) {
                        var14 = params[230];
                    } else {
                        var14 = params[231];
                    }
                }
            }
        }
        double var15;
        if (input[4] >= 1.097) {
            if (input[4] >= 1.7586501) {
                if (input[6] >= 0.40775) {
                    if (input[4] >= 3.6377501) {
                        var15 = params[232];
                    } else {
                        var15 = params[233];
                    }
                } else {
                    if (input[5] >= 0.08305) {
                        var15 = params[234];
                    } else {
                        var15 = params[235];
                    }
                }
            } else {
                if (input[15] >= 21.5) {
                    if (input[6] >= 0.20594999) {
                        var15 = params[236];
                    } else {
                        var15 = params[237];
                    }
                } else {
                    if (input[7] >= 0.05565) {
                        var15 = params[238];
                    } else {
                        var15 = params[239];
                    }
                }
            }
        } else {
            if (input[8] >= 0.00605) {
                if (input[29] >= 0.5) {
                    if (input[8] >= 0.49635) {
                        var15 = params[240];
                    } else {
                        var15 = params[241];
                    }
                } else {
                    if (input[9] >= 0.013) {
                        var15 = params[242];
                    } else {
                        var15 = params[243];
                    }
                }
            } else {
                if (input[0] >= 64.09) {
                    if (input[4] >= 0.7283) {
                        var15 = params[244];
                    } else {
                        var15 = params[245];
                    }
                } else {
                    if (input[0] >= 37.135002) {
                        var15 = params[246];
                    } else {
                        var15 = params[247];
                    }
                }
            }
        }
        double var16;
        if (input[0] >= 3.3850002) {
            if (input[13] >= 184.5) {
                if (input[15] >= 78.5) {
                    if (input[5] >= 0.00905) {
                        var16 = params[248];
                    } else {
                        var16 = params[249];
                    }
                } else {
                    if (input[5] >= 0.0447) {
                        var16 = params[250];
                    } else {
                        var16 = params[251];
                    }
                }
            } else {
                if (input[13] >= 141.5) {
                    if (input[3] >= 0.67955) {
                        var16 = params[252];
                    } else {
                        var16 = params[253];
                    }
                } else {
                    if (input[15] >= 152.5) {
                        var16 = params[254];
                    } else {
                        var16 = params[255];
                    }
                }
            }
        } else {
            var16 = params[256];
        }
        double var17;
        if (input[11] >= 3.5) {
            if (input[0] >= 110.165) {
                if (input[4] >= 5.25035) {
                    if (input[15] >= 5.5) {
                        var17 = params[257];
                    } else {
                        var17 = params[258];
                    }
                } else {
                    if (input[6] >= 0.35165) {
                        var17 = params[259];
                    } else {
                        var17 = params[260];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[11] >= 24.5) {
                        var17 = params[261];
                    } else {
                        var17 = params[262];
                    }
                } else {
                    if (input[7] >= 0.1167) {
                        var17 = params[263];
                    } else {
                        var17 = params[264];
                    }
                }
            }
        } else {
            if (input[12] >= 1.5) {
                if (input[13] >= 15.5) {
                    if (input[13] >= 113.5) {
                        var17 = params[265];
                    } else {
                        var17 = params[266];
                    }
                } else {
                    if (input[8] >= 0.0063) {
                        var17 = params[267];
                    } else {
                        var17 = params[268];
                    }
                }
            } else {
                if (input[0] >= 44.845) {
                    if (input[8] >= 0.11215) {
                        var17 = params[269];
                    } else {
                        var17 = params[270];
                    }
                } else {
                    if (input[6] >= 0.4728) {
                        var17 = params[271];
                    } else {
                        var17 = params[272];
                    }
                }
            }
        }
        double var18;
        if (input[6] >= 0.030650001) {
            if (input[11] >= 14.5) {
                if (input[0] >= 9.889999) {
                    if (input[0] >= 33.965) {
                        var18 = params[273];
                    } else {
                        var18 = params[274];
                    }
                } else {
                    if (input[0] >= 6.92) {
                        var18 = params[275];
                    } else {
                        var18 = params[276];
                    }
                }
            } else {
                if (input[14] >= 0.5) {
                    if (input[0] >= 569.66504) {
                        var18 = params[277];
                    } else {
                        var18 = params[278];
                    }
                } else {
                    if (input[7] >= 0.12415) {
                        var18 = params[279];
                    } else {
                        var18 = params[280];
                    }
                }
            }
        } else {
            if (input[8] >= 0.40885) {
                if (input[13] >= 3.5) {
                    if (input[0] >= 408.685) {
                        var18 = params[281];
                    } else {
                        var18 = params[282];
                    }
                } else {
                    if (input[6] >= 0.00575) {
                        var18 = params[283];
                    } else {
                        var18 = params[284];
                    }
                }
            } else {
                if (input[13] >= 30.5) {
                    if (input[12] >= 1.5) {
                        var18 = params[285];
                    } else {
                        var18 = params[286];
                    }
                } else {
                    if (input[8] >= 0.03935) {
                        var18 = params[287];
                    } else {
                        var18 = params[288];
                    }
                }
            }
        }
        double var19;
        if (input[17] >= 0.5) {
            if (input[13] >= 5.5) {
                if (input[6] >= 0.1978) {
                    if (input[3] >= 0.205) {
                        var19 = params[289];
                    } else {
                        var19 = params[290];
                    }
                } else {
                    if (input[11] >= 37.5) {
                        var19 = params[291];
                    } else {
                        var19 = params[292];
                    }
                }
            } else {
                if (input[0] >= 54.625) {
                    if (input[13] >= 1.5) {
                        var19 = params[293];
                    } else {
                        var19 = params[294];
                    }
                } else {
                    var19 = params[295];
                }
            }
        } else {
            if (input[25] >= 0.5) {
                if (input[28] >= 0.5) {
                    if (input[4] >= 4.98315) {
                        var19 = params[296];
                    } else {
                        var19 = params[297];
                    }
                } else {
                    if (input[9] >= 0.058849998) {
                        var19 = params[298];
                    } else {
                        var19 = params[299];
                    }
                }
            } else {
                if (input[1] >= 0.5) {
                    if (input[0] >= 29.865002) {
                        var19 = params[300];
                    } else {
                        var19 = params[301];
                    }
                } else {
                    if (input[5] >= 0.07175) {
                        var19 = params[302];
                    } else {
                        var19 = params[303];
                    }
                }
            }
        }
        double var20;
        if (input[12] >= 2.5) {
            if (input[1] >= 1.5) {
                if (input[3] >= 0.23215) {
                    if (input[7] >= 0.01875) {
                        var20 = params[304];
                    } else {
                        var20 = params[305];
                    }
                } else {
                    if (input[12] >= 10.5) {
                        var20 = params[306];
                    } else {
                        var20 = params[307];
                    }
                }
            } else {
                if (input[15] >= 167.0) {
                    var20 = params[308];
                } else {
                    if (input[8] >= 0.06035) {
                        var20 = params[309];
                    } else {
                        var20 = params[310];
                    }
                }
            }
        } else {
            if (input[8] >= 1.3928499) {
                if (input[4] >= 2.2185001) {
                    var20 = params[311];
                } else {
                    var20 = params[312];
                }
            } else {
                if (input[14] >= 3.5) {
                    if (input[12] >= 0.5) {
                        var20 = params[313];
                    } else {
                        var20 = params[314];
                    }
                } else {
                    if (input[8] >= 1.3840001) {
                        var20 = params[315];
                    } else {
                        var20 = params[316];
                    }
                }
            }
        }
        double var21;
        if (input[13] >= 275.5) {
            if (input[12] >= 0.5) {
                var21 = params[317];
            } else {
                var21 = params[318];
            }
        } else {
            if (input[4] >= 4.40685) {
                if (input[3] >= 0.39165002) {
                    if (input[3] >= 0.4093) {
                        var21 = params[319];
                    } else {
                        var21 = params[320];
                    }
                } else {
                    if (input[0] >= 343.935) {
                        var21 = params[321];
                    } else {
                        var21 = params[322];
                    }
                }
            } else {
                if (input[22] >= 0.5) {
                    if (input[3] >= 0.7271) {
                        var21 = params[323];
                    } else {
                        var21 = params[324];
                    }
                } else {
                    if (input[4] >= 3.7618499) {
                        var21 = params[325];
                    } else {
                        var21 = params[326];
                    }
                }
            }
        }
        double var22;
        if (input[12] >= 1.5) {
            if (input[16] >= 0.5) {
                if (input[10] >= 0.5) {
                    if (input[10] >= 1.5) {
                        var22 = params[327];
                    } else {
                        var22 = params[328];
                    }
                } else {
                    if (input[0] >= 263.66498) {
                        var22 = params[329];
                    } else {
                        var22 = params[330];
                    }
                }
            } else {
                if (input[11] >= 27.5) {
                    if (input[1] >= 5.5) {
                        var22 = params[331];
                    } else {
                        var22 = params[332];
                    }
                } else {
                    if (input[3] >= 0.58575) {
                        var22 = params[333];
                    } else {
                        var22 = params[334];
                    }
                }
            }
        } else {
            if (input[6] >= 0.3879) {
                if (input[1] >= 3.5) {
                    if (input[7] >= 0.0075000003) {
                        var22 = params[335];
                    } else {
                        var22 = params[336];
                    }
                } else {
                    if (input[15] >= 0.5) {
                        var22 = params[337];
                    } else {
                        var22 = params[338];
                    }
                }
            } else {
                if (input[25] >= 0.5) {
                    if (input[11] >= 168.0) {
                        var22 = params[339];
                    } else {
                        var22 = params[340];
                    }
                } else {
                    if (input[8] >= 0.64094996) {
                        var22 = params[341];
                    } else {
                        var22 = params[342];
                    }
                }
            }
        }
        double var23;
        if (input[0] >= 13.155) {
            if (input[0] >= 13.195) {
                if (input[14] >= 2.5) {
                    if (input[0] >= 536.13) {
                        var23 = params[343];
                    } else {
                        var23 = params[344];
                    }
                } else {
                    if (input[11] >= 14.5) {
                        var23 = params[345];
                    } else {
                        var23 = params[346];
                    }
                }
            } else {
                var23 = params[347];
            }
        } else {
            if (input[0] >= 12.655) {
                if (input[4] >= 2.5351) {
                    if (input[8] >= 0.07965) {
                        var23 = params[348];
                    } else {
                        var23 = params[349];
                    }
                } else {
                    if (input[4] >= 2.2596002) {
                        var23 = params[350];
                    } else {
                        var23 = params[351];
                    }
                }
            } else {
                if (input[6] >= 0.02275) {
                    if (input[4] >= 2.93535) {
                        var23 = params[352];
                    } else {
                        var23 = params[353];
                    }
                } else {
                    if (input[0] >= 11.34) {
                        var23 = params[354];
                    } else {
                        var23 = params[355];
                    }
                }
            }
        }
        double var24;
        if (input[0] >= 18.154999) {
            if (input[0] >= 42.190002) {
                if (input[0] >= 57.675) {
                    if (input[4] >= 1.1393) {
                        var24 = params[356];
                    } else {
                        var24 = params[357];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var24 = params[358];
                    } else {
                        var24 = params[359];
                    }
                }
            } else {
                if (input[1] >= 4.5) {
                    var24 = params[360];
                } else {
                    if (input[9] >= 0.00665) {
                        var24 = params[361];
                    } else {
                        var24 = params[362];
                    }
                }
            }
        } else {
            if (input[0] >= 17.625) {
                if (input[4] >= 4.6472) {
                    if (input[3] >= 0.5431) {
                        var24 = params[363];
                    } else {
                        var24 = params[364];
                    }
                } else {
                    if (input[3] >= 0.23840001) {
                        var24 = params[365];
                    } else {
                        var24 = params[366];
                    }
                }
            } else {
                if (input[0] >= 16.235) {
                    if (input[3] >= 0.61095) {
                        var24 = params[367];
                    } else {
                        var24 = params[368];
                    }
                } else {
                    if (input[4] >= 5.5356503) {
                        var24 = params[369];
                    } else {
                        var24 = params[370];
                    }
                }
            }
        }
        double var25;
        if (input[7] >= 0.5217) {
            var25 = params[371];
        } else {
            if (input[7] >= 0.34600002) {
                if (input[27] >= 0.5) {
                    if (input[13] >= 51.5) {
                        var25 = params[372];
                    } else {
                        var25 = params[373];
                    }
                } else {
                    if (input[6] >= 0.18535) {
                        var25 = params[374];
                    } else {
                        var25 = params[375];
                    }
                }
            } else {
                if (input[7] >= 0.24035001) {
                    if (input[3] >= 0.13755) {
                        var25 = params[376];
                    } else {
                        var25 = params[377];
                    }
                } else {
                    if (input[7] >= 0.22415) {
                        var25 = params[378];
                    } else {
                        var25 = params[379];
                    }
                }
            }
        }
        double var26;
        if (input[10] >= 1.5) {
            if (input[0] >= 32.684998) {
                if (input[26] >= 0.5) {
                    if (input[14] >= 1.0) {
                        var26 = params[380];
                    } else {
                        var26 = params[381];
                    }
                } else {
                    if (input[0] >= 728.975) {
                        var26 = params[382];
                    } else {
                        var26 = params[383];
                    }
                }
            } else {
                var26 = params[384];
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[14] >= 0.5) {
                    if (input[8] >= 0.018350001) {
                        var26 = params[385];
                    } else {
                        var26 = params[386];
                    }
                } else {
                    if (input[0] >= 137.01) {
                        var26 = params[387];
                    } else {
                        var26 = params[388];
                    }
                }
            } else {
                if (input[2] >= 0.5) {
                    if (input[13] >= 72.5) {
                        var26 = params[389];
                    } else {
                        var26 = params[390];
                    }
                } else {
                    if (input[3] >= 0.36255002) {
                        var26 = params[391];
                    } else {
                        var26 = params[392];
                    }
                }
            }
        }
        double var27;
        if (input[7] >= 0.00084999995) {
            if (input[11] >= 158.5) {
                if (input[6] >= 0.113299996) {
                    var27 = params[393];
                } else {
                    if (input[6] >= 0.10805) {
                        var27 = params[394];
                    } else {
                        var27 = params[395];
                    }
                }
            } else {
                if (input[1] >= 3.5) {
                    if (input[12] >= 3.5) {
                        var27 = params[396];
                    } else {
                        var27 = params[397];
                    }
                } else {
                    if (input[4] >= 4.9486) {
                        var27 = params[398];
                    } else {
                        var27 = params[399];
                    }
                }
            }
        } else {
            if (input[5] >= 0.03585) {
                if (input[5] >= 0.04745) {
                    if (input[0] >= 53.095) {
                        var27 = params[400];
                    } else {
                        var27 = params[401];
                    }
                } else {
                    if (input[10] >= 1.5) {
                        var27 = params[402];
                    } else {
                        var27 = params[403];
                    }
                }
            } else {
                if (input[5] >= 0.02175) {
                    if (input[4] >= 4.9702) {
                        var27 = params[404];
                    } else {
                        var27 = params[405];
                    }
                } else {
                    if (input[6] >= 0.03645) {
                        var27 = params[406];
                    } else {
                        var27 = params[407];
                    }
                }
            }
        }
        double var28;
        if (input[15] >= 111.5) {
            if (input[13] >= 102.5) {
                if (input[7] >= 0.117400005) {
                    if (input[0] >= 162.295) {
                        var28 = params[408];
                    } else {
                        var28 = params[409];
                    }
                } else {
                    if (input[4] >= 3.41745) {
                        var28 = params[410];
                    } else {
                        var28 = params[411];
                    }
                }
            } else {
                if (input[3] >= 0.64835) {
                    if (input[11] >= 88.5) {
                        var28 = params[412];
                    } else {
                        var28 = params[413];
                    }
                } else {
                    if (input[12] >= 1.5) {
                        var28 = params[414];
                    } else {
                        var28 = params[415];
                    }
                }
            }
        } else {
            if (input[15] >= 101.5) {
                if (input[8] >= 1.0301001) {
                    if (input[1] >= 1.0) {
                        var28 = params[416];
                    } else {
                        var28 = params[417];
                    }
                } else {
                    if (input[0] >= 192.46) {
                        var28 = params[418];
                    } else {
                        var28 = params[419];
                    }
                }
            } else {
                if (input[11] >= 4.5) {
                    if (input[11] >= 9.5) {
                        var28 = params[420];
                    } else {
                        var28 = params[421];
                    }
                } else {
                    if (input[12] >= 1.5) {
                        var28 = params[422];
                    } else {
                        var28 = params[423];
                    }
                }
            }
        }
        double var29;
        if (input[9] >= 0.0015499999) {
            if (input[8] >= 0.20725) {
                if (input[8] >= 0.2474) {
                    if (input[4] >= 4.0284) {
                        var29 = params[424];
                    } else {
                        var29 = params[425];
                    }
                } else {
                    if (input[12] >= 1.5) {
                        var29 = params[426];
                    } else {
                        var29 = params[427];
                    }
                }
            } else {
                if (input[8] >= 0.00655) {
                    if (input[6] >= 0.05915) {
                        var29 = params[428];
                    } else {
                        var29 = params[429];
                    }
                } else {
                    if (input[13] >= 4.5) {
                        var29 = params[430];
                    } else {
                        var29 = params[431];
                    }
                }
            }
        } else {
            if (input[8] >= 0.08125) {
                if (input[3] >= 0.39425) {
                    if (input[6] >= 0.00845) {
                        var29 = params[432];
                    } else {
                        var29 = params[433];
                    }
                } else {
                    if (input[3] >= 0.39405) {
                        var29 = params[434];
                    } else {
                        var29 = params[435];
                    }
                }
            } else {
                if (input[8] >= 0.06565) {
                    if (input[5] >= 0.0725) {
                        var29 = params[436];
                    } else {
                        var29 = params[437];
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var29 = params[438];
                    } else {
                        var29 = params[439];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
