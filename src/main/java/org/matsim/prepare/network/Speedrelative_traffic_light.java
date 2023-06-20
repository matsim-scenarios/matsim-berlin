package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public class Speedrelative_traffic_light implements FeatureRegressor {
    
    @Override
    public double predict(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 126.82555102040814) / 90.04318134479612;
		data[1] = (ft.getDouble("speed") - 13.172408163265308) / 2.6013146347313705;
		data[2] = (ft.getDouble("numFoes") - 2.419954648526077) / 0.6588390791260084;
		data[3] = (ft.getDouble("numLanes") - 1.900907029478458) / 0.9894793944707013;
		data[4] = (ft.getDouble("junctionSize") - 13.878231292517007) / 4.383374335807856;
		data[5] = ft.getDouble("dir_l");
		data[6] = ft.getDouble("dir_r");
		data[7] = ft.getDouble("dir_s");
		data[8] = ft.getDouble("dir_multiple_s");
		data[9] = ft.getDouble("dir_exclusive");
		data[10] = ft.getDouble("priority_lower");
		data[11] = ft.getDouble("priority_equal");
		data[12] = ft.getDouble("priority_higher");
		data[13] = ft.getDouble("changeNumLanes");

        return score(data);
    }
    public static double score(double[] input) {
        double var0;
        if (input[0] >= -0.2012429) {
            if (input[0] >= 0.4387278) {
                if (input[0] >= 1.0238359) {
                    if (input[1] >= 0.8102026) {
                        var0 = 0.03971167;
                    } else {
                        var0 = 0.121152446;
                    }
                } else {
                    if (input[1] >= -0.792833) {
                        var0 = 0.078283235;
                    } else {
                        var0 = 0.12317744;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[1] >= -1.3271782) {
                        var0 = 0.05196501;
                    } else {
                        var0 = 0.104065135;
                    }
                } else {
                    if (input[4] >= 0.3699818) {
                        var0 = -0.029283153;
                    } else {
                        var0 = 0.05238769;
                    }
                }
            }
        } else {
            if (input[1] >= -1.3271782) {
                if (input[6] >= 0.5) {
                    if (input[0] >= -0.588446) {
                        var0 = -0.028489934;
                    } else {
                        var0 = -0.07390235;
                    }
                } else {
                    if (input[0] >= -0.7639729) {
                        var0 = 0.024604129;
                    } else {
                        var0 = -0.028417213;
                    }
                }
            } else {
                if (input[0] >= -0.65830135) {
                    if (input[4] >= 0.5981165) {
                        var0 = 0.0055108895;
                    } else {
                        var0 = 0.06960457;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var0 = -0.02362348;
                    } else {
                        var0 = 0.017733535;
                    }
                }
            }
        }
        double var1;
        if (input[0] >= -0.12083703) {
            if (input[1] >= 0.8102026) {
                if (input[4] >= 0.3699818) {
                    if (input[0] >= 1.104686) {
                        var1 = 0.0;
                    } else {
                        var1 = -0.068172045;
                    }
                } else {
                    if (input[0] >= 1.111072) {
                        var1 = 0.048470892;
                    } else {
                        var1 = -0.0042989934;
                    }
                }
            } else {
                if (input[0] >= 0.7104863) {
                    if (input[4] >= 1.054386) {
                        var1 = 0.0324431;
                    } else {
                        var1 = 0.06828334;
                    }
                } else {
                    if (input[4] >= 0.5981165) {
                        var1 = 0.017775543;
                    } else {
                        var1 = 0.04836453;
                    }
                }
            }
        } else {
            if (input[4] >= 0.5981165) {
                if (input[0] >= -0.38037917) {
                    if (input[3] >= 0.6054628) {
                        var1 = -0.024772502;
                    } else {
                        var1 = 0.0;
                    }
                } else {
                    if (input[4] >= 1.054386) {
                        var1 = -0.062745646;
                    } else {
                        var1 = -0.03871284;
                    }
                }
            } else {
                if (input[1] >= 1.8769709) {
                    if (input[0] >= -0.545189) {
                        var1 = -0.10193594;
                    } else {
                        var1 = -0.056232385;
                    }
                } else {
                    if (input[0] >= -0.782464) {
                        var1 = 0.013913005;
                    } else {
                        var1 = -0.024216888;
                    }
                }
            }
        }
        double var2;
        if (input[0] >= 0.10511011) {
            if (input[0] >= 1.4366934) {
                if (input[4] >= 0.5981165) {
                    var2 = 0.03440101;
                } else {
                    var2 = 0.047997374;
                }
            } else {
                if (input[4] >= 1.054386) {
                    if (input[11] >= 0.5) {
                        var2 = -0.016826855;
                    } else {
                        var2 = 0.007687343;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var2 = 0.03185384;
                    } else {
                        var2 = 0.0035667308;
                    }
                }
            }
        } else {
            if (input[3] >= -0.40516964) {
                if (input[4] >= 0.14184704) {
                    if (input[8] >= 0.5) {
                        var2 = -0.019976346;
                    } else {
                        var2 = -0.04434455;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var2 = 0.008999407;
                    } else {
                        var2 = -0.012009241;
                    }
                }
            } else {
                if (input[4] >= 1.054386) {
                    if (input[8] >= 0.5) {
                        var2 = -0.04769006;
                    } else {
                        var2 = -0.017807063;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var2 = -0.02303765;
                    } else {
                        var2 = 0.012824774;
                    }
                }
            }
        }
        double var3;
        if (input[0] >= -0.4793317) {
            if (input[1] >= 0.8102026) {
                if (input[1] >= 1.8769709) {
                    if (input[4] >= -1.3410288) {
                        var3 = -0.048929483;
                    } else {
                        var3 = 0.0109839905;
                    }
                } else {
                    if (input[0] >= -0.16986907) {
                        var3 = 0.00014615635;
                    } else {
                        var3 = -0.022035843;
                    }
                }
            } else {
                if (input[1] >= -1.3271782) {
                    if (input[10] >= 0.5) {
                        var3 = -0.012115494;
                    } else {
                        var3 = 0.015851868;
                    }
                } else {
                    if (input[3] >= 1.6160953) {
                        var3 = -0.005410205;
                    } else {
                        var3 = 0.035343047;
                    }
                }
            }
        } else {
            if (input[1] >= -0.25848782) {
                if (input[6] >= 0.5) {
                    if (input[0] >= -0.86581296) {
                        var3 = -0.015523503;
                    } else {
                        var3 = -0.03995332;
                    }
                } else {
                    if (input[2] >= 0.12149453) {
                        var3 = -0.020683028;
                    } else {
                        var3 = 0.0052559525;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= -0.9221748) {
                        var3 = 0.016766049;
                    } else {
                        var3 = -0.018391378;
                    }
                } else {
                    if (input[4] >= -0.20035508) {
                        var3 = -0.008886717;
                    } else {
                        var3 = 0.05035601;
                    }
                }
            }
        }
        double var4;
        if (input[0] >= -0.81461525) {
            if (input[4] >= 0.5981165) {
                if (input[11] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var4 = -0.008179135;
                    } else {
                        var4 = -0.023926161;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var4 = 0.00402235;
                    } else {
                        var4 = -0.012442676;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[4] >= -0.5425572) {
                        var4 = 0.006577729;
                    } else {
                        var4 = 0.017576175;
                    }
                } else {
                    if (input[4] >= -0.9988267) {
                        var4 = -0.012334201;
                    } else {
                        var4 = 0.0011618568;
                    }
                }
            }
        } else {
            if (input[2] >= -1.3963268) {
                if (input[7] >= 0.5) {
                    if (input[4] >= 0.14184704) {
                        var4 = -0.02284836;
                    } else {
                        var4 = -0.005411112;
                    }
                } else {
                    if (input[3] >= 0.6054628) {
                        var4 = 0.0;
                    } else {
                        var4 = -0.036619537;
                    }
                }
            } else {
                if (input[3] >= 1.6160953) {
                    if (input[2] >= -2.914148) {
                        var4 = 0.0;
                    } else {
                        var4 = -0.07510611;
                    }
                } else {
                    if (input[4] >= -2.1395004) {
                        var4 = 0.026708966;
                    } else {
                        var4 = 0.0;
                    }
                }
            }
        }
        double var5;
        if (input[1] >= -1.3271782) {
            if (input[1] >= 0.8102026) {
                if (input[3] >= 1.6160953) {
                    if (input[4] >= 0.5981165) {
                        var5 = -0.01318179;
                    } else {
                        var5 = 0.030510567;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var5 = -0.019552814;
                    } else {
                        var5 = -0.034894254;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[4] >= 1.5106556) {
                        var5 = -0.016618816;
                    } else {
                        var5 = 0.005444862;
                    }
                } else {
                    if (input[4] >= -0.77069193) {
                        var5 = -0.011618446;
                    } else {
                        var5 = 0.00071956014;
                    }
                }
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[10] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var5 = -0.011421468;
                    } else {
                        var5 = 0.0137009835;
                    }
                } else {
                    if (input[4] >= 1.054386) {
                        var5 = 0.0;
                    } else {
                        var5 = 0.025034597;
                    }
                }
            } else {
                if (input[4] >= -0.77069193) {
                    if (input[12] >= 0.5) {
                        var5 = -0.021418598;
                    } else {
                        var5 = 0.0;
                    }
                } else {
                    var5 = 0.014264329;
                }
            }
        }
        double var6;
        if (input[0] >= -0.3261274) {
            if (input[0] >= 2.032019) {
                if (input[0] >= 4.09092) {
                    var6 = 0.0;
                } else {
                    if (input[0] >= 2.824472) {
                        var6 = 0.025806963;
                    } else {
                        var6 = 0.01489085;
                    }
                }
            } else {
                if (input[1] >= -1.3271782) {
                    if (input[6] >= 0.5) {
                        var6 = -0.00085058034;
                    } else {
                        var6 = 0.011852837;
                    }
                } else {
                    if (input[0] >= -0.29269904) {
                        var6 = 0.0115278745;
                    } else {
                        var6 = 0.0;
                    }
                }
            }
        } else {
            if (input[0] >= -0.9386669) {
                if (input[3] >= -0.40516964) {
                    if (input[9] >= 0.5) {
                        var6 = -0.012624192;
                    } else {
                        var6 = 0.00028049372;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var6 = -0.029857963;
                    } else {
                        var6 = 0.006596151;
                    }
                }
            } else {
                if (input[4] >= -1.6832309) {
                    if (input[4] >= -0.9988267) {
                        var6 = -0.010585304;
                    } else {
                        var6 = -0.03207519;
                    }
                } else {
                    if (input[2] >= -2.914148) {
                        var6 = -0.007409053;
                    } else {
                        var6 = 0.041616186;
                    }
                }
            }
        }
        double var7;
        if (input[0] >= -0.61504436) {
            if (input[1] >= 1.8769709) {
                if (input[0] >= 1.9581654) {
                    var7 = 0.010164746;
                } else {
                    if (input[6] >= 0.5) {
                        var7 = -0.027472133;
                    } else {
                        var7 = -0.0056460234;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= 2.0931008) {
                        var7 = 0.010432313;
                    } else {
                        var7 = 0.0006129983;
                    }
                } else {
                    if (input[3] >= 0.6054628) {
                        var7 = 0.026488144;
                    } else {
                        var7 = 0.006495731;
                    }
                }
            }
        } else {
            if (input[1] >= 0.8102026) {
                if (input[0] >= -0.703613) {
                    if (input[0] >= -0.6942841) {
                        var7 = -0.015774092;
                    } else {
                        var7 = 0.03730609;
                    }
                } else {
                    if (input[3] >= 1.6160953) {
                        var7 = 0.0;
                    } else {
                        var7 = -0.04234555;
                    }
                }
            } else {
                if (input[0] >= -1.1375715) {
                    if (input[0] >= -1.0936481) {
                        var7 = -0.004101464;
                    } else {
                        var7 = 0.025719477;
                    }
                } else {
                    if (input[0] >= -1.1877141) {
                        var7 = -0.03973444;
                    } else {
                        var7 = -0.0010131928;
                    }
                }
            }
        }
        double var8;
        if (input[1] >= 0.8102026) {
            if (input[4] >= -1.9113657) {
                if (input[4] >= -1.2269614) {
                    if (input[13] >= -0.5) {
                        var8 = 0.0;
                    } else {
                        var8 = -0.0151875615;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var8 = 0.0;
                    } else {
                        var8 = 0.06776655;
                    }
                }
            } else {
                if (input[2] >= -2.914148) {
                    var8 = -0.05193275;
                } else {
                    var8 = 0.0;
                }
            }
        } else {
            if (input[4] >= -1.6832309) {
                if (input[7] >= 0.5) {
                    if (input[4] >= -0.77069193) {
                        var8 = 0.0005139384;
                    } else {
                        var8 = 0.006796148;
                    }
                } else {
                    if (input[3] >= 0.6054628) {
                        var8 = 0.013843014;
                    } else {
                        var8 = -0.009580435;
                    }
                }
            } else {
                if (input[0] >= -1.2431319) {
                    if (input[0] >= -1.1937112) {
                        var8 = 0.015828473;
                    } else {
                        var8 = -0.07495361;
                    }
                } else {
                    var8 = 0.074810185;
                }
            }
        }
        double var9;
        if (input[1] >= -1.3271782) {
            if (input[10] >= 0.5) {
                if (input[6] >= 0.5) {
                    if (input[3] >= 1.6160953) {
                        var9 = 0.0029129426;
                    } else {
                        var9 = -0.010131708;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var9 = 0.012870677;
                    } else {
                        var9 = -0.02168926;
                    }
                }
            } else {
                if (input[1] >= 1.8769709) {
                    var9 = -0.00818339;
                } else {
                    if (input[13] >= 0.5) {
                        var9 = -0.00492794;
                    } else {
                        var9 = 0.0014405734;
                    }
                }
            }
        } else {
            if (input[3] >= 0.6054628) {
                if (input[13] >= -0.5) {
                    if (input[2] >= 0.12149453) {
                        var9 = 0.0;
                    } else {
                        var9 = -0.04644824;
                    }
                } else {
                    var9 = 0.0;
                }
            } else {
                if (input[13] >= 0.5) {
                    var9 = 0.0002723528;
                } else {
                    if (input[8] >= 0.5) {
                        var9 = 0.016701823;
                    } else {
                        var9 = 0.006504535;
                    }
                }
            }
        }
        double var10;
        if (input[4] >= -1.6832309) {
            if (input[4] >= -1.4550962) {
                if (input[4] >= -0.5425572) {
                    if (input[0] >= -1.1872698) {
                        var10 = -0.0014603711;
                    } else {
                        var10 = 0.019970821;
                    }
                } else {
                    if (input[0] >= -1.0601087) {
                        var10 = 0.004310387;
                    } else {
                        var10 = -0.014007007;
                    }
                }
            } else {
                if (input[3] >= 0.6054628) {
                    if (input[6] >= 0.5) {
                        var10 = 0.0;
                    } else {
                        var10 = -0.15165421;
                    }
                } else {
                    if (input[0] >= -1.0472815) {
                        var10 = -0.014384295;
                    } else {
                        var10 = 0.005890906;
                    }
                }
            }
        } else {
            if (input[4] >= -2.1395004) {
                if (input[10] >= 0.5) {
                    if (input[0] >= -0.8050643) {
                        var10 = 0.0037291297;
                    } else {
                        var10 = -0.039792594;
                    }
                } else {
                    if (input[0] >= -0.8674233) {
                        var10 = 0.013814431;
                    } else {
                        var10 = 0.04333687;
                    }
                }
            } else {
                if (input[2] >= -2.914148) {
                    if (input[0] >= -0.8165033) {
                        var10 = 0.0;
                    } else {
                        var10 = -0.020862864;
                    }
                } else {
                    if (input[0] >= -1.1960989) {
                        var10 = 0.03091778;
                    } else {
                        var10 = 0.0;
                    }
                }
            }
        }
        double var11;
        if (input[1] >= -1.3271782) {
            if (input[12] >= 0.5) {
                if (input[9] >= 0.5) {
                    if (input[4] >= -0.5425572) {
                        var11 = -0.0043002153;
                    } else {
                        var11 = 0.0023501415;
                    }
                } else {
                    if (input[4] >= -2.3676353) {
                        var11 = 0.003599598;
                    } else {
                        var11 = -0.0427972;
                    }
                }
            } else {
                if (input[2] >= 0.12149453) {
                    if (input[4] >= 0.14184704) {
                        var11 = -0.00639288;
                    } else {
                        var11 = 0.0011105244;
                    }
                } else {
                    if (input[4] >= 1.2825208) {
                        var11 = -0.011448616;
                    } else {
                        var11 = -0.00044222994;
                    }
                }
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[12] >= 0.5) {
                    if (input[4] >= -0.0862877) {
                        var11 = 0.013292165;
                    } else {
                        var11 = -0.004878639;
                    }
                } else {
                    if (input[4] >= 1.7387903) {
                        var11 = 0.02711162;
                    } else {
                        var11 = 0.0031536464;
                    }
                }
            } else {
                if (input[3] >= 0.6054628) {
                    if (input[11] >= 0.5) {
                        var11 = 0.0;
                    } else {
                        var11 = -0.055241417;
                    }
                } else {
                    var11 = -0.0030883926;
                }
            }
        }
        double var12;
        if (input[7] >= 0.5) {
            if (input[4] >= 1.5106556) {
                if (input[4] >= 1.7387903) {
                    if (input[4] >= 2.4231944) {
                        var12 = -0.01815033;
                    } else {
                        var12 = 0.0027641256;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var12 = 0.0;
                    } else {
                        var12 = -0.021020511;
                    }
                }
            } else {
                if (input[1] >= 1.8769709) {
                    if (input[4] >= -0.5425572) {
                        var12 = -0.010153985;
                    } else {
                        var12 = 0.0;
                    }
                } else {
                    var12 = 0.0013471646;
                }
            }
        } else {
            if (input[4] >= -0.9988267) {
                if (input[4] >= -0.31442246) {
                    if (input[9] >= 0.5) {
                        var12 = -0.025535582;
                    } else {
                        var12 = 0.0057416516;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var12 = 0.0;
                    } else {
                        var12 = -0.014473294;
                    }
                }
            } else {
                if (input[1] >= -0.25848782) {
                    if (input[1] >= 4.0124297) {
                        var12 = 0.043600447;
                    } else {
                        var12 = -0.0050455295;
                    }
                } else {
                    var12 = 0.01342528;
                }
            }
        }
        double var13;
        if (input[0] >= -1.1375715) {
            if (input[0] >= -1.1049759) {
                if (input[0] >= -1.0649952) {
                    if (input[0] >= -1.0221268) {
                        var13 = -0.00007804717;
                    } else {
                        var13 = 0.010704323;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var13 = 0.0;
                    } else {
                        var13 = -0.045079984;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[1] >= 0.8102026) {
                        var13 = -0.008850517;
                    } else {
                        var13 = 0.038144395;
                    }
                } else {
                    if (input[4] >= -1.9113657) {
                        var13 = -0.016644614;
                    } else {
                        var13 = 0.05489003;
                    }
                }
            }
        } else {
            if (input[0] >= -1.1877141) {
                if (input[2] >= -2.914148) {
                    if (input[13] >= 0.5) {
                        var13 = 0.0;
                    } else {
                        var13 = -0.036669128;
                    }
                } else {
                    var13 = 0.0;
                }
            } else {
                if (input[2] >= 0.12149453) {
                    if (input[1] >= -0.792833) {
                        var13 = 0.0041885753;
                    } else {
                        var13 = 0.09871203;
                    }
                } else {
                    if (input[2] >= -1.3963268) {
                        var13 = -0.028005753;
                    } else {
                        var13 = 0.0;
                    }
                }
            }
        }
        double var14;
        if (input[0] >= 0.18779266) {
            if (input[0] >= 0.25598216) {
                if (input[0] >= 0.2719745) {
                    if (input[0] >= 2.455871) {
                        var14 = 0.006618029;
                    } else {
                        var14 = 0.00039401322;
                    }
                } else {
                    var14 = -0.019797448;
                }
            } else {
                if (input[11] >= 0.5) {
                    var14 = 0.0;
                } else {
                    if (input[13] >= 1.5) {
                        var14 = 0.0;
                    } else {
                        var14 = 0.021077892;
                    }
                }
            }
        } else {
            if (input[0] >= 0.14886689) {
                if (input[0] >= 0.15564142) {
                    var14 = -0.004874332;
                } else {
                    var14 = -0.05921633;
                }
            } else {
                if (input[4] >= 1.5106556) {
                    if (input[0] >= -0.7113315) {
                        var14 = -0.004376758;
                    } else {
                        var14 = -0.02058402;
                    }
                } else {
                    if (input[3] >= 0.6054628) {
                        var14 = 0.0031205278;
                    } else {
                        var14 = -0.0015306615;
                    }
                }
            }
        }
        double var15;
        if (input[0] >= -0.61504436) {
            if (input[0] >= -0.6036054) {
                if (input[8] >= 0.5) {
                    if (input[12] >= 0.5) {
                        var15 = 0.0043816464;
                    } else {
                        var15 = -0.0014959486;
                    }
                } else {
                    if (input[0] >= 0.9560907) {
                        var15 = 0.0022447174;
                    } else {
                        var15 = -0.001589222;
                    }
                }
            } else {
                if (input[0] >= -0.606604) {
                    var15 = 0.039067723;
                } else {
                    if (input[0] >= -0.6106576) {
                        var15 = -0.019476378;
                    } else {
                        var15 = 0.022585602;
                    }
                }
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[0] >= -1.0290124) {
                    if (input[0] >= -1.0210162) {
                        var15 = -0.0021381476;
                    } else {
                        var15 = 0.03931224;
                    }
                } else {
                    if (input[0] >= -1.278004) {
                        var15 = -0.029198892;
                    } else {
                        var15 = 0.0065429565;
                    }
                }
            } else {
                if (input[0] >= -1.0298454) {
                    if (input[5] >= 0.5) {
                        var15 = -0.005751648;
                    } else {
                        var15 = 0.0027666246;
                    }
                } else {
                    if (input[0] >= -1.0357869) {
                        var15 = 0.052128784;
                    } else {
                        var15 = 0.009498604;
                    }
                }
            }
        }
        double var16;
        if (input[13] >= -0.5) {
            if (input[2] >= -2.914148) {
                if (input[0] >= -0.21440326) {
                    if (input[0] >= -0.033934288) {
                        var16 = 0.00058540754;
                    } else {
                        var16 = -0.0056539793;
                    }
                } else {
                    if (input[0] >= -0.24255642) {
                        var16 = 0.031530835;
                    } else {
                        var16 = 0.0026073225;
                    }
                }
            } else {
                if (input[0] >= -1.0172958) {
                    if (input[9] >= 0.5) {
                        var16 = 0.0;
                    } else {
                        var16 = -0.038469847;
                    }
                } else {
                    if (input[0] >= -1.1763861) {
                        var16 = 0.028809348;
                    } else {
                        var16 = 0.0;
                    }
                }
            }
        } else {
            if (input[2] >= 0.12149453) {
                if (input[0] >= -0.9488287) {
                    if (input[3] >= 1.6160953) {
                        var16 = 0.004044354;
                    } else {
                        var16 = -0.014759872;
                    }
                } else {
                    if (input[0] >= -0.9647654) {
                        var16 = 0.048156798;
                    } else {
                        var16 = 0.00096041954;
                    }
                }
            } else {
                if (input[0] >= -0.80395377) {
                    if (input[7] >= 0.5) {
                        var16 = 0.0063783606;
                    } else {
                        var16 = -0.0073597264;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var16 = -0.015999578;
                    } else {
                        var16 = -0.0006533016;
                    }
                }
            }
        }
        double var17;
        if (input[13] >= -1.5) {
            if (input[4] >= -0.0862877) {
                if (input[12] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var17 = 0.0068064304;
                    } else {
                        var17 = -0.0044993497;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var17 = -0.00064412033;
                    } else {
                        var17 = 0.012793271;
                    }
                }
            } else {
                if (input[2] >= 0.12149453) {
                    if (input[5] >= 0.5) {
                        var17 = -0.011826994;
                    } else {
                        var17 = 0.0017574716;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var17 = 0.00141632;
                    } else {
                        var17 = -0.0016671752;
                    }
                }
            }
        } else {
            if (input[4] >= 0.3699818) {
                var17 = -0.011539539;
            } else {
                if (input[4] >= -1.9113657) {
                    if (input[5] >= 0.5) {
                        var17 = 0.026127165;
                    } else {
                        var17 = 0.0;
                    }
                } else {
                    var17 = -0.050154753;
                }
            }
        }
        double var18;
        if (input[4] >= 0.5981165) {
            if (input[4] >= 0.82625127) {
                if (input[2] >= 0.12149453) {
                    if (input[4] >= 1.9669251) {
                        var18 = -0.017532641;
                    } else {
                        var18 = 0.003065754;
                    }
                } else {
                    if (input[4] >= 1.9669251) {
                        var18 = 0.012834578;
                    } else {
                        var18 = -0.0039869137;
                    }
                }
            } else {
                if (input[0] >= -1.0431724) {
                    if (input[13] >= -0.5) {
                        var18 = -0.011512789;
                    } else {
                        var18 = 0.0;
                    }
                } else {
                    if (input[2] >= 0.12149453) {
                        var18 = 0.0;
                    } else {
                        var18 = 0.05280298;
                    }
                }
            }
        } else {
            if (input[0] >= -0.9418875) {
                if (input[0] >= -0.7707474) {
                    if (input[2] >= -1.3963268) {
                        var18 = -0.000011735022;
                    } else {
                        var18 = 0.008674052;
                    }
                } else {
                    if (input[4] >= -1.2269614) {
                        var18 = 0.010914453;
                    } else {
                        var18 = -0.0033958978;
                    }
                }
            } else {
                if (input[0] >= -0.95299333) {
                    var18 = -0.028138481;
                } else {
                    if (input[13] >= 0.5) {
                        var18 = 0.008370919;
                    } else {
                        var18 = -0.0041921954;
                    }
                }
            }
        }
        double var19;
        if (input[0] >= -0.89729786) {
            if (input[0] >= -0.89452136) {
                if (input[0] >= -0.79867846) {
                    if (input[0] >= -0.7881835) {
                        var19 = 0.00018801728;
                    } else {
                        var19 = -0.018377911;
                    }
                } else {
                    if (input[0] >= -0.80484223) {
                        var19 = 0.029355321;
                    } else {
                        var19 = 0.0011562778;
                    }
                }
            } else {
                var19 = 0.048607178;
            }
        } else {
            if (input[0] >= -0.90368366) {
                if (input[4] >= -0.9988267) {
                    var19 = -0.03891389;
                } else {
                    var19 = 0.0;
                }
            } else {
                if (input[4] >= -0.9988267) {
                    if (input[4] >= 0.5981165) {
                        var19 = -0.0059082992;
                    } else {
                        var19 = 0.004382766;
                    }
                } else {
                    if (input[0] >= -1.1525087) {
                        var19 = -0.0018221048;
                    } else {
                        var19 = -0.017111158;
                    }
                }
            }
        }
        double var20;
        if (input[0] >= -1.2647882) {
            if (input[5] >= 0.5) {
                if (input[3] >= 1.6160953) {
                    if (input[6] >= 0.5) {
                        var20 = 0.0;
                    } else {
                        var20 = 0.024410693;
                    }
                } else {
                    if (input[0] >= -0.6945063) {
                        var20 = 0.0005613487;
                    } else {
                        var20 = -0.0029768848;
                    }
                }
            } else {
                if (input[3] >= 1.6160953) {
                    if (input[11] >= 0.5) {
                        var20 = -0.063472964;
                    } else {
                        var20 = -0.004861073;
                    }
                } else {
                    if (input[0] >= 0.43606243) {
                        var20 = -0.007714659;
                    } else {
                        var20 = 0.00088103407;
                    }
                }
            }
        } else {
            if (input[0] >= -1.3381419) {
                var20 = 0.016773649;
            } else {
                var20 = -0.015560949;
            }
        }
        double var21;
        if (input[0] >= -1.0936481) {
            if (input[0] >= -1.0756012) {
                if (input[0] >= -1.0478367) {
                    if (input[2] >= 0.12149453) {
                        var21 = 0.0005037291;
                    } else {
                        var21 = -0.0012667249;
                    }
                } else {
                    var21 = 0.008598907;
                }
            } else {
                var21 = -0.024103621;
            }
        } else {
            if (input[0] >= -1.1162484) {
                if (input[2] >= 0.12149453) {
                    var21 = -0.032994322;
                } else {
                    if (input[2] >= -1.3963268) {
                        var21 = 0.075484104;
                    } else {
                        var21 = 0.011971103;
                    }
                }
            } else {
                if (input[0] >= -1.1202464) {
                    var21 = -0.057844143;
                } else {
                    if (input[3] >= -0.40516964) {
                        var21 = 0.012438174;
                    } else {
                        var21 = -0.006572822;
                    }
                }
            }
        }
        double var22;
        if (input[1] >= 2.9437392) {
            var22 = 0.01645815;
        } else {
            if (input[4] >= -1.6832309) {
                if (input[4] >= -1.4550962) {
                    if (input[2] >= -1.3963268) {
                        var22 = -0.00013318173;
                    } else {
                        var22 = 0.0052218153;
                    }
                } else {
                    if (input[2] >= 0.12149453) {
                        var22 = -0.01967655;
                    } else {
                        var22 = 0.0;
                    }
                }
            } else {
                if (input[4] >= -1.9113657) {
                    if (input[3] >= -0.40516964) {
                        var22 = 0.02260038;
                    } else {
                        var22 = -0.005880285;
                    }
                } else {
                    if (input[1] >= 0.8102026) {
                        var22 = -0.018343754;
                    } else {
                        var22 = 0.00010894894;
                    }
                }
            }
        }
        double var23;
        if (input[0] >= -0.16564888) {
            if (input[0] >= 0.1650258) {
                if (input[3] >= -0.40516964) {
                    if (input[0] >= 0.4142396) {
                        var23 = 0.00067419914;
                    } else {
                        var23 = 0.010864103;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var23 = 0.0;
                    } else {
                        var23 = -0.006958949;
                    }
                }
            } else {
                if (input[4] >= -0.9988267) {
                    if (input[4] >= 0.14184704) {
                        var23 = -0.0016616647;
                    } else {
                        var23 = -0.016302137;
                    }
                } else {
                    if (input[0] >= 0.06712833) {
                        var23 = -0.009235988;
                    } else {
                        var23 = 0.0066195694;
                    }
                }
            }
        } else {
            if (input[0] >= -0.17564407) {
                if (input[2] >= 0.12149453) {
                    if (input[1] >= 0.8102026) {
                        var23 = 0.0;
                    } else {
                        var23 = 0.052706283;
                    }
                } else {
                    var23 = 0.0;
                }
            } else {
                if (input[0] >= -0.19063687) {
                    if (input[0] >= -0.18519504) {
                        var23 = 0.0;
                    } else {
                        var23 = -0.026646283;
                    }
                } else {
                    if (input[0] >= -0.3170762) {
                        var23 = 0.00553448;
                    } else {
                        var23 = 0.00026932813;
                    }
                }
            }
        }
        double var24;
        if (input[1] >= -0.25848782) {
            if (input[0] >= 2.410171) {
                if (input[12] >= 0.5) {
                    var24 = 0.0;
                } else {
                    var24 = 0.012635219;
                }
            } else {
                if (input[4] >= 1.054386) {
                    if (input[12] >= 0.5) {
                        var24 = -0.007693856;
                    } else {
                        var24 = -0.00050366536;
                    }
                } else {
                    if (input[0] >= -0.9418875) {
                        var24 = -0.00008932342;
                    } else {
                        var24 = -0.004478186;
                    }
                }
            }
        } else {
            if (input[0] >= -1.1858261) {
                if (input[4] >= 0.3699818) {
                    if (input[0] >= -1.1013665) {
                        var24 = 0.004887793;
                    } else {
                        var24 = -0.038326986;
                    }
                } else {
                    if (input[0] >= -0.78746164) {
                        var24 = -0.0048353453;
                    } else {
                        var24 = 0.009611015;
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    var24 = 0.0;
                } else {
                    var24 = 0.039919224;
                }
            }
        }
        double var25;
        if (input[0] >= -0.9232298) {
            if (input[0] >= -0.87653005) {
                if (input[0] >= -0.8679786) {
                    if (input[0] >= -0.8012328) {
                        var25 = 0.00057565817;
                    } else {
                        var25 = 0.005406169;
                    }
                } else {
                    if (input[0] >= -0.8718656) {
                        var25 = -0.040080216;
                    } else {
                        var25 = -0.0005749194;
                    }
                }
            } else {
                if (input[3] >= 0.6054628) {
                    var25 = -0.007465733;
                } else {
                    if (input[13] >= 0.5) {
                        var25 = -0.0036939296;
                    } else {
                        var25 = 0.022632616;
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[0] >= -1.2831683) {
                    if (input[0] >= -1.0413399) {
                        var25 = -0.00020585516;
                    } else {
                        var25 = 0.015787799;
                    }
                } else {
                    if (input[4] >= -0.4284898) {
                        var25 = 0.0;
                    } else {
                        var25 = -0.035827857;
                    }
                }
            } else {
                if (input[3] >= 0.6054628) {
                    if (input[0] >= -1.1254661) {
                        var25 = 0.012486657;
                    } else {
                        var25 = -0.008314575;
                    }
                } else {
                    if (input[0] >= -1.1229674) {
                        var25 = -0.01188099;
                    } else {
                        var25 = 0.00304957;
                    }
                }
            }
        }
        double var26;
        if (input[2] >= -2.914148) {
            if (input[3] >= 0.6054628) {
                if (input[13] >= -1.5) {
                    if (input[2] >= -1.3963268) {
                        var26 = 0.0032604095;
                    } else {
                        var26 = -0.019974893;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var26 = 0.00028356528;
                    } else {
                        var26 = -0.009218178;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[2] >= -1.3963268) {
                        var26 = 0.00008312965;
                    } else {
                        var26 = 0.007644351;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var26 = -0.004354987;
                    } else {
                        var26 = 0.001931796;
                    }
                }
            }
        } else {
            if (input[3] >= 1.6160953) {
                if (input[11] >= 0.5) {
                    var26 = -0.06277143;
                } else {
                    var26 = 0.0;
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[3] >= 0.6054628) {
                        var26 = -0.0032106899;
                    } else {
                        var26 = 0.01214619;
                    }
                } else {
                    var26 = -0.014495662;
                }
            }
        }
        double var27;
        if (input[4] >= 0.5981165) {
            if (input[3] >= -0.40516964) {
                if (input[3] >= 0.6054628) {
                    var27 = -0.0035728286;
                } else {
                    if (input[12] >= 0.5) {
                        var27 = 0.004793616;
                    } else {
                        var27 = -0.003358878;
                    }
                }
            } else {
                var27 = -0.0047811265;
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[13] >= -0.5) {
                    if (input[3] >= 1.6160953) {
                        var27 = 0.060806572;
                    } else {
                        var27 = 0.0017838008;
                    }
                } else {
                    if (input[4] >= -1.2269614) {
                        var27 = -0.0035815695;
                    } else {
                        var27 = 0.013276824;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[4] >= -0.31442246) {
                        var27 = -0.016415916;
                    } else {
                        var27 = -0.002098929;
                    }
                } else {
                    var27 = 0.0059368964;
                }
            }
        }
        double var28;
        if (input[4] >= -2.1395004) {
            if (input[3] >= -0.40516964) {
                if (input[0] >= -1.0527787) {
                    if (input[0] >= -1.0215161) {
                        var28 = 0.00063776114;
                    } else {
                        var28 = -0.027860237;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var28 = -0.0019492529;
                    } else {
                        var28 = 0.02699015;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[4] >= 1.8528577) {
                        var28 = 0.01730238;
                    } else {
                        var28 = -0.012945301;
                    }
                } else {
                    if (input[0] >= -1.125133) {
                        var28 = 0.0003784226;
                    } else {
                        var28 = -0.009133796;
                    }
                }
            }
        } else {
            if (input[4] >= -2.59577) {
                if (input[0] >= -0.82633185) {
                    var28 = 0.0;
                } else {
                    if (input[0] >= -0.99830496) {
                        var28 = -0.039418925;
                    } else {
                        var28 = -0.0030972543;
                    }
                }
            } else {
                var28 = 0.0;
            }
        }
        double var29;
        if (input[0] >= -0.1342195) {
            if (input[4] >= 1.7387903) {
                if (input[4] >= 2.6513293) {
                    var29 = -0.02364826;
                } else {
                    if (input[13] >= -0.5) {
                        var29 = 0.02422374;
                    } else {
                        var29 = 0.0;
                    }
                }
            } else {
                if (input[0] >= 0.07840071) {
                    if (input[7] >= 0.5) {
                        var29 = -0.0012004041;
                    } else {
                        var29 = 0.003346254;
                    }
                } else {
                    if (input[4] >= 1.054386) {
                        var29 = -0.017054478;
                    } else {
                        var29 = -0.004463759;
                    }
                }
            }
        } else {
            if (input[2] >= 0.12149453) {
                if (input[0] >= -0.33501205) {
                    if (input[0] >= -0.29442042) {
                        var29 = 0.00369827;
                    } else {
                        var29 = 0.0154649895;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var29 = -0.00094977155;
                    } else {
                        var29 = 0.0078235455;
                    }
                }
            } else {
                if (input[0] >= -0.1532104) {
                    if (input[5] >= 0.5) {
                        var29 = 0.0;
                    } else {
                        var29 = 0.029013973;
                    }
                } else {
                    if (input[0] >= -0.24527733) {
                        var29 = -0.011415063;
                    } else {
                        var29 = -0.0001719771;
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
