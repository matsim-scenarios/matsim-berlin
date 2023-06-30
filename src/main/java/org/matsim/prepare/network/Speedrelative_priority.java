package org.matsim.prepare.network;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
* Generated model, do not modify.
*/
public class Speedrelative_priority implements FeatureRegressor {

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 146.19835633626096) / 105.72932259026847;
		data[1] = (ft.getDouble("speed") - 13.76408333576493) / 4.076913004789775;
		data[2] = (ft.getDouble("numFoes") - 1.284905605322284) / 1.1453318033366016;
		data[3] = (ft.getDouble("numLanes") - 1.2330250065653177) / 0.6202185676684495;
		data[4] = (ft.getDouble("junctionSize") - 7.121677219806834) / 4.776491989487674;
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
        if (input[3] >= 0.43045312) {
            if (input[0] >= -0.5772605) {
                if (input[13] >= -0.5) {
                    if (input[0] >= 0.65636134) {
                        var0 = 0.1888564;
                    } else {
                        var0 = 0.18104818;
                    }
                } else {
                    if (input[1] >= 4.118537) {
                        var0 = 0.16143057;
                    } else {
                        var0 = 0.17617561;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[4] >= -0.7582295) {
                        var0 = 0.15550487;
                    } else {
                        var0 = 0.16968238;
                    }
                } else {
                    if (input[1] >= -0.9919474) {
                        var0 = 0.1705431;
                    } else {
                        var0 = 0.18418506;
                    }
                }
            }
        } else {
            if (input[1] >= -0.31005895) {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.2560629) {
                        var0 = 0.16763726;
                    } else {
                        var0 = 0.16117033;
                    }
                } else {
                    if (input[0] >= 0.07804498) {
                        var0 = 0.14852498;
                    } else {
                        var0 = 0.1286254;
                    }
                }
            } else {
                if (input[6] >= 0.5) {
                    if (input[0] >= -0.79158133) {
                        var0 = 0.1712288;
                    } else {
                        var0 = 0.1617287;
                    }
                } else {
                    if (input[5] >= 0.5) {
                        var0 = 0.1711422;
                    } else {
                        var0 = 0.17614679;
                    }
                }
            }
        }
        double var1;
        if (input[3] >= 0.43045312) {
            if (input[0] >= -0.33555835) {
                if (input[10] >= 0.5) {
                    if (input[0] >= 0.59389055) {
                        var1 = 0.086546466;
                    } else {
                        var1 = 0.053711362;
                    }
                } else {
                    if (input[1] >= 2.4150422) {
                        var1 = 0.09588494;
                    } else {
                        var1 = 0.10718819;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[3] >= 2.042788) {
                        var1 = 0.101470456;
                    } else {
                        var1 = 0.096411675;
                    }
                } else {
                    var1 = 0.06736557;
                }
            }
        } else {
            if (input[1] >= 1.7331536) {
                if (input[13] >= 0.5) {
                    if (input[0] >= 1.1871035) {
                        var1 = 0.07557541;
                    } else {
                        var1 = 0.099804156;
                    }
                } else {
                    if (input[1] >= 2.4150422) {
                        var1 = 0.07486619;
                    } else {
                        var1 = 0.08248144;
                    }
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -0.84204036) {
                        var1 = 0.09716446;
                    } else {
                        var1 = 0.092090994;
                    }
                } else {
                    if (input[1] >= -0.31005895) {
                        var1 = 0.08089443;
                    } else {
                        var1 = 0.095458195;
                    }
                }
            }
        }
        double var2;
        if (input[8] >= 0.5) {
            if (input[1] >= 4.118537) {
                if (input[4] >= -0.54887086) {
                    var2 = 0.07963477;
                } else {
                    if (input[11] >= 0.5) {
                        var2 = 0.05039306;
                    } else {
                        var2 = 0.073792145;
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    if (input[13] >= 0.5) {
                        var2 = 0.048910923;
                    } else {
                        var2 = 0.021266619;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var2 = 0.059559442;
                    } else {
                        var2 = 0.06380141;
                    }
                }
            }
        } else {
            if (input[1] >= 1.0524917) {
                if (input[13] >= 1.5) {
                    var2 = 0.06235062;
                } else {
                    if (input[7] >= 0.5) {
                        var2 = 0.04767774;
                    } else {
                        var2 = 0.026950454;
                    }
                }
            } else {
                if (input[12] >= 0.5) {
                    if (input[4] >= -0.7582295) {
                        var2 = 0.0570242;
                    } else {
                        var2 = 0.044612475;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var2 = 0.04970776;
                    } else {
                        var2 = 0.056065295;
                    }
                }
            }
        }
        double var3;
        if (input[0] >= 0.14306952) {
            if (input[8] >= 0.5) {
                if (input[12] >= 0.5) {
                    var3 = 0.039056495;
                } else {
                    if (input[2] >= -0.6853085) {
                        var3 = 0.027812362;
                    } else {
                        var3 = 0.036015097;
                    }
                }
            } else {
                if (input[13] >= 1.5) {
                    var3 = 0.039890528;
                } else {
                    if (input[12] >= 0.5) {
                        var3 = 0.034407873;
                    } else {
                        var3 = 0.03274811;
                    }
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[5] >= 0.5) {
                    if (input[11] >= 0.5) {
                        var3 = 0.025148151;
                    } else {
                        var3 = 0.030721838;
                    }
                } else {
                    if (input[13] >= 2.5) {
                        var3 = 0.048614703;
                    } else {
                        var3 = 0.032046624;
                    }
                }
            } else {
                if (input[4] >= -1.1769469) {
                    if (input[10] >= 0.5) {
                        var3 = 0.028025933;
                    } else {
                        var3 = 0.02120694;
                    }
                } else {
                    var3 = 0.035198834;
                }
            }
        }
        double var4;
        if (input[1] >= -0.31005895) {
            if (input[7] >= 0.5) {
                if (input[0] >= -0.86322653) {
                    if (input[4] >= 1.5447159) {
                        var4 = 0.014846236;
                    } else {
                        var4 = 0.019065423;
                    }
                } else {
                    if (input[0] >= -1.2620752) {
                        var4 = 0.015084179;
                    } else {
                        var4 = 0.007031629;
                    }
                }
            } else {
                if (input[0] >= 1.2903388) {
                    if (input[1] >= 0.37182954) {
                        var4 = 0.0044706105;
                    } else {
                        var4 = 0.023516517;
                    }
                } else {
                    if (input[4] >= -1.1769469) {
                        var4 = 0.007816034;
                    } else {
                        var4 = 0.025951868;
                    }
                }
            }
        } else {
            if (input[5] >= 0.5) {
                if (input[0] >= -0.96405005) {
                    if (input[0] >= -0.75876164) {
                        var4 = 0.022757303;
                    } else {
                        var4 = 0.019313332;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var4 = 0.01786411;
                    } else {
                        var4 = 0.003552178;
                    }
                }
            } else {
                if (input[0] >= -0.6706593) {
                    if (input[0] >= -0.5668565) {
                        var4 = 0.022789488;
                    } else {
                        var4 = 0.01854089;
                    }
                } else {
                    if (input[7] >= 0.5) {
                        var4 = 0.027001208;
                    } else {
                        var4 = 0.014189383;
                    }
                }
            }
        }
        double var5;
        if (input[1] >= 1.0524917) {
            if (input[4] >= 0.28856382) {
                if (input[0] >= 2.714116) {
                    var5 = 0.015016946;
                } else {
                    if (input[0] >= -0.3609534) {
                        var5 = 0.0036740887;
                    } else {
                        var5 = -0.0027324522;
                    }
                }
            } else {
                if (input[13] >= -1.5) {
                    if (input[0] >= -1.1726487) {
                        var5 = 0.007165794;
                    } else {
                        var5 = -0.0073500983;
                    }
                } else {
                    var5 = 0.02306534;
                }
            }
        } else {
            if (input[1] >= -0.9919474) {
                if (input[0] >= -0.6503717) {
                    if (input[10] >= 0.5) {
                        var5 = 0.0037313679;
                    } else {
                        var5 = 0.011499821;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var5 = 0.0051103244;
                    } else {
                        var5 = 0.009621708;
                    }
                }
            } else {
                if (input[0] >= -1.2361126) {
                    if (input[4] >= -0.7582295) {
                        var5 = 0.012594239;
                    } else {
                        var5 = 0.014724854;
                    }
                } else {
                    if (input[4] >= -0.54887086) {
                        var5 = -0.0032194115;
                    } else {
                        var5 = 0.009138417;
                    }
                }
            }
        }
        double var6;
        if (input[1] >= 2.4150422) {
            if (input[3] >= 0.43045312) {
                if (input[0] >= 1.3587682) {
                    if (input[1] >= 5.5485406) {
                        var6 = 0.0016217628;
                    } else {
                        var6 = 0.018412648;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var6 = 0.011002339;
                    } else {
                        var6 = 0.0014653626;
                    }
                }
            } else {
                if (input[0] >= 3.1231322) {
                    if (input[4] >= -0.9675882) {
                        var6 = 0.019472929;
                    } else {
                        var6 = 0.0;
                    }
                } else {
                    var6 = -0.0036843603;
                }
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[0] >= -1.0161169) {
                    if (input[3] >= 2.042788) {
                        var6 = 0.011644573;
                    } else {
                        var6 = 0.007894148;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var6 = 0.007613476;
                    } else {
                        var6 = 0.0012702007;
                    }
                }
            } else {
                if (input[13] >= 1.5) {
                    if (input[4] >= -0.54887086) {
                        var6 = 0.0030030145;
                    } else {
                        var6 = 0.018346021;
                    }
                } else {
                    if (input[12] >= 0.5) {
                        var6 = 0.0067479615;
                    } else {
                        var6 = 0.0049486337;
                    }
                }
            }
        }
        double var7;
        if (input[1] >= -0.9919474) {
            if (input[0] >= 0.69173473) {
                if (input[1] >= 2.4150422) {
                    if (input[3] >= 0.43045312) {
                        var7 = 0.005092508;
                    } else {
                        var7 = -0.0014717348;
                    }
                } else {
                    if (input[4] >= 0.28856382) {
                        var7 = 0.0042745755;
                    } else {
                        var7 = 0.006298272;
                    }
                }
            } else {
                if (input[5] >= 0.5) {
                    if (input[10] >= 0.5) {
                        var7 = -0.01172407;
                    } else {
                        var7 = 0.0017880978;
                    }
                } else {
                    if (input[0] >= -0.6049727) {
                        var7 = 0.004029916;
                    } else {
                        var7 = 0.002391288;
                    }
                }
            }
        } else {
            if (input[3] >= 0.43045312) {
                if (input[4] >= -0.7582295) {
                    var7 = 0.005857868;
                } else {
                    var7 = 0.012402769;
                }
            } else {
                if (input[0] >= -1.2702092) {
                    if (input[13] >= 0.5) {
                        var7 = 0.0024183746;
                    } else {
                        var7 = 0.005204008;
                    }
                } else {
                    if (input[0] >= -1.2920101) {
                        var7 = -0.013208846;
                    } else {
                        var7 = 0.002491874;
                    }
                }
            }
        }
        double var8;
        if (input[8] >= 0.5) {
            if (input[4] >= -0.9675882) {
                if (input[3] >= 0.43045312) {
                    if (input[6] >= 0.5) {
                        var8 = 0.0015688919;
                    } else {
                        var8 = 0.004505562;
                    }
                } else {
                    if (input[0] >= -1.3059134) {
                        var8 = 0.00068658957;
                    } else {
                        var8 = 0.017619982;
                    }
                }
            } else {
                if (input[1] >= 1.0524917) {
                    if (input[0] >= -0.5014064) {
                        var8 = 0.0018342463;
                    } else {
                        var8 = -0.008074141;
                    }
                } else {
                    if (input[1] >= 0.37182954) {
                        var8 = 0.016342636;
                    } else {
                        var8 = 0.006002292;
                    }
                }
            }
        } else {
            if (input[1] >= -0.9919474) {
                if (input[12] >= 0.5) {
                    if (input[1] >= 0.37182954) {
                        var8 = -0.0017679675;
                    } else {
                        var8 = 0.0025272183;
                    }
                } else {
                    if (input[4] >= -0.9675882) {
                        var8 = -0.0010466316;
                    } else {
                        var8 = 0.0025566723;
                    }
                }
            } else {
                if (input[4] >= -0.7582295) {
                    if (input[0] >= -1.1018075) {
                        var8 = 0.0030756698;
                    } else {
                        var8 = -0.0067251194;
                    }
                } else {
                    if (input[4] >= -0.9675882) {
                        var8 = 0.011041195;
                    } else {
                        var8 = 0.0030340012;
                    }
                }
            }
        }
        double var9;
        if (input[1] >= 1.0524917) {
            if (input[3] >= 3.6551228) {
                var9 = 0.0054739127;
            } else {
                if (input[0] >= -0.7559242) {
                    var9 = -0.0013600625;
                } else {
                    var9 = 0.002234003;
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[4] >= -0.9675882) {
                    if (input[13] >= 0.5) {
                        var9 = -0.0020825728;
                    } else {
                        var9 = 0.0010971393;
                    }
                } else {
                    if (input[1] >= -0.31005895) {
                        var9 = -0.016336514;
                    } else {
                        var9 = -0.00038177441;
                    }
                }
            } else {
                if (input[4] >= -0.9675882) {
                    if (input[13] >= 1.5) {
                        var9 = 0.005066076;
                    } else {
                        var9 = 0.0010869269;
                    }
                } else {
                    if (input[2] >= -0.6853085) {
                        var9 = -0.013702775;
                    } else {
                        var9 = 0.0033015124;
                    }
                }
            }
        }
        double var10;
        if (input[0] >= -0.13892415) {
            if (input[0] >= 1.7739321) {
                if (input[3] >= 0.43045312) {
                    var10 = 0.005935202;
                } else {
                    if (input[7] >= 0.5) {
                        var10 = 0.0011748275;
                    } else {
                        var10 = 0.0058277114;
                    }
                }
            } else {
                if (input[11] >= 0.5) {
                    var10 = 0.00032980554;
                } else {
                    if (input[0] >= 0.25779647) {
                        var10 = 0.00085112045;
                    } else {
                        var10 = 0.002490375;
                    }
                }
            }
        } else {
            if (input[4] >= -0.9675882) {
                if (input[11] >= 0.5) {
                    if (input[8] >= 0.5) {
                        var10 = 0.0006667676;
                    } else {
                        var10 = -0.0023945272;
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var10 = -0.0018931887;
                    } else {
                        var10 = 0.0009328617;
                    }
                }
            } else {
                if (input[2] >= -0.6853085) {
                    if (input[0] >= -1.290024) {
                        var10 = -0.0053442316;
                    } else {
                        var10 = -0.03491465;
                    }
                } else {
                    if (input[1] >= 1.7331536) {
                        var10 = -0.006101388;
                    } else {
                        var10 = 0.0019782195;
                    }
                }
            }
        }
        double var11;
        if (input[2] >= 1.0609105) {
            if (input[0] >= 0.2916565) {
                var11 = 0.0009551052;
            } else {
                if (input[12] >= 0.5) {
                    if (input[0] >= -0.67713815) {
                        var11 = 0.0006915505;
                    } else {
                        var11 = -0.0024426722;
                    }
                } else {
                    if (input[1] >= -0.6510032) {
                        var11 = -0.007430486;
                    } else {
                        var11 = -0.0003048799;
                    }
                }
            }
        } else {
            if (input[0] >= -1.2780595) {
                if (input[3] >= 2.042788) {
                    if (input[1] >= 4.118537) {
                        var11 = -0.0016886153;
                    } else {
                        var11 = 0.003109684;
                    }
                } else {
                    if (input[1] >= 1.0524917) {
                        var11 = -0.0007451259;
                    } else {
                        var11 = 0.0006387596;
                    }
                }
            } else {
                if (input[0] >= -1.2989146) {
                    if (input[4] >= -0.54887086) {
                        var11 = 0.0030642208;
                    } else {
                        var11 = -0.013004604;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var11 = -0.0104829725;
                    } else {
                        var11 = 0.0020879775;
                    }
                }
            }
        }
        double var12;
        if (input[4] >= 2.8008678) {
            var12 = -0.006274494;
        } else {
            if (input[13] >= 2.5) {
                if (input[0] >= 0.3909194) {
                    var12 = -0.0012760408;
                } else {
                    if (input[4] >= -0.33951217) {
                        var12 = -0.0028075294;
                    } else {
                        var12 = 0.01094564;
                    }
                }
            } else {
                if (input[1] >= -0.9919474) {
                    if (input[0] >= 0.41967207) {
                        var12 = 0.0008271246;
                    } else {
                        var12 = -0.00009427229;
                    }
                } else {
                    if (input[4] >= -1.1769469) {
                        var12 = 0.0010029379;
                    } else {
                        var12 = -0.0023831169;
                    }
                }
            }
        }
        double var13;
        if (input[13] >= 0.5) {
            if (input[2] >= -0.6853085) {
                if (input[0] >= -0.7032898) {
                    if (input[4] >= -0.54887086) {
                        var13 = 0.00038655053;
                    } else {
                        var13 = -0.015608075;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var13 = -0.0009661109;
                    } else {
                        var13 = -0.012938993;
                    }
                }
            } else {
                if (input[9] >= 0.5) {
                    if (input[4] >= -0.7582295) {
                        var13 = 0.002776509;
                    } else {
                        var13 = 0.009527779;
                    }
                } else {
                    if (input[4] >= -0.9675882) {
                        var13 = 0.0006959455;
                    } else {
                        var13 = 0.0051622675;
                    }
                }
            }
        } else {
            if (input[12] >= 0.5) {
                if (input[2] >= -0.6853085) {
                    if (input[13] >= -0.5) {
                        var13 = 0.0006121909;
                    } else {
                        var13 = -0.0033892363;
                    }
                } else {
                    if (input[4] >= 0.079205155) {
                        var13 = 0.0;
                    } else {
                        var13 = -0.013930724;
                    }
                }
            } else {
                if (input[4] >= 0.49792248) {
                    if (input[3] >= 0.43045312) {
                        var13 = -0.014886638;
                    } else {
                        var13 = -0.0012186293;
                    }
                } else {
                    if (input[13] >= -0.5) {
                        var13 = -0.0002967941;
                    } else {
                        var13 = 0.0015414233;
                    }
                }
            }
        }
        double var14;
        if (input[0] >= -1.3174524) {
            if (input[0] >= -1.3098387) {
                if (input[4] >= -0.33951217) {
                    if (input[4] >= -0.13015352) {
                        var14 = 0.0001996497;
                    } else {
                        var14 = 0.0021845878;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var14 = -0.00197855;
                    } else {
                        var14 = 0.00010094449;
                    }
                }
            } else {
                var14 = 0.00894484;
            }
        } else {
            if (input[0] >= -1.3210466) {
                var14 = -0.02221397;
            } else {
                if (input[2] >= 0.18780094) {
                    var14 = -0.01856024;
                } else {
                    var14 = 0.004551255;
                }
            }
        }
        double var15;
        if (input[4] >= -1.1769469) {
            if (input[4] >= 2.3821504) {
                var15 = -0.0039598164;
            } else {
                if (input[13] >= -0.5) {
                    var15 = 0.000016511296;
                } else {
                    if (input[2] >= 0.18780094) {
                        var15 = -0.001870308;
                    } else {
                        var15 = 0.0018162188;
                    }
                }
            }
        } else {
            if (input[1] >= 0.37182954) {
                var15 = -0.008556665;
            } else {
                var15 = -0.0011090295;
            }
        }
        double var16;
        if (input[0] >= -1.3390169) {
            if (input[0] >= -1.3174524) {
                if (input[0] >= -1.303549) {
                    var16 = 0.000029354644;
                } else {
                    if (input[1] >= -0.6510032) {
                        var16 = 0.00927405;
                    } else {
                        var16 = -0.0071371323;
                    }
                }
            } else {
                var16 = -0.007219896;
            }
        } else {
            var16 = 0.012040465;
        }
        double var17;
        if (input[0] >= 2.8495088) {
            var17 = 0.0020440246;
        } else {
            if (input[1] >= 2.4150422) {
                if (input[0] >= 2.1712675) {
                    if (input[0] >= 2.4560513) {
                        var17 = -0.0021530977;
                    } else {
                        var17 = -0.01269372;
                    }
                } else {
                    var17 = -0.0012458734;
                }
            } else {
                if (input[0] >= 2.8206143) {
                    var17 = -0.007931508;
                } else {
                    var17 = 0.00005560837;
                }
            }
        }
        double var18;
        if (input[0] >= -1.2794309) {
            if (input[0] >= -1.2655747) {
                if (input[0] >= -1.2642033) {
                    var18 = -0.00000221176;
                } else {
                    var18 = -0.017227829;
                }
            } else {
                if (input[12] >= 0.5) {
                    var18 = 0.015523691;
                } else {
                    if (input[0] >= -1.2673244) {
                        var18 = 0.019265637;
                    } else {
                        var18 = 0.00008436395;
                    }
                }
            }
        } else {
            if (input[1] >= 0.37182954) {
                var18 = -0.019434096;
            } else {
                if (input[0] >= -1.2838289) {
                    var18 = -0.015135375;
                } else {
                    var18 = -0.00008667827;
                }
            }
        }
        double var19;
        if (input[0] >= 1.7739321) {
            if (input[7] >= 0.5) {
                var19 = 0.0003697489;
            } else {
                var19 = 0.0036315368;
            }
        } else {
            if (input[0] >= 1.7672169) {
                var19 = -0.013333117;
            } else {
                if (input[1] >= -0.9919474) {
                    if (input[10] >= 0.5) {
                        var19 = -0.0033602347;
                    } else {
                        var19 = -0.000051426872;
                    }
                } else {
                    var19 = 0.00042006403;
                }
            }
        }
        double var20;
        if (input[3] >= 3.6551228) {
            if (input[6] >= 0.5) {
                if (input[5] >= 0.5) {
                    var20 = -0.01681069;
                } else {
                    if (input[13] >= -1.5) {
                        var20 = -0.0027248317;
                    } else {
                        var20 = 0.011448342;
                    }
                }
            } else {
                if (input[0] >= -1.23389) {
                    if (input[13] >= -1.5) {
                        var20 = 0.004375306;
                    } else {
                        var20 = -0.004127439;
                    }
                } else {
                    var20 = 0.02368769;
                }
            }
        } else {
            if (input[1] >= 4.118537) {
                if (input[0] >= -0.09324145) {
                    if (input[0] >= 0.30839735) {
                        var20 = -0.0031943435;
                    } else {
                        var20 = 0.009536413;
                    }
                } else {
                    var20 = -0.015712563;
                }
            } else {
                if (input[13] >= -0.5) {
                    var20 = -0.000042025626;
                } else {
                    if (input[0] >= -0.3583997) {
                        var20 = -0.0021586758;
                    } else {
                        var20 = 0.0023396637;
                    }
                }
            }
        }
        double var21;
        if (input[0] >= -0.13892415) {
            if (input[7] >= 0.5) {
                if (input[3] >= 0.43045312) {
                    if (input[0] >= 1.7189332) {
                        var21 = 0.0032777044;
                    } else {
                        var21 = 0.00036917996;
                    }
                } else {
                    var21 = -0.00004005292;
                }
            } else {
                if (input[1] >= 0.37182954) {
                    var21 = -0.007785443;
                } else {
                    if (input[11] >= 0.5) {
                        var21 = 0.00042813376;
                    } else {
                        var21 = 0.002959837;
                    }
                }
            }
        } else {
            if (input[6] >= 0.5) {
                if (input[2] >= -0.6853085) {
                    if (input[13] >= 0.5) {
                        var21 = -0.004236799;
                    } else {
                        var21 = -0.00021642142;
                    }
                } else {
                    if (input[1] >= 0.37182954) {
                        var21 = 0.004996732;
                    } else {
                        var21 = -0.007446449;
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[1] >= 0.37182954) {
                        var21 = 0.0048965244;
                    } else {
                        var21 = 0.00050569046;
                    }
                } else {
                    if (input[1] >= 0.37182954) {
                        var21 = -0.001626061;
                    } else {
                        var21 = -0.0000027492733;
                    }
                }
            }
        }
        double var22;
        if (input[12] >= 0.5) {
            if (input[1] >= 1.7331536) {
                if (input[0] >= 1.8924897) {
                    if (input[0] >= 2.8171148) {
                        var22 = 0.0073419716;
                    } else {
                        var22 = -0.015126595;
                    }
                } else {
                    var22 = 0.0074178805;
                }
            } else {
                if (input[0] >= -1.2075019) {
                    if (input[0] >= -1.1956792) {
                        var22 = 0.00014508606;
                    } else {
                        var22 = -0.013137202;
                    }
                } else {
                    var22 = 0.0039140643;
                }
            }
        } else {
            if (input[8] >= 0.5) {
                if (input[2] >= -0.6853085) {
                    if (input[5] >= 0.5) {
                        var22 = 0.0;
                    } else {
                        var22 = -0.0104733845;
                    }
                } else {
                    if (input[0] >= -1.0112933) {
                        var22 = -0.00010445105;
                    } else {
                        var22 = -0.0022857368;
                    }
                }
            } else {
                if (input[0] >= -1.024251) {
                    if (input[0] >= -1.019522) {
                        var22 = -0.000108420914;
                    } else {
                        var22 = -0.01706931;
                    }
                } else {
                    if (input[0] >= -1.0612321) {
                        var22 = 0.00603064;
                    } else {
                        var22 = -0.00012386522;
                    }
                }
            }
        }
        double var23;
        if (input[0] >= -1.2880378) {
            if (input[0] >= -1.2838289) {
                if (input[4] >= -0.9675882) {
                    if (input[2] >= -0.6853085) {
                        var23 = 0.00011244611;
                    } else {
                        var23 = -0.00063757895;
                    }
                } else {
                    if (input[13] >= 0.5) {
                        var23 = 0.004793601;
                    } else {
                        var23 = 0.0002438121;
                    }
                }
            } else {
                var23 = 0.007935866;
            }
        } else {
            if (input[2] >= -0.6853085) {
                if (input[4] >= -0.86290884) {
                    var23 = -0.00359195;
                } else {
                    var23 = -0.016440112;
                }
            } else {
                if (input[6] >= 0.5) {
                    var23 = -0.008749929;
                } else {
                    if (input[4] >= -0.9675882) {
                        var23 = -0.0039069047;
                    } else {
                        var23 = 0.0044760644;
                    }
                }
            }
        }
        double var24;
        if (input[5] >= 0.5) {
            if (input[0] >= -1.0317702) {
                if (input[0] >= -1.0003219) {
                    if (input[0] >= -0.9968697) {
                        var24 = -0.0002358972;
                    } else {
                        var24 = -0.017216112;
                    }
                } else {
                    var24 = 0.006312089;
                }
            } else {
                if (input[7] >= 0.5) {
                    if (input[0] >= -1.0592461) {
                        var24 = -0.008785895;
                    } else {
                        var24 = -0.0011284654;
                    }
                } else {
                    if (input[0] >= -1.1571374) {
                        var24 = -0.008345505;
                    } else {
                        var24 = -0.026275601;
                    }
                }
            }
        } else {
            if (input[4] >= -0.33951217) {
                if (input[0] >= -0.92271805) {
                    var24 = 0.00032584448;
                } else {
                    if (input[0] >= -1.0422213) {
                        var24 = 0.0051156264;
                    } else {
                        var24 = 0.0006338778;
                    }
                }
            } else {
                if (input[3] >= 0.43045312) {
                    if (input[13] >= 0.5) {
                        var24 = 0.0020362006;
                    } else {
                        var24 = -0.00009133204;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var24 = -0.000396655;
                    } else {
                        var24 = 0.001698077;
                    }
                }
            }
        }
        double var25;
        if (input[13] >= 0.5) {
            if (input[1] >= 1.7331536) {
                if (input[4] >= -0.9675882) {
                    if (input[3] >= 0.43045312) {
                        var25 = 0.005885267;
                    } else {
                        var25 = -0.0042739417;
                    }
                } else {
                    var25 = 0.015148019;
                }
            } else {
                if (input[4] >= -0.7582295) {
                    if (input[0] >= -1.2284516) {
                        var25 = -0.0007312411;
                    } else {
                        var25 = 0.010083653;
                    }
                } else {
                    if (input[11] >= 0.5) {
                        var25 = 0.00057563157;
                    } else {
                        var25 = 0.004433172;
                    }
                }
            }
        } else {
            if (input[4] >= -0.54887086) {
                if (input[10] >= 0.5) {
                    if (input[1] >= -0.6510032) {
                        var25 = -0.004457086;
                    } else {
                        var25 = 0.00005184911;
                    }
                } else {
                    if (input[0] >= -0.42328236) {
                        var25 = -0.0000844143;
                    } else {
                        var25 = 0.0008871678;
                    }
                }
            } else {
                if (input[4] >= -0.9675882) {
                    if (input[0] >= -0.3754716) {
                        var25 = 0.0012323484;
                    } else {
                        var25 = -0.002873566;
                    }
                } else {
                    if (input[0] >= -1.0115771) {
                        var25 = -0.00014105397;
                    } else {
                        var25 = 0.0017620121;
                    }
                }
            }
        }
        double var26;
        if (input[0] >= -1.1709462) {
            if (input[0] >= -1.1589344) {
                if (input[0] >= -1.1452203) {
                    var26 = -0.000024736897;
                } else {
                    if (input[1] >= -0.9919474) {
                        var26 = -0.005339584;
                    } else {
                        var26 = 0.0051447884;
                    }
                }
            } else {
                var26 = 0.006056288;
            }
        } else {
            if (input[0] >= -1.173027) {
                var26 = -0.031068884;
            } else {
                if (input[0] >= -1.1739728) {
                    var26 = 0.015467568;
                } else {
                    var26 = -0.0009145012;
                }
            }
        }
        double var27;
        if (input[1] >= 2.4150422) {
            if (input[0] >= -0.53521913) {
                if (input[5] >= 0.5) {
                    var27 = 0.0056858375;
                } else {
                    if (input[0] >= -0.4375168) {
                        var27 = -0.001901516;
                    } else {
                        var27 = -0.023182597;
                    }
                }
            } else {
                var27 = 0.0021745549;
            }
        } else {
            if (input[3] >= 3.6551228) {
                if (input[4] >= -0.9675882) {
                    if (input[4] >= -0.7582295) {
                        var27 = 0.001146483;
                    } else {
                        var27 = 0.008516337;
                    }
                } else {
                    var27 = -0.009465358;
                }
            } else {
                if (input[4] >= -0.9675882) {
                    if (input[1] >= 0.37182954) {
                        var27 = -0.0016486235;
                    } else {
                        var27 = 0.00003022694;
                    }
                } else {
                    if (input[6] >= 0.5) {
                        var27 = -0.0037639672;
                    } else {
                        var27 = 0.00052365253;
                    }
                }
            }
        }
        double var28;
        if (input[0] >= -1.0845463) {
            if (input[0] >= -1.0775948) {
                if (input[0] >= -1.0469031) {
                    var28 = 0.000059530634;
                } else {
                    if (input[4] >= -0.7582295) {
                        var28 = -0.003496614;
                    } else {
                        var28 = 0.0011808056;
                    }
                }
            } else {
                if (input[3] >= 0.43045312) {
                    var28 = 0.0010689845;
                } else {
                    if (input[4] >= -0.9675882) {
                        var28 = -0.0068786554;
                    } else {
                        var28 = -0.025542336;
                    }
                }
            }
        } else {
            if (input[0] >= -1.0890391) {
                var28 = 0.011510122;
            } else {
                if (input[10] >= 0.5) {
                    var28 = -0.00584546;
                } else {
                    if (input[7] >= 0.5) {
                        var28 = 0.0005602566;
                    } else {
                        var28 = 0.011537886;
                    }
                }
            }
        }
        double var29;
        if (input[5] >= 0.5) {
            if (input[2] >= -0.6853085) {
                var29 = -0.00016906073;
            } else {
                if (input[1] >= 1.3934358) {
                    var29 = 0.015128375;
                } else {
                    var29 = -0.0032959697;
                }
            }
        } else {
            var29 = 0.00012784384;
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + var14 + var15 + var16 + var17 + var18 + var19 + var20 + var21 + var22 + var23 + var24 + var25 + var26 + var27 + var28 + var29);
    }
}
