package net.blancworks.figura.lua.api.model;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.LuaAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class ArmorModelAPI implements LuaAPI {
    private static final ArmorModelAPI INSTANCE = new ArmorModelAPI();
    public static ArmorModelAPI getInstance() {
        return INSTANCE;
    }

    public static final String VANILLA_HELMET = "HELMET";
    public static final String VANILLA_CHESTPLATE = "CHESTPLATE";
    public static final String VANILLA_LEGGINGS = "LEGGINGS";
    public static final String VANILLA_BOOTS = "BOOTS";

    
    public Identifier getID() {
        return new Identifier("default", "armor_model");
    }

    public ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            PlayerEntityModel mdl = script.playerData.vanillaModel;

            set(VANILLA_HELMET, getTableForPart(VANILLA_HELMET, script));

            set(VANILLA_CHESTPLATE, getTableForPart(VANILLA_CHESTPLATE, script));
            set(VANILLA_LEGGINGS, getTableForPart(VANILLA_LEGGINGS, script));

            set(VANILLA_BOOTS, getTableForPart(VANILLA_BOOTS, script));
        }});

        return producedTable;
    }

    public static ReadOnlyLuaTable getTableForPart(String accessor, CustomScript script) {
        ArmorPartTable producedTable = new ArmorPartTable(accessor, script);
        return producedTable;
    }

    private static class ArmorPartTable extends ScriptLocalAPITable {
        String accessor;

        public ArmorPartTable(String accessor, CustomScript script) {
            super(script);
            this.accessor = accessor;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();

            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vector3f v = targetScript.getOrMakePartCustomization(accessor).pos;

                    if(v == null)
                        return NIL;

                    return LuaUtils.getTableFromVector3f(v);
                }
            });
            
            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.pos = new Vector3f(
                            fas.getFloat(0),
                            fas.getFloat(1),
                            fas.getFloat(2)
                    );

                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Vector3f v = targetScript.getOrMakePartCustomization(accessor).rot;

                    if (v == null)
                        return NIL;

                    return LuaUtils.getTableFromVector3f(v);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    FloatArrayList fas = LuaUtils.getFloatsFromTable(arg1.checktable());
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);
                    customization.rot = new Vector3f(
                            fas.getFloat(0),
                            fas.getFloat(1),
                            fas.getFloat(2)
                    );
                    return NIL;
                }
            });

            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);

                    if (customization != null)
                        return LuaBoolean.valueOf(customization.visible);

                    return NIL;
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    VanillaModelPartCustomization customization = targetScript.getOrMakePartCustomization(accessor);

                    if (arg.isnil()) {
                        customization.visible = null;
                        return NIL;
                    }

                    customization.visible = arg.checkboolean();

                    return NIL;
                }
            });

            return ret;
        }
    }

}
