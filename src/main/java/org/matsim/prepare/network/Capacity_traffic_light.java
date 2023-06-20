package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public class Capacity_traffic_light implements FeatureRegressor {
    
    @Override
    public double predict(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 129.80599755865998) / 93.91484389753218;
		data[1] = (ft.getDouble("speed") - 12.726744879967448) / 3.0571847342843816;
		data[2] = (ft.getDouble("numFoes") - 2.4327953343279534) / 0.6498808003630541;
		data[3] = (ft.getDouble("numLanes") - 1.8202902482029024) / 0.9477667668865584;
		data[4] = (ft.getDouble("junctionSize") - 13.943035399430354) / 4.355585031201389;
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
        if (input[13] >= -0.5) {
            if (input[4] >= 0.5870542) {
                if (input[3] >= -0.33794206) {
                    if (input[9] >= 0.5) {
                        var0 = 224.65182;
                    } else {
                        var0 = 407.5918;
                    }
                } else {
                    if (input[4] >= 2.1941862) {
                        var0 = 266.93903;
                    } else {
                        var0 = 470.06732;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[4] >= -0.5608972) {
                        var0 = 508.4261;
                    } else {
                        var0 = 591.2892;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var0 = 464.87048;
                    } else {
                        var0 = 536.3855;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[9] >= 0.5) {
                    if (input[4] >= -0.10171662) {
                        var0 = 198.92749;
                    } else {
                        var0 = 265.99216;
                    }
                } else {
                    if (input[4] >= 0.12787366) {
                        var0 = 283.22607;
                    } else {
                        var0 = 391.39468;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[4] >= -1.0200777) {
                        var0 = 222.13257;
                    } else {
                        var0 = 338.2781;
                    }
                } else {
                    if (input[4] >= 0.47225907) {
                        var0 = 277.19818;
                    } else {
                        var0 = 451.16202;
                    }
                }
            }
        }
        double var1;
        if (input[3] >= -0.33794206) {
            if (input[9] >= 0.5) {
                if (input[3] >= 0.7171699) {
                    if (input[2] >= 0.10341075) {
                        var1 = 71.69372;
                    } else {
                        var1 = 86.87915;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var1 = 135.33931;
                    } else {
                        var1 = 171.50755;
                    }
                }
            } else {
                if (input[3] >= 0.7171699) {
                    if (input[6] >= 0.5) {
                        var1 = 203.98961;
                    } else {
                        var1 = 273.0456;
                    }
                } else {
                    if (input[0] >= -0.8943847) {
                        var1 = 316.98032;
                    } else {
                        var1 = 228.46957;
                    }
                }
            }
        } else {
            if (input[4] >= -0.5608972) {
                if (input[10] >= 0.5) {
                    if (input[0] >= -0.9461337) {
                        var1 = 260.24078;
                    } else {
                        var1 = 203.19333;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var1 = 292.60162;
                    } else {
                        var1 = 246.77379;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.97653353) {
                        var1 = 398.65366;
                    } else {
                        var1 = 248.27354;
                    }
                } else {
                    if (input[0] >= -0.9517239) {
                        var1 = 297.2276;
                    } else {
                        var1 = 225.90425;
                    }
                }
            }
        }
        double var2;
        if (input[13] >= -0.5) {
            if (input[0] >= -0.8468416) {
                if (input[4] >= 0.12787366) {
                    if (input[1] >= -0.9835012) {
                        var2 = 162.2054;
                    } else {
                        var2 = 126.5741;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var2 = 222.84433;
                    } else {
                        var2 = 160.10461;
                    }
                }
            } else {
                if (input[3] >= -0.33794206) {
                    if (input[8] >= 0.5) {
                        var2 = 119.20952;
                    } else {
                        var2 = 54.47133;
                    }
                } else {
                    if (input[1] >= 0.83516544) {
                        var2 = 42.53688;
                    } else {
                        var2 = 145.88853;
                    }
                }
            }
        } else {
            if (input[13] >= -1.5) {
                if (input[4] >= 0.12787366) {
                    if (input[4] >= 1.275825) {
                        var2 = 118.9732;
                    } else {
                        var2 = 70.26795;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var2 = 161.61566;
                    } else {
                        var2 = 110.40671;
                    }
                }
            } else {
                if (input[4] >= 0.35746396) {
                    if (input[4] >= 1.7350056) {
                        var2 = 77.116196;
                    } else {
                        var2 = 29.096216;
                    }
                } else {
                    if (input[4] >= -1.2496681) {
                        var2 = 55.91672;
                    } else {
                        var2 = 105.103195;
                    }
                }
            }
        }
        double var3;
        if (input[3] >= -0.33794206) {
            if (input[9] >= 0.5) {
                if (input[13] >= -0.5) {
                    if (input[12] >= 0.5) {
                        var3 = -33.556305;
                    } else {
                        var3 = -0.317025;
                    }
                } else {
                    if (input[3] >= 0.7171699) {
                        var3 = 23.047297;
                    } else {
                        var3 = 56.43252;
                    }
                }
            } else {
                if (input[4] >= 0.5870542) {
                    if (input[12] >= 0.5) {
                        var3 = 42.72369;
                    } else {
                        var3 = 78.06834;
                    }
                } else {
                    if (input[3] >= 1.7722819) {
                        var3 = 55.26218;
                    } else {
                        var3 = 124.7739;
                    }
                }
            }
        } else {
            if (input[1] >= -0.9835012) {
                if (input[1] >= 0.83516544) {
                    if (input[1] >= 2.6505613) {
                        var3 = -62.594543;
                    } else {
                        var3 = 56.0878;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var3 = 119.11364;
                    } else {
                        var3 = 150.74219;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[7] >= 0.5) {
                        var3 = 92.91759;
                    } else {
                        var3 = 132.12279;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var3 = 95.25986;
                    } else {
                        var3 = 61.783886;
                    }
                }
            }
        }
        double var4;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.9835012) {
                if (input[3] >= 1.7722819) {
                    if (input[12] >= 0.5) {
                        var4 = 12.783765;
                    } else {
                        var4 = -93.11116;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var4 = 67.046234;
                    } else {
                        var4 = 37.769726;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[4] >= 1.5054154) {
                        var4 = 75.93732;
                    } else {
                        var4 = 0.7443254;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var4 = 17.272245;
                    } else {
                        var4 = 52.876392;
                    }
                }
            }
        } else {
            if (input[4] >= 0.35746396) {
                if (input[4] >= 1.7350056) {
                    if (input[12] >= 0.5) {
                        var4 = 96.74862;
                    } else {
                        var4 = 25.508215;
                    }
                } else {
                    if (input[1] >= -0.5288346) {
                        var4 = 18.472343;
                    } else {
                        var4 = -7.1366906;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[1] >= -0.5288346) {
                        var4 = 51.186836;
                    } else {
                        var4 = 3.5287051;
                    }
                } else {
                    if (input[1] >= 1.7428633) {
                        var4 = -26.816605;
                    } else {
                        var4 = 18.424143;
                    }
                }
            }
        }
        double var5;
        if (input[0] >= -0.7748615) {
            if (input[6] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var5 = 46.790287;
                    } else {
                        var5 = 18.446743;
                    }
                } else {
                    if (input[1] >= -0.5288346) {
                        var5 = 61.3345;
                    } else {
                        var5 = 18.528656;
                    }
                }
            } else {
                if (input[1] >= -0.5288346) {
                    if (input[13] >= -0.5) {
                        var5 = 86.25778;
                    } else {
                        var5 = 44.499775;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var5 = -34.99748;
                    } else {
                        var5 = 32.22757;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0982928) {
                if (input[0] >= -0.9774386) {
                    if (input[1] >= 0.83516544) {
                        var5 = -81.89008;
                    } else {
                        var5 = 14.291702;
                    }
                } else {
                    if (input[0] >= -1.0580436) {
                        var5 = -75.43823;
                    } else {
                        var5 = 8.673592;
                    }
                }
            } else {
                if (input[0] >= -1.2741436) {
                    if (input[0] >= -1.2494404) {
                        var5 = 43.11906;
                    } else {
                        var5 = 104.4374;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var5 = -8.510905;
                    } else {
                        var5 = -127.549;
                    }
                }
            }
        }
        double var6;
        if (input[3] >= -0.33794206) {
            if (input[8] >= 0.5) {
                if (input[0] >= -0.8926277) {
                    if (input[4] >= 0.5870542) {
                        var6 = 9.519523;
                    } else {
                        var6 = 44.73279;
                    }
                } else {
                    if (input[0] >= -1.0702887) {
                        var6 = -59.180485;
                    } else {
                        var6 = 34.279522;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[9] >= 0.5) {
                        var6 = -43.567795;
                    } else {
                        var6 = 4.1955934;
                    }
                } else {
                    if (input[4] >= -1.7088487) {
                        var6 = 5.0982404;
                    } else {
                        var6 = 46.02345;
                    }
                }
            }
        } else {
            if (input[2] >= 0.10341075) {
                if (input[4] >= -0.3313069) {
                    if (input[1] >= 1.7428633) {
                        var6 = -78.26265;
                    } else {
                        var6 = 23.049074;
                    }
                } else {
                    if (input[4] >= -0.5608972) {
                        var6 = -58.386192;
                    } else {
                        var6 = 20.705666;
                    }
                }
            } else {
                if (input[0] >= -1.2875068) {
                    if (input[0] >= -1.1684096) {
                        var6 = 33.15232;
                    } else {
                        var6 = 82.44034;
                    }
                } else {
                    var6 = -81.37138;
                }
            }
        }
        double var7;
        if (input[3] >= 0.7171699) {
            if (input[8] >= 0.5) {
                if (input[0] >= -0.9773854) {
                    if (input[13] >= -1.5) {
                        var7 = 12.552032;
                    } else {
                        var7 = -25.596859;
                    }
                } else {
                    if (input[0] >= -1.2352254) {
                        var7 = -72.56144;
                    } else {
                        var7 = 50.402508;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[4] >= -0.10171662) {
                        var7 = -38.618782;
                    } else {
                        var7 = -108.48937;
                    }
                } else {
                    if (input[1] >= 0.83516544) {
                        var7 = -28.128963;
                    } else {
                        var7 = -6.8063025;
                    }
                }
            }
        } else {
            if (input[4] >= 2.1941862) {
                if (input[4] >= 2.6533668) {
                    var7 = -140.53485;
                } else {
                    if (input[0] >= 0.58876747) {
                        var7 = -65.549675;
                    } else {
                        var7 = -31.522371;
                    }
                }
            } else {
                if (input[13] >= 1.5) {
                    if (input[0] >= -0.86861664) {
                        var7 = 45.287094;
                    } else {
                        var7 = -11.449074;
                    }
                } else {
                    if (input[1] >= -0.07416786) {
                        var7 = 16.477478;
                    } else {
                        var7 = 3.814261;
                    }
                }
            }
        }
        double var8;
        if (input[13] >= -0.5) {
            if (input[4] >= 0.5870542) {
                if (input[12] >= 0.5) {
                    if (input[4] >= 1.7350056) {
                        var8 = 25.689985;
                    } else {
                        var8 = -16.669868;
                    }
                } else {
                    if (input[4] >= 2.1941862) {
                        var8 = -50.44824;
                    } else {
                        var8 = 8.866128;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[3] >= -0.33794206) {
                        var8 = -59.90474;
                    } else {
                        var8 = 10.413151;
                    }
                } else {
                    if (input[3] >= 1.7722819) {
                        var8 = -51.409363;
                    } else {
                        var8 = 27.145306;
                    }
                }
            }
        } else {
            if (input[4] >= -1.0200777) {
                if (input[4] >= 1.275825) {
                    if (input[3] >= 1.7722819) {
                        var8 = 7.392889;
                    } else {
                        var8 = 35.742966;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var8 = -8.503186;
                    } else {
                        var8 = 25.444883;
                    }
                }
            } else {
                if (input[2] >= -2.9740767) {
                    if (input[1] >= 2.6505613) {
                        var8 = -39.037323;
                    } else {
                        var8 = 7.018694;
                    }
                } else {
                    if (input[4] >= -2.053234) {
                        var8 = 65.87245;
                    } else {
                        var8 = 38.53956;
                    }
                }
            }
        }
        double var9;
        if (input[0] >= -0.9311733) {
            if (input[13] >= -1.5) {
                if (input[1] >= -0.9835012) {
                    if (input[0] >= -0.42965516) {
                        var9 = 14.089063;
                    } else {
                        var9 = 2.4148993;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var9 = -9.888995;
                    } else {
                        var9 = 3.0669236;
                    }
                }
            } else {
                if (input[3] >= 2.8273938) {
                    if (input[0] >= -0.28500286) {
                        var9 = -8.0165825;
                    } else {
                        var9 = 16.10163;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var9 = -6.1877437;
                    } else {
                        var9 = -23.072315;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0625689) {
                if (input[1] >= -0.5288346) {
                    if (input[7] >= 0.5) {
                        var9 = -42.525307;
                    } else {
                        var9 = -128.29193;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var9 = 25.677942;
                    } else {
                        var9 = 91.067474;
                    }
                }
            } else {
                if (input[3] >= 0.7171699) {
                    if (input[0] >= -1.1198016) {
                        var9 = -74.645874;
                    } else {
                        var9 = -2.742765;
                    }
                } else {
                    if (input[2] >= -2.9740767) {
                        var9 = 14.129375;
                    } else {
                        var9 = 96.81832;
                    }
                }
            }
        }
        double var10;
        if (input[0] >= 1.2169429) {
            if (input[4] >= -1.4792583) {
                if (input[1] >= -0.9835012) {
                    if (input[0] >= 3.7753243) {
                        var10 = -51.325012;
                    } else {
                        var10 = 18.297436;
                    }
                } else {
                    if (input[4] >= 1.1610299) {
                        var10 = -34.578545;
                    } else {
                        var10 = 0.42790878;
                    }
                }
            } else {
                if (input[3] >= 0.7171699) {
                    var10 = 94.79976;
                } else {
                    var10 = 51.840282;
                }
            }
        } else {
            if (input[4] >= -2.6272097) {
                if (input[4] >= -1.0200777) {
                    if (input[0] >= -0.9213772) {
                        var10 = 0.45564803;
                    } else {
                        var10 = -11.603626;
                    }
                } else {
                    if (input[1] >= 0.83516544) {
                        var10 = -9.093723;
                    } else {
                        var10 = 8.463709;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    var10 = 41.131725;
                } else {
                    if (input[1] >= 0.83516544) {
                        var10 = 0.1868596;
                    } else {
                        var10 = -107.20009;
                    }
                }
            }
        }
        double var11;
        if (input[4] >= -2.6272097) {
            if (input[4] >= -1.7088487) {
                if (input[4] >= -1.4792583) {
                    if (input[6] >= 0.5) {
                        var11 = -0.39197782;
                    } else {
                        var11 = 7.9586635;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var11 = 4.4565954;
                    } else {
                        var11 = -50.936096;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var11 = 26.809788;
                    } else {
                        var11 = 70.67542;
                    }
                } else {
                    if (input[0] >= -1.0989318) {
                        var11 = -8.188584;
                    } else {
                        var11 = 72.983864;
                    }
                }
            }
        } else {
            if (input[0] >= -0.75313973) {
                var11 = 23.079987;
            } else {
                if (input[2] >= -2.9740767) {
                    var11 = -120.03712;
                } else {
                    var11 = -0.3630173;
                }
            }
        }
        double var12;
        if (input[0] >= -1.2706298) {
            if (input[13] >= 1.5) {
                if (input[13] >= 2.5) {
                    if (input[0] >= -0.88932693) {
                        var12 = 0.5769238;
                    } else {
                        var12 = -75.02654;
                    }
                } else {
                    if (input[3] >= -0.33794206) {
                        var12 = 52.111042;
                    } else {
                        var12 = 16.496164;
                    }
                }
            } else {
                if (input[0] >= -1.2641878) {
                    if (input[0] >= -1.2569472) {
                        var12 = -0.4004818;
                    } else {
                        var12 = -97.72868;
                    }
                } else {
                    var12 = 68.24495;
                }
            }
        } else {
            if (input[4] >= -0.79048747) {
                var12 = -88.79036;
            } else {
                var12 = 9.185803;
            }
        }
        double var13;
        if (input[0] >= -1.1236882) {
            if (input[0] >= -0.98893845) {
                if (input[4] >= 0.5870542) {
                    if (input[4] >= 1.0462348) {
                        var13 = 1.9761951;
                    } else {
                        var13 = -7.3105125;
                    }
                } else {
                    if (input[1] >= -0.07416786) {
                        var13 = 4.8168235;
                    } else {
                        var13 = -2.4065704;
                    }
                }
            } else {
                if (input[1] >= -0.5288346) {
                    if (input[0] >= -1.062356) {
                        var13 = -48.349274;
                    } else {
                        var13 = -1.7723162;
                    }
                } else {
                    if (input[13] >= 1.5) {
                        var13 = 110.1326;
                    } else {
                        var13 = 13.919783;
                    }
                }
            }
        } else {
            if (input[0] >= -1.1379564) {
                if (input[10] >= 0.5) {
                    var13 = 95.14222;
                } else {
                    if (input[0] >= -1.1321001) {
                        var13 = 13.761243;
                    } else {
                        var13 = 78.65305;
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[4] >= -1.2496681) {
                        var13 = 54.347008;
                    } else {
                        var13 = -7.648903;
                    }
                } else {
                    if (input[4] >= 0.12787366) {
                        var13 = -35.282864;
                    } else {
                        var13 = 14.316175;
                    }
                }
            }
        }
        double var14;
        if (input[1] >= 1.7428633) {
            if (input[2] >= -1.4353329) {
                if (input[10] >= 0.5) {
                    if (input[2] >= 0.10341075) {
                        var14 = -42.140427;
                    } else {
                        var14 = -6.99837;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var14 = 5.897393;
                    } else {
                        var14 = -18.422873;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    var14 = -119.75891;
                } else {
                    if (input[6] >= 0.5) {
                        var14 = -54.365337;
                    } else {
                        var14 = 13.378394;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[1] >= -0.9835012) {
                    if (input[2] >= -2.9740767) {
                        var14 = 3.3759756;
                    } else {
                        var14 = -13.275985;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var14 = -19.519146;
                    } else {
                        var14 = 0.5295038;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var14 = 15.547491;
                    } else {
                        var14 = -16.596258;
                    }
                } else {
                    if (input[1] >= -0.9835012) {
                        var14 = -18.035519;
                    } else {
                        var14 = 1.7871339;
                    }
                }
            }
        }
        double var15;
        if (input[13] >= -0.5) {
            if (input[1] >= 1.7428633) {
                if (input[0] >= -1.011512) {
                    if (input[2] >= 0.10341075) {
                        var15 = -86.03142;
                    } else {
                        var15 = -17.00819;
                    }
                } else {
                    if (input[0] >= -1.1630856) {
                        var15 = 117.10525;
                    } else {
                        var15 = -49.962036;
                    }
                }
            } else {
                if (input[0] >= -1.0774229) {
                    if (input[0] >= -1.0477684) {
                        var15 = 0.8510537;
                    } else {
                        var15 = -35.560116;
                    }
                } else {
                    if (input[0] >= -1.2741436) {
                        var15 = 13.557737;
                    } else {
                        var15 = -35.838497;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0119913) {
                if (input[0] >= -0.94554806) {
                    if (input[0] >= 0.76445854) {
                        var15 = 21.416334;
                    } else {
                        var15 = -3.806093;
                    }
                } else {
                    if (input[0] >= -0.9927717) {
                        var15 = 15.289816;
                    } else {
                        var15 = 38.990974;
                    }
                }
            } else {
                if (input[1] >= -0.5288346) {
                    if (input[8] >= 0.5) {
                        var15 = -7.27664;
                    } else {
                        var15 = -38.17552;
                    }
                } else {
                    if (input[0] >= -1.0888162) {
                        var15 = 23.208094;
                    } else {
                        var15 = -11.464898;
                    }
                }
            }
        }
        double var16;
        if (input[4] >= -1.0200777) {
            if (input[4] >= -0.10171662) {
                if (input[6] >= 0.5) {
                    if (input[0] >= -1.260674) {
                        var16 = 0.37491155;
                    } else {
                        var16 = -58.50001;
                    }
                } else {
                    if (input[0] >= -0.72455) {
                        var16 = 55.913628;
                    } else {
                        var16 = -27.64284;
                    }
                }
            } else {
                if (input[0] >= -1.1568033) {
                    if (input[0] >= -0.9213772) {
                        var16 = -2.6884477;
                    } else {
                        var16 = -40.32547;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var16 = 40.521194;
                    } else {
                        var16 = -15.701777;
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[0] >= -1.1379032) {
                    if (input[0] >= -1.0655503) {
                        var16 = -4.012107;
                    } else {
                        var16 = 69.25419;
                    }
                } else {
                    if (input[0] >= -1.2152605) {
                        var16 = -98.19192;
                    } else {
                        var16 = 2.3518279;
                    }
                }
            } else {
                if (input[3] >= 0.7171699) {
                    if (input[6] >= 0.5) {
                        var16 = 12.895247;
                    } else {
                        var16 = -26.881197;
                    }
                } else {
                    if (input[0] >= -1.2550306) {
                        var16 = 9.178977;
                    } else {
                        var16 = 70.30398;
                    }
                }
            }
        }
        double var17;
        if (input[4] >= 2.4237766) {
            if (input[3] >= -0.33794206) {
                var17 = -80.09684;
            } else {
                var17 = -12.404519;
            }
        } else {
            if (input[0] >= 1.8111515) {
                if (input[4] >= 1.5054154) {
                    var17 = 60.791477;
                } else {
                    if (input[11] >= 0.5) {
                        var17 = 20.953953;
                    } else {
                        var17 = 2.2383487;
                    }
                }
            } else {
                if (input[0] >= 1.7123917) {
                    if (input[9] >= 0.5) {
                        var17 = -20.070187;
                    } else {
                        var17 = -74.867195;
                    }
                } else {
                    if (input[0] >= -1.0575112) {
                        var17 = -0.81948197;
                    } else {
                        var17 = 7.0526905;
                    }
                }
            }
        }
        double var18;
        if (input[0] >= 3.7753243) {
            if (input[5] >= 0.5) {
                var18 = -60.941498;
            } else {
                var18 = -13.568989;
            }
        } else {
            if (input[4] >= -2.1680293) {
                if (input[4] >= -1.7088487) {
                    if (input[0] >= -1.2990066) {
                        var18 = -0.10436404;
                    } else {
                        var18 = -63.22506;
                    }
                } else {
                    if (input[0] >= -1.0259401) {
                        var18 = 7.0843263;
                    } else {
                        var18 = 55.45533;
                    }
                }
            } else {
                if (input[0] >= -0.83954775) {
                    if (input[12] >= 0.5) {
                        var18 = -24.138493;
                    } else {
                        var18 = 44.06366;
                    }
                } else {
                    if (input[2] >= -2.9740767) {
                        var18 = -76.748184;
                    } else {
                        var18 = 23.676903;
                    }
                }
            }
        }
        double var19;
        if (input[0] >= 3.8257422) {
            var19 = -39.627354;
        } else {
            if (input[0] >= 3.4641914) {
                if (input[0] >= 3.566731) {
                    if (input[1] >= -0.5288346) {
                        var19 = 23.447056;
                    } else {
                        var19 = -20.13806;
                    }
                } else {
                    var19 = 51.28677;
                }
            } else {
                if (input[0] >= -1.2499728) {
                    if (input[0] >= -1.2450747) {
                        var19 = -0.20233697;
                    } else {
                        var19 = -53.597218;
                    }
                } else {
                    if (input[4] >= 0.12787366) {
                        var19 = -11.6509075;
                    } else {
                        var19 = 32.678486;
                    }
                }
            }
        }
        double var20;
        if (input[13] >= 0.5) {
            if (input[4] >= -0.79048747) {
                if (input[0] >= -1.1830504) {
                    if (input[0] >= -1.0597472) {
                        var20 = 1.3912898;
                    } else {
                        var20 = 46.39079;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var20 = -34.88642;
                    } else {
                        var20 = -101.56466;
                    }
                }
            } else {
                if (input[0] >= -1.2426257) {
                    if (input[0] >= -1.150734) {
                        var20 = 10.596602;
                    } else {
                        var20 = -44.254642;
                    }
                } else {
                    var20 = 73.36145;
                }
            }
        } else {
            if (input[4] >= -1.7088487) {
                if (input[7] >= 0.5) {
                    if (input[4] >= -1.0200777) {
                        var20 = -1.6756858;
                    } else {
                        var20 = 6.604386;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var20 = -17.679983;
                    } else {
                        var20 = 24.405869;
                    }
                }
            } else {
                if (input[0] >= -1.1617013) {
                    if (input[9] >= 0.5) {
                        var20 = -5.959808;
                    } else {
                        var20 = 22.824348;
                    }
                } else {
                    var20 = 69.61678;
                }
            }
        }
        double var21;
        if (input[0] >= -1.0009173) {
            if (input[0] >= -0.9953804) {
                if (input[0] >= -0.86052424) {
                    if (input[3] >= -0.33794206) {
                        var21 = -1.8168283;
                    } else {
                        var21 = 2.239115;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var21 = 0.9880572;
                    } else {
                        var21 = 44.44058;
                    }
                }
            } else {
                var21 = 75.65577;
            }
        } else {
            if (input[0] >= -1.0028341) {
                var21 = -122.3698;
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -1.0112458) {
                        var21 = 56.77315;
                    } else {
                        var21 = -0.19232766;
                    }
                } else {
                    if (input[0] >= -1.1123481) {
                        var21 = -65.57257;
                    } else {
                        var21 = -4.9006877;
                    }
                }
            }
        }
        double var22;
        if (input[1] >= 1.7428633) {
            if (input[2] >= -1.4353329) {
                if (input[0] >= -0.17804426) {
                    if (input[0] >= 1.6686819) {
                        var22 = -31.170115;
                    } else {
                        var22 = 16.99719;
                    }
                } else {
                    if (input[0] >= -1.0116186) {
                        var22 = -9.644373;
                    } else {
                        var22 = 14.805026;
                    }
                }
            } else {
                if (input[4] >= -1.4792583) {
                    var22 = -99.424866;
                } else {
                    if (input[0] >= -0.5295329) {
                        var22 = 25.02504;
                    } else {
                        var22 = -29.19082;
                    }
                }
            }
        } else {
            if (input[3] >= 0.7171699) {
                if (input[4] >= -0.10171662) {
                    if (input[5] >= 0.5) {
                        var22 = 0.2912454;
                    } else {
                        var22 = 55.96404;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var22 = -6.3860674;
                    } else {
                        var22 = -25.307865;
                    }
                }
            } else {
                if (input[4] >= 2.6533668) {
                    var22 = -73.42045;
                } else {
                    if (input[4] >= 1.0462348) {
                        var22 = 11.681308;
                    } else {
                        var22 = 0.84437484;
                    }
                }
            }
        }
        double var23;
        if (input[12] >= 0.5) {
            if (input[1] >= -0.5288346) {
                if (input[4] >= 1.7350056) {
                    if (input[0] >= -0.57963145) {
                        var23 = -0.15765445;
                    } else {
                        var23 = 77.6984;
                    }
                } else {
                    if (input[0] >= -1.0010239) {
                        var23 = -0.77552664;
                    } else {
                        var23 = -17.185593;
                    }
                }
            } else {
                if (input[4] >= 1.5054154) {
                    var23 = 30.375347;
                } else {
                    if (input[9] >= 0.5) {
                        var23 = -7.418737;
                    } else {
                        var23 = -24.14621;
                    }
                }
            }
        } else {
            if (input[3] >= 1.7722819) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -0.7590493) {
                        var23 = -1.9769775;
                    } else {
                        var23 = -48.565327;
                    }
                } else {
                    if (input[0] >= -0.37817234) {
                        var23 = -13.529393;
                    } else {
                        var23 = -64.68633;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[7] >= 0.5) {
                        var23 = 2.4303782;
                    } else {
                        var23 = -3.6476872;
                    }
                } else {
                    if (input[0] >= -1.2170174) {
                        var23 = 5.604375;
                    } else {
                        var23 = 63.699448;
                    }
                }
            }
        }
        double var24;
        if (input[8] >= 0.5) {
            if (input[0] >= -1.1220909) {
                if (input[3] >= -0.33794206) {
                    if (input[0] >= -1.1046283) {
                        var24 = 4.025825;
                    } else {
                        var24 = -79.838806;
                    }
                } else {
                    if (input[4] >= 1.0462348) {
                        var24 = 1.3323941;
                    } else {
                        var24 = -41.10608;
                    }
                }
            } else {
                if (input[0] >= -1.1795899) {
                    if (input[4] >= -0.10171662) {
                        var24 = 11.6621475;
                    } else {
                        var24 = 73.19351;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var24 = 35.720444;
                    } else {
                        var24 = -1.448457;
                    }
                }
            }
        } else {
            if (input[4] >= 1.275825) {
                if (input[13] >= 1.5) {
                    if (input[4] >= 1.7350056) {
                        var24 = -47.041462;
                    } else {
                        var24 = 2.4574926;
                    }
                } else {
                    if (input[0] >= 0.2908912) {
                        var24 = -77.838974;
                    } else {
                        var24 = -26.850855;
                    }
                }
            } else {
                if (input[3] >= -0.33794206) {
                    if (input[13] >= -0.5) {
                        var24 = -14.613788;
                    } else {
                        var24 = -0.7339002;
                    }
                } else {
                    if (input[0] >= -0.84822583) {
                        var24 = 3.102525;
                    } else {
                        var24 = -10.016759;
                    }
                }
            }
        }
        double var25;
        if (input[4] >= -0.3313069) {
            if (input[7] >= 0.5) {
                if (input[0] >= -0.9703578) {
                    if (input[0] >= -0.9571543) {
                        var25 = -1.0023346;
                    } else {
                        var25 = -44.909904;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var25 = -8.290587;
                    } else {
                        var25 = 21.403719;
                    }
                }
            } else {
                if (input[0] >= -0.48257545) {
                    if (input[5] >= 0.5) {
                        var25 = 12.508285;
                    } else {
                        var25 = -49.390465;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var25 = -2.9699633;
                    } else {
                        var25 = -62.378845;
                    }
                }
            }
        } else {
            if (input[3] >= 0.7171699) {
                if (input[7] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var25 = -7.2551293;
                    } else {
                        var25 = -37.979134;
                    }
                } else {
                    if (input[0] >= -0.27222532) {
                        var25 = 70.68;
                    } else {
                        var25 = 28.222092;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var25 = 4.0428977;
                    } else {
                        var25 = 26.280685;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var25 = -11.731407;
                    } else {
                        var25 = 3.140283;
                    }
                }
            }
        }
        double var26;
        if (input[0] >= -1.0293474) {
            if (input[0] >= -0.98563755) {
                if (input[0] >= -0.9817511) {
                    if (input[0] >= -0.97578824) {
                        var26 = -0.5121014;
                    } else {
                        var26 = -50.17085;
                    }
                } else {
                    var26 = 47.55406;
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= -1.0095422) {
                        var26 = -15.146154;
                    } else {
                        var26 = 23.119864;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var26 = -35.519726;
                    } else {
                        var26 = -77.7494;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0406342) {
                if (input[13] >= 0.5) {
                    if (input[0] >= -1.0356296) {
                        var26 = -9.901512;
                    } else {
                        var26 = 6.074389;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var26 = 50.30254;
                    } else {
                        var26 = 128.04031;
                    }
                }
            } else {
                if (input[0] >= -1.0580436) {
                    if (input[0] >= -1.0477684) {
                        var26 = 10.030698;
                    } else {
                        var26 = -58.3826;
                    }
                } else {
                    if (input[0] >= -1.0641661) {
                        var26 = 80.867775;
                    } else {
                        var26 = 3.6726596;
                    }
                }
            }
        }
        double var27;
        if (input[0] >= -1.2990066) {
            if (input[13] >= 2.5) {
                if (input[0] >= 0.28556722) {
                    if (input[4] >= 0.12787366) {
                        var27 = 30.306313;
                    } else {
                        var27 = -4.606572;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var27 = -16.401201;
                    } else {
                        var27 = -48.87657;
                    }
                }
            } else {
                if (input[0] >= -1.274676) {
                    if (input[0] >= -0.13082062) {
                        var27 = -1.1041931;
                    } else {
                        var27 = 1.4549177;
                    }
                } else {
                    var27 = 51.093987;
                }
            }
        } else {
            var27 = -35.78399;
        }
        double var28;
        if (input[11] >= 0.5) {
            if (input[4] >= -2.1680293) {
                if (input[3] >= -0.33794206) {
                    if (input[8] >= 0.5) {
                        var28 = 6.522358;
                    } else {
                        var28 = -6.241323;
                    }
                } else {
                    if (input[1] >= 1.7428633) {
                        var28 = 53.72794;
                    } else {
                        var28 = 4.5644407;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[2] >= -1.4353329) {
                        var28 = 77.29673;
                    } else {
                        var28 = 23.499998;
                    }
                } else {
                    if (input[4] >= -2.3976195) {
                        var28 = 4.893454;
                    } else {
                        var28 = -6.504165;
                    }
                }
            }
        } else {
            if (input[4] >= -2.1680293) {
                if (input[13] >= 0.5) {
                    if (input[3] >= -0.33794206) {
                        var28 = 17.276672;
                    } else {
                        var28 = -1.1125884;
                    }
                } else {
                    if (input[2] >= -2.9740767) {
                        var28 = -1.2689964;
                    } else {
                        var28 = -20.01504;
                    }
                }
            } else {
                if (input[2] >= -1.4353329) {
                    if (input[12] >= 0.5) {
                        var28 = -115.39779;
                    } else {
                        var28 = -36.21837;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var28 = 39.86261;
                    } else {
                        var28 = -5.3660254;
                    }
                }
            }
        }
        double var29;
        if (input[0] >= -1.295333) {
            if (input[0] >= -1.2559357) {
                if (input[8] >= 0.5) {
                    if (input[4] >= 0.35746396) {
                        var29 = -1.3964148;
                    } else {
                        var29 = 5.546218;
                    }
                } else {
                    if (input[4] >= 1.5054154) {
                        var29 = -19.953684;
                    } else {
                        var29 = 0.007974442;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[12] >= 0.5) {
                        var29 = -28.164265;
                    } else {
                        var29 = 28.321108;
                    }
                } else {
                    var29 = 64.36744;
                }
            }
        } else {
            var29 = -37.09929;
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
