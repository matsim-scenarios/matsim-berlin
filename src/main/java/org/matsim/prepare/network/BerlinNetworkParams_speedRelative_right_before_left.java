package org.matsim.prepare.network;
import org.matsim.application.prepare.network.opt.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class BerlinNetworkParams_speedRelative_right_before_left implements FeatureRegressor {
    
    public static BerlinNetworkParams_speedRelative_right_before_left INSTANCE = new BerlinNetworkParams_speedRelative_right_before_left();
    public static final double[] DEFAULT_PARAMS = {0.8646015117982583, 0.8788410625723447, 0.8484615384615384, 0.945952380952381, 0.8242520350020349, 0.8464025000000001, 0.8659559502272193, 0.8851620039682541, 0.8854518342303758, 0.9036853232070694, 0.8808906070032868, 0.872854898457248, 0.9044877414925957, 0.8882173312737994, 0.8725341191417284, 0.8904124999999997, 0.8766650168909327, 0.95, 0.841978021978022, 0.9156062500000001, 0.7878571428571428, 0.8427712711265344, 0.8648233821407353, 0.8876901544401542, 0.883205103692242, 0.8646209972822875, 0.8866945551896072, 0.8473611111111112, 0.8831034482758621, 0.947572549019608, 0.9169444444444443, 0.858, 0.8815480769230769, 0.8442475052521007, 0.8791538686343874, 0.9170643939393941, 0.8552576260838972, 0.8228541666666666, 0.8655722718142072, 0.8228571428571427, 0.8846874505658122, 0.8803174156285094, 0.89547561762474, 0.8859449046872315, 0.9583333333333334, 0.8745461096443238, 0.81, 0.8613491295425671, 0.8801602372823522, 0.8972979242979244, 0.9307508116883118, 0.8521188716101303, 0.8238969696969697, 0.8798309523809525, 0.8591700823789435, 0.8233333333333333, 0.775, 0.89, 0.8530980392156863, 0.8833953014408202, 0.8859223703206229, 0.8855555555555557, 0.9439956896551724, 0.9228888888888889, 0.8783163410683079, 0.8604195804195804, 0.9267001262626261, 0.8281261581555698, 0.8477010869565219, 0.8666127761824998, 0.8062962962962963, 0.84, 0.83, 0.8183333333333334, 0.8853516286666312, 0.8805828454301285, 0.8871780218515735, 0.8768336002207475, 0.8638316796279627, 0.8226550733254063, 0.8773566710996508, 0.9227794011544013, 0.8507118505868506, 0.76, 0.867317187233316, 0.8889737274220032, 0.8850625631178106, 0.8704901477832513, 0.8791849007500389, 0.9340909090909091, 0.898711547291093, 0.9155819047619047, 0.8867853668262621, 0.8707731999800963, 0.8659184808203007, 0.88038522560916, 0.938935185185185, 0.8903836088154271, 0.8333437801833327, 0.76, 0.864311372655123, 0.8281666666666666, 0.7916666666666666, 0.8852040464273085, 0.8811688825317017, 0.9097142857142859, 0.9416666666666668, 0.8865754901134888, 0.835, 0.8557006531204645, 0.8779330792944756, 0.9231411152949612, 0.8818026315789473, 0.8398879629629631, 0.79, 0.8694108344163016, 0.7833333333333332, 0.8837289313753577, 0.887051019605796, 0.8555420168067227, 0.8775083476857672, 0.9176398467432951, 0.874, 0.97, 0.955, 0.8794895289863226, 0.8645938263125766, 0.8096666666666668, 0.9198954175905397, 0.9562499999999999, 0.918, 0.8626657599459774, 0.822037037037037, 0.8826614275944579, 0.8862027221096526, 0.8649898831327402, 0.8804261005414634, 0.9217642432653642, 0.8912820512820514, 0.97, 0.8868548356308275, 0.9259298245614033, 0.8635123963716513, 0.8782659865693775, 0.7993095238095239, 0.8433930386430386, 0.9460000000000001, 0.8666941731601728, 0.8322794117647059, 0.8844726660358417, 0.8703792582417583, 0.8853286661255411, 0.86, 0.9367663962136579, 0.975, 0.883190939869121, 0.8743693906754619, 0.8237655677655678, 0.9183613980716253, 0.8166904761904762, 0.8594422316924882, 0.8838009720684139, 0.806, 0.8853339766250385, 0.8812506179148929, 0.8628600185528758, 0.7999999999999999, 0.8878542845816493, 0.9633333333333333, 0.8777018713209191, 0.8266666666666668, 0.9316666666666666, 0.877054049516478, 0.8402930402930403, 0.9241172348484848, 0.945, 0.8337327999213593, 0.8649633698480658, 0.8134523809523808, 0.8840620595140162, 0.8572565864833905, 0.8876985803890554, 0.9355, 0.867561111111111, 0.8064285714285714, 0.9324999999999999, 0.8772805995015088, 0.8616292418980559, 0.8764349915696938, 0.8277216117216116, 0.9262029220779221, 0.846422560690943, 0.7988916666666667, 0.97, 0.8646500941575708, 0.8823639945193488, 0.8345238095238096, 0.7966666666666666, 0.8559901960784314, 0.8868004715687479, 0.9254051282051281, 0.9390000000000001, 0.8782161777941421, 0.8678469370796373, 0.8799823422307548, 0.8342978021978019, 0.8683570305983279, 0.75, 0.8839555839005792, 0.8864544178041903, 0.8099999999999999, 0.8647058823529412, 0.9041666666666667, 0.8816009852216748, 0.8980769230769231, 0.9506042780748661, 0.8869804203732776, 0.8543678750368408, 0.8785034662238783, 0.9150772357723577, 0.8510621882696062, 0.8648894198524483, 0.95, 0.8779582885785894, 0.8859811805275934, 0.853828125, 0.8772443936440636, 0.883097584153205, 0.9137179487179485, 0.8877334940237789, 0.868840953917877, 0.75};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 135.45585940281805) / 80.1533714259824;
		data[1] = (ft.getDouble("speed") - 8.33240398799532) / 0.1138739274502052;
		data[2] = (ft.getDouble("num_lanes") - 1.0053919324482425) / 0.07859221404615896;
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
        if (input[0] <= -0.8098955750465393) {
            if (input[5] <= 2.5) {
                if (input[11] <= 0.5) {
                    if (input[0] <= -1.4280853867530823) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[0] <= -1.4638917446136475) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[0] <= -1.2924079895019531) {
                    if (input[0] <= -1.3904076218605042) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    if (input[0] <= -0.8750830888748169) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                }
            }
        } else {
            if (input[0] <= 0.2488871067762375) {
                if (input[5] <= 2.5) {
                    if (input[6] <= 3.5) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                } else {
                    if (input[0] <= 0.18600765615701675) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                }
            } else {
                if (input[0] <= 2.8384850025177) {
                    if (input[0] <= 0.2687864452600479) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                } else {
                    if (input[6] <= 3.5) {
                        var0 = params[14];
                    } else {
                        var0 = params[15];
                    }
                }
            }
        }
        double var1;
        if (input[0] <= -0.7619873881340027) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[13] <= 0.5) {
                        var1 = params[16];
                    } else {
                        var1 = params[17];
                    }
                } else {
                    if (input[0] <= -1.3798653483390808) {
                        var1 = params[18];
                    } else {
                        var1 = params[19];
                    }
                }
            } else {
                if (input[0] <= -1.2768753170967102) {
                    if (input[0] <= -1.4838534593582153) {
                        var1 = params[20];
                    } else {
                        var1 = params[21];
                    }
                } else {
                    if (input[0] <= -0.8041565716266632) {
                        var1 = params[22];
                    } else {
                        var1 = params[23];
                    }
                }
            }
        } else {
            if (input[2] <= 6.293346848338842) {
                if (input[0] <= -0.029142373241484165) {
                    if (input[0] <= -0.04074512794613838) {
                        var1 = params[24];
                    } else {
                        var1 = params[25];
                    }
                } else {
                    if (input[0] <= 4.404295444488525) {
                        var1 = params[26];
                    } else {
                        var1 = params[27];
                    }
                }
            } else {
                if (input[5] <= 1.5) {
                    if (input[0] <= -0.6937432587146759) {
                        var1 = params[28];
                    } else {
                        var1 = params[29];
                    }
                } else {
                    if (input[0] <= 0.5972816869616508) {
                        var1 = params[30];
                    } else {
                        var1 = params[31];
                    }
                }
            }
        }
        double var2;
        if (input[0] <= -0.8362200856208801) {
            if (input[5] <= 2.5) {
                if (input[0] <= -1.4977643489837646) {
                    if (input[0] <= -1.5889021158218384) {
                        var2 = params[32];
                    } else {
                        var2 = params[33];
                    }
                } else {
                    if (input[2] <= 6.293346848338842) {
                        var2 = params[34];
                    } else {
                        var2 = params[35];
                    }
                }
            } else {
                if (input[0] <= -1.2901623249053955) {
                    if (input[0] <= -1.3366981148719788) {
                        var2 = params[36];
                    } else {
                        var2 = params[37];
                    }
                } else {
                    if (input[0] <= -0.8462633192539215) {
                        var2 = params[38];
                    } else {
                        var2 = params[39];
                    }
                }
            }
        } else {
            if (input[0] <= 2.747970938682556) {
                if (input[0] <= -0.1159135214984417) {
                    if (input[5] <= 2.5) {
                        var2 = params[40];
                    } else {
                        var2 = params[41];
                    }
                } else {
                    if (input[0] <= -0.0778614692389965) {
                        var2 = params[42];
                    } else {
                        var2 = params[43];
                    }
                }
            } else {
                if (input[5] <= 1.5) {
                    var2 = params[44];
                } else {
                    if (input[0] <= 4.5192625522613525) {
                        var2 = params[45];
                    } else {
                        var2 = params[46];
                    }
                }
            }
        }
        double var3;
        if (input[0] <= -0.8377796113491058) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[0] <= -1.4280853867530823) {
                        var3 = params[47];
                    } else {
                        var3 = params[48];
                    }
                } else {
                    if (input[0] <= -1.2201714515686035) {
                        var3 = params[49];
                    } else {
                        var3 = params[50];
                    }
                }
            } else {
                if (input[0] <= -1.1532372832298279) {
                    if (input[0] <= -1.1816204190254211) {
                        var3 = params[51];
                    } else {
                        var3 = params[52];
                    }
                } else {
                    if (input[0] <= -0.9801940619945526) {
                        var3 = params[53];
                    } else {
                        var3 = params[54];
                    }
                }
            }
        } else {
            if (input[3] <= -2.7799999713897705) {
                if (input[0] <= -0.16163835674524307) {
                    if (input[0] <= -0.46343477070331573) {
                        var3 = params[55];
                    } else {
                        var3 = params[56];
                    }
                } else {
                    if (input[5] <= 1.5) {
                        var3 = params[57];
                    } else {
                        var3 = params[58];
                    }
                }
            } else {
                if (input[11] <= 0.5) {
                    if (input[0] <= -0.11528971418738365) {
                        var3 = params[59];
                    } else {
                        var3 = params[60];
                    }
                } else {
                    if (input[6] <= 3.5) {
                        var3 = params[61];
                    } else {
                        var3 = params[62];
                    }
                }
            }
        }
        double var4;
        if (input[0] <= -0.8400876820087433) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[0] <= -1.5900249481201172) {
                        var4 = params[63];
                    } else {
                        var4 = params[64];
                    }
                } else {
                    if (input[0] <= -1.3598412871360779) {
                        var4 = params[65];
                    } else {
                        var4 = params[66];
                    }
                }
            } else {
                if (input[0] <= -1.2901623249053955) {
                    if (input[0] <= -1.3904076218605042) {
                        var4 = params[67];
                    } else {
                        var4 = params[68];
                    }
                } else {
                    if (input[0] <= -0.8460138142108917) {
                        var4 = params[69];
                    } else {
                        var4 = params[70];
                    }
                }
            }
        } else {
            if (input[8] <= 0.5) {
                if (input[9] <= 0.5) {
                    if (input[0] <= 0.6279104650020599) {
                        var4 = params[71];
                    } else {
                        var4 = params[72];
                    }
                } else {
                    var4 = params[73];
                }
            } else {
                if (input[0] <= 0.22405970096588135) {
                    if (input[5] <= 2.5) {
                        var4 = params[74];
                    } else {
                        var4 = params[75];
                    }
                } else {
                    if (input[0] <= 2.757702350616455) {
                        var4 = params[76];
                    } else {
                        var4 = params[77];
                    }
                }
            }
        }
        double var5;
        if (input[0] <= -0.7785182297229767) {
            if (input[5] <= 2.5) {
                if (input[0] <= -1.429208219051361) {
                    if (input[0] <= -1.449045181274414) {
                        var5 = params[78];
                    } else {
                        var5 = params[79];
                    }
                } else {
                    if (input[2] <= 6.293346848338842) {
                        var5 = params[80];
                    } else {
                        var5 = params[81];
                    }
                }
            } else {
                if (input[0] <= -1.2479931712150574) {
                    if (input[0] <= -1.2507379055023193) {
                        var5 = params[82];
                    } else {
                        var5 = params[83];
                    }
                } else {
                    if (input[0] <= -0.7987294793128967) {
                        var5 = params[84];
                    } else {
                        var5 = params[85];
                    }
                }
            }
        } else {
            if (input[0] <= -0.11528971418738365) {
                if (input[5] <= 2.5) {
                    if (input[0] <= -0.13082244247198105) {
                        var5 = params[86];
                    } else {
                        var5 = params[87];
                    }
                } else {
                    if (input[4] <= 0.5) {
                        var5 = params[88];
                    } else {
                        var5 = params[89];
                    }
                }
            } else {
                if (input[0] <= -0.09507846459746361) {
                    if (input[0] <= -0.09832224622368813) {
                        var5 = params[90];
                    } else {
                        var5 = params[91];
                    }
                } else {
                    if (input[0] <= 3.716614007949829) {
                        var5 = params[92];
                    } else {
                        var5 = params[93];
                    }
                }
            }
        }
        double var6;
        if (input[0] <= -0.838216245174408) {
            if (input[5] <= 2.5) {
                if (input[6] <= 3.5) {
                    if (input[0] <= -1.4280853867530823) {
                        var6 = params[94];
                    } else {
                        var6 = params[95];
                    }
                } else {
                    if (input[5] <= 1.5) {
                        var6 = params[96];
                    } else {
                        var6 = params[97];
                    }
                }
            } else {
                if (input[0] <= -1.3090012073516846) {
                    if (input[0] <= -1.3111221194267273) {
                        var6 = params[98];
                    } else {
                        var6 = params[99];
                    }
                } else {
                    if (input[0] <= -0.8490080833435059) {
                        var6 = params[100];
                    } else {
                        var6 = params[101];
                    }
                }
            }
        } else {
            if (input[0] <= -0.062453508377075195) {
                if (input[3] <= -2.7799999713897705) {
                    var6 = params[102];
                } else {
                    if (input[5] <= 2.5) {
                        var6 = params[103];
                    } else {
                        var6 = params[104];
                    }
                }
            } else {
                if (input[0] <= -0.06101876124739647) {
                    if (input[0] <= -0.06226636841893196) {
                        var6 = params[105];
                    } else {
                        var6 = params[106];
                    }
                } else {
                    if (input[0] <= 4.499675035476685) {
                        var6 = params[107];
                    } else {
                        var6 = params[108];
                    }
                }
            }
        }
        double var7;
        if (input[0] <= -0.7750872671604156) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[0] <= -1.5388729572296143) {
                        var7 = params[109];
                    } else {
                        var7 = params[110];
                    }
                } else {
                    if (input[0] <= -1.0035867094993591) {
                        var7 = params[111];
                    } else {
                        var7 = params[112];
                    }
                }
            } else {
                if (input[0] <= -1.2901623249053955) {
                    if (input[0] <= -1.2925951480865479) {
                        var7 = params[113];
                    } else {
                        var7 = params[114];
                    }
                } else {
                    if (input[0] <= -0.7767715752124786) {
                        var7 = params[115];
                    } else {
                        var7 = params[116];
                    }
                }
            }
        } else {
            if (input[2] <= 6.293346848338842) {
                if (input[0] <= 2.8364888429641724) {
                    if (input[0] <= 0.20759127289056778) {
                        var7 = params[117];
                    } else {
                        var7 = params[118];
                    }
                } else {
                    if (input[0] <= 2.927688956260681) {
                        var7 = params[119];
                    } else {
                        var7 = params[120];
                    }
                }
            } else {
                if (input[0] <= 1.0345683693885803) {
                    if (input[0] <= 0.5394550412893295) {
                        var7 = params[121];
                    } else {
                        var7 = params[122];
                    }
                } else {
                    if (input[4] <= 1.5) {
                        var7 = params[123];
                    } else {
                        var7 = params[124];
                    }
                }
            }
        }
        double var8;
        if (input[0] <= -0.8380291163921356) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[0] <= -0.8648526966571808) {
                        var8 = params[125];
                    } else {
                        var8 = params[126];
                    }
                } else {
                    if (input[0] <= -1.3985170722007751) {
                        var8 = params[127];
                    } else {
                        var8 = params[128];
                    }
                }
            } else {
                if (input[6] <= 2.5) {
                    if (input[3] <= 2.7799999713897705) {
                        var8 = params[129];
                    } else {
                        var8 = params[130];
                    }
                } else {
                    if (input[0] <= -0.8495071232318878) {
                        var8 = params[131];
                    } else {
                        var8 = params[132];
                    }
                }
            }
        } else {
            if (input[2] <= 6.293346848338842) {
                if (input[0] <= 1.8170058131217957) {
                    if (input[0] <= -0.11528971418738365) {
                        var8 = params[133];
                    } else {
                        var8 = params[134];
                    }
                } else {
                    if (input[0] <= 1.9167520999908447) {
                        var8 = params[135];
                    } else {
                        var8 = params[136];
                    }
                }
            } else {
                if (input[0] <= 1.571164608001709) {
                    if (input[0] <= -0.10867739096283913) {
                        var8 = params[137];
                    } else {
                        var8 = params[138];
                    }
                } else {
                    var8 = params[139];
                }
            }
        }
        double var9;
        if (input[0] <= -0.7557493448257446) {
            if (input[5] <= 2.5) {
                if (input[5] <= 1.5) {
                    if (input[6] <= 3.5) {
                        var9 = params[140];
                    } else {
                        var9 = params[141];
                    }
                } else {
                    if (input[0] <= -1.3648940324783325) {
                        var9 = params[142];
                    } else {
                        var9 = params[143];
                    }
                }
            } else {
                if (input[0] <= -1.2979598641395569) {
                    if (input[0] <= -1.4834167957305908) {
                        var9 = params[144];
                    } else {
                        var9 = params[145];
                    }
                } else {
                    if (input[6] <= 2.5) {
                        var9 = params[146];
                    } else {
                        var9 = params[147];
                    }
                }
            }
        } else {
            if (input[11] <= 0.5) {
                if (input[0] <= 2.6008156538009644) {
                    if (input[3] <= -2.7799999713897705) {
                        var9 = params[148];
                    } else {
                        var9 = params[149];
                    }
                } else {
                    if (input[0] <= 3.5318431854248047) {
                        var9 = params[150];
                    } else {
                        var9 = params[151];
                    }
                }
            } else {
                if (input[6] <= 3.0) {
                    var9 = params[152];
                } else {
                    if (input[6] <= 4.5) {
                        var9 = params[153];
                    } else {
                        var9 = params[154];
                    }
                }
            }
        }
        double var10;
        if (input[0] <= -0.7560612857341766) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[6] <= 2.5) {
                        var10 = params[155];
                    } else {
                        var10 = params[156];
                    }
                } else {
                    if (input[0] <= -1.4703169465065002) {
                        var10 = params[157];
                    } else {
                        var10 = params[158];
                    }
                }
            } else {
                if (input[0] <= -0.8366567492485046) {
                    if (input[0] <= -1.4838534593582153) {
                        var10 = params[159];
                    } else {
                        var10 = params[160];
                    }
                } else {
                    if (input[0] <= -0.7565603256225586) {
                        var10 = params[161];
                    } else {
                        var10 = params[162];
                    }
                }
            }
        } else {
            if (input[0] <= 0.1985461190342903) {
                if (input[0] <= 0.18669383972883224) {
                    if (input[5] <= 2.5) {
                        var10 = params[163];
                    } else {
                        var10 = params[164];
                    }
                } else {
                    if (input[0] <= 0.1982342153787613) {
                        var10 = params[165];
                    } else {
                        var10 = params[166];
                    }
                }
            } else {
                if (input[0] <= 2.7108547687530518) {
                    if (input[0] <= 2.6998133659362793) {
                        var10 = params[167];
                    } else {
                        var10 = params[168];
                    }
                } else {
                    if (input[0] <= 4.5192625522613525) {
                        var10 = params[169];
                    } else {
                        var10 = params[170];
                    }
                }
            }
        }
        double var11;
        if (input[0] <= -0.838216245174408) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[0] <= -1.6221508383750916) {
                        var11 = params[171];
                    } else {
                        var11 = params[172];
                    }
                } else {
                    if (input[0] <= -1.4983881115913391) {
                        var11 = params[173];
                    } else {
                        var11 = params[174];
                    }
                }
            } else {
                if (input[0] <= -1.2955893874168396) {
                    if (input[6] <= 2.5) {
                        var11 = params[175];
                    } else {
                        var11 = params[176];
                    }
                } else {
                    if (input[0] <= -0.8438304960727692) {
                        var11 = params[177];
                    } else {
                        var11 = params[178];
                    }
                }
            }
        } else {
            if (input[0] <= 2.6652544736862183) {
                if (input[0] <= -0.23618294298648834) {
                    if (input[0] <= -0.24510334432125092) {
                        var11 = params[179];
                    } else {
                        var11 = params[180];
                    }
                } else {
                    if (input[2] <= 6.293346848338842) {
                        var11 = params[181];
                    } else {
                        var11 = params[182];
                    }
                }
            } else {
                if (input[0] <= 2.910908579826355) {
                    if (input[0] <= 2.8960620164871216) {
                        var11 = params[183];
                    } else {
                        var11 = params[184];
                    }
                } else {
                    if (input[0] <= 2.9170843362808228) {
                        var11 = params[185];
                    } else {
                        var11 = params[186];
                    }
                }
            }
        }
        double var12;
        if (input[0] <= -0.838216245174408) {
            if (input[5] <= 2.5) {
                if (input[2] <= 6.293346848338842) {
                    if (input[0] <= -1.3799277544021606) {
                        var12 = params[187];
                    } else {
                        var12 = params[188];
                    }
                } else {
                    if (input[0] <= -1.4403743743896484) {
                        var12 = params[189];
                    } else {
                        var12 = params[190];
                    }
                }
            } else {
                if (input[0] <= -1.3081902861595154) {
                    if (input[0] <= -1.3366981148719788) {
                        var12 = params[191];
                    } else {
                        var12 = params[192];
                    }
                } else {
                    if (input[0] <= -1.305882215499878) {
                        var12 = params[193];
                    } else {
                        var12 = params[194];
                    }
                }
            }
        } else {
            if (input[0] <= -0.2361205667257309) {
                if (input[0] <= -0.24092385917901993) {
                    if (input[5] <= 3.5) {
                        var12 = params[195];
                    } else {
                        var12 = params[196];
                    }
                } else {
                    if (input[0] <= -0.24030005186796188) {
                        var12 = params[197];
                    } else {
                        var12 = params[198];
                    }
                }
            } else {
                if (input[0] <= 2.6360604763031006) {
                    if (input[2] <= 6.293346848338842) {
                        var12 = params[199];
                    } else {
                        var12 = params[200];
                    }
                } else {
                    if (input[5] <= 1.5) {
                        var12 = params[201];
                    } else {
                        var12 = params[202];
                    }
                }
            }
        }
        double var13;
        if (input[0] <= -0.8080865144729614) {
            if (input[0] <= -0.8084608018398285) {
                if (input[5] <= 2.5) {
                    if (input[0] <= -1.333890974521637) {
                        var13 = params[203];
                    } else {
                        var13 = params[204];
                    }
                } else {
                    if (input[0] <= -1.4200383424758911) {
                        var13 = params[205];
                    } else {
                        var13 = params[206];
                    }
                }
            } else {
                var13 = params[207];
            }
        } else {
            if (input[2] <= 6.293346848338842) {
                if (input[1] <= 24.39185311924666) {
                    if (input[0] <= 0.2494485303759575) {
                        var13 = params[208];
                    } else {
                        var13 = params[209];
                    }
                } else {
                    if (input[0] <= 0.32062457501888275) {
                        var13 = params[210];
                    } else {
                        var13 = params[211];
                    }
                }
            } else {
                if (input[0] <= -0.4999273270368576) {
                    if (input[10] <= 0.5) {
                        var13 = params[212];
                    } else {
                        var13 = params[213];
                    }
                } else {
                    if (input[4] <= -0.5) {
                        var13 = params[214];
                    } else {
                        var13 = params[215];
                    }
                }
            }
        }
        double var14;
        if (input[0] <= -0.7786429822444916) {
            if (input[5] <= 2.5) {
                if (input[0] <= -1.4189778566360474) {
                    if (input[6] <= 2.5) {
                        var14 = params[216];
                    } else {
                        var14 = params[217];
                    }
                } else {
                    if (input[2] <= 6.293346848338842) {
                        var14 = params[218];
                    } else {
                        var14 = params[219];
                    }
                }
            } else {
                if (input[0] <= -0.8704045414924622) {
                    if (input[0] <= -1.1514906287193298) {
                        var14 = params[220];
                    } else {
                        var14 = params[221];
                    }
                } else {
                    if (input[0] <= -0.868283599615097) {
                        var14 = params[222];
                    } else {
                        var14 = params[223];
                    }
                }
            }
        } else {
            if (input[0] <= 0.22337352484464645) {
                if (input[5] <= 2.5) {
                    if (input[0] <= 0.2148897871375084) {
                        var14 = params[224];
                    } else {
                        var14 = params[225];
                    }
                } else {
                    if (input[0] <= -0.20935937017202377) {
                        var14 = params[226];
                    } else {
                        var14 = params[227];
                    }
                }
            } else {
                if (input[10] <= 0.5) {
                    if (input[0] <= 0.2297363206744194) {
                        var14 = params[228];
                    } else {
                        var14 = params[229];
                    }
                } else {
                    if (input[3] <= 2.7799999713897705) {
                        var14 = params[230];
                    } else {
                        var14 = params[231];
                    }
                }
            }
        }
        return (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14) * 0.06666666666666667;
    }
}
