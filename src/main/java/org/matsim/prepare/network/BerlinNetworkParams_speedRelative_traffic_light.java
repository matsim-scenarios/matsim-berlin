package org.matsim.prepare.network;
import org.matsim.application.prepare.network.opt.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class BerlinNetworkParams_speedRelative_traffic_light implements FeatureRegressor {
    
    public static BerlinNetworkParams_speedRelative_traffic_light INSTANCE = new BerlinNetworkParams_speedRelative_traffic_light();
    public static final double[] DEFAULT_PARAMS = {-0.009691463, 0.052806545, -0.007887105, 0.040375125, 0.08020092, 0.044737976, 0.10002494, 0.07672291, -0.073485434, -0.031119274, -0.0029490574, -0.04096618, -0.003189836, 0.051309492, -0.039599933, -0.002244698, -0.09188805, -0.03255971, -0.046271984, -0.0089371875, -0.034567412, 0.0095733525, 0.035447326, 0.0093019055, -0.014522874, -0.03195848, 0.0258405, 0.05134684, 0.026390059, 0.058743622, 0.044136044, 0.0, -0.05342304, -0.004916638, -0.03518663, 0.046198614, 0.03864144, 0.023041246, 0.0125265885, -0.0744131, -0.009136273, -0.02858817, -0.039193172, -0.003910333, 0.018595288, 0.053226132, -0.031997606, 0.017666463, -0.009177802, 0.013870448, -0.024133638, 0.0017637078, 0.013503278, 0.012302528, 0.022415783, -0.027985837, -0.009695222, 0.023504179, -0.04091435, -0.068559885, -0.023473417, 0.027454723, -0.0051399437, 0.044723466, 0.032068558, -0.055419356, 0.030762753, 0.016817205, 0.0049013947, 0.016444787, -0.0050147683, -0.017774953, 0.010770579, -0.007024835, -0.028456237, -0.04430385, -0.04920439, -0.014479365, -0.055298172, 0.003580703, 0.024280135, 0.00012065734, -0.015506362, 0.0, -0.023378937, -0.0037427654, 0.024222957, 0.02459689, 0.0070823175, -0.042305857, 0.048024807, 0.006686085, -0.010287019, -0.026067832, -0.071393386, 0.0010783518, -0.059132952, -0.005298848, -0.04494835, 0.021275008, 0.010835968, 8.2577106e-05, 0.010464311, -0.009353127, -0.0249503, -0.0072903493, 0.001235597, -0.020357493, 0.039757032, -0.022714151, 0.0, 0.0089792395, 0.021386923, -0.023778018, 0.0050843307, 0.0, -0.04394475, -0.009849588, -0.00014703265, 6.5884946e-05, -0.04002534, 0.017317673, -0.0039160196, -0.02059181, -0.0032235435, 0.004753312, -0.001508611, -0.017107166, -0.05112474, 0.022588141, 0.0028790687, 0.025551684, -0.028207319, 0.02195619, 0.0055198413, 0.00020586084, -0.019469675, -0.02347914, 0.012801895, 0.0009753697, -0.0072770226, 0.008765168, 0.0023732872, -0.017578049, 0.01648564, -0.010396203, 0.024333026, -0.01315347, 0.014487011, -0.006163603, 0.037203528, 0.0017512897, -0.020835035, -0.002548354, -0.026667723, 0.018359296, 0.0072020893, 0.0050423923, 0.00044790143, -0.027442059, -0.00096547365, -0.0069647734, 0.01001807, -0.009705856, -0.0052660042, 0.013949067, -0.026002107, -0.010855486, 0.025405338, -0.0033861094, -0.00031673856, -0.005639268, 0.0045916494, -0.0139834555, 0.036172427, 0.0060296264, -0.03260621, 0.012408854, -0.0016873477, -0.020022927, 0.007492209, -0.00090844306, -0.010700184, -0.0027465443, 0.004754536, -0.0020539644, -0.006757483, 0.025317531, -0.042646702, 0.006759128, 0.054740362, 0.008250882, 0.0046073906, 0.0036139812, -0.03549706, 0.021130577, 0.006828929, -0.0021696975, 0.0070812055, -0.002950563, -0.010785684, 0.0053102938, 0.024732364, 0.0005054463, -0.009463466, 0.00058466016, 0.01913217, -0.046123926, 0.004099381, 0.0002872889, -0.009742624, 0.07934595, 0.008971758, -0.044807564, 0.032934666, -0.0130616855, 0.042893715, 0.0, -0.05652076, 0.0001768227, 0.015892096, -0.00039065434, -0.019764507, -0.0064509115, 0.041617133, -0.0024496582, -0.009738962, -0.0037766302, 0.059347965, 0.019324142, -0.04831028, 0.0010182589, -0.0035932402, 0.07725186, -0.012348397, -0.000888707, -0.029118203, -0.0012476847, 0.009733933, -0.0015899612, 0.002954369, 0.00097563176, -0.037165143, -0.0014619974, 0.04074392, 0.004665218, 0.014222303, -0.020277845, 0.00023789848, 0.020044547, -0.0006568671, 0.038258757, -0.006738621, -0.01877156, 0.0031751285, -0.0027716074, 0.0038918837, -0.0045367656, 0.024126088, -0.0010834001, 0.0060397424, -0.00019711263, 0.053207103, -0.046372145, -0.007249567, 0.020201366, 0.00063026237, -0.0024504277, 0.0, -0.025623698, 0.026081556, 0.0026610843, 0.08195641, -0.014070673, 0.017456952, -0.0038943712, 0.01441529, 0.0037959935, -3.962708e-05, -0.0002599367, -0.010006913, 4.3176435e-05, 0.00028878148, -0.027744187, 0.0009405044, -0.009326682, 0.002973707, 0.014578586, 0.0025378715, -0.013808471, 0.0010386048, -0.008083755, -0.0008020814, 0.008065958, -0.0005759925, -0.01000639, 0.0058533046, -0.012963097, -0.0063678655, -0.00026994172, -0.0073991637, 0.0, -0.03400921, 0.09104042, -0.0051409546, 0.007422014, 0.0057841185, -0.006905834, 0.0003366733, -0.006027959, 0.0008941353, 0.000519069, -0.0005199342, 0.005894454, -0.008135738, -0.0039718575, 0.0015707628, -0.00075509364, 0.006195928, -0.015468669, 0.0067351935, -0.007824543, 0.016324313, 0.004857269, 0.0007449557, -0.006883453, 0.018816726, -0.047341812, 0.0, -0.00044907376, -0.024142545, 0.0013048442, 0.00924097, -0.00036043677, -0.0075887926, -0.00037642964, 0.016635275, 0.0030533548, -0.00055347156, 0.036696237, -0.0020395438, -0.011763727, 0.00069853663, 0.00029121758, -0.003924451, -1.7108683e-05, -0.012010769, 0.0024333575, -0.014091563, -0.0019432369, 0.08319307, 0.014988385, 0.0025857252, -0.0028567961, 0.024763905, 0.0, 0.0005542842, 0.0012215936, -0.0034549076, 0.010691001, 0.0022397253, -0.0066507044, 3.4034332e-05, -0.0055839876, 0.0015449949, -0.0020639682, 0.013047953, -0.000727456, -0.017532172, -0.0028941687, 0.0027559754, 0.029733252, -0.0012608962, 0.024209188, -0.034218468, 0.027198361, -0.01697907, 0.003973015, 0.0009875128, 0.0016746384, -0.02260061, 0.041699503, -0.0050081983, 0.012105348, -0.007477945, -0.010574549, -0.0032009913, 0.007871424, -0.0005993663, 0.020305945};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 126.0499887471868) / 87.86859846568598;
		data[1] = (ft.getDouble("speed") - 12.740663915978995) / 2.6516169764830786;
		data[2] = (ft.getDouble("num_lanes") - 1.847336834208552) / 0.9858081652916906;
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
        if (input[0] >= -0.22351545) {
            if (input[6] >= 6.5) {
                if (input[0] >= 0.5079745) {
                    if (input[1] >= 0.95765567) {
                        var0 = params[0];
                    } else {
                        var0 = params[1];
                    }
                } else {
                    if (input[1] >= -1.139178) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                }
            } else {
                if (input[1] >= -1.139178) {
                    if (input[0] >= 1.0604472) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                } else {
                    if (input[0] >= 0.60772574) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                }
            }
        } else {
            if (input[6] >= 6.5) {
                if (input[5] >= 2.5) {
                    if (input[1] >= -0.6149696) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                } else {
                    if (input[0] >= -0.6765214) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                }
            } else {
                if (input[0] >= -0.688528) {
                    if (input[1] >= -0.6149696) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                } else {
                    if (input[1] >= -0.6149696) {
                        var0 = params[14];
                    } else {
                        var0 = params[15];
                    }
                }
            }
        }
        double var1;
        if (input[6] >= 6.5) {
            if (input[6] >= 10.5) {
                if (input[1] >= 2.0041869) {
                    var1 = params[16];
                } else {
                    if (input[11] >= 0.5) {
                        var1 = params[17];
                    } else {
                        var1 = params[18];
                    }
                }
            } else {
                if (input[1] >= -0.6149696) {
                    if (input[10] >= 0.5) {
                        var1 = params[19];
                    } else {
                        var1 = params[20];
                    }
                } else {
                    if (input[3] >= 2.78) {
                        var1 = params[21];
                    } else {
                        var1 = params[22];
                    }
                }
            }
        } else {
            if (input[1] >= -0.090761185) {
                if (input[6] >= 4.5) {
                    if (input[9] >= 0.5) {
                        var1 = params[23];
                    } else {
                        var1 = params[24];
                    }
                } else {
                    if (input[1] >= 0.95765567) {
                        var1 = params[25];
                    } else {
                        var1 = params[26];
                    }
                }
            } else {
                if (input[6] >= 4.5) {
                    if (input[11] >= 0.5) {
                        var1 = params[27];
                    } else {
                        var1 = params[28];
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var1 = params[29];
                    } else {
                        var1 = params[30];
                    }
                }
            }
        }
        double var2;
        if (input[0] >= -0.33362305) {
            if (input[1] >= 0.95765567) {
                if (input[3] >= 1.385) {
                    if (input[2] >= 1.676455) {
                        var2 = params[31];
                    } else {
                        var2 = params[32];
                    }
                } else {
                    if (input[4] >= -1.5) {
                        var2 = params[33];
                    } else {
                        var2 = params[34];
                    }
                }
            } else {
                if (input[0] >= 0.22510898) {
                    if (input[3] >= 5.5550003) {
                        var2 = params[35];
                    } else {
                        var2 = params[36];
                    }
                } else {
                    if (input[3] >= 5.5550003) {
                        var2 = params[37];
                    } else {
                        var2 = params[38];
                    }
                }
            }
        } else {
            if (input[1] >= -0.6149696) {
                if (input[0] >= -0.5709661) {
                    if (input[13] >= 0.5) {
                        var2 = params[39];
                    } else {
                        var2 = params[40];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var2 = params[41];
                    } else {
                        var2 = params[42];
                    }
                }
            } else {
                if (input[0] >= -0.89246887) {
                    if (input[3] >= 2.78) {
                        var2 = params[43];
                    } else {
                        var2 = params[44];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var2 = params[45];
                    } else {
                        var2 = params[46];
                    }
                }
            }
        }
        double var3;
        if (input[4] >= -0.5) {
            if (input[6] >= 8.5) {
                if (input[11] >= 0.5) {
                    if (input[2] >= 1.676455) {
                        var3 = params[47];
                    } else {
                        var3 = params[48];
                    }
                } else {
                    if (input[4] >= 2.5) {
                        var3 = params[49];
                    } else {
                        var3 = params[50];
                    }
                }
            } else {
                if (input[6] >= 4.5) {
                    if (input[1] >= -1.139178) {
                        var3 = params[51];
                    } else {
                        var3 = params[52];
                    }
                } else {
                    if (input[1] >= -1.139178) {
                        var3 = params[53];
                    } else {
                        var3 = params[54];
                    }
                }
            }
        } else {
            if (input[6] >= 10.5) {
                if (input[5] >= 2.5) {
                    if (input[1] >= -0.6149696) {
                        var3 = params[55];
                    } else {
                        var3 = params[56];
                    }
                } else {
                    if (input[2] >= 1.676455) {
                        var3 = params[57];
                    } else {
                        var3 = params[58];
                    }
                }
            } else {
                if (input[5] >= 2.5) {
                    if (input[1] >= 2.0041869) {
                        var3 = params[59];
                    } else {
                        var3 = params[60];
                    }
                } else {
                    if (input[2] >= 0.662059) {
                        var3 = params[61];
                    } else {
                        var3 = params[62];
                    }
                }
            }
        }
        double var4;
        if (input[0] >= -0.16536042) {
            if (input[0] >= 1.4692395) {
                if (input[3] >= -6.95) {
                    if (input[0] >= 2.7091022) {
                        var4 = params[63];
                    } else {
                        var4 = params[64];
                    }
                } else {
                    var4 = params[65];
                }
            } else {
                if (input[0] >= 0.3859742) {
                    if (input[2] >= 0.662059) {
                        var4 = params[66];
                    } else {
                        var4 = params[67];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var4 = params[68];
                    } else {
                        var4 = params[69];
                    }
                }
            }
        } else {
            if (input[0] >= -0.8314687) {
                if (input[5] >= 2.5) {
                    if (input[0] >= -0.49215522) {
                        var4 = params[70];
                    } else {
                        var4 = params[71];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var4 = params[72];
                    } else {
                        var4 = params[73];
                    }
                }
            } else {
                if (input[5] >= 2.5) {
                    if (input[0] >= -0.9568832) {
                        var4 = params[74];
                    } else {
                        var4 = params[75];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var4 = params[76];
                    } else {
                        var4 = params[77];
                    }
                }
            }
        }
        double var5;
        if (input[6] >= 8.5) {
            if (input[0] >= -0.111302435) {
                if (input[1] >= 2.0041869) {
                    var5 = params[78];
                } else {
                    if (input[1] >= -1.139178) {
                        var5 = params[79];
                    } else {
                        var5 = params[80];
                    }
                }
            } else {
                if (input[0] >= -0.6630354) {
                    if (input[2] >= 1.676455) {
                        var5 = params[81];
                    } else {
                        var5 = params[82];
                    }
                } else {
                    if (input[2] >= 2.6908512) {
                        var5 = params[83];
                    } else {
                        var5 = params[84];
                    }
                }
            }
        } else {
            if (input[0] >= -0.43536586) {
                if (input[0] >= 1.5625606) {
                    if (input[1] >= 2.0041869) {
                        var5 = params[85];
                    } else {
                        var5 = params[86];
                    }
                } else {
                    if (input[2] >= 0.662059) {
                        var5 = params[87];
                    } else {
                        var5 = params[88];
                    }
                }
            } else {
                if (input[1] >= 0.95765567) {
                    if (input[6] >= 2.5) {
                        var5 = params[89];
                    } else {
                        var5 = params[90];
                    }
                } else {
                    if (input[2] >= 0.662059) {
                        var5 = params[91];
                    } else {
                        var5 = params[92];
                    }
                }
            }
        }
        double var6;
        if (input[1] >= 2.0041869) {
            if (input[0] >= -0.088370465) {
                if (input[5] >= 2.5) {
                    if (input[2] >= 0.662059) {
                        var6 = params[93];
                    } else {
                        var6 = params[94];
                    }
                } else {
                    var6 = params[95];
                }
            } else {
                if (input[0] >= -0.63327503) {
                    var6 = params[96];
                } else {
                    if (input[0] >= -1.0539031) {
                        var6 = params[97];
                    } else {
                        var6 = params[98];
                    }
                }
            }
        } else {
            if (input[0] >= -0.61278987) {
                if (input[0] >= 0.7613643) {
                    if (input[2] >= -0.35233715) {
                        var6 = params[99];
                    } else {
                        var6 = params[100];
                    }
                } else {
                    if (input[1] >= -1.139178) {
                        var6 = params[101];
                    } else {
                        var6 = params[102];
                    }
                }
            } else {
                if (input[5] >= 2.5) {
                    if (input[0] >= -0.9566556) {
                        var6 = params[103];
                    } else {
                        var6 = params[104];
                    }
                } else {
                    if (input[6] >= 5.5) {
                        var6 = params[105];
                    } else {
                        var6 = params[106];
                    }
                }
            }
        }
        double var7;
        if (input[0] >= -0.063674495) {
            if (input[1] >= 0.95765567) {
                if (input[0] >= 2.1382499) {
                    if (input[4] >= 0.5) {
                        var7 = params[107];
                    } else {
                        var7 = params[108];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var7 = params[109];
                    } else {
                        var7 = params[110];
                    }
                }
            } else {
                if (input[0] >= 0.99159443) {
                    if (input[0] >= 1.085257) {
                        var7 = params[111];
                    } else {
                        var7 = params[112];
                    }
                } else {
                    if (input[5] >= 3.5) {
                        var7 = params[113];
                    } else {
                        var7 = params[114];
                    }
                }
            }
        } else {
            if (input[1] >= -1.139178) {
                if (input[3] >= 4.165) {
                    if (input[0] >= -0.097759485) {
                        var7 = params[115];
                    } else {
                        var7 = params[116];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var7 = params[117];
                    } else {
                        var7 = params[118];
                    }
                }
            } else {
                if (input[3] >= 5.5550003) {
                    if (input[5] >= 1.5) {
                        var7 = params[119];
                    } else {
                        var7 = params[120];
                    }
                } else {
                    if (input[0] >= -1.0416689) {
                        var7 = params[121];
                    } else {
                        var7 = params[122];
                    }
                }
            }
        }
        double var8;
        if (input[6] >= 4.5) {
            if (input[1] >= 2.0041869) {
                var8 = params[123];
            } else {
                if (input[2] >= -0.35233715) {
                    if (input[5] >= 2.5) {
                        var8 = params[124];
                    } else {
                        var8 = params[125];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var8 = params[126];
                    } else {
                        var8 = params[127];
                    }
                }
            }
        } else {
            if (input[2] >= -0.35233715) {
                if (input[8] >= 0.5) {
                    if (input[5] >= 3.5) {
                        var8 = params[128];
                    } else {
                        var8 = params[129];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var8 = params[130];
                    } else {
                        var8 = params[131];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[6] >= 2.5) {
                        var8 = params[132];
                    } else {
                        var8 = params[133];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var8 = params[134];
                    } else {
                        var8 = params[135];
                    }
                }
            }
        }
        double var9;
        if (input[0] >= -0.7943678) {
            if (input[5] >= 3.5) {
                if (input[6] >= 4.5) {
                    var9 = params[136];
                } else {
                    if (input[8] >= 0.5) {
                        var9 = params[137];
                    } else {
                        var9 = params[138];
                    }
                }
            } else {
                if (input[6] >= 11.5) {
                    if (input[0] >= 0.18760982) {
                        var9 = params[139];
                    } else {
                        var9 = params[140];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var9 = params[141];
                    } else {
                        var9 = params[142];
                    }
                }
            }
        } else {
            if (input[4] >= 0.5) {
                if (input[6] >= 6.5) {
                    if (input[0] >= -1.1542233) {
                        var9 = params[143];
                    } else {
                        var9 = params[144];
                    }
                } else {
                    if (input[0] >= -1.041043) {
                        var9 = params[145];
                    } else {
                        var9 = params[146];
                    }
                }
            } else {
                if (input[4] >= -0.5) {
                    if (input[0] >= -1.2862387) {
                        var9 = params[147];
                    } else {
                        var9 = params[148];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var9 = params[149];
                    } else {
                        var9 = params[150];
                    }
                }
            }
        }
        double var10;
        if (input[0] >= -0.7211335) {
            if (input[1] >= 2.0041869) {
                if (input[0] >= -0.05747205) {
                    if (input[2] >= -0.35233715) {
                        var10 = params[151];
                    } else {
                        var10 = params[152];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var10 = params[153];
                    } else {
                        var10 = params[154];
                    }
                }
            } else {
                if (input[0] >= 2.035767) {
                    if (input[6] >= 6.5) {
                        var10 = params[155];
                    } else {
                        var10 = params[156];
                    }
                } else {
                    if (input[2] >= 0.662059) {
                        var10 = params[157];
                    } else {
                        var10 = params[158];
                    }
                }
            }
        } else {
            if (input[1] >= -0.6149696) {
                if (input[0] >= -0.72602713) {
                    var10 = params[159];
                } else {
                    if (input[2] >= 0.662059) {
                        var10 = params[160];
                    } else {
                        var10 = params[161];
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[2] >= -0.35233715) {
                        var10 = params[162];
                    } else {
                        var10 = params[163];
                    }
                } else {
                    if (input[6] >= 7.5) {
                        var10 = params[164];
                    } else {
                        var10 = params[165];
                    }
                }
            }
        }
        double var11;
        if (input[1] >= -1.139178) {
            if (input[3] >= 1.385) {
                if (input[5] >= 2.5) {
                    var11 = params[166];
                } else {
                    if (input[4] >= -1.5) {
                        var11 = params[167];
                    } else {
                        var11 = params[168];
                    }
                }
            } else {
                if (input[5] >= 2.5) {
                    if (input[6] >= 4.5) {
                        var11 = params[169];
                    } else {
                        var11 = params[170];
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var11 = params[171];
                    } else {
                        var11 = params[172];
                    }
                }
            }
        } else {
            if (input[5] >= 2.5) {
                if (input[5] >= 3.5) {
                    var11 = params[173];
                } else {
                    if (input[3] >= 9.725) {
                        var11 = params[174];
                    } else {
                        var11 = params[175];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[4] >= 0.5) {
                        var11 = params[176];
                    } else {
                        var11 = params[177];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var11 = params[178];
                    } else {
                        var11 = params[179];
                    }
                }
            }
        }
        double var12;
        if (input[6] >= 3.5) {
            if (input[1] >= -0.090761185) {
                if (input[2] >= -0.35233715) {
                    if (input[0] >= 0.17856222) {
                        var12 = params[180];
                    } else {
                        var12 = params[181];
                    }
                } else {
                    if (input[6] >= 4.5) {
                        var12 = params[182];
                    } else {
                        var12 = params[183];
                    }
                }
            } else {
                if (input[0] >= -0.9367964) {
                    if (input[5] >= 2.5) {
                        var12 = params[184];
                    } else {
                        var12 = params[185];
                    }
                } else {
                    if (input[0] >= -1.2581854) {
                        var12 = params[186];
                    } else {
                        var12 = params[187];
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[0] >= -1.0618126) {
                    if (input[5] >= 2.5) {
                        var12 = params[188];
                    } else {
                        var12 = params[189];
                    }
                } else {
                    if (input[0] >= -1.1441514) {
                        var12 = params[190];
                    } else {
                        var12 = params[191];
                    }
                }
            } else {
                if (input[0] >= -1.0707464) {
                    var12 = params[192];
                } else {
                    if (input[2] >= -0.35233715) {
                        var12 = params[193];
                    } else {
                        var12 = params[194];
                    }
                }
            }
        }
        double var13;
        if (input[6] >= 5.5) {
            if (input[0] >= 1.4313989) {
                if (input[4] >= 1.5) {
                    var13 = params[195];
                } else {
                    var13 = params[196];
                }
            } else {
                if (input[2] >= 0.662059) {
                    if (input[6] >= 8.5) {
                        var13 = params[197];
                    } else {
                        var13 = params[198];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var13 = params[199];
                    } else {
                        var13 = params[200];
                    }
                }
            }
        } else {
            if (input[4] >= -0.5) {
                if (input[2] >= -0.35233715) {
                    if (input[0] >= -1.0624385) {
                        var13 = params[201];
                    } else {
                        var13 = params[202];
                    }
                } else {
                    if (input[0] >= -0.98214823) {
                        var13 = params[203];
                    } else {
                        var13 = params[204];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[5] >= 2.5) {
                        var13 = params[205];
                    } else {
                        var13 = params[206];
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var13 = params[207];
                    } else {
                        var13 = params[208];
                    }
                }
            }
        }
        double var14;
        if (input[0] >= -1.2804346) {
            if (input[0] >= -1.1680508) {
                if (input[0] >= -1.1546786) {
                    if (input[3] >= -1.39) {
                        var14 = params[209];
                    } else {
                        var14 = params[210];
                    }
                } else {
                    if (input[0] >= -1.1575238) {
                        var14 = params[211];
                    } else {
                        var14 = params[212];
                    }
                }
            } else {
                if (input[0] >= -1.1767571) {
                    var14 = params[213];
                } else {
                    if (input[6] >= 8.5) {
                        var14 = params[214];
                    } else {
                        var14 = params[215];
                    }
                }
            }
        } else {
            if (input[6] >= 7.5) {
                if (input[1] >= -0.6149696) {
                    var14 = params[216];
                } else {
                    var14 = params[217];
                }
            } else {
                if (input[4] >= 0.5) {
                    if (input[6] >= 6.5) {
                        var14 = params[218];
                    } else {
                        var14 = params[219];
                    }
                } else {
                    var14 = params[220];
                }
            }
        }
        double var15;
        if (input[1] >= 0.95765567) {
            if (input[6] >= 2.5) {
                if (input[6] >= 8.5) {
                    var15 = params[221];
                } else {
                    if (input[3] >= 1.385) {
                        var15 = params[222];
                    } else {
                        var15 = params[223];
                    }
                }
            } else {
                var15 = params[224];
            }
        } else {
            if (input[6] >= 9.5) {
                if (input[0] >= -1.1949091) {
                    if (input[5] >= 2.5) {
                        var15 = params[225];
                    } else {
                        var15 = params[226];
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var15 = params[227];
                    } else {
                        var15 = params[228];
                    }
                }
            } else {
                if (input[2] >= 1.676455) {
                    if (input[0] >= -1.1116598) {
                        var15 = params[229];
                    } else {
                        var15 = params[230];
                    }
                } else {
                    if (input[0] >= -0.92131877) {
                        var15 = params[231];
                    } else {
                        var15 = params[232];
                    }
                }
            }
        }
        double var16;
        if (input[1] >= -0.090761185) {
            if (input[3] >= 4.165) {
                if (input[8] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var16 = params[233];
                    } else {
                        var16 = params[234];
                    }
                } else {
                    if (input[1] >= 2.0041869) {
                        var16 = params[235];
                    } else {
                        var16 = params[236];
                    }
                }
            } else {
                if (input[2] >= 1.676455) {
                    if (input[6] >= 11.5) {
                        var16 = params[237];
                    } else {
                        var16 = params[238];
                    }
                } else {
                    if (input[6] >= 3.5) {
                        var16 = params[239];
                    } else {
                        var16 = params[240];
                    }
                }
            }
        } else {
            if (input[3] >= 5.5550003) {
                if (input[5] >= 1.5) {
                    var16 = params[241];
                } else {
                    if (input[6] >= 5.5) {
                        var16 = params[242];
                    } else {
                        var16 = params[243];
                    }
                }
            } else {
                if (input[3] >= 1.39) {
                    var16 = params[244];
                } else {
                    var16 = params[245];
                }
            }
        }
        double var17;
        if (input[4] >= 1.5) {
            if (input[9] >= 0.5) {
                if (input[6] >= 7.5) {
                    var17 = params[246];
                } else {
                    var17 = params[247];
                }
            } else {
                if (input[0] >= 0.053830504) {
                    if (input[8] >= 0.5) {
                        var17 = params[248];
                    } else {
                        var17 = params[249];
                    }
                } else {
                    if (input[0] >= -1.1640108) {
                        var17 = params[250];
                    } else {
                        var17 = params[251];
                    }
                }
            }
        } else {
            if (input[6] >= 7.5) {
                if (input[3] >= 1.385) {
                    if (input[5] >= 2.5) {
                        var17 = params[252];
                    } else {
                        var17 = params[253];
                    }
                } else {
                    if (input[2] >= 1.676455) {
                        var17 = params[254];
                    } else {
                        var17 = params[255];
                    }
                }
            } else {
                if (input[2] >= -0.35233715) {
                    if (input[0] >= -0.9569402) {
                        var17 = params[256];
                    } else {
                        var17 = params[257];
                    }
                } else {
                    if (input[3] >= 9.725) {
                        var17 = params[258];
                    } else {
                        var17 = params[259];
                    }
                }
            }
        }
        double var18;
        if (input[0] >= 2.3857217) {
            var18 = params[260];
        } else {
            if (input[0] >= -1.2804346) {
                if (input[0] >= -1.1570685) {
                    if (input[0] >= -1.1546786) {
                        var18 = params[261];
                    } else {
                        var18 = params[262];
                    }
                } else {
                    if (input[3] >= 5.5550003) {
                        var18 = params[263];
                    } else {
                        var18 = params[264];
                    }
                }
            } else {
                if (input[0] >= -1.3070652) {
                    var18 = params[265];
                } else {
                    var18 = params[266];
                }
            }
        }
        double var19;
        if (input[1] >= 0.95765567) {
            if (input[6] >= 2.5) {
                if (input[0] >= -0.7105495) {
                    var19 = params[267];
                } else {
                    if (input[4] >= -0.5) {
                        var19 = params[268];
                    } else {
                        var19 = params[269];
                    }
                }
            } else {
                var19 = params[270];
            }
        } else {
            if (input[2] >= 0.662059) {
                if (input[0] >= -1.1253735) {
                    if (input[0] >= -1.1170657) {
                        var19 = params[271];
                    } else {
                        var19 = params[272];
                    }
                } else {
                    if (input[4] >= -1.5) {
                        var19 = params[273];
                    } else {
                        var19 = params[274];
                    }
                }
            } else {
                if (input[6] >= 6.5) {
                    if (input[0] >= -1.0170867) {
                        var19 = params[275];
                    } else {
                        var19 = params[276];
                    }
                } else {
                    if (input[4] >= 0.5) {
                        var19 = params[277];
                    } else {
                        var19 = params[278];
                    }
                }
            }
        }
        double var20;
        if (input[6] >= 9.5) {
            if (input[9] >= 0.5) {
                if (input[1] >= -0.090761185) {
                    if (input[2] >= 1.676455) {
                        var20 = params[279];
                    } else {
                        var20 = params[280];
                    }
                } else {
                    var20 = params[281];
                }
            } else {
                if (input[3] >= -2.78) {
                    var20 = params[282];
                } else {
                    var20 = params[283];
                }
            }
        } else {
            if (input[2] >= -0.35233715) {
                if (input[6] >= 5.5) {
                    if (input[0] >= -1.0520822) {
                        var20 = params[284];
                    } else {
                        var20 = params[285];
                    }
                } else {
                    if (input[0] >= -1.0618126) {
                        var20 = params[286];
                    } else {
                        var20 = params[287];
                    }
                }
            } else {
                if (input[6] >= 4.5) {
                    if (input[4] >= 0.5) {
                        var20 = params[288];
                    } else {
                        var20 = params[289];
                    }
                } else {
                    if (input[0] >= -1.1441514) {
                        var20 = params[290];
                    } else {
                        var20 = params[291];
                    }
                }
            }
        }
        double var21;
        if (input[2] >= 0.662059) {
            if (input[6] >= 8.5) {
                if (input[0] >= -0.57517695) {
                    if (input[5] >= 2.5) {
                        var21 = params[292];
                    } else {
                        var21 = params[293];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var21 = params[294];
                    } else {
                        var21 = params[295];
                    }
                }
            } else {
                if (input[0] >= -1.1248614) {
                    if (input[3] >= -1.39) {
                        var21 = params[296];
                    } else {
                        var21 = params[297];
                    }
                } else {
                    var21 = params[298];
                }
            }
        } else {
            if (input[0] >= -1.1234956) {
                if (input[0] >= -1.1026123) {
                    if (input[5] >= 1.5) {
                        var21 = params[299];
                    } else {
                        var21 = params[300];
                    }
                } else {
                    if (input[3] >= 2.78) {
                        var21 = params[301];
                    } else {
                        var21 = params[302];
                    }
                }
            } else {
                if (input[0] >= -1.1262839) {
                    var21 = params[303];
                } else {
                    if (input[5] >= 2.5) {
                        var21 = params[304];
                    } else {
                        var21 = params[305];
                    }
                }
            }
        }
        double var22;
        if (input[0] >= 2.7776704) {
            var22 = params[306];
        } else {
            if (input[1] >= -0.090761185) {
                if (input[0] >= -0.8556526) {
                    if (input[3] >= 1.385) {
                        var22 = params[307];
                    } else {
                        var22 = params[308];
                    }
                } else {
                    if (input[0] >= -1.0408154) {
                        var22 = params[309];
                    } else {
                        var22 = params[310];
                    }
                }
            } else {
                if (input[3] >= 5.5550003) {
                    var22 = params[311];
                } else {
                    if (input[0] >= 0.19620219) {
                        var22 = params[312];
                    } else {
                        var22 = params[313];
                    }
                }
            }
        }
        double var23;
        if (input[4] >= -1.5) {
            if (input[5] >= 3.5) {
                var23 = params[314];
            } else {
                if (input[2] >= -0.35233715) {
                    if (input[4] >= 0.5) {
                        var23 = params[315];
                    } else {
                        var23 = params[316];
                    }
                } else {
                    if (input[0] >= -1.1116598) {
                        var23 = params[317];
                    } else {
                        var23 = params[318];
                    }
                }
            }
        } else {
            if (input[4] >= -2.5) {
                if (input[0] >= -0.19483626) {
                    var23 = params[319];
                } else {
                    if (input[0] >= -0.44794148) {
                        var23 = params[320];
                    } else {
                        var23 = params[321];
                    }
                }
            } else {
                var23 = params[322];
            }
        }
        double var24;
        if (input[0] >= 0.79379904) {
            if (input[0] >= 0.82094187) {
                if (input[4] >= 0.5) {
                    var24 = params[323];
                } else {
                    if (input[0] >= 0.90902793) {
                        var24 = params[324];
                    } else {
                        var24 = params[325];
                    }
                }
            } else {
                var24 = params[326];
            }
        } else {
            if (input[0] >= 0.78657234) {
                if (input[1] >= -0.6149696) {
                    var24 = params[327];
                } else {
                    var24 = params[328];
                }
            } else {
                if (input[3] >= -4.165) {
                    var24 = params[329];
                } else {
                    if (input[7] >= 0.5) {
                        var24 = params[330];
                    } else {
                        var24 = params[331];
                    }
                }
            }
        }
        double var25;
        if (input[4] >= 1.5) {
            if (input[3] >= 4.17) {
                var25 = params[332];
            } else {
                var25 = params[333];
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[0] >= -0.54939973) {
                    if (input[4] >= 0.5) {
                        var25 = params[334];
                    } else {
                        var25 = params[335];
                    }
                } else {
                    if (input[3] >= 1.39) {
                        var25 = params[336];
                    } else {
                        var25 = params[337];
                    }
                }
            } else {
                if (input[0] >= -1.2840763) {
                    var25 = params[338];
                } else {
                    if (input[5] >= 2.5) {
                        var25 = params[339];
                    } else {
                        var25 = params[340];
                    }
                }
            }
        }
        double var26;
        if (input[6] >= 3.5) {
            if (input[4] >= -1.5) {
                if (input[2] >= -0.35233715) {
                    if (input[3] >= 6.9449997) {
                        var26 = params[341];
                    } else {
                        var26 = params[342];
                    }
                } else {
                    if (input[5] >= 2.5) {
                        var26 = params[343];
                    } else {
                        var26 = params[344];
                    }
                }
            } else {
                if (input[2] >= 1.676455) {
                    var26 = params[345];
                } else {
                    if (input[6] >= 6.5) {
                        var26 = params[346];
                    } else {
                        var26 = params[347];
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                var26 = params[348];
            } else {
                if (input[7] >= 0.5) {
                    if (input[4] >= -0.5) {
                        var26 = params[349];
                    } else {
                        var26 = params[350];
                    }
                } else {
                    if (input[3] >= 5.5550003) {
                        var26 = params[351];
                    } else {
                        var26 = params[352];
                    }
                }
            }
        }
        double var27;
        if (input[4] >= 2.5) {
            if (input[6] >= 10.5) {
                var27 = params[353];
            } else {
                if (input[6] >= 6.5) {
                    var27 = params[354];
                } else {
                    var27 = params[355];
                }
            }
        } else {
            if (input[6] >= 5.5) {
                if (input[2] >= 0.662059) {
                    var27 = params[356];
                } else {
                    if (input[4] >= 0.5) {
                        var27 = params[357];
                    } else {
                        var27 = params[358];
                    }
                }
            } else {
                if (input[2] >= -0.35233715) {
                    if (input[7] >= 0.5) {
                        var27 = params[359];
                    } else {
                        var27 = params[360];
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var27 = params[361];
                    } else {
                        var27 = params[362];
                    }
                }
            }
        }
        double var28;
        if (input[6] >= 4.5) {
            if (input[0] >= -0.72750664) {
                if (input[1] >= 2.0041869) {
                    var28 = params[363];
                } else {
                    if (input[2] >= -0.35233715) {
                        var28 = params[364];
                    } else {
                        var28 = params[365];
                    }
                }
            } else {
                if (input[4] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var28 = params[366];
                    } else {
                        var28 = params[367];
                    }
                } else {
                    if (input[0] >= -0.7412203) {
                        var28 = params[368];
                    } else {
                        var28 = params[369];
                    }
                }
            }
        } else {
            if (input[0] >= -0.9906837) {
                if (input[5] >= 2.5) {
                    if (input[0] >= -0.9367964) {
                        var28 = params[370];
                    } else {
                        var28 = params[371];
                    }
                } else {
                    if (input[0] >= -0.94550264) {
                        var28 = params[372];
                    } else {
                        var28 = params[373];
                    }
                }
            } else {
                if (input[4] >= 0.5) {
                    if (input[0] >= -1.053391) {
                        var28 = params[374];
                    } else {
                        var28 = params[375];
                    }
                } else {
                    if (input[5] >= 1.5) {
                        var28 = params[376];
                    } else {
                        var28 = params[377];
                    }
                }
            }
        }
        double var29;
        if (input[0] >= 0.025378931) {
            if (input[0] >= 0.032947052) {
                if (input[3] >= -1.39) {
                    var29 = params[378];
                } else {
                    if (input[0] >= 0.7686479) {
                        var29 = params[379];
                    } else {
                        var29 = params[380];
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    var29 = params[381];
                } else {
                    var29 = params[382];
                }
            }
        } else {
            if (input[0] >= -0.22419828) {
                if (input[11] >= 0.5) {
                    if (input[0] >= -0.14794806) {
                        var29 = params[383];
                    } else {
                        var29 = params[384];
                    }
                } else {
                    if (input[6] >= 6.5) {
                        var29 = params[385];
                    } else {
                        var29 = params[386];
                    }
                }
            } else {
                if (input[4] >= -2.5) {
                    if (input[0] >= -0.28901097) {
                        var29 = params[387];
                    } else {
                        var29 = params[388];
                    }
                } else {
                    var29 = params[389];
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
