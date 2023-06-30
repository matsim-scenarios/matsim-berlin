package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public class Capacity_right_before_left implements FeatureRegressor {

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 143.2389153599584) / 82.89404850064653;
		data[1] = (ft.getDouble("speed") - 8.335057610673134) / 0.16560556934846477;
		data[2] = (ft.getDouble("numFoes") - 2.2646625660573507) / 0.5530393650197418;
		data[3] = (ft.getDouble("numLanes") - 1.001732651823616) / 0.04742831736799205;
		data[4] = (ft.getDouble("junctionSize") - 10.911721389586763) / 3.6843422614733417;
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
        if (input[6] >= 0.5) {
            if (input[0] >= -1.5304345) {
                if (input[0] >= -0.8743561) {
                    var0 = 543.02203;
                } else {
                    if (input[0] >= -1.358649) {
                        var0 = 551.5959;
                    } else {
                        var0 = 537.0214;
                    }
                }
            } else {
                var0 = 490.56137;
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -1.4772341) {
                        var0 = 562.7193;
                    } else {
                        var0 = 508.44012;
                    }
                } else {
                    if (input[2] >= -1.3826549) {
                        var0 = 481.01984;
                    } else {
                        var0 = 552.8317;
                    }
                }
            } else {
                if (input[2] >= -1.3826549) {
                    var0 = 406.14957;
                } else {
                    if (input[4] >= -2.2831) {
                        var0 = 423.45297;
                    } else {
                        var0 = 532.2111;
                    }
                }
            }
        }
        double var1;
        if (input[6] >= 0.5) {
            if (input[0] >= 0.11612516) {
                var1 = 354.51038;
            } else {
                var1 = 358.45264;
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[5] >= 0.5) {
                    var1 = 368.85345;
                } else {
                    if (input[4] >= -1.4688432) {
                        var1 = 316.52917;
                    } else {
                        var1 = 365.6316;
                    }
                }
            } else {
                if (input[4] >= -2.011681) {
                    var1 = 268.80936;
                } else {
                    var1 = 360.54126;
                }
            }
        }
        double var2;
        if (input[0] >= -1.5846001) {
            if (input[0] >= -0.06452714) {
                if (input[0] >= 2.3802202) {
                    if (input[0] >= 2.3961442) {
                        var2 = 227.44934;
                    } else {
                        var2 = 195.80382;
                    }
                } else {
                    var2 = 233.4072;
                }
            } else {
                if (input[0] >= -1.4278337) {
                    if (input[5] >= 0.5) {
                        var2 = 239.41277;
                    } else {
                        var2 = 234.45143;
                    }
                } else {
                    if (input[4] >= -0.11174896) {
                        var2 = 196.45692;
                    } else {
                        var2 = 221.93068;
                    }
                }
            }
        } else {
            var2 = 170.41333;
        }
        double var3;
        if (input[6] >= 0.5) {
            if (input[0] >= -0.9497535) {
                if (input[0] >= 2.3870964) {
                    if (input[0] >= 2.4688876) {
                        var3 = 150.76233;
                    } else {
                        var3 = 134.64958;
                    }
                } else {
                    if (input[4] >= -0.6545867) {
                        var3 = 153.18239;
                    } else {
                        var3 = 157.96965;
                    }
                }
            } else {
                if (input[0] >= -1.3503251) {
                    if (input[0] >= -1.2762547) {
                        var3 = 157.86868;
                    } else {
                        var3 = 167.71962;
                    }
                } else {
                    if (input[0] >= -1.375719) {
                        var3 = 135.61624;
                    } else {
                        var3 = 152.26018;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[0] >= 0.20069094) {
                    if (input[4] >= -0.6545867) {
                        var3 = 153.88422;
                    } else {
                        var3 = 138.51962;
                    }
                } else {
                    if (input[0] >= -1.4451449) {
                        var3 = 162.93457;
                    } else {
                        var3 = 131.97504;
                    }
                }
            } else {
                if (input[0] >= -1.0968087) {
                    if (input[4] >= -2.011681) {
                        var3 = 89.50018;
                    } else {
                        var3 = 155.0651;
                    }
                } else {
                    var3 = 119.29793;
                }
            }
        }
        double var4;
        if (input[4] >= -0.6545867) {
            if (input[6] >= 0.5) {
                if (input[4] >= 0.838217) {
                    if (input[0] >= -1.5008787) {
                        var4 = 100.17956;
                    } else {
                        var4 = 125.07426;
                    }
                } else {
                    if (input[0] >= -1.5846001) {
                        var4 = 101.55701;
                    } else {
                        var4 = 33.580494;
                    }
                }
            } else {
                if (input[4] >= -0.38316783) {
                    if (input[0] >= -0.45767957) {
                        var4 = 104.90588;
                    } else {
                        var4 = 75.31006;
                    }
                } else {
                    if (input[0] >= -0.33577698) {
                        var4 = 101.95027;
                    } else {
                        var4 = 110.16751;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[4] >= -1.6045527) {
                    if (input[0] >= -0.6972625) {
                        var4 = 104.37561;
                    } else {
                        var4 = 96.292656;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var4 = 116.222946;
                    } else {
                        var4 = 105.27524;
                    }
                }
            } else {
                if (input[4] >= -2.011681) {
                    if (input[7] >= 0.5) {
                        var4 = 92.31727;
                    } else {
                        var4 = 62.111286;
                    }
                } else {
                    if (input[0] >= -0.8903403) {
                        var4 = 113.45996;
                    } else {
                        var4 = 98.70161;
                    }
                }
            }
        }
        double var5;
        if (input[0] >= -1.6262195) {
            if (input[0] >= -1.5846001) {
                if (input[0] >= -0.8740545) {
                    if (input[0] >= -0.84691143) {
                        var5 = 66.37503;
                    } else {
                        var5 = 57.4509;
                    }
                } else {
                    if (input[4] >= -1.061715) {
                        var5 = 70.12865;
                    } else {
                        var5 = 62.45739;
                    }
                }
            } else {
                var5 = 15.395686;
            }
        } else {
            var5 = 101.0086;
        }
        double var6;
        if (input[5] >= 0.5) {
            if (input[6] >= 0.5) {
                if (input[0] >= -1.3524363) {
                    if (input[0] >= -1.2760134) {
                        var6 = 43.219643;
                    } else {
                        var6 = 61.00119;
                    }
                } else {
                    if (input[0] >= -1.3763222) {
                        var6 = -16.218624;
                    } else {
                        var6 = 41.674767;
                    }
                }
            } else {
                if (input[0] >= 0.5568082) {
                    if (input[0] >= 0.57478297) {
                        var6 = 40.5038;
                    } else {
                        var6 = 17.24149;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var6 = 49.03576;
                    } else {
                        var6 = 25.952227;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[0] >= -0.92671204) {
                    if (input[0] >= -0.7916602) {
                        var6 = 42.48562;
                    } else {
                        var6 = 38.10122;
                    }
                } else {
                    if (input[4] >= -1.6045527) {
                        var6 = 46.721798;
                    } else {
                        var6 = 63.55647;
                    }
                }
            } else {
                if (input[4] >= -1.4688432) {
                    if (input[0] >= -0.45044142) {
                        var6 = 36.77849;
                    } else {
                        var6 = 0.25321835;
                    }
                } else {
                    if (input[0] >= -0.6943673) {
                        var6 = 55.99813;
                    } else {
                        var6 = 37.537594;
                    }
                }
            }
        }
        double var7;
        if (input[4] >= -1.7402622) {
            if (input[0] >= -1.3696872) {
                if (input[0] >= -0.5538868) {
                    if (input[0] >= 2.8411098) {
                        var7 = 22.419878;
                    } else {
                        var7 = 28.323011;
                    }
                } else {
                    if (input[4] >= -1.061715) {
                        var7 = 30.736572;
                    } else {
                        var7 = 24.638706;
                    }
                }
            } else {
                if (input[0] >= -1.3775889) {
                    var7 = -9.827128;
                } else {
                    if (input[0] >= -1.4211384) {
                        var7 = 19.67911;
                    } else {
                        var7 = 26.064049;
                    }
                }
            }
        } else {
            if (input[0] >= -1.3551506) {
                if (input[7] >= 0.5) {
                    if (input[0] >= 1.2156613) {
                        var7 = 16.103474;
                    } else {
                        var7 = 36.99192;
                    }
                } else {
                    if (input[0] >= -0.92671204) {
                        var7 = 28.380285;
                    } else {
                        var7 = 20.853481;
                    }
                }
            } else {
                var7 = 47.911446;
            }
        }
        double var8;
        if (input[3] >= 10.505693) {
            var8 = -1.3740605;
        } else {
            if (input[2] >= -1.3826549) {
                if (input[4] >= -1.061715) {
                    if (input[0] >= -1.4658943) {
                        var8 = 19.018583;
                    } else {
                        var8 = 9.421148;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var8 = 22.61962;
                    } else {
                        var8 = 2.840992;
                    }
                }
            } else {
                if (input[0] >= -1.2745054) {
                    if (input[0] >= -1.2036319) {
                        var8 = 22.45902;
                    } else {
                        var8 = -3.9138093;
                    }
                } else {
                    if (input[0] >= -1.4386306) {
                        var8 = 31.737265;
                    } else {
                        var8 = 44.07152;
                    }
                }
            }
        }
        double var9;
        if (input[0] >= 0.5568082) {
            if (input[0] >= 0.56284) {
                if (input[0] >= 2.8411098) {
                    if (input[4] >= -0.92600554) {
                        var9 = 9.682289;
                    } else {
                        var9 = -4.70924;
                    }
                } else {
                    if (input[0] >= 2.476005) {
                        var9 = 14.64853;
                    } else {
                        var9 = 11.442158;
                    }
                }
            } else {
                var9 = -20.384542;
            }
        } else {
            if (input[3] >= 10.505693) {
                var9 = -13.706076;
            } else {
                if (input[8] >= 0.5) {
                    if (input[0] >= -0.58766484) {
                        var9 = 23.868244;
                    } else {
                        var9 = 37.65763;
                    }
                } else {
                    if (input[0] >= -1.6226003) {
                        var9 = 12.734839;
                    } else {
                        var9 = 31.730734;
                    }
                }
            }
        }
        double var10;
        if (input[2] >= -1.3826549) {
            if (input[0] >= -1.4278337) {
                if (input[0] >= -1.4233701) {
                    if (input[4] >= -0.6545867) {
                        var10 = 8.150048;
                    } else {
                        var10 = 0.9488669;
                    }
                } else {
                    var10 = 45.64961;
                }
            } else {
                if (input[4] >= -0.92600554) {
                    if (input[0] >= -1.4402592) {
                        var10 = -15.72133;
                    } else {
                        var10 = 5.2316422;
                    }
                } else {
                    var10 = -28.511341;
                }
            }
        } else {
            if (input[0] >= -1.3562965) {
                if (input[0] >= -1.2368671) {
                    if (input[0] >= -1.0021698) {
                        var10 = 10.42216;
                    } else {
                        var10 = 19.795193;
                    }
                } else {
                    if (input[0] >= -1.2951945) {
                        var10 = -5.035073;
                    } else {
                        var10 = -15.515878;
                    }
                }
            } else {
                if (input[4] >= -1.7402622) {
                    var10 = 28.46493;
                } else {
                    var10 = 15.796429;
                }
            }
        }
        double var11;
        if (input[6] >= 0.5) {
            if (input[0] >= -1.6146384) {
                if (input[0] >= -1.5304345) {
                    if (input[0] >= -1.5124598) {
                        var11 = 5.0131564;
                    } else {
                        var11 = 27.167768;
                    }
                } else {
                    var11 = -23.874939;
                }
            } else {
                var11 = 35.637455;
            }
        } else {
            if (input[0] >= 0.21221167) {
                if (input[5] >= 0.5) {
                    if (input[0] >= 0.3298317) {
                        var11 = 4.202126;
                    } else {
                        var11 = 9.688707;
                    }
                } else {
                    if (input[0] >= 0.7130317) {
                        var11 = 10.963935;
                    } else {
                        var11 = -36.82294;
                    }
                }
            } else {
                if (input[3] >= 10.505693) {
                    var11 = -20.268118;
                } else {
                    if (input[0] >= -1.2059842) {
                        var11 = 8.79956;
                    } else {
                        var11 = 3.1335795;
                    }
                }
            }
        }
        double var12;
        if (input[8] >= 0.5) {
            if (input[4] >= -1.7402622) {
                var12 = 34.82415;
            } else {
                if (input[0] >= -0.38409168) {
                    var12 = -0.02732322;
                } else {
                    var12 = 3.823143;
                }
            }
        } else {
            if (input[0] >= -1.6343021) {
                if (input[0] >= -1.5936477) {
                    if (input[0] >= -1.5440061) {
                        var12 = 3.3823683;
                    } else {
                        var12 = 17.747597;
                    }
                } else {
                    var12 = -34.317272;
                }
            } else {
                var12 = 25.036434;
            }
        }
        double var13;
        if (input[8] >= 0.5) {
            if (input[3] >= 10.505693) {
                var13 = 23.745955;
            } else {
                if (input[4] >= -2.011681) {
                    var13 = 9.957307;
                } else {
                    var13 = 2.5570643;
                }
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[2] >= 0.42553467) {
                    if (input[4] >= -0.11174896) {
                        var13 = 1.8485298;
                    } else {
                        var13 = -4.337325;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var13 = 3.8602858;
                    } else {
                        var13 = 2.2540298;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -1.6045527) {
                        var13 = 1.7718652;
                    } else {
                        var13 = 4.742608;
                    }
                } else {
                    if (input[4] >= -2.2831) {
                        var13 = -9.571589;
                    } else {
                        var13 = 12.383347;
                    }
                }
            }
        }
        double var14;
        if (input[3] >= 10.505693) {
            var14 = -10.783418;
        } else {
            if (input[0] >= -1.6343021) {
                if (input[0] >= -1.5846001) {
                    if (input[0] >= -1.5285044) {
                        var14 = 1.5586574;
                    } else {
                        var14 = 16.950235;
                    }
                } else {
                    var14 = -18.127615;
                }
            } else {
                var14 = 15.967343;
            }
        }
        double var15;
        if (input[3] >= 10.505693) {
            var15 = -15.85842;
        } else {
            if (input[0] >= -1.6110797) {
                if (input[0] >= -1.5304345) {
                    if (input[0] >= -1.5166218) {
                        var15 = 0.9036607;
                    } else {
                        var15 = 21.02109;
                    }
                } else {
                    if (input[0] >= -1.5474442) {
                        var15 = -10.50429;
                    } else {
                        var15 = -21.103048;
                    }
                }
            } else {
                var15 = 21.620377;
            }
        }
        double var16;
        if (input[0] >= -1.366611) {
            if (input[0] >= -1.3592522) {
                if (input[2] >= -3.1908445) {
                    if (input[5] >= 0.5) {
                        var16 = 0.9689615;
                    } else {
                        var16 = -0.1255827;
                    }
                } else {
                    if (input[8] >= 0.5) {
                        var16 = 4.1350226;
                    } else {
                        var16 = 17.781046;
                    }
                }
            } else {
                var16 = 31.038706;
            }
        } else {
            if (input[0] >= -1.3758999) {
                var16 = -26.341656;
            } else {
                if (input[0] >= -1.6218766) {
                    if (input[2] >= 0.42553467) {
                        var16 = -10.234127;
                    } else {
                        var16 = 0.55429405;
                    }
                } else {
                    var16 = -15.756909;
                }
            }
        }
        double var17;
        if (input[8] >= 0.5) {
            if (input[4] >= -1.7402622) {
                var17 = 26.483034;
            } else {
                var17 = 0.117565565;
            }
        } else {
            if (input[4] >= 0.838217) {
                if (input[4] >= 1.5167642) {
                    if (input[0] >= -0.21031564) {
                        var17 = -6.698242;
                    } else {
                        var17 = 6.0077324;
                    }
                } else {
                    if (input[0] >= -1.1696728) {
                        var17 = -0.32344314;
                    } else {
                        var17 = 6.0142803;
                    }
                }
            } else {
                if (input[2] >= 0.42553467) {
                    if (input[0] >= -0.84854) {
                        var17 = 0.42032775;
                    } else {
                        var17 = -23.634436;
                    }
                } else {
                    if (input[0] >= -1.5846001) {
                        var17 = 0.8066593;
                    } else {
                        var17 = -20.790316;
                    }
                }
            }
        }
        double var18;
        if (input[3] >= 10.505693) {
            var18 = -15.158027;
        } else {
            if (input[0] >= -1.6157844) {
                if (input[0] >= -1.5050406) {
                    if (input[0] >= -1.4601038) {
                        var18 = 0.11258989;
                    } else {
                        var18 = 14.442382;
                    }
                } else {
                    if (input[0] >= -1.5142694) {
                        var18 = -35.922295;
                    } else {
                        var18 = 1.2593673;
                    }
                }
            } else {
                var18 = -20.33668;
            }
        }
        double var19;
        if (input[8] >= 0.5) {
            if (input[3] >= 10.505693) {
                var19 = 23.921568;
            } else {
                var19 = 0.49384382;
            }
        } else {
            if (input[9] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[4] >= -0.6545867) {
                        var19 = 0.12744687;
                    } else {
                        var19 = 2.4686174;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var19 = 0.22484401;
                    } else {
                        var19 = -6.859681;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= 3.1452775) {
                        var19 = 0.34566537;
                    } else {
                        var19 = 4.5314803;
                    }
                } else {
                    var19 = 7.110166;
                }
            }
        }
        double var20;
        if (input[0] >= -1.6110797) {
            if (input[0] >= -1.5474442) {
                if (input[0] >= -1.5328473) {
                    if (input[0] >= -1.5237997) {
                        var20 = 0.13573115;
                    } else {
                        var20 = -21.876308;
                    }
                } else {
                    var20 = 14.282313;
                }
            } else {
                var20 = -19.858559;
            }
        } else {
            if (input[0] >= -1.6343021) {
                var20 = 25.054792;
            } else {
                var20 = 19.373396;
            }
        }
        double var21;
        if (input[0] >= -1.1005484) {
            if (input[0] >= -1.0699673) {
                if (input[0] >= 2.6892893) {
                    if (input[0] >= 2.7411628) {
                        var21 = -1.8941181;
                    } else {
                        var21 = -16.262783;
                    }
                } else {
                    if (input[2] >= -3.1908445) {
                        var21 = 0.07606414;
                    } else {
                        var21 = 6.7022796;
                    }
                }
            } else {
                if (input[0] >= -1.0818498) {
                    if (input[0] >= -1.0771451) {
                        var21 = -19.556412;
                    } else {
                        var21 = -41.727806;
                    }
                } else {
                    if (input[0] >= -1.0968087) {
                        var21 = 5.044799;
                    } else {
                        var21 = -22.938114;
                    }
                }
            }
        } else {
            if (input[0] >= -1.1656919) {
                if (input[0] >= -1.1409011) {
                    if (input[0] >= -1.1366789) {
                        var21 = 5.7243176;
                    } else {
                        var21 = -7.8977947;
                    }
                } else {
                    if (input[0] >= -1.1557393) {
                        var21 = 9.533543;
                    } else {
                        var21 = 14.563201;
                    }
                }
            } else {
                if (input[0] >= -1.1700348) {
                    var21 = -26.660381;
                } else {
                    if (input[0] >= -1.1798666) {
                        var21 = 13.019152;
                    } else {
                        var21 = 1.886038;
                    }
                }
            }
        }
        double var22;
        if (input[4] >= -1.061715) {
            if (input[0] >= -1.5846001) {
                if (input[2] >= 0.42553467) {
                    if (input[0] >= -1.3445346) {
                        var22 = -0.22792563;
                    } else {
                        var22 = -11.211833;
                    }
                } else {
                    if (input[0] >= -1.5285044) {
                        var22 = 0.23592049;
                    } else {
                        var22 = 29.51904;
                    }
                }
            } else {
                var22 = -13.298237;
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[6] >= 0.5) {
                    if (input[0] >= -0.061511233) {
                        var22 = -3.6082382;
                    } else {
                        var22 = 3.9404857;
                    }
                } else {
                    if (input[0] >= 2.105291) {
                        var22 = -14.739092;
                    } else {
                        var22 = 6.560977;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -2.2831) {
                        var22 = 3.27665;
                    } else {
                        var22 = -6.206473;
                    }
                } else {
                    if (input[4] >= -2.011681) {
                        var22 = -16.98648;
                    } else {
                        var22 = -2.0693188;
                    }
                }
            }
        }
        double var23;
        if (input[0] >= -1.6110797) {
            if (input[0] >= -1.5304345) {
                if (input[0] >= -1.4247575) {
                    if (input[0] >= -1.4069637) {
                        var23 = 0.0058279335;
                    } else {
                        var23 = -12.574236;
                    }
                } else {
                    if (input[0] >= -1.4281353) {
                        var23 = 28.729404;
                    } else {
                        var23 = 1.0624564;
                    }
                }
            } else {
                if (input[0] >= -1.5441871) {
                    var23 = -21.719168;
                } else {
                    var23 = -6.0660253;
                }
            }
        } else {
            if (input[0] >= -1.6262195) {
                var23 = 8.824266;
            } else {
                var23 = 17.930897;
            }
        }
        double var24;
        if (input[0] >= -1.2988136) {
            if (input[0] >= -1.2829499) {
                if (input[9] >= 0.5) {
                    if (input[4] >= -1.061715) {
                        var24 = 0.078482985;
                    } else {
                        var24 = -2.069406;
                    }
                } else {
                    if (input[4] >= -1.7402622) {
                        var24 = 2.6634102;
                    } else {
                        var24 = 10.424127;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    var24 = 11.484903;
                } else {
                    var24 = 4.8381333;
                }
            }
        } else {
            if (input[0] >= -1.3074391) {
                var24 = -24.193026;
            } else {
                if (input[0] >= -1.3154614) {
                    var24 = 11.129827;
                } else {
                    if (input[4] >= 0.023960479) {
                        var24 = 3.2866764;
                    } else {
                        var24 = -2.8106174;
                    }
                }
            }
        }
        double var25;
        if (input[3] >= 10.505693) {
            var25 = -13.873767;
        } else {
            if (input[0] >= -1.0870372) {
                if (input[0] >= -1.0699673) {
                    if (input[0] >= -0.9522869) {
                        var25 = -0.12327542;
                    } else {
                        var25 = 1.7734003;
                    }
                } else {
                    if (input[2] >= 0.42553467) {
                        var25 = -27.763315;
                    } else {
                        var25 = -4.3645062;
                    }
                }
            } else {
                if (input[0] >= -1.0944563) {
                    var25 = 11.844988;
                } else {
                    if (input[0] >= -1.0986183) {
                        var25 = -14.054545;
                    } else {
                        var25 = 1.4738897;
                    }
                }
            }
        }
        double var26;
        if (input[3] >= 10.505693) {
            var26 = -12.905225;
        } else {
            if (input[13] >= 0.5) {
                if (input[8] >= 0.5) {
                    var26 = -8.218271;
                } else {
                    var26 = -2.7714384;
                }
            } else {
                if (input[2] >= -3.1908445) {
                    if (input[5] >= 0.5) {
                        var26 = 0.1919713;
                    } else {
                        var26 = -0.34239304;
                    }
                } else {
                    var26 = 13.057341;
                }
            }
        }
        double var27;
        if (input[0] >= -1.6110797) {
            if (input[5] >= 0.5) {
                if (input[0] >= -1.3544871) {
                    if (input[4] >= 0.838217) {
                        var27 = -0.28236327;
                    } else {
                        var27 = 0.49259615;
                    }
                } else {
                    if (input[4] >= -1.061715) {
                        var27 = -6.032808;
                    } else {
                        var27 = 6.1769953;
                    }
                }
            } else {
                if (input[0] >= -0.27232254) {
                    if (input[6] >= 0.5) {
                        var27 = 0.30610603;
                    } else {
                        var27 = 7.8584433;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var27 = -0.79939336;
                    } else {
                        var27 = -9.435571;
                    }
                }
            }
        } else {
            var27 = 11.286037;
        }
        double var28;
        if (input[3] >= 10.505693) {
            var28 = 7.1790824;
        } else {
            if (input[4] >= -1.7402622) {
                if (input[13] >= 0.5) {
                    var28 = -4.953042;
                } else {
                    if (input[7] >= 0.5) {
                        var28 = 0.11932213;
                    } else {
                        var28 = -0.42846715;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -2.2831) {
                        var28 = 6.0286536;
                    } else {
                        var28 = -3.695003;
                    }
                } else {
                    if (input[4] >= -2.2831) {
                        var28 = -3.099296;
                    } else {
                        var28 = 4.8989415;
                    }
                }
            }
        }
        double var29;
        if (input[4] >= -0.38316783) {
            if (input[0] >= -1.5389395) {
                if (input[0] >= -1.463361) {
                    if (input[0] >= -1.0731037) {
                        var29 = -0.045030884;
                    } else {
                        var29 = 4.8160086;
                    }
                } else {
                    var29 = -14.874015;
                }
            } else {
                var29 = 22.95401;
            }
        } else {
            if (input[0] >= -1.6187401) {
                if (input[0] >= -1.4884533) {
                    if (input[0] >= -1.4665577) {
                        var29 = -0.22108805;
                    } else {
                        var29 = -22.836859;
                    }
                } else {
                    if (input[0] >= -1.5106503) {
                        var29 = 20.822256;
                    } else {
                        var29 = 3.0943563;
                    }
                }
            } else {
                var29 = -25.067308;
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
