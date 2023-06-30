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

        for (int i = 0; i < data.length; i++)
            if (Double.isNaN(data[i])) throw new IllegalArgumentException("Invalid data at index: " + i);

        return score(data);
    }
    public static double score(double[] input) {
        double var0;
        if (input[13] >= -0.5) {
            if (input[4] >= 0.5870542) {
                if (input[13] >= 0.5) {
                    if (input[3] >= -0.33794206) {
                        var0 = 367.50482;
                    } else {
                        var0 = 424.35815;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var0 = 206.34163;
                    } else {
                        var0 = 364.96725;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[4] >= -0.5608972) {
                        var0 = 462.75888;
                    } else {
                        var0 = 541.3571;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var0 = 423.2037;
                    } else {
                        var0 = 484.65845;
                    }
                }
            }
        } else {
            if (input[4] >= 0.12787366) {
                if (input[9] >= 0.5) {
                    if (input[3] >= 0.7171699) {
                        var0 = 147.20288;
                    } else {
                        var0 = 192.373;
                    }
                } else {
                    if (input[13] >= -1.5) {
                        var0 = 270.54285;
                    } else {
                        var0 = 219.35835;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[3] >= 0.7171699) {
                        var0 = 185.97235;
                    } else {
                        var0 = 272.20184;
                    }
                } else {
                    if (input[13] >= -1.5) {
                        var0 = 396.2951;
                    } else {
                        var0 = 305.99268;
                    }
                }
            }
        }
        double var1;
        if (input[3] >= -0.33794206) {
            if (input[9] >= 0.5) {
                if (input[6] >= 0.5) {
                    if (input[3] >= 0.7171699) {
                        var1 = 88.558716;
                    } else {
                        var1 = 123.2088;
                    }
                } else {
                    if (input[0] >= -0.88001) {
                        var1 = 194.34872;
                    } else {
                        var1 = 101.6791;
                    }
                }
            } else {
                if (input[3] >= 0.7171699) {
                    if (input[6] >= 0.5) {
                        var1 = 195.41751;
                    } else {
                        var1 = 278.5694;
                    }
                } else {
                    if (input[0] >= -0.9177569) {
                        var1 = 304.28644;
                    } else {
                        var1 = 219.76962;
                    }
                }
            }
        } else {
            if (input[4] >= -0.5608972) {
                if (input[10] >= 0.5) {
                    if (input[0] >= -0.9436314) {
                        var1 = 252.97453;
                    } else {
                        var1 = 198.98586;
                    }
                } else {
                    if (input[0] >= -0.67690045) {
                        var1 = 286.61203;
                    } else {
                        var1 = 250.8239;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.89715314) {
                        var1 = 384.269;
                    } else {
                        var1 = 262.40286;
                    }
                } else {
                    if (input[0] >= -0.9429393) {
                        var1 = 287.37;
                    } else {
                        var1 = 192.14041;
                    }
                }
            }
        }
        double var2;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.9835012) {
                if (input[4] >= 0.5870542) {
                    if (input[13] >= 0.5) {
                        var2 = 181.357;
                    } else {
                        var2 = 126.57919;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var2 = 203.57613;
                    } else {
                        var2 = 150.32642;
                    }
                }
            } else {
                if (input[4] >= -0.5608972) {
                    if (input[12] >= 0.5) {
                        var2 = 96.03897;
                    } else {
                        var2 = 140.53992;
                    }
                } else {
                    if (input[2] >= -1.4353329) {
                        var2 = 171.74016;
                    } else {
                        var2 = 230.64528;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[4] >= -0.10171662) {
                    if (input[4] >= 1.275825) {
                        var2 = 103.24647;
                    } else {
                        var2 = 66.63641;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var2 = 123.71024;
                    } else {
                        var2 = 85.63842;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[13] >= -1.5) {
                        var2 = 173.88304;
                    } else {
                        var2 = 111.6038;
                    }
                } else {
                    if (input[4] >= -1.0200777) {
                        var2 = 59.0332;
                    } else {
                        var2 = 135.35942;
                    }
                }
            }
        }
        double var3;
        if (input[13] >= -0.5) {
            if (input[4] >= 0.12787366) {
                if (input[3] >= -0.33794206) {
                    if (input[9] >= 0.5) {
                        var3 = 11.842827;
                    } else {
                        var3 = 91.307655;
                    }
                } else {
                    if (input[1] >= -0.5288346) {
                        var3 = 114.71714;
                    } else {
                        var3 = 86.89338;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[7] >= 0.5) {
                        var3 = 140.3354;
                    } else {
                        var3 = 45.98516;
                    }
                } else {
                    if (input[3] >= -0.33794206) {
                        var3 = 75.785545;
                    } else {
                        var3 = 120.961426;
                    }
                }
            }
        } else {
            if (input[4] >= 0.35746396) {
                if (input[4] >= 1.275825) {
                    if (input[13] >= -1.5) {
                        var3 = 72.557045;
                    } else {
                        var3 = 29.454908;
                    }
                } else {
                    if (input[13] >= -1.5) {
                        var3 = 42.771923;
                    } else {
                        var3 = 24.680012;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var3 = 54.55675;
                    } else {
                        var3 = 81.069145;
                    }
                } else {
                    if (input[13] >= -1.5) {
                        var3 = 104.61941;
                    } else {
                        var3 = 63.19876;
                    }
                }
            }
        }
        double var4;
        if (input[3] >= -0.33794206) {
            if (input[9] >= 0.5) {
                if (input[13] >= -0.5) {
                    if (input[4] >= 0.5870542) {
                        var4 = 10.065217;
                    } else {
                        var4 = -63.55682;
                    }
                } else {
                    if (input[3] >= 0.7171699) {
                        var4 = 12.907462;
                    } else {
                        var4 = 37.413372;
                    }
                }
            } else {
                if (input[4] >= 0.5870542) {
                    if (input[8] >= 0.5) {
                        var4 = 42.821278;
                    } else {
                        var4 = -51.201416;
                    }
                } else {
                    if (input[3] >= 1.7722819) {
                        var4 = 39.46324;
                    } else {
                        var4 = 86.999664;
                    }
                }
            }
        } else {
            if (input[2] >= 0.10341075) {
                if (input[4] >= 2.079391) {
                    if (input[1] >= -0.5288346) {
                        var4 = -50.248425;
                    } else {
                        var4 = -97.71123;
                    }
                } else {
                    if (input[1] >= 1.7428633) {
                        var4 = -55.380566;
                    } else {
                        var4 = 62.089447;
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[4] >= -1.0200777) {
                        var4 = 78.4107;
                    } else {
                        var4 = 51.70315;
                    }
                } else {
                    if (input[1] >= 0.83516544) {
                        var4 = 25.73279;
                    } else {
                        var4 = 103.682045;
                    }
                }
            }
        }
        double var5;
        if (input[0] >= -0.8952365) {
            if (input[13] >= -0.5) {
                if (input[1] >= -0.9835012) {
                    if (input[6] >= 0.5) {
                        var5 = 50.868576;
                    } else {
                        var5 = 91.90699;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var5 = 47.022156;
                    } else {
                        var5 = 18.278261;
                    }
                }
            } else {
                if (input[1] >= -0.07416786) {
                    if (input[4] >= 0.35746396) {
                        var5 = 17.759998;
                    } else {
                        var5 = 34.367268;
                    }
                } else {
                    if (input[13] >= -1.5) {
                        var5 = -2.7607558;
                    } else {
                        var5 = -42.612473;
                    }
                }
            }
        } else {
            if (input[0] >= -1.062356) {
                if (input[0] >= -0.98590374) {
                    if (input[0] >= -0.8997086) {
                        var5 = -122.39893;
                    } else {
                        var5 = 14.461245;
                    }
                } else {
                    if (input[1] >= -0.5288346) {
                        var5 = -96.585014;
                    } else {
                        var5 = 5.1462226;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[0] >= -1.1253918) {
                        var5 = -24.820234;
                    } else {
                        var5 = 22.29909;
                    }
                } else {
                    if (input[1] >= 1.7428633) {
                        var5 = -63.869198;
                    } else {
                        var5 = 47.73467;
                    }
                }
            }
        }
        double var6;
        if (input[3] >= -0.33794206) {
            if (input[8] >= 0.5) {
                if (input[0] >= -0.77262545) {
                    if (input[4] >= 0.5870542) {
                        var6 = 18.032515;
                    } else {
                        var6 = 55.521206;
                    }
                } else {
                    if (input[0] >= -1.1238478) {
                        var6 = -8.596528;
                    } else {
                        var6 = 44.01731;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[12] >= 0.5) {
                        var6 = -56.772415;
                    } else {
                        var6 = -3.060491;
                    }
                } else {
                    if (input[1] >= 2.6505613) {
                        var6 = -55.203384;
                    } else {
                        var6 = 12.321973;
                    }
                }
            }
        } else {
            if (input[2] >= 0.10341075) {
                if (input[4] >= 2.079391) {
                    if (input[0] >= -0.21568474) {
                        var6 = -72.007034;
                    } else {
                        var6 = -48.37604;
                    }
                } else {
                    if (input[1] >= 0.83516544) {
                        var6 = -54.694817;
                    } else {
                        var6 = 24.901882;
                    }
                }
            } else {
                if (input[0] >= -0.9593371) {
                    if (input[1] >= -0.9835012) {
                        var6 = 49.22337;
                    } else {
                        var6 = 26.828186;
                    }
                } else {
                    if (input[0] >= -1.0187526) {
                        var6 = -68.58256;
                    } else {
                        var6 = 26.439283;
                    }
                }
            }
        }
        double var7;
        if (input[0] >= -0.81649494) {
            if (input[1] >= -0.07416786) {
                if (input[13] >= -0.5) {
                    if (input[4] >= 0.35746396) {
                        var7 = 20.20199;
                    } else {
                        var7 = 35.359833;
                    }
                } else {
                    if (input[4] >= -0.79048747) {
                        var7 = 3.465759;
                    } else {
                        var7 = 17.98743;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var7 = -28.217916;
                    } else {
                        var7 = -5.8268604;
                    }
                } else {
                    if (input[13] >= 1.5) {
                        var7 = 43.058315;
                    } else {
                        var7 = 8.249209;
                    }
                }
            }
        } else {
            if (input[3] >= 0.7171699) {
                if (input[0] >= -0.9773854) {
                    if (input[4] >= -0.10171662) {
                        var7 = 7.316835;
                    } else {
                        var7 = -31.032711;
                    }
                } else {
                    if (input[0] >= -1.0923831) {
                        var7 = -98.45283;
                    } else {
                        var7 = -29.935713;
                    }
                }
            } else {
                if (input[0] >= -1.0580436) {
                    if (input[0] >= -0.9774386) {
                        var7 = 6.3834796;
                    } else {
                        var7 = -47.006844;
                    }
                } else {
                    if (input[0] >= -1.3048097) {
                        var7 = 21.243135;
                    } else {
                        var7 = -110.55501;
                    }
                }
            }
        }
        double var8;
        if (input[4] >= -1.7088487) {
            if (input[1] >= 0.83516544) {
                if (input[7] >= 0.5) {
                    if (input[4] >= -1.2496681) {
                        var8 = 1.5170248;
                    } else {
                        var8 = -33.17703;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var8 = -35.911354;
                    } else {
                        var8 = -76.122894;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[13] >= 1.5) {
                        var8 = 26.433702;
                    } else {
                        var8 = 6.529245;
                    }
                } else {
                    if (input[3] >= 1.7722819) {
                        var8 = -37.337563;
                    } else {
                        var8 = 20.534056;
                    }
                }
            }
        } else {
            if (input[4] >= -2.3976195) {
                if (input[7] >= 0.5) {
                    if (input[4] >= -2.1680293) {
                        var8 = 39.35434;
                    } else {
                        var8 = -10.004152;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var8 = 78.96174;
                    } else {
                        var8 = 33.651665;
                    }
                }
            } else {
                if (input[2] >= -2.9740767) {
                    if (input[10] >= 0.5) {
                        var8 = 1.8065205;
                    } else {
                        var8 = -99.45925;
                    }
                } else {
                    if (input[3] >= -0.33794206) {
                        var8 = 33.425495;
                    } else {
                        var8 = 61.353897;
                    }
                }
            }
        }
        double var9;
        if (input[0] >= -0.77816236) {
            if (input[0] >= 1.1071093) {
                if (input[1] >= -0.9835012) {
                    if (input[0] >= 3.8216429) {
                        var9 = -37.22993;
                    } else {
                        var9 = 24.57778;
                    }
                } else {
                    if (input[0] >= 1.1284053) {
                        var9 = 0.3728113;
                    } else {
                        var9 = 66.340096;
                    }
                }
            } else {
                if (input[13] >= -1.5) {
                    if (input[1] >= -0.9835012) {
                        var9 = 9.357347;
                    } else {
                        var9 = 0.60598314;
                    }
                } else {
                    if (input[3] >= 2.8273938) {
                        var9 = 3.6079726;
                    } else {
                        var9 = -16.304659;
                    }
                }
            }
        } else {
            if (input[13] >= 2.5) {
                var9 = -92.2187;
            } else {
                if (input[1] >= 1.7428633) {
                    if (input[0] >= -1.0156115) {
                        var9 = -87.49487;
                    } else {
                        var9 = -10.97046;
                    }
                } else {
                    if (input[0] >= -1.1236882) {
                        var9 = -6.368576;
                    } else {
                        var9 = 12.208469;
                    }
                }
            }
        }
        double var10;
        if (input[4] >= 2.6533668) {
            var10 = -88.87163;
        } else {
            if (input[4] >= 1.7350056) {
                if (input[12] >= 0.5) {
                    if (input[0] >= -0.57963145) {
                        var10 = 34.971157;
                    } else {
                        var10 = 104.556496;
                    }
                } else {
                    if (input[0] >= 0.056317) {
                        var10 = -37.35852;
                    } else {
                        var10 = 14.457261;
                    }
                }
            } else {
                if (input[13] >= 1.5) {
                    if (input[0] >= -0.83342516) {
                        var10 = 24.28627;
                    } else {
                        var10 = -11.021199;
                    }
                } else {
                    if (input[4] >= -0.5608972) {
                        var10 = -0.095924966;
                    } else {
                        var10 = 6.294528;
                    }
                }
            }
        }
        double var11;
        if (input[4] >= -1.7088487) {
            if (input[0] >= -1.2990066) {
                if (input[2] >= -2.9740767) {
                    if (input[6] >= 0.5) {
                        var11 = 0.9681548;
                    } else {
                        var11 = 11.654723;
                    }
                } else {
                    if (input[3] >= 1.7722819) {
                        var11 = -112.22902;
                    } else {
                        var11 = -4.612288;
                    }
                }
            } else {
                var11 = -103.43151;
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[0] >= -1.0409536) {
                    if (input[0] >= -0.6289314) {
                        var11 = 28.585384;
                    } else {
                        var11 = 76.80479;
                    }
                } else {
                    var11 = -41.350395;
                }
            } else {
                if (input[2] >= -2.9740767) {
                    if (input[4] >= -2.3976195) {
                        var11 = 2.070047;
                    } else {
                        var11 = -54.752262;
                    }
                } else {
                    if (input[0] >= -1.1103783) {
                        var11 = 54.594933;
                    } else {
                        var11 = 6.3451734;
                    }
                }
            }
        }
        double var12;
        if (input[1] >= 1.7428633) {
            if (input[0] >= -1.1630856) {
                if (input[7] >= 0.5) {
                    if (input[0] >= -1.015718) {
                        var12 = -7.1897907;
                    } else {
                        var12 = 30.621687;
                    }
                } else {
                    if (input[0] >= -0.7237514) {
                        var12 = -30.915073;
                    } else {
                        var12 = -95.31315;
                    }
                }
            } else {
                var12 = -131.99312;
            }
        } else {
            if (input[0] >= -1.2741436) {
                if (input[0] >= -1.2439567) {
                    if (input[7] >= 0.5) {
                        var12 = 2.8154845;
                    } else {
                        var12 = -5.132764;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var12 = 123.264465;
                    } else {
                        var12 = 2.819096;
                    }
                }
            } else {
                if (input[4] >= -0.79048747) {
                    var12 = -125.41991;
                } else {
                    var12 = 38.844765;
                }
            }
        }
        double var13;
        if (input[0] >= -0.97573495) {
            if (input[0] >= -0.9735521) {
                if (input[0] >= 1.4593966) {
                    if (input[4] >= -1.4792583) {
                        var13 = 5.815014;
                    } else {
                        var13 = 40.06303;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var13 = 5.9172087;
                    } else {
                        var13 = -1.522723;
                    }
                }
            } else {
                var13 = 94.280464;
            }
        } else {
            if (input[0] >= -1.0580436) {
                if (input[1] >= -0.5288346) {
                    if (input[0] >= -1.0477684) {
                        var13 = -36.895367;
                    } else {
                        var13 = -119.79888;
                    }
                } else {
                    if (input[3] >= -0.33794206) {
                        var13 = 58.00334;
                    } else {
                        var13 = -6.6933064;
                    }
                }
            } else {
                if (input[3] >= 1.7722819) {
                    var13 = -92.35661;
                } else {
                    if (input[0] >= -1.0691174) {
                        var13 = 48.734802;
                    } else {
                        var13 = 2.117843;
                    }
                }
            }
        }
        double var14;
        if (input[11] >= 0.5) {
            if (input[3] >= 1.7722819) {
                if (input[5] >= 0.5) {
                    if (input[13] >= -1.5) {
                        var14 = -7.957535;
                    } else {
                        var14 = -0.5557697;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var14 = -104.637955;
                    } else {
                        var14 = -6.9212604;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[2] >= -1.4353329) {
                        var14 = 28.980356;
                    } else {
                        var14 = -2.3783262;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var14 = 4.044478;
                    } else {
                        var14 = -6.5452895;
                    }
                }
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[7] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var14 = -0.44899765;
                    } else {
                        var14 = -11.136123;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var14 = 13.100871;
                    } else {
                        var14 = 25.965353;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[1] >= -0.5288346) {
                        var14 = 2.1125808;
                    } else {
                        var14 = -7.3830647;
                    }
                } else {
                    if (input[1] >= -0.9835012) {
                        var14 = -18.711391;
                    } else {
                        var14 = -2.7296703;
                    }
                }
            }
        }
        double var15;
        if (input[5] >= 0.5) {
            if (input[6] >= 0.5) {
                if (input[12] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var15 = -0.6816591;
                    } else {
                        var15 = -12.157176;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var15 = -2.0696847;
                    } else {
                        var15 = 7.578963;
                    }
                }
            } else {
                if (input[0] >= -0.70538366) {
                    if (input[8] >= 0.5) {
                        var15 = 26.441994;
                    } else {
                        var15 = 9.369286;
                    }
                } else {
                    if (input[2] >= -1.4353329) {
                        var15 = -12.483892;
                    } else {
                        var15 = 35.922173;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[8] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var15 = 22.111153;
                    } else {
                        var15 = -6.503288;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var15 = -20.640434;
                    } else {
                        var15 = 6.24046;
                    }
                }
            } else {
                if (input[0] >= -1.0711937) {
                    if (input[13] >= 1.5) {
                        var15 = -110.477066;
                    } else {
                        var15 = -18.03973;
                    }
                } else {
                    if (input[0] >= -1.0825338) {
                        var15 = 67.67926;
                    } else {
                        var15 = 5.2958064;
                    }
                }
            }
        }
        double var16;
        if (input[3] >= -0.33794206) {
            if (input[8] >= 0.5) {
                if (input[0] >= -0.8923615) {
                    if (input[0] >= -0.8263444) {
                        var16 = 4.6402855;
                    } else {
                        var16 = 38.941223;
                    }
                } else {
                    if (input[0] >= -0.90295625) {
                        var16 = -112.70789;
                    } else {
                        var16 = -12.201777;
                    }
                }
            } else {
                if (input[4] >= 1.275825) {
                    if (input[3] >= 1.7722819) {
                        var16 = -10.222688;
                    } else {
                        var16 = -67.16906;
                    }
                } else {
                    if (input[0] >= 1.2416995) {
                        var16 = -56.433655;
                    } else {
                        var16 = -7.2505217;
                    }
                }
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[0] >= -0.9555571) {
                    if (input[4] >= 0.8166445) {
                        var16 = -3.7876277;
                    } else {
                        var16 = -63.65588;
                    }
                } else {
                    var16 = 48.45108;
                }
            } else {
                if (input[4] >= 1.5054154) {
                    if (input[0] >= 0.0721292) {
                        var16 = -14.576662;
                    } else {
                        var16 = -54.5959;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var16 = 6.5006685;
                    } else {
                        var16 = -2.639079;
                    }
                }
            }
        }
        double var17;
        if (input[4] >= -1.0200777) {
            if (input[4] >= -0.10171662) {
                if (input[4] >= 0.5870542) {
                    if (input[10] >= 0.5) {
                        var17 = 4.3190055;
                    } else {
                        var17 = -6.19649;
                    }
                } else {
                    if (input[3] >= 0.7171699) {
                        var17 = 23.102257;
                    } else {
                        var17 = 0.5622126;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var17 = 6.520137;
                    } else {
                        var17 = -47.899445;
                    }
                } else {
                    if (input[0] >= -0.9142963) {
                        var17 = 7.2601857;
                    } else {
                        var17 = -32.4989;
                    }
                }
            }
        } else {
            if (input[13] >= 2.5) {
                var17 = -97.06859;
            } else {
                if (input[7] >= 0.5) {
                    if (input[4] >= -1.4792583) {
                        var17 = 13.207709;
                    } else {
                        var17 = -6.060059;
                    }
                } else {
                    if (input[4] >= -1.4792583) {
                        var17 = -12.0581665;
                    } else {
                        var17 = 21.24667;
                    }
                }
            }
        }
        double var18;
        if (input[3] >= 0.7171699) {
            if (input[4] >= -0.10171662) {
                if (input[4] >= 0.35746396) {
                    if (input[0] >= -0.9264882) {
                        var18 = -1.5360795;
                    } else {
                        var18 = -33.38052;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var18 = 32.38799;
                    } else {
                        var18 = -11.132994;
                    }
                }
            } else {
                if (input[4] >= -1.7088487) {
                    if (input[7] >= 0.5) {
                        var18 = -19.722092;
                    } else {
                        var18 = 9.975108;
                    }
                } else {
                    if (input[0] >= -0.70533043) {
                        var18 = 34.78269;
                    } else {
                        var18 = -26.166338;
                    }
                }
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[4] >= -2.1680293) {
                    if (input[1] >= -0.5288346) {
                        var18 = 1.0084791;
                    } else {
                        var18 = -13.03896;
                    }
                } else {
                    if (input[2] >= -1.4353329) {
                        var18 = -111.12665;
                    } else {
                        var18 = -9.917932;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[7] >= 0.5) {
                        var18 = 3.0441973;
                    } else {
                        var18 = -5.132044;
                    }
                } else {
                    if (input[3] >= -0.33794206) {
                        var18 = 31.354824;
                    } else {
                        var18 = 3.2640269;
                    }
                }
            }
        }
        double var19;
        if (input[0] >= 1.8111515) {
            if (input[0] >= 3.8108885) {
                var19 = -40.914886;
            } else {
                if (input[0] >= 2.6085758) {
                    if (input[0] >= 3.4641914) {
                        var19 = 21.752987;
                    } else {
                        var19 = -4.062738;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var19 = 24.770464;
                    } else {
                        var19 = 4.686978;
                    }
                }
            }
        } else {
            if (input[4] >= -1.0200777) {
                if (input[0] >= -1.0619833) {
                    if (input[0] >= -0.9213772) {
                        var19 = -1.3459871;
                    } else {
                        var19 = -17.356071;
                    }
                } else {
                    if (input[2] >= 0.10341075) {
                        var19 = 21.783834;
                    } else {
                        var19 = -6.78555;
                    }
                }
            } else {
                if (input[0] >= -1.2499195) {
                    if (input[0] >= -1.045479) {
                        var19 = 4.8528085;
                    } else {
                        var19 = -10.240755;
                    }
                } else {
                    if (input[0] >= -1.2683938) {
                        var19 = 75.3572;
                    } else {
                        var19 = 2.5274491;
                    }
                }
            }
        }
        double var20;
        if (input[4] >= 1.7350056) {
            if (input[12] >= 0.5) {
                if (input[4] >= 2.1941862) {
                    if (input[0] >= -0.31705314) {
                        var20 = -41.948612;
                    } else {
                        var20 = 18.974371;
                    }
                } else {
                    if (input[0] >= -0.62914443) {
                        var20 = 49.45087;
                    } else {
                        var20 = 91.05704;
                    }
                }
            } else {
                if (input[0] >= -0.7876923) {
                    if (input[13] >= 0.5) {
                        var20 = 23.00791;
                    } else {
                        var20 = -10.842686;
                    }
                } else {
                    var20 = -43.38333;
                }
            }
        } else {
            if (input[4] >= 0.5870542) {
                if (input[12] >= 0.5) {
                    if (input[4] >= 1.0462348) {
                        var20 = -0.8745569;
                    } else {
                        var20 = -10.110093;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var20 = 0.21081121;
                    } else {
                        var20 = 50.255238;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[1] >= -0.5288346) {
                        var20 = 9.206179;
                    } else {
                        var20 = -21.05868;
                    }
                } else {
                    if (input[13] >= 1.5) {
                        var20 = 14.225177;
                    } else {
                        var20 = -1.3628882;
                    }
                }
            }
        }
        double var21;
        if (input[0] >= -1.3048097) {
            if (input[0] >= -1.0982928) {
                if (input[0] >= -1.0899341) {
                    if (input[0] >= -1.0788071) {
                        var21 = -0.30390498;
                    } else {
                        var21 = 25.795858;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var21 = 2.6885552;
                    } else {
                        var21 = -93.70193;
                    }
                }
            } else {
                if (input[0] >= -1.1053736) {
                    if (input[3] >= -0.33794206) {
                        var21 = 23.336504;
                    } else {
                        var21 = 66.62827;
                    }
                } else {
                    if (input[0] >= -1.1236882) {
                        var21 = -15.759997;
                    } else {
                        var21 = 8.292577;
                    }
                }
            }
        } else {
            var21 = -58.131153;
        }
        double var22;
        if (input[0] >= -1.0315834) {
            if (input[0] >= -1.0243428) {
                if (input[0] >= -1.0187526) {
                    if (input[0] >= -1.0148661) {
                        var22 = 0.032961894;
                    } else {
                        var22 = -75.93611;
                    }
                } else {
                    if (input[4] >= -0.79048747) {
                        var22 = -30.125967;
                    } else {
                        var22 = 99.532455;
                    }
                }
            } else {
                if (input[0] >= -1.0270048) {
                    var22 = -101.17118;
                } else {
                    if (input[12] >= 0.5) {
                        var22 = -70.19988;
                    } else {
                        var22 = 7.367056;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0406342) {
                if (input[5] >= 0.5) {
                    if (input[4] >= -0.5608972) {
                        var22 = 71.505905;
                    } else {
                        var22 = -47.84937;
                    }
                } else {
                    var22 = 159.42519;
                }
            } else {
                if (input[0] >= -1.0580436) {
                    if (input[2] >= 0.10341075) {
                        var22 = 14.025388;
                    } else {
                        var22 = -46.703384;
                    }
                } else {
                    if (input[0] >= -1.072578) {
                        var22 = 42.089104;
                    } else {
                        var22 = 1.7467211;
                    }
                }
            }
        }
        double var23;
        if (input[11] >= 0.5) {
            if (input[0] >= 0.04944908) {
                if (input[4] >= 0.5870542) {
                    if (input[0] >= 0.34556842) {
                        var23 = 4.179637;
                    } else {
                        var23 = -26.827099;
                    }
                } else {
                    if (input[0] >= 0.34615403) {
                        var23 = 7.450673;
                    } else {
                        var23 = 26.639925;
                    }
                }
            } else {
                if (input[0] >= -0.098344386) {
                    if (input[9] >= 0.5) {
                        var23 = -10.470457;
                    } else {
                        var23 = -75.14347;
                    }
                } else {
                    if (input[0] >= -1.1230493) {
                        var23 = -0.56143826;
                    } else {
                        var23 = 20.072157;
                    }
                }
            }
        } else {
            if (input[0] >= -1.2706298) {
                if (input[0] >= -1.2559357) {
                    if (input[4] >= -2.3976195) {
                        var23 = -1.2197328;
                    } else {
                        var23 = -30.672867;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var23 = -6.778308;
                    } else {
                        var23 = 96.23706;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    var23 = -0.8686448;
                } else {
                    var23 = -64.14468;
                }
            }
        }
        double var24;
        if (input[0] >= -1.3034787) {
            if (input[4] >= 1.0462348) {
                if (input[2] >= 0.10341075) {
                    if (input[0] >= 1.2677867) {
                        var24 = -22.290045;
                    } else {
                        var24 = -0.11668297;
                    }
                } else {
                    if (input[3] >= 0.7171699) {
                        var24 = 6.5693946;
                    } else {
                        var24 = 30.435068;
                    }
                }
            } else {
                if (input[0] >= -1.2817037) {
                    if (input[0] >= -1.2706298) {
                        var24 = -0.13412413;
                    } else {
                        var24 = -70.36895;
                    }
                } else {
                    var24 = 46.042397;
                }
            }
        } else {
            var24 = -57.535797;
        }
        double var25;
        if (input[0] >= 3.6052234) {
            if (input[3] >= -0.33794206) {
                var25 = 5.3210993;
            } else {
                if (input[0] >= 3.8257422) {
                    var25 = -56.23565;
                } else {
                    if (input[0] >= 3.7195292) {
                        var25 = 12.495119;
                    } else {
                        var25 = -35.313454;
                    }
                }
            }
        } else {
            if (input[0] >= 3.4756913) {
                if (input[0] >= 3.5295167) {
                    var25 = 27.95722;
                } else {
                    var25 = 60.733276;
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[3] >= -0.33794206) {
                        var25 = 2.9827752;
                    } else {
                        var25 = -23.103254;
                    }
                } else {
                    if (input[3] >= -0.33794206) {
                        var25 = -5.7312903;
                    } else {
                        var25 = 0.3952112;
                    }
                }
            }
        }
        double var26;
        if (input[0] >= 0.7945922) {
            if (input[0] >= 0.81434417) {
                if (input[6] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var26 = -4.437909;
                    } else {
                        var26 = 32.362137;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var26 = 4.9694543;
                    } else {
                        var26 = 45.244526;
                    }
                }
            } else {
                if (input[2] >= 0.10341075) {
                    var26 = -90.08293;
                } else {
                    if (input[0] >= 0.8024185) {
                        var26 = -9.868344;
                    } else {
                        var26 = -2.538421;
                    }
                }
            }
        } else {
            if (input[0] >= 0.7876711) {
                var26 = 56.135788;
            } else {
                if (input[0] >= 0.7830924) {
                    var26 = -52.967896;
                } else {
                    if (input[0] >= 0.7261791) {
                        var26 = 20.134798;
                    } else {
                        var26 = 0.7366501;
                    }
                }
            }
        }
        double var27;
        if (input[4] >= -1.9384389) {
            if (input[4] >= -1.4792583) {
                if (input[0] >= -1.2941618) {
                    if (input[13] >= -1.5) {
                        var27 = 0.3939423;
                    } else {
                        var27 = -7.9987173;
                    }
                } else {
                    var27 = -45.682926;
                }
            } else {
                if (input[2] >= -1.4353329) {
                    if (input[6] >= 0.5) {
                        var27 = 22.475674;
                    } else {
                        var27 = -13.486884;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var27 = -54.619587;
                    } else {
                        var27 = 20.175884;
                    }
                }
            }
        } else {
            if (input[2] >= 0.10341075) {
                var27 = -45.88502;
            } else {
                if (input[0] >= -1.0286553) {
                    if (input[0] >= -0.8878362) {
                        var27 = 13.42434;
                    } else {
                        var27 = -36.88931;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var27 = -1.9395915;
                    } else {
                        var27 = 48.769196;
                    }
                }
            }
        }
        double var28;
        if (input[1] >= 1.7428633) {
            if (input[13] >= -0.5) {
                if (input[2] >= -1.4353329) {
                    if (input[4] >= 0.2426688) {
                        var28 = -62.849438;
                    } else {
                        var28 = -9.177687;
                    }
                } else {
                    var28 = -71.55584;
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[4] >= 0.70184934) {
                        var28 = 15.813318;
                    } else {
                        var28 = 2.7658036;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var28 = -35.10312;
                    } else {
                        var28 = -7.872855;
                    }
                }
            }
        } else {
            if (input[3] >= 0.7171699) {
                if (input[4] >= -0.10171662) {
                    if (input[5] >= 0.5) {
                        var28 = -2.2804086;
                    } else {
                        var28 = 28.995634;
                    }
                } else {
                    if (input[4] >= -0.79048747) {
                        var28 = -14.542291;
                    } else {
                        var28 = 0.9258674;
                    }
                }
            } else {
                if (input[4] >= 2.1941862) {
                    if (input[13] >= 0.5) {
                        var28 = -38.98531;
                    } else {
                        var28 = -19.796707;
                    }
                } else {
                    if (input[2] >= -1.4353329) {
                        var28 = 1.3527946;
                    } else {
                        var28 = 11.355799;
                    }
                }
            }
        }
        double var29;
        if (input[4] >= -2.3976195) {
            if (input[0] >= -1.2169108) {
                if (input[1] >= -0.07416786) {
                    if (input[0] >= -0.9213772) {
                        var29 = 2.0215807;
                    } else {
                        var29 = -5.4085274;
                    }
                } else {
                    if (input[0] >= -0.531556) {
                        var29 = -4.501681;
                    } else {
                        var29 = 8.182131;
                    }
                }
            } else {
                if (input[2] >= 0.10341075) {
                    if (input[4] >= -0.44610205) {
                        var29 = 11.59023;
                    } else {
                        var29 = 87.30313;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var29 = -33.024693;
                    } else {
                        var29 = 23.35533;
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                var29 = 27.671478;
            } else {
                if (input[2] >= -2.9740767) {
                    if (input[0] >= -1.0329144) {
                        var29 = -89.100525;
                    } else {
                        var29 = -41.065678;
                    }
                } else {
                    if (input[0] >= -1.0396758) {
                        var29 = -7.8055677;
                    } else {
                        var29 = -6.5929723;
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
