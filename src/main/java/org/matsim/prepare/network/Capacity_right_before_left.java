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

        return score(data);
    }
    public static double score(double[] input) {
        double var0;
        if (input[6] >= 0.5) {
            if (input[0] >= -1.5852636) {
                if (input[0] >= -0.9445662) {
                    if (input[0] >= 0.68438065) {
                        var0 = 466.62134;
                    } else {
                        var0 = 470.00494;
                    }
                } else {
                    if (input[0] >= -1.3503251) {
                        var0 = 478.4692;
                    } else {
                        var0 = 462.68674;
                    }
                }
            } else {
                if (input[0] >= -1.6163273) {
                    var0 = 292.73615;
                } else {
                    if (input[0] >= -1.6256766) {
                        var0 = 394.74445;
                    } else {
                        var0 = 464.99957;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -1.6157844) {
                        var0 = 485.69092;
                    } else {
                        var0 = 394.1112;
                    }
                } else {
                    if (input[2] >= -1.3826549) {
                        var0 = 414.3968;
                    } else {
                        var0 = 483.19583;
                    }
                }
            } else {
                if (input[2] >= -1.3826549) {
                    if (input[4] >= -1.6045527) {
                        var0 = 343.04166;
                    } else {
                        var0 = 381.57214;
                    }
                } else {
                    if (input[4] >= -2.011681) {
                        var0 = 356.06854;
                    } else {
                        var0 = 466.00543;
                    }
                }
            }
        }
        double var1;
        if (input[6] >= 0.5) {
            if (input[0] >= -1.5304345) {
                if (input[0] >= -0.88364506) {
                    if (input[4] >= -1.6045527) {
                        var1 = 329.8695;
                    } else {
                        var1 = 337.96313;
                    }
                } else {
                    var1 = 334.5658;
                }
            } else {
                if (input[0] >= -1.5328473) {
                    var1 = 212.8456;
                } else {
                    if (input[4] >= 0.838217) {
                        var1 = 339.2703;
                    } else {
                        var1 = 292.02188;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var1 = 292.33136;
                    } else {
                        var1 = 341.24716;
                    }
                } else {
                    if (input[4] >= -1.4688432) {
                        var1 = 289.9826;
                    } else {
                        var1 = 340.83737;
                    }
                }
            } else {
                if (input[4] >= -2.011681) {
                    var1 = 250.50919;
                } else {
                    var1 = 327.6152;
                }
            }
        }
        double var2;
        if (input[0] >= 0.14543968) {
            if (input[0] >= 0.14694765) {
                if (input[12] >= 0.5) {
                    var2 = 177.52448;
                } else {
                    if (input[4] >= -0.6545867) {
                        var2 = 231.19627;
                    } else {
                        var2 = 223.10208;
                    }
                }
            } else {
                if (input[0] >= 0.14616351) {
                    var2 = 143.72774;
                } else {
                    var2 = 214.07964;
                }
            }
        } else {
            if (input[0] >= -1.427713) {
                if (input[5] >= 0.5) {
                    if (input[4] >= 0.838217) {
                        var2 = 233.46802;
                    } else {
                        var2 = 238.90514;
                    }
                } else {
                    if (input[1] >= 8.362897) {
                        var2 = 138.64879;
                    } else {
                        var2 = 232.9262;
                    }
                }
            } else {
                if (input[0] >= -1.4294623) {
                    var2 = 164.45105;
                } else {
                    if (input[0] >= -1.6478736) {
                        var2 = 218.29344;
                    } else {
                        var2 = 265.30496;
                    }
                }
            }
        }
        double var3;
        if (input[13] >= -0.5) {
            if (input[6] >= 0.5) {
                if (input[0] >= -1.5852636) {
                    if (input[0] >= -1.0929484) {
                        var3 = 162.98166;
                    } else {
                        var3 = 167.5452;
                    }
                } else {
                    if (input[0] >= -1.6146384) {
                        var3 = 55.67564;
                    } else {
                        var3 = 156.83183;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= 0.20069094) {
                        var3 = 162.38368;
                    } else {
                        var3 = 170.86473;
                    }
                } else {
                    if (input[4] >= -2.011681) {
                        var3 = 105.848495;
                    } else {
                        var3 = 159.89294;
                    }
                }
            }
        } else {
            var3 = 3.3443801;
        }
        double var4;
        if (input[6] >= 0.5) {
            if (input[0] >= 2.3579628) {
                if (input[4] >= -0.92600554) {
                    if (input[0] >= 2.48077) {
                        var4 = 110.30134;
                    } else {
                        var4 = 91.96336;
                    }
                } else {
                    if (input[0] >= 2.6081014) {
                        var4 = 117.92624;
                    } else {
                        var4 = 102.021126;
                    }
                }
            } else {
                if (input[4] >= -1.6045527) {
                    if (input[0] >= -1.5304345) {
                        var4 = 114.95214;
                    } else {
                        var4 = 99.02398;
                    }
                } else {
                    if (input[4] >= -2.2831) {
                        var4 = 125.27053;
                    } else {
                        var4 = 110.92638;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -0.34977078) {
                        var4 = 116.76641;
                    } else {
                        var4 = 123.02831;
                    }
                } else {
                    if (input[4] >= -1.4688432) {
                        var4 = 90.145386;
                    } else {
                        var4 = 120.992165;
                    }
                }
            } else {
                if (input[4] >= -2.011681) {
                    if (input[0] >= -1.4094367) {
                        var4 = 71.74196;
                    } else {
                        var4 = 104.67503;
                    }
                } else {
                    if (input[0] >= -0.8258001) {
                        var4 = 123.89898;
                    } else {
                        var4 = 88.326744;
                    }
                }
            }
        }
        double var5;
        if (input[13] >= -0.5) {
            if (input[0] >= -1.5852636) {
                if (input[0] >= -1.366611) {
                    if (input[0] >= -0.5542487) {
                        var5 = 80.540985;
                    } else {
                        var5 = 83.037796;
                    }
                } else {
                    if (input[0] >= -1.3758999) {
                        var5 = 51.634266;
                    } else {
                        var5 = 76.0526;
                    }
                }
            } else {
                if (input[0] >= -1.6262195) {
                    if (input[6] >= 0.5) {
                        var5 = 39.528603;
                    } else {
                        var5 = -21.731543;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var5 = 97.49278;
                    } else {
                        var5 = 60.885918;
                    }
                }
            }
        } else {
            var5 = -33.031845;
        }
        double var6;
        if (input[4] >= -1.061715) {
            if (input[6] >= 0.5) {
                if (input[0] >= -1.0927072) {
                    if (input[0] >= 2.38589) {
                        var6 = 52.071594;
                    } else {
                        var6 = 56.467945;
                    }
                } else {
                    if (input[0] >= -1.3503251) {
                        var6 = 63.172752;
                    } else {
                        var6 = 52.752777;
                    }
                }
            } else {
                if (input[0] >= -1.596724) {
                    if (input[0] >= -0.51021653) {
                        var6 = 58.39916;
                    } else {
                        var6 = 63.135002;
                    }
                } else {
                    if (input[0] >= -1.6211528) {
                        var6 = -17.791441;
                    } else {
                        var6 = 21.879438;
                    }
                }
            }
        } else {
            if (input[13] >= -0.5) {
                if (input[4] >= -1.4688432) {
                    if (input[5] >= 0.5) {
                        var6 = 59.408897;
                    } else {
                        var6 = 43.521816;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var6 = 65.346756;
                    } else {
                        var6 = 52.609497;
                    }
                }
            } else {
                var6 = -23.307207;
            }
        }
        double var7;
        if (input[13] >= -0.5) {
            if (input[0] >= -1.5100471) {
                if (input[4] >= -1.061715) {
                    if (input[0] >= 0.53292227) {
                        var7 = 39.092808;
                    } else {
                        var7 = 40.821293;
                    }
                } else {
                    if (input[3] >= 10.505693) {
                        var7 = 65.264824;
                    } else {
                        var7 = 37.026802;
                    }
                }
            } else {
                if (input[0] >= -1.5120978) {
                    var7 = -50.087914;
                } else {
                    if (input[0] >= -1.6343021) {
                        var7 = 27.572048;
                    } else {
                        var7 = 55.395817;
                    }
                }
            }
        } else {
            var7 = -16.44551;
        }
        double var8;
        if (input[0] >= -1.3659475) {
            if (input[0] >= 1.0397741) {
                if (input[12] >= 0.5) {
                    var8 = -40.67526;
                } else {
                    if (input[0] >= 1.0493044) {
                        var8 = 26.800558;
                    } else {
                        var8 = -5.831334;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= -1.1484408) {
                        var8 = 27.786251;
                    } else {
                        var8 = 32.66298;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var8 = 31.144882;
                    } else {
                        var8 = 4.350095;
                    }
                }
            }
        } else {
            if (input[2] >= -1.3826549) {
                if (input[4] >= -0.92600554) {
                    if (input[0] >= -1.3758999) {
                        var8 = 0.9267223;
                    } else {
                        var8 = 24.349064;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var8 = -24.21722;
                    } else {
                        var8 = 31.879498;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -1.5112534) {
                        var8 = 40.195362;
                    } else {
                        var8 = 62.471424;
                    }
                } else {
                    if (input[0] >= -1.5227139) {
                        var8 = 24.514246;
                    } else {
                        var8 = -11.253212;
                    }
                }
            }
        }
        double var9;
        if (input[0] >= -1.5852636) {
            if (input[0] >= -1.5371299) {
                if (input[0] >= -1.5304345) {
                    if (input[0] >= -1.5294695) {
                        var9 = 19.906067;
                    } else {
                        var9 = 61.4174;
                    }
                } else {
                    if (input[0] >= -1.5328473) {
                        var9 = -45.538467;
                    } else {
                        var9 = -6.110075;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[5] >= 0.5) {
                        var9 = 32.877327;
                    } else {
                        var9 = 55.53233;
                    }
                } else {
                    var9 = 9.498032;
                }
            }
        } else {
            if (input[0] >= -1.6343021) {
                if (input[4] >= 0.5667982) {
                    var9 = 56.610706;
                } else {
                    if (input[4] >= -0.92600554) {
                        var9 = -40.120193;
                    } else {
                        var9 = 30.482803;
                    }
                }
            } else {
                if (input[0] >= -1.6440735) {
                    if (input[4] >= 0.43108875) {
                        var9 = 29.240757;
                    } else {
                        var9 = 44.92831;
                    }
                } else {
                    if (input[0] >= -1.6478736) {
                        var9 = 3.5151563;
                    } else {
                        var9 = 38.313503;
                    }
                }
            }
        }
        double var10;
        if (input[5] >= 0.5) {
            if (input[0] >= 2.3825727) {
                if (input[0] >= 2.4833035) {
                    if (input[6] >= 0.5) {
                        var10 = 13.464442;
                    } else {
                        var10 = 7.183055;
                    }
                } else {
                    if (input[0] >= 2.4369192) {
                        var10 = -26.441107;
                    } else {
                        var10 = 10.802474;
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    var10 = -15.074027;
                } else {
                    if (input[6] >= 0.5) {
                        var10 = 14.060504;
                    } else {
                        var10 = 15.414316;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[0] >= -1.5978098) {
                    if (input[0] >= -1.4456275) {
                        var10 = 13.548023;
                    } else {
                        var10 = 37.39084;
                    }
                } else {
                    if (input[0] >= -1.644918) {
                        var10 = -38.709038;
                    } else {
                        var10 = 11.308897;
                    }
                }
            } else {
                if (input[0] >= -0.66903377) {
                    if (input[0] >= 0.19870044) {
                        var10 = -4.688328;
                    } else {
                        var10 = 28.190065;
                    }
                } else {
                    if (input[2] >= -1.3826549) {
                        var10 = -25.673597;
                    } else {
                        var10 = 16.354755;
                    }
                }
            }
        }
        double var11;
        if (input[13] >= 0.5) {
            if (input[0] >= -0.47354323) {
                if (input[6] >= 0.5) {
                    if (input[5] >= 0.5) {
                        var11 = 15.648128;
                    } else {
                        var11 = 4.988207;
                    }
                } else {
                    if (input[4] >= -1.4688432) {
                        var11 = 32.60557;
                    } else {
                        var11 = 20.084703;
                    }
                }
            } else {
                if (input[0] >= -0.6880339) {
                    if (input[4] >= -1.6045527) {
                        var11 = 38.09304;
                    } else {
                        var11 = 24.0836;
                    }
                } else {
                    if (input[4] >= 0.7025077) {
                        var11 = 10.526462;
                    } else {
                        var11 = 24.78605;
                    }
                }
            }
        } else {
            if (input[13] >= -0.5) {
                if (input[4] >= -2.011681) {
                    if (input[1] >= 16.756334) {
                        var11 = 30.747383;
                    } else {
                        var11 = 9.675043;
                    }
                } else {
                    if (input[0] >= -0.64955103) {
                        var11 = 18.263987;
                    } else {
                        var11 = 10.674244;
                    }
                }
            } else {
                var11 = -31.448286;
            }
        }
        double var12;
        if (input[2] >= -1.3826549) {
            if (input[4] >= -0.6545867) {
                if (input[0] >= -1.3504457) {
                    if (input[0] >= -1.0997643) {
                        var12 = 6.896288;
                    } else {
                        var12 = 9.650572;
                    }
                } else {
                    if (input[0] >= -1.3511093) {
                        var12 = -59.13093;
                    } else {
                        var12 = 4.162933;
                    }
                }
            } else {
                if (input[0] >= -0.66903377) {
                    if (input[0] >= 0.20069094) {
                        var12 = 0.19959787;
                    } else {
                        var12 = 15.993559;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var12 = -17.865093;
                    } else {
                        var12 = 12.746868;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[0] >= 2.9364724) {
                    if (input[0] >= 3.6909294) {
                        var12 = 7.737254;
                    } else {
                        var12 = -46.958233;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var12 = 11.803724;
                    } else {
                        var12 = -22.189867;
                    }
                }
            } else {
                if (input[0] >= 0.2529866) {
                    if (input[0] >= 0.4409374) {
                        var12 = 10.420569;
                    } else {
                        var12 = 21.520323;
                    }
                } else {
                    if (input[0] >= 0.17511612) {
                        var12 = -49.609844;
                    } else {
                        var12 = 0.26695496;
                    }
                }
            }
        }
        double var13;
        if (input[1] >= 25.149773) {
            if (input[6] >= 0.5) {
                var13 = 26.310762;
            } else {
                var13 = 7.7730246;
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[8] >= 0.5) {
                    var13 = -21.108683;
                } else {
                    if (input[12] >= 0.5) {
                        var13 = -12.091308;
                    } else {
                        var13 = 5.1118402;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[4] >= -0.6545867) {
                        var13 = 4.255017;
                    } else {
                        var13 = 7.2176733;
                    }
                } else {
                    if (input[2] >= -1.3826549) {
                        var13 = -4.3654823;
                    } else {
                        var13 = 6.700545;
                    }
                }
            }
        }
        double var14;
        if (input[0] >= -1.2037525) {
            if (input[0] >= -1.1345074) {
                if (input[0] >= -1.1327583) {
                    if (input[13] >= 0.5) {
                        var14 = 11.715785;
                    } else {
                        var14 = 3.3673573;
                    }
                } else {
                    var14 = -44.981644;
                }
            } else {
                if (input[4] >= -0.11174896) {
                    if (input[0] >= -1.1696728) {
                        var14 = -2.2092047;
                    } else {
                        var14 = 4.057778;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var14 = 8.025472;
                    } else {
                        var14 = 12.23667;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[2] >= -1.3826549) {
                    if (input[0] >= -1.5155361) {
                        var14 = 3.2378309;
                    } else {
                        var14 = 12.172564;
                    }
                } else {
                    if (input[0] >= -1.2504386) {
                        var14 = -77.07368;
                    } else {
                        var14 = 0.13527858;
                    }
                }
            } else {
                if (input[0] >= -1.2152731) {
                    if (input[4] >= -0.92600554) {
                        var14 = -14.534853;
                    } else {
                        var14 = -73.489006;
                    }
                } else {
                    if (input[0] >= -1.2204605) {
                        var14 = 35.709373;
                    } else {
                        var14 = -1.7984558;
                    }
                }
            }
        }
        double var15;
        if (input[5] >= 0.5) {
            if (input[12] >= 0.5) {
                if (input[7] >= 0.5) {
                    var15 = 13.637571;
                } else {
                    var15 = -55.15232;
                }
            } else {
                if (input[0] >= -0.16616291) {
                    if (input[0] >= -0.12882608) {
                        var15 = 2.1979604;
                    } else {
                        var15 = -2.261051;
                    }
                } else {
                    if (input[0] >= -1.3581665) {
                        var15 = 3.2767138;
                    } else {
                        var15 = -0.39162663;
                    }
                }
            }
        } else {
            if (input[0] >= -1.4289798) {
                if (input[0] >= -1.4250591) {
                    if (input[0] >= -1.3511093) {
                        var15 = 1.4868857;
                    } else {
                        var15 = 7.145003;
                    }
                } else {
                    if (input[0] >= -1.4275924) {
                        var15 = 89.03074;
                    } else {
                        var15 = 1.2014859;
                    }
                }
            } else {
                if (input[0] >= -1.4380274) {
                    var15 = -74.90811;
                } else {
                    if (input[0] >= -1.4490657) {
                        var15 = 11.031182;
                    } else {
                        var15 = -3.9957147;
                    }
                }
            }
        }
        double var16;
        if (input[0] >= -1.3735476) {
            if (input[0] >= -1.354849) {
                if (input[0] >= -1.3540648) {
                    if (input[6] >= 0.5) {
                        var16 = 1.44668;
                    } else {
                        var16 = 2.4234858;
                    }
                } else {
                    var16 = -41.093052;
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[5] >= 0.5) {
                        var16 = 16.280134;
                    } else {
                        var16 = 22.384052;
                    }
                } else {
                    var16 = -60.687233;
                }
            }
        } else {
            if (input[0] >= -1.3758999) {
                var16 = -69.89846;
            } else {
                if (input[13] >= 0.5) {
                    var16 = -36.098602;
                } else {
                    if (input[6] >= 0.5) {
                        var16 = 3.557815;
                    } else {
                        var16 = -4.037232;
                    }
                }
            }
        }
        double var17;
        if (input[0] >= 2.8449097) {
            if (input[13] >= 0.5) {
                var17 = -43.665493;
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= 3.3359582) {
                        var17 = -2.9900134;
                    } else {
                        var17 = 1.4542055;
                    }
                } else {
                    if (input[0] >= 2.8773005) {
                        var17 = -1.1137507;
                    } else {
                        var17 = -45.842186;
                    }
                }
            }
        } else {
            if (input[4] >= -2.011681) {
                if (input[2] >= -1.3826549) {
                    if (input[4] >= -0.6545867) {
                        var17 = 1.2896487;
                    } else {
                        var17 = -1.4374533;
                    }
                } else {
                    if (input[0] >= 0.2876091) {
                        var17 = 7.738073;
                    } else {
                        var17 = 1.5536275;
                    }
                }
            } else {
                if (input[3] >= 10.505693) {
                    var17 = -6.3130155;
                } else {
                    if (input[0] >= -1.0173097) {
                        var17 = 3.154225;
                    } else {
                        var17 = 13.211945;
                    }
                }
            }
        }
        double var18;
        if (input[5] >= 0.5) {
            if (input[0] >= -1.6039622) {
                if (input[0] >= -1.5804381) {
                    if (input[0] >= -0.15759775) {
                        var18 = 0.54053617;
                    } else {
                        var18 = 1.574238;
                    }
                } else {
                    var18 = -24.0854;
                }
            } else {
                if (input[0] >= -1.6291751) {
                    var18 = 34.680035;
                } else {
                    if (input[0] >= -1.6446164) {
                        var18 = 6.493754;
                    } else {
                        var18 = 19.096186;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[0] >= -1.3511093) {
                    if (input[0] >= -1.3494203) {
                        var18 = 0.66897804;
                    } else {
                        var18 = -62.157246;
                    }
                } else {
                    if (input[0] >= -1.6201878) {
                        var18 = 7.5635023;
                    } else {
                        var18 = -12.761621;
                    }
                }
            } else {
                if (input[0] >= -1.4949073) {
                    if (input[0] >= -1.4530466) {
                        var18 = -4.928922;
                    } else {
                        var18 = 51.944523;
                    }
                } else {
                    var18 = -52.316296;
                }
            }
        }
        double var19;
        if (input[2] >= -3.1908445) {
            if (input[3] >= 10.505693) {
                var19 = -43.482193;
            } else {
                if (input[1] >= 16.756334) {
                    if (input[6] >= 0.5) {
                        var19 = 12.223103;
                    } else {
                        var19 = 2.7963955;
                    }
                } else {
                    if (input[4] >= -2.011681) {
                        var19 = 0.5642157;
                    } else {
                        var19 = 3.4723518;
                    }
                }
            }
        } else {
            if (input[4] >= -1.4688432) {
                var19 = 27.016024;
            } else {
                if (input[9] >= 0.5) {
                    var19 = 8.271986;
                } else {
                    if (input[4] >= -1.7402622) {
                        var19 = 3.7981765;
                    } else {
                        var19 = 1.3676744;
                    }
                }
            }
        }
        double var20;
        if (input[0] >= -1.6183782) {
            if (input[0] >= -1.6039622) {
                if (input[0] >= -1.5852636) {
                    if (input[0] >= -1.5508823) {
                        var20 = 0.43587607;
                    } else {
                        var20 = 14.597141;
                    }
                } else {
                    var20 = -22.778797;
                }
            } else {
                var20 = 58.376743;
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -1.637137) {
                        var20 = 19.646;
                    } else {
                        var20 = 3.0549476;
                    }
                } else {
                    if (input[0] >= -1.6343021) {
                        var20 = -34.137146;
                    } else {
                        var20 = 3.3725185;
                    }
                }
            } else {
                if (input[0] >= -1.6211528) {
                    var20 = -32.571907;
                } else {
                    var20 = -42.23918;
                }
            }
        }
        double var21;
        if (input[11] >= 0.5) {
            if (input[0] >= -1.5304345) {
                if (input[0] >= -1.4884533) {
                    if (input[0] >= -1.4787421) {
                        var21 = 0.3482988;
                    } else {
                        var21 = -79.88982;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var21 = 15.597628;
                    } else {
                        var21 = -7.415638;
                    }
                }
            } else {
                if (input[0] >= -1.5347171) {
                    var21 = -55.825638;
                } else {
                    if (input[0] >= -1.5417743) {
                        var21 = 14.120041;
                    } else {
                        var21 = -4.3358445;
                    }
                }
            }
        } else {
            if (input[0] >= 0.6731615) {
                var21 = 40.94199;
            } else {
                if (input[0] >= -0.83376205) {
                    if (input[0] >= -0.2579065) {
                        var21 = 4.6565022;
                    } else {
                        var21 = 18.349714;
                    }
                } else {
                    var21 = -3.8226035;
                }
            }
        }
        double var22;
        if (input[0] >= -1.6478736) {
            if (input[0] >= -1.6183782) {
                if (input[11] >= 0.5) {
                    if (input[0] >= -1.6039622) {
                        var22 = 0.24439019;
                    } else {
                        var22 = 42.467007;
                    }
                } else {
                    if (input[0] >= 0.6731615) {
                        var22 = 28.888588;
                    } else {
                        var22 = 4.679001;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= -1.6428069) {
                        var22 = 4.9078875;
                    } else {
                        var22 = -20.070957;
                    }
                } else {
                    var22 = -55.66446;
                }
            }
        } else {
            var22 = 22.851473;
        }
        double var23;
        if (input[0] >= -1.3122644) {
            if (input[0] >= -1.2831309) {
                if (input[0] >= -1.2109907) {
                    if (input[0] >= -0.9442646) {
                        var23 = 0.0636748;
                    } else {
                        var23 = 1.881123;
                    }
                } else {
                    if (input[2] >= 0.42553467) {
                        var23 = 7.8208694;
                    } else {
                        var23 = -5.5967727;
                    }
                }
            } else {
                if (input[0] >= -1.2983913) {
                    if (input[7] >= 0.5) {
                        var23 = 10.014177;
                    } else {
                        var23 = 19.277205;
                    }
                } else {
                    if (input[0] >= -1.3074391) {
                        var23 = -6.2367563;
                    } else {
                        var23 = 13.043918;
                    }
                }
            }
        } else {
            if (input[0] >= -1.3187186) {
                if (input[5] >= 0.5) {
                    if (input[0] >= -1.3154614) {
                        var23 = -32.111248;
                    } else {
                        var23 = -87.10817;
                    }
                } else {
                    if (input[0] >= -1.3179344) {
                        var23 = -15.950799;
                    } else {
                        var23 = 43.07536;
                    }
                }
            } else {
                if (input[2] >= 0.42553467) {
                    if (input[0] >= -1.5373712) {
                        var23 = -15.925337;
                    } else {
                        var23 = 11.06607;
                    }
                } else {
                    if (input[0] >= -1.3205884) {
                        var23 = 25.36079;
                    } else {
                        var23 = 0.17738214;
                    }
                }
            }
        }
        double var24;
        if (input[13] >= -0.5) {
            if (input[0] >= -1.6478736) {
                if (input[0] >= -1.5100471) {
                    if (input[5] >= 0.5) {
                        var24 = 0.135155;
                    } else {
                        var24 = -0.481404;
                    }
                } else {
                    if (input[4] >= 0.023960479) {
                        var24 = 3.5958784;
                    } else {
                        var24 = -6.741524;
                    }
                }
            } else {
                var24 = 16.053377;
            }
        } else {
            var24 = -19.986526;
        }
        double var25;
        if (input[0] >= 1.0397741) {
            if (input[0] >= 1.0421264) {
                if (input[12] >= 0.5) {
                    var25 = -56.775375;
                } else {
                    if (input[0] >= 1.1725943) {
                        var25 = -0.06442593;
                    } else {
                        var25 = -5.2056046;
                    }
                }
            } else {
                var25 = -76.71;
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[3] >= 31.590145) {
                    var25 = -8.205693;
                } else {
                    if (input[0] >= -0.45792088) {
                        var25 = 4.740659;
                    } else {
                        var25 = 11.995896;
                    }
                }
            } else {
                if (input[0] >= 0.75857174) {
                    if (input[4] >= -0.92600554) {
                        var25 = 1.4232748;
                    } else {
                        var25 = 17.300095;
                    }
                } else {
                    if (input[0] >= 0.73022234) {
                        var25 = -8.497546;
                    } else {
                        var25 = 0.12893362;
                    }
                }
            }
        }
        double var26;
        if (input[7] >= 0.5) {
            if (input[4] >= 0.838217) {
                if (input[8] >= 0.5) {
                    var26 = -20.270277;
                } else {
                    if (input[5] >= 0.5) {
                        var26 = -0.19726118;
                    } else {
                        var26 = -8.695145;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[12] >= 0.5) {
                        var26 = 15.484591;
                    } else {
                        var26 = 0.93560696;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var26 = 0.2716806;
                    } else {
                        var26 = -12.913544;
                    }
                }
            }
        } else {
            if (input[11] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[2] >= -1.3826549) {
                        var26 = -0.35829026;
                    } else {
                        var26 = -5.42865;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var26 = -12.19611;
                    } else {
                        var26 = 3.367754;
                    }
                }
            } else {
                var26 = -20.874466;
            }
        }
        double var27;
        if (input[0] >= -1.0459607) {
            if (input[7] >= 0.5) {
                if (input[0] >= -0.8777942) {
                    if (input[2] >= -1.3826549) {
                        var27 = -0.10440314;
                    } else {
                        var27 = 1.4415932;
                    }
                } else {
                    if (input[2] >= -3.1908445) {
                        var27 = 3.9411235;
                    } else {
                        var27 = -29.65056;
                    }
                }
            } else {
                if (input[0] >= -0.99046814) {
                    if (input[6] >= 0.5) {
                        var27 = -0.279562;
                    } else {
                        var27 = -13.49964;
                    }
                } else {
                    if (input[4] >= -1.061715) {
                        var27 = 7.0898876;
                    } else {
                        var27 = 15.100019;
                    }
                }
            }
        } else {
            if (input[2] >= -3.1908445) {
                if (input[0] >= -1.0474083) {
                    var27 = -91.69284;
                } else {
                    if (input[4] >= -1.061715) {
                        var27 = -0.16142209;
                    } else {
                        var27 = -4.0291386;
                    }
                }
            } else {
                if (input[0] >= -1.2378924) {
                    var27 = 15.073005;
                } else {
                    var27 = 2.2967687;
                }
            }
        }
        double var28;
        if (input[4] >= -2.011681) {
            if (input[4] >= -1.061715) {
                if (input[6] >= 0.5) {
                    if (input[4] >= -0.6545867) {
                        var28 = -0.20078391;
                    } else {
                        var28 = 12.32144;
                    }
                } else {
                    if (input[4] >= -0.6545867) {
                        var28 = 0.450919;
                    } else {
                        var28 = -58.469585;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[2] >= -1.3826549) {
                        var28 = 6.992091;
                    } else {
                        var28 = 0.86424536;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var28 = -3.0282528;
                    } else {
                        var28 = -17.008268;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[4] >= -2.2831) {
                    if (input[2] >= -3.1908445) {
                        var28 = 11.62107;
                    } else {
                        var28 = -8.82208;
                    }
                } else {
                    var28 = -6.2628446;
                }
            } else {
                if (input[2] >= -1.3826549) {
                    var28 = 25.961262;
                } else {
                    if (input[7] >= 0.5) {
                        var28 = 2.94718;
                    } else {
                        var28 = 7.0245457;
                    }
                }
            }
        }
        double var29;
        if (input[4] >= -1.061715) {
            if (input[0] >= -1.6478736) {
                if (input[0] >= -1.5852636) {
                    if (input[0] >= -1.531279) {
                        var29 = 0.058193047;
                    } else {
                        var29 = 16.915045;
                    }
                } else {
                    if (input[0] >= -1.6146384) {
                        var29 = -27.914465;
                    } else {
                        var29 = -3.6429138;
                    }
                }
            } else {
                var29 = 23.174778;
            }
        } else {
            if (input[0] >= -1.2204605) {
                if (input[0] >= -1.2135843) {
                    if (input[0] >= -1.1322154) {
                        var29 = -0.14102592;
                    } else {
                        var29 = -8.260329;
                    }
                } else {
                    var29 = 39.825603;
                }
            } else {
                if (input[0] >= -1.2263716) {
                    var29 = -65.10586;
                } else {
                    if (input[2] >= -1.3826549) {
                        var29 = -14.132271;
                    } else {
                        var29 = 0.33492744;
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
