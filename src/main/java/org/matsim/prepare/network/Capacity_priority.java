package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public class Capacity_priority implements FeatureRegressor {

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 148.0745794277257) / 111.09738260925752;
		data[1] = (ft.getDouble("speed") - 14.667442032560434) / 5.4536411840395225;
		data[2] = (ft.getDouble("numFoes") - 1.1944992599901332) / 1.1479306507188651;
		data[3] = (ft.getDouble("numLanes") - 1.2444499259990134) / 0.6166874690379405;
		data[4] = (ft.getDouble("junctionSize") - 6.785273803650715) / 4.8225180177060665;
		data[5] = ft.getDouble("dir_l");
		data[6] = ft.getDouble("dir_r");
		data[7] = ft.getDouble("dir_s");
		data[8] = ft.getDouble("dir_multiple_s");
		data[9] = ft.getDouble("dir_exclusive");
		data[10] = ft.getDouble("priority_lower");
		data[11] = ft.getDouble("priority_equal");
		data[12] = ft.getDouble("priority_higher");
		data[13] = ft.getDouble("changeNumLanes");

        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);

        return score(data);
    }
    public static double score(double[] input) {
        double var0;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.90718144) {
                if (input[7] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var0 = 528.2315;
                    } else {
                        var0 = 678.9136;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var0 = 545.95953;
                    } else {
                        var0 = 513.5971;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.053012766) {
                        var0 = 532.0486;
                    } else {
                        var0 = 551.90857;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var0 = 530.28046;
                    } else {
                        var0 = 490.33154;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[3] >= 3.6575255) {
                    if (input[3] >= 5.2790923) {
                        var0 = 329.68658;
                    } else {
                        var0 = 407.84097;
                    }
                } else {
                    if (input[1] >= 1.8946164) {
                        var0 = 551.58856;
                    } else {
                        var0 = 463.95148;
                    }
                }
            } else {
                var0 = 344.90738;
            }
        }
        double var1;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.90718144) {
                if (input[7] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var1 = 351.37134;
                    } else {
                        var1 = 452.8441;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var1 = 362.52734;
                    } else {
                        var1 = 341.0468;
                    }
                }
            } else {
                if (input[3] >= 2.0359585) {
                    var1 = 244.8547;
                } else {
                    if (input[7] >= 0.5) {
                        var1 = 364.16348;
                    } else {
                        var1 = 349.63425;
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                var1 = 229.99834;
            } else {
                if (input[3] >= 3.6575255) {
                    if (input[13] >= -1.5) {
                        var1 = 276.54538;
                    } else {
                        var1 = 232.33159;
                    }
                } else {
                    if (input[1] >= 1.8946164) {
                        var1 = 367.8001;
                    } else {
                        var1 = 304.73376;
                    }
                }
            }
        }
        double var2;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.90718144) {
                if (input[7] >= 0.5) {
                    if (input[4] >= 0.5629271) {
                        var2 = 267.97882;
                    } else {
                        var2 = 303.57843;
                    }
                } else {
                    if (input[0] >= -0.7527142) {
                        var2 = 236.92879;
                    } else {
                        var2 = 251.3941;
                    }
                }
            } else {
                if (input[0] >= -0.37489253) {
                    if (input[4] >= -1.0959573) {
                        var2 = 234.57248;
                    } else {
                        var2 = 172.93842;
                    }
                } else {
                    if (input[0] >= -1.0727037) {
                        var2 = 252.1177;
                    } else {
                        var2 = 216.38428;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[3] >= 3.6575255) {
                    if (input[13] >= -1.5) {
                        var2 = 183.27914;
                    } else {
                        var2 = 160.37532;
                    }
                } else {
                    if (input[1] >= 0.62115526) {
                        var2 = 244.39363;
                    } else {
                        var2 = 199.57362;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    var2 = 153.73575;
                } else {
                    var2 = 126.66589;
                }
            }
        }
        double var3;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.39743024) {
                if (input[3] >= 3.6575255) {
                    if (input[0] >= -0.9738265) {
                        var3 = 10.114798;
                    } else {
                        var3 = 74.12102;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var3 = 182.49957;
                    } else {
                        var3 = 205.18987;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[3] >= 0.41439155) {
                        var3 = 50.8256;
                    } else {
                        var3 = 156.43225;
                    }
                } else {
                    if (input[0] >= -1.0977719) {
                        var3 = 171.65904;
                    } else {
                        var3 = 138.68129;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[3] >= 3.6575255) {
                    if (input[3] >= 5.2790923) {
                        var3 = 83.71184;
                    } else {
                        var3 = 122.21861;
                    }
                } else {
                    if (input[0] >= -0.08721699) {
                        var3 = 160.40189;
                    } else {
                        var3 = 144.6932;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    var3 = 76.86066;
                } else {
                    var3 = 102.30043;
                }
            }
        }
        double var4;
        if (input[7] >= 0.5) {
            if (input[13] >= -0.5) {
                if (input[1] >= -0.39743024) {
                    if (input[0] >= -1.0632976) {
                        var4 = 137.5552;
                    } else {
                        var4 = 98.03021;
                    }
                } else {
                    if (input[0] >= -0.6347996) {
                        var4 = 104.684204;
                    } else {
                        var4 = 120.135826;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    var4 = 68.47832;
                } else {
                    if (input[1] >= 1.8946164) {
                        var4 = 105.024254;
                    } else {
                        var4 = 82.94903;
                    }
                }
            }
        } else {
            if (input[2] >= -0.60500103) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -1.187963) {
                        var4 = 101.44326;
                    } else {
                        var4 = 42.216213;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var4 = 154.08447;
                    } else {
                        var4 = 111.6845;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -0.8885968) {
                        var4 = 124.653305;
                    } else {
                        var4 = 38.009064;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var4 = 99.03142;
                    } else {
                        var4 = 33.777233;
                    }
                }
            }
        }
        double var5;
        if (input[13] >= -0.5) {
            if (input[3] >= 2.0359585) {
                if (input[4] >= -0.47387564) {
                    if (input[3] >= 3.6575255) {
                        var5 = -52.25111;
                    } else {
                        var5 = 43.67992;
                    }
                } else {
                    if (input[3] >= 3.6575255) {
                        var5 = -28.815895;
                    } else {
                        var5 = 46.78004;
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var5 = -10.979303;
                    } else {
                        var5 = 63.3409;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var5 = 90.672165;
                    } else {
                        var5 = 64.64078;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[4] >= -0.68123615) {
                    if (input[3] >= 5.2790923) {
                        var5 = 25.173052;
                    } else {
                        var5 = 54.33675;
                    }
                } else {
                    if (input[3] >= 3.6575255) {
                        var5 = 49.93983;
                    } else {
                        var5 = 71.85198;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var5 = 24.934956;
                    } else {
                        var5 = 45.695312;
                    }
                } else {
                    var5 = 19.088688;
                }
            }
        }
        double var6;
        if (input[3] >= 0.41439155) {
            if (input[0] >= -1.0631626) {
                if (input[9] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var6 = -269.20898;
                    } else {
                        var6 = 30.201042;
                    }
                } else {
                    if (input[3] >= 2.0359585) {
                        var6 = 34.990143;
                    } else {
                        var6 = 67.52307;
                    }
                }
            } else {
                if (input[2] >= -0.60500103) {
                    if (input[13] >= -0.5) {
                        var6 = -137.58873;
                    } else {
                        var6 = 38.572998;
                    }
                } else {
                    if (input[0] >= -1.2234273) {
                        var6 = -8.754881;
                    } else {
                        var6 = -176.93155;
                    }
                }
            }
        } else {
            if (input[2] >= 1.1372645) {
                if (input[12] >= 0.5) {
                    if (input[1] >= -0.65230584) {
                        var6 = 73.2211;
                    } else {
                        var6 = 43.449955;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var6 = 10.302028;
                    } else {
                        var6 = 39.984016;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[1] >= -0.39743024) {
                        var6 = 66.583626;
                    } else {
                        var6 = 48.81097;
                    }
                } else {
                    if (input[0] >= -0.7527142) {
                        var6 = 44.597507;
                    } else {
                        var6 = 53.671276;
                    }
                }
            }
        }
        double var7;
        if (input[3] >= 0.41439155) {
            if (input[0] >= -1.1372867) {
                if (input[8] >= 0.5) {
                    if (input[3] >= 3.6575255) {
                        var7 = 7.308279;
                    } else {
                        var7 = 37.1175;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var7 = -128.86482;
                    } else {
                        var7 = 20.46987;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var7 = 21.48796;
                    } else {
                        var7 = -70.786705;
                    }
                } else {
                    var7 = 33.37381;
                }
            }
        } else {
            if (input[2] >= 1.1372645) {
                if (input[12] >= 0.5) {
                    if (input[1] >= -0.65230584) {
                        var7 = 49.545773;
                    } else {
                        var7 = 28.931652;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var7 = 7.2723947;
                    } else {
                        var7 = 27.238398;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[1] >= -0.39743024) {
                        var7 = 44.53098;
                    } else {
                        var7 = 33.792645;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var7 = 33.040085;
                    } else {
                        var7 = 16.181316;
                    }
                }
            }
        }
        double var8;
        if (input[0] >= -1.1373768) {
            if (input[10] >= 0.5) {
                if (input[0] >= -1.0084357) {
                    if (input[1] >= -0.39743024) {
                        var8 = 8.954352;
                    } else {
                        var8 = 24.357721;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var8 = -2.263031;
                    } else {
                        var8 = -71.02918;
                    }
                }
            } else {
                if (input[3] >= 0.41439155) {
                    if (input[8] >= 0.5) {
                        var8 = 23.2148;
                    } else {
                        var8 = 5.490529;
                    }
                } else {
                    if (input[0] >= 0.47796285) {
                        var8 = 19.852242;
                    } else {
                        var8 = 30.092987;
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                if (input[10] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var8 = -2.0775354;
                    } else {
                        var8 = -63.5745;
                    }
                } else {
                    if (input[0] >= -1.1523635) {
                        var8 = -2.125128;
                    } else {
                        var8 = 25.38647;
                    }
                }
            } else {
                if (input[0] >= -1.1417873) {
                    var8 = -146.80832;
                } else {
                    if (input[0] >= -1.1794119) {
                        var8 = 6.429794;
                    } else {
                        var8 = -66.40415;
                    }
                }
            }
        }
        double var9;
        if (input[2] >= 1.1372645) {
            if (input[12] >= 0.5) {
                if (input[3] >= 3.6575255) {
                    var9 = -48.911915;
                } else {
                    if (input[7] >= 0.5) {
                        var9 = 18.719929;
                    } else {
                        var9 = -19.283041;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    var9 = -66.93798;
                } else {
                    if (input[4] >= 3.2586143) {
                        var9 = -31.181293;
                    } else {
                        var9 = 4.2541704;
                    }
                }
            }
        } else {
            if (input[3] >= 0.41439155) {
                if (input[10] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var9 = -57.253754;
                    } else {
                        var9 = 44.259327;
                    }
                } else {
                    if (input[4] >= 0.77028763) {
                        var9 = 34.63077;
                    } else {
                        var9 = 10.169308;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[4] >= -0.8885968) {
                        var9 = 22.80177;
                    } else {
                        var9 = 14.995254;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var9 = 14.998435;
                    } else {
                        var9 = 4.0855465;
                    }
                }
            }
        }
        double var10;
        if (input[1] >= 0.112320915) {
            if (input[3] >= 0.41439155) {
                if (input[10] >= 0.5) {
                    var10 = 57.883648;
                } else {
                    if (input[2] >= -0.60500103) {
                        var10 = -4.4244;
                    } else {
                        var10 = 10.891248;
                    }
                }
            } else {
                if (input[13] >= 1.5) {
                    if (input[11] >= 0.5) {
                        var10 = 16.430185;
                    } else {
                        var10 = 28.339996;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var10 = 23.248491;
                    } else {
                        var10 = 19.238781;
                    }
                }
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[2] >= -0.60500103) {
                    if (input[1] >= -0.65230584) {
                        var10 = 17.401398;
                    } else {
                        var10 = 4.569379;
                    }
                } else {
                    if (input[3] >= 0.41439155) {
                        var10 = -38.15419;
                    } else {
                        var10 = -52.68213;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[2] >= -0.60500103) {
                        var10 = 3.4235497;
                    } else {
                        var10 = 16.195328;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var10 = 6.5853243;
                    } else {
                        var10 = -0.56392515;
                    }
                }
            }
        }
        double var11;
        if (input[0] >= -1.1373768) {
            if (input[0] >= 0.55411226) {
                if (input[1] >= 0.112320915) {
                    if (input[1] >= 2.9132018) {
                        var11 = 0.62620103;
                    } else {
                        var11 = 14.878931;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var11 = -2.5142908;
                    } else {
                        var11 = 6.8762465;
                    }
                }
            } else {
                if (input[2] >= 1.1372645) {
                    if (input[0] >= -0.8838154) {
                        var11 = 6.8013735;
                    } else {
                        var11 = -15.94711;
                    }
                } else {
                    if (input[1] >= -0.90718144) {
                        var11 = 10.624735;
                    } else {
                        var11 = 5.6352816;
                    }
                }
            }
        } else {
            if (input[3] >= 0.41439155) {
                if (input[6] >= 0.5) {
                    if (input[0] >= -1.1603746) {
                        var11 = 8.050981;
                    } else {
                        var11 = -116.49522;
                    }
                } else {
                    if (input[0] >= -1.1650553) {
                        var11 = -34.69784;
                    } else {
                        var11 = 6.8808713;
                    }
                }
            } else {
                if (input[0] >= -1.1523635) {
                    if (input[10] >= 0.5) {
                        var11 = -87.16453;
                    } else {
                        var11 = -13.80964;
                    }
                } else {
                    if (input[2] >= 0.2661317) {
                        var11 = -9.089337;
                    } else {
                        var11 = 11.343403;
                    }
                }
            }
        }
        double var12;
        if (input[3] >= 3.6575255) {
            if (input[13] >= -0.5) {
                if (input[0] >= -0.9738265) {
                    if (input[3] >= 5.2790923) {
                        var12 = -119.68611;
                    } else {
                        var12 = -42.352478;
                    }
                } else {
                    var12 = 16.52595;
                }
            } else {
                if (input[13] >= -1.5) {
                    if (input[3] >= 5.2790923) {
                        var12 = -30.987036;
                    } else {
                        var12 = 5.687293;
                    }
                } else {
                    if (input[1] >= 0.8760308) {
                        var12 = 11.329675;
                    } else {
                        var12 = -29.204445;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0579419) {
                if (input[0] >= 0.0083748195) {
                    if (input[1] >= 0.112320915) {
                        var12 = 10.043247;
                    } else {
                        var12 = 0.8739378;
                    }
                } else {
                    if (input[3] >= 2.0359585) {
                        var12 = -4.389075;
                    } else {
                        var12 = 7.804355;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[4] >= -0.68123615) {
                        var12 = -56.314365;
                    } else {
                        var12 = 1.5425118;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var12 = -25.511044;
                    } else {
                        var12 = 5.712395;
                    }
                }
            }
        }
        double var13;
        if (input[12] >= 0.5) {
            if (input[2] >= -0.60500103) {
                if (input[0] >= -1.0002449) {
                    if (input[0] >= -0.86824346) {
                        var13 = 6.2731028;
                    } else {
                        var13 = 18.373245;
                    }
                } else {
                    if (input[3] >= 0.41439155) {
                        var13 = -39.403736;
                    } else {
                        var13 = 5.872574;
                    }
                }
            } else {
                if (input[4] >= -0.8885968) {
                    if (input[4] >= -0.68123615) {
                        var13 = -12.1119375;
                    } else {
                        var13 = 27.933044;
                    }
                } else {
                    if (input[0] >= -0.28290117) {
                        var13 = 13.355004;
                    } else {
                        var13 = -157.3527;
                    }
                }
            }
        } else {
            if (input[4] >= -0.68123615) {
                if (input[0] >= -1.1889081) {
                    if (input[0] >= -1.1485381) {
                        var13 = 0.27297387;
                    } else {
                        var13 = 32.984024;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var13 = -13.410682;
                    } else {
                        var13 = -76.172905;
                    }
                }
            } else {
                if (input[4] >= -0.8885968) {
                    if (input[0] >= -1.1870629) {
                        var13 = 8.722857;
                    } else {
                        var13 = 37.79114;
                    }
                } else {
                    if (input[0] >= -0.8034355) {
                        var13 = 0.74079645;
                    } else {
                        var13 = 6.788251;
                    }
                }
            }
        }
        double var14;
        if (input[0] >= -1.1613197) {
            if (input[0] >= -1.1592944) {
                if (input[0] >= -1.1579443) {
                    if (input[3] >= 2.0359585) {
                        var14 = -2.9327502;
                    } else {
                        var14 = 2.4178932;
                    }
                } else {
                    var14 = -66.88381;
                }
            } else {
                var14 = 62.04901;
            }
        } else {
            if (input[0] >= -1.16393) {
                var14 = -163.3757;
            } else {
                if (input[10] >= 0.5) {
                    if (input[5] >= 0.5) {
                        var14 = -43.509354;
                    } else {
                        var14 = 17.002201;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var14 = -5.082277;
                    } else {
                        var14 = 25.77814;
                    }
                }
            }
        }
        double var15;
        if (input[1] >= 0.112320915) {
            if (input[0] >= -1.179322) {
                if (input[0] >= -1.1613197) {
                    if (input[0] >= -1.1573143) {
                        var15 = 3.3508396;
                    } else {
                        var15 = 65.48243;
                    }
                } else {
                    if (input[0] >= -1.1664953) {
                        var15 = -109.310425;
                    } else {
                        var15 = -35.480015;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    var15 = 67.554756;
                } else {
                    if (input[4] >= -0.68123615) {
                        var15 = 38.588894;
                    } else {
                        var15 = 18.756956;
                    }
                }
            }
        } else {
            if (input[3] >= 5.2790923) {
                var15 = -61.343594;
            } else {
                if (input[0] >= -1.2025898) {
                    if (input[0] >= -0.28114593) {
                        var15 = -0.42895037;
                    } else {
                        var15 = 2.6195402;
                    }
                } else {
                    if (input[0] >= -1.2156863) {
                        var15 = -47.398;
                    } else {
                        var15 = -1.7252911;
                    }
                }
            }
        }
        double var16;
        if (input[13] >= 2.5) {
            if (input[4] >= -0.47387564) {
                if (input[10] >= 0.5) {
                    var16 = 19.59329;
                } else {
                    var16 = 70.468575;
                }
            } else {
                if (input[1] >= 0.112320915) {
                    var16 = 11.641106;
                } else {
                    var16 = 6.0238376;
                }
            }
        } else {
            if (input[4] >= 1.1850088) {
                if (input[13] >= 0.5) {
                    if (input[4] >= 2.014451) {
                        var16 = 8.684455;
                    } else {
                        var16 = 22.966534;
                    }
                } else {
                    if (input[4] >= 1.3923693) {
                        var16 = -2.7507231;
                    } else {
                        var16 = -77.75282;
                    }
                }
            } else {
                if (input[3] >= 0.41439155) {
                    if (input[4] >= 0.77028763) {
                        var16 = 16.853094;
                    } else {
                        var16 = -2.1627543;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var16 = 2.7756567;
                    } else {
                        var16 = -0.7487754;
                    }
                }
            }
        }
        double var17;
        if (input[0] >= 2.6104612) {
            if (input[2] >= 1.1372645) {
                if (input[0] >= 2.8206823) {
                    if (input[11] >= 0.5) {
                        var17 = -38.839565;
                    } else {
                        var17 = -62.620193;
                    }
                } else {
                    var17 = 7.287822;
                }
            } else {
                if (input[0] >= 2.6351695) {
                    if (input[6] >= 0.5) {
                        var17 = 6.541465;
                    } else {
                        var17 = -4.9654703;
                    }
                } else {
                    var17 = -41.533714;
                }
            }
        } else {
            if (input[1] >= 0.112320915) {
                if (input[0] >= -1.1831024) {
                    if (input[0] >= -1.1613197) {
                        var17 = 2.9192882;
                    } else {
                        var17 = -36.10568;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var17 = 43.377895;
                    } else {
                        var17 = 16.026234;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var17 = -0.56135464;
                    } else {
                        var17 = 5.624888;
                    }
                } else {
                    if (input[0] >= -1.2654626) {
                        var17 = -0.8403878;
                    } else {
                        var17 = 31.056335;
                    }
                }
            }
        }
        double var18;
        if (input[3] >= 3.6575255) {
            if (input[11] >= 0.5) {
                if (input[4] >= -0.68123615) {
                    if (input[0] >= -1.0060055) {
                        var18 = -13.423488;
                    } else {
                        var18 = 48.401585;
                    }
                } else {
                    if (input[0] >= -1.0651429) {
                        var18 = 0.15271015;
                    } else {
                        var18 = -8.452369;
                    }
                }
            } else {
                var18 = -38.282375;
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[10] >= 0.5) {
                    if (input[0] >= 0.031237647) {
                        var18 = -91.87799;
                    } else {
                        var18 = -27.677452;
                    }
                } else {
                    if (input[0] >= -1.0598772) {
                        var18 = 5.6159496;
                    } else {
                        var18 = -13.98993;
                    }
                }
            } else {
                if (input[3] >= 0.41439155) {
                    if (input[2] >= -0.60500103) {
                        var18 = -27.822721;
                    } else {
                        var18 = -2.6279118;
                    }
                } else {
                    if (input[0] >= -0.8071709) {
                        var18 = 0.06455274;
                    } else {
                        var18 = 3.1713042;
                    }
                }
            }
        }
        double var19;
        if (input[0] >= 2.6839554) {
            if (input[4] >= 1.4960496) {
                if (input[0] >= 2.8206823) {
                    var19 = -45.474518;
                } else {
                    var19 = 3.229492;
                }
            } else {
                if (input[3] >= 0.41439155) {
                    if (input[3] >= 2.0359585) {
                        var19 = 1.8878864;
                    } else {
                        var19 = 14.81505;
                    }
                } else {
                    if (input[4] >= 0.6666074) {
                        var19 = 23.195015;
                    } else {
                        var19 = -6.715007;
                    }
                }
            }
        } else {
            if (input[4] >= -0.68123615) {
                if (input[0] >= -1.0604622) {
                    if (input[3] >= 2.0359585) {
                        var19 = -9.758646;
                    } else {
                        var19 = 0.86491394;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var19 = -46.288296;
                    } else {
                        var19 = -2.084998;
                    }
                }
            } else {
                if (input[0] >= -0.9702261) {
                    if (input[10] >= 0.5) {
                        var19 = 13.765902;
                    } else {
                        var19 = 0.071049064;
                    }
                } else {
                    if (input[1] >= -0.39743024) {
                        var19 = 3.4194317;
                    } else {
                        var19 = 33.17832;
                    }
                }
            }
        }
        double var20;
        if (input[0] >= -1.0579419) {
            if (input[3] >= 5.2790923) {
                if (input[13] >= -0.5) {
                    var20 = -69.22155;
                } else {
                    if (input[13] >= -1.5) {
                        var20 = -13.856637;
                    } else {
                        var20 = 0.12842758;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var20 = -1.250055;
                    } else {
                        var20 = 5.624028;
                    }
                } else {
                    if (input[3] >= 0.41439155) {
                        var20 = -6.4796576;
                    } else {
                        var20 = 0.14111054;
                    }
                }
            }
        } else {
            if (input[1] >= 2.9132018) {
                var20 = 78.854515;
            } else {
                if (input[0] >= -1.0641077) {
                    if (input[9] >= 0.5) {
                        var20 = -11.734342;
                    } else {
                        var20 = -150.81587;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var20 = 4.1645727;
                    } else {
                        var20 = -7.5857835;
                    }
                }
            }
        }
        double var21;
        if (input[13] >= -0.5) {
            if (input[3] >= 2.0359585) {
                if (input[3] >= 3.6575255) {
                    if (input[3] >= 5.2790923) {
                        var21 = -46.729935;
                    } else {
                        var21 = -20.255753;
                    }
                } else {
                    if (input[0] >= -1.074099) {
                        var21 = -9.346616;
                    } else {
                        var21 = 18.665245;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[3] >= 0.41439155) {
                        var21 = -147.08301;
                    } else {
                        var21 = 0.3073819;
                    }
                } else {
                    if (input[0] >= -1.010776) {
                        var21 = 4.6676335;
                    } else {
                        var21 = -10.136607;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0566819) {
                if (input[10] >= 0.5) {
                    var21 = -29.316162;
                } else {
                    if (input[4] >= -0.68123615) {
                        var21 = 7.7767005;
                    } else {
                        var21 = 1.0589573;
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    if (input[0] >= -1.1779718) {
                        var21 = 14.425096;
                    } else {
                        var21 = -0.71505195;
                    }
                } else {
                    var21 = 41.934597;
                }
            }
        }
        double var22;
        if (input[0] >= -1.1028125) {
            if (input[0] >= -1.1013722) {
                if (input[0] >= -1.0604622) {
                    if (input[0] >= -1.0407948) {
                        var22 = 0.36555678;
                    } else {
                        var22 = 9.767401;
                    }
                } else {
                    if (input[0] >= -1.0641077) {
                        var22 = -45.325027;
                    } else {
                        var22 = -4.7544327;
                    }
                }
            } else {
                var22 = -175.64833;
            }
        } else {
            if (input[1] >= 2.9132018) {
                var22 = 76.40113;
            } else {
                if (input[0] >= -1.1373768) {
                    if (input[9] >= 0.5) {
                        var22 = 8.45536;
                    } else {
                        var22 = 31.564167;
                    }
                } else {
                    if (input[0] >= -1.1423724) {
                        var22 = -36.197815;
                    } else {
                        var22 = 2.8641822;
                    }
                }
            }
        }
        double var23;
        if (input[13] >= 2.5) {
            if (input[0] >= -0.75563955) {
                if (input[0] >= -0.39190465) {
                    if (input[0] >= 0.7170324) {
                        var23 = 2.352394;
                    } else {
                        var23 = 17.483053;
                    }
                } else {
                    var23 = -8.217469;
                }
            } else {
                var23 = 68.741554;
            }
        } else {
            if (input[1] >= 2.1494918) {
                if (input[0] >= -1.0704535) {
                    if (input[0] >= -1.0414249) {
                        var23 = 2.6117754;
                    } else {
                        var23 = -85.30798;
                    }
                } else {
                    if (input[3] >= 0.41439155) {
                        var23 = 54.56402;
                    } else {
                        var23 = 7.834694;
                    }
                }
            } else {
                if (input[0] >= 0.96159256) {
                    if (input[0] >= 0.9669933) {
                        var23 = -1.7996783;
                    } else {
                        var23 = -64.47756;
                    }
                } else {
                    if (input[0] >= 0.92792845) {
                        var23 = 21.341352;
                    } else {
                        var23 = -0.177293;
                    }
                }
            }
        }
        double var24;
        if (input[4] >= 3.2586143) {
            if (input[0] >= -0.8377297) {
                if (input[0] >= 0.25869575) {
                    if (input[0] >= 1.0008824) {
                        var24 = -25.567339;
                    } else {
                        var24 = 14.744424;
                    }
                } else {
                    if (input[0] >= 0.11539804) {
                        var24 = -58.946354;
                    } else {
                        var24 = 4.666309;
                    }
                }
            } else {
                var24 = -65.78561;
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[4] >= 0.77028763) {
                    if (input[1] >= -0.65230584) {
                        var24 = 6.796015;
                    } else {
                        var24 = -3.8748004;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var24 = 1.5722843;
                    } else {
                        var24 = -5.0968976;
                    }
                }
            } else {
                if (input[0] >= -1.2044351) {
                    if (input[0] >= -1.1927786) {
                        var24 = -0.2537231;
                    } else {
                        var24 = 13.428278;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var24 = -47.856255;
                    } else {
                        var24 = -1.6130934;
                    }
                }
            }
        }
        double var25;
        if (input[5] >= 0.5) {
            if (input[4] >= -1.0959573) {
                if (input[0] >= 0.8400326) {
                    if (input[0] >= 0.9067308) {
                        var25 = -1.2772002;
                    } else {
                        var25 = -22.573118;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var25 = 0.16855976;
                    } else {
                        var25 = 4.5786915;
                    }
                }
            } else {
                var25 = -46.09022;
            }
        } else {
            if (input[2] >= 1.1372645) {
                if (input[11] >= 0.5) {
                    var25 = 63.907482;
                } else {
                    if (input[12] >= 0.5) {
                        var25 = 6.5244155;
                    } else {
                        var25 = -12.243132;
                    }
                }
            } else {
                if (input[4] >= -0.26651508) {
                    if (input[0] >= -1.1603296) {
                        var25 = -2.693653;
                    } else {
                        var25 = -46.41377;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var25 = 4.54096;
                    } else {
                        var25 = -0.46053287;
                    }
                }
            }
        }
        double var26;
        if (input[1] >= 0.112320915) {
            if (input[4] >= -0.8885968) {
                if (input[13] >= -0.5) {
                    if (input[11] >= 0.5) {
                        var26 = -4.120392;
                    } else {
                        var26 = 1.8585286;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var26 = 9.289226;
                    } else {
                        var26 = 2.014836;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[2] >= -0.60500103) {
                        var26 = -22.293093;
                    } else {
                        var26 = 4.087899;
                    }
                } else {
                    var26 = 50.089314;
                }
            }
        } else {
            if (input[4] >= -0.8885968) {
                if (input[4] >= -0.68123615) {
                    if (input[3] >= 0.41439155) {
                        var26 = -4.367316;
                    } else {
                        var26 = -0.08913819;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var26 = 3.4653053;
                    } else {
                        var26 = 22.376299;
                    }
                }
            } else {
                if (input[2] >= -0.60500103) {
                    if (input[7] >= 0.5) {
                        var26 = 4.639402;
                    } else {
                        var26 = 32.232265;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var26 = -75.83786;
                    } else {
                        var26 = -2.4805021;
                    }
                }
            }
        }
        double var27;
        if (input[10] >= 0.5) {
            if (input[1] >= -0.90718144) {
                if (input[2] >= 1.1372645) {
                    if (input[0] >= -0.5392978) {
                        var27 = -65.09315;
                    } else {
                        var27 = -23.705198;
                    }
                } else {
                    if (input[0] >= -1.1443976) {
                        var27 = -3.8233435;
                    } else {
                        var27 = -47.873787;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[0] >= -1.0881857) {
                        var27 = 6.3074617;
                    } else {
                        var27 = 48.21055;
                    }
                } else {
                    if (input[0] >= -1.2313483) {
                        var27 = 3.3293352;
                    } else {
                        var27 = 63.340492;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[1] >= -0.65230584) {
                        var27 = 3.2068865;
                    } else {
                        var27 = -6.119835;
                    }
                } else {
                    if (input[0] >= -1.2661376) {
                        var27 = 0.26134154;
                    } else {
                        var27 = 35.127155;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    var27 = 43.92848;
                } else {
                    if (input[0] >= -1.009786) {
                        var27 = -7.3419294;
                    } else {
                        var27 = 18.001553;
                    }
                }
            }
        }
        double var28;
        if (input[0] >= -1.220052) {
            if (input[0] >= -1.211996) {
                if (input[0] >= -1.0113611) {
                    if (input[0] >= -0.9522239) {
                        var28 = -0.04635202;
                    } else {
                        var28 = 7.458867;
                    }
                } else {
                    if (input[0] >= -1.0132064) {
                        var28 = -78.54606;
                    } else {
                        var28 = -1.7559289;
                    }
                }
            } else {
                if (input[0] >= -1.2156863) {
                    var28 = -72.64668;
                } else {
                    var28 = -3.2740362;
                }
            }
        } else {
            if (input[0] >= -1.2272528) {
                if (input[0] >= -1.2240574) {
                    var28 = 48.41117;
                } else {
                    var28 = 64.93376;
                }
            } else {
                if (input[0] >= -1.2413396) {
                    if (input[0] >= -1.2332835) {
                        var28 = -13.813472;
                    } else {
                        var28 = 57.241142;
                    }
                } else {
                    if (input[0] >= -1.2436349) {
                        var28 = -81.38212;
                    } else {
                        var28 = 3.4469526;
                    }
                }
            }
        }
        double var29;
        if (input[0] >= 3.157864) {
            var29 = -20.65781;
        } else {
            if (input[9] >= 0.5) {
                if (input[0] >= -1.1677555) {
                    if (input[0] >= -1.1532637) {
                        var29 = -0.20954037;
                    } else {
                        var29 = 21.08421;
                    }
                } else {
                    if (input[0] >= -1.1792319) {
                        var29 = -27.783361;
                    } else {
                        var29 = 0.8293829;
                    }
                }
            } else {
                if (input[0] >= -1.1218048) {
                    if (input[0] >= -1.0119011) {
                        var29 = 1.6578834;
                    } else {
                        var29 = -15.785636;
                    }
                } else {
                    if (input[4] >= -0.47387564) {
                        var29 = 52.502968;
                    } else {
                        var29 = -2.1309283;
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
