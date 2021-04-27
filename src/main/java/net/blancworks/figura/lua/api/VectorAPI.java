package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;

public class VectorAPI implements LuaAPI {
    private static final VectorAPI INSTANCE = new VectorAPI();
    public static VectorAPI getInstance() {
        return INSTANCE;
    }

    public static final ArrayList<String> componentNames = new ArrayList<String>() {{
        add("x,u,r");
        add("y,v,g");
        add("z,b");
        add("w,a");
        add("t");
        add("h");
    }};
    private static ReadOnlyLuaTable globalLuaTable;
    private static World lastWorld;

    private static World getWorld() {
        return MinecraftClient.getInstance().world;
    }

    public Identifier getID() {
        return new Identifier("default", "vectors");
    }

    public ReadOnlyLuaTable getForScript(CustomScript script) {

        if (getWorld() != lastWorld)
            updateGlobalTable();

        return globalLuaTable;
    }

    public static void updateGlobalTable() {
        globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
            set("worldToPart", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vector3f arg1 = checkVec3(arg);

                    arg1.scale(16);
                    arg1.set(-arg1.getX(), -arg1.getY(), arg1.getZ());

                    return vec3fToLua(arg1);
                }
            });

            set("getVector", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    int count = arg.checkint();

                    return getVector(count);
                }
            });

            set("hsvToRGB", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vector3f hsv = checkVec3(arg);

                    return getVector(HSVToRGB(hsv));
                }
            });


            set("rgbToHSV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vector3f rgb = checkVec3(arg);

                    return getVector(RGBToHSV(rgb));
                }
            });

        }});
    }

    public static Vector3f checkVec3(LuaValue val) {
        LuaTable table = val.checktable();

        if (table.length() != 3)
            LuaValue.error("Invalid number of components in vector, 3 expected.");

        Vector3f ret = new Vector3f(
                table.get(1).checknumber().tofloat(),
                table.get(2).checknumber().tofloat(),
                table.get(3).checknumber().tofloat()
        );

        return ret;
    }

    public static Vector4f checkVec4f(LuaValue val) {
        LuaTable table = val.checktable();

        if (table.length() != 4)
            LuaValue.error("Invalid number of components in vector, 4 expected.");

        Vector4f ret = new Vector4f(
                table.get(1).checknumber().tofloat(),
                table.get(2).checknumber().tofloat(),
                table.get(3).checknumber().tofloat(),
                table.get(4).checknumber().tofloat()
        );

        return ret;
    }

    public static Vec2f checkVec2f(LuaValue val) {
        LuaTable table = val.checktable();

        if (table.length() != 2)
            LuaValue.error("Invalid number of components in vector, 2 expected.");

        Vec2f ret = new Vec2f(
                table.get(1).checknumber().tofloat(),
                table.get(2).checknumber().tofloat()
        );

        return ret;
    }

    public static LuaValue vec3fToLua(Vector3f v) {
        LuaTable table = getVector(3);
        table.set("x", LuaValue.valueOf(v.getX()));
        table.set("y", LuaValue.valueOf(v.getY()));
        table.set("z", LuaValue.valueOf(v.getZ()));
        return table;
    }

    public static LuaTable getVector(Vector3f v) {
        LuaTable ret = getVector(3);
        ret.set("x", v.getX());
        ret.set("y", v.getY());
        ret.set("z", v.getZ());
        return ret;
    }

    public static LuaTable getVector(int componentCount) {
        LuaTable ret = new LuaTable();
        LuaTable metaTableSource = new LuaTable();
        LuaTable altAccessors = new LuaTable();

        LuaTable components = new LuaTable();

        componentCount = Math.min(componentCount, componentNames.size());

        for (int i = 0; i < componentCount; i++) {
            String name = componentNames.get(i);

            components.set(i, LuaValue.valueOf(0));

            for (String s : name.split(",")) {
                altAccessors.set(s, i + 1);
            }
        }

        ret.set("length", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                double total = 0;

                for (LuaValue key : components.keys()) {
                    LuaValue value = components.get(key);

                    float val = value.tofloat();

                    total += val * val;
                }

                return LuaValue.valueOf(Math.sqrt(total));
            }
        });

        ret.set("lengthSqr", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                double total = 0;

                for (LuaValue key : components.keys()) {
                    LuaValue value = components.get(key);

                    float val = value.tofloat();

                    total += val * val;
                }

                return LuaValue.valueOf(total);
            }
        });

        metaTableSource.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                LuaTable tbl = table.checktable();

                if (!key.isint()) {
                    LuaTable alts = tbl.get("alt_accessors").checktable();
                    LuaValue alt = alts.get(key);

                    if (!alt.isnil()) {
                        return components.rawget(alt);
                    }
                }

                return components.rawget(key);
            }
        });

        metaTableSource.set("__newindex", new ThreeArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key, LuaValue value) {
                LuaTable tbl = table.checktable();

                if (!key.isint()) {
                    LuaTable alts = tbl.get("alt_accessors").checktable();
                    LuaValue alt = alts.get(key);

                    if (!alt.isnil()) {
                        components.rawset(alt.tonumber().toint(), value);
                        return LuaBoolean.valueOf(true);
                    }
                } else {
                    components.rawset(key, value);
                }

                return LuaBoolean.valueOf(true);
            }
        });

        ret.set("components", components);
        ret.set("alt_accessors", altAccessors);
        ret.setmetatable(new ReadOnlyLuaTable(metaTableSource));
        return ret;
    }

    public static Vector3f RGBToHSV(Vector3f rgbColor) {
        // when blue is highest valued
        if ((rgbColor.getX() > rgbColor.getY()) && (rgbColor.getZ() > rgbColor.getX()))
            return RGBToHSVHelper((float) 4, rgbColor.getZ(), rgbColor.getX(), rgbColor.getY());
            //when green is highest valued
        else if (rgbColor.getY() > rgbColor.getX())
            return RGBToHSVHelper((float) 2, rgbColor.getY(), rgbColor.getZ(), rgbColor.getX());
            //when red is highest valued
        else
            return RGBToHSVHelper((float) 0, rgbColor.getX(), rgbColor.getY(), rgbColor.getZ());
    }

    public static Vector3f RGBToHSVHelper(float offset, float dominantcolor, float colorone, float colortwo) {
        float V = dominantcolor;
        float S = 0;
        float H = 0;
        //we need to find out which is the minimum color
        if (V != 0) {
            //we check which color is smallest
            float small = 0;
            if (colorone > colortwo) small = colortwo;
            else small = colorone;

            float diff = V - small;

            //if the two values are not the same, we compute the like this
            if (diff != 0) {
                //S = max-min/max
                S = diff / V;
                //H = hue is offset by X, and is the difference between the two smallest colors
                H = offset + ((colorone - colortwo) / diff);
            } else {
                //S = 0 when the difference is zero
                S = 0;
                //H = 4 + (R-G) hue is offset by 4 when blue, and is the difference between the two smallest colors
                H = offset + (colorone - colortwo);
            }

            H /= 6;

            //conversion values
            if (H < 0)
                H += 1.0f;
        } else {
            S = 0;
            H = 0;
        }

        return new Vector3f(H, S, V);
    }

    public static Vector3f HSVToRGB(Vector3f hsv) {
        return HSVToRGB(hsv.getX(), hsv.getY(), hsv.getZ(), true);
    }

    // Convert a set of HSV values to an RGB Color.
    public static Vector3f HSVToRGB(float H, float S, float V, boolean hdr) {
        float R;
        float G;
        float B;

        if (S == 0) {
            R = V;
            G = V;
            B = V;
        } else if (V == 0) {
            R = 0;
            G = 0;
            B = 0;
        } else {
            R = 0;
            G = 0;
            B = 0;

            //crazy hsv conversion
            float t_S, t_V, h_to_floor;

            t_S = S;
            t_V = V;
            h_to_floor = H * 6.0f;

            int temp = (int) Math.floor(h_to_floor);
            float t = h_to_floor - ((float) temp);
            float var_1 = (t_V) * (1 - t_S);
            float var_2 = t_V * (1 - t_S * t);
            float var_3 = t_V * (1 - t_S * (1 - t));

            switch (temp) {
                case 0:
                    R = t_V;
                    G = var_3;
                    B = var_1;
                    break;

                case 1:
                    R = var_2;
                    G = t_V;
                    B = var_1;
                    break;

                case 2:
                    R = var_1;
                    G = t_V;
                    B = var_3;
                    break;

                case 3:
                    R = var_1;
                    G = var_2;
                    B = t_V;
                    break;

                case 4:
                    R = var_3;
                    G = var_1;
                    B = t_V;
                    break;

                case 5:
                    R = t_V;
                    G = var_1;
                    B = var_2;
                    break;

                case 6:
                    R = t_V;
                    G = var_3;
                    B = var_1;
                    break;

                case -1:
                    R = t_V;
                    G = var_1;
                    B = var_2;
                    break;
            }

            if (!hdr) {
                R = MathHelper.clamp(R, 0.0f, 1.0f);
                G = MathHelper.clamp(G, 0.0f, 1.0f);
                B = MathHelper.clamp(B, 0.0f, 1.0f);
            }
        }
        return new Vector3f(R, G, B);
    }

}
