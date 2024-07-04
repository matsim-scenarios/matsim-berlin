package org.matsim.prepare.network;
import org.matsim.application.prepare.network.params.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public final class BerlinNetworkParams_speedRelative_priority implements FeatureRegressor {

    public static BerlinNetworkParams_speedRelative_priority INSTANCE = new BerlinNetworkParams_speedRelative_priority();
    public static final double[] DEFAULT_PARAMS = {0.12550950050354004, 0.189355731010437, 0.17886589467525482, 0.15100084245204926, 0.16275350749492645, 0.22602279484272003, 0.15281672775745392, 0.15926821529865265, 0.07377482205629349, 0.08893188834190369, 0.091558538377285, 0.09821221232414246, 0.10520006716251373, 0.0773724764585495, 0.11609984934329987, 0.11544670909643173, 0.057235196232795715, 0.07558291405439377, 0.009422936476767063, 0.008980573154985905, 0.05040629580616951, 0.08543317019939423, -0.006561304908245802, -0.005893743131309748, -0.004282128997147083, -0.026850620284676552, 0.054473187774419785, 0.08489955961704254, 0.01490788534283638, 0.006346927955746651, 0.02943824604153633, 0.008724082261323929, -0.026141716167330742, -0.033894672989845276, 0.04418030381202698, 0.01323855109512806, 0.004540024790912867, 0.012382707558572292, 0.04508198797702789, -0.004851947072893381, 0.03212041035294533, 0.018353469669818878, 0.015060266479849815, -0.01619911752641201, 0.06256382167339325, 0.012398114427924156, 0.04449858516454697, -0.018833644688129425, 0.015152745880186558, 0.01700606942176819, -0.02672872692346573, 0.0416702926158905, 0.010799357667565346, -0.07898901402950287, 0.006450137123465538, -0.005750605836510658, -0.014523745514452457, -0.04408177360892296, 0.04883847013115883, -0.022843588143587112, -0.023223113268613815, -0.019399531185626984, 0.021782495081424713, 0.019667288288474083, -0.02515421062707901, -0.00809520948678255, 0.06308271735906601, -0.0023248018696904182, 0.015324900858104229, -0.010448208078742027, -0.005368105135858059, 0.0036487847100943327, 0.014546352438628674, -0.010148151777684689, -0.011488407850265503, 0.011713712476193905, 0.03729243949055672, -0.028871363028883934, -0.020864658057689667, 0.0068322136066854, 0.026309804990887642, -0.008680366910994053, 0.009399128146469593, -0.06411990523338318, 0.0028396653942763805, -0.027854491025209427, 0.0044095274060964584, 0.0025082863867282867, 0.02044624462723732, 0.005750803276896477, -0.0233260877430439, -0.017139121890068054, 0.01264281664043665, -0.013013861142098904, 0.0036902816500514746, 0.03193708509206772, 0.0081612728536129, -0.04379268363118172, -0.016621025279164314, 0.01968771405518055, -0.016309073194861412, -0.01988919824361801, -0.022129802033305168, -0.0036659406032413244, -0.054972246289253235, -0.048217542469501495, 0.013189898803830147, -0.023465821519494057, -0.014551885426044464, -0.01085179764777422, -0.004108797758817673, -0.021892372518777847, 0.038366325199604034, -0.00669700326398015, -0.002341065788641572, -0.012381287291646004, -0.003254153998568654, 0.018539857119321823, -0.004215467255562544, 0.006622281391173601, 0.025530768558382988, -0.010200202465057373, -0.015365288592875004, -0.043065737932920456, -0.0172751322388649, 0.00842320080846548, -0.013684536330401897, 0.006557685323059559, 0.02774382010102272, -0.04138639569282532, 0.014490441419184208, 0.029965436086058617, -0.02057560347020626, 0.008018595166504383, -0.07125191390514374, -0.013040341436862946, 0.025720685720443726, 0.034274134784936905, -0.007738122250884771, -0.02914026565849781, 0.002046799287199974, 0.002551571000367403, 0.003382128896191716, -0.013111705891788006, 0.010415175929665565, 0.022097080945968628, -0.038005538284778595, -0.0016290235798805952, -0.03373470902442932, 0.008343146182596684, -0.031475234776735306, 0.03533920645713806, -0.02184664085507393, 0.04885302484035492, -0.011739714071154594, -0.043699465692043304, -0.006461615674197674, 0.0024099647998809814, 0.001549588399939239, 0.002348459791392088, 0.0012500251177698374, -0.015067570842802525, 0.006313795689493418, 0.0053595551289618015, 0.04169558733701706, -0.010738281533122063, -0.024225503206253052, -0.03299573063850403, -0.02137511409819126, 0.0010198460659012198, 0.01464577205479145, -0.010252954438328743, 0.010739730671048164, -0.016007104888558388, -0.0064740474335849285, -0.011928597465157509, 0.01651456579566002, -0.007379315327852964, -0.05368182808160782, -0.0026138005778193474, -0.00727581512182951, -0.026764769107103348, -0.014498701319098473, 0.0025670919567346573, 0.007335836533457041, -0.0023193827364593744, -0.020489776507019997, 0.022850999608635902, -0.011527427472174168, -0.047909364104270935, 0.013235253281891346, 0.027461105957627296, -0.03219754993915558, -0.020433438941836357, 0.007957947440445423, 0.02916964516043663, -0.01599435694515705, -0.02039303071796894, -0.005623361561447382, 0.037930265069007874, -0.02354472316801548, 0.013999874703586102, -0.01911725476384163, -0.014107367023825645, 0.011351631954312325, -0.003752398770302534, -0.02974548190832138, -0.04380866512656212, 0.01848854497075081, 0.006680892314761877, 0.004972042050212622, 0.03653397038578987, -0.011084431782364845, -0.006494482047855854, 0.020584935322403908, -0.003768667345866561, -3.340105104143731e-05, -0.029125778004527092, 0.006228133104741573, 0.008600886911153793, -0.033562663942575455, -0.0025175928603857756, 0.016567666083574295, -0.027320487424731255, 0.017588986083865166, 0.0015938619617372751, -0.012684625573456287, 0.01542857475578785, 0.013579197227954865, -0.01283981278538704, -0.03461580350995064, -0.02341916412115097, -0.005692454520612955, -0.0031673801131546497, -0.029014386236667633, -0.0018230746500194073, -0.004706212785094976, 0.011962894350290298, 0.011240237392485142, 0.015453718602657318, 0.011790980584919453, -0.01706736907362938, -0.03504134342074394, -0.06385992467403412, -0.014527950435876846, -0.0013383073965087533, 0.021808616816997528, -0.008378241211175919, -0.0390448197722435, -0.02441360056400299, -0.03476740047335625, 0.02811412513256073, -0.011769075877964497, 0.004476679023355246, 0.004439139738678932, -0.009369712322950363, 0.01290037203580141, 0.038308948278427124, -0.0024049500934779644, -0.04359044134616852, 0.010047076269984245, -0.016951922327280045, -0.017950627952814102, 0.0002252952690469101, -0.0859515517950058, 0.012960616499185562, -0.009868291206657887, 0.020009377971291542, 0.01893169991672039, 0.014683406800031662, -0.0030375521164387465, -0.001094939187169075, -0.022562043741345406, -0.013336949981749058, 0.0033218443859368563, -0.007926808670163155, -0.015716295689344406, -0.008151188492774963, -0.05472773686051369, -0.07977693527936935};

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
