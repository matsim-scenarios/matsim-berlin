package org.matsim.prepare.facilities;
import org.matsim.application.prepare.Predictor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
    
/**
* Generated model, do not modify.
* Model: XGBRegressor(alpha=0.38676411361213503, base_score=0.5, booster='gbtree',
             callbacks=None, colsample_bylevel=1, colsample_bynode=0.9,
             colsample_bytree=0.9, early_stopping_rounds=None,
             enable_categorical=False, eta=0.5130365792723001,
             eval_metric='mae', feature_types=None, gamma=0.012943980679692784,
             gpu_id=-1, grow_policy='depthwise', importance_type=None,
             interaction_constraints='', lambda=0.669064597858222,
             learning_rate=0.513036609, max_bin=256, max_cat_threshold=64,
             max_cat_to_onehot=4, max_delta_step=0, max_depth=4, max_leaves=0,
             min_child_weight=10, missing=nan, monotone_constraints='()',
             n_estimators=30, n_jobs=0, ...)
* Error: 0.038664
*/
public final class FacilityAttractionModelOther implements Predictor {
    
    public static FacilityAttractionModelOther INSTANCE = new FacilityAttractionModelOther();
    public static final double[] DEFAULT_PARAMS = {-0.19120647, -0.21544395, -0.24561352, -0.22795276, -0.21394487, -0.20291902, -0.15955746, -0.18970205, -0.19473802, -0.23693506, -0.20146284, -0.2303382, -0.21616362, -0.20097785, -0.16717474, -0.03329817, -0.09521938, -0.10616502, -0.09562808, -0.07983333, -0.10209244, -0.08706439, -0.10831736, -0.07489157, -0.1257557, -0.11461685, -0.103787825, -0.086615644, -0.019538827, 0.002300759, -0.046563406, -0.034066223, -0.04225948, -0.05574401, -0.03085557, -0.04824915, -0.048481815, -0.06389991, -0.053476326, -0.056743115, -0.03770388, -0.051023822, -0.014120954, -0.030703563, 0.009647463, -0.011717529, -0.022449084, -0.008964606, 0.0, -0.023808807, -0.018975427, 0.0044123204, -0.02602822, -0.016385198, -0.030171694, -0.026699351, 0.0, 0.015140042, 0.01509542, -0.0058580204, -0.01636645, 0.0052654496, 0.0034550265, -0.0084092, 0.0032706636, -0.02611719, -0.0076765716, -0.014188832, -0.011400385, 0.0053586247, -0.009150267, 0.018767033, 0.0, -0.0018990446, 0.001958792, -0.017443, 0.0005824891, 0.016390784, 0.0018297584, -0.008568364, 0.044628333, -0.006046177, -0.01835564, 0.0033763836, 0.015462291, -0.0058534844, 0.002892445, 0.017501997, -0.0005166871, -0.013446667, 0.0012932719, -0.0025227652, 0.0015414358, -0.011770653, -0.0038251996, 7.2739545e-05, 0.012047838, 0.0032230231, -0.009509736, 0.0016221643, 0.032248944, 0.008109049, -0.0054452335, 0.00811509, 0.014862692, 0.012176751, 0.00019675975, 0.02114268, -0.001677216, 0.0, -0.009217657, -0.0013252627, -0.0007103103, 0.010789065, -0.0046287123, 0.010294565, 0.0017368435, -0.0017939341, 0.0025711048, -0.0006504898, 0.0, -0.013186111, 0.0, 0.028552113, 0.0, 0.0, 0.013852832, 0.0057132845, -0.0055112788, -0.0020431376, -0.00022372205, -0.007749204, 0.004826835, 0.015587179, -0.0015933946, 0.0058425143, -0.007804661, 0.0, 0.0004416953, 0.010754214, -0.0059289443, -0.008716668, -0.0023599828, 0.017339407, -0.00031251466, 0.0, 0.009203198, 0.0030588997, -0.0032642898, -0.0009475371, -0.007607703, -0.0074665886, 6.076673e-05, 0.02441646, 0.0040437514, -0.016884638, 0.0, -0.0018241111, 0.0030271579, 0.0, 0.01416777, 0.0, -0.004867998, -0.0037333164, -2.0667332e-05, 0.0, 0.013903742, -0.007628961, 4.3675254e-06, 0.0, 0.013018352, -0.0012610003, 0.008476731, -0.00022219155, 0.007777653, -0.0038773527, -0.0019484207, 0.003000052, -0.0046468135, -0.00013903232, 0.0025403441, 0.009887723, -0.00040583266, 0.0018972411, 0.0040823217, -0.003190092, 0.00091765885, -0.0005568173, 0.0, 0.01354988, -0.01120661, 0.0, 0.000702149, 0.008325187, 0.0003086261, -0.01177865, 0.00032562646, -0.003259402, 0.0046615573, -0.007214233, -0.008795629, 0.0032250367, -0.0027392802, 0.0054725115, 0.0022755493, 0.0, 0.0072411504, -0.0026634475, 0.012566408, 0.0, 0.0, -0.013973392, 0.0, 0.0004584588, -0.0031417334, 0.010836599, -0.0016273378, 0.0011950341, 0.010181999, 0.0037211801, 3.59309e-05, -0.001538061, 0.016332423, 0.013136123, -0.00010379798, -0.0064572925, 0.00038133477, 0.003864874, -0.00068554503, 0.005422235, -0.006042015, -0.010478834, 0.009136493, 0.004153007, 9.9093246e-05, 0.0, 0.008636402, -0.005528234, -0.00032573545, 0.0017203045, -0.00016688212, 0.023454975, 0.0, -0.0024592762, 0.008242776, -0.011009559, 0.01862933, 0.00081926736, 0.005791739, -0.0014741069, 0.00059491256, -0.0053027584, 0.012380845, 0.0006966785, -0.002206946, 0.008710933, 0.008599077, -0.00303403, 0.0019886778, 0.013068698, -0.0020459918, 0.004103897, -0.0015655279, 6.417063e-05, 0.010153224, 0.0, 0.016124865, 0.0010549584, -0.0026950429, 0.009305348, 1.1777206e-05, -0.008698372, 0.003396451, 0.0063213897, -0.00037645758, 0.0, -0.00831018, 0.0012522287, -0.009953721, -0.008453991, 0.014524538, 0.0, 0.002683152, 8.010548e-05, -0.0053177453, 0.014899546, -0.00054772786, -0.001551509, 0.007339037, 0.014715689, 0.0, -0.00034507358, -0.0029873943, 0.0010542416, -0.0007416305, 0.0010108859, 0.011877695, -0.0016241109, 6.8110705e-05, 0.00027952218, 0.016313732, -0.0021017403, -0.00964256, -0.0009030501, 0.0070898077, 0.00032387377, 0.0, 0.014621791, -8.331219e-05, -0.014562358, 0.0, 0.008306464, -0.00015869575, 0.013947732, 0.0, -0.0044524516, 0.004801721, 0.0, 0.0, -0.0069771605, -0.0005317706, 0.022706605, -0.0012514418, 0.0076321224, 0.000842854, -0.00068890676, -1.3764001e-05, 0.0, 0.0, -0.019687261, 0.006598481, -0.00064870616, 0.018821958, 0.0015755226, 0.0, -0.018771281, 0.01636683, 0.0, -0.0020345727, 0.0017587318, 0.003313411, -7.640684e-05, -0.0006133416, -0.00947991, 0.0020459038, -0.005799137, -0.001293848, 4.2439693e-05, 0.0008543773, -0.008529851, 0.003888652, -0.0006709776, -0.0049108244, 0.003398581, 0.0, 0.0, -0.014460024, 0.0, 0.008164088, 0.0, -0.0023600166, 7.3098e-05, 0.00319647, 2.5677393e-05, -0.0018085259, 0.0026671304, -0.008660301, -0.0022033867, -0.0048490968, 0.0024857325, -0.00043020103, 0.00016602647, 0.016929602, 0.0069206995, -0.00010019835, 0.010143461, -0.005342358, 0.00019916303, 0.004376315, -0.0073216553, 0.0054006604, -0.0028898634, 0.0011992506, 0.0011827721, -0.0072587286, -0.0011454263, 0.0027247802, -0.0014655103, -0.0018519809, -2.6215132e-06};

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
        if (input[15] >= 2.5) {
            if (input[0] >= 330.34) {
                if (input[29] >= 0.5) {
                    if (input[12] >= 3.5) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[0] >= 2513.9849) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[0] >= 111.78) {
                    if (input[11] >= 23.5) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    if (input[14] >= 0.5) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                }
            }
        } else {
            if (input[0] >= 36.905) {
                if (input[0] >= 86.055) {
                    if (input[30] >= 0.5) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                }
            } else {
                if (input[0] >= 12.645) {
                    if (input[0] >= 25.529999) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                } else {
                    var0 = params[14];
                }
            }
        }
        double var1;
        if (input[14] >= 0.5) {
            if (input[11] >= 22.5) {
                if (input[7] >= 0.01095) {
                    if (input[11] >= 154.5) {
                        var1 = params[15];
                    } else {
                        var1 = params[16];
                    }
                } else {
                    var1 = params[17];
                }
            } else {
                if (input[0] >= 478.33002) {
                    var1 = params[18];
                } else {
                    var1 = params[19];
                }
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[30] >= 0.5) {
                    if (input[11] >= 27.5) {
                        var1 = params[20];
                    } else {
                        var1 = params[21];
                    }
                } else {
                    if (input[0] >= 44.235) {
                        var1 = params[22];
                    } else {
                        var1 = params[23];
                    }
                }
            } else {
                if (input[0] >= 48.105) {
                    if (input[10] >= 0.5) {
                        var1 = params[24];
                    } else {
                        var1 = params[25];
                    }
                } else {
                    if (input[0] >= 16.215) {
                        var1 = params[26];
                    } else {
                        var1 = params[27];
                    }
                }
            }
        }
        double var2;
        if (input[13] >= 25.5) {
            if (input[26] >= 0.5) {
                if (input[15] >= 65.5) {
                    if (input[0] >= 260.145) {
                        var2 = params[28];
                    } else {
                        var2 = params[29];
                    }
                } else {
                    if (input[0] >= 789.17004) {
                        var2 = params[30];
                    } else {
                        var2 = params[31];
                    }
                }
            } else {
                if (input[0] >= 417.155) {
                    if (input[1] >= 5.5) {
                        var2 = params[32];
                    } else {
                        var2 = params[33];
                    }
                } else {
                    if (input[12] >= 2.5) {
                        var2 = params[34];
                    } else {
                        var2 = params[35];
                    }
                }
            }
        } else {
            if (input[0] >= 60.515) {
                if (input[0] >= 1811.41) {
                    if (input[1] >= 4.5) {
                        var2 = params[36];
                    } else {
                        var2 = params[37];
                    }
                } else {
                    if (input[1] >= 2.5) {
                        var2 = params[38];
                    } else {
                        var2 = params[39];
                    }
                }
            } else {
                if (input[0] >= 7.4399996) {
                    if (input[6] >= 0.06815) {
                        var2 = params[40];
                    } else {
                        var2 = params[41];
                    }
                } else {
                    var2 = params[42];
                }
            }
        }
        double var3;
        if (input[25] >= 0.5) {
            if (input[0] >= 5461.805) {
                var3 = params[43];
            } else {
                if (input[5] >= 0.02135) {
                    if (input[13] >= 60.5) {
                        var3 = params[44];
                    } else {
                        var3 = params[45];
                    }
                } else {
                    if (input[1] >= 4.5) {
                        var3 = params[46];
                    } else {
                        var3 = params[47];
                    }
                }
            }
        } else {
            if (input[26] >= 0.5) {
                if (input[0] >= 302.535) {
                    if (input[13] >= 158.0) {
                        var3 = params[48];
                    } else {
                        var3 = params[49];
                    }
                } else {
                    if (input[3] >= 0.31655002) {
                        var3 = params[50];
                    } else {
                        var3 = params[51];
                    }
                }
            } else {
                if (input[18] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var3 = params[52];
                    } else {
                        var3 = params[53];
                    }
                } else {
                    if (input[0] >= 411.365) {
                        var3 = params[54];
                    } else {
                        var3 = params[55];
                    }
                }
            }
        }
        double var4;
        if (input[12] >= 1.5) {
            if (input[7] >= 0.01055) {
                if (input[24] >= 0.5) {
                    if (input[11] >= 23.5) {
                        var4 = params[56];
                    } else {
                        var4 = params[57];
                    }
                } else {
                    if (input[14] >= 3.5) {
                        var4 = params[58];
                    } else {
                        var4 = params[59];
                    }
                }
            } else {
                if (input[11] >= 13.5) {
                    if (input[4] >= 2.46315) {
                        var4 = params[60];
                    } else {
                        var4 = params[61];
                    }
                } else {
                    if (input[15] >= 9.5) {
                        var4 = params[62];
                    } else {
                        var4 = params[63];
                    }
                }
            }
        } else {
            if (input[1] >= 10.5) {
                var4 = params[64];
            } else {
                if (input[26] >= 0.5) {
                    if (input[1] >= 0.5) {
                        var4 = params[65];
                    } else {
                        var4 = params[66];
                    }
                } else {
                    if (input[0] >= 93.5) {
                        var4 = params[67];
                    } else {
                        var4 = params[68];
                    }
                }
            }
        }
        double var5;
        if (input[8] >= 1.11185) {
            if (input[0] >= 1536.035) {
                if (input[15] >= 54.5) {
                    var5 = params[69];
                } else {
                    var5 = params[70];
                }
            } else {
                if (input[31] >= 0.5) {
                    if (input[0] >= 216.215) {
                        var5 = params[71];
                    } else {
                        var5 = params[72];
                    }
                } else {
                    var5 = params[73];
                }
            }
        } else {
            if (input[5] >= 0.10715) {
                if (input[9] >= 0.0009) {
                    if (input[3] >= 0.4339) {
                        var5 = params[74];
                    } else {
                        var5 = params[75];
                    }
                } else {
                    if (input[4] >= 3.69495) {
                        var5 = params[76];
                    } else {
                        var5 = params[77];
                    }
                }
            } else {
                if (input[0] >= 485.51) {
                    if (input[1] >= 7.5) {
                        var5 = params[78];
                    } else {
                        var5 = params[79];
                    }
                } else {
                    if (input[2] >= 0.5) {
                        var5 = params[80];
                    } else {
                        var5 = params[81];
                    }
                }
            }
        }
        double var6;
        if (input[5] >= 0.00845) {
            if (input[30] >= 0.5) {
                if (input[0] >= 6125.64) {
                    var6 = params[82];
                } else {
                    if (input[28] >= 0.5) {
                        var6 = params[83];
                    } else {
                        var6 = params[84];
                    }
                }
            } else {
                if (input[0] >= 1107.935) {
                    if (input[8] >= 0.07) {
                        var6 = params[85];
                    } else {
                        var6 = params[86];
                    }
                } else {
                    if (input[5] >= 0.13865) {
                        var6 = params[87];
                    } else {
                        var6 = params[88];
                    }
                }
            }
        } else {
            if (input[1] >= 3.5) {
                if (input[4] >= 4.2152004) {
                    if (input[1] >= 4.5) {
                        var6 = params[89];
                    } else {
                        var6 = params[90];
                    }
                } else {
                    if (input[0] >= 243.76001) {
                        var6 = params[91];
                    } else {
                        var6 = params[92];
                    }
                }
            } else {
                if (input[0] >= 18.994999) {
                    if (input[20] >= 0.5) {
                        var6 = params[93];
                    } else {
                        var6 = params[94];
                    }
                } else {
                    if (input[0] >= 8.95) {
                        var6 = params[95];
                    } else {
                        var6 = params[96];
                    }
                }
            }
        }
        double var7;
        if (input[14] >= 2.5) {
            if (input[0] >= 1070.74) {
                if (input[13] >= 49.0) {
                    var7 = params[97];
                } else {
                    var7 = params[98];
                }
            } else {
                if (input[6] >= 0.1343) {
                    var7 = params[99];
                } else {
                    if (input[5] >= 0.016150001) {
                        var7 = params[100];
                    } else {
                        var7 = params[101];
                    }
                }
            }
        } else {
            if (input[11] >= 36.5) {
                if (input[0] >= 76.39) {
                    if (input[3] >= 0.273) {
                        var7 = params[102];
                    } else {
                        var7 = params[103];
                    }
                } else {
                    var7 = params[104];
                }
            } else {
                if (input[13] >= 8.5) {
                    if (input[9] >= 0.1014) {
                        var7 = params[105];
                    } else {
                        var7 = params[106];
                    }
                } else {
                    if (input[2] >= 0.5) {
                        var7 = params[107];
                    } else {
                        var7 = params[108];
                    }
                }
            }
        }
        double var8;
        if (input[20] >= 0.5) {
            if (input[11] >= 10.5) {
                if (input[11] >= 79.5) {
                    var8 = params[109];
                } else {
                    var8 = params[110];
                }
            } else {
                var8 = params[111];
            }
        } else {
            if (input[24] >= 0.5) {
                if (input[11] >= 19.5) {
                    if (input[4] >= 2.6859) {
                        var8 = params[112];
                    } else {
                        var8 = params[113];
                    }
                } else {
                    if (input[0] >= 3652.02) {
                        var8 = params[114];
                    } else {
                        var8 = params[115];
                    }
                }
            } else {
                if (input[0] >= 269.64502) {
                    if (input[1] >= 5.5) {
                        var8 = params[116];
                    } else {
                        var8 = params[117];
                    }
                } else {
                    if (input[6] >= 0.03825) {
                        var8 = params[118];
                    } else {
                        var8 = params[119];
                    }
                }
            }
        }
        double var9;
        if (input[27] >= 0.5) {
            if (input[1] >= 1.5) {
                var9 = params[120];
            } else {
                if (input[3] >= 0.13945001) {
                    var9 = params[121];
                } else {
                    var9 = params[122];
                }
            }
        } else {
            if (input[4] >= 5.2237997) {
                if (input[13] >= 30.5) {
                    if (input[3] >= 0.63005) {
                        var9 = params[123];
                    } else {
                        var9 = params[124];
                    }
                } else {
                    if (input[8] >= 0.04295) {
                        var9 = params[125];
                    } else {
                        var9 = params[126];
                    }
                }
            } else {
                if (input[13] >= 159.5) {
                    if (input[8] >= 0.14815) {
                        var9 = params[127];
                    } else {
                        var9 = params[128];
                    }
                } else {
                    if (input[11] >= 21.5) {
                        var9 = params[129];
                    } else {
                        var9 = params[130];
                    }
                }
            }
        }
        double var10;
        if (input[30] >= 0.5) {
            if (input[7] >= 0.00715) {
                if (input[0] >= 1975.23) {
                    var10 = params[131];
                } else {
                    if (input[28] >= 0.5) {
                        var10 = params[132];
                    } else {
                        var10 = params[133];
                    }
                }
            } else {
                if (input[5] >= 0.0037500001) {
                    if (input[7] >= 0.00145) {
                        var10 = params[134];
                    } else {
                        var10 = params[135];
                    }
                } else {
                    if (input[6] >= 0.06795) {
                        var10 = params[136];
                    } else {
                        var10 = params[137];
                    }
                }
            }
        } else {
            if (input[25] >= 0.5) {
                if (input[15] >= 0.5) {
                    if (input[15] >= 35.5) {
                        var10 = params[138];
                    } else {
                        var10 = params[139];
                    }
                } else {
                    var10 = params[140];
                }
            } else {
                if (input[6] >= 0.27394998) {
                    if (input[9] >= 0.0001) {
                        var10 = params[141];
                    } else {
                        var10 = params[142];
                    }
                } else {
                    if (input[15] >= 166.5) {
                        var10 = params[143];
                    } else {
                        var10 = params[144];
                    }
                }
            }
        }
        double var11;
        if (input[0] >= 15.005) {
            if (input[24] >= 0.5) {
                if (input[12] >= 0.5) {
                    if (input[0] >= 631.255) {
                        var11 = params[145];
                    } else {
                        var11 = params[146];
                    }
                } else {
                    if (input[7] >= 0.0052500004) {
                        var11 = params[147];
                    } else {
                        var11 = params[148];
                    }
                }
            } else {
                if (input[4] >= 3.30145) {
                    if (input[0] >= 58.43) {
                        var11 = params[149];
                    } else {
                        var11 = params[150];
                    }
                } else {
                    if (input[9] >= 0.01305) {
                        var11 = params[151];
                    } else {
                        var11 = params[152];
                    }
                }
            }
        } else {
            if (input[31] >= 0.5) {
                var11 = params[153];
            } else {
                var11 = params[154];
            }
        }
        double var12;
        if (input[7] >= 0.456) {
            if (input[6] >= 0.1742) {
                var12 = params[155];
            } else {
                var12 = params[156];
            }
        } else {
            if (input[8] >= 0.60735) {
                if (input[0] >= 151.295) {
                    if (input[4] >= 3.1336) {
                        var12 = params[157];
                    } else {
                        var12 = params[158];
                    }
                } else {
                    if (input[8] >= 0.95914996) {
                        var12 = params[159];
                    } else {
                        var12 = params[160];
                    }
                }
            } else {
                if (input[22] >= 0.5) {
                    if (input[8] >= 0.46235) {
                        var12 = params[161];
                    } else {
                        var12 = params[162];
                    }
                } else {
                    if (input[17] >= 0.5) {
                        var12 = params[163];
                    } else {
                        var12 = params[164];
                    }
                }
            }
        }
        double var13;
        if (input[5] >= 0.08505) {
            if (input[7] >= 0.055200003) {
                if (input[8] >= 1.0074) {
                    if (input[0] >= 1864.5549) {
                        var13 = params[165];
                    } else {
                        var13 = params[166];
                    }
                } else {
                    if (input[8] >= 0.4853) {
                        var13 = params[167];
                    } else {
                        var13 = params[168];
                    }
                }
            } else {
                if (input[7] >= 0.00785) {
                    if (input[6] >= 0.41545) {
                        var13 = params[169];
                    } else {
                        var13 = params[170];
                    }
                } else {
                    var13 = params[171];
                }
            }
        } else {
            if (input[3] >= 0.63785) {
                if (input[31] >= 0.5) {
                    if (input[5] >= 0.00055) {
                        var13 = params[172];
                    } else {
                        var13 = params[173];
                    }
                } else {
                    if (input[5] >= 0.0137) {
                        var13 = params[174];
                    } else {
                        var13 = params[175];
                    }
                }
            } else {
                if (input[1] >= 6.5) {
                    if (input[6] >= 0.1276) {
                        var13 = params[176];
                    } else {
                        var13 = params[177];
                    }
                } else {
                    if (input[5] >= 0.05615) {
                        var13 = params[178];
                    } else {
                        var13 = params[179];
                    }
                }
            }
        }
        double var14;
        if (input[0] >= 74.125) {
            if (input[1] >= 2.5) {
                if (input[7] >= 0.0858) {
                    if (input[4] >= 2.5855) {
                        var14 = params[180];
                    } else {
                        var14 = params[181];
                    }
                } else {
                    if (input[0] >= 246.235) {
                        var14 = params[182];
                    } else {
                        var14 = params[183];
                    }
                }
            } else {
                if (input[7] >= 0.083450004) {
                    if (input[14] >= 0.5) {
                        var14 = params[184];
                    } else {
                        var14 = params[185];
                    }
                } else {
                    if (input[13] >= 3.5) {
                        var14 = params[186];
                    } else {
                        var14 = params[187];
                    }
                }
            }
        } else {
            if (input[15] >= 18.5) {
                if (input[5] >= 0.0514) {
                    var14 = params[188];
                } else {
                    var14 = params[189];
                }
            } else {
                if (input[3] >= 0.51199996) {
                    if (input[4] >= 3.7838001) {
                        var14 = params[190];
                    } else {
                        var14 = params[191];
                    }
                } else {
                    if (input[28] >= 0.5) {
                        var14 = params[192];
                    } else {
                        var14 = params[193];
                    }
                }
            }
        }
        double var15;
        if (input[0] >= 6100.04) {
            if (input[12] >= 0.5) {
                if (input[1] >= 4.5) {
                    var15 = params[194];
                } else {
                    var15 = params[195];
                }
            } else {
                if (input[15] >= 2.5) {
                    var15 = params[196];
                } else {
                    var15 = params[197];
                }
            }
        } else {
            if (input[26] >= 0.5) {
                if (input[28] >= 0.5) {
                    if (input[4] >= 4.91355) {
                        var15 = params[198];
                    } else {
                        var15 = params[199];
                    }
                } else {
                    if (input[1] >= 0.5) {
                        var15 = params[200];
                    } else {
                        var15 = params[201];
                    }
                }
            } else {
                if (input[4] >= 4.38415) {
                    if (input[8] >= 0.058849998) {
                        var15 = params[202];
                    } else {
                        var15 = params[203];
                    }
                } else {
                    if (input[14] >= 1.5) {
                        var15 = params[204];
                    } else {
                        var15 = params[205];
                    }
                }
            }
        }
        double var16;
        if (input[7] >= 0.24335) {
            if (input[7] >= 0.30075002) {
                if (input[8] >= 0.71715) {
                    if (input[1] >= 2.5) {
                        var16 = params[206];
                    } else {
                        var16 = params[207];
                    }
                } else {
                    if (input[8] >= 0.4916) {
                        var16 = params[208];
                    } else {
                        var16 = params[209];
                    }
                }
            } else {
                if (input[3] >= 0.14574999) {
                    if (input[3] >= 0.3179) {
                        var16 = params[210];
                    } else {
                        var16 = params[211];
                    }
                } else {
                    var16 = params[212];
                }
            }
        } else {
            if (input[11] >= 17.5) {
                if (input[3] >= 0.2271) {
                    if (input[13] >= 81.5) {
                        var16 = params[213];
                    } else {
                        var16 = params[214];
                    }
                } else {
                    if (input[13] >= 29.5) {
                        var16 = params[215];
                    } else {
                        var16 = params[216];
                    }
                }
            } else {
                if (input[15] >= 16.5) {
                    if (input[11] >= 13.5) {
                        var16 = params[217];
                    } else {
                        var16 = params[218];
                    }
                } else {
                    if (input[14] >= 0.5) {
                        var16 = params[219];
                    } else {
                        var16 = params[220];
                    }
                }
            }
        }
        double var17;
        if (input[12] >= 2.5) {
            if (input[13] >= 82.5) {
                if (input[15] >= 77.5) {
                    if (input[6] >= 0.04875) {
                        var17 = params[221];
                    } else {
                        var17 = params[222];
                    }
                } else {
                    if (input[11] >= 22.5) {
                        var17 = params[223];
                    } else {
                        var17 = params[224];
                    }
                }
            } else {
                if (input[8] >= 0.39715) {
                    var17 = params[225];
                } else {
                    var17 = params[226];
                }
            }
        } else {
            if (input[11] >= 9.5) {
                if (input[6] >= 0.03545) {
                    if (input[9] >= 0.073300004) {
                        var17 = params[227];
                    } else {
                        var17 = params[228];
                    }
                } else {
                    if (input[8] >= 0.63755) {
                        var17 = params[229];
                    } else {
                        var17 = params[230];
                    }
                }
            } else {
                if (input[15] >= 12.5) {
                    if (input[13] >= 68.5) {
                        var17 = params[231];
                    } else {
                        var17 = params[232];
                    }
                } else {
                    if (input[14] >= 0.5) {
                        var17 = params[233];
                    } else {
                        var17 = params[234];
                    }
                }
            }
        }
        double var18;
        if (input[0] >= 24.735) {
            if (input[14] >= 2.5) {
                if (input[12] >= 0.5) {
                    var18 = params[235];
                } else {
                    var18 = params[236];
                }
            } else {
                if (input[16] >= 0.5) {
                    if (input[0] >= 1482.8049) {
                        var18 = params[237];
                    } else {
                        var18 = params[238];
                    }
                } else {
                    if (input[7] >= 0.01325) {
                        var18 = params[239];
                    } else {
                        var18 = params[240];
                    }
                }
            }
        } else {
            if (input[15] >= 1.5) {
                if (input[0] >= 19.945) {
                    if (input[8] >= 0.0942) {
                        var18 = params[241];
                    } else {
                        var18 = params[242];
                    }
                } else {
                    if (input[0] >= 15.715) {
                        var18 = params[243];
                    } else {
                        var18 = params[244];
                    }
                }
            } else {
                if (input[8] >= 0.3408) {
                    var18 = params[245];
                } else {
                    if (input[8] >= 0.24024999) {
                        var18 = params[246];
                    } else {
                        var18 = params[247];
                    }
                }
            }
        }
        double var19;
        if (input[11] >= 31.5) {
            if (input[28] >= 0.5) {
                if (input[13] >= 78.5) {
                    if (input[11] >= 104.5) {
                        var19 = params[248];
                    } else {
                        var19 = params[249];
                    }
                } else {
                    if (input[9] >= 0.0029000002) {
                        var19 = params[250];
                    } else {
                        var19 = params[251];
                    }
                }
            } else {
                if (input[3] >= 0.4637) {
                    if (input[26] >= 0.5) {
                        var19 = params[252];
                    } else {
                        var19 = params[253];
                    }
                } else {
                    if (input[8] >= 0.19569999) {
                        var19 = params[254];
                    } else {
                        var19 = params[255];
                    }
                }
            }
        } else {
            if (input[10] >= 1.5) {
                if (input[14] >= 1.5) {
                    if (input[3] >= 0.605) {
                        var19 = params[256];
                    } else {
                        var19 = params[257];
                    }
                } else {
                    if (input[0] >= 1251.9651) {
                        var19 = params[258];
                    } else {
                        var19 = params[259];
                    }
                }
            } else {
                if (input[12] >= 1.5) {
                    if (input[0] >= 650.04504) {
                        var19 = params[260];
                    } else {
                        var19 = params[261];
                    }
                } else {
                    if (input[3] >= 0.46885002) {
                        var19 = params[262];
                    } else {
                        var19 = params[263];
                    }
                }
            }
        }
        double var20;
        if (input[2] >= 0.5) {
            var20 = params[264];
        } else {
            if (input[14] >= 3.5) {
                if (input[0] >= 562.37) {
                    var20 = params[265];
                } else {
                    var20 = params[266];
                }
            } else {
                if (input[11] >= 54.5) {
                    if (input[15] >= 106.5) {
                        var20 = params[267];
                    } else {
                        var20 = params[268];
                    }
                } else {
                    if (input[13] >= 206.5) {
                        var20 = params[269];
                    } else {
                        var20 = params[270];
                    }
                }
            }
        }
        double var21;
        if (input[6] >= 0.19835001) {
            if (input[6] >= 0.21445) {
                if (input[15] >= 100.5) {
                    if (input[7] >= 0.00295) {
                        var21 = params[271];
                    } else {
                        var21 = params[272];
                    }
                } else {
                    if (input[13] >= 118.5) {
                        var21 = params[273];
                    } else {
                        var21 = params[274];
                    }
                }
            } else {
                if (input[13] >= 24.5) {
                    if (input[13] >= 215.5) {
                        var21 = params[275];
                    } else {
                        var21 = params[276];
                    }
                } else {
                    if (input[0] >= 318.57) {
                        var21 = params[277];
                    } else {
                        var21 = params[278];
                    }
                }
            }
        } else {
            if (input[9] >= 0.112) {
                var21 = params[279];
            } else {
                if (input[5] >= 0.068100005) {
                    if (input[13] >= 32.0) {
                        var21 = params[280];
                    } else {
                        var21 = params[281];
                    }
                } else {
                    if (input[24] >= 0.5) {
                        var21 = params[282];
                    } else {
                        var21 = params[283];
                    }
                }
            }
        }
        double var22;
        if (input[25] >= 0.5) {
            if (input[0] >= 1401.725) {
                if (input[8] >= 0.198) {
                    var22 = params[284];
                } else {
                    if (input[11] >= 17.0) {
                        var22 = params[285];
                    } else {
                        var22 = params[286];
                    }
                }
            } else {
                if (input[28] >= 0.5) {
                    if (input[4] >= 3.5353498) {
                        var22 = params[287];
                    } else {
                        var22 = params[288];
                    }
                } else {
                    if (input[3] >= 0.24525) {
                        var22 = params[289];
                    } else {
                        var22 = params[290];
                    }
                }
            }
        } else {
            if (input[11] >= 8.5) {
                if (input[5] >= 0.00725) {
                    if (input[0] >= 380.83002) {
                        var22 = params[291];
                    } else {
                        var22 = params[292];
                    }
                } else {
                    if (input[8] >= 0.30505002) {
                        var22 = params[293];
                    } else {
                        var22 = params[294];
                    }
                }
            } else {
                if (input[5] >= 0.00795) {
                    if (input[5] >= 0.00965) {
                        var22 = params[295];
                    } else {
                        var22 = params[296];
                    }
                } else {
                    if (input[3] >= 0.2368) {
                        var22 = params[297];
                    } else {
                        var22 = params[298];
                    }
                }
            }
        }
        double var23;
        if (input[1] >= 2.5) {
            if (input[9] >= 0.00365) {
                if (input[4] >= 3.20535) {
                    if (input[7] >= 0.024) {
                        var23 = params[299];
                    } else {
                        var23 = params[300];
                    }
                } else {
                    var23 = params[301];
                }
            } else {
                if (input[4] >= 4.20195) {
                    if (input[6] >= 0.21625) {
                        var23 = params[302];
                    } else {
                        var23 = params[303];
                    }
                } else {
                    if (input[11] >= 62.5) {
                        var23 = params[304];
                    } else {
                        var23 = params[305];
                    }
                }
            }
        } else {
            if (input[4] >= 5.10225) {
                if (input[15] >= 11.5) {
                    if (input[11] >= 18.5) {
                        var23 = params[306];
                    } else {
                        var23 = params[307];
                    }
                } else {
                    var23 = params[308];
                }
            } else {
                if (input[4] >= 5.061) {
                    if (input[8] >= 0.0458) {
                        var23 = params[309];
                    } else {
                        var23 = params[310];
                    }
                } else {
                    if (input[2] >= 0.5) {
                        var23 = params[311];
                    } else {
                        var23 = params[312];
                    }
                }
            }
        }
        double var24;
        if (input[13] >= 140.5) {
            if (input[5] >= 0.01165) {
                if (input[4] >= 4.2597) {
                    if (input[7] >= 0.02045) {
                        var24 = params[313];
                    } else {
                        var24 = params[314];
                    }
                } else {
                    if (input[6] >= 0.107700005) {
                        var24 = params[315];
                    } else {
                        var24 = params[316];
                    }
                }
            } else {
                if (input[13] >= 204.5) {
                    var24 = params[317];
                } else {
                    if (input[6] >= 0.19105) {
                        var24 = params[318];
                    } else {
                        var24 = params[319];
                    }
                }
            }
        } else {
            if (input[15] >= 106.5) {
                if (input[4] >= 4.2061) {
                    var24 = params[320];
                } else {
                    if (input[4] >= 3.3965) {
                        var24 = params[321];
                    } else {
                        var24 = params[322];
                    }
                }
            } else {
                if (input[13] >= 99.5) {
                    if (input[9] >= 0.00014999999) {
                        var24 = params[323];
                    } else {
                        var24 = params[324];
                    }
                } else {
                    if (input[4] >= 3.35565) {
                        var24 = params[325];
                    } else {
                        var24 = params[326];
                    }
                }
            }
        }
        double var25;
        if (input[4] >= 4.8779) {
            if (input[5] >= 0.04305) {
                if (input[10] >= 0.5) {
                    var25 = params[327];
                } else {
                    if (input[15] >= 27.5) {
                        var25 = params[328];
                    } else {
                        var25 = params[329];
                    }
                }
            } else {
                if (input[8] >= 0.03765) {
                    if (input[8] >= 0.2132) {
                        var25 = params[330];
                    } else {
                        var25 = params[331];
                    }
                } else {
                    if (input[31] >= 0.5) {
                        var25 = params[332];
                    } else {
                        var25 = params[333];
                    }
                }
            }
        } else {
            if (input[5] >= 0.13865) {
                if (input[7] >= 0.037) {
                    if (input[8] >= 0.42895) {
                        var25 = params[334];
                    } else {
                        var25 = params[335];
                    }
                } else {
                    if (input[13] >= 33.0) {
                        var25 = params[336];
                    } else {
                        var25 = params[337];
                    }
                }
            } else {
                if (input[6] >= 0.19064999) {
                    if (input[7] >= 0.00415) {
                        var25 = params[338];
                    } else {
                        var25 = params[339];
                    }
                } else {
                    if (input[5] >= 0.04385) {
                        var25 = params[340];
                    } else {
                        var25 = params[341];
                    }
                }
            }
        }
        double var26;
        if (input[6] >= 0.41435) {
            if (input[6] >= 0.43980002) {
                var26 = params[342];
            } else {
                var26 = params[343];
            }
        } else {
            if (input[27] >= 0.5) {
                if (input[3] >= 0.5734) {
                    var26 = params[344];
                } else {
                    var26 = params[345];
                }
            } else {
                if (input[28] >= 0.5) {
                    if (input[15] >= 32.5) {
                        var26 = params[346];
                    } else {
                        var26 = params[347];
                    }
                } else {
                    if (input[6] >= 0.00495) {
                        var26 = params[348];
                    } else {
                        var26 = params[349];
                    }
                }
            }
        }
        double var27;
        if (input[14] >= 1.5) {
            if (input[6] >= 0.025850002) {
                if (input[10] >= 1.5) {
                    if (input[5] >= 0.02225) {
                        var27 = params[350];
                    } else {
                        var27 = params[351];
                    }
                } else {
                    if (input[6] >= 0.32005) {
                        var27 = params[352];
                    } else {
                        var27 = params[353];
                    }
                }
            } else {
                if (input[3] >= 0.60445) {
                    var27 = params[354];
                } else {
                    if (input[7] >= 0.105450004) {
                        var27 = params[355];
                    } else {
                        var27 = params[356];
                    }
                }
            }
        } else {
            if (input[10] >= 1.5) {
                if (input[5] >= 0.0025) {
                    if (input[3] >= 0.51035) {
                        var27 = params[357];
                    } else {
                        var27 = params[358];
                    }
                } else {
                    var27 = params[359];
                }
            } else {
                if (input[15] >= 2.5) {
                    if (input[16] >= 0.5) {
                        var27 = params[360];
                    } else {
                        var27 = params[361];
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var27 = params[362];
                    } else {
                        var27 = params[363];
                    }
                }
            }
        }
        double var28;
        if (input[0] >= 831.12) {
            if (input[7] >= 0.090450004) {
                if (input[8] >= 0.54455) {
                    if (input[4] >= 1.87605) {
                        var28 = params[364];
                    } else {
                        var28 = params[365];
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var28 = params[366];
                    } else {
                        var28 = params[367];
                    }
                }
            } else {
                if (input[12] >= 2.5) {
                    var28 = params[368];
                } else {
                    if (input[13] >= 52.5) {
                        var28 = params[369];
                    } else {
                        var28 = params[370];
                    }
                }
            }
        } else {
            if (input[28] >= 0.5) {
                if (input[8] >= 0.8478) {
                    if (input[8] >= 0.87615) {
                        var28 = params[371];
                    } else {
                        var28 = params[372];
                    }
                } else {
                    if (input[4] >= 5.3215) {
                        var28 = params[373];
                    } else {
                        var28 = params[374];
                    }
                }
            } else {
                if (input[14] >= 0.5) {
                    if (input[4] >= 1.1447) {
                        var28 = params[375];
                    } else {
                        var28 = params[376];
                    }
                } else {
                    if (input[3] >= 0.118149996) {
                        var28 = params[377];
                    } else {
                        var28 = params[378];
                    }
                }
            }
        }
        double var29;
        if (input[17] >= 0.5) {
            if (input[7] >= 0.00865) {
                if (input[5] >= 0.0377) {
                    var29 = params[379];
                } else {
                    if (input[13] >= 29.5) {
                        var29 = params[380];
                    } else {
                        var29 = params[381];
                    }
                }
            } else {
                var29 = params[382];
            }
        } else {
            if (input[7] >= 0.25895) {
                if (input[8] >= 1.1168499) {
                    var29 = params[383];
                } else {
                    if (input[8] >= 0.98644996) {
                        var29 = params[384];
                    } else {
                        var29 = params[385];
                    }
                }
            } else {
                if (input[7] >= 0.093600005) {
                    if (input[8] >= 0.4123) {
                        var29 = params[386];
                    } else {
                        var29 = params[387];
                    }
                } else {
                    if (input[8] >= 0.71505) {
                        var29 = params[388];
                    } else {
                        var29 = params[389];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
