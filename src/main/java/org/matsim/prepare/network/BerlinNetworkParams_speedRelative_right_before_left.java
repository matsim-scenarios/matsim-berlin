package org.matsim.prepare.network;
import org.matsim.application.prepare.network.params.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public final class BerlinNetworkParams_speedRelative_right_before_left implements FeatureRegressor {

    public static BerlinNetworkParams_speedRelative_right_before_left INSTANCE = new BerlinNetworkParams_speedRelative_right_before_left();
    public static final double[] DEFAULT_PARAMS = {0.8501698970794678, 0.9006523489952087, 0.8484615087509155, 0.9332157373428345, 0.9654031991958618, 0.7838205695152283, 0.9256957173347473, 0.9549677968025208, 0.8052073121070862, 0.8382558226585388, 0.9484443068504333, 0.9078091979026794, 0.9063796997070312, 0.8464484214782715, 0.8108808994293213, 0.9163745641708374, 0.941914439201355, 0.949999988079071, 0.8465233445167542, 0.8688034415245056, 0.8775915503501892, 0.9402863383293152, 0.9495895504951477, 0.8825960755348206, 0.8238087296485901, 0.9011247158050537, 0.8459143042564392, 0.8655627965927124, 0.8199752569198608, 0.8992147445678711, 0.7966489195823669, 0.8579999804496765, 0.8710649609565735, 0.7907753586769104, 0.9156076908111572, 0.8679876327514648, 0.9876474142074585, 0.794172465801239, 0.9140607714653015, 0.9037666320800781, 0.8168045878410339, 0.8536452651023865, 1.0151387453079224, 0.8642212152481079, 1.0296967029571533, 0.8043543696403503, 0.7827005386352539, 0.8400399684906006, 0.9112281799316406, 0.9027798771858215, 0.8668215870857239, 0.9703286290168762, 0.8510969877243042, 0.9588707685470581, 0.8380865454673767, 0.9060183167457581, 0.8086714148521423, 0.8899999856948853, 0.8702351450920105, 0.8167619705200195, 0.881017804145813, 0.8917708396911621, 0.8898671865463257, 0.9124057292938232, 0.9038119912147522, 0.8082835674285889, 0.9032228589057922, 0.9692773818969727, 0.7851191163063049, 0.9151013493537903, 0.8872057795524597, 0.8399999737739563, 0.8969735503196716, 0.8183333277702332, 0.7989745736122131, 0.9540218710899353, 0.8480196595191956, 0.8248308897018433, 0.8740478754043579, 0.7323729395866394, 0.9444176554679871, 0.8704118132591248, 0.96578049659729, 0.7970685362815857, 0.9479656219482422, 0.826766848564148, 0.7914180159568787, 0.8081436157226562, 0.8508738875389099, 0.9340909123420715, 0.9815088510513306, 0.9774714112281799, 0.8721084594726562, 0.8936017155647278, 0.85148686170578, 0.9024903178215027, 0.8605471849441528, 0.9828650951385498, 0.9500955939292908, 0.7599999904632568, 0.9269111156463623, 0.8720079660415649, 0.8653984069824219, 0.8197725415229797, 0.9061167240142822, 0.9564887285232544, 0.8769751191139221, 0.8558287024497986, 0.8147779107093811, 0.8483192324638367, 0.9398745894432068, 0.893629789352417, 0.8337939977645874, 0.964969277381897, 0.7900000214576721, 0.9434866905212402, 0.8190807104110718, 0.8359490036964417, 0.8498650789260864, 0.8630268573760986, 0.8285882472991943, 0.8156688809394836, 0.8775268793106079, 0.9993680119514465, 0.8820398449897766, 0.9098674654960632, 0.8552159070968628, 0.8126517534255981, 0.8735414743423462, 0.8983948826789856, 0.9142025709152222, 0.9788086414337158, 0.8658783435821533, 0.8190967440605164, 0.8848844170570374, 0.8562158346176147, 0.909334123134613, 0.8601728677749634, 0.7592419981956482, 1.0365158319473267, 0.8781071901321411, 0.847541868686676, 0.9003839492797852, 0.9481457471847534, 0.8890439867973328, 0.9426897168159485, 0.9181755781173706, 0.9372392892837524, 0.9060111045837402, 0.815224289894104, 0.7869310975074768, 0.9040620923042297, 0.8649681210517883, 0.8579493761062622, 1.0132747888565063, 0.8378110527992249, 0.9725226163864136, 0.8364429473876953, 0.8697134852409363, 0.9064249396324158, 0.9608400464057922, 0.9030491709709167, 0.8059999942779541, 0.7702260613441467, 0.9495320916175842, 0.7942577600479126, 0.8006508350372314, 0.8567498326301575, 0.9633333086967468, 0.815035343170166, 0.7993671894073486, 0.8521023392677307, 0.9075300097465515, 0.8529704213142395, 0.8750404715538025, 0.8677250742912292, 0.9662194848060608, 0.9261354804039001, 0.8400701284408569, 0.8164852857589722, 0.7565231323242188, 0.9053943753242493, 0.8107093572616577, 0.7893573641777039, 0.8567478060722351, 0.9325000047683716, 0.8400388360023499, 0.8383146524429321, 0.9119933843612671, 0.8517348170280457, 0.8734019994735718, 0.9788123369216919, 0.7063427567481995, 0.9700000286102295, 0.9358868598937988, 0.8132143020629883, 0.9118583798408508, 0.7074904441833496, 0.7708765268325806, 0.9016913771629333, 0.8006144762039185, 1.010364055633545, 0.8237125873565674, 0.8798007369041443, 0.8935883641242981, 0.9630178809165955, 0.9531705975532532, 0.75, 0.8560128808021545, 0.8395475149154663, 0.8837317228317261, 0.8818429708480835, 0.9545405507087708, 0.8352820873260498, 0.7400753498077393, 0.9284844994544983, 0.8388035297393799, 0.8536258339881897, 0.945603609085083, 0.8691257238388062, 0.9723203182220459, 0.9076040387153625, 0.949999988079071, 0.8989589810371399, 0.7738989591598511, 0.7819089293479919, 0.7974745035171509, 1.0653083324432373, 0.9143388271331787, 0.8224573731422424, 0.9618649482727051, 0.7823584675788879};

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
