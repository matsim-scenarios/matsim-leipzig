package org.matsim.run.prepare.network;
import org.matsim.application.prepare.network.opt.FeatureRegressor;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
    
/**
* Generated model, do not modify.
*/
public final class LeipzigNetworkParams_speedRelative_traffic_light implements FeatureRegressor {
    
    public static LeipzigNetworkParams_speedRelative_traffic_light INSTANCE = new LeipzigNetworkParams_speedRelative_traffic_light();
    public static final double[] DEFAULT_PARAMS = {-0.0005853415, -0.056984592, 0.10556553, 0.059261553, -0.0070313965, 0.04247025, -0.10854561, -0.06721704, -0.046254724, 0.0018164597, -0.14827141, -0.021646218, -0.096079, 0.0075520137, 0.0243951, -0.0074252463, -0.040037293, -0.0020486235, -0.06218741, -0.09015097, -0.008085187, 0.07048607, 0.029819319, -0.024231883, 0.01267586, -0.036964916, 0.01276994, -0.01598942, -0.070422806, 0.0026384317, -0.076450765, 0.022511918, -0.0060494463, 0.03161604, -0.042578764, -0.013000462, 0.026894731, -0.017964019, 0.009706211, -0.00983027, -0.02897729, 0.0, -0.06472101, 0.012978244, 0.036895797, 0.0, -0.027868774, -0.014294908, 0.023026736, 0.021313682, -0.01393062, 0.012230705, 0.02504618, -0.01114704, 0.0, 0.038856175, -0.034876816, 0.015382692, -0.006861806, -0.01844896, 0.0, 0.07883432, -0.042563304, -0.020757928, -0.04518106, -0.03687191, 0.01544447, 0.0, 0.018230865, -0.0034691815, 0.009732502, 0.012406356, -0.0041996036, -0.003492154, 0.020188902, -0.0112725515, 0.0013578036, 0.009781743, 0.0, -0.035242002, -0.008478101, 0.042960852, 0.013988037, -0.003570727, -0.014108883, 0.0, 0.020749517, -0.015349007, 0.0021983695, 0.09652796, 0.0, -0.018573502, -0.0028159423, -0.0017987072, 0.012038754, 0.0029114375, -0.011215173, -0.003047508, 0.020411523, -0.004478382, 0.04531501, 0.0, -0.045304935, -0.012436348, 0.0058259135, 0.012321003, -0.016223243, 0.0, -0.023454629, 0.0043456205, 0.01384394, -0.0059106247, 0.0, 0.02135136, -0.00305764, 0.0038347442, -0.02744456, 0.010154057, -0.021041408, 0.039345693, 0.0, 0.0036288523, -0.013137056, -0.0043147895, 0.013559428, 0.021517863, -0.017456925, 1.1231075e-05, 0.059048515, 0.0003549934, -0.008710844, 0.0011202805, 0.0, -0.011636807, 0.0, 0.017107576, -0.0053847493, 0.0, 0.0008258014, 0.043530498, 0.0, 0.019235363, 0.0, -0.025610417, 0.0017611998, 0.00661128, -0.008221263, 0.0032480236, -0.0017702815, 0.00029704956, -0.015606487, 0.003970893, -0.011909077, 0.012954333, 0.001385998, 0.0, 0.022702625, -0.0057645044, -0.018013347, 0.0025046794, 0.0, 0.009367706, -0.0009032354, 0.0, 0.045524105, 0.00064976624, 0.0, 0.0, -0.016959198, 0.0, 0.0023774903, -0.0058948873, -1.1108639e-05, 0.0, 0.037927557, -0.0010516223, 0.0067169513, 0.0073235715, -0.025543846, -0.0008636431, 0.0, 0.057537228, 0.0, -0.009359605, 4.4206e-05, 0.0041542617, -0.027370531, 0.0, 0.011559264, -0.0006627706, -0.004633205, 0.0011867727, -0.009559247, 0.0012475067, -0.0024899081, 0.023071319};

    @Override
    public double predict(Object2DoubleMap<String> ft) {
        return predict(ft, DEFAULT_PARAMS);
    }
    
    @Override
    public double[] getData(Object2DoubleMap<String> ft) {
        double[] data = new double[14];
		data[0] = (ft.getDouble("length") - 90.39818584070798) / 68.35523008895615;
		data[1] = (ft.getDouble("speed") - 14.009761061946904) / 2.401915245291448;
		data[2] = (ft.getDouble("num_lanes") - 1.9292035398230087) / 0.826730570650225;
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
        if (input[0] >= -0.009775197) {
            if (input[6] >= 7.5) {
                if (input[0] >= 0.5900326) {
                    var0 = params[0];
                } else {
                    var0 = params[1];
                }
            } else {
                if (input[0] >= 0.71518767) {
                    if (input[0] >= 2.014064) {
                        var0 = params[2];
                    } else {
                        var0 = params[3];
                    }
                } else {
                    if (input[6] >= 5.5) {
                        var0 = params[4];
                    } else {
                        var0 = params[5];
                    }
                }
            }
        } else {
            if (input[0] >= -0.52070904) {
                if (input[6] >= 6.5) {
                    if (input[6] >= 7.5) {
                        var0 = params[6];
                    } else {
                        var0 = params[7];
                    }
                } else {
                    if (input[6] >= 4.5) {
                        var0 = params[8];
                    } else {
                        var0 = params[9];
                    }
                }
            } else {
                if (input[6] >= 5.5) {
                    if (input[0] >= -1.1253445) {
                        var0 = params[10];
                    } else {
                        var0 = params[11];
                    }
                } else {
                    if (input[6] >= 2.5) {
                        var0 = params[12];
                    } else {
                        var0 = params[13];
                    }
                }
            }
        }
        double var1;
        if (input[6] >= 6.5) {
            if (input[0] >= -0.31977344) {
                if (input[0] >= 0.3574242) {
                    if (input[0] >= 1.3871186) {
                        var1 = params[14];
                    } else {
                        var1 = params[15];
                    }
                } else {
                    if (input[6] >= 7.5) {
                        var1 = params[16];
                    } else {
                        var1 = params[17];
                    }
                }
            } else {
                if (input[0] >= -1.0990115) {
                    if (input[0] >= -0.68719226) {
                        var1 = params[18];
                    } else {
                        var1 = params[19];
                    }
                } else {
                    var1 = params[20];
                }
            }
        } else {
            if (input[0] >= -0.27317858) {
                if (input[0] >= 0.12562044) {
                    if (input[3] >= 5.5550003) {
                        var1 = params[21];
                    } else {
                        var1 = params[22];
                    }
                } else {
                    if (input[6] >= 5.5) {
                        var1 = params[23];
                    } else {
                        var1 = params[24];
                    }
                }
            } else {
                if (input[0] >= -0.76180255) {
                    if (input[6] >= 3.5) {
                        var1 = params[25];
                    } else {
                        var1 = params[26];
                    }
                } else {
                    if (input[4] >= 0.5) {
                        var1 = params[27];
                    } else {
                        var1 = params[28];
                    }
                }
            }
        }
        double var2;
        if (input[1] >= 1.6841722) {
            if (input[0] >= 2.5352385) {
                var2 = params[29];
            } else {
                var2 = params[30];
            }
        } else {
            if (input[0] >= 0.11991495) {
                if (input[6] >= 5.5) {
                    if (input[11] >= 0.5) {
                        var2 = params[31];
                    } else {
                        var2 = params[32];
                    }
                } else {
                    var2 = params[33];
                }
            } else {
                if (input[1] >= -1.7859752) {
                    if (input[7] >= 0.5) {
                        var2 = params[34];
                    } else {
                        var2 = params[35];
                    }
                } else {
                    var2 = params[36];
                }
            }
        }
        double var3;
        if (input[0] >= -0.22204864) {
            if (input[5] >= 2.5) {
                if (input[4] >= -0.5) {
                    if (input[6] >= 8.5) {
                        var3 = params[37];
                    } else {
                        var3 = params[38];
                    }
                } else {
                    if (input[6] >= 7.5) {
                        var3 = params[39];
                    } else {
                        var3 = params[40];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    if (input[0] >= 0.1619015) {
                        var3 = params[41];
                    } else {
                        var3 = params[42];
                    }
                } else {
                    if (input[0] >= 0.022117022) {
                        var3 = params[43];
                    } else {
                        var3 = params[44];
                    }
                }
            }
        } else {
            if (input[6] >= 6.5) {
                if (input[4] >= 0.5) {
                    var3 = params[45];
                } else {
                    var3 = params[46];
                }
            } else {
                if (input[4] >= -0.5) {
                    if (input[6] >= 2.5) {
                        var3 = params[47];
                    } else {
                        var3 = params[48];
                    }
                } else {
                    if (input[10] >= 0.5) {
                        var3 = params[49];
                    } else {
                        var3 = params[50];
                    }
                }
            }
        }
        double var4;
        if (input[0] >= 0.7329624) {
            var4 = params[51];
        } else {
            if (input[2] >= 0.69042623) {
                if (input[5] >= 2.5) {
                    if (input[5] >= 3.5) {
                        var4 = params[52];
                    } else {
                        var4 = params[53];
                    }
                } else {
                    if (input[0] >= 0.20169362) {
                        var4 = params[54];
                    } else {
                        var4 = params[55];
                    }
                }
            } else {
                if (input[3] >= 5.5550003) {
                    if (input[10] >= 0.5) {
                        var4 = params[56];
                    } else {
                        var4 = params[57];
                    }
                } else {
                    if (input[2] >= -0.5191577) {
                        var4 = params[58];
                    } else {
                        var4 = params[59];
                    }
                }
            }
        }
        double var5;
        if (input[1] >= 0.52884424) {
            if (input[4] >= 0.5) {
                if (input[8] >= 0.5) {
                    if (input[1] >= 1.6841722) {
                        var5 = params[60];
                    } else {
                        var5 = params[61];
                    }
                } else {
                    var5 = params[62];
                }
            } else {
                if (input[11] >= 0.5) {
                    var5 = params[63];
                } else {
                    var5 = params[64];
                }
            }
        } else {
            if (input[3] >= 1.39) {
                if (input[1] >= -1.7859752) {
                    var5 = params[65];
                } else {
                    var5 = params[66];
                }
            } else {
                if (input[2] >= -0.5191577) {
                    if (input[5] >= 2.5) {
                        var5 = params[67];
                    } else {
                        var5 = params[68];
                    }
                } else {
                    var5 = params[69];
                }
            }
        }
        double var6;
        if (input[6] >= 4.5) {
            if (input[0] >= -0.14275697) {
                if (input[2] >= 0.69042623) {
                    var6 = params[70];
                } else {
                    if (input[0] >= 1.8063258) {
                        var6 = params[71];
                    } else {
                        var6 = params[72];
                    }
                }
            } else {
                if (input[8] >= 0.5) {
                    if (input[0] >= -0.951254) {
                        var6 = params[73];
                    } else {
                        var6 = params[74];
                    }
                } else {
                    if (input[0] >= -0.95322895) {
                        var6 = params[75];
                    } else {
                        var6 = params[76];
                    }
                }
            }
        } else {
            if (input[1] >= 0.52884424) {
                if (input[4] >= 0.5) {
                    var6 = params[77];
                } else {
                    if (input[0] >= -0.067049526) {
                        var6 = params[78];
                    } else {
                        var6 = params[79];
                    }
                }
            } else {
                if (input[2] >= -0.5191577) {
                    if (input[3] >= 1.39) {
                        var6 = params[80];
                    } else {
                        var6 = params[81];
                    }
                } else {
                    if (input[0] >= -0.762534) {
                        var6 = params[82];
                    } else {
                        var6 = params[83];
                    }
                }
            }
        }
        double var7;
        if (input[0] >= -0.66788137) {
            if (input[6] >= 10.5) {
                var7 = params[84];
            } else {
                if (input[2] >= 0.69042623) {
                    if (input[1] >= 1.6841722) {
                        var7 = params[85];
                    } else {
                        var7 = params[86];
                    }
                } else {
                    if (input[1] >= 1.6841722) {
                        var7 = params[87];
                    } else {
                        var7 = params[88];
                    }
                }
            }
        } else {
            if (input[5] >= 3.5) {
                if (input[6] >= 10.5) {
                    var7 = params[89];
                } else {
                    var7 = params[90];
                }
            } else {
                if (input[6] >= 6.5) {
                    var7 = params[91];
                } else {
                    var7 = params[92];
                }
            }
        }
        double var8;
        if (input[0] >= -0.8184185) {
            if (input[6] >= 3.5) {
                if (input[2] >= -0.5191577) {
                    if (input[6] >= 6.5) {
                        var8 = params[93];
                    } else {
                        var8 = params[94];
                    }
                } else {
                    if (input[0] >= 1.3293322) {
                        var8 = params[95];
                    } else {
                        var8 = params[96];
                    }
                }
            } else {
                if (input[2] >= -0.5191577) {
                    var8 = params[97];
                } else {
                    var8 = params[98];
                }
            }
        } else {
            if (input[7] >= 0.5) {
                if (input[6] >= 4.5) {
                    var8 = params[99];
                } else {
                    if (input[6] >= 3.5) {
                        var8 = params[100];
                    } else {
                        var8 = params[101];
                    }
                }
            } else {
                if (input[13] >= 0.5) {
                    var8 = params[102];
                } else {
                    if (input[6] >= 3.5) {
                        var8 = params[103];
                    } else {
                        var8 = params[104];
                    }
                }
            }
        }
        double var9;
        if (input[6] >= 8.5) {
            if (input[11] >= 0.5) {
                if (input[0] >= 1.0414245) {
                    var9 = params[105];
                } else {
                    var9 = params[106];
                }
            } else {
                var9 = params[107];
            }
        } else {
            if (input[3] >= 1.385) {
                if (input[1] >= -1.7859752) {
                    if (input[0] >= -0.91029155) {
                        var9 = params[108];
                    } else {
                        var9 = params[109];
                    }
                } else {
                    if (input[0] >= -0.36709973) {
                        var9 = params[110];
                    } else {
                        var9 = params[111];
                    }
                }
            } else {
                if (input[2] >= 0.69042623) {
                    if (input[0] >= 0.04588989) {
                        var9 = params[112];
                    } else {
                        var9 = params[113];
                    }
                } else {
                    if (input[6] >= 6.5) {
                        var9 = params[114];
                    } else {
                        var9 = params[115];
                    }
                }
            }
        }
        double var10;
        if (input[3] >= 8.335) {
            if (input[0] >= -0.52568305) {
                var10 = params[116];
            } else {
                var10 = params[117];
            }
        } else {
            if (input[13] >= 0.5) {
                if (input[6] >= 2.5) {
                    var10 = params[118];
                } else {
                    if (input[4] >= 0.5) {
                        var10 = params[119];
                    } else {
                        var10 = params[120];
                    }
                }
            } else {
                if (input[2] >= -0.5191577) {
                    if (input[0] >= -0.9546919) {
                        var10 = params[121];
                    } else {
                        var10 = params[122];
                    }
                } else {
                    if (input[0] >= -0.95769095) {
                        var10 = params[123];
                    } else {
                        var10 = params[124];
                    }
                }
            }
        }
        double var11;
        if (input[0] >= 3.7499957) {
            var11 = params[125];
        } else {
            if (input[0] >= -0.9523512) {
                if (input[5] >= 3.5) {
                    var11 = params[126];
                } else {
                    var11 = params[127];
                }
            } else {
                if (input[0] >= -0.96573716) {
                    var11 = params[128];
                } else {
                    var11 = params[129];
                }
            }
        }
        double var12;
        if (input[1] >= 1.6841722) {
            var12 = params[130];
        } else {
            if (input[1] >= -1.7859752) {
                if (input[10] >= 0.5) {
                    var12 = params[131];
                } else {
                    if (input[4] >= -0.5) {
                        var12 = params[132];
                    } else {
                        var12 = params[133];
                    }
                }
            } else {
                if (input[10] >= 0.5) {
                    var12 = params[134];
                } else {
                    if (input[0] >= -0.25715935) {
                        var12 = params[135];
                    } else {
                        var12 = params[136];
                    }
                }
            }
        }
        double var13;
        if (input[0] >= -0.922946) {
            if (input[5] >= 1.5) {
                var13 = params[137];
            } else {
                if (input[2] >= -0.5191577) {
                    if (input[0] >= -0.812713) {
                        var13 = params[138];
                    } else {
                        var13 = params[139];
                    }
                } else {
                    var13 = params[140];
                }
            }
        } else {
            if (input[0] >= -1.0477352) {
                if (input[5] >= 3.5) {
                    var13 = params[141];
                } else {
                    if (input[4] >= 0.5) {
                        var13 = params[142];
                    } else {
                        var13 = params[143];
                    }
                }
            } else {
                var13 = params[144];
            }
        }
        double var14;
        if (input[3] >= 5.5550003) {
            var14 = params[145];
        } else {
            if (input[9] >= 0.5) {
                if (input[6] >= 10.5) {
                    var14 = params[146];
                } else {
                    var14 = params[147];
                }
            } else {
                var14 = params[148];
            }
        }
        double var15;
        if (input[0] >= -1.0930866) {
            if (input[0] >= -0.7869652) {
                var15 = params[149];
            } else {
                if (input[0] >= -0.8933945) {
                    var15 = params[150];
                } else {
                    if (input[0] >= -0.98892486) {
                        var15 = params[151];
                    } else {
                        var15 = params[152];
                    }
                }
            }
        } else {
            var15 = params[153];
        }
        double var16;
        if (input[2] >= -0.5191577) {
            var16 = params[154];
        } else {
            if (input[4] >= 0.5) {
                if (input[0] >= -0.5253173) {
                    var16 = params[155];
                } else {
                    if (input[0] >= -1.0640471) {
                        var16 = params[156];
                    } else {
                        var16 = params[157];
                    }
                }
            } else {
                if (input[6] >= 4.5) {
                    if (input[0] >= -1.0355928) {
                        var16 = params[158];
                    } else {
                        var16 = params[159];
                    }
                } else {
                    var16 = params[160];
                }
            }
        }
        double var17;
        if (input[0] >= 1.768655) {
            var17 = params[161];
        } else {
            if (input[0] >= -0.57081497) {
                var17 = params[162];
            } else {
                if (input[0] >= -0.637891) {
                    if (input[6] >= 5.5) {
                        var17 = params[163];
                    } else {
                        var17 = params[164];
                    }
                } else {
                    var17 = params[165];
                }
            }
        }
        double var18;
        if (input[0] >= -0.6084858) {
            if (input[0] >= -0.4198389) {
                var18 = params[166];
            } else {
                if (input[9] >= 0.5) {
                    var18 = params[167];
                } else {
                    if (input[0] >= -0.5341681) {
                        var18 = params[168];
                    } else {
                        var18 = params[169];
                    }
                }
            }
        } else {
            var18 = params[170];
        }
        double var19;
        if (input[1] >= 1.6841722) {
            var19 = params[171];
        } else {
            if (input[4] >= -1.5) {
                var19 = params[172];
            } else {
                if (input[5] >= 2.5) {
                    var19 = params[173];
                } else {
                    var19 = params[174];
                }
            }
        }
        double var20;
        if (input[6] >= 6.5) {
            var20 = params[175];
        } else {
            if (input[2] >= -0.5191577) {
                if (input[0] >= -0.94481707) {
                    var20 = params[176];
                } else {
                    if (input[1] >= 0.52884424) {
                        var20 = params[177];
                    } else {
                        var20 = params[178];
                    }
                }
            } else {
                var20 = params[179];
            }
        }
        double var21;
        if (input[8] >= 0.5) {
            if (input[6] >= 6.5) {
                if (input[0] >= -0.94211054) {
                    var21 = params[180];
                } else {
                    if (input[0] >= -1.0204513) {
                        var21 = params[181];
                    } else {
                        var21 = params[182];
                    }
                }
            } else {
                var21 = params[183];
            }
        } else {
            var21 = params[184];
        }
        double var22;
        if (input[2] >= 0.69042623) {
            if (input[6] >= 5.5) {
                var22 = params[185];
            } else {
                var22 = params[186];
            }
        } else {
            if (input[4] >= -0.5) {
                if (input[2] >= -0.5191577) {
                    if (input[6] >= 6.5) {
                        var22 = params[187];
                    } else {
                        var22 = params[188];
                    }
                } else {
                    var22 = params[189];
                }
            } else {
                var22 = params[190];
            }
        }
        double var23;
        if (input[7] >= 0.5) {
            var23 = params[191];
        } else {
            if (input[3] >= 4.165) {
                var23 = params[192];
            } else {
                if (input[2] >= 0.69042623) {
                    var23 = params[193];
                } else {
                    if (input[0] >= -1.1271) {
                        var23 = params[194];
                    } else {
                        var23 = params[195];
                    }
                }
            }
        }
        return 0.5 + (var0 + var1 + var2 + var3 + var4 + var5 + var6 + var7 + var8 + var9 + var10 + var11 + var12 + var13 + -0.00017490867 + var14 + var15 + var16 + var17 + 0.0 + var18 + 0.00040553696 + var19 + 0.00071492634 + var20 + var21 + 0.00012683406 + var22 + -0.00015865514 + var23);
    }
}
