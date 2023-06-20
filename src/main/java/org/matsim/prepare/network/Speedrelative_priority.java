package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public class Speedrelative_priority implements FeatureRegressor {
    
    @Override
    public double predict(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 146.95340968130267) / 105.80114952647662;
		data[1] = (ft.getDouble("speed") - 13.674404739276435) / 3.8719408702779563;
		data[2] = (ft.getDouble("numFoes") - 1.2830577203320028) / 1.1464110764057247;
		data[3] = (ft.getDouble("numLanes") - 1.230121016283343) / 0.5865609588116212;
		data[4] = (ft.getDouble("junctionSize") - 7.115408984350251) / 4.7724053327584235;
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
        if (input[9] >= 0.5) {
            if (input[1] >= -1.0212978) {
                if (input[7] >= 0.5) {
                    if (input[1] >= 3.2840366) {
                        var0 = 0.18053065;
                    } else {
                        var0 = 0.20093474;
                    }
                } else {
                    if (input[0] >= -0.033065896) {
                        var0 = 0.18239714;
                    } else {
                        var0 = 0.16372155;
                    }
                }
            } else {
                var0 = 0.21000688;
            }
        } else {
            if (input[3] >= 0.46010387) {
                if (input[0] >= -0.16638201) {
                    if (input[1] >= 4.3597245) {
                        var0 = 0.2044004;
                    } else {
                        var0 = 0.22437973;
                    }
                } else {
                    var0 = 0.21295604;
                }
            } else {
                var0 = 0.20558883;
            }
        }
        double var1;
        if (input[8] >= 0.5) {
            if (input[1] >= 2.5660503) {
                if (input[3] >= 0.46010387) {
                    if (input[13] >= -0.5) {
                        var1 = 0.109084874;
                    } else {
                        var1 = 0.09555577;
                    }
                } else {
                    var1 = 0.07541251;
                }
            } else {
                if (input[3] >= 0.46010387) {
                    if (input[10] >= 0.5) {
                        var1 = 0.08047744;
                    } else {
                        var1 = 0.11006813;
                    }
                } else {
                    var1 = 0.10508158;
                }
            }
        } else {
            if (input[1] >= -0.30331162) {
                if (input[12] >= 0.5) {
                    var1 = 0.10340588;
                } else {
                    if (input[6] >= 0.5) {
                        var1 = 0.08994296;
                    } else {
                        var1 = 0.10009043;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    var1 = 0.106703416;
                } else {
                    var1 = 0.102834985;
                }
            }
        }
        double var2;
        if (input[0] >= -0.24894255) {
            if (input[13] >= 0.5) {
                if (input[2] >= -0.6830514) {
                    var2 = 0.0532948;
                } else {
                    if (input[12] >= 0.5) {
                        var2 = 0.06892653;
                    } else {
                        var2 = 0.058337156;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[0] >= 0.862293) {
                        var2 = 0.061778132;
                    } else {
                        var2 = 0.05389955;
                    }
                } else {
                    if (input[0] >= 0.4621083) {
                        var2 = 0.053061362;
                    } else {
                        var2 = 0.051152516;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[13] >= -0.5) {
                    if (input[11] >= 0.5) {
                        var2 = 0.049059294;
                    } else {
                        var2 = 0.051127914;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var2 = 0.048366968;
                    } else {
                        var2 = 0.057078134;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    var2 = 0.030749878;
                } else {
                    if (input[0] >= -0.8072541) {
                        var2 = 0.046601906;
                    } else {
                        var2 = 0.03949388;
                    }
                }
            }
        }
        double var3;
        if (input[1] >= 1.1313694) {
            if (input[9] >= 0.5) {
                if (input[13] >= 0.5) {
                    var3 = 0.03771763;
                } else {
                    if (input[13] >= -0.5) {
                        var3 = 0.017547185;
                    } else {
                        var3 = 0.026695943;
                    }
                }
            } else {
                if (input[3] >= 0.46010387) {
                    if (input[1] >= 2.9250436) {
                        var3 = 0.025175799;
                    } else {
                        var3 = 0.031061323;
                    }
                } else {
                    var3 = 0.020386415;
                }
            }
        } else {
            if (input[1] >= -0.30331162) {
                if (input[7] >= 0.5) {
                    if (input[4] >= 0.70920026) {
                        var3 = 0.023616254;
                    } else {
                        var3 = 0.026639115;
                    }
                } else {
                    if (input[4] >= -1.1766412) {
                        var3 = 0.017094137;
                    } else {
                        var3 = 0.029195135;
                    }
                }
            } else {
                if (input[3] >= 0.46010387) {
                    if (input[2] >= -0.6830514) {
                        var3 = 0.028300662;
                    } else {
                        var3 = 0.037857015;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var3 = 0.026681602;
                    } else {
                        var3 = 0.02880303;
                    }
                }
            }
        }
        double var4;
        if (input[0] >= -0.3889694) {
            if (input[3] >= 0.46010387) {
                if (input[10] >= 0.5) {
                    var4 = -0.009417049;
                } else {
                    if (input[13] >= 0.5) {
                        var4 = 0.01889187;
                    } else {
                        var4 = 0.015734246;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    var4 = 0.014535784;
                } else {
                    if (input[4] >= 1.3378141) {
                        var4 = 0.010823026;
                    } else {
                        var4 = 0.01344061;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[4] >= -0.7575654) {
                    if (input[12] >= 0.5) {
                        var4 = 0.012268305;
                    } else {
                        var4 = 0.008789336;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var4 = 0.017176287;
                    } else {
                        var4 = -0.002131171;
                    }
                }
            } else {
                if (input[4] >= -0.96710324) {
                    if (input[4] >= -0.5480274) {
                        var4 = 0.012740928;
                    } else {
                        var4 = 0.009564778;
                    }
                } else {
                    if (input[2] >= -0.6830514) {
                        var4 = -0.007077322;
                    } else {
                        var4 = 0.01570109;
                    }
                }
            }
        }
        double var5;
        if (input[0] >= 0.6556317) {
            if (input[3] >= 0.46010387) {
                var5 = 0.011062378;
            } else {
                if (input[4] >= -0.96710324) {
                    if (input[7] >= 0.5) {
                        var5 = 0.0076582446;
                    } else {
                        var5 = 0.010699769;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var5 = -0.006735775;
                    } else {
                        var5 = 0.006553277;
                    }
                }
            }
        } else {
            if (input[13] >= 2.5) {
                if (input[4] >= -0.5480274) {
                    var5 = 0.0077685313;
                } else {
                    var5 = 0.02370945;
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[3] >= 2.1649566) {
                        var5 = 0.008354001;
                    } else {
                        var5 = 0.0061282874;
                    }
                } else {
                    if (input[0] >= -1.190662) {
                        var5 = 0.0042660115;
                    } else {
                        var5 = -0.009112109;
                    }
                }
            }
        }
        double var6;
        if (input[1] >= 1.1313694) {
            if (input[0] >= -0.76396537) {
                if (input[1] >= 2.5660503) {
                    if (input[4] >= -0.96710324) {
                        var6 = -0.0008901955;
                    } else {
                        var6 = -0.006612808;
                    }
                } else {
                    if (input[3] >= 2.1649566) {
                        var6 = 0.00948556;
                    } else {
                        var6 = -0.0005172183;
                    }
                }
            } else {
                if (input[4] >= -0.96710324) {
                    if (input[0] >= -1.0943491) {
                        var6 = 0.010899206;
                    } else {
                        var6 = 0.0010045648;
                    }
                } else {
                    if (input[0] >= -1.2301701) {
                        var6 = 0.0035875724;
                    } else {
                        var6 = -0.017140407;
                    }
                }
            }
        } else {
            if (input[1] >= -0.30331162) {
                if (input[7] >= 0.5) {
                    if (input[0] >= 0.23923738) {
                        var6 = 0.004966214;
                    } else {
                        var6 = 0.0026139433;
                    }
                } else {
                    if (input[0] >= 1.2838385) {
                        var6 = 0.0098048225;
                    } else {
                        var6 = -0.0023909588;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[0] >= -1.1899059) {
                        var6 = 0.005090615;
                    } else {
                        var6 = -0.0017701143;
                    }
                } else {
                    if (input[0] >= -1.2430243) {
                        var6 = 0.009083183;
                    } else {
                        var6 = -0.01204994;
                    }
                }
            }
        }
        double var7;
        if (input[13] >= 0.5) {
            if (input[2] >= -0.6830514) {
                if (input[4] >= 0.08058641) {
                    if (input[0] >= -1.175303) {
                        var7 = 0.0017216457;
                    } else {
                        var7 = 0.019724306;
                    }
                } else {
                    if (input[0] >= -0.7197314) {
                        var7 = -0.0039532064;
                    } else {
                        var7 = -0.020788295;
                    }
                }
            } else {
                if (input[4] >= -0.96710324) {
                    if (input[0] >= -0.25012404) {
                        var7 = 0.0018979175;
                    } else {
                        var7 = 0.0052624093;
                    }
                } else {
                    if (input[1] >= 0.41467455) {
                        var7 = 0.019572023;
                    } else {
                        var7 = 0.0062020347;
                    }
                }
            }
        } else {
            if (input[1] >= 1.8480642) {
                if (input[0] >= 0.37444383) {
                    var7 = -0.004248452;
                } else {
                    if (input[1] >= 5.4354124) {
                        var7 = 0.03138115;
                    } else {
                        var7 = 0.0003735117;
                    }
                }
            } else {
                if (input[4] >= -0.96710324) {
                    if (input[2] >= -0.6830514) {
                        var7 = 0.0016755011;
                    } else {
                        var7 = -0.0021160825;
                    }
                } else {
                    if (input[2] >= -0.6830514) {
                        var7 = -0.004493736;
                    } else {
                        var7 = 0.0027463713;
                    }
                }
            }
        }
        double var8;
        if (input[3] >= 2.1649566) {
            if (input[0] >= -0.97511613) {
                if (input[0] >= -0.18864077) {
                    if (input[0] >= 0.60341114) {
                        var8 = 0.0056569795;
                    } else {
                        var8 = -0.00017228426;
                    }
                } else {
                    var8 = 0.005812301;
                }
            } else {
                if (input[0] >= -1.2777121) {
                    var8 = -0.0054268953;
                } else {
                    var8 = 0.01103895;
                }
            }
        } else {
            if (input[0] >= -1.3166058) {
                if (input[0] >= -1.3121636) {
                    if (input[0] >= -0.61850375) {
                        var8 = 0.00094563497;
                    } else {
                        var8 = 0.00016296984;
                    }
                } else {
                    var8 = -0.015843106;
                }
            } else {
                if (input[0] >= -1.3189688) {
                    var8 = 0.018148033;
                } else {
                    var8 = 0.0039531738;
                }
            }
        }
        double var9;
        if (input[13] >= 1.5) {
            if (input[0] >= -0.21501099) {
                var9 = 0.00076910097;
            } else {
                if (input[5] >= 0.5) {
                    var9 = -0.009074635;
                } else {
                    var9 = 0.006656394;
                }
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[7] >= 0.5) {
                    if (input[0] >= -1.0822983) {
                        var9 = 0.0007031316;
                    } else {
                        var9 = 0.0034494644;
                    }
                } else {
                    var9 = -0.0027150933;
                }
            } else {
                if (input[2] >= 1.0615234) {
                    if (input[0] >= 0.23257394) {
                        var9 = 0.002037374;
                    } else {
                        var9 = -0.0033257443;
                    }
                } else {
                    if (input[0] >= -1.2981278) {
                        var9 = 0.00022408867;
                    } else {
                        var9 = 0.0050762687;
                    }
                }
            }
        }
        double var10;
        if (input[6] >= 0.5) {
            if (input[0] >= -1.1252091) {
                if (input[0] >= -0.9369313) {
                    if (input[0] >= -0.7756854) {
                        var10 = 0.000020280837;
                    } else {
                        var10 = -0.0023618096;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var10 = -0.0067722416;
                    } else {
                        var10 = 0.002534714;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[2] >= 0.18923602) {
                        var10 = 0.00024627926;
                    } else {
                        var10 = -0.019358424;
                    }
                } else {
                    var10 = -0.011665206;
                }
            }
        } else {
            if (input[0] >= -1.0870714) {
                if (input[0] >= -1.0863154) {
                    if (input[3] >= 2.1649566) {
                        var10 = 0.0030252342;
                    } else {
                        var10 = 0.00023834985;
                    }
                } else {
                    var10 = -0.039449524;
                }
            } else {
                if (input[8] >= 0.5) {
                    var10 = -0.0015172419;
                } else {
                    if (input[2] >= -0.6830514) {
                        var10 = -0.00067441876;
                    } else {
                        var10 = 0.005691063;
                    }
                }
            }
        }
        double var11;
        if (input[1] >= -0.30331162) {
            if (input[10] >= 0.5) {
                if (input[4] >= 0.49966228) {
                    if (input[0] >= 0.6922098) {
                        var11 = -0.006246882;
                    } else {
                        var11 = -0.026310079;
                    }
                } else {
                    if (input[4] >= -0.33848947) {
                        var11 = -0.0066558663;
                    } else {
                        var11 = 0.0030039249;
                    }
                }
            } else {
                if (input[0] >= -0.19894311) {
                    if (input[1] >= 5.4354124) {
                        var11 = 0.01250274;
                    } else {
                        var11 = 0.0005545488;
                    }
                } else {
                    if (input[4] >= -0.5480274) {
                        var11 = -0.00001197447;
                    } else {
                        var11 = -0.0012348592;
                    }
                }
            }
        } else {
            if (input[0] >= 0.82202876) {
                if (input[10] >= 0.5) {
                    var11 = 0.0015532692;
                } else {
                    var11 = -0.0042854436;
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[0] >= -1.0915138) {
                        var11 = 0.0040504197;
                    } else {
                        var11 = -0.008991941;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var11 = 0.00070055295;
                    } else {
                        var11 = 0.004769901;
                    }
                }
            }
        }
        double var12;
        if (input[1] >= 3.2840366) {
            if (input[12] >= 0.5) {
                if (input[0] >= -0.9212415) {
                    var12 = 0.008230212;
                } else {
                    var12 = -0.018476278;
                }
            } else {
                if (input[0] >= -0.65191555) {
                    if (input[0] >= -0.29615378) {
                        var12 = -0.0031131343;
                    } else {
                        var12 = -0.013351221;
                    }
                } else {
                    var12 = 0.0060395147;
                }
            }
        } else {
            if (input[13] >= -0.5) {
                if (input[1] >= -1.0212978) {
                    if (input[10] >= 0.5) {
                        var12 = -0.0028372237;
                    } else {
                        var12 = -0.00005523225;
                    }
                } else {
                    if (input[0] >= -0.2708705) {
                        var12 = -0.0003922606;
                    } else {
                        var12 = 0.0012749326;
                    }
                }
            } else {
                if (input[2] >= -0.6830514) {
                    var12 = -0.0023272012;
                } else {
                    var12 = 0.0025940519;
                }
            }
        }
        double var13;
        if (input[0] >= 3.131928) {
            var13 = 0.0038516966;
        } else {
            if (input[1] >= 1.1313694) {
                if (input[7] >= 0.5) {
                    var13 = -0.0008470667;
                } else {
                    var13 = -0.018624606;
                }
            } else {
                if (input[13] >= 2.5) {
                    var13 = 0.006485595;
                } else {
                    var13 = 0.00016409949;
                }
            }
        }
        double var14;
        if (input[0] >= 1.0709391) {
            if (input[1] >= 4.3597245) {
                var14 = 0.01239826;
            } else {
                if (input[1] >= 0.41467455) {
                    if (input[0] >= 1.1490574) {
                        var14 = -0.0016809821;
                    } else {
                        var14 = 0.010589391;
                    }
                } else {
                    var14 = 0.00095190963;
                }
            }
        } else {
            if (input[0] >= 1.0511851) {
                var14 = -0.0069169905;
            } else {
                if (input[1] >= 4.3597245) {
                    if (input[10] >= 0.5) {
                        var14 = -0.039369494;
                    } else {
                        var14 = -0.0027293689;
                    }
                } else {
                    var14 = -0.000061021357;
                }
            }
        }
        double var15;
        if (input[0] >= 3.2961984) {
            var15 = -0.007963096;
        } else {
            if (input[4] >= -0.96710324) {
                var15 = -0.000084384534;
            } else {
                if (input[7] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var15 = 0.005159814;
                    } else {
                        var15 = 0.0006673158;
                    }
                } else {
                    if (input[0] >= -1.1225153) {
                        var15 = -0.0036392165;
                    } else {
                        var15 = 0.021327723;
                    }
                }
            }
        }
        double var16;
        if (input[0] >= -1.2641962) {
            if (input[0] >= -1.2227504) {
                var16 = 0.000017158314;
            } else {
                var16 = 0.0038715254;
            }
        } else {
            if (input[0] >= -1.2813036) {
                if (input[13] >= 0.5) {
                    var16 = 0.007933452;
                } else {
                    if (input[12] >= 0.5) {
                        var16 = -0.00018555525;
                    } else {
                        var16 = -0.016336221;
                    }
                }
            } else {
                var16 = -0.0005506117;
            }
        }
        double var17;
        if (input[11] >= 0.5) {
            if (input[5] >= 0.5) {
                if (input[13] >= 0.5) {
                    if (input[3] >= 0.46010387) {
                        var17 = -0.045562465;
                    } else {
                        var17 = -0.0065832897;
                    }
                } else {
                    var17 = -0.001542052;
                }
            } else {
                if (input[3] >= 0.46010387) {
                    if (input[2] >= -0.6830514) {
                        var17 = -0.007881187;
                    } else {
                        var17 = 0.0009208005;
                    }
                } else {
                    if (input[4] >= -0.33848947) {
                        var17 = 0.0015086916;
                    } else {
                        var17 = -0.00033866207;
                    }
                }
            }
        } else {
            if (input[13] >= -1.5) {
                if (input[2] >= -0.6830514) {
                    var17 = 0.00015965266;
                } else {
                    if (input[4] >= -0.96710324) {
                        var17 = 0.0043783714;
                    } else {
                        var17 = -0.0097251665;
                    }
                }
            } else {
                var17 = -0.01240981;
            }
        }
        double var18;
        if (input[0] >= -0.52086776) {
            if (input[2] >= 0.18923602) {
                if (input[1] >= 0.41467455) {
                    if (input[0] >= 0.28021047) {
                        var18 = -0.005300631;
                    } else {
                        var18 = 0.0018896247;
                    }
                } else {
                    var18 = 0.00009736756;
                }
            } else {
                if (input[1] >= -1.0212978) {
                    if (input[0] >= 0.4890929) {
                        var18 = 0.000044637727;
                    } else {
                        var18 = 0.001655795;
                    }
                } else {
                    if (input[0] >= 1.1712216) {
                        var18 = -0.006605775;
                    } else {
                        var18 = -0.0004686897;
                    }
                }
            }
        } else {
            if (input[0] >= -0.52441216) {
                var18 = -0.013099712;
            } else {
                if (input[4] >= 2.3855038) {
                    var18 = -0.0146767665;
                } else {
                    if (input[1] >= -1.0212978) {
                        var18 = -0.00062068645;
                    } else {
                        var18 = 0.0007842429;
                    }
                }
            }
        }
        double var19;
        if (input[1] >= 1.8480642) {
            if (input[4] >= -1.1766412) {
                if (input[4] >= -0.96710324) {
                    var19 = 0.00011410464;
                } else {
                    if (input[0] >= -0.10045646) {
                        var19 = -0.0013838477;
                    } else {
                        var19 = -0.0074282773;
                    }
                }
            } else {
                if (input[0] >= -0.45853385) {
                    var19 = 0.018343974;
                } else {
                    var19 = -0.008483722;
                }
            }
        } else {
            var19 = -0.000010605317;
        }
        double var20;
        if (input[0] >= 1.2166369) {
            var20 = 0.00079109485;
        } else {
            if (input[0] >= -0.8936898) {
                if (input[0] >= -0.8782363) {
                    var20 = -0.000063889514;
                } else {
                    var20 = -0.0056543825;
                }
            } else {
                if (input[1] >= 0.41467455) {
                    if (input[0] >= -0.91349113) {
                        var20 = 0.01651545;
                    } else {
                        var20 = 0.0027585253;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var20 = 0.00085217034;
                    } else {
                        var20 = -0.0015314965;
                    }
                }
            }
        }
        double var21;
        if (input[0] >= -0.73069537) {
            if (input[0] >= -0.7198732) {
                var21 = 0.00011614944;
            } else {
                if (input[5] >= 0.5) {
                    var21 = -0.0010739389;
                } else {
                    if (input[11] >= 0.5) {
                        var21 = 0.013389875;
                    } else {
                        var21 = 0.00023367196;
                    }
                }
            }
        } else {
            if (input[0] >= -0.7356575) {
                if (input[1] >= 1.1313694) {
                    var21 = -0.033301212;
                } else {
                    var21 = -0.0078398;
                }
            } else {
                var21 = -0.00020899897;
            }
        }
        double var22;
        if (input[0] >= -1.2711904) {
            if (input[1] >= 4.3597245) {
                if (input[0] >= 0.8905536) {
                    var22 = 0.0070436304;
                } else {
                    if (input[3] >= 5.574662) {
                        var22 = 0.016635625;
                    } else {
                        var22 = -0.007339597;
                    }
                }
            } else {
                if (input[0] >= -1.2382985) {
                    var22 = 0.00010624143;
                } else {
                    if (input[1] >= -0.6623047) {
                        var22 = 0.005582;
                    } else {
                        var22 = -0.0046137744;
                    }
                }
            }
        } else {
            if (input[4] >= -0.33848947) {
                var22 = 0.002969129;
            } else {
                if (input[6] >= 0.5) {
                    var22 = -0.02145454;
                } else {
                    if (input[0] >= -1.2723246) {
                        var22 = -0.030892462;
                    } else {
                        var22 = -0.00027632844;
                    }
                }
            }
        }
        double var23;
        if (input[0] >= -1.3289876) {
            if (input[0] >= -1.2917006) {
                if (input[0] >= -1.2813036) {
                    var23 = -0.00011780583;
                } else {
                    var23 = 0.008452083;
                }
            } else {
                if (input[0] >= -1.2919842) {
                    var23 = -0.052540462;
                } else {
                    if (input[12] >= 0.5) {
                        var23 = -0.010236428;
                    } else {
                        var23 = 0.00020858344;
                    }
                }
            }
        } else {
            var23 = 0.004255832;
        }
        double var24;
        if (input[1] >= -0.30331162) {
            if (input[10] >= 0.5) {
                if (input[2] >= 1.0615234) {
                    var24 = -0.0100902;
                } else {
                    var24 = -0.0012042255;
                }
            } else {
                var24 = 0.00008576525;
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[2] >= -0.6830514) {
                    var24 = -0.000502762;
                } else {
                    var24 = -0.021100601;
                }
            } else {
                var24 = 0.0010400178;
            }
        }
        double var25;
        if (input[11] >= 0.5) {
            if (input[0] >= 0.13739538) {
                if (input[4] >= -0.33848947) {
                    if (input[0] >= 0.37387675) {
                        var25 = 0.002166527;
                    } else {
                        var25 = -0.002766359;
                    }
                } else {
                    var25 = -0.00015311364;
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[1] >= 0.41467455) {
                        var25 = 0.00720539;
                    } else {
                        var25 = -0.0021156128;
                    }
                } else {
                    if (input[0] >= 0.11716877) {
                        var25 = -0.00902415;
                    } else {
                        var25 = -0.00021269245;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0275731) {
                var25 = 0.0002984671;
            } else {
                if (input[4] >= -0.12895153) {
                    if (input[0] >= -1.0625916) {
                        var25 = -0.0059903427;
                    } else {
                        var25 = 0.00065418024;
                    }
                } else {
                    if (input[0] >= -1.0832908) {
                        var25 = -0.01922939;
                    } else {
                        var25 = -0.002801858;
                    }
                }
            }
        }
        double var26;
        if (input[0] >= 0.3135277) {
            if (input[0] >= 0.35464257) {
                var26 = -0.000278259;
            } else {
                if (input[1] >= 2.9250436) {
                    if (input[0] >= 0.31839532) {
                        var26 = 0.01269682;
                    } else {
                        var26 = -0.02675031;
                    }
                } else {
                    var26 = -0.0038670786;
                }
            }
        } else {
            if (input[1] >= 2.5660503) {
                if (input[13] >= 1.5) {
                    var26 = -0.023588367;
                } else {
                    if (input[0] >= 0.2999645) {
                        var26 = 0.033863846;
                    } else {
                        var26 = 0.002727884;
                    }
                }
            } else {
                var26 = 0.00009595473;
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + -0.000108733846 + var22 + var23 + var24 + var25 + var26 + 0.000043286633 + -0.000022022794);
    }
}
