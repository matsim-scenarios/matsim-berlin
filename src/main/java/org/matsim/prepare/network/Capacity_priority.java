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

        return score(data);
    }
    public static double score(double[] input) {
        double var0;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.90718144) {
                if (input[7] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var0 = 509.41208;
                    } else {
                        var0 = 656.05255;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var0 = 529.2304;
                    } else {
                        var0 = 490.94595;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.06727953) {
                        var0 = 513.88495;
                    } else {
                        var0 = 533.9743;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var0 = 513.2615;
                    } else {
                        var0 = 471.1662;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[3] >= 3.6575255) {
                    if (input[3] >= 5.2790923) {
                        var0 = 319.83466;
                    } else {
                        var0 = 393.5442;
                    }
                } else {
                    if (input[1] >= 1.8946164) {
                        var0 = 533.05133;
                    } else {
                        var0 = 447.12842;
                    }
                }
            } else {
                var0 = 332.99774;
            }
        }
        double var1;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.90718144) {
                if (input[7] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var1 = 348.77722;
                    } else {
                        var1 = 444.36017;
                    }
                } else {
                    if (input[2] >= -0.60500103) {
                        var1 = 358.3817;
                    } else {
                        var1 = 332.9323;
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[9] >= 0.5) {
                        var1 = 347.0346;
                    } else {
                        var1 = 325.56976;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var1 = 361.0058;
                    } else {
                        var1 = 339.76807;
                    }
                }
            }
        } else {
            if (input[9] >= 0.5) {
                var1 = 225.94044;
            } else {
                if (input[3] >= 3.6575255) {
                    if (input[3] >= 5.2790923) {
                        var1 = 217.39204;
                    } else {
                        var1 = 268.78107;
                    }
                } else {
                    if (input[1] >= 1.8946164) {
                        var1 = 361.4131;
                    } else {
                        var1 = 299.132;
                    }
                }
            }
        }
        double var2;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.90718144) {
                if (input[7] >= 0.5) {
                    if (input[4] >= 0.5629271) {
                        var2 = 269.876;
                    } else {
                        var2 = 303.3839;
                    }
                } else {
                    if (input[0] >= -0.7527142) {
                        var2 = 237.14993;
                    } else {
                        var2 = 254.45773;
                    }
                }
            } else {
                if (input[3] >= 0.41439155) {
                    if (input[0] >= -0.74384815) {
                        var2 = 250.44672;
                    } else {
                        var2 = 145.9058;
                    }
                } else {
                    if (input[0] >= -0.60014534) {
                        var2 = 235.97841;
                    } else {
                        var2 = 252.24367;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[3] >= 3.6575255) {
                    if (input[13] >= -1.5) {
                        var2 = 185.45172;
                    } else {
                        var2 = 146.94464;
                    }
                } else {
                    if (input[1] >= 0.62115526) {
                        var2 = 244.26106;
                    } else {
                        var2 = 201.45161;
                    }
                }
            } else {
                var2 = 152.74196;
            }
        }
        double var3;
        if (input[13] >= -0.5) {
            if (input[1] >= -0.90718144) {
                if (input[3] >= 3.6575255) {
                    if (input[0] >= 0.5901167) {
                        var3 = 7.9092455;
                    } else {
                        var3 = 33.02568;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var3 = 184.07344;
                    } else {
                        var3 = 207.68169;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -0.8885968) {
                        var3 = 160.09726;
                    } else {
                        var3 = 100.82015;
                    }
                } else {
                    if (input[0] >= -1.0977719) {
                        var3 = 173.18535;
                    } else {
                        var3 = 130.26865;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[3] >= 3.6575255) {
                    if (input[3] >= 5.2790923) {
                        var3 = 95.19673;
                    } else {
                        var3 = 124.01566;
                    }
                } else {
                    if (input[0] >= -0.4813307) {
                        var3 = 162.77406;
                    } else {
                        var3 = 136.27011;
                    }
                }
            } else {
                var3 = 103.59811;
            }
        }
        double var4;
        if (input[7] >= 0.5) {
            if (input[13] >= -0.5) {
                if (input[1] >= -0.39743024) {
                    if (input[0] >= -1.0597873) {
                        var4 = 141.86269;
                    } else {
                        var4 = 106.50369;
                    }
                } else {
                    if (input[0] >= -0.38137335) {
                        var4 = 106.31593;
                    } else {
                        var4 = 117.204605;
                    }
                }
            } else {
                if (input[1] >= 2.1494918) {
                    if (input[9] >= 0.5) {
                        var4 = 72.195786;
                    } else {
                        var4 = 106.84414;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var4 = 70.90285;
                    } else {
                        var4 = 83.844284;
                    }
                }
            }
        } else {
            if (input[2] >= -0.60500103) {
                if (input[5] >= 0.5) {
                    if (input[6] >= 0.5) {
                        var4 = 105.20787;
                    } else {
                        var4 = 38.913345;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var4 = 127.77123;
                    } else {
                        var4 = 203.62233;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[0] >= 0.8556945) {
                        var4 = 86.9575;
                    } else {
                        var4 = 104.16765;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var4 = 140.34818;
                    } else {
                        var4 = 52.55062;
                    }
                }
            }
        }
        double var5;
        if (input[13] >= -0.5) {
            if (input[3] >= 2.0359585) {
                if (input[4] >= -0.47387564) {
                    if (input[3] >= 3.6575255) {
                        var5 = -29.581224;
                    } else {
                        var5 = 46.687584;
                    }
                } else {
                    if (input[3] >= 3.6575255) {
                        var5 = -23.509655;
                    } else {
                        var5 = 48.343304;
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var5 = -52.818314;
                    } else {
                        var5 = 67.42486;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var5 = 94.97364;
                    } else {
                        var5 = 71.83863;
                    }
                }
            }
        } else {
            if (input[3] >= 2.0359585) {
                if (input[13] >= -1.5) {
                    if (input[3] >= 3.6575255) {
                        var5 = 54.997746;
                    } else {
                        var5 = 74.467834;
                    }
                } else {
                    var5 = 30.515083;
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[4] >= 0.35556656) {
                        var5 = 46.740196;
                    } else {
                        var5 = 35.345654;
                    }
                } else {
                    var5 = 47.801273;
                }
            }
        }
        double var6;
        if (input[3] >= 0.41439155) {
            if (input[0] >= -1.1578543) {
                if (input[8] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var6 = 37.709766;
                    } else {
                        var6 = 62.904778;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var6 = -149.4832;
                    } else {
                        var6 = 32.31453;
                    }
                }
            } else {
                if (input[2] >= -0.60500103) {
                    var6 = -122.75464;
                } else {
                    if (input[0] >= -1.1732912) {
                        var6 = -59.331696;
                    } else {
                        var6 = -22.100824;
                    }
                }
            }
        } else {
            if (input[2] >= 1.1372645) {
                if (input[12] >= 0.5) {
                    if (input[1] >= -0.65230584) {
                        var6 = 76.769516;
                    } else {
                        var6 = 46.9438;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var6 = 15.064776;
                    } else {
                        var6 = 44.340286;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[1] >= -0.39743024) {
                        var6 = 71.252556;
                    } else {
                        var6 = 50.097557;
                    }
                } else {
                    if (input[0] >= -1.1615448) {
                        var6 = 50.00092;
                    } else {
                        var6 = 20.666553;
                    }
                }
            }
        }
        double var7;
        if (input[3] >= 0.41439155) {
            if (input[0] >= -1.0631626) {
                if (input[8] >= 0.5) {
                    if (input[3] >= 2.0359585) {
                        var7 = 25.58427;
                    } else {
                        var7 = 49.622658;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var7 = -83.30489;
                    } else {
                        var7 = 22.068895;
                    }
                }
            } else {
                if (input[2] >= -0.60500103) {
                    if (input[5] >= 0.5) {
                        var7 = -61.34055;
                    } else {
                        var7 = -109.027435;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var7 = -30.76405;
                    } else {
                        var7 = 22.950455;
                    }
                }
            }
        } else {
            if (input[10] >= 0.5) {
                if (input[0] >= -1.0184271) {
                    if (input[2] >= 0.2661317) {
                        var7 = 30.163404;
                    } else {
                        var7 = 52.507698;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var7 = -46.476753;
                    } else {
                        var7 = 5.4090004;
                    }
                }
            } else {
                if (input[1] >= -0.39743024) {
                    if (input[1] >= 0.112320915) {
                        var7 = 54.631596;
                    } else {
                        var7 = 44.03921;
                    }
                } else {
                    if (input[2] >= 1.1372645) {
                        var7 = 25.69026;
                    } else {
                        var7 = 34.719795;
                    }
                }
            }
        }
        double var8;
        if (input[3] >= 0.41439155) {
            if (input[0] >= -1.0060055) {
                if (input[9] >= 0.5) {
                    if (input[13] >= -0.5) {
                        var8 = -179.94221;
                    } else {
                        var8 = 15.185558;
                    }
                } else {
                    if (input[3] >= 3.6575255) {
                        var8 = 3.301742;
                    } else {
                        var8 = 30.327604;
                    }
                }
            } else {
                if (input[13] >= -0.5) {
                    if (input[11] >= 0.5) {
                        var8 = -23.062336;
                    } else {
                        var8 = -73.37702;
                    }
                } else {
                    if (input[0] >= -1.1515086) {
                        var8 = 16.677279;
                    } else {
                        var8 = 35.907322;
                    }
                }
            }
        } else {
            if (input[0] >= 0.5342198) {
                if (input[13] >= 0.5) {
                    if (input[1] >= -0.39743024) {
                        var8 = 35.41796;
                    } else {
                        var8 = 20.528238;
                    }
                } else {
                    if (input[1] >= 0.112320915) {
                        var8 = 35.177597;
                    } else {
                        var8 = 16.355116;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[1] >= -0.39743024) {
                        var8 = 34.35211;
                    } else {
                        var8 = 24.455872;
                    }
                } else {
                    if (input[0] >= -0.7244957) {
                        var8 = 18.416975;
                    } else {
                        var8 = 29.081787;
                    }
                }
            }
        }
        double var9;
        if (input[12] >= 0.5) {
            if (input[3] >= 2.0359585) {
                if (input[13] >= -0.5) {
                    if (input[4] >= 0.14820603) {
                        var9 = -14.910792;
                    } else {
                        var9 = 5.8038936;
                    }
                } else {
                    var9 = 11.577083;
                }
            } else {
                if (input[2] >= -0.60500103) {
                    if (input[13] >= -0.5) {
                        var9 = 25.855652;
                    } else {
                        var9 = 13.595954;
                    }
                } else {
                    if (input[4] >= -0.47387564) {
                        var9 = 23.182533;
                    } else {
                        var9 = -12.234288;
                    }
                }
            }
        } else {
            if (input[2] >= 1.1372645) {
                if (input[9] >= 0.5) {
                    if (input[4] >= 0.97764826) {
                        var9 = 4.7257433;
                    } else {
                        var9 = -5.449653;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var9 = -22.438972;
                    } else {
                        var9 = 14.537351;
                    }
                }
            } else {
                if (input[3] >= 0.41439155) {
                    if (input[4] >= -0.47387564) {
                        var9 = -7.7449923;
                    } else {
                        var9 = 14.696409;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var9 = 20.869585;
                    } else {
                        var9 = 14.84225;
                    }
                }
            }
        }
        double var10;
        if (input[1] >= 0.112320915) {
            if (input[3] >= 0.41439155) {
                if (input[2] >= -0.60500103) {
                    if (input[13] >= -0.5) {
                        var10 = -53.273815;
                    } else {
                        var10 = 8.215704;
                    }
                } else {
                    if (input[3] >= 3.6575255) {
                        var10 = 5.1443386;
                    } else {
                        var10 = 15.6153145;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[11] >= 0.5) {
                        var10 = 2.784759;
                    } else {
                        var10 = 16.065817;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var10 = 19.136833;
                    } else {
                        var10 = 46.193924;
                    }
                }
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[2] >= -0.60500103) {
                    if (input[1] >= -0.65230584) {
                        var10 = 19.310862;
                    } else {
                        var10 = 8.307443;
                    }
                } else {
                    var10 = -37.043446;
                }
            } else {
                if (input[2] >= 1.1372645) {
                    if (input[1] >= -0.65230584) {
                        var10 = -8.362408;
                    } else {
                        var10 = 11.836471;
                    }
                } else {
                    if (input[3] >= 2.0359585) {
                        var10 = -0.27433363;
                    } else {
                        var10 = 10.863973;
                    }
                }
            }
        }
        double var11;
        if (input[0] >= -1.140122) {
            if (input[0] >= 0.96168256) {
                if (input[0] >= 1.0496686) {
                    if (input[1] >= 0.112320915) {
                        var11 = 11.136226;
                    } else {
                        var11 = 2.4355383;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var11 = -19.45409;
                    } else {
                        var11 = 3.6421704;
                    }
                }
            } else {
                if (input[2] >= 1.1372645) {
                    if (input[0] >= -0.872159) {
                        var11 = 7.761823;
                    } else {
                        var11 = -13.210497;
                    }
                } else {
                    if (input[0] >= -1.1258104) {
                        var11 = 10.433381;
                    } else {
                        var11 = 29.317333;
                    }
                }
            }
        } else {
            if (input[0] >= -1.1429574) {
                var11 = -61.217625;
            } else {
                if (input[10] >= 0.5) {
                    if (input[1] >= -0.65230584) {
                        var11 = -59.26084;
                    } else {
                        var11 = -1.7967228;
                    }
                } else {
                    if (input[1] >= 1.8946164) {
                        var11 = 29.225368;
                    } else {
                        var11 = -3.9046576;
                    }
                }
            }
        }
        double var12;
        if (input[3] >= 0.41439155) {
            if (input[0] >= -1.1578543) {
                if (input[8] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var12 = -61.24131;
                    } else {
                        var12 = 6.621575;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var12 = -79.48373;
                    } else {
                        var12 = -0.8644342;
                    }
                }
            } else {
                if (input[0] >= -1.1690607) {
                    var12 = -89.70051;
                } else {
                    if (input[4] >= -0.68123615) {
                        var12 = -52.350384;
                    } else {
                        var12 = 16.514862;
                    }
                }
            }
        } else {
            if (input[0] >= -0.18505909) {
                if (input[1] >= 0.112320915) {
                    if (input[11] >= 0.5) {
                        var12 = 8.275491;
                    } else {
                        var12 = 14.150704;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var12 = 3.8633258;
                    } else {
                        var12 = -2.9213593;
                    }
                }
            } else {
                if (input[0] >= -1.0731988) {
                    if (input[4] >= -0.8885968) {
                        var12 = 11.178605;
                    } else {
                        var12 = 5.4522204;
                    }
                } else {
                    if (input[4] >= -0.68123615) {
                        var12 = -7.102004;
                    } else {
                        var12 = 7.686936;
                    }
                }
            }
        }
        double var13;
        if (input[0] >= -1.099122) {
            if (input[3] >= 5.2790923) {
                var13 = -44.08033;
            } else {
                if (input[4] >= 1.8070904) {
                    if (input[12] >= 0.5) {
                        var13 = 6.270534;
                    } else {
                        var13 = -5.529274;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var13 = 6.2147236;
                    } else {
                        var13 = 2.5057926;
                    }
                }
            }
        } else {
            if (input[0] >= -1.1028125) {
                var13 = -85.39324;
            } else {
                if (input[9] >= 0.5) {
                    if (input[0] >= -1.2654626) {
                        var13 = 2.0895839;
                    } else {
                        var13 = 32.997166;
                    }
                } else {
                    if (input[4] >= -0.26651508) {
                        var13 = -89.36435;
                    } else {
                        var13 = -11.812815;
                    }
                }
            }
        }
        double var14;
        if (input[0] >= -1.0604622) {
            if (input[0] >= -0.9411975) {
                if (input[6] >= 0.5) {
                    if (input[11] >= 0.5) {
                        var14 = -2.6534336;
                    } else {
                        var14 = 2.7979712;
                    }
                } else {
                    if (input[3] >= 3.6575255) {
                        var14 = -12.578801;
                    } else {
                        var14 = 4.142154;
                    }
                }
            } else {
                if (input[4] >= 1.1850088) {
                    if (input[1] >= -0.65230584) {
                        var14 = -12.428717;
                    } else {
                        var14 = -29.440285;
                    }
                } else {
                    if (input[1] >= -0.39743024) {
                        var14 = 7.6166005;
                    } else {
                        var14 = 25.587652;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0641077) {
                var14 = -57.93794;
            } else {
                if (input[4] >= -0.68123615) {
                    if (input[8] >= 0.5) {
                        var14 = -37.874725;
                    } else {
                        var14 = -0.971523;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var14 = 17.786041;
                    } else {
                        var14 = 2.172266;
                    }
                }
            }
        }
        double var15;
        if (input[4] >= -0.8885968) {
            if (input[4] >= -0.68123615) {
                if (input[0] >= -1.1356665) {
                    if (input[3] >= 2.0359585) {
                        var15 = -7.5403795;
                    } else {
                        var15 = 2.2239335;
                    }
                } else {
                    if (input[0] >= -1.2619071) {
                        var15 = -15.420448;
                    } else {
                        var15 = 41.721867;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -1.1110935) {
                        var15 = 5.655243;
                    } else {
                        var15 = 13.346464;
                    }
                } else {
                    if (input[1] >= -0.65230584) {
                        var15 = 37.970135;
                    } else {
                        var15 = 17.172663;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[0] >= -1.1457478) {
                    if (input[12] >= 0.5) {
                        var15 = -46.64769;
                    } else {
                        var15 = 1.4922985;
                    }
                } else {
                    if (input[0] >= -1.1589794) {
                        var15 = -35.149082;
                    } else {
                        var15 = -6.1900373;
                    }
                }
            } else {
                if (input[2] >= -0.60500103) {
                    var15 = 48.45582;
                } else {
                    if (input[5] >= 0.5) {
                        var15 = -4.0153623;
                    } else {
                        var15 = -36.471478;
                    }
                }
            }
        }
        double var16;
        if (input[10] >= 0.5) {
            if (input[1] >= -0.90718144) {
                if (input[7] >= 0.5) {
                    if (input[2] >= 0.2661317) {
                        var16 = -67.27465;
                    } else {
                        var16 = 2.0995238;
                    }
                } else {
                    if (input[4] >= 0.5629271) {
                        var16 = 34.433716;
                    } else {
                        var16 = -0.76671565;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[7] >= 0.5) {
                        var16 = 9.369451;
                    } else {
                        var16 = 3.5235744;
                    }
                } else {
                    if (input[4] >= 0.35556656) {
                        var16 = -29.745825;
                    } else {
                        var16 = -1.3337857;
                    }
                }
            }
        } else {
            if (input[1] >= -0.39743024) {
                if (input[7] >= 0.5) {
                    if (input[4] >= -0.059154533) {
                        var16 = 5.2407575;
                    } else {
                        var16 = 0.9901938;
                    }
                } else {
                    if (input[4] >= 0.35556656) {
                        var16 = -6.699581;
                    } else {
                        var16 = 3.9893348;
                    }
                }
            } else {
                if (input[3] >= 0.41439155) {
                    if (input[4] >= -0.8885968) {
                        var16 = -18.370224;
                    } else {
                        var16 = -6.9736714;
                    }
                } else {
                    if (input[2] >= 0.2661317) {
                        var16 = -3.6064994;
                    } else {
                        var16 = 1.244417;
                    }
                }
            }
        }
        double var17;
        if (input[9] >= 0.5) {
            if (input[0] >= 2.736117) {
                if (input[0] >= 2.7582598) {
                    if (input[0] >= 2.987428) {
                        var17 = -13.059216;
                    } else {
                        var17 = -1.6051484;
                    }
                } else {
                    var17 = -41.261295;
                }
            } else {
                if (input[1] >= 0.112320915) {
                    if (input[13] >= -0.5) {
                        var17 = 3.1617482;
                    } else {
                        var17 = -2.8939433;
                    }
                } else {
                    if (input[0] >= -0.7244957) {
                        var17 = -0.9129956;
                    } else {
                        var17 = 1.930864;
                    }
                }
            }
        } else {
            if (input[0] >= -1.1563691) {
                if (input[0] >= -1.1110935) {
                    if (input[0] >= -1.0624425) {
                        var17 = 3.9489856;
                    } else {
                        var17 = -27.51146;
                    }
                } else {
                    if (input[0] >= -1.1355314) {
                        var17 = 49.33598;
                    } else {
                        var17 = -4.9901323;
                    }
                }
            } else {
                if (input[0] >= -1.1676204) {
                    var17 = -55.99859;
                } else {
                    if (input[0] >= -1.1960189) {
                        var17 = 11.272197;
                    } else {
                        var17 = -26.987518;
                    }
                }
            }
        }
        double var18;
        if (input[0] >= 0.7982224) {
            if (input[4] >= 1.7034101) {
                if (input[11] >= 0.5) {
                    if (input[0] >= 0.92563313) {
                        var18 = -9.953891;
                    } else {
                        var18 = -46.874985;
                    }
                } else {
                    if (input[0] >= 1.4418919) {
                        var18 = 3.0006256;
                    } else {
                        var18 = -9.191165;
                    }
                }
            } else {
                if (input[4] >= 0.5629271) {
                    if (input[0] >= 1.1805446) {
                        var18 = 13.117846;
                    } else {
                        var18 = 38.80508;
                    }
                } else {
                    if (input[0] >= 1.4152486) {
                        var18 = -0.07100273;
                    } else {
                        var18 = -2.8181353;
                    }
                }
            }
        } else {
            if (input[3] >= 0.41439155) {
                if (input[9] >= 0.5) {
                    if (input[0] >= -1.1366117) {
                        var18 = -7.2639427;
                    } else {
                        var18 = 21.78928;
                    }
                } else {
                    if (input[0] >= -1.1646951) {
                        var18 = -0.6109911;
                    } else {
                        var18 = 14.162834;
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[0] >= -1.0076706) {
                        var18 = 0.76893616;
                    } else {
                        var18 = -24.90308;
                    }
                } else {
                    if (input[9] >= 0.5) {
                        var18 = 2.0082495;
                    } else {
                        var18 = 8.414211;
                    }
                }
            }
        }
        double var19;
        if (input[13] >= 2.5) {
            if (input[4] >= -0.47387564) {
                var19 = 36.8444;
            } else {
                if (input[0] >= 0.36679912) {
                    var19 = 1.295419;
                } else {
                    var19 = 5.655388;
                }
            }
        } else {
            if (input[1] >= 1.1299896) {
                if (input[0] >= -1.0608222) {
                    if (input[3] >= 5.2790923) {
                        var19 = -18.393776;
                    } else {
                        var19 = 1.6730748;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var19 = 57.994267;
                    } else {
                        var19 = 9.136049;
                    }
                }
            } else {
                if (input[3] >= 2.0359585) {
                    if (input[13] >= -0.5) {
                        var19 = -11.469073;
                    } else {
                        var19 = -1.5259391;
                    }
                } else {
                    if (input[4] >= -0.8885968) {
                        var19 = 0.6904257;
                    } else {
                        var19 = -1.0127594;
                    }
                }
            }
        }
        double var20;
        if (input[3] >= 5.2790923) {
            var20 = -28.664755;
        } else {
            if (input[4] >= 3.2586143) {
                if (input[0] >= 0.30343127) {
                    var20 = -5.989906;
                } else {
                    var20 = -22.458088;
                }
            } else {
                if (input[0] >= -1.2044351) {
                    if (input[4] >= 2.014451) {
                        var20 = 11.8992605;
                    } else {
                        var20 = 0.5664128;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var20 = -18.86304;
                    } else {
                        var20 = 12.485246;
                    }
                }
            }
        }
        double var21;
        if (input[0] >= 0.96168256) {
            if (input[0] >= 0.97266394) {
                if (input[3] >= 0.41439155) {
                    if (input[3] >= 2.0359585) {
                        var21 = -7.9013743;
                    } else {
                        var21 = 10.685793;
                    }
                } else {
                    if (input[4] >= 1.7034101) {
                        var21 = -9.402132;
                    } else {
                        var21 = -0.83903515;
                    }
                }
            } else {
                var21 = -37.74692;
            }
        } else {
            if (input[0] >= 0.9276134) {
                if (input[5] >= 0.5) {
                    var21 = 21.660984;
                } else {
                    if (input[0] >= 0.9468758) {
                        var21 = 5.4834895;
                    } else {
                        var21 = 15.559204;
                    }
                }
            } else {
                if (input[0] >= 0.8857582) {
                    if (input[0] >= 0.9057857) {
                        var21 = 2.7825782;
                    } else {
                        var21 = -22.814884;
                    }
                } else {
                    if (input[3] >= 5.2790923) {
                        var21 = 22.975115;
                    } else {
                        var21 = 0.52792525;
                    }
                }
            }
        }
        double var22;
        if (input[0] >= -1.1871079) {
            if (input[0] >= -1.1767116) {
                if (input[0] >= -1.1646951) {
                    if (input[0] >= -1.1586194) {
                        var22 = 0.16614617;
                    } else {
                        var22 = -55.226986;
                    }
                } else {
                    if (input[0] >= -1.1687906) {
                        var22 = 49.090675;
                    } else {
                        var22 = -3.2370355;
                    }
                }
            } else {
                if (input[0] >= -1.1826074) {
                    var22 = -49.443127;
                } else {
                    var22 = -1.4395958;
                }
            }
        } else {
            if (input[1] >= -0.65230584) {
                if (input[0] >= -1.2619071) {
                    if (input[0] >= -1.2322484) {
                        var22 = 7.5111995;
                    } else {
                        var22 = -16.463339;
                    }
                } else {
                    var22 = 23.378597;
                }
            } else {
                if (input[5] >= 0.5) {
                    var22 = 1.9975114;
                } else {
                    var22 = 38.880943;
                }
            }
        }
        double var23;
        if (input[13] >= 2.5) {
            if (input[0] >= -0.31836554) {
                if (input[0] >= 0.7170324) {
                    var23 = -1.2248958;
                } else {
                    var23 = 5.5198092;
                }
            } else {
                var23 = 27.176308;
            }
        } else {
            if (input[0] >= -1.2044351) {
                if (input[0] >= -1.1965591) {
                    if (input[0] >= -1.091111) {
                        var23 = -0.072679095;
                    } else {
                        var23 = 3.5715573;
                    }
                } else {
                    var23 = 22.34693;
                }
            } else {
                if (input[0] >= -1.213211) {
                    var23 = -35.5965;
                } else {
                    if (input[8] >= 0.5) {
                        var23 = -29.041653;
                    } else {
                        var23 = -0.7297045;
                    }
                }
            }
        }
        double var24;
        if (input[0] >= -1.0638827) {
            if (input[0] >= -1.0486257) {
                if (input[0] >= -1.0353491) {
                    if (input[0] >= -1.0298135) {
                        var24 = 0.22030629;
                    } else {
                        var24 = 18.946169;
                    }
                } else {
                    if (input[0] >= -1.0390847) {
                        var24 = -40.8064;
                    } else {
                        var24 = 3.0827618;
                    }
                }
            } else {
                if (input[4] >= -0.8885968) {
                    if (input[0] >= -1.057267) {
                        var24 = 17.683899;
                    } else {
                        var24 = -0.62372345;
                    }
                } else {
                    var24 = 26.66661;
                }
            }
        } else {
            if (input[0] >= -1.0658629) {
                var24 = -42.41713;
            } else {
                if (input[4] >= 0.35556656) {
                    if (input[6] >= 0.5) {
                        var24 = -3.6150358;
                    } else {
                        var24 = 16.367094;
                    }
                } else {
                    if (input[4] >= -0.26651508) {
                        var24 = -43.741894;
                    } else {
                        var24 = -0.8840301;
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24);
    }
}
